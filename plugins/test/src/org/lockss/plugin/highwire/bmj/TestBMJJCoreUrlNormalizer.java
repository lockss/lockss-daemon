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

package org.lockss.plugin.highwire.bmj;

import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
/*
 * Parent UrlNormalizer removes  suffixes, etc.
 * http://www.bmj.com/content/303/1/C1/F1.large.jpg?width=800&height=600
 * & http://www.bmj.com/content/303/1/C1/F1.large.jpg?download=true
 * to http://www.bmj.com/content/303/1/C1/F1.large.jpg
 * 
 * http://static.www.bmj.com/content/304/2/H253.full-text.pdf
 * & http://static.beta.www.bmj.com/content/304/2/H253.full-text.pdf
 * to http://www.bmj.com/content/ajpheart/304/2/H253.full.pdf
 */
import org.lockss.test.MockArchivalUnit;

public class TestBMJJCoreUrlNormalizer extends LockssTestCase {
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private DefinablePlugin plugin;
  private MockArchivalUnit m_mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.highwire.bmj.BMJDrupalPlugin");
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "303");
    props.setProperty(BASE_URL_KEY, "http://www.bmj.com/");
    
    Configuration config = ConfigurationUtil.fromProps(props);
    m_mau = new MockArchivalUnit();
    m_mau.setConfiguration(config);
    plugin.configureAu(config, null);
    }
  
  public void testUrlNormalizer() throws Exception {
    UrlNormalizer normalizer = new BMJJCoreUrlNormalizer();
    // http://static.www.bmj.com/sites/default/files/attachments/bmj-article/pre-pub-history
    
    assertEquals("http://www.bmj.com/content/303/1/C1/F1.large.jpg",
        normalizer.normalizeUrl("http://www.bmj.com/content/303/1/C1/F1.large.jpg?width=800&height=600", m_mau));
    
    assertEquals("http://www.bmj.com/content/bmj.h3779.full.pdf",
        normalizer.normalizeUrl("https://static.www.bmj.com/content/bmj.h3779.full.pdf", m_mau));
    
    assertEquals("http://www.bmj.com/content/bmj.h3779.full.pdf",
        normalizer.normalizeUrl("http://static.beta.www.bmj.com/content/bmj.h3779.full.pdf", m_mau));
    
    assertEquals("http://www.bmj.com/content/304/2/H253",
        normalizer.normalizeUrl("https://static.beta.ww.bmj.com/content/304/2/H253", m_mau));
    
    assertEquals("https://staticbeta.www.bmj.com/content/304/2/H253",
        normalizer.normalizeUrl("https://staticbeta.www.bmj.com/content/304/2/H253", m_mau));
  }
  
}
