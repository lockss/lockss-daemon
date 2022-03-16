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

import java.io.IOException;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.filters.*;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.Html;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

/**
 * BaseAtyponHtmlHashFilterFactory
 * The basic AtyponHtmlHashFilterFactory
 * Child plugins can extend this class and add publisher specific hash filters,
 * if necessary.  Common hashes can be easily added and be available to children.
 * Otherwise, this can be used by child plugins if no other hash filters are 
 * needed.
 */

public class BaseAtyponHtmlHashFilterFactory implements FilterFactory {
  private static final Logger log = Logger.getLogger(BaseAtyponHtmlHashFilterFactory.class);
  // String used to see if text matches a size description of an article
  // usually "PDF Plus (527 KB)" or similar (PDFPlus, PDF-Plus)
  // (?i) makes it case insensitive
  private static final String SIZE_REGEX = "PDF(\\s|-)?(Plus)?\\s?\\(\\s?[0-9]+";
  private static final Pattern SIZE_PATTERN = Pattern.compile(SIZE_REGEX, Pattern.CASE_INSENSITIVE);
  protected static final Pattern CITED_BY_PATTERN =
      Pattern.compile("Cited by", Pattern.CASE_INSENSITIVE);
  

  protected static NodeFilter[] baseAtyponFilters = new NodeFilter[] {
    // 7/22/2013 starting to use a more aggressive hashing policy-
    // these are on both issue and article pages
    // leave only the content
    HtmlNodeFilters.tag("head"),
    // filter out javascript
    HtmlNodeFilters.tag("script"),
    HtmlNodeFilters.tag("noscript"),
    //filter out comments after everything else so child plugins can use for filtering
    //HtmlNodeFilters.comment(),
    HtmlNodeFilters.tag("style"),
    
    HtmlNodeFilters.tag("header"),
    HtmlNodeFilters.tag("footer"),
    HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
    HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
    // pageHeader/Footer
    HtmlNodeFilters.tagWithAttribute("div", "id", "pageHeader"),
    HtmlNodeFilters.tagWithAttribute("div", "id", "pageFooter"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "pageHeader"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "pageFooter"),

    // login etc popups
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "username-popup"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "registration-popup"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "change-password-drawer"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "login-popup"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "request-reset-password-drawer"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "verify-phone-drawer"),
    
    HtmlNodeFilters.tag("nav"),
    // more common tags
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "(altmetric|trendmd)", true),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "(altmetric|trendmd)", true),
    // search bar
    HtmlNodeFilters.tagWithAttribute("div", "id", "search-overlay"),
    
    // sections that may show up with this skin - from CRAWL filter
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumBookIssueNavigation"),
    // http://www.birpublications.org/toc/bjr/88/1052
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumMostReadWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumMostCitedWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumMostRecentWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumListOfIssuesWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumListOfIssuesResponsiveWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumBreadcrumbs"),
    //seen in TandF but likely to spread
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumArticleMetricsWidget"),
    // additional just for hashing - may or may not be necessary
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumAd"),
    //http://press.endocrine.org/doi/full/10.1210/en.2013-1159
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "doubleClickAdWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumInstitutionBanner"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumContentItemDownloadCount"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "data-widget-def", "literatumContentItemDownloadCount"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "articleCount"),
    
    // crossref to site library
    HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"),
    // stylesheets
    HtmlNodeFilters.tagWithAttribute("link", "rel", "stylesheet"),
    // these are only on issue toc pages
    HtmlNodeFilters.tagWithAttributeRegex("img", "class", "^accessIcon"),
    // first seen in Edinburgh august 2016 toc
    HtmlNodeFilters.tagWithAttribute("td", "class", "accessIconContainer"),
    // Contains the changeable list of citations
    HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),
    // some size notes are within an identifying span
    // (see future science on an article page
    HtmlNodeFilters.tagWithAttribute("span", "class", "fileSize"),
    
    // toc - select pulldown menu under volume title
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publicationToolContainer"),
    // on article page
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "articleMetaDrop"),
    // listed author links href= a varying numletter sequence
    HtmlNodeFilters.tagWithAttributeRegex("a", "class", "tooltipTrigger"), 
    HtmlNodeFilters.tagWithAttribute("div", "class", "ui-helper-hidden-accessible"),
    // invisible jump to form whose choice labels have changed
    HtmlNodeFilters.tagWithAttribute("div", "class", "sectionJumpTo"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "references"),
    // we don't need to leave in the showCitFormats part of this for hashing
    HtmlNodeFilters.tagWithAttribute("div", "class", "articleTools"),
    // toc - article type seems to change and this isn't important
    HtmlNodeFilters.tagWithAttribute("span", "class", "ArticleType"),
    // abs (some Sage Atypon) - table of references on abs page
    HtmlNodeFilters.tagWithAttribute("table", "class", "references"),
    HtmlNodeFilters.tagWithAttribute("div", "id", "relatedContent"),
    // related for Asm pages
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "related-tabs"),

    //  showPopup&citid=citart1 for when the email address is not available?
    HtmlNodeFilters.tagWithAttributeRegex("a", "href", "email-protection"),
    // sometimes the a tag has the appropriate email address, yet the inner span has the protected hash
    HtmlNodeFilters.tagWithAttributeRegex("span", "class", "cf_email"),

    // <span class="video-source" style="display:none">https://thoracic-prod-streaming.literatumonline.com/journals/content/annalsats/2019/annalsats.2019.16.issue-12/annalsats.201905-414cc/20191115/media/annalsats.201905-414cc_vid1.,964,750,300,180,.mp4.m3u8?b92b4ad1b4f274c70877528314abb28bd3f723a7d6082e6507476c036d1b3402e209f95f47cb691aca526557783e82bc64ff0999d3d535157ece591a7960e52d0ad6ff2e906196e220cb93f961768e02064b91a1ad9c7348821c98f7acc9bd5e389723630f66ab576db0f419f0c939f58d827bfa2eac7787d4b56d13de187b3827fc74a9d5fbda90a8b17c06c05d2720b3f7c0d3e1346cc83905b6bb1906c3b9d888e9193497328183834474e8c05f9b2eee691ed114090d8fb9bb9bea87d9b35ba05edca8b3b902 </span>
    HtmlNodeFilters.tagWithAttribute("span", "class", "video-source"),

    HtmlNodeFilters.tagWithAttribute("table", "class", "loginForm"),

    // bottom of page has a whole "pill" section for metrics, shares, etc.
    // remove parent div.id=pill, rather than remove each sub div.id
    //  pill-info-authors pill-metrics-citations pill-share pill-purchase-access pill-view-options
    //  pill-tables pill-media-other pill-references
    HtmlNodeFilters.tagWithAttribute("div", "id", "pill"),

    // A number of children add a link item "Cited By" only after the article
    // has been cited...remove the entire list item - look for text pattern
    new NodeFilter() {
      @Override public boolean accept(Node node) {
        if (!(node instanceof Bullet)) return false;
        String allText = ((CompositeTag)node).toPlainTextString();
        return CITED_BY_PATTERN.matcher(allText).find();
      }
    },
    
  };

  //And while we're visiting the tag, also remove data-request-id from html tag
  protected final HtmlTransform xform_spanID = new HtmlTransform() {
    //; The "id" attribute of <span> tags can have a gensym
    @Override
    public NodeList transform(NodeList nodeList) throws IOException {
      try {
        nodeList.visitAllNodesWith(new NodeVisitor() {
          @Override
          public void visitTag(Tag tag) {
            if (tag instanceof Span && tag.getAttribute("id") != null) {
              tag.removeAttribute("id");
              // some size notes are just text children of the link tag
              // eg <a ..> PDF Plus (123 kb)</a>
            } else if (tag instanceof Html && tag.getAttribute("data-request-id") != null) {
              //changeable html attribute data-request-id first seen in BiR, see all html
              tag.removeAttribute("data-request-id");
            } else if
            (tag instanceof LinkTag && ((CompositeTag) tag).getChildCount() == 1 &&
            ((CompositeTag) tag).getFirstChild() instanceof Text) {
              if (SIZE_PATTERN.matcher(((CompositeTag) tag).getStringText()).find()) {
                log.debug3("removing link child text : " + ((CompositeTag) tag).getStringText());
                ((CompositeTag) tag).removeChild(0);
              }
            }
          }
        });
      } catch (ParserException pe) {
        IOException ioe = new IOException();
        ioe.initCause(pe);
        throw ioe;
      }
      return nodeList;
    }
  };

  /*
   * Removes all "id' attributes and/or white space if activated by
   * child plugins.  The "id" attribute of various tags <span>, <section> ..
   * can have a gensym. Also removes pdf(plus) file sizes.
   */
  //And while we're visiting the tag, also remove data-request-id from html tag
  protected final HtmlTransform xform_allIDs = new HtmlTransform() {
    @Override
    public NodeList transform(NodeList nodeList) throws IOException {
      try {
        nodeList.visitAllNodesWith(new NodeVisitor() {
          @Override
          public void visitTag(Tag tag) {
            if (tag.getAttribute("id") != null) {
              tag.removeAttribute("id");
            }
            //changeable html attribute data-request-id first seen in BiR, see all html
            //<html lang="en" class="pb-page" data-request-id="cc2ac1e1-80e0-472e-acc8-2e97853c3c01" >
            if (tag instanceof Html && tag.getAttribute("data-request-id") != null) {
              tag.removeAttribute("data-request-id");
            } else if (tag instanceof LinkTag && ((CompositeTag) tag).getChildCount() == 1 &&
                ((CompositeTag) tag).getFirstChild() instanceof Text) {
              if (SIZE_PATTERN.matcher(((CompositeTag) tag).getStringText()).find()) {
                log.debug3("removing link child text : " + ((CompositeTag) tag).getStringText());
                ((CompositeTag) tag).removeChild(0);
              }
            }
          }
        });
      } catch (ParserException pe) {
        IOException ioe = new IOException();
        ioe.initCause(pe);
        throw ioe;
      }
      return nodeList;
    }
  };

  /** Create an array of NodeFilters that combines the atyponBaseFilters with
   *  the given array
   *  @param nodes The array of NodeFilters to add
   */
  protected NodeFilter[] addTo(NodeFilter[] nodes) {
    NodeFilter[] result  = Arrays.copyOf(baseAtyponFilters, baseAtyponFilters.length + nodes.length);
    System.arraycopy(nodes, 0, result, baseAtyponFilters.length, nodes.length);
    return result;
  }

  /** Create a FilteredInputStream that excludes only the atyponBaseFilters
   * @param au  The archival unit
   * @param in  Incoming input stream
   * @param encoding  The encoding
   */
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) {

    return doFiltering(in, encoding, null, doWSFiltering());
  }

  /** Create a FilteredInputStream that excludes the the atyponBaseFilters and
   * moreNodes
   * @param au  The archival unit
   * @param in  Incoming input stream
   * @param encoding  The encoding
   * @param moreNodes An array of NodeFilters to be excluded with atyponBaseFilters
   */
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding, NodeFilter[] moreNodes) {

    return doFiltering(in, encoding, moreNodes, doWSFiltering());
  }

  /* the shared portion of the filtering
   * pick up the extra nodes from the child if there are any
   * Use the correct xform, depending on the return value of the getter.
   *
   */
  private InputStream doFiltering(InputStream in, String encoding, NodeFilter[] moreNodes, boolean doWS) {
    NodeFilter[] bothFilters = baseAtyponFilters;
    if (moreNodes != null) {
      bothFilters = addTo(moreNodes);
    }

    // Remove script tags that can confuse the parser with " and ' out of match order (even if escaped)
    in = new BufferedInputStream(new ReaderInputStream(
        new HtmlTagFilter(FilterUtil.getReader(in, encoding), new TagPair("<script","</script>"))));

    InputStream combinedFiltered;
    if (doTagIDFiltering()) {
      combinedFiltered = new HtmlFilterInputStream(in, encoding,
          new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(bothFilters)), xform_allIDs));
    } else {
      combinedFiltered = new HtmlFilterInputStream(in, encoding,
          new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(bothFilters)), xform_spanID));
    }

    return commonFiltering(combinedFiltered, encoding, doWS);
  }

  protected InputStream commonFiltering(InputStream combinedFiltered, String encoding, boolean doWS) {

    // a little inefficient but pull out comments after the child nodes are applied
    combinedFiltered = filterComments(combinedFiltered,encoding);

    if (!doTagRemovalFiltering() && !doWS && !doHttpsConversion()) {
      // already done, return without converting back to a reader
      return combinedFiltered;
    }

    /*
     * optional additional processing -
     *    removal of all tags and/or removal of WS & https conversion
     */
    Reader tagFilter;
    
    // if removing both tags and WS, add a space before each tag
    if (doTagRemovalFiltering() && doWS) {
      tagFilter = new StringFilter(FilterUtil.getReader(combinedFiltered, encoding), "<", " <");
    } else {
      tagFilter = FilterUtil.getReader(combinedFiltered, encoding);
    }
    
    if (doTagRemovalFiltering()) {
      tagFilter = new HtmlTagFilter(tagFilter, new TagPair("<", ">"));
    }
    
    // as Atypon publishers move to https this will support them.
    // It doesn't matter if it changes http to http unnecessarily for hash purposes
    if (doHttpsConversion()) {
      tagFilter = new StringFilter(tagFilter, "http:", "https:");
    }
    
    if (doWS) {
      // first substitute plain white space for &nbsp;
      // add spaces before all "<"
      // consolidate spaces down to 1
      // If the tags were removed above (with space added) it will not find tags
      // to add the space before. This is fine.
      String[][] unifySpaces = new String[][] {
          // inconsistent use of nbsp v empty space - do this replacement first
          {"&nbsp;", " "},
          {"<", " <"},
      };
      Reader NBSPFilter = StringFilter.makeNestedFilter(tagFilter,
          unifySpaces, false);
      return new ReaderInputStream(new WhiteSpaceFilter(NBSPFilter));
    } else {
      return new ReaderInputStream(tagFilter);
    }
  }
  
  /*
   * Takes include and exclude nodes as input. Removes all "id' attributes
   * and/or white space if activated by child plugins.  The "id" attribute of
   * various tags <span>, <section> .. can have a gensym.
   * Also removes pdf(plus) file sizes.
   */
  public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in,
      String encoding, NodeFilter[] includeNodes, NodeFilter[] excludeNodes) {
    NodeFilter[] allExcludeNodes = baseAtyponFilters;
    if (excludeNodes == null) {
      throw new NullPointerException("excludeNodes array is null");
    }
    if (includeNodes == null) {
      throw new NullPointerException("includeNodes array is null!");
    }
    // combine baseAtyponFilters and excludeNodes
    allExcludeNodes = addTo(excludeNodes);
    
    // Remove script tags that can confuse the parser with " and ' out of match order (even if escaped)
    in = new BufferedInputStream(new ReaderInputStream(
        new HtmlTagFilter(FilterUtil.getReader(in, encoding), new TagPair("<script","</script>"))));

    InputStream combinedFiltered;
    // xform_allIDs filters out all "id" attributes and
    // also removes pdf(plus) file sizes
    if (doTagIDFiltering()) {
      combinedFiltered = new HtmlFilterInputStream(in, encoding,
        new HtmlCompoundTransform(
            HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
            HtmlNodeFilterTransform.exclude(new OrFilter(allExcludeNodes)),
            xform_allIDs)
        );
    } else {
      combinedFiltered = new HtmlFilterInputStream(in, encoding,
        new HtmlCompoundTransform(
            HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
            HtmlNodeFilterTransform.exclude(new OrFilter(allExcludeNodes)),
            xform_spanID)
        );
    }
    return commonFiltering(combinedFiltered, encoding, doWSFiltering());
  }

  /** Create a FilteredInputStream that excludes the the atyponBaseFilters and
   * moreNodes as specified in the params, also do a WhiteSpace filter if boolean set
   * @param au  The archival unit
   * @param in  Incoming input stream
   * @param encoding  The encoding
   * @param moreNodes An array of NodeFilters to be excluded with atyponBaseFilters
   * @param doWS A boolean to indicate if returned stream should also have  white spaces consolidated
   * 
   * @deprecated Use {@link #createFilteredInputStream(ArchivalUnit, InputStream, String, NodeFilter[])}
   *  and override {@link #doWSFiltering()} to return the desired value for
   *  <code>doWS</code> instead.
   */
  @Deprecated
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding, NodeFilter[] moreNodes, boolean doWS) {

    return doFiltering(in, encoding, moreNodes, doWS);
  }

  /*
   * BaseAtypon children can turn on/off extra levels of filtering
   * by overriding the getting/setter methods.
   * The BaseAtypon filter will query this method and if it is true,
   * will remove the values associated with all "id" attributes on all tags
   * after using the node filters that rely on id attributes.
   * That is, <div id="foo" class="blah"> will become <div class="blah">
   * default is false;
   */
  public boolean doTagIDFiltering() {
    return false;
  }

  /*
   * BaseAtypon children can turn on/off extra levels of filtering
   * by overriding the getting/setter methods.
   * The BaseAtypon filter will query this method and if it is true,
   * will remove extra WS, 
   * Specifically &nbsp; becomes ascii space, multiple spaces become one space
   * and all "<" have a leading space before them.
   * default is false;
   */
  public boolean doWSFiltering() {
    return false;
  }
  
  /*
   * BaseAtypon children can turn on/off extra levels of filtering
   * by overriding the getting/setter methods.
   * The BaseAtypon filter will query this method and if it is true,
   * will remove all html tags after using the tags to identify other nodes
   * to remove.  That is, this
   *   <div id=...>does not remove text </div>
   * becomes
   *   does not remove text
   * If whitespace filtering is turned on, we guarantee a space between.
   * default is false;
   */
  public boolean doTagRemovalFiltering() {
    return false;
  }
  
  /*
   * BaseAtypon children can turn on/off extra levels of filtering
   * by overriding the getting/setter methods.
   * The BaseAtypon filter will query this method and if it is true,
   * will turn all http to https as part of handling an http to https conversion
   */
  public boolean doHttpsConversion() {
    return false;
  }
  
  public InputStream filterComments(InputStream in,  String encoding) {
    return new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.comment()));
  }

}
