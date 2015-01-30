/*
 * $Id: TafHtmlLinkExtractorFactory.java,v 1.1 2015-01-30 21:16:05 thib_gc Exp $
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

package org.lockss.plugin.taylorandfrancis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkExtractorFactory;
import org.lockss.util.StringUtil;

public class TafHtmlLinkExtractorFactory extends BaseAtyponHtmlLinkExtractorFactory {

  private static final String P_TAG = "p";

  private static final String ONCLICK_ATTR = "onclick";

  /* in addition to the default extractors set up by BaseAtypon,
   * add one for "p" tag
   */
  protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {

    super.registerExtractors(extractor);
    // register extractor for 'onclick' attribute of 'p' tag 
    extractor.registerTagExtractor(P_TAG, new TafPOnclickExtractor());
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
              cb.foundLink(newUrl);
              return;
            }
          }
        }
      }

      super.tagBegin(node, au, cb);
    }
  }

}
