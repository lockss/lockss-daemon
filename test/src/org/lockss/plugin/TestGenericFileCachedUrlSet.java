/*
 * $Id: TestGenericFileCachedUrlSet.java,v 1.2 2002-11-06 00:04:23 aalto Exp $
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

import org.lockss.daemon.*;
import org.lockss.test.*;
import java.io.File;
import java.util.Iterator;
import org.lockss.repository.*;
import java.net.MalformedURLException;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;

/**
 * This is the test class for org.lockss.plugin.simulated.GenericFileUrlCacher
 *
 * @author  Emil Aalto
 * @version 0.0
 */


public class TestGenericFileCachedUrlSet extends LockssTestCase {
  private LockssRepository repo;
  private MockGenericFileArchivalUnit mau;

  public TestGenericFileCachedUrlSet(String msg) {
    super(msg);
  }
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = "";
    try {
      tempDirPath = super.getTempDir().getAbsolutePath() + File.separator;
    } catch (Exception ex) { assertTrue("Couldn't get tempDir.", false); }
    mau = new MockGenericFileArchivalUnit(null);
    mau.setPluginId(tempDirPath);
    repo = LockssRepositoryImpl.repositoryFactory(mau);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testFlatSetIterator() throws MalformedURLException {
    LeafNode leaf =
        repo.createLeafNode("http://www.example.com/testDir/branch1/leaf1");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createLeafNode("http://www.example.com/testDir/branch1/leaf2");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createLeafNode("http://www.example.com/testDir/leaf4");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createLeafNode("http://www.example.com/testDir/branch2/leaf3");
    leaf.makeNewVersion();
    leaf.sealNewVersion();

    CachedUrlSetSpec rSpec =
        new RECachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mau.makeCachedUrlSet(rSpec);
    Iterator setIt = fileSet.flatSetIterator();
    assertTrue(setIt.hasNext());
    CachedUrlSet childSet = (CachedUrlSet)setIt.next();
    String url = (String)childSet.getSpec().getPrefixList().get(0);
    assertTrue(url.equals("http://www.example.com/testDir/branch1"));
    childSet = (CachedUrlSet)setIt.next();
    url = (String)childSet.getSpec().getPrefixList().get(0);
    assertTrue(url.equals("http://www.example.com/testDir/branch2"));
    childSet = (CachedUrlSet)setIt.next();
    url = (String)childSet.getSpec().getPrefixList().get(0);
    assertTrue(url.equals("http://www.example.com/testDir/leaf4"));
    assertTrue(!setIt.hasNext());
  }

  public void testLeafIterator() throws MalformedURLException {
    LeafNode leaf =
        repo.createLeafNode("http://www.example.com/testDir/branch1/leaf1");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createLeafNode("http://www.example.com/testDir/branch1/leaf2");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createLeafNode("http://www.example.com/testDir/leaf4");
    leaf.makeNewVersion();
    leaf.sealNewVersion();
    leaf = repo.createLeafNode("http://www.example.com/testDir/branch2/leaf3");
    leaf.makeNewVersion();
    leaf.sealNewVersion();

    CachedUrlSetSpec rSpec =
        new RECachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mau.makeCachedUrlSet(rSpec);
    Iterator setIt = fileSet.leafIterator();
    assertTrue(setIt.hasNext());
    CachedUrl childUrl = (CachedUrl)setIt.next();
    assertTrue(childUrl.getUrl().equals("http://www.example.com/testDir/branch1/leaf1"));
    childUrl = (CachedUrl)setIt.next();
    assertTrue(childUrl.getUrl().equals("http://www.example.com/testDir/branch1/leaf2"));
    childUrl = (CachedUrl)setIt.next();
    assertTrue(childUrl.getUrl().equals("http://www.example.com/testDir/branch2/leaf3"));
    childUrl = (CachedUrl)setIt.next();
    assertTrue(childUrl.getUrl().equals("http://www.example.com/testDir/leaf4"));
    assertTrue(!setIt.hasNext());
  }
}
