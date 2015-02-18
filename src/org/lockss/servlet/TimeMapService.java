/*
 * $Id$
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

package org.lockss.servlet;

import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.servlet.CuTimeMap.CuMemento;
import org.lockss.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * The TimeMapService servlet links to all versions of a URI that LOCKSS knows about.
 * It follows the Memento spec (draft-vandesompel-memento-04); citations are
 * from the Memento spec unless otherwise written.
 */
public class TimeMapService extends TimeServlet {

  private static final Logger log = Logger.getLogger(TimeMapService.class);
  
  @Override
  protected void lockssHandleRequest() throws ServletException, IOException {
    checkValidRequest();
    /*
     * As in TimeGateService, we call blankIfNull here because getParameter will set
     * url to be null if the user provides the empty string, and that causes us
     * to return a 500 error instead of 404.
     */
    String url = blankIfNull(getParameter("url"));
    Collection<CachedUrl> cachedUrls = pluginMgr.findCachedUrls(url);

    if (cachedUrls.isEmpty()) {
      // No content, so 404.
      sendShortMessage(HttpServletResponse.SC_NOT_FOUND,
          "This LOCKSS box does not have any copies of " + url);
    }
    else {
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("application/link-format");
      /*
       * Optional reverse link header between this TimeMapService and the original
       * resource, as described in 3.4.2 ("However, in case a TimeMapService...").
       */
      StringBuilder linkBody = new StringBuilder().append("<");
      linkBody.append(UrlUtil.getRequestURL(req)).append(">; anchor=\"");
      linkBody.append(url);
      linkBody.append("\"; rel=\"timemap\"; type=\"application/link-format\"");
      resp.addHeader("Link", linkBody.toString());

      Collection<String> links =
          timeMapLinks(cachedUrls, UrlUtil.getRequestURL(req), url);

      StringBuilder pageBuilder = new StringBuilder(1000);
      pageBuilder.append(StringUtil.separatedString(links, ",\n"));
      resp.setContentLength(pageBuilder.length());
      resp.setHeader("connection", "close");
      Writer writer = resp.getWriter();
      writer.write(pageBuilder.toString());
    }
  }

  /**
   * Returns all links that should be part of a TimeMapService response, specified by
   * section 3.4. Includes:
   * <p/>
   * <ul> <li>the original resource that this TimeMapService is about</li> <li>all
   * time-stamp-having versions of all elements of cachedUrls</li> <li>the
   * TimeMapService for the requested URL</li> <li>this TimeGateService</li> </ul>
   * <p/>
   * Releases any CachedUrls accessed.
   *
   * @param cachedUrls a collection CachedUrls, each from its own AU, containing
   * content for the same URL
   * @param requestUrl the URL of this web request, for generating links to self
   * and TimeGateService
   * @param origResource the URL parameter in the original web request
   */
  private Collection<String> timeMapLinks(Collection<CachedUrl> cachedUrls,
                                          String requestUrl,
                                          String origResource) {

    Collection<CachedUrl[]> versionArrays =
        CuTimeMap.getVersionArrays(cachedUrls);
    int resultLength = 0; // an overestimate of the result length
    for (CachedUrl[] array : versionArrays) {
      resultLength += array.length;
    }
    Collection<String> result = new ArrayList<String>(resultLength);

    // URI-R
    result.add("<" + origResource + ">; rel=\"original\"");

    try {
      String prefix = UrlUtil.getUrlPrefix(requestUrl);
      CuTimeMap timemap = new CuTimeMap(cachedUrls);
      CuMemento first = timemap.first();
      CuMemento last = timemap.last();
      // URI-T: link to this TimeMap Service
      result.add(timeMapLink(origResource, true, prefix,first.time,last.time));

      // URI-G: link to TimeGate Service
      result.add(timeGateLink(origResource, prefix));

      // First & Last memento(URI-N).  Add them both even if they're equal.
      result.add(mementoLink(first, FIRST_MEM, prefix));
      result.add(mementoLink(last, LAST_MEM, prefix));

      for (CachedUrl[] cuArray : versionArrays) {
        for (int ix=0; ix< cuArray.length; ix++) {
          CachedUrl cu = cuArray[ix];
          try {
            Date time = cuTime(cu);
            /**
             * Add mem to the TimeMapService unless it's First or Last. This
             * suffices for avoiding duplicates (except when firstMemento ==
             * lastMemento, which we allow) because the arrays in
             * versionArrays are disjoint because each element of cachedUrls
             * is from a different AU.
             */
            CuMemento mem = new CuMemento(cu, time, ix);
            if (!mem.sameAuAndVersion(timemap.first()) &&
                !mem.sameAuAndVersion(timemap.last())) {
              result.add(mementoLink(mem, MEMENTO, prefix));
            }
          }
          catch (IllegalArgumentException ex) {
          /* Since cu lacks time stamps, we ignore it.*/
            if(log.isDebug2())
              log.debug2("no timestamp available for cu:" + cu);
          }
          AuUtil.safeRelease(cu);
        }
      }
      return result;
    }
    catch (MalformedURLException ex) {

      // This shouldn't happen, because the request reached the servlet.
      throw new AssertionError("URL is invalid but also reached servlet: " +
                               requestUrl);
    }
  }
}
