/*
 * $Id: SqlStoredProcedures.java,v 1.6 2012-03-03 23:09:56 pgust Exp $
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

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Tdb;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbPublisher;
import org.lockss.config.TdbTitle;
import org.lockss.daemon.TitleConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginManager;
import org.lockss.state.AuState;
import org.lockss.util.Logger;

/**
 * This utility class contains static methods that enable SQL stored
 * procedures to access LOCKSS functionality.
 * 
 * @author pgust, mellen
 *
 */
public class SqlStoredProcedures {
  /** logger to report issues */
  static Logger log = Logger.getLogger("SqlStoredProcedures");
  
  /** logger to report query issues */
  static Logger queryLog = Logger.getLogger("SqlStoredProcedureQueryLog");

  /** Formatter for ISO formatted date */
  static SimpleDateFormat isoDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
  
  /** The plugin manager */
  static PluginManager pluginManager = null;

  /**
   * Constructor prevents creating instances.
   */
  private SqlStoredProcedures() {
  }
  
  /**
   * Set the cached plugin manager for this class. Primarily used 
   * in testing
   * 
   * @param pluginManager the plugin manager
   */
  static void setPluginManager(PluginManager pluginManager) {
    SqlStoredProcedures.pluginManager = pluginManager;
  }
  
  /**
   * Returns the plugin manager for the current LOCKSS daemon. Does
   * lazy initialization on first reference.
   * 
   * @return the plugin manager
   */
  static private PluginManager getPluginManager() {
    // get lockss daemon plugin manager
    if (pluginManager == null) {
      setPluginManager(LockssDaemon.getLockssDaemon().getPluginManager());
    }
    return pluginManager;
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
    
    // get the CachedUrl from the article URL
    CachedUrl cu = getPluginManager().findCachedUrl(articleUrl);
    if (cu == null) {
      queryLog.debug2("No CachedUrl for articleUrl " + articleUrl);
      return null;
    }
    
    // get the AU from the CachedUrl
    ArchivalUnit au = cu.getArchivalUnit();
    
    // return the TdbAu from the AU
    TitleConfig tc = au.getTitleConfig();
    if (tc == null) {
      log.debug2("no titleconfig for au " + au.toString());
      return null;
    }

    return tc.getTdbAu();
  }

  /**
   * Return the title from the title title database that corresponds
   * to an auid for an AU in that title.
   * 
   * @param pluginId the pluginId
   * @param auKey the AU key
   * @return the title for the given URL and null otherwise
   */
  static TdbAu getTdbAuFromAuId(String pluginId, String auKey) {
    if (StringUtil.isNullString(pluginId) || StringUtil.isNullString(auKey)) {
      return null;
    }
    String auId = PluginManager.generateAuId(pluginId, auKey);
    
    // get the AU from the Auid
    ArchivalUnit au = getPluginManager().getAuFromId(auId);
    if (au == null) {
      queryLog.debug2(  "No ArchivalUnit for pluginId: " + pluginId 
                      + " auKey: " + auKey);
      return null;
    }
    
    // return the TdbAu from the AU
    TitleConfig tc = au.getTitleConfig();
    if (tc == null) {
      log.debug2("no titleconfig for au " + au.toString());
      return null;
    }

    return tc.getTdbAu();
  }

  /**
   * Return the volume title that corresponds to the auid 
   * of a journal or series AU.
   * 
   * @param pluginId the pluginId part of the auid
   * @param auKey the auKey part of the auid
   * @return the publisher for the given auid
   */
  static public String getVolumeTitleFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    return tdbAu == null ? null : tdbAu.getName();
  }
  
  /**
   * Return the journal or series title from that corresponds
   * to the URL of an article in that journal.
   * 
   * @param articleUrl the URL of the article
   * @return the title for the given URL or null if not available
   */
  static public String getVolumeTitleFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    return (tdbAu == null) ? null : tdbAu.getName();
  }

  /**
   * Return the journal or series title that corresponds to the auid 
   * of a journal or series AU.
   * 
   * @param pluginId the pluginId part of the auid
   * @param auKey the auKey part of the auid
   * @return the publisher for the given auid
   */
  static public String getTitleFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    return tdbAu == null ? null : tdbAu.getJournalTitle();
  }
  
  /**
   * Return the journal or series title from that corresponds
   * to the URL of an article in that journal.
   * 
   * @param articleUrl the URL of the article
   * @return the title for the given URL or null if not available
   */
  static public String getTitleFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    return (tdbAu == null) ? null : tdbAu.getJournalTitle();
  }

  /**
   * Return the publisher from the title database that corresponds
   * to the URL of an article by that publisher.
   * 
   * @param articleUrl the URL of the article
   * @return the publisher for the given URL or null if not available
   */
  static public String getPublisherFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    return (tdbAu == null) ? null : tdbAu.getTdbPublisher().getName();
  }

  /**
   * Remove punctuation from a string.
   * 
   * @param s the string
   * @return an unpunctated version of the string
   */
  static private String unpunctuate(String s) {
    return (s == null) ? null : s.replaceAll("-", "");
  }
  
  /**
   * Return the eISBN from that corresponds
   * to the URL of an article.
   * The value returned is without punctuation.
   * 
   * @param articleUrl the URL of the article
   * @return the eISBN for the given URL or null if not available
   */
  static public String getEisbnFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      queryLog.debug2("No TdbAu for article url: " + articleUrl);
      return null;
    }
    String eisbn = tdbAu.getEisbn();
    return unpunctuate(eisbn);
  }

  /**
   * Return the eISBN from that corresponds
   * to the auId of an AU.
   * The value returned is without punctuation.
   * 
   * @param pluginId the pluginID of an article AU
   * @param auKey the auKey of an article AU
   * @return the eISBN for the given auId or null if not available
   */
  static public String getEisbnFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    if (tdbAu == null) {
      queryLog.debug2(  "No TdbAu for pluginId: " + pluginId 
                      + " auKey: " + auKey);
      return null;
    }
    String eisbn = tdbAu.getEisbn();
    return unpunctuate(eisbn);
  }

  /**
   * Return the print ISBN from that corresponds
   * to the URL of an article.
   * The value returned is without punctuation.
   * 
   * @param articleUrl the URL of the article
   * @return the print ISBN for the given URL or null if not available
   */
  static public String getPrintIsbnFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      queryLog.debug2("No TdbAu for article url: " + articleUrl);
      return null;
    }
    String printIsbn = tdbAu.getPrintIsbn();
    return unpunctuate(printIsbn);
  }

  /**
   * Return the print ISBN from that corresponds
   * to the auId of an AU.
   * The value returned is without punctuation.
   * 
   * @param pluginId the pluginID of an article AU
   * @param auKey the auKey of an article AU
   * @return the print ISBN for the given auId or null if not available
   */
  static public String getPrintIsbnFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    if (tdbAu == null) {
      queryLog.debug2(  "No TdbAu for pluginId: " + pluginId 
                      + " auKey: " + auKey);
      return null;
    }
    String printIsbn = tdbAu.getPrintIsbn();
    return unpunctuate(printIsbn);
  }

  /**
   * Return an ISBN from that corresponds
   * to the URL of an article.
   * The value returned is without punctuation.
   * 
   * @param articleUrl the URL of the article
   * @return the eISBN for the given URL or null if not available
   */
  static public String getIsbnFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      queryLog.debug2("No TdbAu for article url: " + articleUrl);
      return null;
    }
    String isbn = tdbAu.getIsbn();
    return unpunctuate(isbn);
  }

  /**
   * Return an ISBN from that corresponds
   * to the auId of an AU.
   * The value returned is without punctuation.
   * 
   * @param pluginId the pluginID of an article AU
   * @param auKey the auKey of an article AU
   * @return an ISBN for the given auId or null if not available
   */
  static public String getIsbnFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    if (tdbAu == null) {
      queryLog.debug2(  "No TdbAu for pluginId: " + pluginId 
                      + " auKey: " + auKey);
      return null;
    }
    String isbn = tdbAu.getIsbn();
    return unpunctuate(isbn);
  }

  /**
   * Return the eISSN from that corresponds to the URL 
   * of an article in that journal or series.
   * The value returned is without punctuation.
   * 
   * @param articleUrl the URL of the article
   * @return the eISSN for the given URL or null if not available
   */
  static public String getEissnFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      queryLog.debug2("No TdbAu for article url: " + articleUrl);
      return null;
    }
    String eissn = tdbAu.getEissn();
    return unpunctuate(eissn);
  }

  /**
   * Return the eISSN from that corresponds to the auId 
   * of an AU in that journal or series
   * The value returned is without punctuation.
   * 
   * @param pluginId the pluginID of an article AU
   * @param auKey the auKey of an article AU
   * @return the eISSN for the given auId or null if not available
   */
  static public String getEissnFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    if (tdbAu == null) {
      queryLog.debug2(  "No TdbAu for pluginId: " + pluginId 
                      + " auKey: " + auKey);
      return null;
    }
    String eissn = tdbAu.getEissn();
    return unpunctuate(eissn);
  }

  /**
   * Return the ISSN-L from that corresponds to the URL 
   * of an article in that journal or series.
   * The value returned is without punctuation.
   * 
   * @param articleUrl the URL of the article
   * @return the ISSN-L for the given URL or null if not available
   */
  static public String getIssnLFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      queryLog.debug2("No TdbAu for article url: " + articleUrl);
      return null;
    }
    String issnl = tdbAu.getIssnL();
    return unpunctuate(issnl);
  }

  /**
   * Return the ISSN-L from that corresponds to the auId 
   * of an AU in that journal or series.
   * The value returned is without punctuation.
   * 
   * @param pluginId the pluginID of an article AU
   * @param auKey the auKey of an article AU
   * @return the ISSN-L for the given auId or null if not available
   */
  static public String getIssnLFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    if (tdbAu == null) {
      return null;
    }
    String issnl = tdbAu.getEissn();
    return unpunctuate(issnl);
  }

  /**
   * Return a ISSN that corresponds to the URL 
   * of an article in that journal or series.
   * The value returned is without punctuation.
   * 
   * @param articleUrl the URL of the article
   * @return an ISSN for the given URL or null if not available
   */
  static public String getIssnFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      queryLog.debug2("No TdbAu for article url: " + articleUrl);
      return null;
    }
    String issn = tdbAu.getIssn();
    return unpunctuate(issn);
  }

  /**
   * Return a ISSN that corresponds to the auId 
   * of an AU in that journal or series.
   * The value returned is without punctuation.
   * 
   * @param pluginId the pluginID of an article AU
   * @param auKey the auKey of an article AU
   * @return an ISSN for the given auId or null if not available
   */
  static public String getIssnFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    if (tdbAu == null) {
      queryLog.debug2(  "No TdbAu for pluginId: " + pluginId 
                      + " auKey: " + auKey);
      return null;
    }
    String issn = tdbAu.getIssn();
    return unpunctuate(issn);
  }

  /**
   * Return the print ISSN from that corresponds to the URL 
   * of an article in that journal or series.
   * The value returned is without punctuation.
   * 
   * @param articleUrl the URL of the article
   * @return the print ISSN for the given URL or null if not available
   */
  static public String getPrintIssnFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      queryLog.debug2("No tdbAu for articleUrl: " + articleUrl);
      return null;
    }
    String printIssn = tdbAu.getPrintIssn();
    return unpunctuate(printIssn);
  }

  /**
   * Return the print ISSN from that corresponds to the auId 
   * of an AU in that journal or series
   * The value returned is without punctuation.
   * 
   * @param pluginId the pluginID of an article AU
   * @param auKey the auKey of an article AU
   * @return the print ISSN for the given auId or null if not available
   */
  static public String getPrintIssnFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    if (tdbAu == null) {
      queryLog.debug2(  "No tdbAu for pluginId: " + pluginId 
                      + " auKey: " + auKey);
      return null;
    }
    String printIssn = tdbAu.getPrintIssn();
    return unpunctuate(printIssn);
  }

  /**
   * Return the year from an ISO formatted date string of the form
   * yyyy or yyyy-mm or yyyy-mm-dd
   * @param dateStr the date string
   * @return the year
   */
  static public String getYearFromDate(String dateStr) {
    if (dateStr != null) {
      int i = dateStr.indexOf('-');
      String year = (i > 0) ? dateStr.substring(0,i) : dateStr;
      try {
        if (Integer.parseInt(year) > 0) {
          return year;
        }
      } catch (NumberFormatException ex) {
        queryLog.debug2("Year field of date is not a number: " + dateStr);
      }
    }
    return null;
  }
  
  /**
   * Return the ingest year from the daemon that corresponds
   * to the URL of an article in that publisher.
   * 
   * @param articleUrl the URL of the article
   * @return the ingest date for the given URL or null if not available
   */
  static public String getIngestYearFromArticleUrl(String articleUrl) {
    String ingestDate = getIngestDateFromArticleUrl(articleUrl);
    String ingestYear = getYearFromDate(ingestDate);
    return ingestYear;
  }
  
  /**
   * Return the article ingest date from the daemon that corresponds
   * to the URL of an article in that publisher.
   * 
   * @param articleUrl the URL of the article
   * @return the ingest date for the given URL or null if not available
   */
  static public String getIngestDateFromArticleUrl(String articleUrl) {
    // get the CachedUrl from the article URL
    CachedUrl cu = getPluginManager().findCachedUrl(articleUrl);
    if (cu == null) {
      queryLog.debug2("No CachedUrl for articleUrl: " + articleUrl);
      return null;
    }
    
    // get the ingest date from the CachedUrl
    CIProperties ciProps = cu.getProperties();
    String fetchTime = ciProps.getProperty(CachedUrl.PROPERTY_FETCH_TIME);
    if (fetchTime == null) {
      log.warning("No fetch time for articleUrl: " + articleUrl);
      return null;
    }
    
    try {
      Date date = new Date(Long.parseLong(fetchTime));
      String ingestDate = isoDateFormatter.format(date);
      return ingestDate;
    } catch (NumberFormatException ex) {
      log.warning(  "error parsing fetchtime: " + fetchTime
                 + " for article url: " + articleUrl);
      return null;
    }
  }
  
  /**
   * Return the title from the title database that corresponds
   * to the ISBN of a book volume.
   * 
   * @param isbn the ISBN of the book
   * @return the book title for the given ISBN
   */
  static public String getVolumeTitleFromIsbn(String isbn) {
    if (isbn == null) {
      return null;
    }

    // get the tdb
    Tdb tdb = ConfigManager.getCurrentConfig().getTdb(); 
    if (tdb == null) {
      log.debug2("No Tdb in configuration");
      return null;
    }
  
    // get the tdbAus for this isbn
    Collection<TdbAu> tdbAus = tdb.getTdbAusByIsbn(isbn);
  
    // return the title
    return tdbAus.isEmpty() ? null : tdbAus.iterator().next().getName();
  }

  /**
   * Return the series title from the title database that corresponds
   * to the ISSB of the volume.
   * 
   * @param isbn the ISBN of the series
   * @return the series title for the given ISBN
   */
  static public String getTitleFromIsbn(String isbn) {
    if (isbn == null) {
      return null;
    }

    // get the tdb
    Tdb tdb = ConfigManager.getCurrentConfig().getTdb(); 
    if (tdb == null) {
      log.debug2("No Tdb in configuration");
      return null;
    }
  
    // get the tdbAus from the ISBN
    Collection<TdbAu> tdbAus = tdb.getTdbAusByIsbn(isbn);
  
    // return the title
    return tdbAus.isEmpty() ? null : tdbAus.iterator().next().getJournalTitle();
  }
  
  /**
   * Return the title from the title database that corresponds
   * to the ISSN of the journal or series.
   * 
   * @param issn the ISSN of the journal or series
   * @return the title for the given ISSN
   */
  static public String getTitleFromIssn(String issn) {
    if (issn == null) {
      return null;
    }

    // get the tdb
    Tdb tdb = ConfigManager.getCurrentConfig().getTdb(); 
    if (tdb == null) {
      log.debug2("No Tdb in configuration");
      return null;
    }
  
    // get the title from the ISSN
    TdbTitle tdbTitle = tdb.getTdbTitleByIssn(issn);
  
    // return the title
    return tdbTitle == null ? null : tdbTitle.getName();
  }

  /**
   * Return the publisher from the title database that corresponds
   * to the ISBN of a book volume. If the volume has multiple publishers,
   * the name of the first one is returned.
   * 
   * @param journalIssn the ISSN of the journal
   * @return the publisher for the given ISSN
   */
  static public String getPublisherFromIsbn(String isbn) {
    if (isbn == null) {
      return null;
    }

    Tdb tdb = ConfigManager.getCurrentConfig().getTdb(); 
    if (tdb == null) {
      log.debug2("No Tdb in configuration");
      return null;
    }
  
    // get the publisher from the ISSN
    Collection<TdbAu> tdbAus = tdb.getTdbAusByIsbn(isbn);
    if (tdbAus.isEmpty()) {
      queryLog.debug2("No TdbAus for isbn: " + isbn);
      return null;
    }
    
    // return the publisher
    return tdbAus.iterator().next().getPublisherName();
  }
  
  /**
   * Return the publisher from the title database that corresponds
   * to the ISSN of the journal.
   * 
   * @param journalIssn the ISSN of the journal
   * @return the publisher for the given ISSN
   */
  static public String getPublisherFromIssn(String journalIssn) {
    if (journalIssn == null) {
      return null;
    }

    Tdb tdb = ConfigManager.getCurrentConfig().getTdb(); 
    if (tdb == null) {
      log.debug2("No Tdb in configuration");
      return null;
    }
  
    // get the publisher from the ISSN
    TdbTitle tdbTitle = tdb.getTdbTitleByIssn(journalIssn);
    if (tdbTitle == null) {
      queryLog.debug2("No TdbTitle for journal issn: " + journalIssn);
      return null;
    }
    
    // return the publisher
    TdbPublisher tdbPublisher = tdbTitle.getTdbPublisher();
    return tdbPublisher == null ? null : tdbPublisher.getName();
  }
  
  /**
   * Return the journal publisher that corresponds to the auid of a journal AU.
   * 
   * @param pluginId the pluginId part of the auid
   * @param auKey the auKey part of the auid
   * @return the publisher for the given auid
   */
  static public String getPublisherFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    return tdbAu == null ? null : tdbAu.getPublisherName();
  }
  
  /**
   * Return the creation date of an Au of the form YYYY-MM-DD.
   * @param au the Archival Unit
   * @return the date
   */
  static String getAuDateFromAu(ArchivalUnit au) {
    if (au == null) {
      return null;
    }

    AuState auState = AuUtil.getAuState(au);
    if (auState == null) {
      return null;
    }
    long creationTime = AuUtil.getAuState(au).getAuCreationTime();
    if (creationTime < 0) {
      return null;
    }
    
    // AU date is of the form: Wed, 02 Nov 2011 06:11:51 GMT 
    String ingestDate = isoDateFormatter.format(creationTime);
    return ingestDate;
  }
  
  /**
   * Return the au creation year from the daemon that corresponds
   * to an AU. This method is faster than
   * getIngestDateFromArticleUrl(String articleUrl) because it does
   * not require a disk access per article. Au creation date is a
   * close approximation to article ingest date because articles for an
   * AU are generally ingested close to the time when the AU is created. 
   * 
   * @param pluginId the pluginId
   * @param au_key the au key
   * @return the AU creation date for the given URL or null if not available
   */
  static public String getIngestYearFromAuId(String pluginId, String auKey) {
    if (StringUtil.isNullString(pluginId) || StringUtil.isNullString(auKey)) {
      return null;
    }
    String ingestDate = getIngestDateFromAuId(pluginId, auKey);
    String ingestYear = getYearFromDate(ingestDate);
    return ingestYear;
  }
  
  /**
   * Return the au creation date from the daemon that corresponds
   * to the AU. This method is faster than
   * getIngestDateFromArticleUrl(String articleUrl) because it does
   * not require a disk access per article. Au creation date is a
   * close approximation to article ingest date because articles for an
   * AU are generally ingested close to the time when the AU is created. 
   * 
   * @param pluginId the pluginId
   * @param au_key the au key
   * @return the AU creation date for the given AU or null if not available
   */
  static public String getIngestDateFromAuId(String pluginId, String auKey) {
    if (StringUtil.isNullString(pluginId) || StringUtil.isNullString(auKey)) {
      return null;
    }

    String auId = PluginManager.generateAuId(pluginId, auKey);
    ArchivalUnit au = getPluginManager().getAuFromId(auId);
    return getAuDateFromAu(au);
  }
  
  /**
   * Return the starting volume for the specified AU.
   * 
   * @param pluginId the pluginId
   * @param au_key the au key
   * @return the starting volume for the given AU or null if not available
   */
  static public String getStartVolumeFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    return (tdbAu == null) ? null : tdbAu.getStartVolume();
  }
  
  /**
   * Return the starting volume for the specified article URL
   * 
   * @param articleUrl
   * @return the starting volume for the given url or null if not available
   */
  static public String getStartVolumeFromArticleUrl(String articleUrl) {
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    return (tdbAu == null) ? null : tdbAu.getStartVolume();
  }
  
  /**
   * Return the ending volume for the specified AU.
   * 
   * @param pluginId the pluginId
   * @param au_key the au key
   * @return the ending volume for the given AU or null if not available
   */
  static public String getEndVolumeFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    return (tdbAu == null) ? null : tdbAu.getEndVolume();
  }
  
  /**
   * Return the ending volume for the specified article URL
   * 
   * @param articleUrl
   * @return the ending volume for the given url or null if not available
   */
  static public String getEndVolumeFromArticleUrl(String articleUrl) {
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    return (tdbAu == null) ? null : tdbAu.getEndVolume();
  }
  
  /**
   * Return the starting year for the specified AU.
   * 
   * @param pluginId the pluginId
   * @param au_key the au key
   * @return the starting year for the given AU or null if not available
   */
  static public String getStartYearFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    return (tdbAu == null) ? null : tdbAu.getStartYear();
  }
  
  /**
   * Return the starting year for the specified article URL.
   * 
   * @param articleUrl the article URL
   * @return the starting year for the given url or null if not available
   */
  static public String getStartYearFromArticleUrl(String articleUrl) {
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    return (tdbAu == null) ? null : tdbAu.getStartYear();
  }
  
  /**
   * Return the ending year for the specified AU.
   * 
   * @param pluginId the pluginId
   * @param au_key the au key
   * @return the ending year for the given AU or null if not available
   */
  static public String getEndYearFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    return (tdbAu == null) ? null : tdbAu.getEndYear();
  }
  
  /**
   * Return the ending year for the specified article URL.
   * 
   * @param articleUrl the article URL
   * @return the ending year for the given url or null if not available
   */
  static public String getEndYearFromArticleUrl(String articleUrl) {
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    return (tdbAu == null) ? null : tdbAu.getEndYear();
  }
  
  static public void main(String[] args) {
  }
}
