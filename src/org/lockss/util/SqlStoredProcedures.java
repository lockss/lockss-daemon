/*
 * $Id: SqlStoredProcedures.java,v 1.4 2012-01-04 23:35:38 pgust Exp $
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
 * The following SQL stored procedure definitions can be used in conjunction
 * with these functions:
 * 
 * create function publisherFromUrl(url varchar(4096)) returns varchar(256) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getPublisherFromArticleUrl' 
 * parameter style java no sql;
 * 
 * create function titleFromUrl(url varchar(4096)) returns varchar(512) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getJournalTitleFromArticleUrl' 
 * parameter style java no sql;
 * 
 * create function issnFromUrl(url varchar(4096)) returns varchar(8) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getJournalIssnFromArticleUrl' 
 * parameter style java no sql;
 * 
 * create function printIssnFromUrl(url varchar(4096)) returns varchar(8) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getJournalPrintIssnFromArticleUrl' 
 * parameter style java no sql;
 * 
 * create function eissnFromUrl(url varchar(4096)) returns varchar(8) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getJournalEissnFromArticleUrl' 
 * parameter style java no sql;
 * 
 * create function issnlFromUrl(url varchar(4096)) returns varchar(8) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getJournalIssnLFromArticleUrl' 
 * parameter style java no sql;
 * 
 * create function ingestDateFromUrl(url varchar(4096)) returns varchar(16) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getIngestDateFromArticleUrl' 
 * parameter style java no sql;
 * 
 * create function ingestYearFromUrl(url varchar(4096)) returns varchar(4) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getIngestYearFromArticleUrl' 
 * parameter style java no sql;
 * 
 * create function publisherFromAuId(pluginId varchar(128), auId varchar(512)) 
 * returns varchar(256) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getPublisherFromAuId' 
 * parameter style java no sql;
 * 
 * create function titleFromAuId(pluginId varchar(128), auId varchar(512)) 
 * returns varchar(256) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getJournalTitleFromAuId' 
 * parameter style java no sql;
 * 
 * create function issnFromAuId(pluginId varchar(128), auId varchar(512)) returns varchar(8) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getPrintIssnFromAuId' 
 * parameter style java no sql;
 * 
 * create function printIssnFromAuId(pluginId varchar(128), auId varchar(512)) returns varchar(8) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getJournalPrintIssnFromAuId' 
 * parameter style java no sql;
 * 
 * create function eissnFromAuId(pluginId varchar(128), auId varchar(512)) returns varchar(8) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getEissnFromAuId' 
 * parameter style java no sql;
 * 
 * create function issnlFromAuId(pluginId varchar(128), auId varchar(512)) returns varchar(8) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getIssnLFromAuId' 
 * parameter style java no sql;
 * 
 * create function startVolumeFromAuId(pluginId varchar(128), auId varchar(512)) returns varchar(16) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getStartVolumeFromAuId' 
 * parameter style java no sql;
 * 
 * create function endVolumeFromAuId(pluginId varchar(128), auId varchar(512)) returns varchar(16) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getEndVolumeFromAuId' 
 * parameter style java no sql;
 * 
 * create function startYearFromAuId(pluginId varchar(128), auId varchar(512)) returns varchar(16) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getStartYearFromAuId' 
 * parameter style java no sql;
 * 
 * create function endYearFromAuId(pluginId varchar(128), auId varchar(512)) returns varchar(16) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getEndYearFromAuId' 
 * parameter style java no sql;
 * 
 * create function ingestDateFromAuId(pluginId varchar(128), auId varchar(512)) 
 * returns varchar(16) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getingestDateFromAuId' 
 * parameter style java no sql;
 * 
 * create function ingestYearFromAuId(pluginId varchar(128), auId varchar(512)) 
 * returns varchar(4) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getIngestYearFromAuId' 
 * parameter style java no sql;
 * 
 * create function generateAuId(pluginId varchar(128), auId varchar(512)) 
 * returns varchar(640) 
 * language java external name 'org.lockss.plugin.PluginManager.generateAuId' 
 * parameter style java no sql;
 * 
 * create function yearFromDate(date varchar(16)) returns varchar(4) 
 * language java external name 'org.lockss.util.SqlStoredProcedures.getYearFromDate' 
 * parameter style java no sql;
 *  
 * @author pgust, mellen
 *
 */
public class SqlStoredProcedures {
  static Logger log = Logger.getLogger("SqlStoredProcedures");

  static SimpleDateFormat isoDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
  static PluginManager pluginManager = null;

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
    
    // get lockss daemon plugin manager
    if (pluginManager == null) {
      LockssDaemon daemon = LockssDaemon.getLockssDaemon();
      if (daemon == null) {
        log.info("no lockss daemon");
        return null;
      }
      pluginManager = daemon.getPluginManager();
    }

    // get the CachedUrl from the article URL
    CachedUrl cu = pluginManager.findCachedUrl(articleUrl);
    if (cu == null) {
      log.info("no cu for articleUrl " + articleUrl);
      return null;
    }
    
    // get the AU from the CachedUrl
    ArchivalUnit au = cu.getArchivalUnit();
    
    // return the TdbAu from the AU
    TitleConfig tc = au.getTitleConfig();
    if (tc == null) {
      log.info("no titleconfig for au " + au.toString());
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
    
    // get lockss daemon plugin manager
    if (pluginManager == null) {
      LockssDaemon daemon = LockssDaemon.getLockssDaemon();
      if (daemon == null) {
        log.info("no lockss daemon");
        return null;
      }
      pluginManager = daemon.getPluginManager();
    }

    // get the AU from the Auid
    ArchivalUnit au = pluginManager.getAuFromId(auId);
    if (au == null) {
      log.info("no au for auid " + auId);
      return null;
    }
    
    // return the TdbAu from the AU
    TitleConfig tc = au.getTitleConfig();
    if (tc == null) {
      log.info("no titleconfig for au " + au.toString());
      return null;
    }

    return tc.getTdbAu();
  }

  /**
   * Return the Journal title from that corresponds
   * to the URL of an article in that journal.
   * 
   * @param articleUrl the URL of the article
   * @return the title for the given URL or null if not available
   */
  static public String getJournalTitleFromArticleUrl(String articleUrl) {
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
   * Return the journal eISSN from that corresponds
   * to the URL of an article in that journal.
   * 
   * @param articleUrl the URL of the article
   * @return the eISSN for the given URL or null if not available
   */
  static public String getJournalEissnFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      return null;
    }
    String eissn = tdbAu.getEissn();
    return (eissn == null) ? null : eissn.replaceAll("-", "");
  }

  /**
   * Return the journal eISSN from that corresponds
   * to the auId of an AU in that journal.
   * 
   * @param pluginId the pluginID of an article AU
   * @param auKey the auKey of an article AU
   * @return the eISSN for the given auId or null if not available
   */
  static public String getJournalEissnFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    if (tdbAu == null) {
      return null;
    }
    String eissn = tdbAu.getEissn();
    return (eissn == null) ? null : eissn.replaceAll("-", "");
  }

  /**
   * Return the journal ISSN-L from that corresponds
   * to the URL of an article in that journal.
   * 
   * @param articleUrl the URL of the article
   * @return the ISSN-L for the given URL or null if not available
   */
  static public String getJournalIssnLFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      return null;
    }
    String issnl = tdbAu.getIssnL();
    return (issnl == null) ? null : issnl.replaceAll("-", "");
  }

  /**
   * Return the journal ISSN-L from that corresponds
   * to the auId of an AU in that journal.
   * 
   * @param pluginId the pluginID of an article AU
   * @param auKey the auKey of an article AU
   * @return the ISSN-L for the given auId or null if not available
   */
  static public String getJournalIssnLFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    if (tdbAu == null) {
      return null;
    }
    String issnl = tdbAu.getEissn();
    return (issnl == null) ? null : issnl.replaceAll("-", "");
  }

  /**
   * Return a journal ISSN that corresponds
   * to the URL of an article in that journal.
   * 
   * @param articleUrl the URL of the article
   * @return an ISSN for the given URL or null if not available
   */
  static public String getJournalIssnFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      return null;
    }
    String issn = tdbAu.getIssn();
    return (issn == null) ? null : issn.replaceAll("-", "");
  }

  /**
   * Return a journal ISSN that corresponds
   * to the auId of an AU in that journal.
   * 
   * @param pluginId the pluginID of an article AU
   * @param auKey the auKey of an article AU
   * @return an ISSN for the given auId or null if not available
   */
  static public String getJournalIssnFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    if (tdbAu == null) {
      return null;
    }
    String issn = tdbAu.getIssn();
    return (issn == null) ? null : issn.replaceAll("-", "");
  }

  /**
   * Return the journal print ISSN from that corresponds
   * to the URL of an article in that journal.
   * 
   * @param articleUrl the URL of the article
   * @return the print ISSN for the given URL or null if not available
   */
  static public String getJournalPrintIssnFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      return null;
    }
    String issn = tdbAu.getPrintIssn();
    return (issn == null) ? null : issn.replaceAll("-", "");
  }

  /**
   * Return the journal print ISSN from that corresponds
   * to the auId of an AU in that journal.
   * 
   * @param pluginId the pluginID of an article AU
   * @param auKey the auKey of an article AU
   * @return the print ISSN for the given auId or null if not available
   */
  static public String getJournalPrintIssnFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    if (tdbAu == null) {
      return null;
    }
    String printIssn = tdbAu.getPrintIssn();
    return (printIssn == null) ? null : printIssn.replaceAll("-", "");
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
    log.info("articleUrl: " + articleUrl + " ingestDate: " + ingestDate + " ingestYear: " + ingestYear);
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
    // get lockss daemon plugin manager
    if (pluginManager == null) {
      LockssDaemon daemon = LockssDaemon.getLockssDaemon();
      if (daemon == null) {
        log.info("no lockss daemon");
        return null;
      }
      pluginManager = daemon.getPluginManager();
    }
    
    // get the CachedUrl from the article URL
    CachedUrl cu = pluginManager.findCachedUrl(articleUrl);
    if (cu == null) {
      return null;
    }
    
    // get the ingest date from the CachedUrl
    CIProperties ciProps = cu.getProperties();
    String fetchTime = ciProps.getProperty(CachedUrl.PROPERTY_FETCH_TIME);
    if (fetchTime == null) {
      return null;
    }
    
    // last-modified date is of the form: Wed, 02 Nov 2011 06:11:51 GMT 
    try {
      Date date = new Date(Long.parseLong(fetchTime));
      String ingestDate = isoDateFormatter.format(date);
      return ingestDate;
    } catch (NumberFormatException ex) {
      log.warning("error parsing date: " + fetchTime);
      return null;
    }
  }
  
  /**
   * Return the title from the title database that corresponds
   * to the ISSN of the journal.
   * 
   * @param journalIssn the ISSN of the journal
   * @return the title for the given ISSN
   */
  static public String getJournalTitleFromIssn(String journalIssn) {
    if (journalIssn == null) {
      return null;
    }

    // get the tdb
    Tdb tdb = ConfigManager.getCurrentConfig().getTdb(); 
    if (tdb == null) {
      return null;
    }
  
    // get the title from the ISSN
    TdbTitle tdbTitle = tdb.getTdbTitleByIssn(journalIssn);
  
    // return the title
    return tdbTitle == null ? null : tdbTitle.getName();
  }

  /**
   * Return the publisher from the publisher database that corresponds
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
      return null;
    }
  
    // get the publisher from the ISSN
    TdbTitle tdbTitle = tdb.getTdbTitleByIssn(journalIssn);
    if (tdbTitle == null) {
      return null;
    }
    
    // return the publisher
    TdbPublisher tdbPublisher = tdbTitle.getTdbPublisher();
    return tdbPublisher == null ? null : tdbPublisher.getName();
  }
  
  /**
   * Return the journal title that corresponds to the auid of a journal AU.
   * 
   * @param pluginId the pluginId part of the auid
   * @param auKey the auKey part of the auid
   * @return the publisher for the given ISSN
   */
  static public String getJournalTitleFromAuId(String pluginId, String auKey) {
    TdbAu tdbAu = getTdbAuFromAuId(pluginId, auKey);
    return tdbAu == null ? null : tdbAu.getJournalTitle();
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
    log.debug3(  "pluginId: " + pluginId
               + " auKey: " + auKey
               + " ingestDate: " + ingestDate 
               + " ingestYear: " + ingestYear);
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

    // get lockss daemon plugin manager
    if (pluginManager == null) {
      LockssDaemon daemon = LockssDaemon.getLockssDaemon();
      if (daemon == null) {
        log.info("no lockss daemon");
        return null;
      }
      pluginManager = daemon.getPluginManager();
    }
    
    String auId = PluginManager.generateAuId(pluginId, auKey);
    ArchivalUnit au = pluginManager.getAuFromId(auId);
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
   * Return the starting volume for the specified AU.
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
  
  static public void main(String[] args) {
  }
}
