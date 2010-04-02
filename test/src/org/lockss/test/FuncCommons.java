/*
 * $Id: FuncCommons.java,v 1.8 2010-04-02 23:38:11 pgust Exp $
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

package org.lockss.test;

import java.io.*;
import java.util.*;
import junit.framework.TestCase;
import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.collections.map.*;
import org.lockss.util.*;

/**
 * Tests that verify our understanding of the behavior of Commons
 * Collections classes.
 */

public class FuncCommons extends LockssTestCase {

  /** A {Hard,Weak} ref map should automatically remove entries whose
   * values have no other references.  Add lots of otherwise unreferenced
   * objects and make sure the map doesn't grow monotonically.  Check that
   * an object that did have other pointers is still in the map after
   * several collections, and that others aren't.
   */
  public void testRefMapHW() {
    ReferenceMap map = new ReferenceMap(ReferenceMap.HARD,
					ReferenceMap.WEAK);
    int drop = 5; /* 20 too big - PJG */// number of drops in map size needed
    int loop = 10000;			// inner loop repetitions

    Integer aKey = null;		// one key that we will hold on to
    Object aVal = null;			// one value that we will hold on to
    int lastSize = 0;			// last map size

    // Repeat until we have seen enough drops in size
    for (int ii = 0; drop > 0; ii++) {
      for (int jj = 0; jj < loop; jj++) {
	Integer key = new Integer(ii*loop+jj); // make a unique key
	Integer val = new Integer(jj);
	map.put(key, val);

	if (key.intValue() == 143) {	// hold on to key 143
	  aKey = key;
	}
	if (key.intValue() == 243) {	// hold on to value for key 243
	  aVal = val;
	}
      }
//       System.out.println(""+ map.size());
      if (map.size() < lastSize) {
	// Map is smaller than last time.  Some entries have been removed.
	drop--;
      } else if ((ii % 100) == 0) {
	System.gc();
      }
      lastSize = map.size();
    }
    // Because the value for key 243 still exists, that entry should still
    // be in the map, and should be the same as the one we put in
    assertTrue(map.containsKey(new Integer(243)));
    assertSame(aVal, (Integer)map.get(new Integer(243)));

    // This key's value has been collected by now (we hope), so it shouldn't
    // be in the map
    assertFalse(map.containsKey(new Integer(221)));

    // Holding on to a key shouldn't have any effect.  This one shouldn't
    // be in the map either.
    assertFalse(map.containsKey(new Integer(143)));
    assertFalse(map.containsKey(aKey));
  }

  // Just here to convince myself that LinkedMap handles null values
  public void testLinkedMap() {
    LinkedMap lm = new LinkedMap();
    lm.put("foo", "1");
    lm.put("bar", null);
    assertIsomorphic(ListUtil.list("foo", "bar").iterator(),
		     lm.keySet().iterator());
    assertIsomorphic(ListUtil.list("1", null).iterator(),
		     lm.values().iterator());
  }
}
