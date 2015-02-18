/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
