/*
 * $Id: TestWrappedCachedUrl.java,v 1.3 2004-06-10 22:03:54 tyronen Exp $
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

package org.lockss.plugin.wrapper;

import java.io.*;
import java.util.*;
import java.math.BigInteger;
import org.lockss.daemon.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * This is the test class for
 * org.lockss.plugin.WrappedCachedUrl.
 *
 * Code adapted from that of TestGenericFileCachedUrl
 */
public class TestWrappedCachedUrl extends LockssTestCase {
  private WrappedArchivalUnit wau;
  private WrappedPlugin plugin;
  private MockPlugin mplug;
  private MockArchivalUnit mau;
  private WrappedCachedUrlSet wcus;
  private MockCachedUrlSet mcus;

  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    wau = (WrappedArchivalUnit)WrapperState.getWrapper(mau);
    mplug = new MockPlugin();
    plugin = (WrappedPlugin)
        WrapperState.getWrapper(mplug);
    mau.setPlugin(plugin);
    assertSame(wau.getPlugin(),plugin);

    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    wcus = (WrappedCachedUrlSet)plugin.makeCachedUrlSet(wau,rSpec);
    mcus = (MockCachedUrlSet)wcus.getOriginal();
  }

  WrappedCachedUrl makeUrl() throws Exception {
    MockCachedUrl murl = new MockCachedUrl(
                             "http://www.example.com/testDir/leaf1",mcus);
    murl.setContent("test stream");
    return (WrappedCachedUrl)WrapperState.getWrapper(murl);
  }

  public void testWrapped() throws Exception {
    WrappedCachedUrl url = makeUrl();
    CachedUrl orig = (CachedUrl)url.getOriginal();
    assertEquals("http://www.example.com/testDir/leaf1", orig.getUrl());
    assertEquals("org.lockss.plugin.CachedUrl",url.getOriginalClassName());
    assertSame(wau,url.getArchivalUnit());
  }

  public void testGetUrl() throws Exception {
    WrappedCachedUrl url = makeUrl();
    assertEquals("http://www.example.com/testDir/leaf1", url.getUrl());
  }

  public void testGetContentSize() throws Exception {
    WrappedCachedUrl url = makeUrl();
    BigInteger bi = new BigInteger(url.getUnfilteredContentSize());
    assertEquals(11, bi.intValue());
  }

  public void testOpenForReading() throws Exception {
    WrappedCachedUrl url = makeUrl();
    Reader urlIs = url.openForReading();
    CharArrayWriter baos = new CharArrayWriter(11);
    StreamUtil.copy(urlIs, baos);
    assertEquals("test stream", baos.toString());
  }

  public void testGetProperties() throws Exception {
    CIProperties newProps = new CIProperties();
    newProps.setProperty("test", "value");
    newProps.setProperty("test2", "value2");

    WrappedCachedUrl url = makeUrl();
    MockCachedUrl murl = (MockCachedUrl)url.getOriginal();
    murl.setProperties(newProps);
    Properties urlProps = url.getProperties();
    assertEquals("value", urlProps.getProperty("test"));
    assertEquals("value2", urlProps.getProperty("test2"));
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestWrappedCachedUrl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
