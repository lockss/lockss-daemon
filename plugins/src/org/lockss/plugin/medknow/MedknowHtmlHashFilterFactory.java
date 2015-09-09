/**
 * $Id$
 */

/**

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.medknow;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * Medknow html is very non-descriptive so pursuing a very minimalist approach
 * INCLUDE - only the very basic informative bits
 *    TOC - just the section headers, the article titles and author/doi information
 *           not including the links to formats of the article 
 *           not including the TOC information block
 *    ABS - just the
 *    FULL TEXT
 *    CITATION LANDING
 * EXCLUDE - from that informative set, exclude everything not-content
 */
public class MedknowHtmlHashFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(MedknowHtmlHashFilterFactory.class);

  @Override
  public InputStream createFilteredInputStream(final ArchivalUnit au,
      InputStream in,
      String encoding)
          throws PluginException {

    HtmlFilterInputStream filtered = new HtmlFilterInputStream(
        in,
        encoding,
        new HtmlCompoundTransform(
            /*
             * KEEP: throw out everything but main content areas
             */
            HtmlNodeFilterTransform.include(new OrFilter(new NodeFilter[] {
                // KEEP toc section titles [TOC]
                HtmlNodeFilters.tagWithAttribute("td", "class", "tochead"),
                // KEEP each toc article titles [TOC]
                HtmlNodeFilters.tagWithAttribute("td", "class", "articleTitle"),
                // KEEP each toc article author/doi info [TOC]
                HtmlNodeFilters.tagWithAttribute("td", "class", "sAuthor"),
                // abstract page is harder - we have to take in <td class="articlepage" to get the content but must exclude LOTS
                // KEEP page content portion of article page [abs, full, citation landing]
                HtmlNodeFilters.tagWithAttribute("td", "class", "articlepage"),
            })),
            /*
             * DROP: filter remaining content areas
             */
            HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
                // DROP scripts, styles, comments
                HtmlNodeFilters.tag("script"),
                HtmlNodeFilters.tag("noscript"),
                HtmlNodeFilters.tag("style"),
                HtmlNodeFilters.comment(),
                // DROP glyph for "Popular" articles [TOC]
                HtmlNodeFilters.tagWithAttributeRegex("img", "alt", "Highly accessed"),
                HtmlNodeFilters.tagWithAttributeRegex("a", "href", "showstats\\.asp"),
                // DROP Author affiliation - it could change over time [abs,full]
                HtmlNodeFilters.tagWithAttribute("font", "class", "AuthorAff"),
                // DROP ticker across top of page [abs,full]
                HtmlNodeFilters.tagWithAttribute("div", "id", "ticker"),
                // DROP social media toolbox [abs,full]
                HtmlNodeFilters.tagWithAttributeRegex("div", "class", "addthis_toolbox"),
                // DROP ad at top of article [abs,full]
                HtmlNodeFilters.tagWithAttribute("div", "id", "g8"),
                // DROP ad at bottom of article [abs,full]
                HtmlNodeFilters.tagWithAttribute("div", "id", "g9"),
                // DROP citation section at bottom of article [abs,full]
                HtmlNodeFilters.tagWithAttribute("table", "class", "sitethis"),
                // DROP a big chunk in order to get rid of cited-by counts that exist
                // in images.  This also takes out the "correspondence address" through
                // the doi but it gets the job done - and the doi is also on the TOC
                HtmlNodeFilters.tagWithAttribute("font", "class", "CorrsAdd"),

            }))
            )
        );

    return filtered;

  }

  /*
  public static void main(String[] args) throws Exception {
    for (String file : Arrays.asList("/tmp/jpmg/toc1",
                "/tmp/jpmg/toc2",
                "/tmp/jpmg/abs1",
                "/tmp/jpmg/abs2",
                "/tmp/jpmg/full1",
                "/tmp/jpmg/full2",
                "/tmp/jpmg/cit2",
                "/tmp/jpmg/cit1")) {
      IOUtils.copy(new MedknowHtmlHashFilterFactory().createFilteredInputStream(null, new FileInputStream(file), null),
                   new FileOutputStream(file + ".out"));
    }
  }
  */

}

