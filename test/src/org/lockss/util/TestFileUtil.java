/*
 * $Id: TestFileUtil.java,v 1.1 2003-09-16 23:22:45 eaalto Exp $
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

package org.lockss.util;

import java.io.File;
import org.lockss.test.LockssTestCase;

/**
 * test class for org.lockss.util.TestFileTestUtil
 */

public class TestFileUtil extends LockssTestCase {
  public void testSysDepPath() {
    String testStr = "test/var\\foo";
    String expectedStr = "test"+File.separator+"var"+File.separator+"foo";
    assertEquals(expectedStr, FileUtil.sysDepPath(testStr));
  }

  public void testSysIndepPath() {
    String testStr = "test/var\\foo";
    String expectedStr = "test/var/foo";
    assertEquals(expectedStr, FileUtil.sysIndepPath(testStr));
  }

  public void testPath() {
    // all paths assumed to start with '/', as in URL.getPath()
    assertTrue(FileUtil.isLegalPath("/var/foo"));
    assertTrue(FileUtil.isLegalPath("/var/../foo"));
    assertTrue(FileUtil.isLegalPath("/var/.."));
    assertTrue(FileUtil.isLegalPath("/var/../foo/.."));
    assertTrue(FileUtil.isLegalPath("var/./foo"));
    assertTrue(FileUtil.isLegalPath("var/."));
    assertTrue(FileUtil.isLegalPath("/."));
    assertFalse(FileUtil.isLegalPath("/var/../.."));
    assertFalse(FileUtil.isLegalPath("/./../"));
    assertFalse(FileUtil.isLegalPath("/var/../../foo"));
    assertFalse(FileUtil.isLegalPath("/var/.././.."));
    assertFalse(FileUtil.isLegalPath("/.."));
  }

}


