/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.swjpcc;

import java.io.*;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlData;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.state.AuState;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;

/**
 * <p>
 * A crawl seed that starts from a static list of many articles and creates a TOC from this
 * </p>
 * 
 * @since 1.67.5
 * @see https://dev.springer.com/
 */
public class SwjpccCrawlSeed extends BaseCrawlSeed {
  
  protected static final String RESOURCE_FILE = "seed_urls.txt";
  protected List<String> urlList;
  protected String baseUrl;
  private static final Logger log = Logger.getLogger(SwjpccCrawlSeed.class);
  protected String year;
  protected CrawlerFacade facade;  

  /**
   * <p>
   * Builds a new crawl seed with the given crawler façade.
   * </p>
   * 
   * @param facade
   *          A crawler façade for this crawl seed.
   * @since 1.67.5
   */
  public SwjpccCrawlSeed(CrawlerFacade facade) {
    super(facade);
    if (au == null) {
      throw new IllegalArgumentException("Valid archival unit required for crawl seed");
    }
    this.facade = facade;
  }    

  @Override
  protected void initialize() 
      throws ConfigurationException ,PluginException ,IOException {
    this.baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    this.urlList = null;
    this.year = au.getConfiguration().get(ConfigParamDescr.YEAR.getKey());
    this.urlList = null;
  }

  @Override
  public Collection<String> doGetStartUrls() throws PluginException, IOException {
    if (urlList == null) {
      populateUrlList();
    }
    if (urlList.isEmpty()) {
      throw new CacheException.UnexpectedNoRetryFailException("Found no start urls");
    }
    return urlList;
  }
  
  protected void populateUrlList() throws IOException {
    AuState aus = AuUtil.getAuState(au);
    InputStream is = null;
    BufferedReader br = null;
    urlList = new ArrayList<String>();
    try {
      is = this.getClass().getResourceAsStream("seed_urls.txt");
      //is = SwjpccCrawlSeed.class.getResourceAsStream("seed_urls.txt");
      if (is == null) {
        throw new ExceptionInInitializerError("Plugin external not found");
      }
      br = new BufferedReader(new InputStreamReader(is, Constants.ENCODING_US_ASCII));
      String next_url = null;
      while ((next_url = br.readLine()) != null) {
        next_url = next_url.trim();
        log.debug3("next url: " + next_url);      
        urlList.add(next_url);
      }
    }
    catch (IOException ioe) {
      ExceptionInInitializerError eiie = new ExceptionInInitializerError("Error reading plugin external");
      eiie.initCause(ioe);
      throw eiie;
    }
    finally {
      IOUtils.closeQuietly(br);
      IOUtils.closeQuietly(is);
    }
    if (urlList.isEmpty()) {
      throw new ExceptionInInitializerError("Plugin external not loaded");
    }

    String storeUrl = baseUrl + "auid=" + UrlUtil.encodeUrl(au.getAuId());
    storeStartUrls(urlList, storeUrl);
    log.debug2(String.format("Ending with %d URLs", urlList.size()));
    if (log.isDebug3()) {
            log.debug3("Start URLs: " + urlList.toString());
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

  /**                                                                                                                                                           
   * All URLs are start urls so don't fail on error
   * @return false                                                                                                                                              
   */
  @Override
  public boolean isFailOnStartUrlError() {
    return false;
  }



}
