/*
 * $Id: ProxyInfo.java,v 1.19 2006-03-24 23:17:23 thib_gc Exp $
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
import java.util.Map.Entry;

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

  /**
   * <p>An abstraction for classes that produce configuration
   * fragments based on all the URL stems being cached.</p>
   * <p>{@link #generateFragment} is the template method;
   * {@link #generateBeginning}, {@link #generateEntry},
   * {@link #generateEnd} and {@link #generateEmpty} are the
   * submethods.</p>
   * @author Thib Guicherd-Callin
   * @see #generateFragment
   */
  public static abstract class FragmentBuilder {

    /**
     * <p>A template method that generates a fragment from a map of
     * URL stems.</p>
     * <p>The template runs in the following manner.</p>
     * <ol>
     *  <li>If the URL stem map is empty, call
     *  {@link #generateEmpty}.</li>
     *  <li>Otherwise,
     *   <ol>
     *    <li>Sort the URLs.</li>
     *    <li>Call {@link #generateBeginning}.</li>
     *    <li>For each URL/AU pair in the map, call
     *    {@link #generateEntry}.</li>
     *    <li>Call {@link #generateEnd}.</li>
     *   </ol>
     *  </li>
     * </ol>
     * <p>Note that the default implementations of
     * {@link #generateBeginning}, {@link #generateEnd} and
     * {@link #generateEmpty} provided by this class do nothing,
     * and that the default behavior of {@link #compare} as
     * implemented in this class is to compare the URLs alphabetically
     * (ignoring case) with the protocol removed.</p>
     * @param urlStems The URL stem map.
     * @return A generated fragment, as a string.
     */
    public String generateFragment(Map urlStems) {
      StringBuffer buffer = new StringBuffer();

      // Bail if empty
      if (urlStems.isEmpty()) {
        generateEmpty(buffer);
        return buffer.toString();
      }

      // Sort entries
      Set entries;
      try {
        entries = new TreeSet(new Comparator() {
          public int compare(Object obj1, Object obj2) {
            Entry entry1 = (Entry)obj1;
            Entry entry2 = (Entry)obj2;
            return FragmentBuilder.this.compare((String)entry1.getKey(),
                                                (ArchivalUnit)entry1.getValue(),
                                                (String)entry2.getKey(),
                                                (ArchivalUnit)entry2.getValue());
          }
          public boolean equals(Object obj) {
            return obj == this;
          }
        });
        entries.addAll(urlStems.entrySet());
      }
      catch (Exception exc) {
        entries = urlStems.entrySet(); // cannot order entries
      }

      // Generate beginning
      generateBeginning(buffer);

      // Generate entries
      for (Iterator iter = entries.iterator() ; iter.hasNext() ; ) {
        Entry entry = (Entry)iter.next();
        generateEntry(buffer,
                      (String)entry.getKey(),
                      (ArchivalUnit)entry.getValue());
      }

      // Generate end
      generateEnd(buffer);

      return buffer.toString();
    }

    /**
     * <p>Compares two URL/AU pairs, to determine in which order
     * the entries should appear in the fragment.</p>
     * <p>The convention used is the same as that for
     * {@link Comparable#compareTo}.</p>
     * @param urlStem1 The first entry's URL stem.
     * @param au1      The first entry's archival unit.
     * @param urlStem2 The second entry's URL stem.
     * @param au2      The second entry's archival unit.
     * @return A negative integer if the first entry comes before the
     *         second entry, a positive integer if the first entry
     *         comes after the second entry, and zero if the two
     *         entries are equivalent.
     */
    protected int compare(String urlStem1,
                          ArchivalUnit au1,
                          String urlStem2,
                          ArchivalUnit au2) {
      return removeProtocol(urlStem1).compareToIgnoreCase(removeProtocol(urlStem2));
    }

    /**
     * <p>Generates the leading part of the fragment.</p>
     * @param buffer A {@link StringBuffer} instance to output into.
     */
    protected void generateBeginning(StringBuffer buffer) {}

    /**
     * <p>Generates a fragment appropriate for an empty url stem
     * map.</p>
     * @param buffer A {@link StringBuffer} instance to output into.
     */
    protected void generateEmpty(StringBuffer buffer) {}

    /**
     * <p>Generates the closing part of the fragment.</p>
     * @param buffer A {@link StringBuffer} instance to output into.
     */
    protected void generateEnd(StringBuffer buffer) {}

    /**
     * <p>Generates a part of the fragment for one URL stem and its
     * associated archival unit.</p>
     * @param buffer  A {@link StringBuffer} instance to output into.
     * @param urlStem A url stem.
     * @param au      The stem's corresponding archival unit.
     */
    protected abstract void generateEntry(StringBuffer buffer,
                                          String urlStem,
                                          ArchivalUnit au);

    /**
     * <p>Removes the protocol and its <code>://</code> component
     * from a URL stem.</p>
     * <p>Assumption: url stems always start with a protocol.</p>
     * @param stem A URL stem.
     * @return A new URL stem with the protocol removed.
     */
    protected static String removeProtocol(String stem) {
      final String PROTOCOL_SUBSTRING = "://";
      return stem.substring(
          stem.indexOf(PROTOCOL_SUBSTRING) + PROTOCOL_SUBSTRING.length());
    }

  }

  /**
   * <p>A version of {@link FragmentBuilder} specialized to generate
   * a PAC file.</p>
   * @author Thib Guicherd-Callin
   */
  class PacFileFragmentBuilder extends FragmentBuilder {

    /* Inherit documentation */
    public void generateBeginning(StringBuffer buffer) {
      commonHeader(buffer);
      buffer.append("function FindProxyForURL(url, host) {\n");
    }

    /* Inherit documentation */
    public void generateEmpty(StringBuffer buffer) {
      commonHeader(buffer);
      buffer.append("// No URLs cached on this LOCKSS cache\n\n");
    }

    /* Inherit documentation */
    public void generateEnd(StringBuffer buffer) {
      buffer.append(" return \"DIRECT\";\n");
      buffer.append("}\n");
    }

    /* Inherit documentation */
    public void generateEntry(StringBuffer buffer, String urlStem, ArchivalUnit au) {
      buffer.append(" // ");
      buffer.append(au.getName());
      buffer.append('\n');
      buffer.append(" if (shExpMatch(url, \"");
      buffer.append(shPattern(urlStem));
      buffer.append("\"))\n");
      buffer.append(" { return \"PROXY ");
      buffer.append(getProxyHost());
      buffer.append(":");
      buffer.append(getProxyPort());
      buffer.append("\"; }\n\n");
    }

    /**
     * <p>Generates a typical header.</p>
     * @param buffer A {@link StringBuffer} instance to output into.
     */
    protected void commonHeader(StringBuffer buffer) {
      buffer.append("// PAC file\n");
      buffer.append("// Generated ");
      buffer.append(TimeBase.nowDate().toString());
      buffer.append(" by LOCKSS cache ");
      buffer.append(getProxyHost());
      buffer.append("\n\n");
    }

    /**
     * <p>Turns a stem into a shell pattern.</p>
     * @param urlStem A URL stem.
     * @return The URL stem followed by <code>/*</code>.
     */
    String shPattern(String urlStem) {
      return urlStem + "/*";
    }

  }

  /**
   * <p>A version of {@link PacFileFragmentBuilder} specialized to
   * combine the fragment it generates with an existing PAC file.</p>
   * @author Thib Guicherd-Callin
   */
  class EncapsulatedPacFileFragmentBuilder extends PacFileFragmentBuilder {

    /**
     * <p>An additional message.</p>
     */
    protected String message;

    /**
     * <p>The PAC file being encapsulated.</p>
     */
    protected String pacFileToBeEncapsulated;

    /**
     * <p>Builds a new {@link EncapsulatedPacFileFragmentBuilder}
     * instance.</p>
     * @param pacFileToBeEncapsulated The PAC file being encapsulated.
     * @param message                 An additional message.
     */
    public EncapsulatedPacFileFragmentBuilder(String pacFileToBeEncapsulated,
                                              String message) {
      this.pacFileToBeEncapsulated = pacFileToBeEncapsulated;
      this.message = message;
    }

    /* Inherit documentation */
    public void generateEnd(StringBuffer buffer) {
      try {
        String encapsulatedName = findUnusedName(pacFileToBeEncapsulated);
        String encapsulated = jsReplace(pacFileToBeEncapsulated,
            "FindProxyForURL", encapsulatedName);

        buffer.append(" return " + encapsulatedName + "(url, host);\n");
        buffer.append("}\n\n");
        buffer.append("// Encapsulated PAC file follows ");
        if (!StringUtil.isNullString(message)) {
          buffer.append(message);
        }
        buffer.append("\n\n");

        if (encapsulated.equals(pacFileToBeEncapsulated)) {
          buffer.append("// File is unmodified; it doesn't look like a PAC file\n");
        }
        buffer.append(encapsulated);
      }
      catch (MalformedPatternException exc) {
        log.error("PAC file patterns", exc);
        throw new RuntimeException(
            "Unexpected malformed pattern: " + exc.toString());
      }
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

  }

  /**
   * <p>A version of {@link FragmentBuilder} specialized to generate
   * an EZProxy configuration fragment.</p>
   * @author Thib Guicherd-Callin
   */
  class EZProxyFragmentBuilder extends FragmentBuilder {

    /* Inherit documentation */
    public void generateBeginning(StringBuffer buffer) {
      commonHeader(buffer);
      buffer.append("Proxy ");
      buffer.append(getProxyHost());
      buffer.append(':');
      buffer.append(getProxyPort());
      buffer.append("\n\n");
    }

    /* Inherit documentation */
    public void generateEmpty(StringBuffer buffer) {
      commonHeader(buffer);
      buffer.append("# No URLs cached on this LOCKSS cache.\n");
    }

    /* Inherit documentation */
    public void generateEnd(StringBuffer buffer) {
      buffer.append("Proxy\n");
      buffer.append("# End of LOCKSS config\n");
    }

    /* Inherit documentation */
    public void generateEntry(StringBuffer buffer, String urlStem, ArchivalUnit au) {
      buffer.append("Title " + au.getName() + "\n");
      buffer.append("URL " + urlStem + "\n");
      buffer.append("Domain ");
      try {
        buffer.append(UrlUtil.getHost(urlStem));
      }
      catch (MalformedURLException e) {
        buffer.append("???");
      }
      buffer.append("\n\n");
    }

    /**
     * <p>Generates a typical header.</p>
     * @param buffer A {@link StringBuffer} instance to output into.
     */
    protected void commonHeader(StringBuffer buffer) {
      buffer.append("# EZproxy config generated ");
      buffer.append(TimeBase.nowDate().toString());
      buffer.append(" by LOCKSS cache ");
      buffer.append(getProxyHost());
      buffer.append('\n');
    }

  }

  /**
   * <p>A version of {#link FragmentBuilder} specialized for Squid
   * configuration fragments.</p>
   * @author Thib Guicherd-Callin
   */
  abstract class SquidFragmentBuilder extends FragmentBuilder {

    /**
     * <p>Generates a typical header.</p>
     * @param buffer A {@link StringBuffer} instance to output into.
     */
    protected void commonHeader(StringBuffer buffer) {
      buffer.append("# Generated " + TimeBase.nowDate().toString() + " by LOCKSS cache " + getProxyHost() + "\n\n");
    }

    /**
     * <p>Generates the typical usage instructions.</p>
     * @param buffer         A {@link StringBuffer} instance to output
     *                       into.
     * @param beforeCommands A string to prepend in front of the
     *                       various Squid commands. For example, if
     *                       this prefix starts with <code>#</code>
     *                       then the commands are merely disabled,
     *                       otherwise they may be picked up by
     *                       Squid.
     */
    protected void commonUsage(StringBuffer buffer, String beforeCommands) {
      if (beforeCommands == null) {
        beforeCommands = "";
      }

      buffer.append(beforeCommands + "acl anyone src 0.0.0.0/0.0.0.0\n");
      buffer.append(beforeCommands + "cache_peer " + getProxyHost() + " parent <PROXYPORT> <ICPPORT> proxy-only\n");
      buffer.append(beforeCommands + "cache_peer_access " + getProxyHost() + " allow " + encodeAclName() + "\n");
      buffer.append(beforeCommands + "cache_peer_access " + getProxyHost() + " deny anyone\n");

      buffer.append("# Replace <PROXYPORT> by the port number used by your LOCKSS box\n");
      buffer.append("#  for proxy requests (typically 9090).\n");
      buffer.append("# Replace <ICPPORT> by the port number used by your LOCKSS box\n");
      buffer.append("#  for ICP requests (typically 3130).\n");
      buffer.append("# If you already have \"acl XYZ src 0.0.0.0/0.0.0.0\" or equivalent elsewhere,\n");
      buffer.append("#  do not include \"acl anyone src 0.0.0.0/0.0.0.0\" and replace\n");
      buffer.append("#  \"deny anyone\" by \"deny XYZ\".\n\n");
    }

    /**
     * <p>Encodes this cache's host name with dashes to produce an
     * ACL name for Squid.</p>
     * @return This cache's host name with dots replaced by dashes
     *         and the suffix <code>-domains</code>.
     */
    protected String encodeAclName() {
      return getProxyHost().replaceAll("\\.", "-") + "-domains";
    }

  }

  /**
   * <p>A version of {@link SquidFragmentBuilder} specialized to
   * generate an external dstdomain file.</p>
   * @author Thib Guicherd-Callin
   */
  class ExternalSquidFragmentBuilder extends SquidFragmentBuilder {

    /* Inherit documentation */
    public void generateBeginning(StringBuffer buffer) {
      buffer.append("# LOCKSS dstdomain file for Squid\n");
      commonHeader(buffer);
      buffer.append("# Suggested file name: " + encodeAclName() + ".txt\n\n");
      buffer.append("# Suggested use is Squid config file:\n");
      buffer.append("#     acl " + encodeAclName() + " dstdomain \"/the/path/to/" + encodeAclName() + ".txt\"\n");
      commonUsage(buffer, "#     ");
    }

    /* Inherit documentation */
    public void generateEmpty(StringBuffer buffer) {
      generateBeginning(buffer);
      buffer.append("# No URLs cached on this LOCKSS cache.\n\n");
    }

    /* Inherit documentation */
    public void generateEntry(StringBuffer buffer, String urlStem, ArchivalUnit au) {
      buffer.append("# " + au.getName() + "\n");
      buffer.append(removeProtocol(urlStem) + "\n\n");
    }

  }

  /**
   * <p>A version of {@link SquidFragmentBuilder} specialized to
   * generate dstdomain rules suitable for insertion directly into a
   * Squid configuration file.</p>
   * @author Thib Guicherd-Callin
   */
  class SquidConfigFragmentBuilder extends SquidFragmentBuilder {

    /* Inherit documentation */
    public void generateBeginning(StringBuffer buffer) {
      buffer.append("# LOCKSS configuration fragment for Squid\n");
      commonHeader(buffer);
      buffer.append("# SCROLL DOWN past all the following \"dstdomain\" lines for further instructions.\n\n");
    }

    /* Inherit documentation */
    public void generateEmpty(StringBuffer buffer) {
      buffer.append("# LOCKSS configuration fragment for Squid\n");
      commonHeader(buffer);
      buffer.append("# No URLs cached on this LOCKSS cache.\n\n");
    }

    /* Inherit documentation */
    public void generateEnd(StringBuffer buffer) {
      buffer.append("#\n");
      buffer.append("# CONTINUED: LOCKSS cache config\n");
      buffer.append("#\n");
      commonUsage(buffer, "");
    }

    /* Inherit documentation */
    public void generateEntry(StringBuffer buffer, String urlStem, ArchivalUnit au) {
      buffer.append("# " + au.getName() + "\n");
      buffer.append("acl " + encodeAclName() + " dstdomain " + removeProtocol(urlStem) + "\n\n");
    }

  }

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

  /** Generate a PAC file string from the URL stem map, delegating to the
   * FindProxyForURL function in the pacFileToBeEncapsulated if no match is
   * found. */
  public String generateEncapsulatedPacFile(Map urlStems,
				            String pacFileToBeEncapsulated,
				            String message) {
    return new EncapsulatedPacFileFragmentBuilder(
        pacFileToBeEncapsulated, message).generateFragment(urlStems);
  }

  /** Generate a PAC file string from the URL stem map, delegating to the
   * FindProxyForURL function in the pacFileToBeEncapsulated if no match is
   * found. */
  public String generateEncapsulatedPacFileFromURL(Map urlStems,
                                                   String url)
      throws IOException {
    InputStream bis = null;
    try {
      InputStream istr = UrlUtil.openInputStream(url);
      bis = new BufferedInputStream(istr);
      String old = StringUtil.fromInputStream(bis, MAX_ENCAPSULATED_PAC_SIZE);
      return generateEncapsulatedPacFile(urlStems, old,
          " (generated from " + url + ")");
    }
    finally {
      if (bis != null) {
	bis.close();
      }
    }
  }

  public String generateExternalSquidFragment(Map urlStems) {
    return new ExternalSquidFragmentBuilder().generateFragment(urlStems);
  }

  /** Generate an EZproxy config fragment from the URL stem map */
  public String generateEZProxyFragment(Map urlStems) {
    return new EZProxyFragmentBuilder().generateFragment(urlStems);
  }

  /** Generate a PAC file string from the URL stem map */
  public String generatePacFile(Map urlStems) {
    return new PacFileFragmentBuilder().generateFragment(urlStems);
  }

  public String generateSquidConfigFragment(Map urlStems) {
    return new SquidConfigFragmentBuilder().generateFragment(urlStems);
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

  protected static Logger log = Logger.getLogger("ProxyInfo");

  static final int MAX_ENCAPSULATED_PAC_SIZE = 100 * 1024;

}
