/*
 * $Id: TestGenericNameHasher.java,v 1.6 2003-02-26 20:38:54 aalto Exp $
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

import java.io.*;
import java.security.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

public class TestGenericNameHasher extends LockssTestCase {
  private static final char DELIMITER = '\n';

  private static final byte IS_NOT_LEAF=0;
  private static final byte IS_LEAF=1;

  public TestGenericNameHasher(String msg) {
    super(msg);
  }

  public void testNullMessageDigest() throws IOException {
    CachedUrlSet cus = new MockCachedUrlSet();
    try {
      CachedUrlSetHasher hasher = new GenericNameHasher(cus, null);
      fail("Creating a GenericNameHasher with a null digest should throw "+
	   "an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testNullCachedUrlSet() throws IOException {
    MessageDigest dig = new MockMessageDigest();
    try {
      CachedUrlSetHasher hasher = new GenericNameHasher(null, dig);
      fail("Creating a GenericNameHasher with a null cus should throw "+
	   "an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testNullIterator() {
    MockCachedUrlSet cus = new MockCachedUrlSet();
    cus.setFlatIterator(null);
    MessageDigest dig = new MockMessageDigest();
    try {
      CachedUrlSetHasher hasher = new GenericNameHasher(cus, dig);
      fail("Creating a GenericNameHasher with a null iterator should "+
 	   "throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testUnfinishedHasherNotFinished() {
    String name = "TestName1";
    MockMessageDigest dig = new MockMessageDigest();
    CachedUrlSetHasher hasher = createHasherFromName(name, dig);
    assertTrue(!hasher.finished());
  }

  public void testHashOneName() throws IOException {
    String name = "TestName1";
    MockMessageDigest dig = new MockMessageDigest();
    CachedUrlSetHasher hasher = createHasherFromName(name, dig);

    byte[] bytes = name.getBytes();
    for (int ix=0; ix<bytes.length; ix++) {
      assertEquals(1, hasher.hashStep(1));
      assertEquals(bytes[ix], dig.getUpdatedByte());
    }


    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());
  }

  public void testHashMultNameEqualToNumBytes() throws IOException {
    String nameStub = "TestName";
    Vector childNodeNames = new Vector();
    int numChildNodes = 5;
    for (int ix=0; ix<numChildNodes; ix++) {
      String name = nameStub+ix;
      childNodeNames.add(name);
    }
    MockMessageDigest dig = new MockMessageDigest();
    CachedUrlSetHasher hasher =
      createHasherFromNameVector(childNodeNames, dig);

    for (int ix=0; ix<numChildNodes; ix++) {
      String name = (String)childNodeNames.elementAt(ix);
      if (ix != 0) {
	String delimStr = String.valueOf(DELIMITER);
	hashAndCompareByteArray(delimStr.getBytes(), hasher, dig);
      }
      hashAndCompareByteArray(name.getBytes(), hasher, dig);
    }
    assertEquals(0, hasher.hashStep(1));
    assertTrue(hasher.finished());
  }

  public void testHashMultNameLessThanNumBytes() throws IOException {
    Vector childNodeNames = new Vector();
    int numChildNodes = 5;
    for (int ix=0; ix<numChildNodes; ix++) {
      String name = "TestName"+ix;
      childNodeNames.add(name);
    }
    MockMessageDigest dig = new MockMessageDigest();
    CachedUrlSetHasher hasher =
      createHasherFromNameVector(childNodeNames, dig);

    byte[] bytes = nameVectorToBytes(childNodeNames, DELIMITER);

    assertEquals(bytes.length, hasher.hashStep(bytes.length+100));
    for (int ix=0; ix<bytes.length; ix++) {
      assertEquals(bytes[ix], dig.getUpdatedByte());
    }
    assertTrue(hasher.finished());
  }

  private GenericNameHasher createHasherFromNameVector(Vector names,
						       MessageDigest dig) {
    Iterator it = names.iterator();
    Vector childNodes = new Vector();
    while(it.hasNext()) {
      String curName = (String)it.next();
      CachedUrlSetSpec cuss = new MockCachedUrlSetSpec(curName, null);
      MockCachedUrlSet cus = new MockCachedUrlSet(null, cuss);
      childNodes.add(cus);
    }
    MockCachedUrlSet root = new MockCachedUrlSet();
    root.setFlatIterator(childNodes.iterator());

    return new GenericNameHasher(root, dig);
  }

  private GenericNameHasher createHasherFromName(String name,
						 MessageDigest dig) {
    Vector names = new Vector();
    names.add(name);
    return createHasherFromNameVector(names, dig);
  }

  private byte[] nameVectorToBytes(Vector names, char delim) {
    Iterator it = names.iterator();
    StringBuffer sb = new StringBuffer();
    sb.append((String)it.next());
    while (it.hasNext()) {
      sb.append(delim);
      sb.append((String)it.next());
    }
    return sb.toString().getBytes();
  }

  private void hashAndCompareByteArray(byte[] bytes,
				       CachedUrlSetHasher hasher,
				       MockMessageDigest dig)
      throws IOException {
    assertEquals(bytes.length, hasher.hashStep(bytes.length));
    for (int ix=0; ix<bytes.length; ix++) {
      assertEquals("Byte mismatch at index "+ix, bytes[ix],
		   dig.getUpdatedByte());
    }
  }
}



