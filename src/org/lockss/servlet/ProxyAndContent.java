/*
 * $Id: ProxyAndContent.java,v 1.26 2008-06-30 08:43:59 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
import javax.servlet.http.*;
import org.mortbay.html.*;

import org.apache.commons.collections.iterators.ObjectArrayIterator;
import org.apache.commons.lang.StringUtils;
import org.lockss.app.LockssApp;
import org.lockss.config.*;
import org.lockss.daemon.ResourceManager;
import org.lockss.proxy.*;
import org.lockss.proxy.icp.IcpManager;
import org.lockss.servlet.ServletUtil.LinkWithExplanation;
import org.lockss.crawler.*;
import org.lockss.util.*;

/*
 * UI to configure content and audit proxies, and content server
 */
public class ProxyAndContent extends LockssServlet {
  private static Logger log = Logger.getLogger("ProxyAndContent");

  private static final String SUFFIX_KEY_ENABLE = "_ena";
  private static final String SUFFIX_KEY_PORT = "_port";
  private static final String SUFFIX_KEY_HOST = "_host";

  private static final String ACTION_MAIN = "Main";
  private static final String ACTION_CONTENT = "Content";
  private static final String ACTION_CONTENT_SERVER = "ContentServer";
  private static final String ACTION_PROXY_CLIENT = "ProxyClient";
  private static final String ACTION_UPDATE_CONTENT_SERVER =
    "Update Content Servers";
  private static final String ACTION_UPDATE_PROXY_CLIENT =
    "Update Proxy Client";

  private static final String BAD_ACTION = "Unknown_Action";

  private static final String CONFIG_FILE_COMMENT = "Content Access Options";

  private static final String CONTENT_EXPLANATION =
    "Define access groups and manage access control rules for the content preserved on this box.";

  private static final String CONTENT_PROXY_FOOT =
    "The content proxy attempts to satisfy all requests by forwarding them to the publisher (origin server).  If content cannot be obtained from the publisher and that content is preserved on the LOCKSS box, the preserved content will be transparently served to the user.";

  private static final String SERVE_CONTENT_FOOT =
    "The content server provides a direct browsable interface to the preserved content.";

  private static final String AUDIT_PROXY_FOOT =
    "The audit proxy serves <b>only</b> preserved content, and never forwards requests to the publisher or any other site. By configuring a browser to proxy to this port, you can view the content stored on the LOCKSS box.  All requests for content not on the box will return a \"404 Not Found\" error.";

  private static final String ICP_FOOT =
    "The ICP server responds to queries sent by other proxies and caches to let them know if this LOCKSS box has the content they are looking for. This is useful if you are integrating this box into an existing network structure with other proxies and caches that support ICP.";

  private static final String FILTER_FOOT =
    "Other ports can be configured but may not be reachable due to packet filters.";

  private static final String CRAWL_PROXY_ENABLE_NAME = "proxy_client_ena";
  private static final String CRAWL_PROXY_HOST_NAME = "proxy_client_host";
  private static final String CRAWL_PROXY_PORT_NAME = "proxy_client_port";

  private static final String MAIN_EXPLANATION =
// FIXME: change back after 1.15 branch
//    "Configure proxy options, such as the audit proxy and the ICP server. Manage access groups and configure access rules for the content preserved on this cache.";
    "Configure how content is collected from publishers and served to users.";

  private static final String CONTENT_SERVER_EXPLANATION =
    "Manage this box's content servers and proxies.";

  private static final String PROXY_CLIENT_EXPLANATION =
    "Configure the LOCKSS crawler to access the net through a proxy server.";

  private static final String BACK_LINK_TXT = "Back to Content Access Options";

  ServerInfo contentServerInfo =
    new ServerInfo("Content server",
		   "content_server_",
		   ContentServletManager.PARAM_START,
		   ContentServletManager.PARAM_PORT,
		   ContentServletManager.DEFAULT_START) {
      List getUsablePorts() {
	return
	  resourceMgr.getUsableTcpPorts(ContentServletManager.SERVER_NAME);
      }
      boolean isLegalPort(int port) {
	return
	  resourceMgr.isTcpPortAvailable(port,
					 ContentServletManager.SERVER_NAME);
      }
    };

  ServerInfo contentProxyInfo =
    new ServerInfo("Content proxy",
		   "content_proxy_",
		   ProxyManager.PARAM_START,
		   ProxyManager.PARAM_PORT,
		   ProxyManager.DEFAULT_START) {
      List getUsablePorts() {
	return resourceMgr.getUsableTcpPorts(ProxyManager.SERVER_NAME);
      }
      boolean isLegalPort(int port) {
	return resourceMgr.isTcpPortAvailable(port, ProxyManager.SERVER_NAME);
      }
    };

  ServerInfo auditProxyInfo =
    new ServerInfo("Audit proxy",
		   "audit_proxy_",
		   AuditProxyManager.PARAM_START,
		   AuditProxyManager.PARAM_PORT,
		   AuditProxyManager.DEFAULT_START) {
      List getUsablePorts() {
	return resourceMgr.getUsableTcpPorts(AuditProxyManager.SERVER_NAME);
      }
      boolean isLegalPort(int port) {
	return resourceMgr.isTcpPortAvailable(port,
					      AuditProxyManager.SERVER_NAME);
      }
    };

  ServerInfo icpServerInfo =
    new ServerInfo("ICP server",
		   "icp_",
		   IcpManager.PARAM_ICP_ENABLED,
		   IcpManager.PARAM_ICP_PORT,
		   false) {
      List getUsablePorts() {
	return resourceMgr.getUsableUdpPorts(IcpManager.SERVER_NAME);
      }
      boolean isLegalPort(int port) {
	return resourceMgr.isUdpPortAvailable(port, IcpManager.SERVER_NAME);
      }
    };

  ServerInfo[] serverInfos = {
    contentServerInfo,
    contentProxyInfo,
    auditProxyInfo,
    icpServerInfo};

  abstract class ServerInfo {
    // constants
    String name;
    String formKeyPrefix;
    String enableParam;
    String portParam;
    String hostParam;
    boolean enableParamDefault;

    // state
    boolean enable;
    boolean formEnable;
    int port;
    String formPort;
    String host;
    String formHost;

    ServerInfo(String name,
	       String formKeyPrefix,
	       String enableParam,
	       String portParam,
	       boolean enableParamDefault) {
      this(name, formKeyPrefix, enableParam, portParam,
	   null, enableParamDefault);
    }

    ServerInfo(String name,
	       String formKeyPrefix,
	       String enableParam,
	       String portParam,
	       String hostParam,
	       boolean enableParamDefault) {
      this.name = name;
      this.formKeyPrefix = formKeyPrefix;
      this.enableParam = enableParam;
      this.portParam = portParam;
      this.hostParam = hostParam;
      this.enableParamDefault = enableParamDefault;
    }

    String getName() { return name; }
    String getEnableKey() { return formKeyPrefix + SUFFIX_KEY_ENABLE; }
    String getPortKey() { return formKeyPrefix + SUFFIX_KEY_PORT; }
    String getHostKey() { return formKeyPrefix + SUFFIX_KEY_HOST; }
    String getEnableParam() { return enableParam; }
    String getPortParam() { return portParam; }
    String getHostParam() { return hostParam; }
    boolean getEnableParamDefault() { return enableParamDefault; }

    String getDefaultHost() { return null; }

    boolean getDefaultEnable() {
      if (isForm) {
	return formEnable;
      }
      return CurrentConfig.getBooleanParam(getEnableParam(),
					   getEnableParamDefault());
    }

    String getDefaultPort() {
      String port = null;
      if (isForm) {
	port = formPort;
      }
      if (StringUtil.isNullString(port)) {
	port = CurrentConfig.getParam(getPortParam());
      }
      if (!StringUtil.isNullString(port)) {
	try {
	  int portNumber = Integer.parseInt(port);
	  if (portNumber <= 0) {
	    port = "";
	  }
	} catch (NumberFormatException nfeIgnore) {}
      }
      return port;
    }

    void readForm(HttpServletRequest req) {
      formEnable =
	!StringUtil.isNullString(req.getParameter(getEnableKey()));
      formPort = req.getParameter(getPortKey());
    }

    void processForm(ArrayList errList) {
      enable = formEnable;
      port = -1;
      try {
	port = Integer.parseInt(formPort);
      } catch (NumberFormatException nfe) {
	if (formEnable) {
        // bad number is an error only if enabling
	  errList.add(getName() + " port must be a number: " + formPort);
	}
      }
      if (formEnable && !isLegalPort(port)) {
	errList.add("Illegal " + getName() + " port number: " + formPort
          + ", must be >=1024 and not in use");
      }
    }

    void updateConfig(Configuration config) {
      config.put(getEnableParam(), Boolean.toString(enable));
      String p = (enable ? Integer.toString(port)
		  : CurrentConfig.getParam(getPortParam()));
      if (StringUtils.isNotEmpty(p)) {
	config.put(getPortParam(), p);
      }
    }

    abstract List getUsablePorts();
    abstract boolean isLegalPort(int port);
  }

  private ConfigManager configMgr;
  private ResourceManager resourceMgr;

  protected boolean isForm;

  private String action;

  private int auditPort;


  private boolean formAuditEnable;

  private String formAuditPort;

  private boolean formIcpEnable;

  private String formIcpPort;

  private int icpPort;

  private boolean formCrawlProxyEnable;
  private String formCrawlProxyHost;
  private String formCrawlProxyPort;


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
    else if (action.equals(ACTION_CONTENT_SERVER)) displayContentServer();
    else if (action.equals(ACTION_PROXY_CLIENT)) displayProxyClient();
// FIXME: disabled until after the 1.15 branch
//    else if (action.equals(ACTION_CONTENT)) displayMenu_Content();
    else if (action.equals(ACTION_UPDATE_CONTENT_SERVER)) processUpdateContentServer();
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
		srvLink(myServletDescr(), BACK_LINK_TXT));
  }

  private void displayMenu_Main() throws IOException {
    displayMenu(MAIN_EXPLANATION, getDescriptors_Main(), null);
  }

  void layoutEnablePortRow(Table tbl,
			   ServerInfo si,
			   String enableDescription,
			   String enableFootnote) {
    ServletUtil.layoutEnablePortRow(this,
                                    tbl,
                                    si.getEnableKey(),
                                    si.getDefaultEnable(),
                                    enableDescription,
                                    enableFootnote,
                                    FILTER_FOOT,
				    si.getPortKey(),
				    si.getDefaultPort(),
				    si.getUsablePorts());
  }

  private void displayContentServer() throws IOException {
    // Start page
    Page page = newPage();
    addJavaScript(page);
    ServletUtil.layoutExplanationBlock(page, CONTENT_SERVER_EXPLANATION);
    layoutErrorBlock(page);

    // Start form
    Form frm = ServletUtil.newForm(srvURL(myServletDescr()));
    Table tbl = ServletUtil.newEnablePortTable();

    // ServeContent
    layoutEnablePortRow(tbl, contentServerInfo, "content server",
			SERVE_CONTENT_FOOT);
    // Content proxy
    layoutEnablePortRow(tbl, contentProxyInfo, "content proxy",
			CONTENT_PROXY_FOOT);
    // Audit proxy
    layoutEnablePortRow(tbl, auditProxyInfo, "audit proxy", AUDIT_PROXY_FOOT);
    // ICP server
    if (getLockssDaemon().getIcpManager().isIcpServerAllowed()) {
      layoutEnablePortRow(tbl, icpServerInfo, "ICP server", ICP_FOOT);
    } else {
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
				   ACTION_UPDATE_CONTENT_SERVER);
    page.add(frm);

    // Finish up
    ServletUtil.layoutBackLink(page, srvLink(myServletDescr(), BACK_LINK_TXT));
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
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
    ServletUtil.layoutBackLink(page, srvLink(myServletDescr(), BACK_LINK_TXT));
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
        makeDescriptor("Content Server Options",
                       ACTION_CONTENT_SERVER,
		       "Configure content servers and proxies and ICP."),

        makeDescriptor("Proxy Client Options",
                       ACTION_PROXY_CLIENT,
                       "Configure the crawler to access sites through a proxy."),
// FIXME: disabled until after the 1.15 branch
//        makeDescriptor("Content Access Control",
//                       ACTION_CONTENT,
//                       "Manage access groups and access control rules."),
    });
  }

  private LinkWithExplanation makeDescriptor(String linkText,
                                             String linkAction,
                                             String linkExpl) {
    return new LinkWithExplanation(
        srvLink(myServletDescr(), linkText, ACTION_TAG + "=" + linkAction),
        linkExpl);
  }

  private void processUpdateContentServer() throws IOException {
    ArrayList errList = new ArrayList();

    // Read form
    for (ServerInfo si : serverInfos) {
      si.readForm(req);
    }

    // Process form values
    for (ServerInfo si : serverInfos) {
      si.processForm(errList);
    }

    // Prepare error message
    if (errList.size() > 0) {
      // There were errors
      StringBuilder buffer = new StringBuilder();
      StringUtil.separatedString(errList, "<br>", buffer);
      errMsg = buffer.toString();
    } else {
      // There were no errors
      try {
        processUpdateContentServer_SaveChanges();
	statusMsg = "Update successful";
      }
      catch (IOException ioe) {
        log.error("Could not save changes", ioe);
        errMsg = "Error: Could not save changes.\n" + ioe.toString();
      }
    }

    displayContentServer();
  }

  private void processUpdateContentServer_SaveChanges() throws IOException {
    Configuration config;

    config = ConfigManager.newConfiguration();

    for (ServerInfo si : serverInfos) {
      si.updateConfig(config);
    }

    configMgr.modifyCacheConfigFile(config,
                                    ConfigManager.CONFIG_FILE_CONTENT_SERVERS,
                                    CONFIG_FILE_COMMENT);

    // If successfully wrote combined server config file, delete old
    // individual files
    try {
      configMgr.deleteCacheConfigFile(ConfigManager.CONFIG_FILE_ICP_SERVER);
      configMgr.deleteCacheConfigFile(ConfigManager.CONFIG_FILE_AUDIT_PROXY);
    } catch (Exception e) {
      log.warning("Error deleting obsolete config file", e);
    }
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
    } else {
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
        log.error("Could not save changes", ioe);
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

}
