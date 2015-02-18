/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import org.lockss.test.*;
import org.lockss.util.IntStepFunction.Point;
import static org.lockss.util.Constants.*;

/**
 * This is the test class for org.lockss.util.IntStepFunction
 */
public class TestIntStepFunction extends LockssTestCase {
  public void testIll() {
    try {
      new IntStepFunction((List)null);
      fail("Should have throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }
    try {
      new IntStepFunction(ListUtil.list());
      fail("Should have throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }
    try {
      new IntStepFunction((String)null);
      fail("Should have throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }
    try {
      new IntStepFunction("");
      fail("Should have throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }
    try {
      new IntStepFunction("foo");
      fail("Should have throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }
  }

  void assertExp(IntStepFunction lsf) {
    assertEquals(0, lsf.getValue(Integer.MIN_VALUE));
    assertEquals(0, lsf.getValue(Integer.MIN_VALUE));
    assertEquals(0, lsf.getValue(-10));
    assertEquals(0, lsf.getValue(0));
    assertEquals(10, lsf.getValue(100));
    assertEquals(10, lsf.getValue(110));
    assertEquals(10, lsf.getValue(199));
    assertEquals(20, lsf.getValue(200));
    assertEquals(20, lsf.getValue(201));
    assertEquals(20, lsf.getValue(499));
    assertEquals(90, lsf.getValue(500));
    assertEquals(90, lsf.getValue(Integer.MAX_VALUE));
    assertEquals(90, lsf.getValue(Integer.MAX_VALUE));
  }

  public void testStep1() {
    assertExp(new IntStepFunction(ListUtil.list(new Point(100, 10),
						 new Point(200, 20),
						 new Point(500, 90))));
  }

  IntStepFunction.Point point(int x, int y) {
    return new IntStepFunction.Point(x, y);
  }

  public void testParse() {
    assertEquals(ListUtil.list(point(100,10), point(200,20), point(500,90)), 
		 IntStepFunction.parseString("[100,10],[200,20],[500,90]"));
  }

  public void testStep2() {
    assertExp(new IntStepFunction("[100,10],[200,20],[500,90]"));
  }
}
