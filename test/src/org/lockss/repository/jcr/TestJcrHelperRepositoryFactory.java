/*
 * $Id: TestJcrHelperRepositoryFactory.java,v 1.1.2.3 2009-09-30 23:02:33 edwardsb1 Exp $
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
import java.util.Properties;

import org.lockss.app.LockssDaemon;
import org.lockss.poller.v3.V3Poller;
import org.lockss.protocol.IdentityManager;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.FileUtil;

import junit.framework.TestCase;

/**
 * @author edwardsb
 *
 */
public class TestJcrHelperRepositoryFactory extends LockssTestCase {

  // Constants
  private static final long k_sizeWarcMax = 10000;
  private static final String k_strDirectory = "TestJcrHelperRepositoryFactory/";

  // Member variables
  private IdentityManager m_idman;
  private MockLockssDaemon m_ldTest;
  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    m_ldTest = getMockLockssDaemon();
    m_ldTest.startDaemon();
  
    // Taken from org.lockss.poller.v3.TestBlockTally
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, k_strDirectory + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, k_strDirectory);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(V3Poller.PARAM_V3_VOTE_MARGIN, "73");
    p.setProperty(V3Poller.PARAM_V3_TRUSTED_WEIGHT, "300");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    
    m_idman = m_ldTest.getIdentityManager();
    m_idman.startService();

    JcrRepositoryHelperFactory.reset();
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    JcrRepositoryHelperFactory.reset();
    
    FileUtil.delTree(new File(k_strDirectory));
    
    super.tearDown();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.JcrRepositoryHelperFactory#preconstructor(long, org.lockss.protocol.IdentityManager, org.lockss.app.LockssDaemon)}.
   */
  public final void testPreconstructor() throws Exception {
    JcrRepositoryHelperFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);

    // Running the preconstructor twice should cause an error.    
    try {
      JcrRepositoryHelperFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);
      
      fail("Running the preconstructor twice should have caused an error.");
    } catch (LockssRepositoryException e) {
      // Pass the test.  
    }
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.JcrRepositoryHelperFactory#isPreonstructed()}.
   */
  public final void testIsPreonstructed() throws Exception {
    // Before anything is preconstructed...
    assertFalse(JcrRepositoryHelperFactory.isPreconstructed());
    
    JcrRepositoryHelperFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);
    
    assertTrue(JcrRepositoryHelperFactory.isPreconstructed());
  }

  
  /**
   * Test method for {@link org.lockss.repository.jcr.JcrRepositoryHelperFactory#getSingleton()}.
   */
  public final void testConstructor() throws Exception {
    JcrRepositoryHelperFactory jhrf1;
    JcrRepositoryHelperFactory jhrf2;
    
    // Running the constructor before the preconstructor should cause an error.
    try {
      jhrf1 = JcrRepositoryHelperFactory.getSingleton();
      
      fail("Running the constructor before the preconstructor should have caused an error.");
    } catch (LockssRepositoryException e) {
      // Passes the test.
    }
    
    JcrRepositoryHelperFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);

    jhrf1 = JcrRepositoryHelperFactory.getSingleton();
    jhrf2 = JcrRepositoryHelperFactory.getSingleton();
    
    assertEquals(jhrf1.hashCode(), jhrf2.hashCode());
  }

  
  public final void testChooseHelperRepository() throws Exception {
    File dir1;
    File dir2;
    JcrRepositoryHelperFactory jcrh;
    JcrRepositoryHelper jcr1;
    JcrRepositoryHelper jcr2;
    JcrRepositoryHelper jcrReturn1;
    JcrRepositoryHelper jcrReturn2;    
    
    JcrRepositoryHelperFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);
    
    // Create two helper repositories
    jcrh = JcrRepositoryHelperFactory.getSingleton();
    
    dir1 = getTempDir();
    jcr1 = jcrh.createHelperRepository("key1", dir1);
    
    dir2 = getTempDir();
    jcr2 = jcrh.createHelperRepository("key2", dir2);
    
    // chooseHelperRepository should return one of the two repositories.
    jcrReturn1 = jcrh.chooseHelperRepository();
    
    assertTrue(jcrReturn1.hashCode() == jcr1.hashCode() ||
        jcrReturn1.hashCode() == jcr2.hashCode());
    
    // chooseHelperRepository should now return the other of the two repositories.
    jcrReturn2 = jcrh.chooseHelperRepository();

    assertTrue(jcrReturn2.hashCode() == jcr1.hashCode() ||
        jcrReturn2.hashCode() == jcr2.hashCode());
    assertFalse(jcrReturn1.hashCode() == jcrReturn2.hashCode());

    FileUtil.delTree(dir2);
    FileUtil.delTree(dir1);
  }
  
  
  public final void testCreateHelperRepository() throws Exception {
    File dir;
    JcrRepositoryHelper jhr;
    JcrRepositoryHelperFactory jhrf;
    
    JcrRepositoryHelperFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);
    
    jhrf = JcrRepositoryHelperFactory.getSingleton();
    
    dir = getTempDir();
    jhr = jhrf.createHelperRepository("key", dir);
    
    // Verify that there's something in the temp directory... 
    assertTrue(dir.isDirectory());
    assertTrue(dir.list().length > 0);
    
    FileUtil.delTree(dir);
  }  
  
  
  /**
   * Test method for {@link org.lockss.repository.jcr.JcrRepositoryHelperFactory#getHelperRepository(java.lang.String)}.
   */
  public final void testGetHelperRepository() throws Exception {
    JcrRepositoryHelperFactory jhrf;
    JcrRepositoryHelper jhr1;
    JcrRepositoryHelper jhr2a;
    JcrRepositoryHelper jhr2b;
    JcrRepositoryHelper jhr3;
    File tempDir1;
    File tempDir2;
    
    JcrRepositoryHelperFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);
    jhrf = JcrRepositoryHelperFactory.getSingleton();
    
    // No repositories added; we SHOULD get null.
    jhr1 = jhrf.getHelperRepository("nullandvoid");
    assertNull(jhr1);
    
    // Add a 'foobar' repository, then get it a few times.
    tempDir1 = getTempDir();
    jhrf.createHelperRepository("foobar", tempDir1);
    
    jhr2a = jhrf.getHelperRepository("foobar");
    jhr2b = jhrf.getHelperRepository("foobar");
    
    assertNotNull(jhr2a);
    assertNotNull(jhr2b);
    assertEquals(jhr2a.hashCode(), jhr2b.hashCode());
    
    // Add a 'wozniak' repository.
    tempDir2 = getTempDir();
    jhrf.createHelperRepository("wozniak", tempDir2);
    jhr3 = jhrf.getHelperRepository("wozniak");
    
    assertNotNull(jhr3);
    assertNotEquals(jhr2a.hashCode(), jhr3.hashCode());
    
    FileUtil.delTree(tempDir2);
    FileUtil.delTree(tempDir1);
  }
  
  
  public final void testGetHelperRepositoryByDirectory() throws Exception {
    File dir1;
    File dir2;
    JcrRepositoryHelperFactory jcrh;
    JcrRepositoryHelper jcr1;
    JcrRepositoryHelper jcr2;
    JcrRepositoryHelper jcrReturn1;
    JcrRepositoryHelper jcrReturn2;    
    
    JcrRepositoryHelperFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);
    
    // Create two helper repositories
    jcrh = JcrRepositoryHelperFactory.getSingleton();
    
    dir1 = getTempDir();
    jcr1 = jcrh.createHelperRepository("key1", dir1);
    
    dir2 = getTempDir();
    jcr2 = jcrh.createHelperRepository("key2", dir2);
    
    // Verify our getHelperRepository...
    jcrReturn1 = jcrh.getHelperRepositoryByDirectory(dir1);
    
    assertTrue(jcrReturn1.hashCode() == jcr1.hashCode());
    
    jcrReturn2 = jcrh.getHelperRepositoryByDirectory(dir2);

    assertTrue(jcrReturn2.hashCode() == jcr2.hashCode());
    
    FileUtil.delTree(dir2);
    FileUtil.delTree(dir1);
  }

  
  public final void testGetIdentityManager() throws Exception {
    IdentityManager idman;
    JcrRepositoryHelperFactory jhrf;
    
    JcrRepositoryHelperFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);
    jhrf = JcrRepositoryHelperFactory.getSingleton();

    idman = jhrf.getIdentityManager();
    
    assertEquals(m_idman.hashCode(), idman.hashCode());
  }
  
  
  public final void testLockssDaemon() throws Exception {
    LockssDaemon ld;
    JcrRepositoryHelperFactory jhrf;
    
    JcrRepositoryHelperFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);
    jhrf = JcrRepositoryHelperFactory.getSingleton();

    ld = jhrf.getLockssDaemon();
    
    assertEquals(m_ldTest.hashCode(), ld.hashCode());
  }

  public final void testGetSizeWarcMax() throws Exception {
    long sizeWarc;
    JcrRepositoryHelperFactory jhrf;
    
    JcrRepositoryHelperFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);
    jhrf = JcrRepositoryHelperFactory.getSingleton();

    sizeWarc = jhrf.getSizeWarcMax();
    
    assertEquals(k_sizeWarcMax, sizeWarc);    
  }
}
