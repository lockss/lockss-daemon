/*
 * $Id$
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
import org.lockss.test.LockssTestCase;
import java.io.*;

public class TestFilterCharRing extends LockssTestCase {

  public void testCharEqualsIgnoreCase() {
    assertTrue(FilterCharRing.charEqualsIgnoreCase('a', 'a'));
    assertTrue(FilterCharRing.charEqualsIgnoreCase('a', 'A'));
    assertTrue(FilterCharRing.charEqualsIgnoreCase('A', 'A'));

    assertFalse(FilterCharRing.charEqualsIgnoreCase('a', 'b'));
    assertFalse(FilterCharRing.charEqualsIgnoreCase('a', 'B'));
    assertFalse(FilterCharRing.charEqualsIgnoreCase('A', 'B'));

    assertFalse(FilterCharRing.charEqualsIgnoreCase('\n', 'b'));
    assertTrue(FilterCharRing.charEqualsIgnoreCase('\n', '\n'));
    assertFalse(FilterCharRing.charEqualsIgnoreCase('\n', '\t'));
  }

  //tests for startsWith(String) method
  public void testThrowsIfTagTooBig()
      throws CharRing.RingFullException {
    try {
      FilterCharRing ring = makeCharRingFromString("Test string");
      ring.startsWith("123456789012");
      fail("Should have thrown since tag was too big");
    } catch (RuntimeException e) {
    }
  }

  public void testThrowsWithNoTag()
      throws CharRing.RingFullException {
    try {
      FilterCharRing ring = makeCharRingFromString("Test string");
      ring.startsWith(null);
      fail("Should have thrown since tag was null");
    } catch (RuntimeException e) {
    }
  }

  public void testTagNotInString() throws CharRing.RingFullException {
    FilterCharRing ring = makeCharRingFromString("Test string");
    assertFalse(ring.startsWith("blah"));
  }

  public void testTagAtStartOfString() throws CharRing.RingFullException {
    FilterCharRing ring = makeCharRingFromString("Test string");
    assertTrue(ring.startsWith("Test"));
  }

  public void testCapsMatter() throws CharRing.RingFullException {
    FilterCharRing ring = makeCharRingFromString("Test string");
    assertFalse(ring.startsWith("test"));
  }

  public void testMustBeAtStart() throws CharRing.RingFullException {
    FilterCharRing ring = makeCharRingFromString("1Test string");
    assertFalse(ring.startsWith("Test"));
  }

  //tests for the startsWith(int, String, boolean) method
  public void testThrowsIfIndexLessThan0() throws CharRing.RingFullException {
    try {
      FilterCharRing ring = makeCharRingFromString("Test string");
      ring.startsWith(-1, "123", true);
      fail("Should have thrown since index < 0");
    } catch (RuntimeException e) {
    }
  }

  public void testCapsMatterMultiArgs() throws CharRing.RingFullException {
    FilterCharRing ring = makeCharRingFromString("Test string");
    assertFalse(ring.startsWith(0, "test", false));
    assertTrue(ring.startsWith(0, "test", true));
  }

  public void testIndexMultiArgs() throws CharRing.RingFullException {
    FilterCharRing ring = makeCharRingFromString("Test string");
    assertFalse(ring.startsWith(0, "est", false));
    assertTrue(ring.startsWith(1, "est", false));
    assertFalse(ring.startsWith(2, "est", false));
  }

  //Tests for refillBuffer

  public void testThrowsOnNullReader() throws IOException {
    FilterCharRing ring = new FilterCharRing(10);
    try {
      ring.refillBuffer(null);
      fail("Calling refillBuffer with a null reader should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testRefillEmptyBuffer() throws IOException {
    FilterCharRing ring = new FilterCharRing(10);
    assertFalse(ring.refillBuffer(new StringReader("1234567890")));
    assertEquals("1234567890", makeStringFromCharRing(ring));
  }

  public void testReaderSmallerThanBuffer() throws IOException {
    FilterCharRing ring = new FilterCharRing(10);
    assertTrue(ring.refillBuffer(new StringReader("123456789")));
    assertEquals("123456789", makeStringFromCharRing(ring));
  }

  public void testRefillEmptyBufferOverflow() throws IOException {
    FilterCharRing ring = new FilterCharRing(9);
    assertFalse(ring.refillBuffer(new StringReader("1234567890")));
    assertEquals("123456789", makeStringFromCharRing(ring));
  }

  public void testRefillFullBuffer()
      throws IOException, CharRing.RingFullException {
    FilterCharRing ring = makeCharRingFromString("Test");
    assertFalse(ring.refillBuffer(new StringReader("1234567890")));
    assertEquals("Test", makeStringFromCharRing(ring));
  }

  public void testRefillPartiallyFullBuffer()
      throws IOException, CharRing.RingFullException {
    FilterCharRing ring = new FilterCharRing(10);
    assertTrue(ring.refillBuffer(new StringReader("Test")));
    assertFalse(ring.refillBuffer(new StringReader("1234567890")));
    assertEquals("Test123456", makeStringFromCharRing(ring));
  }

  private FilterCharRing makeCharRingFromString(String str)
      throws CharRing.RingFullException{
    FilterCharRing ring = new FilterCharRing(str.length());
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
