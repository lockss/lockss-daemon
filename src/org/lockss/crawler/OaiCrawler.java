/*
 * $Id: OaiCrawler.java,v 1.18 2006-11-14 19:21:29 tlipkis Exp $
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

package org.lockss.crawler;

import java.text.SimpleDateFormat;
import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.oai.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.state.AuState;
import org.lockss.util.*;

public class OaiCrawler extends FollowLinkCrawler {

  private static final String PARAM_OAI_REQUEST_RETRY_TIMES =
    Configuration.PREFIX + "OaiHandler.numOaiRequestRetries";
  private static final int DEFAULT_OAI_REQUEST_RETRY_TIMES = 3;

  private int maxOaiRetries = DEFAULT_OAI_REQUEST_RETRY_TIMES;

  private OaiCrawlSpec spec;

  private static Logger logger = Logger.getLogger("OaiCrawler");

  //dateformat following ISO 8601
  private static SimpleDateFormat iso8601DateFormatter =
    new SimpleDateFormat ("yyyy-MM-dd");

  public OaiCrawler(ArchivalUnit au, CrawlSpec crawlSpec, AuState aus) {
    super(au, crawlSpec, aus);
    spec = (OaiCrawlSpec) crawlSpec;
    String oaiHandlerUrl = spec.getOaiRequestData().getOaiRequestHandlerUrl();
    crawlStatus = new CrawlerStatus(au, ListUtil.list(oaiHandlerUrl),
				    getTypeString());
  }

  protected void setCrawlConfig(Configuration config) {
    super.setCrawlConfig(config);
    maxOaiRetries =
      config.getInt(PARAM_OAI_REQUEST_RETRY_TIMES,
                    DEFAULT_OAI_REQUEST_RETRY_TIMES);
  }

  public int getType() {
    return Crawler.OAI;
  }

  public String getTypeString() {
    return "OAI";
  }

  public boolean isWholeAU() {
    return true;
  }

  /**
   * Here for the test code to override
   * @return a new instance of OaiHandler
   */
  protected OaiHandler getOaiHandler() {
    return new OaiHandler();
  }

  /***
   * Issue a OAI request and get OAI response, fetch and parse the url from the
   * oai response return the set of parsed url if it is in followLink mode
   *
   * @return a set of Url that parsed from the url in the Oai response
   */
  protected Set getUrlsToFollow() {
    OaiRequestData oaiRequestData = spec.getOaiRequestData();

    OaiHandler oaiHandler = getOaiHandler();
    try {
      oaiHandler.issueRequest(oaiRequestData, getFromTime(), getUntilTime());
      oaiHandler.processResponse(maxOaiRetries);
    } catch (RuntimeException ex) {
      logger.error("Error while trying to process the OAI request", ex);
      crawlStatus.setCrawlError("Error in processing Oai Request");
      return Collections.EMPTY_SET;
    }
    
    Set oaiStartUrls = newSet();

    List errList = oaiHandler.getErrors();
    if ( !errList.isEmpty() ){
      crawlStatus.setCrawlError("Error in processing Oai Records");
      //XXX need to think how to reflect errors occurs in OaiHandler back to UI or daemon
//    logger.error("Error in processing Oai Records, here is the stack of error(s):\n");
//    Iterator errIt = errList.iterator();
//    while (errIt.hasNext()){
//    Exception oaiEx = (Exception) errIt.next();
//    logger.error("",oaiEx);
//    }
    }

    Set updatedUrls = oaiHandler.getUpdatedUrls();
    if ( updatedUrls.isEmpty() ) {
      logger.warning("No urls found in the OAI reponse!");
    } else {
      // filter out URLs not in crawl spec, as this is routine for OAI queries
      for (Iterator it = updatedUrls.iterator(); it.hasNext(); ) {
        String url = (String) it.next();

        logger.debug2("Trying to process " +url);

        if (spec.isIncluded(url)) {
	  crawlStatus.addPendingUrl(url);
	  oaiStartUrls.add(url);
        } else {
          logger.debug("OAI response url not in crawl spec: " + url);
        }
      }
    }
    return oaiStartUrls;
  }

  /**
   * getting the last crawl time from AuState of the current AU
   *
   * //XXX noted: OAI protocol just enforce day granularity, we still need
   * to check if-modified-since. we need to implement a better getTime
   * method in aus to make it oai crawler work correctly.
   */
  protected String getFromTime(){
    Date lastCrawlDate = new Date(aus.getLastCrawlTime());
    String lastCrawlDateString = iso8601DateFormatter.format(lastCrawlDate);
    logger.debug3("from=" + lastCrawlDateString);
    //String lastCrawlDateString = "2004-09-08";
    return lastCrawlDateString;
  }

  /**
   * getting the time until when we want all the changed page's
   * url from the publisher/repository
   *
   * //XXX is this the policy we want ?
   * Now it will return the time when this method is called
   */
  protected String getUntilTime(){
    Date currentDate = new Date();
    String currentDateString = iso8601DateFormatter.format(currentDate);
    logger.debug3("until="+currentDateString);
    //String currentDateString = "2004-09-14";
    return currentDateString;
  }

  protected boolean shouldFollowLink(){
    return spec.getFollowLinkFlag();
  }
}
