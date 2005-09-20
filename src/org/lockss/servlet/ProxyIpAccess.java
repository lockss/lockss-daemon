/*
 * $Id: ProxyIpAccess.java,v 1.12.2.1 2005-09-20 17:24:18 thib_gc Exp $
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
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ResourceManager;
import org.lockss.proxy.AuditProxyManager;
import org.lockss.proxy.ProxyManager;
import org.lockss.proxy.icp.IcpManager;
import org.lockss.util.StringUtil;
import org.mortbay.html.Composite;
import org.mortbay.html.Input;
import org.mortbay.html.Table;

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
    return Configuration.getBooleanParam(PARAM_AUDIT_ENABLE,
                                         DEFAULT_AUDIT_ENABLE);
  }

  private String getDefaultAuditPort() {
    String port = formAuditPort;
    if (StringUtil.isNullString(port)) {
      port = Configuration.getParam(PARAM_AUDIT_PORT);
    }
    return port;
  }

  private boolean getDefaultIcpEnable() {
    if (isForm) {
      return formIcpEnable;
    }
    return Configuration.getBooleanParam(IcpManager.PARAM_ICP_ENABLED,
                                         IcpManager.DEFAULT_ICP_ENABLED);
  }
  
  private String getDefaultIcpPort() {
    String port = formIcpPort;
    if (StringUtil.isNullString(port)) {
      port = Configuration.getParam(IcpManager.PARAM_ICP_PORT);
    }
    return port;
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

  protected Composite getAdditionalFormElement() {
    // Start table
    Table tbl = new Table(0, "align=center cellpadding=10");
    
    buildAuditRow(tbl);
    /* buildIcpRow(tbl); */
    // FIXME: disabled; see Issue 1478
    
    return tbl;
  }

  /**
   * <p>Subpart of {@link #getAdditionalFormElement()} in charge of
   * building the row with an Audit PRoxy checkbox, port number text
   * field and port list.</p>
   * @param tbl The Table instance being populated.
   * @see #getAdditionalFormElement
   */
  private void buildAuditRow(Table tbl) {
    // Start "audit" row
    tbl.newRow();
    
    // Start "audit" first line
    tbl.newCell("align=center");
    Input auditEnaElem = new Input(Input.Checkbox, AUDIT_ENABLE_NAME, "1");
    if (getDefaultAuditEnable()) {
      auditEnaElem.check();
    }
    setTabOrder(auditEnaElem);
    tbl.add(auditEnaElem);
    tbl.add("Enable audit proxy");
    tbl.add(addFootnote(AUDIT_FOOT));
    tbl.add(" on port&nbsp;");
    
    Input auditPortElem =
      new Input(Input.Text, AUDIT_PORT_NAME, getDefaultAuditPort());
    auditPortElem.setSize(6);
    setTabOrder(auditPortElem);
    tbl.add(auditPortElem);
    
    // Start "audit" second line
    // avoid breaking anything with this patch
    try {
      List usablePorts =
        resourceMgr.getUsableTcpPorts(AuditProxyManager.SERVER_NAME);
      if (usablePorts != null) {
        tbl.add("<br>");
        if (usablePorts.isEmpty()) {
          tbl.add("(No available ports)");
          tbl.add(addFootnote(AUDIT_FOOT));
        } else {
          tbl.add("Available ports");
          tbl.add(addFootnote(FILTER_FOOT));
          tbl.add(": ");
          tbl.add(StringUtil.separatedString(usablePorts, ", "));
        }
      }
    } catch (Exception ignore) {}
  }
  
  /**
   * <p>Subpart of {@link #getAdditionalFormElement()} in charge of
   * building the row with an ICP checkbox and port number text
   * field.</p>
   * @param tbl The Table instance being populated.
   * @see #getAdditionalFormElement
   */
  private void buildIcpRow(Table tbl) {
    // Start "ICP" row
    tbl.newRow();
    
    // Start "ICP" line
    tbl.newCell("align=center");
    
    Input icpEnaElem = new Input(Input.Checkbox, ICP_ENABLE_NAME, "1");
    if (getDefaultIcpEnable()) {
      icpEnaElem.check();
    }
    setTabOrder(icpEnaElem);
    tbl.add(icpEnaElem);
    tbl.add("Enable ICP server");
    tbl.add(addFootnote(ICP_FOOT));
    tbl.add(" on port&nbsp;");
    
    Input icpPortElem =
      new Input(Input.Text, ICP_PORT_NAME, getDefaultIcpPort());
    icpPortElem.setSize(6);
    setTabOrder(icpPortElem);
    tbl.add(icpPortElem);    
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
