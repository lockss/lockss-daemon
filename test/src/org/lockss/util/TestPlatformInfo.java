/*
 * $Id: TestPlatformInfo.java,v 1.3 2004-05-26 07:00:28 tlipkis Exp $
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

import junit.framework.TestCase;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * test class for org.lockss.util.PlatformInfo
 */
public class TestPlatformInfo extends LockssTestCase {
  PlatformInfo info;

  public void setUp() {
    info = PlatformInfo.getInstance();
  }

  public void testNonexistentPathNullDF() throws Exception {
    PlatformInfo.DF df =
      info.getDF(System.getProperty("java.io.tmpdir"));
    assertNotNull(df);
    df = info.getDF(System.getProperty("/very_unlik_elyd_irect_oryname/4x2"));
    assertNull(df);
  }
  
  public void testMakeDF() throws Exception {
    String str = "/dev/hda2  26667896   9849640  15463576    39% /";
    PlatformInfo.DF df = info.makeDFFromLine(str);
    assertNotNull(df);
    assertEquals(26667896, df.getSize());
    assertEquals(9849640, df.getUsed());
    assertEquals(15463576, df.getAvail());
    assertEquals("39%", df.getPercentString());
    assertEquals(.39, df.getPercent(), .0000001);
  }
  
}
