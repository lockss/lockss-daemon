/*
 * $Id$
 */

/*

 Copyright (c) 2013-2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.subscription;

import static org.lockss.db.SqlConstants.*;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.Tdb;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbProvider;
import org.lockss.config.TdbTestUtil;
import org.lockss.config.TdbTitle;
import org.lockss.config.Tdb.TdbException;
import org.lockss.daemon.ConfigParamAssignment;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.TitleConfig;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.plugin.simulated.SimulatedPlugin;
import org.lockss.protocol.MockIdentityManager;
import org.lockss.remote.RemoteApi;
import org.lockss.remote.RemoteApi.BatchAuStatus;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.MockPlugin;
import org.lockss.util.ListUtil;
import org.lockss.util.PlatformUtil;

/**
 * Test class for org.lockss.subscription.SubscriptionManager.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class TestSubscriptionManager extends LockssTestCase {
  private static final String BASE_URL = "http://www.example.com/foo/";

  private String tempDirPath;
  private PluginManager pluginManager;
  private SubscriptionManager subManager;
  private DbManager dbManager;
  private MetadataManager metadataManager;
  private MockPlugin plugin;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = setUpDiskSpace();

    ConfigurationUtil.addFromArgs(MetadataManager.PARAM_INDEXING_ENABLED,
	"true");
    ConfigurationUtil.addFromArgs(SubscriptionManager
	.PARAM_SUBSCRIPTION_ENABLED, "true");

    MockLockssDaemon theDaemon = getMockLockssDaemon();
    theDaemon.setDaemonInited(true);

    pluginManager = theDaemon.getPluginManager();
    pluginManager.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginManager.startService();

    String key = PluginManager.pluginKeyFromName(MockPlugin.class.getName());
    pluginManager.ensurePluginLoaded(key);
    plugin = (MockPlugin)pluginManager.getPlugin(key);

    theDaemon.setAusStarted(true);

    PluginTestUtil.createAndStartSimAu(SimulatedPlugin.class,
	simAuConfig(tempDirPath + "/0"));

    MockIdentityManager idm = new MockIdentityManager();
    theDaemon.setIdentityManager(idm);

    RemoteApi remoteApi = theDaemon.getRemoteApi();
    remoteApi.startService();

    dbManager = getTestDbManager(tempDirPath);

    metadataManager = new MetadataManager();
    theDaemon.setMetadataManager(metadataManager);
    metadataManager.initService(theDaemon);
    metadataManager.startService();

    subManager = new SubscriptionManager();
    theDaemon.setSubscriptionManager(subManager);
    subManager.initService(theDaemon);
    subManager.startService();
  }

  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "1");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
                                SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }

  /**
   * Creates a TdbAu from passed properties.
   * 
   * @param properties
   *          A Properties with the properties of the TdbAu to be created.
   */
  private TdbAu createTdbAu(Properties properties) throws TdbException {
    Tdb tdb = new Tdb();
    tdb.addTdbAuFromProperties(properties);

    for (TdbAu tdbAu : tdb.getTdbAusByName(properties.getProperty("title"))) {
      return tdbAu;
    }

    return null;
  }

  /**
   * Check the behavior of populateRepositories().
   */
  public final void testPopulateRepositories() {
    Map<String, PlatformUtil.DF> repositoryMap =
	new HashMap<String, PlatformUtil.DF>();

    List<String> repos = subManager.populateRepositories(repositoryMap);
    assertEquals(0, repos.size());

    repositoryMap = new HashMap<String, PlatformUtil.DF>();

    repositoryMap.put("dfn", PlatformUtil.DF.makeThreshold(-1L, 0.5));

    repos = subManager.populateRepositories(repositoryMap);
    assertEquals(1, repos.size());
    assertEquals("dfn", repos.get(0));

    repositoryMap = new HashMap<String, PlatformUtil.DF>();

    repositoryMap.put("df0", PlatformUtil.DF.makeThreshold(0L, 0.5));

    repos = subManager.populateRepositories(repositoryMap);
    assertEquals(1, repos.size());
    assertEquals("df0", repos.get(0));

    repositoryMap = new HashMap<String, PlatformUtil.DF>();

    repositoryMap.put("df1", PlatformUtil.DF.makeThreshold(1L, 0.5));

    repos = subManager.populateRepositories(repositoryMap);
    assertEquals(1, repos.size());
    assertEquals("df1", repos.get(0));

    repositoryMap = new HashMap<String, PlatformUtil.DF>();

    repositoryMap.put("dfn", PlatformUtil.DF.makeThreshold(-1L, 0.5));
    repositoryMap.put("df0", PlatformUtil.DF.makeThreshold(0L, 0.5));

    repos = subManager.populateRepositories(repositoryMap);
    assertEquals(1, repos.size());
    assertEquals("df0", repos.get(0));

    repositoryMap = new HashMap<String, PlatformUtil.DF>();

    repositoryMap.put("dfn", PlatformUtil.DF.makeThreshold(-1L, 0.5));
    repositoryMap.put("df0", PlatformUtil.DF.makeThreshold(0L, 0.5));
    repositoryMap.put("df1", PlatformUtil.DF.makeThreshold(1L, 0.5));

    repos = subManager.populateRepositories(repositoryMap);
    assertEquals(1, repos.size());
    assertEquals("df1", repos.get(0));

    repositoryMap = new HashMap<String, PlatformUtil.DF>();

    repositoryMap.put("df1", PlatformUtil.DF.makeThreshold(1L, 0.5));
    repositoryMap.put("df2", PlatformUtil.DF.makeThreshold(2L, 0.5));

    repos = subManager.populateRepositories(repositoryMap);
    assertEquals(3, repos.size());
    assertEquals("df2", repos.get(0));
    assertEquals("df1", repos.get(1));
    assertEquals("df2", repos.get(2));

    repositoryMap = new HashMap<String, PlatformUtil.DF>();

    repositoryMap.put("df5", PlatformUtil.DF.makeThreshold(5L, 0.5));
    repositoryMap.put("df1", PlatformUtil.DF.makeThreshold(1L, 0.5));

    repos = subManager.populateRepositories(repositoryMap);
    assertEquals(6, repos.size());
    assertEquals("df5", repos.get(0));
    assertEquals("df1", repos.get(1));
    assertEquals("df5", repos.get(2));
    assertEquals("df5", repos.get(3));
    assertEquals("df5", repos.get(4));
    assertEquals("df5", repos.get(5));

    repositoryMap = new HashMap<String, PlatformUtil.DF>();

    repositoryMap.put("df2", PlatformUtil.DF.makeThreshold(2L, 0.5));
    repositoryMap.put("df3", PlatformUtil.DF.makeThreshold(3L, 0.5));

    repos = subManager.populateRepositories(repositoryMap);
    assertEquals(5, repos.size());
    assertEquals("df3", repos.get(0));
    assertEquals("df2", repos.get(1));
    assertEquals("df3", repos.get(2));
    assertEquals("df2", repos.get(3));
    assertEquals("df3", repos.get(4));

    repositoryMap = new HashMap<String, PlatformUtil.DF>();

    repositoryMap.put("df1", PlatformUtil.DF.makeThreshold(1L, 0.5));
    repositoryMap.put("df50", PlatformUtil.DF.makeThreshold(50L, 0.5));

    repos = subManager.populateRepositories(repositoryMap);
    assertEquals(34, repos.size());
    assertEquals("df50", repos.get(0));
    assertEquals("df1", repos.get(1));
    assertEquals("df50", repos.get(33));

    repositoryMap = new HashMap<String, PlatformUtil.DF>();

    repositoryMap.put("df4", PlatformUtil.DF.makeThreshold(4L, 0.5));
    repositoryMap.put("df2", PlatformUtil.DF.makeThreshold(2L, 0.5));
    repositoryMap.put("df1", PlatformUtil.DF.makeThreshold(1L, 0.5));

    repos = subManager.populateRepositories(repositoryMap);
    assertEquals(7, repos.size());
    assertEquals("df4", repos.get(0));
    assertEquals("df2", repos.get(1));
    assertEquals("df1", repos.get(2));
    assertEquals("df4", repos.get(3));
    assertEquals("df4", repos.get(4));
    assertEquals("df2", repos.get(5));
    assertEquals("df4", repos.get(6));

    repositoryMap = new HashMap<String, PlatformUtil.DF>();

    repositoryMap.put("df5", PlatformUtil.DF.makeThreshold(5L, 0.5));
    repositoryMap.put("df6", PlatformUtil.DF.makeThreshold(6L, 0.5));
    repositoryMap.put("df3", PlatformUtil.DF.makeThreshold(3L, 0.5));

    repos = subManager.populateRepositories(repositoryMap);
    assertEquals(14, repos.size());
    assertEquals("df6", repos.get(0));
    assertEquals("df5", repos.get(1));
    assertEquals("df3", repos.get(2));
    assertEquals("df6", repos.get(3));
    assertEquals("df5", repos.get(4));
    assertEquals("df6", repos.get(5));
    assertEquals("df3", repos.get(6));
    assertEquals("df5", repos.get(7));
    assertEquals("df6", repos.get(8));
    assertEquals("df5", repos.get(9));
    assertEquals("df6", repos.get(10));
    assertEquals("df3", repos.get(11));
    assertEquals("df5", repos.get(12));
    assertEquals("df6", repos.get(13));

    repositoryMap = new HashMap<String, PlatformUtil.DF>();

    repositoryMap.put("df1", PlatformUtil.DF.makeThreshold(1L, 0.5));
    repositoryMap.put("df3", PlatformUtil.DF.makeThreshold(3L, 0.5));
    repositoryMap.put("df2", PlatformUtil.DF.makeThreshold(2L, 0.5));
    repositoryMap.put("df4", PlatformUtil.DF.makeThreshold(4L, 0.5));

    repos = subManager.populateRepositories(repositoryMap);
    assertEquals(10, repos.size());
    assertEquals("df4", repos.get(0));
    assertEquals("df3", repos.get(1));
    assertEquals("df2", repos.get(2));
    assertEquals("df1", repos.get(3));
    assertEquals("df4", repos.get(4));
    assertEquals("df3", repos.get(5));
    assertEquals("df4", repos.get(6));
    assertEquals("df2", repos.get(7));
    assertEquals("df3", repos.get(8));
    assertEquals("df4", repos.get(9));
  }
  
  /**
   * Check the behavior of isRangeValid().
   * @throws TdbException for invalid Tdb operations
   */
  public void testIsRangeValid() throws TdbException {
    
    TdbTitle title = TdbTestUtil.makeRangeTestTitle(false);

    List<TdbAu> tdbAus = title.getSortedTdbAus();
    assertEquals(3, tdbAus.size());

    TdbAu tdbAu54 = tdbAus.get(0);

    tdbAu54.setPublicationRanges(Arrays
	.asList(new BibliographicPeriod("1954(4)(2)"),
	    new BibliographicPeriod("1954(4)(4)-1954(4)(6)")));

    TdbAu tdbAu78 = tdbAus.get(1);

    tdbAu78.setPublicationRanges(Arrays
	.asList(new BibliographicPeriod("1978")));

    TdbAu tdbAu88 = tdbAus.get(2);

    tdbAu88.setPublicationRanges(Arrays.
	asList(new BibliographicPeriod("1988(12)(28)-2001(1)(1)")));

    // Create the publication corresponding to the archival unit.
    SerialPublication publication = new SerialPublication();
    publication.setPublicationName("Test Publication");
    publication.setTdbTitle(title);

    assertTrue(isRangeValid("-", publication));
    assertTrue(isRangeValid("", publication));
    assertTrue(isRangeValid(" ", publication));
    assertTrue(isRangeValid("1953-", publication));
    assertTrue(isRangeValid("1954-", publication));
    assertTrue(isRangeValid("1955-", publication));
    assertTrue(isRangeValid("1978-", publication));
    assertTrue(isRangeValid("1979-", publication));
    assertTrue(isRangeValid("1988-", publication));
    assertTrue(isRangeValid("1989-", publication));
    assertTrue(isRangeValid("2001-", publication));
    assertTrue(isRangeValid("2002-", publication));
    assertTrue(isRangeValid("-1953", publication));
    assertTrue(isRangeValid("-1954", publication));
    assertTrue(isRangeValid("-1955", publication));
    assertTrue(isRangeValid("-1978", publication));
    assertTrue(isRangeValid("-1979", publication));
    assertTrue(isRangeValid("-1988", publication));
    assertTrue(isRangeValid("-1989", publication));
    assertTrue(isRangeValid("-2001", publication));
    assertTrue(isRangeValid("-2002", publication));
    assertTrue(isRangeValid("1953-1953", publication));
    assertTrue(isRangeValid("1953-1954", publication));
    assertTrue(isRangeValid("1953-1955", publication));
    assertTrue(isRangeValid("1953-1978", publication));
    assertTrue(isRangeValid("1953-1979", publication));
    assertTrue(isRangeValid("1953-1988", publication));
    assertTrue(isRangeValid("1953-1989", publication));
    assertTrue(isRangeValid("1953-2001", publication));
    assertTrue(isRangeValid("1953-2002", publication));
    assertTrue(isRangeValid("1954-1954", publication));
    assertTrue(isRangeValid("1954-1955", publication));
    assertTrue(isRangeValid("1954-1978", publication));
    assertTrue(isRangeValid("1954-1979", publication));
    assertTrue(isRangeValid("1954-1988", publication));
    assertTrue(isRangeValid("1954-1989", publication));
    assertTrue(isRangeValid("1954-2001", publication));
    assertTrue(isRangeValid("1954-2002", publication));
    assertTrue(isRangeValid("1955-1955", publication));
    assertTrue(isRangeValid("1955-1978", publication));
    assertTrue(isRangeValid("1955-1979", publication));
    assertTrue(isRangeValid("1955-1988", publication));
    assertTrue(isRangeValid("1955-1989", publication));
    assertTrue(isRangeValid("1955-2001", publication));
    assertTrue(isRangeValid("1955-2002", publication));
    assertTrue(isRangeValid("1978-1978", publication));
    assertTrue(isRangeValid("1978-1979", publication));
    assertTrue(isRangeValid("1978-1988", publication));
    assertTrue(isRangeValid("1978-1989", publication));
    assertTrue(isRangeValid("1978-2001", publication));
    assertTrue(isRangeValid("1978-2002", publication));
    assertTrue(isRangeValid("1979-1979", publication));
    assertTrue(isRangeValid("1979-1988", publication));
    assertTrue(isRangeValid("1979-1989", publication));
    assertTrue(isRangeValid("1979-2001", publication));
    assertTrue(isRangeValid("1979-2002", publication));
    assertTrue(isRangeValid("1988-1988", publication));
    assertTrue(isRangeValid("1988-1989", publication));
    assertTrue(isRangeValid("1988-2001", publication));
    assertTrue(isRangeValid("1988-2002", publication));
    assertTrue(isRangeValid("1989-1989", publication));
    assertTrue(isRangeValid("1989-2001", publication));
    assertTrue(isRangeValid("1989-2002", publication));
    assertTrue(isRangeValid("2001-2001", publication));
    assertTrue(isRangeValid("2001-2002", publication));
    assertTrue(isRangeValid("2002-2002", publication));

    assertFalse(isRangeValid("1953-1953(1)(2)", publication));
    assertFalse(isRangeValid("1953-1954(1)(1)", publication));
    assertFalse(isRangeValid("1953-1954(4)", publication));
    assertFalse(isRangeValid("1953-(4)", publication));
    assertFalse(isRangeValid("1953-1954()(2)", publication));
    assertFalse(isRangeValid("1953-1954()(4)", publication));
    assertFalse(isRangeValid("1953-1954()(6)", publication));
    assertFalse(isRangeValid("1953-()(2)", publication));
    assertFalse(isRangeValid("1953-()(4)", publication));
    assertFalse(isRangeValid("1953-()(6)", publication));
    assertFalse(isRangeValid("1953-1954(4)(1)", publication));
    assertTrue(isRangeValid("1953-1954(4)(2)", publication));
    assertFalse(isRangeValid("1953-1954(4)(3)", publication));
    assertTrue(isRangeValid("1953-1954(4)(4)", publication));
    assertFalse(isRangeValid("1953-1954(4)(5)", publication));
    assertTrue(isRangeValid("1953-1954(4)(6)", publication));
    assertFalse(isRangeValid("1953-1954(4)(7)", publication));
    assertFalse(isRangeValid("1953-1955(4)(2)", publication));
    assertFalse(isRangeValid("1953-1978(1)(2)", publication));
    assertFalse(isRangeValid("1953-1988(11)(28)", publication));
    assertFalse(isRangeValid("1953-1988(12)", publication));
    assertFalse(isRangeValid("1953-(12)", publication));
    assertFalse(isRangeValid("1953-1988()(28)", publication));
    assertFalse(isRangeValid("1953-()(28)", publication));
    assertFalse(isRangeValid("1953-1988(12)(27)", publication));
    assertTrue(isRangeValid("1953-1988(12)(28)", publication));
    assertFalse(isRangeValid("1953-1989(8)(9)", publication));
    assertFalse(isRangeValid("1953-1990(9)(0)", publication));
    assertFalse(isRangeValid("1953-1991(9)(1)", publication));
    assertFalse(isRangeValid("1953-1992(9)(2)", publication));
    assertFalse(isRangeValid("1953-1993(9)(3)", publication));
    assertFalse(isRangeValid("1953-1994(9)(4)", publication));
    assertFalse(isRangeValid("1953-1995(9)(5)", publication));
    assertFalse(isRangeValid("1953-1996(9)(6)", publication));
    assertFalse(isRangeValid("1953-1997(9)(7)", publication));
    assertFalse(isRangeValid("1953-1998(9)(8)", publication));
    assertFalse(isRangeValid("1953-1999(9)(9)", publication));
    assertFalse(isRangeValid("1953-2000(0)(0)", publication));
    assertFalse(isRangeValid("1953-2001(1)", publication));
    assertFalse(isRangeValid("1953-(1)", publication));
    assertFalse(isRangeValid("1953-2001()(1)", publication));
    assertFalse(isRangeValid("1953-()(1)", publication));
    assertFalse(isRangeValid("1953-2001(1)(0)", publication));
    assertTrue(isRangeValid("1953-2001(1)(1)", publication));
    assertFalse(isRangeValid("1953-2001(1)(2)", publication));
    assertFalse(isRangeValid("1953-2002(1)(1)", publication));
    assertTrue(isRangeValid("1954-1955(4)(2)", publication));
    assertTrue(isRangeValid("1954-1978(1)(2)", publication));
    assertTrue(isRangeValid("1954-1988(11)(28)", publication));
    assertTrue(isRangeValid("1954-1988(12)(27)", publication));
    assertTrue(isRangeValid("1954-1988(12)(28)", publication));
    assertTrue(isRangeValid("1954-1989(8)(9)", publication));
    assertTrue(isRangeValid("1954-1990(9)(0)", publication));
    assertTrue(isRangeValid("1954-1991(9)(1)", publication));
    assertTrue(isRangeValid("1954-1992(9)(2)", publication));
    assertTrue(isRangeValid("1954-1993(9)(3)", publication));
    assertTrue(isRangeValid("1954-1994(9)(4)", publication));
    assertTrue(isRangeValid("1954-1995(9)(5)", publication));
    assertTrue(isRangeValid("1954-1996(9)(6)", publication));
    assertTrue(isRangeValid("1954-1997(9)(7)", publication));
    assertTrue(isRangeValid("1954-1998(9)(8)", publication));
    assertTrue(isRangeValid("1954-1999(9)(9)", publication));
    assertTrue(isRangeValid("1954-2000(0)(0)", publication));
    assertTrue(isRangeValid("1954-2001(1)(0)", publication));
    assertTrue(isRangeValid("1954-2001(1)(1)", publication));
    assertTrue(isRangeValid("1954-2001(1)(2)", publication));
    assertTrue(isRangeValid("1954-2002(1)(1)", publication));
    assertFalse(isRangeValid("1954(4)-1955(4)(2)", publication));
    assertFalse(isRangeValid("1954(4)-1978(1)(2)", publication));
    assertFalse(isRangeValid("1954(4)-1988(11)(28)", publication));
    assertFalse(isRangeValid("1954(4)-1988(12)(27)", publication));
    assertTrue(isRangeValid("1954(4)-1988(12)(28)", publication));
    assertFalse(isRangeValid("1954(4)-1989(8)(9)", publication));
    assertFalse(isRangeValid("1954(4)-1990(9)(0)", publication));
    assertFalse(isRangeValid("1954(4)-1991(9)(1)", publication));
    assertFalse(isRangeValid("1954(4)-1992(9)(2)", publication));
    assertFalse(isRangeValid("1954(4)-1993(9)(3)", publication));
    assertFalse(isRangeValid("1954(4)-1994(9)(4)", publication));
    assertFalse(isRangeValid("1954(4)-1995(9)(5)", publication));
    assertFalse(isRangeValid("1954(4)-1996(9)(6)", publication));
    assertFalse(isRangeValid("1954(4)-1997(9)(7)", publication));
    assertFalse(isRangeValid("1954(4)-1998(9)(8)", publication));
    assertFalse(isRangeValid("1954(4)-1999(9)(9)", publication));
    assertFalse(isRangeValid("1954(4)-2000(0)(0)", publication));
    assertFalse(isRangeValid("1954(4)-2001(1)(0)", publication));
    assertTrue(isRangeValid("1954(4)-2001(1)(1)", publication));
    assertFalse(isRangeValid("1954(4)-2001(1)(2)", publication));
    assertFalse(isRangeValid("1954(4)-2002(1)(1)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1955(4)(2)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1978(1)(2)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1988(11)(28)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1988(12)(27)", publication));
    assertTrue(isRangeValid("1954(4)(3)-1988(12)(28)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1989(8)(9)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1990(9)(0)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1991(9)(1)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1992(9)(2)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1993(9)(3)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1994(9)(4)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1995(9)(5)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1996(9)(6)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1997(9)(7)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1998(9)(8)", publication));
    assertFalse(isRangeValid("1954(4)(3)-1999(9)(9)", publication));
    assertFalse(isRangeValid("1954(4)(3)-2000(0)(0)", publication));
    assertFalse(isRangeValid("1954(4)(3)-2001(1)(0)", publication));
    assertTrue(isRangeValid("1954(4)(3)-2001(1)(1)", publication));
    assertFalse(isRangeValid("1954(4)(3)-2001(1)(2)", publication));
    assertFalse(isRangeValid("1954(4)(3)-2002(1)(1)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1955(4)(2)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1978(1)(2)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1988(11)(28)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1988(12)(27)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1988(12)(28)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1989(8)(9)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1990(9)(0)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1991(9)(1)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1992(9)(2)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1993(9)(3)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1994(9)(4)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1995(9)(5)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1996(9)(6)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1997(9)(7)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1998(9)(8)", publication));
    assertTrue(isRangeValid("1954(4)(6)-1999(9)(9)", publication));
    assertTrue(isRangeValid("1954(4)(6)-2000(0)(0)", publication));
    assertTrue(isRangeValid("1954(4)(6)-2001(1)(0)", publication));
    assertTrue(isRangeValid("1954(4)(6)-2001(1)(1)", publication));
    assertTrue(isRangeValid("1954(4)(6)-2001(1)(2)", publication));
    assertTrue(isRangeValid("1954(4)(6)-2002(1)(1)", publication));
    assertFalse(isRangeValid("1977-1978(1)(2)", publication));
    assertFalse(isRangeValid("1977-1988(11)(28)", publication));
    assertFalse(isRangeValid("1977-1988(12)(27)", publication));
    assertTrue(isRangeValid("1977-1988(12)(28)", publication));
    assertFalse(isRangeValid("1977-1989(8)(9)", publication));
    assertFalse(isRangeValid("1977-1990(9)(0)", publication));
    assertFalse(isRangeValid("1977-1991(9)(1)", publication));
    assertFalse(isRangeValid("1977-1992(9)(2)", publication));
    assertFalse(isRangeValid("1977-1993(9)(3)", publication));
    assertFalse(isRangeValid("1977-1994(9)(4)", publication));
    assertFalse(isRangeValid("1977-1995(9)(5)", publication));
    assertFalse(isRangeValid("1977-1996(9)(6)", publication));
    assertFalse(isRangeValid("1977-1997(9)(7)", publication));
    assertFalse(isRangeValid("1977-1998(9)(8)", publication));
    assertFalse(isRangeValid("1977-1999(9)(9)", publication));
    assertFalse(isRangeValid("1977-2000(0)(0)", publication));
    assertFalse(isRangeValid("1977-2001(1)(0)", publication));
    assertTrue(isRangeValid("1977-2001(1)(1)", publication));
    assertFalse(isRangeValid("1977-2001(1)(2)", publication));
    assertFalse(isRangeValid("1977-2002(1)(1)", publication));
    assertTrue(isRangeValid("1978-1988(11)(28)", publication));
    assertTrue(isRangeValid("1978-1988(12)(27)", publication));
    assertTrue(isRangeValid("1978-1988(12)(28)", publication));
    assertTrue(isRangeValid("1978-1989(8)(9)", publication));
    assertTrue(isRangeValid("1978-1990(9)(0)", publication));
    assertTrue(isRangeValid("1978-1991(9)(1)", publication));
    assertTrue(isRangeValid("1978-1992(9)(2)", publication));
    assertTrue(isRangeValid("1978-1993(9)(3)", publication));
    assertTrue(isRangeValid("1978-1994(9)(4)", publication));
    assertTrue(isRangeValid("1978-1995(9)(5)", publication));
    assertTrue(isRangeValid("1978-1996(9)(6)", publication));
    assertTrue(isRangeValid("1978-1997(9)(7)", publication));
    assertTrue(isRangeValid("1978-1998(9)(8)", publication));
    assertTrue(isRangeValid("1978-1999(9)(9)", publication));
    assertTrue(isRangeValid("1978-2000(0)(0)", publication));
    assertTrue(isRangeValid("1978-2001(1)(0)", publication));
    assertTrue(isRangeValid("1978-2001(1)(1)", publication));
    assertTrue(isRangeValid("1978-2001(1)(2)", publication));
    assertTrue(isRangeValid("1978-2002(1)(1)", publication));
    assertTrue(isRangeValid("1995-1996(9)(6)", publication));
    assertTrue(isRangeValid("1995-1997(9)(7)", publication));
    assertTrue(isRangeValid("1995-1998(9)(8)", publication));
    assertTrue(isRangeValid("1995-1999(9)(9)", publication));
    assertTrue(isRangeValid("1995-2000(0)(0)", publication));
    assertTrue(isRangeValid("1995-2001(1)(0)", publication));
    assertTrue(isRangeValid("1995-2001(1)(1)", publication));
    assertTrue(isRangeValid("1995-2001(1)(2)", publication));
    assertTrue(isRangeValid("1995-2002(1)(1)", publication));
  }

  private boolean isRangeValid(String range, SerialPublication pub) {
    return subManager.isRangeValid(new BibliographicPeriod(range), pub);
  }

  /**
   * Check the behavior of configureAus().
   */
  public final void testConfigureAus() throws Exception {
    // Specify the relevant properties of the archival unit.
    Properties properties = new Properties();
    properties.setProperty("title", "MyTitle");
    properties.setProperty("journalTitle", "MyJournalTitle");
    properties.setProperty("plugin",
	"org.lockss.plugin.simulated.SimulatedPlugin");
    properties.setProperty("attributes.publisher", "MyPublisher");
    properties.setProperty("attributes.year", "1954");
    properties.setProperty("param.1.key", "root");
    properties.setProperty("param.1.value", tempDirPath + "/0");
    properties.setProperty("param.2.key", "base_url");
    properties.setProperty("param.2.value", "http://www.title3.org/");

    // Create the archival unit.
    TdbAu tdbAu = createTdbAu(properties);
    assertFalse(tdbAu.isDown());

    // Create the publication corresponding to the archival unit.
    SerialPublication publication = new SerialPublication();
    publication.setPublicationName(tdbAu.getName());
    publication.setTdbTitle(tdbAu.getTdbTitle());

    // Create the skeleton subscription to the publication.
    Subscription subscription = new Subscription();
    subscription.setPublication(publication);

    BatchAuStatus status;
    Connection conn = dbManager.getConnection();

    assertNull(configureAu(conn, tdbAu, subscription, "-", null));

    // Delete the AU so that it can be added again.
    pluginManager.deleteAu(pluginManager.getAuFromId(tdbAu
	  .getAuId(pluginManager)));

    status = configureAu(conn, tdbAu, subscription, "-", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "1900-", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "1954-", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    assertNull(configureAu(conn, tdbAu, subscription, "1955-", null));
    assertNull(configureAu(conn, tdbAu, subscription, "-1953", null));

    status = configureAu(conn, tdbAu, subscription, "-1954", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "-1999", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "1900-1999", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "1954-1999", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    assertNull(configureAu(conn, tdbAu, subscription, "1955-1999", null));
    assertNull(configureAu(conn, tdbAu, subscription, "1900-1953", null));

    status = configureAu(conn, tdbAu, subscription, "1900-1954", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    assertNull(configureAu(conn, tdbAu, subscription, "1900-1999",
	"1950-1960"));

    assertNull(configureAu(conn, tdbAu, subscription, "1954(4)-", null));
    assertNull(configureAu(conn, tdbAu, subscription, "1954(4)-1999", null));
    assertNull(configureAu(conn, tdbAu, subscription, "1954(4)-1954(4)", null));
    assertNull(configureAu(conn, tdbAu, subscription, "1900-1954(4)", null));
    assertNull(configureAu(conn, tdbAu, subscription, "-1954(4)", null));

    // Specify the relevant properties of another archival unit.
    properties = new Properties();
    properties.setProperty("title", "MyTitle2");
    properties.setProperty("journalTitle", "MyJournalTitle2");
    properties.setProperty("plugin",
	"org.lockss.plugin.simulated.SimulatedPlugin");
    properties.setProperty("attributes.publisher", "MyPublisher2");
    properties.setProperty("attributes.year", "1954");
    properties.setProperty("attributes.volume", "4");
    properties.setProperty("param.1.key", "root");
    properties.setProperty("param.1.value", tempDirPath + "/0");
    properties.setProperty("param.2.key", "base_url");
    properties.setProperty("param.2.value", "http://www.title3.org/");

    // Create the archival unit.
    tdbAu = createTdbAu(properties);
    assertFalse(tdbAu.isDown());

    // Create the publication corresponding to the archival unit.
    publication = new SerialPublication();
    publication.setPublicationName(tdbAu.getName());
    publication.setTdbTitle(tdbAu.getTdbTitle());

    // Create the skeleton subscription to the publication.
    subscription = new Subscription();
    subscription.setPublication(publication);

    status = configureAu(conn, tdbAu, subscription, "-", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "1900-", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "1954-", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    assertNull(configureAu(conn, tdbAu, subscription, "1955-", null));
    assertNull(configureAu(conn, tdbAu, subscription, "-1953", null));

    status = configureAu(conn, tdbAu, subscription, "-1954", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "-1999", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "1900-1999", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "1954-1999", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    assertNull(configureAu(conn, tdbAu, subscription, "1955-1999", null));
    assertNull(configureAu(conn, tdbAu, subscription, "1900-1953", null));

    status = configureAu(conn, tdbAu, subscription, "1900-1954", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    assertNull(configureAu(conn, tdbAu, subscription, "1900-1999",
	"1950-1960"));

    status = configureAu(conn, tdbAu, subscription, "1954(4)-", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "1954(4)-1999", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "1954(4)-1954(5)", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "1954(4)-1954(4)", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "1954(3)-1954(4)", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "1900-1954(4)", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    status = configureAu(conn, tdbAu, subscription, "-1954(4)", null);
    assertNotNull(status);
    assertEquals(1, status.getOkCnt());

    assertNull(configureAu(conn, tdbAu, subscription, "1954(3)-", null));
    assertNull(configureAu(conn, tdbAu, subscription, "1954(3)-1999", null));
    assertNull(configureAu(conn, tdbAu, subscription, "1900-1954(5)", null));
    assertNull(configureAu(conn, tdbAu, subscription, "-1954(5)", null));
  }

  /**
   * Check the behavior of configureAus() for an AU marked as being down.
   */
  public final void testConfigureDownAus() throws Exception {
    // Specify the relevant properties of the archival unit.
    Properties properties = new Properties();
    properties.setProperty("title", "MyTitle");
    properties.setProperty("journalTitle", "MyJournalTitle");
    properties.setProperty("plugin",
	"org.lockss.plugin.simulated.SimulatedPlugin");
    properties.setProperty("attributes.publisher", "MyPublisher");
    properties.setProperty("attributes.year", "1954");
    properties.setProperty("param.1.key", "root");
    properties.setProperty("param.1.value", tempDirPath + "/0");
    properties.setProperty("param.2.key", "base_url");
    properties.setProperty("param.2.value", "http://www.title3.org/");
    properties.setProperty("param.3.key", "pub_down");
    properties.setProperty("param.3.value", "true");

    // Create the archival unit.
    TdbAu tdbAu = createTdbAu(properties);
    assertTrue(tdbAu.isDown());

    // Create the publication corresponding to the archival unit.
    SerialPublication publication = new SerialPublication();
    publication.setPublicationName(tdbAu.getName());
    publication.setTdbTitle(tdbAu.getTdbTitle());

    // Create the skeleton subscription to the publication.
    Subscription subscription = new Subscription();
    subscription.setPublication(publication);

    Connection conn = dbManager.getConnection();

    assertNull(configureAu(conn, tdbAu, subscription, "-", null));
  }

  private BatchAuStatus configureAu(Connection conn, TdbAu tdbAu,
      Subscription subscription, String subscribedRanges,
      String unsubscribedRanges) throws IOException, DbException,
      SubscriptionException {
    subscription.setSubscribedRanges(Collections
	.singletonList(new BibliographicPeriod(subscribedRanges)));
    subscription.setUnsubscribedRanges(Collections
	.singletonList(new BibliographicPeriod(unsubscribedRanges)));

    // The AU is added.
    BatchAuStatus status = subManager.configureAus(conn, subscription);

    if (status != null && status.getOkCnt() == 1) {
      // Delete the AU so that it can be added again.
      pluginManager.deleteAu(pluginManager.getAuFromId(tdbAu
	  .getAuId(pluginManager)));
    }

    return status;
  }
  
  /**
   * Check the behavior of matchesTitleTdbAu().
   * @throws TdbException for invalid Tdb operations
   */
  public void testMatchesTitleTdbAu() throws TdbException {
    // Specify the relevant properties of the archival unit.
    Properties properties = new Properties();
    properties.setProperty("title", "MyTitle");
    properties.setProperty("journalTitle", "MyJournalTitle");
    properties.setProperty("plugin",
	"org.lockss.plugin.simulated.SimulatedPlugin");
    properties.setProperty("attributes.publisher", "MyPublisher");
    properties.setProperty("attributes.year", "1954");
    properties.setProperty("param.1.key", "root");
    properties.setProperty("param.1.value", tempDirPath + "/0");
    properties.setProperty("param.2.key", "base_url");
    properties.setProperty("param.2.value", "http://www.title3.org/");

    // Create the archival unit.
    TdbAu tdbAu = createTdbAu(properties);

    List<BibliographicPeriod> ranges =
	Arrays.asList(new BibliographicPeriod("1954(4)(2)"),
	    new BibliographicPeriod("1954(4)(4)-1954(4)(6)"),
	    new BibliographicPeriod("1988(12)(28)-2001(1)(1)"));

    tdbAu.setPublicationRanges(ranges);

    // Create the publication corresponding to the archival unit.
    SerialPublication pub = new SerialPublication();
    pub.setPublicationName(tdbAu.getName());
    pub.setTdbTitle(tdbAu.getTdbTitle());

    assertTrue(matchesTdbAu(new BibliographicPeriod("-"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1900-"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("1900-1953"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("1953"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1900-1954"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1954-"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1954"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("1954(1)(1)"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("1954(1)(1)-"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1900-1954(4)(2)"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1954(1)(1)-1954(4)(2)"),
	pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("1954(4)(1)"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1954(4)(1)-1954(4)(2)"),
	pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1954(4)(2)"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1954(4)(2)-"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("1900-1954(4)(3)"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("1954(4)(1)-1954(4)(3)"),
	pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("1954(4)(3)"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("1954(4)(3)-"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1954(4)(3)-1954(4)(4)"),
	pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1954(4)(4)"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1954(4)(4)-"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("1954(4)(5)"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1954(4)(6)"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1954(4)(3)-1954(4)(6)"),
	pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("1954(4)(7)"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("1954(4)(5)-1954(4)(8)"),
	pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1900-1955"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1955-"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("1955-1987"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1987-1988"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("1987-1988(12)(27)"),
	pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1988"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1988(12)(1)-1988(12)(28)"),
	pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1988(12)(28)"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod(
	"1988(12)(27)-1988(12)(29)"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1988-"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1988-1989"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1988-1989(1)(1)"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1989-"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1989-1999"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1999-"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1989-2001"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1989-2001(1)(1)"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("1989-2001(1)(2)"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("2001"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("2001-"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("2001(1)(1)"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("2001(1)(2)"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("2002-"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("-2002"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("-2002"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("-2001"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("-2001(1)(1)"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("-2000(1)(1)"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("-2000"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("-1988"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("-1988(12)(28)"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("-1988(12)(1)"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("-1987"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("-1954"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("-1954(4)(6)"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("-1954(4)(5)"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("-1954(4)(4)"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("-1954(4)(3)"), pub));
    assertTrue(matchesTdbAu(new BibliographicPeriod("-1954(4)(2)"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("-1954(4)(1)"), pub));
    assertFalse(matchesTdbAu(new BibliographicPeriod("-1953"), pub));
  }

  private boolean matchesTdbAu(BibliographicPeriod range,
      SerialPublication publication) {
    return subManager.matchesTitleTdbAu(range, publication);
  }
  
  /**
   * Check the behavior of getCoveredTdbAus().
   * @throws TdbException for invalid Tdb operations
   */
  public void testGetCoveredTdbAus() throws TdbException {
    TdbTitle title = TdbTestUtil.makeRangeTestTitle(false);

    List<TdbAu> tdbAus = title.getSortedTdbAus();
    assertEquals(3, tdbAus.size());

    TdbAu tdbAu54 = tdbAus.get(0);

    tdbAu54.setPublicationRanges(Arrays
	.asList(new BibliographicPeriod("1954(4)(2)"),
	    new BibliographicPeriod("1954(4)(4)-1954(4)(6)")));

    TdbAu tdbAu78 = tdbAus.get(1);

    tdbAu78.setPublicationRanges(Arrays
	.asList(new BibliographicPeriod("1978")));

    TdbAu tdbAu88 = tdbAus.get(2);

    tdbAu88.setPublicationRanges(Arrays.
	asList(new BibliographicPeriod("1988(12)(28)-2001(1)(1)")));

    assertEquals(0, coveredAus(title, "", "").size());
    assertEquals(3, coveredAus(title, "-", "").size());
    assertEquals(0, coveredAus(title, "", "-").size());
    assertEquals(0, coveredAus(title, "-", "-").size());
    assertEquals(3, coveredAus(title, "1953-", "").size());
    assertEquals(0, coveredAus(title, "1953-", "1953-").size());
    assertEquals(0, coveredAus(title, "1953-", "1954-").size());
    assertEquals(1, coveredAus(title, "1953-", "1955-").size());
    assertEquals(1, coveredAus(title, "1953-", "1978-").size());
    assertEquals(2, coveredAus(title, "1953-", "1979-").size());
    assertEquals(2, coveredAus(title, "1953-", "1988-").size());
    assertEquals(2, coveredAus(title, "1953-", "2000-").size());
    assertEquals(2, coveredAus(title, "1953-", "2001-").size());
    assertEquals(3, coveredAus(title, "1953-", "2002-").size());
    assertEquals(3, coveredAus(title, "1954-", "").size());
    assertEquals(0, coveredAus(title, "1954-", "1954-").size());
    assertEquals(1, coveredAus(title, "1954-", "1955-").size());
    assertEquals(1, coveredAus(title, "1954-", "1978-").size());
    assertEquals(2, coveredAus(title, "1954-", "1979-").size());
    assertEquals(2, coveredAus(title, "1954-", "1988-").size());
    assertEquals(2, coveredAus(title, "1954-", "2000-").size());
    assertEquals(2, coveredAus(title, "1954-", "2001-").size());
    assertEquals(3, coveredAus(title, "1954-", "2002-").size());
    assertEquals(2, coveredAus(title, "1955-", "").size());
    assertEquals(0, coveredAus(title, "1955-", "1955-").size());
    assertEquals(0, coveredAus(title, "1955-", "1978-").size());
    assertEquals(1, coveredAus(title, "1955-", "1979-").size());
    assertEquals(1, coveredAus(title, "1955-", "1988-").size());
    assertEquals(1, coveredAus(title, "1955-", "2000-").size());
    assertEquals(1, coveredAus(title, "1955-", "2001-").size());
    assertEquals(2, coveredAus(title, "1955-", "2002-").size());
    assertEquals(2, coveredAus(title, "1977-", "").size());
    assertEquals(2, coveredAus(title, "1978-", "").size());
    assertEquals(0, coveredAus(title, "1978-", "1978-").size());
    assertEquals(1, coveredAus(title, "1978-", "1979-").size());
    assertEquals(1, coveredAus(title, "1978-", "1988-").size());
    assertEquals(1, coveredAus(title, "1978-", "2000-").size());
    assertEquals(1, coveredAus(title, "1978-", "2001-").size());
    assertEquals(2, coveredAus(title, "1978-", "2002-").size());
    assertEquals(1, coveredAus(title, "1979-", "").size());
    assertEquals(0, coveredAus(title, "1979-", "1979-").size());
    assertEquals(0, coveredAus(title, "1979-", "1988-").size());
    assertEquals(0, coveredAus(title, "1979-", "2000-").size());
    assertEquals(0, coveredAus(title, "1979-", "2001-").size());
    assertEquals(1, coveredAus(title, "1979-", "2002-").size());
    assertEquals(1, coveredAus(title, "1987-", "").size());
    assertEquals(1, coveredAus(title, "1988-", "").size());
    assertEquals(0, coveredAus(title, "1988-", "1988-").size());
    assertEquals(0, coveredAus(title, "1988-", "2000-").size());
    assertEquals(0, coveredAus(title, "1988-", "2001-").size());
    assertEquals(1, coveredAus(title, "1988-", "2002-").size());
    assertEquals(1, coveredAus(title, "1989-", "").size());
    assertEquals(0, coveredAus(title, "1989-", "1989-").size());
    assertEquals(0, coveredAus(title, "1989-", "2000-").size());
    assertEquals(0, coveredAus(title, "1989-", "2001-").size());
    assertEquals(1, coveredAus(title, "1989-", "2002-").size());
    assertEquals(1, coveredAus(title, "2000-", "").size());
    assertEquals(1, coveredAus(title, "2001-", "").size());
    assertEquals(0, coveredAus(title, "2001-", "2001-").size());
    assertEquals(1, coveredAus(title, "2001-", "2002-").size());
    assertEquals(0, coveredAus(title, "2002-", "").size());
    assertEquals(0, coveredAus(title, "2002-", "2002-").size());
    assertEquals(0, coveredAus(title, "-1953", "").size());
    assertEquals(0, coveredAus(title, "-1953", "-1953").size());
    assertEquals(0, coveredAus(title, "-1953", "-1954").size());
    assertEquals(1, coveredAus(title, "-1954", "").size());
    assertEquals(1, coveredAus(title, "-1954", "-1953").size());
    assertEquals(0, coveredAus(title, "-1954", "-1954").size());
    assertEquals(1, coveredAus(title, "-1955", "").size());
    assertEquals(1, coveredAus(title, "-1955", "-1953").size());
    assertEquals(0, coveredAus(title, "-1955", "-1954").size());
    assertEquals(0, coveredAus(title, "-1955", "-1955").size());
    assertEquals(1, coveredAus(title, "-1977", "").size());
    assertEquals(0, coveredAus(title, "-1977", "-1977").size());
    assertEquals(2, coveredAus(title, "-1978", "").size());
    assertEquals(2, coveredAus(title, "-1978", "-1953").size());
    assertEquals(1, coveredAus(title, "-1978", "-1954").size());
    assertEquals(1, coveredAus(title, "-1978", "-1955").size());
    assertEquals(0, coveredAus(title, "-1978", "-1978").size());
    assertEquals(2, coveredAus(title, "-1979", "").size());
    assertEquals(2, coveredAus(title, "-1979", "-1953").size());
    assertEquals(1, coveredAus(title, "-1979", "-1954").size());
    assertEquals(1, coveredAus(title, "-1979", "-1955").size());
    assertEquals(0, coveredAus(title, "-1979", "-1978").size());
    assertEquals(0, coveredAus(title, "-1979", "-1979").size());
    assertEquals(2, coveredAus(title, "-1987", "").size());
    assertEquals(0, coveredAus(title, "-1987", "-1987").size());
    assertEquals(3, coveredAus(title, "-1988", "").size());
    assertEquals(3, coveredAus(title, "-1988", "-1953").size());
    assertEquals(2, coveredAus(title, "-1988", "-1954").size());
    assertEquals(2, coveredAus(title, "-1988", "-1955").size());
    assertEquals(1, coveredAus(title, "-1988", "-1978").size());
    assertEquals(1, coveredAus(title, "-1988", "-1979").size());
    assertEquals(0, coveredAus(title, "-1988", "-1988").size());
    assertEquals(3, coveredAus(title, "-1989", "").size());
    assertEquals(3, coveredAus(title, "-1989", "-1953").size());
    assertEquals(2, coveredAus(title, "-1989", "-1954").size());
    assertEquals(2, coveredAus(title, "-1989", "-1955").size());
    assertEquals(1, coveredAus(title, "-1989", "-1978").size());
    assertEquals(1, coveredAus(title, "-1989", "-1979").size());
    assertEquals(0, coveredAus(title, "-1989", "-1988").size());
    assertEquals(0, coveredAus(title, "-1989", "-1989").size());
    assertEquals(3, coveredAus(title, "-2000", "").size());
    assertEquals(0, coveredAus(title, "-2000", "-2000").size());
    assertEquals(3, coveredAus(title, "-2001", "").size());
    assertEquals(3, coveredAus(title, "-2001", "-1953").size());
    assertEquals(2, coveredAus(title, "-2001", "-1954").size());
    assertEquals(2, coveredAus(title, "-2001", "-1955").size());
    assertEquals(1, coveredAus(title, "-2001", "-1978").size());
    assertEquals(1, coveredAus(title, "-2001", "-1979").size());
    assertEquals(0, coveredAus(title, "-2001", "-1988").size());
    assertEquals(0, coveredAus(title, "-2001", "-1989").size());
    assertEquals(0, coveredAus(title, "-2001", "-2001").size());
    assertEquals(3, coveredAus(title, "-2002", "").size());
    assertEquals(3, coveredAus(title, "-2002", "-1953").size());
    assertEquals(2, coveredAus(title, "-2002", "-1954").size());
    assertEquals(2, coveredAus(title, "-2002", "-1955").size());
    assertEquals(1, coveredAus(title, "-2002", "-1978").size());
    assertEquals(1, coveredAus(title, "-2002", "-1979").size());
    assertEquals(0, coveredAus(title, "-2002", "-1988").size());
    assertEquals(0, coveredAus(title, "-2002", "-1989").size());
    assertEquals(0, coveredAus(title, "-2002", "-2001").size());
    assertEquals(0, coveredAus(title, "-2002", "-2001").size());
    assertEquals(1, coveredAus(title, "1953-1954", "").size());
    assertEquals(0, coveredAus(title, "1953-1954", "1953-1954").size());
    assertEquals(0, coveredAus(title, "1953-1954", "1953-1955").size());
    assertEquals(1, coveredAus(title, "1953-1955", "").size());
    assertEquals(0, coveredAus(title, "1953-1955", "1953-1954").size());
    assertEquals(0, coveredAus(title, "1953-1955", "1953-1955").size());
    assertEquals(0, coveredAus(title, "1953-1955", "1953-1977").size());
    assertEquals(1, coveredAus(title, "1953-1977", "").size());
    assertEquals(0, coveredAus(title, "1953-1977", "1953-1954").size());
    assertEquals(0, coveredAus(title, "1953-1977", "1953-1955").size());
    assertEquals(0, coveredAus(title, "1953-1977", "1953-1977").size());
    assertEquals(0, coveredAus(title, "1953-1977", "1953-1978").size());
    assertEquals(2, coveredAus(title, "1953-1978", "").size());
    assertEquals(1, coveredAus(title, "1953-1978", "1953-1954").size());
    assertEquals(1, coveredAus(title, "1953-1978", "1953-1955").size());
    assertEquals(0, coveredAus(title, "1953-1978", "1953-1978").size());
    assertEquals(0, coveredAus(title, "1953-1978", "1953-1979").size());
    assertEquals(2, coveredAus(title, "1953-1979", "").size());
    assertEquals(1, coveredAus(title, "1953-1979", "1953-1954").size());
    assertEquals(1, coveredAus(title, "1953-1979", "1953-1955").size());
    assertEquals(0, coveredAus(title, "1953-1979", "1953-1978").size());
    assertEquals(0, coveredAus(title, "1953-1979", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1953-1979", "1953-1987").size());
    assertEquals(2, coveredAus(title, "1953-1987", "").size());
    assertEquals(0, coveredAus(title, "1953-1987", "1953-1987").size());
    assertEquals(3, coveredAus(title, "1953-1988", "").size());
    assertEquals(2, coveredAus(title, "1953-1988", "1953-1954").size());
    assertEquals(2, coveredAus(title, "1953-1988", "1953-1955").size());
    assertEquals(1, coveredAus(title, "1953-1988", "1953-1978").size());
    assertEquals(1, coveredAus(title, "1953-1988", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1953-1988", "1953-1988").size());
    assertEquals(0, coveredAus(title, "1953-1988", "1953-1989").size());
    assertEquals(3, coveredAus(title, "1953-1989", "").size());
    assertEquals(2, coveredAus(title, "1953-1989", "1953-1954").size());
    assertEquals(2, coveredAus(title, "1953-1989", "1953-1955").size());
    assertEquals(1, coveredAus(title, "1953-1989", "1953-1978").size());
    assertEquals(1, coveredAus(title, "1953-1989", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1953-1989", "1953-1988").size());
    assertEquals(0, coveredAus(title, "1953-1989", "1953-1989").size());
    assertEquals(0, coveredAus(title, "1953-1989", "1953-2000").size());
    assertEquals(3, coveredAus(title, "1953-2000", "").size());
    assertEquals(0, coveredAus(title, "1953-2000", "1953-2000").size());
    assertEquals(3, coveredAus(title, "1953-2001", "").size());
    assertEquals(2, coveredAus(title, "1953-2001", "1953-1954").size());
    assertEquals(2, coveredAus(title, "1953-2001", "1953-1955").size());
    assertEquals(1, coveredAus(title, "1953-2001", "1953-1978").size());
    assertEquals(1, coveredAus(title, "1953-2001", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1953-2001", "1953-1988").size());
    assertEquals(0, coveredAus(title, "1953-2001", "1953-1989").size());
    assertEquals(0, coveredAus(title, "1953-2001", "1953-2001").size());
    assertEquals(0, coveredAus(title, "1953-2001", "1953-2002").size());
    assertEquals(3, coveredAus(title, "1953-2002", "").size());
    assertEquals(2, coveredAus(title, "1953-2002", "1953-1954").size());
    assertEquals(2, coveredAus(title, "1953-2002", "1953-1955").size());
    assertEquals(1, coveredAus(title, "1953-2002", "1953-1978").size());
    assertEquals(1, coveredAus(title, "1953-2002", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1953-2002", "1953-1988").size());
    assertEquals(0, coveredAus(title, "1953-2002", "1953-1989").size());
    assertEquals(0, coveredAus(title, "1953-2002", "1953-2001").size());
    assertEquals(0, coveredAus(title, "1953-2002", "1953-2002").size());
    assertEquals(1, coveredAus(title, "1954-1955", "").size());
    assertEquals(0, coveredAus(title, "1954-1955", "1953-1954").size());
    assertEquals(0, coveredAus(title, "1954-1955", "1953-1955").size());
    assertEquals(0, coveredAus(title, "1954-1955", "1953-1977").size());
    assertEquals(1, coveredAus(title, "1954-1977", "").size());
    assertEquals(0, coveredAus(title, "1954-1977", "1953-1954").size());
    assertEquals(0, coveredAus(title, "1954-1977", "1953-1955").size());
    assertEquals(0, coveredAus(title, "1954-1977", "1953-1977").size());
    assertEquals(0, coveredAus(title, "1954-1977", "1953-1978").size());
    assertEquals(2, coveredAus(title, "1954-1978", "").size());
    assertEquals(1, coveredAus(title, "1954-1978", "1953-1954").size());
    assertEquals(1, coveredAus(title, "1954-1978", "1953-1955").size());
    assertEquals(0, coveredAus(title, "1954-1978", "1953-1978").size());
    assertEquals(0, coveredAus(title, "1954-1978", "1953-1979").size());
    assertEquals(2, coveredAus(title, "1954-1979", "").size());
    assertEquals(1, coveredAus(title, "1954-1979", "1953-1954").size());
    assertEquals(1, coveredAus(title, "1954-1979", "1953-1955").size());
    assertEquals(0, coveredAus(title, "1954-1979", "1953-1978").size());
    assertEquals(0, coveredAus(title, "1954-1979", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1954-1979", "1953-1987").size());
    assertEquals(2, coveredAus(title, "1954-1987", "").size());
    assertEquals(0, coveredAus(title, "1954-1987", "1953-1987").size());
    assertEquals(3, coveredAus(title, "1954-1988", "").size());
    assertEquals(2, coveredAus(title, "1954-1988", "1953-1954").size());
    assertEquals(2, coveredAus(title, "1954-1988", "1953-1955").size());
    assertEquals(1, coveredAus(title, "1954-1988", "1953-1978").size());
    assertEquals(1, coveredAus(title, "1954-1988", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1954-1988", "1953-1988").size());
    assertEquals(0, coveredAus(title, "1954-1988", "1953-1989").size());
    assertEquals(3, coveredAus(title, "1954-1989", "").size());
    assertEquals(2, coveredAus(title, "1954-1989", "1953-1954").size());
    assertEquals(2, coveredAus(title, "1954-1989", "1953-1955").size());
    assertEquals(1, coveredAus(title, "1954-1989", "1953-1978").size());
    assertEquals(1, coveredAus(title, "1954-1989", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1954-1989", "1953-1988").size());
    assertEquals(0, coveredAus(title, "1954-1989", "1953-1989").size());
    assertEquals(0, coveredAus(title, "1954-1989", "1953-2000").size());
    assertEquals(3, coveredAus(title, "1954-2000", "").size());
    assertEquals(0, coveredAus(title, "1954-2000", "1953-2000").size());
    assertEquals(3, coveredAus(title, "1954-2001", "").size());
    assertEquals(2, coveredAus(title, "1954-2001", "1953-1954").size());
    assertEquals(2, coveredAus(title, "1954-2001", "1953-1955").size());
    assertEquals(1, coveredAus(title, "1954-2001", "1953-1978").size());
    assertEquals(1, coveredAus(title, "1954-2001", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1954-2001", "1953-1988").size());
    assertEquals(0, coveredAus(title, "1954-2001", "1953-1989").size());
    assertEquals(0, coveredAus(title, "1954-2001", "1953-2001").size());
    assertEquals(0, coveredAus(title, "1954-2001", "1953-2002").size());
    assertEquals(3, coveredAus(title, "1954-2002", "").size());
    assertEquals(2, coveredAus(title, "1954-2002", "1953-1954").size());
    assertEquals(2, coveredAus(title, "1954-2002", "1953-1955").size());
    assertEquals(1, coveredAus(title, "1954-2002", "1953-1978").size());
    assertEquals(1, coveredAus(title, "1954-2002", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1954-2002", "1953-1988").size());
    assertEquals(0, coveredAus(title, "1954-2002", "1953-1989").size());
    assertEquals(0, coveredAus(title, "1954-2002", "1953-2001").size());
    assertEquals(0, coveredAus(title, "1954-2002", "1953-2002").size());
    assertEquals(0, coveredAus(title, "1955-1977", "").size());
    assertEquals(0, coveredAus(title, "1955-1977", "1953-1954").size());
    assertEquals(0, coveredAus(title, "1955-1977", "1953-1955").size());
    assertEquals(0, coveredAus(title, "1955-1977", "1953-1977").size());
    assertEquals(0, coveredAus(title, "1955-1977", "1953-1978").size());
    assertEquals(1, coveredAus(title, "1955-1978", "").size());
    assertEquals(1, coveredAus(title, "1955-1978", "1953-1954").size());
    assertEquals(1, coveredAus(title, "1955-1978", "1953-1955").size());
    assertEquals(0, coveredAus(title, "1955-1978", "1953-1978").size());
    assertEquals(0, coveredAus(title, "1955-1978", "1953-1979").size());
    assertEquals(1, coveredAus(title, "1955-1979", "").size());
    assertEquals(1, coveredAus(title, "1955-1979", "1953-1954").size());
    assertEquals(1, coveredAus(title, "1955-1979", "1953-1955").size());
    assertEquals(0, coveredAus(title, "1955-1979", "1953-1978").size());
    assertEquals(0, coveredAus(title, "1955-1979", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1955-1979", "1953-1987").size());
    assertEquals(1, coveredAus(title, "1955-1987", "").size());
    assertEquals(0, coveredAus(title, "1955-1987", "1953-1987").size());
    assertEquals(2, coveredAus(title, "1955-1988", "").size());
    assertEquals(2, coveredAus(title, "1955-1988", "1953-1954").size());
    assertEquals(2, coveredAus(title, "1955-1988", "1953-1955").size());
    assertEquals(1, coveredAus(title, "1955-1988", "1953-1978").size());
    assertEquals(1, coveredAus(title, "1955-1988", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1955-1988", "1953-1988").size());
    assertEquals(0, coveredAus(title, "1955-1988", "1953-1989").size());
    assertEquals(2, coveredAus(title, "1955-1989", "").size());
    assertEquals(2, coveredAus(title, "1955-1989", "1953-1954").size());
    assertEquals(2, coveredAus(title, "1955-1989", "1953-1955").size());
    assertEquals(1, coveredAus(title, "1955-1989", "1953-1978").size());
    assertEquals(1, coveredAus(title, "1955-1989", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1955-1989", "1953-1988").size());
    assertEquals(0, coveredAus(title, "1955-1989", "1953-1989").size());
    assertEquals(0, coveredAus(title, "1955-1989", "1953-2000").size());
    assertEquals(2, coveredAus(title, "1955-2000", "").size());
    assertEquals(0, coveredAus(title, "1955-2000", "1953-2000").size());
    assertEquals(2, coveredAus(title, "1955-2001", "").size());
    assertEquals(2, coveredAus(title, "1955-2001", "1953-1954").size());
    assertEquals(2, coveredAus(title, "1955-2001", "1953-1955").size());
    assertEquals(1, coveredAus(title, "1955-2001", "1953-1978").size());
    assertEquals(1, coveredAus(title, "1955-2001", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1955-2001", "1953-1988").size());
    assertEquals(0, coveredAus(title, "1955-2001", "1953-1989").size());
    assertEquals(0, coveredAus(title, "1955-2001", "1953-2001").size());
    assertEquals(0, coveredAus(title, "1955-2001", "1953-2002").size());
    assertEquals(2, coveredAus(title, "1955-2002", "").size());
    assertEquals(2, coveredAus(title, "1955-2002", "1953-1954").size());
    assertEquals(2, coveredAus(title, "1955-2002", "1953-1955").size());
    assertEquals(1, coveredAus(title, "1955-2002", "1953-1978").size());
    assertEquals(1, coveredAus(title, "1955-2002", "1953-1979").size());
    assertEquals(0, coveredAus(title, "1955-2002", "1953-1988").size());
    assertEquals(0, coveredAus(title, "1955-2002", "1953-1989").size());
    assertEquals(0, coveredAus(title, "1955-2002", "1953-2001").size());
    assertEquals(0, coveredAus(title, "1955-2002", "1953-2002").size());
  }

  private Set<TdbAu> coveredAus(TdbTitle tdbTitle,
      String subscribedRanges, String unsubscribedRanges) {
    return subManager.getCoveredTdbAus(tdbTitle,
	BibliographicPeriod.createList(subscribedRanges),
	BibliographicPeriod.createList(unsubscribedRanges));
  }

  /**
   * Check the behavior of unsubscribeAu().
   */
  public final void testUnsubscribeAu() throws Exception {
    // Create the TDB.
    Tdb tdb = new Tdb();
    tdb.addTdbAuFromProperties(makeTitleConfig("123").toProperties());
    ConfigurationUtil.setTdb(tdb);

    TitleConfig tc =
	new TitleConfig(tdb.getTdbAusLikeName("vol123").get(0), plugin);
    String auId = tc.getAuId(pluginManager);

    // Add the archival unit.
    BatchAuStatus status =
	LockssDaemon.getLockssDaemon().getRemoteApi().addByAuId(auId);
    assertEquals(0, status.getOkCnt());

    MockArchivalUnit au = (MockArchivalUnit)pluginManager.getAuFromId(auId);
    au.setTitleConfig(tc);
    TdbAu tdbAu = au.getTdbAu();

    List<BibliographicPeriod> publicationRanges =
	new ArrayList<BibliographicPeriod>();
    publicationRanges.add(new BibliographicPeriod("1954"));
    tdbAu.setPublicationRanges(publicationRanges);

    Connection conn = dbManager.getConnection();

    // Create the publisher.
    String publisherName = "Publisher of [Title of [vol123]]";
    Long publisherSeq =	metadataManager.addPublisher(conn, publisherName);

    // Create the publication.
    Long publicationSeq = metadataManager.addPublication(conn, publisherSeq,
	null, MD_ITEM_TYPE_JOURNAL, "Title of [vol123]");

    Long parentSeq =
	  metadataManager.findPublicationMetadataItem(conn, publicationSeq);

    metadataManager.addMdItemProprietaryIds(conn, parentSeq,
	Collections.singleton("id:11171508429"));

    // Add the provider.
    TdbProvider provider = tdbAu.getTdbProvider();
    // TODO: Replace the second argument with provider.getLid() when available.
    Long providerSeq =
	dbManager.findOrCreateProvider(conn, null, provider.getName());

    // Create the subscription.
    Long subscriptionSeq =
	subManager.persistSubscription(conn, publicationSeq, providerSeq);

    assertFalse(subManager.hasSubscriptionRanges(conn));
    DbManager.commitOrRollback(conn, log);
    assertFalse(subManager.hasSubscriptionRanges());

    // Handle a subscription with no ranges.
    subManager.unsubscribeAu(conn, au);
    DbManager.commitOrRollback(conn, log);

    assertEquals(0,
	subManager.findSubscriptionRanges(conn, subscriptionSeq, true).size());

    assertEquals(0,
	subManager.findSubscriptionRanges(conn, subscriptionSeq, false).size());

    // Handle a subscription with a null collection of subscribed ranges.
    int count = subManager.persistSubscribedRanges(conn, subscriptionSeq, null);
    assertEquals(0, count);
    assertFalse(subManager.hasSubscriptionRanges(conn));
    DbManager.commitOrRollback(conn, log);

    assertFalse(subManager.hasSubscriptionRanges());

    subManager.unsubscribeAu(conn, au);
    DbManager.commitOrRollback(conn, log);

    assertEquals(0,
	subManager.findSubscriptionRanges(conn, subscriptionSeq, true).size());

    assertEquals(0,
	subManager.findSubscriptionRanges(conn, subscriptionSeq, false).size());

    // Handle a subscription with a null collection of unsubscribed ranges.
    count = subManager.persistUnsubscribedRanges(conn, subscriptionSeq, null);
    assertEquals(0, count);
    assertFalse(subManager.hasSubscriptionRanges(conn));
    DbManager.commitOrRollback(conn, log);

    assertFalse(subManager.hasSubscriptionRanges());

    subManager.unsubscribeAu(conn, au);
    DbManager.commitOrRollback(conn, log);

    assertEquals(0,
	subManager.findSubscriptionRanges(conn, subscriptionSeq, true).size());

    assertEquals(0,
	subManager.findSubscriptionRanges(conn, subscriptionSeq, false).size());

    // Handle a subscription with an empty collection of subscribed ranges.
    List<BibliographicPeriod> subscribedRanges =
	new ArrayList<BibliographicPeriod>();

    count = subManager.persistSubscribedRanges(conn, subscriptionSeq,
	subscribedRanges);
    assertEquals(0, count);
    assertFalse(subManager.hasSubscriptionRanges(conn));
    DbManager.commitOrRollback(conn, log);

    assertFalse(subManager.hasSubscriptionRanges());

    subManager.unsubscribeAu(conn, au);
    DbManager.commitOrRollback(conn, log);

    assertEquals(0,
	subManager.findSubscriptionRanges(conn, subscriptionSeq, true).size());

    assertEquals(0,
	subManager.findSubscriptionRanges(conn, subscriptionSeq, false).size());

    // Handle a subscription with an empty collection of unsubscribed ranges.
    List<BibliographicPeriod> unsubscribedRanges =
	new ArrayList<BibliographicPeriod>();

    count = subManager.persistUnsubscribedRanges(conn, subscriptionSeq,
	unsubscribedRanges);
    assertEquals(0, count);
    assertFalse(subManager.hasSubscriptionRanges(conn));
    DbManager.commitOrRollback(conn, log);

    assertFalse(subManager.hasSubscriptionRanges());

    subManager.unsubscribeAu(conn, au);
    DbManager.commitOrRollback(conn, log);

    assertEquals(0,
	subManager.findSubscriptionRanges(conn, subscriptionSeq, true).size());

    assertEquals(0,
	subManager.findSubscriptionRanges(conn, subscriptionSeq, false).size());

    // Handle a subscription with a non-overlapping subscribed range.
    subscribedRanges.add(new BibliographicPeriod("1900-1953"));

    count = subManager.persistSubscribedRanges(conn, subscriptionSeq,
	subscribedRanges);
    assertEquals(1, count);
    assertTrue(subManager.hasSubscriptionRanges(conn));
    DbManager.commitOrRollback(conn, log);

    assertEquals(1, count);
    assertTrue(subManager.hasSubscriptionRanges());

    subManager.unsubscribeAu(conn, au);
    DbManager.commitOrRollback(conn, log);

    subscribedRanges =
	subManager.findSubscriptionRanges(conn, subscriptionSeq, true);
    assertEquals(1, subscribedRanges.size());
    assertEquals("1900-1953", subscribedRanges.get(0).toDisplayableString());

    assertEquals(0,
	subManager.findSubscriptionRanges(conn, subscriptionSeq, false).size());
    assertTrue(subManager.hasSubscriptionRanges(conn));

    // Handle a subscription with an exactly matching subscribed range.
    subscribedRanges = new ArrayList<BibliographicPeriod>();
    subscribedRanges.add(new BibliographicPeriod("1954"));

    count = subManager.persistSubscribedRanges(conn, subscriptionSeq,
	subscribedRanges);
    assertEquals(1, count);
    assertTrue(subManager.hasSubscriptionRanges(conn));
    DbManager.commitOrRollback(conn, log);

    assertTrue(subManager.hasSubscriptionRanges());

    subManager.unsubscribeAu(conn, au);
    DbManager.commitOrRollback(conn, log);

    subscribedRanges =
	subManager.findSubscriptionRanges(conn, subscriptionSeq, true);
    assertEquals(1, subscribedRanges.size());
    assertEquals("1900-1953", subscribedRanges.get(0).toDisplayableString());

    unsubscribedRanges =
	subManager.findSubscriptionRanges(conn, subscriptionSeq, false);
    assertEquals(1, unsubscribedRanges.size());
    assertEquals("1954", unsubscribedRanges.get(0).toDisplayableString());
    assertTrue(subManager.hasSubscriptionRanges(conn));

    // Handle a subscription with an overlapping subscribed range.
    clearSubscriptionRanges(conn, subscriptionSeq, 1, 1);
    assertFalse(subManager.hasSubscriptionRanges(conn));

    subscribedRanges = new ArrayList<BibliographicPeriod>();
    subscribedRanges.add(new BibliographicPeriod("1900-2000"));

    count = subManager.persistSubscribedRanges(conn, subscriptionSeq,
	subscribedRanges);
    assertEquals(1, count);
    assertTrue(subManager.hasSubscriptionRanges(conn));
    DbManager.commitOrRollback(conn, log);

    assertTrue(subManager.hasSubscriptionRanges());

    subManager.unsubscribeAu(conn, au);
    DbManager.commitOrRollback(conn, log);

    subscribedRanges =
	subManager.findSubscriptionRanges(conn, subscriptionSeq, true);
    assertEquals(1, subscribedRanges.size());
    assertEquals("1900-2000", subscribedRanges.get(0).toDisplayableString());

    unsubscribedRanges =
	subManager.findSubscriptionRanges(conn, subscriptionSeq, false);
    assertEquals(1, unsubscribedRanges.size());
    assertEquals("1954", unsubscribedRanges.get(0).toDisplayableString());
    assertTrue(subManager.hasSubscriptionRanges(conn));

    // Handle a subscription with an overlapping subscribed range second edge.
    clearSubscriptionRanges(conn, subscriptionSeq, 1, 1);
    assertFalse(subManager.hasSubscriptionRanges(conn));

    subscribedRanges = new ArrayList<BibliographicPeriod>();
    subscribedRanges.add(new BibliographicPeriod("1900-1954"));

    count = subManager.persistSubscribedRanges(conn, subscriptionSeq,
	subscribedRanges);
    assertEquals(1, count);
    assertTrue(subManager.hasSubscriptionRanges(conn));
    DbManager.commitOrRollback(conn, log);

    assertTrue(subManager.hasSubscriptionRanges());

    subManager.unsubscribeAu(conn, au);
    DbManager.commitOrRollback(conn, log);

    subscribedRanges =
	subManager.findSubscriptionRanges(conn, subscriptionSeq, true);
    assertEquals(1, subscribedRanges.size());
    assertEquals("1900-1954", subscribedRanges.get(0).toDisplayableString());

    unsubscribedRanges =
	subManager.findSubscriptionRanges(conn, subscriptionSeq, false);
    assertEquals(1, unsubscribedRanges.size());
    assertEquals("1954", unsubscribedRanges.get(0).toDisplayableString());
    assertTrue(subManager.hasSubscriptionRanges(conn));

    // Handle a subscription with an overlapping subscribed range first edge.
    clearSubscriptionRanges(conn, subscriptionSeq, 1, 1);
    assertFalse(subManager.hasSubscriptionRanges(conn));

    subscribedRanges = new ArrayList<BibliographicPeriod>();
    subscribedRanges.add(new BibliographicPeriod("1954-2000"));

    count = subManager.persistSubscribedRanges(conn, subscriptionSeq,
	subscribedRanges);
    assertEquals(1, count);
    assertTrue(subManager.hasSubscriptionRanges(conn));
    DbManager.commitOrRollback(conn, log);

    assertTrue(subManager.hasSubscriptionRanges());

    subManager.unsubscribeAu(conn, au);
    DbManager.commitOrRollback(conn, log);

    subscribedRanges =
	subManager.findSubscriptionRanges(conn, subscriptionSeq, true);
    assertEquals(1, subscribedRanges.size());
    assertEquals("1954-2000", subscribedRanges.get(0).toDisplayableString());

    unsubscribedRanges =
	subManager.findSubscriptionRanges(conn, subscriptionSeq, false);
    assertEquals(1, unsubscribedRanges.size());
    assertEquals("1954", unsubscribedRanges.get(0).toDisplayableString());
    assertTrue(subManager.hasSubscriptionRanges(conn));

    subManager.unsubscribeAu(conn, au);
    DbManager.commitOrRollback(conn, log);

    assertTrue(subManager.hasSubscriptionRanges());

    // Handle a subscription with an already unsubscribed range.
    subscribedRanges =
	subManager.findSubscriptionRanges(conn, subscriptionSeq, true);
    assertEquals(1, subscribedRanges.size());
    assertEquals("1954-2000", subscribedRanges.get(0).toDisplayableString());

    unsubscribedRanges =
	subManager.findSubscriptionRanges(conn, subscriptionSeq, false);
    assertEquals(1, unsubscribedRanges.size());
    assertEquals("1954", unsubscribedRanges.get(0).toDisplayableString());
    assertTrue(subManager.hasSubscriptionRanges(conn));
  }

  private TitleConfig makeTitleConfig(String vol) {
    ConfigParamDescr d1 = new ConfigParamDescr("base_url");
    ConfigParamDescr d2 = new ConfigParamDescr("volume");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, BASE_URL);
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, vol);
    a1.setEditable(false);
    a2.setEditable(false);
    TitleConfig tc1 = new TitleConfig("vol" + vol, plugin.getPluginId());
    tc1.setParams(ListUtil.list(a1, a2));
    return tc1;
  }

  private void clearSubscriptionRanges(Connection conn, Long subscriptionSeq,
      int subscribedCount, int unsubscribedCount) throws DbException {
    // Delete all the subscribed ranges.
    int count =
	subManager.deleteSubscriptionTypeRanges(conn, subscriptionSeq, true);
    assertEquals(subscribedCount, count);

    // Delete all the unsubscribed ranges.
    count =
	subManager.deleteSubscriptionTypeRanges(conn, subscriptionSeq, false);
    assertEquals(unsubscribedCount, count);
  }
  
  public void testIsSubscribable() throws TdbException {
    TdbTitle title = TdbTestUtil.makeRangeTestTitle(false);
    assertTrue(subManager.isSubscribable(title));

    title = TdbTestUtil.makeBookSeriesTestTitle("v1", "1954", "2000");
    assertTrue(subManager.isSubscribable(title));

    title = TdbTestUtil.makeBookTestTitle("v1", "1954", "2000");
    assertFalse(subManager.isSubscribable(title));

    Tdb tdb = new Tdb();

    Properties auDown = new Properties();
    auDown.setProperty("title", "Title[10.2468/24681357]");
    auDown.setProperty("issn", "1144-875X");
    auDown.setProperty("eissn", "7744-6521");
    auDown.setProperty("attributes.volume", "42");
    auDown.setProperty("journalTitle", "Journal[10.2468/24681357]");
    auDown.setProperty("attributes.publisher", "Publisher[10.2468/24681357]");
    auDown.setProperty("plugin", "org.lockss.plugin.simulated.SimulatedPlugin");
    auDown.setProperty("param.1.key", "pub_down");
    auDown.setProperty("param.1.value", "true");

    tdb.addTdbAuFromProperties(auDown);
    title = tdb.getTdbTitlesByIssn("1144-875X").iterator().next();
    assertFalse(subManager.isSubscribable(title));

    Properties auUp = new Properties();
    auUp.setProperty("title", "Title[10.2468/24681357]");
    auUp.setProperty("issn", "1144-875X");
    auUp.setProperty("eissn", "7744-6521");
    auUp.setProperty("attributes.volume", "54");
    auUp.setProperty("journalTitle", "Journal[10.2468/24681357]");
    auUp.setProperty("attributes.publisher", "Publisher[10.2468/24681357]");
    auUp.setProperty("plugin", "org.lockss.plugin.simulated.SimulatedPlugin");

    tdb.addTdbAuFromProperties(auUp);
    title = tdb.getTdbTitlesByIssn("1144-875X").iterator().next();
    assertTrue(subManager.isSubscribable(title));
  }
}
