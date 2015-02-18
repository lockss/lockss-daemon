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
import java.net.*;
import java.math.BigInteger;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.status.*;

public class SimulatedStatusAccessor {
  private static Logger log = Logger.getLogger("SimStatus");

  public static void register(LockssDaemon daemon) {
    log.debug("registering");
    StatusService statusService = daemon.getStatusService();
    statusService.registerStatusAccessor("table1", new SimAccessor1());
    statusService.registerStatusAccessor("table2", new SimAccessor2());
  }

  private static class SimAccessor implements StatusAccessor {
    private List columns;
    private List rows;
    private List sortRules;
    private String title = "Untitled";

    public SimAccessor(String title, int nrows, int ncols) {
      this.title = title;
      columns = makeColumns(ncols);
      rows = makeRows(nrows, ncols);
      sortRules = makeSortRules();
    }

    int ctype[] = {
      ColumnDescriptor.TYPE_STRING,
      ColumnDescriptor.TYPE_INT,
      ColumnDescriptor.TYPE_PERCENT,
      ColumnDescriptor.TYPE_TIME_INTERVAL,
      ColumnDescriptor.TYPE_FLOAT,
      ColumnDescriptor.TYPE_IP_ADDRESS,
      ColumnDescriptor.TYPE_DATE,
    };

    public String getDisplayName() {
      return title;
    }

    private List makeColumns(int ncols) {
      List columns = new ArrayList(ncols);
      for (int ix = 1; ix <= ncols; ix++) {
	columns.add(new ColumnDescriptor(coltag(ix),
					 "Column " + ix,
					 coltype(ix-1),
					 "Footnote for colemn " + ix));
      }
      return columns;
    }

    private int coltype(int col) {
      return ctype[col % ctype.length];
    }

    private String coltag(int col) {
      return "column_" + col;
    }

    private Object colval(int row, int col) {
      switch (coltype(col-1)) {
      case ColumnDescriptor.TYPE_STRING:
      default:
	return "xyzzy_" + row;
      case ColumnDescriptor.TYPE_INT:
	return new Integer(row);
      case ColumnDescriptor.TYPE_PERCENT:
	return new Float(1.0 / row);
      case ColumnDescriptor.TYPE_TIME_INTERVAL:
	return new Long(Math.round(Math.pow(59, row)) * Constants.SECOND);
// 	return new Integer((59 << row) * Constants.SECOND);
      case ColumnDescriptor.TYPE_FLOAT:
	return new Float(1.0 / row);
      case ColumnDescriptor.TYPE_IP_ADDRESS:
	try {
	  return IPAddr.getByName("10.1.0.42");
	} catch (UnknownHostException e) {
	  return null;
	}
      case ColumnDescriptor.TYPE_DATE:
	return new Date(TimeBase.nowMs() + (Constants.DAY * row));
      }
    }

    private List makeRows(int nrows, int ncols) {
      List rows = new ArrayList();
      for (int rx=0; rx < nrows; rx++) {
	Map row = new HashMap();
	for (int cx = 1; cx <= ncols; cx++) {
	  if (rx != 3 || cx != 2) {
	    row.put(coltag(cx), colval(rx, cx));
	  }
	}
	rows.add(row);
      }
      return rows;
    }

    private List makeSortRules() {
      List sortRules = new ArrayList();
      sortRules.add(new StatusTable.SortRule("column_1", true));
      return sortRules;
    }

    public boolean requiresKey() {
      return false;
    }

    public void populateTable(StatusTable table) {
      table.setTitle(title);
      table.setColumnDescriptors(columns);
      table.setDefaultSortRules(sortRules);
      table.setRows(rows);
    }
  }

  private static class SimAccessor1 extends SimAccessor {
    SimAccessor1() {
      super("Table 1", 9, 2);
    }

  }

  private static class SimAccessor2 extends SimAccessor {
    SimAccessor2() {
      super("Silly Table 2", 7, 6);
    }

  }
}
