/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.regex.*;

import org.apache.commons.jxpath.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.plugin.*;

/**
 * A set of titles defined as an XPath predicate used to match some subset
 * of the known {@link TitleConfig}s */
public class TitleSetXpath extends BaseTitleSet {

  // static context used to predefine RE class functions, and to compile
  // xpath in constructor
  private static JXPathContext sharedContext = JXPathContext.newContext(null);
  static {
    sharedContext.setFunctions(new ClassFunctions(RegexpUtil.XpathUtil.class,
						  "RE"));
  }

  private String xpath;
  private CompiledExpression expr;

  /** Create a TitleSet that consists of all known titles whose {@link
   * TitleConfig} matches the supplied xpath predicate.  In addition to the
   * standard XPath functions, the extension
   * <code>RE:isMatchRe(<i>string</i>, <i>regexp</i>)</code> performs a
   * regexp match against the string.
   * @param daemon used to get list of all known titles
   * @param xpathPred an XPath predicate (<i>eg</i>,
   * <code>[journalTitle='Dog Journal']</code> )
   */
  TitleSetXpath(LockssDaemon daemon, String name, String xpathPred) {
    super(daemon, name);
    if (!(xpathPred.startsWith("[") && xpathPred.endsWith("]"))) {
      throw new IllegalArgumentException("XPath predicate must be enclosed in \"[\" ... \"]\"");
    }
    xpath = "." + xpathPred;
    expr = JXPathContext.compile(xpath);
  }

  /** Filter a collection of titles by the xpath predicate
   * @param allTitles collection of {@link TitleConfig}s to be filtered
   * @return a collection of {@link TitleConfig}s
   */
  protected Collection<TitleConfig>
    filterTitles(Collection<TitleConfig> allTitles) {

    JXPathContext context = JXPathContext.newContext(sharedContext, allTitles);
    return selectNodes(expr, context);
  }

  private List selectNodes(CompiledExpression expr, JXPathContext context) {
    ArrayList list = new ArrayList();
    for (Iterator iter = expr.iteratePointers(context); iter.hasNext(); ) {
      Pointer pointer = (Pointer)iter.next();
      list.add(pointer.getNode());
    }
    return list;
  }

  public String getPath() {
    return xpath;
  }

  /** Return all the actionable bits for the set. */
  protected int getActionables() {
    return SET_DELABLE + SET_ADDABLE + SET_REACTABLE;
  }

  protected int getMajorOrder() {
    return 4;
  }

  public boolean equals(Object o) {
    if (! (o instanceof TitleSetXpath)) {
      return false;
    }
    TitleSetXpath oset = (TitleSetXpath)o;
    return xpath.equals(oset.getPath()) && name.equals(oset.getName());
  }

  public int hashCode() {
    int hash = 0x272053;
    hash += name.hashCode();
    hash += xpath.hashCode();
    return hash;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(40);
    sb.append("[TS.XPath: ");
    sb.append(xpath);
    sb.append("]");
    return sb.toString();
  }

  /** Special case for xpath: <code>[pluginName='plugname']</code>,
   * optimized to avoid evaluating zpath expression */
  static class TSPlugin extends TitleSetXpath {

    private String pluginName;

    /** Create a TitleSet that consists of all known AUs using the named plugin
     * @param pluginName plugin name
     */
    public TSPlugin(LockssDaemon daemon, String name,
		    String xpath, String pluginName) {
      super(daemon, name, xpath);
      this.pluginName = pluginName;
    }

    protected Collection<TitleConfig>
      filterTitles(Collection<TitleConfig> allTitles) {

      ArrayList<TitleConfig> res = new ArrayList<TitleConfig>();
      for (TitleConfig tc : allTitles) {
	if (pluginName.equals(tc.getPluginName())) {
	  res.add(tc);
	}
      }
      res.trimToSize();
      return res;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder(40);
      sb.append("[TS.Plugin: ");
      sb.append(pluginName);
      sb.append("]");
      return sb.toString();
    }
  }

  /** Special case for xpath:
   * <code>[attributes/publisher='pubname']</code>, optimized to avoid
   * evaluating zpath expression */
  static class TSAttr extends TitleSetXpath {

    private String attr;
    private String val;

    /** Create a TitleSet that consists of all AUs with the given attribute
     * value
     * @param attribute attribute name
     * @param val attribute value
     */
    public TSAttr(LockssDaemon daemon, String name,
		  String xpath, String attribute, String value) {
      super(daemon, name, xpath);
      this.attr = attribute;
      this.val = value;
    }

    protected Collection<TitleConfig>
      filterTitles(Collection<TitleConfig> allTitles) {

      ArrayList<TitleConfig> res = new ArrayList<TitleConfig>();
      for (TitleConfig tc : allTitles) {
	if (val.equals(tc.getAttributes().get(attr))) {
	  res.add(tc);
	}
      }
      res.trimToSize();
      return res;
    }
    
    /** Return the number of titles in the set that can be
     * added/delated/reactivated. */
    public int countTitles(int action) {
      if ("publisher".equals(attr)) {
	// Publisher titlesets are efficiently represented as TdbPublisher

	PluginManager pmgr = daemon.getPluginManager();
	ConfigManager cmgr = daemon.getConfigManager();
	Tdb tdb = cmgr.getCurrentConfig().getTdb();
	TdbPublisher pub = tdb.getTdbPublisher(val);
	if (pub == null) {
	  return 0;
	}
	int res = 0;
	for (TdbTitle title : pub.getTdbTitles()) {
	  Plugin plug = null;
	  for (TdbAu tau : title.getTdbAus()) {
	    if (plug == null || !plug.getPluginId().equals(tau.getPluginId())) {
	      plug = pmgr.getPluginFromId(tau.getPluginId());
	    }
	    if (plug == null) {
	      continue;
	    }
	    // For now, find the TitleConfig from the plugin's name->tc map
	    // XXX not guaranteed to find the right TitleConfig if there's
	    // more than one with the same name (as in one set down,
	    // another not)
	    TitleConfig tc = plug.getTitleConfig(tau.getName());
	    if (tc == null) {
	      continue;
	    }
	    if (tc.isActionable(pmgr, action)) {
	      res++;
	    }
	  }
	}
	return res;
      }
      
      return super.countTitles(action);
    }

    public String toString() {
      StringBuilder sb = new StringBuilder(40);
      sb.append("[TS.Attr: ");
      sb.append(attr);
      sb.append(" = ");
      sb.append(val);
      sb.append("]");
      return sb.toString();
    }
  }

  // Identifier
  private static String IDENT = "[a-zA-Z0-9_$.]+";
  // String quoted with ' or "
  private static String STRVAL = "(?:(?:'([^']+)')|(?:\"([^\"]+)\"))";

  private static Pattern XPATH_PLUGIN =
    Pattern.compile("\\[pluginName=" + STRVAL + "\\]",
		    Pattern.CASE_INSENSITIVE);

  private static Pattern XPATH_ATTR =
    Pattern.compile("\\[attributes/(" + IDENT + ")=" + STRVAL + "\\]",
		    Pattern.CASE_INSENSITIVE);


  /** Factory method to create TitleSetXpath or an optimized version if
   * possible */
  public static TitleSetXpath create(LockssDaemon daemon,
				     String name,
				     String xpathPred) {

    Matcher mplug = XPATH_PLUGIN.matcher(xpathPred);
    if (mplug.matches()) {
      return new TSPlugin(daemon, name, xpathPred,
			  or(mplug.group(1), mplug.group(2)));
    }
    Matcher mattr = XPATH_ATTR.matcher(xpathPred);
    if (mattr.matches()) {
      return new TSAttr(daemon, name, xpathPred, mattr.group(1),
			or(mattr.group(2), mattr.group(3)));
    }
    return new TitleSetXpath(daemon, name, xpathPred);
  }

  private static String or(String s1, String s2) {
    if (s1 == null) return s2;
    return s1;
  }
}
