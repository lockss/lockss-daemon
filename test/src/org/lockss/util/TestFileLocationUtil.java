/*
 * $Id: TestFileLocationUtil.java,v 1.3 2003-02-24 22:13:43 claire Exp $
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

package org.lockss.util;

import java.net.MalformedURLException;
import org.lockss.util.*;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.daemon.LockssRepositoryImpl
 */

public class TestFileLocationUtil extends LockssTestCase {
  public TestFileLocationUtil(String msg) {
    super(msg);
  }

  public void testMapAuToFileLocation() {
    ArchivalUnit mau = new MockArchivalUnit();
    String expectedStr = "root/none/";
    assertEquals(expectedStr,
                 FileLocationUtil.mapAuToFileLocation("root", mau));
  }

  public void testMapUrlToFileLocation() throws MalformedURLException {
    String testStr = "http://www.example.com/branch1/branch2/index.html";
    String expectedStr = "root/www.example.com/http/branch1/branch2/index.html";
    assertEquals(expectedStr,
                 FileLocationUtil.mapUrlToFileLocation("root", testStr));

    try {
      testStr = ":/brokenurl.com/branch1/index/";
      FileLocationUtil.mapUrlToFileLocation("root", testStr);
      fail("Should have thrown MalformedURLException");
    } catch (MalformedURLException mue) { }
  }

}
