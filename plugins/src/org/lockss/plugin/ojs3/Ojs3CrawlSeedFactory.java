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
import java.util.Collection;

import org.lockss.crawler.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;

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

  public static class AddStemCrawlSeed extends BaseCrawlSeed {
    
    public AddStemCrawlSeed(CrawlerFacade crawlerFacade) {
      super(crawlerFacade);
    }

    @Override
    public Collection<String> doGetPermissionUrls() throws ConfigurationException,
        PluginException, IOException {
      return Ojs3StartStemHelper.addStartStem(au, super.doGetPermissionUrls());
    }    

	@Override
    public Collection<String> doGetStartUrls() throws ConfigurationException,
        PluginException, IOException {
      return Ojs3StartStemHelper.addStartStem(au, super.doGetStartUrls());
    }
  }
  
  @Override
  public CrawlSeed createCrawlSeed(CrawlerFacade facade) {
	  return new AddStemCrawlSeed(facade);
  }
}
