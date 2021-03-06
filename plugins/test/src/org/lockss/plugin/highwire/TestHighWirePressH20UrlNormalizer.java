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

package org.lockss.plugin.highwire;

import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
/*
 * UrlNormalizer removes  ?sid=.. (search ids)
 * 
 * http://www.fasebj.org/content/28/1_Supplement/LB31.abstract?sid=a166e1db-4d2a-4d45-b09e-aa2eaaf5bc21
 * to http://www.fasebj.org/content/28/1_Supplement/LB31.abstract
 */
import org.lockss.test.MockArchivalUnit;

public class TestHighWirePressH20UrlNormalizer extends LockssTestCase {
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private DefinablePlugin plugin;
  private MockArchivalUnit m_mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.highwire.HighWirePressH20Plugin");
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "303");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    
    Configuration config = ConfigurationUtil.fromProps(props);
    m_mau = new MockArchivalUnit();
    m_mau.setConfiguration(config);
    plugin.configureAu(config, null);
  }
  
  public void testUrlNormalizer() throws Exception {
    UrlNormalizer normalizer = new HighWirePressH20UrlNormalizer();
    
    assertEquals("http://www.example.com/content/28/1_Supplement/LB31.abstract",
        normalizer.normalizeUrl("https://www.example.com/content/28/1_Supplement/LB31.abstract?sid=a166e1db-4d2a-4d45-b09e-aa2eaaf5bc21", m_mau));
    
    assertEquals("http://www.example.com/content/303/1/C1?rss=foo",
        normalizer.normalizeUrl("https://www.example.com/content/303/1/C1?rss=foo", m_mau));
    
    assertEquals("http://www.example.com/sites/all/modules/highwire/highwire/highwire_theme_tools/fonts/hwicons.tiff?-2mifpm",
        normalizer.normalizeUrl("http://www.example.com/sites/all/modules/highwire/highwire/highwire_theme_tools/fonts/hwicons.tiff?-2mifpm", m_mau));
    
    assertEquals("http://www.example.com/search?submit=yes&sortspec=first-page&tocsectionid=Abstracts&volume=101&issue=Suppl%202",
        normalizer.normalizeUrl("http://www.example.com/search?submit=yes&issue=Suppl%202&volume=101&sortspec=first-page&tocsectionid=Abstracts&FIRSTINDEX=0", m_mau));
    
    assertEquals("http://www.example.com/search?submit=yes&sortspec=first-page&tocsectionid=Abstracts&volume=101&issue=Suppl%202&FIRSTINDEX=20",
        normalizer.normalizeUrl("http://www.example.com/search?submit=yes&issue=Suppl%202&sortspec=first-page&volume=101&tocsectionid=Abstracts&FIRSTINDEX=20", m_mau));
    
  }
  
}
