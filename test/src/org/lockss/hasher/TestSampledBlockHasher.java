/*
 * $Id: TestSampledBlockHasher.java,v 1.1.2.3 2013-02-22 20:07:17 dshr Exp $
 */

/*

Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;

public class TestSampledBlockHasher extends LockssTestCase {
  private static final String TEST_URL_BASE = "http://www.test.com/blah/";
  private static final String TEST_URL = TEST_URL_BASE+"blah.html";
  private static final String TEST_FILE_CONTENT = "This is a test file ";
  private static final String TEST_NONCE = "Test nonce";
  private static final byte[] testNonce = TEST_NONCE.getBytes();
  private static final byte[] testContent = TEST_FILE_CONTENT.getBytes();

  private File tmpDir = null;
  MockMessageDigest dig = null;
  MockArchivalUnit mau = null;
  private int numFilesInSample = 0;

  public TestSampledBlockHasher(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    tmpDir = getTempDir();
    dig = new MockMessageDigest();
    mau = new MockArchivalUnit(new MockPlugin(), TEST_URL_BASE);
    ConfigurationUtil.addFromArgs(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
                                  "SHA-1");
  }

  public void testConstructors() {
    MockCachedUrlSet cus = new MockCachedUrlSet(mau);
    cus.setHashIterator(CollectionUtil.EMPTY_ITERATOR);
    cus.setFlatIterator(null);
    cus.setEstimatedHashDuration(54321);
    MessageDigest[] digs = { dig };
    byte[][] inits = { testContent };
    RecordingEventHandler handRec = new RecordingEventHandler();
    MySampledBlockHasher hasher =
      new MySampledBlockHasher(cus, 1, 1, testNonce, digs, inits, handRec);
    assertEquals(cus, hasher.getCUS());
    assertEquals(dig, hasher.getInitialMessageDigest(0));
    assertEquals(testNonce, hasher.getPollerNonce());
    assertEquals(1, hasher.getMod());
    assertEquals("SHA-1", hasher.getAlgorithm());
    MessageDigest dig2 = new MockMessageDigest();
    hasher =
      new MySampledBlockHasher(cus, 1, 1, testNonce, digs, inits, handRec, dig2);
    assertEquals(cus, hasher.getCUS());
    assertEquals(dig, hasher.getInitialMessageDigest(0));
    assertEquals(testNonce, hasher.getPollerNonce());
    assertEquals(1, hasher.getMod());
    assertEquals(dig2, hasher.getSampleHasher());
    assertEquals("Mock hash algorithm", hasher.getAlgorithm());
  }

  public void testBadSampleHasherAlgorithm() {
    MockCachedUrlSet cus = new MockCachedUrlSet(mau);
    cus.setHashIterator(CollectionUtil.EMPTY_ITERATOR);
    cus.setFlatIterator(null);
    cus.setEstimatedHashDuration(54321);
    ConfigurationUtil.addFromArgs(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
                                  "BOGUS_HASH");
    MessageDigest[] digs = { dig };
    byte[][] inits = { testContent };
    RecordingEventHandler handRec = new RecordingEventHandler();
    try {
      CachedUrlSetHasher hasher =
	new SampledBlockHasher(cus, 1, 1, testNonce, digs, inits, handRec);
      fail("Creating a SampledBlockHasher with a bad hash algorithm "+
	   "should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testNullSampleHasher() {
    MockCachedUrlSet cus = new MockCachedUrlSet(mau);
    cus.setHashIterator(CollectionUtil.EMPTY_ITERATOR);
    cus.setFlatIterator(null);
    cus.setEstimatedHashDuration(54321);
    MessageDigest[] digs = { dig };
    byte[][] inits = { testContent };
    RecordingEventHandler handRec = new RecordingEventHandler();
    try {
      CachedUrlSetHasher hasher =
	new SampledBlockHasher(cus, 1, 1, testNonce, digs, inits, handRec,
			       null);
      fail("Creating a SampledBlockHasher with a null sample hasher "+
	   "should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testNullPollerNonce() {
    MockCachedUrlSet cus = new MockCachedUrlSet(mau);
    cus.setHashIterator(CollectionUtil.EMPTY_ITERATOR);
    cus.setFlatIterator(null);
    cus.setEstimatedHashDuration(54321);
    MessageDigest[] digs = { dig };
    byte[][] inits = { testContent };
    RecordingEventHandler handRec = new RecordingEventHandler();
    try {
      CachedUrlSetHasher hasher =
	new SampledBlockHasher(cus, 1, 1, null, digs, inits, handRec, dig);
      fail("Creating a SampledBlockHasher with a null poller nonce "+
	   "should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testZeroMod() {
    MockCachedUrlSet cus = new MockCachedUrlSet(mau);
    cus.setHashIterator(CollectionUtil.EMPTY_ITERATOR);
    cus.setFlatIterator(null);
    cus.setEstimatedHashDuration(54321);
    MessageDigest[] digs = { dig };
    byte[][] inits = { testContent };
    RecordingEventHandler handRec = new RecordingEventHandler();
    try {
      CachedUrlSetHasher hasher =
	new SampledBlockHasher(cus, 1, 0, testNonce, digs, inits, handRec, dig);
      fail("Creating a SampledBlockHasher with a zero mod "+
	   "should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  private void doTestMultipleFilesWithMod(int numFiles, int mod)
   throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(numFiles);
    numFilesInSample = 0;
    byte[] expectedBytes = getExpectedCusBytes(cus, mod);
    log.debug3("Expect " + expectedBytes.length + " bytes from " +
	       numFiles + " files mod " + mod);
    MockMessageDigest dig2 = new MockMessageDigest();
    MessageDigest[] digs = { dig };
    byte[][] inits = { { } };
    if (log.isDebug3()) {
      for (int i = 0; i < testContent.length; i++) {
	log.debug3("doTest: " + i + " : " + testContent[i]);
      }
    }
    RecordingEventHandler handRec = new RecordingEventHandler();
    MySampledBlockHasher hasher =
      new MySampledBlockHasher(cus, 1, mod, testNonce, digs, inits, handRec,
			     dig2);
    hashToLength(hasher, expectedBytes.length, expectedBytes.length);
    List<Event> events = handRec.getEvents();
    assertEquals(numFilesInSample, events.size());
    int ex = 0;
    int eventCount = 0;
    for (Event ev : events) {
      for (int i = 0; i < ev.byteArrays.length; i++) {
	for (int j = 0; j < ev.byteArrays[i].length; j++) {
	  if (ev.byteArrays[i][j] != expectedBytes[ex++]) {
	    fail("ev[" + eventCount + "].byteArrays[" + i + "][" + j +
		 "} mismatch expectedBytes[" + ex + "]");
	  }
	}
      }
      eventCount++;
    }
  }

  public void testHashMultipleFilesMod1()
      throws IOException, FileNotFoundException {
    doTestMultipleFilesWithMod(10, 1);
  }

  public void testHashMultipleFilesMod2()
      throws IOException, FileNotFoundException {
    doTestMultipleFilesWithMod(10, 2);
  }

  public void testHashSixtyFilesMod1()
      throws IOException, FileNotFoundException {
    doTestMultipleFilesWithMod(60, 1);
  }

  public void testHashSixtyFilesMod5()
      throws IOException, FileNotFoundException {
    doTestMultipleFilesWithMod(60, 5);
  }

  // XXX DSHR - need tests with substance checking enabled

  private MockArchivalUnit newMockArchivalUnit(String url) {
    MockArchivalUnit mau = new MockArchivalUnit(new MockPlugin(), url);
    MockCachedUrlSet cus = new MockCachedUrlSet(url);
    cus.setArchivalUnit(mau);
    return mau;
  }
  private MockCachedUrlSet makeFakeCachedUrlSet(int numFiles)
      throws IOException, FileNotFoundException {
    Vector files = new Vector(numFiles+1);

    MockArchivalUnit mau = newMockArchivalUnit(TEST_URL_BASE);
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(TEST_URL_BASE, true, true);
    mau.addContent(TEST_URL_BASE,  TEST_FILE_CONTENT+" base");

    for (int ix=0; ix < numFiles; ix++) {
      String url = TEST_URL+ix;
      String content = TEST_FILE_CONTENT+ix;
      MockCachedUrl cu = new MockCachedUrl(url);

      log.debug3(url + " has " + content.length() + " bytes");
      if (log.isDebug3()) {
	byte[] foo = content.getBytes();
	for (int i = 0; i < foo.length; i++) {
	  log.debug3("makeFake: " + i + " : " + foo[i]);
	}
      }
      cu.setContent(content);
      cu.setExists(true);
      files.add(cu);
    }
    cus.setHashItSource(files);
    return cus;
  }

  private byte[] getExpectedCusBytes(CachedUrlSet cus, int mod)
    throws IOException {
    Iterator it = cus.contentHashIterator();
    List byteArrays = new LinkedList();
    int totalSize = 0;

    while (it.hasNext()) {
      CachedUrl cu = cachedUrlSetNodeToCachedUrl((CachedUrlSetNode) it.next());
      String urlName = cu.getUrl();
      byte[] hash = (testNonce + urlName).getBytes();
      if (mod > 0 && cu.hasContent() &&
	  ((((int)hash[hash.length-1] + 128) % mod) == 0)) {
	log.debug3(urlName + " is in sample");
	numFilesInSample++;
	byte[] arr = getExpectedCuBytes(cu);
	log.debug3(urlName + " contains " + arr.length + " bytes");
	totalSize += arr.length;
	byteArrays.add(arr);
      } else {
	log.debug3(urlName + " isn't in sample");
      }
    }
    byte[] returnArr = new byte[totalSize];
    int pos = 0;
    it = byteArrays.iterator();
    while (it.hasNext()) {
      byte[] curArr = (byte[]) it.next();
      for (int ix=0; ix<curArr.length; ix++) {
	returnArr[pos++] = curArr[ix];
      }
    }
    return returnArr;
  }

  private CachedUrl cachedUrlSetNodeToCachedUrl(CachedUrlSetNode cusn)
      throws IOException {
    switch (cusn.getType()) {
      case CachedUrlSetNode.TYPE_CACHED_URL_SET:
	CachedUrlSet cus = (CachedUrlSet)cusn;
	return cus.getArchivalUnit().makeCachedUrl(cus.getUrl());
      case CachedUrlSetNode.TYPE_CACHED_URL:
	return (CachedUrl)cusn;
    }
    return null;
  }

  private byte[] getExpectedCuBytes(CachedUrl cu) throws IOException {
    String name = cu.getUrl();
    InputStream contentStream = cu.openForHashing();
    StringBuffer sb = new StringBuffer();
    //sb.append(name);
    int curKar;
    int contentSize = 0;
    while ((curKar = contentStream.read()) != -1) {
      sb.append((char)curKar);
      contentSize++;
    }
    //    byte[] sizeArray =
    //  (new BigInteger(Integer.toString(contentSize)).toByteArray());

    byte[] returnArr = new byte[/*sizeArray.length+*/sb.length()/*+1*/];
    int curPos = 0;
    byte[] nameBytes = sb.toString().getBytes();
    for (int ix=0; ix<nameBytes.length; ix++) {
      returnArr[curPos++] = nameBytes[ix];
    }
    log.debug3("nameBytes " + nameBytes.length + " returnArr " +
	       returnArr.length);
//     returnArr[curPos++] = (byte)sizeArray.length;
//     for (int ix=0; ix<sizeArray.length; ix++) {
//       returnArr[curPos++] = sizeArray[ix];
//     }
    return returnArr;
  }

  private void hashToLength(MySampledBlockHasher hasher,
			    int length, int stepSize) throws IOException {
    int numBytesHashed = 0;
    MockMessageDigest mockDig = (MockMessageDigest)hasher.getInitialMessageDigest(0);
    log.debug3(mockDig.toString() + " left " + mockDig.getNumRemainingBytes());
    log.debug3("length: " + length + " step: " + stepSize);
    while (numBytesHashed < length) {
      assertFalse(hasher.finished());
      numBytesHashed += hasher.hashStep(stepSize);
      log.debug3(numBytesHashed + " bytes hashed so far out of " + length);
      mockDig = (MockMessageDigest)hasher.getPeerMessageDigest(0);
      log.debug3(mockDig.toString() + " left " + mockDig.getNumRemainingBytes());
    }
    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());
  }

  private void hashToEnd(SampledBlockHasher hasher, int stepSize)
    throws IOException {
    while (!hasher.finished()) {
      hasher.hashStep(stepSize);
    }
  }

  public class MySampledBlockHasher extends SampledBlockHasher {
    public MySampledBlockHasher(CachedUrlSet cus,
				int maxVersions,
				int modulus,
				byte[] sampleNonce,
				MessageDigest[] digests,
				byte[][] initByteArrays,
				EventHandler cb) {
      super(cus, maxVersions, modulus, sampleNonce, digests, initByteArrays,
	    cb);
    }

    public MySampledBlockHasher(CachedUrlSet cus,
				int maxVersions,
				int modulus,
				byte[] sampleNonce,
				MessageDigest[] digests,
				byte[][] initByteArrays,
				EventHandler cb,
				MessageDigest sampleHasher) {
      super(cus, maxVersions, modulus, sampleNonce, digests, initByteArrays,
	    cb, sampleHasher);
    }

    public CachedUrlSet getCUS() {
      return cus;
    }
    public MessageDigest getInitialMessageDigest(int ix) {
      return initialDigests[ix];
    }
    public MessageDigest getPeerMessageDigest(int ix) {
      return peerDigests[ix];
    }
    public byte[] getPollerNonce() {
      return sampleNonce;
    }
    public int getMod() {
      return modulus;
    }
    public MessageDigest getSampleHasher() {
      return sampleHasher;
    }
    public String getAlgorithm() {
      return alg;
    }
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
    List<Event> events = new ArrayList();

    public void blockDone(HashBlock hblock) {
      events.add(new Event(hblock, hblock.currentVersion().getHashes()));
    }
 
    public void reset() {
      events = new ArrayList();
    }

    public List<Event> getEvents() {
      return events;
    }
  }
  
  public static void main(String[] argv) {
    String[] testCaseList = { TestSampledBlockHasher.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
