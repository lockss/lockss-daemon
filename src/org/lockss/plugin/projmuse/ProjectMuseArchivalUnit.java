/*
 * $Id: ProjectMuseArchivalUnit.java,v 1.23 2004-02-10 01:09:09 clairegriffin Exp $
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

import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.plugin.configurable.*;
import org.lockss.util.*;
import gnu.regexp.*;

/**
 * This is a first cut at making a Project Muse plugin
 *
 * @author  Emil Aalto
 * @version 0.0
 */

public class ProjectMuseArchivalUnit extends ConfigurableArchivalUnit {
  /**
   * Configuration parameter for new content crawl interval
   */
  static final String AUPARAM_NEW_CONTENT_CRAWL = NEW_CONTENT_CRAWL_KEY;
  private static final long DEFAULT_NEW_CONTENT_CRAWL = 2 * Constants.WEEK;

  /**
   * Configuration parameter for pause time between fetchs.
   */
  public static final String AUPARAM_PAUSE_TIME = PAUSE_TIME_KEY;
  private static final long DEFAULT_PAUSE_TIME = 6 * Constants.SECOND;

  static final int REFETCH_DEPTH = 2;

  protected Logger logger = Logger.getLogger("ProjectMusePlugin");

  private int volume; // the volume index
  private String journalDir;

  protected ProjectMuseArchivalUnit(Plugin myPlugin) {
    super(myPlugin);
    defaultFetchDelay = DEFAULT_PAUSE_TIME;
  }

  /**
   * Override to filter javascript for html.
   * @param mimeType the mime type
   * @return the FilterRule if 'text/html', else null
   */
  protected FilterRule constructFilterRule(String mimeType) {
    if (mimeType!=null) {
      if (StringUtil.startsWithIgnoreCase(mimeType, "text/html")) {
        return new ProjectMuseFilterRule();
      }
    }
    return null;
  }

  protected void loadAuConfigDescrs(Configuration config)
      throws ConfigurationException {
    super.loadAuConfigDescrs(config);
    // get the base url string
    volume = configurationMap.getInt(ProjectMusePlugin.AUPARAM_VOL, -1);
    if (volume < 0) {
      throw new ConfigurationException("Negative volume");
    }

    // get the journal directory
    journalDir = configurationMap.getString(ProjectMusePlugin.AUPARAM_JOURNAL_DIR, null);

  }


  protected String makeName() {
    StringBuffer name = new StringBuffer(baseUrl.getHost());
    name.append(", ");
    name.append(journalDir);
    name.append(", vol. ");
    name.append(volume);
    return name.toString();
  }



  protected String makeStartUrl() {
    String ret;
    StringBuffer sb = new StringBuffer();
    sb.append(baseUrl.toString());
    // always 3 digit?
    sb.append("journals/");
    sb.append(journalDir);
    sb.append("/v");
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

  /**
   * Override to use refetchDepth of 2.
   * @return the CrawlSpec need by this au.
   * @throws REException if the CrawlRules contain an invalid regular expression
   */
  protected CrawlSpec makeCrawlSpec() throws REException {
    CrawlRule rule = makeRules();
    return new CrawlSpec(startUrlString, rule, REFETCH_DEPTH);
  }


  protected CrawlRule makeRules() throws REException {
    List rules = new LinkedList();
    final int incl = CrawlRules.RE.MATCH_INCLUDE;
    final int excl = CrawlRules.RE.MATCH_EXCLUDE;
    String urlRoot = baseUrl.toString();
    rules.add(new CrawlRules.RE("^" + urlRoot, CrawlRules.RE.NO_MATCH_EXCLUDE));
    rules.add(new CrawlRules.RE(startUrlString, incl));
    rules.add(new CrawlRules.RE(urlRoot +
                                "journals/"+journalDir+"/toc/[a-zA-Z]*" + volume +
                                "\\..*", incl));
    rules.add(new CrawlRules.RE(urlRoot + "images/.*", incl));
    return new CrawlRules.FirstMatch(rules);
  }

}
