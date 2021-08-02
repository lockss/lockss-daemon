/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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
