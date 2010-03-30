/*
 * $Id: ProjectMuseHtmlFilterFactory.java,v 1.3 2010-03-30 20:45:36 greya Exp $*/

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

package org.lockss.plugin.projmuse;

import java.io.*;

import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

public class ProjectMuseHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    HtmlTransform[] transforms = new HtmlTransform[] {
        // Filter out <div id="access_statement">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "access_statement")),
        
        // Filter out <div id="sidebar2">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "sidebar2")),         
        // Filter out <div id="credits">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "credits")),                                                                  
        // Filter out <div id="citationsblock">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "citationsblock")), 
    };
    
    // First filter with HtmlParser
    InputStream filteredStream = new HtmlFilterInputStream(in,
                                                           encoding,
                                                           new HtmlCompoundTransform(transforms));
    
    // Then filter with ProjectMuseFilterRule
    Reader reader = FilterUtil.getReader(filteredStream, encoding);
    Reader filtReader = ProjectMuseFilterRule.makeFilteredReader(reader);
    return new ReaderInputStream(filtReader);
  }

}
