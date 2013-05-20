/*
 * $Id: TestLocalHasher.java,v 1.1.2.2 2013-05-20 03:25:09 dshr Exp $
 */

/*

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.hasher;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import java.security.MessageDigest;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.repository.*;
import org.lockss.crawler.*;
import org.lockss.config.*;
import static org.lockss.util.DateTimeUtil.GMT_DATE_FORMATTER;

/**
 * This is the test class for org.lockss.hasher.LocalHasher
 *
 * @author David Rosenthal
 * @version 0.0
 */
public class TestLocalHasher extends LockssTestCase {

  protected static Logger logger = Logger.getLogger("TestLocalHasher");

  private static final SimpleDateFormat GMT_DATE_PARSER =
    new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
  static {
    GMT_DATE_PARSER.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  MyMockBaseUrlCacher cacher;
  MockCachedUrlSet mcus;
  MockPlugin plugin;

  private MyMockArchivalUnit mau;
  private MockLockssDaemon theDaemon;
  private LockssRepository repo;
  private int pauseBeforeFetchCounter;
  private int callbackCount;

  private MockNodeManager nodeMgr = new MockNodeManager();

  private static final String TEST_URL = "http://www.example.com/testDir/leaf1";
  private boolean saveDefaultSuppressStackTrace;
  private static final String TEST_CONTENT = "test content";
  private static final String TEST_CONTENT_GOOD_HASH =
    "SHA-1:1EEBDF4FDC9FC7BF283031B93F9AEF3338DE9052";
  private static final byte[] TEST_CONTENT_GOOD_HASH_ARRAY =
    ByteArray.fromHexString(TEST_CONTENT_GOOD_HASH.substring(6));
  private static final String TEST_CONTENT_GOOD_MD5 =
    "MD5:9473fdd0d880a43c21b7778d34872157";
  private static final String TEST_CONTENT_BAD_HASH =
    "SHA-1:1EEBDF4FDC9FC7BF283031B93F9AEF3338DE9053";
  private static final byte[] TEST_CONTENT_BAD_HASH_ARRAY =
    ByteArray.fromHexString(TEST_CONTENT_BAD_HASH.substring(6));
  private static final byte[] TEST_CONTENT_GOOD_MD5_ARRAY =
    ByteArray.fromHexString(TEST_CONTENT_GOOD_MD5.substring(4));

  public void setUp() throws Exception {
    super.setUp();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    CIProperties props = new CIProperties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();

    mau = new MyMockArchivalUnit();
    mau.setCrawlSpec(new SpiderCrawlSpec(tempDirPath, null));
    mau.setConfiguration(ConfigManager.newConfiguration());

    plugin = new MockPlugin();
    plugin.initPlugin(theDaemon);
    mau.setPlugin(plugin);

    repo =
      (LockssRepository)theDaemon.newAuManager(LockssDaemon.LOCKSS_REPOSITORY,
                                               mau);
    theDaemon.setLockssRepository(repo, mau);

    theDaemon.setNodeManager(nodeMgr, mau);

    saveDefaultSuppressStackTrace =
      CacheException.setDefaultSuppressStackTrace(false);
    getMockLockssDaemon().getAlertManager();
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    CacheException.setDefaultSuppressStackTrace(saveDefaultSuppressStackTrace);
    super.tearDown();
  }

  public void testUrlGoodChecksum() throws IOException {
    ConfigurationUtil.addFromArgs(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
				  "SHA-1");
    String alg =
      CurrentConfig.getParam(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
			     BaseUrlCacher.DEFAULT_CHECKSUM_ALGORITHM);
    MockCachedUrl cu = new MockCachedUrl(TEST_URL);
    cu.setChecksumAlgorithm(alg);
    cu.addVersion(TEST_CONTENT);
    cu.putChecksum(TEST_CONTENT_GOOD_HASH_ARRAY, cu.getChecksumAlgorithm());
    
    LocalHasher lh = new LocalHasher(new MismatchShouldBeCalledBack());
    callbackCount = 0;
    try {
      lh.doLocalHashNode(cu);
    } catch (IOException ex) {
      fail("threw " + ex);
    }
    assertEquals(1, lh.getFilesHashed());
    assertEquals(12, lh.getBytesHashed());
    assertEquals(0, callbackCount);
  }

  public void testUrlBadChecksum() throws IOException {
    ConfigurationUtil.addFromArgs(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
				  "SHA-1");
    String alg =
      CurrentConfig.getParam(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
			     BaseUrlCacher.DEFAULT_CHECKSUM_ALGORITHM);
    MockCachedUrl cu = new MockCachedUrl(TEST_URL);
    cu.setChecksumAlgorithm(alg);
    cu.addVersion(TEST_CONTENT);
    cu.putChecksum(TEST_CONTENT_BAD_HASH_ARRAY, cu.getChecksumAlgorithm());
    
    LocalHasher lh = new LocalHasher(new MismatchShouldBeCalledBack());
    callbackCount = 0;
    try {
      lh.doLocalHashNode(cu);
    } catch (IOException ex) {
      fail("threw " + ex);
    }
    assertEquals(1, lh.getFilesHashed());
    assertEquals(12, lh.getBytesHashed());
    assertEquals(1, callbackCount);
  }

  public void testUrlChecksumMissing() throws IOException {
    ConfigurationUtil.addFromArgs(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
				  "SHA-1");
    String alg =
      CurrentConfig.getParam(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
			     BaseUrlCacher.DEFAULT_CHECKSUM_ALGORITHM);
    MockCachedUrl cu = new MockCachedUrl(TEST_URL);
    cu.setChecksumAlgorithm(alg);
    cu.addVersion(TEST_CONTENT);
    
    LocalHasher lh = new LocalHasher(new MissingShouldBeCalledBack());
    callbackCount = 0;
    try {
      lh.doLocalHashNode(cu);
    } catch (IOException ex) {
      fail("threw " + ex);
    }
    assertEquals(1, lh.getFilesHashed());
    assertEquals(12, lh.getBytesHashed());
    assertEquals(1, callbackCount);
  }

  public void testUrlChecksumButNoContent() throws IOException {
    ConfigurationUtil.addFromArgs(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
				  "SHA-1");
    String alg =
      CurrentConfig.getParam(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
			     BaseUrlCacher.DEFAULT_CHECKSUM_ALGORITHM);
    MockCachedUrl cu = new MockCachedUrl(TEST_URL);
    cu.setChecksumAlgorithm(alg);
    cu.putChecksum(TEST_CONTENT_GOOD_HASH_ARRAY, cu.getChecksumAlgorithm());
    
    LocalHasher lh = new LocalHasher(new HashButNoContentShouldBeCalledBack());
    callbackCount = 0;
    try {
      lh.doLocalHashCachedUrlNode((CachedUrlSetNode)cu);
    } catch (IOException ex) {
      fail("threw " + ex);
    }
    // Nothing is hashed because there is no content
    assertEquals(0, lh.getFilesHashed());
    assertEquals(0, lh.getBytesHashed());
    assertEquals(1, callbackCount);
  }

  public void testUrlChecksumObsolete() throws IOException {
    ConfigurationUtil.addFromArgs(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
				  "SHA-1");
    String alg =
      CurrentConfig.getParam(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
			     BaseUrlCacher.DEFAULT_CHECKSUM_ALGORITHM);
    MockCachedUrl cu = new MockCachedUrl(TEST_URL);
    cu.addVersion(TEST_CONTENT);
    cu.putChecksum(TEST_CONTENT_GOOD_MD5_ARRAY, "MD5");
    
    LocalHasher lh = new LocalHasher(new ObsoleteShouldBeCalledBack());
    callbackCount = 0;
    try {
      lh.doLocalHashNode(cu);
    } catch (IOException ex) {
      fail("threw " + ex);
    }
    // The URL gets hashed with both old and new algorithms.
    assertEquals(2, lh.getFilesHashed());
    assertEquals(24, lh.getBytesHashed());
    assertEquals(1, callbackCount);
  }

  public void testUrlChecksumAlgorithmNotSupported() throws IOException {
    ConfigurationUtil.addFromArgs(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
				  "FOO");
    String alg =
      CurrentConfig.getParam(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
			     BaseUrlCacher.DEFAULT_CHECKSUM_ALGORITHM);
    MockCachedUrl cu = new MockCachedUrl(TEST_URL);
    cu.addVersion(TEST_CONTENT);
    cu.putChecksum(TEST_CONTENT_GOOD_MD5_ARRAY, "MD5");
    
    LocalHasher lh = new LocalHasher(new ShouldNotBeCalledBack());
    callbackCount = 0;
    try {
      lh.doLocalHashNode(cu);
    } catch (IOException ex) {
      fail("threw " + ex);
    }
    // Nothing is hashed because there is no content
    assertEquals(0, lh.getFilesHashed());
    assertEquals(0, lh.getBytesHashed());
    assertEquals(0, callbackCount);
  }

  public void testUrlChecksumOldAlgorithmObsolete() throws IOException {
    ConfigurationUtil.addFromArgs(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
				  "SHA-1");
    String alg =
      CurrentConfig.getParam(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
			     BaseUrlCacher.DEFAULT_CHECKSUM_ALGORITHM);
    MockCachedUrl cu = new MockCachedUrl(TEST_URL);
    cu.addVersion(TEST_CONTENT);
    cu.putChecksum(TEST_CONTENT_GOOD_MD5_ARRAY, "FOO");
    
    LocalHasher lh = new LocalHasher(new ShouldNotBeCalledBack());
    callbackCount = 0;
    try {
      lh.doLocalHashNode(cu);
    } catch (IOException ex) {
      logger.debug3("Threw " + ex);
      return;
    }
    fail("Should have thrown");
  }

  public void testAuOneUrlGoodChecksum() throws IOException {
    mcus = new MockCachedUrlSet(TEST_URL);
    mcus.setArchivalUnit(mau);
    mau.setAuCachedUrlSet(mcus);
    cacher = new MyMockBaseUrlCacher(mau, TEST_URL);
    ConfigurationUtil.addFromArgs(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
				  "SHA-1");
    cacher._input = new StringInputStream("test content");
    CIProperties props = new CIProperties();
    props.setProperty("test1", "value1");
    cacher._headers = props;
    cacher.cache();

    CachedUrl url = new BaseCachedUrl(mau, TEST_URL);
    InputStream is = url.getUnfilteredInputStream();
    assertReaderMatchesString(TEST_CONTENT, new InputStreamReader(is));

    Collection col = new ArrayList(1);
    col.add(url);
    mcus.setHashIterator(col.iterator());

    props = url.getProperties();
    assertEquals("value1", props.getProperty("test1"));
    assertEquals(TEST_CONTENT_GOOD_HASH,
		 props.getProperty(CachedUrl.PROPERTY_CHECKSUM));
    LocalHasher lh = new LocalHasher(new ShouldNotBeCalledBack());
    callbackCount = 0;
    try {
      lh.doLocalHash(mau);
    } catch (IOException ex) {
      fail("threw " + ex);
    }
    assertEquals(1, lh.getFilesHashed());
    assertEquals(12, lh.getBytesHashed());
    assertEquals(0, callbackCount);
  }

  // Test with CachedUrlSet, AU

  // Mock BaseUrlCacher that fakes the content and headers
  private class MyMockBaseUrlCacher extends BaseUrlCacher {
    InputStream _input = null;
    CIProperties _headers = null;

    public MyMockBaseUrlCacher(ArchivalUnit owner, String url) {
      super(owner, url);
    }

    @Override
    public CIProperties getUncachedProperties() {
      return _headers;
    }

    @Override
    protected LockssUrlConnection makeConnection0(String url,
						  LockssUrlConnectionPool pool)
	throws IOException {
      MockLockssUrlConnection conn;

      logger.debug3("Using old method for getUncachedInputStream");
      // this is way too much smarts for mock code, but is left here for
      // legacy support. It should be cleaned up
      conn = new MockLockssUrlConnection() {
	  String ifMod;
	  public void setRequestProperty(String key, String value) {
	    super.setRequestProperty(key, value);
	    if ("If-Modified-Since".equals(key)) {
	      ifMod = value;
	    }
	  }
	  public void execute() throws IOException {
	    super.execute();
	    long last = -1;
	    if (ifMod != null) {
	      try {
		last = GMT_DATE_PARSER.parse(ifMod).getTime();
	      } catch (ParseException e) {
	      }
	    }
	    if (last < TimeBase.nowMs()) {
	    } else {
	      setResponseCode(304);
	      setResponseInputStream(null);
	    }
	  }
	};
      conn.setResponseInputStream(_input);
      conn.setResponseCode(200);
      return conn;
    }

  }

  private class MyMockArchivalUnit extends MockArchivalUnit {
    boolean returnRealCachedUrl = false;

    public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
      return new BaseCachedUrlSet(this, cuss);
    }

    public CachedUrl makeCachedUrl(String url) {
      if (returnRealCachedUrl) {
        return new BaseCachedUrl(this, url);
      } else {
        return super.makeCachedUrl(url);
      }
    }
  }

  private class MyBaseCachedUrl extends BaseCachedUrl {
    byte[] checksum = null;
    public MyBaseCachedUrl(ArchivalUnit au, String url) {
      super(au,url);
    }
    public void setChecksum(byte[] hash) {
      checksum = hash;
    }
    public byte[] getChecksum() {
      return checksum;
    }
    public CachedUrl[] getVersions() {
      CachedUrl[] ret = new CachedUrl[1];
      ret[0] = this;
      return ret;
    }
  }
  private class ShouldNotBeCalledBack implements LocalHasher.Callback {
    public ShouldNotBeCalledBack() {
    }

    public void hashMismatch(CachedUrl cu, byte[] digest, String alg) {
      fail("hashMismatch() called");
    }
    public void hashMissing(CachedUrl cu, byte[] digest, String alg) {
      fail("hashMissing() called");
    }
    public void hashButNoContent(CachedUrl cu) {
      fail("hashButNoContent() called");
    }
    public void hashObsolete(CachedUrl cu, byte[] digest, String alg) {
      fail("hashObsolete() called");
    }
  }

  private class MismatchShouldBeCalledBack extends ShouldNotBeCalledBack {
    public MismatchShouldBeCalledBack() {
    }
    public void hashMismatch(CachedUrl cu, byte[] digest, String alg) {
      callbackCount++;
    }
  }    

  private class MissingShouldBeCalledBack extends ShouldNotBeCalledBack {
    public MissingShouldBeCalledBack() {
    }
    public void hashMissing(CachedUrl cu, byte[] digest, String alg) {
      callbackCount++;
    }
  }

  private class HashButNoContentShouldBeCalledBack extends ShouldNotBeCalledBack {
    public HashButNoContentShouldBeCalledBack() {
    }
    public void hashButNoContent(CachedUrl cu) {
      callbackCount++;
    }
  }

  private class ObsoleteShouldBeCalledBack extends ShouldNotBeCalledBack {
    public ObsoleteShouldBeCalledBack() {
    }
    public void hashObsolete(CachedUrl cu, byte[] digest, String alg) {
      callbackCount++;
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestLocalHasher.class.getName() };
    junit.swingui.TestRunner.main(testCaseList);
  }

}
