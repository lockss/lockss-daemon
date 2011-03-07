/*
 * $Id: OpenUrlResolver.java,v 1.1.2.2 2011-03-07 21:14:18 pgust Exp $
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
import java.util.ArrayList;
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
import org.lockss.plugin.PrintfConverter.UrlListConverter;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.util.ExternalizableMap;
import org.lockss.util.Logger;
import org.lockss.util.MetadataUtil;
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
   *   or YYYY-nQ for nth quarter, or YYYY-nX for nth season for
   *   n between 1 and 4.
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
          date += "-Q1";
        } else if (quarter.equals("2")) {
          date += "-Q2";
        } else if (quarter.equals("3")) {
          date += "-Q3";
        } else if (quarter.equals("4")) {
          date += "-Q4";
        } else {
          log.warning("Invalid quarter: " + quarter);
        }
      } else if (ssn != null) {
        // fill in month based on season
        if (ssn.equalsIgnoreCase("spring") || ssn.equals(1)) {
          date += "-S1";
        } else if (ssn.equalsIgnoreCase("summer") || ssn.equals(2)) {
          date += "-S2";
        } else if (ssn.equalsIgnoreCase("fall") || ssn.equals("3")) {
          date += "-S3";
        } else if (ssn.equalsIgnoreCase("winter") || ssn.equals("4")) {
          date += "-S4";
        }
        log.warning("Invalid ssn: " + ssn);
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
          log.debug3("Failed to resolve from DOI: " + doi);
        }
        return url;
      }
    }
    
    String spage = getRftStartPage(params);
    String author = getRftParam(params, "au");
    String atitle = getRftParam(params, "atitle");
    String isbn = getRftParam(params, "isbn");
    String eissn = getRftParam(params, "eissn");
    String issn = getRftParam(params, "issn");
    String volume = getRftParam(params, "volume");
    String issue = getRftParam(params, "issue");
    String edition = getRftParam(params, "edition");
    String date = getRftDate(params);

    if (isbn != null) {
      // process a book or monographic series based on ISBN
      String url = resolveFromIsbn(isbn, edition, spage, author, atitle);
      if (url == null) {
        log.debug3("Failed to resolve from ISBN: " + isbn);
      } else {
        log.debug3("Located url " + url + " for book ISBN " + isbn); 
      }
      return url;
    } else if ((eissn != null) || (issn != null)) {
      // process a journal based on EISSN or ISSN
      if (eissn != null) {
        // resolve from its eISSN
        String url = 
          resolveFromIssn(eissn, date, volume, issue, spage, author, atitle);
        if (url != null) {
          String title = getRftParam(params, "jtitle");
          if (title == null) {
            title = getRftParam(params, "title");
          }
          if (log.isDebug3()) log.debug3("Located url " + url +
                    " for article \"" + atitle + "\"" +
                    ", ISSN " + issn +
                    ", title \"" + title + "\"");
          return url;
        }
        log.debug3("Failed to resolve from eISSN: " + eissn);
      }
      
      if (issn != null) {
        String url = 
          resolveFromIssn(issn, date, volume, issue, spage, author, atitle);
        if (url != null) {
          String title = getRftParam(params, "jtitle");
          if (title == null) {
            title = getRftParam(params, "title");
          }
          if (log.isDebug3()) log.debug3("Located url " + url +
                    " for article \"" + atitle + "\"" +
                    ", ISSN " + issn +
                    ", title \"" + title + "\"");
          return url;
        }
        log.debug3("Failed to resolve from ISSN: " + issn);
      }
      return null;
    }
    
    
    // process a journal or book based on its title
    String title = getRftParam(params, "title");
    boolean isbook = false;
    boolean isjournal = false;
    if (title == null) {
      title = params.get("rft.jtitle");
      isjournal = title != null;
    }
    if (title == null) {
      title = params.get("rft.btitle");
      isbook = title != null;
    }
    if (title != null) {
      Configuration config = ConfigManager.getCurrentConfig();
      if (config != null) {
        Tdb tdb = ConfigManager.getCurrentConfig().getTdb();
        if (tdb != null) {
          Collection<TdbTitle> titles = Collections.emptySet();
          String pub = getRftParam(params, "pub");
          // limit search to publisher if specified, 
          // otherwise search all matching titles
          if (pub != null) {
            TdbPublisher tdbPub = tdb.getTdbPublisher(pub);
            if (tdbPub != null) {
              titles = tdbPub.getTdbTitlesByName(title);
            }
          } else {
            titles = tdb.getTdbTitlesByName(title);
          }

          // search all titles with either ISBN or ISSN first, because they
          // can be resolved to a specific article in the metadata database
          Collection <TdbTitle> notitles = new ArrayList<TdbTitle>();
          for (TdbTitle tdbTitle : titles) {
            String id = tdbTitle.getId();
            
            // PJG: monographic serials can have ISBNs too; not sure
            // whether OpenURL treats these as journals or books.
            if (!isjournal && MetadataUtil.isISBN(id, false)) {
              // try resolving from ISBN
              String url = resolveFromIsbn(id, edition, spage, author, atitle);
              if (url != null) {
                log.debug3("Located url " + url + " for book ISBN " + isbn); 
              }
              return url;
            }  else if (!isbook && MetadataUtil.isISSN(id)) {
              // try resolving from ISSN
              String url = 
                resolveFromIssn(id, date, volume, issue, spage, author, atitle);
              if (url != null) {
                if (log.isDebug3()) log.debug3("Located url " + url +
                          " for article \"" + atitle + "\"" +
                          ", ISSN " + issn +
                          ", title \"" + title + "\"" +
                          ", publisher \""  + 
                          tdbTitle.getTdbPublisher().getName() + "\"");
                return url;
              }
            } else {
              notitles.add(tdbTitle);
            }
          }

          // search titles with no isbn or issn identifier 
          for (TdbTitle tdbTitle : notitles) {
            String url = resolveFromTdbTitle(tdbTitle, date, volume, issue);
            if (url != null) {
              if (log.isDebug3()) log.debug3("Located url " + url +
                        ", title \"" + tdbTitle.getName() + "\"" +
                        ", publisher \""  + 
                        tdbTitle.getTdbPublisher().getName() + "\"");
              return url;
            }            
          }
        }
      }
      log.debug3("Failed to resolve from title: \"" + title + "\"");
      return null;
    }
    
    String bici = params.get("rft.bici");
    if (bici != null) {
      // get cached URL from book book ICI code
      String url = null;
      try {
        url = resolveFromBici(bici);
        if (url == null) {
          log.debug3("Failed to resolve from BICI: " + bici);
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
          log.debug3("Failed to resolve from SICI: " + sici);
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
    if (!MetadataUtil.isISSN(issn)) {
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
      if (log.isDebug3())  {
        String s = "Located cachedURL " + url
                   + " for ISSN " + issn
                   + ", volume: " + volume
                   + ", issue: " + issue 
                   + ", start page: " + spage;
        if (jTitle != null) {
          s += ", journal title \"" + jTitle + "\"";
        }
        log.debug3(s);
      }
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
   * @throws ParseException if error parsing BICI
   */
  public String resolveFromBici(String bici) throws ParseException {
    int i = bici.indexOf('(');
    if (i < 0) {
      // did not find end of date section
      throw new ParseException("Missing start of date section", 0);
    }
    String isbn = bici.substring(0,i).replaceAll("-", "");

    // match ISBN-10 or ISBN-13 with 0-9 or X checksum character
    if (!MetadataUtil.isISBN(isbn, false)) {
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
      if (log.isDebug3())  {
        String s = "Located cachedURL " + url +
        " for ISBN " + isbn +
        ", chapter: " + chapter +
        ", start page: " + spage;
        if (bTitle != null) {
          s += ", book title \"" + bTitle + "\"";
        }
        log.debug3(s);
      }
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
    String url = null;
    try {
      // resolve from metadata manager
      MetadataManager metadataMgr = daemon.getMetadataManager();
      url = resolveFromDoi(metadataMgr, doi);
    } catch (IllegalArgumentException ex) {
    }
    
    if (url == null) {
      // use DOI International resolver for DOI
      PluginManager pluginMgr = daemon.getPluginManager();

      url = "http://dx.doi.org/" + doi;
      try {
          for (int i = 0; i < MAX_REDIRECTS; i++) {
            // test case: 10.1063/1.3285176
            // Question: do we need to check for and resolve more levels of 
            //  redirect? In the the test case, there is a second one
            LockssUrlConnection conn = 
              UrlUtil.openConnection(url, connectionPool);
            conn.setFollowRedirects(false);
            conn.execute();
            String url2 = conn.getResponseHeaderValue("Location");
            if (url2 == null) {
              break;
            }
            url = UrlUtil.resolveUri(url, url2);
            log.debug3(i + " resolved to: " + url);
            if (pluginMgr.findCachedUrl(url) != null) {
              break;
            }
        }
      } catch (Exception ex) {
        log.error("Getting DOI:" + doi, ex);
      }
    }
    return url;
  }    

  /**
   * Return the article URL from a DOI using the MDB.
   * @param metadataMgr the metadata manager
   * @param doi the DOI
   * @return the article url
   */
  private String resolveFromDoi(MetadataManager metadataMgr, String doi) {
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
   * Return article URL from an ISSN, date, volume, issue, spage, and author. 
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
    String url = null;
    try {
      // resolve from metadata manager
      MetadataManager metadataMgr = daemon.getMetadataManager();
      url = resolveFromIssn(metadataMgr, issn, date, 
                            volume, issue, spage, author, atitle);
    } catch (IllegalArgumentException ex) {
    }

    if (url == null) {
      // resolve from TDB
      Configuration config = ConfigManager.getCurrentConfig();
      TdbTitle title = config.getTdb().getTdbTitleById(issn);
      if (title == null) {
        log.debug3("No TdbTitle for issn " + issn);
        return null;
      }
      return resolveFromTdbTitle(title, date, volume, issue);
    }
    return url;
  }
  
  /**
   * Return article URL from an ISSN, date, volume, issue, spage, and author. 
   * The first author will only be used when the starting page is not given.
   * 
   * @param metadataMgr the metadata manager
   * @param issn the issn
   * @param date the publication date
   * @param volume the volume
   * @param issue the issue
   * @param spage the starting page
   * @param author the first author's full name
   * @param atitle the article title 
   * @return the article URL
   */
  private String resolveFromIssn(
      MetadataManager metadataMgr,
      String issn, String date, String volume, String issue, 
      String spage, String author, String atitle) {

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
        // enables query "2009" to match "2009-05-10" in database
        query+= " and date like '" + date + "%'";
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
   * Return article URL from a TdbTitle, date, volume, and issue. 
   * 
   * @param title the TdbTitle
   * @param date the publication date
   * @param volume the volume
   * @param issue the issue
   * @return the article URL
   */
  private String resolveFromTdbTitle(
    TdbTitle title, String date, String volume, String issue) {
    TdbAu tdbau = null;
    boolean found = false;
    for (Iterator<TdbAu> itr = title.getTdbAus().iterator(); 
         !found && itr.hasNext(); ) {
      tdbau = itr.next();
      
      if (volume != null) {
        String auVolume = tdbau.getParam("volume");
        if (auVolume == null) {
          auVolume = tdbau.getParam("volume_name");
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
        if (auYear == null) {
          auYear = tdbau.getAttr("year");
        }
        if ((auYear != null) && !date.startsWith(auYear)) {
          continue;
        }
      }

      found = true;
    }

    if (log.isDebug3()) { 
      log.debug3(  "tdbau = " + ((tdbau == null) ? null : tdbau.getId()) 
                + " found = " + found);
    }
    String url = null;  // should be the title URL
    if (tdbau != null) {
      if (found) {
        url = getStartingUrl(tdbau);
      }
      if (url == null) {
        url = tdbau.getParam("base_url");  // baseURL isn't always a real URL
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
      UrlListConverter converter = 
        new  PrintfConverter.UrlListConverter(plugin, paramMap); 
      List<String> urls = converter.getUrlList(printfString);
      if (urls.size() > 0) {
        url = urls.get(0);
        log.debug3("evaluated: " + printfString + " to url: " + url);
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
          log.debug3("key: '" + key + "' + value: '" + val + "'");
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
          log.debug3("Evaluated format string to: " + result);
        } catch (IllegalFormatException ex) {
          log.debug3("Error evaluating format string with tdbau args", ex);
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
    String url = null;
    try {
      // resolve from metadata manager
      MetadataManager metadataMgr = daemon.getMetadataManager();
      url = resolveFromIsbn(metadataMgr, isbn, edition, spage, author, atitle);
    } catch (IllegalArgumentException ex) {
    }

    if (url == null) {
      // resolve from TDB
      Configuration config = ConfigManager.getCurrentConfig();
      TdbTitle title = config.getTdb().getTdbTitleById(isbn);
      if (title == null) {
        log.debug3("No TdbTitle for isbn " + isbn);
        return null;
      }
      return resolveFromTdbTitle(title, edition);
    }
    return url;
  }

  /**
   * Return the book URL from TDBTitle and edition.
   * 
   * @param title the TdbTitle
   * @param edition the edition
   * @return the book URL
   */
  private String resolveFromTdbTitle(TdbTitle title, String edition) {
    TdbAu tdbau = null;
    boolean found = false;
    for (Iterator<TdbAu> itr = title.getTdbAus().iterator(); 
         !found && itr.hasNext(); ) {
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
    if (log.isDebug3()) { 
      log.debug3(  "tdbau = " + ((tdbau == null) ? null : tdbau.getId()) 
                + " found = " + found);
    }
    String url = null;  // should be the title URL
    if (tdbau != null) {
      if (found) {
        url = getStartingUrl(tdbau);
      }
      if (url == null) {
        url = tdbau.getParam("base_url");  // baseURL isn't always a real URL
      }
    }
    return url;
  }
  
  /**
   * Get starting url from TdbAu.
   * @param tdbau the TdbAu
   * @return the starting URL
   */
  private String getStartingUrl(TdbAu tdbau) {
    PluginManager pluginMgr = daemon.getPluginManager();
    String pluginKey = PluginManager.pluginKeyFromId(tdbau.getPluginId());
    Plugin plugin = pluginMgr.getPlugin(pluginKey);

    String url = null;
    if (plugin != null) {
      log.debug3("geting starting url for plugin: " + plugin.getClass().getName());
      // get starting URL from a DefinablePlugin
      if (plugin instanceof DefinablePlugin) {
        url = getStartingUrl((DefinablePlugin)plugin, tdbau);
        log.debug3("Found starting url from definable plugin: " + url);
      }
    } else {
      log.debug3("No plugin found for key: " + pluginKey); 
    }
    return url;
  }
    
  /**
   * Return the article URL from an ISBN, edition, spage, and author using the
   * metadata database. The first author is only used when the starting page 
   * is not given. "Volume" is used to hold edition information in the metadata 
   * manager schema for books.  First author can be used in place of start page.
   * 
   * @param metadataMgr the metadata manager
   * @param isbn the isbn
   * @param edition the edition
   * @param spage the start page
   * @param author the first author
   * @param atitle the chapter title
   * @return the article URL
   */
  private String resolveFromIsbn(
        MetadataManager metadataMgr, String isbn, 
        String edition, String spage, String author, String atitle) {
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
        query+= " and (";
        // match single author
        query+= " author = '" + author + "'";
        // match first author
        query+= " or author like '" + author + ";%'";
        // match last author
        query+= " or author like '%;" + author + "'";
        // match any middle author
        query+= " or author like '%;" + author + ";%'";
        query+= ")";
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
