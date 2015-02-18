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

package org.lockss.protocol;

import java.io.IOException;

/**
 * Provide a wrapper for a {@link VoteBlocksIterator} which will check
 * that the blocks are in the canonical order.
 */
public class OrderedVoteBlocksIterator implements VoteBlocksIterator {
  private final VoteBlocksIterator iterator;
  private VoteBlock prevBlock = null;

  public static class OrderException extends IllegalStateException {
    public OrderException(String msg) {
      super(msg);
    }
  }

  /**
   * @param iterator An instance to wrap.
   */
  public OrderedVoteBlocksIterator(VoteBlocksIterator iterator) {
    if (iterator == null) {
      throw new IllegalArgumentException("wrapped iterator must be non-null");
    }
    this.iterator = iterator;
  }

  private void check() throws IOException {
    if (prevBlock != null && iterator.peek() != null) {
      if (prevBlock.compareTo(iterator.peek()) >= 0) {
	throw new OrderException("VoteBlocks misordered: prev:\""+ prevBlock+
				 "\", peek: \""+iterator.peek()+"\"");
      }
    }
  }

  /**
   * Return true if the iteration has more VoteBlock objects.
   *  
   * @return true if the iteration has more elements.
   * @throws OrderException if blocks are detected to be out of order.
   */
  @Override public boolean hasNext() throws IOException { 
    check();
    return iterator.hasNext(); 
  }

  /**
   * Returns the next VoteBlock in the iteration.
   * 
   * @return The next VoteBlock in the iteration
   * VoteBlocks
   * @throws IOException
   * @throws NoSuchElementException
   * @throws OrderException if blocks are detected to be out of order.
   */
  @Override public VoteBlock next() throws IOException { 
    check();
    prevBlock = iterator.next();
    return prevBlock;
  }

  /**
   * Returns the VoteBlock that next() would return, or null if next()
   * would throw NoSuchElementException.  Does not advance the iterator
   * cursor forward.
   * 
   * @return The next VoteBlock in the iteration, or null if no more
   * VoteBlocks.
   * @throws OrderException if blocks are detected to be out of order.
   */
  @Override public VoteBlock peek() throws IOException { 
    check();
    return iterator.peek(); 
  }

  /** Release any resources held by the iterator */
  @Override public void release() {
    iterator.release(); 
  }
}