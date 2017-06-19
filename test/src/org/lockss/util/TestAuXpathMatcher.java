/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.*;

import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.plugin.*;

public class TestAuXpathMatcher extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestAuXpathMatcher");

  MockPlugin mp1;
  MockPlugin mp2;
  TitleConfig tc1, tc2, tc3, tc4, tc5, tc6;
  List titles;

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    setUpTdb();
    // makeTitles();
  }

  private MockArchivalUnit au1, au2, au3;
  private TdbAu tau1, tau2;
  private Tdb tdb;

  private void setUpTdb() throws Tdb.TdbException {
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_FQDN,
				  "my.host47.example.com");

    // set title database properties
    Properties p1 = new Properties();
    p1.put("title", "It's");
    p1.put("journalTitle", "jtitle aabc");
    p1.put("plugin", "Plug1");
    p1.put("pluginVersion", "4");
    p1.put("param.1.key", "volume");
    p1.put("param.1.value", "vol_1");
    p1.put("param.2.key", "year");
    p1.put("param.2.value", "2010");

    Properties p2 = new Properties();
    p2.put("title", "Howl");
    // p2.put("publicationTitle", "jtitle baca");
    p2.put("journalTitle", "jtitle baca");
    // p2.put("journal.title", "jtitle baca");
    // p2.put("publicationName", "jtitle baca");
    // p1.put("journalName", "jtitle baca");
    p2.put("plugin", "Plug2");
    p2.put("pluginVersion", "4");
    p2.put("param.1.key", "volume");
    p2.put("param.1.value", "vol_2");
    p2.put("param.2.key", "year");
    p2.put("param.2.value", "2012");
    p2.put("attributes.attr1", "av111");
    p2.put("attributes.year", "2014");
    p2.put("attributes.pollerhost", ConfigManager.getPlatformHostname());
    
    Tdb tdb = new Tdb();
    tau1 = tdb.addTdbAuFromProperties(p1);
    tau2 = tdb.addTdbAuFromProperties(p2);
    au1 = new MockArchivalUnit();
    au1.setTdbAu(tau1);
    au2 = new MockArchivalUnit();
    au2.setTdbAu(tau2);
  }
  
  private void assertMatch(ArchivalUnit au, String xpath) {
    AuXpathMatcher aux = AuXpathMatcher.create(xpath);
    assertTrue(aux.isMatch(au));
  }

  private void assertNotMatch(ArchivalUnit au, String xpath) {
    AuXpathMatcher aux = AuXpathMatcher.create(xpath);
    assertFalse(aux.isMatch(au));
  }

  private void logVal(ArchivalUnit au, String xpath) {
    AuXpathMatcher aux = AuXpathMatcher.create("[" + xpath + "]");
    log.debug(xpath + ": " + aux.eval(au));
  }

  private static String ABC = "[tdbAu/publicationTitle='jtitle aabc']";
  private static String ABCRE = 
    "[RE:isMatchRe(tdbAu/publicationTitle, 'jtitle .*a.*b.*c')]";

  public void testMatch() {
    assertNotMatch(au2, "[tdbAu/name='wrong_name']");
    assertMatch(au2, "[tdbAu/attrs/attr1='av111']");
    assertNotMatch(au2, "[tdbAu/attrs/attr1!='av111']");
    assertNotMatch(au1, "[tdbAu/attrs/attr1='av111']");
    assertMatch(au2, "[tdbAu/attrs/year > 2013]");
    assertMatch(au2, "[tdbAu/year > 2013]");
    assertNotMatch(au1, "[tdbAu/attrs/year > 2013]");
    assertMatch(au2, "[tdbAu/attrs/year='2014']");
    // logVal(au2, "tdbAu/attrs/year");

    assertMatch(au2, "[tdbAu/params/volume='vol_2']");
    assertNotMatch(au2, "[tdbAu/params/volume='vol_1']");
    assertNotMatch(au2, "[tdbAu/params/no_param='vol_1']");

    assertMatch(au1, ABC);
    assertNotMatch(au2, ABC);
    assertMatch(au1, ABCRE);
    assertNotMatch(au2, ABCRE);

    assertMatch(au1, "[pluginId='org.lockss.test.MockPlugin']");
    assertMatch(au1, "[RE:isMatchRe(pluginId, 'MockPlugin')]");

//     logVal(au2, "tdbAu/attrs/pollerhost");
//     logVal(au2, "$myhost");
    assertMatch(au2, "[tdbAu/attrs/pollerhost = $myhost]");
    assertNotMatch(au2, "[tdbAu/attrs/pollerhost = 'not.my.host']");
    assertNotMatch(au1, "[tdbAu/attrs/pollerhost = $myhost]");
  }

}
