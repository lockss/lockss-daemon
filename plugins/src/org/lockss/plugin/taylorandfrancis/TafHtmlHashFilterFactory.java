/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.taylorandfrancis;

import java.io.*;
import java.util.regex.Pattern;

import org.apache.commons.io.input.CountingInputStream;
import org.htmlparser.*;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.tags.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

import javax.swing.text.html.HTML;

/**
 * This filter will eventually replace
 * {@link TaylorAndFrancisHtmlHashFilterFactory}.
 */
public class TafHtmlHashFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(TafHtmlHashFilterFactory.class);
  
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
            // KEEP top part of main content area [TOC, abs, full, ref]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "overview"),
            // KEEP each article block [TOC]
            //need to keep the second \\b so we don't pick up articleMetrics, or articleTools
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "\\barticle\\b[^-_]"), // avoid match on pageArticle, article-card
            // KEEP abstract [abs, full, ref]
            /* removing this, it is too broad the string 'abstract' occurs in many div classes
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "abstract"),
             */
            // KEEP active content area [abs, full, ref, suppl]
            HtmlNodeFilters.tagWithAttribute("div", "id", "informationPanel"), // article info [abs]
            HtmlNodeFilters.tagWithAttribute("div", "id", "fulltextPanel"), // full text [full]
            HtmlNodeFilters.tagWithAttribute("div", "id", "referencesPanel"), // references [ref]
            HtmlNodeFilters.tagWithAttribute("div", "id", "supplementaryPanel"), // supplementary materials [suppl]
            //HtmlNodeFilters.tagWithAttribute("div", "class", "figuresContent"), //doi/figures/...
            // KEEP citation format form [showCitFormats]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "citationContainer"),
            //HtmlNodeFilters.tagWithAttributeRegex("div", "class", "citationFormats"),
            // KEEP popup window content area [showPopup]
            HtmlNodeFilters.tagWithAttribute("body", "class", "popupBody"),
            // New skin 2017 - re-examining all aspects from scratch
            // TOC (now Regex, to catch class= with multiple entries
            HtmlNodeFilters.tagWithAttributeRegex("div", "class","tocArticleEntry"),
            // Abstract
            // Full text html - used doi/(full|abs)/10.1080/01650424.2016.1167222
            /* Do not include this anymore, we will grab title from elsewhere -- markom April 6, 2021
            HtmlNodeFilters.tagWithAttribute("div", "class","publicationContentTitle"),

            HtmlNodeFilters.tagWithAttributeRegex("div", "class","abstractSection "),
            */
            // likewise, this is too broad, grab the child divs instead, doing this lower in '//new content...' section
//            HtmlNodeFilters.tagWithAttribute("div", "class","hlFld-Fulltext"),
            // Figures page (may or may not have contents
            HtmlNodeFilters.tagWithAttribute("div","class","figuresContent"),
            // showCitFormats form page 
            HtmlNodeFilters.tagWithAttribute("div","class","downloadCitation"),
            // an article with suppl and in-line video plus zip
            //doi/suppl/10.1080/11263504.2013.877535
            //and one with multiple downloadable files
            //doi/suppl/10.1080/1070289X.2013.822381
            HtmlNodeFilters.tagWithAttribute("div","class", "supplemental-material-container"),

            // older content that we need on the ingest machines
            //HtmlNodeFilters.allExceptSubtree(
            //    HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "references"),
            //    HtmlNodeFilters.tag("strong")
            //),

            // new content that we need to include to compare with older content, also, just like to keep this content
            //HtmlNodeFilters.tagWithAttribute("div", "id", "references-Section"),
              // grab the title of the article (note, the span tag, the div tag is not in the 'correct' order
            HtmlNodeFilters.tagWithAttributeRegex("span","class","hlFld-title"),
              // grab the author names, but exclude the email and affiliation
//            HtmlNodeFilters.allExceptSubtree(
//                HtmlNodeFilters.tagWithAttribute("a","class","entryAuthor"),
//                HtmlNodeFilters.tag("span")
//            ),
            HtmlNodeFilters.tagWithAttribute("div", "class","hlFld-Abstract"),
            HtmlNodeFilters.tagWithAttribute("div", "class","hlFld-KeywordText"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class","NLM_sec_level_1"),
            // keep Ack[nowledgments] section
            // Note, sometimes the acknowledgemetns does not have this div
            // and is instead in a more generic NLM_sec_level_1 class
            HtmlNodeFilters.tagWithAttribute("div", "id", "ack"),

            // we get rid of all tags at the end so won't keep links unless explicitly
            // included here
            // This includes the links on a manifest page
            new NodeFilter() {
              @Override
              public boolean accept(Node node) {
                if (node instanceof LinkTag) {
                  String link = ((LinkTag) node).getAttribute("href");
                  if(link != null && !link.isEmpty() && link.matches("^(https?://[^/]+)?/toc/[^/]+/[^/]+/[^/]+/?$")) {
                    Node parent = node.getParent().getParent();
                    if(parent instanceof BulletList) {
                      if(parent.getParent() instanceof BodyTag) {
                        return true;
                      }
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
            // DROP social media bar [overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "social"),
            // DROP access box (changes e.g. when the article becomes free) [article block, abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "accessmodule"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "access"), // formerly by itself
            // DROP number of article views [article block, abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "articleUsage"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumArticleMetricsWidget"),
            // DROP "Related articles" variants [article block, abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("a", "class", "relatedLink"), // old?
            HtmlNodeFilters.tagWithAttributeRegex("li", "class", "relatedArticleLink"), // [article block]
            HtmlNodeFilters.tagWithText("h3", "Related articles"), // [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("a", "class", "searchRelatedLink"), // [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "combinedRecommendationsWidget"), // all - "People also read"
            // DROP title options (e.g. 'Publication History', 'Sample this title') [TOC overview]
            HtmlNodeFilters.tagWithAttribute("div", "class", "options"),
            // DROP title icons (e.g. 'Routledge Open Select') [TOC overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journalOverviewAds"),
            // DROP book review subtitle (added later)
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "subtitle"), // [TOC] e.g. http://www.tandfonline.com/toc/wtsw20/33/1)
            // ...placeholder for [abs/full/ref/suppl overview] e.g. http://www.tandfonline.com/doi/full/10.1080/08841233.2013.751003
            // DROP Google Translate artifacts [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttribute("div", "id", "google_translate_element"), // current
            HtmlNodeFilters.tagWithAttribute("div", "id", "goog-gt-tt"), // old
            HtmlNodeFilters.tagWithText("a", "Translator disclaimer"),
            HtmlNodeFilters.tagWithText("a", "Translator&nbsp;disclaimer"),
            // DROP "Alert me" variants [abs/full/ref overview]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "alertDiv"), // current
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "alertDiv"), // old
            // DROP "Publishing models and article dates explained" link [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("a", "href", "models-and-dates-explained"),
            // DROP article dates which sometimes get fixed later [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttribute("div", "class", "articleDates"),
            // DROP subtitle for journal section/subject (added later) [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttribute("span", "class", "subj-group"),
            // DROP non-access box article links (e.g. "View full text"->"Full text HTML") [abs/full/ref/suppl overview]
            HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "top_article_links"),
            // DROP outgoing links and SFX links [article block, full, ref, probably showPopup]
            HtmlNodeFilters.allExceptSubtree(HtmlNodeFilters.tagWithAttribute("span", "class", "referenceDiv"),
                                             HtmlNodeFilters.tagWithAttribute("a", "class", "dropDownLabel")), // popup at each inline citation [full]
            HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/servlet/linkout\\?"), // [article block, full/ref referencesPanel]
            HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"), // [article block, full/ref referencesPanel]
            HtmlNodeFilters.tagWithAttributeRegex("a", "href", "javascript:newWindow\\('http://dx.doi.org/"), // [showPopup, probably article block, full/ref referencesPanel]
            // DROP "Jump to section" popup menus [full]
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "summationNavigation"),
            HtmlNodeFilters.tagWithAttributeRegex("a", "title", "(Next|Previous) issue"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "breadcrumb"),
            //descriptive text that often changes
            HtmlNodeFilters.tagWithAttribute("td", "class", "note"),
            
            // New skin 2017 - exclusion based on new includes
            // the following are brought in by regex "\\barticle[^-]"
            // which needs to stay in for old/gln content...work around
            //HtmlNodeFilters.tagWithAttribute("div", "class","articleMetricsContainer"), // brought in by the regex "\\barticle[^-]"
            //HtmlNodeFilters.tagWithAttribute("div", "class", "articleTools"),
            //TOC
            HtmlNodeFilters.tagWithAttribute("div", "class", "sfxLinkButton"),
            //Abstract
            //Full
            HtmlNodeFilters.tagWithAttribute("span", "class","ref-lnk"), //in-line rollover ref info
            //Figures
            //showCit
            
            /* 
             * Noticed changes in 10/2/2018 - views counts on TOC
             */
            HtmlNodeFilters.tagWithAttributeRegex("span", "class", "access-icon"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "metrics-panel"),

            // older content that we do not need on the ingest machines
            HtmlNodeFilters.allExceptSubtree(
              HtmlNodeFilters.tag("h3"),
              // in new content there is a table caption embedded in an h3 tag. wild
              HtmlNodeFilters.tag("p")
            ),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "summationHeading"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "sectionHeadingDiv"),
            HtmlNodeFilters.tagWithAttribute("ul", "class", "references"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "pageRange"),
//            HtmlNodeFilters.allExceptSubtree(
                HtmlNodeFilters.tagWithAttribute("div", "class", "doiMeta clear"),
//                HtmlNodeFilters.tagWithAttribute("span", "class", "hlFld-ContribAuthor")
//            ),
            // title and author info is embedded in the figures and tables (but with differing format, exclude
            // figureViewerArticleInfo, tableViewerArticleInfo, for the new content this is a class name
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "ViewerArticleInfo" ),
            // old crawls have the tables at the bottom.
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "table-content-" ),

            // this is for both new and old crawls,
            // figureDownloadOption tableDownloadOption
            // old crawls have 'PowerPoint...' new crawls 'Display...'
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "DownloadOption" ),

            // newer content that we need to exclude to agree with older content
            // We would like to keep the references, but the new theme does not embed the list numbers while the old theme does0
            // HtmlNodeFilters.tagWithAttribute("span", "class", "googleScholar-container"),
            HtmlNodeFilters.tagWithAttributeRegex("h2", "id", "."),
            HtmlNodeFilters.tagWithAttributeRegex("h2", "class", "."),
            HtmlNodeFilters.tagWithAttribute("p", "class", "kwd-title"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "ViewerArticleInfo" ),

            new NodeFilter() {
              @Override
              public boolean accept(Node node) {
                if (node instanceof Span) {
                  Span span = ((Span) node);
                  for(Node child:span.getChildrenAsNodeArray()) {
                    if (child != null && child instanceof LinkTag) {
                      String title = ((LinkTag) child).getAttribute("title");
                      if (title != null && !title.isEmpty() && title.contains("Previous issue")) {
                        return true;
                      }
                    }
                  }
                }
                return false;
              }
            },
            
            new NodeFilter() {
              @Override
              public boolean accept(Node node) {
                if (node instanceof Div) {
                  Div div = ((Div) node);
                  String divClass = div.getAttribute("class");
                  if(divClass != null && !divClass.isEmpty() && divClass.contains("right")) {
                    Node parent = div.getParent();
                    if (parent != null && parent instanceof Div) {
                      String parentClass = ((Div) parent).getAttribute("class");
                        if (parentClass != null && !parentClass.isEmpty() && parentClass.contains("bodyFooterContent")) {
                          return true;
                        }
                      }
                  }
                }
                return false;
              }
            }
        }))
      )
    );
    
    
    Reader reader = FilterUtil.getReader(filtered, encoding);

    LineRewritingReader rewritingReader = new LineRewritingReader(reader) {
      @Override
      public String rewriteLine(String line) {
        // Markup changes over time [anywhere]
        line = PAT_NBSP.matcher(line).replaceAll(REP_NBSP);
        line = PAT_AMP.matcher(line).replaceAll(REP_AMP);
        line = PAT_PUNCTUATION.matcher(line).replaceAll(REP_PUNCTUATION); // e.g. \(, \-, during encoding glitch (or similar)
        // Alternate forms of citation links [article block]
        line = PAT_CITING_ARTICLES.matcher(line).replaceAll(REP_CITING_ARTICLES);
        // Wording change over time, and publication dates get fixed much later [article block, abs/full/ref/suppl overview]
        // For older versions with plain text instead of <div class="articleDates">
        line = PAT_PUBLISHED_ONLINE.matcher(line).replaceAll(REP_PUBLISHED_ONLINE);
        // Leftover commas after outgoing/SFX links removed [full/ref referencesPanel]
        line = PAT_PUB_ID.matcher(line).replaceAll(REP_PUB_ID);
        return line;
      }
    };

    // Remove all inner tag content
    Reader noTagFilter = new HtmlTagFilter(new StringFilter(rewritingReader, "<", " <"), new TagPair("<", ">"));
    
    // Remove white space
    Reader noWhiteSpace = new WhiteSpaceFilter(noTagFilter);
    
    InputStream ret = new ReaderInputStream(noWhiteSpace);

    // Instrumentation
    return new CountingInputStream(ret) {
      @Override
      public void close() throws IOException {
        long bytes = getByteCount();
        if (bytes <= 100L) {
          log.debug(String.format("%d byte%s in %s", bytes, bytes == 1L ? "" : "s", au.getName()));
        }
        if (log.isDebug2()) {
          log.debug2(String.format("%d byte%s in %s", bytes, bytes == 1L ? "" : "s", au.getName()));
        }
        super.close();
      }
    };
  }
  
  public static final String EMPTY_STRING = "";

  public static final Pattern PAT_NBSP = Pattern.compile("&nbsp;", Pattern.CASE_INSENSITIVE);
  public static final String REP_NBSP = " ";
  
  public static final Pattern PAT_AMP = Pattern.compile("&amp;", Pattern.CASE_INSENSITIVE);
  public static final String REP_AMP = "&";
  
  public static final Pattern PAT_PUNCTUATION = Pattern.compile("[,\\\\]", Pattern.CASE_INSENSITIVE);
  public static final String REP_PUNCTUATION = EMPTY_STRING;
  
  public static final Pattern PAT_CITING_ARTICLES = Pattern.compile("<li>(<div>)?(<strong>)?(Citing Articles:|Citations:|Citation information:|<a href=\"/doi/citedby/).*?</li>", Pattern.CASE_INSENSITIVE); 
  public static final String REP_CITING_ARTICLES = EMPTY_STRING;
  
  public static final Pattern PAT_PUBLISHED_ONLINE = Pattern.compile("(<(b|h[23456])>)?(Published online:|Available online:|Version of record first published:)(</\\2>)?.*?>", Pattern.CASE_INSENSITIVE);
  public static final String REP_PUBLISHED_ONLINE = EMPTY_STRING;
  
  public static final Pattern PAT_PUB_ID = Pattern.compile("</pub-id>.*?</li>", Pattern.CASE_INSENSITIVE); 
  public static final String REP_PUB_ID = EMPTY_STRING;

}
