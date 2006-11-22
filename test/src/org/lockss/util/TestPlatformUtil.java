/*
 * $Id: TestPlatformUtil.java,v 1.1.2.1 2006-11-22 00:53:22 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.test.*;

/**
 * test class for org.lockss.util.PlatformInfo
 */
public class TestPlatformUtil extends LockssTestCase {
  PlatformUtil info;

  public void setUp() throws Exception {
    super.setUp();
    info = PlatformUtil.getInstance();
  }

  public void testGetSystemTempDir() {
    String javatmp = System.getProperty("java.io.tmpdir");
    assertEquals(javatmp, PlatformUtil.getSystemTempDir());
    String parmtmp = "/another/tmp/dir";
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_TMPDIR, parmtmp);
    assertEquals(parmtmp, PlatformUtil.getSystemTempDir());
  }

  public void testGetUnfilteredTcpPorts() throws Exception {
    assertEmpty(info.getUnfilteredTcpPorts());
    ConfigurationUtil.setFromArgs(PlatformUtil.PARAM_UNFILTERED_TCP_PORTS, "9909");
    assertEquals(ListUtil.list("9909"), info.getUnfilteredTcpPorts());
    ConfigurationUtil.setFromArgs(PlatformUtil.PARAM_UNFILTERED_TCP_PORTS,
				  "9900;1234");
    assertEquals(ListUtil.list("9900", "1234"), info.getUnfilteredTcpPorts());
  }

  public void testDiskUsageNonexistentPath() throws Exception {
    long du = info.getDiskUsage("/very_unlik_elyd_irect_oryname/4x2");
    assertEquals(-1, du);
  }

  public void testDiskUsage() throws Exception {
    long du;
    File tmpdir = getTempDir();
    du = info.getDiskUsage(tmpdir.toString());
    assertTrue(du >= 0);
    StringBuffer sb = new StringBuffer(1500);
    while (sb.length() < 1200) {
      sb.append("01234567890123456789012345678901234567890123456789");
    }
    FileTestUtil.writeFile(new File(tmpdir, "foobar"), sb.toString());
    long du2 = info.getDiskUsage(tmpdir.toString());
    assertTrue(du2 > du);
  }

  public void testNonexistentPathNullDF() throws Exception {
    PlatformUtil.DF df =
      info.getDF(System.getProperty("java.io.tmpdir"));
    assertNotNull(df);
    df = info.getDF("/very_unlik_elyd_irect_oryname/4x3");
    assertNull(df);
  }

  public void testMakeDF() throws Exception {
    String str = "/dev/hda2  26667896   9849640  15463576    39% /";
    PlatformUtil.DF df = info.makeDFFromLine(str);
    assertNotNull(df);
    assertEquals(26667896, df.getSize());
    assertEquals(9849640, df.getUsed());
    assertEquals(15463576, df.getAvail());
    assertEquals("39%", df.getPercentString());
    assertEquals(.39, df.getPercent(), .0000001);
  }

  public void testMakeDFIll1() throws Exception {
    String str = "/dev/hda2  26667896   9849640  -1546    39% /";
    PlatformUtil.DF df = info.makeDFFromLine(str);
    assertNotNull(df);
    assertEquals(26667896, df.getSize());
    assertEquals(9849640, df.getUsed());
    assertEquals(-1546, df.getAvail());
    assertEquals("39%", df.getPercentString());
    assertEquals(.39, df.getPercent(), .0000001);
  }

  public void testMakeDFIll2() throws Exception {
    // linux df running under linux emul on OpenBSD can produce this
    String str = "-  26667896   9849640  4294426204    101% /";
    PlatformUtil.DF df = info.makeDFFromLine(str);
    assertNotNull(df);
    assertEquals(26667896, df.getSize());
    assertEquals(9849640, df.getUsed());
    assertEquals(0, df.getAvail());
    assertEquals("101%", df.getPercentString());
    assertEquals(1.01, df.getPercent(), .0000001);
  }

  public void xtestThreadDump() throws Exception {
    info.threadDump(true);
  }

}
