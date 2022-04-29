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

package org.lockss.plugin.springer.link;

import java.io.*;
import java.util.*;

import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.*;

/**
 * <p>
 * A crawl seed that queries Springer's Meta API to enumerate article metadata
 * and synthesize start URLs for crawls.
 * </p>
 * <p>
 * Note that this is the newer Meta API, not the older Metadata API.
 * </p>
 * 
 * @since 1.67.5
 * @see https://dev.springer.com/
 */
public class SpringerLinkJournalsCrawlSeed extends BaseSpringerLinkCrawlSeed {

  /**
   * <p>
   * A logger for this class.
   * </p>
   * 
   * @since 1.67.5
   */
  private static final Logger log = Logger.getLogger(SpringerLinkJournalsCrawlSeed.class);
  
  /**
   * <p>
   * The journal ISSN (<code>journal_issn</code>) of this crawl seed's AU.
   * </p>
   * 
   * @since 1.67.5
   */
  protected String eissn;
  
  public static final String JOURNAL_EISSN_KEY = "journal_eissn";
  /**
   * <p>
   * The volume name (<code>volume_name</code>) of this crawl seed's AU.
   * </p>
   * 
   * @since 1.67.5
   */
  protected String volume;

  /**
   * <p>
   * Builds a new crawl seed with the given crawler façade.
   * </p>
   * 
   * @param facade
   *          A crawler façade for this crawl seed.
   * @since 1.67.5
   */
  public SpringerLinkJournalsCrawlSeed(CrawlerFacade facade) {
    super(facade);
  }

  @Override
  protected void initialize() 
      throws ConfigurationException ,PluginException ,IOException {
    super.initialize();
    this.eissn = au.getConfiguration().get(JOURNAL_EISSN_KEY);
    this.volume = au.getConfiguration().get(ConfigParamDescr.VOLUME_NAME.getKey());
    this.urlList = null;
  }

  /**
   * <p>
   * Assembles the query URL for a given starting index.
   * </p>
   * 
   * @param startingIndex
   *          A starting index (starts at 1).
   * @return The query URL for the given starting index.
   * @since 1.67.5
   */
  protected String makeApiUrl(int startingIndex) throws PluginException {
    String url = String.format("%smeta/v1/pam?q=issn:%s%%20volume:%s&api_key=%s&p=%d&s=%d",
                               API_URL,
                               eissn,
                               volume,
                               getApiKey(),
                               EXPECTED_RECORDS_PER_RESPONSE,
                               startingIndex);
    return url;
  }
/*
 * (non-Javadoc)
 * encode the doi so that ":" and similar in the doi portion of the URL are
 * consitent with the href links within the article pages
 */
  @Override
  protected List<String> convertDoisToUrls(Collection<String> dois) {
    List<String> urls = new ArrayList<String>();
    for(String doi:dois) {
      // Encode the doi, then revert the FIRST %2F (slash) back to a "/":
      // 10.1023/A%3A1026541510549, not
      // 10.1023%2FA%3A1026541510549
      String url = String.format("%sarticle/%s", baseUrl, encodeDoi(doi).replaceFirst("%2F","/"));
      urls.add(url);
    }
    return urls;
  }
}
