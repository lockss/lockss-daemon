/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.portlandpress;

import java.io.IOException;
import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;

public class PortlandPressCrawlSeed extends BaseCrawlSeed {

  /* Currently these are three JIDs that warrant a special start URL */
  protected static final String WP_JID = "wp";
  protected static final String WS_JID = "ws";
  protected static final String WST_JID = "wst";
  protected static final Set<String> SPECIAL_JIDS =
      new HashSet<String>(Arrays.asList(WP_JID, WST_JID, WS_JID));

  protected Collection<String> urls;

  public PortlandPressCrawlSeed(CrawlerFacade facade) {
    super(facade);
  }
  
  protected void initialize()
      throws ConfigurationException, PluginException, IOException {
    super.initialize();
    Configuration config = au.getConfiguration();
    if (config == null) {
      throw new PluginException("Cannot compute start URL: null configuration");
    }
    String base_url = config.get(ConfigParamDescr.BASE_URL.getKey());
    String jid = config.get(ConfigParamDescr.JOURNAL_ID.getKey());
    String vol = config.get(ConfigParamDescr.VOLUME_NAME.getKey());
    if (base_url == null || jid == null || vol == null) {
      throw new ConfigurationException(
          String.format("Cannot compute start URL: invalid parameters (%s: %s, %s: %s, %s: %s)",
                        ConfigParamDescr.BASE_URL.getKey(),
                        base_url,
                        ConfigParamDescr.JOURNAL_ID.getKey(),
                        jid,
                        ConfigParamDescr.VOLUME_NAME.getKey(),
                        vol));
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
  public Collection<String> doGetStartUrls() {
    return urls;
  }

  @Override
  public Collection<String> doGetPermissionUrls() {
    return urls;
  }
  
}