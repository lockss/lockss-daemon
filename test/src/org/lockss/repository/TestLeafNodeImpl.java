/*
 * $Id: TestLeafNodeImpl.java,v 1.3 2002-11-06 00:01:30 aalto Exp $
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

package org.lockss.repository;
import java.io.*;
import java.util.*;
import junit.framework.TestCase;
import org.lockss.test.*;
import org.lockss.util.StreamUtil;

/**
 * This is the test class for org.lockss.daemon.LeafEntryImpl
 */

public class TestLeafNodeImpl extends LockssTestCase {
  private LeafNode leaf;

  public TestLeafNodeImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    File tempDir = super.getTempDir();
    leaf = new LeafNodeImpl("test.cache", tempDir.getAbsolutePath() + File.separator, null);
  }

  public void testMakeNewCache() throws IOException {
    assertTrue(!leaf.exists());
    assertTrue(leaf.getCurrentVersion()==0);
    leaf.makeNewVersion();

    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    leaf.sealNewVersion();
    assertTrue(leaf.getCurrentVersion()==1);
    assertTrue(leaf.exists());
  }

  public void testMakeNewVersion() throws IOException {
    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("testing stream 1");
    StreamUtil.copy(is, os);
    os.close();
    is.close();

    leaf.sealNewVersion();
    assertTrue(leaf.getCurrentVersion()==1);
    leaf.makeNewVersion();
    os = leaf.getNewOutputStream();
    is = new StringInputStream("testing stream 2");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    leaf.sealNewVersion();
    assertTrue(leaf.getCurrentVersion()==2);

    is = leaf.getInputStream();
    OutputStream baos = new ByteArrayOutputStream(16);
    StreamUtil.copy(is, baos);
    is.close();
    String resultStr = baos.toString();
    baos.close();
    assertTrue(resultStr.equals("testing stream 2"));
  }

  public void testGetInputStream() throws IOException {
    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    leaf.sealNewVersion();

    is = leaf.getInputStream();
    OutputStream baos = new ByteArrayOutputStream(14);
    StreamUtil.copy(is, baos);
    is.close();
    String resultStr = baos.toString();
    baos.close();
    assertTrue(resultStr.equals("testing stream"));
  }

  public void testGetProperties() throws IOException {
    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();

    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    leaf.setNewProperties(props);
    leaf.sealNewVersion();

    props = leaf.getProperties();
    assertTrue(props.getProperty("test 1").equals("value 1"));

    leaf.makeNewVersion();
    os = leaf.getNewOutputStream();
    is = new StringInputStream("testing stream");
    StreamUtil.copy(is, os);
    os.close();
    is.close();
    props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.setNewProperties(props);
    leaf.sealNewVersion();

    props = leaf.getProperties();
    assertTrue(props.getProperty("test 1").equals("value 2"));
  }
}
