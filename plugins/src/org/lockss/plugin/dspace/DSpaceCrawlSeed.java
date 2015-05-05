package org.lockss.plugin.dspace;

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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.Interval;
import org.lockss.util.ListUtil;

import com.lyncode.xoai.services.api.MetadataSearch;


public class DSpaceCrawlSeed extends RecordFilteringOaiPmhCrawlSeed {
  public String DEFAULT_DATE_TAG = "dc.date.issued.none";
  protected Collection<String> startUrls;
  protected int year;
  protected Pattern yearPattern = Pattern.compile("([0-9]{4})");
  

  public DSpaceCrawlSeed(CrawlerFacade cf) {
    super(cf);
  }
  
  @Override
  public Collection<String> doGetStartUrls() 
      throws ConfigurationException, PluginException, IOException{
    if(startUrls == null) {
      startUrls = super.doGetStartUrls();
    }
    return startUrls;
  }
  
  @Override
  public Collection<String> doGetPermissionUrls() 
      throws ConfigurationException, PluginException, IOException {
    if(startUrls == null) {
      startUrls = super.doGetStartUrls();
    }
    if(permUrls == null && startUrls != null && !startUrls.isEmpty()) {
      List<String> tmpUrls = new ArrayList<String>(startUrls);
      Collections.sort(tmpUrls);
      permUrls = Arrays.asList(tmpUrls.get(0));
    }
    return permUrls;
  }

  @Override
  protected void parseRules(String rule) throws ConfigurationException {
    if(rule.length() == 4) {
      try {
        year = Integer.parseInt(rule);
      } catch(NumberFormatException ex) {
        throw new ConfigurationException("Must OAI date must be a 4 digit year");
      }
    } else {
      throw new ConfigurationException("Must OAI date must be a 4 digit year");
    }
    
  }

  @Override
  protected boolean checkMetaRules(MetadataSearch<String> metaSearch) {
    List<String> matchingTags;
    DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
    matchingTags = metaSearch.findAll(DEFAULT_DATE_TAG);
    if(matchingTags!= null && !matchingTags.isEmpty()) {
      for(String value : matchingTags) {
        try{
          String subYear;
          Matcher yearMatch = yearPattern.matcher(value);
          if(yearMatch.find()) {
            subYear = yearMatch.group();
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
}
