/*
 * $Id: TestSimulatedUrlCacher.java,v 1.7 2003-02-24 22:13:42 claire Exp $
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

import java.util.Properties;
import org.lockss.test.*;
import org.lockss.daemon.*;
import org.lockss.repository.TestLockssRepositoryImpl;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.plugin.simulated.SimulatedUrlCacher
 *
 * @author  Emil Aalto
 * @version 0.0
 */


public class TestSimulatedUrlCacher extends LockssTestCase {
  public TestSimulatedUrlCacher(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    TestLockssRepositoryImpl.configCacheLocation("null");
  }

  public void testHtmlProperties() throws Exception {
    String testStr = "http://www.example.com/index.html";
    SimulatedUrlCacher suc = new SimulatedUrlCacher(
        new MockCachedUrlSet(null), testStr, "");
    Properties prop = suc.getUncachedProperties();
    assertEquals("text/html", prop.getProperty("content-type"));
    assertEquals(testStr, prop.getProperty("content-url"));
  }
  public void testTextProperties() throws Exception {
    String testStr = "http://www.example.com/file.txt";
    SimulatedUrlCacher suc = new SimulatedUrlCacher(
        new MockCachedUrlSet(null), testStr, "");
    Properties prop = suc.getUncachedProperties();
    assertEquals("text/plain", prop.getProperty("content-type"));
    assertEquals(testStr, prop.getProperty("content-url"));
  }
  public void testPdfProperties() throws Exception {
    String testStr = "http://www.example.com/file.pdf";
    SimulatedUrlCacher suc = new SimulatedUrlCacher(
        new MockCachedUrlSet(null), testStr, "");
    Properties prop = suc.getUncachedProperties();
    assertEquals("application/pdf", prop.getProperty("content-type"));
    assertEquals(testStr, prop.getProperty("content-url"));
  }
  public void testJpegProperties() throws Exception {
    String testStr = "http://www.example.com/image.jpg";
    SimulatedUrlCacher suc = new SimulatedUrlCacher(
        new MockCachedUrlSet(null), testStr, "");
    Properties prop = suc.getUncachedProperties();
    assertEquals("image/jpeg", prop.getProperty("content-type"));
    assertEquals(testStr, prop.getProperty("content-url"));
  }

}
