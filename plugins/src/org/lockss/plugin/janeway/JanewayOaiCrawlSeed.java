/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.janeway;

import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.serviceprovider.exceptions.BadArgumentException;
import org.dspace.xoai.serviceprovider.exceptions.InvalidOAIResponse;
import org.dspace.xoai.serviceprovider.model.Context;
import org.dspace.xoai.serviceprovider.model.Context.KnownTransformer;
import org.dspace.xoai.serviceprovider.parameters.ListRecordsParameters;
import org.dspace.xoai.services.api.MetadataSearch;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlData;
import org.lockss.util.CIProperties;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JanewayOaiCrawlSeed extends RecordFilteringOaiPmhCrawlSeed {
  public static final String DEFAULT_DATE_TAG = "dc.date";
  public static final String DEFAULT_IDENTIFIER_TAG = "dc.identifier";
  public static final String FULL_TEXT_URL_TAG = "dc.fullTextUrl";
  protected Collection<String> startUrls;
  protected int year;
  protected Pattern yearPattern = Pattern.compile("^([0-9]{4})-[0-9]{2}-[0-9]{2}");
  public static final String OAI_DC_METADATA_PREFIX = "oai_dc";
  private static Logger logger =
	      Logger.getLogger(JanewayOaiCrawlSeed.class);

  public JanewayOaiCrawlSeed(CrawlerFacade cf) {
    super(cf);
    setMetadataPrefix(OAI_DC_METADATA_PREFIX);
    String optional_journal_id = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());
    logger.debug3(" optional_journal_id " + optional_journal_id);
    if (optional_journal_id != null) {
        setUrlPostfix(optional_journal_id + "/api/oai/");
    } else {
        setUrlPostfix("api/oai/");
    }
  }

  @Override
  protected Context buildContext(String url) {
    Context con = super.buildContext(url);
    con.withMetadataTransformer(OAI_DC_METADATA_PREFIX, KnownTransformer.OAI_DC);
    return con;
  }
  
  @Override
  protected void initialize() 
      throws PluginException, ConfigurationException, IOException {
    super.initialize();
  }
  
  @Override
  protected Collection<String> getRecordList(ListRecordsParameters params)
		  throws ConfigurationException, IOException {
      logger.debug3("auid: " + au.getAuId() + ", encoded auid:" + UrlUtil.encodeUrl(au.getAuId()));

      String url = UrlUtil.encodeUrl(au.getAuId());

      String storeUrl = baseUrl + "auid=" + UrlUtil.encodeUrl(au.getAuId());

      logger.debug3("baseUrl = " + baseUrl + ", url = " + url + ", storeUrl = " + storeUrl);

      String link;
      Boolean error = false;
      Set<String> idSet = new HashSet<String>();
      try {
          Iterator<Record> recIter = getServiceProvider().listRecords(params);
          if (recIter != null) {
              while (recIter.hasNext()) {
                  Record rec = recIter.next();
                  if (rec != null) {
                      MetadataSearch<String> metaSearch =
                              rec.getMetadata().getValue().searcher();
                      logger.debug3(" - inside Try , inside for");
                      if (checkMetaRules(metaSearch)) {
                          link = findRecordArticleLink(rec);
                          if (link != null) {
                              logger.debug3(" - link = %s" + link);
                              idSet.add(link);
                          } else {
                              logger.debug3(" - empty link");
                          }
                      }
                  } else {
                      logger.debug3(" - recIter is not null, but rec is null");
                  }
              }
          }
      } catch (InvalidOAIResponse e) {
    	  if(e.getCause() != null && e.getCause().getMessage().contains("LOCKSS")) {
    		  error = true;
    		  logger.debug3("OAI result errored due to LOCKSS audit proxy. Trying alternate start Url", e);
    	  } else {
    		  throw e;
    	  }
      } catch (BadArgumentException e) {
    	  throw new ConfigurationException("Incorrectly formatted OAI parameter", e);
      }  catch (Exception e) {
          logger.debug("Unexpected exception when trying to getRecordList");
      }
      
      List<String> idList = new ArrayList<String>();
	  if(error) {
		  idList.add(storeUrl);
	  } else if(!idSet.isEmpty()) {
		  idList.addAll(idSet);
		  Collections.sort(idList);
		  storeStartUrls(idList, storeUrl);
	  }
	  return idList;
  }
  
  protected void storeStartUrls(Collection<String> urlList, String url) throws IOException {
	  StringBuilder sb = new StringBuilder();
	  sb.append("<html>\n");
	  for (String u : urlList) {
		  sb.append("<a href=\"" + u + "\">" + u + "</a><br/>\n");
          logger.debug3(" storeStartUrl = %s" + u);
	  }
	  sb.append("</html>");
	  CIProperties headers = new CIProperties();
	  //Should use a constant here
	  headers.setProperty("content-type", "text/html; charset=utf-8");
      UrlData ud = new UrlData(new ByteArrayInputStream(sb.toString().getBytes(Constants.ENCODING_UTF_8)), headers, url);
      UrlCacher cacher = facade.makeUrlCacher(ud);
      cacher.storeContent();
  }
  
  protected String findRecordArticleLink(Record rec) { 
    MetadataSearch<String> recSearcher = rec.getMetadata().getValue().searcher();
    List<String> idTags = recSearcher.findAll(FULL_TEXT_URL_TAG);
    if(idTags != null && !idTags.isEmpty()) {
      for(String value : idTags) {
          logger.debug3(" idTag is not null, its value = " + value);
        if (value.startsWith(baseUrl)) {
          return value;
        }
      }
    }
    logger.debug3(" idTag is null");
    return null;
  }
  
  
  @Override
  protected void parseRules(String rule) throws ConfigurationException {
    if(rule.length() == 4) {
      try {
        year = Integer.parseInt(rule);
      } catch(NumberFormatException ex) {
        throw new ConfigurationException("OAI date must be a 4 digit year");
      }
    } else {
      throw new ConfigurationException("OAI date must be a 4 digit year");
    }
    
  }

  @Override
  protected boolean checkMetaRules(MetadataSearch<String> metaSearch) {
    List<String> matchingTags;
    matchingTags = metaSearch.findAll(DEFAULT_DATE_TAG);
    if(matchingTags!= null && !matchingTags.isEmpty()) {
      for(String value : matchingTags) {
        try{
          String subYear;
          Matcher yearMatch = yearPattern.matcher(value);
          if(yearMatch.find()) {
            subYear = yearMatch.group(1);
            logger.debug3(" subYear = " + subYear + " value = " + value + ", expected year = " + year);
            if(year == Integer.parseInt(subYear)) {
                logger.debug3(" subYear = " + subYear + " value = " + value + " === expected year = " + year);
              return true;
            }
            return false;
          }
        } catch(NumberFormatException|IllegalStateException ex) {
            logger.debug3(" yearPattern match does not expectation");
        }
      }
    } else if (matchingTags!= null) {
        logger.debug3(" matchingTags is not null, checkMetaRules metaSearch = " + metaSearch);
        for(String value : matchingTags) {
            logger.debug3(" checkMetaRules metaSearch value = " + value);
        }
    } else if (matchingTags == null) {
        logger.debug3(" matchingTags is NULL, checkMetaRules metaSearch = " + metaSearch);
    }
    return false;
  }
}
