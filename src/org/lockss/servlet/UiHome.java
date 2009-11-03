/*
 * $Id: UiHome.java,v 1.20.12.1 2009-11-03 23:44:52 edwardsb1 Exp $
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

import javax.servlet.*;
import java.io.*;
import java.util.Iterator;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.collections.iterators.ObjectArrayIterator;
import org.apache.commons.collections.iterators.TransformIterator;
import org.lockss.servlet.ServletUtil.LinkWithExplanation;
import org.mortbay.html.*;

/** UiHome servlet
 */
public class UiHome extends LockssServlet {

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
  }

  /** Handle a request */
  public void lockssHandleRequest() throws IOException {
    Page page = newPage();
    page.add(getHomeHeader());
    ServletUtil.layoutMenu(page, getDescriptors());
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  protected Table getHomeHeader() {
    Table tab = new Table(0, "align=\"center\" width=\"80%\"");
    tab.newRow();
    tab.newCell("align=\"center\"");
    tab.add("Welcome to the administration page for LOCKSS box <b>");
    tab.add(getMachineName());
    tab.add("</b>.");
    tab.newRow();
    tab.newCell("align=\"center\"");
    tab.add("&nbsp;");
    return tab;
  }

  protected Iterator getDescriptors() {
    // Iterate over the servlet descriptors...
    Iterator iterateOverDescr = new ObjectArrayIterator(getServletDescrs());

    // ...select those that appear in UiHome...
    Predicate selectUiHome = new Predicate() {
      public boolean evaluate(Object obj) {
        ServletDescr d = (ServletDescr)obj;
        return isServletDisplayed(d) && d.isInUiHome(UiHome.this);
      }
    };

    // ...and transform them into LinkWithExplanation pairs
    Transformer fromDescrToLink = new Transformer() {
      public Object transform(Object obj) {
        ServletDescr d = (ServletDescr)obj;
        return new LinkWithExplanation(srvLink(d, d.heading), d.getExplanation());
      }
    };

    return new TransformIterator(
        new FilterIterator(iterateOverDescr, selectUiHome),
        fromDescrToLink
    );
  }

}
