/*
 * $Id: TestGenericContentHasher.java,v 1.11 2003-02-26 02:40:56 troberts Exp $
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
  private static final String TEST_URL = "http://www.test.com/blah/blah.html";
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

  public void testHashNoFiles() throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(0);
    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());

    assertEquals(-1, dig.getUpdatedByte());
  }

  public void testHashSingleFile() throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(1);
    byte[] bytes = getExpectedCUSBytes(cus);

    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    int bytesHashed = 0;
    int bytesExpected = bytes.length;
    while (bytesHashed < bytesExpected) {
      assertTrue(!hasher.finished());
      bytesHashed += hasher.hashStep(bytesExpected);
    }
    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());
    for (int ix=0; ix<bytes.length; ix++) {
      assertEquals(bytes[ix], dig.getUpdatedByte());
    }
  }

  public void testHashSingleFileBigContent() 
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
    Vector files = new Vector();
    files.add(cu);
    MockCachedUrlSet cus = new MockCachedUrlSet();
    cus.setTreeItSource(files);

    byte[] bytes = getExpectedCUSBytes(cus);

    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    int bytesHashed = 0;
    int bytesExpected = bytes.length;
    while (bytesHashed < bytesExpected) {
      assertTrue(!hasher.finished());
      bytesHashed += hasher.hashStep(bytesExpected);
    }
    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());
    for (int ix=0; ix<bytes.length; ix++) {
      assertEquals(bytes[ix], dig.getUpdatedByte());
    }
  }

  public void testInputStreamIsClosed() throws IOException {
    MockCachedUrl cu = new MockCachedUrl("http://www.example.com");
    MockInputStream is = new MockInputStream();
    is.setContent("blah");
    cu.setInputStream(is);

    Vector files = new Vector();
    files.add(cu);
    MockCachedUrlSet cus = new MockCachedUrlSet();
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
    

    Vector files = new Vector();
    files.add(cu);
    MockCachedUrlSet cus = new MockCachedUrlSet();
    cus.setTreeItSource(files);

    byte[] expectedBytes = getExpectedCUSBytes(cus);
    //set the input stream again, since the above call uses it up
    is.regenerate();
    is.setZeroInterval(2);

    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    int bytesLeftToHash = expectedBytes.length;
    while (bytesLeftToHash > 0) {
      assertTrue(!hasher.finished());
      int bytesHashed = hasher.hashStep(2);
      assertTrue(bytesHashed >= 0);
      bytesLeftToHash -= bytesHashed;
    }
    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());

    for (int ix=0; ix<expectedBytes.length; ix++) {
      assertEquals(expectedBytes[ix], dig.getUpdatedByte());
    }
  }

  public void testHashMultipleFiles()
      throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(5);
    byte[] bytes = getExpectedCUSBytes(cus);


    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    assertEquals(bytes.length, hasher.hashStep(bytes.length));
    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());

    for (int ix=0; ix<bytes.length; ix++) {
      assertEquals(bytes[ix], dig.getUpdatedByte());
    }
  }

  public void testHashMultipleFilesSmallSteps()
      throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(5);
    byte[] bytes = getExpectedCUSBytes(cus);


    GenericContentHasher hasher = new GenericContentHasher(cus, dig);
    int totalHashed = 0;
    while (!hasher.finished()) {
      totalHashed += hasher.hashStep(5);
    }
    assertEquals(bytes.length, totalHashed);
    for (int ix=0; ix<bytes.length; ix++) {
      assertEquals(bytes[ix], dig.getUpdatedByte());
    }
  }

  public void testHashMultipleFilesStepsLargerThanCU()
      throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(5);
    byte[] bytes = getExpectedCUSBytes(cus);


    GenericContentHasher hasher = new GenericContentHasher(cus, dig);
    int totalHashed = 0;
    while (!hasher.finished()) {
      totalHashed += hasher.hashStep(bytes.length/5 + 10);
    }
    assertEquals(bytes.length, totalHashed);

    for (int ix=0; ix<bytes.length; ix++) {
      assertEquals(bytes[ix], dig.getUpdatedByte());
    }
  }

  public void testHashMultipleFilesTooLargeStep()
      throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(5);
    byte[] bytes = getExpectedCUSBytes(cus);


    GenericContentHasher hasher = new GenericContentHasher(cus, dig);
    int totalHashed = 0;
    while (!hasher.finished()) {
      totalHashed += hasher.hashStep(bytes.length + 10);
    }
    assertEquals(bytes.length, totalHashed);

    for (int ix=0; ix<bytes.length; ix++) {
      assertEquals(bytes[ix], dig.getUpdatedByte());
    }
  }

  private CachedUrlSet makeFakeCachedUrlSet(int numFiles)
      throws IOException, FileNotFoundException {
    Vector files = new Vector(numFiles);
    for (int ix=0; ix < numFiles; ix++) {
      String url = TEST_URL+ix;
      MockCachedUrl cu = new MockCachedUrl(url);

      cu.setContent(TEST_FILE_CONTENT+ix);

      files.add(cu);
    }
    MockCachedUrlSet cus = new MockCachedUrlSet();
    cus.setTreeItSource(files);
    return cus;
  }

  private byte[] getExpectedCUSBytes(CachedUrlSet cus) throws IOException {
    Iterator it = cus.treeIterator();
    List byteArrays = new LinkedList();
    int totalSize = 0;
    while (it.hasNext()) {
      CachedUrl cu = cachedUrlSetNodeToCachedUrl((CachedUrlSetNode) it.next());
      byte[] arr = getExpectedCUBytes(cu);
      totalSize += arr.length;
      byteArrays.add(arr);
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
}
