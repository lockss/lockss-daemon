/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair;

import java.io.*;
import java.util.Arrays;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.htmlparser.Attribute;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class BaseScHtmlHashFilterFactory implements FilterFactory {

  /*
   * ASH - American Speech Language, Hearing (1 ex: http://lshss.pubs.asha.org/)
   * AMA = American Medical Association (http://jamanetwork.com/) - deprecated!
   *    new ama plugin uses it's own filters
   * SPIE = SPIE (http://spiedigitallibrary.org/) - deprecated
   *    we no longer support SPIE preservation
   *    
   */
  private static final Logger log = Logger.getLogger(BaseScHtmlHashFilterFactory.class);
  
  // HTML transform to remove generated attributes like <a href="...?Expires">
  protected static HtmlTransform xform = new HtmlTransform() {
    @Override
    public NodeList transform(NodeList nodeList) throws IOException {
      try {
        nodeList.visitAllNodesWith(new NodeVisitor() {
          @Override
          public void visitTag(Tag tag) {
            String tagName = tag.getTagName().toLowerCase();
            try {
              if ("a".equals(tagName) ||
                  "div".equals(tagName) ||
                  "img".equals(tagName)) {
                Attribute a = tag.getAttributeEx(tagName);
                Vector<Attribute> v = new Vector<Attribute>();
                v.add(a);
                if (tag.isEmptyXmlTag()) {
                  Attribute end = tag.getAttributeEx("/");
                  v.add(end);
                }
                tag.setAttributesEx(v);
              }
              super.visitTag(tag);
            }
            catch (Exception exc) {
              log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
            }
          }
        });
      }
      catch (ParserException pe) {
        log.debug2("Internal error (parser)", pe); // Bail
      }
      return nodeList;
    }
  };
  
  protected static NodeFilter[] baseSCExcludeFilters = new NodeFilter[] {
      // DROP scripts, styles, comments
      HtmlNodeFilters.tag("script"),
      HtmlNodeFilters.tag("noscript"),
      HtmlNodeFilters.tag("style"),
      HtmlNodeFilters.comment(),
      HtmlNodeFilters.tagWithAttributeRegex("a", "class", "comment"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "comment"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "graphic-wrap"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toolbar-wrap"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^ref-list"),
      // DROP eventual "Free"/"Open Access" text/icon [AMA/SPIE TOC/article]
      HtmlNodeFilters.tagWithAttributeRegex("span", "class", "freeArticle"), // [AMA TOC/article]
      HtmlNodeFilters.tagWithText("h4", "Open Access"), // [SPIE TOC/article]
      HtmlNodeFilters.tagWithAttributeRegex("i", "class", "icon-availability"),
      // DROP RSS and e-mail alert buttons [AMA/SPIE TOC]
      HtmlNodeFilters.tagWithAttribute("div", "class", "subscribe"),
      // DROP expand/collapse buttons [AMA/SPIE TOC]
      HtmlNodeFilters.tagWithAttribute("div", "class", "expandCollapse"),
      // DROP previous/next article link text [AMA/SPIE TOC/article]
      // (also in crawl filter)
      HtmlNodeFilters.tagWithAttribute("a", "class", "prev"),
      HtmlNodeFilters.tagWithAttribute("a", "class", "next"),
      // DROP designated separator
      // [AMA article]: vertical bar in breadcrumb
      // [SPIE article]: semicolon between authors
      HtmlNodeFilters.tagWithAttribute("span", "class", "separator"),
      // DROP text size picker [AMA/SPIE article]
      HtmlNodeFilters.tagWithAttribute("div", "class", "textSize"),
      // DROP internal jump links [AMA/SPIE article]
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "contentJumpLinks"),
      // DROP sections appended to end of main area [AMA/SPIE article]
      HtmlNodeFilters.tagWithAttribute("div", "id", "divSignInSubscriptionUpsell"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "relatedArticlesMobile"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "collectionsMobile"),
      // DROP parts of figures/tables other than captions [AMA/SPIE article/figures/tables]
      HtmlNodeFilters.allExceptSubtree(HtmlNodeFilters.tagWithAttributeRegex("div", "class", "figureSection"),
          new OrFilter(HtmlNodeFilters.tagWithAttributeRegex("h6", "class", "figureLabel"), // [AMA article/figures]
              HtmlNodeFilters.tagWithAttribute("div", "class", "figureCaption"))), // [AMA/SPIE article/figures]
      HtmlNodeFilters.allExceptSubtree(HtmlNodeFilters.tagWithAttributeRegex("div", "class", "tableSection"),
          new OrFilter(HtmlNodeFilters.tagWithAttribute("div", "class", "tableCaption"), // [SPIE article/tables]
              HtmlNodeFilters.tagWithAttributeRegex("span", "class", "^Table "))), // [AMA article/tables]
      HtmlNodeFilters.tagWithAttributeRegex("span", "class", "^Figure "), // freeform, e.g. http://jama.jamanetwork.com/article.aspx?articleid=1487499 [AMA]
      // DROP Letters(7), CME(8), Citing(9) & Responses(10) tabs and panels [AMA  article]
      // DROP Figures(3),Tables(4) because they are in random order when presented outside
      // of the Article(1) view
      // [SPIE]: has only (10) for now
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "tab(3|4|7|8|9|10)Div"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "id", "^tab(3|4|7|8|9|10)$"),
      // DROP external links in References [AMA/SPIE article/references]
      HtmlNodeFilters.tagWithAttributeRegex("span", "class", "pubmedLink"), // [AMA article/references]
      HtmlNodeFilters.tagWithAttributeRegex("span", "class", "crossrefDoi"), // [AMA/SPIE article/references]
      // First page preview sometimes appears, sometimes not [AMA article]
      HtmlNodeFilters.tagWithAttribute("div", "id", "divFirstPagePreview"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "adPortlet"), // rightside ads
      HtmlNodeFilters.tagWithAttribute("div", "class", "portletContentHolder"), // rightside
      // related content and metrics
      //figure links
      HtmlNodeFilters.tagWithAttribute("div", "class", "figurelinks"),
      //figures section changes
      HtmlNodeFilters.tagWithAttribute("div", "class", "abstractFigures"),
      // ASHA - additions
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-AltmetricLink"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-CitingArticles"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-ArticleLinks"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-Toolbox"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "access-state-logos"),
      HtmlNodeFilters.tagWithAttributeRegex("span", "class", "article-groups"),
      // copyright date did not have a tag suitable for filtering, using containing tag for ASHA
      HtmlNodeFilters.tagWithAttribute("div", "id", "getCitation"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "copyright"),
      // changeable
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-metadata"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "terms-wrapper"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "related.topic"),
  };
  
  @Override
  public InputStream createFilteredInputStream(final ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    NodeFilter[] includeFilters = new NodeFilter[] {
        // KEEP manifest page content [AMA, SPIE, ASH]
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "-IssuesAndVolumeListManifest"),
        // KEEP main area of TOCs [AMA, SPIE]
        HtmlNodeFilters.tagWithAttribute("div", "class", "articleBodyContainer"),
        // KEEP main area of article [AMA, SPIE]
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "contentColumn"),
        // KEEP proper citation of article [AMA]
        HtmlNodeFilters.tagWithAttribute("span", "class", "citationCopyPaste"),
        // KEEP proper citation of article [AMA]
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-section"),
        // KEEP items for ASHA which is using vanilla Silverchair
        // KEEP main area of TOC [ASHA]
        HtmlNodeFilters.tagWithAttribute("div", "id", "ArticleList"),
        // KEEP main area of article [ASHA - checked it was okay for others]
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "leftColumn"),
    };
    
    return createFilteredInputStream(au, in, encoding, includeFilters, null);
  }
  
  /*
   * Takes include and exclude nodes as input. Removes all "id' attributes
   * and/or white space if activated by child plugins.  The "id" attribute of
   * various tags <span>, <section> .. can have a gensym.
   * Also removes pdf(plus) file sizes.
   */
  protected InputStream createFilteredInputStream(final ArchivalUnit au, InputStream in, String encoding,
      NodeFilter[] includeNodes, NodeFilter[] moreExcludeNodes) {
    
    NodeFilter[] excludeNodes = baseSCExcludeFilters;
    if (moreExcludeNodes != null) {
      excludeNodes  = Arrays.copyOf(baseSCExcludeFilters, baseSCExcludeFilters.length + moreExcludeNodes.length);
      System.arraycopy(moreExcludeNodes, 0, excludeNodes, baseSCExcludeFilters.length, moreExcludeNodes.length);
    }

    /*
     * KEEP: throw out everything but main content areas
     * DROP: filter remaining content areas
     */
    HtmlCompoundTransform compoundTransform;
    if (doXForm()) {
      compoundTransform =
          new HtmlCompoundTransform(
              HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
              HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes)),
              xform);
    } else {
      compoundTransform =
          new HtmlCompoundTransform(
              HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
              HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes)));
    }

    InputStream filtered = new HtmlFilterInputStream(in, encoding, encoding, compoundTransform);
    Reader reader = FilterUtil.getReader(filtered, encoding);

    // Remove all inner tag content
    // on OUP in 2024, there are literal unicode replacement characters (U+FFFD) so we are filtering those out
    Reader noTagFilter = new HtmlTagFilter(new StringFilter(new StringFilter(reader,"\ufffd",""), "<", " <"), new TagPair("<", ">"));

    // Remove white space
    Reader whiteSpaceFilter = new WhiteSpaceFilter(noTagFilter);
    InputStream ret;

    if (doExtraSpecialFilter()) {
      // All instances of "Systemic Infection" have been replaced with Sepsis on AMA
      ret = new ReaderInputStream(new StringFilter(whiteSpaceFilter, "systemic infection", "sepsis"));
    } else {
      ret = new ReaderInputStream(whiteSpaceFilter);
    }
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
  
  /*
   * Silverchair children should turn off special conversion
   */
  protected boolean doExtraSpecialFilter() {
    return true;
  }
  
  protected boolean doXForm() {
    return false;
  }
  
}
