/*
 * $Id: TestPluginManager.java,v 1.9 2003-03-04 21:47:27 tal Exp $
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
import org.lockss.repository.TestLockssRepositoryImpl;
import org.lockss.repository.LockssRepositoryServiceImpl;

/**
 * Test class for org.lockss.plugin.PluginManager
 */

public class TestPluginManager extends LockssTestCase {
  private MockLockssDaemon theDaemon = new MockLockssDaemon(null);

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
    p1a2param + MockPlugin.CONFIG_PROP_2 + "=va.l3\n" + // value contains a dot
// needed to allow PluginManager to register AUs
    LockssRepositoryServiceImpl.PARAM_CACHE_LOCATION + "=/tmp";


  PluginManager mgr;

  public TestPluginManager(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();

    mgr = new PluginManager();
    mgr.initService(theDaemon);
  }

  public void tearDown() throws Exception {
    mgr.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }



  private void doConfig() throws Exception {
    mgr.startService();
    TestConfiguration.setCurrentConfigFromString(configStr);
  }

  public void testNameFromId() {
    assertEquals("org.lockss.Foo", mgr.pluginNameFromId("org|lockss|Foo"));
  }

  public void testEnsurePluginLoaded() throws Exception {
    // non-existent class shouldn't load
    assertFalse(mgr.ensurePluginLoaded("org|lockss|NoSuchClass"));
    // MockPlugin should load
    assertTrue(mgr.ensurePluginLoaded(mockPlugId));
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugId);
    assertNotNull(mpi);
    assertEquals(1, mpi.getInitCtr());	// should have been inited once

    // second time shouldn't reload, reinstantiate, or reinitialize plugin
    assertTrue(mgr.ensurePluginLoaded(mockPlugId));
    MockPlugin mpi2 = (MockPlugin)mgr.getPlugin(mockPlugId);
    assertSame(mpi, mpi2);
    assertEquals(1, mpi.getInitCtr());
  }

  public void testStop() throws Exception {
    doConfig();
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugId);
    assertEquals(0, mpi.getStopCtr());
    mgr.stopService();
    assertEquals(1, mpi.getStopCtr());
  }

  public void testAUConfig() throws Exception {
    doConfig();
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugId);
    // plugin should be registered
    assertNotNull(mpi);
    // should have been inited once
    assertEquals(1, mpi.getInitCtr());

    // get the two archival units
    ArchivalUnit au1 = mpi.getAU(mauauidkey1);
    ArchivalUnit au2 = mpi.getAU(mauauidkey2);

    // verify the plugin's set of all AUs is {au1, au2}
    Collection aus = mpi.getAllAUs();
    assertEquals(SetUtil.set(au1, au2), new HashSet(mgr.getAllAUs()));

    // verify au1's configuration
    assertEquals(mauauid1, au1.getAUId());
    MockArchivalUnit mau1 = (MockArchivalUnit)au1;
    Configuration c1 = mau1.getConfiguration();
    assertEquals("val1", c1.get(MockPlugin.CONFIG_PROP_1));
    assertEquals("val2", c1.get(MockPlugin.CONFIG_PROP_2));

    // verify au1's configuration
    assertEquals(mauauid2, au2.getAUId());
    MockArchivalUnit mau2 = (MockArchivalUnit)au2;
    Configuration c2 = mau2.getConfiguration();
    assertEquals("val1", c2.get(MockPlugin.CONFIG_PROP_1));
    assertEquals("va.l3", c2.get(MockPlugin.CONFIG_PROP_2));
  }

  public void testFindCUS() throws Exception {
    String url = "http://foo.bar/";
    String lower = "lll";
    String upper = "hhh";

    doConfig();
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugId);

    // make a PollSpec with info from a manually created CUS, which should
    // match one of the registered AUs
    CachedUrlSet protoCus = makeCUS(mockPlugId, mauauid1, url, lower, upper);
    PollSpec ps1 = new PollSpec(protoCus);

    // verify PluginManager can make a CUS for the PollSpec
    CachedUrlSet cus = mgr.findCachedUrlSet(ps1);
    assertNotNull(cus);
    // verify the CUS's CUSS
    CachedUrlSetSpec cuss = cus.getSpec();
    assertEquals(url, cuss.getUrl());
    RangeCachedUrlSetSpec rcuss = (RangeCachedUrlSetSpec)cuss;
    assertEquals(lower, rcuss.getLowerBound());
    assertEquals(upper, rcuss.getUpperBound());

    assertEquals(mauauid1, cus.getArchivalUnit().getAUId());
    // can't test protoCus.getArchivalUnit() .equals( cus.getArchivalUnit() )
    // as we made a fake mock one to build PollSpec, and PluginManager will
    // have created & configured a real mock one.

    CachedUrlSet protoAuCus = makeAUCUS(mockPlugId, mauauid1);
    PollSpec ps2 = new PollSpec(protoAuCus);

    CachedUrlSet aucus = mgr.findCachedUrlSet(ps2);
    assertNotNull(aucus);
    CachedUrlSetSpec aucuss = aucus.getSpec();
    assertTrue(aucuss instanceof AUCachedUrlSetSpec);
  }



  public void testFindMostRecentCachedUrl() throws Exception {
    String prefix = "http://foo.bar/";
    String url1 = "http://foo.bar/baz";
    String url2 = "http://foo.bar/not";
    doConfig();
    MockPlugin mpi = (MockPlugin)mgr.getPlugin(mockPlugId);

    // get the two archival units
    MockArchivalUnit au1 = (MockArchivalUnit)mpi.getAU(mauauidkey1);
    ArchivalUnit au2 = mpi.getAU(mauauidkey2);
    assertNull(mgr.findMostRecentCachedUrl(url1));
    CachedUrlSetSpec cuss = new MockCachedUrlSetSpec(prefix, null);
    MockCachedUrlSet mcuss = new MockCachedUrlSet(au1, cuss);
    mcuss.addUrl("foo", url1, true, true, null);
    au1.setAUCachedUrlSet(mcuss);
    CachedUrl cu = mgr.findMostRecentCachedUrl(url1);
    assertNotNull(cu);
    assertEquals(url1, cu.getUrl());
    assertNull(mgr.findMostRecentCachedUrl(url2));
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
