/*
 * $Id: HighWireArchivalUnit.java,v 1.19 2003-04-17 00:55:50 troberts Exp $
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
import org.lockss.plugin.base.*;

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
  public static final String AUPARAM_PAUSE_TIME =
      Configuration.PREFIX + "highwire.pause.time";
  private static final long DEFAULT_PAUSE_TIME = 10 * Constants.SECOND;

  private static final String EXPECTED_URL_PATH = "/";

  protected Logger logger = Logger.getLogger(LOG_NAME);
  private long pauseMS;

  private int volume;
  private URL base;

  public static final String AUPARAM_NC_INTERVAL =
      Configuration.PREFIX + "highwire.nc_interval";
  private static final long DEFAULT_NC_INTERVAL = 14 * Constants.DAY;;

  private long ncCrawlInterval;



  /**
   * Standard constructor for HighWireArchivalUnit.
   *
   * @param myPlugin owner plugin
   * @param config configuration info for AU
   * @throws ArchivalUnit.ConfigurationException
   */
  public HighWireArchivalUnit(Plugin myPlugin) {
    super(myPlugin);
  }


  public void setConfiguration(Configuration config) 
      throws ConfigurationException {
    super.setConfiguration(config);

    if (config == null) {
      throw new ArchivalUnit.ConfigurationException("Null configInfo");
    }
    String urlStr = config.get(HighWirePlugin.AUPARAM_BASE_URL);
    if (urlStr == null) {
      throw new
	ArchivalUnit.ConfigurationException("No configuration value for "+
					    HighWirePlugin.AUPARAM_BASE_URL);
    }
    String volStr = config.get(HighWirePlugin.AUPARAM_VOL);
    if (volStr == null) {
      throw new
	ArchivalUnit.ConfigurationException("No Configuration value for "+
					    HighWirePlugin.AUPARAM_VOL);
    }

    try {
      this.base = new URL(urlStr);
      this.volume = Integer.parseInt(volStr);
      
    } catch (MalformedURLException murle) {
      throw new 
	ArchivalUnit.ConfigurationException(HighWirePlugin.AUPARAM_BASE_URL+
					    " set to a bad url "+
					    urlStr, murle);
    }
    if (base == null) {
      throw new ArchivalUnit.ConfigurationException("Null base url");
    } if (volume < 0) {
      throw new ArchivalUnit.ConfigurationException("Negative volume");
    } if (!EXPECTED_URL_PATH.equals(base.getPath())) {
      throw new ArchivalUnit.ConfigurationException("Url has illegal path: "+
						    base.getPath());
    }
 
   try {
      this.crawlSpec = makeCrawlSpec(base, volume);
    } catch (REException e) {
      // tk - not right.  Illegal RE is caused by internal error, not config
      // error
      throw new ArchivalUnit.ConfigurationException("Illegal RE", e);
    }
    pauseMS = Configuration.getTimeIntervalParam(AUPARAM_PAUSE_TIME,
						 DEFAULT_PAUSE_TIME);
    ncCrawlInterval =
      Configuration.getTimeIntervalParam(AUPARAM_NC_INTERVAL,
 					 DEFAULT_NC_INTERVAL);
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

  String makeStartUrl(URL base, int volume) {
    StringBuffer sb = new StringBuffer();
    sb.append(base.toString());
    sb.append("lockss-volume");
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

  public void pause() {
    pause(pauseMS);
  }

  public String getName() {
    String host = StringUtil.trimHostName(base.getHost());
    return host + ", vol. " + volume;
  }

  public int getVolumeNumber() {
    return volume;
  }

  public URL getBaseUrl() {
    return base;
  }

  public boolean shouldCrawlForNewContent(AuState aus) {
    long timeDiff = TimeBase.msSince(aus.getLastCrawlTime());
    logger.debug("Deciding whether to do new content crawl for "+aus);
    if (aus.getLastCrawlTime() == 0 ||
	timeDiff > (ncCrawlInterval)) {
      return true;
    }
    return false;
  }

  public List getNewContentCrawlUrls() {
    return ListUtil.list(makeStartUrl(base, volume));
  }


}
