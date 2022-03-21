
/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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

Except as contained in this notice, tMassachusettsMedicalSocietyHtmlFilterFactoryhe name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.ojs3;

import java.io.InputStream;


//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import org.apache.commons.io.IOUtils;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.filter.html.HtmlCompoundTransform;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import org.apache.commons.io.FileUtils;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;
import org.lockss.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/*
 * SO far we only have two publishers who have migrated to OJS3 and the html is basic and
 * very similar 
 */

public class Ojs3HtmlHashFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(Ojs3HtmlHashFilterFactory.class);

  private static final NodeFilter[] includeNodes = new NodeFilter[] {
	    	// manifest page
	    	HtmlNodeFilters.tagWithAttributeRegex("div",  "class", "page clockss"),
            HtmlNodeFilters.tagWithAttributeRegex("div",  "class", "page lockss"),
	        // toc - contents only
	        HtmlNodeFilters.tagWithAttribute("div", "class", "issue-toc"),
	        // abstract landing page html
	        HtmlNodeFilters.tagWithAttribute("article", "class", "article-details"),
	        // pdf landing page - just get the header with title and link information
	        HtmlNodeFilters.tagWithAttribute("header", "class", "header_view"),
            // article page, https://journals.vgtu.lt/index.php/BME/article/view/10292

            HtmlNodeFilters.tagWithAttribute("h2", "class", "headings"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "authors"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "article_tab"),
            // article page: https://www.clei.org/cleiej/index.php/cleiej/article/view/202
            HtmlNodeFilters.tagWithAttribute("div", "class", "main_entry"),

            // article page: 	https://haematologica.org/article/view/10276
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-container"),
            // issue page: https://haematologica.org/issue/view/377
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "one-article-intoc"),
	    };

    private static final NodeFilter[] includeNodes2 = new NodeFilter[] {};

    private static final NodeFilter[] excludeNodes = new NodeFilter[] {
	        // on the article landing page - remove the bottom stuff
	        HtmlNodeFilters.tagWithAttribute("section","class","article-more-details"),

	    };

    private static final NodeFilter[] excludeNodes2 = new NodeFilter[] {};
 
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, 
      String encoding) {

      return new HtmlFilterInputStream(in,
            encoding,
            new HtmlCompoundTransform(
                HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
                HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes))
                ));
    
  }

}
