/*
 * $Id: TestWrappedCachedUrl.java,v 1.1 2003-09-04 23:11:17 tyronen Exp $
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
  private LockssRepository repo;
  private WrappedArchivalUnit wau;
  private MockGenericFileArchivalUnit mgfau;
  private MockLockssDaemon theDaemon;
  private WrappedCachedUrlSet cus;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = new MockLockssDaemon();
    theDaemon.getHashService();

    mgfau = new MockGenericFileArchivalUnit();
    wau = (WrappedArchivalUnit)WrapperState.getWrapper(mgfau);
    MockPlugin mplug = new MockPlugin();
    WrappedPlugin plugin = (WrappedPlugin)
        WrapperState.getWrapper(mplug);
    plugin.initPlugin(theDaemon);
    mplug.setDefiningConfigKeys(Collections.EMPTY_LIST);
    mgfau.setPlugin(plugin);
    assertSame(wau.getPlugin(),plugin);

    repo = theDaemon.getLockssRepository(wau);
    theDaemon.getNodeManager(wau);
    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    cus = (WrappedCachedUrlSet)wau.makeCachedUrlSet(rSpec);
  }

  public void tearDown() throws Exception {
    if (repo!=null)
      repo.stopService();
    super.tearDown();
  }

  WrappedCachedUrl makeUrl() throws Exception {
    String str = "http://www.example.com/testDir/leaf1";
    createLeaf(str, "test stream", null);
    return makeUrl(str);
  }

  WrappedCachedUrl makeUrl(String str) {
    return (WrappedCachedUrl)wau.makeCachedUrl(cus,str);
  }

  public void testWrapped() throws Exception {
    WrappedCachedUrl url = makeUrl();
    CachedUrl orig = (CachedUrl)url.getOriginal();
    assertEquals("http://www.example.com/testDir/leaf1", orig.getUrl());
    assertEquals("org.lockss.plugin.CachedUrl",url.getOriginalClassName());
  }

  public void testGetUrl() throws Exception {
    WrappedCachedUrl url = makeUrl();
    assertEquals("http://www.example.com/testDir/leaf1", url.getUrl());
  }

  public void testIsLeaf() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf2", null, null);
    WrappedCachedUrl url = makeUrl();
    assertTrue(url.isLeaf());
    url = makeUrl("http://www.example.com/testDir/leaf2");
    assertTrue(url.isLeaf());
  }

  public void testGetContentSize() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf2", "test stream2", null);
    createLeaf("http://www.example.com/testDir/leaf3", "", null);

    WrappedCachedUrl url = makeUrl();
    BigInteger bi = new BigInteger(url.getUnfilteredContentSize());
    assertEquals(11, bi.intValue());

    url = makeUrl("http://www.example.com/testDir/leaf2");
    bi = new BigInteger(url.getUnfilteredContentSize());
    assertEquals(12, bi.intValue());

    url = makeUrl("http://www.example.com/testDir/leaf3");
    bi = new BigInteger(url.getUnfilteredContentSize());
    assertEquals(0, bi.intValue());
  }

  public void testOpenForReading() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf2", "test stream2", null);
    createLeaf("http://www.example.com/testDir/leaf3", "", null);

    WrappedCachedUrl url = makeUrl();
    InputStream urlIs = url.openForReading();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(11);
    StreamUtil.copy(urlIs, baos);
    assertEquals("test stream", baos.toString());

    url = makeUrl("http://www.example.com/testDir/leaf2");
    urlIs = url.openForReading();
    baos = new ByteArrayOutputStream(12);
    StreamUtil.copy(urlIs, baos);
    assertEquals("test stream2", baos.toString());

    url = makeUrl("http://www.example.com/testDir/leaf3");
    urlIs = url.openForReading();
    baos = new ByteArrayOutputStream(0);
    StreamUtil.copy(urlIs, baos);
    assertEquals("", baos.toString());
  }

  public void testOpenForHashingDefaultsToNoFiltering() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", "<test stream>", null);
    WrappedCachedUrl url = makeUrl("http://www.example.com/testDir/leaf1");
    InputStream urlIs = url.openForReading();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(11);
    StreamUtil.copy(urlIs, baos);
    assertEquals("<test stream>", baos.toString());
  }

  public void testOpenForHashingCanFilter() throws Exception {
    String config =
      "org.lockss.genericFileCachedUrl.filterHashStream=true\n"+
      "org.lockss.genericFileCachedUrl.useNewFilter=true";
    ConfigurationUtil.setCurrentConfigFromString(config);
    Properties props = new Properties();
    props.setProperty("content-type", "text/html");
    String urlstr = "http://www.example.com/testDir/leaf1";
    createLeaf(urlstr, "<test stream>", props);

    WrappedCachedUrl url = makeUrl(urlstr);
    assertSame(url.getArchivalUnit(),wau);
    assertSame(url.getArchivalUnit().getPlugin().getDaemon(),theDaemon);
    InputStream urlIs = url.openForHashing();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(11);
    StreamUtil.copy(urlIs, baos);
    assertEquals("", baos.toString());
  }

  public void testOpenForHashingDoesntFilterNonHtml() throws Exception {
    String config = "org.lockss.genericFileCachedUrl.filterHashStream=true";
    ConfigurationUtil.setCurrentConfigFromString(config);
    Properties props = new Properties();
    props.setProperty("content-type", "blah");
    String urlstr = "http://www.example.com/testDir/leaf1";
    createLeaf(urlstr, "<test stream>", props);

    WrappedCachedUrl url = makeUrl(urlstr);
    InputStream urlIs = url.openForHashing();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(11);
    StreamUtil.copy(urlIs, baos);
    assertEquals("<test stream>", baos.toString());
  }

  public void testOpenForHashingWontFilterIfConfiguredNotTo() throws Exception {
    String config = "org.lockss.genericFileCachedUrl.filterHashStream=false";
    ConfigurationUtil.setCurrentConfigFromString(config);
    createLeaf("http://www.example.com/testDir/leaf1", "<test stream>", null);

    WrappedCachedUrl url = makeUrl("http://www.example.com/testDir/leaf1");
    InputStream urlIs = url.openForReading();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(11);
    StreamUtil.copy(urlIs, baos);
    assertEquals("<test stream>", baos.toString());
  }

  public void testGetProperties() throws Exception {
    Properties newProps = new Properties();
    newProps.setProperty("test", "value");
    newProps.setProperty("test2", "value2");
    createLeaf("http://www.example.com/testDir/leaf1", null, newProps);

    WrappedCachedUrl url = makeUrl("http://www.example.com/testDir/leaf1");
    Properties urlProps = url.getProperties();
    assertEquals("value", urlProps.getProperty("test"));
    assertEquals("value2", urlProps.getProperty("test2"));
  }

   public void testGetReader() throws Exception {
    WrappedCachedUrl url = makeUrl();
    Reader reader = url.getReader();
    CharArrayWriter writer = new CharArrayWriter(11);
    StreamUtil.copy(reader, writer);
    assertEquals("test stream", writer.toString());
  }

  private RepositoryNode createLeaf(String url, String content,
                                    Properties props) throws Exception {
    return TestRepositoryNodeImpl.createLeaf(repo, url, content, props);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestGenericFileCachedUrl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
