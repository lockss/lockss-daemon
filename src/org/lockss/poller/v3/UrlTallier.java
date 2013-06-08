/*
 * $Id: UrlTallier.java,v 1.9.8.1 2013-06-08 22:25:01 dshr Exp $
 */

/*

Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.hasher.HashBlock;
import org.lockss.protocol.PeerIdentity;
import org.lockss.protocol.VoteBlock;
import org.lockss.protocol.VoteBlocks;
import org.lockss.protocol.VoteBlocksIterator;
import org.lockss.util.*;

/**
 * <p>This class is used by {@link V3Poller} to tally poll results in
 * a {@link BlockTally} for each URL in the union of the poller and
 * the participants.
 */
final class UrlTallier {
  // todo(bhayes): It only makes sense to create this when
  // theParticipants has been firmed up, and won't ever
  // change. But I don't see anything in V3Poller that makes
  // sure that is so.

  private static Logger log = Logger.getLogger("UrlTallier");
    
  /* A class to link participants and iterators, so internally we control
   * the exceptions that are raised when trying to get the url. */
  private static final class Entry {
  
    /** <p>An empty, immutable VoteBlock iterator.  Calling next()
     * throws NoSuchElementException. This instance will replace a
     * voter's iterator if the iterator ever throws a known
     * exception.</p>
     */
    static final VoteBlocksIterator ERROR_ITERATOR =
      new VoteBlocksIterator() {
	public boolean hasNext() { return false; }
	public VoteBlock next() { throw new NoSuchElementException(); }
	public VoteBlock peek() { return null; }
	public void release() { }
      };

    final ParticipantUserData userData;
    // Not final: will be replaced if the provided iterator throws IOException.
    private VoteBlocksIterator iter = null;
    // The current voteBlock. Changed by nextVoteBlock().
    private VoteBlock voteBlock = null;
      
    Entry(ParticipantUserData userData) {
      this.userData = userData;
      try {
	VoteBlocks voteBlocks = userData.getVoteBlocks();
	if (voteBlocks == null) {
	  log.warning("Voter " + userData + " has no voteBlocks.");
	} else {
	  this.iter = voteBlocks.iterator();
	}
      } catch (FileNotFoundException e) {
	installErrorIterator(e);
      }
      if (this.iter == null) {
	log.warning("Voter " + userData +
		    " has a null vote block iterator.");
	installIterator(VoteBlocksIterator.EMPTY_ITERATOR);
      }
      nextVoteBlock();
    }
      
    /**
     * @return true iff the iterator has thrown, and there is no idea
     * what the vote might have been.
     */
    boolean voteSpoiled() {
      return iter == ERROR_ITERATOR;
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
      log.error("Unable to use the iterator for voter " + userData + ".", e);
      installIterator(ERROR_ITERATOR);
    }
  }

  // Ordered by the URL in their current voteBlock, to step through
  // the union of URLs.
  private final java.util.PriorityQueue<Entry> participantsQueue;
  // todo(bhayes): Try to reform Poller to not care about the order,
  // which will involve BlockHasher's use of arrays.
  // Ordered in the same order as the participants.
  private final List<Entry> participantsList;
    
  /**
   * @param participants An ordered List of participants.
   */
  UrlTallier(List<ParticipantUserData> participants) {
    Comparator<Entry> comparator = new Comparator<Entry>() {
      public int compare(Entry o1, Entry o2) {
	// null sorts after everything else.
	String url1 = o1.getUrl();
	String url2 = o2.getUrl();
	return VoteBlock.compareUrls(url1, url2);
      }
    };
    this.participantsQueue =
      new java.util.PriorityQueue<Entry>(participants.size(), comparator);
    this.participantsList = new ArrayList<Entry>(participants.size());
      
    for (ParticipantUserData participant : participants) {
      Entry entry = new Entry(participant);
      participantsList.add(entry);
      participantsQueue.add(entry);
    }
  }

  /**
   * Release unneeded resources used by this object at the end of a poll.
   */
  void release() {
    for (Entry e : participantsList) {
      e.release();
    }
  }

  /**
   * Peek at the next URL known to any participant.
   * @return The next URL known to any participant, or null if
   * there are no partcipants with URLs remaining.
   */
  String peekUrl() {
    Entry e = participantsQueue.peek();
    if (e == null) {
      return null;
    }
    return e.getUrl();
  }
    
  /**
   * <p>Skip all the voters' URLs which are less than the given's
   * URL. Can be useful when checking a repair. The poller has the
   * given URL.</p>
   *
   * @param url Must be non-null.
   */
  void seek(String url) {
    if (url == null) {
      throw new ShouldNotHappenException("url is null.");
    }
    if (StringUtil.compareToNullHigh(peekUrl(), url) > 0) {
      throw new ShouldNotHappenException("Current URL is "+
					 peekUrl()+", past "+url);
    }
    for (Entry e : participantsList) {
      participantsQueue.remove(e);
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
      while (StringUtil.compareToNullHigh(e.getUrl(), url) < 0) {
	e.nextVoteBlock();
      }
      participantsQueue.add(e);
    }
    // NOTE: Since the voters' iterators may not read from disk the
    // same as in the initial poll, some or all of the voters which
    // had the URL in the initial poll may deny having it now.
    // peekUrl() may not equal url.
    if (StringUtil.compareToNullHigh(peekUrl(), url) < 0) {
      throw new ShouldNotHappenException("Current URL is "+
					 peekUrl()+", before "+url);
    }
  }

  /**
   * <p>Call the appropriate voting routine on the tally for each
   * participant. The poller does not have the given URL, but some
   * voter does.</p>
   *
   * @param url Must be non-null and equal to {@link peekUrl}, the
   * current URL known to any participant.
   * @return tally Collects the votes.
   */
  BlockTally tallyVoterUrl(String url) {
    if (url == null) {
      throw new ShouldNotHappenException("url is null.");
    }
    if (! url.equals(peekUrl())) {
      throw new ShouldNotHappenException("Current URL is "+
					 peekUrl()+" not "+url);
    }

    BlockTally tally = new BlockTally();
    VoteBlockTallier voteBlockTallier = new VoteBlockTallier();
    log.debug3("tallyVoterUrl: "+url);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.addTally(ParticipantUserData.voteTally);
    voteAllParticipants(url, voteBlockTallier);
    return tally;
  }

  /**
   * <p>Call the appropriate voting routine on the tally for each
   * participant.  The poller has the given URL.</p>
   *
   * @param url Must be non-null and equal to {@link peekUrl}, the
   * minimum URL known to any participant.
   * @param hashBlock The poller's {@link HashBlock}.
   * @return tally Collects the votes.
   */
  BlockTally tallyPollerUrl(String url, HashBlock hashBlock) {
    if (url == null) {
      throw new ShouldNotHappenException("url is null.");
    }
    if (StringUtil.compareToNullHigh(peekUrl(), url) < 0) {
      throw new ShouldNotHappenException("Current URL "+peekUrl()+
					 " comes before "+url);
    }

    BlockTally tally = new BlockTally();
    VoteBlockTallier voteBlockTallier = new VoteBlockTallier(hashBlock);
    log.debug3("tallyPollerUrl: "+url);
    voteBlockTallier.addTally(tally);
    voteBlockTallier.addTally(ParticipantUserData.voteTally);
    voteAllParticipants(url, voteBlockTallier);
    return tally;
  }

  /**
   * <p>Call the appropriate voting routine on the tally for each
   * participant.  The poller has the given URL.</p>
   *
   * @param url Must be non-null and equal to {@link peekUrl}, the
   * minimum URL known to any participant.
   * @param hashBlock The poller's {@link HashBlock}.
   * @return tally Collects the votes.
   */
  BlockTally tallyRepairUrl(String url, HashBlock hashBlock) {
    if (url == null) {
      throw new ShouldNotHappenException("url is null.");
    }
    if (StringUtil.compareToNullHigh(peekUrl(), url) < 0) {
      throw new ShouldNotHappenException("Current URL "+peekUrl()+
					 " comes before "+url);
    }

    log.debug3("tallyRepairUrl: "+url);
    VoteBlockTallier voteBlockTallier = new VoteBlockTallier(hashBlock);
    BlockTally tally = new BlockTally();
    voteBlockTallier.addTally(tally);
    voteAllParticipants(url, voteBlockTallier);
    return tally;
  }

  void voteAllParticipants(String url, VoteBlockTallier voteBlockTallier) {
    for (int participantIndex = 0; participantIndex < participantsList.size();
	 participantIndex++) {
      Entry e = participantsList.get(participantIndex);
      if (e.voteSpoiled()) {
	voteBlockTallier.voteSpoiled(e.userData);
      } else {
	VoteBlock voteBlock = e.voteBlock;
	if (url.equals(e.getUrl())) {
	  nextVoteBlock(e);
	  voteBlockTallier.vote(voteBlock, e.userData, participantIndex);
	} else {
	  voteBlockTallier.voteMissing(url, e.userData, participantIndex);
	}
      }
    }
  }

  // Called only from testing
  boolean voteSpoiled(ParticipantUserData participant) {
    for (Entry e : participantsList) {
      if (e.userData == participant) {
	return e.voteSpoiled();
      }
    }
    throw new ShouldNotHappenException("participant unknown.");
  }

  /**
   * Move the entry to the next vote block.
   */
  private void nextVoteBlock(Entry e) {
    // There's no way to tell the PriorityQueue that the entry has
    // changed, and needs to be resorted, other than remove/add.
    participantsQueue.remove(e);
    e.nextVoteBlock();
    participantsQueue.add(e);
  }
}
