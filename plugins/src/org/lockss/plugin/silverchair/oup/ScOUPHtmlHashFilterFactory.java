/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.oup;

import java.io.*;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class ScOUPHtmlHashFilterFactory implements FilterFactory {

  /*
   * AMA = American Medical Association (http://jamanetwork.com/)
   * SPIE = SPIE (http://spiedigitallibrary.org/)
   */
  private static final Logger log = Logger.getLogger(ScOUPHtmlHashFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(final ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {

    InputStream filtered = new HtmlFilterInputStream(
      in,
      encoding,
      new HtmlCompoundTransform(
    	  HtmlNodeFilterTransform.include(new OrFilter(new NodeFilter[] {
              HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-list-resources"),
              HtmlNodeFilters.tagWithAttributeRegex("div", "id", "resourceTypeList-OUP_Issue"),
              HtmlNodeFilters.tagWithAttributeRegex("div", "id", "ContentColumn"),
              HtmlNodeFilters.tagWithAttributeRegex("span", "class", "content-inner-wrap"),
              HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-body"),
          })),
      
    	  HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
    		  HtmlNodeFilters.tagWithAttributeRegex("a", "class", "comments"),
    	  }))
      )
    );
    
    Reader reader = FilterUtil.getReader(filtered, encoding);

    // Remove all inner tag content
    Reader noTagFilter = new HtmlTagFilter(new StringFilter(reader, "<", " <"), new TagPair("<", ">"));
    
    // Remove white space
    Reader whiteSpaceFilter = new WhiteSpaceFilter(noTagFilter);
    // All instances of "Systemic Infection" have been replaced with Sepsis on AMA
    InputStream ret =  new ReaderInputStream(new StringFilter(whiteSpaceFilter, "systemic infection", "sepsis"));
    return ret;
    // Instrumentation
  }
}
