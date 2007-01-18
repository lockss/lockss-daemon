/*
 * $Id: TestBlockHasher.java,v 1.7 2007-01-18 02:26:49 tlipkis Exp $
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

package org.lockss.hasher;

import java.util.*;
import java.io.*;
import java.math.*;
import java.security.*;
import junit.framework.TestCase;
import org.lockss.test.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

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
  };

  MockMessageDigest dig = null;

  public void setUp() throws Exception {
    super.setUp();
    dig = new MockMessageDigest(); 
  }

  MockArchivalUnit setupContentTree() {
    return setupContentTree(null);
  }

  MockArchivalUnit setupContentTree(MockArchivalUnit mau) {
    if (mau == null) {
      mau = new MockArchivalUnit();
    }
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    List files = new ArrayList();
    for (int ix = 0; ix < urls.length; ix++) {
      CachedUrl cu = mau.addUrl(urls[ix], false, true);
      files.add(cu);
    }
    cus.setHashItSource(files);
    return mau;
  }

  void addContent(MockArchivalUnit mau, String url, String content) {
    MockCachedUrl cu = (MockCachedUrl)mau.makeCachedUrl(url);
    cu.setContent(content);
  }
  
  void addVersion(MockArchivalUnit mau, String url, String content) {
    MockCachedUrl cu = (MockCachedUrl)mau.makeCachedUrl(url);
    cu.addVersion(content);
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
    assertEvent(expectedUrl, expectedString.length(),
		ListUtil.list(bytes(expectedString)), eventObj);
  }

  void assertEvent(String expectedUrl, long expectedLength, List hashed,
		   Object eventObj) {
    assertEvent(expectedUrl, expectedLength,
		(byte[][])hashed.toArray(EMPTY_BYTE_ARRAY_ARRAY),
		eventObj);
  }

  void assertEvent(String expectedUrl, long expectedLength,
		   byte[][]expectedHashed, Object eventObj) {
    Event event = (Event)eventObj;
    HashBlock hblock = event.hblock;
    assertEquals(expectedUrl, hblock.getUrl());
    HashBlock.Version curver = hblock.currentVersion();
    assertEquals(expectedLength, curver.getUnfilteredLength());
    assertEquals(expectedLength, curver.getFilteredLength());
    for (int ix = 0; ix < event.byteArrays.length; ix++) {
      assertEquals(expectedHashed[ix], event.byteArrays[ix]);
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
    MockCachedUrlSet cus = new MockCachedUrlSet();
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
    MockCachedUrlSet cus = new MockCachedUrlSet();
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    cus.setHashItSource(Collections.EMPTY_LIST);
    CachedUrlSetHasher hasher = new BlockHasher(cus, digs, inits, handRec);
    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());
    assertEmpty(handRec.getEvents());
  }

  public void testSetConfig() throws Exception {
    MockCachedUrlSet cus = new MockCachedUrlSet();
    cus.setHashIterator(CollectionUtil.EMPTY_ITERATOR);
    cus.setFlatIterator(null);
    cus.setEstimatedHashDuration(54321);
    MessageDigest[] digs = { dig, dig };
    byte[][] inits = {null, null};
    
    // First hasher should have default hashUpTo
    BlockHasher hasher = new BlockHasher(cus, digs, inits, hand0);
    assertEquals(hasher.getMaxVersions(),
                 BlockHasher.DEFAULT_HASH_MAX_VERSIONS);
    
    ConfigurationUtil.setFromArgs(BlockHasher.PARAM_HASH_MAX_VERSIONS, "99");
    
    BlockHasher hasher2 = new BlockHasher(cus, digs, inits, hand0);
    assertEquals(hasher2.getMaxVersions(), 99);
    
    ConfigurationUtil.setFromArgs(BlockHasher.PARAM_HASH_MAX_VERSIONS, "18");

    BlockHasher hasher3 = new BlockHasher(cus, digs, inits, hand0);
    assertEquals(hasher3.getMaxVersions(), 18);
  }

  public void testAccessors() throws IOException {
    MockCachedUrlSet cus = new MockCachedUrlSet();
    cus.setHashIterator(CollectionUtil.EMPTY_ITERATOR);
    cus.setFlatIterator(null);
    cus.setEstimatedHashDuration(54321);
    MessageDigest[] digs = { dig, dig };
    byte[][] inits = {null, null};
    CachedUrlSetHasher hasher = new BlockHasher(cus, digs, inits, hand0);
    assertSame(cus, hasher.getCachedUrlSet());
    assertEquals(54321, hasher.getEstimatedHashDuration());
    assertEquals("B(2)", hasher.typeString());
    hasher.storeActualHashDuration(12345, null);
    assertEquals(12345, cus.getActualHashDuration());
    assertEquals(digs, hasher.getDigests());
  }

  public void testNoContent() throws Exception {
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    CachedUrlSetHasher hasher = new BlockHasher(cus, digs, inits, handRec);
    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());
    assertEmpty(handRec.getEvents());
  }

  public void testOneContent(int stepSize) throws Exception {
    RecordingEventHandler handRec = new RecordingEventHandler();
    MockArchivalUnit mau = setupContentTree();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    addContent(mau, urls[4], "foo");
    MessageDigest[] digs = { dig };
    byte[][] inits = {null};
    CachedUrlSetHasher hasher = new BlockHasher(cus, digs, inits, handRec);
    assertEquals(3, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List events = handRec.getEvents();
    assertEquals(1, events.size());
    assertEvent(urls[4], 3, ListUtil.list(bytes("foo")), events.get(0));
  }
  
  /**
   * Ensure that MessageDigests implementations used on the test platform
   * are Cloneable.  This is fairly fragile, since the underlying implementation
   * of the MessageDigests may or may not be the same between VMs,
   * but this seems to be the only way to test for it.
   * 
   * @throws Exception
   */
  public void tesMessageDigestImplementationsAreCloneable() throws Exception {
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
      new BlockHasher(cus, testDigs, inits, blockHandler);
  }
  
  public void testOneContent() throws Exception {
    testOneContent(1);
    testOneContent(3);
    testOneContent(100);
  }
  
  public void testOneContentThreeVersions(int stepSize) throws Exception {
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
    CachedUrlSetHasher hasher = new BlockHasher(cus, digs, inits, blockHandler);
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
  }
  
  public void testOneContentThreeVersions() throws Exception {
    testOneContentThreeVersions(1);
    testOneContentThreeVersions(3);
    testOneContentThreeVersions(100);
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
    CachedUrlSetHasher hasher = new BlockHasher(cus, digs, inits, handRec);
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
    CachedUrlSetHasher hasher = new BlockHasher(cus, digs, inits, handRec);
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

  public void testSeveralContent(int stepSize) throws Exception {
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
    CachedUrlSetHasher hasher = new BlockHasher(cus, digs, inits, handRec);
    int len = s1.length() + s2.length() + s3.length() +
      s4.length() + s5.length();
    assertEquals(len, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List events = handRec.getEvents();
    assertEquals(5, events.size());
    assertEvent(urls[4], s1, events.get(0));
    assertEvent(urls[6], s2, events.get(1));
    assertEvent(urls[7], s3, events.get(2));
    assertEvent(urls[8], s4, events.get(3));
    assertEvent(urls[9], s5, events.get(4));
  }

  public void testSeveralContent() throws Exception {
    testSeveralContent(1);
    testSeveralContent(3);
    testSeveralContent(10000);
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
    CachedUrlSetHasher hasher = new BlockHasher(cus, digs, inits, handler);
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

  public void testSeveralContentSeveralVersions() throws Exception {
    testSeveralContentSeveralVersions(1);
    testSeveralContentSeveralVersions(3);
    testSeveralContentSeveralVersions(100);
    testSeveralContentSeveralVersions(10000);
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
    CachedUrlSetHasher hasher = new BlockHasher(cus, digs, inits, handRec);
    int len = s1.length() + s2.length() + s3.length();
    assertEquals(len, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List events = handRec.getEvents();
    assertEquals(3, events.size());
    assertEvent(urls[4], s1.length(), ListUtil.list(bytes(chal, s1)),
		events.get(0));
    assertEvent(urls[6], s2.length(), ListUtil.list(bytes(chal, s2)),
		events.get(1));
    assertEvent(urls[7], s3.length(), ListUtil.list(bytes(chal, s3)),
		events.get(2));
  }

  public void testInitBytes() throws Exception {
    testInitBytes(1);
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
    CachedUrlSetHasher hasher = new BlockHasher(cus, digs, inits, handRec);
    int len = s1.length() + s2.length() + s3.length();
    assertEquals(len * digs.length, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List events = handRec.getEvents();
    assertEquals(3, events.size());
    assertEvent(urls[4], s1.length(), ListUtil.list(bytes(s1), bytes(s1)),
		events.get(0));
    assertEvent(urls[6], s2.length(), ListUtil.list(bytes(s2), bytes(s2)),
		events.get(1));
    assertEvent(urls[7], s3.length(), ListUtil.list(bytes(s3), bytes(s3)),
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
    CachedUrlSetHasher hasher = new BlockHasher(cus, digs, inits, handRec);
    int len = s1.length() + s2.length() + s3.length();
    assertEquals(len * digs.length, hashToEnd(hasher, stepSize));
    assertTrue(hasher.finished());
    List events = handRec.getEvents();
    assertEquals(3, events.size());
    assertEvent(urls[4], s1.length(),
		ListUtil.list(bytes(s1), bytes(chal2, s1), bytes(chal3, s1)),
		events.get(0));
    assertEvent(urls[6], s2.length(),
		ListUtil.list(bytes(s2), bytes(chal2, s2), bytes(chal3, s2)),
		events.get(1));
    assertEvent(urls[7], s3.length(),
		ListUtil.list(bytes(s3), bytes(chal2, s3), bytes(chal3, s3)),
		events.get(2));
  }

  public void testMultipleDigestsWithInit() throws Exception {
    testMultipleDigestsWithInit(1);
    testMultipleDigestsWithInit(1000);
  }

  class Event {
    HashBlock hblock;
    byte[][] byteArrays;
    Event(HashBlock hblock, byte[][] byteArrays) {
      this.hblock = hblock;
      this.byteArrays = byteArrays;
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
    List events = new ArrayList();

    public void blockDone(HashBlock hblock) {
      events.add(new Event(hblock, hblock.currentVersion().getHashes()));
    }
 
    public void reset() {
      events = new ArrayList();
    }

    public List getEvents() {
      return events;
    }
  }
}
