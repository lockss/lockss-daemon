/*
 * $Id: TestLeafEntryImpl.java,v 1.1 2002-10-28 23:54:17 aalto Exp $
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

package org.lockss.daemon;
import java.io.*;
import java.util.*;
import junit.framework.TestCase;
import org.lockss.test.*;
import org.lockss.util.StreamUtil;

/**
 * This is the test class for org.lockss.daemon.LeafEntryImpl
 */

public class TestLeafEntryImpl extends LockssTestCase {
  private LeafEntry leaf;

  public TestLeafEntryImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    File tempDir = super.getTempDir();
    leaf = new LeafEntryImpl("test.cache", tempDir.getAbsolutePath());
  }

  public void testMakeNewCache() {
    assertTrue(!leaf.exists());
    assertTrue(leaf.getCurrentVersion()==0);
    leaf.makeNewVersion();
    try {
      OutputStream os = leaf.getNewOutputStream();
      InputStream is = new StringInputStream("testing stream");
      StreamUtil.copy(is, os);
      os.close();
      is.close();
      leaf.closeCurrentVersion();
      assertTrue(leaf.getCurrentVersion()==1);
      assertTrue(leaf.exists());
    } catch (Exception e) { System.out.println(e); }
  }

  public void testMakeNewVersion() {
    leaf.makeNewVersion();
    assertTrue(leaf.getCurrentVersion()==1);
    try {
      OutputStream os = leaf.getNewOutputStream();
      InputStream is = new StringInputStream("testing stream 1");
      StreamUtil.copy(is, os);
      os.close();
      is.close();

      leaf.closeCurrentVersion();
      leaf.makeNewVersion();
      assertTrue(leaf.getCurrentVersion()==2);
      os = leaf.getNewOutputStream();
      is = new StringInputStream("testing stream 2");
      StreamUtil.copy(is, os);
      os.close();
      is.close();
      leaf.closeCurrentVersion();

      is = leaf.getInputStream();
      OutputStream baos = new ByteArrayOutputStream(16);
      StreamUtil.copy(is, baos);
      is.close();
      String resultStr = baos.toString();
      baos.close();
      assertTrue(resultStr.equals("testing stream 2"));
    } catch (Exception e) { System.out.println(e); }
  }

  public void testGetInputStream() {
    try {
      leaf.makeNewVersion();
      OutputStream os = leaf.getNewOutputStream();
      InputStream is = new StringInputStream("testing stream");
      StreamUtil.copy(is, os);
      os.close();
      is.close();

      is = leaf.getInputStream();
      OutputStream baos = new ByteArrayOutputStream(14);
      StreamUtil.copy(is, baos);
      is.close();
      String resultStr = baos.toString();
      baos.close();
      assertTrue(resultStr.equals("testing stream"));
    } catch (Exception e) { System.out.println(e); }
  }

  public void testGetProperties() {
    try {
      leaf.makeNewVersion();
      Properties props = new Properties();
      props.setProperty("test 1", "value 1");

      leaf.setNewProperties(props);
      leaf.closeCurrentVersion();
      props = leaf.getProperties();
      assertTrue(props.getProperty("test 1").equals("value 1"));

      leaf.makeNewVersion();
      props = new Properties();
      props.setProperty("test 1", "value 2");

      leaf.setNewProperties(props);
      leaf.closeCurrentVersion();
      props = leaf.getProperties();
      assertTrue(props.getProperty("test 1").equals("value 2"));
    } catch (Exception e) { System.out.println(e); }
  }
}
