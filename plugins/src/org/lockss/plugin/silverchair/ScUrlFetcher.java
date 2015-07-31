/*
 * $Id: DispatchingUrlFetcher.java 39864 2015-02-18 09:10:24Z thib_gc $
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
package org.lockss.plugin.silverchair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.base.BaseUrlFetcher;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;
import org.lockss.util.urlconn.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScUrlFetcher extends BaseUrlFetcher {

  protected static final Pattern PATTERN_POST =
    Pattern.compile("&post=json$",
                    Pattern.CASE_INSENSITIVE);

  private static final Logger log = Logger.getLogger("ScDispatchingUrlFetcher");


  public ScUrlFetcher(final CrawlerFacade crawlFacade,
                                 final String url) {
    super(crawlFacade, url);
  }

  @Override
  protected LockssUrlConnection makeConnection(String url,
                                               LockssUrlConnectionPool pool)
    throws IOException {
    LockssUrlConnection res;

    Matcher postMatch = PATTERN_POST.matcher(url);
    if (postMatch.find()) {
      log.info("found post url:" + url);
      res = makePostConnection(url, pool);
    }
    else {
      res = makeConnection0(url, pool);
    }
    String cookiePolicy = au.getCookiePolicy();
    if (cookiePolicy != null) {
      res.setCookiePolicy(cookiePolicy);
    }
    return res;
  }

  /**
   * make a POST connection using the url which has appended the POST payload as query arguments.
   * EXtract the pdf url from the POST results and return a connection to it.
   * @param url the url we will be posting to
   * @param pool  the Connection pool for this au
   * @return a new connection to the pdf url
   * @throws IOException
   */
  protected LockssUrlConnection makePostConnection(String url,
                                                   LockssUrlConnectionPool pool)
    throws IOException {
    LockssUrlConnection conn;

    // strip the arguments from the url and turn into json equivalent
    String baseurl = UrlUtil.stripQuery(url);
    // get the connection and add the payload
    conn = openConnection(PostHttpClientUrlConnection.METHOD_POST,
                                  baseurl,
                                  pool);
    conn.setRequestProperty("content-type", "application/json");
    StringRequestEntity reqEnt = new StringRequestEntity(queryToJsonString(url));
    ((PostHttpClientUrlConnection)conn).setRequestEntity(reqEnt);
//    // execute and make a second non-post connection
//    pauseBeforeFetch();
//    conn.execute();
//    checkConnectException(conn);
//    String ctype = conn.getResponseContentType();
//    String mimeType = HeaderUtil.getMimeTypeFromContentType(ctype);
//    if ("application/json".equalsIgnoreCase(mimeType)) {
//      InputStream in = conn.getResponseInputStream();
//      String pdf_url = UrlUtil.getHost(url) + extractUrlFromInput(in);
//      conn.release();
//      conn = makeConnection0(pdf_url, pool) ;
//    }
    return conn;
  }

  /**
   * convert the query portion of a GET url into  json  for POST
   * @param url the url with the json arguments as GET query arguments
   * @return the json string to be sent in the POST
   * @throws IOException thrown if the url is foobar
   */
  protected String queryToJsonString(String url) throws IOException {
    org.apache.commons.httpclient.URI uri =
      new org.apache.commons.httpclient.URI(url, true);
    String query = uri.getQuery();
    String[] pairs = query.split("&");
    StringBuilder sb = new StringBuilder();

    // there should be a minimum of two arguments the first is real, the second is virtual
    int idx;
    String key;
    String value;
    for (int i = 0; i < pairs.length-1; i++) {
      idx = pairs[i].indexOf("=");
      key = URLDecoder.decode(pairs[i].substring(0, idx), "UTF-8");
      value = URLDecoder.decode(pairs[i].substring(idx + 1), "UTF-8");
      if("json".equals(key)) {
        return value;
      }
    }
    return "";
  }

  /**
   * A mirror of the UrlUtil#openConnection that uses PostHttpClientUrlConnection instead of the
   * standard HttpClientUrlConnection until we have a released version with POST capability
   * @param methodCode the method to use for the connection POST or GET
   * @param urlString the url to which we will connect
   * @param connectionPool  the connection pool
   * @return a new LockssUrlConnection
   * @throws IOException if any of the HttpConnection calls fail
   */
  LockssUrlConnection openConnection(int methodCode, String urlString,
                 LockssUrlConnectionPool connectionPool)
    throws IOException {
    LockssUrlConnection luc;
    HttpClient client;
    if (connectionPool != null) {
      client = connectionPool.getHttpClient();
    } else {
      client = new HttpClient();
    }
    luc = new PostHttpClientUrlConnection(methodCode, urlString, client,
                                      connectionPool);
    return luc;
  }
}
