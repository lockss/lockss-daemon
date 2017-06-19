/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.*;

import org.apache.commons.jxpath.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.plugin.*;

/**
 * Matches an AU against an XPath predicate.  The object to which the XPath
 * is applied is a restricted view of an AU, with the attributes
 * <code>name</code>, <code>auid</code>, and </code>tdbAu</code>, from which
 * the AU's config params, tdb attributes, title, publisher, etc. may be
 * accessed.  Examples:
 * <ul>

 * <li><code>[name='AuName']</code> - True if the AU name is "AuName"</li>
 * <li><code>[year >= 1996 and year <= 2005]</code> - True if the year (see
 * below) is between 1996 and 2005</li>
 * <li><code>[RE:isMatchRe(tdbAu/journalTitle, '^JAMA')]</code> - True if the journal title starts with "JAMA"</li>
 * <li><code>[RE:isMatchRe(tdbAu/params/base_url, '\.univ\.edu/(path1|path2)')]</code> - True if the base_url config param matches the regexp</li>
 * </ul><br>
 * Expressions may access the tdb attribute and AU config param maps,
 * various scalar data and a number of convenience accessors.  For a
 * complete list see the getters in {@link org.lockss.config.TdbAu}.

 * <ul>
 * <li><code>name</code> - AU name</li>
 * <li><code>auid</code> - AUID</li>
 * <li><code>tdbAu/name</code> - AU name in tdb</li>
 * <li><code>tdbAu/params/<i>param_name</i></code> - named AU config parameter</li>
 * <li><code>tdbAu/attrs/<i>attr_name</i></code> - named tdb attribute</li>
 * <li><code>tdbAu/year</code> - The <code>year</code> attribute if present, else the <code>year</code> AU config param, else null</li>
 * <li><code>tdbAu/journalTitle</code></li>
 * <li><code>tdbAu/publisherName</code></li>
 * <li><code>tdbAu/publicationType</code></li>
 * <li><code>tdbAu/issn</code></li>
 * <li>...</li>
 * </ul>
 *
 * In addition, the following variables are available:<ul>
 * <li><code>$myhost</code> - the value of {@value
 * org.lockss.config.ConfigManager#PARAM_PLATFORM_FQDN}
 * </ul>
 */
public class AuXpathMatcher {
  private static final Logger log = Logger.getLogger(AuXpathMatcher.class);

  // static context used to predefine RE class functions, and to compile
  // xpath in constructor
  private static JXPathContext sharedContext = JXPathContext.newContext(null);
  static {
    sharedContext.setFunctions(new ClassFunctions(RegexpUtil.XpathUtil.class,
						  "RE"));
  }

  private String xpath;
  private CompiledExpression expr;

  /** Create an AuXpathMatcher that matches AUs against the supplied XPath
   * expression.  In addition to the standard XPath functions, the
   * extension <code>RE:isMatchRe(<i>string</i>, <i>regexp</i>)</code>
   * performs a regexp match against the string.
   * @param xpathPred an XPath predicate (<i>eg</i>,
   * <code>[journalTitle='Dog Journal']</code> )
   */
  AuXpathMatcher(String xpathPred) {
    if (!(xpathPred.startsWith("[") && xpathPred.endsWith("]"))) {
      throw new IllegalArgumentException("XPath predicate must be enclosed in \"[\" ... \"]\"");
    }
    xpath = xpathPred;
    expr = JXPathContext.compile("." + xpath);
  }

  /** Return true if the XPath predicate returns true when applied to the
   * AU */
  public boolean isMatch(ArchivalUnit au) {
    return isMatch(new AuXpathAccessor(au));
   }
 
  /** Return true if the XPath predicate returns true when applied to the
   * AU */
  public boolean isMatch(AuXpathAccessor auxa) {
    JXPathContext context = newContext(sharedContext, auxa);
    Iterator iter = expr.iteratePointers(context);
    return iter.hasNext();
  }

  private JXPathContext newContext(JXPathContext sharedContext,
				   AuXpathAccessor auxa) {
    JXPathContext context = JXPathContext.newContext(sharedContext, auxa);
    context.getVariables().declareVariable("myhost",
					   ConfigManager.getPlatformHostname());
    return context;
  }

  public String getXpath() {
    return xpath;
  }

  public boolean equals(Object o) {
    if (! (o instanceof AuXpathMatcher)) {
      return false;
    }
    AuXpathMatcher oset = (AuXpathMatcher)o;
    return xpath.equals(oset.getXpath());
  }

  public int hashCode() {
    int hash = 0x227035;
    hash += xpath.hashCode();
    return hash;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(40);
    sb.append("[AuXpath: ");
    sb.append(xpath);
    sb.append("]");
    return sb.toString();
  }

  /** Return the object located by the XPath.  Useful for debugging. */
  public Object eval(ArchivalUnit au) {
    return eval(new AuXpathAccessor(au));
   }
 
  /** Return the object located by the XPath.  Useful for debugging. */
  public Object eval(AuXpathAccessor auxa) {
    JXPathContext context = newContext(sharedContext, auxa);
    return context.getValue("" + xpath.substring(1, xpath.length()-1));
  }

  /** Factory method to create an AuXpathMatcher */
  public static AuXpathMatcher create(String xpathPred) {
    return new AuXpathMatcher(xpathPred);
  }

  /** Bean to provide access to a limited set of AU features, for XPath
   * matching */
  public static class AuXpathAccessor {
    private ArchivalUnit au;

    public AuXpathAccessor(ArchivalUnit au) {
      this.au = au;
    }

    /** Return the tdb, from which */
    public TdbAu getTdbAu() {
      return au.getTdbAu();
    }

    public String getAuId() {
      return au.getAuId();
    }

    public String getPluginId() {
      return au.getPluginId();
    }

    public String getName() {
      return au.getName();
    }

    public ArchivalUnit getAu() {
      return au;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder(40);
      sb.append("[AuA: ");
      sb.append(au.toString());
      sb.append("]");
      return sb.toString();
    }
  }
}
