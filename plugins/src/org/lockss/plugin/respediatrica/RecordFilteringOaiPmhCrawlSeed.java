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

import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.serviceprovider.exceptions.BadArgumentException;
import org.dspace.xoai.serviceprovider.model.Context;
import org.dspace.xoai.serviceprovider.model.Context.KnownTransformer;
import org.dspace.xoai.serviceprovider.parameters.ListRecordsParameters;
import org.dspace.xoai.services.api.MetadataSearch;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.Logger;
import java.util.Iterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;


public abstract class RecordFilteringOaiPmhCrawlSeed extends BaseOaiPmhCrawlSeed {
  private static Logger logger =
      Logger.getLogger(RecordFilteringOaiPmhCrawlSeed.class);

  protected boolean usesDateRange = true;
  protected boolean usesSet = false;
  protected boolean usesGranularity = true;
  protected Map<String, Pattern> metadataRules;
  public static final String KEY_AU_OAI_FILTER_RULES = "au_oai_filter_rules";
  public static final String KEY_AU_OAI_DATE = "au_oai_date";
  public static final String KEY_AU_OAI_GRANULARITY = "oai_granularity";
  public static final String DEFAULT_FILTERING_METADATAPREFIX = "oai_lockss";

  public RecordFilteringOaiPmhCrawlSeed(CrawlerFacade cf) {
    super(cf);
    setMetadataPrefix(DEFAULT_FILTERING_METADATAPREFIX);
  }

  /**
   * Pulls needed params from the au pconfig
   * @param config
   * @throws PluginException
   * @throws ConfigurationException
   */
  @Override
  protected void populateFromConfig(Configuration config) 
      throws PluginException, ConfigurationException {
    super.populateFromConfig(config);
    if (config.containsKey(KEY_AU_OAI_DATE)) {
      parseRules(config.get(KEY_AU_OAI_DATE));
    } else if (config.containsKey(KEY_AU_OAI_FILTER_RULES)) {
      parseRules(config.get(KEY_AU_OAI_FILTER_RULES));
    } else if (usesDateRange) {
      parseRules(config.get(ConfigParamDescr.YEAR.getKey()));
    }
  }
  
  /**
   * Sets url for OAI query. Called to create service provider.
   * 
   * @param url
   * @return Context for OAI query
   */
  @Override
  protected Context buildContext(String url) {
    Context con = super.buildContext(url);
    con.withMetadataTransformer("oai_dc", KnownTransformer.OAI_DC);
    return con;
  }

  /**
   * parse the au provided filter rules and populate values
   * @param string
   * @throws ConfigurationException 
   */
  protected abstract void parseRules(String string) throws ConfigurationException;

  /**
   * Builds a param set for the OAI query
   * 
   * @param from
   * @param until
   * @param set
   * @param granularity
   * @param metadataPrefix
   * @return ListIdentifiersParameters
   */
  protected ListRecordsParameters buildParams() {
    ListRecordsParameters lip = ListRecordsParameters.request();
    lip.withMetadataPrefix(metadataPrefix);
    if (usesDateRange) {
      lip.withFrom(from);
      lip.withUntil(until);

      logger.debug3("Fei - buildParams from = " + from + ", until = " + until);
    }
    if (usesSet && !set.equals(NULL_SET)) {
      lip.withSetSpec(set);
    }
    if (usesGranularity) {
      lip.withGranularity("YYYY-MM-DD");
      logger.debug3("Fei - buildParams granularity set granularity");
    }
    return lip;
  }

  /**
   * Iterates through OAI response returning a Collection of the IDs
   * 
   * @param params
   * @return
   * @throws ConfigurationException
   */
  protected Collection<String> getRecordList(ListRecordsParameters params)
      throws ConfigurationException, IOException {
    try {
      Collection<String> idList = new ArrayList<String>();
      for (Iterator<Record> recIter = getServiceProvider().listRecords(params);
           recIter.hasNext();) {
        Record rec = recIter.next();
        MetadataSearch<String> metaSearch = 
            rec.getMetadata().getValue().searcher();
        if (checkMetaRules(metaSearch)) {
          idList.add(rec.getHeader().getIdentifier());
        }
      }
      return idList;
    } catch (BadArgumentException e) {
      throw new ConfigurationException("Incorrectly formatted OAI parameter", e);
    }
  }

  protected abstract boolean checkMetaRules(MetadataSearch<String> metaSearch);

  /**
   * Fetches OAI response and iterates through converting article IDs to atricle
   * URLs and returning the list.
   * 
   * @throws ConfigurationException
   */
  @Override
  public Collection<String> doGetStartUrls() throws ConfigurationException,
                                          PluginException, IOException {
    logger.debug3("Fei: doGetStartUrls...");
    return getRecordList(buildParams());
  }
}
