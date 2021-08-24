/*
 * $Id:$
 */

/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ojs3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.lockss.crawler.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.Logger;

/**
 * <p>
 * A custom crawl seed factory -it adds an optional attr "start_stem" to the path
 * defined as a start_url in the plugin 
 * https://foo.com/<journal_id>/gateway/clockss?year=1234
 * becomes
 * https://foo.com/<start_stem>/<journal_id>/gateway/clockss?year=1234
 * and if there is no start_stem then just stays
 * https://foo.com/<journal_id>/gateway/clockss?year=1234
 * </p>
 * 
 * @since 1.67.5
 */
public class Ojs3CrawlSeedFactory implements CrawlSeedFactory {
	private static final String START_STEM_ATTRIBUTE = "start_stem";
  
  private static final Logger log = Logger.getLogger(Ojs3CrawlSeedFactory.class);
  
  public static class AddStemCrawlSeed extends BaseCrawlSeed {
    
    public AddStemCrawlSeed(CrawlerFacade crawlerFacade) {
      super(crawlerFacade);
    }

    @Override
    public Collection<String> doGetPermissionUrls() throws ConfigurationException,
        PluginException, IOException {
      return addStartStem(super.doGetPermissionUrls());
    }    

	@Override
    public Collection<String> doGetStartUrls() throws ConfigurationException,
        PluginException, IOException {
      return addStartStem(super.doGetStartUrls());
    }

	private Collection<String> addStartStem(Collection<String> origUrls) {
       String start_stem = AuUtil.getTitleAttribute(super.au,START_STEM_ATTRIBUTE);
       log.debug3("In addStartStem. start_stem = " + start_stem);
       if(start_stem == null) {
    	   log.debug3("No start_stem attributes, start_url is at represented in the plugin");
    	   return origUrls;
       }
       String baseUrl = (super.au).getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
       int afterBaseUrl = baseUrl.length(); // will be first index after end of base url
       Collection<String> newUrls = new ArrayList<String>(origUrls.size());
       for (Iterator<String> iter = origUrls.iterator(); iter.hasNext();) {
         String url = iter.next();
         // insert start_stem between baseUrl and anything that follows
         // There are cases "http" and "https" urls are embedded in the same html source
         if (url.replace("https://", "").replace("http://", "").startsWith(
                 baseUrl.replace("https://", "").replace("http://", ""))) {
        	 StringBuilder sb = new StringBuilder(baseUrl);
        	 sb.append(start_stem);
        	 sb.append(url.substring(afterBaseUrl));
        	 newUrls.add(sb.toString());
         } else {
        	 // if it doesn't start with start_url just leave it alone, not that this is currently happening
        	 newUrls.add(url);
         }
       }
       if (log.isDebug3()) {
    	   for(Iterator<String> debIt = newUrls.iterator(); debIt.hasNext();) {
    		   String url = debIt.next();
    		   log.debug3("start url: " +  url);
    	   }
       }
       return newUrls;
	}

  }
  
  @Override
  public CrawlSeed createCrawlSeed(CrawlerFacade facade) {
	  return new AddStemCrawlSeed(facade);
  }

}
