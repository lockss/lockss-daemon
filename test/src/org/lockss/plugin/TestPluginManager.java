/*
 * $Id: TestPluginManager.java,v 1.3 2003-02-27 07:31:11 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.io.*;
import java.net.*;
import java.util.*;
import junit.framework.TestCase;
//import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * Test class for org.lockss.plugin.PluginManager
 */

public class TestPluginManager extends LockssTestCase {
  PluginManager mgr;

  public TestPluginManager(String msg) {
    super(msg);
  }

  public void setUp() {
    mgr = new PluginManager();
  }

  static String mockPlugId = "org|lockss|test|MockPlugin";
  static String mauauid1 = "val1|val2";
  static String mauauidkey1 = "val1|val2";
  static String mauauid2 = "val1|va.l3"; // auid contains a dot
  static String mauauidkey2 = "val1|va|l3"; // so its key is escaped

  static String p1param = PluginManager.PARAM_AU_TREE +
    ".org|lockss|test|MockPlugin.";

  static String p1a1param = p1param + mauauidkey1 + ".";
  static String p1a2param = p1param + mauauidkey2 + ".";

  static String configStr =
    p1a1param + MockPlugin.CONFIG_PROP_1 + "=val1\n" +
    p1a1param + MockPlugin.CONFIG_PROP_2 + "=val2\n" +
    p1a2param + MockPlugin.CONFIG_PROP_1 + "=val1\n" +
    p1a2param + MockPlugin.CONFIG_PROP_2 + "=va.l3\n"; // value contains a dot

  public void testNameFromId() {
    assertEquals("org.lockss.Foo", mgr.pluginNameFromId("org|lockss|Foo"));
  }

  public void testEnsurePluginLoaded() throws Exception {
    assertTrue(!mgr.ensurePluginLoaded("org.lockss.NoSuchClass"));
    assertTrue(mgr.ensurePluginLoaded(mockPlugId));
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugId);
    assertEquals(1, mpi.getInitCtr());
    assertTrue(mgr.ensurePluginLoaded(mockPlugId));
    MockPlugin mpi2 = (MockPlugin)mgr.getPlugin(mockPlugId);
    assertSame(mpi, mpi2);
    assertEquals(1, mpi.getInitCtr());
  }

  public void testMgr() throws Exception {
    mgr.startService();
    TestConfiguration.setCurrentConfigFromString(configStr);
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugId);
    assertNotNull(mpi);
    assertEquals(1, mpi.getInitCtr());
    Collection aus = mpi.getAllAUs();
    assertEquals(2, aus.size());
    ArchivalUnit au1 = mpi.getAU(mauauid1);
    assertNotNull(au1);
    MockArchivalUnit mau1 = (MockArchivalUnit)au1;
    Configuration c1 = mau1.getConfiguration();
    assertEquals("val1", c1.get(MockPlugin.CONFIG_PROP_1));
    assertEquals("val2", c1.get(MockPlugin.CONFIG_PROP_2));
    ArchivalUnit au2 = mpi.getAU("val1|va|l3");
    assertNotNull(au2);
    assertEquals(mauauid1, au1.getAUId());
    assertEquals(mauauid2, au2.getAUId());
    assertTrue(au1 != au2);
    assertTrue(!au1.equals(au2));
    MockArchivalUnit mau2 = (MockArchivalUnit)au2;
    Configuration c2 = mau2.getConfiguration();
    assertEquals("val1", c2.get(MockPlugin.CONFIG_PROP_1));
    assertEquals("va.l3", c2.get(MockPlugin.CONFIG_PROP_2));
    assertEquals(SetUtil.set(mau1, mau2), new HashSet(mgr.getAllAUs()));
  }

  public void testFindCUS() throws Exception {
    String url = "http://foo.bar/";
    String lower = "lll";
    String upper = "hhh";
    mgr.startService();
    TestConfiguration.setCurrentConfigFromString(configStr);
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugId);

    CachedUrlSet protoCus = makeCUS(mockPlugId, mauauid1, url, lower, upper);
    PollSpec ps1 = new PollSpec(protoCus);

    CachedUrlSet cus = mgr.findCachedUrlSet(ps1);
    assertNotNull(cus);
    CachedUrlSetSpec cuss = cus.getSpec();
    assertEquals(url, cuss.getUrl());
    RangeCachedUrlSetSpec rcuss = (RangeCachedUrlSetSpec)cuss;
    assertEquals(lower, rcuss.getLowerBound());
    assertEquals(upper, rcuss.getUpperBound());
    // can't test protoCus.getArchivalUnit() .equals( cus.getArchivalUnit() )
    // as we made a fake mock one to build PollSpec, and PluginManager will
    // have created & configured a real mock one.

    CachedUrlSet protoAuCus = makeAUCUS(mockPlugId, mauauid1);
    PollSpec ps2 = new PollSpec(protoAuCus);

    CachedUrlSet aucus = mgr.findCachedUrlSet(ps2);
    assertNotNull(aucus);
    CachedUrlSetSpec aucuss = aucus.getSpec();
    assertEquals("lockssau:", aucuss.getUrl());
    assertTrue(aucuss instanceof AUCachedUrlSetSpec);
  }

  public CachedUrlSet makeCUS(String pluginid, String auid, String url,
			       String lower, String upper) {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setAuId(auid);
    au.setPluginId(pluginid);

    CachedUrlSet cus = new MockCachedUrlSet(au,
					    new RangeCachedUrlSetSpec(url,
								      lower,
								      upper));
    return cus;
  }

  public CachedUrlSet makeAUCUS(String pluginid, String auid) {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setAuId(auid);
    au.setPluginId(pluginid);

    CachedUrlSet cus = new MockCachedUrlSet(au, new AUCachedUrlSetSpec());
    return cus;
  }
}
