/*
 * $Id: TestCharRing.java,v 1.11 2006-11-15 21:17:53 troberts Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import java.io.*;
import org.lockss.test.*;
import org.lockss.util.CharRing.RingFullException;

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


  private void doWhiteSpaceTest(char[] chars) throws RingFullException {

    CharRing cr2 = new CharRing(chars.length);
    cr2.add(chars);
    assertTrue(cr2.skipLeadingWhiteSpace());
    assertEquals(2, cr2.size());
    assertEquals('x', cr2.get(0));
    assertEquals('y', cr2.get(1));
  }

  private static final char chars1[] = {' ', 'x', 'y'};

  private static final char chars2[] = {' ', ' ', '\n', ' ', ' ', 'x', 'y'};


  public void testSkipLeadingWhiteSpace() throws RingFullException {
    assertFalse(cr.skipLeadingWhiteSpace());

    doWhiteSpaceTest(chars1);
    doWhiteSpaceTest(chars2);
  }

  public void testStartsWithIgnoreCase() throws RingFullException {
    assertFalse(cr.startsWithIgnoreCase("ZZ"));
    assertFalse(cr.startsWithIgnoreCase("ZZZZZZZZ"));
    assertTrue(cr.startsWithIgnoreCase("ab"));
    assertTrue(cr.startsWithIgnoreCase("AB"));
    cr.remove(new StringBuffer(), 4);
    cr.add('p');
    assertTrue(cr.startsWithIgnoreCase("ep"));
  }

  public void testStartsWithIgnoreCaseWithIdx() throws RingFullException {
    assertFalse(cr.startsWithIgnoreCase("ZZ", 2));
    assertTrue(cr.startsWithIgnoreCase("ab", 0));
    assertTrue(cr.startsWithIgnoreCase("BC", 1));
    assertFalse(cr.startsWithIgnoreCase("BC", 6));


    //    cr.remove(new StringBuffer(), 4);
//    cr.add('p');
//    assertTrue(cr.startsWithIgnoreCase("ep"));
  }

  public void testZeroCapacityThrows() {
    try {
      CharRing cr2 = new CharRing(0);
      fail("Trying to create a CharRing with a capacity of "
	   +"zero should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testNegativeCapacityThrows() {
    try {
      CharRing cr2 = new CharRing(-1);
      fail("Trying to create a CharRing with a capacity of "
	   +"zero should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testGet() {
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
    } catch (IndexOutOfBoundsException e) {
    }
  }

  public void testGetNthCharThrowsIfLargerThanSize() {
    cr.remove();
    try {
      cr.get(4);
      fail("getNthChar should have thrown when N was greater than size");
    } catch (IndexOutOfBoundsException e) {
    }
  }

  public void testCapacityIsCorrect() {
    CharRing cr2 = new CharRing(7);
    assertEquals(7, cr2.capacity());
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
    while (cr.size() > 0) {
      cr.remove();
    }
    try {
      cr.remove();
      fail("remove() on empty char ring should have thrown");
    } catch (IndexOutOfBoundsException e) {
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

  public void testSkipNegative() {
    try {
      cr.skip(-1);
      fail("skip(-1) should have thrown");
    } catch (IndexOutOfBoundsException e) {
    }
  }

  public void testSkipZero() {
    cr.skip(0);
    assertEquals('a', cr.get(0));
  }

  public void testSkipMany() {
    assertEquals('a', cr.get(0));
    cr.skip(2);
    assertEquals('c', cr.get(0));
    cr.skip(1);
    assertEquals('d', cr.get(0));
    assertEquals(2, cr.size());
  }

  public void testSkipThrowsIfOverSize() {
    try {
      cr.skip(cr.size()+1);
      fail("skip(6) Should have thrown");
    } catch (IndexOutOfBoundsException e) {
    }
  }

  public void testClear() {
    assertEquals('a', cr.get(0));
    cr.clear();
    assertEquals(0, cr.size());
  }

  public void testArrayAddThrowsIfArrayTooBig() {
    char chars[] = {'z', 'x', 'y'};
    CharRing cr2 = new CharRing(1);
    try {
      cr2.add(chars);
      fail("Add should have thrown with too many chars");
    } catch (CharRing.RingFullException e) {
    }
  }

  public void testArrayAddNoWrap() throws CharRing.RingFullException {
    char chars[] = {'z', 'x', 'y'};
    CharRing cr2 = new CharRing(3);
    cr2.add(chars);
    assertEquals('z', cr2.remove());
    assertEquals('x', cr2.remove());
    assertEquals('y', cr2.remove());
    assertEquals(0, cr2.size());
  }

  public void testArrayAddWrap1() throws CharRing.RingFullException {
    char chars[] = {'z', 'y', 'x', 'w'};
    CharRing cr2 = new CharRing(4);
    cr2.add('b');
    cr2.remove();
    cr2.add(chars);
    assertEquals('z', cr2.remove());
    assertEquals('y', cr2.remove());
    assertEquals('x', cr2.remove());
    assertEquals('w', cr2.remove());
    assertEquals(0, cr2.size());
  }

  public void testArrayAddWrap2() throws CharRing.RingFullException {
    char chars[] = {'z', 'y', 'x', 'w'};
    CharRing cr2 = new CharRing(4);
    cr2.add('b');
    cr2.add('c');
    cr2.remove();
    cr2.remove();
    cr2.add(chars);
    assertEquals('z', cr2.remove());
    assertEquals('y', cr2.remove());
    assertEquals('x', cr2.remove());
    assertEquals('w', cr2.remove());
    assertEquals(0, cr2.size());
  }

  public void testArrayAddWrap3() throws CharRing.RingFullException {
    char chars[] = {'z', 'y', 'x', 'w'};
    CharRing cr2 = new CharRing(4);
    cr2.add('b');
    cr2.add('c');
    cr2.add('d');
    cr2.remove();
    cr2.remove();
    cr2.remove();
    cr2.add(chars);
    assertEquals('z', cr2.remove());
    assertEquals('y', cr2.remove());
    assertEquals('x', cr2.remove());
    assertEquals('w', cr2.remove());
    assertEquals(0, cr2.size());
  }

  public void testArrayAddWrapWPosAndLength()
      throws CharRing.RingFullException {
    char chars[] = {'z', 'x', 'y', 'q'};
    CharRing cr2 = new CharRing(3);
    cr2.add('b');
    cr2.remove();
    cr2.add(chars, 1, 2);
    assertEquals('x', cr2.remove());
    assertEquals('y', cr2.remove());
    assertEquals(0, cr2.size());

    cr2.add(chars, 1, 2);
    assertEquals('x', cr2.remove());
    assertEquals('y', cr2.remove());
    assertEquals(0, cr2.size());
  }

  public void testAddArrayShorterThanCharRing()
      throws CharRing.RingFullException {
    char chars[] = {'z', 'x', 'y', 'q'};
    CharRing cr2 = new CharRing(5);
    cr2.add(chars, 1, 2);
    assertEquals('x', cr2.remove());
    assertEquals('y', cr2.remove());
    assertEquals(0, cr2.size());
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

  public void testArrayRemoveWrap1() throws CharRing.RingFullException {
    char chars[] = new char[5];
    cr.remove();
    cr.add('f');
    assertEquals(5, cr.remove(chars));
    assertEquals('b', chars[0]);
    assertEquals('c', chars[1]);
    assertEquals('d', chars[2]);
    assertEquals('e', chars[3]);
    assertEquals('f', chars[4]);
  }

  public void testArrayRemoveWrap2() throws CharRing.RingFullException {
    char chars[] = new char[5];
    cr.remove();
    cr.remove();
    cr.add('f');
    cr.add('g');
    assertEquals(5, cr.remove(chars));
    assertEquals('c', chars[0]);
    assertEquals('d', chars[1]);
    assertEquals('e', chars[2]);
    assertEquals('f', chars[3]);
    assertEquals('g', chars[4]);
  }

  public void testArrayRemoveWrap3() throws CharRing.RingFullException {
    char chars[] = new char[5];
    cr.remove();
    cr.remove();
    cr.remove();
    cr.add('f');
    cr.add('g');
    cr.add('h');
    assertEquals(5, cr.remove(chars));
    assertEquals('d', chars[0]);
    assertEquals('e', chars[1]);
    assertEquals('f', chars[2]);
    assertEquals('g', chars[3]);
    assertEquals('h', chars[4]);
  }

  public void testArrayAddThrowsIfPosNegative()
      throws CharRing.RingFullException {
    CharRing ring = new CharRing(5);
    try {
      ring.add(new char[4], -1, 3);
      fail("Adding with a negative position should have thrown");
    } catch (IndexOutOfBoundsException e) {
    }
  }

  public void testArrayAddThrowsIfLengthNegative()
      throws CharRing.RingFullException {
    CharRing ring = new CharRing(5);
    try {
      ring.add(new char[4], 0, -1);
      fail("Adding with a negative length should have thrown");
    } catch (IndexOutOfBoundsException e) {
    }
  }

  public void testArrayRemoveThrowsIfPosNegative()
      throws CharRing.RingFullException {
    CharRing ring = new CharRing(5);
    ring.add('a');
    try {
      ring.remove(new char[4], -1, 3);
      fail("Removing with a negative position should have thrown");
    } catch (IndexOutOfBoundsException e) {
    }
  }

  public void testArrayRemoveThrowsIfLengthNegative()
      throws CharRing.RingFullException {
    CharRing ring = new CharRing(5);
    try {
      ring.remove(new char[4], 0, -1);
      fail("Removing with a negative length should have thrown");
    } catch (IndexOutOfBoundsException e) {
    }
  }

  public void testSBRemove() throws Exception {
    StringBuffer sb = new StringBuffer();
    assertEquals(5, cr.remove(sb, 10));
    assertEquals("abcde", sb.toString());
    cr.add("1234".toCharArray());
    assertEquals(3, cr.remove(sb, 3));
    assertEquals("abcde123", sb.toString());
  }

  public void testIndexOf() throws Exception {
    assertEquals(1, cr.indexOf("bc", -1, false));
    assertEquals(-1, cr.indexOf("bb", -1, false));
    CharRing ring = new CharRing(5);
    ring.add("12345".toCharArray());
    ring.remove();
    ring.remove();
    assertEquals('3', ring.get(0));
    ring.add("46".toCharArray());
    assertEquals(0, ring.indexOf("34", -1, false));
    assertEquals(1, ring.indexOf("45", -1, false));
    assertEquals(3, ring.indexOf("46", -1, false));
    assertEquals(1, ring.indexOf("4546", -1, false));
    assertEquals(-1, ring.indexOf("463", -1, false));
  }

  public void testIndexOfLast() throws Exception {
    CharRing ring = new CharRing(5);
    ring.add("12345".toCharArray());
    ring.remove();
    ring.remove();
    assertEquals('3', ring.get(0));
    ring.add("46".toCharArray());
    assertEquals(0, ring.indexOf("34", -1, false));
    assertEquals(0, ring.indexOf("34", 0, false));
    assertEquals(0, ring.indexOf("34", 1, false));
    assertEquals(3, ring.indexOf("46", -1, false));
    assertEquals(3, ring.indexOf("46", 3, false));
    assertEquals(3, ring.indexOf("46", 4, false));
    assertEquals(-1, ring.indexOf("46", 2, false));
  }

  public void testIndexOfIgn() throws Exception {
    CharRing ring = new CharRing(5);
    ring.add("abCDe".toCharArray());
    ring.remove();
    ring.add("F".toCharArray());
    assertEquals(2, ring.indexOf("De", -1, false));
    assertEquals(-1, ring.indexOf("de", -1, false));
    assertEquals(-1, ring.indexOf("dE", -1, false));
    assertEquals(-1, ring.indexOf("DE", -1, false));
    assertEquals(2, ring.indexOf("De", -1, true));
    assertEquals(2, ring.indexOf("de", -1, true));
    assertEquals(2, ring.indexOf("DE", -1, true));
  }

  public void testRefillEmptyBuffer() throws IOException {
    CharRing ring = new CharRing(10);
    assertFalse(ring.refillBuffer(new StringReader("1234567890")));
    assertEquals("1234567890", makeStringFromCharRing(ring));
  }

  public void testReaderSmallerThanBuffer() throws IOException {
    CharRing ring = new CharRing(10);
    assertTrue(ring.refillBuffer(new StringReader("123456789")));
    assertEquals("123456789", makeStringFromCharRing(ring));
  }

  public void testRefillEmptyBufferOverflow() throws IOException {
    CharRing ring = new CharRing(9);
    assertFalse(ring.refillBuffer(new StringReader("1234567890")));
    assertEquals("123456789", makeStringFromCharRing(ring));
  }

  public void testRefillFullBuffer()
      throws IOException, CharRing.RingFullException {
    CharRing ring = makeCharRingFromString("Test");
    assertFalse(ring.refillBuffer(new StringReader("1234567890")));
    assertEquals("Test", makeStringFromCharRing(ring));
  }

  public void testRefillPartiallyFullBuffer()
      throws IOException, CharRing.RingFullException {
    CharRing ring = new CharRing(10);
    assertTrue(ring.refillBuffer(new StringReader("Test")));
    assertFalse(ring.refillBuffer(new StringReader("1234567890")));
    assertEquals("Test123456", makeStringFromCharRing(ring));
  }

  private CharRing makeCharRingFromString(String str)
      throws CharRing.RingFullException{
    CharRing ring = new CharRing(str.length());
    for (int ix=0; ix<str.length(); ix++) {
      ring.add(str.charAt(ix));
    }
    return ring;
  }

  private String makeStringFromCharRing(CharRing ring) {
    StringBuffer sb = new StringBuffer();
    for (int ix=0; ix<ring.size(); ix++) {
      sb.append(ring.get(ix));
    }
    return sb.toString();
  }
}
