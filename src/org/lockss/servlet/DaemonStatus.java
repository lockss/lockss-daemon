// ========================================================================
// $Id: DaemonStatus.java,v 1.1 2003-03-12 22:15:35 tal Exp $
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

/** DaemonStatus servlet
 */
public class DaemonStatus extends LockssServlet {
  private static final String cellContrastColor = "#DDDDDD";
  private static final String bAddRem = "Add/Remove Cluster Clients";
  private static final String bDelete = "Delete";
  private static final String bAdd = "Add";
  private boolean isForm = false;
  private String errorMsg = null;

  public void lockssHandle() throws IOException {
    Page page = null;

    // Display table
    page = newPage();
    page.add(getMachineTable());
    page.add(getFooter());
    page.write(resp.getWriter());
  }

  // Build the table
  private Composite getMachineTable() throws IOException {
    Composite comp;
    Table table =
      new Table(0, "align=center border=0 cellspacing=5 cellpadding=0");

    comp = table;

    table.newRow();
    table.newCell();
    table.add("<b>Machine Name&nbsp&nbsp</b>");

    table.newCell();
    table.add("<b>IP Address&nbsp&nbsp</b>");

    table.newCell();
    table.add("<b>Links</b>");

    table.newRow();
    table.newCell("COLSPAN=3");
    table.add("<hr noshade>");

    table.newRow();
    table.newCell();

    boolean odd=false;
    String clients[] = {"foobar"};
    // arrange for me to be first
    for (int i = 0; i < clients.length; i++) {
      String c = clients[i];
      if (c.equals(adminAddr)) {
	if (i != 0) {
	  clients[i] = clients[0];
	  clients[0] = c;
	}
	break;
      }
    }
    for (int i = 0; i < clients.length; i++) {
      String client = clients[i];
      File f = getClientPropFile(client);

      if (f.exists()) {
        if (odd) {
          table.newRow("bgcolor="+cellContrastColor);
        } else {
          table.newRow();
	}
	odd = !odd;
        // display configuration parameters for dir name(machine-ip)
        getTableEntry(client, table);
      }
    }
    if (comp != table) {
      comp.add(table);
    }
    comp.add("<p>");

    return comp;
  }

  // Add a client row to the table
  public void getTableEntry(String ip, Table table)
      throws IOException {

    String machineName = getMachineName(ip);

    table.newCell();
    table.add(machineName);

    table.newCell();
    table.add(ip);

    table.newCell();
    // XXX need api to pass client to srvLink
    // XXX barring that, rebind it
    String saveClient = clientAddr;
    try {
      clientAddr = ip;
//       table.add(srvLink(SERVLET_JOURNAL_STATUS, null));
//       table.add("<br>");
//       table.add(srvLink(SERVLET_JOURNAL_SETUP, null));
      if (isForm && !ip.equals(adminAddr)) {
	table.newCell();
	table.add("&nbsp&nbsp&nbsp&nbsp&nbsp");
	table.add(new Input(Input.Submit, "action", bDelete + " " + ip));
      }
    } finally {
      clientAddr = saveClient;
    }
  }

}
