/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.michigan.deepblue;

import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.serviceprovider.exceptions.BadArgumentException;
import org.dspace.xoai.serviceprovider.exceptions.InvalidOAIResponse;
import org.dspace.xoai.serviceprovider.model.Context;
import org.dspace.xoai.serviceprovider.model.Context.KnownTransformer;
import org.dspace.xoai.serviceprovider.parameters.ListRecordsParameters;
import org.dspace.xoai.services.api.MetadataSearch;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlData;
import org.lockss.plugin.pensoft.oai.RecordFilteringOaiPmhCrawlSeed;
import org.lockss.util.CIProperties;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeepBlueOaiCrawlSeed extends RecordFilteringOaiPmhCrawlSeed {
  public static final String DEFAULT_DATE_TAG = "dc.date";
  public static final String DEFAULT_IDENTIFIER_TAG = "dc.identifier";
  protected Collection<String> startUrls;
  protected int year;
  protected Pattern yearPattern = Pattern.compile("^([0-9]{4})$");
  public static final String OAI_DC_METADATA_PREFIX = "oai_dc";
  private static Logger logger =
	      Logger.getLogger(DeepBlueOaiCrawlSeed.class);

  /*
  Sample record:
  https://deepblue.lib.umich.edu/dspace-oai/request?verb=ListRecords&metadataPrefix=oai_dc&set=col_2027.42_41251&from=2020-01-01&until=2020-12-31
      <oai_dc:dc xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
       <dc:title>New Specimens of the Late Eocene Turtle Cordichelys (Pleurodira: Podocnemididae) From Wadi Al Hitan and Qasr El-Sagha in the Fayum Province of Eqypt</dc:title>
       <dc:creator>Cherney, Michael D</dc:creator>
       <dc:creator>Wilson Mantilla, Jeffrey A</dc:creator>
       <dc:creator>Gingerich, Philip D.</dc:creator>
       <dc:creator>Zalmout, Iyad</dc:creator>
       <dc:creator>Antar, Mohammed Sameh M.</dc:creator>
       <dc:contributor>Saudi Geological Survey, Sedimentary Rocks and Palaeontology Department, Jeddah, Saudi Arabia</dc:contributor>
       <dc:contributor>Egyptian Environmental Affairs Agency, Wadi Al Hitan World Heritage Site, Fayum, Egypt</dc:contributor>
       <dc:contributor>Ann Arbor</dc:contributor>
       <dc:subject>Geology and Earth Sciences</dc:subject>
       <dc:subject>Anthropology and Archaeology</dc:subject>
       <dc:subject>Science</dc:subject>
       <dc:subject>Social Sciences</dc:subject>
       <dc:description>http://deepblue.lib.umich.edu/bitstream/2027.42/163364/2/ContributionsVol33No2_High_Res.pdf</dc:description>
       <dc:description>http://deepblue.lib.umich.edu/bitstream/2027.42/163364/1/ContributionsVol33No2_Lo_Res.pdf</dc:description>
       <dc:date>2020-11-03T15:38:14Z</dc:date>
       <dc:date>2020-11-03T15:38:14Z</dc:date>
       <dc:date>2020-11-02</dc:date>
       <dc:type>Other</dc:type>
       <dc:identifier>Vol 33, No2</dc:identifier>
       <dc:identifier>http://hdl.handle.net/2027.42/163364</dc:identifier>
       <dc:relation>Contributions</dc:relation>
       <dc:format>29-64</dc:format>
       <dc:format>application/pdf</dc:format>
       <dc:format>application/pdf</dc:format>
       <dc:publisher>Museum of Paleontology, The University of Michigan</dc:publisher>
    </oai_dc:dc>
   */

  public DeepBlueOaiCrawlSeed(CrawlerFacade cf) {
    super(cf);
    setMetadataPrefix(OAI_DC_METADATA_PREFIX);
    setUrlPostfix("dspace-oai/request");
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
    if(UrlUtil.isHttpUrl(baseUrl)) {
      baseUrl = UrlUtil.replaceScheme(baseUrl, "http", "https");
    }
    
  }

  /*
   * Here's an example of an OAI request of DeepBlue:
   * https://deepblue.lib.umich.edu/dspace-oai/request?verb=ListRecords&metadataPrefix=oai_dc&set=col_2027.42_41251&from=2020-01-01&until=2020-12-31
   */
  
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
	      for (Iterator<Record> recIter = getServiceProvider().listRecords(params);
	           recIter.hasNext();) {
	        Record rec = recIter.next();
            if (rec == null) {
                logger.debug3("Rec is null");
            }
	        MetadataSearch<String> metaSearch = 
	            rec.getMetadata().getValue().searcher();
	        if (checkMetaRules(metaSearch)) {
                logger.debug3(" -  checkMetaRules passed");
	        	link = findRecordArticleLink(rec);
                if (link != null) {
                    logger.debug3(" - link = " + link);
                    /*
                    replace what's in identifier with url from base_url
                    <dc:identifier>https://hdl.handle.net/2027.42/48172</dc:identifier>
                    https://deepblue.lib.umich.edu/handle/2027.42/48172
                     */

                    if (link.contains("http://hdl.handle.net/") || 	link.contains("https://hdl.handle.net/")) {
                        String replaced_link = link.replace("http://hdl.handle.net/",baseUrl + "handle/").replace("https://hdl.handle.net/",baseUrl + "handle/");
                        logger.debug3(" - link = " + link + ", replaced_link = " + replaced_link);
                        idSet.add(replaced_link);
                    }
                } else {
                    logger.debug3(" - empty link");
                }
	        } else {
                logger.debug3(" - checkMetaRules failed");
            }
	      }
      } catch (InvalidOAIResponse e) {
    	  if(e.getCause() != null && e.getCause().getMessage().contains("LOCKSS")) {
    		  error = true;
    		  logger.debug("OAI result errored due to LOCKSS audit proxy. Trying alternate start Url", e);
    	  } else {
    		  throw e;
    	  }
      } catch (BadArgumentException e) {
    	  throw new ConfigurationException("Incorrectly formatted OAI parameter", e);
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
          logger.debug3("storeStartUrl = " + u);
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
        if (value.startsWith("http")) {
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
                      logger.debug3(" subYear = " + subYear + " value = " + value + ", expected year = " + year);
                      if(year == Integer.parseInt(subYear)) {
                          logger.debug3(" subYear = " + subYear + " value = " + value + " === expected year = " + year);
                          return true;
                      }
                      return true;
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
      return true;
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
  
  @Override
  public boolean isFailOnStartUrlError() {
    return false;
  }
  
}
