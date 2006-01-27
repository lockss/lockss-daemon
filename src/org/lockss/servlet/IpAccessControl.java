/*
 * $Id: IpAccessControl.java,v 1.34 2006-01-27 04:33:59 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import java.util.*;

import javax.servlet.*;

import org.mortbay.html.*;

import org.lockss.config.*;
import org.lockss.util.*;

/** Display and update IP access control lists.
 */
public abstract class IpAccessControl extends LockssServlet {

  private static final String DENY_IPS_NAME = "exc_ips";
  private static final String ALLOW_IPS_NAME = "inc_ips";
  public static final String SUFFIX_IP_INCLUDE = "include";
  public static final String SUFFIX_IP_EXCLUDE = "exclude";
  public static final String SUFFIX_PLATFORM_ACCESS = "platformAccess";

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

  // Values read from form
  private Vector formIncl;
  private Vector formExcl;

  // Used to insert error messages into the page
  private Vector inclErrs;
  private Vector exclErrs;
  protected boolean isForm;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    configMgr = getLockssApp().getConfigManager();
  }

  public void lockssHandleRequest() throws IOException {
    String action = req.getParameter("action");
    isForm = !StringUtil.isNullString(action);

    inclErrs = null;
    exclErrs = null;
    formIncl = null;
    formExcl = null;
    errMsg = null;

    if ("Update".equals(action)){
      readForm();
      doUpdate();
    } else {
      displayPage();
    }
  }

  protected void readForm() {
    formIncl = ipStrToVector(req.getParameter(ALLOW_IPS_NAME));
    formExcl = ipStrToVector(req.getParameter(DENY_IPS_NAME));
    inclErrs = findInvalidIps(formIncl);
    exclErrs = findInvalidIps(formExcl);
  }

  protected void doUpdate() throws IOException {
    if (inclErrs.size() > 0 || exclErrs.size() > 0) {
      displayPage();
    } else {
      try {
	saveChanges();
      } catch (Exception e) {
	log.error("Error saving changes", e);
	errMsg = "Error: Couldn't save changes:<br>" + e.toString();
      }
      displayPage();
    }
  }

  /**
   * Display the page, given no new ip addresses and no errors
   */
  protected void displayPage()
      throws IOException {
    if (formIncl != null || formExcl != null) {
      displayPage(formIncl, formExcl);
    } else {
      Vector incl = getListFromParam(getIncludeParam());
      Vector excl = getListFromParam(getExcludeParam());
      // hack to remove possibly duplicated first element from platform subnet
      if (incl.size() >= 2 && incl.get(0).equals(incl.get(1))) {
	incl.remove(0);
      }
      displayPage(incl, excl);
    }
  }

  private Vector getListFromParam(String param) {
    Configuration config = CurrentConfig.getCurrentConfig();
    return new Vector(config.getList(param));
  }

  /**
   * Display the UpdateIps page.
   * @param incl vector of included ip addresses
   * @param excl vector of excluded ip addresses
   */
  private void displayPage(Vector incl, Vector excl)
      throws IOException {
    // Create and start laying out page
    Page page = newPage();
    ServletUtil.layoutExplanationBlock(page, getExplanation());
    page.add("<br>");

    // Create and layout form
    Form form = ServletUtil.newForm(srvURL(myServletDescr()));
    layoutErrorBlock(form);
    ServletUtil.layoutIpAllowDenyTable(this, form, incl, excl, footIP,
        inclErrs, exclErrs, ALLOW_IPS_NAME, DENY_IPS_NAME);
    additionalFormLayout(form);
    ServletUtil.layoutSubmitButton(this, form, "Update");
    page.add(form);

    // Finish laying out page
    layoutFooter(page);
    page.write(resp.getWriter());
  }

  protected void additionalFormLayout(Composite composite) {
    // nothing by default
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
	  // value not used, constructor called to check if malformed
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
   * Save the include and exclude lists to the access control file
   */
  protected void saveChanges() throws IOException {
    Properties props = new Properties();
    addConfigProps(props);
    configMgr.writeCacheConfigFile(props,
				   getConfigFileName(),
				   getConfigFileComment());
  }

  protected void addConfigProps(Properties props) {
    String incStr = StringUtil.separatedString(formIncl, ";");
    String excStr = StringUtil.separatedString(formExcl, ";");
    String prefix = getParamPrefix();

    props.put(getIncludeParam(), incStr);
    props.put(getExcludeParam(), excStr);
    // Save current value of platform param so can detect change (in
    // ConfigManager.appendPlatformAccess()).
    String plat =
      CurrentConfig.getParam(ConfigManager.PARAM_PLATFORM_ACCESS_SUBNET);
    if (!StringUtil.isNullString(plat)) {
      props.put(prefix + SUFFIX_PLATFORM_ACCESS, plat);
    }
  }

  protected abstract String getExplanation();

  protected String getIncludeParam() {
    return getParamPrefix() + SUFFIX_IP_INCLUDE;
  }

  protected String getExcludeParam() {
    return getParamPrefix() + SUFFIX_IP_EXCLUDE;
  }

  protected abstract String getParamPrefix();

  protected abstract String getConfigFileName();

  protected abstract String getConfigFileComment();
}
