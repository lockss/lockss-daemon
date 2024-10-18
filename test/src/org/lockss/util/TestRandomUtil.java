/*

Copyright (c) 2000-2024 Board of Trustees of Leland Stanford Jr. University,
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

public class TestRandomUtil extends LockssTestCase {

  public void testRandomAlphabetic() {
    for (int ix = 0; ix < 100; ix++) {
      String s = RandomUtil.randomAlphabetic(100);
      assertTrue(s, isAlphabetic(s));
    }
  }

  // Ensure at least one digit appears.  Longer string & more
  // iterations to reduce chance of legitimate failure
  public void testRandomAlphanumeric() {
    boolean numSeen = false;
    for (int ix = 0; ix < 1000; ix++) {
      String s = RandomUtil.randomAlphanumeric(1000);
      assertTrue(s, isAlphanumeric(s));
      numSeen = numSeen || !isAlphabetic(s);
    }
    assertTrue("No numeric chars included", numSeen);
  }

  public void testPredicates() {
    assertTrue(isAlphabetic("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"));
    assertFalse(isAlphabetic("foo5"));
    assertFalse(isAlphabetic("foo "));
    assertFalse(isAlphabetic("foo_"));

    assertTrue(isAlphanumeric("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"));
    assertFalse(isAlphanumeric("foo5 "));
    assertFalse(isAlphanumeric("foo5_"));
  }

  boolean isAlphabetic(String s) {
    for (int ix = 0; ix < s.length(); ++ix) {
      int codePoint = s.codePointAt(ix);
      if (!Character.isAlphabetic(codePoint)) {
        return false;
      }
    }
    return true;
  }

  boolean isAlphanumeric(String s) {
    for (int ix = 0; ix < s.length(); ++ix) {
      int codePoint = s.codePointAt(ix);
      if (!Character.isAlphabetic(codePoint) && !Character.isDigit(codePoint)) {
        return false;
      }
    }
    return true;
  }

}
