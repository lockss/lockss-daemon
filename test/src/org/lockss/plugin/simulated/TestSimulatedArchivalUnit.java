/*
 * $Id: TestSimulatedArchivalUnit.java,v 1.8 2008-05-06 21:35:36 dshr Exp $
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

package org.lockss.plugin.simulated;

import org.lockss.test.LockssTestCase;
import org.lockss.util.FileUtil;

/**
 * This is the test class for org.lockss.plugin.simulated.SimulatedArchivalUnit
 *
 * @author  Emil Aalto
 * @version 0.0
 */
public class TestSimulatedArchivalUnit extends LockssTestCase {

  public TestSimulatedArchivalUnit(String msg) {
    super(msg);
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

  public void testMapUrlToContentFileName() {
    SimulatedArchivalUnit sau = new SimulatedArchivalUnit();
    String testStr = sau.getUrlRoot() + "/branch1/branch2";
    String expectedStr = SimulatedContentGenerator.ROOT_NAME + "/branch1/branch2";
    assertEquals(FileUtil.sysDepPath(expectedStr),
        SimulatedArchivalUnit.mapUrlToContentFileName(testStr));

    testStr = "ftp://www.wrong.com/branch2/branch3";
    expectedStr = "ftp://www.wrong.com/branch2/branch3";
    assertEquals(FileUtil.sysDepPath(expectedStr),
        SimulatedArchivalUnit.mapUrlToContentFileName(testStr));
  }

  public void testMapContentFileNameToUrl() {
    // make sure to test proper file separator manipulation
    String MY_ROOT = "/simcontent/test";
    SimulatedArchivalUnit sau = new SimulatedArchivalUnit();
    sau.simRoot = FileUtil.sysDepPath(MY_ROOT);
    String testStr = MY_ROOT + "/branch1/branch2";
    String expectedStr = sau.getUrlRoot() + "/branch1/branch2";
    assertEquals(expectedStr, sau.mapContentFileNameToUrl(FileUtil.sysDepPath(testStr)));
  }

}
