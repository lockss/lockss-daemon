/*

Copyright (c) 2017-2020 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.respediatrica;

import org.apache.commons.lang.time.DateUtils;
import org.dspace.xoai.model.oaipmh.Granularity;
import org.dspace.xoai.serviceprovider.ServiceProvider;
import org.dspace.xoai.serviceprovider.model.Context;
import org.lockss.config.Configuration;
import org.lockss.config.Configuration.InvalidParam;
import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.Logger;
import org.lockss.util.TimeZoneUtil;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;

/**
 * CrawlSeed that uses an OAI library to fetch a list of article URLs
 * using list identifiers to use as start urls. This still uses a standard 
 * permission URL list.
 */
public abstract class BaseOaiPmhCrawlSeed extends BaseCrawlSeed {
  private static final Logger logger = 
      Logger.getLogger(BaseOaiPmhCrawlSeed.class);
  
  public static final String DATE_FORMAT = "yyyy-MM-dd";
  public static final String KEY_AU_OAI_FROM_DATE = "oai_from_date";
  public static final String KEY_AU_OAI_UNTIL_DATE = "oai_until_date";
  public static final String KEY_AU_OAI_SET = "au_oai_set";
  public static final String KEY_AU_OAI_URL_POSTFIX = "oai_url_postfix";
  public static final String KEY_AU_OAI_METADATA_PREFIX = "oai_metadata_prefix";
  public static final String KEY_AU_OAI_GRANULARITY = "oai_granularity";
  public static final String DEFAULT_OAI_URL_POSTFIX = "oai/request";
  public static final String YEAR_START_POSTFIX = "-01-01";
  public static final String YEAR_END_POSTFIX = "-12-31";
  public static final Granularity DEFAULT_GRANULARITY = Granularity.Day;
  public static final String NULL_SET = "[NONE]";
  
  protected ServiceProvider sp;
  protected String baseUrl;
  protected Date from;
  protected Date until;
  protected String set;
  protected Granularity granularity = DEFAULT_GRANULARITY;
  //The OAI pmh accepted format for the metadata response
  //protected String metadataPrefix = DEFAULT_METADATA_PREFIX;
  protected String metadataPrefix = KEY_AU_OAI_METADATA_PREFIX;
  //The path to the home of the OAI PMH server from the base url
  protected String oaiUrlPostfix = DEFAULT_OAI_URL_POSTFIX;
  protected boolean usesDateRange = true;
  protected boolean usesSet = false;
  protected CrawlerFacade facade;
  protected Collection<String> permUrls = new ArrayList<String>();
  
  public BaseOaiPmhCrawlSeed(CrawlerFacade cf) {
    super(cf);
    facade = cf;
    if(facade == null || au == null) {
      throw new IllegalArgumentException(
          "Valid ArchivalUnit required for crawl initializer");
    }
  }
  
  @Override
  protected void initialize() 
      throws PluginException, ConfigurationException, IOException {
    super.initialize();
    populateFromConfig(au.getConfiguration());
  }
  
  /**
   * Pulls needed params from the au config. 
   * @param config
   * @throws PluginException
   * @throws ConfigurationException
   * @throws InvalidParam 
   */
  protected void populateFromConfig(Configuration config) 
      throws PluginException, ConfigurationException {
    this.baseUrl = config.get(ConfigParamDescr.BASE_URL.getKey());
    if(config.containsKey(ConfigParamDescr.YEAR.getKey())) {
      try {
        setDates(config.getInt(ConfigParamDescr.YEAR.getKey()));
      } catch (InvalidParam e) {
        throw new ConfigurationException("Year must be an integer", e);
      }
    } else if (config.containsKey(KEY_AU_OAI_FROM_DATE) &&
               config.containsKey(KEY_AU_OAI_UNTIL_DATE)) {
      setDates(config.get(KEY_AU_OAI_FROM_DATE), 
               config.get(KEY_AU_OAI_UNTIL_DATE));
    } else {
      usesDateRange=false;
    }
    if(config.containsKey(KEY_AU_OAI_SET)) {
      this.set = config.get(KEY_AU_OAI_SET);
    } else {
      usesSet=false;
    }
    if(config.containsKey(KEY_AU_OAI_URL_POSTFIX)) {
      if(!setUrlPostfix(config.get(KEY_AU_OAI_URL_POSTFIX))){
        throw new ConfigurationException(KEY_AU_OAI_URL_POSTFIX +
                                         " must not be null");
      }
    }
    if(config.containsKey(KEY_AU_OAI_METADATA_PREFIX)) {
      if(!setMetadataPrefix(config.get(KEY_AU_OAI_METADATA_PREFIX))) {
        throw new ConfigurationException(KEY_AU_OAI_METADATA_PREFIX +
                                         " must not be null");
      }
    }
    if(config.containsKey(KEY_AU_OAI_GRANULARITY)) {
      logger.debug3("Fei - KEY_AU_OAI_GRANULARITY is set");
      if(!setGranularity(config.get(KEY_AU_OAI_GRANULARITY))) {
        throw new ConfigurationException(KEY_AU_OAI_GRANULARITY + 
                                         " must be " + Granularity.Day + 
                                         " or " + Granularity.Second);
      }  else {
        logger.debug3("Fei - granularity is set to : " + config.get(KEY_AU_OAI_GRANULARITY));
      }
    }
      
  }

  /**
   * If the plugin uses a year param convert that to from and until datetimes
   * @param year
   * @throws PluginException 
   * @throws ConfigurationException 
   */
  protected void setDates(int year) throws ConfigurationException {
    setDates(year + YEAR_START_POSTFIX, year  + YEAR_END_POSTFIX);
    logger.debug3("Fei - setDates: from " + year + YEAR_START_POSTFIX + " until" + (year + 1) + YEAR_END_POSTFIX);
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
    TimeZone utc = TimeZoneUtil.getExactTimeZone("UTC");
    DateFormat df = new SimpleDateFormat(DATE_FORMAT);
    df.setTimeZone(utc);
    this.from = parseDate(from, df, "from");
    this.until = parseDate(until, df, "until");

    logger.debug3("Fei - setRange:  from = " + this.from + ", until = " + this.until);
  }

  
  protected Date parseDate(String date, DateFormat df, String name) 
      throws ConfigurationException {
    Date ret;
    
    if(date.length() == 10) {
      date = date + "T00:00:00";
    }
    try {
      ret = df.parse(date);
    } catch (ParseException e) {
      throw new ConfigurationException(
              "Incorrectly formatted OAI " + name + " date", e);
    }
    return ret;
  }
  
  /**
   * Default is oai_lockss. this cannot be null.
   * @param metadataPrefix
   */
  public boolean setMetadataPrefix(String metadataPrefix) {
    if(metadataPrefix != null) {
      this.metadataPrefix = metadataPrefix;
      return true;
    }
    return false;
  }
  
  public boolean setUrlPostfix(String urlPostfix) {
    if(urlPostfix != null) {
      this.oaiUrlPostfix = urlPostfix;
      return true;
    }
    return false;
  }
  
  public boolean setGranularity(String granularity) {
    try {
      this.granularity = Granularity.fromRepresentation(granularity);
      return true;
    } catch(IllegalArgumentException e) {
      return false;
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
    con.withGranularity(granularity);
    con.withOAIClient(new UrlFetcherOaiClient(url, facade));
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
  
  /**
   * All URLs are start urls so don't fail on error
   * @return false
   */
  @Override
  public boolean isFailOnStartUrlError() {
    return false;
  }
  
  //Override to force children to implement their own
  @Override
  public abstract Collection<String> doGetStartUrls() 
      throws ConfigurationException, PluginException, IOException;

  
}
