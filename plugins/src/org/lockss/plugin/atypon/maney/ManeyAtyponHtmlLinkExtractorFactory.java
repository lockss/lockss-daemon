/*
 * $Id: ManeyAtyponHtmlLinkExtractorFactory.java,v 1.3 2014-08-28 19:18:58 alexandraohlson Exp $
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

package org.lockss.plugin.atypon.maney;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.lockss.extractor.*;
import org.lockss.extractor.JsoupHtmlLinkExtractor.ScriptTagLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkExtractorFactory;
import org.lockss.util.Logger;


public class ManeyAtyponHtmlLinkExtractorFactory extends
BaseAtyponHtmlLinkExtractorFactory {
  
  private static final String SCRIPT_TAG="script";

  public org.lockss.extractor.LinkExtractor createLinkExtractor(String mimeType) {

    // create the parent to handle other restrictions set in BaseAtypon
    JsoupHtmlLinkExtractor extractor = (JsoupHtmlLinkExtractor) super.createLinkExtractor(mimeType);
    // Now do additional processing with a link extractor that knows about images
    extractor.registerTagExtractor(SCRIPT_TAG, new ManeyScriptTagLinkExtractor());
    return extractor;
  }

  /*
   * Extracts image URLs from Maney Press browsable figure interface
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
  public static class ManeyScriptTagLinkExtractor extends ScriptTagLinkExtractor {

    private static Logger log = Logger.getLogger(ManeyScriptTagLinkExtractor.class);

    public ManeyScriptTagLinkExtractor() {
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
