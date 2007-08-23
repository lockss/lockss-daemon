/*
 * $Id: ProxyAndContent.java,v 1.23 2007-08-23 06:33:27 tlipkis Exp $
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

import org.apache.commons.collections.iterators.ObjectArrayIterator;
import org.lockss.app.LockssApp;
import org.lockss.config.*;
import org.lockss.daemon.ResourceManager;
import org.lockss.proxy.AuditProxyManager;
import org.lockss.proxy.icp.IcpManager;
import org.lockss.servlet.ServletUtil.LinkWithExplanation;
import org.lockss.crawler.*;
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

  private boolean formCrawlProxyEnable;
  private String formCrawlProxyHost;
  private String formCrawlProxyPort;

  private ResourceManager resourceMgr;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    configMgr = getLockssApp().getConfigManager();
    resourceMgr = getLockssApp().getResourceManager();
  }

  protected void lockssHandleRequest() throws ServletException, IOException {
    action = req.getParameter(ACTION_TAG);
    isForm = !StringUtil.isNullString(req.getParameter("isForm"));

    errMsg = null;
    statusMsg = null;

    if (StringUtil.isNullString(action)) displayMenu_Main();
    else if (action.equals(ACTION_MAIN)) displayMenu_Main();
    else if (action.equals(ACTION_PROXY_SERVER)) displayProxyServer();
    else if (action.equals(ACTION_PROXY_CLIENT)) displayProxyClient();
// FIXME: disabled until after the 1.15 branch
//    else if (action.equals(ACTION_CONTENT)) displayMenu_Content();
    else if (action.equals(ACTION_UPDATE_PROXY_SERVER)) processUpdateProxyServer();
    else if (action.equals(ACTION_UPDATE_PROXY_CLIENT)) processUpdateProxyClient();
    else if (action.equals(BAD_ACTION)) {
      // FIXME: This error condition is artificially singled out for testing
      errMsg = "Unimplemented (" + action + ")";
      displayMenu_Main();
    }
    else {
      errMsg = "Unknown action (" + action + ")";
      displayMenu_Main();
    }
  }

  private void displayMenu(String explanation,
                           Iterator descriptorIter,
                           String backLink) throws IOException {
    // Start page
    Page page = newPage();
    ServletUtil.layoutExplanationBlock(page, explanation);
    layoutErrorBlock(page);

    // Menu
    ServletUtil.layoutMenu(page, descriptorIter);

    // Finish up
    if (!StringUtil.isNullString(backLink)) {
      ServletUtil.layoutBackLink(page, backLink);
    }
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  private void displayMenu_Content() throws IOException {
    displayMenu(CONTENT_EXPLANATION, getDescriptors_Content(),
        srvLink(myServletDescr(), "Back to Proxy Options and Content Access Control"));
  }

  private void displayMenu_Main() throws IOException {
    displayMenu(MAIN_EXPLANATION, getDescriptors_Main(), null);
  }

  private void displayProxyServer() throws IOException {
    // Start page
    Page page = newPage();
    addJavaScript(page);
    ServletUtil.layoutExplanationBlock(page, PROXY_SERVER_EXPLANATION);
    layoutErrorBlock(page);

    // Start form
    Form frm = ServletUtil.newForm(srvURL(myServletDescr()));
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
    if (getLockssDaemon().getIcpManager().isIcpServerAllowed()) {
      ServletUtil.layoutEnablePortRow(this,
                                      tbl,
                                      ICP_ENABLE_NAME,
                                      getDefaultIcpEnable(),
                                      "ICP server",
                                      ICP_FOOT,
                                      FILTER_FOOT,
                                      ICP_PORT_NAME,
                                      getDefaultIcpPort(),
                                      resourceMgr.getUsableUdpPorts(IcpManager.SERVER_NAME));
    }
    else {
      final String ICP_DISABLED_FOOT =
        "To enable ICP you must perform a platform reconfiguration reboot.";
      tbl.newRow();
      tbl.newCell();
      tbl.add("The platform is configured to disable the ICP server");
      tbl.add(addFootnote(ICP_DISABLED_FOOT));
      tbl.add(".");
    }

    // Put parts together
    frm.add(tbl);
    frm.add(new Input(Input.Hidden, "isForm", "true"));

    ServletUtil.layoutSubmitButton(this, frm, ACTION_TAG,
				   ACTION_UPDATE_PROXY_SERVER);
    page.add(frm);

    // Finish up
    ServletUtil.layoutBackLink(page,
        srvLink(myServletDescr(), "Back to Proxy Options and Content Access Control"));
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  private boolean getDefaultAuditEnable() {
    if (isForm) {
      return formAuditEnable;
    }
    return CurrentConfig.getBooleanParam(PARAM_AUDIT_ENABLE,
                                         DEFAULT_AUDIT_ENABLE);
  }

  private String getDefaultAuditPort() {
    String port = null;
    if (isForm) {
      port = formAuditPort;
    }
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
    return isForm
      ? formIcpEnable
      : getLockssDaemon().getIcpManager().isIcpServerRunning();
  }

  private String getDefaultIcpPort() {
    String port = null;
    if (isForm) {
      port = formIcpPort;
    }
    if (StringUtil.isNullString(port)) {
      int portNumber = getLockssDaemon().getIcpManager().getCurrentPort();
      port = portNumber > 0 ? Integer.toString(portNumber) : "";
    }
    return port;
  }

  private void displayProxyClient() throws IOException {
    // Start page
    Page page = newPage();
    addJavaScript(page);
    ServletUtil.layoutExplanationBlock(page, PROXY_CLIENT_EXPLANATION);
    layoutErrorBlock(page);

    // Start form
    Form frm = ServletUtil.newForm(srvURL(myServletDescr()));
    Table tbl = new Table(0, "align=\"center\" cellpadding=\"10\"");


    tbl.newRow();
    tbl.newCell("align=\"center\"");

    Input hostElem = new Input(Input.Text, CRAWL_PROXY_HOST_NAME,
			       getDefaultCrawlProxyHost());
    Input portElem = new Input(Input.Text, CRAWL_PROXY_PORT_NAME,
			       getDefaultCrawlProxyPort());

    // "enable" element
    Input enaElem = new Input(Input.Checkbox, CRAWL_PROXY_ENABLE_NAME, "1");
    if (getDefaultCrawlProxyEnable()) {
      enaElem.check();
    } else {
      hostElem.attribute("disabled", "true");
      portElem.attribute("disabled", "true");
    }
    enaElem.attribute("onchange",
		      "selectEnable(this,'host_entry', 'port_entry')");
    setTabOrder(enaElem);
    tbl.add(enaElem);
    tbl.add("Proxy crawls");

    tbl.newRow();
    tbl.newCell("align=\"center\"");

    tbl.add("HTTP Proxy:&nbsp;");
    hostElem.setSize(40);
    hostElem.attribute("id", "host_entry");
    setTabOrder(hostElem);
    tbl.add(hostElem);
    tbl.add(" Port:&nbsp;");

    portElem.setSize(6);
    portElem.attribute("id", "port_entry");
    setTabOrder(portElem);
    tbl.add(portElem);

    frm.add(tbl);
    frm.add(new Input(Input.Hidden, "isForm", "true"));
    ServletUtil.layoutSubmitButton(this, frm, ACTION_TAG,
				   ACTION_UPDATE_PROXY_CLIENT);
    page.add(frm);

    // Finish up
    ServletUtil.layoutBackLink(page,
        srvLink(myServletDescr(), "Back to Proxy Options and Content Access Control"));
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  private boolean getDefaultCrawlProxyEnable() {
    return isForm
      ? formCrawlProxyEnable
      : CurrentConfig.getBooleanParam(BaseCrawler.PARAM_PROXY_ENABLED,
				      BaseCrawler.DEFAULT_PROXY_ENABLED);
  }

  private String getDefaultCrawlProxyHost() {
    return isForm
      ? formCrawlProxyHost
      : CurrentConfig.getParam(BaseCrawler.PARAM_PROXY_HOST);
  }

  private String getDefaultCrawlProxyPort() {
    return isForm
      ? formCrawlProxyPort
      : CurrentConfig.getParam(BaseCrawler.PARAM_PROXY_PORT);
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
        makeDescriptor("Proxy Server Options",
                       ACTION_PROXY_SERVER,
		       "Configure the audit proxy and the ICP server."),

        makeDescriptor("Proxy Client Options",
                       ACTION_PROXY_CLIENT,
                       "Configure the crawler to access sites through a proxy."),
// FIXME: disabled until after the 1.15 branch
//        makeDescriptor("Content Access Control",
//                       ACTION_CONTENT,
//                       "Manage access groups and access control rules."),
    });
  }

  private boolean isLegalAuditPort(int port) {
    return port >= 1024 &&
        resourceMgr.isTcpPortAvailable(port, AuditProxyManager.SERVER_NAME);
  }

  private boolean isLegalIcpPort(int port) {
    return port >= 1024 &&
        resourceMgr.isUdpPortAvailable(port, IcpManager.SERVER_NAME);
  }

  private LinkWithExplanation makeDescriptor(String linkText,
                                             String linkAction,
                                             String linkExpl) {
    return new LinkWithExplanation(
        srvLink(myServletDescr(), linkText, ACTION_TAG + "=" + linkAction),
        linkExpl);
  }

  private void processUpdateProxyServer() throws IOException {
    ArrayList errList = new ArrayList();

    // Read form
    processUpdateProxyServer_ReadForm();

    // Process form
    processUpdateProxyServer_Audit(errList);
    processUpdateProxyServer_Icp(errList);

    // Prepare error message
    if (errList.size() > 0) {
      // There were errors
      StringBuilder buffer = new StringBuilder();
      StringUtil.separatedString(errList, "<br>", buffer);
      errMsg = buffer.toString();
    }
    else {
      // There were no errors
      try {
        processUpdateProxyServer_SaveChanges();
	statusMsg = "Update successful";
      }
      catch (IOException ioe) {
        logger.error("Could not save changes", ioe);
        errMsg = "Error: Could not save changes.\n" + ioe.toString();
      }
    }

    displayProxyServer();
  }

  private void processUpdateProxyServer_Audit(ArrayList errList) {
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

  private void processUpdateProxyServer_Icp(ArrayList errList) {
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

  private void processUpdateProxyServer_ReadForm() {
    formAuditEnable = !StringUtil.isNullString(req.getParameter(AUDIT_ENABLE_NAME));
    formAuditPort = req.getParameter(AUDIT_PORT_NAME);
    formIcpEnable = !StringUtil.isNullString(req.getParameter(ICP_ENABLE_NAME));
    formIcpPort = req.getParameter(ICP_PORT_NAME);
  }

  /* package */ static void saveAuditAndIcp(ConfigManager configMgr,
                                            boolean auditEnable,
                                            String auditPort,
                                            boolean icpEnable,
                                            String icpPort)
        throws IOException {
    final String TRUE = "true";
    final String FALSE = "false";
    Configuration config;

    // Save audit proxy config
    config = ConfigManager.newConfiguration();
    config.put(PARAM_AUDIT_ENABLE, auditEnable ? TRUE : FALSE);
    config.put(PARAM_AUDIT_PORT, auditPort);
    configMgr.modifyCacheConfigFile(config,
                                    ConfigManager.CONFIG_FILE_AUDIT_PROXY,
                                    CONFIG_FILE_COMMENT);

    // Save ICP server config
    config = ConfigManager.newConfiguration();
    config.put(IcpManager.PARAM_ICP_ENABLED, icpEnable ? TRUE : FALSE);
    config.put(IcpManager.PARAM_ICP_PORT, icpPort);
    configMgr.modifyCacheConfigFile(config,
                                    ConfigManager.CONFIG_FILE_ICP_SERVER,
                                    CONFIG_FILE_COMMENT);
  }

  private void processUpdateProxyServer_SaveChanges() throws IOException {
    // temporary measure
    String auditp =
      formAuditEnable ? Integer.toString(auditPort) :
      CurrentConfig.getParam(PARAM_AUDIT_PORT);

    String icpp =
      formIcpEnable ? Integer.toString(icpPort) :
      CurrentConfig.getParam(IcpManager.PARAM_ICP_PORT);

    saveAuditAndIcp(configMgr, formAuditEnable, auditp, formIcpEnable, icpp);
  }

  private void processUpdateProxyClient() throws IOException {
    ArrayList errList = new ArrayList();

    formCrawlProxyEnable =
      !StringUtil.isNullString(req.getParameter(CRAWL_PROXY_ENABLE_NAME));
    formCrawlProxyPort = req.getParameter(CRAWL_PROXY_PORT_NAME);
    formCrawlProxyHost = req.getParameter(CRAWL_PROXY_HOST_NAME);

    if (formCrawlProxyEnable && StringUtil.isNullString(formCrawlProxyHost)) {
      errList.add("Proxy host must be filled in");
    }      

    int proxyPort = 0;
    try {
      proxyPort = Integer.parseInt(formCrawlProxyPort);
    } catch (NumberFormatException nfe) {
      if (formCrawlProxyEnable) {
        // bad number is an error only if enabling
        errList.add("Proxy port must be a number: " + formCrawlProxyPort);
      }
    }

    // Prepare error message
    if (errList.size() > 0) {
      // There were errors
      StringBuilder buffer = new StringBuilder();
      StringUtil.separatedString(errList, "<br>", buffer);
      errMsg = buffer.toString();
    }
    else {
      // There were no errors
      try {
	if (formCrawlProxyEnable) {
	  processUpdateProxyClient_SaveChanges(configMgr,
					       formCrawlProxyEnable,
					       formCrawlProxyHost,
					       formCrawlProxyPort);
	} else {
	  String host = CurrentConfig.getParam(BaseCrawler.PARAM_PROXY_HOST);
	  String port = CurrentConfig.getParam(BaseCrawler.PARAM_PROXY_PORT);
	  processUpdateProxyClient_SaveChanges(configMgr,
					       formCrawlProxyEnable,
					       host, port);
	}
	statusMsg = "Update successful";
      }
      catch (IOException ioe) {
        logger.error("Could not save changes", ioe);
        errMsg = "Error: Could not save changes.\n" + ioe.toString();
      }
    }

    displayProxyClient();
  }

  private void
    processUpdateProxyClient_SaveChanges(ConfigManager configMgr,
					 boolean crawlProxyEnable,
					 String crawlProxyHost,
					 String crawlProxyPort)
      throws IOException {

    Configuration config = ConfigManager.newConfiguration();
    config.put(BaseCrawler.PARAM_PROXY_ENABLED,
	       Boolean.toString(crawlProxyEnable));
    if (!StringUtil.isNullString(crawlProxyHost)) {
      config.put(BaseCrawler.PARAM_PROXY_HOST, crawlProxyHost);
    }
    config.put(BaseCrawler.PARAM_PROXY_PORT, crawlProxyPort);
    configMgr.modifyCacheConfigFile(config,
                                    ConfigManager.CONFIG_FILE_CRAWL_PROXY,
                                    "Crawl proxy options");
  }

  public static final String PARAM_AUDIT_ENABLE =
    AuditProxyManager.PARAM_START;

  public static final String PARAM_AUDIT_PORT =
    AuditProxyManager.PARAM_PORT;

  static final boolean DEFAULT_AUDIT_ENABLE =
    AuditProxyManager.DEFAULT_START;

  private static final String ACTION_CONTENT = "Content";

  private static final String ACTION_MAIN = "Main";

  private static final String ACTION_PROXY_SERVER = "ProxyServer";

  private static final String ACTION_PROXY_CLIENT = "ProxyClient";

  private static final String ACTION_UPDATE_PROXY_SERVER = "Update ProxyServer";
  private static final String ACTION_UPDATE_PROXY_CLIENT = "Update ProxyClient";

  private static final String AUDIT_ENABLE_NAME = "audit_ena";

  private static final String AUDIT_FOOT =
    "The audit proxy serves <b>only</b> cached content, and never forwards requests to the publisher or any other site. By configuring a browser to proxy to this port, you can view the content stored on the cache.  All requests for content not on the cache will return a \"404 Not Found\" error.";

  private static final String AUDIT_PORT_NAME = "audit_port";

  private static final String BAD_ACTION = "Unknown_Action";

  private static final String CONFIG_FILE_COMMENT = "Proxy Options and Content Access Control";

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

  private static final String CRAWL_PROXY_ENABLE_NAME = "proxy_client_ena";
  private static final String CRAWL_PROXY_HOST_NAME = "proxy_client_host";
  private static final String CRAWL_PROXY_PORT_NAME = "proxy_client_port";

  private static Logger logger = Logger.getLogger("ProxyAndContent");

  private static final String MAIN_EXPLANATION =
// FIXME: change back after 1.15 branch
//    "Configure proxy options, such as the audit proxy and the ICP server. Manage access groups and configure access rules for the content preserved on this cache.";
//     "Configure proxy options, such as the audit proxy and the ICP server.";
    "Configure proxies running on this machine, or the use of other proxies.";

  private static final String PROXY_SERVER_EXPLANATION =
    "Manage this cache's audit proxy and ICP server.";

  private static final String PROXY_CLIENT_EXPLANATION =
    "Configure the LCOKSS crawler to access the net through a proxy server.";

}
