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
import org.lockss.util.*;
import org.lockss.daemon.status.*;

public class MockStatusAccessor implements StatusAccessor {
  private boolean requiresKey = false;
  private Map columnDescriptors = new HashMap();
  private Map rows = new HashMap();
  private Map defaultSortRules = new HashMap();
  private Map titles = new HashMap();
  private Map summaryInfo = new HashMap();
  private Map titleFeet = new HashMap();

  public String getDisplayName() {
    return "MockStatusAccessor";
  }

  public void setColumnDescriptors(List columnDescriptors, String key) {
    this.columnDescriptors.put(key, columnDescriptors);
  }

  public void setRows(List rows, String key) {
    this.rows.put(key, rows);
  }

  public void setDefaultSortRules(List sortRules, String key) {
    defaultSortRules.put(key, sortRules);
  }

  public void setRequiresKey(boolean requiresKey) {
    this.requiresKey = requiresKey;
  }

  public boolean requiresKey() {
    return requiresKey;
  }

  public void setTitle(String tableTitle, String key) {
    titles.put(key, tableTitle);
  }

  public void setTitleFoot(String titleFoot, String key) {
    titleFeet.put(key, titleFoot);
  }

  public void setSummaryInfo(String key, List summaryInfo) {
    this.summaryInfo.put(key, summaryInfo);
  }

  public void populateTable(StatusTable table)
      throws StatusService.NoSuchTableException {
    String key = table.getKey();
    table.setTitle((String)titles.get(key));
    String titleFoot = (String)titleFeet.get(key);
    if (titleFoot != null) {
      table.setTitleFootnote(titleFoot);
    }
    table.setColumnDescriptors((List)columnDescriptors.get(key));
    table.setDefaultSortRules((List)defaultSortRules.get(key));
    table.setRows((List)rows.get(key));
    table.setSummaryInfo((List)summaryInfo.get(key));
  }

  // utilities for building rows & columns and MockStatusAccessors

  public static List makeSummaryInfoFrom(Object[][] summaryInfoArray) {
    List list = new ArrayList(summaryInfoArray.length);
    for (int ix = 0; ix < summaryInfoArray.length; ix++) {
      StatusTable.SummaryInfo summaryInfo =
	new StatusTable.SummaryInfo((String)summaryInfoArray[ix][0],
			       ((Integer)summaryInfoArray[ix][1]).intValue(),
			       summaryInfoArray[ix][2]);
      if (summaryInfoArray[ix].length >= 4) {
	summaryInfo.setHeaderFootnote((String)summaryInfoArray[ix][3]);
      }
      list.add(summaryInfo);
    }
    return list;
  }

  public static MockStatusAccessor generateStatusAccessor(Object[][]colArray,
							  Object[][]rowArray) {
    return generateStatusAccessor(colArray, rowArray, null);
  }

  public static MockStatusAccessor generateStatusAccessor(Object[][]colArray,
							  Object[][]rowArray,
							  String key) {
    MockStatusAccessor statusAccessor = new MockStatusAccessor();
    List columns = MockStatusAccessor.makeColumnDescriptorsFrom(colArray);
    List rows = MockStatusAccessor.makeRowsFrom(columns, rowArray);

    statusAccessor.setColumnDescriptors(columns, key);
    statusAccessor.setRows(rows, key);

    return statusAccessor;
  }

  public static MockStatusAccessor generateStatusAccessor(Object[][]colArray,
							  Object[][]rowArray,
							  String key,
							  Object[][]summaryInfos) {
    MockStatusAccessor statusAccessor =
      generateStatusAccessor(colArray, rowArray, key);
    statusAccessor.
      setSummaryInfo(key,
		     MockStatusAccessor.makeSummaryInfoFrom(summaryInfos));
    return statusAccessor;
  }

  public static void addToStatusAccessor(MockStatusAccessor statusAccessor,
					 Object[][]colArray,
					 Object[][]rowArray, String key) {
    List columns = MockStatusAccessor.makeColumnDescriptorsFrom(colArray);
    List rows = MockStatusAccessor.makeRowsFrom(columns, rowArray);
    statusAccessor.setColumnDescriptors(columns, key);
    statusAccessor.setRows(rows, key);
  }

  public static List makeColumnDescriptorsFrom(Object[][] cols) {
    List list = new ArrayList(cols.length);
    for (int ix = 0; ix < cols.length; ix++) {
      String footNote = null;
      if (cols[ix].length == 4) {
 	footNote = (String) cols[ix][3];
      }
      ColumnDescriptor col =
	new ColumnDescriptor((String)cols[ix][0], (String)cols[ix][1],
			     ((Integer)cols[ix][2]).intValue(), footNote);
      list.add(col);
    }
    return list;
  }

  public static List makeRowsFrom(List cols, Object[][] rows) {
    List rowList = new ArrayList();
    for (int ix=0; ix<rows.length; ix++) {
      Map row = new HashMap();
      for (int jy=0; jy<rows[ix].length; jy++) {
	Object colEnt = cols.get(jy);
	Object colName;
	if (colEnt instanceof ColumnDescriptor) {
	  colName = ((ColumnDescriptor)colEnt).getColumnName();
	} else {
	  colName = colEnt;
	}
	row.put(colName, rows[ix][jy]);

// 	if (rows[ix][jy] != null) {
// 	}
      }
      rowList.add(row);
    }
    return rowList;
  }
}
