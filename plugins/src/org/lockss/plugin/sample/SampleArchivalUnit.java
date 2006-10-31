/*
 * $Id: SampleArchivalUnit.java,v 1.3 2006-10-31 07:01:07 thib_gc Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.sample;

import java.net.*;
import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.definable.*;
import org.lockss.util.*;

/**
 * <p>SampleArchivalUnit: The Archival Unit Class for SamplePlugin.  This archival unit
 * uses a base url and a volume to define a archival unit. </p>
 * @author Claire Griffin
 * @version 1.0
 */

public class SampleArchivalUnit extends DefinableArchivalUnit {
  /**
   * Configuration parameter for new content crawl interval
   */
  static final String AUPARAM_NEW_CONTENT_CRAWL = KEY_NEW_CONTENT_CRAWL_INTERVAL;
  private static final long DEFAULT_NEW_CONTENT_CRAWL = 2 * Constants.WEEK;

  /**
   * Configuration parameter for pause time between fetchs.
   */
  public static final String AUPARAM_PAUSE_TIME = KEY_PAUSE_TIME;
  private static final long DEFAULT_PAUSE_TIME = 10 * Constants.SECOND;

  protected Logger logger = Logger.getLogger("SamplePlugin");

  private int volume; // the volume number

  public SampleArchivalUnit(DefinablePlugin myPlugin,
                            ExternalizableMap map) {
    super(myPlugin, map);
  }


  public void loadAuConfigDescrs(Configuration config)
      throws ConfigurationException {
    super.loadAuConfigDescrs(config);
    // get the volume string
    volume = paramMap.getInt(SamplePlugin.AUPARAM_VOL, -1);
    if (volume <= 0) {
      throw new ConfigurationException("Invalid volume: "+volume);
    }
  }

   /**
    * return a string that represents the journal and volume to be displayed
    * in the UI to identify the plugin
    * @return a concatenated string: <baseUrl>, vol. <vol>
    */
   protected String makeName() {
    URL baseUrl = paramMap.getUrl(KEY_AU_BASE_URL, null);
    StringBuffer name = new StringBuffer(baseUrl.getHost());
    name.append(", vol. ");
    name.append(volume);
    return name.toString();
  }

  /**
   * return a string that points to the volume index page
   * @return a concatenated string that points to the volume index page for
   * this journal: <base>/lockss-volume<vol>.html
   */
  protected String makeStartUrl() {
    String ret;
    StringBuffer sb = new StringBuffer();
    sb.append(paramMap.getUrl(KEY_AU_BASE_URL, null).toString());
    sb.append("lockss-volume");
    sb.append(volume);
    sb.append(".html");
    ret = sb.toString();
    // s/b: http://www.sample.com/samplejournal/lockss-volume<vol>.html
    logger.debug("starting url is " + ret);
    return ret;
  }

  /**
   * return the collection of crawl rules used to crawl and cache a journal
   * @return CrawlRule
   * @throws LockssRegexpException
   */
  protected CrawlRule makeRules() throws LockssRegexpException {
    List rules = new LinkedList();
    final int incl = CrawlRules.RE.MATCH_INCLUDE;
    final int excl = CrawlRules.RE.MATCH_EXCLUDE;
    String base = paramMap.getUrl(KEY_AU_BASE_URL, null).toString();

    // exclude anything which doesn't start with our base url
    rules.add(new CrawlRules.RE("^" + base, CrawlRules.RE.NO_MATCH_EXCLUDE));

    // include the start url
    rules.add(new CrawlRules.RE(makeStartUrl(), incl));

    // *** rules to store gif and jpg.
    rules.add(new CrawlRules.RE(base + ".*\\.gif", incl));
    rules.add(new CrawlRules.RE(base + ".*\\.jpg", incl));

    // *** rules to include your html &/or pdf files
    rules.add(new CrawlRules.RE(base + volume +".*\\.html", incl));
    rules.add(new CrawlRules.RE(base + volume +".*\\.pdf", incl));
    return new CrawlRules.FirstMatch(rules);
  }
}
