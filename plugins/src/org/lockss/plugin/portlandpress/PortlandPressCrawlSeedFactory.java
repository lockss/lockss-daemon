/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.portlandpress;

import java.io.IOException;
import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;

/**
 * @since 1.67.5
 */
public class PortlandPressCrawlSeedFactory implements CrawlSeedFactory {

  /* Currently these are three JIDs that warrant a special start URL */
  protected static final String WP_JID = "wp";
  protected static final String WS_JID = "ws";
  protected static final String WST_JID = "wst";
  protected static final Set<String> SPECIAL_JIDS =
      new HashSet<String>(Arrays.asList(WP_JID, WST_JID, WS_JID));

  protected Collection<String> urls;

  @Override
  public CrawlSeed createCrawlSeed(CrawlerFacade facade) {
    return new PortlandPressCrawlSeed(facade);
  }

  public class PortlandPressCrawlSeed extends BaseCrawlSeed {

    public PortlandPressCrawlSeed(CrawlerFacade facade) {
      super(facade);
    }
    
    protected void initialize()
        throws ConfigurationException, PluginException, IOException {
      super.initialize();
      Configuration config = au.getConfiguration();
      if (config == null) {
        throw new PluginException("Null configuration, can't get start URL");
      }
      String base_url = config.get(ConfigParamDescr.BASE_URL.getKey());
      String jid = config.get(ConfigParamDescr.JOURNAL_ID.getKey());
      String vol = config.get(ConfigParamDescr.VOLUME_NAME.getKey());
      if (base_url == null || jid == null || vol == null) {
        throw new PluginException.InvalidDefinition(
            "Crawl seed cannot set a start URL based on the params");
      }
      /* 
       * Most start URLs are:    <base>/<jid>/<volume>/lockss.htm
       * For special JIDs, it's: <base>/<jid>/<volume>01/lockss.htm
       */
      String url = String.format("%s%s/%s%s/lockss.htm",
                                 base_url,
                                 jid,
                                 vol,
                                 (SPECIAL_JIDS.contains(jid) ? "01" : ""));
      urls = Arrays.asList(url);
    }

    @Override
    public Collection<String> doGetStartUrls() 
        throws ConfigurationException, PluginException, IOException {
      return urls;
    }

    @Override
    public Collection<String> doGetPermissionUrls() 
        throws ConfigurationException, PluginException, IOException {
      return urls;
    }

  }

}
