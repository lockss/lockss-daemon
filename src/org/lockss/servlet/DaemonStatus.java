/*
 * $Id: DaemonStatus.java,v 1.18 2003-05-01 23:30:07 tal Exp $
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

  /** Format to display date/time in tables */
  public static final DateFormat df =
  new SimpleDateFormat("MM/dd/yy HH:mm:ss");

//   public static final DateFormat df =
//     DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

  private String tableName;
  private String key;

  private boolean html = false;
  private StatusService statSvc;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    statSvc = getLockssDaemon().getStatusService();
  }

  /**
   * Handle a request
   * @throws IOException
   */
  public void lockssHandleRequest() throws IOException {
    Page page = null;
    Date now = new Date();

    PrintWriter wrtr = null;
    html = req.getParameter("text") == null;

    // After resp.getWriter() has been called, throwing an exception will
    // result in a blank page, so don't call it until the end
    // (unless producing a text page, where we use it as we go along).
    //  (HttpResponse.sendError() calls getOutputStream(), and only one of
    //  getWriter() or getOutputStream() may be called.)

    if (!html) {
      wrtr = resp.getWriter();
    }

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
      page.write(resp.getWriter());
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
	page.add("No such table: ");
	page.add(e.toString());
      } else {
	wrtr.println("No table: " + e.toString());
      }
      return;
    } catch (Exception e) {
      if (html) {
	page.add("Error getting table: ");
	String emsg = e.toString();
	page.add(emsg);
	page.add("<br><pre>    ");
	page.add(StringUtil.trimStackTrace(emsg,
					   StringUtil.stackTraceString(e)));
	page.add("</pre>");
      } else {
	wrtr.println("Error getting table: " + e.toString());
      }
      return;
    }
    java.util.List colList = statTable.getColumnDescriptors();
    java.util.List rowList = statTable.getSortedRows();
    String title0 = statTable.getTitle();
    String titleFoot = statTable.getTitleFootnote();

    Table table = null;

    // convert list of ColumnDescriptors to array of ColumnDescriptors
    ColumnDescriptor cds[] =
      (ColumnDescriptor [])colList.toArray(new ColumnDescriptor[0]);
    int cols = cds.length;
    if (true || !rowList.isEmpty()) {
      // if table not empty, output column headings

      // Make the table.  Make a narrow empty column between real columns,
      // for spacing.  Resulting table will have 2*cols-1 columns
      table = new Table(0, "ALIGN=CENTER CELLSPACING=2 CELLPADDING=0");
      if (html) {
	String title = title0 + addFootnote(titleFoot);

	table.newRow();
	table.addHeading(title, "ALIGN=CENTER COLSPAN=" + (cols * 2 - 1));
	table.newRow();
	addSummaryInfo(table, statTable, cols);

	// output column headings
	for (int ix = 0; ix < cols; ix++) {
	  ColumnDescriptor cd = cds[ix];
	  String head = cd.getTitle() + addFootnote(cd.getFootNote());
	  table.addHeading(head, "valign=bottom align=" +
			   ((cols == 1) ? "center" : getColAlignment(cd)));
	  if (ix < (cols - 1)) {
	    table.newCell("width = 8");
	  }
	}
      } else {
	wrtr.println();
	wrtr.println("table=" + title0);
	if (tableKey != null) {
	  wrtr.println("key=" + tableKey);
	}
	// tk write summary info
      }

    }
    // output rows
    for (Iterator rowIter = rowList.iterator(); rowIter.hasNext(); ) {
      Map rowMap = (Map)rowIter.next();
      if (html) {
	table.newRow();
	for (int ix = 0; ix < cols; ix++) {
	  ColumnDescriptor cd = cds[ix];
	  Object val = rowMap.get(cd.getColumnName());

	  table.newCell("align=" + getColAlignment(cd));
	  table.add(getDisplayString(rowMap.get(cd.getColumnName()),
				     cd.getType()));
	  if (ix < (cols - 1)) {
	    table.newCell();	// empty column for spacing
	  }
	}
      } else {
	for (Iterator iter = rowMap.keySet().iterator(); iter.hasNext(); ) {
	  String key = (String)iter.next();
	  Object val = rowMap.get(key);
	  String valStr = StatusTable.getActualValue(val).toString();
	  wrtr.print(key + "=" + valStr);
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
      String heading = getHeading();
      // put table name in page title so appears in browser title & tabs
      page.title("LOCKSS: " + title0 + " - " + heading);
    }
  }

  private void addSummaryInfo(Table table, StatusTable statTable, int cols) {
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
	sb.append(getDisplayString(sInfo.getValue(), sInfo.getType()));
	table.newCell("COLSPAN=" + (cols * 2 - 1));
	table.add(sb.toString());
      }
      table.newRow();
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


  // turn References into html links
  private String getDisplayString(Object val, int type) {
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
      return srvLink(myServletDescr(), getDisplayString1(ref.getValue(), type),
		     sb.toString());
    } else {
      return getDisplayString1(val, type);
    }
  }

  // add display attributes from a DisplayedValue
  private String getDisplayString1(Object val, int type) {
    if (val instanceof StatusTable.DisplayedValue) {
      StatusTable.DisplayedValue aval = (StatusTable.DisplayedValue)val;
      String str = getDisplayString1(aval.getValue(), type);
      String color = aval.getColor();
      if (color != null) {
	str = "<font color=" + color + ">" + str + "</font>";
      }
      if (aval.getBold()) {
	str = "<b>" + str + "</b>";
      }
      return str;
    } else {
      return convertDisplayString(val, type);
    }
  }

  static NumberFormat bigIntFmt = NumberFormat.getInstance();
  static {
    if (bigIntFmt instanceof DecimalFormat) {
//       ((DecimalFormat)bigIntFmt).setDecimalSeparatorAlwaysShown(true);
    }
  };

  // turn a value into a display string
  String convertDisplayString(Object val, int type) {
    if (val == null) {
      return "";
    }
    try {
      switch (type) {
      case ColumnDescriptor.TYPE_INT:
	if (val instanceof Number) {
	  long lv = ((Number)val).longValue();
	  if (lv >= 1000000) {
	    return bigIntFmt.format(lv);
	  }
	}
	// fall thru
      case ColumnDescriptor.TYPE_STRING:
      case ColumnDescriptor.TYPE_FLOAT:
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
	} else if (val instanceof Deadline) {
	  d = ((Deadline)val).getExpiration();
	} else {
	  return val.toString();
	}
	return dateString(d);
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

  String dateString(Date d) {
    long val = d.getTime();
    if (val == 0 || val == -1) {
      return "never";
    } else {
      return df.format(d);
    }
  }


  // make me a link in nav table unless I'm displaying table of all tables
  protected boolean includeMeInNav() {
    return !StatusService.ALL_TABLES_TABLE.equals(tableName);
  }
}
