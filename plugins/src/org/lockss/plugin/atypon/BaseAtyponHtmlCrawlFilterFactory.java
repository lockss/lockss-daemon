/*
 * $Id$
 */

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

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

 */

package org.lockss.plugin.atypon;

import java.io.InputStream;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Div;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/**
 * BaseAtyponHtmlCrawlFilterFactory
 * The basic AtyponHtmlCrawlFilterFactory
 * Child plugins can extend this class and add publisher specific crawl filters,
 * if necessary.  Common crawl filters can be easily added and be available to 
 * children.  Otherwise, this can be used by child plugins if no other crawl filters needed
 * NOTE - we are trending to adding more here so children are protected 
 * automatically when they upgrade their html skins.
 * But be sure only for tags that are specific enough as to be quite unlikely 
 * to catch something else inadvertently
 *  */

public class BaseAtyponHtmlCrawlFilterFactory implements FilterFactory {
  protected static Logger log = Logger.getLogger(BaseAtyponHtmlCrawlFilterFactory.class);
  protected static final Pattern corrections = Pattern.compile("( |&nbsp;)*(Original Article|Corrigendum|Corrigenda|Correction(s)?|Errata|Erratum)( |&nbsp;)*", Pattern.CASE_INSENSITIVE);
  protected static NodeFilter[] baseAtyponFilters = new NodeFilter[] {
    
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "citedBySection"),
    // toc, abs, full, text and ref right column - most read 
    // toc - right column, current issue or book landing page
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumBookIssueNavigation"),

    // sections that may show up with this skin
    // http://www.birpublications.org/toc/bjr/88/1052
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumAd"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumMostReadWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumMostCitedWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumMostRecentWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumListOfIssuesWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumListOfIssuesResponsiveWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumOpenAccessWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumBreadcrumbs"),
    // seen in RSNA http://pubs.rsna.org/doi/abs/10.1148/rg.2016150233
    // right column, related articles and other links out within pub
    HtmlNodeFilters.tagWithAttributeRegex("div","class", "literatumRelatedArticles"),
    //see http://ascopubs.org/doi/abs/10.1200/jco.2008.26.15_suppl.1027 right column
    // we avoid the whole right column, but this seems like a good general catch
    HtmlNodeFilters.tagWithAttributeRegex("div","class", "relatedArticlesWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div","id", "relatedArticlesColumn"),
    // new for 2020? at least appears on https://ascopubs.org/doi/full/10.1200/JCO.2009.46.2473
    HtmlNodeFilters.tagWithAttributeRegex("div","class", "article-tools"),
    HtmlNodeFilters.tagWithAttribute("div","id", "TrendMD Widget"),
    // other trendmd id/classnames
    HtmlNodeFilters.tagWithAttribute("div","id", "trendmd-suggestions"),

    // Since overcrawling is a constant problem for Atypon, put common
    // next article-previous article link for safety; 
    // AIAA, AMetSoc, ASCE, Ammons, APHA, SEG,Siam, 
    HtmlNodeFilters.tagWithAttribute("a", "class", "articleToolsNav"),
    // BIR, Maney, Endocrine - also handles next/prev issue - also for issues
    HtmlNodeFilters.tagWithAttributeRegex("td", "class", "journalNavRightTd"),
    HtmlNodeFilters.tagWithAttributeRegex("td", "class", "journalNavLeftTd"),
    // BQ, BioOne, Edinburgh, futurescience, nrc
    //        all handle next/prev article link in plugin
    // T&F doesn't have prev/next article links

    // breadcrumb or other link back to TOC from article page
    // AMetSoc, Ammons, APHA, NRC,
    HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumbs"),
    // ASCE, BiR, Maney, SEG, SIAM, Endocrine 
    HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "^(linkList )?breadcrumbs$"),

    // on TOC next-prev issue
    // AIAA, AMetSoc, Ammons, APHA, 
    HtmlNodeFilters.tagWithAttribute("div", "id", "nextprev"),
    // ASCE, SEG, SIAM
    HtmlNodeFilters.tagWithAttribute("div", "id", "prevNextNav"),

    //on TOC left column with listing of all volumes/issues
    HtmlNodeFilters.tagWithAttribute("ul", "class", "volumeIssues"),
    
    //have started finding cases of direct in-publication links within references
    // there are a variety of ways these blocks are identified, but 
    // these are unlikely to be used anywhere else so put in parent
    // emerald, AIAA
    // widen match for MarkAllen
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^references"),
    // ASCE
    HtmlNodeFilters.tagWithAttribute("li", "class", "reference"),
    // T&F: <ul class=\"references numeric-ordered-list\" id=\"references-Section\">
    HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "^references"),
    HtmlNodeFilters.tagWithAttributeRegex("ol", "class", "^references"),
    //maney, future-science
    HtmlNodeFilters.tagWithAttribute("table", "class", "references"),
    // Article landing - ajax tabs
    HtmlNodeFilters.tagWithAttribute("li", "id", "pane-pcw-references"),
    HtmlNodeFilters.tagWithAttribute("li", "id", "pane-pcw-related"),
    // References
    HtmlNodeFilters.tagWithAttributeRegex("li", "class", "references__item"),
    // more common tags
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "(altmetric|trendmd)", true),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(altmetric|trendmd)", true),
    
    // This used to be Regex but that was too broad and was a problem for Sage
    // Did an analysis and made it specific to the class
    // Publishers that need more will have to ask for it themselves
    HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttribute("div", "class", "articleTools"),
          HtmlNodeFilters.tagWithAttributeRegex(
                 "a", "href", "/action/showCitFormats\\?")),
    
    // related content from Related tab of Errata full text
    // http://press.endocrine.org/doi/full/10.1210/en.2013-1802
    HtmlNodeFilters.tagWithAttribute("div", "id", "relatedContent"),
    //http://www.tandfonline.com/doi/full/10.1080/02678373.2015.1004225
    HtmlNodeFilters.tagWithAttribute("div", "id", "metrics-content"),
    // toc - erratum section linking to Original Article - other flavor
    // related content near Erratum
    // http://press.endocrine.org/toc/endo/154/10       
    HtmlNodeFilters.tagWithAttribute("div", "class", "relatedLayer"),
    
    // Not all Atypon plugins necessarily need this but MANY do and it is
    // an insidious source of over crawling
    // 12/13/17 - adding limitation to be "match" not "find" since a book title
    // such as "Evaluating Corrections" would get excluded!
    new NodeFilter() {
      @Override public boolean accept(Node node) {
        if (!(node instanceof LinkTag)) return false;
        String allText = ((CompositeTag)node).toPlainTextString();
        return corrections.matcher(allText).matches();
      }
    },
    // there is a 'box' embedded in article for asco (may be others) that includes only a few aarticles outside the AU
    // and is difficult to pin down as the id, and class name are not unique
    // it seems that div.id = 'b1' is always a 'The Bottom Line' section, if it is present. But ...
    // this box is not always present. So we need to filter on the wording title itself 'Related ASCO Guidelines'
    // on this page is an example: https://ascopubs.org/doi/full/10.1200/JCO.20.02672
    //  <div id="b2" class="boxed-text-anchor">
    //    <div class="NLM_sec NLM_sec_level_2">
    //      <div class="head-b">Related ASCO Guidelines</div>
    // This page uses id=b1 for the box, as it is missing a 'The Bottom Line' section
    // https://ascopubs.org/doi/full/10.1200/JCO.20.00611
    // these filters would be nice, but would not catch all instances
    // HtmlNodeFilters.tagWithAttributeRegex("div","id", "b2"), // this is not guaranteed
    // <div class="boxed-text-float" >
    // <div class="boxed-text-anchor">   is used in older articles
    // e.g.   https://ascopubs.org/doi/10.1200/JCO.2016.70.1474
    // so lets put it all together
    new NodeFilter() {
      @Override public boolean accept(Node node) {
        if (HtmlNodeFilters.tagWithAttributeRegex("div", "class", "boxed-text-(float|anchor)").accept(node)) {
          log.debug2("found tag btfa: " + node.toPlainTextString());
          Node firstChild = node.getFirstChild();
          if (firstChild != null && firstChild instanceof Div) {
            // case the firstChild to a Div type
            Div firstDiv = (Div)firstChild;
            Node secondChild = firstDiv.getFirstChild();
            if (secondChild != null && secondChild instanceof Div) {
              Div secondDiv = (Div)secondChild;
              // this title seems regular, however, the capitalization is not. additionally, there is sometimes a ':' at the
              // end, i don't think this matters, just noting the inconsistency
              Pattern titlePattern = Pattern.compile("Related ASCO Guidelines", Pattern.CASE_INSENSITIVE);
              // this method returns the inner text, e.g. <div class="head-b">Related ASCO Guidelines</div>
              // becomes 'Related ASCO Guidelines'
              String tagText = secondDiv.toPlainTextString();
              log.debug2("related guidelines? " + tagText);
              return titlePattern.matcher(tagText).find();
            }
          }
        }
        return false;
      }
    },
  };

  /** Create an array of NodeFilters that combines the atyponBaseFilters with
   *  the given array
   *  @param nodes The array of NodeFilters to add
   */
  private NodeFilter[] addTo(NodeFilter[] nodes) {
    NodeFilter[] result  = Arrays.copyOf(baseAtyponFilters, baseAtyponFilters.length + nodes.length);
    System.arraycopy(nodes, 0, result, baseAtyponFilters.length, nodes.length);
    return result;
  }
  
  /** Create a FilteredInputStream that excludes the the atyponBaseFilters
   * @param au  The archival unit
   * @param in  Incoming input stream
   * @param encoding  The encoding
   */
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{

    HtmlFilterInputStream hfis =  new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(baseAtyponFilters)));
    // to handle errors like java.io.IOException: org.htmlparser.util.EncodingChangeException:
    // Unable to sync new encoding within range of +/- 100 chars
    // Allows the default of 100 to be overridden in tdb
    if (au != null) {
      TdbAu tdbau = au.getTdbAu();
      if (tdbau != null) {
        String range = tdbau.getAttr("EncodingMatchRange");
        if (range != null && !range.isEmpty()) {
          hfis.setEncodingMatchRange(Integer.parseInt(range));
          log.debug3("Set setEncodingMatchRange: " + range);
        }
      } else {log.debug("tdbau was null");}
    } else {log.warning("au was null");}
    return hfis;
  }
  
  /** Create a FilteredInputStream that excludes the the atyponBaseFilters and
   * moreNodes
   * @param au  The archival unit
   * @param in  Incoming input stream
   * @param encoding  The encoding
   * @param moreNodes An array of NodeFilters to be excluded with atyponBaseFilters
   */ 
  public InputStream createFilteredInputStream(ArchivalUnit au,
              InputStream in, String encoding, NodeFilter[] moreNodes) 
    throws PluginException {
    NodeFilter[] bothFilters = addTo(moreNodes);
    HtmlFilterInputStream hfis =  new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(bothFilters)));
    // to handle errors like java.io.IOException: org.htmlparser.util.EncodingChangeException:
    // Unable to sync new encoding within range of +/- 100 chars
    // Allows the default of 100 to be overridden in tdb
    if (au != null) {
      TdbAu tdbau = au.getTdbAu();
      if (tdbau != null) {
        String range = tdbau.getAttr("EncodingMatchRange");
        if (range != null && !range.isEmpty()) {
          hfis.setEncodingMatchRange(Integer.parseInt(range));
          log.debug3("Set setEncodingMatchRange: " + range);
        }
      } else {log.debug("tdbau was null");}
    } else {log.warning("au was null");}
    return hfis;
  }
}
