/*
 * $Id: TestSimulatedArchivalUnit.java,v 1.3 2002-11-07 02:15:30 aalto Exp $
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

package org.lockss.plugin.simulated;

import junit.framework.TestCase;
import org.lockss.daemon.*;

/**
 * This is the test class for org.lockss.plugin.simulated.SimulatedArchivalUnit
 *
 * @author  Emil Aalto
 * @version 0.0
 */


public class TestSimulatedArchivalUnit extends TestCase {
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
  public void testMapUrlToContentFileName(String url) {
    String testStr = SimulatedArchivalUnit.SIMULATED_URL_ROOT + "/branch1/branch2";
    String expectedStr = SimulatedContentGenerator.ROOT_NAME + "/branch1/branch2/index.html";
    assertTrue(SimulatedArchivalUnit.mapUrlToContentFileName(testStr).equals(expectedStr));

    testStr = "ftp://www.wrong.com/branch2/branch3";
    expectedStr = "ftp://www.wrong.com/branch2/branch3/index.html";
    assertTrue(SimulatedArchivalUnit.mapUrlToContentFileName(testStr).equals(expectedStr));
  }
}
