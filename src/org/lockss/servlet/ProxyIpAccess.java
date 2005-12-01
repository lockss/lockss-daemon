/*
 * $Id: ProxyIpAccess.java,v 1.18 2005-12-01 23:28:01 troberts Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.Properties;

import javax.servlet.*;

import org.mortbay.html.*;

import org.lockss.config.*;
import org.lockss.daemon.ResourceManager;
import org.lockss.proxy.*;
import org.lockss.proxy.icp.IcpManager;
import org.lockss.util.StringUtil;

/** Display and update proxy IP access control lists.
 */
public class ProxyIpAccess extends IpAccessControl {
  public static final String PARAM_AUDIT_ENABLE =
    AuditProxyManager.PARAM_START;
  static final boolean DEFAULT_AUDIT_ENABLE = AuditProxyManager.DEFAULT_START;
  public static final String PARAM_AUDIT_PORT = AuditProxyManager.PARAM_PORT;

  private static final String exp =
    "Enter the list of IP addresses that should be allowed to use this " +
    "cache as a proxy server, and access the content stored on it.  " +
    commonExp;

  private ResourceManager resourceMgr;
  private boolean formAuditEnable;
  private String formAuditPort;
  private boolean formIcpEnable;
  private String formIcpPort;
  private int auditPort;
  private int icpPort;

  protected void resetLocals() {
    super.resetLocals();
    formAuditEnable = false;
    formAuditPort = null;
    formIcpEnable = false;
    formIcpPort = null;
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    resourceMgr = getLockssApp().getResourceManager();
  }

  protected String getExplanation() {
    return exp;
  }

  protected String getParamPrefix() {
    return ProxyManager.IP_ACCESS_PREFIX;
  }

  protected String getConfigFileName() {
    return ConfigManager.CONFIG_FILE_PROXY_IP_ACCESS;
  }

  protected String getConfigFileComment() {
    return "Proxy IP Access Control";
  }

  protected void readForm() {
    formAuditEnable = !StringUtil.isNullString(req.getParameter(AUDIT_ENABLE_NAME));
    formAuditPort = req.getParameter(AUDIT_PORT_NAME);
    formIcpEnable = !StringUtil.isNullString(req.getParameter(ICP_ENABLE_NAME));
    formIcpPort = req.getParameter(ICP_PORT_NAME);
    super.readForm();
  }

  protected void doUpdate() throws IOException {
    auditPort = -1;
    try {
      auditPort = Integer.parseInt(formAuditPort);
    } catch (NumberFormatException nfe) {
      if (formAuditEnable) {
        // bad number is an error only if enabling
        errMsg = "Audit proxy port must be a number: " + formAuditPort;
        displayPage();
        return;
      }
    }
    if (formAuditEnable && !isLegalAuditPort(auditPort)) {
      errMsg = "Illegal audit proxy port number: " + formAuditPort +
      ", must be >=1024 and not in use";
      displayPage();
      return;
    }

    icpPort = -1;
    try {
      icpPort = Integer.parseInt(formIcpPort);
    }
    catch (NumberFormatException nfe) {
      if (formIcpEnable) {
        // bad number is an error only if enabling
        errMsg = "ICP port must be a number: " + formIcpPort;
        displayPage();
        return;
      }
    }
    if (formIcpEnable && !isLegalIcpPort(icpPort)) {
      errMsg = "Illegal ICP port number: " + formIcpPort +
      ", must be >=1024 and not in use";
      displayPage();
      return;
    }

    super.doUpdate();
  }


  private boolean isLegalAuditPort(int port) {
    return port >= 1024 &&
           resourceMgr.isTcpPortAvailable(
               port, AuditProxyManager.SERVER_NAME);
  }

  private boolean isLegalIcpPort(int port) {
    return port >= 1024 &&
           resourceMgr.isUdpPortAvailable(
               port, IcpManager.class);
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
    return getLockssDaemon().getIcpManager().isIcpServerRunning();
  }

  private String getDefaultIcpPort() {
    return Integer.toString(getLockssDaemon().getIcpManager().getCurrentPort());
  }

  private static final String AUDIT_FOOT =
    "The audit proxy serves <b>only</b> cached content, and never " +
    "forwards requests to the publisher or any other site.  " +
    "By configuring a browser to proxy to this port, you can view the " +
    "content stored on the cache.  All requests for content not on the " +
    "cache will return a \"404 Not Found\" error.";

  private static final String FILTER_FOOT =
    "Other ports can be configured but may not be reachable due to " +
    "packet filters.";

  /**
   * <p>A footnote explaining the role of the ICP server.</p>
   */
  private static final String ICP_FOOT =
    "The ICP server responds to queries sent by other proxies and caches " +
    "to let them know if this cache has the content they are looking for. " +
    "This is useful if you are integrating this cache into an existing " +
    "network structure with other proxies and caches that support ICP.";

  protected void additionalFormLayout(Composite composite) {
    final int BORDER = 0;
    final String ATTRIBUTES = "align=\"center\" cellpadding=\"10\"";

    Table tbl = new Table(BORDER, ATTRIBUTES);
    layoutEnablePortRow(tbl, AUDIT_ENABLE_NAME, getDefaultAuditEnable(), "audit proxy",
        AUDIT_FOOT, FILTER_FOOT, AUDIT_PORT_NAME, getDefaultAuditPort(),
        resourceMgr.getUsableTcpPorts(AuditProxyManager.SERVER_NAME));
    if (CurrentConfig.getBooleanParam(IcpManager.PARAM_PLATFORM_ICP_ENABLED,
                                      true)) {
      // unset: behave like true
      layoutEnablePortRow(tbl, ICP_ENABLE_NAME, getDefaultIcpEnable(), "ICP server",
          ICP_FOOT, FILTER_FOOT, ICP_PORT_NAME, getDefaultIcpPort(),
          resourceMgr.getUsableUdpPorts(AuditProxyManager.SERVER_NAME));
    }
    else {
      final String ICP_DISABLED_FOOT =
        "To enable ICP you must perform a platform reconfiguration reboot.";
      tbl.newRow(); tbl.newCell();
      tbl.add("The platform is configured to disable the ICP server");
      tbl.add(addFootnote(ICP_DISABLED_FOOT));
      tbl.add(".");
    }
    composite.add(tbl);
  }

  protected void addConfigProps(Properties props) {
    super.addConfigProps(props);

    final String TRUE = "true";
    final String FALSE = "false";

    props.setProperty(PARAM_AUDIT_ENABLE,  formAuditEnable ? TRUE : FALSE);
    props.put(PARAM_AUDIT_PORT, Integer.toString(auditPort));
    props.setProperty(IcpManager.PARAM_ICP_ENABLED, formIcpEnable ? TRUE : FALSE);
    props.put(IcpManager.PARAM_ICP_PORT, Integer.toString(icpPort));
  }

  private static final String AUDIT_ENABLE_NAME = "audit_ena";
  private static final String AUDIT_PORT_NAME = "audit_port";
  private static final String ICP_ENABLE_NAME = "icp_ena";
  private static final String ICP_PORT_NAME = "icp_port";
}
