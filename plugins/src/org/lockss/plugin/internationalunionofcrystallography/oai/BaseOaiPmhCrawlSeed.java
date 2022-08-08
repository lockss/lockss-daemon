/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.internationalunionofcrystallography.oai;

import java.io.IOException;
import java.text.*;
import java.util.*;
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
// XXX import org.lockss.util.TimeZoneUtil;

/**
 * CrawlSeed that uses an OAI library to fetch a list of article URLs
 * using list identifiers to use as start urls. This still uses a standard 
 * permission URL list.
 */
public abstract class BaseOaiPmhCrawlSeed extends BaseCrawlSeed {
  private static final Logger logger = Logger.getLogger(BaseOaiPmhCrawlSeed.class);
  
  public static final String DEFAULT_METADATA_PREFIX = "oai_lockss";
  public static final String DATE_FORMAT = "yyyy-MM-dd";
  public static final String DATETIME_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'";
  public static final String KEY_AU_OAI_FROM_DATE = "oai_from_date";
  public static final String KEY_AU_OAI_UNTIL_DATE = "oai_until_date";
  public static final String KEY_AU_OAI_SET = "au_oai_set";
  public static final String KEY_AU_OAI_URL_POSTFIX = "oai_url_postfix";
  public static final String KEY_AU_OAI_METADATA_PREFIX = "oai_metadata_prefix";
  public static final String KEY_AU_OAI_GRANULARITY = "oai_granularity";
  public static final String DEFAULT_OAI_URL_POSTFIX = "oai/request";
  public static final String YEAR_POSTFIX = "-01-01";
  public static final Granularity DEFAULT_GRANULARITY = Granularity.Day;
  public static final String NULL_SET = "[NONE]";
  
  protected ServiceProvider sp;
  protected String baseUrl;
  protected Date from;
  protected Date until;
  protected String set;
  protected Granularity granularity = DEFAULT_GRANULARITY;
  //The OAI pmh accepted format for the metadata response
  protected String metadataPrefix = DEFAULT_METADATA_PREFIX;
  //The path to the home of the OAI PMH server from the base url
  protected String oaiUrlPostfix = DEFAULT_OAI_URL_POSTFIX;
  protected boolean usesDateRange = true;
  protected boolean usesSet = true;
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
    // See https://www.openarchives.org/OAI/openarchivesprotocol.html#Datestamp for discussion
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
      if(!setGranularity(config.get(KEY_AU_OAI_GRANULARITY))) {
        throw new ConfigurationException(KEY_AU_OAI_GRANULARITY + 
                                         " must be " + Granularity.Day + 
                                         " or " + Granularity.Second);
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
    // TimeZoneUtil.getExactTimeZone depends on 1.74 build
    // As GMT is the default return value from getTimeZone, we don't care if the call fails silently
    TimeZone utc = TimeZone.getTimeZone("GMT"); // XXX     TimeZoneUtil.getExactTimeZone("GMT");
    DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
    df.setTimeZone(utc);
    this.from = parseDate(from, df, "from");
    this.until = parseDate(until, df, "until");
  }
  
  protected Date parseDate(String date, DateFormat df, String name) 
      throws ConfigurationException {
    Date ret;
    if(date.length() == 10) {
      date = date + "T00:00:00Z";
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
  
  /*
  public static void main(String[] args) throws Exception {
    SimpleDateFormat dayFormat = new SimpleDateFormat(DATE_FORMAT);
    SimpleDateFormat secFormat = new SimpleDateFormat(DATETIME_FORMAT);
    Date convertedDate = null;
    String yearMonth = "2016-01";
    TimeZone utc = TimeZoneUtil.getExactTimeZone("GMT");
    dayFormat.setTimeZone(utc);
    secFormat.setTimeZone(utc);
    
    try {
      convertedDate = secFormat.parse(yearMonth + "-01T00:00:00Z");
      convertedDate = dayFormat.parse(yearMonth + "-01");
    } catch (ParseException e) {
      e.printStackTrace();
      throw new ConfigurationException("Invalid value yearMonth: " + yearMonth, e);
    }
    Calendar c = Calendar.getInstance();
    c.setTime(convertedDate);
    int lday = c.getActualMaximum(Calendar.DAY_OF_MONTH);
    String sday = String.valueOf(lday);
    String[] ary = new String[] { yearMonth + "-01", yearMonth + "-" + sday};
    System.out.println(ary[1]);
  }
  */
}
