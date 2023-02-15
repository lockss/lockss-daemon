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

package org.lockss.plugin.royalsocietyofchemistry;

import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;

/*
 * UrlNormalizer lowercases some urls
 */

public class TestRSC2014UrlNormalizer extends LockssTestCase {

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String RESOLVER_URL_KEY = "resolver_url";
  static final String GRAPHICS_URL_KEY = "graphics_url";
  static final String BASE_URL2_KEY = ConfigParamDescr.BASE_URL2.getKey();
  static final String JOURNAL_CODE_KEY = "journal_code";
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  
  public void testUrlNormalizer() throws Exception {
    
    DefinablePlugin plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.royalsocietyofchemistry.ClockssRSC2014Plugin");
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://pubs.rsc.org/");
    props.setProperty(RESOLVER_URL_KEY, "http://xlink.rsc.org/");
    props.setProperty(GRAPHICS_URL_KEY, "http://img.rsc.org/");
    props.setProperty(BASE_URL2_KEY, "http://www.rsc.org/");
    props.setProperty(JOURNAL_CODE_KEY, "an");
    props.setProperty(VOLUME_NAME_KEY, "123");
    props.setProperty(YEAR_KEY, "2013");
    DefinableArchivalUnit au = null;
    try {
      Configuration config = ConfigurationUtil.fromProps(props);
      au = (DefinableArchivalUnit)plugin.configureAu(config, null);
    }
    catch (ConfigurationException ex) {
      au = null;
    }
    
    UrlNormalizer normalizer = new RSC2014UrlNormalizer();
    assertEquals("http://pubs.rsc.org/en/content/articlelanding/2008/gc/b712109a",
        normalizer.normalizeUrl("http://pubs.rsc.org/en/Content/ArticleLanding/2008/GC/B712109A", au));
    assertEquals("http://xlink.rsc.org/?doi=b712109a",
        normalizer.normalizeUrl("http://xlink.rsc.org/?DOI=B712109A", au));
    
    assertNotEquals(
        "http://pubs.rsc.org/services/images/rscpubs.eplatform.service.freecontent." +
        "imageservice.svc/imageservice/image/ga?id=b712863k", normalizer.normalizeUrl(
        "http://pubs.rsc.org/services/images/RSCpubs.ePlatform.Service.FreeContent" +
        ".ImageService.svc/ImageService/image/GA?id=B712863K", au));
  }
  
}
