/*
 * $Id: TestCharRing.java,v 1.4 2003-06-13 00:34:28 troberts Exp $
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
  CharRing cr;

  public void setUp() throws CharRing.RingFullException {
    cr = new CharRing(5);
    cr.add('a');
    cr.add('b');
    cr.add('c');
    cr.add('d');
    cr.add('e');
  }


  public void testBadCapacityThrows() {
    try {
      CharRing cr = new CharRing(0);
      fail("Trying to create a CharRing with a capacity of "
	   +"zero should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testGetNthChar() {
    assertEquals('a', cr.get(0));
    assertEquals('b', cr.get(1));
    assertEquals('c', cr.get(2));
    assertEquals('d', cr.get(3));
    assertEquals('e', cr.get(4));
  }

  public void testWrapping() throws CharRing.RingFullException {
    cr.remove();
    cr.add('f');
    assertEquals('b', cr.get(0));
    assertEquals('c', cr.get(1));
    assertEquals('d', cr.get(2));
    assertEquals('e', cr.get(3));
    assertEquals('f', cr.get(4));

    cr.remove();
    cr.add('g');
    assertEquals('c', cr.get(0));
    assertEquals('d', cr.get(1));
    assertEquals('e', cr.get(2));
    assertEquals('f', cr.get(3));
    assertEquals('g', cr.get(4));
  }

  public void testGetNthCharThrowsIfLargerThanCapacity() {
    try {
      cr.get(5);
      fail("getNthChar should have thrown when N was greater than capacity");
    } catch (CharRing.BadIndexException e) {
    }
  }

  public void testGetNthCharThrowsIfLargerThanSize() {
    cr.remove();
    try {
      cr.get(4);
      fail("getNthChar should have thrown when N was greater than size");
    } catch (CharRing.BadIndexException e) {
    }
  }

  public void testCapacityIsCorrect() {
    CharRing cr = new CharRing(5);
    assertEquals(5, cr.capacity());
  }

  public void testAddingTooManyThrows() {
    try {
      cr.add('f');
      fail("Adding a 6th element to a 5 element CharRing should have thrown");
    } catch (CharRing.RingFullException e) {
    }
  }

  public void testRemoveDoes() {
    assertEquals('a', cr.remove());
    assertEquals('b', cr.get(0));
  }
  
  public void testRemoveThrowsIfNothingLeft() {
    cr.remove();
    cr.remove();
    cr.remove();
    cr.remove();
    cr.remove();
    try {
      cr.remove();
      fail("remove() on empty char ring should have thrown");
    } catch (CharRing.BadIndexException e) {
    }
  }

  public void testSize() {
    assertEquals(5, cr.size());
    cr.remove();
    assertEquals(4, cr.size());
    cr.remove();
    cr.remove();
    cr.remove();
    cr.remove();
    assertEquals(0, cr.size());
  }

  public void testClear0() {
    cr.clear(0);
    assertEquals('a', cr.get(0));
  }

  public void testClearMany() {
    cr.clear(2);
    assertEquals('c', cr.get(0));
    cr.clear(1);
    assertEquals('d', cr.get(0));
    assertEquals(2, cr.size());
  }

  public void testClearThrowsIfOverSize() {
    try {
      cr.clear(6);
      fail("clear(6) Should have thrown");
    } catch (CharRing.BadIndexException e) {
    }
  }

  public void testArrayAddThrowsIfArrayTooBig() {
    char chars[] = {'z', 'x', 'y'};
    CharRing cr = new CharRing(1);
    try {
      cr.add(chars);
      fail("Add should have thrown with too many chars");
    } catch (CharRing.RingFullException e) {
    }
  }

  public void testArrayAddNoWrap() throws CharRing.RingFullException {
    char chars[] = {'z', 'x', 'y'};
    CharRing cr = new CharRing(3);
    cr.add(chars);
    assertEquals('z', cr.remove());
    assertEquals('x', cr.remove());
    assertEquals('y', cr.remove());
    assertEquals(0, cr.size());
  }

  public void testArrayAddWrap() throws CharRing.RingFullException {
    char chars[] = {'z', 'x', 'y'};
    CharRing cr = new CharRing(3);
    cr.add('b');
    cr.remove();
    cr.add(chars);
    assertEquals('z', cr.remove());
    assertEquals('x', cr.remove());
    assertEquals('y', cr.remove());
    assertEquals(0, cr.size());
  }

  public void testArrayAddWrapWPosAndLength()
      throws CharRing.RingFullException {
    char chars[] = {'z', 'x', 'y', 'q'};
    CharRing cr = new CharRing(3);
    cr.add('b');
    cr.remove();
    cr.add(chars, 1, 2);
    assertEquals('x', cr.remove());
    assertEquals('y', cr.remove());
    assertEquals(0, cr.size());
  }

  public void testAddArrayShorterThanCharRing()
      throws CharRing.RingFullException {
    char chars[] = {'z', 'x', 'y', 'q'};
    CharRing cr = new CharRing(5);
    cr.add(chars, 1, 2);
    assertEquals('x', cr.remove());
    assertEquals('y', cr.remove());
    assertEquals(0, cr.size());
  }

  public void testArrayRemoveEmptyCharRing() {
    char chars[] = new char[4];
    assertEquals(0, new CharRing(4).remove(chars));
  }

  public void testArrayRemoveNoWrap() {
    char chars[] = new char[4];
    assertEquals(4, cr.remove(chars));
    assertEquals('a', chars[0]);
    assertEquals('b', chars[1]);
    assertEquals('c', chars[2]);
    assertEquals('d', chars[3]);
  }

  public void testArrayRemoveNoWrapWithPosAndLen() {
    char chars[] = {'w', 'x', 'y', 'z'};
    assertEquals(2, cr.remove(chars, 1, 2));
    assertEquals('w', chars[0]);
    assertEquals('a', chars[1]);
    assertEquals('b', chars[2]);
    assertEquals('z', chars[3]);
  }

  public void testArrayRemoveWrap() throws CharRing.RingFullException {
    char chars[] = new char[4];
    cr.remove();
    cr.add('e');
    assertEquals(4, cr.remove(chars));
    assertEquals('b', chars[0]);
    assertEquals('c', chars[1]);
    assertEquals('d', chars[2]);
    assertEquals('e', chars[3]);
  }
}
