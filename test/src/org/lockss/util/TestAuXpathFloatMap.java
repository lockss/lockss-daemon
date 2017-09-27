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

public class TestAuXpathFloatMap extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestAuXpathFloatMap");

  private MockArchivalUnit au1, au2, au3;
  private TdbAu tau1, tau2, tau3;
  private Tdb tdb;

  public void setUp() throws Exception {
    super.setUp();
    setUpTdb();
  }

  private void setUpTdb() throws Tdb.TdbException {
    Properties p1 = new Properties();
    p1.put("title", "Not me");
    p1.put("plugin", "org.lockss.NotThisClass");
    
    Properties p2 = new Properties();
    p2.put("title", "It's");
    p2.put("journalTitle", "jtitle");
    p2.put("plugin", "Plug1");
    p2.put("pluginVersion", "4");
    p2.put("param.1.key", "volume");
    p2.put("param.1.value", "vol_1");
    p2.put("param.2.key", "year");
    p2.put("param.2.value", "2010");
    p2.put("attributes.year", "2010");

    Properties p3 = new Properties();
    p3.put("title", "Howl");
    p3.put("journalTitle", "hj");
    p3.put("plugin", "Plug2");
    p3.put("pluginVersion", "4");
    p3.put("param.1.key", "volume");
    p3.put("param.1.value", "vol_2");
    p3.put("param.2.key", "year");
    p3.put("param.2.value", "2012");
    p3.put("attributes.attr1", "av111");
    p3.put("attributes.year", "2014");
    
    Tdb tdb = new Tdb();
    tau1 = tdb.addTdbAuFromProperties(p1);
    tau2 = tdb.addTdbAuFromProperties(p2);
    tau3 = tdb.addTdbAuFromProperties(p3);
    au1 = new MockArchivalUnit();
    au1.setTdbAu(tau1);
    au2 = new MockArchivalUnit();
    au2.setTdbAu(tau2);
    au3 = new MockArchivalUnit();
    au3.setTdbAu(tau3);
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
    AuXpathMatcher aux = AuXpathMatcher.create(xpath);
    log.critical("foo: " + aux.eval(au));
  }

  String map1 = 
    "[tdbAu/attrs/year > 2012],10.0;" +
    "[tdbAu/attrs/year <= 2012],20.0;";

  public void testEmpty() {
    AuXpathFloatMap map = AuXpathFloatMap.EMPTY;
    assertTrue(map.isEmpty());
    assertEquals(0.0f, map.getMatch(au1));
  }

  public void testGetMatch() {
    AuXpathFloatMap map = new AuXpathFloatMap(map1);
    assertFalse(map.isEmpty());
    assertEquals(0.0f, map.getMatch(au1));
    assertEquals(10.0f, map.getMatch(au3));
    assertEquals(20.0f, map.getMatch(au2));
  }

  public void testToString() {
    assertEquals("[auxpathmap: [[tdbAu/attrs/year > 2012]: 10.0], [[tdbAu/attrs/year <= 2012]: 20.0]]",
		 new AuXpathFloatMap(map1).toString());
  }

}
