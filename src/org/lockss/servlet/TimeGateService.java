/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.servlet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The TimeGateService servlet redirects Accept-Datetime GET requests to the most
 * appropriate document on LOCKSS. Complies with the Memento spec
 * (draft-vandesompel-memento-06); citations are from the Memento spec unless
 * otherwise written. flow sequence:
 * <pre>
 * UA --- HTTP HEAD/GET; Accept-Datetime: T --------------------- URI-G
 * UA <-- HTTP 302; Location: URI-M; Vary; Link: URI-R, URI-T --- URI-G
 * Response Headers:
 *  Vary: accept-datetime    Req, 1
 *  Link:
 *    URI-R (original)      Req, 1
 *    URI-T (timegate)      Rec (0..N)
 *    URI-M (memento)       Opt (0..N)
 *
 * Sample query:
 *    HEAD /timegate/http://a.example.org/ HTTP/1.1
 *    Host: arxiv.example.net
 *    Accept-Datetime: Tue, 11 Sep 2001 20:35:00 GMT
 *    Connection: close
 * Sample response:
 *    HTTP/1.1 302 Found
 *    Date: Thu, 21 Jan 2010 00:02:14 GMT
 *    Server: Apache
 *    Vary: accept-datetime
 *    Location:
 *      http://arxiv.example.net/web/20010911203610/http://a.example.org/
 *    Link: <http://a.example.org/>; rel="original",
 *      <http://arxiv.example.net/timemap/http://a.example.org/>
 *        ; rel="timemap"; type="application/link-format"
 *        ; from="Tue, 15 Sep 2000 11:28:26 GMT"
 *        ; until="Wed, 20 Jan 2010 09:34:33 GMT"
 *    Content-Length: 0
 *    Content-Type: text/plain; charset=UTF-8
 *    Connection: close
 * </pre>
 */
public class TimeGateService extends TimeServlet {
  protected static final Logger log = Logger.getLogger("TimeGateService");

  /**
   * Given a CachedUrl, return the ServeContent URL for accessing it. The result
   * will be a request for a Memento: a ServeContent request with url,
   * au id and version params.
   * <p/>
   * Accesses cu but does not release it.
   *
   * @param cu a CachedUrl with content
   *
   * @return the ServeContent URL for accessing that CachedUrl
   *
   * @throws MalformedURLException if the request URL has no prefix
   */
  private static String getServeContentUrl(CachedUrl cu, HttpServletRequest req)
                                           throws MalformedURLException {
    StringBuilder sb = new StringBuilder();
    sb.append(UrlUtil.getUrlPrefix(UrlUtil.getRequestURL(req)));
    sb.append("ServeContent?url=").append(UrlUtil.encodeUrl(cu.getUrl()));
    sb.append("&auid=");
    sb.append(UrlUtil.encodeUrl(cu.getArchivalUnit().getAuId()));
    sb.append("&version=").append(cu.getVersion());
    return sb.toString();
  }

  /**
   * Redirect the agent to the best version of the requested URL from the
   * requested time, as specified in section 4.2.1.
   */
  @Override
  protected void lockssHandleRequest() throws ServletException, IOException {
    checkValidRequest();
    // Make sure request headers are right, and parse Accept-Datetime.
    long   acceptDatetime;
    String acceptDatetimeStr = req.getHeader("Accept-Datetime");
    try {
      // getDateHeader returns minus one if no value is specified and the
      // most recent item will be selected.
      acceptDatetime = req.getDateHeader("Accept-Datetime");
    } catch (IllegalArgumentException ex) {
      sendShortMessage(HttpServletResponse.SC_BAD_REQUEST,
        "Couldn't parse Accept-Datetime header: " + acceptDatetimeStr);
      return;
    }
    if (log.isDebug2()) {
      log.debug2("Memento Accept-Datetime parsed as: " + acceptDatetime);
    }

    /*
     * Without blankIfNull, url will be null if the browser provides the empty
     * string as the url parameter. And if we let url remain null, then
     * findCachedUrls will cause us to display a 500 error instead of a 404.
     */
    String url = blankIfNull(getParameter("url"));
    Collection<CachedUrl> cachedUrls = pluginMgr.findCachedUrls(url);

    // These two Vary headers are required by section 3.2.2.1.
    resp.addHeader("Vary", "accept-datetime");

    if (cachedUrls.isEmpty()) {
      /*
       * We don't have any content for this URL. Respond with 404, as required
       * by section 3.2.2.7 -- that is, with the two Vary headers but none of
       * the Link headers.
       */
      sendShortMessage(HttpServletResponse.SC_NOT_FOUND,
                       "This LOCKSS box does not have any copies of " + url);
    }
    else {

      // Add the required Link headers to the response (section 3.2.2.1).
      CuTimeMap timeMap;
      try {
        timeMap = new CuTimeMap(cachedUrls, acceptDatetime);
      }
      catch(ParseException ex) {
        throw new RuntimeException(
          "The LOCKSS cache has an unparseable date stamp.");
      }
      catch(IllegalArgumentException e) {
        throw new RuntimeException(e);
      }
      Iterable<String> linkHeadersToAdd = makeLinkHeaders(timeMap);
      for (String linkText : linkHeadersToAdd) {
        resp.addHeader("Link", linkText);
      }

      // Redirect to pointers.thisMemento.
      String destUrl = getServeContentUrl(timeMap.selected().cu, req);
      if (log.isDebug()) {
        log.debug("Redirecting request for " +
                      acceptDatetimeStr + " to " + destUrl);
      }
      send302Found(destUrl);
    }
  }

  /**
   * Sends the browser a 302 Found redirect to the given destination. Keeps
   * any headers that have already been added to the response. If the request is
   * a GET, also provides a page with a text note
   * "The requested version is here" with "here" being a hyperlink to the given
   * destination URL.
   * <p/>
   * Overlaps in functionality with HttpServletResponse.sendRedirect, except
   * uses "Found" as the status description, as required by the HTTP/1.1 and
   * Memento specs, rather than "Moved Temporarily".
   *
   * @param destUrl the URL to redirect to
   *
   * @throws IOException if the page cannot be written to the response
   */
  private void send302Found(String destUrl) throws IOException {
    resp.setStatus(HttpServletResponse.SC_FOUND);
    resp.setHeader("Location", destUrl);
    /*
     * Following RFC 2616 (HTTP 1.1) section 10.3.3, the redirect response has a
     * note with a link to the destination.
     */
    StringBuilder msg = new StringBuilder()
        .append("The requested version is <a href=\"").append(destUrl)
        .append("\">here</a>.");
    oneLineMessagePage(msg.toString());
  }

  /**
   * Returns a Collection of Link headers compatible with section 2.2.1.
   * <p/>
   * Accesses but does not release the CachedUrls in pointers.
   *
   * @param timeMap the CuTimeMap with the relevant Mementos we
   *                want to link to
   */
  Collection<String> makeLinkHeaders(CuTimeMap timeMap) {
    Collection<String> result = new ArrayList<String>();
    /*
     * Section 2.2.1 talks about the link headers. Below is a comment for each
     * of its four subsections.
     */

    // 2.2.1.1  Relation Type "original": link to publisher.
    result.add("<" + timeMap.selected().url() + ">; rel=\"original\"");

    /*
     * 2.2.1.2 Relation Type "timegate": not required since we have no other
     * TimeGates to link to.
     */

    // 2.2.1.3  Relation Type "timemap": link to LOCKSS's TimeMapService.

    String prefix; // the URL for accessing this LOCKSS server
    try {
      prefix = UrlUtil.getUrlPrefix(UrlUtil.getRequestURL(req));
    } catch (MalformedURLException ex) {
      // Jetty is giving us a URL without a prefix.  This shouldn't happen.
      throw new RuntimeException(
               "The server unexpectedly failed to parse the request URL.");
    }
    CuTimeMap.CuMemento first = timeMap.first();
    CuTimeMap.CuMemento last = timeMap.last();

    result.add(timeMapLink(timeMap.selected().url(), false, prefix,
        first.time, last.time));

    /*
     * The five navigation pointers from section 2.2.1.4, using one set of
     * suggested link names from section 2.2.2.
     */
    result.add(mementoLink(first, FIRST_MEM, prefix));
    if (timeMap.prev() != null) {
      result.add(mementoLink(timeMap.prev(), PREV_MEM, prefix));
    }
    result.add(mementoLink(timeMap.selected(), MEMENTO, prefix));

    if (timeMap.next() != null) {
      result.add(mementoLink(timeMap.next(), NEXT_MEM, prefix));
    }
    result.add(mementoLink(last, LAST_MEM, prefix));

    return result;
  }

}
