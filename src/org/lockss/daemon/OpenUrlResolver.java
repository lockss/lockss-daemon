/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import static org.lockss.db.SqlConstants.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.sql.*;
import java.text.ParseException;
import java.util.*;

import org.apache.commons.lang3.StringEscapeUtils;
import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.daemon.AuParamType.InvalidFormatException;
import org.lockss.db.*;
import org.lockss.exporter.biblio.*;
import org.lockss.plugin.*;
import org.lockss.plugin.AuUtil.AuProxyInfo;
import org.lockss.plugin.PluginManager.CuContentReq;
import org.lockss.plugin.PrintfConverter.UrlListConverter;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.proxy.ProxyManager;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

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
 * Note: the TDB of the current configuration is used to resolve journal or
 * if the entry is not in the metadata database, or if the query gives a
 * journal or book title but no ISSN or ISBN.  If there are multiple entries 
 * for the journal or book title, one of them is selected. OpenURL 1.0 allows
 * specifying a book publisher, so if both publisher and title are specified,
 * there is a good chance that the match will be unique.
 * 
 * @author  Philip Gust
 * @version 1.0
 */
public class OpenUrlResolver {
  
  private static final Logger log = Logger.getLogger(OpenUrlResolver.class);

  /** the LOCKSS daemon */
  private final LockssDaemon daemon;
  /** the PluginManager */
  private final PluginManager pluginMgr;
  /** the ProxyManager */
  private final ProxyManager proxyMgr;
  
  /** maximum redirects for looking up DOI url */
  private static final int MAX_REDIRECTS = 10;
  
  /** prefix for config properties */
  public static final String PREFIX = Configuration.PREFIX + "openUrlResolver.";

  /**
   * Determines the maximum number of OpenUrlResolver publishers+providers 
   * that publish the same article when querying the metadata database.This 
   * number will certainly be very small (< 10)
   * 
   */
  public static final String PARAM_MAX_PUBLISHERS_PER_ARTICLE = PREFIX
      + "max_publishers_per_article";

  /**
   * Default value of OpenUrlResolver max_publishers_per_article default
   * configuration parameter.
   */
  public static final int DEFAULT_MAX_PUBLISHERS_PER_ARTICLE = 10;

  
  private static final class FeatureEntry {
    final String auFeatureKey;
    final OpenUrlInfo.ResolvedTo resolvedTo;
    
    public FeatureEntry(String auFeatureKey, 
                        OpenUrlInfo.ResolvedTo resolvedTo) {
      this.auFeatureKey = auFeatureKey;
      this.resolvedTo = resolvedTo;
    }
  }
  
  private static final String FEATURE_URLS =
    DefinableArchivalUnit.KEY_AU_FEATURE_URL_MAP;

  private static final String START_URLS =
    DefinableArchivalUnit.KEY_AU_START_URL;





  /**
   * Keys to search for a matching journal feature. The order of the keys 
   * is the order they will be tried, from article, to issue, to volume, 
   * to title, to publisher.
   */
  private static final FeatureEntry[] auJournalFeatures = {
//    FEATURE_URLS + "/au_abstract",
    new FeatureEntry(FEATURE_URLS + "/au_article",OpenUrlInfo.ResolvedTo.ARTICLE),
    new FeatureEntry(FEATURE_URLS + "/au_issue", OpenUrlInfo.ResolvedTo.ISSUE),
    new FeatureEntry(FEATURE_URLS + "/au_volume", OpenUrlInfo.ResolvedTo.VOLUME),
    new FeatureEntry(START_URLS, OpenUrlInfo.ResolvedTo.VOLUME),
    new FeatureEntry(FEATURE_URLS + "/au_title", OpenUrlInfo.ResolvedTo.TITLE),
    new FeatureEntry(FEATURE_URLS + "/au_publisher", OpenUrlInfo.ResolvedTo.PUBLISHER),
  };
  
  /**
   * Keys to search for a matching book feature. The order of the keys is the
   * the order they will be tried, from chapter, to volume, to title, to 
   * publisher.
   */
  private static final FeatureEntry[] auBookAuFeatures = {
    new FeatureEntry(FEATURE_URLS + "/au_chapter", OpenUrlInfo.ResolvedTo.CHAPTER),
    new FeatureEntry(FEATURE_URLS + "/au_volume", OpenUrlInfo.ResolvedTo.VOLUME),
    new FeatureEntry(START_URLS, OpenUrlInfo.ResolvedTo.VOLUME),
    new FeatureEntry(FEATURE_URLS + "/au_title", OpenUrlInfo.ResolvedTo.TITLE),
    new FeatureEntry(FEATURE_URLS + "/au_publisher", OpenUrlInfo.ResolvedTo.PUBLISHER),
  };
  
  /** The name of the TDB au_feature key selector */
  static final String AU_FEATURE_KEY = "au_feature_key";
  
  // pre-defined OpenUrlInfo for no url
  public static final OpenUrlInfo noOpenUrlInfo = 
      new OpenUrlInfo(null, null, OpenUrlInfo.ResolvedTo.NONE);

  /**
   * Information returned by OpenUrlResolver includes the resolvedUrl
   * and the resolvedTo enumeration.
   */
  public static final class OpenUrlInfo 
    implements Iterable<OpenUrlInfo> {
    static public enum ResolvedTo {
      PUBLISHER, // resolved to a publisher
      TITLE,     // resolved to a tite of a serial (e.g. a journal or
                 // a book series) or other pubication
      VOLUME,    // resolved to a volume of a serial or other pubication,
                 // or the title of an individual book
      CHAPTER,   // resolved to a chapter of a book or other publication
      ISSUE,     // resolved to an issue of a serial or other publication
      ARTICLE,   // resolved to an article of a serial, book, or other pubication
      OTHER,     // resolved to an element of a publication
      NONE,      // not resolved if URL is null, or not in cache if has URL
    };
    
    private String resolvedUrl;
    private String proxySpec;
    private ResolvedTo resolvedTo;
    private BibliographicItem resolvedBibliographicItem = null;
    private OpenUrlInfo nextInfo = null;
    
    private OpenUrlInfo(String resolvedUrl, 
                        String proxySpec,
                        ResolvedTo resolvedTo) {
      this.resolvedUrl = resolvedUrl;
      this.resolvedTo = resolvedTo;
      this.proxySpec = proxySpec;
    }

    protected static OpenUrlInfo newInstance(
        String resolvedUrl, String proxySpec, ResolvedTo resolvedTo) {
      return ((resolvedTo == ResolvedTo.NONE) && (resolvedUrl == null)) 
          ? OpenUrlResolver.noOpenUrlInfo
          : new OpenUrlInfo(resolvedUrl, proxySpec, resolvedTo);
    }
    protected static OpenUrlInfo newInstance(String resolvedUrl) {
      return (resolvedUrl == null) 
          ? noOpenUrlInfo 
          : new OpenUrlInfo(resolvedUrl, null, OpenUrlInfo.ResolvedTo.OTHER);
    }
    protected static OpenUrlInfo newInstance(String resolvedUrl,
                                             String proxySpec) {
      return (resolvedUrl == null) 
        ? noOpenUrlInfo 
        : new OpenUrlInfo(resolvedUrl, proxySpec, OpenUrlInfo.ResolvedTo.OTHER);
    }
    
    public String getProxySpec() {
      return proxySpec;
    }
    public String getProxyHost() {
      if (proxySpec == null) { 
        return null;
      }
      int i = proxySpec.indexOf(':');
      return (i < 0) ? proxySpec : proxySpec.substring(0,i);
    }
    public int getProxyPort() {
      if (proxySpec == null) {
        return 0;
      }
      int i = proxySpec.indexOf(':');
      try {
        return (i < 0) ? 0 : Integer.parseInt(proxySpec.substring(i+1));
      } catch (NumberFormatException ex) {
        return 0;
      }
    }
    public String getResolvedUrl() {
      return resolvedUrl;
    }
    public ResolvedTo getResolvedTo() {
      return resolvedTo;
    }
    public BibliographicItem getBibliographicItem() {
      return resolvedBibliographicItem;
    }

    @Override
    public Iterator<OpenUrlInfo> iterator() {
      return new Iterator<OpenUrlInfo>() {
        OpenUrlInfo nextInfo = OpenUrlInfo.this;
        
        @Override
        public boolean hasNext() {
          return nextInfo != null;
        }

        @Override
        public OpenUrlInfo next() {
          if (nextInfo == null) {
            throw new NoSuchElementException();
          }
          OpenUrlInfo curInfo = nextInfo;
          nextInfo = curInfo.nextInfo;
          return curInfo;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }        
      };
    }

    public void add(OpenUrlInfo nextInfo) {
      if (nextInfo == null) {
        throw new IllegalArgumentException("nextInfo cannot be null");
      }
      last().nextInfo = nextInfo;
    }
    
    public int size() {
      int count = 1;
      OpenUrlInfo info = this;
      while (info.nextInfo != null) { info = info.nextInfo; count++; }
      return count;
    }
    
    public OpenUrlInfo last() {
      OpenUrlInfo info = this;
      while (info.nextInfo != null) { info = info.nextInfo; }
      return info;
    }

    public OpenUrlInfo next() {
      return nextInfo;
    }
    
    public boolean hasNext() {
      return nextInfo != null;
    }
    
    public String getOpenUrlQuery() {
      // don't use publisher or title url
      // because we don't preserve them
      if (   resolvedUrl != null
          && resolvedTo != null
          && !resolvedTo.equals(ResolvedTo.PUBLISHER)
          && !resolvedTo.equals(ResolvedTo.TITLE)) {
        return "rft_id=" + UrlUtil.encodeQueryArg(resolvedUrl);
      }

      if (resolvedBibliographicItem != null) {
        return OpenUrlResolver
            .getOpenUrlQueryForBibliographicItem(resolvedBibliographicItem);
      }
      
      return null;
    }
  }
  
  /**
   * Create a resolver for the specified database manager.
   * 
   * @param daemon the LOCKSS daemon
   */
  public OpenUrlResolver(LockssDaemon daemon) {
    if (daemon == null) {
      throw new IllegalArgumentException("LOCKSS daemon not specified");
    }
    this.daemon = daemon;
    this.pluginMgr = daemon.getPluginManager();
    this.proxyMgr = daemon.getProxyManager();
  }
  
  /**
   * Get an parameter either without or with the "rft." prefix.
   * 
   * @param params the parameters
   * @param key the key
   * @return the value or <code>null</code> if not present
   */
  private String getRftParam(Map<String,String> params, String key) {
    String value = params.get(key);
    if (value == null) {
      value = params.get("rft." + key);
    }
    return value;
  }
  
  /**
   * Get date based on date, ssn (season), and quarter rft parameters.
   * 
   * @param params the parameters
   * @return a normalized date string of the form YYYY{-MM{-DD}}
   *   or YYYY-Qn for nth quarter, or YYYY-Sn for nth season for
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
        if (ssn.equalsIgnoreCase("spring")) {
          date += "-S1";
        } else if (ssn.equalsIgnoreCase("summer")) {
          date += "-S2";
        } else if (ssn.equalsIgnoreCase("fall")) {
          date += "-S3";
        } else if (ssn.equalsIgnoreCase("winter")) {
          date += "-S4";
        }
        log.warning("Invalid ssn: " + ssn);
      }
    }
    return date;
  }
  
  /**
   * Returns the TdbTitle corresponding to the specified OpenUrl params.
   * 
   * @param params the OpenURL parameters
   * @return a TdbTitle or <code>null</code> if not found
   */
  public TdbTitle resolveTdbTitleForOpenUrl(Map<String,String> params) {
    Tdb tdb = ConfigManager.getCurrentConfig().getTdb();
    if (tdb != null) {
      // get TdbTitle for ISBN
      String isbn = getRftParam(params, "isbn");
      if (isbn != null) {
        Collection<TdbAu> tdbAus = tdb.getTdbAusByIsbn(isbn);
        return tdbAus.isEmpty() ? null : tdbAus.iterator().next().getTdbTitle();
      }
      
      // get TdbTitle for ISSN
      String issn = getRftParam(params, "issn");
      if (issn != null) {
        return tdb.getTdbTitleByIssn(issn);
      }
      
      
      // get TdbTitle for BICI
      String bici = getRftParam(params, "bici");
      if (bici != null) {
        int i = bici.indexOf('(');
        if (i > 0) {
          isbn = bici.substring(0,i);
          Collection<TdbAu> tdbAus = tdb.getTdbAusByIsbn(isbn);
          return tdbAus.isEmpty() ? null : tdbAus.iterator().next().getTdbTitle();
        }
      }

      // get TdbTitle for SICI
      String sici = getRftParam(params, "sici");
      if (sici != null) {
        int i = sici.indexOf('(');
        if (i > 0) {
          issn = sici.substring(0,i);
          return tdb.getTdbTitleByIssn(issn);
        }
      }

      // get TdbTitle for journal pubisher and title
      String publisher = getRftParam(params, "publisher");
      String title = getRftParam(params, "jtitle");
      if (title == null) {
        title = getRftParam(params, "title");
      }
      if (title != null) {
        Collection<TdbTitle> tdbTitles;
        if (publisher != null) {
          TdbPublisher tdbPublisher = tdb.getTdbPublisher(publisher);
          tdbTitles = (tdbPublisher == null) 
              ? Collections.<TdbTitle>emptyList() 
              :tdbPublisher.getTdbTitlesLikeName(title);
        } else {
          tdbTitles = tdb.getTdbTitlesByName(title);
        }
        return tdbTitles.isEmpty() ? null : tdbTitles.iterator().next();
      }
      
      // get TdbTitle for book pubisher and title
      String btitle = getRftParam(params, "btitle");
      if (btitle != null) {
        Collection<TdbAu> tdbAus;
        if (publisher != null) {
          TdbPublisher tdbPublisher = tdb.getTdbPublisher(publisher);
          tdbAus = (tdbPublisher == null) 
              ? Collections.<TdbAu>emptyList() 
              :tdbPublisher.getTdbAusLikeName(title);
        } else {
          tdbAus = tdb.getTdbAusByName(title);
        }
        return tdbAus.isEmpty() ? null : tdbAus.iterator().next().getTdbTitle();
      }
    }

    return null;
  }

  /**
   * Resolve an OpenURL from a set of parameter keys and values.
   * 
   * @param params the OpenURL parameters
   * @return a url or <code>null</code> if not found
   */
  public OpenUrlInfo resolveOpenUrl(Map<String,String> params) {
    log.debug3("params = " + params);

    OpenUrlInfo resolvedDirectly = noOpenUrlInfo;
    if (params.containsKey("rft_id")) {
      String rft_id = params.get("rft_id");
      // handle rft_id that is an HTTP or HTTPS URL
      if (UrlUtil.isHttpUrl(rft_id)) {
        resolvedDirectly = resolveFromUrl(rft_id);
        if (resolvedDirectly.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
          return resolvedDirectly;
        }
        log.debug3("Failed to resolve from URL: " + rft_id);
      } else if (rft_id.startsWith("info:doi/")) {
        String doi = rft_id.substring("info:doi/".length());
        resolvedDirectly = resolveFromDOI(doi); 
        if (resolvedDirectly.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
          return resolvedDirectly;
        }
        log.debug3("Failed to resolve from DOI: " + doi);
      }
    }
    
    if (params.containsKey("id")) {
      // handle OpenURL 0.1 DOI specification
      String id = params.get("id");
      if (id.startsWith("doi:")) {
        String doi = id.substring("doi:".length());
        resolvedDirectly = resolveFromDOI(doi);
        if (resolvedDirectly.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
          return resolvedDirectly;
        }
        log.debug3("Failed to resolve from DOI: " + doi);
      }
    }

    if (params.containsKey("doi")) {
      String doi = params.get("doi");
      resolvedDirectly = resolveFromDOI(doi);
      if (resolvedDirectly.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
        return resolvedDirectly;
      }
      log.debug3("Failed to resolve from DOI: " + doi);
    }      

    String pub = getRftParam(params, "pub");
    String spage = getRftParam(params, "spage");
    String artnum = getRftParam(params, "artnum");
    String author = getRftParam(params, "au");
    String atitle = getRftParam(params, "atitle");
    String isbn = getRftParam(params, "isbn");
    String eisbn = getRftParam(params, "eisbn");
    String edition = getRftParam(params, "edition");
    String date = getRftDate(params);
    String volume = getRftParam(params, "volume");

    Tdb tdb = ConfigManager.getCurrentConfig().getTdb();

    String anyIsbn = (eisbn != null) ? eisbn : isbn;
    if (anyIsbn != null) {
      OpenUrlInfo resolved = resolveFromIsbn(
          anyIsbn, pub, date, volume, edition, artnum, spage, author, atitle);
      if (resolved.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
        log.debug3(
            "Located url " 
          + ((resolved.resolvedUrl == null) ? "" : resolved.resolvedUrl) 
          + " for book ISBN " + anyIsbn); 
        return resolved;
      }
      log.debug3("Failed to resolve from ISBN: " + isbn);
    }
    
    String eissn = getRftParam(params, "eissn");
    String issn = getRftParam(params, "issn");
    String issue = getRftParam(params, "issue");
    
    // process a journal based on EISSN or ISSN
    String anyIssn = (eissn != null) ? eissn : issn;
    if (anyIssn != null) {
      // allow returning one result per publisher because
      // the item may be available from multiple publishers
      OpenUrlInfo resolved = resolveFromIssn(
            anyIssn, pub, date, volume, issue, spage, artnum, author, atitle);
        
      if (resolved.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
        if (log.isDebug3()) {
          String title = getRftParam(params, "jtitle");
          if (title == null) {
            title = getRftParam(params, "title");
          }
          log.debug3("Located url " 
              + ((resolved.resolvedUrl == null) ? "" : resolved.resolvedUrl)
              + " for article \"" + atitle + "\""
              + ", ISSN " + anyIssn
              + ", title \"" + title + "\"");
        }
        return resolved;
      }
      log.debug3("Failed to resolve from ISSN: " + anyIssn);
    }
    
    
    String bici = params.get("rft.bici");
    if (bici != null) {
      // get cached URL from book book ICI code
      OpenUrlInfo resolved = null;
      try {
        resolved = resolveFromBici(bici);
        if (resolved.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
          log.debug3(
              "Located url " 
            + ((resolved.resolvedUrl == null) ? "" : resolved.resolvedUrl) 
            + "for bici " + bici);
          return resolved;
        }
      } catch (ParseException ex) {
        log.warning(ex.getMessage());
      }
      log.debug3("Failed to resolve from BICI: " + bici);
    }

    String sici = params.get("rft.sici");
    // get cached URL from serial ICI code
    if (sici != null) {
      OpenUrlInfo resolved = null;
      try {
        resolved = resolveFromSici(sici);
        if (resolved.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
          log.debug3(
              "Located url " 
            + ((resolved.resolvedUrl == null) ? "" : resolved.resolvedUrl) 
            + "for sici " + sici);
          return resolved;
        }
      } catch (ParseException ex) {
        log.warning(ex.getMessage());
      }
      log.debug3("Failed to resolve from SICI: " + sici);
    }

    // process a journal or book based on its title
    String title = getRftParam(params, "title");
    boolean isbook = false;
    if (title == null) {
      title = params.get("rft.btitle");
      isbook = title != null;
    }
    if (title == null) {
      title = params.get("rft.jtitle");
    }
    
    if (title != null) {
      if (tdb == null) {
        // TODO: need to search metadata database only
        // for articles if no title database is specified
      } else {
        // only search the named publisher
        TdbPublisher tdbPub = null;
        if (pub != null) {
          tdbPub = tdb.getTdbPublisher(pub);
          // report no match if no matching publisher
          if (tdbPub == null) {
            return resolvedDirectly;
          }
        }
        
        if (isbook) {
          // search as though it is a book title
          Collection<TdbAu> tdbAus;
          if (tdbPub != null) {
            tdbAus = tdbPub.getTdbAusLikeName(title);
          } else {
            tdbAus = tdb.getTdbAusLikeName(title);
          }
          
          OpenUrlInfo resolved = null;
          Collection<TdbAu> noTdbAus = new ArrayList<TdbAu>();
          for (TdbAu tdbAu : tdbAus) {
            // search for book through its ISBN to ensure that both
            // metadata database and title database are consulted
            String id = tdbAu.getIsbn();
            if (id != null) {
              // try resolving from ISBN
              // note: non-standard treatment of 'artnum' as chapter identifier
              String tdbPubName = tdbAu.getPublisherName();
              OpenUrlInfo info = resolveFromIsbn(
                    id, tdbPubName, date, volume, issue, 
                    artnum, spage, author, atitle);
              if (info.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
                if (log.isDebug3()) {
                  log.debug3("Located url " 
                    + ((info.resolvedUrl == null) ? "" : info.resolvedUrl)
                    + " for article \"" + atitle + "\""
                    + ", ISBN " + id
                    + ", title \"" + title + "\""
                    + ", publisher \"" 
                    + tdbAu.getPublisherName() + "\"");
                }
                if (resolved == null) {
                  resolved = info;
                } else {
                  resolved.add(info);
                }
              }
            } else {
              // add to list of titles with no ISBN
              noTdbAus.add(tdbAu);
            }
          }
          if (resolved != null) {
            return resolved;
          }
        
          // search matching titles without ISBNs
          resolved =resolveBookFromTdbAus(
                      noTdbAus, date, volume, edition, artnum, spage);
          if (resolved.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
            if (log.isDebug3()) {
              log.debug3(  "Located url " + resolved.resolvedUrl 
                         + ", title \"" + title + "\"");
            }
          }       
          return resolved;
        } else {
          // search as though it is a journal title
          Collection<TdbTitle> tdbTitles;
          if (tdbPub != null) {
            // find title from specified publisher
            tdbTitles = tdbPub.getTdbTitlesByName(title);
            // find "like" titles if no exact matches
            if (tdbTitles.isEmpty()) {
              tdbTitles = tdbPub.getTdbTitlesLikeName(title);
            }
          } else {
            // find title from any publisher
            tdbTitles = tdb.getTdbTitlesByName(title);
            // find "like" titles if no exact matches
            if (tdbTitles.isEmpty()) {
              tdbTitles = tdb.getTdbTitlesLikeName(title);
            }
          }
          
          OpenUrlInfo resolved = null;
          Collection<TdbTitle> noTdbTitles = new ArrayList<TdbTitle>();
          for (TdbTitle tdbTitle : tdbTitles) {
            // search for journal through its ISSN to ensure that both
            // metadata database and title database are consulted
            String id = tdbTitle.getIssn();
            if (id != null) {
              // try resolving from ISSN
              String tdbPubName = tdbTitle.getPublisherName();
              OpenUrlInfo info = 
                resolveFromIssn(id, tdbPubName, 
                                date, volume, issue, spage, artnum, author, atitle);
              if (info.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
                if (log.isDebug3()) {
                  log.debug3("Located url " 
                    + ((info.resolvedUrl == null) ? "" : info.resolvedUrl)
                    + " for article \"" + atitle + "\""
                    + ", ISSN " + id 
                    + ", title \"" + title + "\""
                    + ", publisher \"" + tdbPubName + "\"");
                }
                if (resolved == null) {
                  resolved = info;
                } else {
                  resolved.add(info);
                }
              }
            } else {
              // add to list of titles with no ISBN or ISSN
              noTdbTitles.add(tdbTitle);
            }
          }
          if (resolved != null) {
            return resolved;
          }
          
          // search matching titles without ISSNs
          for (TdbTitle noTdbTitle : noTdbTitles) {
            Collection<TdbAu> tdbAus =  noTdbTitle.getTdbAus();
            OpenUrlInfo info = 
              resolveJournalFromTdbAus(tdbAus,date,volume,issue, spage, artnum);
            if (info.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
              if (log.isDebug3()) {
                log.debug3(  "Located url " 
                  + ((resolved.resolvedUrl == null) ? "" : resolved.resolvedUrl) 
                  + ", title \"" + title + "\"");
              }
              if (resolved == null) {
                resolved = info;
              } else {
                resolved.add(info);
              }
            }            
          }
          if (resolved != null) {
            return resolved;
          }
        }
      }

      OpenUrlInfo resolved = 
          OpenUrlInfo.newInstance(null, null, 
              isbook ? OpenUrlInfo.ResolvedTo.VOLUME 
                     : OpenUrlInfo.ResolvedTo.TITLE);

      // create bibliographic item with only title properties
      resolved.resolvedBibliographicItem =  
            new BibliographicItemImpl()
              .setPublisherName(pub)
              .setPublicationTitle(title);
      return resolved;
      
    } else  if (pub != null) {
      OpenUrlInfo resolved = OpenUrlInfo.newInstance(
          null, null, OpenUrlInfo.ResolvedTo.PUBLISHER);
      
      // create bibliographic item with only publisher properties
      resolved.resolvedBibliographicItem =  
        new BibliographicItemImpl()
          .setPublisherName(pub);
      return resolved;
    }

    return resolvedDirectly;
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
  public OpenUrlInfo resolveFromSici(String sici) throws ParseException {
    int i = sici.indexOf('(');
    if (i < 0) {
      // did not find end of date section
      throw new ParseException("Missing start of date section", 0);
    }

    // validate ISSN after normalizing to remove punctuation
    String issn = sici.substring(0,i).replaceFirst("-", "");
    if (!MetadataUtil.isIssn(issn)) {
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
    // note: no publisher with sici
    OpenUrlInfo resolved = 
        resolveFromIssn(issn, null, null, volume, issue, spage, null, null, null);
    if ((resolved.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) && log.isDebug()) {
      // report on the found article
      Tdb tdb = ConfigManager.getCurrentConfig().getTdb();
      String jTitle = null;
      if (tdb != null) {
        TdbTitle title = tdb.getTdbTitleByIssn(issn);
        if (title != null) {
          jTitle = title.getName();
        }
      }
      if (log.isDebug3())  {
        String s = "Located cachedURL "
                   + ((resolved.resolvedUrl == null) ? "" : resolved.resolvedUrl)
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
    
    return noOpenUrlInfo;
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
  public OpenUrlInfo resolveFromBici(String bici) throws ParseException {
    int i = bici.indexOf('(');
    if (i < 0) {
      // did not find end of date section
      throw new ParseException("Missing start of date section", 0);
    }
    String isbn = bici.substring(0,i).replaceAll("-", "");

    // match ISBN-10 or ISBN-13 with 0-9 or X checksum character
    if (!MetadataUtil.isIsbn(isbn, false)) {
      // ISSB is 10 or 13 characters
      throw new ParseException("Malformed ISBN", 0);
    }

    // skip over date section (1993)
    int j = bici.indexOf(')',i+1);
    if (j < 0) {
      // did not find end of date section
      throw new ParseException("Missing end of date section", i+5);
    }
    String date = bici.substring(i+1, j);

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
    
    // (isbn, date, volume, edition, chapter, spage, author, title) 
    // note: no publisher specified with bici
    OpenUrlInfo resolved = 
        resolveFromIsbn(isbn, null, date, null, null, chapter, spage, null, null);
    if ((resolved.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) && log.isDebug()) {
      Tdb tdb = ConfigManager.getCurrentConfig().getTdb();
      String bTitle = null;
      if (tdb != null) {
        Collection<TdbAu> tdbAus = tdb.getTdbAusByIsbn(isbn);
        if (!tdbAus.isEmpty()) {
          bTitle = tdbAus.iterator().next().getPublicationTitle();
        }
      }
      if (log.isDebug3())  {
        String s = "Located cachedURL " 
          + ((resolved.resolvedUrl == null) ? "" : resolved.resolvedUrl)
          + " for ISBN " + isbn
          + ", year: " + date
          + ", chapter: " + chapter
          + ", start page: " + spage;
        if (bTitle != null) {
          s += ", book title \"" + bTitle + "\"";
        }
        log.debug3(s);
      }
    }
    
    return noOpenUrlInfo;
    
  }

  
  /**
   * Resolves from a url.
   *  
   * @param aUrl the URL
   * @return a resolved URL
   */
  public OpenUrlInfo resolveFromUrl(String aUrl) {
    return resolveFromUrl(aUrl, null); // no proxy specified
  }

  /**
   * Resolves from a url. If URL is not in cache, returned OpenUrlInfo
   * resolvedTo indicator is ResolvedTo.NONE.
   *  
   * @param aUrl the URL
   * @param proxySpec a proxy string of the form "host:port"
   * @return OpenURLInfo with resolved URL
   */
  public OpenUrlInfo resolveFromUrl(String aUrl, String proxySpec) {
    String url = resolveUrl(aUrl, proxySpec);
    if (url != null) {
      CachedUrl cu = pluginMgr.findCachedUrl(url, CuContentReq.PreferContent);
      if (cu != null) {
        return OpenUrlInfo.newInstance(url, proxySpec);
      }
    }
    return noOpenUrlInfo;
  }
  
  
  /**
   * Validates a URL and resolve it by following indirects, and stopping
   * early if a URL that is in the LOCKSS cache is found.
   *  
   * @param aUrl the URL
   * @param auProxySpec an AU proxy spec of the form "host:port"
   * @return a resolved URL
   */
  String resolveUrl(String aUrl, String auProxySpec) { // protected for testing
    String url = aUrl;
    try {
      final LockssUrlConnectionPool connectionPool =
        proxyMgr.getQuickConnectionPool();
      
      // get proxy host and port for the proxy spec or the current config
      AuProxyInfo proxyInfo = AuUtil.getAuProxyInfo(auProxySpec);
      String proxyHost = proxyInfo.getHost();
      int proxyPort = proxyInfo.getPort();
      
      for (int i = 0; i < MAX_REDIRECTS; i++) {
        // no need to look further if content already cached
        if (pluginMgr.findCachedUrl(url) != null) {
          return url;
        }
        
        // test URL by opening connection
        LockssUrlConnection conn = null;
        try {
          conn = UrlUtil.openConnection(url, connectionPool);
          conn.setFollowRedirects(false);
          conn.setRequestProperty("user-agent", LockssDaemon.getUserAgent());

          if (!StringUtil.isNullString(proxyHost) && (proxyPort > 0)) {
            try {
              conn.setProxy(proxyHost, proxyPort);
            } catch (UnsupportedOperationException ex) {
              log.warning("Unsupported connection request proxy: " 
                          + proxyHost + ":" + proxyPort);
            }
          }
          
          conn.execute();
          
          // if not redirected, validate based on response code
          String url2 = conn.getResponseHeaderValue("Location");
          if (url2 == null) {
            int response = conn.getResponseCode();
            log.debug3(i + " response code: " + response);
            if (response == HttpURLConnection.HTTP_OK) {
              return url;
            }
            return null;
          }
          
          // resolve redirected URL and try again
          url = UrlUtil.resolveUri(url, url2);
          log.debug3(i + " resolved to: " + url);
        } finally {
          IOUtil.safeRelease(conn);
        }
      }
    } catch (IOException ex) {
      log.error("resolving from URL:" + aUrl + " with URL: " + url, ex);
    }
    return null;
  }
  
  /**
   * Return the article URL from a DOI, using either the MDB or TDB.
   * @param doi the DOI
   * @return the article url
   */
  public OpenUrlInfo resolveFromDOI(String doi) {
    if (!MetadataUtil.isDoi(doi)) {
      return noOpenUrlInfo;
    }
    OpenUrlInfo resolved = noOpenUrlInfo;
    try {
      // resolve from database manager
      DbManager dbMgr = daemon.getDbManager();
      resolved = resolveFromDoi(dbMgr, doi);
    } catch (IllegalArgumentException ex) {
    }
    
    if (resolved.resolvedTo == OpenUrlInfo.ResolvedTo.NONE) {
      // use DOI International resolver for DOI
      resolved = resolveFromUrl("http://dx.doi.org/" + doi);
    }
    return resolved;
  }    

  /**
   * Return the OpenUrl query string for the specified auid.
   * 
   * @param auid the auid
   * @return the OpenUrl query string, or null if not available
   */
  public String getOpenUrlQueryForAuid(String auid) {
    TdbAu tdbau = TdbUtil.getTdbAu(auid);
    if (tdbau != null) {
      return getOpenUrlQueryForBibliographicItem(tdbau);
    }
      
    // Try returning an OpenURL with the starting URL
    // corresponding to the AU with a SpiderCrawlSpec; 
    // by convention, the first URL is the manifest page 
    // (not for OAICrawlSpec or other types of CrawlSpec)
    ArchivalUnit au = pluginMgr.getAuFromId(auid);
    if (au != null) {
      Collection<String> urls = au.getStartUrls();
      if (urls.size() > 0) {
        return "rft_id=" + urls.iterator().next();
      }
    }
    
    return null;
  }
  
  
  /**
   * Return the OpenUrl query string for the specified bibliographic item.
   * 
   * @param bibitem the BibliographicItem
   * @return the OpenUrl query string, or null if not available
   */
  static public String getOpenUrlQueryForBibliographicItem(
      BibliographicItem bibitem) {
    StringBuffer sb = new StringBuffer();
    
    String isbn = bibitem.getIsbn();
    if (!StringUtil.isNullString(isbn)) {
      sb.append("&isbn=");
      sb.append(UrlUtil.encodeQueryArg(MetadataUtil.formatIsbn(isbn)));
    } 
    
    String issn = bibitem.getIssn();
    if (!StringUtil.isNullString(issn)) {
      sb.append("&issn=");
      sb.append(UrlUtil.encodeQueryArg(MetadataUtil.formatIssn(issn)));
    }

    String publisher = bibitem.getPublisherName();
    if (!StringUtil.isNullString(publisher)) {
      sb.append("&publisher=");
      sb.append(UrlUtil.encodeQueryArg(publisher));
    }

    String title = bibitem.getPublicationTitle();
    if (!StringUtil.isNullString(title)) {
      String pubType = bibitem.getPublicationType();
      if (   !StringUtil.isNullString(pubType)
          && pubType.startsWith("book")) {
        sb.append("&btitle");
      } else {
        sb.append("&jtitle=");
      }
      sb.append(UrlUtil.encodeQueryArg(title));
    }
    
    String year = bibitem.getStartYear();
    if (   !StringUtil.isNullString(year)
        && year.equals(bibitem.getYear())) {
      sb.append("&year="); 
      sb.append(UrlUtil.encodeQueryArg(year));
    }

    String volume = bibitem.getStartVolume();
    if (   !StringUtil.isNullString(volume)
        && volume.equals(bibitem.getVolume())) {
      sb.append("&volume="); 
      sb.append(UrlUtil.encodeQueryArg(volume));
    }
    
    String issue = bibitem.getStartIssue();
    if (   !StringUtil.isNullString(issue)
        && volume.equals(bibitem.getIssue())) {
      sb.append("&issue="); 
      sb.append(UrlUtil.encodeQueryArg(issue));
    }
    
    return (sb.length() == 0) ? null : sb.substring(1);    
  }

  /**
   * Return the article URL from a DOI using the MDB.
   * @param dbMgr the database manager
   * @param doi the DOI
   * @return the OpenUrlInfo
   */
  private OpenUrlInfo resolveFromDoi(DbManager dbMgr, String doi) {
    Connection conn = null;
    try {
      conn = dbMgr.getConnection();

      String query = "select u." + URL_COLUMN
	  + " from " + URL_TABLE + " u,"
	  + DOI_TABLE + " d"
	  + " where u." + MD_ITEM_SEQ_COLUMN + " = d." + MD_ITEM_SEQ_COLUMN
	  + " and upper(d." + DOI_COLUMN + ") = ?";
      
      PreparedStatement stmt = dbMgr.prepareStatement(conn, query);
      stmt.setString(1, doi.toUpperCase());
      ResultSet resultSet = dbMgr.executeQuery(stmt);
      if (resultSet.next()) {
        String url = resultSet.getString(1);
        OpenUrlInfo resolved = resolveFromUrl(url);
        return resolved;
        
      }
    } catch (SQLException ex) {
      log.error("Getting DOI:" + doi, ex);
    } catch (DbException dbe) {
      log.error("Getting DOI:" + doi, dbe);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
    return noOpenUrlInfo;
  }

  /**
   * Return article URL from an ISSN, date, volume, issue, spage, and author. 
   * The first author will only be used when the starting page is not given.
   * 
   * @param issn the issn
   * @param pub the publisher
   * @param date the publication date
   * @param volume the volume
   * @param issue the issue
   * @param spage the starting page
   * @param artnum the article number
   * @param author the first author's full name
   * @param atitle the article title 
   * @return the article URL
   */
  public OpenUrlInfo resolveFromIssn(
    String issn, String pub, String date, String volume, String issue, 
    String spage, String artnum, String author, String atitle) {
    OpenUrlInfo resolved = null;
    
    // try resolving from the metadata database first
    try {
      DbManager dbMgr = daemon.getDbManager();
      OpenUrlInfo aResolved = resolveFromIssn(dbMgr, issn, pub, date, 
                                  volume, issue, spage, artnum, author, atitle);
      if (aResolved.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
        return aResolved;
      }
    } catch (IllegalArgumentException ex) {
    }

    // get list of TdbTitles for issn
    Tdb tdb = ConfigManager.getCurrentConfig().getTdb();
    Collection<TdbTitle> titles;
    if (tdb == null) {
      titles = Collections.<TdbTitle>emptyList();
    } else if (pub != null) {
      TdbPublisher tdbPub = tdb.getTdbPublisher(pub);
      titles = (tdbPub == null) 
          ? Collections.<TdbTitle>emptyList() : tdbPub.getTdbTitlesByIssn(issn);
    } else {
        titles = tdb.getTdbTitlesByIssn(issn);
    }

    // try resolving from the title database
    for (TdbTitle title : titles) {
      OpenUrlInfo aResolved = null;
      
      // resolve title, volume, AU, or issue TOC from TDB
      Collection<TdbAu> tdbAus = title.getTdbAus();
      aResolved = resolveJournalFromTdbAus(tdbAus, date, volume, issue, spage, artnum);
      if (aResolved.resolvedTo.equals(OpenUrlInfo.ResolvedTo.NONE)) {
        aResolved = OpenUrlInfo.newInstance(
            null,null, OpenUrlInfo.ResolvedTo.TITLE);
          // create bibliographic item with only title properties
        aResolved.resolvedBibliographicItem =  
            new BibliographicItemImpl()
              .setPublisherName(title.getPublisherName())
              .setPublicationTitle(title.getName())
              .setProprietaryIds(title.getProprietaryIds())
              .setCoverageDepth(title.getCoverageDepth())
              .setPrintIssn(title.getPrintIssn())
              .setEissn(title.getEissn())
              .setIssnL(title.getIssnL());
         if (resolved == null) {
           resolved = aResolved;
         } else {
           resolved.add(aResolved);
         }
      } else {
        if (resolved != null) {
          // add ahead of any fall-back OpenUrlInfo records
          aResolved.add(resolved);
        }
        resolved = aResolved;
      }
    }
    return (resolved == null) ? noOpenUrlInfo : resolved;
  }
  
  /**
   * Return article URL from an ISSN, date, volume, issue, spage, and author. 
   * The first author will only be used when the starting page is not given.
   * 
   * @param dbMgr the database manager
   * @param issns a list of alternate ISSNs for the title
   * @param pub the publisher
   * @param date the publication date
   * @param volume the volume
   * @param issue the issue
   * @param spage the starting page
   * @param artnum the article number
   * @param author the first author's full name
   * @param atitle the article title 
   * @return the article URL
   */
  private OpenUrlInfo resolveFromIssn(
      DbManager dbMgr,
      String issn, String pub, String date, String volume, String issue, 
      String spage, String artnum, String author, String atitle) {
          
    // true if properties specified a journal item
    boolean hasJournalSpec =
        (date != null) || (volume != null) || (issue != null);

    // true if properties specify an article
    boolean hasArticleSpec =    (spage != null) || (artnum != null) 
                             || (author != null) || (atitle != null);

    Connection conn = null;
    OpenUrlInfo resolved = null;
    try {
      conn = dbMgr.getConnection();

      StringBuilder select = new StringBuilder("select distinct ");
      StringBuilder from = new StringBuilder(" from ");
      StringBuilder where = new StringBuilder(" where ");
      ArrayList<String> args = new ArrayList<String>();

      // return all related values for debugging purposes
      select.append("u." + URL_COLUMN);
      select.append(",pb." + PUBLISHER_NAME_COLUMN);
      select.append(",n1." + NAME_COLUMN + " publication_name");
      select.append(",i." + ISSN_COLUMN);
      select.append(",bi." + VOLUME_COLUMN);
      select.append(",bi." + ISSUE_COLUMN);
      select.append(",bi." + START_PAGE_COLUMN);
      select.append(",bi." + END_PAGE_COLUMN);
      select.append(",bi." + ITEM_NO_COLUMN);
      select.append(",n2." + NAME_COLUMN + " article_name");
      select.append(",pv2." + PROVIDER_NAME_COLUMN);
      
      from.append(MD_ITEM_TABLE + " mi1");              // publication md_item
      from.append("," + MD_ITEM_TABLE + " mi2");        // article md_item
      from.append("," + ISSN_TABLE + " i");
      from.append("," + PUBLICATION_TABLE + " pu");
      from.append("," + PUBLISHER_TABLE + " pb");
      from.append("," + MD_ITEM_NAME_TABLE + " n1");  // publication name
      from.append("," + MD_ITEM_NAME_TABLE + " n2");  // article name
      from.append("," + URL_TABLE + " u");
      from.append("," + BIB_ITEM_TABLE + " bi");
      from.append("," + PROVIDER_TABLE + " pv2");
      from.append("," + AU_MD_TABLE + " am2");

      where.append("mi2." + PARENT_SEQ_COLUMN + "=");
      where.append("mi1." + MD_ITEM_SEQ_COLUMN);
      
      where.append(" and i." + MD_ITEM_SEQ_COLUMN + "=");
      where.append("mi1." + MD_ITEM_SEQ_COLUMN);
      where.append(" and i." + ISSN_COLUMN + " = ?");
      args.add(MetadataUtil.toUnpunctuatedIssn(issn)); // strip punctuation

      where.append(" and pu." + MD_ITEM_SEQ_COLUMN + "=");
      where.append("mi1." + MD_ITEM_SEQ_COLUMN);
      where.append(" and pb." + PUBLISHER_SEQ_COLUMN + "=");
      where.append("pu." + PUBLISHER_SEQ_COLUMN);
      if (pub != null) {
        // match publisher if specified
        where.append(" and pb." + PUBLISHER_NAME_COLUMN + "= ?");
        args.add(pub);
      }
      where.append(" and n1." + MD_ITEM_SEQ_COLUMN + "=");
      where.append("mi1." + MD_ITEM_SEQ_COLUMN);
      where.append(" and n1." + NAME_TYPE_COLUMN + "='primary'");
      where.append(" and u." + MD_ITEM_SEQ_COLUMN + "=");
      where.append("mi2." + MD_ITEM_SEQ_COLUMN);
      where.append(" and u." + FEATURE_COLUMN + "='Access'");
      
      where.append(" and bi." + MD_ITEM_SEQ_COLUMN + "=");
      where.append("mi2." + MD_ITEM_SEQ_COLUMN);
      
      where.append(" and n2." + MD_ITEM_SEQ_COLUMN + "=");
      where.append("mi2." + MD_ITEM_SEQ_COLUMN);
      where.append(" and n2." + NAME_TYPE_COLUMN + "='primary'");
      
      where.append(" and mi2." + AU_MD_SEQ_COLUMN + "=");
      where.append("am2." + AU_MD_SEQ_COLUMN);
      where.append(" and am2." + PROVIDER_SEQ_COLUMN + "=");
      where.append("pv2." + PROVIDER_SEQ_COLUMN);
      
      if (hasJournalSpec) {
        // can specify an issue by a combination of date, volume and issue;
        // how these combine varies, so do the most liberal match possible
        // and filter based on multiple results
        if (date != null) {
          // enables query "2009" to match "2009-05-10" in database
          where.append(" and mi2." + DATE_COLUMN);
          where.append(" like ? escape '\\'");
          args.add(date.replace("\\","\\\\").replace("%","\\%") + "%");
        }
        
        if (volume != null) {
          where.append(" and bi." + VOLUME_COLUMN + " = ?");
          args.add(volume);
        }

        if (issue != null) {
          where.append(" and bi." + ISSUE_COLUMN + " = ?");
          args.add(issue);
        }
      }
                  
      // handle start page, author, and article title as
      // equivalent ways to specify an article within an issue
      if (hasArticleSpec) {
        // accept any of the three
        where.append(" and ( ");
          
        if (spage != null) {
          where.append("bi." + START_PAGE_COLUMN + " = ?");
          args.add(spage);
        }

        if (artnum != null) {
          if (spage != null) {
            where.append(" or ");
          }
          where.append("bi." + ITEM_NO_COLUMN + " = ?");
          args.add(artnum);
        }

        if (atitle != null) {
          if ((spage != null) || (artnum != null)) {
            where.append(" or ");
          }

          where.append("upper(n2." + NAME_COLUMN);
          where.append(") like ? escape '\\'");

          args.add(atitle.toUpperCase().replace("%","\\%") + "%");
        }

        if (author != null) {
          if ((spage != null) || (artnum != null) || (atitle != null)) {
            where.append(" or ");
          }

          from.append("," + AUTHOR_TABLE + " au");

          // add the author query to the query
          addAuthorQuery(author, where, args);
        }
    
        where.append(")");
      }
      
      String qstr = select.toString() + from.toString() + where.toString();
      // only one value expected; any more and the query was under-specified
      int maxPublishersPerArticle = getMaxPublishersPerArticle();
      String[][] results = new String[maxPublishersPerArticle+1][11];
      int count = resolveFromQuery(conn, qstr, args, results);
      if (count <= maxPublishersPerArticle) {
        // ensure at most one result per publisher+provider in case
        // more than one publisher+provider publishes the same serial
        Set<String> pubs = new HashSet<String>();
        for (int i = 0; i < count; i++) {
          // combine publisher and provider columns to determine uniqueness
          if (!pubs.add(results[i][1] + results[i][10])) {
            return noOpenUrlInfo;
          }
          OpenUrlInfo info = OpenUrlInfo.newInstance(results[i][0], null, 
                                                OpenUrlInfo.ResolvedTo.ARTICLE);
          if (resolved == null) {
            resolved = info;
          } else {
            resolved.add(info);
          }
        }
      }
    } catch (DbException dbe) {
      log.error("Getting ISSN:" + issn, dbe);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
    return (resolved == null) ? noOpenUrlInfo : resolved;
  }

  /**
   * Returns the maximumn number of publishers per article to allow when
   * querying the metadata database.
   * 
   * @return the maximum number of publishers per article to allow
   */
  static private int getMaxPublishersPerArticle() {
    int maxpubs = ConfigManager.getCurrentConfig()
                    .getInt(PARAM_MAX_PUBLISHERS_PER_ARTICLE, 
                            DEFAULT_MAX_PUBLISHERS_PER_ARTICLE);
    return (maxpubs <= 0) ? DEFAULT_MAX_PUBLISHERS_PER_ARTICLE : maxpubs;
    
  }
  
  /** 
   * Resolve query if a single URL matches.
   * 
   * @param conn the connection
   * @param query the query
   * @param args the args
   * @param results the results
   * @return the number of results returned
   * @throws DbException
   */
  private int resolveFromQuery(Connection conn, String query,
      List<String> args, String[][] results) throws DbException {
    final String DEBUG_HEADER = "resolveFromQuery(): ";
    log.debug3(DEBUG_HEADER + "query: " + query);

    PreparedStatement stmt =
	daemon.getDbManager().prepareStatement(conn, query);

    int count = 0;

    try {
      for (int i = 0; i < args.size(); i++) {
	log.debug3(DEBUG_HEADER + "  query arg:  " + args.get(i));
	stmt.setString(i + 1, args.get(i));
      }

      stmt.setMaxRows(results.length); // only need 2 to to determine if unique
      ResultSet resultSet = daemon.getDbManager().executeQuery(stmt);
      
      for ( ; count < results.length && resultSet.next(); count++) {
        for (int i = 0; i < results[count].length; i++) {
          results[count][i] = resultSet.getString(i+1);
        }
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot resolve from query", sqle);
    }

    return count;
  }

  /**
   * Return article URL from a TdbTitle, date, volume, and issue. 
   * 
   * @param tdbAus a collection of TdbAus that match an ISSNs
   * @param date the publication date
   * @param volume the volume
   * @param issue the issue
   * @param spage the start page
   * @param artnum the article number
   * @return the article URL
   */
  private OpenUrlInfo resolveJournalFromTdbAus(
    Collection<TdbAu> tdbAus, 
    String date, String volume, String issue, String spage, String artnum) {
    
    // get the year from the date
    String year = null;
    if (date != null) {
      try {
        year = Integer.toString(PublicationDate.parse(date).getYear());
      } catch (ParseException ex) {}
    }

    // list of AUs that match volume and year specified
    ArrayList<TdbAu> foundTdbAuList = new ArrayList<TdbAu>();
    
    // list of AUs that do not match volume and year specified
    ArrayList<TdbAu> notFoundTdbAuList = new ArrayList<TdbAu>();
    
    // find a TdbAu that matches the date, and volume
    for (TdbAu tdbAu : tdbAus) {

      // if neither year or volume specified, pick any TdbAu
      if ((volume == null) && (year == null)) {
        notFoundTdbAuList.add(tdbAu);
        continue;
      }
      
      // if volume specified, see if this TdbAu matches
      if (volume != null) {
        if (!tdbAu.includesVolume(volume)) {
          notFoundTdbAuList.add(tdbAu);
          continue;
        }
      }

      // if year specified, see if this TdbAu matches
      if (year != null) {
        if (!tdbAu.includesYear(year)) {
          notFoundTdbAuList.add(tdbAu);
          continue;
        }
      }
      
      foundTdbAuList.add(tdbAu);
    }

    // look for URL that is cached from list of matching AUs
    for (TdbAu tdbau : foundTdbAuList) {
      String aYear = year;
      if (aYear == null) {
        aYear = tdbau.getStartYear();
      }
      String aVolume = volume;
      if (aVolume == null) {
        aVolume = tdbau.getStartVolume();
      }
      String anIssue = issue;
      if (anIssue == null) {
        anIssue  = tdbau.getStartIssue();
      }
      OpenUrlInfo aResolved = 
          getJournalUrl(tdbau, aYear, aVolume, anIssue, spage, artnum);
      if (aResolved.resolvedUrl != null) {
        if  ( pluginMgr.findCachedUrl(aResolved.resolvedUrl) != null) {
          // found the URL if in cache
          return aResolved;
        }
        // even though getJournalUrl() checks that page exists,
        // we can't rely on resolved URL being usable if the TdbAu is down
        if (!tdbau.isDown()) {
          return aResolved;
        }
        log.debug2(  "discarding URL " + aResolved.resolvedUrl 
                   + " because tdbau is down: " + tdbau.getName());
      }
    }
    
    // use tdbau that is not down from notFoundTdbAuList to find the
    // title or publisher URL, since that is all we can return at this point
    for (TdbAu tdbau : notFoundTdbAuList) {
      if (!tdbau.isDown()) {
        OpenUrlInfo aResolved = getJournalUrl(tdbau, null, null, null, null, null);
        return aResolved;
      }
      log.debug2("discarding URL because tdbau is down: " + tdbau.getName());
    }
    
    // pick any AU to use for resolving the title as a last resort
    if (!notFoundTdbAuList.isEmpty()) {
      OpenUrlInfo aResolved = 
          getJournalUrl(notFoundTdbAuList.get(0), null, null, null, null, null);
      aResolved.resolvedUrl = null;
      return aResolved;
    }
    return noOpenUrlInfo;
  }
  
  /**
   * Return the type entry parameter map for the specified Plugin and TdbAu.
   * @param plugin the plugin
   * @param tdbau the AU
   * @return the parameter map
   */
  private static TypedEntryMap getParamMap(Plugin plugin, TdbAu tdbau) {
    TypedEntryMap paramMap = new TypedEntryMap();
    List<ConfigParamDescr> descrs = plugin.getAuConfigDescrs();
    for (ConfigParamDescr descr : descrs) {
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
    
    // add entries for attributes that do not correspond to AU params
    for (Map.Entry<String,String> entry : tdbau.getAttrs().entrySet()) {
      if (!paramMap.containsKey(entry.getKey())) {
        paramMap.setMapElement(entry.getKey(), entry.getValue());
      }
    }
    
	return paramMap;
  }
  
  /**
   * Return the type entry parameter map for the specified AU.
   * @param au the AU
   * @return the parameter map
   */
 /* for later use (pjg)
  private static TypedEntryMap getParamMap(ArchivalUnit au) {
    TypedEntryMap paramMap = new TypedEntryMap();

    Configuration config = au.getConfiguration();
    Plugin plugin = au.getPlugin();
    for (ConfigParamDescr descr : plugin.getAuConfigDescrs()) {
      String key = descr.getKey();
      if (config.containsKey(key)) {
        try {
          Object val = descr.getValueOfType(config.get(key));
          paramMap.setMapElement(key, val);
        } catch (Exception ex) {
          log.error("Error configuring: " + key + " "  + ex.getMessage());
        }
      }
    }
    return paramMap;
  }
*/
  
  /**
   * Gets the book URL for an AU indicated by the DefinablePlugin 
   * and parameter definitions specified by the TdbAu.
   * 
   * @param plugin the DefinablePlugin
   * @param tdbau the TdbAu
   * @param year the year
   * @param volumeName the volume name
   * @param issue the issue
   * @return the issue URL
   */
/* for later use (pjg)  
  private static String getBooklUrl(
	  ArchivalUnit au, String volumeName, String year, String edition) {
	TypedEntryMap paramMap = getParamMap(au);
	Plugin plugin = au.getPlugin();
	String url = getBookUrl(plugin, paramMap, volumeName, year, edition);
    return url;
  }
*/

  /**
   * Gets the book URL for a TdbAU indicated by the DefinablePlugin 
   * and parameter definitions specified by the TdbAu.
   * @param tdbau the TdbAu
   * @param year the year
   * @param volumeName the volume name
   * @param edition the edition
   * @param chapter the chapter
   * @param spage the start page
   * @return the starting URL
   */
  private OpenUrlInfo getBookUrl(
	  TdbAu tdbau, String year, String volumeName, 
	  String edition, String chapter, String spage) {
    String pluginKey = PluginManager.pluginKeyFromId(tdbau.getPluginId());
    Plugin plugin = pluginMgr.getPlugin(pluginKey);

    OpenUrlInfo resolved = null;
    if (plugin != null) {
      log.debug3(  "getting issue url for plugin: " 
                 + plugin.getClass().getName());
      // get starting URL from a DefinablePlugin
      TypedEntryMap paramMap = getParamMap(plugin, tdbau);
      
      // add volume with type and spelling of existing element
      paramMap.setMapElement("volume", volumeName);
      paramMap.setMapElement("volume_str",volumeName);
      paramMap.setMapElement("volume_name", volumeName);
      paramMap.setMapElement("year", year);
      if (!StringUtil.isNullString(year)) {
        try {
          paramMap.setMapElement("au_short_year",
              String.format("%02d", NumberUtil.parseInt(year)%100));
        } catch (NumberFormatException ex) {
          log.info(  "Error parsing year '" + year 
                   + "' as an int -- not setting au_short_year");
        }
      }
      paramMap.setMapElement("edition", edition);
      paramMap.setMapElement("chapter", chapter);
      paramMap.setMapElement("page", spage);      
      // auFeatureKey selects feature from a map of values
      // for the same feature (e.g. au_feature_urls/au_year)
      paramMap.setMapElement("auFeatureKey", tdbau.getAttr(AU_FEATURE_KEY));
      String isbn = tdbau.getAttr("isbn");
      if (isbn != null) {
        paramMap.setMapElement("isbn", isbn);
      }
      String eisbn = tdbau.getAttr("eisbn");
      if (eisbn != null) {
        paramMap.setMapElement("eisbn", eisbn);
      }
      
      resolved = getBookUrl(plugin, paramMap);
      if (resolved.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
        resolved.resolvedBibliographicItem = tdbau;
        log.debug3("Resolved book url from plugin: " + resolved.resolvedUrl);
      }
    } else {
      log.debug3("No plugin found for key: " + pluginKey); 
    }
    return resolved;
  }
    

  /**
   * Gets the book URL for a DefinablePlugin and parameter definitions.
   * @param plugin the plugin
   * @param paramMap the param map
   * @return the issue URL
   */
  private OpenUrlInfo getBookUrl(Plugin plugin, TypedEntryMap paramMap) {
    OpenUrlInfo resolved = getPluginUrl(plugin, auBookAuFeatures, paramMap);
    return resolved;
  }

  /**
   * Gets the issue URL for an AU indicated by the DefinablePlugin 
   * and parameter definitions specified by the TdbAu.
   * 
   * @param plugin the DefinablePlugin
   * @param tdbau the TdbAu
   * @param year the year
   * @param volumeName the volume name
   * @param issue the issue
   * @return the issue URL
   */
/*  for later use (pjg)
  private static String getJournalUrl(
	  ArchivalUnit au, String year, String volumeName, String issue) {
	TypedEntryMap paramMap = getParamMap(au);
	Plugin plugin = au.getPlugin();
	String url = getJournalUrl(plugin, paramMap, year, volumeName, issue);
    return url;
  }
*/
  /**
   * Get starting url from TdbAu.
   * @param tdbau the TdbAu
   * @param year the year
   * @param volumeName the volume name
   * @param issue the issue
   * @param spage the start page
   * @param artnum the article number
   * @return the starting URL
   */
  private OpenUrlInfo getJournalUrl(
	  TdbAu tdbau, String year, String volumeName, String issue, 
	  String spage, String artnum) {
    String pluginKey = PluginManager.pluginKeyFromId(tdbau.getPluginId());
    Plugin plugin = pluginMgr.getPlugin(pluginKey);

    OpenUrlInfo resolved = noOpenUrlInfo;
    if (plugin != null) {
      log.debug3(  "getting issue url for plugin: " 
                 + plugin.getClass().getName());
      // get starting URL from a DefinablePlugin
      // add volume with type and spelling of existing element
  	  TypedEntryMap paramMap = getParamMap(plugin, tdbau);
  	  paramMap.setMapElement("volume", volumeName);
      paramMap.setMapElement("volume_str", volumeName);
      paramMap.setMapElement("volume_name", volumeName);
      paramMap.setMapElement("year", year);
      String issn = tdbau.getPrintIssn();
      if (issn != null) {
        paramMap.setMapElement("issn", issn);
      }
      String eissn = tdbau.getEissn();
      if (eissn != null) {
        paramMap.setMapElement("eissn", eissn);
      }
      if (!StringUtil.isNullString(year)) {
        try {
          paramMap.setMapElement("au_short_year",
              String.format("%02d", NumberUtil.parseInt(year)%100));
        } catch (NumberFormatException ex) {
          log.info("Error parsing year '" + year
                   + "' as an integer -- not setting au_short_year");
        }
      }
      paramMap.setMapElement("issue", issue);
      paramMap.setMapElement("article", spage);
      paramMap.setMapElement("page", spage);      
      paramMap.setMapElement("item", artnum);   // for journals without page numbers 
      // AU_FEATURE_KEY selects feature from a map of values
      // for the same feature (e.g. au_feature_urls/au_year)
      paramMap.setMapElement(AU_FEATURE_KEY, tdbau.getAttr(AU_FEATURE_KEY));
      resolved = getJournalUrl(plugin, paramMap);
      if (resolved.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
        if (resolved.resolvedTo == OpenUrlInfo.ResolvedTo.TITLE) {
          // create bibliographic item with only title properties
          resolved.resolvedBibliographicItem =  
            new BibliographicItemImpl()
              .setPublisherName(tdbau.getPublisherName())
              .setPublicationTitle(tdbau.getPublicationTitle())
              .setProprietaryIds(tdbau.getProprietaryIds())
              .setCoverageDepth(tdbau.getCoverageDepth())
              .setPrintIssn(tdbau.getPrintIssn())
              .setEissn(tdbau.getEissn())
              .setIssnL(tdbau.getIssnL());
        } else {
          resolved.resolvedBibliographicItem = tdbau;
        }
        log.debug3("Resolved journal url from plugin: " + resolved.resolvedUrl);
      }
    } else {
      log.debug3("No plugin found for key: " + pluginKey); 
    }
    return resolved;
  }
    
  /**
   * Get the issueURL for the plugin.
   * @param plugin the plugin
   * @param paramMap the param map
   * @return the issue URL
   */
  private OpenUrlInfo getJournalUrl(Plugin plugin, TypedEntryMap paramMap) { 
    OpenUrlInfo resolved = getPluginUrl(plugin, auJournalFeatures, paramMap);
    return resolved;
  }
  
  /**
   * Get the URL for the specified key from the plugin.
   * @param plugin the plugin
   * @param pluginKeys the plugin keys
   * @param paramMap the param map
   * @return the URL for the specified key
   */
  private OpenUrlInfo
  	getPluginUrl(Plugin plugin, FeatureEntry[] pluginEntries, TypedEntryMap paramMap) {
    ExternalizableMap map;

    // get printf pattern for pluginKey property
    try {
      Method method = 
        plugin.getClass().getMethod("getDefinitionMap", (new Class[0]));
      Object obj = method.invoke(plugin);
      if (!(obj instanceof ExternalizableMap)) {
       return noOpenUrlInfo;
      }
      map = (ExternalizableMap)obj;
    } catch (Exception ex) {
      log.error("getDefinitionMap", ex);
      return noOpenUrlInfo;
    }

    String proxySpec = null;
    try {
      proxySpec = paramMap.getString(ConfigParamDescr.CRAWL_PROXY.getKey());
    } catch (NoSuchElementException ex) {
      // no crawl_proxy param specified
    }
    
    for (FeatureEntry pluginEntry : pluginEntries) {
      // locate object value for plugin key path
      String pluginKey = pluginEntry.auFeatureKey;
      String[] pluginKeyPath = pluginKey.split("/");
      Object obj = map.getMapElement(pluginKeyPath[0]);
      for (int i = 1; (i < pluginKeyPath.length); i++) {
        if (obj instanceof Map) {
          obj = ((Map<String,?>)obj).get(pluginKeyPath[i]);
        } else {
          // all path elements except last one must be a map;
          obj = null;
          break;
        }
      }
      
      if (obj instanceof Map) {
        // match TDB AU_FEATURE_KEY value to key in map 
        String auFeatureKey = "*";  // default entry
        try {
          auFeatureKey = paramMap.getString(AU_FEATURE_KEY);
        } catch (NoSuchElementException ex) {}
        
        // entry may have multiple keys; '*' is the default entry
        Object val = null;
        for (Map.Entry<String,?> entry : ((Map<String,?>)obj).entrySet()) {
          String key = entry.getKey();
          if (   key.equals(auFeatureKey)
              || key.startsWith(auFeatureKey + ";")
              || key.endsWith(";" + auFeatureKey)
              || (key.indexOf(";" + auFeatureKey + ";") >= 0)) {
            val = entry.getValue();
            break;
          }
        }
        obj = val;
        pluginKey += "/" + auFeatureKey;
      }

      if (obj == null) {
        log.debug("unknown plugin key: " + pluginKey);
        continue;
      } 
      
      Collection<String> printfStrings = null;
      if (obj instanceof String) {
        // get single pattern for start url
        printfStrings = Collections.singleton((String)obj);
      } else if (obj instanceof Collection) {
        printfStrings = (Collection<String>)obj;
      } else {
        log.debug(  "unknown type for plugin key: " + pluginKey 
                  + ": " + obj.getClass().getName());
        continue;
      }
      
      log.debug3(  "Trying plugin key: " + pluginKey 
                 + " for plugin: " + plugin.getPluginId()
                 + " with " + printfStrings.size() + " printf strings");

      // set up converter for use with feature URL printf strings
      UrlListConverter converter = 
        PrintfConverter.newUrlListConverter(plugin, paramMap);
      converter.setAllowUntypedArgs(true);
      
      for (String s : printfStrings) {
        String url = null;
        s = StringEscapeUtils.unescapeHtml4(s);
        try {
          List<String> urls = converter.getUrlList(s);
          if ((urls != null) && !urls.isEmpty()) {
            // if multiple urls match, the first one will do
            url = urls.get(0);
          } 
        } catch (Throwable ex) {
          log.debug("invalid  conversion for " + s, ex);
          continue;
        }
            
        // validate URL: either it's cached, or it can be reached
        if (!StringUtil.isNullString(url)) {
          log.debug3("Resolving from url: " + url);
          url = resolveUrl(url, proxySpec);
          if (url != null) {
            return OpenUrlInfo.newInstance(url, proxySpec, 
                                           pluginEntry.resolvedTo);
          }
        }
      }
    }
      
    return noOpenUrlInfo;
  }
  
  /**
   * Return the book URL from TdbTitle and edition.
   * 
   * @param tdbAus a collection of TdbAus that match an ISBN
   * @param date the publication date
   * @param volume the volume
   * @param edition the edition
   * @param chapter the chapter
   * @param spage the start page
   * @return the book URL
   */
  private OpenUrlInfo resolveBookFromTdbAus(
	  Collection<TdbAu> tdbAus, String date, 
	  String volume, String edition, String chapter, String spage) {

    // get the year from the date
    String year = null;
    if (date != null) {
      try {
        year = Integer.toString(PublicationDate.parse(date).getYear());
      } catch (ParseException ex) {}
    }

    // list of AUs that match volume and year specified
    ArrayList<TdbAu> foundTdbAuList = new ArrayList<TdbAu>();
    
    // list of AUs that do not match volume, edition, and year specified
    ArrayList<TdbAu> notFoundTdbAuList = new ArrayList<TdbAu>();
    
    for (TdbAu tdbAu : tdbAus) {
      
      // if none of year, volume, or edition specified, pick any TdbAu
      if ((volume == null) && (year == null) && (edition == null)) {
        notFoundTdbAuList.add(tdbAu);
        continue;
      }
      
      // if volume specified, see if this TdbAu matches
      if (volume != null) {
        if (!tdbAu.includesVolume(volume)) {
          notFoundTdbAuList.add(tdbAu);
          continue;
        }
      }
      // if year specified, see if this TdbAu matches
      if (year != null) {
        if (!tdbAu.includesYear(year)) {
          notFoundTdbAuList.add(tdbAu);
          continue;
        }
      }
      
      // get the plugin id for the TdbAu that matches the specified edition
      if (edition != null) {
        String auEdition = tdbAu.getEdition();
        if ((auEdition != null) && !edition.equals(auEdition)) {
          notFoundTdbAuList.add(tdbAu);
          continue;
        }
      }
      foundTdbAuList.add(tdbAu);
    }
    
    OpenUrlInfo resolved = null;
    
    // look for URL that is cached from list of matching AUs
    for (TdbAu tdbau : foundTdbAuList) {
      String aYear = year;
      if (aYear == null) {
        aYear = tdbau.getStartYear();
      }
      String aVolume = volume;
      if (aVolume == null) {
        aVolume = tdbau.getStartVolume();
      }
      String anEdition = edition;
      if (edition == null) {
        anEdition  = tdbau.getEdition();
      }
      OpenUrlInfo aResolved = getBookUrl(
          tdbau, year, aVolume, anEdition, chapter, spage);
      if (aResolved.resolvedUrl != null) {
        // found the URL if in cache
        if  (pluginMgr.findCachedUrl(aResolved.resolvedUrl) != null) {
          if (resolved == null) {
            resolved = aResolved;
          } else {
            resolved.add(aResolved);
          }
        }
        // not a viable URL if the AU is down
        // note: even though getBookUrl() checks that page exists,
        // we can't rely on it being usable if TdbAu is down
        else if (!tdbau.isDown()) {
          if (resolved == null) {
            resolved = aResolved;
          } else {
            resolved.add(aResolved);
          }
        } else {
          log.debug2(  "discarding URL " + aResolved.resolvedUrl 
                     + " because tdbau is down: " + tdbau.getName());
        }
      }
    }
    if (resolved != null) {
      return resolved;
    }
      
    // use tdbau that is not down from notFoundTdbAuList to find the
    // title or publisher URL, since that is all we can return at this point
    for (TdbAu tdbau : notFoundTdbAuList) {
      if (!tdbau.isDown()) {
        OpenUrlInfo aResolved = getBookUrl(tdbau, 
            tdbau.getStartYear(), tdbau.getStartVolume(), 
            tdbau.getStartIssue(), null, null);
        if (aResolved.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
          if (resolved == null) {
            resolved = aResolved;
          } else {
            resolved.add(aResolved);
          }
        }
      } else {
        log.debug2("discarding URL because tdbau is down: " + tdbau.getName());
      }
    }
    if (resolved != null) {
      return resolved;
    }
    
    // pick any AU to use for resolving the title as a last resort
    if (!notFoundTdbAuList.isEmpty()) {
      OpenUrlInfo aResolved = 
          OpenUrlInfo.newInstance(null, null, OpenUrlInfo.ResolvedTo.VOLUME);
      aResolved.resolvedBibliographicItem = notFoundTdbAuList.get(0);
      return aResolved;
    }
    return noOpenUrlInfo;
  }
  
  /**
   * Return the article URL from an ISBN, edition, spage, and author.
   * The first author will only be used when the starting page is not given.
   * "Volume" is used to hold edition information in the database manager 
   * schema for books.  First author can be used in place of start page.
   * 
   * @param isbn the isbn
   * @param pub the publisher
   * @param date the date
   * @param volume the volume
   * @param edition the edition
   * @param chapter the chapter
   * @param spage the start page
   * @param author the first author
   * @param atitle the chapter title
   * @return the article URL
   */
  public OpenUrlInfo resolveFromIsbn(
    String isbn, String pub, String date, String volume, String edition, 
    String chapter, String spage, String author, String atitle) {
    // only go to database manager if requesting individual article/chapter
    try {
      // resolve from database manager
      DbManager dbMgr = daemon.getDbManager();
      OpenUrlInfo aResolved = resolveFromIsbn(
          dbMgr, isbn, pub, date, volume, edition, 
          chapter, spage, author, atitle);
      if (aResolved.resolvedTo != OpenUrlInfo.ResolvedTo.NONE) {
        return aResolved;
      }
    } catch (IllegalArgumentException ex) {
    }

    // resolve from TDB
    Tdb tdb = ConfigManager.getCurrentConfig().getTdb();
    // get list of TdbTitles for issn
    Collection<TdbAu> tdbAus;
    if (tdb == null) {
      tdbAus = Collections.<TdbAu>emptyList();
    } else if (pub != null) {
      TdbPublisher tdbPub = tdb.getTdbPublisher(pub);
      tdbAus = (tdbPub == null) 
          ? Collections.<TdbAu>emptyList() : tdbPub.getTdbAusByIsbn(isbn);
    } else {
        tdbAus = tdb.getTdbAusByIsbn(isbn);
    }
    OpenUrlInfo resolved = 
        resolveBookFromTdbAus(tdbAus, date, volume, edition, chapter, spage);
    return resolved;
  }

  /**
   * Return the article URL from an ISBN, edition, start page, author, and
   * article title using the metadata database.
   * <p>
   * The algorithm matches the ISBN and optionally the edition, and either 
   * the start page, author, or article title. The reason for matching on any
   * of the three is that typos in author and article title are always 
   * possible so we want to be more forgiving in matching an article.
   * <p>
   * If none of the three are specified, the URL for the book table of contents 
   * is returned.
   * 
   * @param dbMgr the database manager
   * @param isbn the isbn
   * @param pub the publisher
   * @param String date the date
   * @param String volumeName the volumeName
   * @param edition the edition
   * @param chapter the chapter
   * @param spage the start page
   * @param author the first author
   * @param atitle the chapter title
   * @return the url
   */
  private OpenUrlInfo resolveFromIsbn(
      DbManager dbMgr, String isbn, String pub,
      String date, String volume, String edition, 
      String chapter, String spage, String author, String atitle) {
    final String DEBUG_HEADER = "resolveFromIsbn(): ";
    OpenUrlInfo resolved = null;
    Connection conn = null;

    // error if input ISBN is not a ISBN-10 or ISBN-13
    String strippedIsbn10 = MetadataUtil.toUnpunctuatedIsbn10(isbn);
    String strippedIsbn13 = MetadataUtil.toUnpunctuatedIsbn13(isbn);
    if ((strippedIsbn10 == null) && (strippedIsbn13 == null)) {
      return noOpenUrlInfo;
    }

    boolean hasBookSpec = 
        (date != null) || (volume != null) || (edition != null); 
      
      boolean hasArticleSpec =    (chapter != null) || (spage != null) 
                               || (author != null) || (atitle != null);

    try {
      conn = dbMgr.getConnection();
      
      StringBuilder select = new StringBuilder("select distinct ");
      StringBuilder from = new StringBuilder(" from ");
      StringBuilder where = new StringBuilder(" where ");
      ArrayList<String> args = new ArrayList<String>();
          
      // return all related values for debugging purposes
      select.append("u." + URL_COLUMN);
      select.append(",pb." + PUBLISHER_NAME_COLUMN);
      select.append(",n1." + NAME_COLUMN + " book_title");
      select.append(",i." + ISBN_COLUMN);
      select.append(",bi." + VOLUME_COLUMN);
      select.append(",bi." + ISSUE_COLUMN + " edition");
      select.append(",bi." + START_PAGE_COLUMN);
      select.append(",bi." + END_PAGE_COLUMN);
      select.append(",bi." + ITEM_NO_COLUMN + " chapt_no");
      select.append(",n2." + NAME_COLUMN + " chapt_title");
      select.append(",pv2." + PROVIDER_NAME_COLUMN);
      
      from.append(MD_ITEM_TABLE + " mi1");              // publication md_item
      from.append("," + MD_ITEM_TABLE + " mi2");        // article md_item
      from.append("," + ISBN_TABLE + " i");
      from.append("," + PUBLICATION_TABLE + " pu");
      from.append("," + PUBLISHER_TABLE + " pb");
      from.append("," + MD_ITEM_NAME_TABLE + " n1");  // publication name
      from.append("," + MD_ITEM_NAME_TABLE + " n2");  // article name
      from.append("," + URL_TABLE + " u");
      from.append("," + BIB_ITEM_TABLE + " bi");
      from.append("," + PROVIDER_TABLE + " pv2");
      from.append("," + AU_MD_TABLE + " am2");

      where.append("mi2." + PARENT_SEQ_COLUMN + "=");
      where.append("mi1." + MD_ITEM_SEQ_COLUMN);
      
      where.append(" and i." + MD_ITEM_SEQ_COLUMN + "=");
      where.append("mi1." + MD_ITEM_SEQ_COLUMN);
      where.append(" and i." + ISBN_COLUMN);
      if ((strippedIsbn10 != null) && (strippedIsbn13 != null)) {
        // check both ISBN-10 and ISBN-13 forms
        where.append(" in (?,?)");
        args.add(strippedIsbn10);
        args.add(strippedIsbn13);
      } else {
        // can't convert to ISBN-10 or ISBN-13 because input isbn 
        // is not well formed, so use whichever one is available
        where.append(" = ?");
        args.add((strippedIsbn13 != null) ? strippedIsbn13 : strippedIsbn10);
      }

      where.append(" and pu." + MD_ITEM_SEQ_COLUMN + "=");
      where.append("mi1." + MD_ITEM_SEQ_COLUMN);
      where.append(" and pb." + PUBLISHER_SEQ_COLUMN + "=");
      where.append("pu." + PUBLISHER_SEQ_COLUMN);
      if (pub != null) {
        // match publisher if specified
        where.append(" and pb." + PUBLISHER_NAME_COLUMN + "= ?");
        args.add(pub);
      }
      where.append(" and n1." + MD_ITEM_SEQ_COLUMN + "=");
      where.append("mi1." + MD_ITEM_SEQ_COLUMN);
      where.append(" and n1." + NAME_TYPE_COLUMN + "='primary'");
      where.append(" and u." + MD_ITEM_SEQ_COLUMN + "=");
      where.append("mi2." + MD_ITEM_SEQ_COLUMN);
      where.append(" and u." + FEATURE_COLUMN + "='Access'");
      
      where.append(" and bi." + MD_ITEM_SEQ_COLUMN + "=");
      where.append("mi2." + MD_ITEM_SEQ_COLUMN);
      
      where.append(" and n2." + MD_ITEM_SEQ_COLUMN + "=");
      where.append("mi2." + MD_ITEM_SEQ_COLUMN);
      where.append(" and n2." + NAME_TYPE_COLUMN + "='primary'");
      
      where.append(" and mi2." + AU_MD_SEQ_COLUMN + "=");
      where.append("am2." + AU_MD_SEQ_COLUMN);
      where.append(" and am2." + PROVIDER_SEQ_COLUMN + "=");
      where.append("pv2." + PROVIDER_SEQ_COLUMN);

      if (hasBookSpec) {
        // can specify an issue by a combination of date, volume and edition;
        // how these combine varies, so do the most liberal match possible
        // and filter based on multiple results
        if (date != null) {
          // enables query "2009" to match "2009-05-10" in database
          where.append(" and mi2." + DATE_COLUMN);
          where.append(" like ? escape '\\'");
          args.add(date.replace("\\","\\\\").replace("%","\\%") + "%");
        }
        
        if (volume != null) {
          where.append(" and bi." + VOLUME_COLUMN + " = ?");
          args.add(volume);
        }

        if (edition != null) {
          where.append(" and bi." + ISSUE_COLUMN + " = ?");
          args.add(edition);
        }
      }

      // handle start page, author, and article title as
      // equivalent ways to specify an article within an issue
      if (hasArticleSpec) {
        // accept any of the three
        where.append(" and ( ");
          
        if (spage != null) {
          where.append("bi." + START_PAGE_COLUMN + " = ?");
          args.add(spage);
        }

        if (chapter != null) {
          if (spage != null) {
            where.append(" or ");
          }

          where.append("bi." + ITEM_NO_COLUMN + " = ?");
          args.add(chapter);
        }

        if (atitle != null) {
          if ((spage != null) || (chapter != null)) {
            where.append(" or ");
          }

          where.append("upper(n2." + NAME_COLUMN);
          where.append(") like ? escape '\\'");

          args.add(atitle.toUpperCase().replace("%","\\%") + "%");
        }

        if (author != null) {
          if ((spage != null) || (chapter != null) || (atitle != null)) {
            where.append(" or ");
          }

          from.append("," + AUTHOR_TABLE + " au");

          // add the author query to the query
          addAuthorQuery(author, where, args);
        }
        where.append(")");
      }

      String qstr = select.toString() + from.toString() + where.toString();
      int maxPublishersPerArticle = getMaxPublishersPerArticle();
      String[][] results = new String[maxPublishersPerArticle+1][11];
      int count = resolveFromQuery(conn, qstr, args, results);
      log.debug3(DEBUG_HEADER + "count = " + count);
      if (count <= maxPublishersPerArticle) {
        // ensure at most one result per publisher+provider in case
        // more than one publisher+provider publishes the same book
        Set<String> pubs = new HashSet<String>();
        for (int i = 0; i < count; i++) {
          // combine publisher and provider columns to determine uniqueness
          if (!pubs.add(results[i][1] + results[i][10])) {
            return noOpenUrlInfo;
          }
          OpenUrlInfo info = OpenUrlInfo.newInstance(results[i][0], null, 
                                                OpenUrlInfo.ResolvedTo.CHAPTER);
          if (resolved == null) {
            resolved = info;
          } else {
            resolved.add(info);
          }
        }
      }
    } catch (DbException dbe) {
      log.error("Getting ISBN:" + isbn, dbe);
        
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
    return (resolved == null) ? noOpenUrlInfo : resolved;
  }  

  /**
   * Add author query to the query buffer and argument list.  
   * @param author the author
   * @param where the query buffer
   * @param args the argument list
   */
  private void addAuthorQuery(String author, StringBuilder where,
      List<String> args) {
    where.append("mi2." + MD_ITEM_SEQ_COLUMN + " = ");
    where.append("au." + MD_ITEM_SEQ_COLUMN + " and (");

    String authorUC = author.toUpperCase();
    // match single author
    where.append("upper(au.");
    where.append(AUTHOR_NAME_COLUMN);
    where.append(") = ?");
    args.add(authorUC);

    // escape escape character and then wildcard characters
    String authorEsc = authorUC.replace("\\", "\\\\").replace("%","\\%");
            
    // match last name of author 
    // (last, first name separated by ',')
    where.append(" or upper(au.");
    where.append(AUTHOR_NAME_COLUMN);
    where.append(") like ? escape '\\'");
    args.add(authorEsc+",%");
    
    // match last name of author
    // (first last name separated by ' ')
    where.append(" or upper(au.");
    where.append(AUTHOR_NAME_COLUMN);
    where.append(") like ? escape '\\'");
    args.add("% " + authorEsc);    
    
    where.append(")");
  }
}
