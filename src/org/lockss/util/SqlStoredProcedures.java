/*
 * $Id: SqlStoredProcedures.java,v 1.3 2011-11-19 00:37:11 mellen22 Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.util;

import java.sql.Date;
import java.text.SimpleDateFormat;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Tdb;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbPublisher;
import org.lockss.config.TdbTitle;
import org.lockss.daemon.TitleConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginManager;

/**
 * This utility class contains static methods that enable SQL stored
 * procedures to access LOCKSS functionality.
 *  
 * @author pgust mellen
 *
 */
public class SqlStoredProcedures {
  static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

  /**
   * Constructor prevents creating instances.
   */
  private SqlStoredProcedures() {
    
  }
  
  /**
   * Return the title from the title title database that corresponds
   * to the URL of an article in that title.
   * 
   * @param articleUrl the URL of the article
   * @return the title for the given URL and null otherwise
   */
  static TdbAu getTdbAuFromArticleUrl(String articleUrl) {
    if (articleUrl == null) {
      throw new IllegalArgumentException("null articleUrl");
    }
    
    // get lockss daemon
    LockssDaemon daemon = LockssDaemon.getLockssDaemon();
    if (daemon == null) {
      throw new IllegalStateException("no LOCKSS daemon");
    }
    
    // get the CachedUrl from the article URL
    PluginManager pluginManager = daemon.getPluginManager();
    CachedUrl cu = pluginManager.findCachedUrl(articleUrl);
    if (cu == null) {
      return null;
    }
    
    // get the AU from the CachedUrl
    ArchivalUnit au = cu.getArchivalUnit();
    
    // return the TdbAu from the AU
    TitleConfig tc = au.getTitleConfig();
    if (tc == null) {
      return null;
    }
    return tc.getTdbAu();
  }

  /**
   * Return the title from the title database that corresponds
   * to the URL of an article in that title.
   * 
   * @param articleUrl the URL of the article
   * @return the title for the given URL
   */
  static public String getTitleFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      throw new IllegalArgumentException(
          "No title for articleUrl " + articleUrl);
    }

    // get the title from the TdbAu
    String title = tdbAu.getJournalTitle();
    
    // return the title
    return title;
  }

  /**
   * Return the publisher from the title database that corresponds
   * to the URL of an article in that publisher.
   * 
   * @param articleUrl the URL of the article
   * @return the publisher for the given URL
   */
  static public String getPublisherFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      throw new IllegalArgumentException(
          "No publisher for articleUrl " + articleUrl);
    }
  
    // get the publisher from the TdbAu
    String publisher = tdbAu.getTdbPublisher().getName();
    
    // return the publisher
    return publisher;
  }
  
  /**
   * Return the ingest date from the title database that corresponds
   * to the URL of an article in that publisher.
   * 
   * @param articleUrl the URL of the article
   * @return the ingest date for the given URL
   */
  static public String getDateOfIngestFromArticleUrl(String articleUrl) {
    // get lockss daemon
    LockssDaemon daemon = LockssDaemon.getLockssDaemon();
    if (daemon == null) {
      throw new IllegalStateException("no LOCKSS daemon");
    }
    
    // get the CachedUrl from the article URL
    PluginManager pluginManager = daemon.getPluginManager();
    CachedUrl cu = pluginManager.findCachedUrl(articleUrl);
    if (cu == null) {
      return null;
    }
    
    // get the ingest date from the CachedUrl
    String ingestDate = cu.getProperties().getProperty(CachedUrl.PROPERTY_LAST_MODIFIED);
    if (ingestDate == null) {
      return null;
    }
    
    // get formatted date and return null if there is an exception
    try {
      long date = Long.parseLong(ingestDate);
      ingestDate = formatter.format(new Date(date));
      return ingestDate;
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  /**
   * Return the title from the title database that corresponds
   * to the ISSN of the journal.
   * 
   * @param journalISSN the ISSN of the journal
   * @return the title for the given ISSN
   */
  static public String getTitleFromISSN(String journalISSN) {
    if (journalISSN == null) {
      throw new IllegalArgumentException("null journalISSN");
    }

    // get the tdb
    Tdb tdb = ConfigManager.getCurrentConfig().getTdb(); 
    if (tdb == null) {
      throw new IllegalStateException("No tdb.");
    }
  
    // get the title from the ISSN
    TdbTitle title = tdb.getTdbTitleByIssn(journalISSN);
  
    // return the title
    return title == null ? null : title.getName();
  }

  /**
   * Return the publisher from the publisher database that corresponds
   * to the ISSN of the journal.
   * 
   * @param journalISSN the ISSN of the journal
   * @return the publisher for the given ISSN
   */
  static public String getPublisherFromISSN(String journalISSN) {
    if (journalISSN == null) {
      throw new IllegalArgumentException("null journalISSN");
    }

    Tdb tdb = ConfigManager.getCurrentConfig().getTdb(); 
    if (tdb == null) {
      throw new IllegalStateException("No tdb.");
    }
  
    // get the publisher from the ISSN
    TdbPublisher publisher = tdb.getTdbTitleByIssn(journalISSN).getTdbPublisher();
  
    // return the publisher
    return publisher == null ? null : publisher.getName();
  }

}
