/*
 * $Id: HispanicIssuesOnLineHtmlFilterFactory.java,v 1.2 2008-01-23 08:08:13 thib_gc Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.hispanicissuesonline;

import java.io.*;
import java.util.List;

import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class HispanicIssuesOnLineHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    List pairs = ListUtil.list(new HtmlTagFilter.TagPair(// Filter between "<!-- BEGIN PARENT LINKS -->"...
                                                         "<!-- BEGIN PARENT LINKS -->",
                                                         // ...and "<!-- END PARENT LINKS -->"
                                                         "<!-- END PARENT LINKS -->"),
                               new HtmlTagFilter.TagPair(// Filter between "<!-- BEGIN U of M TEMPLATE HEADER -->"...
                                                         "<!-- BEGIN U of M TEMPLATE HEADER -->",
                                                         // ...and "<!-- END U of M TEMPLATE HEADER -->"
                                                         "<!-- END U of M TEMPLATE HEADER -->"),
                               new HtmlTagFilter.TagPair(// Filter between "<!-- BEGIN U of M TEMPLATE FOOTER -->"...
                                                         "<!-- BEGIN U of M TEMPLATE FOOTER -->",
                                                         // ...and "<!-- END U of M TEMPLATE FOOTER -->"
                                                         "<!-- END U of M TEMPLATE FOOTER -->"));
    Reader reader = FilterUtil.getReader(in, encoding);
    Reader pairFilter = HtmlTagFilter.makeNestedFilter(reader, pairs);
    return new ReaderInputStream(pairFilter);
  }
}
