/*
 * $Id: ProxyInfo.java,v 1.1 2003-07-13 20:53:48 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;
import java.util.*;
import java.net.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.proxy.*;
import org.lockss.protocol.*;

/**
 * Generate config files for external proxies, specifying patterns of URLs
 * that should be directed to this cache.
 */
public class ProxyInfo {
  private String proxyHost;
  private int proxyPort = -1;

  /** Create a ProxyInfo using the local IP address as the proxy host */
  public ProxyInfo() {
  }

  /** Create a ProxyInfo using the supplied host as the proxy host */
  public ProxyInfo(String proxyHost) {
    this();
    this.proxyHost = proxyHost;
  }

  /** Determine the local proxy hostname.  Can be supplied by constructor
   * (useful when invoked from servlet, which knows the name by which the
   * user referred to this mmachine), otherwise comes from configured local
   * IP address.
   * @return the proxy hostname
   */
  String getProxyHost() {
    if (proxyHost == null) {
      proxyHost = Configuration.getParam(IdentityManager.PARAM_LOCAL_IP);
    }
    return proxyHost;
  }

  /** Determine the local proxy port.  Tries to get it from ProxyManager if
   * exists, else from configuration.
   * @return the proxy port number
   */
  int getProxyPort() {
    if (proxyPort == -1) {
      try {
	ProxyManager mgr =
	  (ProxyManager)LockssDaemon.getManager(LockssDaemon.PROXY_MANAGER);
	proxyPort = mgr.getProxyPort();
      } catch (IllegalArgumentException e) {
	proxyPort = Configuration.getIntParam(ProxyManager.PARAM_PORT,
					      ProxyManager.DEFAULT_PORT);
      }
    }
    return proxyPort;
  }

  /** Convenience method to get all AUs */
  private Collection getAus() {
    PluginManager pmgr =
      (PluginManager)LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER);
    return pmgr.getAllAUs();
  }

  /** Convenience method to get URL stem map for all AUs */
  public Map getUrlStemMap() {
    return getUrlStemMap(getAus());
  }

  /** 
   * @return Map of all URL stems to their AU
   */
  public Map getUrlStemMap(Collection aus) {
    Map map = new HashMap();
    PluginManager pmgr =
      (PluginManager)LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER);
    for (Iterator iter = pmgr.getAllAUs().iterator(); iter.hasNext(); ) {
      ArchivalUnit au = (ArchivalUnit)iter.next();
      for (Iterator urlIter = au.getUrlStems().iterator();
	   urlIter.hasNext(); ) {
	String urlStem = (String)urlIter.next();
	map.put(urlStem, au);
      }
    }
    return map;
  }

  /** Generate a PAC file string from the URL stem map */
  public String generatePacFile(Map urlStems) {
    StringBuffer sb = new StringBuffer();
    sb.append("// PAC file generated ");
    sb.append(TimeBase.nowDate().toString());
    sb.append(" by LOCKSS cache ");
    sb.append(getProxyHost());
    sb.append("\n\n");
    generatePacFunction(sb, urlStems);
    return sb.toString();
  }

  void generatePacFunction(StringBuffer sb, Map urlStems) {
    sb.append("function FindProxyForURL(url, host) {\n");
    for (Iterator iter = urlStems.keySet().iterator(); iter.hasNext(); ) {
      String urlStem = (String)iter.next();
      generatePacEntry(sb, urlStem);
    }
    sb.append(" return \"DIRECT\";\n");
    sb.append("}\n");
  }

  /** Generate a single if clause for a PAC file */
  void generatePacEntry(StringBuffer sb, String urlStem) {
    sb.append(" if (shExpMatch(url, \"");
    sb.append(shPattern(urlStem));
    sb.append("\"))\n { return \"PROXY ");
    sb.append(getProxyHost());
    sb.append(":");
    sb.append(getProxyPort());
    sb.append("\"; }\n\n");
  }

  /** Turn a stem into a shell pattern */
  private String shPattern(String urlStem) {
    return urlStem + "/*";
  }

  /** Generate an EZproxy config fragment from the URL stem map */
  public String generateEZProxyFragment(Map urlStems) {
    StringBuffer sb = new StringBuffer();
    sb.append("# EZproxy config generated ");
    sb.append(TimeBase.nowDate().toString());
    sb.append(" by LOCKSS cache ");
    sb.append(getProxyHost());
    sb.append("\n\n");
    for (Iterator iter = urlStems.keySet().iterator(); iter.hasNext(); ) {
      String urlStem = (String)iter.next();
      ArchivalUnit au = (ArchivalUnit)urlStems.get(urlStem);
      generateEZProxyEntry(sb, urlStem, au.getName());
    }
    return sb.toString();
  }

  void generateEZProxyEntry(StringBuffer sb, String urlStem, String title) {
    sb.append("Proxy ");
    sb.append(getProxyHost());
    sb.append(":");
    sb.append(getProxyPort());
    sb.append("\nTitle ");
    sb.append(title);
    sb.append("\nURL ");
    sb.append(urlStem);
    sb.append("\nDomain ");
    try {
      sb.append(UrlUtil.getHost(urlStem));
    } catch (MalformedURLException e) {
      sb.append("???");
    }
    sb.append("\n\n");
  }

}
