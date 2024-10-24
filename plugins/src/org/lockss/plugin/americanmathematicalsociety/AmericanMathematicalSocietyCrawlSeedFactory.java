/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.americanmathematicalsociety;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.Logger;

public class AmericanMathematicalSocietyCrawlSeedFactory implements CrawlSeedFactory {

    private static final Logger log = Logger.getLogger(AmericanMathematicalSocietyCrawlSeedFactory.class);
    private static final Pattern YEAR_PAT = Pattern.compile("/year/", Pattern.CASE_INSENSITIVE);
    private static final String SPECIAL_STR = "/";

    @Override
    public CrawlSeed createCrawlSeed(CrawlerFacade facade) {
        String collectionID = facade.getAu().getConfiguration().get("collection_id");
        log.debug3("the collectionID is " + collectionID);
        if (collectionID != null) {
          //the "special" books for AMS are the ones with collection ids of chel and simon
          //add whatever new special books come up (ones that are categorized by volume instead of year)
            if (!collectionID.equals("chel") && !collectionID.equals("simon")) {
                return new BaseCrawlSeed(facade);
            }
            else {
                return new AMSSpecialBookCrawlSeed(facade);
            }
        }
        return new BaseCrawlSeed(facade);
    }

    public static class AMSSpecialBookCrawlSeed extends BaseCrawlSeed {
    
    public AMSSpecialBookCrawlSeed(CrawlerFacade crawlerFacade) {
      super(crawlerFacade);
    }
    
    @Override
    public Collection<String> doGetStartUrls() throws ConfigurationException, PluginException, IOException {
      
        Collection<String> sUrls = super.doGetStartUrls();
        Collection<String> uUrls = new ArrayList<String>(sUrls.size());
        for (Iterator<String> iter = sUrls.iterator(); iter.hasNext();) {
            String sUrl = iter.next();
            Matcher urlMat = YEAR_PAT.matcher(sUrl); 
            if (urlMat.find()) {
              sUrl = urlMat.replaceFirst(SPECIAL_STR);
              if (log.isDebug2()) {
                log.debug2(sUrl);
              }
            }
            uUrls.add(sUrl);
          }
          return uUrls;
    }
    
  }
    
}
