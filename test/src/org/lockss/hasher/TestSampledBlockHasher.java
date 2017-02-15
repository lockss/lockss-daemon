/*
 * $Id$
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
import org.lockss.protocol.*;
import org.lockss.repository.*;

public class TestSampledBlockHasher extends LockssTestCase {
  private static final Logger log = Logger.getLogger("TestSampledBlockHasher");

  private static final String TEST_URL_BASE = "http://www.test.com/blah/";
  private static final String TEST_URL = TEST_URL_BASE+"blah.html";
  private static final String TEST_FILE_CONTENT = "This is a test file ";
  private static final String TEST_NONCE = "Test nonce";
  private static final byte[] sampleNonce = TEST_NONCE.getBytes();
  private static final byte[] testContent = TEST_FILE_CONTENT.getBytes();

  MockLockssDaemon daemon;
  MessageDigest urlHasher = null;
  MockMessageDigest dig = null;
  MessageDigest[] digests = null;
  byte[][] initByteArrays = null;
  MockArchivalUnit mau = null;
  MockCachedUrlSet cus = null;

  private int numFilesInSample = 0;

  public void setUp() throws Exception {
    super.setUp();
    String alg =
      CurrentConfig.getParam(LcapMessage.PARAM_HASH_ALGORITHM,
			     LcapMessage.DEFAULT_HASH_ALGORITHM);
    setUpDiskSpace();
    daemon = getMockLockssDaemon();
    RepositoryManager repoMgr = daemon.getRepositoryManager();
    repoMgr.startService();

    assertEquals("SHA-1", alg);
    urlHasher = MessageDigest.getInstance(alg);
    dig = new MockMessageDigest();
    digests = new MessageDigest[]{ dig };
    initByteArrays = new byte[][]{ testContent };
    mau = new MockArchivalUnit(new MockPlugin(daemon), TEST_URL_BASE);
    cus = makeFakeCachedUrlSet(1);
    MockNodeManager nodeMgr = new MockNodeManager();
    daemon.setNodeManager(nodeMgr, mau);
    nodeMgr.setAuState(new MockAuState(mau));

    ConfigurationUtil.addFromArgs(LcapMessage.PARAM_HASH_ALGORITHM,
                                  LcapMessage.DEFAULT_HASH_ALGORITHM);
  }

  public void testConstructors() {
    RecordingEventHandler cb = new RecordingEventHandler();
    SampledBlockHasher hasher =
      new SampledBlockHasher(cus, 1, digests, initByteArrays, cb, 
			       1, sampleNonce);
    assertEquals("SHA-1", hasher.getInclusionPolicy().getAlgorithm());
    MessageDigest sampleHasher = new MockMessageDigest();
    hasher =
      new SampledBlockHasher(cus, 1, digests, initByteArrays, cb, 
			       1, sampleNonce, sampleHasher);
    assertEquals("Mock hash algorithm", hasher.getInclusionPolicy().getAlgorithm());
  }

  public void testBadSampleHasherAlgorithm() {
    MockCachedUrlSet cus = new MockCachedUrlSet(mau);
    cus.setHashIterator(CollectionUtil.EMPTY_ITERATOR);
    cus.setFlatIterator(null);
    cus.setEstimatedHashDuration(54321);
    ConfigurationUtil.addFromArgs(LcapMessage.PARAM_HASH_ALGORITHM,
                                  "BOGUS_HASH");
    RecordingEventHandler cb = new RecordingEventHandler();
    try {
      CachedUrlSetHasher hasher =
	new SampledBlockHasher(cus, 1, digests, initByteArrays, cb, 
			       1, sampleNonce);
      fail("Creating a SampledBlockHasher with a bad hash algorithm "+
	   "should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testNullSampleHasher() {
    RecordingEventHandler cb = new RecordingEventHandler();
    try {
      CachedUrlSetHasher hasher =
	new SampledBlockHasher(cus, 1, digests, initByteArrays, cb, 
			       1, sampleNonce, null);
      fail("Creating a SampledBlockHasher with a null sample hasher "+
	   "should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testNullPollerNonce() {
    RecordingEventHandler cb = new RecordingEventHandler();
    try {
      CachedUrlSetHasher hasher =
	new SampledBlockHasher(cus, 1, digests, initByteArrays, cb, 
			       1, null);
      fail("Creating a SampledBlockHasher with a null poller nonce "+
	   "should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testZeroMod() {
    RecordingEventHandler cb = new RecordingEventHandler();
    try {
      CachedUrlSetHasher hasher =
	new SampledBlockHasher(cus, 1, digests, initByteArrays, cb, 
			       0, sampleNonce);
      fail("Creating a SampledBlockHasher with a zero mod "+
	   "should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  private void doTestMultipleFilesWithMod(int numFiles, int mod)
   throws IOException, FileNotFoundException {
    cus = makeFakeCachedUrlSet(numFiles);
    numFilesInSample = 0;
    byte[] expectedBytes = getExpectedCusBytes(cus, mod);
    log.debug3("Expect " + expectedBytes.length + " bytes from " +
	       numFiles + " files mod " + mod);
    assertNotEquals("Zero bytes in sample. Test needs to be fixed.",
		    0, expectedBytes.length);
    byte[][] initByteArrays = { { } };
    if (log.isDebug3()) {
      for (int i = 0; i < testContent.length; i++) {
	log.debug3("doTest: " + i + " : " + testContent[i]);
      }
    }
    RecordingEventHandler cb = new RecordingEventHandler();
    SampledBlockHasher hasher =
      new SampledBlockHasher(cus, 1, digests, initByteArrays, cb,
			       mod, sampleNonce, urlHasher);
    hashToLength(hasher, expectedBytes.length, expectedBytes.length);
    List<Event> events = cb.getEvents();
    assertEquals(numFilesInSample, events.size());
    int ex = 0;
    int eventCount = 0;
    for (Event ev : events) {
      for (int i = 0; i < ev.byteArrays.length; i++) {
	for (int j = 0; j < ev.byteArrays[i].length; j++) {
	  if (ev.byteArrays[i][j] != expectedBytes[ex]) {
	    fail("ev[" + eventCount + "].byteArrays[" + i + "][" + j +
		 "] mismatch expectedBytes[" + ex + "]");
	  }
	  ex++;
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

  // Make sure that inclusion/exclusion hasn't changed.
  public void testInclusionUnchanged() {
    // 30 urls, mod 2 means any change has a 1/2^30 chance to be not seen.
    // no need to check other mods, unless SHA-1 is broken.
    int modulus = 2;
    Integer[] includedMod2 =
      {0, 1, 4, 5, 9, 12, 13, 17, 19, 22, 23, 24, 25, 27, 28};
    Collection<Integer> included = 
      new HashSet<Integer>(Arrays.asList(includedMod2));
    SampledBlockHasher.FractionalInclusionPolicy inclusionPolicy =
      new SampledBlockHasher.FractionalInclusionPolicy(
	modulus, sampleNonce, urlHasher);

    for (int i = 0; i < 30; i++) {
      String url = TEST_URL_BASE + TEST_URL + i;
      assertEquals("url: "+url,
		   included.contains(i), inclusionPolicy.isIncluded(url));
    }
    
  }

  // XXX DSHR - need tests with substance checking enabled

  private MockArchivalUnit newMockArchivalUnit(String url) {
    MockArchivalUnit mau = new MockArchivalUnit(new MockPlugin(daemon), url);
    MockCachedUrlSet cus = new MockCachedUrlSet(url);
    cus.setArchivalUnit(mau);
    LockssRepositoryImpl repo =
      (LockssRepositoryImpl)LockssRepositoryImpl.createNewLockssRepository(
        mau);
    daemon.setLockssRepository(repo, mau);
    repo.initService(daemon);
    repo.startService();
    MockNodeManager nodeMgr = new MockNodeManager();
    daemon.setNodeManager(nodeMgr, mau);
    nodeMgr.setAuState(new MockAuState(mau));
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

  private boolean isIncluded(String url, int mod) {
    urlHasher.reset();
    urlHasher.update(sampleNonce);
    urlHasher.update(url.getBytes());
    byte[] hash = urlHasher.digest();
    int value = ((hash[0] & 0xFF) << 24) | ((hash[1] & 0xFF) << 16) |
      ((hash[2] & 0xFF) << 8) | (hash[3] & 0xFF);
    boolean res = ((value % mod) == 0);
    return res;
  }

  private byte[] getExpectedCusBytes(CachedUrlSet cus, int mod)
    throws IOException {
    List byteArrays = new LinkedList();
    int totalSize = 0;

    for (CachedUrl cu : cus.getCuIterable()) {
      String urlName = cu.getUrl();
      byte[] hash = (sampleNonce + urlName).getBytes();
      if (mod > 0 && cu.hasContent() && isIncluded(urlName, mod)) {
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
    Iterator it = byteArrays.iterator();
    while (it.hasNext()) {
      byte[] curArr = (byte[]) it.next();
      for (int ix=0; ix<curArr.length; ix++) {
	returnArr[pos++] = curArr[ix];
      }
    }
    return returnArr;
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

  private void hashToLength(SampledBlockHasher hasher,
			    int length, int stepSize) throws IOException {
    int numBytesHashed = 0;
    while (numBytesHashed < length) {
      assertFalse(hasher.finished());
      numBytesHashed += hasher.hashStep(stepSize);
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
