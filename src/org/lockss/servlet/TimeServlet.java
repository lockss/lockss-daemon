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

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginManager;
import org.lockss.servlet.CuTimeMap.CuMemento;
import org.lockss.util.CIProperties;
import org.lockss.util.DateTimeUtil;
import org.lockss.util.UrlUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;

/**
 An abstract servlet with methods for creating Memento link headers and error
 pages that comply with the Memento spec (draft-vandesompel-memento-06);
 citations are from the Memento spec.

 Original Resource: An Original Resource is a resource that exists or
 used to exist, and for which access to one of its prior states may be required.
 URI-R is used to denote the URI of an Original Resource.

 Memento: A Memento for an Original Resource is a resource that
 encapsulates a prior state of the Original Resource. A Memento for an
 Original Resource as it existed at time T is a resource that encapsulates the
 state the Original Resource had at time T.
 URI-M is used to denote the URI of a Memento.


 TimeGate: A TimeGate for an Original Resource is a resource (service) that is
 capable of datetime negotiation to support access to prior states of the
 Original Resource.
 URI-T is used to denote the URI of a TimeMapService.

 TimeMap: A TimeMap for an Original Resource is a resource (service) from which
 a list of URIs of Mementos of the Original Resource is available.
 URI-G is used to denote the URI of a TimeGateService.
 */

public abstract class TimeServlet extends LockssServlet {

  public static final String MEMENTO = "memento";
  public static final String FIRST_MEM = "first memento";
  public static final String LAST_MEM = "last memento";
  public static final String PREV_MEM = "prev memento";
  public static final String NEXT_MEM = "next memento";
  /** For finding AUs that have content for a given URL. */
  protected PluginManager pluginMgr;


  /**
   * override standard servlet config
   *
   * @param config configuration object send to this servlet
   * @throws ServletException thrown if unable to initialize servlet
   */
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    pluginMgr = getLockssDaemon().getPluginManager();
  }

  /**
   * Returns the CU's time stamp. This is either parsed from the publisher's
   * Last-Modified header, if it is provided at crawl-time, or else equals the
   * fetch time. If neither datum is available, throws IllegalArgumentException.
   * <p/>
   * Caller must ensure that cu is released afterwards.
   * <p/>
   * Precondition: cu must have a last-modified or fetch time which can be
   * parsed
   *
   * @param cu the CachedUrl whose time should be determined
   * @return cu's Last-Modified time, if possible, else fetch time
   * @throws IllegalArgumentException if both the CU's last-modified and fetch
   * time are missing or unparsable
   */
  public static Date cuTime(CachedUrl cu) throws IllegalArgumentException {
    Date result = null;
    /*
     * It seems PROPERTY_LAST_MODIFIED is stored as a human-readable string,
     * while PROPERTY_FETCH_TIME is stored as a string representation of a long.
     * We will assume that either might be missing or unparsable, though in
     * theory everything should have a parsable fetch time.
     */
    CIProperties props = cu.getProperties();
    // check 'last-modified'
    if (props.containsKey(CachedUrl.PROPERTY_LAST_MODIFIED)) {
      String last_mod = props.getProperty(CachedUrl.PROPERTY_LAST_MODIFIED);
      try {
        if (CuTimeMap.log.isDebug2()) {
          CuTimeMap.log.debug2("cuTime parsing CU value 'last-modified': "
                               + last_mod);
        }
        result = CuTimeMap.formatter.parse(last_mod);
      }
      catch (ParseException pe) {
        StringBuilder sb = new StringBuilder("CU ").append(cu).append(":");
        sb.append(": unparsable 'last-modified'=").append(last_mod);
        CuTimeMap.log.warning(sb.toString());
      }
    }
    // if we don't have last-modified, check 'fetch-time'
    if (result == null) {
      if (props.containsKey(CachedUrl.PROPERTY_FETCH_TIME)) {
        String fetch_time = props.getProperty(CachedUrl.PROPERTY_FETCH_TIME);
        if (CuTimeMap.log.isDebug2()) {
          CuTimeMap.log.debug2("cuTime parsing CU value 'fetch-time'="
                               + fetch_time);
        }
        try {
          // fetch_time is in UTC, because it was written with TimeBase.nowMs
          result = new Date(Long.parseLong(fetch_time));
        }
        catch (NumberFormatException ex) {
          StringBuilder sb = new StringBuilder("CU ").append(cu).append(":");
          sb.append(": unparsable 'fetch-time'=").append(fetch_time);
          CuTimeMap.log.warning(sb.toString());
        }
      }
    }
    // we weren't able to assign a time to this cu version fetch time
    if (result == null) {
      StringBuilder sb = new StringBuilder("CU ").append(cu).append(":");
      sb.append("last-modified & fetch-time are both missing or unparsable.");
      throw new IllegalArgumentException(sb.toString());
    }
    return result;
  }

  /**
   * Checks that the request to determine if the plugin manager is started
   * and the parameters are valid and displays and appropriate message
   * @throws IOException if the request is not valid.
   */
  protected void checkValidRequest() throws IOException {
    if (!pluginMgr.areAusStarted()) {
      displayNotStarted();
      return;
    }

    // Make sure URL params are right.
    if (!hasOnlyOneUrlParam()) {
      sendShortMessage(HttpServletResponse.SC_BAD_REQUEST,
      "TimeServlet requests must have exactly one URL parameter and no others");
    }
  }

  /**
   * If string is null, returns "".  Otherwise, returns string.
   * @param str the string to check for null
   * @return if str is null, return ""; otherwise simple return the string.
   */
  protected static String blankIfNull(String str) {
    return (str == null) ? "" : str;
  }

  /**
   * Constructs a link to a TimeGateService(URI-G) as specified by 2.2.1.2.
   * link points from the Original Resource(URI-R) or from a Memento (URI-M)
   * associated with the URI-R, to a TimeGate Service for the URI-R.
   * format:<code><daemonBase/timegate/URI-R>; rel="timegate"</code>
   *
   * @param origResource the URL of the resource this is a TimeGateService of
   * @param daemonPrefix the URL prefix (protocol, host, port) of this
   *        LOCKSS daemon
   * @return the text of a Link header pointing to the TimeGateService of
   *        origResource on this LOCKSS server
   */
  protected static String timeGateLink(String origResource,
                                       String daemonPrefix) {
    StringBuilder sb = new StringBuilder();

    sb.append("<").append(daemonPrefix);
    sb.append("timegate/").append(origResource);
    sb.append(">; rel=\"timegate\"");

    return sb.toString();
  }

  /**
   * Constructs a to a TimeMapService(URI-T) link as specified by section 2.2.1.3. The
   * boolean self should be true if the link is meant to be from a URI-T to
   * itself, otherwise false.
   * format:<code><daemonBase/timemap?url=URI-R>;
   *            rel="timemap"; type="application/link-format";
   *            from="firstdate"
   *            until="lastdate"
   *        </code>
   * @param origResource the URL of the resource this is a TimeMapService of
   * @param self true iff this header should be of rel "self timemap"; otherwise
   *        header will be "timemap"
   * @param daemonPrefix the URL prefix (protocol, host, port) of this
   *        LOCKSS daemon
   * @param firstDate  the datetime of the earliest memento (optional)
   * @param lastDate the datetime of the most recent memento (optional)
   * @return the text of a Link header pointing to the TimeMapService of origResource
   *         on this LOCKSS server
   */
  protected static String timeMapLink(String origResource,
                                      boolean self,
                                      String daemonPrefix,
                                      Date firstDate,
                                      Date lastDate) {
    StringBuilder sb = new StringBuilder();

    sb.append("<").append(daemonPrefix);
    sb.append("timemap/").append(origResource);
    sb.append(">; rel=\"").append((self ? "self" : "timemap")).append("\"; ");
    sb.append("type=\"application/link-format\"");
    if(firstDate != null && lastDate != null) {
      sb.append("; from=\"");
      sb.append(DateTimeUtil.GMT_DATE_FORMATTER.format(firstDate)).append("\"");
      sb.append("; until=\"");
      sb.append(DateTimeUtil.GMT_DATE_FORMATTER.format(lastDate)).append("\"");
    }
    return sb.toString();
  }

  /**
   * Construct a Link header to a Memento as specified by section 2.2.1.4. Links
   * to the given CachedUrl with the given relation type, and includes the
   * required "datetime" attribute indicating the CU's time stamp.
   * <code>
   * <daemonPrefix/ServeContent?url=URI-M>; rel="relationship"; datetime="date"
   * </code>
   *
   * @param cuMemento the memento from which to construct the url
   * @param rel the desired Link header's relation string
   * @param daemonPrefix the prefix for the daemon
   * @return the text of the Link header.
   * @throws IllegalArgumentException if the precondition is not met
   */
  protected static String mementoLink(CuMemento cuMemento,
                                      String rel,
                                      String daemonPrefix)
      throws IllegalArgumentException {

    CachedUrl cu = cuMemento.cu;
    Date date = cuMemento.time;
    StringBuilder sb = new StringBuilder();
    sb.append("<").append(daemonPrefix);
    sb.append("ServeContent?url=").append(UrlUtil.encodeUrl(cu.getUrl()));
    sb.append("&auid=");
    sb.append(UrlUtil.encodeUrl(cu.getArchivalUnit().getAuId()));
    sb.append("&version=").append(cu.getVersion());
    sb.append(">; rel=\"").append(rel);
    sb.append("\"; datetime=\"");
    sb.append(DateTimeUtil.GMT_DATE_FORMATTER.format(date)).append("\"");
    return sb.toString();
  }

  /**
   * Determine whether this request has more than one param or no 'url' param
   *
   * @return true if params > 1 or param key != 'url'
   */
  protected boolean hasOnlyOneUrlParam() {

    Collection<String> paramKeys = getParamsAsMap().keySet();
    return (paramKeys.size() == 1 && paramKeys.contains("url"));
  }

}
