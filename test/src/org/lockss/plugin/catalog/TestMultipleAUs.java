/*
 * $Id: TestMultipleAUs.java,v 1.1.2.3 2012-06-19 22:17:52 tlipkis Exp $
 */

/*

 Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.catalog;

import java.util.*;
import java.io.*;
import java.net.MalformedURLException;
import java.security.*;

import org.lockss.app.LockssDaemon;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.simulated.*;
import org.lockss.protocol.MockIdentityManager;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.test.*;

public class TestMultipleAUs extends LockssTestCase {
  private static final String checksumAlgorithm = "SHA-1";

  static String mockPlugKey = PluginManager.pluginKeyFromName(MockPlugin.class.getName());
  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit testau[];
  private MockArchivalUnit catalogAU;
  private MessageDigest checksumProducer = null;
  private String tempDirPath;
  Properties props;
  private MockIdentityManager idmgr;

  private static final String[] BASE_URL = { "http://www.test0.org/",
      "http://www.test1.org/", "http://www.test2.org/" };

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    props = new Properties();
    props.put("org.lockss.platform.diskSpacePaths", tempDirPath);
    props.put(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM, "SHA-1");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    checksumProducer = MessageDigest.getInstance(checksumAlgorithm);
    theDaemon = getMockLockssDaemon();
    // Create the identity manager...
    idmgr = new MockIdentityManager();
    theDaemon.setIdentityManager(idmgr);
    idmgr.initService(theDaemon);
    theDaemon.getPluginManager();
    theDaemon.setDaemonInited(true);

    TimeBase.setSimulated();
    
    testau = new SimulatedArchivalUnit[BASE_URL.length];

    for (int i = 0; i < BASE_URL.length; i++) {
      testau[i] = PluginTestUtil.createAndStartSimAu(simAuConfig(BASE_URL[i]));
      PluginTestUtil.crawlSimAu(testau[i]);
    }

    catalogAU = setupCatalogAu(RootPageProducer.CATALOG_AU_ID, "Catalog", "");
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String base) throws IOException {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", getTempDir("simau").toString());
    conf.put("base_url", base);
    conf.put("depth", "1");
    conf.put("branch", "2");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_HTML +
				SimulatedContentGenerator.FILE_TYPE_XML));
    return conf;
  }

  private MockArchivalUnit setupCatalogAu(String id, String name,
					  String base_url) {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setAuId(id);
    mau.setName(name);
    PluginTestUtil.registerArchivalUnit(mau);

    MockCachedUrlSet cus = (MockCachedUrlSet) mau.getAuCachedUrlSet();
    cus.setEstimatedHashDuration(1000);
    return mau;
  }

  public void testProducer() throws Exception {
    RootPageProducer.produce(catalogAU);
  }

  public void xtestDumpAll() throws Exception {
    // for (Plugin plugin : (Collection<Plugin>) pm.getRegisteredPlugins()) {
    // for(ArchivalUnit au : plugin.getAllAus() ) {
    // System.err.println(String.format("Plugin=%s Au=%s",
    // plugin.getPluginName(), au.getName()));
    // }
    // }
    UrlToChecksumMapper mapper = new UrlToChecksumMapperBuffered();
    for (ArchivalUnit au : theDaemon.getPluginManager().getAllAus()) {
      System.err.println(String.format("Au id=%s name=%s", au.getAuId(),
          au.getName()));
      mapper.generateXMLMap(au, new OutputStreamWriter(System.err));
      System.err.println();
    }
  }

  public void xtestFillCatalog() throws Exception {
    UrlToChecksumMapper mapper = new UrlToChecksumMapperBuffered();
    for (ArchivalUnit au : theDaemon.getPluginManager().getAllAus()) {
      // System.err.println(String.format("Au id=%s name=%s", au.getAuId(),
      // au.getName()));
      mapper.generateXMLMap(au, new OutputStreamWriter(System.err));
      System.err.println();
    }
  }

  private String checksum(String url) {
    // for lack of content, just do a checksum of the url itself
    checksumProducer.reset();
    checksumProducer.update(url.getBytes());
    byte[] bchecksum = checksumProducer.digest();
    return ByteArray.toHexString(bchecksum);
  }

}
