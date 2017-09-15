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
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import javax.servlet.*;

import static org.lockss.db.SqlConstants.*;

import org.apache.commons.collections.*;
import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.daemon.OpenUrlResolver.OpenUrlInfo;
import org.lockss.db.*;
import org.lockss.exporter.biblio.*;
import org.lockss.exporter.counter.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.PluginManager.CuContentReq;
import org.lockss.entitlement.EntitlementRegistryClient;
import org.lockss.entitlement.PublisherWorkflow;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.mortbay.html.*;
import org.mortbay.http.*;

@SuppressWarnings("serial")
public class EntitlementCheckServeContent extends ServeContent {

  private static final Logger log = Logger.getLogger(EntitlementCheckServeContent.class);

  // If true, scope can be 'mocked' from the URL parameters. This is for testing purposes, and should never be true in production
  public static final String PARAM_MOCK_SCOPE = PREFIX + "mockScope";

  private static final String INSTITUTION_HEADER = "X-Lockss-Institution";

  public static final String INSTITUTION_SCOPE_SESSION_KEY = "scope";

  private static boolean mockScope = false;

  private PublisherWorkflow workflow;
  private String institution;
  private String issn;
  private String start;
  private String end;
  private EntitlementRegistryClient entitlementRegistry;

  // don't hold onto objects after request finished
  protected void resetLocals() {
    workflow = null;
    institution = null;
    issn = null;
    start = null;
    end = null;
    super.resetLocals();
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    LockssDaemon daemon = getLockssDaemon();
    entitlementRegistry = daemon.getCachedEntitlementRegistryClient();
  }

  /** Called by ServletUtil.setConfig() */
  static void setConfig(Configuration config,
                        Configuration oldConfig,
                        Configuration.Differences diffs) {
      ServeContent.setConfig(config, oldConfig, diffs);
    if (diffs.contains(PREFIX)) {
      mockScope = config.getBoolean(PARAM_MOCK_SCOPE, false);
    }
  }

  protected boolean isNeverProxyForAu(ArchivalUnit archivalUnit) {
    return super.isNeverProxyForAu(archivalUnit) || workflow == PublisherWorkflow.PRIMARY_LOCKSS;
  }

  /**
   * Handle a request
   * @throws IOException
   */
  public void lockssHandleRequest() throws IOException {

    if ( mockScope ) {
      String userInstScope = req.getParameter(INSTITUTION_SCOPE_SESSION_KEY);
      if ( ! "".equals(userInstScope) ) {
        log.warning("Setting scope from parameters:"+userInstScope+" This should not be done in production");
        this.getSession().setAttribute(INSTITUTION_SCOPE_SESSION_KEY, userInstScope);
      }
    }

    updateInstitution();

    super.lockssHandleRequest();
  }

  protected boolean setCachedUrlAndAu() throws IOException {
    // Find a CU that the user is entitled to access, and with content
    List<CachedUrl> cachedUrls = pluginMgr.findCachedUrls(url, CuContentReq.HasContent);
    if(cachedUrls != null && !cachedUrls.isEmpty()) {
      for(CachedUrl cachedUrl: cachedUrls) {
        try {
          if(isUserEntitled(cachedUrl, cachedUrl.getArchivalUnit())) {
            cu = cachedUrl;
            au = cu.getArchivalUnit();
            if (log.isDebug3()) log.debug("cu: " + cu + " au: " + au);
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
      if (!isUserEntitled(cu, au)) {
        handleUnauthorisedUrlRequest(url);
        return;
      }
      workflow = getPublisherWorkflow(cu, au);
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

  @Override
  protected LockssUrlConnection openConnection(String url, LockssUrlConnectionPool pool) throws IOException {
    LockssUrlConnection conn = doOpenConnection(url, pool);
    conn.addRequestProperty(INSTITUTION_HEADER, (String) getSession().getAttribute(INSTITUTION_SCOPE_SESSION_KEY));
    return conn;
  }

  // Extracted out so that the connection can be mocked in test classes
  protected LockssUrlConnection doOpenConnection(String url, LockssUrlConnectionPool pool) throws IOException {
    return super.openConnection(url, pool);
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
      String institutionScope = (String) this.getSession().getAttribute(INSTITUTION_SCOPE_SESSION_KEY);
      institution = entitlementRegistry.getInstitution(institutionScope);
  }

  boolean isUserEntitled(CachedUrl cachedUrl, ArchivalUnit archivalUnit) throws IOException, IllegalArgumentException {
      validateBibInfo(cachedUrl, archivalUnit);

      return entitlementRegistry.isUserEntitled(issn, institution, start, end);
  }

  PublisherWorkflow getPublisherWorkflow(CachedUrl cachedUrl, ArchivalUnit archivalUnit) throws IOException, IllegalArgumentException {
      validateBibInfo(cachedUrl, archivalUnit);

      String publisher = entitlementRegistry.getPublisher(issn, institution, start, end);
      if(StringUtil.isNullString(publisher)) {
        throw new IllegalArgumentException("No publisher found");
      }

      return entitlementRegistry.getPublisherWorkflow(publisher);
  }

  private void setBibInfoFromBibliographicItem(BibliographicItem item) {
    log.debug2("Setting bib info from BibliographicItem");
    if(!StringUtil.isNullString(issn) && !StringUtil.isNullString(start) && !StringUtil.isNullString(end)) {
      log.debug2("Bib info already set");
      return;
    }

    if(item == null) {
      log.debug2("No BibliographicItem");
      return;
    }

    if(StringUtil.isNullString(issn)) {
      issn = item.getIssn();
      log.debug("Setting issn to " + issn);
    }

    if(StringUtil.isNullString(start)) {
      // Despite being called StartYear, this is actually a full date
      start = item.getStartYear();
      log.debug("Setting start to " + start);
    }

    if(StringUtil.isNullString(end)) {
      end = item.getEndYear();
      log.debug("Setting end to " + end);
    }
  }

  private void setBibInfoFromMetadata(ArticleMetadata md) {
    log.debug2("Setting bib info from TDB");
    if(!StringUtil.isNullString(issn) && !StringUtil.isNullString(start) && !StringUtil.isNullString(end)) {
      log.debug2("Bib info already set");
      return;
    }

    if(md == null) {
      log.debug2("No ArticleMetadata");
      return;
    }

    BibliographicItemImpl item = new BibliographicItemImpl();
    item.setPrintIssn(md.get("issn"));
    item.setEissn(md.get("eissn"));
    item.setIssnL(md.get("issnl"));
    item.setYear(md.get("date"));
    setBibInfoFromBibliographicItem(item);
  }

  private void setBibInfoFromTdb(ArchivalUnit archivalUnit) throws IllegalArgumentException {
    log.debug2("Setting bib info from TDB");
    if(!StringUtil.isNullString(issn) && !StringUtil.isNullString(start) && !StringUtil.isNullString(end)) {
      log.debug2("Bib info already set");
      return;
    }

    if(archivalUnit == null) {
      log.debug2("No ArchivalUnit");
      return;
    }

    TdbAu tdbAu = archivalUnit.getTdbAu();
    if(tdbAu == null) {
      log.debug2("No TdbAu");
      return;
    }

    if(StringUtil.isNullString(issn)) {
      issn = tdbAu.getIssn();
      log.debug("Setting issn to " + issn);
    }

    if(StringUtil.isNullString(start)) {
      start = tdbAu.getStartYear();
      if(!StringUtil.isNullString(start)) {
          start += "0101";
      }
      log.debug("Setting start to " + start);
    }

    if(StringUtil.isNullString(end)) {
      end = tdbAu.getEndYear();
      if(!StringUtil.isNullString(end)) {
          end += "1231";
      }
      log.debug("Setting end to " + end);
    }
  }

  private void setBibInfoFromMetadataDB(CachedUrl cachedUrl) {
    log.debug2("Setting bib info from database");
    if(!StringUtil.isNullString(issn) && !StringUtil.isNullString(start) && !StringUtil.isNullString(end)) {
      log.debug2("Bib info already set");
      return;
    }

    if(cachedUrl == null) {
      log.debug2("No CachedUrl");
      return;
    }

    Connection conn = null;
    OpenUrlInfo resolved = null;
    try {
      LockssDaemon daemon = getLockssDaemon();
      conn = daemon.getDbManager().getConnection();

      StringBuilder select = new StringBuilder("select distinct ");
      StringBuilder from = new StringBuilder(" from ");
      StringBuilder where = new StringBuilder(" where ");
      ArrayList<String> args = new ArrayList<String>();

      // return all related values for debugging purposes
      select.append("mi1." + DATE_COLUMN);
      select.append(",i." + ISSN_COLUMN);
      select.append(",i." + ISSN_TYPE_COLUMN);

      from.append(URL_TABLE + " u");
      from.append(", " + MD_ITEM_TABLE + " mi1");              // publication md_item
      from.append("," + MD_ITEM_TABLE + " mi2");        // article md_item
      from.append("," + ISSN_TABLE + " i");

      where.append("u." + URL_COLUMN + "= ?");
      args.add(cachedUrl.getUrl());
      where.append(" and u." + FEATURE_COLUMN + "='Access'");

      where.append(" and u." + MD_ITEM_SEQ_COLUMN + "=");
      where.append("mi1." + MD_ITEM_SEQ_COLUMN);

      where.append(" and mi1." + PARENT_SEQ_COLUMN + "=");
      where.append("mi2." + MD_ITEM_SEQ_COLUMN);

      where.append(" and i." + MD_ITEM_SEQ_COLUMN + "=");
      where.append("mi2." + MD_ITEM_SEQ_COLUMN);

      String query = select.toString() + from.toString() + where.toString();

      PreparedStatement stmt = daemon.getDbManager().prepareStatement(conn, query);

      for (int i = 0; i < args.size(); i++) {
        log.debug3("query arg:  " + args.get(i));
        stmt.setString(i + 1, args.get(i));
      }

      ResultSet resultSet = daemon.getDbManager().executeQuery(stmt);

      BibliographicItemImpl item = new BibliographicItemImpl();
      while ( resultSet.next() ) {
        String year = resultSet.getString(1);
        String issn = resultSet.getString(2);
        String issnType = resultSet.getString(3);
        item.setYear(year);
        if(P_ISSN_TYPE.equals(issnType)){
          item.setPrintIssn(issn);
        }
        else if(E_ISSN_TYPE.equals(issnType)){
          item.setEissn(issn);
        }
        else if(L_ISSN_TYPE.equals(issnType)){
          item.setIssnL(issn);
        }
      }
      setBibInfoFromBibliographicItem(item);

    } catch (DbException e) {
      log.error("Error fetching metadata", e);
    } catch (SQLException e) {
      log.error("Error fetching metadata", e);
    }
  }

  private void validateBibInfo(CachedUrl cachedUrl, ArchivalUnit archivalUnit) {
     setBibInfoFromMetadataDB(cachedUrl);
     setBibInfoFromTdb(archivalUnit);

     if(StringUtil.isNullString(issn)) {
       throw new IllegalArgumentException("ArchivalUnit has no ISSN");
     }
     if(StringUtil.isNullString(start)) {
       throw new IllegalArgumentException("ArchivalUnit has no start year");
     }
     if(StringUtil.isNullString(end)) {
       throw new IllegalArgumentException("ArchivalUnit has no end year");
     }
  }

  void logAccess(String url, String msg) {
      super.logAccess(url, "UA: \"" + req.getHeader("User-Agent") + "\" " + msg);
  }
}

