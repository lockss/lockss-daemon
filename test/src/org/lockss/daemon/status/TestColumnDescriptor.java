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

package org.lockss.daemon.status;

import java.util.*;
import org.lockss.test.*;

public class TestColumnDescriptor extends LockssTestCase {
  public void testAccessors() {
    ColumnDescriptor cd1 = new ColumnDescriptor("name1", "title1", 0);
    assertEquals("name1", cd1.getColumnName());
    assertEquals("title1", cd1.getTitle());
    assertEquals(0, cd1.getType());
    assertNull(cd1.getFootnote());
    assertNull(cd1.getComparator());
    assertTrue(cd1.isSortable());

    ColumnDescriptor cd2 = new ColumnDescriptor("name2", "title2", 2, "foot2");
    assertEquals("name2", cd2.getColumnName());
    assertEquals("title2", cd2.getTitle());
    assertEquals(2, cd2.getType());
    assertEquals("foot2", cd2.getFootnote());

    Comparator cmpr = new MockComparatorThatReturns0();
    assertSame(cd2, cd2.setComparator(cmpr));
    assertSame(cmpr, cd2.getComparator());

    assertSame(cd2, cd2.setSortable(false));
    assertFalse(cd2.isSortable());
  }

  public void testEqualsIfEqual() {
    ColumnDescriptor cd1 = new ColumnDescriptor("name", "title", 0);
    ColumnDescriptor cd2 = new ColumnDescriptor("name", "title", 0);
    assertTrue(cd1.equals(cd2));
    assertTrue(cd2.equals(cd1));
  }

  public void testNotEqualsIfDifferent() {
    ColumnDescriptor cd1 = new ColumnDescriptor("name", "title", 0);
    ColumnDescriptor cd2 = new ColumnDescriptor("name2", "title", 0);
    ColumnDescriptor cd3 = new ColumnDescriptor("name", "title2", 0);
    ColumnDescriptor cd4 = new ColumnDescriptor("name", "title", 2);
    assertFalse(cd1.equals(cd2));
    assertFalse(cd1.equals(cd3));
    assertFalse(cd1.equals(cd4));

    assertFalse(cd2.equals(cd1));
    assertFalse(cd2.equals(cd3));
    assertFalse(cd2.equals(cd4));

    assertFalse(cd3.equals(cd1));
    assertFalse(cd3.equals(cd2));
    assertFalse(cd3.equals(cd4));

    assertFalse(cd4.equals(cd1));
    assertFalse(cd4.equals(cd2));
    assertFalse(cd4.equals(cd3));
  }

  public void testNotEqualsIfDiffType() {
    ColumnDescriptor cd1 = new ColumnDescriptor("name", "title", 0);
    assertFalse(cd1.equals("String"));
  }

  class MockComparatorThatReturns0 implements Comparator {
    public int compare(Object o1, Object o2) {
      return 0;
    }
  }
}
