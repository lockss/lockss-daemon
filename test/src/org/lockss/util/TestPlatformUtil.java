/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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
import org.apache.commons.lang3.*;
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

  public void testEnsureRuntime_execDoesntImplicitlyInvokeShell() {
    // This command echoes a number.
    String[] cmd1 = {"sh", "-c", "echo $$"};
    // This command should not echo a number
    String[] cmd2 = {"echo", "$$"};

    if (false) {
      // Here only to verify validity of following test.  Disabled as might
      // fail on non- *nix
      assertMatchesRE("[0-9]", exec(cmd1));
    }
    assertNotMatchesRE("[0-9]", exec(cmd2));
  }

  String exec(String cmd[]) {
    try {
      Process p = Runtime.getRuntime().exec(cmd);
      Reader rdr =
	new InputStreamReader(new BufferedInputStream(p.getInputStream()),
			      Constants.DEFAULT_ENCODING);
      try {
	String s = StringUtil.fromReader(rdr);
	rdr.close();
	return s;
      } catch (IOException e) {
	log.warning("Couldn't read from process stream", e);
	return null;
      }
    } catch (Exception e) {
      log.warning("exec() failed", e);
      return null;
    }
  }

  public void testGetSystemTempDir() throws IOException {
    String javatmp = System.getProperty("java.io.tmpdir");
    assertEquals(javatmp, PlatformUtil.getSystemTempDir());
    String parmtmp = new File(getTempDir(), "another/tmp/dir").toString();
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_TMPDIR, parmtmp);
    assertEquals(new File(parmtmp, "dtmp").toString(),
		 PlatformUtil.getSystemTempDir());
  }

  public void testGetCwd() {
    log.info("cwd: " + info.getCwd());
  }

  public void testGetUnfilteredTcpPorts() throws Exception {
    assertEmpty(info.getUnfilteredTcpPorts());
    ConfigurationUtil.setFromArgs(PlatformUtil.PARAM_UNFILTERED_TCP_PORTS, "9909");
    assertEquals(ListUtil.list("9909"), info.getUnfilteredTcpPorts());
    ConfigurationUtil.setFromArgs(PlatformUtil.PARAM_UNFILTERED_TCP_PORTS,
				  "9900;1234");
    assertEquals(ListUtil.list("9900", "1234"), info.getUnfilteredTcpPorts());
    ConfigurationUtil.setFromArgs(PlatformUtil.PARAM_UNFILTERED_TCP_PORTS,
				  "9900,1234,333");
    assertEquals(ListUtil.list("9900", "1234", "333"),
		 info.getUnfilteredTcpPorts());
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
    String javatmp = System.getProperty("java.io.tmpdir");
    PlatformUtil.DF df =
      info.getPlatformDF(javatmp);
    assertNotNull(javatmp + " is null", df);
    javatmp = "/very_unlik_elyd_irect_oryname/4x3";
    df = info.getPlatformDF(javatmp);
    assertNull(javatmp, df);
  }

  public void testNonexistentPathNullJavaDF() throws Exception {
    String javatmp = System.getProperty("java.io.tmpdir");
    PlatformUtil.DF df = info.getJavaDF(javatmp);
    assertNotNull(javatmp + " is null", df);
    javatmp = "/very_unlik_elyd_irect_oryname/4x3";
    df = info.getJavaDF(javatmp);
    assertNull(javatmp, df);
  }

  public void testJavaDFEqualsDF() throws Exception {
    if (SystemUtils.IS_OS_MAC_OSX) {
      // Starting with MacOS BigSur, statistics returned by Java File
      // differ substantially from what df displays.  We don't know
      // why, nor which is correct.
      return;
    }

    assertSuccessRate(.1, 10);
    String javatmp = System.getProperty("java.io.tmpdir");
    PlatformUtil.DF df = info.getPlatformDF(javatmp);

    PlatformUtil.DF jdf = info.getJavaDF(javatmp);

    assertEquals(df.getAvail(), jdf.getAvail());
    assertEquals(df.getSize(), jdf.getSize());
    assertEquals(df.getUsed(), jdf.getUsed());
    assertEquals(df.getPercent(), jdf.getPercent(), 1.0);
    assertEquals(df.getPath(), jdf.getPath());
    assertEquals(PlatformUtil.DiskSpaceSource.DF, df.getSource());
    assertEquals(PlatformUtil.DiskSpaceSource.Java, jdf.getSource());
  }

  public void testGetDFSource() throws Exception {
    String javatmp = System.getProperty("java.io.tmpdir");
    assertEquals(PlatformUtil.DiskSpaceSource.Java,
                 info.getDF(javatmp).getSource());
    ConfigurationUtil.setFromArgs(PlatformUtil.PARAM_DISK_SPACE_SOURCE, "df");
    assertEquals(PlatformUtil.DiskSpaceSource.DF,
                 info.getDF(javatmp).getSource());
  }

  public void testMakeDF() throws Exception {
    String str = "/dev/hda2  26667896   9849640  15463576    39% /";
    PlatformUtil.DF df = info.makeDFFromLine("/mnt", str);
    assertNotNull(df);
    assertEquals("/mnt", df.getPath());
    assertEquals(26667896, df.getSize());
    assertEquals(9849640, df.getUsed());
    assertEquals(15463576, df.getAvail());
    assertEquals("39%", df.getPercentString());
    assertEquals(.39, df.getPercent(), .0000001);
  }

  public void testMakeDFLong() throws Exception {
    String str = "/dev/md0     2826607136 411558468 2269149176      16% /";
    PlatformUtil.DF df = info.makeDFFromLine("/cache.wd3", str);
    assertNotNull(df);
    assertEquals("/cache.wd3", df.getPath());
    assertEquals(2826607136L, df.getSize());
    assertEquals(411558468, df.getUsed());
    assertEquals(2269149176L, df.getAvail());
    assertEquals("16%", df.getPercentString());
    assertEquals(.16, df.getPercent(), .0000001);
  }

  public void testMakeDFIll1() throws Exception {
    String str = "/dev/hda2  26667896   9849640  -1546    39% /";
    PlatformUtil.DF df = info.makeDFFromLine("/mnt", str);
    assertNotNull(df);
    assertEquals("/mnt", df.getPath());
    assertEquals(26667896, df.getSize());
    assertEquals(9849640, df.getUsed());
    assertEquals(-1546, df.getAvail());
    assertEquals("39%", df.getPercentString());
    assertEquals(.39, df.getPercent(), .0000001);
  }

  public void testMakeDFIll2() throws Exception {
    // linux df running under linux emul on OpenBSD can produce this
    String str = "-  26667896   9849640  4294426204    101% /";
    PlatformUtil.DF df = info.makeDFFromLine("/mnt", str);
    assertNotNull(df);
    assertEquals("/mnt", df.getPath());
    assertEquals(26667896, df.getSize());
    assertEquals(9849640, df.getUsed());
    assertEquals(4294426204L, df.getAvail());
    assertEquals("101%", df.getPercentString());
    assertEquals(1.01, df.getPercent(), .0000001);
  }

  PlatformUtil.DF makeThresh(int minFreeMB, double minFreePercent) {
    return PlatformUtil.DF.makeThreshold(minFreeMB, minFreePercent);
  }

  public void testIsFullerThan() throws Exception {
    String str = "/dev/hda2  26667896   9849640  15463576    73% /";
    PlatformUtil.DF df = info.makeDFFromLine("/mnt", str);
    assertFalse(df.isFullerThan(makeThresh(100, 0)));
    assertFalse(df.isFullerThan(makeThresh(15000, 0)));
    assertTrue(df.isFullerThan(makeThresh(16000, 0)));
    assertTrue(df.isFullerThan(makeThresh(16000, .3)));
    assertTrue(df.isFullerThan(makeThresh(100, .30)));
    assertFalse(df.isFullerThan(makeThresh(100, .20)));
    assertFalse(df.isFullerThan(makeThresh(0, 0)));
  }

  public void testisDiskFullError() throws Exception {
    assertFalse(info.isDiskFullError(new IOException("jjjjj: No such file or directory")));
    assertTrue(info.isDiskFullError(new IOException("No space left on device")));
    assertTrue(info.isDiskFullError(new IOException("disk: No space left on device")));
  }

  public void xtestThreadDump() throws Exception {
    info.threadDump(true);
  }

  boolean isBuggy(String str) {
    return PlatformUtil.isBuggyDoubleString(str);
  }

  double parseDouble(String str) {
    return PlatformUtil.parseDouble(str);
  }

  public void testIsBuggyDoubleString() {
    assertTrue(isBuggy("2.2250738585072012e-308"));
    assertTrue(isBuggy("0.00022250738585072012E-304"));
    assertTrue(isBuggy("0.0000000022250738585072012E-299"));
    assertTrue(isBuggy("0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000022250738585072012"));
    assertTrue(isBuggy("0.00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002225073858507201212345"));
    assertTrue(isBuggy("22.250738585072012e-309"));
    assertTrue(isBuggy("22.250738585072012e-0309"));
    assertTrue(isBuggy("00000000002.2250738585072012e-308"));
    assertTrue(isBuggy("2.225073858507201200000e-308"));
    assertTrue(isBuggy("2.2250738585072012e-00308"));
    assertTrue(isBuggy("2.2250738585072012997800001e-308"));

    assertFalse(isBuggy("0"));
    assertFalse(isBuggy("4.5"));
    assertFalse(isBuggy("2.2e-308"));
    assertFalse(isBuggy("2.2E-308"));
    assertFalse(isBuggy("2.2250738585072011e-308"));

    // BaseServletManager.CompressingFilterWrapper checks the
    // Accept-Encoding: header without parsing out the qvalue
    assertTrue(isBuggy("gzip;q=1.0, identity; q=2.2250738585072012997800001e-308, *;q=0"));
    assertFalse(isBuggy("gzip;q=1.0, identity; q=2.2, *;q=0"));
  }

  public void testParseDouble() {
    assertEquals(0.0, parseDouble("0"));
    assertEquals(4.5, parseDouble("4.5"));
    try {
      parseDouble("2.2250738585072012e-308");
      fail("should throw");
    } catch (NumberFormatException e) {
    }
    try {
      parseDouble("0.00022250738585072012E-304");
      fail("should throw");
    } catch (NumberFormatException e) {
    }
  }

  public void testLinux() {
    PlatformUtil pi = new PlatformUtil.Linux();
    assertTrue(pi.isCaseSensitiveFileSystem());
    assertEquals(255, pi.maxFilename());
    assertEquals(4096, pi.maxPathname());
    assertTrue(pi.hasScriptingSupport());
  }

  public void testMacOS() {
    PlatformUtil pi = new PlatformUtil.MacOS();
    assertFalse(pi.isCaseSensitiveFileSystem());
    assertEquals(255, pi.maxFilename());
    assertEquals(1024, pi.maxPathname());
    assertTrue(pi.hasScriptingSupport());
  }

  public void testWindows() {
    PlatformUtil pi = new PlatformUtil.Windows();
    assertFalse(pi.isCaseSensitiveFileSystem());
    assertEquals(255, pi.maxFilename());
    assertEquals(260, pi.maxPathname());
    assertTrue(pi.hasScriptingSupport());
  }

}
