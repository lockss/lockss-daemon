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

import java.io.InputStream;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableTag;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
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
 *    MANIFEST (backIssues.asp) includes all issus for all volumes and will increase
 *             over time, so just include those "<td> bits that have the correct
 *             volume 
 * EXCLUDE - from that informative set, exclude everything not-content
 */
public class MedknowHtmlHashFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(MedknowHtmlHashFilterFactory.class);

  @Override
  public InputStream createFilteredInputStream(final ArchivalUnit au,
      InputStream in,
      String encoding)
          throws PluginException {
    Configuration auConfig = au.getConfiguration();
    String AuVol = auConfig.get(ConfigParamDescr.VOLUME_NAME.getKey());
    String AuIssn = auConfig.get(ConfigParamDescr.JOURNAL_ISSN.getKey());
    final Pattern THIS_VOL_ISSN_PAT = Pattern.compile(String.format("showBackIssue\\.asp\\?issn=%s;year=[0-9]{4};volume=%s;",AuIssn, AuVol),Pattern.CASE_INSENSITIVE);
    HtmlFilterInputStream filtered = new HtmlFilterInputStream(
        in,
        encoding,
        new HtmlCompoundTransform(
            /*
             * KEEP: throw out everything but main content areas
             * examples to look at -
             * toc page
             *   http://www.japtr.org/showbackIssue.asp?issn=2231-4040;year=2013;volume=4;issue=1 
             * full-text page
             *   http://www.japtr.org/article.asp?issn=2231-4040;year=2013;volume=4;issue=1;spage=4;epage=8;aulast=Chauhan
             *   
             * This is quite tricky because very little of the html is usefully labeled
             * On the TOC, just get the article titles, authors and doi information
             * On the article page, just get the main content box
             *   being careful to differentiate the same name table on the TOC
             *   And then within the article page, remove pieces that are going to change over time (citation information, etc)
             */
            HtmlNodeFilterTransform.include(new OrFilter(new NodeFilter[] {
                // KEEP toc section titles [TOC]
                HtmlNodeFilters.tagWithAttribute("td", "class", "tochead"),
                // KEEP each toc article titles [TOC]
                HtmlNodeFilters.tagWithAttribute("td", "class", "articleTitle"),
                // KEEP each toc article author/doi info [TOC]
                HtmlNodeFilters.tagWithAttribute("td", "class", "sAuthor"),
                // we have to include this one for the article pages
                // but it means lots of exclusions in the next filter)
                // [ABSTRACT,FULL - special case for main article content
                // http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2015;volume=61;issue=1;spage=15;epage=20;aulast=Shevra;type=0
                // jpgmonline uses <td class="articlepage", not <table as the others do
                // the [TOC] still uses the <table tag as the other journals do
                HtmlNodeFilters.tagWithAttribute("td",  "class", "articlepage"),
                // [ABSTRACT,FULL, TOC]
                // we have to take in <table class="articlepage" to get the content 
                // but don't want this table on the TOC
                // identify the TOC version by finding the access policy text
                new NodeFilter() {
                  @Override
                  public boolean accept(Node node) {
                    if (node instanceof TableTag) {
                      String tclass = ((TableTag) node).getAttribute("class");
                      if(tclass != null && !tclass.isEmpty() && "articlepage".equals(tclass)) {
                        String longContents = ((TableTag)node).getStringText();
                        // the PDF access policy is stated on TOC
                        if (!(longContents.toLowerCase().contains("pdf access policy"))) {
                          return true;
                        }
                      }
                    } else if (node instanceof LinkTag) {
                      String title = ((LinkTag) node).getAttribute("title");
                      if(title != null && "Table of Contents".equalsIgnoreCase(title)) {
                        // Is the link for this journal & volume
                        String href = ((LinkTag) node).getAttribute("href");
                        if (THIS_VOL_ISSN_PAT.matcher(href).find()) {
                          return true; 
                        }
                      }
                    }
                    return false;
                  }
                },
                
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
                // do NOT take TOC per-article link sections - variable over time

            }))
            )
        );

    return filtered;

  }

}

