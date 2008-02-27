/*
 * $Id: ProxyInfo.java,v 1.27 2008-02-27 06:06:32 tlipkis Exp $
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

package org.lockss.daemon;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.Map.Entry;

import org.apache.oro.text.regex.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.protocol.IdentityManager;
import org.lockss.proxy.*;
import org.lockss.proxy.icp.*;
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
  public abstract class FragmentBuilder {

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
    public String generateFragment(Set urlStems) {
      StringBuffer buffer = new StringBuffer();

      // Bail if empty
      if (urlStems.isEmpty()) {
        generateEmpty(buffer);
        return buffer.toString();
      }

      // Generate beginning
      generateBeginning(buffer);

      // Generate entries
      for (Iterator iter = urlStems.iterator() ; iter.hasNext() ; ) {
	String stem = (String)iter.next();
	Collection aus = getPluginMgr().getCandidateAusFromStem(stem);
	if (aus != null && !aus.isEmpty()) {
	  // Assemble comment string:  AU1, AU2, (n more)
	  StringBuffer sb = new StringBuffer();
	  Iterator auIter = aus.iterator();
	  int n = 0;
	  while (auIter.hasNext() && (n < getMaxCommentAus())) {
	    ArchivalUnit au = (ArchivalUnit)auIter.next();
	    if (n != 0) {
	      sb.append(", ");
	    }
	    sb.append(au.getName());
	    n++;
	  }
	  if (auIter.hasNext()) {
	    sb.append(", (");
	    sb.append(aus.size() - n);
	    sb.append(" more)");
	  }
	  generateEntry(buffer, StringUtil.removeTrailing(stem, "/"),
			sb.toString());
	} else {
	  log.warning("Stem has no AUs: " + stem);
	}
      }

      // Generate end
      generateEnd(buffer);

      return buffer.toString();
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
     * @param comment E.g., AU name
     */
    protected abstract void generateEntry(StringBuffer buffer,
                                          String urlStem,
                                          String comment);

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
    public void generateEntry(StringBuffer buffer, String urlStem, 
			      String comment) {
      buffer.append(" // ");
      buffer.append(comment);
      buffer.append('\n');
      buffer.append(" if (shExpMatch(url, \"");
      buffer.append(shPattern(urlStem));
      buffer.append("\"))\n");
      buffer.append(" { return \"PROXY ");
      buffer.append(getProxyHost());
      buffer.append(":");
      buffer.append(getProxyPort());
      buffer.append("; DIRECT\"; }\n\n");
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
                                        "FindProxyForURL",
                                        encapsulatedName);

        buffer.append(" return ");
        buffer.append(encapsulatedName);
        buffer.append("(url, host);\n");
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
        throw new RuntimeException("Unexpected malformed pattern: "
                                   + exc.toString());
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
      return Util.substitute(RegexpUtil.getMatcher(), fromPat, subst,
                             js, Util.SUBSTITUTE_ALL);
    }

  }

  /**
   * <p>A version of {@link FragmentBuilder} specialized to generate
   * an EZproxy configuration fragment.</p>
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
    public void generateEntry(StringBuffer buffer, String urlStem,
			      String comment) {
      buffer.append("Title ");
      buffer.append(comment);
      buffer.append("\n");
      buffer.append("URL ");
      buffer.append(urlStem);
      buffer.append("\n");
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
      buffer.append("# Generated ");
      buffer.append(TimeBase.nowDate().toString());
      buffer.append(" by LOCKSS cache ");
      buffer.append(getProxyHost());
      buffer.append("\n\n");
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

      buffer.append(beforeCommands);
      buffer.append('\n');

      // Define a catch-all ACL
      buffer.append(beforeCommands);
      buffer.append("# If you already have \"acl XYZ src 0.0.0.0/0.0.0.0\" or equivalent elsewhere,\n");
      buffer.append(beforeCommands);
      buffer.append("# comment out this next line and replace \"deny anyone\" by \"deny XYZ\" in\n");
      buffer.append(beforeCommands);
      buffer.append("# \"cache_peer_access ");
      buffer.append(getProxyHost());
      buffer.append(" deny anyone\" (see below).\n");
      buffer.append(beforeCommands);
      buffer.append("acl anyone src 0.0.0.0/0.0.0.0\n");
      buffer.append(beforeCommands);
      buffer.append('\n');

      // Declare the LOCKSS cache to be a parent peer
      buffer.append(beforeCommands);
      buffer.append("cache_peer ");
      buffer.append(getProxyHost());
      buffer.append(" parent ");
      buffer.append(CurrentConfig.getIntParam(ProxyManager.PARAM_PORT,
                                              ProxyManager.DEFAULT_PORT));
      buffer.append(' ');
      int port = CurrentConfig.getIntParam(IcpManager.PARAM_ICP_PORT,
                                           CurrentConfig.getIntParam(IcpManager.PARAM_PLATFORM_ICP_PORT,
                                                                     -1));
      buffer.append(port > 0 ? Integer.toString(port) : "???");
      buffer.append(" proxy-only\n");

      // Additional explanation if ICP is not quite ready
      IcpManager icpManager = (IcpManager)LockssDaemon.getManager(LockssDaemon.ICP_MANAGER);
      if (!icpManager.isIcpServerAllowed()) {
        buffer.append(beforeCommands);
        buffer.append("# (The platform on ");
        buffer.append(getProxyHost());
        buffer.append(" is configured to disallow ICP.\n");
        buffer.append(beforeCommands);
        buffer.append("# To enable ICP you must perform a platform reconfiguration reboot.)\n");
      }
      else if (!(port > 0)) {
        buffer.append(beforeCommands);
        buffer.append("# (The ICP server is not running on ");
        buffer.append(getProxyHost());
        buffer.append(".\n");
        buffer.append(beforeCommands);
        buffer.append("# Replace \"???\" by the ICP port after setting it up.)\n");
      }
      buffer.append(beforeCommands);
      buffer.append('\n');

      // Allow the domain list
      buffer.append(beforeCommands);
      buffer.append("cache_peer_access ");
      buffer.append(getProxyHost());
      buffer.append(" allow ");
      buffer.append(encodeAclName());
      buffer.append('\n');
      buffer.append(beforeCommands);
      buffer.append('\n');

      // Deny everything else
      buffer.append(beforeCommands);
      buffer.append("# If you already have \"acl XYZ src 0.0.0.0/0.0.0.0\" or equivalent elsewhere,\n");
      buffer.append(beforeCommands);
      buffer.append("# replace \"deny anyone\" by \"deny XYZ\" (see above).\n");
      buffer.append(beforeCommands);
      buffer.append("cache_peer_access ");
      buffer.append(getProxyHost());
      buffer.append(" deny anyone\n");
      buffer.append(beforeCommands);
      buffer.append("\n\n");
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

      buffer.append("# Suggested file name: ");
      buffer.append(encodeAclName());
      buffer.append(".txt\n\n");

      buffer.append("# Suggested use in Squid config file:\n");
      buffer.append("#\n");

      buffer.append("#    # Edit the path accordingly\n");
      buffer.append("#    acl ");
      buffer.append(encodeAclName());
      buffer.append(" dstdomain \"/the/path/to/");
      buffer.append(encodeAclName());
      buffer.append(".txt\"\n");

      commonUsage(buffer, "#    ");
    }

    /* Inherit documentation */
    public void generateEmpty(StringBuffer buffer) {
      generateBeginning(buffer);
      buffer.append("# No URLs cached on this LOCKSS cache.\n\n");
    }

    /* Inherit documentation */
    public void generateEntry(StringBuffer buffer, String urlStem,
			      String comment) {
      buffer.append("# " + comment + "\n");
      buffer.append(UrlUtil.stripProtocol(urlStem) + "\n\n");
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
      buffer.append("# Go look for further instructions after all the following \"dstdomain\" lines.\n\n");
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
      buffer.append("# LOCKSS CACHE CONFIG\n");
      buffer.append("#\n");
      commonUsage(buffer, "");
    }

    /* Inherit documentation */
    public void generateEntry(StringBuffer buffer, String urlStem,
			      String comment) {
      buffer.append("# " + comment + "\n");
      buffer.append("acl " + encodeAclName() + " dstdomain " +
		    UrlUtil.stripProtocol(urlStem) + "\n\n");
    }

  }

  static final String PARAM_MAX_COMMENT_AUS =
    Configuration.PREFIX + "proxyInfo.maxCommentAus";
  static final int DEFAULT_MAX_COMMENT_AUS = 2;

  private String proxyHost;
  private int proxyPort = -1;

  private int maxCommentAus = -1;

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
  public String generateEncapsulatedPacFile(Set urlStems,
				            String pacFileToBeEncapsulated,
				            String message) {
    return new EncapsulatedPacFileFragmentBuilder(
        pacFileToBeEncapsulated, message).generateFragment(urlStems);
  }

  /** Generate a PAC file string from the URL stem map, delegating to the
   * FindProxyForURL function in the pacFileToBeEncapsulated if no match is
   * found. */
  public String generateEncapsulatedPacFileFromURL(Set urlStems,
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

  public String generateExternalSquidFragment(Set urlStems) {
    return new ExternalSquidFragmentBuilder().generateFragment(urlStems);
  }

  /** Generate an EZproxy config fragment from the URL stem map */
  public String generateEZProxyFragment(Set urlStems) {
    return new EZProxyFragmentBuilder().generateFragment(urlStems);
  }

  /** Generate a PAC file string from the URL stem map */
  public String generatePacFile(Set urlStems) {
    return new PacFileFragmentBuilder().generateFragment(urlStems);
  }

  public String generateSquidConfigFragment(Set urlStems) {
    return new SquidConfigFragmentBuilder().generateFragment(urlStems);
  }

  /** Convenience method to get URL stem map for all AUs */
  public Set getUrlStems() {
    return getPluginMgr().getAllStems();
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

  private int getMaxCommentAus() {
    if (maxCommentAus < 0) {
      maxCommentAus =
        CurrentConfig.getIntParam(PARAM_MAX_COMMENT_AUS,
				  DEFAULT_MAX_COMMENT_AUS);
    }
    return maxCommentAus;
  }

  PluginManager getPluginMgr() {
    return
      (PluginManager)LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER);
  }

  /** Convenience method to get all AUs */
  private Collection getAus() {
    return getPluginMgr().getAllAus();
  }

  protected static Logger log = Logger.getLogger("ProxyInfo");

  static final int MAX_ENCAPSULATED_PAC_SIZE = 100 * 1024;

}
