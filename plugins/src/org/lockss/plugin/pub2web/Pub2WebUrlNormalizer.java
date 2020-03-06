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
14:01:47.812: Debug3: 11-JsoupHtmlLinkExtractor: begin tag: a
14:01:47.812: Debug3: 11-JsoupHtmlLinkExtractor: FoundLink (before resolver):https://www.asmscience.org/deliver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?itemId=/content/book/10.1128/9781683670247.cont01&mimeType=pdf
14:01:47.812: Debug3: 11-Pub2WebUrlNormalizer: Fei: about to norm: https://www.asmscience.org/deliver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?itemId=/content/book/10.1128/9781683670247.cont01&mimeType=pdf
14:01:47.812: Debug3: 11-Pub2WebUrlNormalizer: Fei: full text url: https://www.asmscience.org/deliver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?itemId=/content/book/10.1128/9781683670247.cont01&mimeType=pdf
14:01:47.812: Debug3: 11-Pub2WebUrlNormalizer: Fei: normalized to: https://www.asmscience.org/content/book/10.1128/9781683670247.cont01?crawler=true&mimetype=application/pdf
14:01:47.812: Debug3: 11-UrlUtil: Normalizing https://www.asmscience.org/content/book/10.1128/9781683670247.cont01?crawler=true&mimetype=application/pdf
14:01:47.812: Debug3: 11-UrlUtil: protocol: https
14:01:47.812: Debug3: 11-UrlUtil: host: www.asmscience.org
14:01:47.812: Debug3: 11-UrlUtil: port: -1
14:01:47.812: Debug3: 11-UrlUtil: path: /content/book/10.1128/9781683670247.cont01
14:01:47.812: Debug3: 11-UrlUtil: query: crawler=true&mimetype=application/pdf
14:01:47.812: Debug3: 11-FollowLinkCrawler: Found https://www.asmscience.org/deliver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?itemId=/content/book/10.1128/9781683670247.cont01&mimeType=pdf
14:01:47.812: Debug3: 11-FollowLinkCrawler: Normalized to https://www.asmscience.org/content/book/10.1128/9781683670247.cont01?crawler=true&mimetype=application/pdf
14:01:47.812: Debug3: 11-CrawlRules: [CrawlRules$RE: match_incl, '^https?://www\.asmscience\.org/[^?]*\.(bmp|css|eot|gif|ico|jpe?g|js|png|svg|tif?f|ttf|woff.?)(\?([0-9]+|config=|v=).*)?$'].match(https://www.asmscience.org/content/book/10.1128/9781683670247.cont01?crawler=true&mimetype=application/pdf): 0
14:01:47.812: Debug3: 11-CrawlRules: [CrawlRules$RE: match_incl, '^https\:\/\/www\.asmscience\.org\/content/book/10\.1128\/9781683670247'].match(https://www.asmscience.org/content/book/10.1128/9781683670247.cont01?crawler=true&mimetype=application/pdf): 1
14:01:47.812: Debug2: 11-FollowLinkCrawler: Included url: https://www.asmscience.org/content/book/10.1128/9781683670247.cont01?crawler=true&mimetype=application/pdf


(python27) DN0a2330b4:Desktop feili18$ ./wget-url.sh "https://www.asmscience.org/content/book/10.1128/9781683670247" delete.txt
--2020-03-05 15:19:22--  https://www.asmscience.org/content/book/10.1128/9781683670247
Resolving www.asmscience.org (www.asmscience.org)... 104.26.4.204, 104.26.5.204
Connecting to www.asmscience.org (www.asmscience.org)|104.26.4.204|:443... connected.
HTTP request sent, awaiting response...
  HTTP/1.1 200 OK
  Date: Thu, 05 Mar 2020 23:19:23 GMT
  Content-Type: text/html;charset=UTF-8
  Transfer-Encoding: chunked
  Connection: close
  Set-Cookie: __cfduid=d6367312e65d7d8525fc484474e1894a21583450362; expires=Sat, 04-Apr-20 23:19:22 GMT; path=/; domain=.asmscience.org; HttpOnly; SameSite=Lax
  Set-Cookie: AWSALB=Yn5i5ZLO4/7GQfuEca65JMVYrIq/LsaHQnIw4F+GLalFI+rf9j8yVdJpm9X6syVsHu9k/Y8reGFB/5K4f/Jl3sskR4rYDt3D4rZ4Fhkzz7UA3SN7DZRqxq1WxbsI; Expires=Thu, 12 Mar 2020 23:19:22 GMT; Path=/
  Set-Cookie: AWSALBCORS=Yn5i5ZLO4/7GQfuEca65JMVYrIq/LsaHQnIw4F+GLalFI+rf9j8yVdJpm9X6syVsHu9k/Y8reGFB/5K4f/Jl3sskR4rYDt3D4rZ4Fhkzz7UA3SN7DZRqxq1WxbsI; Expires=Thu, 12 Mar 2020 23:19:22 GMT; Path=/; SameSite=None; Secure
  Cache-Control: no-cache
  Set-Cookie: JSESSIONID=uhSDfr4py6EwW6luvhQ2cips.asmlive-10-241-2-73; path=/
  Pragma: no-cache
  Vary: Accept-Encoding
  CF-Cache-Status: DYNAMIC
  Expect-CT: max-age=604800, report-uri="https://report-uri.cloudflare.com/cdn-cgi/beacon/expect-ct"
  Server: cloudflare
  CF-RAY: 56f78fbde9f993ca-SJC
Length: ignored [text/html]
Saving to: ‘delete.txt’

delete.txt              [ <=>                ] 173.81K  --.-KB/s    in 0.06s

2020-03-05 15:19:23 (3.03 MB/s) - ‘delete.txt’ saved [177978]

(python27) DN0a2330b4:Desktop feili18$ clear

(python27) DN0a2330b4:Desktop feili18$ ./wget-url.sh "https://www.asmscience.org/content/book/10.1128/9781683670247.cont01?crawler=true&mimetype=application/pdf" delete.pdf
--2020-03-05 16:27:59--  https://www.asmscience.org/content/book/10.1128/9781683670247.cont01?crawler=true&mimetype=application/pdf
Resolving www.asmscience.org (www.asmscience.org)... 104.26.5.204, 104.26.4.204
Connecting to www.asmscience.org (www.asmscience.org)|104.26.5.204|:443... connected.
HTTP request sent, awaiting response...
  HTTP/1.1 302 Found
  Date: Fri, 06 Mar 2020 00:28:00 GMT
  Content-Length: 0
  Connection: close
  Set-Cookie: __cfduid=d8d7c5bbbf3dd34ad76e8fa0a07a87c5b1583454480; expires=Sun, 05-Apr-20 00:28:00 GMT; path=/; domain=.asmscience.org; HttpOnly; SameSite=Lax
  Set-Cookie: AWSALB=cCTNSzxPiEUZDKf45sAlPslMa9dbpVnuLEiC5Kj38JAc53odewvoLM6wEDu9jc2hQHbkwA5Hvd+vzImpCiFsk1tdMhgZToQBFjkW03xykdzuneXUiWDMr0r5wj1V; Expires=Fri, 13 Mar 2020 00:28:00 GMT; Path=/
  Set-Cookie: AWSALBCORS=cCTNSzxPiEUZDKf45sAlPslMa9dbpVnuLEiC5Kj38JAc53odewvoLM6wEDu9jc2hQHbkwA5Hvd+vzImpCiFsk1tdMhgZToQBFjkW03xykdzuneXUiWDMr0r5wj1V; Expires=Fri, 13 Mar 2020 00:28:00 GMT; Path=/; SameSite=None; Secure
  Set-Cookie: JSESSIONID=kS3dQLFbOj90AKQwRJHjf2o0.asmlive-10-241-2-73; path=/
  Location: https://www.asmscience.org/deliver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?itemId=/content/book/10.1128/9781683670247.cont01&mimeType=application/pdf
  CF-Cache-Status: DYNAMIC
  Expect-CT: max-age=604800, report-uri="https://report-uri.cloudflare.com/cdn-cgi/beacon/expect-ct"
  Server: cloudflare
  CF-RAY: 56f7f4448985ed3f-SJC
Location: https://www.asmscience.org/deliver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?itemId=/content/book/10.1128/9781683670247.cont01&mimeType=application/pdf [following]
--2020-03-05 16:28:00--  https://www.asmscience.org/deliver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?itemId=/content/book/10.1128/9781683670247.cont01&mimeType=application/pdf
Connecting to www.asmscience.org (www.asmscience.org)|104.26.5.204|:443... connected.
HTTP request sent, awaiting response...
  HTTP/1.1 302 Found
  Date: Fri, 06 Mar 2020 00:28:01 GMT
  Content-Type: application/pdf
  Content-Length: 0
  Connection: close
  Set-Cookie: AWSALB=Eysvx7z4VFMDic3nnHIudXGE3NXbm+sSDNFKptJXszZU0c+RW0ZHa8pEec7ll43py8mUL240NFjBOpMbPBtcWeMHg98eJkmrZG0oRu90Axmu7AKXD+hc2sPXzOsF; Expires=Fri, 13 Mar 2020 00:28:01 GMT; Path=/
  Set-Cookie: AWSALBCORS=Eysvx7z4VFMDic3nnHIudXGE3NXbm+sSDNFKptJXszZU0c+RW0ZHa8pEec7ll43py8mUL240NFjBOpMbPBtcWeMHg98eJkmrZG0oRu90Axmu7AKXD+hc2sPXzOsF; Expires=Fri, 13 Mar 2020 00:28:01 GMT; Path=/; SameSite=None; Secure
  Location: http://www.asmscience.org/docserver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?expires=1583455381&id=id&accname=guest&checksum=144E5CCC1FBEFB4FE35FE0EB035B2F50
  CF-Cache-Status: DYNAMIC
  Expect-CT: max-age=604800, report-uri="https://report-uri.cloudflare.com/cdn-cgi/beacon/expect-ct"
  Vary: Accept-Encoding
  Server: cloudflare
  CF-RAY: 56f7f4499d686d7c-SJC
Location: http://www.asmscience.org/docserver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?expires=1583455381&id=id&accname=guest&checksum=144E5CCC1FBEFB4FE35FE0EB035B2F50 [following]
--2020-03-05 16:28:01--  http://www.asmscience.org/docserver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?expires=1583455381&id=id&accname=guest&checksum=144E5CCC1FBEFB4FE35FE0EB035B2F50
Connecting to www.asmscience.org (www.asmscience.org)|104.26.5.204|:80... connected.
HTTP request sent, awaiting response...
  HTTP/1.1 301 Moved Permanently
  Date: Fri, 06 Mar 2020 00:28:01 GMT
  Content-Type: text/html; charset=iso-8859-1
  Transfer-Encoding: chunked
  Connection: close
  Location: https://www.asmscience.org/docserver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?expires=1583455381&id=id&accname=guest&checksum=144E5CCC1FBEFB4FE35FE0EB035B2F50
  CF-Cache-Status: DYNAMIC
  Vary: Accept-Encoding
  Server: cloudflare
  CF-RAY: 56f7f44bff4c6be4-SJC
Location: https://www.asmscience.org/docserver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?expires=1583455381&id=id&accname=guest&checksum=144E5CCC1FBEFB4FE35FE0EB035B2F50 [following]
--2020-03-05 16:28:01--  https://www.asmscience.org/docserver/fulltext/10.1128/9781683670247/9781683670230_FM.pdf?expires=1583455381&id=id&accname=guest&checksum=144E5CCC1FBEFB4FE35FE0EB035B2F50
Connecting to www.asmscience.org (www.asmscience.org)|104.26.5.204|:443... connected.
HTTP request sent, awaiting response...
  HTTP/1.1 200 OK
  Date: Fri, 06 Mar 2020 00:28:01 GMT
  Content-Type: application/pdf
  Content-Length: 2439054
  Connection: close
  Set-Cookie: AWSALB=TmYKSkNod5sQxgBWqATBiVRVMEXVvsb69ecyWuJXgfNc1XSKzlUZAHY5pDrhIXF4TtvXqAQ9DBMSxZyyb55xlIrR8aSNztnekq9RiJ5ZUkca/Re0SiyltiZcFVu+; Expires=Fri, 13 Mar 2020 00:28:01 GMT; Path=/
  Set-Cookie: AWSALBCORS=TmYKSkNod5sQxgBWqATBiVRVMEXVvsb69ecyWuJXgfNc1XSKzlUZAHY5pDrhIXF4TtvXqAQ9DBMSxZyyb55xlIrR8aSNztnekq9RiJ5ZUkca/Re0SiyltiZcFVu+; Expires=Fri, 13 Mar 2020 00:28:01 GMT; Path=/; SameSite=None; Secure
  Set-Cookie: JSESSIONID=V8yHyQjW_A9fpSolG5o2P96B.asmlive-10-241-2-73; path=/docserver
  CF-Cache-Status: DYNAMIC
  Expect-CT: max-age=604800, report-uri="https://report-uri.cloudflare.com/cdn-cgi/beacon/expect-ct"
  Vary: Accept-Encoding
  Server: cloudflare
  CF-RAY: 56f7f44d2ff99316-SJC
Length: ignored [application/pdf]
Saving to: ‘delete.pdf’

delete.pdf                                      [   <=>                                                                                    ]   2.33M  3.88MB/s    in 0.6s

2020-03-05 16:28:02 (3.88 MB/s) - ‘delete.pdf’ saved [2439054]

(python27) DN0a2330b4:Desktop feili18$

 */
  @Override
  public String additionalNormalization(String url, ArchivalUnit au) throws PluginException {

    if (url.contains(";jsessionid=")) {
      url = url.replaceFirst(";jsessionid=[^?]+", "");
    }
    
    Matcher ftMat = FULLTEXT_URL_PATTERN.matcher(url);
    log.debug3("Fei: about to norm: " + url);
    // journals - the tocpdf doesn't have a crawler equivalent.  Pass it through for url consumption
    // books - there are no fulltext links on the book landing page
    if (ftMat.matches() && (!(url.contains("/toc.pdf")))) {
      log.debug3("Fei: full text url: " + url);
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
      log.debug3("Fei: normalized to: " + url);
    } 
    // just remove any undesired suffixes
    // if the suffix doesn't exist the url doesn't change, no sense wasting
    // cycles checking if it exists before trying to remove
    // for the URLs that flow through - this may still be an unnecessary suffix
     url = StringUtils.substringBeforeLast(url, "&isFastTrackArticle=");
    return url;
  }

}
