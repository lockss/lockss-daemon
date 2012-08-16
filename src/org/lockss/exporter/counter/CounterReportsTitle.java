/*
 * $Id: CounterReportsTitle.java,v 1.1 2012-08-16 22:19:14 fergaloy-sf Exp $
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
 * Representation of a title used in COUNTER reports.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 * 
 */
package org.lockss.exporter.counter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public interface CounterReportsTitle {

  /**
   * Provides the DOI of the title.
   * 
   * @return a String with the DOI of the title.
   */
  String getDoi();

  /**
   * Provides the ISBN of the title if it is a book.
   * 
   * @return a String with the ISBN of the title.
   */
  String getIsbn();

  /**
   * Provides the ISSN of the title if it is a book.
   * 
   * @return a String with the ISSN of the title.
   */
  String getIssn();

  /**
   * Provides the LOCKSS identifier of the title.
   * 
   * @return a long with the LOCKSS identifier of the title.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  long getLockssId() throws SQLException;

  /**
   * Provides the name of the title.
   * 
   * @return a String with the name of the title.
   */
  String getName();

  /**
   * Provides the online ISSN of the title if it is a journal.
   * 
   * @return a String with the online ISSN of the title.
   */
  String getOnlineIssn();

  /**
   * Provides the print ISSN of the title if it is a journal.
   * 
   * @return a String with the print ISSN of the title.
   */
  String getPrintIssn();

  /**
   * Provides the proprietary identifier of the title.
   * 
   * @return a String with the proprietary identifier of the title.
   */
  String getProprietaryId();

  /**
   * Provides the publisher name of the title.
   * 
   * @return a String with the publisher name of the title.
   */
  String getPublisherName();

  /**
   * Provides the publishing platform name of the title.
   * 
   * @return a String with the publishing platform name of the title.
   */
  String getPublishingPlatform();

  /**
   * Computes the LOCKSS identifier of the title and persists the title if
   * necessary.
   * 
   * @throws SQLException
   */
  void identify() throws SQLException;

  /**
   * Provides an indication of whether the title is a book.
   * 
   * @return a boolean with the indication.
   */
  boolean isBook();

  /**
   * Persists a title request.
   * 
   * @param requestData
   *          A Map<String, Object> with properties of the request to be
   *          persisted.
   * @param conn
   *          A Connection representing the database connection.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  void persistRequest(Map<String, Object> requestData, Connection conn)
      throws SQLException;
}
