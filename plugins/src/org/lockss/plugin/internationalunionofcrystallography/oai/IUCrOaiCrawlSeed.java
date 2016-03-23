package org.lockss.plugin.internationalunionofcrystallography.oai;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.Interval;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;

import com.lyncode.xoai.model.oaipmh.Record;
import com.lyncode.xoai.model.xoai.Element;
import com.lyncode.xoai.model.xoai.Field;
import com.lyncode.xoai.serviceprovider.exceptions.BadArgumentException;
import com.lyncode.xoai.serviceprovider.model.Context;
import com.lyncode.xoai.serviceprovider.model.Context.KnownTransformer;
import com.lyncode.xoai.serviceprovider.parameters.ListRecordsParameters;
import com.lyncode.xoai.services.api.MetadataSearch;


public class IUCrOaiCrawlSeed extends RecordFilteringOaiPmhCrawlSeed {
  public static final String DEFAULT_DATE_TAG = "dc.date";
  public static final String DEFAULT_IDENTIFIER_TAG = "dc.identifier";
  public static final String START_URL_PREFIX = "cgi-bin/paper?";
  protected Collection<String> startUrls;
  protected String yearMonth;
  protected Pattern yearPattern = Pattern.compile("^([0-9]{4}-[0-9]{2})-[0-9]{2}$");
  protected Pattern idPattern = Pattern.compile("^http://dx.doi.org/[^/]+/([^/]+)$");
  public static final String OAI_DC_METADATA_PREFIX = "oai_dc";
  private static Logger logger =
	      Logger.getLogger(IUCrOaiCrawlSeed.class);

  public IUCrOaiCrawlSeed(CrawlerFacade cf) {
    super(cf);
    setMetadataPrefix(OAI_DC_METADATA_PREFIX);
    setUrlPostfix("cgi-bin/oai");
  }
  
  protected void populateFromConfig(Configuration config) 
	      throws PluginException, ConfigurationException {
	  super.populateFromConfig(config);
	  this.baseUrl = config.get("script_url");
  }

  @Override
  protected Context buildContext(String url) {
    Context con = super.buildContext(url);
    con.withMetadataTransformer(OAI_DC_METADATA_PREFIX, KnownTransformer.OAI_DC);
    return con;
  }
  
  @Override
  protected Collection<String> getRecordList(ListRecordsParameters params)
	      throws ConfigurationException {
	    try {
	      String link;
	      Collection<String> idList = new ArrayList<String>();
	      for (Iterator<Record> recIter = getServiceProvider().listRecords(params);
	           recIter.hasNext();) {
	        Record rec = recIter.next();
	        MetadataSearch<String> metaSearch = 
	            rec.getMetadata().getValue().searcher();
	        if (checkMetaRules(metaSearch)) {
	        	link = findRecordArticleLink(rec);
	        	if(link != null) {
	        		idList.add(link);
	        	}
	        }
	      }
	      return idList;
	    } catch (BadArgumentException e) {
	      throw new ConfigurationException("Incorrectly formatted OAI parameter", e);
	    }
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
        } catch(NumberFormatException|IllegalStateException ex) {
          //wasn't a correctly formatted date, so we ignore it
          //log here
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
   */
  public Collection<String> idsToUrls(Collection<String> ids) {
	Collection<String> urlList = new ArrayList<String>();
	for (String id : ids) {
		Matcher idMatch = idPattern.matcher(id);
		if(idMatch.find()) {
			urlList.add(baseUrl + START_URL_PREFIX + idMatch.group(1));
		}
	}
	return urlList;
  }
}
