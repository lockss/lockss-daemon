/*
 * $Id: TitleSetXpath.java,v 1.1 2005-01-04 02:58:13 tlipkis Exp $
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
import org.apache.commons.jxpath.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * A set of titles that match an XPath predicate */
public class TitleSetXpath extends BaseTitleSet {

  // static context used to predefine RE class functions, and to compile
  // xpath in constructor
  private static JXPathContext sharedContext = JXPathContext.newContext(null);
  static {
    sharedContext.setFunctions(new ClassFunctions(RegexpUtil.class, "RE"));
  };

  private String xpath;
  private CompiledExpression expr;

  /** Create a TitleSet that consists of all known titles whose TitleConfig
   * matches the supplied xpath predicate.  In addition to the standard
   * XPath functions, the extension <code>RE:isMatchRe(<i>string</i>,
   * <i>regexp</i>)</code> performs a regexp match against the string.
   * @param daemon used to get list of all known titles
   * @param xpathPred an XPath predicate (<i>eg</i>,  <code>[journalTitle='Dog Journal']</code> )
   */
  public TitleSetXpath(LockssDaemon daemon, String name, String xpathPred) {
    super(daemon, name);
    if (!(xpathPred.startsWith("[") && xpathPred.endsWith("]"))) {
      throw new IllegalArgumentException("XPath predicate must be enclosed in \"[\" ... \"]\"");
    }
    xpath = "." + xpathPred;
    expr = sharedContext.compile(xpath);
  }

  /** Filter a collection of titles by the xpath predicate
   * @param allTitles collection of titles to be filtered
   * @return a collection of TitleConfig
   */
  Collection getTitles(Collection allTitles) {
    JXPathContext context = JXPathContext.newContext(sharedContext, allTitles);
    return selectNodes(expr, context);
  }

  List selectNodes(CompiledExpression expr, JXPathContext context) {
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

  protected int getMajorOrder() {
    return 2;
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
    StringBuffer sb = new StringBuffer(40);
    sb.append("[XPath: ");
    sb.append(xpath);
    sb.append("]");
    return sb.toString();
  }
}
