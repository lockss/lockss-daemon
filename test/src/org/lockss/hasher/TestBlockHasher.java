/*
 * $Id: TestBlockHasher.java,v 1.4 2006-03-01 02:50:13 smorabito Exp $
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
    assertEquals(expectedLength, hblock.getUnfilteredLength());
    for (int ix = 0; ix < event.byteArrays.length; ix++) {
      assertEquals(expectedHashed[ix], event.byteArrays[ix]);
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

  public void testOneContent() throws Exception {
    testOneContent(1);
    testOneContent(3);
    testOneContent(100);
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
  String s5 = rpt("a long string", 1000);

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
    assertEquals(len, hashToEnd(hasher, stepSize));
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
    assertEquals(len, hashToEnd(hasher, stepSize));
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

  class RecordingEventHandler implements BlockHasher.EventHandler {
    List events = new ArrayList();

    public void blockDone(HashBlock hblock) {
      events.add(new Event(hblock, hblock.getHashes()));
    }

    public void reset() {
      events = new ArrayList();
    }

    public List getEvents() {
      return events;
    }
  }

}
