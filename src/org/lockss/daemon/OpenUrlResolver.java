/*
 * $Id: OpenUrlResolver.java,v 1.1 2011-01-25 00:49:30 pgust Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.daemon;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.Tdb;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbPublisher;
import org.lockss.config.TdbTitle;
import org.lockss.daemon.ConfigParamDescr.InvalidFormatException;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PrintfConverter;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.util.ExternalizableMap;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;
import org.lockss.util.UrlUtil;
import org.lockss.util.urlconn.LockssUrlConnection;
import org.lockss.util.urlconn.LockssUrlConnectionPool;


/**
 * This class  implements an OpenURL resolver that locates an article matching 
 * properties corresponding to OpenURL keys.  Both OpenURL 1.0 and the earlier 
 * OpenURL 0.1 syntax are supported. Queries can be made by:
 * <ul>
 * <li>URL</li> 
 * <li>DOI</li>
 * 
 * <li>ISSN/volume/issue/page</li>
 * <li>ISSN/volume/issue/article-number</li>
 * <li>ISSN/volume/issue/author</li>
 * <li>ISSN/volume/issue/article-title</li>
 * <li>ISSN/date/page</li>
 * <li>ISSN/date/article-number</li>
 * <li>ISSN/date/author</li>
 * <li>ISSN/date/article-title</li>
 *
 * <li>journal-title/volume/issue/page</li>
 * <li>journal-title/volume/issue/article-number</li>
 * <li>journal-title/volume/issue/author</li>
 * <li>journal-title/volume/issue/article-title</li>
 * <li>journal-title/date/page</li>
 * <li>journal-title/date/article-number</li>
 * <li>journal-title/date/author</li>
 * <li>journal-title/date/article-title</li>
 *
 * <li>ISBN/page</li>
 * <li>ISBN/chapter-author</li>
 * <li>ISBN/chapter-title</li>
 * 
 * <li>book-title/page</li>
 * <li>book-title/chapter-author</li>
 * <li>book-title/chapter-title</li>
 *
 * <li>book-publisher/book-title/page</li>
 * <li>book-publisher/book-title/chapter-author</li>
 * <li>book-publisher/book-title/chapter-title</li>
 * 
 * <li>SICI</li>
 * <li>BICI</li>
 * </ul>
 * <p>
 * Note: the TDB of the current configuration is used to resolve journal
 * or book title to an ISSN or ISBN.  If there are multiple entries for the
 * journal or book title, one of them is selected. OpenURL 1.0 allows
 * specifying a book publisher, so if both publisher and title are specified,
 * there is a good chance that the match will be unique.
 * 
 * @author  Philip Gust
 * @version 1.0
 */
public class OpenUrlResolver {
  private static Logger log = Logger.getLogger("OpenUrlResolver");

  private final LockssDaemon daemon;
  private final LockssUrlConnectionPool connectionPool;

  /** maximum redirects for looking up DOI url */
  private final int MAX_REDIRECTS = 10;
  
  /**
   * Create a resolver for the specified metadata manager.
   * 
   * @param metadataMgr the metadata manager
   */
  public OpenUrlResolver(LockssDaemon daemon) {
    if (daemon == null) {
      throw new IllegalArgumentException("Metadata Manager not specified");
    }
    this.daemon = daemon;
    connectionPool = new LockssUrlConnectionPool();
  }
  
  /**
   * Get an parameter either without or with the "rft." prefix.
   * 
   * @param params the parameters
   * @param key the key
   * @return the value or <code>null</code> if not present
   */
  private String getRftParam(Map<String,String> params, String key) {
    String value = null;
    if (params.containsKey(key)) {
      value = params.get(key);
    } else if (params.containsKey("rft." + key)) {
      value = params.get("rft." + key);
    }
    return value;
  }
  
  /**
   * Get date based on date, ssn (season), and quarter rft parameters.
   * 
   * @param params the parameters
   * @return a normalized date string of the form YYYY{-MM{-DD}}
   */
  private String getRftDate(Map<String,String> params) {
    String ssn = getRftParam(params, "ssn"); // spring, summer, fall, winter
    String quarter = getRftParam(params, "quarter");  // 1, 2, 3, 4
    String date = getRftParam(params, "date"); // YYYY{-MM{-DD}}
    
    // fill in month if only year specified
    if ((date != null) && (date.indexOf('-') < 0)) {
      if (quarter != null) {
        // fill in month based on quarter
        if (quarter.equals("1")) {
          date += "-01";
        } else if (quarter.equals("2")) {
          date += "-04";
        } else if (quarter.equals("3")) {
          date += "-07";
        } else if (quarter.equals("4")) {
          date += "-10";
        } else {
          log.debug("Invalid quarter: " + quarter);
        }
      } else if (ssn != null) {
        // fill in month based on season
        if (ssn.equals("spring")) {
          date += "-01";
        } else if (ssn.equals("summer")) {
          date += "-04";
        } else if (ssn.equals("fall")) {
          date += "-07";
        } else if (ssn.equals("winter")) {
          date += "-10";
        }
        log.debug("Invalid ssn: " + ssn);
      }
    }
    return date;
  }
  
  /**
   * Get start page base rft spage parameter or artnum if page not specified.
   * 
   * @param params the parameters
   * @return a start page -- not necessarily numeric
   */
  private String getRftStartPage(Map<String,String> params) {
    String spage = getRftParam(params, "spage");
    if (spage == null) {
      spage = getRftParam(params, "artnum");
    }
    return spage;
  }
  
  /**
   * Resolve an OpenURL from a set of parameter keys and values.
   * 
   * @param params the OpenURL parameters
   * @return a CachedURL or <code>null</code> if not found
   */
  public String resolveOpenUrl(Map<String,String> params) {
    if (params.containsKey("rft_id")) {
      String rft_id = params.get("rft_id");
      // handle rft_id that is an HTTP or HTTPS URL
      if (rft_id.startsWith("http:/") || rft_id.startsWith("https:/")) {
        return rft_id;
      } else if (rft_id.startsWith("info:doi/")) {
        String doi = rft_id.substring("info:doi/".length());
        String url = resolveFromDOI(doi); 
        if (url == null) {
          log.debug("Failed to resolve from DOI: " + doi);
        }
        return url;
      }
    }
    
    String spage = getRftStartPage(params);
    String author = getRftParam(params, "au");
    String isbn = getRftParam(params, "isbn");
    String atitle = getRftParam(params, "atitle");

    if (isbn != null) {
      // process a book based on ISBN
      String edition = getRftParam(params, "edition");
      String url = resolveFromIsbn(isbn, edition, spage, author, atitle);
      if (url == null) {
        log.debug("Failed to resolve from ISBN: " + isbn);
      } else {
        log.debug("Located url " + url + " for book ISBN " + isbn); 
      }
      return url;
    } else {
      // process a journal based on EISSN or ISSN
      String eissn = getRftParam(params, "eissn");
      String issn = getRftParam(params, "issn");
      String volume = getRftParam(params, "volume");
      String issue = getRftParam(params, "issue");
      String date = getRftDate(params);
      
      if ((eissn != null) || (issn != null)) {
        if (eissn != null) {
          // resolve from its eISSN
          String url = resolveFromIssn(eissn, date, volume, issue, spage, author, atitle);
          if (url != null) {
            String journalTitle = getRftParam(params, "jtitle");
            if (journalTitle == null) {
              journalTitle = getRftParam(params, "title");
            }
            log.debug("Located url " + url +
                      " for article \"" + atitle + "\"" +
                      ", ISSN " + issn +
                      ", title \"" + journalTitle + "\"");
            return url;
          }
          log.debug("Failed to resolve from eISSN: " + eissn);
        }
        
        if (issn != null) {
          String url = resolveFromIssn(issn, date, volume, issue, spage, author, atitle);
          if (url != null) {
            String journalTitle = getRftParam(params, "jtitle");
            if (journalTitle == null) {
              journalTitle = getRftParam(params, "title");
            }
            log.debug("Located url " + url +
                      " for article \"" + atitle + "\"" +
                      ", ISSN " + issn +
                      ", title \"" + journalTitle + "\"");
            return url;
          }
          log.debug("Failed to resolve from ISSN: " + issn);
        }
        return null;
      }
    }
    
    // process a book based on its title
    String bookTitle = params.get("rft.btitle");
    if (bookTitle != null) {
      // look up ISBN using book title
      Configuration config = ConfigManager.getCurrentConfig();
      if (config != null) {
        Tdb tdb = ConfigManager.getCurrentConfig().getTdb();
        if (tdb != null) {
          String edition = getRftParam(params, "edition");

          Collection<TdbTitle> titles = Collections.emptyList();
          String publisherName = params.get("rft.pub");
          if (publisherName != null) {
            TdbPublisher publisher = tdb.getTdbPublisher(publisherName);
            if (publisher != null) {
              titles = publisher.getTdbTitlesByName(bookTitle);
            } else {
              log.debug("Failed to locate publisher by name: \"" + publisherName + "\"");
            }
          }
          if (titles.isEmpty()) {
            // look up title for any publisher -- maybe publisher name not exact match
            titles = tdb.getTdbTitlesByName(bookTitle);
          }

          // try the ISBNs for each TdbTitle with a matching journal title
          for (TdbTitle title : titles) {
            isbn = title.getId();
            if (isbn != null) {
              String url = resolveFromIsbn(isbn, edition, spage, author, atitle);
              if (url != null) {
                String articleTitle = getRftParam(params, "atitle");
                log.debug("Located cachedURL " + url +
                          " for article \"" + articleTitle + "\"" +
                          ", ISBN " + isbn +
                          ", title \"" + title.getName() + "\"" +
                          ", publisher \""  + 
                          title.getTdbPublisher().getName() + "\"");
                return url;
              }
            }
          }
        } else {
          log.error("Failed to get tdb from current configuration");
        }
      }
      log.debug("Failed to resolve from book title: \"" + bookTitle + "\"");
      return null;
    }
    
    // process a journal based on its title
    String journalTitle = params.get("rft.jtitle");
    if (journalTitle == null) {
      journalTitle = getRftParam(params, "title");
    }
    if (journalTitle != null) {
      // look up ISSN using journal title
      Configuration config = ConfigManager.getCurrentConfig();
      if (config != null) {
        Tdb tdb = ConfigManager.getCurrentConfig().getTdb();
        if (tdb != null) {
          String volume = getRftParam(params, "volume");
          String issue = getRftParam(params, "issue");
          String date = getRftDate(params);
          
          // try the ISSNs for each TdbTitle with a matching journal title
          for (TdbTitle title : tdb.getTdbTitlesByName(journalTitle)) {
            String issn = title.getId();
            if (issn != null) {
              String url = resolveFromIssn(issn, date, volume, issue, spage, author, atitle);
              if (url != null) {
                String articleTitle = getRftParam(params, "atitle");
                log.debug("Located url " + url +
                          " for article \"" + articleTitle + "\"" +
                          ", ISSN " + issn +
                          ", title \"" + title.getName() + "\"" +
                          ", publisher \""  + 
                          title.getTdbPublisher().getName() + "\"");
                return url;
              }
            }
          }
        }
      }
      log.debug("Failed to resolve from journal title: \"" + journalTitle + "\"");
      return null;
    }
    
    String bici = params.get("rft.bici");
    if (bici != null) {
      // get cached URL from book book ICI code
      String url = null;
      try {
        url = resolveFromBici(bici);
        if (url == null) {
          log.debug("Failed to resolve from BICI: " + bici);
        }
      } catch (ParseException ex) {
        log.warning(ex.getMessage());
      }
      return url;
    }

    String sici = params.get("rft.sici");
    // get cached URL from serial ICI code
    if (sici != null) {
      String url = null;
      try {
        url = resolveFromSici(sici);
        if (url == null) {
          log.debug("Failed to resolve from SICI: " + sici);
        }
      } catch (ParseException ex) {
        log.warning(ex.getMessage());
      }
      return url;
    }

    return null;
  }

  /**
   * Resolve serials article based on the SICI descriptor. For an article 
   * "Who are These Independent Information Brokers?", Bulletin of the 
   * American Society for Information Science, Feb-Mar 1995, Vol. 21, no 3, 
   * page 12, the SICI would be: 0095-4403(199502/03)21:3<12:WATIIB>2.0.TX;2-J
   * 
   * @param sici a string representing the serials article SICI
   * @return the article url or <code>null</code> if not resolved
   * @throws ParseException if error parsing SICI
   */
  public String resolveFromSici(String sici) throws ParseException {
    int i = sici.indexOf('(');
    if (i < 0) {
      // did not find end of date section
      throw new ParseException("Missing start of date section", 0);
    }

    // validate ISSN after normalizing to remove punctuation
    String issn = sici.substring(0,i).replaceFirst("-", "");
    if (!issn.matches("^\\d{8}$")) {
      // ISSN is 8 characters
      throw new ParseException("Malformed ISSN", 0);
    }
    
    // skip over date section (199502/03)
    int j = sici.indexOf(')',i+1);
    if (j < 0) {
      // did not find end of date section
      throw new ParseException("Missing end of date section", i+1);
    }

    // get volume and issue between end of
    // date section and start of article section
    i = j+1;   // advance to start of volume
    j = sici.indexOf('<',i);
    if (j < 0) {
      // did not find start of issue section
      throw new ParseException("Missing start of issue section", i);
    }
    // get volume delimiter
    int k = sici.indexOf(':', i);
    if ((k < 0) || (k >= j)) {
      // no volume delimiter before start of issue section 
      throw new ParseException("Missing volume delimiter", i);
    }
    String volume = sici.substring(i,k);
    String issue = sici.substring(k+1,j);
    
    // get end of issue section
    i = j+1;
    k = sici.indexOf('>', i+1);
    if (k < 0) {
      // did not find end of issue section
      throw new ParseException("Missing end of issue section", i+1);
    }
    j = sici.indexOf(':',i+1);
    if ((j < 0) || (j >= k)) {
      throw new ParseException("Missing page delimiter", i+1);
    }
    String spage = sici.substring(i,j);
    
    // get the cached URL from the parsed paramaters
    String url = resolveFromIssn(issn, null, volume, issue, spage, null, null);
    if ((url != null) && log.isDebug()) {
      // report on the found article
      Tdb tdb = ConfigManager.getCurrentConfig().getTdb();
      String jTitle = null;
      if (tdb != null) {
        TdbTitle title = tdb.getTdbTitleById(issn);
        if (title != null) {
          jTitle = title.getName();
        }
      }
      String s = "Located cachedURL " + url +
      " for ISSN " + issn +
      ", volume: " + volume +
      ", issue: " + issue +
      ", start page: " + spage;
      if (jTitle != null) {
        s += ", journal title \"" + jTitle + "\"";
      }
      log.debug(s);
    }
    
    return url;
  }

  /**
   * Resolve a book chapter based on the BICI descriptor. For an item "English 
   * as a World Language", Chapter 10, in "The English Language: A Historical 
   * Introduction", 1993, pp. 234-261, ISBN 0-521-41620-5, the BICI would be 
   * 0521416205(1993)(10;EAAWL;234-261)2.2.TX;1-1
   * 
   * @param bici a string representing the book chapter BICI
   * @return the article url or <code>null</code> if not resolved
   * @throws ParseException if error parsing SICI
   */
  public String resolveFromBici(String bici) throws ParseException {
    int i = bici.indexOf('(');
    if (i < 0) {
      // did not find end of date section
      throw new ParseException("Missing start of date section", 0);
    }
    String isbn = bici.substring(0,i).replaceAll("-", "");

    // match ISBN-10 or ISBN-13 with 0-9 or X checksum character
    if (!isbn.matches("^(\\d{9}|\\d{12})[\\dX]$")) {
      // ISSB is 10 or 13 characters
      throw new ParseException("Malformed ISBN", 0);
    }

    // skip over date section (1993)
    int j = bici.indexOf(')',i+5);
    if (j < 0) {
      // did not find end of date section
      throw new ParseException("Missing end of date section", i+5);
    }

    // get volume and issue between end of
    // date section and start of article section
    if (bici.charAt(j+1) != '(') {
      // did not find start of chapter section
      throw new ParseException("Missing start of chapter section", j+1);
    }
    
    i = j+2;   // advance to start of chapter
    j = bici.indexOf(')',i);
    if (j < 0) {
      // did not find end of chapter section
      throw new ParseException("Missing end of chapter section", i);
    }
    
    // get chapter number delimiter
    int k = bici.indexOf(';', i);
    if ((k < 0) || (k >= j)) {
      // no chapter number delimiter before end of chapter section 
      throw new ParseException("Missing chapter number delimiter", i);
    }
    String chapter = bici.substring(i,k);
    
    // get end of chapter section
    i = k+1;
    k = bici.indexOf(';', i+1);
    if ((k < 0) || (k >= j)) {
      // no chapter abbreviation delimiter before end of chapter section
      throw new ParseException("Missing chapter abbreviation delimiter", i);
    }
    
    // extract the start page
    String spage = bici.substring(k+1,j);
    if (spage.indexOf('-') > 0) {
      spage = spage.substring(0, spage.indexOf('-'));
    }
    
    // PJG: what about chapter number?
    String url = resolveFromIsbn(isbn, null, spage, null, null);
    if ((url != null) && log.isDebug()) {
      Tdb tdb = ConfigManager.getCurrentConfig().getTdb();
      String bTitle = null;
      if (tdb != null) {
        TdbTitle title = tdb.getTdbTitleById(isbn);
        if (title != null) {
          bTitle = title.getName();
        }
      }
      String s = "Located cachedURL " + url +
      " for ISBN " + isbn +
      ", chapter: " + chapter +
      ", start page: " + spage;
      if (bTitle != null) {
        s += ", book title \"" + bTitle + "\"";
      }
      log.debug(s);
    }
    
    return url;
    
  }

  /**
   * Return the article URL from a DOI, using either the MDB or TDB.
   * @param doi the DOI
   * @return the article url
   */
  public String resolveFromDOI(String doi) {
    if (!doi.startsWith("10.")) {
      return null;
    }
    String url = resolveFromDoiWithMdb(doi);
    if (url == null) {
      url = resolveFromDoiWithTdb(doi);
    }
    return url;
  }    

  /**
   * Return the article URL from a DOI using the MDB.
   * @param doi the DOI
   * @return the article url
   */
  private String resolveFromDoiWithMdb(String doi) {
    MetadataManager metadataMgr;
    try {
      metadataMgr = daemon.getMetadataManager();
    } catch (IllegalArgumentException ex) {
      return null;
    }

    String url = null;
    Connection conn = null;
    try {
      conn = metadataMgr.newConnection();

      String MTN = MetadataManager.METADATA_TABLE_NAME;
      String DTN = MetadataManager.DOI_TABLE_NAME;
      String query =           
        "select access_url from " + MTN + "," + DTN 
      + " where " + DTN + ".md_id = " + MTN + ".md_id"
      + " and doi = '" + doi + "'";
      Statement stmt = conn.createStatement();
      ResultSet resultSet = stmt.executeQuery(query);
      if (resultSet.next()) {
        url = resultSet.getString(1);
      }
    } catch (SQLException ex) {
      log.error("Getting DOI:" + doi, ex);
      
    } finally {
      MetadataManager.safeClose(conn);
    }
    return url;
  }

  /**
   * Return the article URL from a DOI without using the MDB
   * @param doi the DOI
   * @return the article url
   */
  private String resolveFromDoiWithTdb(String doi) {
    PluginManager pluginMgr = daemon.getPluginManager();

    String url = "http://dx.doi.org/" + doi;
    try {
        for (int i = 0; i < MAX_REDIRECTS; i++) {
          // test case: 10.1063/1.3285176
          // Question: do we need to check for and resolve more levels of redirect?
          //   in the the test case, there is a second one
          LockssUrlConnection conn = UrlUtil.openConnection(url, connectionPool);
          conn.setFollowRedirects(false);
          conn.execute();
          String url2 = conn.getResponseHeaderValue("Location");
          if (url2 == null) {
            break;
          }
          url = UrlUtil.resolveUri(url, url2);
          log.debug(i + " resolved to: " + url);
          if (pluginMgr.findCachedUrl(url) != null) {
            break;
          }
      }
    } catch (Exception ex) {
      log.error("Getting DOI:" + doi, ex);
    }
    return url;
  }

  /**
   * Return the article URL from an ISSN, date, volume, issue, spage, and author. 
   * The first author will only be used when the starting page is not given.
   * 
   * @param issn the issn
   * @param date the publication date
   * @param volume the volume
   * @param issue the issue
   * @param spage the starting page
   * @param author the first author's full name
   * @param atitle the article title 
   * @return the article URL
   */
  public String resolveFromIssn(
    String issn, String date, String volume, String issue, 
    String spage, String author, String atitle) {
    String url = resolveFromIssnWithMdb(issn, date, volume, issue, spage, author, atitle);
    if (url == null) {
      url = resolveFromIssnWithTdb(issn, date, volume, issue);
    }
    return url;
  }
  
  /**
   * Return the article URL from an ISSN, date, volume, issue, spage, and author. 
   * The first author will only be used when the starting page is not given.
   * 
   * @param issn the issn
   * @param date the publication date
   * @param volume the volume
   * @param issue the issue
   * @param spage the starting page
   * @param author the first author's full name
   * @param atitle the article title 
   * @return the article URL
   */
  private String resolveFromIssnWithMdb(
      String issn, String date, String volume, String issue, 
      String spage, String author, String atitle) {
    MetadataManager metadataMgr;
    try {
      metadataMgr = daemon.getMetadataManager();
    } catch (IllegalArgumentException ex) {
      return null;
    }
    
    // strip punctuation
    issn = issn.replaceAll("-", "");
    
    String url = null;
    Connection conn = null;
    try {
      conn = metadataMgr.newConnection();

      String MTN = MetadataManager.METADATA_TABLE_NAME;
      String ITN = MetadataManager.ISSN_TABLE_NAME;
      String query =           
        "select access_url from " + MTN + "," + ITN 
      + " where " + ITN + ".md_id = " + MTN + ".md_id"
      + " and issn = '" + issn + "'";
      if (date != null) {
        query+= " and date='" + date + "'";
      }
      if (volume != null) {
        query+= " and volume='" + volume + "'";
      }
      if (issue != null) {
        query+= " and issue='" + issue + "'";
      }
      if (spage != null) {
        query+= " and start_page='" + spage + "'";
      } else if (author != null) {
        query+= " and author like '" + author + "%'";
      } else if (atitle != null) {
        query+= " and article_title like '" + atitle + "%'";
      }
      Statement stmt = conn.createStatement();
      ResultSet resultSet = stmt.executeQuery(query);
      if (resultSet.next()) {
        url = resultSet.getString(1);
      }
      stmt.close();
    } catch (SQLException ex) {
      log.error("Getting ISSN:" + issn, ex);
      
    } finally {
      MetadataManager.safeClose(conn);
    }
    return url;
  }

  /**
   * Return the article URL from an ISSN, date, volume, issue, spage, and author. 
   * The first author will only be used when the starting page is not given.
   * 
   * @param issn the issn
   * @param date the publication date
   * @param volume the volume
   * @param issue the issue
   * @return the article URL
   */
  private String resolveFromIssnWithTdb(
      String issn, String date, String volume, String issue) {
    String url = null;
    
    Configuration config = ConfigManager.getCurrentConfig();
    Tdb tdb = config.getTdb();
    TdbTitle title = tdb.getTdbTitleById(issn);
    if (title == null) {
      log.debug("No TdbTitle for issn " + issn);
      return null;
    }
    log.debug("TdbTitle found for issn: " + issn);
    TdbAu tdbau = null;
    boolean found = false;
    for (Iterator<TdbAu> itr = title.getTdbAus().iterator(); !found && itr.hasNext(); ) {
      tdbau = itr.next();
      
      if (volume != null) {
        String auVolume = tdbau.getParam("volume_name");
        if (auVolume == null) {
          auVolume = tdbau.getParam("volume");
        }
        if (auVolume == null) {
          auVolume = tdbau.getParam("volume_str");
        }
        if ((auVolume != null) && !volume.equals(auVolume)) {
          continue;
        }
      }

      if (date != null) {
        String auYear = tdbau.getParam("year");
        if ((auYear != null) && !date.startsWith(auYear)) {
          continue;
        }
      }

      found = true;;
    }

    log.debug("tdbau = " + ((tdbau == null) ? null : tdbau.getId()) + " found = " + found);

    if (tdbau != null) {
      url = tdbau.getParam("base_url");

      if (found) {
        PluginManager pluginMgr = daemon.getPluginManager();
        Plugin plugin = pluginMgr.getPlugin(PluginManager.pluginKeyFromId(tdbau.getPluginId()));
  
        if (plugin != null) {
          found = false;
/*
          log.debug("Found " + plugin.getAllAus().size() + " AU(s) for plugin: " + plugin.getPluginId());
          // find the au for the ISSN/EISSN, volume or year
          for (ArchivalUnit au : plugin.getAllAus()) {
            log.debug("auid: " + au.getAuId());
            TitleConfig tc = au.getTitleConfig();
            if (tdbau.equals(tc.getTdbAu())) {
              List<String> urls = au.getCrawlSpec().getStartingUrls();
              log.debug("auId: " + au.getAuId() + " number of urls: " + ((urls == null) ? -1 : urls.size()));
              url = (String)urls.get(0);
              found = true;
              break;
            }
          }
*/
          
          if (!found) {
            log.debug("geting starting url for plugin: " + plugin.getClass().getName());
            // get starting URL from a DefinablePlugin
            if (plugin instanceof DefinablePlugin) {
              url = getStartingUrl((DefinablePlugin)plugin, tdbau);
              log.debug("found starting url from definable plugin: " + url);
            }
          }
        } else {
          log.debug("No plugin found for key: " + PluginManager.pluginKeyFromId(tdbau.getPluginId()));
        }
      }
    }
    
    return url;
  }
  
  /**
   * Gets the starting URL for an AU indicated by the DefinablePlugin 
   * and parameter definitions specified by the TdbAu.
   * 
   * @param plugin the DefinablePlugin
   * @param tdbau the TdbAu
   * @return the starting URL
   */
  public String getStartingUrl(DefinablePlugin plugin, TdbAu tdbau) {
    String url = null;

    // get printf pattern for "au_start_url" property
    DefinablePlugin dplugin = (DefinablePlugin)plugin;
    ExternalizableMap map = dplugin.getDefinitionMap();
    Object obj = map.getMapElement("au_start_url");
    String printfString = null;
    if (obj instanceof String) {
      // get single pattern for start url
      printfString = (String)obj;
    } else if (obj instanceof Collection) {
      // get the first pattern for start url
      @SuppressWarnings("rawtypes")
      Collection c = (Collection)obj;
      if (!c.isEmpty()) {
        Object o = c.iterator().next();
        if (o instanceof String) {
          printfString = (String)o;
        }
      }
    }
    
    TypedEntryMap paramMap = new TypedEntryMap();
    for (ConfigParamDescr descr : plugin.getAuConfigDescrs()) {
      String key = descr.getKey();
      String sval = tdbau.getParam(key);
      if (sval == null) {
        sval = tdbau.getPropertyByName(key);
        if (sval == null) {
          sval = tdbau.getAttr(key);
        }
      }
      if (sval != null) {
        try {
          Object val = descr.getValueOfType(sval);
          paramMap.setMapElement(key, val);
        } catch (InvalidFormatException ex) {
          log.warning("invalid value for key: " + key + " value: " + sval, ex);
        }
      }
    }
    
    try {
      List<String> urls = new PrintfConverter.UrlListConverter(plugin, paramMap).getUrlList(printfString);
      if (urls.size() > 0) {
        url = urls.get(0);
        log.debug("evaluated: " + printfString + " to url: " + url);
      }
    } catch (Throwable ex) {
      log.warning("invalid  conversion", ex);
    }
    
    return url;
  }
  
  /**
   * Evaluate printf pattern using params, attrs, and props of TdbAu.
   * The printf pattern is of the form '"<printf str>", key1, ... , key',
   * where the first item is a quoted printf format string, and subsequent
   * items are property names whose values are used with the format string.
   * 
   * @param printfpat the printf pattern
   * @param tdbau the TdbAu
   * @return the result
   */
  String evalPrintfPat(String printfpat, TdbAu tdbau) {
    String result = null;
    // find the end of of the quoted format string
    int endpos = printfpat.lastIndexOf('"');
    if (endpos > 0) {
      // find the start of the parameter keys
      int firstpos = printfpat.indexOf(',', endpos+1);
      if (firstpos > 0) {
        // split into individual parameter keys
        String[] args = printfpat.substring(firstpos+1).split(",");
        for (int i = 0; i < args.length; i++) {
          // get parameter value for key from tdbau param, attr, or property
          String key = args[i].trim();
          String val = tdbau.getParam(key);
          if (val == null) {
            val = tdbau.getAttr(key);
          }
          if (val == null) {
            val = tdbau.getPropertyByName(key);
          }
          log.debug("key: '" + key + "' + value: '" + val + "'");
          if (val == null) {
            // can't evaluate pattern if argument is missing
            return null;
          }
          args[i] = val;
        }
        
        try {
          // evaluate format string with args
          String formatstr = printfpat.substring(1,endpos);
          result = String.format(formatstr, (Object[])args);
          log.debug("Evaluated format string to: " + result);
        } catch (IllegalFormatException ex) {
          log.debug("Error evaluating format string with tdbau args", ex);
        }
      }
    }
    
    return result;
  }

  /**
   * Return the article URL from an ISBN, edition, spage, and author.
   * The first author will only be used when the starting page is not given.
   * "Volume" is used to hold edition information in the metadata manager 
   * schema for books.  First author can be used in place of start page.
   * 
   * @param isbn the isbn
   * @param edition the edition
   * @param spage the start page
   * @param author the first author
   * @param atitle the chapter title
   * @return the article URL
   */
  public String resolveFromIsbn(
        String isbn, String edition, String spage, String author, String atitle) {
    String url = resolveFromIsbnWithMdb(isbn, edition, spage, author, atitle);
    if (url == null) {
      url = resolveFromIsbnWithTdb(isbn, edition);
    }
    return url;
  }

  /**
   * Return the article URL from an ISBN and edition using the Tdb.
   * "Volume" is used to hold edition information in the metadata manager 
   * schema for books..
   * 
   * @param isbn the isbn
   * @param edition the edition
   * @return the article URL
   */
  private String resolveFromIsbnWithTdb(String isbn, String edition) {
    String url = null;
    
    Configuration config = ConfigManager.getCurrentConfig();
    Tdb tdb = config.getTdb();
    TdbTitle title = tdb.getTdbTitleById(isbn);
    if (title == null) {
      log.debug("No TdbTitle for issn " + isbn);
      return null;
    }
    log.debug("TdbTitle found for issn: " + isbn);
    TdbAu tdbau = null;
    boolean found = false;
    for (Iterator<TdbAu> itr = title.getTdbAus().iterator(); !found && itr.hasNext(); ) {
      tdbau = itr.next();
      
      // get the plugin id for the TdbAu that matches the specified edition
      if (edition != null) {
        String auEdition = tdbau.getParam("edition");
        if ((auEdition != null) && !edition.equals(auEdition)) {
          continue;
        }
      }
      found = true;
    }
    log.debug("tdbau = " + ((tdbau == null) ? null : tdbau.getId()) + " found = " + found);

    if (tdbau != null) {
      url = tdbau.getParam("base_url");

      if (found) {
        PluginManager pluginMgr = daemon.getPluginManager();
        Plugin plugin = pluginMgr.getPlugin(PluginManager.pluginKeyFromId(tdbau.getPluginId()));
  
        if (plugin != null) {
          found = false;
/*
          log.debug("Found " + plugin.getAllAus().size() + " AU(s) for plugin: " + plugin.getPluginId());
          // find the au for the ISBN and edition
          for (ArchivalUnit au : plugin.getAllAus()) {
            log.debug("auid: " + au.getAuId());
            TitleConfig tc = au.getTitleConfig();
            if (tdbau.equals(tc.getTdbAu())) {
              List<String> urls = au.getCrawlSpec().getStartingUrls();
              log.debug("auId: " + au.getAuId() + " number of urls: " + ((urls == null) ? -1 : urls.size()));
              url = (String)urls.get(0);
              found = true;
              break;
            }
          }
*/
          
          if (!found) {
            log.debug("geting starting url for plugin: " + plugin.getClass().getName());
            // get starting URL from a DefinablePlugin
            if (plugin instanceof DefinablePlugin) {
              url = getStartingUrl((DefinablePlugin)plugin, tdbau);
              log.debug("found starting url from definable plugin: " + url);
            }
          }
        } else {
          log.debug("No plugin found for key: " + PluginManager.pluginKeyFromId(tdbau.getPluginId()));
        }
      }
    }
    
    return url;
  }
  
  /**
   * Return the article URL from an ISBN, edition, spage, and author using the
   * metadata database. The first author will only be used when the starting page 
   * is not given. "Volume" is used to hold edition information in the metadata 
   * manager schema for books.  First author can be used in place of start page.
   * 
   * @param isbn the isbn
   * @param edition the edition
   * @param spage the start page
   * @param author the first author
   * @param atitle the chapter title
   * @return the article URL
   */
  private String resolveFromIsbnWithMdb(
        String isbn, String edition, String spage, String author, String atitle) {
    MetadataManager metadataMgr;
    try {
      metadataMgr = daemon.getMetadataManager();
    } catch (IllegalArgumentException ex) {
      return null;
    }

    // strip punctuation
    isbn = isbn.replaceAll("[- ]", "");
    
    String url = null;
    Connection conn = null;
    try {
      conn = metadataMgr.newConnection();

      String MTN = MetadataManager.METADATA_TABLE_NAME;
      String ITN = MetadataManager.ISBN_TABLE_NAME;
      String query =           
        "select access_url from " + MTN + "," + ITN 
      + " where " + ITN + ".md_id = " + MTN + ".md_id"
      + " and isbn = '" + isbn + "'";

      if (edition != null) {
        query+= " and volume='" + edition + "'";
      }
      if (spage != null) {
        query+= " and start_page='" + spage + "'";
      } else if (author != null) {
        query+= " and author like '" + author + "%'";
      } else if (atitle != null) {
        query+= " and article_title like '" + atitle + "%'";
      }
      Statement stmt = conn.createStatement();
      ResultSet resultSet = stmt.executeQuery(query);
      if (resultSet.next()) {
        url = resultSet.getString(1);
      }
      stmt.close();
    } catch (SQLException ex) {
      log.error("Getting ISBN:" + isbn, ex);
      
    } finally {
      MetadataManager.safeClose(conn);
    }
    return url;
  }
}
