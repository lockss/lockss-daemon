/*
 * $Id:$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pub2web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


public class Pub2WebUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  protected static Logger log = 
      Logger.getLogger(Pub2WebUrlNormalizer.class);
  protected static final String SUFFIX = "&isFastTrackArticle=";
  protected static final String EXPIRINGSUFFIX = "?expires=";
  protected static final String CONTENT_URL = "/deliver/fulltext/"; // how we identify these URLs 
  protected static final String FULLHTML_URL = "/docserver/ahah/fulltext";
  
  protected static final Pattern FULLTEXT_URL_PATTERN =
      //<base>/deliver/fulltext/<jid>/<vol>/<issue>/<stuff>.(pdf|html)?itemId="/content/journal/<jid>/...fooo...&mimeType=pdf&isFastTrackArticle=
      Pattern.compile("^(https?://[^/]+)/deliver/fulltext/[^/]+/[^?]+\\.(pdf|html)\\?itemId=(/content/(book|journals?)/[^&]+)&mimeType=([^&]+)(&fmt=ahah)?(&isFastTrackArticle=)?$", Pattern.CASE_INSENSITIVE); 

   
/*
 * 1. Simple normalizations:
 *   remove jsessionid=.... from any url before anythingelse
 *   remove unnecessary "&isFastTrackArticle=" from end of any url
 *   
 * 2. Complicated transformations  
 *    turn one-time expiring URLS in to crawler friendly stable urls
 *    This occurs for both full-text HTML and full-text PDF of the article
 *    It does not occur for full-text version of the TOC which has no crawler version
 * 
 * pdf link: 
 * http://jgv.microbiologyresearch.org/deliver/fulltext/jgv/96/2/390_vir070219.pdf?itemId=/content/journal/jgv/10.1099/vir.0.070219-0&mimeType=pdf&isFastTrackArticle=
 *           (GROUP1)/deliver/fulltext/jgv/96/10/3131_jgv000245.(GROUP2)?itemId=(GROUP3)&mimeType=(GROUP4)&isFastTrackArticle=
 *   becomes:
 *       http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.070219-0?crawwler=true&mimetype=application/pdf
 * or for a book:
 * http://www.asmscience.org/deliver/fulltext/10.1128/9781555817992/9781555812058_Chap03.pdf?itemId=/content/book/10.1128/9781555817992.chap3&mimeType=pdf&isFastTrackArticle=
 *    becomes       
 *         http://www.asmscience.org/content/book/10.1128/9781555817992.chap3?crawler=true&mimeType=application/pdf
 * html link: 
 * http://jgv.microbiologyresearch.org/deliver/fulltext/jgv/96/1/183.html?itemId=/content/journal/jgv/10.1099/vir.0.064816-0&mimeType=html&fmt=ahah
 *       (GROUP1)/deliver/fulltext/jgv/96/1/183.(GROUP2)?itemId=(GROUP3)&mimeType=(GROUP4)&fmt=ahah
 *   becomes: 
 *     http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.064816-0?crawler=true&mimetype=html
 * or for a book
 *  http://www.asmscience.org/deliver/fulltext/10.1128/9781555817992/chap3.html?itemId=/content/book/10.1128/9781555817992.chap3&mimeType=html&fmt=ahah
 *   becomes:
 *     http://www.asmscience.org/content/book/10.1128/9781555817992.chap3?crawler=true&mimetype=html
 *     
 * Other PDF links will go through a URL consumer to attach the one-time redirect to the original URL:
 * TOC pdf - special case this out as it doesn't have crawler version...just swallow the redirect with a consumer
 * Supplementary Data - won't get transformed by this because it is under "/content/suppdata - let it get consumed
 *    links on a page could look like this:
 *    /deliver/fulltext/jgv/96/1/67363.pdf?itemId=/content/suppdata/jgv/10.1099/vir.0.067363-0-1&amp;mimeType=pdf&amp;isFastTrackArticle=
 * 
 * Figures - won't get transformed by this because it doesn't have mimetype...it's a direct link   
 *     content/journal/jgv/10.1099/vir.0.067363-0/figures?fmt=ahah 
 *     
 */

 /*
  The following code will cause redirect loop, please see the log:
  """
  > Warning         https://www.asmscience.org/content/book/10.1128/9781683670247.cont01?crawler=true&mimetype=application/pdf              302 Found

  This is in the log:

  11:06:50.274: Warning: 1622-BaseUrlFetcher: Max redirects hit, not redirecting https://www.asmscience.org/content/book/10.1128/9781683670247.cont01?crawler=true&mimetype=application/pdf past https://www.asmscience.org/content/book/10.1128/9781683670247.cont01?crawler=true&mimetype=application/pdf

  It will follow a maxinum of 10 redirects, but as both URLs in the

  message are the same it looks like a redirect loop.
  """

  15:16:07.588: Debug3: 9-JsoupHtmlLinkExtractor: FoundLink (before resolver):https://www.asmscience.org/deliver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?itemId=/content/book/10.1128/9781683670247.cont01&mimeType=pdf
  15:16:07.588: Debug3: 9-Pub2WebUrlNormalizer: about to norm: https://www.asmscience.org/deliver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?itemId=/content/book/10.1128/9781683670247.cont01&mimeType=pdf
  15:16:07.588: Debug3: 9-Pub2WebUrlNormalizer: full text url: https://www.asmscience.org/deliver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?itemId=/content/book/10.1128/9781683670247.cont01&mimeType=pdf
  15:16:07.588: Debug3: 9-Pub2WebUrlNormalizer: normalized to: https://www.asmscience.org/content/book/10.1128/9781683670247.cont01?crawler=true&mimetype=application/pdf
  15:16:07.588: Debug3: 9-UrlUtil: Normalizing https://www.asmscience.org/content/book/10.1128/9781683670247.cont01?crawler=true&mimetype=application/pdf
  */
  @Override
  public String additionalNormalization(String url, ArchivalUnit au) throws PluginException {

    if (url.contains(";jsessionid=")) {
      url = url.replaceFirst(";jsessionid=[^?]+", "");
    }
    
    Matcher ftMat = FULLTEXT_URL_PATTERN.matcher(url);
    log.debug3("about to norm: " + url);
    // journals - the tocpdf doesn't have a crawler equivalent.  Pass it through for url consumption
    // books - there are no fulltext links on the book landing page
    if (ftMat.matches() && (!(url.contains("/toc.pdf")))) {
      log.debug3("full text url: " + url);
      // create the crawler equivalent
      // group1 is the base_url
      // group2 is the extension...html or pdf - use to determing mimetype
      // group3 is the article identifier
      // group4 is an "or" grouping for book/journal - unused
      // note that group5 isn't complete mimetype. - unused
      String mt;
      if ("pdf".equals(ftMat.group(2))) {
        mt = "application/pdf";
      } else {
        mt = "html";
      }
      url = ftMat.group(1) + ftMat.group(3) + "?crawler=true&mimetype=" + mt;
      log.debug3("normalized to: " + url);
    } 
    // just remove any undesired suffixes
    // if the suffix doesn't exist the url doesn't change, no sense wasting
    // cycles checking if it exists before trying to remove
    // for the URLs that flow through - this may still be an unnecessary suffix
     url = StringUtils.substringBeforeLast(url, "&isFastTrackArticle=");
    return url;
  }

}
