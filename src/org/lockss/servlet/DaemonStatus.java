// ========================================================================
// $Id: DaemonStatus.java,v 1.10 2003-03-26 23:12:04 tal Exp $
// ========================================================================

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

package org.lockss.servlet;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
//  import com.mortbay.servlet.*;
//  import org.mortbay.util.*;
import org.mortbay.html.*;
import org.mortbay.tools.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

/** DaemonStatus servlet
 */
public class DaemonStatus extends LockssServlet {
  private static final String cellContrastColor = "#DDDDDD";
  private static final String bAddRem = "Add/Remove Cluster Clients";
  private static final String bDelete = "Delete";
  private static final String bAdd = "Add";
//   public static final DateFormat df =
//     DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
  public static final DateFormat df =
    new SimpleDateFormat("MM/dd/yy HH:mm:ss");

  private String tableName;
  private String key;

  private boolean isForm = false;
  private boolean html = false;
  private String errorMsg = null;
  private StatusService statSvc;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    statSvc = getLockssDaemon().getStatusService();
  }


  public void lockssHandle() throws IOException {
    PrintWriter wrtr = resp.getWriter();
    Page page = null;
    Date now = new Date();

    html = req.getParameter("text") == null;

    resp.setContentType(html ? "text/html" : "text/plain");

    tableName = req.getParameter("table");
    key = req.getParameter("key");
    if (StringUtil.isNullString(tableName)) {
      tableName = StatusService.ALL_TABLES_TABLE;
    }
    if (StringUtil.isNullString(key)) {
      key = null;
    }

    if (html) {
      page = newPage();

      page.add("<center>" + getMachineName() + " at " +
	       df.format(now) + "</center>");
      page.add("<br>");

//       page.add("<center>");
//       page.add(srvLink(SERVLET_DAEMON_STATUS, ".",
// 		       concatParams("text=1", req.getQueryString())));
//       page.add("</center><br><br>");
    } else {
      wrtr.println("host=" + getLcapIPAddr() +
		   ",time=" + now.getTime() +
		   ",version=" + "0.0");
    }

    doStatusTable(page, wrtr, tableName, key);
    if (html) {
      page.add(getFooter());
      page.write(wrtr);
    }
  }

  // Build the table
  private void doStatusTable(Page page, PrintWriter wrtr,
			     String tableName, String tableKey)
      throws IOException {
    StatusTable statTable;
    try {
      statTable = statSvc.getTable(tableName, tableKey);
    } catch (StatusService.NoSuchTableException e) {
      if (html) {
	page.add("Can't get table: ");
	page.add(e.toString());
      } else {
	wrtr.println("Error getting table: " + e.toString());
      }
      return;
    }
    java.util.List colList = statTable.getColumnDescriptors();
    java.util.List rowList = statTable.getSortedRows();
    String title = statTable.getTitle();
    String titleFoot = statTable.getTitleFootnote();

    Table table = null;

    ColumnDescriptor cds[] =
      (ColumnDescriptor [])colList.toArray(new ColumnDescriptor[0]);
    int cols = cds.length;
    Iterator rowIter = rowList.iterator();
    if (true || rowIter.hasNext()) {
      // if table not empty, output column headings
//       table = new Table(0, "CELLSPACING=2 CELLPADDING=0 WIDTH=\"100%\"");
      table = new Table(0, "ALIGN=CENTER CELLSPACING=2 CELLPADDING=0");
      if (html) {
	// ensure title footnote numbered before ColDesc.HEADs
	title = title + addFootnote(titleFoot);

	table.newRow();
	table.addHeading(title, "ALIGN=CENTER COLSPAN=" +
			   (cols * 2 - 1));
	table.newRow();
	java.util.List summary = statTable.getSummaryInfo();
	if (summary != null && !summary.isEmpty()) {
	  for (Iterator iter = summary.iterator(); iter.hasNext(); ) {
	    StatusTable.SummaryInfo sInfo = 
	      (StatusTable.SummaryInfo)iter.next();
	    table.newRow();
	    StringBuffer sb = new StringBuffer();
	    sb.append("<b>");
	    sb.append(sInfo.getTitle());
	    sb.append("</b>: ");
	    sb.append(dispString(sInfo.getValue(), sInfo.getType()));
	    table.newCell("COLSPAN=" + (cols * 2 - 1));
	    table.add(sb.toString());

	  }
	  table.newRow();
	}

	for (int ix = 0; ix < cols; ix++) {
	  ColumnDescriptor cd = cds[ix];
	  String head = cd.getTitle() + addFootnote(cd.getFootNote());
	  table.addHeading(head, "align=" + ((cols != 1) ?
					     getColAlignment(cd)
					     : "center" ));
	  if (ix < (cols - 1)) {
	    table.newCell("width = 8");
	  }
	}
      } else {
	wrtr.println();
	wrtr.println("table=" + title);
	if (tableKey != null) {
	  wrtr.println("key=" + tableKey);
	}	  
      }

    }
    while (rowIter.hasNext()) {
      Map rowMap = (Map)rowIter.next();
      if (html) {
	table.newRow();
	for (int ix = 0; ix < cols; ix++) {
	  ColumnDescriptor cd = cds[ix];
	  Object val = rowMap.get(cd.getColumnName());
	  String disp;

	  table.newCell("align=" + getColAlignment(cd));
	  table.add(dispString(rowMap.get(cd.getColumnName()), cd.getType()));
	  if (ix < (cols - 1)) {
	    table.newCell();	// empty column for spacing
	  }
	}
      } else {
	Iterator iter = rowMap.keySet().iterator();
	while (iter.hasNext()) {
	  String key = (String)iter.next();
	  wrtr.print(key + "=" + rowMap.get(key).toString());
	  if (iter.hasNext()) {
	    wrtr.print(",");
	  } else {
	    wrtr.println();
	  }
	}
      }
    }
    if (html && table != null) {
      page.add(table);
      page.add("<br>");
    }
  }

  private String getColAlignment(ColumnDescriptor cd) {
    switch (cd.getType()) {
    case ColumnDescriptor.TYPE_STRING:
    case ColumnDescriptor.TYPE_FLOAT:	// tk - should align decimal points?
    case ColumnDescriptor.TYPE_DATE:
    case ColumnDescriptor.TYPE_IP_ADDRESS:
    case ColumnDescriptor.TYPE_TIME_INTERVAL:
    default:
      return "LEFT";
    case ColumnDescriptor.TYPE_INT:
    case ColumnDescriptor.TYPE_PERCENT:
      return "RIGHT";
    }
  }


  private String dispString(Object val, int type) {
    if (val instanceof StatusTable.Reference) {
      StatusTable.Reference ref = (StatusTable.Reference)val;
      StringBuffer sb = new StringBuffer();
      sb.append("table=");
      sb.append(ref.getTableName());
      String key = ref.getKey();
      if (!StringUtil.isNullString(key)) {
	sb.append("&key=");
	sb.append(urlEncode(key));
      }
      return srvLink(myServletDescr(), dispString1(ref.getValue(), type),
		     sb.toString());
    } else {
      return dispString1(val, type);
    }
  }

  private String dispString1(Object val, int type) {
    if (val instanceof StatusTable.DisplayedValue) {
      StatusTable.DisplayedValue aval = (StatusTable.DisplayedValue)val;
      String str = dispString2(aval.getValue(), type);
      String color = aval.getColor();
      if (color != null) {
	str = "<font color=red>" + str + "</font>";
      }
      return str;
    } else {
      return dispString2(val, type);
    }
  }

  private String dispString2(Object val, int type) {
    if (val == null) {
      return "";
    }
    try {
      switch (type) {
      case ColumnDescriptor.TYPE_STRING:
      case ColumnDescriptor.TYPE_FLOAT:
      case ColumnDescriptor.TYPE_INT:
      default:
	return val.toString();
      case ColumnDescriptor.TYPE_PERCENT:
	float fv = ((Number)val).floatValue();
	return Integer.toString(Math.round(fv * 100)) + "%";
      case ColumnDescriptor.TYPE_DATE:
	Date d;
	if (val instanceof Number) {
	  d = new Date(((Number)val).longValue());
	} else if (val instanceof Date) {
	  d = (Date)val;
	} else {
	  return val.toString();
	}
	return df.format(d);
      case ColumnDescriptor.TYPE_IP_ADDRESS:
	return ((InetAddress)val).getHostAddress();
      case ColumnDescriptor.TYPE_TIME_INTERVAL:
	long millis = ((Number)val).longValue();
	return StringUtil.timeIntervalToString(millis);
      }
    } catch (NumberFormatException e) {
      log.warning("Bad number: " + val.toString() + ": " + e.toString());
      return val.toString();
    } catch (ClassCastException e) {
      log.warning("Wrong type value: " + val.toString() + ": " + e.toString());
      return val.toString();
    } catch (Exception e) {
      log.warning("Error formatting value: " + val.toString() + ": " + e.toString());
      return val.toString();
    }
  }

  // don't make me a link in nav table if I'm displaying table of all tables
  protected boolean includeMeInNav() {
    return !StatusService.ALL_TABLES_TABLE.equals(tableName);
  }
}
