/*
 * $Id$
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

package org.lockss.plugin.hindawi;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;

public class TestHindawiUrlNormalizer extends LockssTestCase {
  
  private final String PLUGIN_NAME = "org.lockss.plugin.hindawi.HindawiPublishingCorporationPlugin";
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String DOWNLOAD_URL_KEY = "download_url";
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  private final String BASE_URL = "http://www.example.com/";
  private final String DOWNLOAD_URL = "http://download.example.com/";
  private final String VOLUME_NAME = "2008";
  private final String JOURNAL_ID = "jid";
  
  private final Configuration AU_CONFIG =
      ConfigurationUtil.fromArgs(BASE_URL_KEY, BASE_URL,
          DOWNLOAD_URL_KEY, DOWNLOAD_URL,
          VOLUME_NAME_KEY, VOLUME_NAME,
          JOURNAL_ID_KEY, JOURNAL_ID);
  
  private DefinablePlugin plugin;
  private MockArchivalUnit m_mau;
  
  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(), PLUGIN_NAME);
    m_mau = new MockArchivalUnit();
    m_mau.setConfiguration(AU_CONFIG);
    plugin.configureAu(AU_CONFIG, null);
  }
  
  public void testUrlNormalizer() throws Exception {
    UrlNormalizer normalizer = new HindawiUrlNormalizer();
    assertEquals("http://www.example.com/foo/bar/123.gif",
        normalizer.normalizeUrl("http://www.example.com/foo/bar/123.gif", m_mau));
    assertEquals("http://www.example.com/foo/bar/123.gif",
        normalizer.normalizeUrl("http://www.example.com/foo\\bar\\123.gif", m_mau));
    assertEquals("http://www.example.com/foo/bar/123.gif?p1=v1&p2=v\\2",
        normalizer.normalizeUrl("http://www.example.com/foo/bar/123.gif?p1=v1&p2=v\\2", m_mau));
    assertEquals("http://www.example.com/foo/bar/123.gif?p1=v1&p2=v\\2",
        normalizer.normalizeUrl("http://www.example.com/foo\\bar\\123.gif?p1=v1&p2=v\\2", m_mau));
  }
  
}
