package org.lockss.plugin.pensoft.oai;

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
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.dspace.RecordFilteringOaiPmhCrawlSeed;
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


public class PensoftOaiCrawlSeed extends RecordFilteringOaiPmhCrawlSeed {
  public static final String DEFAULT_DATE_TAG = "dc.date";
  public static final String DEFAULT_IDENTIFIER_TAG = "dc.identifier";
  protected Collection<String> startUrls;
  protected int year;
  protected Pattern yearPattern = Pattern.compile("^([0-9]{4})$");
  public static final String OAI_DC_METADATA_PREFIX = "oai_dc";
  private static Logger logger =
	      Logger.getLogger(PensoftOaiCrawlSeed.class);

  public PensoftOaiCrawlSeed(CrawlerFacade cf) {
    super(cf);
    setMetadataPrefix(OAI_DC_METADATA_PREFIX);
    setUrlPostfix("oai.php");
  }
  
  @Override
  public Collection<String> doGetStartUrls() 
      throws ConfigurationException, PluginException, IOException{
      startUrls = super.doGetStartUrls();
    return startUrls;
  }
  
  @Override
  public Collection<String> doGetPermissionUrls() 
      throws ConfigurationException, PluginException, IOException {
    return au.getPermissionUrls();
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
			  if(value.startsWith(baseUrl)) {
				  logger.debug("To Follow: " + value);
				  return value;
			  }
		  }
	  }
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
            if(year == Integer.parseInt(subYear)) {
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
    return ids;
  }
}
