/*
 * $Id: TestJcrCollection.java,v 1.1.2.1 2009-08-15 00:51:25 edwardsb1 Exp $
 */
/*
 Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.repository.jcr;

import java.io.File;
import java.util.Map;

import org.lockss.util.FileUtil;

import junit.framework.TestCase;

/**
 * @author edwardsb
 *
 */
public class TestJcrCollection extends TestCase {

  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.JcrCollection#generateAuRepository(java.io.File)}.
   */
  public final void testGenerateAuRepository() throws Exception {
    File dirTest;
    File fileDatastore;
    JcrCollection jcTest;
    
    dirTest = FileUtil.createTempDir("test", "generateAuRepository");
    
    jcTest = new JcrCollection();
    jcTest.generateAuRepository(dirTest);
    
    // Verify it...
    fileDatastore = new File(dirTest, JcrCollection.k_FILENAME_DATASTORE);
    assertTrue(fileDatastore.exists());
    
    // Delete everything.
    fileDatastore.delete();
    dirTest.delete();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.JcrCollection#listAuRepositories(java.io.File)}.
   */
  public final void testListAuRepositories() throws Exception {
    File dir1;
    File dir2;
    File dir3;
    File dirParent;
    File fileDatastore1;
    File fileDatastore2;
    JcrCollection jcTest;
    Map<String, Object> mastrobjResult;
    
    // Create two directories that will have the right file...
    dirParent = FileUtil.createTempDir("test", "listAuRepositories");
    
    dir1 = new File(dirParent, "dir1");
    dir1.mkdir();
    fileDatastore1 = new File(dir1, JcrCollection.k_FILENAME_DATASTORE);
    fileDatastore1.createNewFile();
    
    dir2 = new File(dirParent, "parent");
    dir2.mkdir();
    dir3 = new File(dirParent, "child");
    dir3.mkdir();
    fileDatastore2 = new File(dir3, JcrCollection.k_FILENAME_DATASTORE);
    fileDatastore2.createNewFile();
    
    // Run listAuRepositories...
    jcTest = new JcrCollection();
    mastrobjResult = jcTest.listAuRepositories(dirParent);
    
    // Check it.
    assertTrue(mastrobjResult.containsKey("dir1"));
    assertTrue(mastrobjResult.containsKey("child"));
    
    // Clean up.
    fileDatastore2.delete();
    fileDatastore1.delete();
    dir3.delete();
    dir2.delete();
    dir1.delete();
  }

}
