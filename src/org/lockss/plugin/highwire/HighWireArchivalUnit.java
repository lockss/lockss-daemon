/*
 * $Id: HighWireArchivalUnit.java,v 1.45 2004-02-10 01:09:09 clairegriffin Exp $
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

package org.lockss.plugin.highwire;

import java.net.*;
import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.plugin.configurable.*;
import org.lockss.util.*;
import gnu.regexp.*;

/**
 * This is a first cut at making a HighWire plugin
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class HighWireArchivalUnit extends ConfigurableArchivalUnit {
  public static final String LOG_NAME = "HighWireArchivalUnit";
  /**
   * Configuration parameter for pause time in Highwire crawling.
   */
  public static final String AUPARAM_PAUSE_TIME = PAUSE_TIME_KEY;

  /**
   * Test parameter to activate use of crawl window in highwire.
   */
  public static final String AUPARAM_USE_CRAWL_WINDOW = USE_CRAWL_WINDOW;

  protected Logger logger = Logger.getLogger(LOG_NAME);

  public static final String AUPARAM_NEW_CONTENT_CRAWL = NEW_CONTENT_CRAWL_KEY;

  private int volume;

  /**
   * Standard constructor for HighWireArchivalUnit.
   *
   * @param myPlugin owner plugin
   */
  public HighWireArchivalUnit(Plugin myPlugin) {
    super(myPlugin);
  }

  protected FilterRule constructFilterRule(String mimeType) {
    if (mimeType!=null) {
      if (StringUtil.startsWithIgnoreCase(mimeType, "text/html")) {
        return new HighWireFilterRule();
      }
    }
    return null;
  }

  protected void loadAuConfigDescrs(Configuration config)
      throws ConfigurationException {

    super.loadAuConfigDescrs(config);
    volume = configurationMap.getInt(HighWirePlugin.AUPARAM_VOL, -1);
    if (volume < 0) {
      throw new ConfigurationException("Negative volume");
    }

  }

  protected String makeName() {
    String host = baseUrl.getHost();
    return host + ", vol. " + volume;
  }

  protected String makeStartUrl() {
    StringBuffer sb = new StringBuffer();
    sb.append(baseUrl.toString());
    sb.append("lockss-volume");
    sb.append(volume);
    sb.append(".shtml");
    logger.debug("makeStartUrl returning: "+sb.toString());
    return sb.toString();
  }

  protected CrawlRule makeRules()
      throws REException {
    List rules = new LinkedList();
    final int incl = CrawlRules.RE.MATCH_INCLUDE;
    final int excl = CrawlRules.RE.MATCH_EXCLUDE;

    rules.add(new CrawlRules.RE("^" + baseUrl.toString(),
				CrawlRules.RE.NO_MATCH_EXCLUDE));
    rules.add(new CrawlRules.RE(startUrlString, incl));
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

  protected CrawlWindow makeCrawlWindow() {
    logger.debug("Creating crawl window...");
    // disallow crawls from 6am->9am
    Calendar start = Calendar.getInstance();
    start.set(Calendar.HOUR_OF_DAY, 6);
    start.set(Calendar.MINUTE, 0);
    Calendar end = Calendar.getInstance();
    end.set(Calendar.HOUR_OF_DAY, 9);
    end.set(Calendar.MINUTE, 0);
    CrawlWindow interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.TIME, null);
    // disallow using 'NOT'
    return new CrawlWindows.Not(interval);
  }

  public int getVolumeNumber() {
    return volume;
  }

  public URL getBaseUrl() {
    return baseUrl;
  }

}
