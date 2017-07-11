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

package org.lockss.plugin.atypon;

import java.io.IOException;

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
    //filter out comments
    HtmlNodeFilters.comment(),
    
    HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
    HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
    
    // sections that may show up with this skin - from CRAWL filter   
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumBookIssueNavigation"),
    // http://www.birpublications.org/toc/bjr/88/1052
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumMostReadWidget"),    
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumMostCitedWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div",  "class","literatumMostRecentWidget"),                                      
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumListOfIssuesWidget"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumBreadcrumbs"),
    //seen in TandF but likely to spread
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumArticleMetricsWidget"),
    // additional just for hashing - may or may not be necessary
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumAd"),
    //http://press.endocrine.org/doi/full/10.1210/en.2013-1159
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "doubleClickAdWidget"),    
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumInstitutionBanner"),    
    
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
  HtmlTransform xform_spanID = new HtmlTransform() {
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
  HtmlTransform xform_allIDs = new HtmlTransform() {
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
    InputStream combinedFiltered;
    if (doTagIDFiltering()) {
      combinedFiltered = new HtmlFilterInputStream(in, encoding,
          new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(bothFilters)), xform_allIDs));
    } else {
      combinedFiltered = new HtmlFilterInputStream(in, encoding,
          new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(bothFilters)), xform_spanID));
    }
    if (!doTagRemovalFiltering() && !doWS && !doHttpsConversion()) {
      // already done, return without converting back to a reader
      return combinedFiltered;
    }
    
    /* 
     * optional additional processing - 
     *    removal of all tags and/or removal of WS & https conversion
     */
    Reader tagFilter = FilterUtil.getReader(combinedFiltered, encoding);
    // if removing both tags and WS, add a space before each tag
    if (doTagRemovalFiltering() && doWS) {
      tagFilter = new StringFilter(FilterUtil.getReader(combinedFiltered, encoding), "<", " <");
    } 
    if (doTagRemovalFiltering()) {
      tagFilter = new HtmlTagFilter(tagFilter, new TagPair("<", ">"));
    }
    if (doHttpsConversion()) {
      tagFilter = new StringFilter(tagFilter, "http:", "https:");
    }
    if (doWS) {
      // first subsitute plain white space for &nbsp;   
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
            HtmlNodeFilterTransform.exclude(new OrFilter(allExcludeNodes)))
        );
    }
    // as Atyon publishers move to https this will support them. 
    // It doesn't matter if it changes http to http unnecessarily for hash purposes
    Reader freader;
    if (doHttpsConversion()) {
      freader = FilterUtil.getReader(combinedFiltered, encoding);
      freader = new StringFilter(freader, "http:", "https:");
    } else {
      freader = FilterUtil.getReader(combinedFiltered, encoding);
    }
    if (doWSFiltering()) {
      // first subsitute plain white space for &nbsp;                                                                                                  
      // add spaces before all "<"
      // consolidate spaces down to 1
      String[][] unifySpaces = new String[][] {
          // inconsistent use of nbsp v empty space - do this replacement first                                                                        
          {"&nbsp;", " "},
          {"<", " <"},
      };
      Reader NBSPFilter = StringFilter.makeNestedFilter(freader,
          unifySpaces, false);
      return new ReaderInputStream(new WhiteSpaceFilter(NBSPFilter)); 
    } else { 
      return new ReaderInputStream(freader);
    }
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


}
