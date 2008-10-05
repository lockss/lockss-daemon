/*
 * $Id: TestMaxSizeRecordingMap.java,v 1.1 2007-01-14 08:52:46 tlipkis Exp $
 */

/*

Copyright (c) 2006-2007 Board of Trustees of Leland Stanford Jr. University,
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
import junit.framework.*;
import org.lockss.test.LockssTestCase;

public class TestMaxSizeRecordingMap extends LockssTestCase {

  public static Class testedClasses[] = {
    org.lockss.util.MaxSizeRecordingMap.class
  };

  public void testFromEmpty() {
    MaxSizeRecordingMap map = new MaxSizeRecordingMap(new HashMap());
    assertEquals(0, map.size());
    assertEquals(0, map.getMaxSize());
    map.put("1", "foo");
    assertEquals(1, map.size());
    assertEquals(1, map.getMaxSize());
    map.put("2", "bar");
    map.put("3", "frob");
    assertEquals(3, map.size());
    assertEquals(3, map.getMaxSize());
    assertEquals("bar", map.put("2", "baz"));
    assertEquals(3, map.size());
    assertEquals(3, map.getMaxSize());
    map.put("2", null);
    assertEquals(3, map.size());
    assertEquals(3, map.getMaxSize());
    map.remove("2");
    assertEquals(2, map.size());
    assertEquals(3, map.getMaxSize());
    map.remove("3");
    assertEquals(1, map.size());
    assertEquals(3, map.getMaxSize());
    map.put("2", "bar");
    map.put("3", "frob");
    map.put("4", "gorp");
    assertEquals(4, map.size());
    assertEquals(4, map.getMaxSize());
  }

  public void testFromMap() {
    HashMap m1 = new HashMap();
    m1.put("a", "A");
    m1.put("b", "B");
    MaxSizeRecordingMap map = new MaxSizeRecordingMap(m1);
    assertEquals(2, map.size());
    assertEquals(2, map.getMaxSize());
    map.put("1", "foo");
    assertEquals(3, map.size());
    assertEquals(3, map.getMaxSize());
    map.remove("a");
    assertEquals(2, map.size());
    assertEquals(3, map.getMaxSize());
    map.remove("1");
    assertEquals(1, map.size());
    assertEquals(3, map.getMaxSize());
    map.put("2", "bar");
    map.put("3", "frob");
    map.put("4", "gorp");
    assertEquals(4, map.size());
    assertEquals(4, map.getMaxSize());
  }
}
