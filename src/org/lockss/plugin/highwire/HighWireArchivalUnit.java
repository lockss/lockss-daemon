/*
 * $Id: HighWireArchivalUnit.java,v 1.5 2003-02-10 23:46:51 troberts Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.net.*;
import java.util.*;
import gnu.regexp.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.state.*;

/**
 * This is a first cut at making a HighWire plugin
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class HighWireArchivalUnit extends BaseArchivalUnit {
  public static final String LOG_NAME = "HighWireArchivalUnit";
  /**
   * Configuration parameter for pause time in Highwire crawling.
   */
  public static final String PARAM_HIGHWIRE_PAUSE_TIME =
      Configuration.PREFIX + "highwire.pause.time";
  private static final int DEFAULT_PAUSE_TIME = 10000;

  private static final String EXPECTED_URL_PATH = "/";

  protected Logger logger = Logger.getLogger(LOG_NAME);
  private int pauseMS;

  private int volume;
  private URL base;

  public static final String PARAM_HIGHWIRE_NC_INTERVAL =
      Configuration.PREFIX + "highwire.nc_interval";
  private static final int DEFAULT_NC_INTERVAL = 14;

  private static final long DAY_MS = 1000 * 60 * 60 * 24;

  private int ncCrawlInterval;



  /**
   * Standard constructor for HighWirePlugin.
   *
   * @param base URL to start crawl
   * @param volume volume number
   * @throws REException
   */
  public HighWireArchivalUnit(URL base, int volume)
      throws REException {
    super();
    if (base == null) {
      throw new IllegalArgumentException("Called with null base url");
    } if (volume < 0) {
      throw new IllegalArgumentException("Called with negative volume");
    } if (!EXPECTED_URL_PATH.equals(base.getPath())) {
      throw new IllegalArgumentException("Called with url that had path: "+
					 base.getPath());
    }

    this.volume = volume;
    this.base = base;
    this.crawlSpec = makeCrawlSpec(base, volume);
    loadProps();
  }

  public CachedUrlSet cachedUrlSetFactory(ArchivalUnit owner,
      CachedUrlSetSpec cuss) {
    return new GenericFileCachedUrlSet(owner, cuss);
  }

  public CachedUrl cachedUrlFactory(CachedUrlSet owner, String url) {
    return new GenericFileCachedUrl(owner, url);
  }

  public UrlCacher urlCacherFactory(CachedUrlSet owner, String url) {
    return new HighWireUrlCacher(owner, url);
  }

  private CrawlSpec makeCrawlSpec(URL base, int volume)
      throws REException {

    CrawlRule rule = makeRules(base, volume);
    return new CrawlSpec(makeStartUrl(base, volume), rule);
  }

  private String makeStartUrl(URL base, int volume) {
    StringBuffer sb = new StringBuffer();
    sb.append(base.toString());
    sb.append("lockss-volumme");
    sb.append(volume);
    sb.append(".shtml");
    logger.debug("makeStartUrl returning: "+sb.toString());
    return sb.toString();
  }

  private CrawlRule makeRules(URL urlRoot, int volume)
      throws REException {
    List rules = new LinkedList();
    final int incl = CrawlRules.RE.MATCH_INCLUDE;
    final int excl = CrawlRules.RE.MATCH_EXCLUDE;

    rules.add(new CrawlRules.RE("^" + urlRoot.toString(),
				CrawlRules.RE.NO_MATCH_EXCLUDE));
    rules.add(new CrawlRules.RE(urlRoot.toString()+"lockss-volume"+
				volume+".shtml",
				incl));
    rules.add(new CrawlRules.RE(".*ck=nck.*", excl));
    rules.add(new CrawlRules.RE(".*ck=nck.*", excl));
    rules.add(new CrawlRules.RE(".*adclick.*", excl));
    rules.add(new CrawlRules.RE(".*/cgi/mailafriend.*", excl));
    rules.add(new CrawlRules.RE(".*/content/current/.*", incl));
    rules.add(new CrawlRules.RE(".*/content/vol"+volume+"/.*", incl));
    rules.add(new CrawlRules.RE(".*/cgi/content/.*/"+volume+"/.*", incl));
    rules.add(new CrawlRules.RE(".*/cgi/reprint/"+volume+"/.*", incl));
    rules.add(new CrawlRules.RE(".*/icons.*", incl));
    rules.add(new CrawlRules.RE(".*/math.*", incl));
    rules.add(new CrawlRules.RE("http://.*/.*/.*", excl));
    logger.debug("Rules: "+rules);
    return new CrawlRules.FirstMatch(rules);
  }

  private void loadProps() {
    pauseMS = Configuration.getIntParam(PARAM_HIGHWIRE_PAUSE_TIME,
                                        DEFAULT_PAUSE_TIME);
    ncCrawlInterval = 
      Configuration.getIntParam(PARAM_HIGHWIRE_NC_INTERVAL,
				DEFAULT_NC_INTERVAL);
  }

  public void pause() {
    pause(pauseMS);
  }

  public String getPluginId() {
    return "highwire";
  }

  public String getAUId() {
    return getAUId(base, volume);
  }

  public static String getAUId(URL url, int vol) {
    StringBuffer sb = new StringBuffer();
    sb.append(url.toString());
    sb.append("||");
    sb.append(vol);
    return sb.toString();
  }

  public int getVolumeNumber() {
    return volume;
  }

  public URL getBaseUrl() {
    return base;
  }

  public boolean shouldCrawlForNewContent(AuState aus) {
    long timeDiff = TimeBase.nowMs() - aus.getLastCrawlTime();
    logger.debug("Deciding whether to do new content crawl for "+aus);
    if (aus.getLastCrawlTime() == 0 || 
	timeDiff > (ncCrawlInterval * DAY_MS)) {
      return true;
    }
    return false;
  }


}
