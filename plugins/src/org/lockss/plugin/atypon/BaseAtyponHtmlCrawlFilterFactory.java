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

package org.lockss.plugin.atypon;

import java.io.InputStream;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
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
    // this used to be a simpler Regex, but was omitting all article from Taylor & Francis, so checking for
    // tocArticleEntry is necessary.
    // new for 2020? at least appears on https://ascopubs.org/doi/full/10.1200/JCO.2009.46.2473
    HtmlNodeFilters.tagWithAttributeRegex("div","class", "^(?!.*tocArticleEntry).*article-tools"),
    HtmlNodeFilters.tagWithAttributeRegex("div","id", "TrendMD Widget"),
    // other trendmd id/classnames
    HtmlNodeFilters.tagWithAttributeRegex("div","id", "trendmd-suggestions"),
    
    // Since overcrawling is a constant problem for Atypon, put common
    // next article-previous article link for safety; 
    // AIAA, AMetSoc, ASCE, Ammons, APHA, SEG,Siam, 
    HtmlNodeFilters.tagWithAttributeRegex("a", "class", "articleToolsNav"),
    // BIR, Maney, Endocrine - also handles next/prev issue - also for issues
    HtmlNodeFilters.tagWithAttributeRegex("td", "class", "journalNavRightTd"),
    HtmlNodeFilters.tagWithAttributeRegex("td", "class", "journalNavLeftTd"),
    // BQ, BioOne, Edinburgh, futurescience, nrc
    //        all handle next/prev article link in plugin
    // T&F doesn't have prev/next article links

    // breadcrumb or other link back to TOC from article page
    // AMetSoc, Ammons, APHA, NRC,
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "breadcrumbs"),
    // ASCE, BiR, Maney, SEG, SIAM, Endocrine 
    HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "^(linkList )?breadcrumbs$"),

    // on TOC next-prev issue
    // AIAA, AMetSoc, Ammons, APHA, 
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "nextprev"),
    // ASCE, SEG, SIAM
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "prevNextNav"),

    //on TOC left column with listing of all volumes/issues
    HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "volumeIssues"),
    
    //have started finding cases of direct in-publication links within references
    // there are a variety of ways these blocks are identified, but 
    // these are unlikely to be used anywhere else so put in parent
    // emerald, AIAA
    // widen match for MarkAllen
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^references"),
    // ASCE
    HtmlNodeFilters.tagWithAttributeRegex("li", "class", "reference"),
    // T&F: <ul class=\"references numeric-ordered-list\" id=\"references-Section\">
    HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "^references"),
    HtmlNodeFilters.tagWithAttributeRegex("ol", "class", "^references"),
    //maney, future-science
    HtmlNodeFilters.tagWithAttributeRegex("table", "class", "references"),
    // Article landing - ajax tabs
    HtmlNodeFilters.tagWithAttributeRegex("li", "id", "pane-pcw-references"),
          //https://www.acpjournals.org/doi/10.7326/L19-0549 leads to overcrawl: https://www.acpjournals.org/doi/10.7326/0003-4819-154-9-201105030-00002
    HtmlNodeFilters.tagWithAttributeRegex("li", "id", "pane-pcw-related"),
          //x-lockss-referrer: 	https://www.acpjournals.org/doi/10.7326/M19-1539 leads overcrawl: https://www.acpjournals.org/doi/10.7326/L19-0596
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "pane-pcw-related"),
          
    // References
    HtmlNodeFilters.tagWithAttributeRegex("li", "class", "references__item"),
    // more common tags
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "(altmetric|trendmd)", true),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(altmetric|trendmd)", true),

    // This used to be Regex but that was too broad and was a problem for Sage
    // Did an analysis and made it specific to the class
    // Publishers that need more will have to ask for it themselves
    HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "articleTools"),
          HtmlNodeFilters.tagWithAttributeRegex(
                 "a", "href", "/action/showCitFormats\\?")),
    
    // related content from Related tab of Errata full text
    // http://press.endocrine.org/doi/full/10.1210/en.2013-1802
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "relatedContent"),
    //http://www.tandfonline.com/doi/full/10.1080/02678373.2015.1004225
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "metrics-content"),
    // toc - erratum section linking to Original Article - other flavor
    // related content near Erratum
    // http://press.endocrine.org/toc/endo/154/10       
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "relatedLayer"),

    // ASM related section
    HtmlNodeFilters.tagWithAttributeRegex("section", "class", "related-articles"),


    // Links to "Prev" & "Next" at Atypon Seg on March/2021 at: https://library.seg.org/toc/leedff/21/9
    // Also the issues and other top navigation will go to other volumes
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-navigation"), 
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-navigation"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-navigation__btn--pre"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-navigation__extra"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "content-navigation__btn--next"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publication__menu"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "generic-menu"),

    // Right menu for related articles, figure and other stuff
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "pane-pcw-figures"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "pane-pcw-references"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "pane-pcw-related"),
    // Can not filter our all div#id="pane-pcw-details", since PDF links are in inside
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "cover-details"),
    HtmlNodeFilters.tagWithAttributeRegex("section", "class", "copywrites"),
    HtmlNodeFilters.tagWithAttributeRegex("section", "class", "publisher"),
    HtmlNodeFilters.tagWithAttributeRegex("section", "class", "article__history"),
    
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
