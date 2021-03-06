/* $Id:

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.massachusettsmedicalsociety;

import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;

/*
 * UrlNormalizer removes ?cookieSet=1 suffix
 */
public class TestMassachusettsMedicalSocietyUrlNormalizer extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  private MockArchivalUnit m_mau;

  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "3");
    props.setProperty(BASE_URL_KEY, "http://www.www.nejm.org/");
    props.setProperty(JID_KEY, "foo");

    Configuration config = ConfigurationUtil.fromProps(props);
    m_mau = new MockArchivalUnit();
    m_mau.setConfiguration(config);
    }


  public void testUrlNormalizer() throws Exception { 
    
    UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();
    assertEquals("http://www.nejm.org/doi/full/10.1056/NEJM191103091641004",
                 normalizer.normalizeUrl("http://www.nejm.org/doi/full/10.1056/NEJM191103091641004", m_mau));
    assertEquals("http://www.nejm.org/doi/full/10.1056/NEJM191103091641004",
                 normalizer.normalizeUrl("http://www.nejm.org/doi/full/10.1056/NEJM191103091641004?cookieSet=1", m_mau));
    assertEquals("http://www.nejm.org/action/downloadCitation?doi=10.1056%2FNEJMc073037&format=bibTex&include=cit",
                 normalizer.normalizeUrl("http://www.nejm.org/action/downloadCitation?format=bibTex&doi=10.1056%2FNEJMc073037&include=cit&direct=checked", m_mau));
  }
  
}