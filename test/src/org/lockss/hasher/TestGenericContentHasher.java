/*
 * $Id: TestGenericContentHasher.java,v 1.27 2010-02-22 07:02:39 tlipkis Exp $
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

public class TestGenericContentHasher extends LockssTestCase {
  private static final String TEST_URL_BASE = "http://www.test.com/blah/";
  private static final String TEST_URL = TEST_URL_BASE+"blah.html";
  private static final String TEST_FILE_CONTENT = "This is a test file ";


  private File tmpDir = null;
  MockMessageDigest dig = null;
  MockArchivalUnit mau = null;

  public TestGenericContentHasher(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    tmpDir = getTempDir();
    dig = new MockMessageDigest();
    mau = new MockArchivalUnit(new MockPlugin(), TEST_URL_BASE);
  }

  public void testNullMessageDigest() throws IOException {
    CachedUrlSet cus = new MockCachedUrlSet(mau);
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
    } catch (NullPointerException iae) {
    }
  }

  public void testNullIterator() {
    MockCachedUrlSet cus = new MockCachedUrlSet(mau);
    cus.setHashIterator(null);
    MessageDigest dig = new MockMessageDigest();
    try {
      CachedUrlSetHasher hasher = new GenericContentHasher(cus, dig);
      fail("Creating a GenericContentHasher with a null iterator should "+
	   "throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testAccessors() throws IOException {
    MockCachedUrlSet cus = new MockCachedUrlSet(mau);
    cus.setHashIterator(CollectionUtil.EMPTY_ITERATOR);
    cus.setFlatIterator(null);
    cus.setEstimatedHashDuration(54321);
    MessageDigest dig = new MockMessageDigest();
    CachedUrlSetHasher hasher = new GenericContentHasher(cus, dig);
    assertSame(cus, hasher.getCachedUrlSet());
    assertEquals(54321, hasher.getEstimatedHashDuration());
    assertEquals("C", hasher.typeString());
    hasher.storeActualHashDuration(12345, null);
    assertEquals(12345, cus.getActualHashDuration());
    MessageDigest[] digs = hasher.getDigests();
    assertEquals(1, digs.length);
    assertEquals(dig, digs[0]);
  }

  public void testHashNoChildren() throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(0);
    byte[] bytes = getExpectedCusBytes(cus);

    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    hashAndCompare(bytes, hasher, bytes.length);
  }

//   public void testHashFilesOfDiffSize() throws IOException {
//     String url = TEST_URL;
//     String content = TEST_FILE_CONTENT;

//     MockMessageDigest dig1 = new MockMessageDigest();
//     MockCachedUrlSet cus1 = newMockCachedUrlSet(TEST_URL_BASE);
//     BadSizeMockCachedUrl cu1 = new BadSizeMockCachedUrl(url, 1000);
//     cu1.setContent(content);
//     cu1.setExists(true);
//     Vector files = new Vector();
//     files.add(cu1);
//     cus1.setHashItSource(files);

//     GenericContentHasher hasher1 = new GenericContentHasher(cus1, dig1);
//     hashToLength(hasher1, 54, 54);

//     MockMessageDigest dig2 = new MockMessageDigest();
//     MockCachedUrlSet cus2 = newMockCachedUrlSet(TEST_URL_BASE);
//     BadSizeMockCachedUrl cu2 = new BadSizeMockCachedUrl(url, 1005);
//     cu2.setContent(content);
//     cu2.setExists(true);
//     files = new Vector();
//     files.add(cu2);
//     cus2.setHashItSource(files);

//     GenericContentHasher hasher2 = new GenericContentHasher(cus2, dig2);
//     hashToLength(hasher2, 54, 54);

//     assertEquals(dig1, dig2);
//   }

  public void testHashFilesOfDiffSize() throws IOException {
    String url = TEST_URL;
    String content = TEST_FILE_CONTENT;

//     MockMessageDigest dig = new MockMessageDigest();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    BadSizeMockCachedUrl cu = new BadSizeMockCachedUrl(url, 1000);
    cu.setContent(content);
    cu.setExists(true);
    Vector files = new Vector();
    files.add(cu);
    cus.setHashItSource(files);

    byte[] bytes = getExpectedCusBytes(cus);

    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    hashAndCompare(bytes, hasher, bytes.length);
  }

  public void testHashSingleChild() throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(1);
    byte[] bytes = getExpectedCusBytes(cus);

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
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(TEST_URL_BASE, true, true);
    mau.addContent(TEST_URL_BASE,  TEST_FILE_CONTENT+" base");
    cus.setHashItSource(files);

    byte[] bytes = getExpectedCusBytes(cus);

    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    hashAndCompare(bytes, hasher, bytes.length);
  }

  public void testHashConsistant() throws IOException {
    MockCachedUrlSet cus = makeFakeCachedUrlSet(3);
    MockArchivalUnit mau = (MockArchivalUnit)cus.getArchivalUnit();
    mau.addUrl(TEST_URL_BASE, true, true);
    mau.addContent(TEST_URL_BASE,  "blah");
    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    hashToEnd(hasher, 10);

    MockMessageDigest dig2 = new MockMessageDigest();
    cus = makeFakeCachedUrlSet(3);
    mau.addUrl(TEST_URL_BASE, true, true);
    mau.addContent(TEST_URL_BASE,  "blah");
    hasher = new GenericContentHasher(cus, dig2);

    hashToEnd(hasher, 15);

    assertEquals(dig, dig2);
  }


  public void testNoContentHashedDifferentThan0Content() throws IOException {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(TEST_URL_BASE, false, true);
//     mau.addContent(TEST_URL_BASE,  "");
    ArrayList list = new ArrayList(1);
    list.add(mau.makeCachedUrl(TEST_URL_BASE));
    cus.setHashItSource(list);
    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    hashToEnd(hasher, 10);

    MockMessageDigest dig2 = new MockMessageDigest();
    mau.addUrl(TEST_URL_BASE, true, true);
    mau.addContent(TEST_URL_BASE,  "");
    list = new ArrayList(1);
    list.add(mau.makeCachedUrl(TEST_URL_BASE));
    cus.setHashItSource(list);
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
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(TEST_URL_BASE, true, true);
    mau.addContent(TEST_URL_BASE,  TEST_FILE_CONTENT+" base");
    cus.setHashIterator(files.iterator());

    GenericContentHasher hasher = new GenericContentHasher(cus, dig);
    hasher.hashStep(100);
    assertTrue(hasher.finished());
    assertTrue(is.isClosed());
  }

  public void testInputStreamIsClosedAfterAbort() throws IOException {
    String name = "http://www.example.com";
    MockCachedUrl cu = new MockCachedUrl(name);
    MockInputStream is = new MockInputStream();
    // ensure content is logner than name
    is.setContent("Content" + name + name + name + "EndContent");
    cu.setInputStream(is);
    cu.setExists(true);

    Vector files = new Vector();
    files.add(cu);
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(TEST_URL_BASE, true, true);
    mau.addContent(TEST_URL_BASE,  TEST_FILE_CONTENT+" base");
    cus.setHashIterator(files.iterator());

    GenericContentHasher hasher = new GenericContentHasher(cus, dig);
    // hash more bytes than the name, fewer than the entire node
    hasher.hashStep(name.length() * 2);
    assertFalse(hasher.finished());
    assertFalse(is.isClosed());
    hasher.abortHash();
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
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    mau.addUrl(TEST_URL_BASE, true, true);
    mau.addContent(TEST_URL_BASE,  TEST_FILE_CONTENT+" base");
    cus.setHashItSource(files);

    byte[] expectedBytes = getExpectedCusBytes(cus);
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
    byte[] expectedBytes = getExpectedCusBytes(cus);


    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    hashAndCompare(expectedBytes, hasher, expectedBytes.length);
  }

  public void testHashMultipleFilesSmallSteps()
      throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(5);
    byte[] expectedBytes = getExpectedCusBytes(cus);


    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    hashAndCompare(expectedBytes, hasher, 5);
  }

  public void testHashMultipleFilesStepsLargerThanCU()
      throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(5);
    byte[] expectedBytes = getExpectedCusBytes(cus);


    GenericContentHasher hasher = new GenericContentHasher(cus, dig);
    hashAndCompare(expectedBytes, hasher, expectedBytes.length/5 + 10);
  }

  public void testHashMultipleFilesTooLargeStep()
      throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(5);
    byte[] expectedBytes = getExpectedCusBytes(cus);


    GenericContentHasher hasher = new GenericContentHasher(cus, dig);
    hashAndCompare(expectedBytes, hasher, expectedBytes.length + 10);
  }

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

      cu.setContent(content);
      cu.setExists(true);
      files.add(cu);
    }
    cus.setHashItSource(files);
    return cus;
  }

  private byte[] getExpectedCusBytes(CachedUrlSet cus) throws IOException {
    Iterator it = cus.contentHashIterator();
    List byteArrays = new LinkedList();
    int totalSize = 0;

    while (it.hasNext()) {
      CachedUrl cu = cachedUrlSetNodeToCachedUrl((CachedUrlSetNode) it.next());
      if (cu.hasContent()) {
	byte[] arr = getExpectedCuBytes(cu);
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
    sb.append(name);
    int curKar;
    int contentSize = 0;
    while ((curKar = contentStream.read()) != -1) {
      sb.append((char)curKar);
      contentSize++;
    }
    byte[] sizeArray =
      (new BigInteger(Integer.toString(contentSize)).toByteArray());

    byte[] returnArr = new byte[sizeArray.length+sb.length()+1];
    int curPos = 0;
    byte[] nameBytes = sb.toString().getBytes();
    for (int ix=0; ix<nameBytes.length; ix++) {
      returnArr[curPos++] = nameBytes[ix];
    }
    returnArr[curPos++] = (byte)sizeArray.length;
    for (int ix=0; ix<sizeArray.length; ix++) {
      returnArr[curPos++] = sizeArray[ix];
    }
    return returnArr;
  }

  /**
   * Will hash through in intervals of stepSize and then compare the hashed
   * bytes to the expected bytes
   * @param expectedBytes the expected bytes
   * @param hasher the hasher
   * @param stepSize the step size
   * @throws IOException
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
    assertEquals(expectedBytes, dig.getUpdatedBytes());
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


  public class BadSizeMockCachedUrl extends MockCachedUrl {
    private int size;

    public BadSizeMockCachedUrl(String url, int size) {
      super(url);
      this.size = size;
    }

    public long getContentSize() {
      return size;
    }
  }
  public static void main(String[] argv) {
    String[] testCaseList = { TestGenericContentHasher.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
