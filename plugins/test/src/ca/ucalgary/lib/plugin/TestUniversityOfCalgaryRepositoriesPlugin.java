/*
 * $Id: $
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package ca.ucalgary.lib.plugin;

import java.net.MalformedURLException;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.ListUtil;

public class TestUniversityOfCalgaryRepositoriesPlugin extends LockssTestCase {
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String COLLECTION_ID_KEY = "collection_id";
  
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;
  
  public TestUniversityOfCalgaryRepositoriesPlugin(String msg) {
    super(msg);
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "ca.ucalgary.lib.plugin.UniversityOfCalgaryRepositoriesPlugin");
    
  }
  
  public void testGetAuNullConfig()
      throws ArchivalUnit.ConfigurationException {
    try {
      plugin.configureAu(null, null);
      fail("Didn't throw ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  public void testCreateAu() throws ConfigurationException {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(COLLECTION_ID_KEY, "cid");
    makeAuFromProps(props);
  }
  
  private DefinableArchivalUnit makeAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return (DefinableArchivalUnit)plugin.configureAu(config, null);
  }
  
  public void testGetAuConstructsProperAu()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(COLLECTION_ID_KEY, "cid");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    
    String starturl =
        "http://www.example.com/lockss/cid/index.html";
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("University of Calgary Repositories Plugin, Base URL http://www.example.com/, Collection ID cid",
        au.getName());
    assertEquals(ListUtil.list(starturl), au.getStartUrls());
  }
  
  public void testGetPluginId() {
    assertEquals("ca.ucalgary.lib.plugin.UniversityOfCalgaryRepositoriesPlugin",
        plugin.getPluginId());
  }
  
  // Test the crawl rules for eLife
  public void testShouldCacheProperPages() throws Exception {
    String ROOT_URL = "http://www.example.com/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(COLLECTION_ID_KEY, "cid");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    
    // Test for pages that should get crawled or not
    // permission page/start url 
    shouldCacheTest(ROOT_URL + "lockss/cid/index.html", true, au);
    shouldCacheTest(ROOT_URL + "lockss/2003/index.html", false, au);
    shouldCacheTest(ROOT_URL + "lockss/index.html", false, au);
    shouldCacheTest(ROOT_URL + "index.html", false, au);
    // content 
    shouldCacheTest(ROOT_URL + "lockss/cid/cid.7z", true, au);
    shouldCacheTest(ROOT_URL + "lockss/cid/foo/bar", true, au);
    
    shouldCacheTest("", false, au);
    
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au);
    
  }
  
  private void shouldCacheTest(String url, boolean shouldCache, ArchivalUnit au) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
}