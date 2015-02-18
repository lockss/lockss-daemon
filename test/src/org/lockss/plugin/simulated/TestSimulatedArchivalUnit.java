/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.simulated;

import java.io.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;

/**
 * This is the test class for org.lockss.plugin.simulated.SimulatedArchivalUnit
 *
 * @author  Emil Aalto
 * @version 0.0
 */
public class TestSimulatedArchivalUnit extends LockssTestCase {

  String tempDirPath;

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
  }

  public void testResetContentTree() {
//    SimulatedArchivalUnit au = new SimulatedArchivalUnit("");
    //SimulatedContentGenerator scg = new SimulatedContentGenerator("");
  //  au.resetContentTree();
//    assertTrue(scg.isContentTree());
    //XXX test for no changes
  }

  public void testAlterContentTree() {
    //XXX test the changes
  }

  public void testMapUrlToContentFileName(String base) throws Exception {
    SimulatedArchivalUnit sau = new SimulatedArchivalUnit();
    Configuration config = ConfigurationUtil.fromArgs("root", tempDirPath);
    if (base != null) {
      config.put("base_url", base);
    }
    sau.setConfiguration(config);
    if (base != null) {
      assertEquals(StringUtil.upToFinal(base, "/"), sau.getUrlRoot());
    } else {
      assertEquals("http://www.example.com", sau.getUrlRoot());
    }
    String testStr = sau.getUrlRoot() + "/branch1/branch2";
    String expectedStr = SimulatedContentGenerator.ROOT_NAME + "/branch1/branch2";
    assertEquals(FileUtil.sysDepPath(expectedStr),
		 sau.mapUrlToContentFileName(testStr));

    testStr = "ftp://www.wrong.com/branch2/branch3";
    expectedStr = "ftp://www.wrong.com/branch2/branch3";
    assertEquals(FileUtil.sysDepPath(expectedStr),
		 sau.mapUrlToContentFileName(testStr));
  }

  public void testMapUrlToContentFileName() throws Exception {
    testMapUrlToContentFileName(null);
  }

  public void testMapUrlToContentFileNameWithBaseUrl() throws Exception {
    testMapUrlToContentFileName("http://foo.bar/");
  }

  public void testMapUrlToContentFileNameWithBaseUrlPath() throws Exception {
    testMapUrlToContentFileName("http://foo.bar/a/path/");
  }


  public void testMapContentFileNameToUrl(String base) throws Exception {
    SimulatedArchivalUnit sau = new SimulatedArchivalUnit();
    Configuration config = ConfigurationUtil.fromArgs("root", tempDirPath);
    if (base != null) {
      config.put("base_url", base);
    }
    sau.setConfiguration(config);

    // make sure to test proper file separator manipulation
    String MY_ROOT = "/simcontent/test";
    sau.simRoot = FileUtil.sysDepPath(MY_ROOT);

    if (base != null) {
      assertEquals(StringUtil.upToFinal(base, "/"), sau.getUrlRoot());
    } else {
      assertEquals("http://www.example.com", sau.getUrlRoot());
    }
    String testStr = MY_ROOT + "/branch1/branch2";
    String expectedStr = sau.getUrlRoot() + "/branch1/branch2";
    assertEquals(expectedStr,
		 sau.mapContentFileNameToUrl(FileUtil.sysDepPath(testStr)));
  }

  public void testMapContentFileNameToUrl() throws Exception {
    testMapContentFileNameToUrl(null);
  }

  public void testMapContentFileNameToUrlWithBaseUrl() throws Exception {
    testMapContentFileNameToUrl("http://foo.bar/");
  }

  public void testMapContentFileNameToUrlWithBaseUrlPath() throws Exception {
    testMapContentFileNameToUrl("http://foo.bar/a/path/");
  }

}
