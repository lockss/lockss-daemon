/*
 * $Id: AbsintheArchivalUnit.java,v 1.9 2004-02-06 23:53:55 clairegriffin Exp $
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
import org.lockss.plugin.configurable.*;

/**
 * AbsintheArchivalUnit: The Archival Unit Class for Absinthe Literary
 * Review
 * @author Emil Aalto
 * @version 1.0
 */

public class AbsintheArchivalUnit extends ConfigurableArchivalUnit {
  /**
   * Configuration parameter for new content crawl interval
   */
  static final String AUPARAM_NEW_CONTENT_CRAWL = NEW_CONTENT_CRAWL_KEY;
  private static final long DEFAULT_NEW_CONTENT_CRAWL = 2 * Constants.WEEK;

  /**
   * Configuration parameter for pause time between fetchs.
   */
  public static final String AUPARAM_PAUSE_TIME = PAUSE_TIME_KEY;

  protected Logger logger = Logger.getLogger("AbsinthePlugin");

  private String year; // the year

  protected AbsintheArchivalUnit(Plugin myPlugin) {
    super(myPlugin);
  }

  protected void loadAuConfigDescrs(Configuration config) throws
      ConfigurationException {
    super.loadAuConfigDescrs(config);

    int i_year = configurationMap.getInt(AbsinthePlugin.AUPARAM_YEAR, -1);
    if (i_year < 0) {
      throw new ConfigurationException("Year Out of Range: " + i_year);
    }
    year = Integer.toString(i_year);

  }

  protected String makeName() {
    StringBuffer name = new StringBuffer(baseUrl.getHost());
    name.append(", ");
    name.append(year);
    return name.toString();
  }

  protected String makeStartUrl() {
    String ret;
    StringBuffer sb = new StringBuffer();
    sb.append(baseUrl.toString());
    sb.append("archives");
    sb.append(year.substring(year.length()-2));
    sb.append(".htm");
    ret = sb.toString();
    logger.debug("starting url is " + ret);
    return ret;
  }

  protected CrawlRule makeRules()
      throws REException {
    List rules = new LinkedList();
    final int incl = CrawlRules.RE.MATCH_INCLUDE;
    final int excl = CrawlRules.RE.MATCH_EXCLUDE;
    String rootUrl = baseUrl.toString();
    rules.add(new CrawlRules.RE("^" + rootUrl, CrawlRules.RE.NO_MATCH_EXCLUDE));
    rules.add(new CrawlRules.RE(startUrlString, incl));
    rules.add(new CrawlRules.RE(rootUrl + "stories/.*", incl));
    rules.add(new CrawlRules.RE(rootUrl + "poetics/.*", incl));
    rules.add(new CrawlRules.RE(rootUrl + "archives/.*", incl));
    // exclude book review index page
    rules.add(new CrawlRules.RE(rootUrl + "book_reviews/book_reviews.htm", excl));
    // include other pages
    rules.add(new CrawlRules.RE(rootUrl + "book_reviews/.*", incl));
    rules.add(new CrawlRules.RE(rootUrl + "images/.*", incl));
    return new CrawlRules.FirstMatch(rules);
  }

  protected void initAuKeys() {
    StringBuffer sb = new StringBuffer("%sarchives%02d.htm\n");
    sb.append(ConfigParamDescr.BASE_URL.getKey());
    sb.append("\n");
    sb.append(CM_AU_SHORT_YEAR_KEY+ ConfigParamDescr.YEAR.getKey());
    String starturl = sb.toString();
    configurationMap.putString(CM_AU_START_URL_KEY,starturl);

    sb = new StringBuffer("%s, %d\n");
    sb.append(ConfigParamDescr.BASE_URL.getKey());
    sb.append("\n");
    sb.append(ConfigParamDescr.YEAR.getKey());
    configurationMap.putString(CM_AU_NAME_KEY, sb.toString());

    List rules = new ArrayList();
    //rules.add(new CrawlRules.RE("^" + rootUrl, CrawlRules.RE.NO_MATCH_EXCLUDE));
    sb = new StringBuffer(String.valueOf(CrawlRules.RE.NO_MATCH_EXCLUDE));
    sb.append("\n^%s\n");
    sb.append(ConfigParamDescr.BASE_URL.getKey());
    rules.add(sb.toString());
    //rules.add(new CrawlRules.RE(startUrlString, incl));
    sb = new StringBuffer(String.valueOf(CrawlRules.RE.MATCH_INCLUDE));
    sb.append("\n");
    sb.append(starturl);
    rules.add(sb.toString());
    //rules.add(new CrawlRules.RE(rootUrl + "stories/.*", incl));
    sb = new StringBuffer(String.valueOf(CrawlRules.RE.MATCH_INCLUDE));
    sb.append("\n%sstories/.*\n");
    sb.append(ConfigParamDescr.BASE_URL.getKey());
    rules.add(sb.toString());
    //rules.add(new CrawlRules.RE(rootUrl + "poetics/.*", incl));
    sb = new StringBuffer(String.valueOf(CrawlRules.RE.MATCH_INCLUDE));
    sb.append("\n%spoetics/.*\n");
    sb.append(ConfigParamDescr.BASE_URL.getKey());
    rules.add(sb.toString());
    //rules.add(new CrawlRules.RE(rootUrl + "archives/.*", incl));
    sb = new StringBuffer(String.valueOf(CrawlRules.RE.MATCH_INCLUDE));
    sb.append("\n%sarchives/.*\n");
    sb.append(ConfigParamDescr.BASE_URL.getKey());
    rules.add(sb.toString());
    //rules.add(new CrawlRules.RE(rootUrl + "book_reviews/book_reviews.htm", excl));
    sb = new StringBuffer(String.valueOf(CrawlRules.RE.MATCH_EXCLUDE));
    sb.append("\n%sbook_reviews/book_reviews.htm\n");
    sb.append(ConfigParamDescr.BASE_URL.getKey());
    rules.add(sb.toString());
    //rules.add(new CrawlRules.RE(rootUrl + "book_reviews/.*", incl));
    sb = new StringBuffer(String.valueOf(CrawlRules.RE.MATCH_INCLUDE));
    sb.append("\n%sbook_reviews/.*\n");
    sb.append(ConfigParamDescr.BASE_URL.getKey());
    rules.add(sb.toString());
    //rules.add(new CrawlRules.RE(rootUrl + "images/.*", incl));
    sb = new StringBuffer(String.valueOf(CrawlRules.RE.MATCH_INCLUDE));
    sb.append("\n%simages/.*\n");
    sb.append(ConfigParamDescr.BASE_URL.getKey());
    rules.add(sb.toString());

    configurationMap.putCollection(CM_AU_RULES_KEY, rules);

  }

}
