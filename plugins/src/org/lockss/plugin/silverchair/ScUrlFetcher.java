/*
 * $Id: DispatchingUrlFetcher.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.base.HttpToHttpsUrlFetcher;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;
import org.lockss.util.urlconn.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URLs ending in "&post=json" are a signal from the link extractor to
 * issue a POST, whose body comes from the "json" query arg.
 */
public class ScUrlFetcher extends HttpToHttpsUrlFetcher {

  protected static final Pattern PATTERN_POST =
    Pattern.compile("&post=json$",
                    Pattern.CASE_INSENSITIVE);
  private static final Logger log = Logger.getLogger("ScUrlFetcher");

  private boolean doPost;

  public ScUrlFetcher(final CrawlerFacade crawlFacade,
                                 final String url) {
    super(crawlFacade, url);
    Matcher postMatch = PATTERN_POST.matcher(url);
    if (postMatch.find()) {
      doPost = true;
    }
  }

  @Override
  protected String getRequestUrl() throws IOException {
    String url = super.getRequestUrl();
    return doPost ? UrlUtil.stripQuery(url) : url;
  }

  @Override
  protected int getMethod() {
    return doPost ? LockssUrlConnection.METHOD_POST : super.getMethod();
  }

  @Override
  protected void customizeConnection(LockssUrlConnection conn)
      throws IOException {
    super.customizeConnection(conn);
    if (doPost) {
      conn.setRequestProperty("content-type", "application/json");
      conn.setRequestEntity(queryToJsonString(fetchUrl));
    }
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

}
