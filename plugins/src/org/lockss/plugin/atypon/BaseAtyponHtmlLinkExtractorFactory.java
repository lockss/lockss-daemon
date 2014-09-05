/* $Id: BaseAtyponHtmlLinkExtractorFactory.java,v 1.4 2014-09-05 20:44:16 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

/* This will require daemon 1.62 and later for JsoupHtmlLinkExtractor support
The vanilla JsoupHtmlLinkExtractor will generate URLs from any forms that it finds on pages
without restrictions (inclusion/exclusion rules) and so long as those resulting URLs satisfy the crawl rules
they will be collected which is too broad because you can't know everything you might encounter. 
This is a thin wrapper that specifies what type of forms to INCLUDE to limit the potential collection. 
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.extractor.HtmlFormExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.SetUtil;
import org.lockss.util.StringUtil;

/* an implementation of JsoupHtmlLinkExtractor with a restrictor set */
public class BaseAtyponHtmlLinkExtractorFactory 
implements LinkExtractorFactory {
  
  private static final Logger log = 
      Logger.getLogger(BaseAtyponHtmlLinkExtractorFactory.class);

  private static final String HREF_NAME = "href";
  private static final String LINK_TAG = "a";
  private static final String SCRIPT_TAG = "script";
  //Identify a URL as being one for a full text html version of the article
  protected static final Pattern PATTERN_FULL_ARTICLE_URL = Pattern.compile("^(https?://[^/]+)/doi/full/([.0-9]+)/([^?&]+)$");


  /*
   * (non-Javadoc)
   * @see org.lockss.extractor.LinkExtractorFactory#createLinkExtractor(java.lang.String)
   * Simple version for most Atypon children
   * restrict the form download URLs to just those forms with the name="frmCitMgr"
   */
  public org.lockss.extractor.LinkExtractor createLinkExtractor(String mimeType) {
    Map<String, HtmlFormExtractor.FormFieldRestrictions> baseRestrictor
    = new HashMap<String, HtmlFormExtractor.FormFieldRestrictions>();

    baseRestrictor = setUpBaseRestrictor();

    // set up the base link extractor to use specific includes and excludes
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
    extractor.setFormRestrictors(baseRestrictor);

    extractor.registerTagExtractor(LINK_TAG, new BaseAtyponSimpleTagLinkExtractor(HREF_NAME));
    
    // TODO: Add the MANEY/BiR/AIAA script tag link extractor once we have 1.67
    //extractor.registerTagExtractor(SCRIPT_TAG, new AtyponScriptTagLinkExtractor());
    return extractor;
  }

  /*
   * A version of the method that allows a child to add additional restrictions
   * for use by the citation download FORM extractor
   * This method merges the child restrictions with the necessary base restriction
   */
  public org.lockss.extractor.LinkExtractor createLinkExtractor(String mimeType, Map<String, HtmlFormExtractor.FormFieldRestrictions> child_restrictor) {
    Map<String, HtmlFormExtractor.FormFieldRestrictions> base_restrictor
    = new HashMap<String, HtmlFormExtractor.FormFieldRestrictions>();

    base_restrictor = setUpBaseRestrictor();

    // did the child add in any additional restrictions?
    if (child_restrictor != null) {
      //Iterate over the child's map
      for (String key : child_restrictor.keySet()) {
        //
        if (base_restrictor.containsKey(key)) {
          // the child also is restricting this key, merge the restrictions in to the base
          HtmlFormExtractor.FormFieldRestrictions child_val = child_restrictor.get(key);
          HtmlFormExtractor.FormFieldRestrictions base_val = base_restrictor.get(key);
          Set<String> tmp_inc = base_val.getInclude();
          Set<String> tmp_exc = base_val.getExclude();
          tmp_inc.addAll(child_val.getInclude());
          tmp_exc.addAll(child_val.getExclude());
          base_restrictor.put(key, new HtmlFormExtractor.FormFieldRestrictions(tmp_inc, tmp_exc)); 
        } else {
          // add the child restrictor 
          base_restrictor.put(key,  child_restrictor.get(key));
        }
      }
    }

    // set up the link extractor with specific includes and excludes
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
    extractor.setFormRestrictors(base_restrictor);
    return extractor;
  }

  private Map<String, HtmlFormExtractor.FormFieldRestrictions> setUpBaseRestrictor() {
    Set<String> include = new HashSet<String>();
    Set<String> exclude = new HashSet<String>();
    Map<String, HtmlFormExtractor.FormFieldRestrictions> restrictor
    = new HashMap<String, HtmlFormExtractor.FormFieldRestrictions>();

    /* only include forms with the name "frmCitMgr" */
    include = SetUtil.fromCSV("frmCitmgr");
    HtmlFormExtractor.FormFieldRestrictions include_restrictions = new HtmlFormExtractor.FormFieldRestrictions(include,null);
    restrictor.put(HtmlFormExtractor.FORM_NAME, include_restrictions);

    /* now set up an exclude restriction on "format" */ 
    exclude = SetUtil.fromCSV("refworks,refworks-cn"); 
    HtmlFormExtractor.FormFieldRestrictions exclude_restrictions = new HtmlFormExtractor.FormFieldRestrictions(null, exclude);
    restrictor.put("format", exclude_restrictions);

    return restrictor;
  }

  public static class BaseAtyponSimpleTagLinkExtractor 
  extends SimpleTagLinkExtractor {

    /*
     *  pattern to isolate id used in popRef call = see comment at tagBegin
     *  pulling "id" from inside javascript:popRef('foo')
     *  the pattern is limited in that it doesn't handle a MIX of single and 
     *  double quotes - but that's extremely unlikely for an id value
     */
    protected static final Pattern POPREF_PATTERN = Pattern.compile(
        "javascript:popRef(Full)?\\([\"']([^\"']+)[\"']\\)", Pattern.CASE_INSENSITIVE);
    
    // define pieces used in the resulting URL
    private static final String ACTION_SHOWPOP = "/action/showPopup?citid=citart1&id=";
    private static final String ACTION_SHOWFULL = "/action/showFullPopup?id=";
    private static final String DOI_ARG = "&doi=";

    // nothing needed in the constructor - just call the parent
    public BaseAtyponSimpleTagLinkExtractor(String attr) {
      super(attr);
    }

    /*
     *  handle <a> tag with href value on a full html page
     *  for <a class="ref" href="javascript:popRef('F1')">
     *    ==> BASE/action/showPopup?citid=citart1&id=F1&doi=10.2466%2F05.08.IT.3.3
     *  for <a class="ref" href="javascript:popRefFull('i1520-0469-66-1-187-f03')">
     *    ==> BASE/action/showFullPopup?id=i1520-0469-66-1-187-f01&doi=10.1175%2F2008JAS2765.1
     */
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      String srcUrl = node.baseUri();
      Matcher fullArticleMat = PATTERN_FULL_ARTICLE_URL.matcher(srcUrl);
      // Are we a page for which this would be pertinent?
      if ( (srcUrl != null) && fullArticleMat.matches()) {
        // build up a DOI value with html encoding of the slash"
        String base_url = fullArticleMat.group(1);
        String doiVal = fullArticleMat.group(2) + "%2F" + fullArticleMat.group(3);
        /*
         * For now this is only called for LINK tags with href set
         * <a href="...">
         * But put in check so it can expand as needed
         */
        if (LINK_TAG.equals(node.nodeName())) {
          String href = node.attr(HREF_NAME);
          if (StringUtil.isNullString(href)) {
            //this is not the href you seek
            super.tagBegin(node, au, cb);
            return;
          }
          //1. Are we javascript:popRef(Full)('id')
          Matcher hrefMat = null;
          String idVal;
          hrefMat = POPREF_PATTERN.matcher(href);
          if (hrefMat.find()) {
            log.debug3("Found popRef pattern");
            // Derive showPopup URL
            idVal = hrefMat.group(2);
            String newUrl;
            if (hrefMat.group(1) != null) {
              // this is testing if we matched the popRefFull variant
              newUrl = base_url + ACTION_SHOWFULL + idVal + DOI_ARG + doiVal;
            } else {
              newUrl = base_url + ACTION_SHOWPOP + idVal + DOI_ARG + doiVal;
            }
            log.debug3("new URL: " + newUrl);
            cb.foundLink(newUrl);
            // if it was a popRef, no other link extraction needed
            return;
          }
        }
      }
      // we didn't handle it, fall back to parent 
      super.tagBegin(node, au, cb);
    }
  }
}