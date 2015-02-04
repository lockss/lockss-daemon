/*
 * $Id: PortlandPressCrawlSeedFactory.java,v 1.3 2015-02-04 19:00:47 alexandraohlson Exp $
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.lockss.config.Configuration;
import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.ListUtil;

public class PortlandPressCrawlSeedFactory  
implements CrawlSeedFactory {

  /* currently these are three jids that warrant special base_url */
  private static final String WP_JID = "wp";
  private static final String WST_JID = "wst";
  private static final String WS_JID = "ws";
  private static final Set<String> issueJids = new HashSet<String>(Arrays.asList(WP_JID, WST_JID, WS_JID));


  public CrawlSeed createCrawlSeed(ArchivalUnit au) {
    return new PortlandPressCrawlSeed(au);
  }
  
  public CrawlSeed createCrawlSeed(Crawler.CrawlerFacade crawlFacade) {
    return new PortlandPressCrawlSeed(crawlFacade.getAu());
  }


  public class PortlandPressCrawlSeed extends BaseCrawlSeed {

    public PortlandPressCrawlSeed(ArchivalUnit au) {
      super(au);
    }

    @Override
    public Collection<String> getStartUrls() 
        throws ConfigurationException, PluginException {
      Configuration config = au.getConfiguration();
      if (config == null) {
        throw new PluginException("Null configuration, can't get start url");
      }
      String base_url = config.get(ConfigParamDescr.BASE_URL.getKey());
      String jid = config.get(ConfigParamDescr.JOURNAL_ID.getKey());
      String vol = config.get(ConfigParamDescr.VOLUME_NAME.getKey());
      if (base_url == null || jid == null || vol == null) {
        throw new PluginException.InvalidDefinition(
            "CrawlSeed cannot set a starting URL based on the params");
      }
      /* 
       * start_url will be one of:
       *    "<base>/<jid>/<volume>/lockss.htm"
       * or
       *   "<base>/<jid>/<volume>01/lockss.htm"
       */
      StringBuilder sb = new StringBuilder();
      sb.append(base_url);
      sb.append(jid);
      sb.append("/");
      sb.append(vol);
      if (issueJids.contains(jid)) {
        sb.append("01");
      }
      sb.append("/lockss.htm");
      return ListUtil.list(sb.toString());
    }

    @Override
    public Collection<String> getPermissionUrls() throws ConfigurationException, PluginException {
      return getStartUrls();
    }
  }
}





