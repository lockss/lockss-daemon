/*
 * $Id: TestJcrHelperRepositoryFactory.java,v 1.1.2.1 2009-09-23 02:03:02 edwardsb1 Exp $
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

    JcrHelperRepositoryFactory.reset();
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    JcrHelperRepositoryFactory.reset();
    
    FileUtil.delTree(new File(k_strDirectory));
    
    super.tearDown();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.JcrHelperRepositoryFactory#preconstructor(long, org.lockss.protocol.IdentityManager, org.lockss.app.LockssDaemon)}.
   */
  public final void testPreconstructor() throws Exception {
    JcrHelperRepositoryFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);

    // Running the preconstructor twice should cause an error.    
    try {
      JcrHelperRepositoryFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);
      
      fail("Running the preconstructor twice should have caused an error.");
    } catch (LockssRepositoryException e) {
      // Pass the test.  
    }
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.JcrHelperRepositoryFactory#isPreonstructed()}.
   */
  public final void testIsPreonstructed() throws Exception {
    // Before anything is preconstructed...
    assertFalse(JcrHelperRepositoryFactory.isPreconstructed());
    
    JcrHelperRepositoryFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);
    
    assertTrue(JcrHelperRepositoryFactory.isPreconstructed());
  }

  
  /**
   * Test method for {@link org.lockss.repository.jcr.JcrHelperRepositoryFactory#constructor()}.
   */
  public final void testConstructor() throws Exception {
    JcrHelperRepositoryFactory jhrf1;
    JcrHelperRepositoryFactory jhrf2;
    
    // Running the constructor before the preconstructor should cause an error.
    try {
      jhrf1 = JcrHelperRepositoryFactory.constructor();
      
      fail("Running the constructor before the preconstructor should have caused an error.");
    } catch (LockssRepositoryException e) {
      // Passes the test.
    }
    
    JcrHelperRepositoryFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);

    jhrf1 = JcrHelperRepositoryFactory.constructor();
    jhrf2 = JcrHelperRepositoryFactory.constructor();
    
    assertEquals(jhrf1.hashCode(), jhrf2.hashCode());
  }

  
  /**
   * Test method for {@link org.lockss.repository.jcr.JcrHelperRepositoryFactory#getHelperRepository(java.lang.String)}.
   */
  public final void testGetHelperRepository() throws Exception {
    JcrHelperRepositoryFactory jhrf;
    JcrHelperRepository jhr1;
    JcrHelperRepository jhr2a;
    JcrHelperRepository jhr2b;
    JcrHelperRepository jhr3;
    File tempDir1;
    File tempDir2;
    
    JcrHelperRepositoryFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);
    jhrf = JcrHelperRepositoryFactory.constructor();
    
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

}
