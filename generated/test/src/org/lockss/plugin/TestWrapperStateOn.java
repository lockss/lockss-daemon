/*
 * $Id: TestWrapperStateOn.java,v 1.3 2004-06-10 22:03:54 tyronen Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.io.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.plugin.wrapper.*;
import org.lockss.repository.*;
import org.lockss.state.*;

/**
 * This is another test class for org.lockss.util.WrapperState.  It runs under
 * JDK 1.4 only.
 */

public class TestWrapperStateOn extends LockssTestCase {

  public TestWrapperStateOn(String msg) {
    super(msg);
  }

  /** Make sure basic functionality works */
  public void testBasic() {
    Plugin plug = new MockPlugin();
    WrappedPlugin wplug;
    wplug = (WrappedPlugin)WrapperState.getWrapper(plug);
    assertNotNull(wplug);
    WrappedPlugin wplug2 = (WrappedPlugin)WrapperState.getWrapper(plug);
    assertSame(wplug,wplug2);
    Plugin orig = (Plugin)WrapperState.getOriginal(wplug);
    assertSame(plug,orig);
  }

  /* Verify archival units within wrapped plugins are correctly wrapped */
  void commonTest(MockPlugin plug, WrappedPlugin wplug) throws Exception {
    // Make an archival unit
    Configuration config = ConfigurationUtil.fromString("");
    WrappedArchivalUnit wau = (WrappedArchivalUnit) wplug.createAu(config);
    assertNotNull(wau);

    // Verify wrapped Au points to wrapped plugin
    WrappedPlugin wplug2 = (WrappedPlugin) wau.getPlugin();
    assertSame(wplug, wplug2);

    // Verify wrapped Au points to original Au
    ArchivalUnit au = (ArchivalUnit) WrapperState.getOriginal(wau);
    Plugin plug2 = au.getPlugin();
    assertSame(plug, plug2);

    // Associate Au with plugin
    plug.registerArchivalUnit(au);

    // Get list of Aus in each plugin,verify nonempty
    Collection ocoll = plug.getAllAus();
    Collection wcoll = wplug.getAllAus();
    assertSame(ocoll,plug.getAllAus());
    assertFalse(plug.getAllAus().isEmpty());
    assertFalse(ocoll.isEmpty());
    assertFalse(wcoll.isEmpty());

    // Get the Au objects
    WrappedArchivalUnit wau2 = (WrappedArchivalUnit) wcoll.iterator().next();
    ArchivalUnit au2 = (ArchivalUnit) ocoll.iterator().next();

    // Verify they are the same ones as before, and point to the right places
    assertSame(au2, WrapperState.getOriginal(wau2));
    assertSame(WrapperState.getWrapper(au2), wau2);
    assertSame(au, au2);
    assertSame(wau,wau2);
  }

  /** Run above tests on directly created wrapped plugin */
  public void testEncapsulate() throws Exception {
    MockPlugin mockPlugin = new MockPlugin();
    WrappedPlugin wrappedPlugin =
        (WrappedPlugin) WrapperState.getWrapper(mockPlugin);
    commonTest(mockPlugin,wrappedPlugin);
  }

  public void testRetrieveWrappedPlugin() throws Exception {
    MockLockssDaemon theDaemon = new MockLockssDaemon();
     WrappedPlugin wplug = (WrappedPlugin)WrapperState.retrieveWrappedPlugin(
        "org|lockss|test|MockPlugin",theDaemon);
     assertNotNull(wplug);
     MockPlugin mock = (MockPlugin)wplug.getOriginal();
     assertNotNull(mock);
  }

  final String URL = "http://www.example.com/testDir/leaf1";

  /** Verify wrapping of CachedUrlSet is nested correctly */
  public void testNestedCachedUrlSet() throws Exception {
    MockPlugin mockPlugin = new MockPlugin();
    WrappedPlugin wrappedPlugin =
        (WrappedPlugin) WrapperState.getWrapper(mockPlugin);
    Configuration config = ConfigurationUtil.fromString("");
    WrappedArchivalUnit wau =
        (WrappedArchivalUnit) wrappedPlugin.createAu(config);
    ArchivalUnit au = (ArchivalUnit)WrapperState.getOriginal(wau);

    CachedUrlSetSpec cspec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");

    WrappedCachedUrlSet wcus = (WrappedCachedUrlSet)wau.getAuCachedUrlSet();
    assertSame(wcus.getArchivalUnit(),wau);

    CachedUrlSet cus = (CachedUrlSet)WrapperState.getOriginal(wcus);
    assertSame(cus.getArchivalUnit(),au);

  }

  /** More sophisticated test uses the daemon */
  public void testNestedCachedUrlAndUrlCacher() throws Exception {
    // Set up mock objects
    MockArchivalUnit mau = new MockArchivalUnit();
    MockPlugin plugin = new MockPlugin();
    mau.setPlugin(plugin);
    assertSame(mau.getPlugin(),plugin);

    // Set up wrapped objects
    WrappedArchivalUnit wau = (WrappedArchivalUnit) WrapperState.getWrapper(
        mau);
    WrappedPlugin wplug = (WrappedPlugin)WrapperState.getWrapper(plugin);
    assertSame(wau.getPlugin(),wplug);

    // Get CachedUrlSets
    CachedUrlSetSpec cspec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    WrappedCachedUrlSet wcus = (WrappedCachedUrlSet)wplug.makeCachedUrlSet(wau,cspec);
    MockCachedUrlSet mcus = (MockCachedUrlSet)WrapperState.getOriginal(wcus);
    mcus.addUrl(URL);

    // Verify wrapped CachedUrlSet
    WrappedPlugin wcplug = (WrappedPlugin)wcus.getArchivalUnit().getPlugin();
    assertSame(wcplug,wplug);
    assertSame(mcus.getArchivalUnit(),mau);
    assertSame(wcus.getArchivalUnit(),wau);

    // Get and verify wrapped CachedUrl
    WrappedCachedUrl wurl = (WrappedCachedUrl) wplug.makeCachedUrl(wcus, URL);
    assertNotNull(wurl);
    assertSame(wurl.getArchivalUnit(), wau);
    CachedUrl curl = (CachedUrl) WrapperState.getOriginal(wurl);
    assertSame(curl.getArchivalUnit(), mau);

    // Make sure wrapped UrlCacher points to right place
    WrappedUrlCacher wc = (WrappedUrlCacher) wplug.makeUrlCacher(wcus, URL);
    assertNotNull(wc);
    assertSame(wc.getCachedUrlSet(), wcus);
    assertSame(wc.getCachedUrl().getArchivalUnit(), wau);
    assertSame(wc.getCachedUrl().getUrl(),URL);

    // Make sure unwrapped UrlCacher points to right place
    UrlCacher uc = (UrlCacher) WrapperState.getOriginal(wc);
    assertSame(uc.getCachedUrlSet(), mcus);
    assertSame(uc.getCachedUrl().getArchivalUnit(), mau);
    assertSame(uc.getCachedUrl().getUrl(),URL);

  }

  /** Verify removal works correctly, and new objects don't map to old */
  public void testRemove() {
    Plugin plugin = new MockPlugin();
    WrappedPlugin wplug = (WrappedPlugin)WrapperState.getWrapper(plugin);
    WrapperState.removeWrapping(plugin);
    Object obj = WrapperState.getOriginal(wplug);
    assertNull(obj);
    WrappedPlugin wplug2 = (WrappedPlugin)WrapperState.getWrapper(plugin);
    assertNotSame(wplug,wplug2);
  }

  public void testIsWrappedPlugin() throws Exception {
    Plugin wplug = (Plugin) Class.forName(
        WrapperState.WRAPPED_PLUGIN_NAME).newInstance();
    assertTrue(WrapperState.isWrappedPlugin(wplug));
  }

  public void testDaemon() throws Exception {
    MockLockssDaemon daemon = new MockLockssDaemon();
    Plugin plugin = new MockPlugin();
    WrappedPlugin wplug = (WrappedPlugin)WrapperState.getWrapper(plugin);
    Properties p = new Properties();
    p.setProperty(MockPlugin.KEY + "." + MockPlugin.CONFIG_PROP_1,
                  "http://www.example.com");
    p.setProperty(MockPlugin.KEY + "." + MockPlugin.CONFIG_PROP_2,"322");
    p.setProperty(MockPlugin.KEY + ".reserved.wrapper","true");
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION, tempDirPath);
    Configuration config = ConfigurationUtil.fromProps(p);
    assertNotNull(config);
    WrappedArchivalUnit wau = (WrappedArchivalUnit)wplug.createAu(config);

    assertNotNull(daemon.getNodeManager(wau));
    assertNotNull(daemon.getHistoryRepository(wau));
  }
}
