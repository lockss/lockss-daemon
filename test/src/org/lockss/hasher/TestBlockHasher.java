/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.io.*;
import java.security.*;

import org.lockss.test.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.filter.*;
import org.lockss.crawler.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.repository.*;

public class TestBlockHasher extends LockssTestCase {
  private static final String BASE_URL = "http://www.test.com/blah/";

  static String[] urls = {
    "lockssau:",
    BASE_URL,
    BASE_URL + "x.html",
    BASE_URL + "foo/",
    BASE_URL + "foo/1",
    BASE_URL + "foo/2",
    BASE_URL + "foo/2/a.txt",
    BASE_URL + "foo/2/b.txt",
    BASE_URL + "foo/2/c.txt",
    BASE_URL + "foo/2/d.txt",
    BASE_URL + "foo/3",
    BASE_URL + "foo/3/a.html",
    BASE_URL + "foo/3/b.html",
    BASE_URL + "foo/outside_crawl_spec/b.html",
  };

  MockArchivalUnit mau = null;
  MockMessageDigest dig = null;
  private MockLockssDaemon daemon;
  private RepositoryManager repoMgr;
  private LockssRepositoryImpl repo;
  private MockAuState maus;
  private String tempDirPath;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    dig = new MockMessageDigest(); 
    mau = new MockArchivalUnit(new MockPlugin(daemon));
    tempDirPath = setUpDiskSpace();
    repoMgr = daemon.getRepositoryManager();
    repoMgr.startService();
    repo = (LockssRepositoryImpl)LockssRepositoryImpl.createNewLockssRepository(
        mau);
    daemon.setLockssRepository(repo, mau);
    repo.initService(daemon);
    repo.startService();
    MockNodeManager nodeMgr = new MockNodeManager();
    daemon.setNodeManager(nodeMgr, mau);
    maus = new MockAuState(mau);
    nodeMgr.setAuState(maus);
  }

  MockArchivalUnit setupContentTree() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    List files = new ArrayList();
    for (String url : urls) {
      CachedUrl cu = mau.addUrl(url, false, true);
      files.add(cu);
      if (url.matches(".*outside_crawl_spec.*")) {
	mau.removeUrlToBeCached(url);
      }
    }
    cus.setHashItSource(files);
    return mau;
  }

  void addContent(MockArchivalUnit mau, String url, String content) {
    MockCachedUrl cu = (MockCachedUrl)mau.makeCachedUrl(url);
    cu.setContent(content);
  }
  
  void addContent(MockArchivalUnit mau, String url, String content,
		  CIProperties props) {
    MockCachedUrl cu = (MockCachedUrl)mau.makeCachedUrl(url);
    cu.setContent(content);
    cu.setProperties(props);
  }
  
  CachedUrl addVersion(MockArchivalUnit mau, String url, String content) {
    MockCachedUrl cu = (MockCachedUrl)mau.makeCachedUrl(url);
    return cu.addVersion(content);
  }

  CachedUrl addVersionAndChecksum(MockArchivalUnit mau, String url,
				  String content, String checksum) {
    CachedUrl ret = addVersion(mau, url, content);
    ret.addProperty(CachedUrl.PROPERTY_CHECKSUM, checksum);
    return ret;
  }

  private long hashToEnd(CachedUrlSetHasher hasher, int stepSize)
      throws IOException {
    long cnt = 0;
    while (!hasher.finished()) {
      cnt += hasher.hashStep(stepSize);
    }
    return cnt;
  }

  /** Return array of bytes in string */
  byte[] bytes(String str) {
    return str.getBytes();
  }

  /** Return array of bytes in concatenated strings */
  byte[] bytes(String s1, String s2) {
    return (s1 + s2).getBytes();
  }

  static final byte[][] EMPTY_BYTE_ARRAY_ARRAY = new byte[0][];

  void assertEvent(String expectedUrl, String expectedString,
		   Object eventObj) {
    assertEvent(expectedUrl, expectedString, eventObj, false);
  }

  void assertEvent(String expectedUrl, String expectedString,
		   Object eventObj, boolean includeUrl) {
    assertEvent(expectedUrl, expectedString.length(), expectedString,
		eventObj, includeUrl);
  }

  void assertEvent(String expectedUrl, int expectedLength,
		   String expectedString,
		   Object eventObj, boolean includeUrl) {
    if (includeUrl) {
      assertEvent(expectedUrl, expectedLength,
		  ListUtil.list(bytes(expectedUrl + expectedString)),
		  eventObj);
    } else {
      assertEvent(expectedUrl, expectedLength,
		  ListUtil.list(bytes(expectedString)), eventObj);
    }
  }

  void assertEvent(String expectedUrl, long expectedLength, List hashed,
		   Object eventObj) {
    assertEvent(expectedUrl, expectedLength,
		(byte[][])hashed.toArray(EMPTY_BYTE_ARRAY_ARRAY),
		eventObj);
  }

  void assertEvent(String expectedUrl,
		   long expectedUnfilteredLength, long expectedFilteredLength,
		   String expectedString, Object eventObj, boolean includeUrl) {
    assertEvent(expectedUrl, expectedUnfilteredLength, expectedFilteredLength,
		( includeUrl
		  ? ListUtil.list(bytes(expectedUrl + expectedString))
		  : ListUtil.list(bytes(expectedString))),
		eventObj);
  }

  void assertEvent(String expectedUrl,
		   long expectedUnfilteredLength, long expectedFilteredLength,
		   List hashed, Object eventObj) {
    assertEvent(expectedUrl, expectedUnfilteredLength, expectedFilteredLength,
		(byte[][])hashed.toArray(EMPTY_BYTE_ARRAY_ARRAY),
		eventObj);
  }

  void assertEvent(String expectedUrl, long expectedLength,
		   byte[][]expectedHashed, Object eventObj) {
    assertEvent(expectedUrl, expectedLength, expectedLength,
		expectedHashed, eventObj);
  }

  void assertEvent(String expectedUrl, long expectedUnfilteredLength,
		   long expectedFilteredLength,
		   byte[][]expectedHashed, Object eventObj) {
    Event event = (Event)eventObj;
    HashBlock hblock = event.hblock;
    assertEquals(expectedUrl, hblock.getUrl());
    HashBlock.Version curver = hblock.currentVersion();
    assertEquals(expectedUnfilteredLength, curver.getUnfilteredLength());
    assertEquals(expectedFilteredLength, curver.getFilteredLength());
    for (int ix = 0; ix < event.byteArrays.length; ix++) {
      assertEquals(""+ix, expectedHashed[ix], event.byteArrays[ix]);
    }
    for (HashBlock.Version v : hblock.getVersions()) {
      assertNull("Hash error should have been null", v.getHashError());
    }
  }

  public void assertEventWithError(String url, Object eventObj) {
    Event event = (Event)eventObj;
    HashBlock hblock = event.hblock;
    assertEquals(event.hblock.url, url);
    for (HashBlock.Version v : hblock.getVersions()) {
      assertNotNull("Hash error should not have been null", 
                    event.hblock.lastVersion().getHashError());  
    }
  }

  public void assertEventWithError(String url, Object eventObj, String exPat) {
    Event event = (Event)eventObj;
    HashBlock hblock = event.hblock;
    assertEquals(event.hblock.url, url);
    for (HashBlock.Version v : hblock.getVersions()) {
      assertMatchesRE(exPat,
		      event.hblock.lastVersion().getHashError().toString());
    }
  }

  void assertEqualBytes(byte[] expectedHashed, byte[][] actualHash) {
    for (int ix = 0; ix < actualHash.length; ix++) {
      assertEquals(expectedHashed, actualHash[ix]);
    }
  }

  // a null event handler
  BlockHasher.EventHandler hand0 = new BlockHasher.EventHandler() {
      public void blockDone(HashBlock hblock) {
      }
    };

  public void testNullArgs() throws IOException {
    MockCachedUrlSet cus = new MockCachedUrlSet(mau);
    cus.setHashItSource(ListUtil.list(BASE_URL));
    MessageDigest[] digs = { dig };
    MessageDigest[] digs2 = { dig, dig };
    byte[][] inits = {null};
    // this should work
    new BlockHasher(cus, digs, inits, hand0);
    try {
      new BlockHasher(null, digs, inits, hand0);
      fail("null cus should throw");
    } catch (NullPointerException e) {}
    try {
      new BlockHasher(cus, null, inits, hand0);
      fail("null digest should throw");
    } catch (NullPointerException e) {}
    try {
      new BlockHasher(cus, digs, null, hand0);
      fail("null initByteArrays should throw");
    } catch (NullPointerException e) {}
    // null handler does not throw
    new BlockHasher(cus, digs, inits, null);
    try {
      new BlockHasher(cus, digs2, inits, hand0);
      fail("Unequal length digests and initByteArrays should throw");
    } catch (IllegalArgumentException e) {}
    cus.setHashIterator(null);
    cus.setHashItSource(null);
    try {
      new BlockHasher(cus, digs, inits, hand0);
      fail("Null iterator should throw");
    } catch (IllegalArgumentException e) {}

  }

  public void testEmptyIterator() throws Exception {
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockCachedUrlSet cus = new MockCachedUrlSet(mau);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    cus.setHashItSource(Collections.EMPTY_LIST);
    CachedUrlSetHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());
    assertEmpty(handRec.getEvents());
  }

  public void testSetConfig() throws Exception {
    MockCachedUrlSet cus = new MockCachedUrlSet(mau);
    cus.setHashIterator(CollectionUtil.EMPTY_ITERATOR);
    cus.setFlatIterator(null);
    cus.setEstimatedHashDuration(54321);
    MessageDigest[] digs = { dig, dig };
    byte[][] inits = {null, null};
    
    // First hasher should have default hashUpTo
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, hand0);
    assertEquals(hasher.getMaxVersions(),
                 BlockHasher.DEFAULT_HASH_MAX_VERSIONS);
    
    ConfigurationUtil.setFromArgs(BlockHasher.PARAM_HASH_MAX_VERSIONS, "99");
    
    BlockHasher hasher2 = new MyBlockHasher(cus, digs, inits, hand0);
    assertEquals(hasher2.getMaxVersions(), 99);
    
    ConfigurationUtil.setFromArgs(BlockHasher.PARAM_HASH_MAX_VERSIONS, "18");

    BlockHasher hasher3 = new MyBlockHasher(cus, digs, inits, hand0);
    assertEquals(hasher3.getMaxVersions(), 18);

    BlockHasher hasher4 = new MyBlockHasher(cus, -1, digs, inits, hand0);
    assertEquals(hasher4.getMaxVersions(), 18);

    BlockHasher hasher5 = new MyBlockHasher(cus, 4, digs, inits, hand0);
    assertEquals(hasher5.getMaxVersions(), 4);
  }

  public void testAccessors() throws IOException {
    MockCachedUrlSet cus = new MockCachedUrlSet(mau);
    cus.setHashIterator(CollectionUtil.EMPTY_ITERATOR);
    cus.setFlatIterator(null);
    cus.setEstimatedHashDuration(54321);
    MessageDigest[] digs = { dig, dig };
    byte[][] inits = {null, null};
    CachedUrlSetHasher hasher = new MyBlockHasher(cus, digs, inits, hand0);
    assertSame(cus, hasher.getCachedUrlSet());
    assertEquals(54321, hasher.getEstimatedHashDuration());
    assertEquals("B(2)", hasher.typeString());
    hasher.storeActualHashDuration(12345, null);
    assertEquals(12345, cus.getActualHashDuration());
  }

  /**
   * Ensure that MessageDigests implementations used on the test platform
   * are Cloneable.  This is fairly fragile, since the underlying implementation
   * of the MessageDigests may or may not be the same between VMs,
   * but this seems to be the only way to test for it.
   * 
   * @throws Exception
   */
  public void testMessageDigestImplementationsAreCloneable() throws Exception {
    MessageDigest md5 = MessageDigest.getInstance("MD5");
    MessageDigest sha1 = MessageDigest.getInstance("SHA1");
    MessageDigest sha = MessageDigest.getInstance("SHA");
    
    MessageDigest[] testDigs = {md5, sha1, sha};
    byte[][] inits = {null, null, null};
    
    CaptureBlocksEventHandler blockHandler = 
      new CaptureBlocksEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    // Should NOT throw IllegalArgumentException.
    CachedUrlSetHasher hasher =
      new MyBlockHasher(cus, testDigs, inits, blockHandler);
  }
  
  public void testIsIncluded() throws Exception {
    // Need CrawlManager to check global exclusion below
    CrawlManager cm = getMockLockssDaemon().getCrawlManager();
    MockArchivalUnit mau = MockArchivalUnit.newInited(getMockLockssDaemon());
    CachedUrl cu = mau.addUrl(urls[2], false, true);
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.setHashItSource(Collections.EMPTY_LIST);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, null);
    assertFalse(hasher.isIncluded(cu));
    addContent(mau, urls[2], s1);
    assertTrue(hasher.isIncluded(cu));

    ConfigurationUtil.addFromArgs(CrawlManagerImpl.PARAM_EXCLUDE_URL_PATTERN,
				  "http:");
    assertFalse(hasher.isIncluded(cu));

    ConfigurationUtil.addFromArgs(CrawlManagerImpl.PARAM_EXCLUDE_URL_PATTERN,
				  "xyznotfound");
    assertTrue(hasher.isIncluded(cu));

    // next test need new hashers as config, exclude pats, are accessed at
    // creation
    ConfigurationUtil.addFromArgs(BlockHasher.PARAM_IGNORE_FILES_OUTSIDE_CRAWL_SPEC, "true");
    hasher = new MyBlockHasher(cus, digs, inits, null);
    assertTrue(hasher.isIncluded(cu));
    mau.removeUrlToBeCached(urls[2]);
    assertFalse(hasher.isIncluded(cu));
    mau.addUrlToBeCached(urls[2]);
    assertTrue(hasher.isIncluded(cu));

    List<String> pats = ListUtil.list("not-there");
    mau.setExcludeUrlsFromPollsPatterns(RegexpUtil.compileRegexps(pats));
    hasher = new MyBlockHasher(cus, digs, inits, null);
    assertTrue(hasher.isIncluded(cu));
    pats = ListUtil.list("x\\.html");
    mau.setExcludeUrlsFromPollsPatterns(RegexpUtil.compileRegexps(pats));
    hasher = new MyBlockHasher(cus, digs, inits, null);
    assertFalse(hasher.isIncluded(cu));
  }

  public void testIsExcludedByPlugin() throws Exception {
    // Need CrawlManager to check global exclusion below
    CrawlManager cm = getMockLockssDaemon().getCrawlManager();
    MockArchivalUnit mau = MockArchivalUnit.newInited(getMockLockssDaemon());
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.setHashItSource(Collections.EMPTY_LIST);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, null);
    CachedUrl cu2 = mau.addUrl(urls[2], false, true);
    CachedUrl cu4 = mau.addUrl(urls[4], false, true);
    CachedUrl cu6 = mau.addUrl(urls[6], false, true);
    addContent(mau, urls[2], s1);
    addContent(mau, urls[4], s1);
    addContent(mau, urls[6], s1);
    assertTrue(hasher.isIncluded(cu2));
    assertTrue(hasher.isIncluded(cu4));
    assertTrue(hasher.isIncluded(cu6));
    mau.setExcludeUrlsFromPollsPatterns(RegexpUtil.compileRegexps(ListUtil.list("/2/")));
    hasher = new MyBlockHasher(cus, digs, inits, null);
    assertTrue(hasher.isIncluded(cu2));
    assertTrue(hasher.isIncluded(cu4));
    assertFalse(hasher.isIncluded(cu6));
  }

  public void testNoContent() throws Exception {
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    CachedUrlSetHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());
    assertEmpty(handRec.getEvents());
  }

  public void testOneContent(int stepSize, boolean includeUrl)
      throws Exception {
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    addContent(mau, urls[4], "foo");
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    if (includeUrl) {
      hasher.setIncludeUrl(includeUrl);
      assertEquals(urls[4].length() + 3, hashToEnd(hasher, stepSize));
    } else {
      assertEquals(3, hashToEnd(hasher, stepSize));
    }
    assertTrue(hasher.finished());
    List<Event> events = handRec.getEvents();
    assertEquals(1, events.size());
    assertEvent(urls[4], 3, "foo", events.get(0), includeUrl);
  }
  
  public void testOneContent() throws Exception {
    testOneContent(1, false);
    testOneContent(3, false);
    testOneContent(100, false);
  }
  
  public void testOneContentIncludeUrl() throws Exception {
    testOneContent(1, true);
    testOneContent(3, true);
    testOneContent(100, true);
  }
  
  public void testDefaultLocalHashAlgorithm() {
    String defHashAlg = BlockHasher.DEFAULT_LOCAL_HASH_ALGORITHM;
    String msg = "BlockHasher.DEFAULT_LOCAL_HASH_ALGORITHM must be set to a " +
      "valid hash algorithm - see PollManager.processConfigMacros()";
    assertFalse(msg, StringUtil.isNullString(defHashAlg));
    try {
      MessageDigest.getInstance(defHashAlg);
    } catch (NoSuchAlgorithmException ex) {
      fail(msg);
    }
  }

  void enableLocalHash(String blockHasherAlg) {
    ConfigurationUtil.addFromArgs(BlockHasher.PARAM_ENABLE_LOCAL_HASH, "true",
				  BlockHasher.PARAM_LOCAL_HASH_ALGORITHM,
				  blockHasherAlg);
  }

  void enableLocalHash(String blockHasherAlg, String urlCacherAlg) {
    enableLocalHash(blockHasherAlg);
    ConfigurationUtil.addFromArgs(DefaultUrlCacher.PARAM_CHECKSUM_ALGORITHM,
				  urlCacherAlg);
  }

  public void testOneContentLocalHashGood(int stepSize)
      throws Exception {
    enableLocalHash("SHA-1", "SHA-1");

    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    CIProperties props = new CIProperties();
    props.put(CachedUrl.PROPERTY_CHECKSUM,
	      "SHA-1:0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33");
    addContent(mau, urls[4], "foo", props);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setFiltered(false);
    assertEquals(3, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List<Event> events = handRec.getEvents();
    assertEquals(1, events.size());
    assertEvent(urls[4], 3, "foo", events.get(0), false);
    LocalHashResult lhr = hasher.getLocalHashResult();
    assertEquals(1, lhr.getMatchingVersions());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlyHashedVersions());
  }
  
  public void testOneContentLocalHashObsolete(int stepSize)
      throws Exception {
    enableLocalHash("SHA-1", "MD5");
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    CIProperties props = new CIProperties();
    props.put(CachedUrl.PROPERTY_CHECKSUM,
	      "MD5:acbd18db4cc2f85cedef654fccc4a4d8");
    addContent(mau, urls[4], "foo", props);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    MyBlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setFiltered(false);
    assertEquals(3, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List<Event> events = handRec.getEvents();
    assertEquals(1, events.size());
    assertEvent(urls[4], 3, "foo", events.get(0), false);
    LocalHashResult lhr = hasher.getLocalHashResult();
    assertEquals(1, lhr.getMatchingVersions());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlyHashedVersions());
    AuSuspectUrlVersions asuv = AuUtil.getSuspectUrlVersions(mau);
    assertFalse(asuv.isSuspect(urls[4], 0));
    maus.recomputeNumCurrentSuspectVersions();
    assertEquals(0, maus.getNumCurrentSuspectVersions());
  }
  
  public void testOneContentLocalHashBad(int stepSize)
      throws Exception {
    enableLocalHash("SHA-1", "MD5");

    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    CIProperties props = new CIProperties();
    props.put(CachedUrl.PROPERTY_CHECKSUM,
	      "SHA-1:deadbeef");
    addContent(mau, urls[4], "foo", props);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setFiltered(false);
    assertEquals(3, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List<Event> events = handRec.getEvents();
    assertEquals(1, events.size());
    assertEvent(urls[4], 3, "foo", events.get(0), false);
    LocalHashResult lhr = hasher.getLocalHashResult();
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(1, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlyHashedVersions());
    assertEquals(1, maus.getNumCurrentSuspectVersions());
  }
  
  public void testOneContentLocalHashSuspect(int stepSize, int urlIndex)
      throws Exception {
    enableLocalHash("SHA-1", "MD5");

    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    CIProperties props = new CIProperties();
    props.put(CachedUrl.PROPERTY_CHECKSUM,
	      "SHA-1:deadbeef");
    addContent(mau, urls[urlIndex], "foo", props);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    MyBlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setFiltered(false);
    hasher.setExcludeSuspectVersions(true);
    assertEquals(3, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List<Event> events = handRec.getEvents();
    assertEquals(1, events.size());
    assertEvent(urls[urlIndex], 3, "foo", events.get(0), false);

    LocalHashResult lhr = hasher.getLocalHashResult();
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(1, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlyHashedVersions());

    AuSuspectUrlVersions asuv = AuUtil.getSuspectUrlVersions(mau);
    assertTrue(asuv.isSuspect(urls[urlIndex], 0));
    // Second pass should exclude the suspect URL
    // NB - test confirms that BlocksHasher handles case of
    // URL with 0 versions.
    RecordingEventHandler handRec2 = new RecordingEventHandler();
    BlockHasher hasher2 = new MyBlockHasher(cus, digs, inits, handRec2);
    hasher2.setFiltered(false);
    hasher2.setExcludeSuspectVersions(true);
    assertEquals(0, hashToEnd(hasher2, stepSize));
    assertTrue(hasher2.finished());
    List<Event> events2 = handRec2.getEvents();
    assertEquals(0, events2.size());
    LocalHashResult lhr2 = hasher2.getLocalHashResult();
    assertEquals(1, lhr2.getSkippedVersions());
    assertEquals(0, lhr2.getMatchingVersions());
    assertEquals(0, lhr2.getNewlySuspectVersions());
    assertEquals(0, lhr2.getNewlyHashedVersions());
  }
  
  public void testOneContentLocalHashMissing(int stepSize)
      throws Exception {
    enableLocalHash("SHA-1");
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    CIProperties props = new CIProperties();
    addContent(mau, urls[4], "foo", props);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setFiltered(false);
    assertEquals(3, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List<Event> events = handRec.getEvents();
    assertEquals(1, events.size());
    assertEvent(urls[4], 3, "foo", events.get(0), false);
    LocalHashResult lhr = hasher.getLocalHashResult();
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(1, lhr.getNewlyHashedVersions());

    // ensure that the checksum property was stored on the CU
    CachedUrl cu = mau.makeCachedUrl(urls[4]);
    CIProperties props2 = cu.getProperties();
    assertEquals("SHA-1:0BEEC7B5EA3F0FDBC95D0DD47F3C5BC275DA8A33",
		 props2.get(CachedUrl.PROPERTY_CHECKSUM));
  }
  
  public void testOneContentLocalHashMissing2(int stepSize)
      throws Exception {
    enableLocalHash("SHA-1");
    // First pass creates the stored hash
    CIProperties props = new CIProperties();
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    addContent(mau, urls[4], "foo", props);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setFiltered(false);
    assertEquals(3, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List<Event> events = handRec.getEvents();
    assertEquals(1, events.size());
    assertEvent(urls[4], 3, "foo", events.get(0), false);
    LocalHashResult lhr = hasher.getLocalHashResult();
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(1, lhr.getNewlyHashedVersions());
    // Second pass validates it
    RecordingEventHandler handRec2 = new RecordingEventHandler();
    BlockHasher hasher2 = new MyBlockHasher(cus, digs, inits, handRec2);
    hasher2.setFiltered(false);
    assertEquals(3, hashToEnd(hasher2, stepSize));
    assertTrue(hasher2.finished());
    List<Event> events2 = handRec2.getEvents();
    assertEquals(1, events2.size());
    assertEvent(urls[4], 3, "foo", events2.get(0), false);
    LocalHashResult lhr2 = hasher2.getLocalHashResult();
    assertEquals(1, lhr2.getMatchingVersions());
    assertEquals(0, lhr2.getNewlySuspectVersions());
    assertEquals(0, lhr2.getNewlyHashedVersions());
    AuSuspectUrlVersions asuv = AuUtil.getSuspectUrlVersions(mau);
    assertFalse(asuv.isSuspect(urls[4], 0));
    // Third pass has corrupt content
    MockCachedUrl cu = (MockCachedUrl)mau.makeCachedUrl(urls[4]);
    String corrupt = "Corrupt Content";
    cu.setContent(corrupt);
    RecordingEventHandler handRec3 = new RecordingEventHandler();
    MyBlockHasher hasher3 = new MyBlockHasher(cus, digs, inits, handRec3);
    hasher3.setFiltered(false);
    assertEquals(corrupt.length(), hashToEnd(hasher3, stepSize));
    assertTrue(hasher3.finished());
    List<Event> events3 = handRec3.getEvents();
    assertEquals(1, events3.size());
    LocalHashResult lhr3 = hasher3.getLocalHashResult();
    assertEquals(0, lhr3.getMatchingVersions());
    assertEquals(1, lhr3.getNewlySuspectVersions());
    assertEquals(0, lhr3.getNewlyHashedVersions());
    assertTrue(asuv.isSuspect(urls[4], 0));
  }
  
  String randomString(int len) {
    return org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(len);
  }

  public void testOneContentLocalHashIncompleteRead()
      throws Exception {
    enableLocalHash("SHA-1");
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    mau.setHashFilterFactory(new IncompleteReadFilterFactory(250));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    CIProperties props = new CIProperties();
    String cont = randomString(100000);	// must be longer than will be buffered
    addContent(mau, urls[4], cont, props);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setFiltered(true);
    assertEquals(250, hashToEnd(hasher, 100));
    assertTrue(hasher.finished());
    // The hash should have finished
    List<Event> events = handRec.getEvents();
    assertEquals(1, events.size());
    assertEvent(urls[4], 100000, 250, cont.substring(0, 250),
		events.get(0), false);
    // But no localhash because the underlying stream wasn't completely read
    LocalHashResult lhr = hasher.getLocalHashResult();
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlyHashedVersions());
    // XXXXXXXXXXXXXXXXXXXXX
    // ensure no valid hash
  }
  
  // Filter reset relies on BaseCachedUrl wrapping HashedInputStream in a
  // BufferedInputStream; this doesn't test that because it uses
  // MockCachedUrl
  public void testOneContentLocalHashMarkReset() throws Exception {
    enableLocalHash("SHA-1");
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    mau.setHashFilterFactory(new MarkResetFilterFactory(20000, 18000));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    CIProperties props = new CIProperties();
    String cont = randomString(100000);	// must be longer than will be buffered
    addContent(mau, urls[4], cont, props);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setFiltered(true);
    assertEquals(118000, hashToEnd(hasher, 100));
    assertTrue(hasher.finished());
    List<Event> events = handRec.getEvents();
    assertEquals(1, events.size());
    assertEvent(urls[4], 100000, 118000, cont.substring(0, 18000) + cont,
		events.get(0), false);
    LocalHashResult lhr = hasher.getLocalHashResult();
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(1, lhr.getNewlyHashedVersions());
  }

  public void testOneContentLocalHashMarkIllegalReset() throws Exception {
    enableLocalHash("SHA-1");
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    mau.setHashFilterFactory(new MarkResetFilterFactory(10000, 18000));
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    CIProperties props = new CIProperties();
    String cont = randomString(100000);	// must be longer than will be buffered
    addContent(mau, urls[4], cont, props);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setFiltered(true);
    // Throws IOException: Resetting to invalid mark before any bytes hashed
    assertEquals(0, hashToEnd(hasher, 100));
    assertTrue(hasher.finished());
    List<Event> events = handRec.getEvents();
    assertEquals(1, events.size());
    HashBlock hb = events.get(0).hblock;
    assertEventWithError(urls[4], events.get(0),
			 "IOException: Resetting to invalid mark");
    LocalHashResult lhr = hasher.getLocalHashResult();
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlyHashedVersions());
  }

  public void testOneContentLocalHashGoodNoDigest(int stepSize)
      throws Exception {
    enableLocalHash("SHA-1", "SHA-1");
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    CIProperties props = new CIProperties();
    props.put(CachedUrl.PROPERTY_CHECKSUM,
	      "SHA-1:0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33");
    addContent(mau, urls[4], "foo", props);
    MessageDigest[] digs = { };
    byte[][] inits = { };
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setFiltered(false);
    assertEquals(0, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List<Event> events = handRec.getEvents();
    assertEquals(1, events.size());
    assertEvent(urls[4], 3, "foo", events.get(0), false);
    LocalHashResult lhr = hasher.getLocalHashResult();
    assertEquals(1, lhr.getMatchingVersions());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlyHashedVersions());
  }
  
  public void testOneContentLocalHashBadNoDigest(int stepSize)
      throws Exception {
    enableLocalHash("SHA-1", "MD5");
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    CIProperties props = new CIProperties();
    props.put(CachedUrl.PROPERTY_CHECKSUM,
	      "SHA-1:deadbeef");
    addContent(mau, urls[4], "foo", props);
    MessageDigest[] digs = { };
    byte[][] inits = { };
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setFiltered(false);
    assertEquals(0, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List<Event> events = handRec.getEvents();
    assertEquals(1, events.size());
    assertEvent(urls[4], 3, "foo", events.get(0), false);
    LocalHashResult lhr = hasher.getLocalHashResult();
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(1, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlyHashedVersions());
  }
  
  public void testOneContentLocalHashGood() throws Exception {
    testOneContentLocalHashGood(1);
    testOneContentLocalHashGood(3);
    testOneContentLocalHashGood(100);
  }
  
  public void testOneContentLocalHashObsolete() throws Exception {
    testOneContentLocalHashObsolete(1);
    testOneContentLocalHashObsolete(3);
    testOneContentLocalHashObsolete(100);
  }
  
  // These mark suspect versions so must be in separate tests
  public void testOneContentLocalHashBad1() throws Exception {
    testOneContentLocalHashBad(1);
  }
  
  public void testOneContentLocalHashBad3() throws Exception {
    testOneContentLocalHashBad(3);
  }
  
  public void testOneContentLocalHashBad100() throws Exception {
    testOneContentLocalHashBad(100);
  }
  
  public void testOneContentLocalHashSuspect() throws Exception {
    // Must use a different urlindex each time as file gets marked suspect
    // and isn't hashed next time
    testOneContentLocalHashSuspect(1, 4);
    testOneContentLocalHashSuspect(3, 5);
    assertEquals(2, maus.getNumCurrentSuspectVersions());
    testOneContentLocalHashSuspect(100, 6);
    assertEquals(3, maus.getNumCurrentSuspectVersions());
  }
  
  public void testOneContentLocalHashMissing() throws Exception {
    testOneContentLocalHashMissing(1);
    testOneContentLocalHashMissing(3);
    testOneContentLocalHashMissing(100);
  }
  
  // These mark suspect versions so must be in separate tests
  public void testOneContentLocalHashMissing2a() throws Exception {
    testOneContentLocalHashMissing2(1);
  }
  
  public void testOneContentLocalHashMissing2b() throws Exception {
    testOneContentLocalHashMissing2(3);
  }
  
  public void testOneContentLocalHashMissing2c() throws Exception {
    testOneContentLocalHashMissing2(100);
  }
  

  public void testOneContentLocalHashGoodNoDigest() throws Exception {
    testOneContentLocalHashGoodNoDigest(1);
    testOneContentLocalHashGoodNoDigest(3);
    testOneContentLocalHashGoodNoDigest(100);
  }
  
  // These mark suspect versions so must be in separate tests
  public void testOneContentLocalHashBadNoDigest1() throws Exception {
    testOneContentLocalHashBadNoDigest(1);
  }
  
  public void testOneContentLocalHashBadNoDigest3() throws Exception {
    testOneContentLocalHashBadNoDigest(3);
  }
  
  public void testOneContentLocalHashBadNoDigest100() throws Exception {
    testOneContentLocalHashBadNoDigest(100);
  }
  
  public void testOneContentThreeVersions(int stepSize, boolean includeUrl)
      throws Exception {
    CaptureBlocksEventHandler blockHandler = 
      new CaptureBlocksEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    
    // Adding versions, from least recent to most recent.
    addVersion(mau, urls[2], "aaaa");
    addVersion(mau, urls[2], "bb");
    addVersion(mau, urls[2], "ccc");
    
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, blockHandler);
    hasher.setIncludeUrl(includeUrl);
    // 9 bytes total for all three versions.
    if (includeUrl) {
      assertEquals(urls[2].length() * 3 + 9, hashToEnd(hasher, stepSize));
    } else {
      assertEquals(9, hashToEnd(hasher, stepSize));
    }
    assertTrue(hasher.finished());
    List blocks = blockHandler.getBlocks();
    assertEquals(1, blocks.size());
    HashBlock b = (HashBlock)blocks.get(0);
    assertEquals(3, b.size());
    
    HashBlock.Version[] versions = b.getVersions();

    if (includeUrl) {
      assertEqualBytes(bytes( urls[2] + "ccc"), versions[0].getHashes());
      assertEqualBytes(bytes( urls[2] + "bb"), versions[1].getHashes());
      assertEqualBytes(bytes( urls[2] + "aaaa"), versions[2].getHashes());
    } else {
      assertEqualBytes(bytes("ccc"), versions[0].getHashes());
      assertEqualBytes(bytes("bb"), versions[1].getHashes());
      assertEqualBytes(bytes("aaaa"), versions[2].getHashes());
    }
  }
  
  public void testOneContentThreeVersions() throws Exception {
    testOneContentThreeVersions(1, false);
    testOneContentThreeVersions(3, false);
    testOneContentThreeVersions(100, false);
  }

  public void testOneContentThreeVersionsIncludeUrl() throws Exception {
    testOneContentThreeVersions(1, true);
    testOneContentThreeVersions(3, true);
    testOneContentThreeVersions(100, true);
  }

  public void testOneContentThreeVersionsLocalHashGood(int stepSize)
      throws Exception {
    enableLocalHash("SHA-1", "SHA-1");
    CaptureBlocksEventHandler blockHandler =
      new CaptureBlocksEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    
    // Adding versions, from least recent to most recent.
    addVersionAndChecksum(mau, urls[2], "aaaa",
			  "SHA-1:70c881d4a26984ddce795f6f71817c9cf4480e79");
    addVersionAndChecksum(mau, urls[2], "bb",
			  "SHA-1:9a900f538965a426994e1e90600920aff0b4e8d2");
    addVersionAndChecksum(mau, urls[2], "ccc",
			  "SHA-1:f36b4825e5db2cf7dd2d2593b3f5c24c0311d8b2");

    if (log.isDebug3()) {
      for (CachedUrl cu : cus.getCuIterable()) {
	CachedUrl[] vers = cu.getCuVersions();
	log.debug3(cu.getUrl() + " has " + vers.length + " versions.");
	for (int i = 0; i < vers.length; i++) {
	  Properties vProps = vers[i].getProperties();
	  log.debug3("Version: " + i + " has " + vProps.size() +
		     " entries");
	  for (Iterator it2 = vProps.keySet().iterator(); it2.hasNext(); ) {
	    String key = (String) it2.next();
	    log.debug("Version: " + i + " key: " + key + " val: " +
		      vProps.get(key));
	  }
	}
      }
    }
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, blockHandler);
    hasher.setFiltered(false);
    // 9 bytes total for all three versions.
    assertEquals(9, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List blocks = blockHandler.getBlocks();
    assertEquals(1, blocks.size());
    HashBlock b = (HashBlock)blocks.get(0);
    assertEquals(3, b.size());
    
    HashBlock.Version[] versions = b.getVersions();

    assertEqualBytes(bytes("ccc"), versions[0].getHashes());
    assertEqualBytes(bytes("bb"), versions[1].getHashes());
    assertEqualBytes(bytes("aaaa"), versions[2].getHashes());
    LocalHashResult lhr = hasher.getLocalHashResult();
    assertEquals(3, lhr.getMatchingVersions());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlyHashedVersions());
    assertEquals(1, lhr.getMatchingUrls());
    assertEquals(0, lhr.getNewlySuspectUrls());
    assertEquals(0, lhr.getNewlyHashedUrls());
  }

  public void testOneContentThreeVersionsLocalHashGood() throws Exception {
    testOneContentThreeVersionsLocalHashGood(1);
    testOneContentThreeVersionsLocalHashGood(3);
    testOneContentThreeVersionsLocalHashGood(100);
  }


  
  public void testUnfiltered(String exp, boolean isFiltered)
      throws Exception {
    String str = "Wicked witch of the west";

    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    mau.setHashFilterFactory(new SimpleFilterFactory());
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    addContent(mau, urls[4], str);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    if (!isFiltered) {
      hasher.setFiltered(false);
    }
    assertEquals(exp.length(), hashToEnd(hasher, 100));
    assertTrue(hasher.finished());
    List<Event> events = handRec.getEvents();
    assertEquals(1, events.size());
    assertEvent(urls[4], str.length(), exp.length(),
		new byte[][] {bytes(exp)}, events.get(0));
  }
  
  public void testUnfiltered() throws Exception {
    testUnfiltered("Wicked witch of the west", false);
    testUnfiltered("icked itch of the est", true);
  }

  public void testInputStreamIsClosed() throws IOException {
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    MockCachedUrl cu = (MockCachedUrl)mau.makeCachedUrl(urls[4]);
    MockInputStream is = new MockInputStream();
    is.setContent("asdf");
    cu.setInputStream(is);
    cu.setExists(true);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    CachedUrlSetHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    assertEquals(4, hashToEnd(hasher, 100));
    assertTrue(hasher.finished());
    assertTrue(is.isClosed());
  }

  public void testInputStreamIsClosedAfterAbort() throws IOException {
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    MockCachedUrl cu = (MockCachedUrl)mau.makeCachedUrl(urls[4]);
    MockInputStream is = new MockInputStream();
    is.setContent("asdf");
    cu.setInputStream(is);
    cu.setExists(true);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    CachedUrlSetHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.hashStep(2);
    assertFalse(hasher.finished());
    assertFalse(is.isClosed());
    hasher.abortHash();
    assertTrue(is.isClosed());
  }

  String s1 = "foo";
  String s2 = "bar";
  String s3 =
    "Now is the time for all good men to come to the aid of their party";
  String s4 = "The quick brown fox";
  String s5 = rpt("a very long string", 2000);

  String rpt(String s, int cnt) {
    StringBuffer sb = new StringBuffer(s.length() * cnt);
    for (int ix = 0; ix < cnt; ix++) {
      sb.append(s);
    }
    return sb.toString();
  }

  public void testSeveralContent(int stepSize, boolean includeUrl,
				 boolean ignoreFilesOutsideCrawlSpec)
      throws Exception {
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    addContent(mau, urls[4], s1);
    addContent(mau, urls[6], s2);
    addContent(mau, urls[7], s3);
    addContent(mau, urls[8], s4);
    addContent(mau, urls[9], s5);
    addContent(mau, urls[13], s5);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setIncludeUrl(includeUrl);
    int len =
      s1.length() + s2.length() + s3.length() + s4.length() + s5.length();
    if (!ignoreFilesOutsideCrawlSpec) {
      len += s5.length();
    }
    if (includeUrl) {
      len += urls[4].length() + urls[6].length() + urls[7].length() +
	urls[8].length() + urls[9].length();
      if (!ignoreFilesOutsideCrawlSpec) {
	len += urls[13].length();
      }
    }
    assertEquals(len, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List events = handRec.getEvents();
    assertEquals(ignoreFilesOutsideCrawlSpec ? 5 : 6,
		 events.size());
    assertEvent(urls[4], s1, events.get(0), includeUrl);
    assertEvent(urls[6], s2, events.get(1), includeUrl);
    assertEvent(urls[7], s3, events.get(2), includeUrl);
    assertEvent(urls[8], s4, events.get(3), includeUrl);
    assertEvent(urls[9], s5, events.get(4), includeUrl);
    if (!ignoreFilesOutsideCrawlSpec) {
      assertEvent(urls[13], s5, events.get(5), includeUrl);
    }
  }
  
  public void testSeveralContentWithThrowing(int stepSize) throws Exception {
//     ConfigurationUtil.addFromArgs(BlockHasher.PARAM_ENABLE_LOCAL_HASH, "false");

    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    addContent(mau, urls[4], s1);
    addContent(mau, urls[6], s2);
    addContent(mau, urls[7], s3);
    addContent(mau, urls[8], s4);
    addContent(mau, urls[9], s5);
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    MyBlockHasher hasher =
      new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setExcludeSuspectVersions(true);
    hasher.throwOnOpen(urls[6]);

    hashToEnd(hasher, stepSize);
    assertTrue(hasher.finished());
    List events = handRec.getEvents();
    assertEquals(5, events.size());
    assertEvent(urls[4], s1, events.get(0));
    assertEventWithError(urls[6], events.get(1));
    assertEvent(urls[7], s3, events.get(2));
    assertEvent(urls[8], s4, events.get(3));
    assertEvent(urls[9], s5, events.get(4));
  }
  
  public void testSeveralContentNoIgnore() throws Exception {
    testSeveralContent(1, false, false);
    testSeveralContent(3, false, false);
    testSeveralContent(10000, false, false);
  }
  
  public void testSeveralContentIgnore() throws Exception {
    ConfigurationUtil.addFromArgs(BlockHasher.PARAM_IGNORE_FILES_OUTSIDE_CRAWL_SPEC, "true");
    testSeveralContent(1, false, true);
    testSeveralContent(3, false, true);
    testSeveralContent(10000, false, true);
  }
  
  public void testSeveralContentIncludeUrl() throws Exception {
    testSeveralContent(1, true, false);
    testSeveralContent(3, true, false);
    testSeveralContent(10000, true, false);
  }
  
  public void testSeveralContentWithThrowing() throws Exception {
    testSeveralContentWithThrowing(1);
    testSeveralContentWithThrowing(3);
    testSeveralContentWithThrowing(10000);
  }
  
  public void testSeveralContentSeveralVersions(int stepSize) throws Exception {
    CaptureBlocksEventHandler handler = new CaptureBlocksEventHandler();
    
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    
    // Added from least recent to most recent...
    String url4v1 = "This URL has only one version.";
    
    String url5v1 = "This is some kind of content for url5, version 1";
    String url5v2 = "This is some kind of content for url5, version 2";
    String url5v3 = "This is some kind of content for url5, version 3";
    String url5v4 = "This is some kind of content for url5, version 4";
    
    String url6v1 = "And here's some more content for version 1 of url6";
    String url6v2 = "And here's some content for version 2 of url5";
    
    String url7v1 = "Let's not forget some content for version 1 of url7";
    String url7v2 = "This version was much shorter!";
    String url7v3 = "This was version 3 of url6.  It was a good version.";
    
    addVersion(mau, urls[4], url4v1);

    addVersion(mau, urls[5], url5v1);
    addVersion(mau, urls[5], url5v2);
    addVersion(mau, urls[5], url5v3);
    addVersion(mau, urls[5], url5v4);

    addVersion(mau, urls[6], url6v1);
    addVersion(mau, urls[6], url6v2);

    addVersion(mau, urls[7], url7v1);
    addVersion(mau, urls[7], url7v2);
    addVersion(mau, urls[7], url7v3);
    
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handler);

    int len = url4v1.length() + url5v1.length() + url5v2.length() + 
              url5v3.length() + url5v3.length() + url6v1.length() +
              url6v2.length() + url7v1.length() + url7v2.length() +
              url7v3.length();
              
    assertEquals(len, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    
    List blocks = handler.getBlocks();
    assertEquals(4, blocks.size());
    
    HashBlock block0 = (HashBlock)blocks.get(0);
    assertEquals(1, block0.size());
    assertEqualBytes(bytes(url4v1), block0.getVersions()[0].getHashes());
    
    HashBlock block1 = (HashBlock)blocks.get(1);
    assertEquals(4, block1.size());
    assertEqualBytes(bytes(url5v4), block1.getVersions()[0].getHashes());
    assertEqualBytes(bytes(url5v3), block1.getVersions()[1].getHashes());
    assertEqualBytes(bytes(url5v2), block1.getVersions()[2].getHashes());
    assertEqualBytes(bytes(url5v1), block1.getVersions()[3].getHashes());
  
    HashBlock block2 = (HashBlock)blocks.get(2);
    assertEquals(2, block2.size());
    assertEqualBytes(bytes(url6v2), block2.getVersions()[0].getHashes());
    assertEqualBytes(bytes(url6v1), block2.getVersions()[1].getHashes());

    HashBlock block3 = (HashBlock)blocks.get(3);
    assertEquals(3, block3.size());
    assertEqualBytes(bytes(url7v3), block3.getVersions()[0].getHashes());
    assertEqualBytes(bytes(url7v2), block3.getVersions()[1].getHashes());
    assertEqualBytes(bytes(url7v1), block3.getVersions()[2].getHashes());
  }
  
  public void testSeveralContentSeveralVersionsWithThrowing(int stepSize)
      throws Exception {
    CaptureBlocksEventHandler handler = new CaptureBlocksEventHandler();
    
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    
    // Added from least recent to most recent...
    String url4v1 = "This URL has only one version.";
    
    String url5v1 = "This is some kind of content for url5, version 1";
    String url5v2 = "This is some kind of content for url5, version 2";
    String url5v3 = "This is some kind of content for url5, version 3";
    String url5v4 = "This is some kind of content for url5, version 4";
    
    String url6v1 = "And here's some more content for version 1 of url6";
    String url6v2 = "And here's some content for version 2 of url5";
    
    String url7v1 = "Let's not forget some content for version 1 of url7";
    String url7v2 = "This version was much shorter!";
    String url7v3 = "This was version 3 of url6.  It was a good version.";
    
    addVersion(mau, urls[4], url4v1);

    addVersion(mau, urls[5], url5v1);
    CachedUrl errcu1 = addVersion(mau, urls[5], url5v2);
    addVersion(mau, urls[5], url5v3);
    addVersion(mau, urls[5], url5v4);

    addVersion(mau, urls[6], url6v1);
    addVersion(mau, urls[6], url6v2);

    addVersion(mau, urls[7], url7v1);
    CachedUrl errcu2 = addVersion(mau, urls[7], url7v2);
    addVersion(mau, urls[7], url7v3);
    
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    MyBlockHasher hasher =
      new MyBlockHasher(cus, digs, inits, handler);
    
    hasher.throwOnRead(errcu1);
    hasher.throwOnOpen(errcu2);

    hashToEnd(hasher, stepSize);
    assertTrue(hasher.finished());
    
    List blocks = handler.getBlocks();
    assertEquals(4, blocks.size());

    
    HashBlock block0 = (HashBlock)blocks.get(0);
    assertEquals(1, block0.size());
    for (HashBlock.Version v : block0.getVersions()) {
      assertNull(v.getHashError());
    }

    HashBlock block1 = (HashBlock)blocks.get(1);
    assertEquals(4, block1.size());
    int ix = 0;
    for (HashBlock.Version v : block1.getVersions()) {
      if (++ix == 3) {
	assertNotNull(v.getHashError());
      } else {
	assertNull(v.getHashError());
      }
    }
  
    HashBlock block2 = (HashBlock)blocks.get(2);
    assertEquals(2, block2.size());
    for (HashBlock.Version v : block2.getVersions()) {
      assertNull(v.getHashError());
    }

    HashBlock block3 = (HashBlock)blocks.get(3);
    assertEquals(3, block3.size());
    ix = 0;
    for (HashBlock.Version v : block3.getVersions()) {
      if (++ix == 2) {
	assertNotNull(v.getHashError());
      } else {
	assertNull(v.getHashError());
      }
    }
  }

  public void testSeveralContentSeveralVersions() throws Exception {
    testSeveralContentSeveralVersions(1);
    testSeveralContentSeveralVersions(3);
    testSeveralContentSeveralVersions(100);
    testSeveralContentSeveralVersions(10000);
  }

  public void testSeveralContentSeveralVersionsWithThrowing() throws Exception {
    testSeveralContentSeveralVersionsWithThrowing(1);
    testSeveralContentSeveralVersionsWithThrowing(3);
    testSeveralContentSeveralVersionsWithThrowing(100);
    testSeveralContentSeveralVersionsWithThrowing(10000);
  }

  public void testInitBytes(int stepSize) throws Exception {
    String chal = "challenge";
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    addContent(mau, urls[4], s1);
    addContent(mau, urls[6], s2);
    addContent(mau, urls[7], s3);
    MessageDigest[] digs = { dig };
    byte[][] inits = { bytes(chal) };
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setIncludeUrl(true);
    int len = s1.length() + s2.length() + s3.length() +
      urls[4].length() + urls[6].length() + urls[7].length();
    assertEquals(len, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List events = handRec.getEvents();
    assertEquals(3, events.size());
    assertEvent(urls[4], s1.length(), chal + urls[4] + s1, events.get(0),
		false);
    assertEvent(urls[6], s2.length(), chal + urls[6] + s2, events.get(1),
		false);
    assertEvent(urls[7], s3.length(), chal + urls[7] + s3, events.get(2),
		false);
  }

  public void testInitBytes1() throws Exception {
    testInitBytes(1);
  }

  public void testInitBytes1000() throws Exception {
    testInitBytes(1000);
  }

  public void testMultipleDigests(int stepSize) throws Exception {
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    addContent(mau, urls[4], s1);
    addContent(mau, urls[6], s2);
    addContent(mau, urls[7], s3);
    MessageDigest dig2 = new MockMessageDigest();
    MessageDigest[] digs = { dig, dig2 };
    byte[][] inits = {null, null};
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setIncludeUrl(true);
    int len = s1.length() + s2.length() + s3.length() +
      urls[4].length() + urls[6].length() + urls[7].length();
    assertEquals(len * digs.length, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List events = handRec.getEvents();
    assertEquals(3, events.size());
    assertEvent(urls[4], s1.length(),
		ListUtil.list(bytes(urls[4] + s1), bytes(urls[4] + s1)),
		events.get(0));
    assertEvent(urls[6], s2.length(),
		ListUtil.list(bytes(urls[6] + s2), bytes(urls[6] + s2)),
		events.get(1));
    assertEvent(urls[7], s3.length(), 
		ListUtil.list(bytes(urls[7] + s3), bytes(urls[7] + s3)),
		events.get(2));
  }

  public void testMultipleDigests() throws Exception {
    testMultipleDigests(10000);
  }

  public void testMultipleDigestsWithInit(int stepSize) throws Exception {
    String chal2 = "2challenge";
    String chal3 = "3challenge";
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    addContent(mau, urls[4], s1);
    addContent(mau, urls[6], s2);
    addContent(mau, urls[7], s3);
    MessageDigest dig2 = new MockMessageDigest();
    MessageDigest dig3 = new MockMessageDigest();
    MessageDigest[] digs = { dig, dig2, dig3 };
    byte[][] inits = { null, bytes(chal2), bytes(chal3) };
    BlockHasher hasher = new MyBlockHasher(cus, digs, inits, handRec);
    hasher.setIncludeUrl(true);
    int len = s1.length() + s2.length() + s3.length() +
      urls[4].length() + urls[6].length() + urls[7].length();
    assertEquals(len * digs.length, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List events = handRec.getEvents();
    assertEquals(3, events.size());
    assertEvent(urls[4], s1.length(),
		ListUtil.list(bytes(urls[4] + s1),
			      bytes(chal2 + urls[4] + s1),
			      bytes(chal3 + urls[4] + s1)),
		events.get(0));
    assertEvent(urls[6], s2.length(),
		ListUtil.list(bytes(urls[6] + s2),
			      bytes(chal2 + urls[6] + s2),
			      bytes(chal3 + urls[6] + s2)),
		events.get(1));
    assertEvent(urls[7], s3.length(),
		ListUtil.list(bytes(urls[7] + s3),
			      bytes(chal2 + urls[7] + s3),
			      bytes(chal3 + urls[7] + s3)),
		events.get(2));
  }

  public void testMultipleDigestsWithInit() throws Exception {
    testMultipleDigestsWithInit(1);
    testMultipleDigestsWithInit(1000);
  }

//  static byte[] bytes = ByteArray.makeRandomBytes(40);
//  static final int multiple = 100000000;
//
//  public void testNewDigestTime() throws Exception {
//    int len = bytes.length;
//    for (int i = 0; i < multiple; i++) {
//      MessageDigest.getInstance("SHA-1").update(bytes, 0, len);
//    }
//  }
//
//  public void testCloneDigestTime() throws Exception {
//    int len = bytes.length;
//    MessageDigest digest = MessageDigest.getInstance("SHA-1");
//    digest.update(bytes, 0, len);
//    for (int i = 0; i < multiple; i++) {
//      digest.clone();
//    }
//  }

  class Event {
    HashBlock hblock;
    byte[][] byteArrays;
    Event(HashBlock hblock, byte[][] byteArrays) {
      this.hblock = hblock;
      this.byteArrays = byteArrays;
    }

    public String toString() {
      return "[Ev: hb: " + hblock + "]";
    }
  }

  class CaptureBlocksEventHandler implements BlockHasher.EventHandler {
    List blocks = new ArrayList();
    
    public void blockDone(HashBlock hblock) {
      blocks.add(hblock);
    }

    public List getBlocks() {
      return blocks;
    }
  }
  
  class RecordingEventHandler implements BlockHasher.EventHandler {
    List<Event> events = new ArrayList();

    public void blockDone(HashBlock hblock) {
      HashBlock.Version hbv = hblock.currentVersion();
      if (hbv != null) {
        events.add(new Event(hblock, hbv.getHashes()));
      } else {
        log.warning("null HashBlock.Version");
      }
    }
 
    public void reset() {
      events = new ArrayList();
    }

    public List<Event> getEvents() {
      return events;
    }
  }
  
  class MyBlockHasher extends BlockHasher {
    Map throwOnOpen = new HashMap();
    Map throwOnRead = new HashMap();

    public MyBlockHasher(CachedUrlSet cus, MessageDigest[] digests,
			 byte[][]initByteArrays, EventHandler cb) {
      super(cus, digests, initByteArrays, cb);
 
    }
    
    public MyBlockHasher(CachedUrlSet cus, int maxVersions,
			 MessageDigest[] digests,
			 byte[][]initByteArrays, EventHandler cb) {
      super(cus, maxVersions, digests, initByteArrays, cb);
    }
    
    public void throwOnOpen(CachedUrl cu) {
      throwOnOpen.put(cu.getUrl(), cu.getVersion());
    }

    public void throwOnOpen(String url) {
      throwOnOpen.put(url, -1);
    }

    public void throwOnRead(CachedUrl cu) {
      throwOnRead.put(cu.getUrl(), cu.getVersion());
    }

    public void throwOnRead(String url) {
      throwOnRead.put(url, -1);
    }

    @Override
    protected InputStream getInputStream(CachedUrl cu) {
      Integer over = (Integer)throwOnOpen.get(cu.getUrl());
      if (over != null && (over == -1 || over == cu.getVersion())) {
	throw new ExpectedRuntimeException("Opening hash input stream");
      }
      Integer rver = (Integer)throwOnRead.get(cu.getUrl());
      return new ThrowingInputStream(super.getInputStream(cu),
				     (rver != null &&
				      (rver == -1 || rver == cu.getVersion()))
				     ? new IOException("Reading from hash input stream") : null,
				     null);
    }

  }

  public class SimpleFilterFactory implements FilterFactory {
    public InputStream createFilteredInputStream(ArchivalUnit au,
						 InputStream in,
						 String encoding) {
      Reader rdr = FilterUtil.getReader(in, encoding);
      StringFilter filt = new StringFilter(rdr, "w");
      filt.setIgnoreCase(true);
      return new ReaderInputStream(filt);
    }
  }

  public class IncompleteReadFilterFactory implements FilterFactory {
    int len;

    IncompleteReadFilterFactory(int len) {
      this.len = len;
    }

    public InputStream createFilteredInputStream(ArchivalUnit au,
						 InputStream in,
						 String encoding) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try {
	StreamUtil.copy(in, baos, len);
      } catch (IOException e) {
	throw new RuntimeException(e);
      }
      return new ByteArrayInputStream(baos.toByteArray());
    }
  }

  public class MarkResetFilterFactory implements FilterFactory {
    int mark;
    int resetAt;

    MarkResetFilterFactory(int mark, int resetAt) {
      this.mark = mark;
      this.resetAt = resetAt;
    }

    public InputStream createFilteredInputStream(ArchivalUnit au,
						 InputStream in,
						 String encoding) {
      in.mark(mark);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try {
	StreamUtil.copy(in, baos, resetAt);
	in.reset();
	StreamUtil.copy(in, baos);
      } catch (IOException e) {
	throw new RuntimeException(e);
      }
      return new ByteArrayInputStream(baos.toByteArray());
    }
  }
}
