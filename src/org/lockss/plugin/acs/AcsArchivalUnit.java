/*
 * $Id: AcsArchivalUnit.java,v 1.9 2003-12-17 02:09:46 tlipkis Exp $
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
import org.lockss.daemon.Configuration.*;

/**
 * AcsArchivalUnit: The Archival Unit Class for American Chemical Society
 * Plugin
 * @author Claire Griffin
 * @version 1.0
 */

public class AcsArchivalUnit extends BaseArchivalUnit {


  /**
   * Configuration parameter for new content crawl interval
   */
  static final String AUPARAM_NEW_CONTENT_CRAWL = NEW_CONTENT_CRAWL_KEY;
  private static final long DEFAULT_NEW_CONTENT_CRAWL= 2 * Constants.WEEK;

  /**
   * Configuration parameter for pause time between fetchs.
   */
  public static final String AUPARAM_PAUSE_TIME = PAUSE_TIME_KEY;
  private static final long DEFAULT_PAUSE_TIME = 10 * Constants.SECOND;

  protected Logger logger = Logger.getLogger("AcsArchivalUnit");

  private URL articleUrl;           // the base url for the articles
  private int volume;               // the volume index
  private String journalKey;        // the key used to specify the journal
  private int year;                 // the year of the volume

  protected AcsArchivalUnit(Plugin myPlugin) {
    super(myPlugin);
  }


  protected void setAuParams(Configuration config)
      throws ConfigurationException {
    // get the article root url
    articleUrl = configMap.getUrl(AcsPlugin.AUPARAM_ARTICLE_URL, null);

    // get the journal key
    journalKey = configMap.getString(AcsPlugin.AUPARAM_JOURNAL_KEY, null);

    // get the volume string
    volume = loadConfigInt(AcsPlugin.AUPARAM_VOL, config);
    if (volume < 0) {
      throw new ConfigurationException("Negative volume");
    }

    // get the volume year
    year = loadConfigInt(AcsPlugin.AUPARAM_YEAR, config);
    if (year < 1900) {
      throw new ConfigurationException("Year out of range - must be after 1900");
    }

    defaultFetchDelay = DEFAULT_PAUSE_TIME;
    defaultContentCrawlIntv = DEFAULT_NEW_CONTENT_CRAWL;
  }

  protected String makeStartUrl() {
    String ret;
    StringBuffer sb = new StringBuffer();
    sb.append(baseUrl.toString());
    sb.append("acs/journals/toc.njs_select_issue?in_coden=");
    sb.append(journalKey);
    sb.append("&in_volume=");
    sb.append(volume);
    sb.append("&in_decade=");
    sb.append(year - (year%10));
    ret = sb.toString();
    logger.debug("starting url is "+ ret);
    return ret;
  }

  protected CrawlSpec makeCrawlSpec()
      throws REException {

    CrawlRule rule = makeRules();
    return new CrawlSpec(startUrlString, rule, 2);
  }

  protected String makeName() {
    StringBuffer name = new StringBuffer(baseUrl.getHost());
    name.append(", ");
    name.append(journalKey);
    name.append(", vol. ");
    name.append(volume);
    return name.toString();
  }


  protected CrawlRule makeRules() throws REException {
    List rules = new LinkedList();
    final int incl = CrawlRules.RE.MATCH_INCLUDE;
    final int excl = CrawlRules.RE.MATCH_EXCLUDE;
    String artRoot = articleUrl.toString();
    String idxRoot = baseUrl.toString();

    // include our journal volume table of contents page

    rules.add(new CrawlRules.RE(idxRoot+"acs/journals/toc.njs_select.*in_coden="
                                +journalKey +".*in_volume=" + volume + ".*", incl));

    // include our volume's issue table of contents pages
    rules.add(new CrawlRules.RE(idxRoot +"acs/journals/toc.page.*incoden=" + journalKey
                                + ".*involume=" + volume, incl));

    // exclude everything else not part of the article root
    rules.add(new CrawlRules.RE("^" + artRoot, CrawlRules.RE.NO_MATCH_EXCLUDE));

    // include the stuff that determines how the page looks
    rules.add(new CrawlRules.RE(artRoot + ".*.gif", incl)); // gifs
    rules.add(new CrawlRules.RE(artRoot + ".*.jpg", incl)); // jpgs
    rules.add(new CrawlRules.RE(artRoot + ".*.css", incl)); // style sheets

    // include the articles for this journal and volume
    rules.add(new CrawlRules.RE(artRoot + ".*incoden=" + journalKey
                                + ".*involume=" + volume, incl));
    rules.add(new CrawlRules.RE(artRoot + "cgi-bin/article.cgi.*" +journalKey
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
