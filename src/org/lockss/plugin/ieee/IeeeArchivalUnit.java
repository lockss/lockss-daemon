package org.lockss.plugin.ieee;

import org.lockss.plugin.base.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.util.*;

import java.util.*;
import java.net.*;

import gnu.regexp.*;
import org.lockss.state.*;

/**
 * <p>IeeeArchivalUnit: The Archival Unit Class for IEEE Plugin</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class IeeeArchivalUnit extends BaseArchivalUnit {
  /**
   * Configuration parameter for new content crawl interval
   */
  static final String AUPARAM_NEW_CONTENT_CRAWL = "nc_interval";
  private static final long DEFAULT_NEW_CONTENT_CRAWL= 14 * Constants.DAY;

  /**
   * Configuration parameter for pause time between fetchs.
   */
  public static final String AUPARAM_PAUSE_TIME = "pause_time";
  private static final long DEFAULT_PAUSE_TIME = 10 * Constants.SECOND;


  private static final String EXPECTED_URL_PATH = "/";

  protected Logger logger = Logger.getLogger("IeeeArchivalUnit");

  private URL baseUrl;              // the base Url for the volume
  private int puNumber;             // the publication number
  private int puYear;               // the publication year
  private long pauseTime;           // the time to pause between fetchs
  private long newContentCrawlIntv; // the new content crawl interval

  protected IeeeArchivalUnit(Plugin myPlugin) {
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
    name.append(", puNumber ");
    name.append(puNumber);
    name.append(", ");
    name.append(puYear);
    return name.toString();
  }


  public List getNewContentCrawlUrls() {
    return ListUtil.list(makeStartUrl(baseUrl, puNumber, puYear));
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
    String urlStr = config.get(IeeePlugin.AUPARAM_BASE_URL);
    if (urlStr == null) {
      exception = "No configuration value for " + IeeePlugin.AUPARAM_BASE_URL;
      throw new ConfigurationException(exception);
    }

    // get the publication number
    String pubStr = config.get(IeeePlugin.AUPARAM_PUNUM);
    if(pubStr == null) {
      exception = "No configuration value for " + IeeePlugin.AUPARAM_PUNUM;
      throw new ConfigurationException(exception);
    }

    // get the volume year
    String yearStr = config.get(IeeePlugin.AUPARAM_YEAR);
    if(yearStr == null) {
      exception = "No Configuration value for " + IeeePlugin.AUPARAM_YEAR;
      throw new ConfigurationException(exception);
    }

    // turn them into appropriate types
    try {
      baseUrl = new URL(urlStr);
      puNumber = Integer.parseInt(pubStr);
      puYear = Integer.parseInt(yearStr);

    } catch (MalformedURLException murle) {
      exception = IeeePlugin.AUPARAM_BASE_URL+ " set to a bad url "+ urlStr;
      throw new ConfigurationException(exception, murle);
    }

    if (baseUrl == null) {
      throw new ConfigurationException("Null base url");
    }
    if (puYear < 2003) {
      throw new ConfigurationException("Volume Year - Out of Range.");
    }
    if (puNumber < 0) {
      throw new ConfigurationException("Publication Number - Out of Range.");
    }
    if (!EXPECTED_URL_PATH.equals(baseUrl.getPath())) {
      throw new ConfigurationException("Url has illegal path: " + baseUrl.getPath());
    }

    // make our crawl spec
    try {
      crawlSpec = makeCrawlSpec(baseUrl, puNumber, puYear);
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

  private CrawlSpec makeCrawlSpec(URL base, int pub, int year)
      throws REException {

    CrawlRule rule = makeRules(base, pub, year);
    return new CrawlSpec(makeStartUrl(base, pub, year), rule);
  }

  public String getManifestPage() {
    return makeStartUrl(baseUrl, puNumber, puYear);
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

  String makeStartUrl(URL base, int pub, int year) {
    String ret;
    StringBuffer sb = new StringBuffer();
    sb.append(base.toString());
    sb.append("xpl/RecentIssue.jsp?puNumber=");
    sb.append(pub);
    sb.append("&year=");
    sb.append(year);
    ret = sb.toString();
    logger.debug("starting url is "+ ret);
    return ret;
  }

  private CrawlRule makeRules(URL urlRoot, int pub, int year)
      throws REException {
    List rules = new LinkedList();
    final int incl = CrawlRules.RE.MATCH_INCLUDE;
    final int excl = CrawlRules.RE.MATCH_EXCLUDE;
    String base = urlRoot.toString();
    if(base.endsWith("/"))
      base = base.substring(0, base.length()-1);

    rules.add(new CrawlRules.RE("^" + base, CrawlRules.RE.NO_MATCH_EXCLUDE));

    // include the stuff that determines how the page looks
    rules.add(new CrawlRules.RE(".*.gif", incl)); // gifs
    rules.add(new CrawlRules.RE(".*.jpg", incl)); // jpgs

    // include the toc for the archival year
    rules.add(new CrawlRules.RE(base +"/xpl/RecentIssue.jsp.*puNumber="
                                + pub+".*year=" +year, incl));

    // issue page
    rules.add(new CrawlRules.RE(base + "/xpl/tocresult.jsp.*isNumber=.*", incl));
    // printable issue page
    rules.add(new CrawlRules.RE(base + "/xpl/tocprint.jsp.*isNumber=.*", incl));

    // article -pdf
    rules.add(new CrawlRules.RE(base + "/iel5/" + pub + "/.*.pdf", incl));

   // abstracts
    rules.add(new CrawlRules.RE(base + ":80/xpls/abs_all.jsp.*", incl));

    logger.debug("Rules: " + rules);
    return new CrawlRules.FirstMatch(rules);
  }
}
