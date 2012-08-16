/*
 * $Id: CounterReportsRequestRecorder.java,v 1.2 2012-08-16 22:29:56 fergaloy-sf Exp $
 */

/*

 Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * Persists the request data necessary to create COUNTER reports.
 * 
 * @version 1.0
 */
package org.lockss.exporter.counter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.lockss.app.LockssDaemon;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbUtil;
import org.lockss.daemon.OpenUrlResolver;
import org.lockss.daemon.OpenUrlResolver.OpenUrlInfo;
import org.lockss.daemon.OpenUrlResolver.OpenUrlInfo.ResolvedTo;
import org.lockss.db.DbManager;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Constants;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;
import org.mortbay.http.HttpResponse;

public class CounterReportsRequestRecorder {
  private static final Logger log = Logger
      .getLogger("CounterReportsRequestRecorder");

  private static final String SQL_QUERY_METADATA_ID_FROM_URL =
      "select md_id from metadata where access_url = ?";

  // The singleton instance of this class.
  private static final CounterReportsRequestRecorder instance =
      new CounterReportsRequestRecorder();

  /**
   * The indication of whether the publisher was contacted or not.
   */
  public static enum PublisherContacted {
    TRUE, FALSE
  }

  // Cached instance of OpenUrlResolver, for efficiency.
  private OpenUrlResolver our = new OpenUrlResolver(
						    LockssDaemon
							.getLockssDaemon());

  /**
   * Constructor.
   * 
   * It is private so it cannot be instantiated by client code.
   */
  private CounterReportsRequestRecorder() {
  }

  /**
   * Provides the singleton instance.
   * 
   * @return a CounterReportsRequestRecorder with the singleton instance.
   */
  public static CounterReportsRequestRecorder getInstance() {
    return instance;
  }

  /**
   * Records a request that is the subject of a report.
   * 
   * @param url
   *          A String with the URL being requested.
   * @param au
   *          An ArchivalUnit corresponding to the request.
   * @param contacted
   *          A PublisherContacted with the indication of whether the publisher
   *          was contacted while processing the request.
   * @param publisherCode
   *          An int with the publisher involvement code.
   */
  public void recordRequest(String url, ArchivalUnit au,
      PublisherContacted contacted, int publisherCode) {
    try {
      final String DEBUG_HEADER = "recordRequest(): ";
      log.debug2(DEBUG_HEADER + "url = '" + url + "'.");

      // Get the metadata identifier of the URL.
      Long mdId = findMatchingFullTextMetadataId(url);

      // Do nothing more if it is not a request needed for any report.
      if (mdId == null) {
	return;
      }

      Map<String, Object> requestData = new HashMap<String, Object>();

      // Collect the necessary data related to the request.
      CounterReportsTitle title =
	  collectRequest(url, au, contacted, publisherCode, requestData);
      title.identify();

      // Persist the request data.
      LockssDaemon.getLockssDaemon().getCounterReportsManager()
	  .persistRequest(title, requestData);
      log.debug2(DEBUG_HEADER + "Done.");
    } catch (SQLException sqle) {
      log.error("Cannot persist request - Statistics not collected", sqle);
    } catch (Exception e) {
      log.error("Cannot persist request - Statistics not collected", e);
    }
  }

  /**
   * Provides the full text metadata identifier that corresponds to a URL.
   * 
   * @param url
   *          A String with the URL.
   * @return a Long with the full text metadata identifier, if any.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private Long findMatchingFullTextMetadataId(String url) throws SQLException {
    final String DEBUG_HEADER = "findMatchingFullTextMetadataId(): ";

    Connection conn = null;
    PreparedStatement getUrlMdId = null;
    ResultSet results = null;
    Long mdId = null;
    DbManager dbManager = LockssDaemon.getLockssDaemon().getDbManager();

    try {
      // Get the database connection.
      conn = dbManager.getConnection();

      // Prepare the query.
      getUrlMdId = conn.prepareStatement(SQL_QUERY_METADATA_ID_FROM_URL);
      getUrlMdId.setString(1, url);

      // Get any results.
      results = getUrlMdId.executeQuery();

      // Get the metadata identifier.
      if (results.next()) {
	mdId = results.getLong("md_id");
      }
    } finally {
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(getUrlMdId);
      DbManager.safeRollbackAndClose(conn);
    }

    log.debug2(DEBUG_HEADER + "mdId = '" + mdId + "'.");
    return mdId;
  }

  /**
   * Obtains the data needed to persist a request that is the subject of a
   * report.
   * 
   * @param url
   *          A String with the URL being requested.
   * @param au
   *          An ArchivalUnit corresponding to the request.
   * @param contacted
   *          A PublisherContacted with the indication of whether the publisher
   *          was contacted while processing the request.
   * @param publisherCode
   *          An int with the publisher response code.
   * @param requestData
   *          A Map<String, Object> with properties of the request to be
   *          persisted.
   * @return a CounterReportsTitle representing the title involved in the
   *         request.
   * @throws Exception
   *           if there are problems computing the LOCKSS identifier of the
   *           title.
   */
  private CounterReportsTitle collectRequest(String url, ArchivalUnit au,
      PublisherContacted contacted, int publisherCode,
      Map<String, Object> requestData) throws Exception {
    final String DEBUG_HEADER = "collectRequest(): ";

    // Get the title database archival unit.
    TdbAu tdbAu = TdbUtil.getTdbAu(au);

    // Get the name of the publisher.
    String publisherName = tdbAu.getPublisherName();
    log.debug2(DEBUG_HEADER + "publisherName = '" + publisherName + "'.");

    // Get the publishing platform.
    String publishingPlatform = au.getPlugin().getPublishingPlatform();
    log.debug2(DEBUG_HEADER + "publishingPlatform = '" + publishingPlatform
	+ "'.");

    // Get an indication of whether the publisher is involved in serving the
    // content.
    log.debug2("publisherCode = " + publisherCode);
    requestData
	.put(CounterReportsJournal.IS_PUBLISHER_INVOLVED_KEY,
	     contacted == PublisherContacted.TRUE
		 && (publisherCode == HttpResponse.__200_OK || publisherCode == HttpResponse.__304_Not_Modified));

    // Check whether the request is for a book.
    if (TdbUtil.isBook(tdbAu)) {
      // Yes.
      return collectBookRequest(url, au, tdbAu, publisherName,
				publishingPlatform, requestData);
      // TODO: Handle other possibilities.
    } else /* if (TdbUtil.isJournal(tdbAu)) */{
      // No: It is a journal.
      String contentType = null;
      CachedUrl cu = null;

      try {
	cu = au.makeCachedUrl(url);
	// Get the content type.
	contentType =
	    HeaderUtil.getMimeTypeFromContentType(cu.getContentType());
	log.debug2(DEBUG_HEADER + "contentType = '" + contentType + "'.");
      } finally {
	AuUtil.safeRelease(cu);
      }

      return collectJournalRequest(url, tdbAu, publisherName,
				   publishingPlatform, requestData, contentType);
    }
  }

  /**
   * Obtains the data needed to persist a book request that is the subject of a
   * report.
   * 
   * @param url
   *          A String with the URL being requested.
   * @param au
   *          An ArchivalUnit corresponding to the request.
   * @param tdbAu
   *          A TdbAu corresponding to the request.
   * @param publisherName
   *          A String with the name of the publisher.
   * @param publishingPlatform
   *          A String with the name of the publishing platform.
   * @param requestData
   *          A Map<String, Object> with properties of the request to be
   *          persisted.
   * @return a CounterReportsTitle representing the book involved in the
   *         request.
   * @throws Exception
   *           if there are problems computing the LOCKSS identifier of the
   *           book.
   */
  private CounterReportsTitle collectBookRequest(String url, ArchivalUnit au,
      TdbAu tdbAu, String publisherName, String publishingPlatform,
      Map<String, Object> requestData) throws Exception {
    final String DEBUG_HEADER = "collectBookRequest(): ";

    // The archival unit name is the title for books.
    String titleName = au.getName();
    log.debug2(DEBUG_HEADER + "titleName = '" + titleName + "'.");

    // TODO: The DOI.
    String doi = /* tdbAu.getDoi() */null;
    log.debug2(DEBUG_HEADER + "DOI = '" + doi + "'.");

    // TODO: The archival unit identifier is the proprietary identifier.
    String proprietaryId = /* tdbAu.getAuId() */null;

    // Get the title ISBN, only applicable to books.
    String isbn = tdbAu.getIsbn();
    log.debug2(DEBUG_HEADER + "isbn = '" + isbn + "'.");

    // Get the title ISSN, only applicable to books.
    String issn = tdbAu.getIssn();
    log.debug2(DEBUG_HEADER + "issn = '" + issn + "'.");

    OpenUrlInfo oui = our.resolveFromUrl(url);
    log.debug2(DEBUG_HEADER + "oui.resolvedTo = '" + oui.getResolvedTo() + "'.");

    // Record whether this is a request for a section of a book.
    requestData.put(CounterReportsBook.IS_SECTION_KEY,
		    Boolean.valueOf(ResolvedTo.CHAPTER == oui.getResolvedTo()));

    return new CounterReportsBook(titleName, publisherName, publishingPlatform,
				  doi, proprietaryId, isbn, issn);
  }

  /**
   * Obtains the data needed to persist a journal request that is the subject of
   * a report.
   * 
   * @param url
   *          A String with the URL being requested.
   * @param tdbAu
   *          A TdbAu corresponding to the request.
   * @param publisherName
   *          A String with the name of the publisher.
   * @param publishingPlatform
   *          A String with the name of the publishing platform.
   * @param requestData
   *          A Map<String, Object> with properties of the request to be
   *          persisted.
   * @param contentType
   *          A String with the request content type.
   * @return a CounterReportsTitle representing the journal involved in the
   *         request.
   * @throws Exception
   *           if there are problems computing the LOCKSS identifier of the
   *           journal.
   */
  private CounterReportsTitle collectJournalRequest(String url, TdbAu tdbAu,
      String publisherName, String publishingPlatform,
      Map<String, Object> requestData, String contentType) throws Exception {
    final String DEBUG_HEADER = "collectJournalRequest(): ";

    // Get the journal title.
    String titleName = tdbAu.getJournalTitle();
    log.debug2(DEBUG_HEADER + "titleName = '" + titleName + "'.");

    // TODO: The DOI.
    String doi = /* tdbAu.getJournalDoi() */null;
    log.debug2(DEBUG_HEADER + "DOI = '" + doi + "'.");

    // TODO: The journal identifier is the proprietary identifier.
    String proprietaryId = /* tdbAu.getJournalId() */null;

    // Get the title print ISSN, only applicable to articles.
    String printIssn = tdbAu.getPrintIssn();
    log.debug2(DEBUG_HEADER + "printIssn = '" + printIssn + "'.");

    // Get the title online ISSN, only applicable to articles.
    String onlineIssn = tdbAu.getEissn();
    log.debug2(DEBUG_HEADER + "onlineIssn = '" + onlineIssn + "'.");

    // Get the year of publication of the content.
    requestData
	.put(CounterReportsJournal.PUBLICATION_YEAR_KEY, tdbAu.getYear());

    // Get an indication of whether the content is served in HTML format.
    requestData.put(CounterReportsJournal.IS_HTML_KEY, Boolean
	.valueOf(contentType.equals(Constants.MIME_TYPE_HTML)));

    // Get an indication of whether the content is served in PDF format.
    requestData.put(CounterReportsJournal.IS_PDF_KEY, Boolean
	.valueOf(contentType.equals(Constants.MIME_TYPE_PDF)));

    return new CounterReportsJournal(titleName, publisherName,
				     publishingPlatform, doi, proprietaryId,
				     printIssn, onlineIssn);
  }
}
