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
 * UrlNormalizer removes  suffixes
 * http://ajpcell.physiology.org/content/ajpcell/303/1/C1/F1.large.jpg?width=800&height=600
 * & http://ajpcell.physiology.org/content/ajpcell/303/1/C1/F1.large.jpg?download=true
 * to http://ajpcell.physiology.org/content/ajpcell/303/1/C1/F1.large.jpg
 * 
 * http://ajpheart.physiology.org/content/ajpheart/304/2/H253.full-text.pdf
 * to http://ajpheart.physiology.org/content/ajpheart/304/2/H253.full.pdf
 * 
 * http://ajpheart.physiology.org/content/304/2/H253.full-text.pdf
 * to http://ajpheart.physiology.org/content/304/2/H253.full.pdf
 * 
 * http://ajpheart.physiology.org/content/304/2/H253.full-text.pdf+html
 * & http://ajpheart.physiology.org/content/304/2/H253.full.pdf%2Bhtml
 * to http://ajpheart.physiology.org/content/304/2/H253.full.pdf+html
 */
import org.lockss.test.MockArchivalUnit;

public class TestHighWireDrupalUrlNormalizer extends LockssTestCase {
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private DefinablePlugin plugin;
  private MockArchivalUnit m_mau;
  private MockArchivalUnit m_mau2;
  
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.highwire.HighWireDrupalPlugin");
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "303");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    
    Configuration config = ConfigurationUtil.fromProps(props);
    props.setProperty(BASE_URL_KEY, "https://www.example.com/");
    m_mau = new MockArchivalUnit();
    m_mau.setConfiguration(config);
    
    Configuration config2 = ConfigurationUtil.fromProps(props);
    m_mau2 = new MockArchivalUnit();
    m_mau2.setConfiguration(config2);
    
    plugin.configureAu(config, null);
    }
  
  public void testUrlNormalizer() throws Exception {
    UrlNormalizer normalizer = new HighWireDrupalUrlNormalizer();
    
    assertEquals("http://www.example.com/content/303/1/C1.full.pdf",
        normalizer.normalizeUrl("https://www.example.com/sites/all/libraries/pdfjs/web/viewer.html?file=/content/ajpcell/303/1/C1.full.pdf", m_mau));
    assertEquals("https://www.example.com/content/303/1/C1.full.pdf",
        normalizer.normalizeUrl("http://www.example.com/sites/all/libraries/pdfjs/web/viewer.html?file=/content/ajpcell/303/1/C1.full.pdf", m_mau2));
    
    assertEquals("https://www.example.com/content/ajpcell/303/1/C1/F1.large.jpg",
        normalizer.normalizeUrl("https://www.example.com/content/ajpcell/303/1/C1/F1.large.jpg?width=800&height=600", m_mau2));
    assertEquals("http://www.example.com/content/ajpcell/303/1/C1/F1.large.jpg",
        normalizer.normalizeUrl("http://www.example.com/content/ajpcell/303/1/C1/F1.large.jpg?download=true", m_mau));
    assertEquals("http://www.example.com/sites/default/files/color/jcore_1-15d49f53/colors.css",
        normalizer.normalizeUrl("http://www.example.com/sites/default/files/color/jcore_1-15d49f53/colors.css?n3sdk7", m_mau));
    assertEquals("http://www.example.com/content/ajpheart/304/2/H253.full.pdf",
        normalizer.normalizeUrl("https://www.example.com/content/ajpheart/304/2/H253.full-text.pdf", m_mau));
    assertEquals("http://www.example.com/content/304/2/H253.full.pdf",
        normalizer.normalizeUrl("http://www.example.com/content/304/2/H253.full-text.pdf", m_mau));
    assertEquals("https://www.example.com/content/304/2/H253.full.pdf+html",
        normalizer.normalizeUrl("http://www.example.com/content/304/2/H253.full-text.pdf+html", m_mau2));
    assertEquals("http://www.example.com/content/304/2/H253.full.pdf+html",
        normalizer.normalizeUrl("https://www.example.com/content/304/2/H253.full.pdf%2Bhtml", m_mau));
    assertEquals("https://www.example.com/content/304/2/H253.full.pdf+html",
        normalizer.normalizeUrl("https://www.example.com/content/304/2/H253.full-text.pdf%2Bhtml", m_mau2));
    assertEquals("http://www.example.com/content/304/2/H253.full.pdf",
        normalizer.normalizeUrl("http://www.example.com/content/304/2/H253.full.pdf?download=yes", m_mau));
    assertEquals("http://www.example.com/content/304/2/H253.full.pdf?sso-checked=yestrue",
        normalizer.normalizeUrl("http://www.example.com/content/304/2/H253.full.pdf?sso-checked=yestrue", m_mau));
    assertEquals("https://www.example.com/content/303/1/C1",
        normalizer.normalizeUrl("https://www.example.com/content/303/1/C1?rss=foo", m_mau2));
    assertEquals("http://www.example.com/content/303/1/C1",
        normalizer.normalizeUrl("https://www.example.com/content/303/1/C1?ijkey=foo", m_mau));
    assertEquals("https://www.example.com/content/303/1/C1.e-letters",
        normalizer.normalizeUrl("http://www.example.com/content/303/1/C1.e-letters?foo", m_mau2));
    assertEquals("https://www.example.com/sites/all/modules/highwire/highwire/highwire_theme_tools/fonts/hwicons.eot",
        normalizer.normalizeUrl("http://www.example.com/sites/all/modules/highwire/highwire/highwire_theme_tools/fonts/hwicons.eot?-2mifpm", m_mau2));
    assertEquals("http://www.example.com/sites/all/modules/highwire/highwire/highwire_theme_tools/fonts/hwicons.svg",
        normalizer.normalizeUrl("http://www.example.com/sites/all/modules/highwire/highwire/highwire_theme_tools/fonts/hwicons.svg?-2mifpm", m_mau));
    assertEquals("https://www.example.com/sites/all/modules/highwire/highwire/highwire_theme_tools/fonts/hwicons.woff",
        normalizer.normalizeUrl("https://www.example.com/sites/all/modules/highwire/highwire/highwire_theme_tools/fonts/hwicons.woff?-2mifpm", m_mau2));
    
    assertEquals("http://www.example.com/content/1/1/e00078-15/DC6/embed/inline-supplementary-material-1.mov",
        normalizer.normalizeUrl("http://www.example.com/content/1/1/e00078-15/DC6/embed/inline-supplementary-material-1.mov?download=no", m_mau));
    
    assertEquals("https://www.example.com/sites/all/modules/highwire/highwire/highwire_theme_tools/fonts/hwicons.tiff?-2mifpm",
        normalizer.normalizeUrl("http://www.example.com/sites/all/modules/highwire/highwire/highwire_theme_tools/fonts/hwicons.tiff?-2mifpm", m_mau2));
  }
  
}
