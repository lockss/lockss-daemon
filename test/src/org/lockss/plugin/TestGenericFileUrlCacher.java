/*
 * $Id: TestGenericFileUrlCacher.java,v 1.5 2002-11-06 00:04:23 aalto Exp $
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

import junit.framework.TestCase;
import org.lockss.daemon.*;
import org.lockss.test.*;
import java.io.*;
import java.util.Properties;
import org.lockss.util.StreamUtil;

/**
 * This is the test class for org.lockss.plugin.simulated.GenericFileUrlCacher
 *
 * @author  Emil Aalto
 * @version 0.0
 */


public class TestGenericFileUrlCacher extends LockssTestCase {
  private MockGenericFileArchivalUnit mau;

  public TestGenericFileUrlCacher(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = "";
    try {
      tempDirPath = super.getTempDir().getAbsolutePath() + File.separator;
    } catch (Exception ex) { assertTrue("Couldn't get tempDir.", false); }
    mau = new MockGenericFileArchivalUnit(new CrawlSpec(tempDirPath, null));
    mau.setPluginId(tempDirPath);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testCache() throws IOException {
    MockGenericFileUrlCacher cacher = new MockGenericFileUrlCacher(
        mau.getAUCachedUrlSet(), "http://www.example.com/testDir/leaf1");
    cacher.setUncachedInputStream(new StringInputStream("test content"));
    Properties props = new Properties();
    props.setProperty("test1", "value1");
    cacher.setUncachedProperties(props);
    cacher.cache();

    CachedUrl url = mau.cachedUrlFactory(mau.getAUCachedUrlSet(),
        "http://www.example.com/testDir/leaf1");
    InputStream is = url.openForReading();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(12);
    StreamUtil.copy(is, baos);
    is.close();
    assertTrue(baos.toString().equals("test content"));
    baos.close();

    props = url.getProperties();
    assertTrue(props.getProperty("test1").equals("value1"));
  }
}
