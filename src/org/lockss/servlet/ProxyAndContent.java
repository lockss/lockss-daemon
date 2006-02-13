/*
 * $Id: ProxyAndContent.java,v 1.10 2006-02-13 23:07:33 thib_gc Exp $
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

import org.apache.commons.collections.iterators.ObjectArrayIterator;
import org.lockss.config.*;
import org.lockss.daemon.ResourceManager;
import org.lockss.proxy.AuditProxyManager;
import org.lockss.proxy.icp.IcpManager;
import org.lockss.servlet.ServletUtil.LinkWithExplanation;
import org.lockss.util.*;
import org.mortbay.html.*;

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

    // FIXME: change this fake message
    statusMsg = "WARNING: This screen is under construction. DO NOT USE.";

    if (StringUtil.isNullString(action)) displayMenu_Main();
    else if (action.equals(ACTION_MAIN)) displayMenu_Main();
    else if (action.equals(BAD_ACTION)) {
      errMsg = "Unknown action (" + action + ")";
      displayMenu_Main();
    }
    else if (action.equals(ACTION_PROXY)) displayProxy();
    else if (action.equals(ACTION_CONTENT)) displayMenu_Content();
    else if (action.equals(ACTION_UPDATE_PROXY)) processUpdateProxy();
  }

  private void displayMenu(String explanation,
                           Iterator descriptorIter) throws IOException {
    Page page = newPage();
    ServletUtil.layoutExplanationBlock(page, explanation);
    layoutErrorBlock(page);
    ServletUtil.layoutMenu(page, descriptorIter);
    layoutFooter(page);
    resp.setContentType("text/html");
    page.write(resp.getWriter());
  }

  private void displayMenu_Content() throws IOException {
    displayMenu(CONTENT_EXPLANATION, getDescriptors_Content());
  }

  private void displayMenu_Main() throws IOException {
    displayMenu(MAIN_EXPLANATION, getDescriptors_Main());
  }

  private void displayProxy() throws IOException {
    // Start page
    Page page = newPage();
    ServletUtil.layoutExplanationBlock(page, PROXY_EXPLANATION);
    layoutErrorBlock(page);

    // Start form
    Form frm = ServletUtil.newForm(srvURL(myServletDescr(),
                                          ACTION_TAG + "=" + ACTION_UPDATE_PROXY));
    Table tbl = ServletUtil.newEnablePortTable();

    // Audit proxy
    ServletUtil.layoutEnablePortRow(this,
                                    tbl,
                                    AUDIT_ENABLE_NAME,
                                    getDefaultAuditEnable(),
                                    "audit proxy",
                                    AUDIT_FOOT,
                                    FILTER_FOOT,
                                    AUDIT_PORT_NAME,
                                    getDefaultAuditPort(),
                                    resourceMgr.getUsableTcpPorts(AuditProxyManager.SERVER_NAME));

    // ICP server
    ServletUtil.layoutEnablePortRow(this,
                                    tbl,
                                    ICP_ENABLE_NAME,
                                    getDefaultIcpEnable(),
                                    "ICP server",
                                    ICP_FOOT,
                                    FILTER_FOOT,
                                    ICP_PORT_NAME,
                                    getDefaultIcpPort(),
                                    resourceMgr.getUsableUdpPorts(AuditProxyManager.SERVER_NAME));

    // Put parts together
    frm.add(tbl);
    ServletUtil.layoutSubmitButton(this, frm, ACTION_UPDATE_PROXY);
    page.add(frm);

    // Finish up
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
    if (!StringUtil.isNullString(port)) {
      try {
        int portNumber = Integer.parseInt(port);
        if (!(portNumber > 0)) {
          port = "";
        }
      }
      catch (NumberFormatException nfeIgnore) {}
    }
    return port;
  }

  private boolean getDefaultIcpEnable() {
    return isForm ? formIcpEnable : getLockssDaemon().getIcpManager().isIcpServerRunning();
  }

  private String getDefaultIcpPort() {
    String port = formIcpPort;
    if (StringUtil.isNullString(port)) {
      int portNumber = getLockssDaemon().getIcpManager().getCurrentPort();
      port = portNumber > 0 ? Integer.toString(portNumber) : "";
    }
    return port;
  }

  private Iterator getDescriptors_Content() {
    return new ObjectArrayIterator(new LinkWithExplanation[] {
        makeDescriptor("Edit Default Access Group",
                       BAD_ACTION,
                       "Edit the members of the default access group."),
        makeDescriptor("Edit Access Group",
                       BAD_ACTION,
                       "Edit the members of an access group."),
        makeDescriptor("Create Access Group",
                       BAD_ACTION,
                       "Create an access group."),
        makeDescriptor("Delete Access Group",
                       BAD_ACTION,
                       "Delete an access group."),
        makeDescriptor("Configure AU Access",
                       BAD_ACTION,
                       "Select the access groups associated with an archival unit."),
    });
  }

  private Iterator getDescriptors_Main() {
    return new ObjectArrayIterator(new LinkWithExplanation[] {
        makeDescriptor("Proxy Options",
                       ACTION_PROXY,
                       "Configure the audit proxy and the ICP server."),
        makeDescriptor("Content Access Control",
                       ACTION_CONTENT,
                       "Manage access groups and access control rules."),
    });
  }

  private boolean isLegalAuditPort(int port) {
    return port >= 1024 &&
        resourceMgr.isTcpPortAvailable(port, AuditProxyManager.SERVER_NAME);
  }

  private boolean isLegalIcpPort(int port) {
    return port >= 1024 &&
        resourceMgr.isUdpPortAvailable(port, IcpManager.class);
  }

  private LinkWithExplanation makeDescriptor(String linkText,
                                             String linkAction,
                                             String linkExpl) {
    return new LinkWithExplanation(
        srvLink(myServletDescr(), linkText, ACTION_TAG + "=" + linkAction),
        linkExpl);
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

  private void processUpdateProxy() throws IOException {
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
        processUpdateProxy_SaveChanges();
      }
      catch (IOException ioe) {
        logger.error("Could not save changes", ioe);
        errMsg = "Error: Could not save changes.\n" + ioe.toString();
      }
    }

    displayProxy();
  }

  private void processUpdateProxy_SaveChanges() throws IOException {
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

  public static final String PARAM_AUDIT_ENABLE =
    AuditProxyManager.PARAM_START;

  public static final String PARAM_AUDIT_PORT =
    AuditProxyManager.PARAM_PORT;

  static final boolean DEFAULT_AUDIT_ENABLE =
    AuditProxyManager.DEFAULT_START;

  private static final String ACTION_CONTENT = "Content";

  private static final String ACTION_MAIN = "Main";

  private static final String ACTION_PROXY = "Proxy";

  private static final String ACTION_UPDATE_PROXY = "UpdateProxy";

  private static final String AUDIT_ENABLE_NAME = "audit_ena";

  private static final String AUDIT_FOOT =
    "The audit proxy serves <b>only</b> cached content, and never forwards requests to the publisher or any other site. By configuring a browser to proxy to this port, you can view the content stored on the cache.  All requests for content not on the cache will return a \"404 Not Found\" error.";

  private static final String AUDIT_PORT_NAME = "audit_port";

  private static final String BAD_ACTION = "foo";

  private static final String COMMENT_PROXY_IP_ACCESS =
    "Proxy Options";

  private static final String CONTENT_EXPLANATION =
    "Define access groups and manage access control rules for the content preserved on this cache.";

  private static final String FILTER_FOOT =
    "Other ports can be configured but may not be reachable due to packet filters.";

  private static final String ICP_ENABLE_NAME =
    "icp_ena";

  /**
   * <p>A footnote explaining the role of the ICP server.</p>
   */
  private static final String ICP_FOOT =
    "The ICP server responds to queries sent by other proxies and caches to let them know if this cache has the content they are looking for. This is useful if you are integrating this cache into an existing network structure with other proxies and caches that support ICP.";

  private static final String ICP_PORT_NAME =
    "icp_port";

  private static Logger logger = Logger.getLogger("ProxyAndContent");

  private static final String MAIN_EXPLANATION =
    "Configure proxy options, such as the audit proxy and the ICP server. Manage access groups and configure access rules for the content preserved on this cache.";

  private static final String PROXY_EXPLANATION =
    "Manage this cache's audit proxy and ICP server.";

}
