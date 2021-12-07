/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.pensoft.oai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.serviceprovider.exceptions.BadArgumentException;
import org.dspace.xoai.serviceprovider.model.Context;
import org.dspace.xoai.serviceprovider.model.Context.KnownTransformer;
import org.dspace.xoai.serviceprovider.parameters.ListRecordsParameters;
import org.dspace.xoai.services.api.MetadataSearch;
import org.lockss.config.Configuration;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.Logger;

public abstract class RecordFilteringOaiPmhCrawlSeed extends BaseOaiPmhCrawlSeed {
  private static Logger logger =
      Logger.getLogger(RecordFilteringOaiPmhCrawlSeed.class);

  protected boolean usesDateRange = true;
  protected boolean usesSet = true;
  protected Map<String, Pattern> metadataRules;
  public static final String KEY_AU_OAI_FILTER_RULES = "au_oai_filter_rules";
  public static final String KEY_AU_OAI_DATE = "au_oai_date";
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
//    con.withMetadataTransformer("oai_dc", KnownTransformer.OAI_DC);
    con.withMetadataTransformer("oai_lockss", KnownTransformer.OAI_DC);
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
   * @param metadataPrefix
   * @return ListIdentifiersParameters
   */
  protected ListRecordsParameters buildParams() {
    ListRecordsParameters lip = ListRecordsParameters.request();
    lip.withMetadataPrefix(metadataPrefix);
    if (usesDateRange) {
      lip.withFrom(from);
      lip.withUntil(until);
    }
    if (usesSet && !set.equals(NULL_SET)) {
      lip.withSetSpec(set);
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
    return idsToUrls(getRecordList(buildParams()));
  }

  /**
   * Override this to provide different logic to convert OAI PMH ids to
   * corresponding article urls
   * 
   * @param id
   * @param url
   * @return
   */
  public Collection<String> idsToUrls(Collection<String> ids) {
    Collection<String> urlList = new ArrayList<String>();
    for (String id : ids) {
      if (id.contains(":") && !id.endsWith(":")) {
        if(permUrls.isEmpty()) {
          permUrls.add(baseUrl + oaiUrlPostfix + "?verb=GetRecord&identifier=" + 
              id + "&metadataPrefix=" + metadataPrefix);
        }
        String id_num = id.substring(id.lastIndexOf(':') + 1);
        urlList.add(baseUrl + "handle/" + id_num);
      }
    }
    return urlList;
  }
}
