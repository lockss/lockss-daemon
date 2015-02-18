/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;
import junit.framework.TestCase;
import org.lockss.test.*;

public class TestMapUtil extends LockssTestCase {
  public void testFromArgs() {
    Map m1 = MapUtil.map("a", "1", "b", "2", "c", "3", "d", "4", "e", "5",
			 "f", "6", "g", "7", "h", "8", "j", "9", "k", "10");
    Map exp = new HashMap();
    exp.put("a", "1");
    exp.put("b", "2");
    exp.put("c", "3");
    exp.put("d", "4");
    exp.put("e", "5");
    exp.put("f", "6");
    exp.put("g", "7");
    exp.put("h", "8");
    exp.put("j", "9");
    exp.put("k", "10");
    assertEquals(exp, m1);


    try {
      MapUtil.map("a", "1", "b", "2", "c", "3", "d", "4", "e", "5",
		  "f", "6", "g", "7", "h", "8", "j", "9", "k");
      fail("Odd length arg list should throw");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testFromList1() {
    assertEquals(MapUtil.map(), MapUtil.fromList(ListUtil.list()));
    assertEquals(MapUtil.map("FOO", "bar", "One", "Two"),
		 MapUtil.fromList(ListUtil.list("FOO", "bar", "One", "Two")));
    assertEquals(MapUtil.map("foo", "bar", "one", "two"),
		 MapUtil.fromList(ListUtil.list(ListUtil.list("foo", "bar"),
						ListUtil.list("one", "two"))));

    try {
      MapUtil.fromList(ListUtil.list("FOO", "bar", "One"));
      fail("Odd length arg list should throw");
    } catch (IllegalArgumentException e) {
    }
    try {
      MapUtil.fromList(ListUtil.list(ListUtil.list("foo", "bar"),
				     ListUtil.list("one")));
      fail("Short sublist should throw");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testExpandMultiKeys() {
    assertEquals(MapUtil.map(),
		 MapUtil.expandAlternativeKeyLists(MapUtil.map()));
    assertEquals(MapUtil.map("1", "A"),
		 MapUtil.expandAlternativeKeyLists(MapUtil.map("1", "A")));
    assertEquals(MapUtil.map("1", "A", "2", "A"),
		 MapUtil.expandAlternativeKeyLists(MapUtil.map("1;2", "A")));
    assertEquals(MapUtil.map("1", "A", "2", "A"),
		 MapUtil.expandAlternativeKeyLists(MapUtil.map("1 ; 2", "A")));
    assertEquals(MapUtil.map("1", "A", "2", "B", "*", "B"),
		 MapUtil.expandAlternativeKeyLists(MapUtil.map("1", "A",
							       "2;*", "B")));
    assertEquals(MapUtil.map("1", "A", "2", "B", "*", "B"),
		 MapUtil.expandAlternativeKeyLists(MapUtil.map(" 1 ", "A",
							       "2;; *", "B")));
  }

}
