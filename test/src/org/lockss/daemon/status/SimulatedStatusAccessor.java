/*
 * $Id: SimulatedStatusAccessor.java,v 1.3 2003-03-15 00:27:22 troberts Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.daemon.status.*;

public class SimulatedStatusAccessor implements StatusAccessor {
  private List columns;
  private List rows;
  private List sortRules;

  public SimulatedStatusAccessor() {
    columns = makeColumns();
    rows = makeRows();
    sortRules = makeSortRules();
  }
  
  private List makeColumns() {
    List columns = new ArrayList();
    columns.add(new StatusTable.ColumnDescriptor("column_1", "Column 1", 
						 StatusTable.TYPE_STRING));
    columns.add(new StatusTable.ColumnDescriptor("column_2", "Column 2", 
						 StatusTable.TYPE_INT));
    return columns;
  }

  private List makeRows() {
    List rows = new ArrayList();
    for (int ix=0; ix<5; ix++) {
      Map row = new HashMap();
      row.put("column_1", "val"+ix);
      row.put("column_2", new Integer(ix));
      rows.add(row);
    }
    return rows;
  }

  private List makeSortRules() {
    List sortRules = new ArrayList();
    sortRules.add(new StatusTable.SortRule("column_1", true));
    return sortRules;
  }

  public List getColumnDescriptors(String key) {
    return columns;
  }

  public List getRows(String key) {
    return rows;
  }

  public List getDefaultSortRules(String key) {
    return sortRules;
  }

  public boolean requiresKey() {
    return false;
  }

  public String getTitle(String key) {
    return "Simulated Table for key "+key;
  }

}
