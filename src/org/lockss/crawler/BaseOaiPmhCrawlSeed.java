/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import java.text.*;
import java.util.*;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.Logger;

import com.lyncode.xoai.model.oaipmh.Granularity;
import com.lyncode.xoai.serviceprovider.ServiceProvider;
import com.lyncode.xoai.serviceprovider.client.HttpOAIClient;
import com.lyncode.xoai.serviceprovider.model.Context;

/**
 * CrawlSeed that uses an OAI library to fetch a list of article URLs
 * using list identifiers to use as start urls. This still uses a standard 
 * permission URL list.
 * 
 * @author wkwilson
 */
public abstract class BaseOaiPmhCrawlSeed extends BaseCrawlSeed {
  public BaseOaiPmhCrawlSeed(ArchivalUnit au) {
    super(au);
  }

  private static final Logger log = Logger.getLogger(BaseOaiPmhCrawlSeed.class);
  
  public static final String DEFAULT_METADATA_PREFIX = "oai_dc";
  public static final String GRANULARITY_DAY = "YYYY-MM-DD";
  public static final String GRANULARITY_SECOND = "YYYY-MM-DDThh:mm:ssZ";
  public static final String DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss";
  public static final String KEY_AU_OAI_FROM_DATE = "oai_from_date";
  public static final String KEY_AU_OAI_UNTIL_DATE = "oai_until_date";
  public static final String KEY_AU_OAI_SET = "oai_set";
  public static final String KEY_AU_OAI_URL_POSTFIX = "oai_url_postfix";
  public static final String DEFAULT_OAI_URL_POSTFIX = "oai/request";
  public static final String YEAR_POSTFIX = "-01-01T00:00:00";
  public static final String DEFAULT_GRANULARITY = GRANULARITY_SECOND;
  
  protected ServiceProvider sp;
  protected String baseUrl;
  protected Date from;
  protected Date until;
  protected String set;
  //The OAI pmh accepted format for the metadata response
  protected String metadataPrefix = DEFAULT_METADATA_PREFIX;
  //The path to the home of the OAI PMH server from the base url
  protected String oaiUrlPostfix = DEFAULT_OAI_URL_POSTFIX;
  
  /**
   * If the plugin uses a year param convert that to from and until datetimes
   * @param year
   * @throws PluginException 
   * @throws ConfigurationException 
   */
  protected void setDates(int year) throws ConfigurationException {
    setDates(year + YEAR_POSTFIX, (year + 1) + YEAR_POSTFIX);
  }
  
  /**
   * Set the from and until dates for the OAI query
   * @param from
   * @param until
   * @throws PluginException 
   * @throws ConfigurationException 
   */
  protected void setDates(String from, String until)
      throws ConfigurationException {
    TimeZone utc = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat(DATE_FORMAT);
    df.setTimeZone(utc);
    //if it's a length that it may be missing timestamp add it 
    if(from.length() == 10) {
      from = from + "T00:00:00";
    }
    if(until.length() == 10) {
      until = until + "T00:00:00";
    }
    try {
      this.from = df.parse(from);
      this.until = df.parse(until);
    } catch (ParseException e) {
      throw new ConfigurationException(
          "Incorrectly formatted OAI date range", e);
    }
  }
  
  /**
   * Default is oai_dc 
   * @param metadataPrefix
   */
  public void setMetadataPrefix(String metadataPrefix) {
    if(metadataPrefix != null) {
      this.metadataPrefix = metadataPrefix;
    }
  }
  
  /**
   * Sets url for OAI query. Called to create service provider.
   * 
   * @param url
   * @return Context for OAI query
   */
  protected Context buildContext(String url) {
    Context con = new Context();
    con.withBaseUrl(url);
    con.withGranularity(Granularity.fromRepresentation(DEFAULT_GRANULARITY));
    con.withOAIClient(new HttpOAIClient(con));
    return con;
  }
  
  /**
   * Return the service provider created using buildcontext at the url baseUrl
   * + oaiUrlPostfix
   * @return
   */
  protected ServiceProvider getServiceProvider() {
    if(sp == null){
      sp = new ServiceProvider(buildContext(baseUrl + oaiUrlPostfix));
    }
    return sp;
  }
  
  @Override
  public boolean isFailOnStartUrlError() {
    return false;
  }
  
  @Override
  public abstract Collection<String> getStartUrls() 
      throws ConfigurationException, PluginException, IOException;
  
}
