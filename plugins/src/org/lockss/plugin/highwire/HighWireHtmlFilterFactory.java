/*
 * $Id: HighWireHtmlFilterFactory.java,v 1.2 2009-06-17 16:38:49 thib_gc Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.io.*;
import org.lockss.util.*;
import org.lockss.filter.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class HighWireHtmlFilterFactory implements FilterFactory {
  // Use the logic in HighWireFilterRule.  That class should be retired in
  // favor of this one once all running daemons support FilterFactory, at
  // which point the filter logic should be moved here.
  public InputStream createFilteredInputStream(ArchivalUnit au,
					       InputStream in,
					       String encoding) {
    HtmlTransform[] transforms = new HtmlTransform[] {
        // Filter out <li id="nav_current_issue">...</li>
        // Seen at Oxford University Press
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("li",
                                                                         "id",
                                                                         "nav_current_issue")),
    };

    // First filter with HtmlParser
    InputStream filtered = new HtmlFilterInputStream(in,
                                                     encoding,
                                                     new HtmlCompoundTransform(transforms));

    // Then filter with HighWireFilterRule
    Reader reader = FilterUtil.getReader(filtered, encoding);
    Reader filtReader = HighWireFilterRule.makeFilteredReader(reader);
    return new ReaderInputStream(filtReader);
  }

}

