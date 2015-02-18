/*
 * $Id$
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
import java.io.*;
import org.lockss.test.LockssTestCase;
import junit.framework.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class TestSimulatedContentStream extends LockssTestCase {

  static final String fn = "test.txt";
  static final String txt=  "Hello, my name is Jack.";
  static final int txtlen = txt.length();

  protected File file;

  public void setUp() throws Exception {
    // create a simple test file
    file = new File(getTempDir(), fn);
    file.createNewFile();
    assertTrue(file.canWrite());
    FileWriter fw = new FileWriter(file);
    fw.write(txt);
    fw.close();
    assertTrue(file.exists());
    assertTrue(file.length()>0);
  }

  public void tearDown() throws Exception {
    file.delete();
    super.tearDown();
  }

  String getStringFromFile(boolean toBeDamaged) {
    try {
      assertTrue(file.exists());
      assertTrue(file.canRead());
      FileInputStream fs = new FileInputStream(file);
      SimulatedContentStream str = new SimulatedContentStream(fs, toBeDamaged);
      InputStreamReader rd = new InputStreamReader(str);
      char[] carr = new char[txtlen];
      rd.read(carr, 0, txtlen);
      rd.close();
      return new String(carr);
    } catch (IOException e) {
      fail(e.getMessage());
      return "";
    }
  }

  public void testTrue() {
    String testStr = getStringFromFile(true);
    assertNotEquals(txt,testStr);
  }

  public void testFalse() {
    String testStr = getStringFromFile(false);
    assertEquals(txt,testStr);
  }

  public static Test suite() {
    return new TestSuite(TestSimulatedContentStream.class);
}

}