/*
 * $Id: AbsintheArchivalUnit.java,v 1.1 2003-09-27 00:35:31 eaalto Exp $
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
package org.lockss.plugin.absinthe;

import java.net.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import gnu.regexp.REException;

/**
 * <p>AbsintheArchivalUnit: The Archival Unit Class for Absinthe Literary
 * Review</p>
 * @author Emil Aalto
 * @version 1.0
 */

public class AbsintheArchivalUnit extends BaseArchivalUnit {
  /**
   * Configuration parameter for new content crawl interval
   */
  static final String AUPARAM_NEW_CONTENT_CRAWL = "nc_interval";
  private static final long DEFAULT_NEW_CONTENT_CRAWL = 2 * Constants.WEEK;

  /**
   * Configuration parameter for pause time between fetchs.
   */
  public static final String AUPARAM_PAUSE_TIME = "pause_time";
  private static final long DEFAULT_PAUSE_TIME = 10 * Constants.SECOND;

  private static final String EXPECTED_URL_PATH = "/";

  protected Logger logger = Logger.getLogger("AbsinthePlugin");

  private URL baseUrl; // the base Url for the volume
  private String year; // the year
  private long pauseTime; // the time to pause between fetchs
  private long newContentCrawlIntv; // the new content crawl interval

  protected AbsintheArchivalUnit(Plugin myPlugin) {
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
    if (year.length()==2) {
      // convert to '03
      name.append("'");
    }
    name.append(year);
    return name.toString();
  }

  public List getNewContentCrawlUrls() {
    return ListUtil.list(makeStartUrl(baseUrl, year));
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

    // get the base url string
    String urlStr = config.get(AbsinthePlugin.AUPARAM_BASE_URL);
    if (urlStr == null) {
      exception = "No configuration value for " +
          AbsinthePlugin.AUPARAM_BASE_URL;
      throw new ConfigurationException(exception);
    }

    // get the volume string
    year = config.get(AbsinthePlugin.AUPARAM_YEAR);
    if (year == null) {
      exception = "No configuration value for " + AbsinthePlugin.AUPARAM_YEAR;
      throw new ConfigurationException(exception);
    }

    // turn them into appropriate types
    try {
      baseUrl = new URL(urlStr);
    } catch (MalformedURLException murle) {
      throw new ConfigurationException("Bad base URL", murle);
    } catch (NumberFormatException e) {
      throw new
        ArchivalUnit.ConfigurationException("Bad volume number", e);
    }

    if (baseUrl == null) {
      throw new ConfigurationException("Null base url");
    }

    if ((year.length()!=4) && (year.length()!=2)) {
      throw new ConfigurationException("Invalid year: "+year);
    }

    if (!EXPECTED_URL_PATH.equals(baseUrl.getPath())) {
      logger.error("Illegal path: "+baseUrl.getPath() + ", expected: " +
                   EXPECTED_URL_PATH);
      throw new ConfigurationException("Url has illegal path: " +
                                       baseUrl.getPath() + ", expected: " +
                                       EXPECTED_URL_PATH);
    }

    // make our crawl spec
    try {
      crawlSpec = makeCrawlSpec(baseUrl, year);
    } catch (REException e) {
      throw new ConfigurationException("Illegal RE", e);
    }

    // get the pause time
    pauseTime = config.getTimeInterval(AUPARAM_PAUSE_TIME, DEFAULT_PAUSE_TIME);
    logger.debug2("Set pause value to " + pauseTime);

    // get the new content crawl interval
    newContentCrawlIntv = config.getTimeInterval(AUPARAM_NEW_CONTENT_CRAWL,
                                                 DEFAULT_NEW_CONTENT_CRAWL);
    logger.debug2("Set new content crawl interval to " + newContentCrawlIntv);
  }

  private CrawlSpec makeCrawlSpec(URL base, String year) throws REException {

    CrawlRule rule = makeRules(base, year);
    return new CrawlSpec(makeStartUrl(base, year), rule);
  }

  public String getManifestPage() {
    return makeStartUrl(baseUrl, year);
  }

  String makeStartUrl(URL base, String year) {
    String ret;
    StringBuffer sb = new StringBuffer();
    sb.append(base.toString());
    sb.append("archives");
    if (year.length()==4) {
      sb.append(year.substring(year.length()-2));
    } else if (year.length()==2) {
      sb.append(year);
    }
    sb.append(".htm");
    ret = sb.toString();
    logger.debug("starting url is " + ret);
    return ret;
  }

  private CrawlRule makeRules(URL urlRoot, String year)
      throws REException {
    List rules = new LinkedList();
    final int incl = CrawlRules.RE.MATCH_INCLUDE;
    final int excl = CrawlRules.RE.MATCH_EXCLUDE;
    String baseUrl = urlRoot.toString();
    rules.add(new CrawlRules.RE("^" + baseUrl, CrawlRules.RE.NO_MATCH_EXCLUDE));
    rules.add(new CrawlRules.RE(makeStartUrl(urlRoot, year), incl));
    rules.add(new CrawlRules.RE(baseUrl + "stories/.*", incl));
    rules.add(new CrawlRules.RE(baseUrl + "poetics/.*", incl));
    rules.add(new CrawlRules.RE(baseUrl + "archives/.*", incl));
    // exclude book review index page
    rules.add(new CrawlRules.RE(baseUrl + "book_reviews/book_reviews.htm", excl));
    // include other pages
    rules.add(new CrawlRules.RE(baseUrl + "book_reviews/.*", incl));
    rules.add(new CrawlRules.RE(baseUrl + "images/.*", incl));
    return new CrawlRules.FirstMatch(rules);
  }

  public boolean shouldCrawlForNewContent(AuState aus) {
    long timeDiff = TimeBase.msSince(aus.getLastCrawlTime());
    logger.debug("Deciding whether to do new content crawl for "+aus);
    if (aus.getLastCrawlTime() == 0 ||
        timeDiff > (newContentCrawlIntv)) {
      return true;
    }
    return false;
  }
}