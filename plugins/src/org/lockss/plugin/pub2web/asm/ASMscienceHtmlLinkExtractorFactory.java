/*
 * $Id:$
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

package org.lockss.plugin.asm;

/* This will require daemon 1.62 and later for JsoupHtmlLinkExtractor support
The vanilla JsoupHtmlLinkExtractor will generate URLs from any forms that it finds on pages
without restrictions (inclusion/exclusion rules) and so long as those resulting URLs satisfy the crawl rules
they will be collected which is too broad because you can't know everything you might encounter. 
This is a thin wrapper that specifies what type of forms to INCLUDE to limit the potential collection. 
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.extractor.*;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

/* an implementation of JsoupHtmlLinkExtractor  */
public class ASMscienceHtmlLinkExtractorFactory 
implements LinkExtractorFactory {

  private static final Logger log = 
      Logger.getLogger(ASMscienceHtmlLinkExtractorFactory.class);

  private static final String DIV_TAG = "div";
  private static final String ID_NAME = "id";
  private static final String CLASS_NAME = "class";
  private static final String META_TAG = "meta";
  private static final String NAME_NAME = "name";
  private static final String CONTENT_NAME = "content";
  private static final String DATA_URL = "data-fullTexturl"; 


  // and for table of contents article listing
  //<div class="tocheadingarticlelisting retrieveTocheadingArticle hiddenjsdiv">/content/journal/microbiolspec/2/4/articles?fmt=ahah&tocHeading=http://asm.metastore.ingenta.com/content/journal/microbiolspec/reviewarticle</div>
  // Identify an article landing page from which all article aspect links originate
  protected static final Pattern PATTERN_TOC_LANDING_URL = Pattern.compile("^(https?://[^/]+)/content/journal/[^/]+/[0-9]+/[0-9]+$", Pattern.CASE_INSENSITIVE);

  //main book chapter or journal article landing page - extract other information from tags here
  // www.asmscience.org/content/journal/microbiolspec/10.1128/microbiolspec.PLAS-0022-2014
  // www.asmscience.org/content/book/10.1128/9781555818289.chap4
  protected static final Pattern PATTERN_ARTICLE_LANDING_URL = Pattern.compile("^(https?://[^/]+)/content/(book|journal/[^/]+)/[0-1]{2}\\.[0-9]{4}/[^/?&]+$", Pattern.CASE_INSENSITIVE);
  
  // TODO: FULLTEXTHTML
  // This Div tag creates a link which returns a page with a one time URL for full-text html
  // To use it you need to turn on the ASMJsoupHtmlLinkExtractor (see ToDo below)
  //data-fullTexturl="/deliver/fulltext/microbiolspec/3/2/MDNA3-0016-2014.html?itemId=/content/journal/jid/10.1128/art.id&mimeType=html&fmt=ahah"
  protected static final Pattern DATA_URL_PATTERN = Pattern.compile("/deliver/fulltext/[^/]+/[0-9]+/[0-9]+/[^/]+\\.html\\?itemId=[^&]+&mimeType=html&fmt=ahah$", Pattern.CASE_INSENSITIVE);
  protected static final Pattern FIGURE_TABLE_PATTERN = Pattern.compile("metadata_(webId|thumbnailImage|fullSizeImage)", Pattern.CASE_INSENSITIVE);
  /*
   * (non-Javadoc)
   * @see org.lockss.extractor.LinkExtractorFactory#createLinkExtractor(java.lang.String)
   * Simple version for most Atypon children
   * restrict the form download URLs to just those forms with the name="frmCitMgr"
   */
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) {
    // TODO: FULLTEXTHTML
    // in place to pull one-time html link from the generator page
    // Use the ASM version which knows how to pull link from an otherwise empty page
    //ASMJsoupHtmlLinkExtractor extractor = new ASMJsoupHtmlLinkExtractor();
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
    registerExtractors(extractor);
    return extractor;
  }


  protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {
    extractor.registerTagExtractor(DIV_TAG, new ASMDivTagLinkExtractor(ID_NAME));
    extractor.registerTagExtractor(META_TAG, new ASMMetaTagLinkExtractor(NAME_NAME));
  }

  
  /*
   * A. 
   * <div id="itemFullTextId" "data-fullTexturl"=<foo>
   *   pull out:  
   * 
   * B.
   * <div class="metadata_webId">/content/microbiolspec/10.1128/microbiolspec.AID-0004-2012.fig1</div>
   * <div class="metadata_thumbnailImage">microbiolspec/2/1/AID-0004-2012-fig1_thmb.gif</div>
   * <div class="metadata_fullSizeImage">microbiolspec/2/1/AID-0004-2012-fig1.gif</div>
   * this one is already picked up
   * <img src="/docserver/fulltext/microbiolspec/2/1/AID-0004-2012-fig1.gif"
   */
  public static class ASMDivTagLinkExtractor extends SimpleTagLinkExtractor {

    // nothing needed in the constructor - just call the parent
    public ASMDivTagLinkExtractor(String attr) {
      super(attr);
    }

    //
    // This will get called when a DIV tag exists with an "id" value
    // but we don't want the JSOUP super class to also create a link using
    // any "id" value of a div tag, so when we are here, do not call super,
    // just return.
    //
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      String srcUrl = node.baseUri();
      Matcher landingMat = PATTERN_ARTICLE_LANDING_URL.matcher(srcUrl);
      Matcher tocMat = PATTERN_TOC_LANDING_URL.matcher(srcUrl);

      // Are we an article landing page?
      if ( (srcUrl != null) && landingMat.matches()) {
        log.debug3("...it's an article landing page");
        // build up a DOI value with html encoding of the slash"
        String base_url = landingMat.group(1);
        
        // Now handle the possible DIV tag extractions
        if (DIV_TAG.equals(node.nodeName())) {

          //TODO: FULLTEXTHTML
          // turn on for two step pickup of full-textHTML
          /*
          // A. checking ID="itemFullTextID and data-fullTextUrl="foo"
            if (isAttrValEquals(node, ID_NAME, "itemFullTextId")) {
              String dataVal = getAttrVal(node, DATA_URL, DATA_URL_PATTERN);
              if (dataVal != null) { 
                log.debug3("div id=itemFullTextId with data_fullTextUrl");
                String newUrl = base_url + dataVal;
                log.debug3("create new URL: " + newUrl);
                cb.foundLink(newUrl);
                // if it was this, no need to do anything further with this link
                return;
              }
            }
            */
            
         // B. checking CLASS="metadata_webId", "metadata_thubnailImage", "metadata_fullSizeImage" 
            if (isAttrValMatches(node, CLASS_NAME, FIGURE_TABLE_PATTERN)) {
              log.debug3("div with figure/table metadata_* link");
              String figure_table_val = null; 
              // the value is the text of this node
              for (Node tnode : node.childNodes()) {
                if (tnode.nodeName().equalsIgnoreCase("#text")) {
                  String full_val = tnode.toString();
                  figure_table_val = (full_val != null) ? full_val.trim() : null;
                break;
                }
              }
              if (figure_table_val != null) {
                // we picked up a link to a figure or table - thumbnail, landing page, or full-size
                // either starts with "/content/..." or needs "/docserver/fulltext" prepended
                log.debug3("picked up url snippet: " + figure_table_val);
                String newUrl;
                if (!figure_table_val.startsWith("/content")) {
                  newUrl = base_url + "/docserver/fulltext/" + figure_table_val;
                } else {
                  newUrl = base_url + figure_table_val;
                }
                log.debug3("create new URL: " + newUrl);
                cb.foundLink(newUrl);
                // if it was this, no need to do anything further with this link
                return;
            }
          } 
        }
      // On a Table of Contents page  
      } else if ( (srcUrl != null) && tocMat.matches()) {
        log.debug3("on a TOC page");
        String base_url = tocMat.group(1);
        // This is currently redundant because we only come here on DIV tags
        // but it might get extended to other tags later.
        if (DIV_TAG.equals(node.nodeName())) {
          String classAttr = node.attr("class");
          if ( classAttr != null && classAttr.contains("tocheadingarticlelisting")) {
            String val = null;
            for (Node tnode : node.childNodes()) {
              if (tnode.nodeName().equalsIgnoreCase("#text")) {
                val = tnode.toString();
                break;
              }
            }
            log.debug3("toc div val: " + val);
            if ( val != null && val.contains("articles?fmt=ahah")) {
              log.debug3("...and we matched that expected URL for toc article generator");
              String newUrl = base_url + val;
              log.debug3("create new URL: " + newUrl);
              cb.foundLink(newUrl);
              // if it was this, no need to do anything further with this link
              return;
            }
          }   
        }
      } // end of check for full text html page
      // Do NOT call super. 
      // Only in these very specific cases will a <DIV> tag generate a URL
      // Sometimes a <DIV> tag is just a <DIV> tag.
    }
      
      /*
       * A convenience method to pull the value from a specified node's attribute
       * If the optional pattern is given (non-null), only return the value if the
       * value matches the given pattern.
       * Return values:
       * null - the node didn't have the attribute OR its value was null OR 
       *     the value didn't match a given pattern
       * otherwise, the value of the attr  
       */
      private String getAttrVal(Node node, String attr_name, Pattern optional_pat) {
        String retVal = null;
        
        if (optional_pat != null) {
          String tempVal = node.attr(attr_name);         
          Matcher mat = optional_pat.matcher(tempVal);
          if (mat.find()) {
            retVal = tempVal;
          }
        } else {
          retVal = node.attr(attr_name);
        }
        return retVal;
      }
      
      /*
       * Does the attribute exist and if so does its value equal the checkVal string?
       */
      private boolean isAttrValEquals(Node node, String attr_name, String checkVal) {
        boolean retVal = false;
        
        if (checkVal!= null) {
          String tempVal = node.attr(attr_name);
          retVal = checkVal.equals(tempVal);
        } 
          return retVal;
      }
      
      private boolean isAttrValMatches(Node node, String attr_name,
          Pattern valPattern) {
        boolean retVal = false;
        
        if (valPattern != null) {
          String tempVal = node.attr(attr_name);         
          Matcher mat = valPattern.matcher(tempVal);
          if (mat.find()) {
            retVal = true;
          }
        } 
        return retVal;
      }
      
  }
  
  public static class ASMMetaTagLinkExtractor extends SimpleTagLinkExtractor {

    // nothing needed in the constructor - just call the parent
    public ASMMetaTagLinkExtractor(String attr) {
      super(attr);
    }

    //
    // This will get called when 
    // <meta name="CRAWLER.fullTextUrl"
    // but we don't want the JSOUP super class to also create a link using
    // any "name value of a meta tag
    // just return.
    //
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      String srcUrl = node.baseUri();
      Matcher landingMat = PATTERN_ARTICLE_LANDING_URL.matcher(srcUrl);

      // Are we a page for which this would be pertinent?
      if ( (srcUrl != null) && landingMat.matches()) {
        log.debug3("...it's an article landing page");
        if (META_TAG.equals(node.nodeName())) {
          String nameAttr = node.attr(NAME_NAME);
          if ("CRAWLER.fullTextLink".equals(nameAttr)) {
            String contentUrl = node.attr(CONTENT_NAME);
            log.debug3("crawler_fullTextUrl: " + contentUrl);
            if (contentUrl != null) {
              cb.foundLink(contentUrl);
              return;
            }
          }
        }
      } // end of check for full text html page
      // Do NOT call super. 
      // Only in these very specific cases will a <Meta> tag generate a URL
    }
  }
}