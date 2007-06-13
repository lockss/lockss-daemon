/*
 * $Id: TestEditableDefinablePlugin.java,v 1.24 2007-06-13 22:14:41 tlipkis Exp $
 */

/*

 Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.devtools.plugindef;

import java.io.*;
import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * <p>Title: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 * <p> </p>
 *  not attributable
 *
 */

public class TestEditableDefinablePlugin
    extends LockssTestCase {
  private EditableDefinablePlugin edPlugin = null;
  private String tempDirPath;

  protected void setUp() throws Exception {
    super.setUp();
    edPlugin = new EditableDefinablePlugin();
    edPlugin.initPlugin(getMockLockssDaemon());
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
  }

  protected void tearDown() throws Exception {
    edPlugin = null;
    super.tearDown();
  }

  public void testEditableDefinablePlugin() {
    edPlugin = new EditableDefinablePlugin();
  }

  public void testAddAndRemoveConfigParamDescr() {
    ConfigParamDescr descr1 = ConfigParamDescr.BASE_URL;
    ConfigParamDescr descr2 = ConfigParamDescr.VOLUME_NUMBER;

    edPlugin.addConfigParamDescr(descr1);
    edPlugin.addConfigParamDescr(descr2);

    List expected = ListUtil.list(descr1, descr2);
    List actual = edPlugin.getLocalAuConfigDescrs();
    assertIsomorphic("ConfigParamDescrs", expected, actual);

    edPlugin.removeConfigParamDescr(descr1.getKey());
    expected = ListUtil.list(descr2);
    assertIsomorphic("Remove ConfigParamDescr", expected, actual);
  }

  public void testAddAndRemoveCrawlRule() {
    String rule1 = CrawlRules.RE.MATCH_INCLUDE + "\n*.gif";
    String rule2 = CrawlRules.RE.MATCH_EXCLUDE + "\n*.pdf";

    edPlugin.addAuCrawlRule(rule1);
    edPlugin.addAuCrawlRule(rule2);
    List expected = ListUtil.list(rule1, rule2);
    List actual = (List) edPlugin.getMap().getCollection(DefinableArchivalUnit.KEY_AU_CRAWL_RULES, null);
    assertIsomorphic("CrawlRules", expected, actual);

    edPlugin.removeCrawlRule(rule2);
    expected = ListUtil.list(rule1);
    assertIsomorphic("CrawlRules", expected, actual);
  }

  public void testAddAndRemoveSingleExceptionHandler() throws
      ClassNotFoundException {

    // nothing installed should give the default
    String name =
        "org.lockss.util.urlconn.CacheException$NoRetryDeadLinkException";
    Class expected = Class.forName(name);
    Class actual = ( (HttpResultMap) edPlugin.getCacheResultMap()).
        getExceptionClass(404);
    assertEquals("default 404", expected, actual);

    // test remapping a 404
    int resultCode1 = 404;
    String exceptionClass =
        "org.lockss.util.urlconn.CacheException$RetryDeadLinkException";
    edPlugin.addSingleExceptionHandler(resultCode1, exceptionClass);
    Collection expList = ListUtil.list(resultCode1 + "=" + exceptionClass);
    Collection actList =
        edPlugin.getMap().getCollection(DefinablePlugin.KEY_EXCEPTION_LIST, null);
    assertIsomorphic("remapped 404", expList, actList);

    // test removing a remapping from multi-entry list
    int resultCode2 = 405;
    edPlugin.addSingleExceptionHandler(resultCode2, exceptionClass);
    edPlugin.removeSingleExceptionHandler(resultCode1);
    expList = ListUtil.list(resultCode2 + "=" + exceptionClass);
    actList = edPlugin.getMap().getCollection(DefinablePlugin.KEY_EXCEPTION_LIST,
                                              null);
    assertIsomorphic("removed 404", expList, actList);

    // test removing a remapping from a single entry list
    edPlugin.removeSingleExceptionHandler(resultCode2);
    actList = edPlugin.getMap().getCollection(DefinablePlugin.KEY_EXCEPTION_LIST,
                                              null);
    assertNull(actList);
  }

  public void testAddUserDefinedConfigParamDescrs() {

    ConfigParamDescr descr1 = new ConfigParamDescr();
    descr1.setKey("key1");
    descr1.setDisplayName("Descr One");
    descr1.setType(ConfigParamDescr.TYPE_POS_INT);
    descr1.setSize(8);
    edPlugin.addConfigParamDescr(descr1);

    ConfigParamDescr descr2 = new ConfigParamDescr();
    descr2.setKey("key2");
    descr2.setDisplayName("Descr Two");
    descr2.setType(ConfigParamDescr.TYPE_STRING);
    descr2.setSize(40);
    edPlugin.addConfigParamDescr(descr2);

    Collection expected = SetUtil.set(descr1, descr2);
    Collection actual = new HashSet();
    edPlugin.addUserDefinedConfigParamDescrs(actual);
    assertIsomorphic("defined params", expected, actual);
  }

  public void testGetKnownCacheExceptions() {
    HashSet set = new HashSet();
    set.add("org.lockss.util.urlconn.CacheSuccess");
    set.add("org.lockss.util.urlconn.CacheException$UnknownCodeException");
    set.add("org.lockss.util.urlconn.CacheException$RetryableException");
    set.add("org.lockss.util.urlconn.CacheException$RetrySameUrlException");
    set.add("org.lockss.util.urlconn.CacheException$RetryDeadLinkException");
    set.add("org.lockss.util.urlconn.CacheException$UnretryableException");
    set.add("org.lockss.util.urlconn.CacheException$HostException");
    set.add("org.lockss.util.urlconn.CacheException$RepositoryException");
    set.add("org.lockss.util.urlconn.CacheException$NoRetryNewUrlException");
    set.add("org.lockss.util.urlconn.CacheException$NoRetryPermUrlException");
    set.add("org.lockss.util.urlconn.CacheException$NoRetryTempUrlException");
    set.add("org.lockss.util.urlconn.CacheException$PermissionException");
    set.add("org.lockss.util.urlconn.CacheException$ExpectedNoRetryException");
    set.add("org.lockss.util.urlconn.CacheException$NoRetryDeadLinkException");
    set.add(
        "org.lockss.util.urlconn.CacheException$UnexpectedNoRetryFailException");
    set.add(
        "org.lockss.util.urlconn.CacheException$UnexpectedNoRetryNoFailException");
    set.add("org.lockss.util.urlconn.CacheException$NoRetryHostException");
    set.add("org.lockss.util.urlconn.CacheException$NoRetryRepositoryException");
    set.add("org.lockss.util.urlconn.CacheException$UnimplementedCodeException");
    set.add("org.lockss.util.urlconn.CacheException$MalformedURLException");

    Collection actual = edPlugin.getKnownCacheExceptions();
    assertEquals("return value", set, actual);

  }

  public void testGetKnownConfigParamDescrs() {
    Collection set = new HashSet();
    set.add(ConfigParamDescr.VOLUME_NUMBER);
    set.add(ConfigParamDescr.VOLUME_NAME);
    set.add(ConfigParamDescr.YEAR);
    set.add(ConfigParamDescr.BASE_URL);
    set.add(ConfigParamDescr.BASE_URL2);
    set.add(ConfigParamDescr.JOURNAL_ID);
    set.add(ConfigParamDescr.JOURNAL_ISSN);
    set.add(ConfigParamDescr.PUBLISHER_NAME);
    set.add(ConfigParamDescr.ISSUE_RANGE);
    set.add(ConfigParamDescr.NUM_ISSUE_RANGE);
    set.add(ConfigParamDescr.ISSUE_SET);
    set.add(ConfigParamDescr.OAI_REQUEST_URL);
    set.add(ConfigParamDescr.OAI_SPEC);
    Collection actualReturn = edPlugin.getKnownConfigParamDescrs();
    assertIsomorphic("default descrs", set, actualReturn);

    // add user-defined descr and check again
    ConfigParamDescr descr1 = new ConfigParamDescr();
    descr1.setKey("key1");
    descr1.setDisplayName("Descr One");
    descr1.setType(ConfigParamDescr.TYPE_POS_INT);
    descr1.setSize(8);
    edPlugin.addConfigParamDescr(descr1);
    set.add(descr1);
    actualReturn = edPlugin.getKnownConfigParamDescrs();
    assertIsomorphic("+ user defined", set, actualReturn);
  }

  public void testLoadAndWriteMap() throws Exception {
    String name = "edMap";
    // load the configuration map from jar file
    String mapFile = name.replace('.', '/') + ".xml";

    String location = tempDirPath + "testMap";
    String version = "Version 1.0";

    edPlugin.setPluginName(name);
    edPlugin.setPluginVersion(version);
    edPlugin.setAuCrawlWindowSer(makeCrawlWindow());
    edPlugin.writeMap(location, mapFile);
    // remove the items so we know we really loaded them
    edPlugin.removePluginName();
    edPlugin.removePluginVersion();

    edPlugin.loadMap(location, mapFile);
    assertEquals("name", name, edPlugin.getPluginName());
    assertEquals("version", version, edPlugin.getVersion());
  }

  public void testSetAndRemoveAuCrawlDepth() {
    int defDepth = 1;
    int expected = 4;
    int actual = edPlugin.getMap().getInt(DefinableArchivalUnit.KEY_AU_CRAWL_DEPTH, defDepth);
    assertEquals("default depth", defDepth, actual);

    edPlugin.setAuCrawlDepth(expected);
    actual = edPlugin.getMap().getInt(DefinableArchivalUnit.KEY_AU_CRAWL_DEPTH, defDepth);
    assertEquals("crawl depth", expected, actual);

    edPlugin.removeAuCrawlDepth();
    actual = edPlugin.getMap().getInt(DefinableArchivalUnit.KEY_AU_CRAWL_DEPTH, defDepth);
    assertEquals("default depth", defDepth, actual);

  }

  public void testSetAndRemoveAuCrawlRules() {
    Collection defRules = ListUtil.list();
    Collection expRules = ListUtil.list("rule1", "rule2");
    Collection actRules;
    // default assigned
    actRules = edPlugin.getMap().getCollection(DefinableArchivalUnit.KEY_AU_CRAWL_RULES, defRules);
    assertIsomorphic("default rules", defRules, actRules);

    // assign a list
    edPlugin.setAuCrawlRules(expRules);
    actRules = edPlugin.getMap().getCollection(DefinableArchivalUnit.KEY_AU_CRAWL_RULES, defRules);
    assertIsomorphic("default rules", expRules, actRules);

    // remove an restore default
    edPlugin.removeAuCrawlRules();
    actRules = edPlugin.getMap().getCollection(DefinableArchivalUnit.KEY_AU_CRAWL_RULES, defRules);
    assertIsomorphic("default rules", defRules, actRules);

  }

  public void testSetAndRemoveAuCrawlWindow() {

    CrawlWindow defWindow = null;
    CrawlWindow actWindow = null;
    CrawlWindow expWindow = makeCrawlWindow();

    // test default
    actWindow = (CrawlWindow) edPlugin.getMap().getMapElement(DefinableArchivalUnit.KEY_AU_CRAWL_WINDOW_SER);
    assertEquals("default window", defWindow, actWindow);

    // test good class name is ok
    edPlugin.setAuCrawlWindowSer(expWindow);
    actWindow = (CrawlWindow) edPlugin.getMap().getMapElement(DefinableArchivalUnit.KEY_AU_CRAWL_WINDOW_SER);
    assertEquals("set window", expWindow, actWindow);

    // test remove
    edPlugin.removeAuCrawlWindowSer();
    actWindow = (CrawlWindow) edPlugin.getMap().getMapElement(DefinableArchivalUnit.KEY_AU_CRAWL_WINDOW_SER);
    assertEquals("default window", defWindow, actWindow);
  }

  private CrawlWindow makeCrawlWindow() {
    Calendar start = Calendar.getInstance();
    start.set(Calendar.HOUR_OF_DAY,1);
    start.set(Calendar.MINUTE,13);
    Calendar end   = Calendar.getInstance();
    start.set(Calendar.HOUR_OF_DAY,22);
    start.set(Calendar.MINUTE,52);
    TimeZone timezone = TimeZone.getTimeZone("America/Los_Angeles");
    CrawlWindow expWindow = new CrawlWindows.Interval(start,end,CrawlWindows.TIME,timezone);
    return expWindow;
  }

  public void testSetAndRemoveAuExpectedBasePath() {
    String defPath = "foobar";
    String expPath = "http://www.example.com/expected";
    String actPath = null;

    // test default
    actPath = edPlugin.getMap().getString(DefinableArchivalUnit.KEY_AU_EXPECTED_BASE_PATH, defPath);
    assertEquals("default path", defPath, actPath);

    // test setting
    edPlugin.setAuExpectedBasePath(expPath);
    actPath = edPlugin.getMap().getString(DefinableArchivalUnit.KEY_AU_EXPECTED_BASE_PATH, defPath);
    assertEquals("expected path", expPath, actPath);

    // test remove
    edPlugin.removeAuExpectedBasePath();
    actPath = edPlugin.getMap().getString(DefinableArchivalUnit.KEY_AU_EXPECTED_BASE_PATH, defPath);
    assertEquals("default path", defPath, actPath);

  }

  public void testSetAndRemoveNewContentCrawlIntv() {
    long defDelay = 0L;
    long expDelay = 1000L;
    long actDelay = 0L;

    actDelay = edPlugin.getMap().getLong(DefinableArchivalUnit.KEY_AU_DEFAULT_NEW_CONTENT_CRAWL_INTERVAL, defDelay);
    assertEquals("default delay", defDelay, actDelay);

    edPlugin.setNewContentCrawlInterval(expDelay);
    actDelay = edPlugin.getMap().getLong(DefinableArchivalUnit.KEY_AU_DEFAULT_NEW_CONTENT_CRAWL_INTERVAL, defDelay);
    assertEquals("fetch delay", expDelay, actDelay);

    edPlugin.removeNewContentCrawlInterval();
    actDelay = edPlugin.getMap().getLong(DefinableArchivalUnit.KEY_AU_DEFAULT_NEW_CONTENT_CRAWL_INTERVAL, defDelay);
    assertEquals("default delay", defDelay, actDelay);

  }

  public void testSetAndRemoveAuFilter() {
    String mimetype = "text/html";
    String defFilter = null;
    String expFilter = "org.lockss.test.MockFilterRule";
    String actFilter = null;

    actFilter = edPlugin.getMap().getString(mimetype +
                                            DefinableArchivalUnit.SUFFIX_FILTER_RULE,
                                            defFilter);
    assertEquals("default filter", defFilter, actFilter);

    edPlugin.setAuFilter(mimetype, expFilter, true);
    actFilter = edPlugin.getMap().getString(mimetype +
                                            DefinableArchivalUnit.SUFFIX_FILTER_RULE,
                                            defFilter);
    assertEquals("Au filter", expFilter, actFilter);

    edPlugin.removeAuFilter(mimetype);
    actFilter = edPlugin.getMap().getString(mimetype +
                                            DefinableArchivalUnit.SUFFIX_FILTER_RULE,
                                            defFilter);
    assertEquals("default filter", defFilter, actFilter);

  }

  public void testSetAndRemoveAuManifestPage() {
    String defManifest = null;
    String expManifest = "http://www.example.com/manifest.html";
    String actManifest = null;

    actManifest = edPlugin.getMap().getString(DefinableArchivalUnit.KEY_AU_MANIFEST,
                                              defManifest);
    assertEquals("default manifest", defManifest, actManifest);

    edPlugin.setAuManifestPage(expManifest);
    actManifest = edPlugin.getMap().getString(DefinableArchivalUnit.KEY_AU_MANIFEST,
                                              defManifest);
    assertEquals("manifest", expManifest, actManifest);

    edPlugin.removeAuManifestPage();
    actManifest = edPlugin.getMap().getString(DefinableArchivalUnit.KEY_AU_MANIFEST,
                                              defManifest);
    assertEquals("default manifest", defManifest, actManifest);

  }

  public void testSetAndRemoveAuName() {
    String defName = "foo";
    String expName = "au name";
    String actName = null;

    actName = edPlugin.getMap().getString(DefinableArchivalUnit.KEY_AU_NAME, defName);
    assertEquals("default name", defName, actName);

    edPlugin.setAuName(expName);
    actName = edPlugin.getMap().getString(DefinableArchivalUnit.KEY_AU_NAME, defName);
    assertEquals("au name", expName, actName);

    edPlugin.removeAuName();
    actName = edPlugin.getMap().getString(DefinableArchivalUnit.KEY_AU_NAME, defName);
    assertEquals("default name", defName, actName);

  }

  public void testSetAndRemoveAuPauseTime() {
    long defPause = 0L;
    long expPause = 1000L;
    long actPause = 0L;

    actPause = edPlugin.getMap().getLong(DefinableArchivalUnit.KEY_AU_DEFAULT_PAUSE_TIME, defPause);
    assertEquals("default pause time", defPause, actPause);

    edPlugin.setAuPauseTime(expPause);
    actPause = edPlugin.getMap().getLong(DefinableArchivalUnit.KEY_AU_DEFAULT_PAUSE_TIME, defPause);
    assertEquals("set pause time", expPause, actPause);

    edPlugin.removeAuPauseTime();
    actPause = edPlugin.getMap().getLong(DefinableArchivalUnit.KEY_AU_DEFAULT_PAUSE_TIME, defPause);
    assertEquals("default pause time", defPause, actPause);

  }

  public void testSetAndRemoveAuStartURL() {
    String defUrl = "foo";
    String expUrl = "http://www.example.com/index.html";
    String actUrl = null;

    actUrl = edPlugin.getMap().getString(DefinableArchivalUnit.KEY_AU_START_URL, defUrl);
    assertEquals("default startUrl", defUrl, actUrl);

    edPlugin.setAuStartUrl(expUrl);
    actUrl = edPlugin.getMap().getString(DefinableArchivalUnit.KEY_AU_START_URL, defUrl);
    assertEquals("startUrl", expUrl, actUrl);

    edPlugin.removeAuStartUrl();
    actUrl = edPlugin.getMap().getString(DefinableArchivalUnit.KEY_AU_START_URL, defUrl);
    assertEquals("default startUrl", defUrl, actUrl);

  }

  public void testSetAndRemovePluginConfigDescrs() {
    Collection defDescrs = null;
    Collection actDescrs = null;

    actDescrs = edPlugin.getMap().getCollection(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS,
                                                defDescrs);
    assertEquals("def descrs", defDescrs, actDescrs);

    HashSet descrs = new HashSet();
    descrs.add(ConfigParamDescr.BASE_URL);
    descrs.add(ConfigParamDescr.YEAR);
    edPlugin.setPluginConfigDescrs(descrs);

    Collection expDescrs = ListUtil.fromArray(descrs.toArray());
    actDescrs = edPlugin.getMap().getCollection(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS,
                                                defDescrs);
    assertEquals("config descrs", expDescrs, actDescrs);

    edPlugin.removePluginConfigDescrs();
    actDescrs = edPlugin.getMap().getCollection(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS,
                                                defDescrs);
    assertEquals("def descrs", defDescrs, actDescrs);

  }

  public void testSetAndRemovePluginExceptionHandler() {
    String actHandler = null;
    actHandler = edPlugin.getMap().getString(DefinablePlugin.KEY_EXCEPTION_HANDLER,
                                             null);
    assertNull(actHandler);
    String expHandler = new MockHttpResultHandler().getClass().getName();
    edPlugin.setPluginExceptionHandler(expHandler, true);
    actHandler = edPlugin.getMap().getString(DefinablePlugin.KEY_EXCEPTION_HANDLER,
                                             null);
    assertEquals("resultHandler", expHandler, actHandler);

    edPlugin.removePluginExceptionHandler();
    actHandler = edPlugin.getMap().getString(DefinablePlugin.KEY_EXCEPTION_HANDLER,
                                             null);
    assertNull(actHandler);

  }

  public void testSetAndRemovePluginName() {
    String defName = null;
    String actName = null;
    String expName = "My Plugin";

    actName = edPlugin.getMap().getString(DefinablePlugin.KEY_PLUGIN_NAME, defName);
    assertEquals("default name", defName, actName);

    edPlugin.setPluginName(expName);
    actName = edPlugin.getMap().getString(DefinablePlugin.KEY_PLUGIN_NAME, defName);
    assertEquals("default name", expName, actName);

    edPlugin.removePluginName();
    actName = edPlugin.getMap().getString(DefinablePlugin.KEY_PLUGIN_NAME, defName);
    assertEquals("default name", defName, actName);

  }

  public void testSetAndRemovePluginVersion() {
    String defVersion = "1.0";
    String expVersion = "2.0";
    String actVersion = null;

    actVersion = edPlugin.getMap().getString(DefinablePlugin.KEY_PLUGIN_VERSION,
                                             defVersion);
    assertEquals("default version", defVersion, actVersion);

    edPlugin.setPluginVersion(expVersion);
    actVersion = edPlugin.getMap().getString(DefinablePlugin.KEY_PLUGIN_VERSION,
                                             defVersion);
    assertEquals("default version", expVersion, actVersion);

    edPlugin.removePluginVersion();
    actVersion = edPlugin.getMap().getString(DefinablePlugin.KEY_PLUGIN_VERSION,
                                             defVersion);
    assertEquals("default version", defVersion, actVersion);

  }

  public void testSetMapName() {
    // no extension
    String name = "foo";
    assertNull(edPlugin.getMapName());
    edPlugin.setMapName(name);
    assertEquals(name + DefinablePlugin.MAP_SUFFIX, edPlugin.getMapName());

    // with extension
    name = "bar.xml";
    edPlugin.setMapName(name);
    assertEquals(name, edPlugin.getMapName());

  }

}
