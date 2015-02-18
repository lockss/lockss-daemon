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

package org.lockss.test;
import java.util.*;
import java.net.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.status.*;

public class TestMockStatusAccessor extends LockssTestCase {

  /**
   * I don't normally like to write tests for test code, but this is a pretty
   * simple one that can help shake out potential testing problems
   */
  public void testMakeColumnDescriptorsFrom() {
    List expectedColumns =
      ListUtil.list(new ColumnDescriptor("blah", "Blah", 0),
		    new ColumnDescriptor("blah2", "Blah2", 0));

    Object[][] cols = {
      {"blah", "Blah", new Integer(0)},
      {"blah2", "Blah2", new Integer(0)}
    };
    List actualColumns = MockStatusAccessor.makeColumnDescriptorsFrom(cols);
    assertColumnDescriptorsEqual(expectedColumns, actualColumns);
  }

  //see comment for testMakeColumnDescriptorsFrom
  public void testMakeRowsFrom() {
    Object[][] cols = {
      {"col1", "Column the first", new Integer(0)},
      {"col2", "Column the second", new Integer(0)}
    };
    List columns = MockStatusAccessor.makeColumnDescriptorsFrom(cols);

    HashMap row1 = new HashMap();
    row1.put("col1", "val1");
    row1.put("col2", "val2");

    HashMap row2 = new HashMap();
    row2.put("col1", "val21");
    row2.put("col2", "val22");

    HashMap row3 = new HashMap();
    row3.put("col1", "val31");
    row3.put("col2", "val32");
    List expectedRows = ListUtil.list(row1, row2, row3);

    Object[][] rows = {
      {"val1", "val2"},
      {"val21", "val22"},
      {"val31", "val32"}
    };
    List actualRows = MockStatusAccessor.makeRowsFrom(columns, rows);
    assertRowsEqual(expectedRows, actualRows);
  }
  // assertions

  private void assertColumnDescriptorsEqual(List expected, List actual) {
    assertEquals("Lists had different sizes", expected.size(), actual.size());
    Iterator expectedIt = expected.iterator();
    Iterator actualIt = actual.iterator();
    while(expectedIt.hasNext()) {
      ColumnDescriptor expectedCol = (ColumnDescriptor)expectedIt.next();
      ColumnDescriptor actualCol = (ColumnDescriptor)actualIt.next();
      assertEquals(expectedCol.getColumnName(), actualCol.getColumnName());
      assertEquals(expectedCol.getTitle(), actualCol.getTitle());
      assertEquals(expectedCol.getType(), actualCol.getType());
      assertEquals(expectedCol.getFootnote(), actualCol.getFootnote());
    }
    assertFalse(actualIt.hasNext());
  }

  private void assertRowsEqual(List expected, List actual) {
    assertEquals("Different number of rows", expected.size(), actual.size());
    Iterator expectedIt = expected.iterator();
    Iterator actualIt = actual.iterator();
    int rowNum=0;
    while (expectedIt.hasNext()) {
      Map expectedMap = (Map)expectedIt.next();
      Map actualMap = (Map)actualIt.next();
      assertEquals("Failed on row "+rowNum, expectedMap, actualMap);
      rowNum++;
    }
  }

}

