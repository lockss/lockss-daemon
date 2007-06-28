/*
 * $Id: ProxyIpAccess.java,v 1.27 2007-06-28 01:14:21 tlipkis Exp $
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

  private static final String exp =
    "Enter the list of IP addresses that should be allowed to use this " +
    "LOCKSS box as a proxy server, and access the content preserved on it.  " +
    commonExp;

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

  protected void saveChanges() throws IOException {
    boolean auditEnable = CurrentConfig.getBooleanParam(
        ProxyAndContent.PARAM_AUDIT_ENABLE, ProxyAndContent.DEFAULT_AUDIT_ENABLE);
    int auditPort = CurrentConfig.getIntParam(ProxyAndContent.PARAM_AUDIT_PORT, 0);
    boolean icpEnable = getLockssDaemon().getIcpManager().isIcpServerRunning();
    int icpPort = getLockssDaemon().getIcpManager().getCurrentPort();
    ProxyAndContent.saveAuditAndIcp(configMgr,
				    auditEnable,
				    Integer.toString(auditPort),
				    icpEnable,
				    Integer.toString(icpPort));
    super.saveChanges();
  }

}
