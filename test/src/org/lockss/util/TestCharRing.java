/*
 * $Id: TestCharRing.java,v 1.1 2003-06-05 21:48:59 troberts Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;
import org.lockss.test.*;

public class TestCharRing extends LockssTestCase {
  RingArray ra;

  public void setUp() {
    ra = new RingArray(5);
    ra.add('a');
    ra.add('b');
    ra.add('c');
    ra.add('d');
    ra.add('e');
  }


  public void testBadSizeThrows() {
    try {
      RingArray ra = new RingArray(0);
      fail("Trying to create a RingArray with a size of "
	   +"zero should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testCapacityIsCorrect() {
    RingArray ra = new RingArray(5);
    assertEquals(5, ra.capacity());
  }

  public void testHeadGetsFirst() {
    assertEquals('a', ra.getHead());
  }

  public void testTailGetsLast() {
    assertEquals('e', ra.getTail());
  }

  public void testAddingTooManyThrows() {
    try {
      ra.add('f');
      fail("Adding a 6th element to a 5 element RingArray should have thrown");
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testRemoveDoes() {
    assertEquals('a', ra.remove());
    assertEquals('b', ra.getHead());
  }

  public void testSize() {
    assertEquals(5, ra.size());
    ra.remove();
    assertEquals(4, ra.size());
  }

  public void testWrapping() {
    ra.remove();
    ra.add('f');
    assertEquals('f', ra.getTail());

    ra.remove();
    ra.add('g');
    assertEquals('g', ra.getTail());
  }

  public void testGetIndex() {
    assertEquals('a', ra.getChar(ra.getHeadIndex()));
    assertEquals('e', ra.getChar(ra.getTailIndex()));
  }

  public void testMultipleGetIndex() {
    int headIndex = ra.getHeadIndex();
    for (int ix = 0; ix < ra.capacity()-1; ix++) {
      headIndex = ra.incrementIndex(headIndex);
    }
    assertEquals('e', ra.getChar(headIndex));
    headIndex = ra.incrementIndex(headIndex);
    assertEquals('a', ra.getChar(headIndex));
  }

  public void testHeadWraps() {
    ra.remove();
    ra.remove();
    ra.remove();
    ra.remove();
    ra.remove();
    ra.add('f');
    assertEquals('f', ra.getHead());
  }

}
