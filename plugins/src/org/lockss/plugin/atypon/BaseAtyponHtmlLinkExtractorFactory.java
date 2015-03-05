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

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.lockss.extractor.*;
import org.lockss.extractor.JsoupHtmlLinkExtractor.ScriptTagLinkExtractor;
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
  private static final String CLASS_NAME = "class";
  private static final String ID_NAME = "id";
  private static final String DOI_NAME = "doi";
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
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) {
    // set up the base link extractor to use specific includes and excludes
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
    setFormRestrictors(extractor, null); // no additional child restrictors
    registerExtractors(extractor);
    return extractor;
  }

  /*
   * Create the link extractor - a version that allows a child to add additional restrictions
   * for use by the citation download FORM extractor
   * This method merges the child restrictions with the necessary base restriction
   */
  public LinkExtractor createLinkExtractor(String mimeType, Map<String, HtmlFormExtractor.FormFieldRestrictions> child_restrictor) {
    // set up the base link extractor to use specific includes and excludes
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
    setFormRestrictors(extractor, child_restrictor);
    registerExtractors(extractor);
    return extractor;
  }

  /* 
   * Merge the given restrictors with the base_atypon restrictors and apply
   * to the link extractor
   * This version is used by the convenience createLinkExtractor(mimeType, child_restrictors)
   */
  protected void setFormRestrictors(JsoupHtmlLinkExtractor extractor, Map<String,HtmlFormExtractor.FormFieldRestrictions> child_restrictor) {
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
    extractor.setFormRestrictors(base_restrictor);
  }

  /*
   * For a child plugin to use its own subclassed simple tag link extractor
   * to handle <a> tags, override this method
   */
  protected JsoupHtmlLinkExtractor.LinkExtractor createLinkTagExtractor(String attr) {
    return new BaseAtyponLinkTagLinkExtractor(attr);
  }

  /* 
   * For a child plugin to use its own subclassed script tag link extractor
   * override this method
   */
  protected JsoupHtmlLinkExtractor.LinkExtractor createScriptTagExtractor() {
    return new BaseAtyponScriptTagLinkExtractor();
  }


  /*
   *  For when it is insufficient to simply use a different link tag or script
   *  tag link extractor class, a child plugin can override this and register 
   *  additional or alternate extractors 
   */
  protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {
    // this is a little inefficient because the link extractor may get called TWICE
    // if there is BOTH an href and a class attribute on the link tag
    // but we can't guarantee that we'll have both so we need to ensure we catch link

    // the super will still only use href (which is default)
    // but this extractor tagBegin will get called for ALL link tags and we can check other attrs there 
    extractor.registerTagExtractor(LINK_TAG, createLinkTagExtractor(HREF_NAME));
    extractor.registerTagExtractor(SCRIPT_TAG, createScriptTagExtractor());
  }


  /*
   * BaseAtypon form restrictors. Exclude "refworks" and "refworks-cn" from possible
   * downloadable citation types  
   */
  private Map<String, HtmlFormExtractor.FormFieldRestrictions> setUpBaseRestrictor() {
    Set<String> include = new HashSet<String>();
    Set<String> exclude_args = new HashSet<String>();
    Map<String, HtmlFormExtractor.FormFieldRestrictions> restrictor
    = new HashMap<String, HtmlFormExtractor.FormFieldRestrictions>();

    /* only include forms with the name "frmCitMgr" */
    include = SetUtil.fromCSV("frmCitmgr");
    HtmlFormExtractor.FormFieldRestrictions include_restrictions = new HtmlFormExtractor.FormFieldRestrictions(include,null);
    restrictor.put(HtmlFormExtractor.FORM_NAME, include_restrictions);

    /* 
     * For downloadCitation urls, such as:
     *     "action/downloadCitation?doi=10.1111%2F123456&format=ris&include=cit",
     * we only want to include: format=ris & include=cit
     * but do this by excluding the usual alternatives instead of just including what we want
     * so that in the cases where there is only ONE choice and therefore no specific arguments, 
     * we still get the one default option (normalized to have the right arguments for the crawl rules) 
     */
    
    /* now set up an exclude restriction on "format" */ 
    exclude_args = SetUtil.fromCSV("refworks,refworks-cn,bibtex,medlars,endnote"); 
    HtmlFormExtractor.FormFieldRestrictions exclude_restrictions = new HtmlFormExtractor.FormFieldRestrictions(null, exclude_args);
    restrictor.put("format", exclude_restrictions);
    
    /* now set up an exclude restriction on "include" */
    exclude_args = SetUtil.fromCSV("ref,abs"); 
    exclude_restrictions = new HtmlFormExtractor.FormFieldRestrictions(null, exclude_args);
    restrictor.put("include", exclude_restrictions);


    return restrictor;
  }


  /*
   * BaseAtypon tag extractor
   * handles:
   *     LINK_TAG ("<a href=>")
   *         javascript:popRef
   *         javascript:popRefFull
   *     LINK_TAG ("<a class="... openFigLayer" or class="...openTablesLayer"...>
   *  fails over to the SimpleTagLinkExtractor super class           
   * 
   */
  public static class BaseAtyponLinkTagLinkExtractor 
  extends SimpleTagLinkExtractor {

    /*
     *  pattern to isolate id used in popRef call = see comment at tagBegin
     *  pulling "id" from inside javascript:popRef('foo')
     *  the pattern is limited in that it doesn't handle a MIX of single and 
     *  double quotes - but that's extremely unlikely for an id value
     */
    protected static final Pattern POPREF_PATTERN = Pattern.compile(
        "javascript:popRef(Full)?\\([\"']([^\"']+)[\"']\\)", Pattern.CASE_INSENSITIVE);
    protected static final Pattern OPEN_CLASS_PATTERN = Pattern.compile(
        "(openFigLayer|openTablesLayer)$", Pattern.CASE_INSENSITIVE);

    // define pieces used in the resulting URL
    private static final String ACTION_SHOWPOP = "/action/showPopup?citid=citart1&id=";
    private static final String ACTION_SHOWFULL = "/action/showFullPopup?id=";
    private static final String DOI_ARG = "&doi=";

    // nothing needed in the constructor - just call the parent
    public BaseAtyponLinkTagLinkExtractor(String attr) {
      super(attr);
    }

    /*
     *  handle <a> tag with href value on a full html page
     *  for <a class="ref" href="javascript:popRef('F1')">
     *    ==> BASE/action/showPopup?citid=citart1&id=F1&doi=10.2466%2F05.08.IT.3.3
     *  for <a class="ref" href="javascript:popRefFull('i1520-0469-66-1-187-f03')">
     *    ==> BASE/action/showFullPopup?id=i1520-0469-66-1-187-f01&doi=10.1175%2F2008JAS2765.1
     *    
     *  handle <a> tag with openFigLayer or openTablesLayer which otherwise
     *  uses AJAX to generate a magnifier overlay for image viewing and 
     *  ultimately showFullPopup for the each image or table. 
     *  Catch the call and create the showFullPopup using the doi arg and the id arg.
     *  examples from NRC: 
     * <a doi="10.1139/g2012-037" id="f1" class="red-link-left openFigLayer">
     * <a class="ref openTablesLayer" href="javascript:void(0);" id="tab3" doi="10.1139/g2012-037">   
     *  href could be there or could be set to "javascript:void(0)...unused
     *  
     *  this could also be made handle the following because the doi comes from the url
     *  is it necessary to do so?? 
     * <a class="openLayerForItem" itemid="f3" href="javascript:void(0);">
     */
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      String srcUrl = node.baseUri();
      Matcher fullArticleMat = PATTERN_FULL_ARTICLE_URL.matcher(srcUrl);

      // Are we a page for which this would be pertinent?
      if ( (srcUrl != null) && fullArticleMat.matches()) {
        // build up a DOI value with html encoding of the slash"
        String base_url = fullArticleMat.group(1);
        String doiVal = fullArticleMat.group(2) + "%2F" + fullArticleMat.group(3);

        // This is currently redundant because we only come here on link tags
        // but it might get extended to other tags later.
        if (LINK_TAG.equals(node.nodeName())) {
          // 1. are we an openFigLayer or openTablesLayer link?
          String classAttr = node.attr(CLASS_NAME);
          if (!StringUtil.isNullString(classAttr)) {
            Matcher classMat = null;
            classMat = OPEN_CLASS_PATTERN.matcher(classAttr);
            if (classMat.find()) {
              String idAttr = node.attr(ID_NAME);
              if (!StringUtil.isNullString(idAttr)) {
                // we can only proceed if we have an id
                String doiAttr = node.attr(DOI_NAME); 
                // we can guess at the id from our current URL if this isn't there
                if (!StringUtil.isNullString(doiAttr)) {
                  // use this one, not the one from the URL
                  doiVal = StringUtil.replaceString(doiAttr, "/", "%2F");
                }
                // we have an id and a doi, generate the link
                String newUrl = base_url + ACTION_SHOWFULL + idAttr + DOI_ARG + doiVal;
                log.debug3("new URL: " + newUrl);
                cb.foundLink(newUrl);
                // if it was this, no need to do anything further with this link
                return;
              }
            }
          } 
          // 2. Fall through - it wasn't an openFigLayer or openTablesLayer - is it href with popref pattern?
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
        } // end of check for if we are on a link tag
      } // end of check for full text html page
      // we didn't handle it, fall back to parent 
      super.tagBegin(node, au, cb);
    }
  }

  /*
   * BaseAtypon script tag extractor
   *    fails over to Jsoup ScriptTagLinkExtractor
   * Extracts image URLs from the new Atypon browsable figure interface 
   * (AIAA, BiR, Maney, etc)
   * which has static links to one version of the image (usually small) but
   * Javascript-generated links to other-size images.
   * In order to know what the other image size & suffixes are we parse the 
   * information out of 
   * 
   * <script type="text/javascript">
   * window.figureViewer=
   *   {doi:'10.1179/0002698013Z.00000000033',
   *    path:'/na101/home/literatum/publisher/maney/journals/content/amb/2013/amb.2013.60.issue-3/0002698013z.00000000033/production',
   *    figures:[{i:'S3F1',g:[{m:'s3-g1.gif',l:'s3-g1.jpeg',size:'116 kB'}]}
   *          ,{i:'S3F2',g:[{m:'s3-g2.gif',l:'s3-g2.jpeg',size:'70 kB'}]}
   *          ,{i:'S3F3',g:[{m:'s3-g3.gif',l:'s3-g3.jpeg',size:'61 kB'}]}
   *          ,{i:'S3F4',g:[{m:'s3-g4.gif',l:'s3-g4.jpeg',size:'37 kB'}]}
   *          ,{i:'S3F5',g:[{m:'s3-g5.gif',l:'s3-g5.jpeg',size:'28 kB'}]}
   *    ]}</script>
   *    
   * The in-line image is not included in the figureViewer arrays but will get
   * picked up by the base link extractor.
   * The end of the image URL is images/<size>/<filename> where 
   *   <size> = small, medium, large - corresponding to "s:", "m:", "l:" in the array.
   * Not all images have all sizes.    
   * 
   * Here are the links that would get generated from the above example, assuming that the URL
   * of the page is 
   *     http://www.maneyonline.com/doi/full/10.1179/0002698013Z.00000000033
   * generated:
   * http://www.maneyonline.com/na101/home/literatum/publisher/maney/journals/\
   *       content/amb/2013/amb.2013.60.issue-3/0002698013z.00000000033/production/\
   *       images/medium/s3-g1.gif
   * http://www.maneyonline.com/na101/home/literatum/publisher/maney/journals/\
   *       content/amb/2013/amb.2013.60.issue-3/0002698013z.00000000033/production/\
   *       images/large/s3-g1.jpeg
   *   and so on, through all the listed images
   *  
   * @author Alexandra Ohlson
   */
  public static class BaseAtyponScriptTagLinkExtractor extends ScriptTagLinkExtractor {

    private static Logger log = Logger.getLogger(BaseAtyponScriptTagLinkExtractor.class);

    public BaseAtyponScriptTagLinkExtractor() {
      super();
    }

    /* Make sure we're on a page that we care to parse for image information
     * Note that the base_url in this match does not include final "/" on purpose
     */
    protected Pattern PATTERN_FULL_ARTICLE_URL = Pattern.compile("^(https?://[^/]+)/doi/full/([.0-9]+)/([^?&]+)$");
    /*
     * Match the figureViewer in 3 steps.
     *  
     * 1. Match the overall "window.figureViewer" information and store the 
     * figures array as a single string for use by another matcher
     * group#1 is the doi, group#2 is the path (used) and group#3 is the figure information
     * Allow for the possibility of whitespace in likely locations - by liberal use 
     * of \\s* which makes the regexp really really ugly
     * Use of Pattern.DOTALL to allow '.' to handle newlines
     * 
     * window.figureViewer=
     * {doi:'(doi)',
     *  path:'(path)',
     *  figures:[(figure data)]}
     *  
     *  For clarity - here is a version of the regexp without white space bits
     * "window\\.figureViewer=\\{doi:'([^']+)',path:'([^']+)',figures:\\[(.*)\\]\\}" 
     */
    protected static final Pattern PATTERN_FIGURE_VIEWER =
        Pattern.compile("window\\.figureViewer=\\s*\\{\\s*doi\\s*:\\s*'([^']+)'\\s*,\\s*path\\s*:\\s*'([^']+)'\\s*,\\s*figures\\s*:\\s*\\[(.*)\\]\\s*\\}",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    /* 
     * 2. Now match the information that represents ONE image
     * We will iterate over the match as many times as there are images listed
     * Allow for whitespace with use of '\\s*'
     * Use of Pattern.DOTALL to allow '.' to handle newlines
     * 
     *  ONE FIGURE's DATA {i:'(figure ID)',g:[(figure data]}
     *  group#1 is the figureID, group#2 is the figure data for that image
     *   
     *  For clarity - here is a version of the regexp without white space bits
     * "\\{i:'([^']+)',g:\\[\\{([^\\}\\]]+)\\}\\]\\}",
     */
    protected static final Pattern PATTERN_FIGUREDATA=
        Pattern.compile("\\s*\\{\\s*i\\s*:\\s*'([^']+)'\\s*,\\s*g\\s*:\\s*\\[\\s*\\{([^\\}\\]]+)\\}\\s*\\]\\s*\\}",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    /*
     * 3. Size and filename for ONE image 
     * Within figure data for one image, there are one or more size/filename pairings
     * Iterate over the match as many times as there is useful information
     * Ignore the "size" entry, just find and use s|m|l
     * {m:'s3-g4.gif',l:'s3-g4.jpeg',size:'37 kB'}
     * allow for white space
     * Use of Pattern.DOTALL to allow '.' to handle newlines
     * 
     * For clarity - here is a version of the regexp without white space bits
     * "(s|m|l):'([^']+)'",
     */
    protected static final Pattern PATTERN_ONE_IMAGE=
        Pattern.compile("\\s*(s|m|l)\\s*:\\s*'([^']+)'",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);          

    /*
     * Translate the size 'key' to a url path section
     * this is more efficient than a hashmap
     * Because of regexp matching, we are guaranteed that the keyVal will
     * match one of the three choices
     */
    private String getSizePath(String keyVal) {
      switch (keyVal.charAt(0)) {
        case 's': return "/images/small/";
        case 'm': return "/images/medium/";
        case 'l': return "/images/large/";
      }
      return null;
    }

    /*
     * Extending the way links are extracted by the Jsoup link extractor in a specific case:
     *   - we are on a full article page
     *   - we hit an script tag of the format:
     *   <script type="text/javascript">...contents...</script>
     *   where the content match the PATTERN_FIGURE_VIEWER
     * The super class will create the link provided by this <img> tag for the in-line image
     * This will create links for the other forms of the image
     * In any case other than this one, fall back to standard Jsoup implementation    
     */
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      //log.setLevel("debug3");
      String srcUrl = node.baseUri();
      Matcher fullArticleMat = PATTERN_FULL_ARTICLE_URL.matcher(srcUrl);
      // For the moment we only get here on a <script> tag, so no need to check 
      // Are we a page for which this would be pertinent?
      if ( (srcUrl != null) && fullArticleMat.matches()) {
        String base_url = fullArticleMat.group(1);
        // the interior javascript is html, not text
        String scriptHTML = ((Element)node).html();
        log.debug3("script string: " + scriptHTML);
        Matcher figViewMat = PATTERN_FIGURE_VIEWER.matcher(scriptHTML);
        if (figViewMat.matches()) {
          // We are in a window.figureViewer=... script
          // doi match not used
          String imgPathSt = figViewMat.group(2);
          String figDataSt = figViewMat.group(3);
          log.debug3("path: " + imgPathSt);
          log.debug3("fig: " + figDataSt);
          Matcher figDataMat = PATTERN_FIGUREDATA.matcher(figDataSt);
          // For each image with data listed in this figureViewer
          while (figDataMat.find()) {
            // This will be the information for ONE of the displayed images
            // we don't use the figure ID
            String figDataStr = figDataMat.group(2);
            log.debug3("figure data: " + figDataStr);
            //dataStr: m:'s3-g3.gif',l:'s3-g3.jpeg',size:'61 kB'
            Matcher imgMat = PATTERN_ONE_IMAGE.matcher(figDataStr);
            // For each size of image listed in the figure data
            while (imgMat.find()) {
              String imgtype = imgMat.group(1);
              String imgFN = imgMat.group(2);
              String newUrl = base_url + imgPathSt + getSizePath(imgtype) + imgFN;
              log.debug3("new URL: " + newUrl);
              cb.foundLink(newUrl);
            }
          }
          // it was a figureViewer, no other link extraction needed
          return;
        }
      }
      // for one reason or another, we didn't handle this. Fall back to standard Jsoup
      super.tagBegin(node, au, cb);
    }
  }
}