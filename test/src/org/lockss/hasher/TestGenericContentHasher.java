/*
 * $Id: TestGenericContentHasher.java,v 1.14 2003-04-10 01:24:35 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
import junit.framework.TestCase;
import org.lockss.test.*;
import org.lockss.daemon.*;
import org.lockss.util.TimeBase;
import org.lockss.plugin.*;

public class TestGenericContentHasher extends LockssTestCase {
  private static final String TEST_URL_BASE = "http://www.test.com/blah/";
  private static final String TEST_URL = TEST_URL_BASE+"blah.html";
  private static final String TEST_FILE_CONTENT = "This is a test file ";


  private File tmpDir = null;
  MockMessageDigest dig = null;

  public TestGenericContentHasher(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    tmpDir = getTempDir();
    dig = new MockMessageDigest();
  }

  public void testNullMessageDigest() throws IOException {
    CachedUrlSet cus = new MockCachedUrlSet();
    try {
      CachedUrlSetHasher hasher = new GenericContentHasher(cus, null);
      fail("Creating a GenericContentHasher with a null digest should throw "+
	   "an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testNullCachedUrlSet() throws IOException {
    MessageDigest dig = new MockMessageDigest();
    try {
      CachedUrlSetHasher hasher = new GenericContentHasher(null, dig);
      fail("Creating a GenericContentHasher with a null cus should throw "+
	   "an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testNullIterator() {
    MockCachedUrlSet cus = new MockCachedUrlSet();
    cus.setTreeIterator(null);
    MessageDigest dig = new MockMessageDigest();
    try {
      CachedUrlSetHasher hasher = new GenericContentHasher(cus, dig);
      fail("Creating a GenericContentHasher with a null iterator should "+
	   "throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testHashNoChildren() throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(0);
    byte[] bytes = getExpectedCUSBytes(cus);

    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    hashAndCompare(bytes, hasher, bytes.length);
  }

  public void testHashSingleChild() throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(1);
    byte[] bytes = getExpectedCUSBytes(cus);

    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    hashAndCompare(bytes, hasher, bytes.length);
  }

  public void testHashSingleChildBigContent()
      throws IOException, FileNotFoundException {
    StringBuffer sb = new StringBuffer();
    for (int ix=0; ix<1000; ix++) {
      sb.append(ix);
      sb.append("blah");
      sb.append(ix);
    }
    String content = sb.toString();
    String url = "http://www.example.com";
    MockCachedUrl cu = new MockCachedUrl(url);
    cu.setContent(content);
    cu.setExists(true);
    Vector files = new Vector();
    files.add(cu);
    MockCachedUrlSet cus = new MockCachedUrlSet(TEST_URL_BASE);
    cus.addUrl(TEST_FILE_CONTENT+" base", TEST_URL_BASE, true, true);
    cus.setTreeItSource(files);

    byte[] bytes = getExpectedCUSBytes(cus);

    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    hashAndCompare(bytes, hasher, bytes.length);
  }

  public void testHashConsistant() throws IOException {
    MockCachedUrlSet cus = makeFakeCachedUrlSet(3);
    cus.addUrl("blah", TEST_URL_BASE, true, true);
    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    hashToEnd(hasher, 10);

    MockMessageDigest dig2 = new MockMessageDigest();
    cus = makeFakeCachedUrlSet(3);
    cus.addUrl("blah", TEST_URL_BASE, true, true);
    hasher = new GenericContentHasher(cus, dig2);

    hashToEnd(hasher, 15);

    assertEquals(dig, dig2);
  }


  public void testNoContentHashedDifferentThan0Content() throws IOException {
    String url = "http://www.example.com";

    MockCachedUrlSet cus = new MockCachedUrlSet(TEST_URL_BASE);
    cus.addUrl("", TEST_URL_BASE, false, true);
    cus.setTreeItSource(new ArrayList());
    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    hashToEnd(hasher, 10);

    MockMessageDigest dig2 = new MockMessageDigest();
    cus = new MockCachedUrlSet(TEST_URL_BASE);
    cus.addUrl("", TEST_URL_BASE, true, true);
    cus.setTreeItSource(new ArrayList());
    hasher = new GenericContentHasher(cus, dig2);

    hashToEnd(hasher, 15);

    assertNotEquals(dig, dig2);
  }

  public void testInputStreamIsClosed() throws IOException {
    MockCachedUrl cu = new MockCachedUrl("http://www.example.com");
    MockInputStream is = new MockInputStream();
    is.setContent("blah");
    cu.setInputStream(is);
    cu.setExists(true);

    Vector files = new Vector();
    files.add(cu);
    MockCachedUrlSet cus = new MockCachedUrlSet(TEST_URL_BASE);
    cus.addUrl(TEST_FILE_CONTENT+" base", TEST_URL_BASE, true, true);
    cus.setTreeIterator(files.iterator());

    GenericContentHasher hasher = new GenericContentHasher(cus, dig);
    hasher.hashStep(100);
    assertTrue(hasher.finished());
    assertTrue(is.isClosed());
  }

  public void testISReturn0() throws IOException {
    String content = "blah;blah;blah";
    String url = "http://www.example.com";
    MockCachedUrl cu = new MockCachedUrl(url);
    MockInputStream is = new MockInputStream();
    is.setContent(content);
    cu.setInputStream(is);
    cu.setExists(true);

    Vector files = new Vector();
    files.add(cu);
    MockCachedUrlSet cus = new MockCachedUrlSet(TEST_URL_BASE);
    cus.addUrl(TEST_FILE_CONTENT+" base", TEST_URL_BASE, true, true);
    cus.setTreeItSource(files);

    byte[] expectedBytes = getExpectedCUSBytes(cus);
    //set the input stream again, since the above call uses it up
    is.regenerate();
    is.setZeroInterval(2);

    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    int bytesLeftToHash = expectedBytes.length;
    while (bytesLeftToHash > 0) {
      assertFalse(hasher.finished());
      int bytesHashed = hasher.hashStep(2);
      assertTrue(bytesHashed >= 0);
      bytesLeftToHash -= bytesHashed;
    }
    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());

    assertBytesEqualDigest(expectedBytes, dig);
  }

  public void testHashMultipleFiles()
      throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(5);
    byte[] expectedBytes = getExpectedCUSBytes(cus);


    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    hashAndCompare(expectedBytes, hasher, expectedBytes.length);
  }

  public void testHashMultipleFilesSmallSteps()
      throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(5);
    byte[] expectedBytes = getExpectedCUSBytes(cus);


    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    hashAndCompare(expectedBytes, hasher, 5);
  }

  public void testHashMultipleFilesStepsLargerThanCU()
      throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(5);
    byte[] expectedBytes = getExpectedCUSBytes(cus);


    GenericContentHasher hasher = new GenericContentHasher(cus, dig);
    hashAndCompare(expectedBytes, hasher, expectedBytes.length/5 + 10);
  }

  public void testHashMultipleFilesTooLargeStep()
      throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(5);
    byte[] expectedBytes = getExpectedCUSBytes(cus);


    GenericContentHasher hasher = new GenericContentHasher(cus, dig);
    hashAndCompare(expectedBytes, hasher, expectedBytes.length + 10);
  }

  public void testGetNextElement() throws Exception {
    // with a normal cus, it should return numFiles + the rootCus
    MockCachedUrlSet mcus = makeFakeCachedUrlSet(2);
    GenericContentHasher hasher = new GenericContentHasher(mcus, dig);
    CachedUrlSetNode node = hasher.getNextElement();
    assertEquals(TEST_URL_BASE, node.getUrl());
    assertNotNull(hasher.getNextElement());
    assertNotNull(hasher.getNextElement());
    assertNull(hasher.getNextElement());

    // with a range, it should return numFiles
    mcus = makeFakeCachedUrlSet(2);
    mcus.setSpec(new RangeCachedUrlSetSpec("test", "1", "2"));
    hasher = new GenericContentHasher(mcus, dig);
    assertNotNull(hasher.getNextElement());
    assertNotNull(hasher.getNextElement());
    assertNull(hasher.getNextElement());

    // with a range of '.', it should return only itself
    mcus = makeFakeCachedUrlSet(2);
    mcus.setSpec(
        new RangeCachedUrlSetSpec("test",
                                  RangeCachedUrlSetSpec.SINGLE_NODE_RANGE,
                                  null));
    hasher = new GenericContentHasher(mcus, dig);
    node = hasher.getNextElement();
    assertEquals(TEST_URL_BASE, node.getUrl());
    assertNull(hasher.getNextElement());
  }

  private MockCachedUrlSet makeFakeCachedUrlSet(int numFiles)
      throws IOException, FileNotFoundException {
    Vector files = new Vector(numFiles+1);

    MockCachedUrlSet cus = new MockCachedUrlSet(TEST_URL_BASE);
    cus.addUrl(TEST_FILE_CONTENT+" base", TEST_URL_BASE, true, true);

    for (int ix=0; ix < numFiles; ix++) {
      String url = TEST_URL+ix;
      String content = TEST_FILE_CONTENT+ix;
      MockCachedUrl cu = new MockCachedUrl(url);

      cu.setContent(content);
      cu.setExists(true);
      files.add(cu);
    }
    cus.setTreeItSource(files);
    return cus;
  }

  private byte[] getExpectedCUSBytes(CachedUrlSet cus) throws IOException {
    Iterator it = cus.treeIterator();
    List byteArrays = new LinkedList();
    int totalSize = 0;

    CachedUrl cu = cus.makeCachedUrl(cus.getUrl());
    if (cu.hasContent()) {
      byte[] arr = getExpectedCUBytes(cu);
      totalSize += arr.length;
      byteArrays.add(arr);
    }

    while (it.hasNext()) {
      cu = cachedUrlSetNodeToCachedUrl((CachedUrlSetNode) it.next());
      if (cu.hasContent()) {
	byte[] arr = getExpectedCUBytes(cu);
	totalSize += arr.length;
	byteArrays.add(arr);
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
	return cus.makeCachedUrl(cus.getUrl());
      case CachedUrlSetNode.TYPE_CACHED_URL:
	return (CachedUrl)cusn;
    }
    return null;
  }

  private byte[] getExpectedCUBytes(CachedUrl cu) throws IOException {
    String name = cu.getUrl();
    InputStream contentStream = cu.openForHashing();
    StringBuffer sb = new StringBuffer();
    sb.append(name);
    int curKar;
    while ((curKar = contentStream.read()) != -1) {
      sb.append((char)curKar);
    }
    byte[] size = cu.getContentSize();
    byte[] returnArr = new byte[size.length+sb.length()+1];
    returnArr[0] = (byte)size.length;
    int curPos = 1;
    for (int ix=0; ix<size.length; ix++) {
      returnArr[curPos++] = size[ix];
    }
    byte[] nameBytes = sb.toString().getBytes();
    for (int ix=0; ix<nameBytes.length; ix++) {
      returnArr[curPos++] = nameBytes[ix];
    }
    return returnArr;
  }

  /**
   * Will hash through in intervals of stepSize and then compare the hashed
   * bytes to the expected bytes
   */
  private void hashAndCompare(byte[] expectedBytes,
			      GenericContentHasher hasher,
			      int stepSize) throws IOException {
    hashToLength(hasher, expectedBytes.length, stepSize);
    assertBytesEqualDigest(expectedBytes, dig);
  }

  private void hashToLength(GenericContentHasher hasher,
			    int length, int stepSize) throws IOException {
    int numBytesHashed = 0;
    while (numBytesHashed < length) {
      assertFalse(hasher.finished());
      numBytesHashed += hasher.hashStep(stepSize);
    }
    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());
  }

  private void hashToEnd(GenericContentHasher hasher, int stepSize)
    throws IOException {
    while (!hasher.finished()) {
      hasher.hashStep(stepSize);
    }
  }

  private void assertBytesEqualDigest(byte[] expectedBytes,
				      MockMessageDigest dig) {
    for (int ix=0; ix<expectedBytes.length; ix++) {
      assertEquals(expectedBytes[ix], dig.getUpdatedByte());
    }
  }

  private void assertEquals(MockMessageDigest dig1, MockMessageDigest dig2) {
    assertEquals(dig1.getNumRemainingBytes(), dig2.getNumRemainingBytes());
    while (dig1.getNumRemainingBytes() > 0) {
      assertEquals(dig1.getUpdatedByte(), dig2.getUpdatedByte());
    }
  }

  private void assertNotEquals(MockMessageDigest dig1,
			       MockMessageDigest dig2) {
    if (dig1.getNumRemainingBytes() != dig2.getNumRemainingBytes()) {
      return;
    }
    while (dig1.getNumRemainingBytes() > 0) {
      if (dig1.getUpdatedByte() != dig2.getUpdatedByte()) {
	return;
      }
    }
    if (dig1.getNumRemainingBytes() != dig2.getNumRemainingBytes()) {
      return;
    }

    fail("MockMessageDigests were equal");
  }
}
