/*
 * $Id: FuncSimulatedContent.java,v 1.2 2002-11-14 03:13:53 aalto Exp $
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

package org.lockss.plugin.simulated;

import java.util.*;
import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.crawler.Crawler;
import org.lockss.daemon.*;
import org.lockss.repository.*;
import org.lockss.plugin.Plugin;

/**
 * Test class for functional tests on the content.
 */
public class FuncSimulatedContent extends LockssTestCase {
  private SimulatedArchivalUnit sau;

  public FuncSimulatedContent(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = "";
    try {
      tempDirPath = super.getTempDir().getAbsolutePath() + File.separator;
    } catch (Exception e) { fail("Couldn't get tempDir."); }
    sau = new SimulatedArchivalUnit(tempDirPath);
    TestLockssRepositoryImpl.configCacheLocation(tempDirPath);
  }

  public void testPluginRegistration() {
    MockArchivalUnit mau = new MockArchivalUnit(new CrawlSpec("http://www.mock.com", null));
    Plugin.registerArchivalUnit(mau);
    Plugin.registerArchivalUnit(sau);
    mau = new MockArchivalUnit(new CrawlSpec("http://www.mock2.com", null));
    Plugin.registerArchivalUnit(mau);

    ArchivalUnit au = Plugin.findArchivalUnit(SimulatedArchivalUnit.SIMULATED_URL_START);
    assertTrue(au==sau);
  }

  public void testSimulatedContent() {
    createContent();
    crawlContent();
    checkContent();
    hashContent();
  }

  private void createContent() {
    SimulatedContentGenerator scgen = sau.getContentGenerator();
    scgen.setTreeDepth(2);
    scgen.setNumBranches(2);
    scgen.setNumFilesPerBranch(2);
    scgen.setFileTypes(scgen.FILE_TYPE_HTML+scgen.FILE_TYPE_TXT);
    scgen.setAbnormalFile("1,1", 1);

    sau.generateContentTree();
    assertTrue(scgen.isContentTree());
  }

  private void crawlContent() {
    CrawlSpec spec = new CrawlSpec(sau.SIMULATED_URL_START, null);
    Crawler.doCrawl(sau, spec);
  }

  private void checkContent() {
    checkRoot();
    checkLeaf();
    checkStoredContent();
  }

  private void hashContent() {
    hashSet(true);
    hashSet(false);
  }

  private void checkRoot() {
    CachedUrlSet set = sau.getAUCachedUrlSet();
    Iterator setIt = set.flatSetIterator();
    assertTrue(setIt.hasNext());
    CachedUrlSet childSet = (CachedUrlSet)setIt.next();
    String url = (String)childSet.getSpec().getPrefixList().get(0);
    assertTrue(url.equals(sau.SIMULATED_URL_ROOT+"/branch1"));
    childSet = (CachedUrlSet)setIt.next();
    url = (String)childSet.getSpec().getPrefixList().get(0);
    assertTrue(url.equals(sau.SIMULATED_URL_ROOT+"/branch2"));
    childSet = (CachedUrlSet)setIt.next();
    url = (String)childSet.getSpec().getPrefixList().get(0);
    assertTrue(url.equals(sau.SIMULATED_URL_ROOT+"/file1.html"));
    childSet = (CachedUrlSet)setIt.next();
    url = (String)childSet.getSpec().getPrefixList().get(0);
    assertTrue(url.equals(sau.SIMULATED_URL_ROOT+"/file1.txt"));
    childSet = (CachedUrlSet)setIt.next();
    url = (String)childSet.getSpec().getPrefixList().get(0);
    assertTrue(url.equals(sau.SIMULATED_URL_ROOT+"/file2.html"));
    childSet = (CachedUrlSet)setIt.next();
    url = (String)childSet.getSpec().getPrefixList().get(0);
    assertTrue(url.equals(sau.SIMULATED_URL_ROOT+"/file2.txt"));
    childSet = (CachedUrlSet)setIt.next();
    url = (String)childSet.getSpec().getPrefixList().get(0);
    assertTrue(url.equals(sau.SIMULATED_URL_ROOT+"/index.html"));
    assertTrue(!setIt.hasNext());
  }

  private void checkLeaf() {
    String parent = sau.SIMULATED_URL_ROOT + "/branch1";
    CachedUrlSetSpec spec = new RECachedUrlSetSpec(parent);
    CachedUrlSet set = sau.cachedUrlSetFactory(sau, spec);
    Iterator setIt = set.leafIterator();
    assertTrue(setIt.hasNext());
    CachedUrl child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/branch1/file1.html"));
    child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/branch1/file1.txt"));
    child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/branch1/file2.html"));
    child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/branch1/file2.txt"));
    child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/branch1/index.html"));
    child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/branch2/file1.html"));
    child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/branch2/file1.txt"));
    child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/branch2/file2.html"));
    child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/branch2/file2.txt"));
    child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/branch2/index.html"));
    child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/file1.html"));
    child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/file1.txt"));
    child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/file2.html"));
    child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/file2.txt"));
    child = (CachedUrl)setIt.next();
    assertTrue(child.getUrl().equals(parent + "/index.html"));
    assertTrue(!setIt.hasNext());
  }

  private void checkStoredContent() {
    String file = sau.SIMULATED_URL_ROOT + "/file1.txt";
    CachedUrl url = sau.cachedUrlFactory(sau.getAUCachedUrlSet(), file);
    InputStream content = url.openForReading();
    String expectedContent = sau.getContentGenerator().getFileContent(1, 0, 0, false);
    ByteArrayOutputStream baos = new ByteArrayOutputStream(expectedContent.length());
    try {
      StreamUtil.copy(content, baos);
      content.close();
      String contentStr = new String(baos.toByteArray());
      baos.close();
      assertTrue(contentStr.equals(expectedContent));
    } catch (IOException ie) {
      fail(ie.getMessage());
    }

    file = sau.SIMULATED_URL_ROOT + "/branch1/branch1/file1.txt";
    url = sau.cachedUrlFactory(sau.getAUCachedUrlSet(), file);
    content = url.openForReading();
    expectedContent = sau.getContentGenerator().getFileContent(1, 2, 1, true);
    baos = new ByteArrayOutputStream(expectedContent.length());
    try {
      StreamUtil.copy(content, baos);
      content.close();
      String contentStr = new String(baos.toByteArray());
      baos.close();
      assertTrue(contentStr.equals(expectedContent));
    } catch (IOException ie) {
      fail(ie.getMessage());
    }
  }

  private void hashSet(boolean namesOnly) {
    CachedUrlSet set = sau.getAUCachedUrlSet();
    MockMessageDigest dig = new MockMessageDigest();
    CachedUrlSetHasher hasher = null;
    if (namesOnly) {
      hasher = set.getNameHasher(dig);
    } else {
      hasher = set.getContentHasher(dig);
    }
    int bytesHashed = 0;
    long timeTaken = System.currentTimeMillis();
    while (!hasher.finished()) {
      try {
        bytesHashed += hasher.hashStep(256);
      } catch (IOException ie) {
        fail(ie.toString());
      }
    }
    timeTaken = System.currentTimeMillis() - timeTaken;
    if ((timeTaken>0)&&(bytesHashed>500)) {
      System.out.println("Bytes hashed: "+bytesHashed);
      System.out.println("Time taken: "+timeTaken+"ms");
      System.out.println("Bytes/sec: "+(bytesHashed*1000/timeTaken));
    }

    byte[] hash = dig.digest();
    dig = new MockMessageDigest();
    if (namesOnly) {
      hasher = set.getNameHasher(dig);
    } else {
      hasher = set.getContentHasher(dig);
    }
    while (!hasher.finished()) {
      try {
        hasher.hashStep(256);
      } catch (IOException ie) {
        fail(ie.toString());
      }
    }
    byte[] hash2 = dig.digest();
    assertTrue(Arrays.equals(hash, hash2));

    String parent = sau.SIMULATED_URL_ROOT + "/branch1";
    CachedUrlSetSpec spec = new RECachedUrlSetSpec(parent);
    set = sau.cachedUrlSetFactory(sau, spec);
    dig = new MockMessageDigest();
    if (namesOnly) {
      hasher = set.getNameHasher(dig);
    } else {
      hasher = set.getContentHasher(dig);
    }
    while (!hasher.finished()) {
      try {
        hasher.hashStep(256);
      } catch (IOException ie) {
        fail(ie.toString());
      }
    }
    hash2 = dig.digest();
    assertTrue(!Arrays.equals(hash, hash2));
  }

}
