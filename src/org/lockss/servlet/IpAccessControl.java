/*
 * $Id$
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
import java.util.List;

import javax.servlet.*;

import org.mortbay.html.*;

import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.account.*;

/** Display and update IP access control lists.
 */
public abstract class IpAccessControl extends LockssServlet {

  public static final String PREFIX = Configuration.PREFIX + "ui.";
  /** Warn and require confirmation if a subnet mask in an IP access
   * control inclusion list has fewer bits than this */
  public static final String PARAM_WARN_BELOW_BITS =
    PREFIX + "subnetMaskBitsWarn";
  public static final int DEFAULT_WARN_BELOW_BITS = 16;
  /** Disallow subnet masks with fewer bits than this in IP access control
   * inclusion lists */
  public static final String PARAM_ERROR_BELOW_BITS =
    PREFIX + "subnetMaskBitsError";
  public static final int DEFAULT_ERROR_BELOW_BITS = 8;

  private static final String DENY_IPS_NAME = "exc_ips";
  private static final String ALLOW_IPS_NAME = "inc_ips";
  public static final String SUFFIX_IP_INCLUDE = "include";
  public static final String SUFFIX_IP_EXCLUDE = "exclude";
  public static final String SUFFIX_PLATFORM_ACCESS = "platformAccess";

  private enum PermissivenessCheck { NONE, WARN, ERROR }

  public static final String ACTION_UPDATE = "Update";
  public static final String ACTION_CONFIRM = "Confirm";

  private static final String footIP =
    "List individual IP addresses (<code>172.16.31.14</code>), " +
    "class A, B or C subnets (<code>10.*.*.*</code> ,&nbsp " +
    "<code>172.16.31.*</code>) or CIDR notation " +
    "subnets (<code>172.16.31.0/24</code>). " +
    "<br>Enter each address or subnet on a separate line. " +
    "Lines that start with # are comments.";

  protected static final String commonExp =
    "To be allowed access, an IP address must match some entry on the " +
    "allow list, and not match any entry on the deny list.";

  static Logger log = Logger.getLogger("IpAccessServlet");

  protected ConfigManager configMgr;

  // Values read from form
  private List<String> formIncl;
  private List<String> formExcl;

  // Used to insert error messages into the page
  private List<String> inclErrs;
  private List<String> exclErrs;
  protected boolean isForm;

  private int warnBits;
  private int errorBits;
  private boolean confirm;
  private int warnings;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    configMgr = getLockssApp().getConfigManager();
  }

  protected void lockssHandleRequest() throws IOException {
    String action = req.getParameter("action");
    isForm = !StringUtil.isNullString(action);

    inclErrs = null;
    exclErrs = null;
    formIncl = null;
    formExcl = null;
    statusMsg = null;
    errMsg = null;
    confirm = false;
    warnings = 0;

    if (ACTION_CONFIRM.equals(action)) {
      confirm = true;
      action = ACTION_UPDATE;
    }
    if (ACTION_UPDATE.equals(action)) {
      readForm();
      doUpdate();
    } else {
      displayPage();
    }
  }

  protected void readForm() {
    Configuration config = configMgr.getCurrentConfig();
    warnBits = config.getInt(PARAM_WARN_BELOW_BITS,
			     DEFAULT_WARN_BELOW_BITS);
    errorBits = config.getInt(PARAM_ERROR_BELOW_BITS,
			      DEFAULT_ERROR_BELOW_BITS);
    formIncl = ipStrToList(req.getParameter(ALLOW_IPS_NAME));
    formExcl = ipStrToList(req.getParameter(DENY_IPS_NAME));
    inclErrs = findInvalidIps(formIncl, (confirm ? PermissivenessCheck.ERROR :
					 PermissivenessCheck.WARN));
    exclErrs = findInvalidIps(formExcl, PermissivenessCheck.NONE);
  }

  protected void doUpdate() throws IOException {
    if (inclErrs.size() > 0 || exclErrs.size() > 0) {
      displayPage();
    } else {
      try {
	saveChanges();
	statusMsg = "Update successful";
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
      List<String> incl = getListFromParam(getIncludeParam());
      List<String> excl = getListFromParam(getExcludeParam());
      // hack to remove possibly duplicated first element from platform subnet
      if (incl.size() >= 2 && incl.get(0).equals(incl.get(1))) {
	incl.remove(0);
      }
      displayPage(incl, excl);
    }
  }

  private List<String> getListFromParam(String param) {
    Configuration config = CurrentConfig.getCurrentConfig();
    return config.getList(param);
  }

  /**
   * Display the UpdateIps page.
   * @param incl list of included ip addresses
   * @param excl list of excluded ip addresses
   */
  private void displayPage(List<String> incl, List<String> excl)
      throws IOException {
    // Create and start laying out page
    Page page = newPage();
    layoutErrorBlock(page);
    ServletUtil.layoutExplanationBlock(page, getExplanation());
    page.add("<br>");

    // Create and layout form
    Form form = ServletUtil.newForm(srvURL(myServletDescr()));
    ServletUtil.layoutIpAllowDenyTable(this, form, incl, excl, footIP,
        inclErrs, exclErrs, ALLOW_IPS_NAME, DENY_IPS_NAME);
    additionalFormLayout(form);
    boolean doConfirm = warnings != 0 && warnings == inclErrs.size();
    String action = doConfirm ? ACTION_CONFIRM : ACTION_UPDATE;
    ServletUtil.layoutSubmitButton(this, form, action, i18n.tr(action));
    page.add(form);

    // Finish laying out page
    endPage(page);
  }


  protected void additionalFormLayout(Composite composite) {
    // nothing by default
  }

  /**
   * Checks the validity of a list of IP addresses
   * Elements that start with # are comments, ignored.
   * @param ipList List containing strings representing the ip addresses
   * @return List of the malformed ip addresses
   */
  public List<String> findInvalidIps(List<String> ipList,
				     PermissivenessCheck perm) {
    List<String> errorIPs = new ArrayList<String>();

    if (ipList != null) {
      for (String ipStr : ipList) {
	if (ipStr.startsWith("#")) {
	  continue;
	}
	IpFilter.Mask ip;
	try {
	  // Constructor throws if malformed
	  ip = IpFilter.newMask(ipStr);
	  switch (perm) {
	  case WARN:
	  case ERROR:
	    int b = ip.getMaskBits();
	    if (b < errorBits) {
	      errorIPs.add(ipStr + ": Subnet mask with fewer than " +
			   errorBits + " bits not allowed");
	    } else if (perm == PermissivenessCheck.WARN && b < warnBits) {
	      warnings++;
	      errorIPs.add(ipStr +
			   ": Please confirm that you wish to allow " +
			   "access from all " + (1 << (32 - b)) +
			   " IP addresses in this range");
	    }
	    break;
	  case NONE:
	  }
	} catch (IpFilter.MalformedException e) {
	  errorIPs.add(ipStr + ":  " + e.getMessage());
	}
      }
    }
    return errorIPs;
  }

  /**
   * Convert a string of newline separated IP addresses and comments to a
   * list of strings.  Comment lines start with # and may not contain
   * semicolon.  (Any semicolons will be changed to colon; as these lists
   * are stored as semicolon-separated strings)
   *
   * @param ipStr string to convert into a list
   * @return List of strings representing ip addresses
   */
  private List<String> ipStrToList(String ipStr) {
    List<String> res = new ArrayList<String>();
    // 
    for (String str : StringUtil.breakAt(ipStr, '\n', 0, true, true)) {
      res.add(str.replace(';', ':'));
    }
    return res;
  }

  /**
   * Save the include and exclude lists to the access control file
   */
  protected void saveChanges() throws IOException {
    List<String> origIncl = getListFromParam(getIncludeParam());
    List<String> origExcl = getListFromParam(getExcludeParam());

    Properties props = new Properties();
    addConfigProps(props);
    configMgr.writeCacheConfigFile(props,
				   getConfigFileName(),
				   getConfigFileComment());
    raiseAlert(origIncl, origExcl, formIncl, formExcl);
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

  protected String diffStr(List<String> lst) {
    return StringUtil.terminatedSeparatedString(lst, "\n", "\n");
  }

  protected void raiseAlert(List<String> origIncl, List<String> origExcl,
			    List<String> newIncl, List<String> newExcl) {
    UserAccount acct = getUserAccount();
    if (acct != null) {
      StringBuilder sb = new StringBuilder();
      sb.append("changed ");
      sb.append(getConfigFileComment());
      sb.append(":\n\n");

      String di = DiffUtil.diff_configText(diffStr(origIncl), diffStr(newIncl));
      String de = DiffUtil.diff_configText(diffStr(origExcl), diffStr(newExcl));

      if (!StringUtil.isNullString(di)) {
	sb.append("Include differences:\n\n");
	sb.append(di);
	sb.append("\n");
      }
      if (!StringUtil.isNullString(de)) {
	sb.append("Exclude differences:\n\n");
	sb.append(de);
      }
      acct.auditableEvent(sb.toString());
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
