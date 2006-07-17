/*
 * $Id: TestRegistryArchivalUnit.java,v 1.4 2006-07-17 05:09:43 tlipkis Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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
import junit.framework.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.state.*;

/**
 * Test class for org.lockss.plugin.RegistryArchivalUnit
 */
public class TestRegistryArchivalUnit extends LockssTestCase {
  static Logger log = Logger.getLogger("TestRegistryArchivalUnit");
  private RegistryPlugin regPlugin;
  private MockLockssDaemon daemon;
  private PluginManager pluginMgr;
  String baseUrl = "http://foo.com/bar";

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    // make and init a real Pluginmgr
    pluginMgr = daemon.getPluginManager();

    // Make and start a UrlManager to set up the URLStreamHandlerFactory.
    // This is all so the cuurl created below can be opened by the parser
    UrlManager uMgr = new UrlManager();
    uMgr.initService(daemon);
    daemon.setDaemonInited(true);
    uMgr.startService();
    regPlugin = new MyRegistryPlugin();
    regPlugin.initPlugin(daemon);
  }

  public void tearDown() throws Exception {
    super.tearDown();
    // more...
  }

  public void testLoadAuConfigDescrs()
      throws ArchivalUnit.ConfigurationException {
    Properties auProps = new Properties();
    auProps.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    Configuration auConfig = ConfigurationUtil.fromProps(auProps);

    Properties props = new Properties();
    props.setProperty(RegistryArchivalUnit.PARAM_REGISTRY_TREEWALK_START,
		      "93m");
    props.setProperty(RegistryArchivalUnit.PARAM_REGISTRY_CRAWL_INTERVAL,
		      "107m");
    props.setProperty(RegistryArchivalUnit.PARAM_REGISTRY_FETCH_RATE,
		      "4/2s");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    ArchivalUnit au = regPlugin.createAu(auConfig);
    TypedEntryMap paramMap = au.getProperties();
    assertEquals(93 * Constants.MINUTE,
		 paramMap.getLong(TreeWalkManager.PARAM_TREEWALK_START_DELAY));
    assertEquals(107 * Constants.MINUTE,
		 paramMap.getLong(ArchivalUnit.AU_NEW_CRAWL_INTERVAL));
    RateLimiter limiter = au.findFetchRateLimiter();
    assertEquals("4/2s", limiter.getRate());
    assertEquals("org|lockss|plugin|TestRegistryArchivalUnit$MyRegistryPlugin&base_url~http%3A%2F%2Ffoo%2Ecom%2Fbar", au.getAuId());
    props.setProperty(RegistryArchivalUnit.PARAM_REGISTRY_FETCH_RATE,
		      "3/7s");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    assertEquals("3/7s", au.findFetchRateLimiter().getRate());
  }

  public void testShouldCallTopLevelPoll() throws Exception {
    RegistryArchivalUnit au = new RegistryArchivalUnit(regPlugin);
    // Expect that "shouldCallTopLevelPoll" will always return false.
    assertFalse(au.shouldCallTopLevelPoll(null));
  }

  public void testRecomputeRegNameTitle() throws Exception {
    Properties auProps = new Properties();
    auProps.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    Configuration auConfig = ConfigurationUtil.fromProps(auProps);
    MyRegistryArchivalUnit au = new MyRegistryArchivalUnit(regPlugin);
    au.setConfiguration(auConfig);
    PluginTestUtil.registerArchivalUnit(regPlugin, au);
    TypedEntryMap map = au.getProperties();
    au.addContent(map.getString(ArchivalUnit.AU_START_URL),
		  "<html><head><h2>foobar</h2>\n" +
		  "<title>This Title No Verb</title></head></html>");
    assertEquals("This Title No Verb", au.recomputeRegName());
  }

  public void testRecomputeRegNameTowTitles() throws Exception {
    Properties auProps = new Properties();
    auProps.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    Configuration auConfig = ConfigurationUtil.fromProps(auProps);
    MyRegistryArchivalUnit au = new MyRegistryArchivalUnit(regPlugin);
    au.setConfiguration(auConfig);
    PluginTestUtil.registerArchivalUnit(regPlugin, au);
    TypedEntryMap map = au.getProperties();
    au.addContent(map.getString(ArchivalUnit.AU_START_URL),
		  "<html><head><h2>foobar</h2>\n" +
		  "<title>First Title No Verb</title>" +
		  "<title>Second Title No Verb</title></head></html>");
    assertEquals("First Title No Verb", au.recomputeRegName());
  }

  public void testRecomputeRegNameNoTitle() throws Exception {
    Properties auProps = new Properties();
    auProps.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    Configuration auConfig = ConfigurationUtil.fromProps(auProps);
    MyRegistryArchivalUnit au = new MyRegistryArchivalUnit(regPlugin);
    au.setConfiguration(auConfig);
    PluginTestUtil.registerArchivalUnit(regPlugin, au);
    TypedEntryMap map = au.getProperties();
    au.addContent(map.getString(ArchivalUnit.AU_START_URL),
		  "<html><h3>This Page No Title</h3></html>");
    assertEquals(null, au.recomputeRegName());
  }

  // Both of these methods are currently empty implementations on
  // RegistryPlugin, but it's nice to exercise them anyway, since they
  // are part of Plugin's public interface.

  public void testSetTitleConfigFromConfig() throws Exception {
    regPlugin.setTitleConfigFromConfig(null);
  }

  static class MyRegistryPlugin
    extends RegistryPlugin implements PluginTestable{
    public void registerArchivalUnit(ArchivalUnit au) {
      aus.add(au);
    }

    public void unregisterArchivalUnit(ArchivalUnit au) {
      aus.remove(au);
    }

    protected RegistryArchivalUnit newRegistryArchivalUnit() {
      return new MyRegistryArchivalUnit(this);
    }
  }

  static class MyRegistryArchivalUnit extends RegistryArchivalUnit {
    Map cumap = new HashMap();
    public MyRegistryArchivalUnit(RegistryPlugin plugin) {
      super(plugin);
    }
    public void addContent(String url, String content) {
      MockCachedUrl cu = new MockCachedUrl(url, this);
      cu.setContent(content);
      cu.setExists(true);
      cu.setProperties(new CIProperties());
      cumap.put(url, cu);
    }
    public CachedUrl makeCachedUrl(String url) {
      return (CachedUrl)cumap.get(url);
    }
  }

}
