/*
 * $Id$
 */

/*

Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.poller.v3;

import java.io.IOException;
import java.util.*;

import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.protocol.OrderedVoteBlocksIterator;
import org.lockss.protocol.VoteBlock;
import org.lockss.protocol.VoteBlocksIterator;
import org.lockss.util.*;

/**
 * Coordinate a collection of {@link VoteBlocksIterator}s so that they
 * are all advanced to the same URL at all times.
 */
final class VoteBlocksCoordinator {
  // todo(bhayes): It only makes sense to create this when
  // theParticipants has been firmed up, and won't ever
  // change. But I don't see anything in V3Poller that makes
  // sure that is so.

  private static Logger log = Logger.getLogger(VoteBlocksCoordinator.class);
    
  /* A class to link participants and iterators, so internally we control
   * the exceptions that are raised when trying to get the url. */
  private static final class Entry {
  
    /** <p>An empty, immutable VoteBlock iterator.  Calling next()
     * throws NoSuchElementException. This instance will replace a
     * voter's iterator if the iterator ever throws a known
     * exception.</p>
     */
    private static final VoteBlocksIterator ERROR_ITERATOR =
      new VoteBlocksIterator() {
	public boolean hasNext() { return false; }
	public VoteBlock next() { throw new NoSuchElementException(); }
	public VoteBlock peek() { return null; }
	public void release() { }
      };

    // Not final: will be replaced if the provided iterator throws IOException.
    private VoteBlocksIterator iter = null;
    // The current voteBlock. Changed by nextVoteBlock().
    private VoteBlock voteBlock = null;
      
    Entry(VoteBlocksIterator iter) {
      this.iter = iter == null ?
	ERROR_ITERATOR :
	new OrderedVoteBlocksIterator(iter);
      nextVoteBlock();
    }
      
    /**
     * @return true iff the iterator has thrown, or was initially null.
     */
    boolean isSpoiled() {
      return iter == ERROR_ITERATOR;
    }

    /**
     * @return The current VoteBlock.
     */
    VoteBlock getVoteBlock() {
      return voteBlock;
    }

    /**
     * @return The URL of the current voteBlock.
     */
    String getUrl() {
      if (voteBlock == null) {
	return null;
      }
      return voteBlock.getUrl();
    }
      
    /** Advance the iterator and set the new voteBlock. */
    void nextVoteBlock() {
      try {
	if (iter.hasNext()) {
	  voteBlock = iter.next();
	} else {
	  voteBlock = null;
	}
      } catch (OrderedVoteBlocksIterator.OrderException e) {
	installErrorIterator(e);
      } catch (IOException e) {
	// Even if the error is transient, we are trying to keep in
	// synch. If we later tried to catch up, we could have a
	// bunch of URLs we'd already counted for other voters.  So
	// call this Entry finished.
	installErrorIterator(e);
      }
    }

    /**
     * Release resources.
     */
    void release() {
      installIterator(VoteBlocksIterator.EMPTY_ITERATOR);
    }
      
    private void installIterator(VoteBlocksIterator iter) {
      voteBlock = null;
      if (this.iter != null) {
	this.iter.release();
      }
      this.iter = iter;
    }
      
    private void installErrorIterator(Exception e) {
      log.warning("Unable to use the iterator.", e);
      installIterator(ERROR_ITERATOR);
    }
  }

  // Sorted by the URL in their current voteBlock, to step through
  // the union of URLs.
  private final java.util.PriorityQueue<Entry> iteratorQueue;
  // Ordered by the order of the iterators passed in at creation.
  private final List<Entry> entryList;
  // Initially, the String which is before all others.
  private String prevUrl = "";

  /**
   * @param iterators An ordered List of VoteBlocksIterator instances.
   */
  public VoteBlocksCoordinator(List<VoteBlocksIterator> iterators) {
    Comparator<Entry> comparator = new Comparator<Entry>() {
      public int compare(Entry o1, Entry o2) {
	// null sorts after everything else.
	String url1 = o1.getUrl();
	String url2 = o2.getUrl();
	return VoteBlock.compareUrls(url1, url2);
      }
    };
    int initialCapacity = iterators.isEmpty()? 1 : iterators.size();
    // Throws IllegalArgumentException if initialCapacity is zero, so
    // make initialCapacity 1 for empty iterators.
    this.iteratorQueue =
      new java.util.PriorityQueue<Entry>(initialCapacity, comparator);
    this.entryList = new ArrayList<Entry>();

    for (VoteBlocksIterator iterator: iterators) {
      Entry entry = new Entry(iterator);
      entryList.add(entry);
      iteratorQueue.add(entry);
    }
  }

  /**
   * Release unneeded resources used by this object at the end of a poll.
   */
  public void release() {
    for (Entry entry : entryList) {
      entry.release();
    }
  }

  /**
   * Peek at the next URL known to any participant.
   * @return The next URL known to any participant, or null if
   * there are no partcipants with URLs remaining.
   */
  public String peekUrl() {
    Entry entry = null;
    if (iteratorQueue != null) {
      entry = iteratorQueue.peek();
    }
    if (entry == null) {
      return null;
    }
    return entry.getUrl();
  }
    
  /**
   * <p>Skip all the voters' URLs which are less than the given's
   * URL. Can be useful when checking a repair. The poller has the
   * given URL.</p>
   *
   * @param url Must be non-null, and greater
   * @throws IllegalArgumentException if url is {@code null} or before
   * the current value of {@link #peekUrl}.
   */
  public void seek(String url) {
    if (url == null) {
      throw new IllegalArgumentException("url is null.");
    }
    if (VoteBlock.compareUrls(url, peekUrl()) < 0) {
      throw new IllegalArgumentException("Current URL is "+
					 peekUrl()+", past "+url);
    }
    // Advance each entry all at once rather than running through the
    // priority queue. Not tested, but it's assumed to be more
    // efficient.
    for (Entry entry : entryList) {
      iteratorQueue.remove(entry);
      // todo(bhayes): Change VoteBlockIterator to support a "seek"
      // operation.

      // VoteBlocks.getVoteBlock(url) has [unused] code trying to do
      // something similar. It creates a VoteBlocksIterator, and
      // iterates over the whole VoteBlocks, [ignoring that it should
      // already be in URL order] looking for a VoteBlock with the
      // given URL, and returns that block. What we could use is a
      // method VoteBlocksIterator.seek(url) that fast-forwards to
      // the right place. But we don't want to just get the VoteBlock,
      // we want to advance the iterator.
      // 
      while (VoteBlock.compareUrls(entry.getUrl(), url) < 0) {
	entry.nextVoteBlock();
      }
      iteratorQueue.add(entry);
    }
  }

  /**
   * @throws IllegalArgumentException if the URL is {@code null}, or
   * sorts before a previously supplied URL, or is greater than the
   * current value of {@link #peekUrl}.
   */
  public void checkUrl(String url) {
    if (url == null) {
      throw new IllegalArgumentException("null URL not allowed.");
    }
    if (VoteBlock.compareUrls(url, prevUrl) < 0) {
      throw new IllegalArgumentException(
        "Supplied URL "+url+" should not be before "+prevUrl+".");
    }
    prevUrl = url;
    
    // The peekUrl needs to have been consumed before a higher URL is
    // allowed. The peekUrl may be null, in which case this must fail.
    if (VoteBlock.compareUrls(peekUrl(), url) < 0) {
      throw new IllegalArgumentException("Caller expected "+url+" "+
					 " but we had "+peekUrl());
    }
  }

  /**
   * If {@code false} then calls to {@link #getVoteBlock} will not
   * throw {@link IllegalArgumentException} due to a spoiled iterator.
   *
   * @param iteratorIndex The index into the {@link List} of {@code
   * iterators} supplied at construction.
   * @return true iff the iterator has thrown, or was initially null.
   */
  public boolean isSpoiled(int iteratorIndex) {
    Entry entry = entryList.get(iteratorIndex);
    return entry.isSpoiled();
  }

  /**
   * Return and consume the next {@link VoteBlock} on the iterator at
   * the given index, if it is for the given URL. Calling again with
   * the same arguments will always return {@code null}.
   * @return A {@link VoteBlock} for the given URL from the {@link
   * VoteBlockIterator} passed in at creation, or {@code null} if the
   * {@link VoteBlockIterator} was does not have a {@link VoteBlock}
   * for that URL.
   *
   * @param url The URL. Must equal or sort before the current value
   * of {@link #peekUrl}.
   * @param iteratorIndex The index into the {@link List} of {@code
   * iterators} supplied at construction.
   * @throws IllegalArgumentException if the URL is {@code null}, or
   * sorts before a previously supplied URL, or greater than the
   * current value of {@link #peekUrl}.
   * @throws IllegalArgumentException if the {@link VoteBlockIterator}
   * is spoiled. 
   * @see #isSpoiled
   */
  public VoteBlock getVoteBlock(String url, int iteratorIndex) {
    checkUrl(url);
    Entry entry = entryList.get(iteratorIndex);
    if (entry.isSpoiled()) {
      throw new IllegalArgumentException("The iterator at index "+iteratorIndex+
					 " is spoiled.");
    }
    if (! url.equals(entry.getUrl())) {
      return null;
    }

    VoteBlock voteBlock = entry.getVoteBlock();
    nextVoteBlock(entry);
    return voteBlock;
  }

  /**
   * Move the entry to the next vote block.
   */
  private void nextVoteBlock(Entry entry) {
    // There's no way to tell the PriorityQueue that the entry has
    // changed, and needs to be resorted, other than remove/add.
    iteratorQueue.remove(entry);
    entry.nextVoteBlock();
    iteratorQueue.add(entry);
  }
}
