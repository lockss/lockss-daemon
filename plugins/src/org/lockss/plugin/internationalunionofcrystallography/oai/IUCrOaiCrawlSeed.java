/*

Copyright (c) 2000-2020 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.internationalunionofcrystallography.oai;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.serviceprovider.exceptions.BadArgumentException;
import org.dspace.xoai.serviceprovider.exceptions.InvalidOAIResponse;
import org.dspace.xoai.serviceprovider.model.Context;
import org.dspace.xoai.serviceprovider.model.Context.KnownTransformer;
import org.dspace.xoai.serviceprovider.parameters.ListRecordsParameters;
import org.dspace.xoai.services.api.MetadataSearch;
import org.lockss.config.Configuration;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlData;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.CIProperties;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

public class IUCrOaiCrawlSeed extends RecordFilteringOaiPmhCrawlSeed {
  public static final String DEFAULT_DATE_TAG = "dc.date";
  public static final String DEFAULT_IDENTIFIER_TAG = "dc.identifier";
  public static final String START_URL_PREFIX = "cgi-bin/paper?";
  protected Collection<String> startUrls;
  protected String yearMonth;
  protected Pattern yearPattern = Pattern.compile("^([0-9]{4}-[0-9]{2})-[0-9]{2}$");
  protected Pattern idPattern = Pattern.compile("^http://dx.doi.org/[^/]+/([^/]+)$");
  public static final String OAI_DC_METADATA_PREFIX = "oai_dc";
  private static Logger logger = Logger.getLogger(IUCrOaiCrawlSeed.class);
  private boolean error = false;

  public IUCrOaiCrawlSeed(CrawlerFacade cf) {
    super(cf);
    setMetadataPrefix(OAI_DC_METADATA_PREFIX);
    setUrlPostfix("cgi-bin/oai");
  }
  
  protected void populateFromConfig(Configuration config) 
      throws PluginException, ConfigurationException {
    super.populateFromConfig(config);
    this.baseUrl = config.get("script_url");
    
    if (usesSet) {
      this.setGranularity(DATE_FORMAT.toUpperCase());
      SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
      Date convertedDate = null;
      String nd = yearMonth + "-01";
      try {
        convertedDate = dateFormat.parse(nd);
      } catch (ParseException e) {
        e.printStackTrace();
        throw new ConfigurationException("Invalid yearMonth: " + yearMonth, e);
      }
      Calendar c = Calendar.getInstance();
      c.setTime(convertedDate);
      int lday = c.getActualMaximum(Calendar.DAY_OF_MONTH);
      String sday = String.valueOf(lday);
      setDates(yearMonth + "-01", yearMonth + "-" + sday);
    }
  }

  @Override
  protected Context buildContext(String url) {
    Context con = super.buildContext(url);
    con.withMetadataTransformer(OAI_DC_METADATA_PREFIX, KnownTransformer.OAI_DC);
    return con;
  }
  
  @Override
  protected Collection<String> getRecordList(ListRecordsParameters params)
	      throws ConfigurationException, IOException {
	    try {
	      String link;
	      int recNum = -1;
	      String pageRecordLink = "";
	      HashSet<String> idSet = new HashSet<String>();
	      for (Iterator<Record> recIter = getServiceProvider().listRecords(params);
	           recIter.hasNext();) {
	        Record rec = recIter.next();
	        recNum = recNum + 1;
	        
	        link = findRecordArticleLink(rec);
	        if(!link.equals(pageRecordLink)) {
		        MetadataSearch<String> metaSearch = 
		            rec.getMetadata().getValue().searcher();
		        if (checkMetaRules(metaSearch)) {
		        	if(link != null) {
		        		idSet.add(link);
		        	}
		        }
	        } else {
	        	return idSet;
	        }
	        
	        if(recNum == 501 || recNum == 0) {
	        	pageRecordLink = link;
	        	recNum = 0;
	        }
	      }
	      return idSet;
	    } catch (InvalidOAIResponse e) {
	    	  if(e.getCause() != null && e.getCause().getMessage().contains("LOCKSS")) {
	    		  error = true;
	    		  logger.debug("OAI result errored due to LOCKSS audit proxy. Trying alternate start Url", e);
	    		  return null;
	    	  } else {
	    		  throw new IOException(e);
	    	  }
	    } catch (BadArgumentException e) {
	      throw new ConfigurationException("Incorrectly formatted OAI parameter", e);
        } catch(Exception e) {
          //wasn't a correctly formatted date, so we ignore it
          //log here
          logger.siteWarning("Unexpected exception", e);
          throw new IOException(e);
	    }
	  }
  
  protected void storeStartUrls(Collection<String> urlList, String url) throws IOException {
	  StringBuilder sb = new StringBuilder();
	  sb.append("<html>\n");
	  for (String u : urlList) {
		  sb.append("<a href=\"" + u + "\">" + u + "</a><br/>\n");
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
	  List<String> idTags = recSearcher.findAll(DEFAULT_IDENTIFIER_TAG);
	  if(idTags != null && !idTags.isEmpty()) {
		  for(String value : idTags) {
			  if(value.startsWith("http://dx.doi.org/")) {
				  return value;
			  }
		  }
	  }
	  return null;
  }
  
  
  @Override
  protected void parseRules(String rule) throws ConfigurationException {
    if(rule.length() == 7) {
      try {
        yearMonth = rule;
      } catch(NumberFormatException ex) {
        throw new ConfigurationException("OAI date must be in format yyyy-mm");
      }
    } else {
      throw new ConfigurationException("OAI date must be in format yyyy-mm");
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
            if(yearMonth.contentEquals(subYear)) {
              return true;
            }
          }
        } catch(NumberFormatException|IllegalStateException|InvalidOAIResponse ex) {
          //wasn't a correctly formatted date, so we ignore it
          //log here
          logger.siteWarning("Ignoring this record", ex);
        } catch(Exception e) {
          //wasn't a correctly formatted date, so we ignore it
          //log here
          logger.siteWarning("Unexpected exception", e);
        }
      }
    }
    return false;
  }
  
  /**
   * Override this to provide different logic to convert OAI PMH ids to
   * corresponding article urls
   * 
   * @param id
   * @param url
   * @return
 * @throws IOException 
   */
  public Collection<String> idsToUrls(Collection<String> ids) throws IOException {
	String storeUrl = baseUrl + "auid=" + UrlUtil.encodeUrl(au.getAuId());
	List<String> urlList = new ArrayList<String>();
	if(error) {
		  urlList.add(storeUrl);
	  } else if(!ids.isEmpty()) {
			for (String id : ids) {
				Matcher idMatch = idPattern.matcher(id);
				if(idMatch.find()) {
					urlList.add(baseUrl + START_URL_PREFIX + idMatch.group(1));
				}
			}
		  Collections.sort(urlList);
		  storeStartUrls(urlList, storeUrl);
	  }
	return urlList;
  }
}
