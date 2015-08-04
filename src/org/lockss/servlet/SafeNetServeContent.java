/*
 * $Id$
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

package org.lockss.servlet;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import javax.servlet.*;

import org.apache.commons.collections.*;
import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.exporter.counter.*;
import org.lockss.plugin.*;
import org.lockss.plugin.PluginManager.CuContentReq;
import org.lockss.safenet.EntitlementRegistryClient;
import org.lockss.safenet.PublisherWorkflow;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.mortbay.html.*;
import org.mortbay.http.*;

@SuppressWarnings("serial")
public class SafeNetServeContent extends ServeContent {

  private static final Logger log = Logger.getLogger(SafeNetServeContent.class);

  private static final String INSTITUTION_HEADER = "X-SafeNet-Institution";

  private PublisherWorkflow workflow;
  private String institution;
  private EntitlementRegistryClient entitlementRegistry;

  // don't hold onto objects after request finished
  protected void resetLocals() {
    workflow = null;
    super.resetLocals();
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    LockssDaemon daemon = getLockssDaemon();
    entitlementRegistry = daemon.getEntitlementRegistryClient();
  }

  /** Called by ServletUtil.setConfig() */
  static void setConfig(Configuration config,
                        Configuration oldConfig,
                        Configuration.Differences diffs) {
      ServeContent.setConfig(config, oldConfig, diffs);
    if (diffs.contains(PREFIX)) {
    }
  }

  protected boolean isNeverProxyForAu(ArchivalUnit au) {
    return super.isNeverProxyForAu(au) || workflow == PublisherWorkflow.PRIMARY_SAFENET;
  }

  /**
   * Handle a request
   * @throws IOException
   */
  public void lockssHandleRequest() throws IOException {
    updateInstitution();

    super.lockssHandleRequest();
  }

  protected boolean setCachedUrlAndAu() throws IOException {
    // Find a CU that the user is entitled to access, and with content
    List<CachedUrl> cachedUrls = pluginMgr.findCachedUrls(url, CuContentReq.HasContent);
    if(cachedUrls != null && !cachedUrls.isEmpty()) {
      for(CachedUrl cachedUrl: cachedUrls) {
        try {
          if(isUserEntitled(cachedUrl.getArchivalUnit())) {
            cu = cachedUrl;
            au = cu.getArchivalUnit();
            if (log.isDebug3()) log.debug3("cu: " + cu + " au: " + au);
            break;
          }
        }
        catch (IOException e) {
          // We can't communicate with the ER, so we have to assume that we can't give the user access to the content at the moment
          log.error("Error communicating with entitlement registry: " + e);
          handleEntitlementRegistryErrorUrlRequest(url);
          return false;
        }
        catch (IllegalArgumentException e) {
          // We don't have enough information about the AU to determine if the user is entitled, but there's nothing they can do about it
          log.error("Error with AU configuration: " + e);
          handleMissingUrlRequest(url, PubState.KnownDown);
          return false;
        }
      }
      if(cu == null) {
        // We found at least one CachedUrl, which means the content is preserved, but the user wasn't entitled to any of them
        handleUnauthorisedUrlRequest(url);
        return false;
      }
    }
    return true;
  }


  /**
   * Handle request for content that belongs to one of our AUs, whether or not
   * we have content for that URL.  If this request contains a version param,
   * serve it from cache with a Memento-Datetime header and no
   * link-rewriting.  For requests without a version param, rewrite links,
   * and serve from publisher if publisher provides it and the daemon options
   * allow it; otherwise, try to serve from cache.
   *
   * @throws IOException for IO errors
   */
  protected void handleAuRequest() throws IOException {
    try {
      if (!isUserEntitled(au)) {
        handleUnauthorisedUrlRequest(url);
        return;
      }
      workflow = getPublisherWorkflow(au);
      if (workflow == PublisherWorkflow.LIBRARY_NOTIFICATION) {
        handleUnauthorisedUrlRequest(url);
        return;
      }
    }
    catch (IOException e) {
      // We can't communicate with the ER, so we have to assume that we can't give the user access to the content at the moment
      log.error("Error communicating with entitlement registry: " + e);
      handleEntitlementRegistryErrorUrlRequest(url);
      return;
    }
    catch (IllegalArgumentException e) {
      // We don't have enough information about the AU to determine if the user is entitled, but there's nothing they can do about it
      log.error("Error with AU configuration: " + e);
      handleMissingUrlRequest(url, PubState.KnownDown);
      return;
    }

    super.handleAuRequest();
  }

  protected LockssUrlConnection doOpenConnection(String url, LockssUrlConnectionPool pool) throws IOException {
    return super.openConnection(url, pool);
  }

  protected LockssUrlConnection openConnection(String url, LockssUrlConnectionPool pool) throws IOException {
    LockssUrlConnection conn = doOpenConnection(url, pool);
    return conn;
  }

  protected void handleEntitlementRegistryErrorUrlRequest(String missingUrl)
      throws IOException {
    handleUrlRequestError(missingUrl, PubState.KnownDown, "An error occurred trying to access the requested URL on this LOCKSS box. This may be temporary and you may wish to report this, and try again later. ", HttpResponse.__503_Service_Unavailable, "entitlement registry error");
  }

  protected void handleUnauthorisedUrlRequest(String missingUrl)
      throws IOException {
    handleUrlRequestError(missingUrl, PubState.KnownDown, "You are not authorised to access the requested URL on this LOCKSS box. ", HttpResponse.__403_Forbidden, "unauthorised");
  }


  void updateInstitution() throws IOException {
      //This is currently called in lockssHandleRequest, it needs to be called from wherever we do the SAML authentication
      institutionScope = "ed.ac.uk";
      institution = entitlementRegistry.getInstitution(institutionScope);
  }

  boolean isUserEntitled(ArchivalUnit au) throws IOException, IllegalArgumentException {
      TdbAu tdbAu = au.getTdbAu();
      String issn = tdbAu.getIssn();
      if(StringUtil.isNullString(issn)) {
        throw new IllegalArgumentException("ArchivalUnit has no ISSN");
      }
      String start = tdbAu.getStartYear() + "0101";
      String end = tdbAu.getEndYear() + "1231";

      return entitlementRegistry.isUserEntitled(issn, institution, start, end);
  }

  PublisherWorkflow getPublisherWorkflow(ArchivalUnit au) throws IOException, IllegalArgumentException {
      TdbAu tdbAu = au.getTdbAu();
      String issn = tdbAu.getIssn();
      if(StringUtil.isNullString(issn)) {
        throw new IllegalArgumentException("ArchivalUnit has no ISSN");
      }
      String start = tdbAu.getStartYear() + "0101";
      String end = tdbAu.getEndYear() + "1231";

      String publisher = entitlementRegistry.getPublisher(issn, institution, start, end);
      if(StringUtil.isNullString(publisher)) {
        throw new IllegalArgumentException("No publisher found");
      }

      return entitlementRegistry.getPublisherWorkflow(publisher);
  }

  void logAccess(String url, String msg) {
      super.logAccess(url, "UA: \"" + req.getHeader("User-Agent") + "\" " + msg);
  }
}

