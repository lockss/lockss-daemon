/*
 * $Id: IpAccessControl.java,v 1.2 2003-05-10 02:44:18 tal Exp $
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
import org.mortbay.html.*;
import org.mortbay.tools.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

/** Display and update IP access control lists.
 */
public class IpAccessControl extends LockssServlet {
  static final String AC_PREFIX = ServletManager.IP_ACCESS_PREFIX;
  public static final String PARAM_IP_INCLUDE = AC_PREFIX + "include";
  public static final String PARAM_IP_EXCLUDE = AC_PREFIX + "exclude";

  private static final String footIP =
    "List individual IP addresses (<code>172.16.31.14</code>), " +
    "class A, B or C subnets (<code>10.*.*.*</code> ,&nbsp " +
    "<code>172.16.31.*</code>) or CIDR notation " +
    "subnets (<code>172.16.31.0/24</code>). " +
    "<br>Enter each address or subnet mask on a separate line. " +
    "<br>Deny addresses supersede Allow addresses." +
    "<br>If the Allow list is empty, no remote access will be allowed.";

  static Logger log = Logger.getLogger("IpAcc");

//   public void init(ServletConfig config) throws ServletException {
//     super.init(config);
//   }

  public void lockssHandleRequest() throws IOException {
    String action = req.getParameter("action");
    boolean isError = false;

    if ("Update".equals(action)){
      Vector incl = ipStrToVector(req.getParameter("inc_ips"));
      Vector excl = ipStrToVector(req.getParameter("exc_ips"));
      Vector inclErrs = findInvalidIps(incl);
      Vector exclErrs = findInvalidIps(excl);

      if (inclErrs.size() > 0 || exclErrs.size() > 0) {
        displayPage(incl, excl, inclErrs, exclErrs);
      } else {
	try {
	  saveIPChanges(incl, excl);
	} catch (Exception e) {
	  log.error("Error saving changes", e);
	  // set to null so will display unedited values.
	  // xxx should display error here
	  incl = null;
	  excl = null;
	}
	displayPage(incl, excl, null, null);
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
    Vector incl = getListFromParam(PARAM_IP_INCLUDE);
    Vector excl = getListFromParam(PARAM_IP_EXCLUDE);
    // hack to remove possibly duplicated first element from platform subnet
    if (incl.size() >= 2 && incl.get(0).equals(incl.get(1))) {
      incl.remove(0);
    }
    displayPage(incl, excl, null, null);
  }

  private Vector getListFromParam(String param) {
    Configuration config = Configuration.getCurrentConfig();
    return StringUtil.breakAt(config.get(param), ';');
  }

  /**
   * Display the UpdateIps page.
   * @param incl vector of included ip addresses
   * @param excl vector of excluded ip addresses
   * @param inclErrs vector of malformed include ip addresses.
   * @param exclErrs vector of malformed include ip addresses.
   */
  private void displayPage(Vector incl, Vector excl,
                           Vector inclErrs, Vector exclErrs)
      throws IOException {
    Page page = newPage();
    page.add(getIncludeExcludeElement(incl, excl,
                                      inclErrs, exclErrs));
    page.add(getFooter());
    page.write(resp.getWriter());
  }

  /**
   * Generate the block of the page that has the included/excluded
   * ip address table.
   * @param incl vector of included ip addresses
   * @param excl vector of excluded ip addresses
   * @param inclErrs vector of malformed include ip addresses.
   * @param exclErrs vector of malformed include ip addresses.
   * @return an html element representing the include/exclude ip address form
   */
  private Element getIncludeExcludeElement(Vector incl, Vector excl,
                                           Vector inclErrs,
                                           Vector exclErrs) {
    boolean isError = false;
    String incString = null;
    String excString = null;

    Composite comp = new Composite();
    Block centeredBlock = new Block(Block.Center);

    Form frm = new Form(srvURL(myServletDescr(), null));
    frm.method("POST");

    Table table = new Table(1, "BORDER=1 CELLPADDING=0");
    //table.center();
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
      if (inclErrs != null && inclErrs.size() > 0) {
        errorStr = getIPString(inclErrs);
        table.add("Please correct the following error: &nbsp;<p><b><font color=\"#FF0000\">Error: Invalid IP Address "+errorStr+"</font></b>");
      } else {
        table.add("&nbsp");
      }

      table.newCell();
      if (exclErrs != null && exclErrs.size() > 0) {
        errorStr = getIPString(exclErrs);
        table.add("Please correct the following error: &nbsp;<p><b><font color=\"#FF0000\">Error: Invalid IP Address "+errorStr+"</font></b>");
      } else {
        table.add("&nbsp");
      }
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
    table.add(incArea);
    table.newCell("align=center");
    table.add(excArea);

    centeredBlock.add(table);
    frm.add(centeredBlock);
    Input submit = new Input(Input.Submit, "action", "Update");
    frm.add("<center>"+submit+"</center>");
    comp.add(frm);
    return comp;
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
	  errorIPs.addElement(ipStr);
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
   * @param realpath path to the cluster.txt file
   * @return whether the save was sucessful
   */
  public void saveIPChanges(Vector incIPsList, Vector excIPsList)
      throws IOException {
    String incStr = StringUtil.separatedString(incIPsList, ";");
    String excStr = StringUtil.separatedString(excIPsList, ";");

    Properties acProps = new Properties();
    acProps.put(PARAM_IP_INCLUDE, incStr);
    acProps.put(PARAM_IP_EXCLUDE, excStr);
    Configuration.writeCacheConfigFile(acProps,
				       Configuration.CONFIG_FILE_UI_IP_ACCESS,
				       "UI IP Access Control");
  }

  /**
   * Read the prop file, change ip access list props and rewrite file
   * @param filename name of the cluster property file
   * @param incStr string of included ip addresses
   * @param excStr string of excluded ip addresses
   * @return whether the save was sucessful
   */
//   public boolean replacePropertyInFile(File file, String incStr,
// 				       String excStr) {
//     PropertyTree t = new PropertyTree();
//     String filename = file.getPath();

//     try {
//       if (file.exists()) {
// 	// get property tree
// 	InputStream istr = new FileInputStream(filename);
// 	t.load(istr);
// 	istr.close();
//       }
//       // replace ip properties
//       t.put(LcapProperties.IP_INCLUDE_PROP_KEY, incStr);
//       t.put(LcapProperties.IP_EXCLUDE_PROP_KEY, excStr);

//       // write properties to file
//       saveTree(t, filename, "LOCKSS Cluster Property file");
//     } catch (Exception e) {
//       LcapLog.error("UpdateIps: Error writing prop file", filename + ": " + e);
//       return false;
//     }
//     return true;
//   }
}
