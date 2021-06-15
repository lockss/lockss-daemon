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
    assertEquals("http://www.example.com/content/304/2/H253.full.pdf",
        normalizer.normalizeUrl("https://www.example.com/content/ajpheart/304/2/H253.full-text.pdf", m_mau));
    assertEquals("https://www.jabfm.org/content/33/1/1.full.pdf",
        normalizer.normalizeUrl("https://www.jabfm.org/content/jabfp/33/1/1.full.pdf", m_mau));
    assertEquals("https://cjasn.asnjournals.org/content/16/1/1.full.pdf",
        normalizer.normalizeUrl("https://cjasn.asnjournals.org/content/clinjasn/16/1/1.full.pdf", m_mau));
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
