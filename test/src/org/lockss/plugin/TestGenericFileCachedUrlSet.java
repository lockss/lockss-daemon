/*
 * $Id: TestGenericFileCachedUrlSet.java,v 1.9 2002-12-12 23:17:38 aalto Exp $
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
import java.util.*;
import java.net.MalformedURLException;
import org.lockss.daemon.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.util.StreamUtil;
import org.lockss.repository.TestRepositoryNodeImpl;

/**
 * This is the test class for
 * org.lockss.plugin.simulated.GenericFileCachedUrlSet.
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
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    TestLockssRepositoryImpl.configCacheLocation(tempDirPath);
    mau = new MockGenericFileArchivalUnit(null);
    repo = LockssRepositoryImpl.repositoryFactory(mau);
  }

  public void testFlatSetIterator() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf4", null, null);
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               null, null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3",
               null, null);
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               null, null);

    CachedUrlSetSpec rSpec =
        new RECachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mau.makeCachedUrlSet(rSpec);
    Iterator setIt = fileSet.flatSetIterator();
    ArrayList childL = new ArrayList(3);
    while (setIt.hasNext()) {
      CachedUrlSet childSet = (CachedUrlSet)setIt.next();
      childL.add(childSet.getSpec().getPrefixList().get(0));
    }
    // should be sorted
    String[] expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/branch2",
      "http://www.example.com/testDir/leaf4"
      };
    assertIsomorphic(expectedA, childL);
  }

  public void testLeafIterator() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf4", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);

    CachedUrlSetSpec rSpec =
        new RECachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mau.makeCachedUrlSet(rSpec);
    Iterator setIt = fileSet.leafIterator();
    ArrayList childL = new ArrayList(4);
    while (setIt.hasNext()) {
      CachedUrl childUrl = (CachedUrl)setIt.next();
      childL.add(childUrl.getUrl());
    }
    // should be sorted
    String[] expectedA = new String[] {
      "http://www.example.com/testDir/branch1/leaf1",
      "http://www.example.com/testDir/branch1/leaf2",
      "http://www.example.com/testDir/branch2/leaf3",
      "http://www.example.com/testDir/leaf4"
      };
    assertIsomorphic(expectedA, childL);
  }

  public void testLeafIteratorWithInternalContent() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf3", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/branch3",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/branch3/leaf2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);

    CachedUrlSetSpec rSpec =
        new RECachedUrlSetSpec("http://www.example.com/testDir");
    CachedUrlSet fileSet = mau.makeCachedUrlSet(rSpec);
    Iterator setIt = fileSet.leafIterator();
    ArrayList childL = new ArrayList(4);
    while (setIt.hasNext()) {
      CachedUrl childUrl = (CachedUrl)setIt.next();
      childL.add(childUrl.getUrl());
    }
    // should be sorted
    String[] expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/branch1/leaf1",
      "http://www.example.com/testDir/branch2/branch3",
      "http://www.example.com/testDir/branch2/branch3/leaf2",
      "http://www.example.com/testDir/leaf3"
      };
    assertIsomorphic(expectedA, childL);
  }


  private RepositoryNode createLeaf(String url, String content,
                                    Properties props) throws Exception {
    return TestRepositoryNodeImpl.createLeaf(repo, url, content, props);
  }

}
