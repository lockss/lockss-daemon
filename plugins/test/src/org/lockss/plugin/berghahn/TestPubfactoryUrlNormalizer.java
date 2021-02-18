/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.berghahn;

import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;

public class TestPubfactoryUrlNormalizer extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  private MockArchivalUnit m_mau;

  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "10");
    props.setProperty(BASE_URL_KEY, "https://www.berghahnjournals.com/");
    props.setProperty(JID_KEY, "foo");

    Configuration config = ConfigurationUtil.fromProps(props);
    m_mau = new MockArchivalUnit();
    m_mau.setConfiguration(config);
    }
  

  public void testNormalizeUrl() throws Exception {
    UrlNormalizer normalizer = new BerghahnUrlNormalizer();
    // No change expected
    assertEquals("http://www.berghahnjournals.com/downloadpdf/journals/foo/10/1/trans070105.xml",
        normalizer.normalizeUrl("http://www.berghahnjournals.com/downloadpdf/journals/foo/10/1/trans070105.xml", m_mau));
    assertEquals("http://www.berghahnjournals.com/view/journals/foo/10/1/trans070105.xml",
        normalizer.normalizeUrl("http://www.berghahnjournals.com/view/journals/foo/10/1/trans070105.xml", m_mau));
    assertEquals("http://www.berghahnjournals.com/view/journals/foo/10/1/trans070105.xml?pdfVersion=true",
        normalizer.normalizeUrl("http://www.berghahnjournals.com/view/journals/foo/10/1/trans070105.xml?pdfVersion=true", m_mau));
    assertEquals("https://www.berghahnjournals.com/cite/$002fjournals$002ffoo$002f10$002f2$002fbhs100206.xml/$N?nojs=true",
        normalizer.normalizeUrl("https://www.berghahnjournals.com/cite/$002fjournals$002ffoo$002f10$002f2$002fbhs100206.xml/$N?nojs=true", m_mau));

    
    // Remove the client argument from ris
    assertEquals("https://www.berghahnjournals.com/cite:exportcitation/ris?t:ac=$002fjournals$002ffoo$002f10$002f2$002fbhs100206.xml/$N",
        normalizer.normalizeUrl("https://www.berghahnjournals.com/cite:exportcitation/ris?t:ac=$002fjournals$002ffoo$002f10$002f2$002fbhs100206.xml/$N&t:state:client=H4sIAAAARhDnAAAA", m_mau));
    // but not from other types of urls for now (haven't seen this)
    assertEquals("https://www.berghahnjournals.com/cite/$002fjournals$002ffoo$002f10$002f2$002fbhs100206.xml/$N?nojs=true&t:state:client=H4sIAAAARhDnAAAA",
        normalizer.normalizeUrl("https://www.berghahnjournals.com/cite/$002fjournals$002ffoo$002f10$002f2$002fbhs100206.xml/$N?nojs=true&t:state:client=H4sIAAAARhDnAAAA", m_mau));
  }
  

  
}
