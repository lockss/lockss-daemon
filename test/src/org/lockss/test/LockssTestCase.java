/*
 * $Id: LockssTestCase.java,v 1.4 2002-10-25 21:47:40 tal Exp $
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

package org.lockss.test;

import java.util.*;
import java.io.*;
import org.lockss.util.*;
import junit.framework.TestCase;


public class LockssTestCase extends TestCase {
  List tmpDirs;

  public LockssTestCase(String msg) {
    super(msg);
  }

  
  /** Create and return the name of a temp dir.  The dir is created within
   * the default temp file dir.
   * It will be deleted following the test, by tearDown().  (So if you
   * override tearDown(), be sure to call <code>super.tearDown();</code>.)
   * @return The newly created directory
   */
  public File getTempDir() throws IOException {
    File tmpdir = FileUtil.createTempDir("locksstest", null);
    if (tmpdir != null) {
      if (tmpDirs == null) {
	tmpDirs = new LinkedList();
      }
      tmpDirs.add(tmpdir);
    }
    return tmpdir;
  }

  /** Remove any temp dirs */
  public void tearDown() throws Exception {
    if (tmpDirs != null) {
      for (ListIterator iter = tmpDirs.listIterator(); iter.hasNext(); ) {
	File dir = (File)iter.next();
	if (FileUtil.delTree(dir)) {
	  iter.remove();
	}
      }
    }
    super.tearDown();
  }

  /** Asserts that two Maps are equal (contain the same mappings).
   * If they are not an AssertionFailedError is thrown. */
  static public void assertEqual(String message, Map expected, Map actual) {
    if (expected == null && actual == null) {
      return;
    }
    if (expected != null && expected.equals(actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }
  
  /** Asserts that two Maps are equal (contain the same mappings).
   * If they are not an AssertionFailedError is thrown. */
  static public void assertEqual(Map expected, Map actual) {
    assertEqual(null, expected, actual);
  }

  /** Asserts that two collections are isomorphic. If they are not
   * an AssertionFailedError is thrown. */
  static public void assertIsomorphic(String message,
				      Collection expected, Collection actual) {
    if (CollectionUtil.isIsomorphic(expected, actual)) {
      return;
    }
	failNotEquals(message, expected, actual);
  }
  
  /** Asserts that two collections are isomorphic. If they are not
   * an AssertionFailedError is thrown. */
  static public void assertIsomorphic(Collection expected, Collection actual) {
    assertIsomorphic(null, expected, actual);
  }

  /** Asserts that the array is isomorphic with the collection. If not
   * an AssertionFailedError is thrown. */
  static public void assertIsomorphic(String message,
				      Object expected[], Collection actual) {
    if (CollectionUtil.isIsomorphic(expected, actual)) {
      return;
    }
	failNotEquals(message, expected, actual);
  }
  
  /** Asserts that the array is isomorphic with the collection. If not
   * an AssertionFailedError is thrown. */
  static public void assertIsomorphic(Object expected[], Collection actual) {
    assertIsomorphic(null, expected, actual);
  }

  /** Asserts that the array is isomorphic with the collection behind the
   * iterator. If not an AssertionFailedError is thrown. */
  static public void assertIsomorphic(String message,
				      Object expected[], Iterator actual) {
    if (CollectionUtil.isIsomorphic(new ArrayIterator(expected), actual)) {
      return;
    }
	failNotEquals(message, expected, actual);
  }
  
  /** Asserts that the array is isomorphic with the collection behind the
   * iterator. If not an AssertionFailedError is thrown. */
  static public void assertIsomorphic(Object expected[], Iterator actual) {
    assertIsomorphic(null, expected, actual);
  }

  // tk do a better job of printing collections
  static private void failNotEquals(String message,
					Object expected, Object actual) {
    String formatted= "";
    if (message != null)
      formatted= message+" ";
    fail(formatted+"expected:<"+expected+"> but was:<"+actual+">");
  }

}
