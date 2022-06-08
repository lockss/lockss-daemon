/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.medknow;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.filters.*;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

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
    final Pattern ONLINE_ACCESSED_PAT = Pattern.compile("Online since .{1,99}?Accessed [0-9,.]{1,99} times?", Pattern.DOTALL);
    final Pattern PMID_PAT = Pattern.compile(":?[0-9]{0,10}");
    
    HtmlFilterInputStream filtered = new HtmlFilterInputStream(
        in,
        encoding,
        new HtmlCompoundTransform(
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
                        // except like http://www.ejo.eg.net/showBackIssue.asp?issn=1012-5574;year=2012;volume=28;issue=1
                        if (!(longContents.toLowerCase().contains("pdf access policy") || 
                              ONLINE_ACCESSED_PAT.matcher(longContents).find()))
                        {
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

            }))),
            
        new HtmlCompoundTransform(
            new HtmlTransform() {
              @Override
              public NodeList transform(NodeList nodeList) throws IOException {
                // <td class="sAuthor" style="line-height:18px;">
                // <b>PMID</b> :24891795
                try {
                  nodeList.visitAllNodesWith(new NodeVisitor() {
                    @Override
                    public void visitTag(Tag tag) {
                      if ("td".equals(tag.getTagName().toLowerCase()) &&
                          "sAuthor".equals(tag.getAttribute("class"))) {
                          boolean p1 = false;
                          NodeList nl = tag.getChildren();
                          if (nl == null) return;
                          for (int sx = 0; sx < nl.size(); sx++) {
                            Node snode = nl.elementAt(sx);
                            String xmin = snode.getText();
                            if (snode instanceof Text &&
                                ("PMID".equals(xmin) ||
                                 (p1 && PMID_PAT.matcher(xmin).find()))) {
                              p1 = true;
                              nl.remove(sx);
                            }
                          }
                        }
                      }
                    });
                }
                catch (ParserException pe) {
                  throw new IOException(pe);
                }
                return nodeList;
              }
            },
            // convert all remaining nodes to plaintext nodes
            new HtmlTransform() {
              @Override
              public NodeList transform(NodeList nodeList) throws IOException {
                NodeList nl = new NodeList();
                for (int sx = 0; sx < nodeList.size(); sx++) {
                  Node snode = nodeList.elementAt(sx);
                  // Add a space for case where to separate nodes,
                  // required where showstats is filtered but extra NL remained and caused diff
                  TextNode tn = new TextNode(snode.toPlainTextString() + " ");
                  nl.add(tn);
                }
                return nl;
              }
            }
            ))
        );
    
    Reader reader = FilterUtil.getReader(filtered, encoding);
    // first substitute plain white space for &nbsp;
    String[][] unifySpaces = new String[][] { 
        // inconsistent use of nbsp v empty space - do this replacement first
        {"&nbsp;", " "}, 
    };
    Reader NBSPFilter = StringFilter.makeNestedFilter(reader,
        unifySpaces, false);   

    //now consolidate white space
    Reader WSReader = new WhiteSpaceFilter(NBSPFilter);
    return new ReaderInputStream(WSReader);

  }

}

