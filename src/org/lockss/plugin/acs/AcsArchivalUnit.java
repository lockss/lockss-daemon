/*
 * $Id: AcsArchivalUnit.java,v 1.7 2003-10-10 19:21:45 eaalto Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.acs;

import org.lockss.plugin.base.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.util.*;

import gnu.regexp.*;

import java.net.*;
import java.util.*;
import org.lockss.state.*;

/**
 * <p>AcsArchivalUnit: The Archival Unit Class for American Chemical Society
 * Plugin</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class AcsArchivalUnit extends BaseArchivalUnit {


  /**
   * Configuration parameter for new content crawl interval
   */
  static final String AUPARAM_NEW_CONTENT_CRAWL = "nc_interval";
  private static final long DEFAULT_NEW_CONTENT_CRAWL= 2 * Constants.WEEK;

  /**
   * Configuration parameter for pause time between fetchs.
   */
  public static final String AUPARAM_PAUSE_TIME = "pause_time";
  private static final long DEFAULT_PAUSE_TIME = 10 * Constants.SECOND;


  private static final String EXPECTED_URL_PATH = "/";

  protected Logger logger = Logger.getLogger("AcsArchivalUnit");

  private URL baseUrl;              // the base rrl for the volume
  private URL articleUrl;           // the base url for the articles
  private int volume;               // the volume index
  private String journalKey;        // the key used to specify the journal
  private int year;              // the year of the volume
  private long pauseTime;           // the time to pause between fetchs
  private long newContentCrawlIntv; // the new content crawl interval

  String startUrlString;    // the starting url string;


  protected AcsArchivalUnit(Plugin myPlugin) {
    super(myPlugin);
  }

  public Collection getUrlStems() {
    try {
      URL stem = new URL(baseUrl.getProtocol(), baseUrl.getHost(),
                         baseUrl.getPort(), "");
      return ListUtil.list(stem.toString());
    } catch (MalformedURLException e) {
      return Collections.EMPTY_LIST;
    }
  }

  public String getName() {
    StringBuffer name = new StringBuffer(baseUrl.getHost());
    name.append(", ");
    name.append(journalKey);
    name.append(", vol. ");
    name.append(volume);
    return name.toString();
  }


  public List getNewContentCrawlUrls() {
    return ListUtil.list(makeStartUrl(baseUrl, journalKey, volume, year));
  }

  public long getFetchDelay() {
    // make sure that pause time is never less than default
    return Math.max(pauseTime, DEFAULT_PAUSE_TIME);
  }

  public void setConfiguration(Configuration config)
      throws ArchivalUnit.ConfigurationException {
    super.setConfiguration(config);
    String exception;

    if (config == null) {
      throw new ConfigurationException("Null configInfo");
    }

    // get the volume root url string
    String urlStr = config.get(AcsPlugin.AUPARAM_BASE_URL);
    if (urlStr == null) {
      exception = "No configuration value for " + AcsPlugin.AUPARAM_BASE_URL;
      throw new ConfigurationException(exception);
    }

    // get the article root url string
    String issueStr = config.get(AcsPlugin.AUPARAM_ARTICLE_URL);
    if (issueStr == null) {
      exception = "No configuration value for " + AcsPlugin.AUPARAM_ARTICLE_URL;
      throw new ConfigurationException(exception);
    }

    // get the journal key
    journalKey = config.get(AcsPlugin.AUPARAM_JOURNAL_KEY);
    if ((journalKey == null) || (journalKey.equals(""))) {
      exception = "No configuration value for " +
          AcsPlugin.AUPARAM_JOURNAL_KEY;
      throw new ConfigurationException(exception);
    }

    // get the volume string
    String volStr = config.get(AcsPlugin.AUPARAM_VOL);
    if (volStr == null) {
      exception = "No Configuration value for " + AcsPlugin.AUPARAM_VOL;
      throw new ConfigurationException(exception);
    }
    // get the volume year
    String yearStr = config.get(AcsPlugin.AUPARAM_YEAR);
    if(yearStr == null) {
      exception = "No Configuration value for " + AcsPlugin.AUPARAM_YEAR;
      throw new ConfigurationException(exception);
    }

    // turn them into appropriate types
    try {
      baseUrl = new URL(urlStr);
      articleUrl = new URL(issueStr);
      volume = Integer.parseInt(volStr);
      year = Integer.parseInt(yearStr);

    } catch (MalformedURLException murle) {
      exception = AcsPlugin.AUPARAM_BASE_URL+ " set to a bad url "+ urlStr;
      throw new ConfigurationException(exception, murle);
    }

    // validity  checks
    if (baseUrl == null) {
      throw new ConfigurationException("Null base url");
    }
    if (articleUrl == null) {
      throw new ConfigurationException("Null article url");
    }
    if (volume < 0) {
      throw new ConfigurationException("Negative volume");
    }
    if (year < 2003) {
      throw new ConfigurationException("Year out of range - must be after 2003");
    }
    if (!EXPECTED_URL_PATH.equals(baseUrl.getPath())) {
      throw new ConfigurationException("Url has illegal path: " +
                                       baseUrl.getPath());
    }
    if (!EXPECTED_URL_PATH.equals(articleUrl.getPath())) {
      throw new ConfigurationException("Url has illegal path: " +
                                       articleUrl.getPath());
    }
    // calculate the starting url string
    startUrlString = makeStartUrl(baseUrl, journalKey, volume, year);

    // make our crawl spec
    try {
      crawlSpec = makeCrawlSpec(baseUrl, journalKey, volume);
    } catch (REException e) {
      throw new ConfigurationException("Illegal RE", e);
    }

    // get the pause time
    pauseTime = config.getTimeInterval(AUPARAM_PAUSE_TIME, DEFAULT_PAUSE_TIME);
    logger.debug3("Set pause value to "+pauseTime);


    // get the new content crawl interval
    newContentCrawlIntv = config.getTimeInterval(AUPARAM_NEW_CONTENT_CRAWL,
                                             DEFAULT_NEW_CONTENT_CRAWL);
    logger.debug3("Set new content crawl interval to "+ newContentCrawlIntv);

  }

  public String getManifestPage() {
    return startUrlString;
  }

  public boolean shouldCrawlForNewContent(AuState aus) {
    long timeDiff = TimeBase.msSince(aus.getLastCrawlTime());
    logger.debug("Deciding whether to do new content crawl for "+aus);
    if (aus.getLastCrawlTime() == 0 || timeDiff > (newContentCrawlIntv)) {
      return true;
    }
    return false;
  }

  /**
   * @param mimeType the mime type
   * @return null since we're not currently filtering Muse content
   */
  public FilterRule getFilterRule(String mimeType) {
    return null;
  }


  String makeStartUrl(URL base, String jkey, int volume, int vol_year) {
    String ret;
    StringBuffer sb = new StringBuffer();
    sb.append(base.toString());
    sb.append("acs/journals/toc.njs_select_issue?in_coden=");
    sb.append(jkey);
    sb.append("&in_volume=");
    sb.append(volume);
    sb.append("&in_decade=");
    sb.append(vol_year - (vol_year%10));
    ret = sb.toString();
    logger.debug("starting url is "+ ret);
    return ret;
  }

  private CrawlSpec makeCrawlSpec(URL base, String jkey, int volume)
      throws REException {

    CrawlRule rule = makeRules(base, jkey, volume);
    return new CrawlSpec(startUrlString, rule, 2);
  }

  private CrawlRule makeRules(URL urlRoot, String jkey, int volume)
      throws REException {
    List rules = new LinkedList();
    final int incl = CrawlRules.RE.MATCH_INCLUDE;
    final int excl = CrawlRules.RE.MATCH_EXCLUDE;
    String artRoot = articleUrl.toString();
    String idxRoot = urlRoot.toString();

    // include our journal volume table of contents page

    rules.add(new CrawlRules.RE(idxRoot+"acs/journals/toc.njs_select.*in_coden="
                                +jkey +".*in_volume=" + volume + ".*", incl));

    // include our volume's issue table of contents pages
    rules.add(new CrawlRules.RE(idxRoot +"acs/journals/toc.page.*incoden=" + jkey
                                + ".*involume=" + volume, incl));

    // exclude everything else not part of the article root
    rules.add(new CrawlRules.RE("^" + artRoot, CrawlRules.RE.NO_MATCH_EXCLUDE));

    // include the stuff that determines how the page looks
    rules.add(new CrawlRules.RE(artRoot + ".*.gif", incl)); // gifs
    rules.add(new CrawlRules.RE(artRoot + ".*.jpg", incl)); // jpgs
    rules.add(new CrawlRules.RE(artRoot + ".*.css", incl)); // style sheets

    // include the articles for this journal and volume
    rules.add(new CrawlRules.RE(artRoot + ".*incoden=" + jkey
                                + ".*involume=" + volume, incl));
    rules.add(new CrawlRules.RE(artRoot + "cgi-bin/article.cgi.*" +jkey
                                + "/" + year +"/" + volume + "/.*", incl));
    // to include the abstracts uncomment this line
    /*rules.add(new CrawlRules.RE(artRoot +"cgi-bin/abstract.cgi.*"+jkey
                                + "/" + year +"/" + volume + "/.*", incl));
     */
    // to include the supporting info uncomment this line
    rules.add(new CrawlRules.RE(".*supporting_information.page.*", incl));

    logger.debug("Rules: " + rules);
    return new CrawlRules.FirstMatch(rules);
  }

}
