/*
 * $Id: TestGenericContentHasher.java,v 1.6 2003-02-20 01:37:23 aalto Exp $
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
import junit.framework.TestCase;
import org.lockss.test.*;
import org.lockss.daemon.*;
import org.lockss.util.TimeBase;

public class TestGenericContentHasher extends LockssTestCase {
  private static final char DELIMITER='&';
  private static final String DELIMITER_STRING="&";
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

  public void testHashNoFiles() throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(0);
    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());

    assertEquals(-1, dig.getUpdatedByte());
  }

  public void testHashSingleFile() throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(1);
    byte[] bytes = getFakeCUBytes(1);

    GenericContentHasher hasher = new GenericContentHasher(cus, dig);

    assertEquals(bytes.length, hasher.hashStep(bytes.length));
    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());

    for (int ix=0; ix<bytes.length; ix++) {
      assertEquals(bytes[ix], dig.getUpdatedByte());
    }
  }

  public void testHashMultipleFiles()
      throws IOException, FileNotFoundException {
    CachedUrlSet cus = makeFakeCachedUrlSet(5);
    byte[] bytes = getFakeCUBytes(5);


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
    byte[] bytes = getFakeCUBytes(5);


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
    byte[] bytes = getFakeCUBytes(5);


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
    byte[] bytes = getFakeCUBytes(5);


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

  private Vector cachedUrlSetToBytes(CachedUrlSet cus) throws IOException {
    Iterator it = cus.treeIterator();
    Vector bytes = new Vector(10000);

    while (it.hasNext()) {
      CachedUrl cu = null;
      Object element = it.next();
      if (element instanceof CachedUrlSet) {
        CachedUrlSet cus2 = (CachedUrlSet)element;
        cu = cus2.makeCachedUrl(cus.getPrimaryUrl());
      } else if (element instanceof CachedUrl) {
        cu = (CachedUrl)element;
      }
      String delimStr = String.valueOf(DELIMITER);
      byte[] nameBytes = (delimStr+cu.getUrl()+delimStr).getBytes();
      for (int ix=0; ix<nameBytes.length; ix++) {
	bytes.add(new Byte(nameBytes[ix]));
      }
      InputStream is = cu.openForReading();
      int curByte;
      while ((curByte = is.read()) > 0) {
	bytes.add(new Byte((byte)curByte));
      }
    }
    return bytes;
  }

  private byte[] getFakeCUBytes(int numFiles) {
    int fileSize = TEST_URL.length()+TEST_FILE_CONTENT.length()+4;
    int size = fileSize*numFiles;
    byte[] bytes = new byte[size];
    int curByteIdx = 0;
    for (int ix=0; ix<numFiles; ix++) {
      StringBuffer sb = new StringBuffer(fileSize);
      sb.append(DELIMITER);
      sb.append(TEST_URL);
      sb.append(ix);
      sb.append(DELIMITER);
      sb.append(TEST_FILE_CONTENT);
      sb.append(ix);
      byte[] curElement = sb.toString().getBytes();
      for (int jx=0; jx < curElement.length; jx++) {
	bytes[curByteIdx++] = curElement[jx];
      }
    }
    return bytes;
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
    MockCachedUrlSet cus = new MockCachedUrlSet(null, null);
    cus.setLeafIterator(files.iterator());
    return cus;
  }

}
