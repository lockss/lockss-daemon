/*
 * $Id: TestSimulatedUrlCacher.java,v 1.1 2002-10-23 23:41:35 aalto Exp $
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
import java.util.Properties;

/**
 * This is the test class for org.lockss.plugin.simulated.SimulatedUrlCacher
 *
 * @author  Emil Aalto
 * @version 0.0
 */


public class TestSimulatedUrlCacher extends TestCase {
  public TestSimulatedUrlCacher(String msg) {
    super(msg);
  }
  public void testHtmlProperties() {
    String testStr = "http://www.example.com/index.html";
    SimulatedUrlCacher suc = new SimulatedUrlCacher(null, testStr);
    Properties prop = suc.getUncachedProperties();
    assertTrue(prop.getProperty("content-type").equals("text/html"));
    assertTrue(prop.getProperty("content-url").equals(testStr));
  }
  public void testTextProperties() {
    String testStr = "http://www.example.com/file.txt";
    SimulatedUrlCacher suc = new SimulatedUrlCacher(null, testStr);
    Properties prop = suc.getUncachedProperties();
    assertTrue(prop.getProperty("content-type").equals("text/plain"));
    assertTrue(prop.getProperty("content-url").equals(testStr));
  }
  public void testPdfProperties() {
    String testStr = "http://www.example.com/file.pdf";
    SimulatedUrlCacher suc = new SimulatedUrlCacher(null, testStr);
    Properties prop = suc.getUncachedProperties();
    assertTrue(prop.getProperty("content-type").equals("application/pdf"));
    assertTrue(prop.getProperty("content-url").equals(testStr));
  }
  public void testJpegProperties() {
    String testStr = "http://www.example.com/image.jpg";
    SimulatedUrlCacher suc = new SimulatedUrlCacher(null, testStr);
    Properties prop = suc.getUncachedProperties();
    assertTrue(prop.getProperty("content-type").equals("image/jpeg"));
    assertTrue(prop.getProperty("content-url").equals(testStr));
  }

}
