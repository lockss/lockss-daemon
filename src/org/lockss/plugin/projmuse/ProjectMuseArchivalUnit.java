/*
 * $Id: ProjectMuseArchivalUnit.java,v 1.4 2003-08-27 19:27:32 eaalto Exp $
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

package org.lockss.plugin.projmuse;

import java.net.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.state.AuState;
import org.lockss.plugin.base.BaseArchivalUnit;
import gnu.regexp.REException;

/**
 * This is a first cut at making a Project Muse plugin
 *
 * @author  Emil Aalto
 * @version 0.0
 */

public class ProjectMuseArchivalUnit extends BaseArchivalUnit {
  /**
   * Configuration parameter for new content crawl interval
   */
  static final String AUPARAM_NEW_CONTENT_CRAWL = "nc_interval";
  private static final long DEFAULT_NEW_CONTENT_CRAWL = 2 * Constants.WEEK;

  /**
   * Configuration parameter for pause time between fetchs.
   */
  public static final String AUPARAM_PAUSE_TIME = "pause_time";
  private static final long DEFAULT_PAUSE_TIME = 6 * Constants.SECOND;

  private static final String EXPECTED_URL_PATH = "/";

  protected Logger logger = Logger.getLogger("ProjectMusePlugin");

  private URL baseUrl; // the base Url for the volume
  private int volume; // the volume index
  private String journalDir;
  private long pauseTime; // the time to pause between fetchs
  private long newContentCrawlIntv; // the new content crawl interval

  protected ProjectMuseArchivalUnit(Plugin myPlugin) {
    super(myPlugin);
  }

  public CachedUrl cachedUrlFactory(CachedUrlSet owner, String url) {
    return new GenericFileCachedUrl(owner, url);
  }

  public CachedUrlSet cachedUrlSetFactory(ArchivalUnit owner,
                                          CachedUrlSetSpec cuss) {
    return new GenericFileCachedUrlSet(owner, cuss);
  }

  public UrlCacher urlCacherFactory(CachedUrlSet owner, String url) {
    return new GenericFileUrlCacher(owner, url);
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
    name.append(journalDir);
    name.append(", vol. ");
    name.append(volume);
    return name.toString();
  }

  public List getNewContentCrawlUrls() {
    return ListUtil.list(makeStartUrl(baseUrl, journalDir, volume));
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
    String urlStr = config.get(ProjectMusePlugin.AUPARAM_BASE_URL);
    if (urlStr == null) {
      exception = "No configuration value for " +
          ProjectMusePlugin.AUPARAM_BASE_URL;
      throw new ConfigurationException(exception);
    }

    // get the volume string
    String volStr = config.get(ProjectMusePlugin.AUPARAM_VOL);
    if (volStr == null) {
      exception = "No configuration value for " + ProjectMusePlugin.AUPARAM_VOL;
      throw new ConfigurationException(exception);
    }

    // get the journal directory
    journalDir = config.get(ProjectMusePlugin.AUPARAM_JOURNAL_DIR);
    if ((journalDir == null) || (journalDir.equals(""))) {
      exception = "No configuration value for " +
          ProjectMusePlugin.AUPARAM_JOURNAL_DIR;
      throw new ConfigurationException(exception);
    }

    // turn them into appropriate types
    try {
      baseUrl = new URL(urlStr);
      volume = Integer.parseInt(volStr);

    } catch (MalformedURLException murle) {
      exception = ProjectMusePlugin.AUPARAM_BASE_URL +
          " set to a bad url " + urlStr;
      throw new ConfigurationException(exception, murle);
    }

    if (baseUrl == null) {
      throw new ConfigurationException("Null base url");
    }
    if (volume < 0) {
      throw new ConfigurationException("Negative volume");
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
      crawlSpec = makeCrawlSpec(baseUrl, volume);
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

  private CrawlSpec makeCrawlSpec(URL base, int volume) throws REException {

    CrawlRule rule = makeRules(base, journalDir, volume);
    return new CrawlSpec(makeStartUrl(base, journalDir, volume), rule);
  }

  //Todo: return the correct starting url here
  String makeStartUrl(URL base, String journal, int volume) {
    String ret;
    StringBuffer sb = new StringBuffer();
    sb.append(base.toString());
    // always 3 digit?
    sb.append("journals/"+journal+"/v");
    if (volume < 100) {
      // make 'v66' into 'v066' and 'v1' into 'v001'
      if (volume < 10) {
        sb.append("00");
      } else {
        sb.append("0");
      }
    }
    sb.append(volume);
    sb.append("/");
    ret = sb.toString();
    logger.debug("starting url is " + ret);
    return ret;
  }

  // Todo: add the crawl rules appropriate for Project Muse
  private CrawlRule makeRules(URL urlRoot, String journal, int volume)
      throws REException {
    List rules = new LinkedList();
    final int incl = CrawlRules.RE.MATCH_INCLUDE;
    final int excl = CrawlRules.RE.MATCH_EXCLUDE;
    rules.add(new CrawlRules.RE("^" + urlRoot.toString(), CrawlRules.RE.NO_MATCH_EXCLUDE));
    String volStr = urlRoot.toString() + "journals/"+journal+"/v";
    // pad out the 'vXXX' to 3 digits
    if (volume < 100) {
      if (volume < 10) {
        volStr += "00";
      } else {
        volStr += "0";
      }
    }
    rules.add(new CrawlRules.RE(volStr + volume + "/.*", incl));
    rules.add(new CrawlRules.RE(urlRoot.toString() +
                                "journals/"+journal+"/toc/[a-zA-Z]*" + volume +
                                "\\..*", incl));
    rules.add(new CrawlRules.RE(urlRoot.toString() + "images/.*", incl));
    //rules.add(new CrawlRules.RE(".*ck=nck.*", excl));
    //rules.add(new CrawlRules.RE(".*ck=nck.*", excl));
    //rules.add(new CrawlRules.RE(".*adclick.*", excl));
    //rules.add(new CrawlRules.RE(".*/cgi/mailafriend.*", excl));
    //rules.add(new CrawlRules.RE(".*/content/current/.*", incl));
    //rules.add(new CrawlRules.RE(".*/content/vol"+volume+"/.*", incl));
    //rules.add(new CrawlRules.RE(".*/cgi/content/.*/"+volume+"/.*", incl));
    //rules.add(new CrawlRules.RE(".*/cgi/reprint/"+volume+"/.*", incl));

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
