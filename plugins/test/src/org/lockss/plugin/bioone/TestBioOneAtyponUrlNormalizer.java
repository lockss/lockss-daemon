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

package org.lockss.plugin.bioone;

import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;

public class TestBioOneAtyponUrlNormalizer extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  private MockArchivalUnit m_mau;

  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "3");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(JID_KEY, "foo");

    Configuration config = ConfigurationUtil.fromProps(props);
    m_mau = new MockArchivalUnit();
    m_mau.setConfiguration(config);
    }
  

  public void testNormalizeUrl() throws Exception {
    UrlNormalizer normalizer = new BioOneAtyponUrlNormalizer();
    // No change expected
    assertEquals("http://www.example.com/foo",
                 normalizer.normalizeUrl("http://www.example.com/foo", m_mau));
    assertEquals("http://www.example.com/foo?",
                 normalizer.normalizeUrl("http://www.example.com/foo?", m_mau));
    assertEquals("http://www.example.com/foo?nothinghappens",
                 normalizer.normalizeUrl("http://www.example.com/foo?nothinghappens", m_mau));
    
    // Remove the right suffixes
    assertEquals("http://www.example.com/foo",
                 normalizer.normalizeUrl("http://www.example.com/foo?cookieSet=1", m_mau));
    assertEquals("http://www.example.com/foo",
                 normalizer.normalizeUrl("http://www.example.com/foo?prevSearch=", m_mau));
    assertEquals("http://www.example.com/foo",
            normalizer.normalizeUrl("http://www.example.com/foo?seq=512", m_mau));
    
    // Remove the first double slash (other than that of http:// or similar)
    assertEquals("http://www.example.com/foo",
                 normalizer.normalizeUrl("http://www.example.com//foo", m_mau));
    assertEquals("http://www.example.com/foo/bar",
                 normalizer.normalizeUrl("http://www.example.com/foo/bar", m_mau));
    assertEquals("http://www.example.com/nothinghappens/",
                 normalizer.normalizeUrl("http://www.example.com/nothinghappens/", m_mau));
    assertEquals("http://www.example.com/foo/",
                 normalizer.normalizeUrl("http://www.example.com/foo//", m_mau));
    assertEquals("http://www.example.com/foo/bar//baz",
                 normalizer.normalizeUrl("http://www.example.com/foo//bar//baz", m_mau));
    assertEquals("https://www.example.com/foo",
                normalizer.normalizeUrl("https://www.example.com//foo", m_mau));
    assertEquals("ftp://www.example.com/foo",
                 normalizer.normalizeUrl("ftp://www.example.com//foo", m_mau));
  }
  

  
}
