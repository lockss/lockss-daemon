/*
 * $Id: IpAccessControl.java,v 1.11 2004-05-12 19:54:36 tlipkis Exp $
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

package org.lockss.servlet;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import org.mortbay.html.*;
import org.mortbay.tools.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

/** Display and update IP access control lists.
 */
public abstract class IpAccessControl extends LockssServlet {

  private static final String footIP =
    "List individual IP addresses (<code>172.16.31.14</code>), " +
    "class A, B or C subnets (<code>10.*.*.*</code> ,&nbsp " +
    "<code>172.16.31.*</code>) or CIDR notation " +
    "subnets (<code>172.16.31.0/24</code>). " +
    "<br>Enter each address or subnet mask on a separate line. ";

  protected static final String commonExp =
    "To be allowed access, an IP address must match some entry on the " +
    "allow list, and not match any entry on the deny list.";

  static Logger log = Logger.getLogger("IpAccessServlet");

  private ConfigManager configMgr;

  // Used to insert error messages into the page
  private Vector inclErrs;
  private Vector exclErrs;
  private String errMsg;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    configMgr = getLockssDaemon().getConfigManager();
  }

  public void lockssHandleRequest() throws IOException {
    String action = req.getParameter("action");

    inclErrs = null;
    exclErrs = null;
    errMsg = null;

    if ("Update".equals(action)){
      Vector incl = ipStrToVector(req.getParameter("inc_ips"));
      Vector excl = ipStrToVector(req.getParameter("exc_ips"));
      inclErrs = findInvalidIps(incl);
      exclErrs = findInvalidIps(excl);

      if (inclErrs.size() > 0 || exclErrs.size() > 0) {
        displayPage(incl, excl);
      } else {
	try {
	  saveIPChanges(incl, excl);
	} catch (Exception e) {
	  log.error("Error saving changes", e);
	  errMsg = "Error: Couldn't save changes:<br>" + e.toString();
	}
	displayPage(incl, excl);
      }
    } else {
      displayPage();
    }
  }

  /**
   * Display the page, given no new ip addresses and no errors
   */
  private void displayPage()
      throws IOException {
    Vector incl = getListFromParam(getIncludeParam());
    Vector excl = getListFromParam(getExcludeParam());
    // hack to remove possibly duplicated first element from platform subnet
    if (incl.size() >= 2 && incl.get(0).equals(incl.get(1))) {
      incl.remove(0);
    }
    displayPage(incl, excl);
  }

  private Vector getListFromParam(String param) {
    Configuration config = Configuration.getCurrentConfig();
    return StringUtil.breakAt(config.get(param), ';');
  }

  /**
   * Display the UpdateIps page.
   * @param incl vector of included ip addresses
   * @param excl vector of excluded ip addresses
   */
  private void displayPage(Vector incl, Vector excl)
      throws IOException {
    Page page = newPage();
    page.add(getExplanationBlock(getExplanation()));
    page.add("<br>");
    page.add(getIncludeExcludeElement(incl, excl));
    page.add(getFooter());
    page.write(resp.getWriter());
  }

  /**
   * Generate the block of the page that has the included/excluded
   * ip address table.
   * @param incl vector of included ip addresses
   * @param excl vector of excluded ip addresses
   * @return an html element representing the include/exclude ip address form
   */
  private Element getIncludeExcludeElement(Vector incl, Vector excl) {
    boolean isError = false;
    String incString = null;
    String excString = null;

    Composite comp = new Composite();
    Block centeredBlock = new Block(Block.Center);

    Form frm = new Form(srvURL(myServletDescr(), null));
    frm.method("POST");

    Table table = new Table(1, "BORDER=1 CELLPADDING=0");
    //table.center();
    if (errMsg != null) {
      table.newRow();
      table.newCell("colspan=2");
      table.add("<font color=red>" + errMsg + "</font>");
    }
    table.newRow("bgcolor=\"#CCCCCC\"");
    table.newCell("align=center");
    table.add("<font size=+1>Allow Access" + addFootnote(footIP) +
	      "&nbsp;</font>");

    table.newCell("align=center");
    table.add("<font size=+1>Deny Access" + addFootnote(footIP) +
	      "&nbsp;</font>");

    if ((inclErrs != null && inclErrs.size() > 0) ||
	(exclErrs != null && exclErrs.size() > 0)) {
      String errorStr = null;
      table.newRow();
      table.newCell();
      addIPErrors(table, inclErrs);
      table.newCell();
      addIPErrors(table, exclErrs);
    }

    incString = getIPString(incl);
    excString = getIPString(excl);

    TextArea incArea = new TextArea("inc_ips");
    incArea.setSize(30, 20);
    incArea.add(incString);

    TextArea excArea = new TextArea("exc_ips");
    excArea.setSize(30, 20);
    excArea.add(excString);

    table.newRow();
    table.newCell("align=center");
    setTabOrder(incArea);
    table.add(incArea);
    table.newCell("align=center");
    setTabOrder(excArea);
    table.add(excArea);

    centeredBlock.add(table);
    frm.add(centeredBlock);
    Input submit = new Input(Input.Submit, "action", "Update");
    setTabOrder(submit);
    frm.add("<br><center>"+submit+"</center>");
    comp.add(frm);
    return comp;
  }

  private void addIPErrors(Composite comp, Vector errs) {
    if (errs != null && errs.size() > 0) {
      comp.add("<font color=red>The following entries have errors:</font><br>");
      for (Iterator iter = errs.iterator(); iter.hasNext(); ) {
	comp.add((String)iter.next());
	comp.add("<br>");
      }
    } else {
      comp.add("&nbsp");
      }
  }

  /**
   * Checks the validity of a vector of IP addresses
   * @param ipList vector containing strings representing the ip addresses
   * @return vector of the malformed ip addresses
   */
  public Vector findInvalidIps(Vector ipList) {
    Vector errorIPs = new Vector();

    if (ipList != null) {
      for (Iterator iter =  ipList.iterator(); iter.hasNext();) {
	String ipStr = (String)iter.next();
	IpFilter.Mask ip;
	try {
	  ip = new IpFilter.Mask(ipStr, true);
	} catch (IpFilter.MalformedException e) {
	  errorIPs.addElement(ipStr + ":  " + e.getMessage());
	}
      }
    }
    return errorIPs;
  }

  /**
   * Convert a string of newline separated IP addresses to a vector of strings,
   * removing duplicates
   *
   * @param ipStr string to convert into a vector
   * @return vector of strings representing ip addresses
   */
  private Vector ipStrToVector(String ipStr) {
    return StringUtil.breakAt(ipStr, '\n', 0, true, true);
  }

  /**
   * Convert a vector of strings representing ip addresses
   * to a single string with the addresses seperated by a newline
   */
  private String getIPString(Vector ipList) {
    return StringUtil.terminatedSeparatedString(ipList, "\n", "\n");
  }

  /**
   * Save the ip addresses to the cluster.txt property file
   * @param incIPsList vector of ip addresses to include
   * @param excIPsList vector of ip addresses to exclude
   * @return whether the save was successful
   */
  public void saveIPChanges(Vector incIPsList, Vector excIPsList)
      throws IOException {
    String incStr = StringUtil.separatedString(incIPsList, ";");
    String excStr = StringUtil.separatedString(excIPsList, ";");

    Properties acProps = new Properties();
    acProps.put(getIncludeParam(), incStr);
    acProps.put(getExcludeParam(), excStr);
    configMgr.writeCacheConfigFile(acProps,
				   getConfigFileName(),
				   getConfigFileComment());
  }

  protected abstract String getExplanation();

  protected abstract String getIncludeParam();

  protected abstract String getExcludeParam();

  protected abstract String getConfigFileName();

  protected abstract String getConfigFileComment();
}
