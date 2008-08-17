/*
 * $Id: TestCartesianProductIterator.java,v 1.1 2008-08-17 08:48:51 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
import junit.framework.TestCase;
import org.lockss.test.LockssTestCase;

/**
 * Test class for org.lockss.util.CartesianProductIterator
 */

public class TestCartesianProductIterator extends LockssTestCase {

  public void testIllIter() {
    try {
      Iterator iter = new CartesianProductIterator(Collections.EMPTY_LIST);
      fail("Should have thrown");
    } catch (Exception e) {
    }
  }

  void assertCartProd(Object[][] expected, Collection<List> listOfSets) {
    int ix = 0;
    for (Iterator iter = new CartesianProductIterator(listOfSets);
	 iter.hasNext();) {
      if (ix >= expected.length) {
	fail("More than the expected " + expected.length +
	     " combinations of " + listOfSets);
      }
      assertEquals("Conbination " + ix,
		   ListUtil.fromArray(expected[ix++]),
		   ListUtil.fromArray((Object[])iter.next()));
    }
    assertEquals("Number of combinations", expected.length, ix);
  }

  public void testSingletons() {
    String[][] exp1 = {
      {"dog"},
    };

    String[][] exp2 = {
      {"dog", "cat", "duck"},
    };

    assertCartProd(exp1, ListUtil.list(ListUtil.list("dog")));

    assertCartProd(exp2, ListUtil.list(ListUtil.list("dog"),
				       ListUtil.list("cat"),
				       ListUtil.list("duck")));
  }

  public void testOneSet() {
    String[][] exp1 = {
      {"dog"},
      {"cat"},
      {"duck"},
    };

    assertCartProd(exp1, ListUtil.list(ListUtil.list("dog", "cat", "duck")));
  }

  public void testTwoSets() {
    String[][] exp1 = {
      {"dog", "bone"},
      {"ham", "bone"},
      {"hand", "bone"},
      {"dog", "jive"},
      {"ham", "jive"},
      {"hand", "jive"},
    };

    assertCartProd(exp1, ListUtil.list(ListUtil.list("dog", "ham", "hand"),
				       ListUtil.list("bone", "jive")));
  }

  public void testMixed() {
    String[][] exp1 = {
      {"dog", "and", "bone"},
      {"ham", "and", "bone"},
      {"hand", "and", "bone"},
      {"dog", "and", "jive"},
      {"ham", "and", "jive"},
      {"hand", "and", "jive"},
    };

    assertCartProd(exp1, ListUtil.list(ListUtil.list("dog", "ham", "hand"),
				       ListUtil.list("and"),
				       ListUtil.list("bone", "jive")));
  }

}
