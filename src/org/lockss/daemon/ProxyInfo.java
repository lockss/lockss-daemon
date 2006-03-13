/*
 * $Id: ProxyInfo.java,v 1.17 2006-03-13 22:35:54 thib_gc Exp $
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

package org.lockss.daemon;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

import org.apache.oro.text.regex.*;
import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.protocol.IdentityManager;
import org.lockss.proxy.ProxyManager;
import org.lockss.util.*;

/**
 * Generate config files for external proxies, specifying patterns of URLs
 * that should be directed to this cache.
 */
public class ProxyInfo {
  public static final int MAX_ENCAPSULATED_PAC_SIZE = 100 * 1024;
  protected static Logger log = Logger.getLogger("ProxyInfo");

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
      proxyHost =
        CurrentConfig.getParam(ConfigManager.PARAM_PLATFORM_FQDN,
                               CurrentConfig.getParam(IdentityManager.
                                                      PARAM_LOCAL_IP));
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
	proxyPort = CurrentConfig.getIntParam(ProxyManager.PARAM_PORT,
	                                      ProxyManager.DEFAULT_PORT);
      }
    }
    return proxyPort;
  }

  /** Convenience method to get all AUs */
  private Collection getAus() {
    PluginManager pmgr =
      (PluginManager)LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER);
    return pmgr.getAllAus();
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
    for (Iterator iter = aus.iterator(); iter.hasNext(); ) {
      ArchivalUnit au = (ArchivalUnit)iter.next();
      if (!pmgr.isInternalAu(au)) {
	for (Iterator urlIter = au.getUrlStems().iterator();
	     urlIter.hasNext(); ) {
	  String urlStem = (String)urlIter.next();
	  map.put(urlStem, au);
	}
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

  /** Generate a PAC file string from the URL stem map, delegating to the
   * FindProxyForURL function in the pacFileToBeEncapsulated if no match is
   * found. */
  public String encapsulatePacFileFromURL(Map urlStems, String url)
      throws IOException {
    InputStream bis = null;
    try {
      InputStream istr = UrlUtil.openInputStream(url);
      bis = new BufferedInputStream(istr);
      String old = StringUtil.fromInputStream(bis, MAX_ENCAPSULATED_PAC_SIZE);
      return encapsulatePacFile(urlStems, old, " (generated from " + url + ")");
    } finally {
      if (bis != null) {
	bis.close();
      }
    }
  }

  /** Generate a PAC file string from the URL stem map, delegating to the
   * FindProxyForURL function in the pacFileToBeEncapsulated if no match is
   * found. */
  public String encapsulatePacFile(Map urlStems,
				   String pacFileToBeEncapsulated,
				   String message) {
    try {
      String encapsulatedName = findUnusedName(pacFileToBeEncapsulated);
      String encapsulated =
	jsReplace(pacFileToBeEncapsulated, "FindProxyForURL",
		  encapsulatedName);
      StringBuffer sb = new StringBuffer();
      sb.append("// PAC file generated ");
      sb.append(TimeBase.nowDate().toString());
      sb.append(" by LOCKSS cache ");
      sb.append(getProxyHost());
      sb.append("\n\n");
      generatePacFunction(sb, urlStems,
			  " return " + encapsulatedName + "(url, host);\n");
      sb.append("// Encapsulated PAC file follows");
      if (message != null) {
	sb.append(message);
      }
      sb.append("\n");
      if (encapsulated.equals(pacFileToBeEncapsulated)) {
	sb.append("// File is unmodified; it doesn't look like a PAC file\n");
      }
      sb.append("\n");
      sb.append(encapsulated);
      return sb.toString();
    } catch (MalformedPatternException e) {
      log.error("PAC file patterns", e);
      throw new RuntimeException("Unexpected malformed pattern: " +
				 e.toString());
    }
  }

  void generatePacFunction(StringBuffer sb, Map urlStems) {
    generatePacFunction(sb, urlStems, " return \"DIRECT\";\n");
  }

  void generatePacFunction(StringBuffer sb, Map urlStems, String elseStmt) {
    sb.append("function FindProxyForURL(url, host) {\n");
    for (Iterator iter = urlStems.keySet().iterator(); iter.hasNext(); ) {
      String urlStem = (String)iter.next();
      generatePacEntry(sb, urlStem);
    }
    sb.append(elseStmt);
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

  String findUnusedName(String js) throws MalformedPatternException {
    return findUnusedName(js, "FindProxyForURL_");
  }

  String findUnusedName(String js, String base)
      throws MalformedPatternException {
    for (int suff = 0; true; suff++) {
      Pattern pat =
	RegexpUtil.getCompiler().compile("\\b" + base + suff + "\\b");
      if (!RegexpUtil.getMatcher().contains(js, pat)) {
	return base + suff;
      }
    }
  }

  String jsReplace(String js, String oldname, String newname)
      throws MalformedPatternException {
    Pattern fromPat =
      RegexpUtil.getCompiler().compile("\\b" + oldname + "\\b");
    Substitution subst = new Perl5Substitution(newname);
    return Util.substitute(RegexpUtil.getMatcher(), fromPat, subst, js,
			   Util.SUBSTITUTE_ALL);
  }

  /** Generate an EZproxy config fragment from the URL stem map */
  public String generateEZProxyFragment(Map urlStems) {
    StringBuffer sb = new StringBuffer();
    sb.append("# EZproxy config generated ");
    sb.append(TimeBase.nowDate().toString());
    sb.append(" by LOCKSS cache ");
    sb.append(getProxyHost());
    sb.append("\n");
    sb.append("Proxy ");
    sb.append(getProxyHost());
    sb.append(":");
    sb.append(getProxyPort());
    sb.append("\n\n");
    for (Iterator iter = urlStems.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry entry = (Map.Entry)iter.next();
      String urlStem = (String)entry.getKey();
      ArchivalUnit au = (ArchivalUnit)entry.getValue();
      generateEZProxyEntry(sb, urlStem, au.getName());
    }
    sb.append("Proxy\n");
    sb.append("# End of LOCKSS config\n");
    return sb.toString();
  }

  public String generateSquidFile(Map urlStems) {
    SortedSet stems = new TreeSet();
    for (Iterator iter = urlStems.keySet().iterator() ; iter.hasNext() ; ) {
      stems.add(generateSquidEntry((String)iter.next()));
    }

    StringBuffer sb = new StringBuffer();
    for (Iterator iter = stems.iterator() ; iter.hasNext() ; ) {
      sb.append((String)iter.next());
      sb.append('\n');
    }
    sb.append('\n');
    return sb.toString();
  }

  void generateEZProxyEntry(StringBuffer sb, String urlStem, String title) {
    sb.append("Title ");
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

  String generateSquidEntry(String stem) {
    final String PROTOCOL_SUBSTRING = "://";
    return "." + stem.substring(
        stem.indexOf(PROTOCOL_SUBSTRING) + PROTOCOL_SUBSTRING.length());
  }

}
