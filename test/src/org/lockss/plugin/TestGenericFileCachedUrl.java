/*
 * $Id: TestGenericFileCachedUrl.java,v 1.3 2003-03-04 00:16:12 aalto Exp $
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

package org.lockss.plugin;

import java.io.*;
import java.util.Properties;
import java.math.BigInteger;
import org.lockss.daemon.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.StreamUtil;

/**
 * This is the test class for
 * org.lockss.plugin.GenericFileCachedUrl.
 *
 * @author  Emil Aalto
 * @version 0.0
 */
public class TestGenericFileCachedUrl extends LockssTestCase {
  private LockssRepository repo;
  private MockGenericFileArchivalUnit mgfau;
  private MockLockssDaemon theDaemon = new MockLockssDaemon(null);
  private CachedUrlSet cus;

  public TestGenericFileCachedUrl(String msg) {
    super(msg);
  }
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    TestLockssRepositoryServiceImpl.configCacheLocation(tempDirPath);
    mgfau = new MockGenericFileArchivalUnit(null);
    repo = theDaemon.getLockssRepository(mgfau);
    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    cus = mgfau.makeCachedUrlSet(rSpec);
  }

  public void testGetUrl() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", "test stream", null);

    CachedUrl url = cus.makeCachedUrl("http://www.example.com/testDir/leaf1");
    assertEquals("http://www.example.com/testDir/leaf1", url.getUrl());
  }

  public void testIsLeaf() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf2", null, null);

    CachedUrl url = cus.makeCachedUrl("http://www.example.com/testDir/leaf1");
    assertTrue(url.isLeaf());
    url = cus.makeCachedUrl("http://www.example.com/testDir/leaf2");
    assertTrue(url.isLeaf());
  }

  public void testGetContentSize() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf2", "test stream2", null);
    createLeaf("http://www.example.com/testDir/leaf3", "", null);

    CachedUrl url = cus.makeCachedUrl("http://www.example.com/testDir/leaf1");
    BigInteger bi = new BigInteger(url.getContentSize());
    assertEquals(11, bi.intValue());

    url = cus.makeCachedUrl("http://www.example.com/testDir/leaf2");
    bi = new BigInteger(url.getContentSize());
    assertEquals(12, bi.intValue());

    url = cus.makeCachedUrl("http://www.example.com/testDir/leaf3");
    bi = new BigInteger(url.getContentSize());
    assertEquals(0, bi.intValue());
  }

  public void testOpenForReading() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf2", "test stream2", null);
    createLeaf("http://www.example.com/testDir/leaf3", "", null);

    CachedUrl url = cus.makeCachedUrl("http://www.example.com/testDir/leaf1");
    InputStream urlIs = url.openForReading();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(11);
    StreamUtil.copy(urlIs, baos);
    assertEquals("test stream", baos.toString());

    url = cus.makeCachedUrl("http://www.example.com/testDir/leaf2");
    urlIs = url.openForReading();
    baos = new ByteArrayOutputStream(12);
    StreamUtil.copy(urlIs, baos);
    assertEquals("test stream2", baos.toString());

    url = cus.makeCachedUrl("http://www.example.com/testDir/leaf3");
    urlIs = url.openForReading();
    baos = new ByteArrayOutputStream(0);
    StreamUtil.copy(urlIs, baos);
    assertEquals("", baos.toString());
  }

  public void testOpenForHashing() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", "test stream", null);

    CachedUrl url = cus.makeCachedUrl("http://www.example.com/testDir/leaf1");
    InputStream urlIs = url.openForReading();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(11);
    StreamUtil.copy(urlIs, baos);
    assertEquals("test stream", baos.toString());
  }

  public void testGetProperties() throws Exception {
    Properties newProps = new Properties();
    newProps.setProperty("test", "value");
    newProps.setProperty("test2", "value2");
    createLeaf("http://www.example.com/testDir/leaf1", null, newProps);

    CachedUrl url = cus.makeCachedUrl("http://www.example.com/testDir/leaf1");
    Properties urlProps = url.getProperties();
    assertEquals("value", urlProps.getProperty("test"));
    assertEquals("value2", urlProps.getProperty("test2"));
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
