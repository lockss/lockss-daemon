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

import java.io.*;
import java.util.*;

import org.lockss.test.LockssTestCase;


public class TestOrderedVoteBlocksIterator extends LockssTestCase {

  private class ListVoteBlocksIterator implements VoteBlocksIterator {
    private final List<VoteBlock> voteBlocks;
    private int cursor = 0;

    ListVoteBlocksIterator(List<VoteBlock> voteBlocks) {
      this.voteBlocks = voteBlocks;
    }

    @Override public boolean hasNext() {
      return (cursor < voteBlocks.size());
    }

    @Override public VoteBlock next() {
      if (hasNext()) {
	return voteBlocks.get(cursor++);
      } else {
	throw new NoSuchElementException();
      }
    }

    @Override public VoteBlock peek() {
      if (hasNext()) {
	return voteBlocks.get(cursor);
      } else {
	return null;
      }
    }

    @Override public void release() {}
  }

  private void sizedTest(int size) throws Exception {
    List<VoteBlock> voteBlockList = V3TestUtils.makeVoteBlockList(size);
    VoteBlocksIterator wrapped = new ListVoteBlocksIterator(voteBlockList);
    VoteBlocksIterator iterator = new OrderedVoteBlocksIterator(wrapped);
    for (int i = 0; i < voteBlockList.size(); i++) {
      assertTrue(iterator.hasNext());
      assertEquals(voteBlockList.get(i), iterator.peek());
      assertEquals(voteBlockList.get(i), iterator.next());
    }
    assertFalse(iterator.hasNext());
    try {
      iterator.next();
      fail("Expected Exception not thrown.");
    } catch (NoSuchElementException ex) {
      // expected
    }
    assertNull(iterator.peek());
  }

  private void sizedMisorderedTest(int size, int i1, int i2) throws Exception {
    assertTrue("Test not set up correctly.", i1 < i2);
    
    List<VoteBlock> voteBlockList = V3TestUtils.makeVoteBlockList(size);
    swap(voteBlockList, i1, i2);
    VoteBlocksIterator wrapped = new ListVoteBlocksIterator(voteBlockList);
    VoteBlocksIterator iterator = new OrderedVoteBlocksIterator(wrapped);

    // The block swapped into i1 is in order with the one before; it's
    // the one after i1 that's out of order with the one previously at i2.
    for (int i = 0; i < i1+1; i++) {
      assertTrue(iterator.hasNext());
      assertEquals(voteBlockList.get(i), iterator.peek());
      assertEquals(voteBlockList.get(i), iterator.next());
    }

    // Everything throws.
    try {
      iterator.hasNext();
      fail("Expected Exception not thrown.");
    } catch (OrderedVoteBlocksIterator.OrderException ex) {
      // expected
    }
    try {
      iterator.peek();
      fail("Expected Exception not thrown.");
    } catch (OrderedVoteBlocksIterator.OrderException ex) {
      // expected
    }
    try {
      iterator.next();
      fail("Expected Exception not thrown.");
    } catch (OrderedVoteBlocksIterator.OrderException ex) {
      // expected
    }
  }

  private void swap(List<VoteBlock> voteBlockList, int i, int j) {
    VoteBlock temp = voteBlockList.get(i);
    voteBlockList.set(i, voteBlockList.get(j));
    voteBlockList.set(j, temp);
  }

  public void testNullCreate() throws Exception {
    try {
      new OrderedVoteBlocksIterator(null);
      fail("Illegal create failed to throw.");
    } catch (IllegalArgumentException ex) {
      // expected
    }
  }

  // Test that misorder is detected
  public void testOrdered() throws Exception {
    sizedTest(0);
    sizedTest(1);
    sizedTest(2);
    sizedTest(10);
  }

  // Test that misorder is detected
  public void testMisordered() throws Exception {
    sizedMisorderedTest(2, 0, 1);
    sizedMisorderedTest(10, 3, 5);
  }
}
