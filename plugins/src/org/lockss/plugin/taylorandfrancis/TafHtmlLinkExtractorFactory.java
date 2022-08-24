/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkExtractorFactory;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class TafHtmlLinkExtractorFactory extends BaseAtyponHtmlLinkExtractorFactory {
  private static final Logger log = Logger.getLogger(TafHtmlLinkExtractorFactory.class);

  private static final String P_TAG = "p";
  private static final String ONCLICK_ATTR = "onclick";

  private static final String LINK_TAG = "a";
  private static final String HREF_ATTR = "href";
  
  /* in addition to the default extractors set up by BaseAtypon,
   * add one for "p" tag
   * 
   * TODO: Possibly temporary
   * The current form link extraactor does not "see" the form submit which uses a a javascript
   * method instead of an <input type="submit" > tag. So to work around this, 
   * generate the downloadCitation from the showCitations link
   *   <a href="action/showCitFormats?doi=10.1080%2F21640629.2015.1091169"
   * The additional link we need to generate is
   *   <a href="action/downloadCitation?doi=10.1080%2F21640629.2015.1091169&format=ris&include=cit"
   */


  protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {

    super.registerExtractors(extractor);
    // register extractor for 'onclick' attribute of 'p' tag 
    extractor.registerTagExtractor(P_TAG, new TafPOnclickExtractor());
    // used to generate the downloadCitations link
    extractor.registerTagExtractor(LINK_TAG, new TafLinkExtractor());
  }

  public static class TafPOnclickExtractor extends SimpleTagLinkExtractor {

    // pattern to isolate URL first argument of 'window.open()' call
    static final protected Pattern OPEN_WINDOW_PATTERN = Pattern.compile(
        "window\\.open\\([\"']([^\"']+)", Pattern.CASE_INSENSITIVE);
    // match PDF file

    // nothing needed in the constructor - just call the parent
    public TafPOnclickExtractor() {
      super(ONCLICK_ATTR);
    }

    /*
     * Extending the way links are extracted by the Jsoup link extractor 
     * in these specific cases:
     * <ul>
     *   <li> we are on the 'Figure' page of an article</li>
     *   <li> the page has links done with 'onclick' handlers on 'p' tags</li>
     *   <li> the 'onclick' handler target is an 'window.open()' function whose
     *   first argument is a link</li>
     * </ul>
     * In this case, extract the first argument of 'window.open()' as a link
     * In any other case, fall back to standard Jsoup implementation    
     */
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      if (P_TAG.equals(node.nodeName())) {
        String onClickUrl = node.attr(ONCLICK_ATTR);
        if (!StringUtil.isNullString(onClickUrl)) {
          Matcher openWindowMatcher = OPEN_WINDOW_PATTERN.matcher(onClickUrl);
          if (openWindowMatcher.find()) {
            String newUrl = openWindowMatcher.group(1);
            if (!StringUtil.isNullString(newUrl)) {
            	cb.foundLink(AuUtil.normalizeHttpHttpsFromBaseUrl(au,newUrl));
            	return;
            }
          }
        }
      }

      super.tagBegin(node, au, cb);
    }
  }
  
  public static class TafLinkExtractor extends SimpleTagLinkExtractor {
    
    private static final String CIT_SUFFIX = "&format=ris&include=cit";
    private static final String DOWNCIT_ACTION = "action/downloadCitation";
    // the acttion subdir is only one level down, but this is simpler
    private static final Pattern SHOWCIT_PATTERN = Pattern.compile("action/showCitFormats(\\?doi=[^?&]+)$");
    private static final Pattern PAGE_PATTERN = Pattern.compile("^(https?://[^/]+/)doi/");


    public TafLinkExtractor() {
      super(HREF_ATTR);
      // TODO Auto-generated constructor stub
    }

 /*
  * When we find an
  * <a href="/action/showCitFormats?doi=10.1080%2F21640629.2015.1106145">Download citation</a>
  * first generate a matching
  *   action/downloadCitation?doi=THEDOI&&format=ris&include=cit link
  * and then call super to get the original
  * 
  */
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      String srcUrl = node.baseUri();
      Matcher pageMatch = PAGE_PATTERN.matcher(srcUrl);
      if (LINK_TAG.equals(node.nodeName())) {
        String hrefUrl = node.attr(HREF_ATTR);
        if (!StringUtil.isNullString(hrefUrl)) {
          Matcher showCitMatcher = SHOWCIT_PATTERN.matcher(hrefUrl);
          if (showCitMatcher.find() && (srcUrl != null) && pageMatch.find()) {
            log.debug3("found a link tag with an href that includes action/showCitFormats...");
            String newUrl = pageMatch.group(1) + DOWNCIT_ACTION + showCitMatcher.group(1) + CIT_SUFFIX;
            log.debug3("created a downloadCitations link: " + newUrl);
            if (!StringUtil.isNullString(newUrl)) {
            	cb.foundLink(AuUtil.normalizeHttpHttpsFromBaseUrl(au,newUrl));
            	// continue on to find the actual link now
            }
          }
        }
      }
      // now pick up the link that was actually there and continue
      super.tagBegin(node, au, cb);
    }
  }

}
