/*
 * $Id: ProxyAndContent.java,v 1.7 2006-01-13 22:44:31 thib_gc Exp $
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

import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.iterators.*;
import org.mortbay.html.*;

import org.lockss.config.*;
import org.lockss.daemon.ResourceManager;
import org.lockss.proxy.AuditProxyManager;
import org.lockss.proxy.icp.IcpManager;
import org.lockss.util.*;

/*
 * This file used to be called AccessControl.java
 */

public class ProxyAndContent extends LockssServlet {

  protected boolean isForm;

  private String action;

  private int auditPort;

  private ConfigManager configMgr;

  private boolean formAuditEnable;

  private String formAuditPort;

  private boolean formIcpEnable;

  private String formIcpPort;

  private int icpPort;

  private ResourceManager resourceMgr;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    configMgr = getLockssApp().getConfigManager();
    resourceMgr = getLockssApp().getResourceManager();
  }

  protected void lockssHandleRequest() throws ServletException, IOException {
    action = req.getParameter(ACTION_TAG);
    isForm = !StringUtil.isNullString(action);
    errMsg = null;
    statusMsg = null;

    if (StringUtil.isNullString(action)) displayMenu_Main();
    else if (action.equals("Update")) processUpdate_Main();
  }

  private void displayMenu_Main() throws IOException {
    final int BORDER = 0;
    final String ATTRIBUTES = "align=\"center\" cellpadding=\"10\"";

    Page page = newPage();
    ServletUtil.layoutExplanationBlock(page, mainExplanation);
    layoutErrorBlock(page);
    ServletUtil.layoutMenu(page, getDescriptors_Main());

    Form frm = ServletUtil.newForm(srvURL(myServletDescr()));
    Table tbl = new Table(BORDER, ATTRIBUTES);
    ServletUtil.layoutEnablePortRow(this, tbl, AUDIT_ENABLE_NAME, getDefaultAuditEnable(),
        "audit proxy", AUDIT_FOOT, FILTER_FOOT, AUDIT_PORT_NAME, getDefaultAuditPort(),
        resourceMgr.getUsableTcpPorts(AuditProxyManager.SERVER_NAME));
    ServletUtil.layoutEnablePortRow(this, tbl, ICP_ENABLE_NAME, getDefaultIcpEnable(),
        "ICP server", ICP_FOOT, FILTER_FOOT, ICP_PORT_NAME, getDefaultIcpPort(),
        resourceMgr.getUsableUdpPorts(AuditProxyManager.SERVER_NAME));
    frm.add(tbl);
    ServletUtil.layoutSubmitButton(this, frm, "Update");
    page.add(frm);

    layoutFooter(page);
    resp.setContentType("text/html");
    page.write(resp.getWriter());
  }

  private boolean getDefaultAuditEnable() {
    if (isForm) {
      return formAuditEnable;
    }
    return CurrentConfig.getBooleanParam(PARAM_AUDIT_ENABLE,
                                         DEFAULT_AUDIT_ENABLE);
  }

  private String getDefaultAuditPort() {
    String port = formAuditPort;
    if (StringUtil.isNullString(port)) {
      port = CurrentConfig.getParam(PARAM_AUDIT_PORT);
    }
    return port;
  }

  private boolean getDefaultIcpEnable() {
    return isForm ? formIcpEnable : getLockssDaemon().getIcpManager().isIcpServerRunning();
  }

  private String getDefaultIcpPort() {
    String port = formIcpPort;
    if (StringUtil.isNullString(port)) {
      port = Integer.toString(getLockssDaemon().getIcpManager().getCurrentPort());
    }
    return port;
  }

  private boolean isLegalAuditPort(int port) {
    return port >= 1024 &&
        resourceMgr.isTcpPortAvailable(port, AuditProxyManager.SERVER_NAME);
  }

  private boolean isLegalIcpPort(int port) {
    return port >= 1024 &&
        resourceMgr.isUdpPortAvailable(port, IcpManager.class);
  }

  private void processUpdate_Main() throws IOException {
    ArrayList errList = new ArrayList();

    // Read form
    processUpdate_Main_ReadForm();

    // Process form
    processUpdate_Main_Audit(errList);
    processUpdate_Main_Icp(errList);

    // Prepare error message
    if (errList.size() > 0) {
      // There were errors
      StringBuffer buffer = new StringBuffer();
      StringUtil.separatedString(errList, "<br>", buffer);
      errMsg = buffer.toString();
    }
    else {
      // There were no errors
      try {
        processUpdate_Main_SaveChanges();
      }
      catch (IOException ioe) {
        logger.error("Could not save changes", ioe);
        errMsg = "Error: Could not save changes.\n" + ioe.toString();
      }
    }

    displayMenu_Main();
  }

  private void processUpdate_Main_Audit(ArrayList errList) {
    auditPort = -1;
    try {
      auditPort = Integer.parseInt(formAuditPort);
    } catch (NumberFormatException nfe) {
      if (formAuditEnable) {
        // bad number is an error only if enabling
        errList.add("Audit proxy port must be a number: " + formAuditPort);
      }
    }
    if (formAuditEnable && !isLegalAuditPort(auditPort)) {
      errList.add("Illegal audit proxy port number: " + formAuditPort
          + ", must be >=1024 and not in use");
    }
  }

  private void processUpdate_Main_Icp(ArrayList errList) {
    icpPort = -1;
    try {
      icpPort = Integer.parseInt(formIcpPort);
    }
    catch (NumberFormatException nfe) {
      if (formIcpEnable) {
        // bad number is an error only if enabling
        errList.add("ICP port must be a number: " + formIcpPort);
      }
    }
    if (formIcpEnable && !isLegalIcpPort(icpPort)) {
      errList.add("Illegal ICP port number: " + formIcpPort
          + ", must be >=1024 and not in use");
    }
  }

  private void processUpdate_Main_ReadForm() {
    formAuditEnable = !StringUtil.isNullString(req.getParameter(AUDIT_ENABLE_NAME));
    formAuditPort = req.getParameter(AUDIT_PORT_NAME);
    formIcpEnable = !StringUtil.isNullString(req.getParameter(ICP_ENABLE_NAME));
    formIcpPort = req.getParameter(ICP_PORT_NAME);
  }

  private void processUpdate_Main_SaveChanges() throws IOException {
    final String TRUE = "true";
    final String FALSE = "false";

    Properties props = new Properties();
    props.setProperty(PARAM_AUDIT_ENABLE, formAuditEnable ? TRUE : FALSE);
    props.setProperty(PARAM_AUDIT_PORT, Integer.toString(auditPort));
    props.setProperty(IcpManager.PARAM_ICP_ENABLED, formIcpEnable ? TRUE : FALSE);
    props.setProperty(IcpManager.PARAM_ICP_PORT, Integer.toString(icpPort));
    configMgr.writeCacheConfigFile(props,
        ConfigManager.CONFIG_FILE_PROXY_IP_ACCESS,
        COMMENT_PROXY_IP_ACCESS);
  }

  public static final String PARAM_AUDIT_ENABLE = AuditProxyManager.PARAM_START;

  public static final String PARAM_AUDIT_PORT = AuditProxyManager.PARAM_PORT;

  static final boolean DEFAULT_AUDIT_ENABLE = AuditProxyManager.DEFAULT_START;

  private static final String AUDIT_ENABLE_NAME = "audit_ena";

  private static final String AUDIT_FOOT =
    "The audit proxy serves <b>only</b> cached content, and never " +
    "forwards requests to the publisher or any other site.  " +
    "By configuring a browser to proxy to this port, you can view the " +
    "content stored on the cache.  All requests for content not on the " +
    "cache will return a \"404 Not Found\" error.";

  private static final String AUDIT_PORT_NAME = "audit_port";

  private static final String COMMENT_PROXY_IP_ACCESS =
    "Proxy IP Access Control";

  private static final String FILTER_FOOT =
    "Other ports can be configured but may not be reachable due to "
    + "packet filters.";

  private static final String ICP_ENABLE_NAME = "icp_ena";

  /**
   * <p>A footnote explaining the role of the ICP server.</p>
   */
  private static final String ICP_FOOT =
    "The ICP server responds to queries sent by other proxies and caches "
    + "to let them know if this cache has the content they are looking for. "
    + "This is useful if you are integrating this cache into an existing "
    + "network structure with other proxies and caches that support ICP.";

  private static final String ICP_PORT_NAME = "icp_port";

  private static Logger logger = Logger.getLogger("IpAccessServlet");

  private static final String mainExplanation =
    "Configure proxy options, such as the audit proxy and the ICP server "
    + "(see below). Manage access groups and configure access rules for "
    + "the content preserved on this cache.";

  protected static Iterator getDescriptors_Main() {
    return new FilterIterator(
        new ObjectArrayIterator(servletDescrs),
        PredicateUtils.falsePredicate());
  }

}
