/*
 * $Id: TestRepositoryManagerManagerImpl.java,v 1.1.2.1 2009-11-03 23:52:01 edwardsb1 Exp $
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

import org.lockss.plugin.ArchivalUnit;
import org.lockss.poller.v3.V3Poller;
import org.lockss.protocol.IdentityManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.repository.v2.RepositoryManagerManager;
import org.lockss.test.*;
import org.lockss.util.FileUtil;

import junit.framework.TestCase;

/**
 * @author edwardsb
 * A set of tests for the RepositoryManagerManagerImpl class.
 */
public class TestRepositoryManagerManagerImpl extends LockssTestCase {
  
  // Constants
  private static final long k_sizeWarcMax = 10000;
  private static final String k_strAuid = "AUID1";
  private static final String k_strDirectory = "TestCollectionOfAuRepositoriesImpl/";
  private static final String k_strPeerID = "TCP:[192.168.0.1]:9723";
  private static final String k_strRepositorySpec = "jcr://tmp/";

  // Member variables
  private ArchivalUnit m_au1;
  private File m_dirTest;
  private IdentityManager m_idman;
  private MockLockssDaemon m_ldTest;
  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    JcrRepositoryHelperFactory jhrf;

    if (!JcrRepositoryHelperFactory.isPreconstructed()) {
      // Taken from org.lockss.state.TestHistoryRepository
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
      
      // The following are the peer identities that should be used in tests...
      m_idman.stringToPeerIdentity(k_strPeerID);
       
      JcrRepositoryHelperFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);
      jhrf = JcrRepositoryHelperFactory.getSingleton();
    } else { // isConstructed
      
      jhrf = JcrRepositoryHelperFactory.getSingleton();
      
      m_ldTest = (MockLockssDaemon) jhrf.getLockssDaemon();
      m_idman = jhrf.getIdentityManager();
    }
    
    m_dirTest = new File("TestRepositoryManagerManagerImpl");
    jhrf.createRepositoryHelper("TestRepositoryManagerManagerImpl", new File("TestRepositoryManagerManagerImpl"));
    m_au1 = new MockArchivalUnit(k_strAuid);
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    JcrRepositoryHelperFactory.reset();
    FileUtil.delTree(m_dirTest);
    
    super.tearDown();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.RepositoryManagerManagerImpl#addAuidToCoar(java.lang.String, java.lang.String)}.
   */
  public final void testAddAuidToCoar() {
    RepositoryManagerManager rmmTest;
    
    rmmTest = new RepositoryManagerManagerImpl();
    
    // Three tests against null...
    try {
      rmmTest.addAuidToCoar(null, null);
      fail("testAddAuidToCoar(null, null) should have failed miserably.");
    } catch (Exception e) {
      // Passes first test against null.
    }
    
    try {
      rmmTest.addAuidToCoar(null, k_strRepositorySpec);
      fail("testAddAuidToCoar(null, k_strRepositorySpec) should have failed.");
    } catch (Exception e) {
      // Passes second test against null.
    }
    
    try {
      rmmTest.addAuidToCoar(k_strAuid, null);
      fail("testAddAuidToCoar(k_strAuid, null) should have failed.");
    } catch (Exception e) {
      // Passes third test against null.
    }
   
    // Check against an impossible repository specification.
    
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.RepositoryManagerManagerImpl#doSizeCalc(org.lockss.repository.v2.RepositoryNode)}.
   */
  public final void testDoSizeCalc() {
//    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.RepositoryManagerManagerImpl#findLeastFullCoar()}.
   */
  public final void testFindLeastFullCoar() {
//    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.RepositoryManagerManagerImpl#getAuRepository(java.lang.String, org.lockss.plugin.ArchivalUnit)}.
   */
  public final void testGetAuRepository() {
//    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.RepositoryManagerManagerImpl#getDiskFullThreshold()}.
   */
  public final void testGetDiskFullThreshold() {
//    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.RepositoryManagerManagerImpl#getDiskWarnThreshold()}.
   */
  public final void testGetDiskWarnThreshold() {
//    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.RepositoryManagerManagerImpl#getExistingCoarSpecsForAuid(java.lang.String)}.
   */
  public final void testGetExistingCoarSpecsForAuid() {
//    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.RepositoryManagerManagerImpl#getGlobalNodeCache()}.
   */
  public final void testGetGlobalNodeCache() {
//    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.RepositoryManagerManagerImpl#getRepository(java.lang.String, org.lockss.plugin.ArchivalUnit)}.
   */
  public final void testGetRepository() {
//    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.RepositoryManagerManagerImpl#getRepositoryList()}.
   */
  public final void testGetRepositoryList() {
//    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.RepositoryManagerManagerImpl#getRepositoryMap()}.
   */
  public final void testGetRepositoryMap() {
//    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.RepositoryManagerManagerImpl#queueSizeCalc(org.lockss.plugin.ArchivalUnit, org.lockss.repository.v2.RepositoryNode)}.
   */
  public final void testQueueSizeCalc() {
//    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.RepositoryManagerManagerImpl#setConfig(org.lockss.config.Configuration, org.lockss.config.Configuration, org.lockss.config.Configuration.Differences)}.
   */
  public final void testSetConfig() {
//    fail("Not yet implemented"); // TODO
  }

}
