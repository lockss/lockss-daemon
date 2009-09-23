/*
 * $Id: TestJcrCollection.java,v 1.1.2.3 2009-09-23 02:03:02 edwardsb1 Exp $
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

import java.io.*;
import java.util.*;

import javax.jcr.Session;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.data.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.poller.v3.V3Poller;
import org.lockss.protocol.IdentityManager;
import org.lockss.repository.*;
import org.lockss.repository.v2.LockssAuRepository;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.PlatformUtil.DF;

import junit.framework.TestCase;

/**
 * @author edwardsb
 *
 */
public class TestJcrCollection extends LockssTestCase {
  // Constants...
  private static final long k_sizeWarcMax = 10000;
  private static final String k_stemFile = "stem";
  private static final String k_strAuId = "AUID";
  private static final String k_strDirectory = "TestJcrCollection/";
  private static final String k_strPeerID = "TCP:[192.168.0.1]:9723";
  private static final String k_url = "http://www.example.com/";
  
  // Member variables
  private IdentityManager m_idman;
  private MockLockssDaemon m_ldTest;

  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    JcrHelperRepositoryFactory jhrf;

    if (!JcrHelperRepositoryFactory.isPreconstructed()) {
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
       
      JcrHelperRepositoryFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);
      jhrf = JcrHelperRepositoryFactory.constructor();
    } else { // isConstructed
      
      jhrf = JcrHelperRepositoryFactory.constructor();
      
      m_ldTest = (MockLockssDaemon) jhrf.getLockssDaemon();
      m_idman = jhrf.getIdentityManager();
    }
    
    jhrf.createHelperRepository("TestJcrCollection", new File("TestJcrCollection"));
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    JcrHelperRepositoryFactory.reset();
    
    // This check verifies that the test shut down correctly.
    checkLockFile();
    
    // Clean up everything! 
    FileUtil.delTree(new File(k_strDirectory));
    
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
    
    jcTest = new JcrCollection(dirTest);
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
    Map<String, File> mastrfileResult;
    
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
    jcTest = new JcrCollection(dirParent);
    mastrfileResult = jcTest.listAuRepositories();
    
    // Check it.
    assertTrue(mastrfileResult.containsKey("dir1"));
    assertTrue(mastrfileResult.containsKey("child"));
    
    // Clean up.
    fileDatastore2.delete();
    fileDatastore1.delete();
    dir3.delete();
    dir2.delete();
    dir1.delete();
  }

  
  /**
   * Test JcrCollection.openAuRepository()
   */
  public final void testOpenAuRepository() throws Exception {
    ArchivalUnit auGood;
    File dirTest;    
    File dirLocation;
    JcrCollection jcTest;
    LockssAuRepository larTest;
    
    dirTest = FileUtil.createTempDir("test", "OpenAuRepository");
    jcTest = new JcrCollection(dirTest);
    
    auGood = createAu(dirTest);
    
    larTest = jcTest.openAuRepository(auGood, dirTest);
    
    // TODO: Test the larTest.  I'm not sure what to do here.

    // Clean up.
    FileUtil.delTree(dirTest);
  }
  
  
  /**
   * Test JcrCollection.getDF()
   */
  public final void testGetDF() throws Exception {
    DF df1;
    DF df2;
    File dirTest;
    File fileFiller;
    int i;
    JcrCollection jcTest;
    OutputStream ostrFiller;
    
    
    dirTest = FileUtil.createTempDir("test", "getDF");
    jcTest = new JcrCollection(dirTest);
    df1 = jcTest.getDF();
    
    // I don't want to create an AU, fill it.  This test just makes sure that the
    // DF goes to the correct directory.
    fileFiller = new File(dirTest, "filler.txt");
    ostrFiller = new FileOutputStream(fileFiller);
    for (i = 0; i < 50000; i++) {
      ostrFiller.write((i % 26) + 65);  // 'ABCDEFG...'
    }
    ostrFiller.close();
    
    df2 = jcTest.getDF();
    
    // Compare the two DF's...
    assertTrue(df2.isFullerThan(df1));
    
    // Clean up.
    FileUtil.delTree(dirTest);
  }
  
  
  // Helper methods
  /**
   * @return a new Archival Unit.
   * 
   * Based on a method in TestLockssAuRepositoryImpl.
   */
  private ArchivalUnit createAu(File fileDirectory) throws LockssRepositoryException, FileNotFoundException {
    ArchivalUnit au;
    String strName;

    // gensym creates an AUID that's unique per run.
    // The combination of gensym and the current time should be unique
    // across runs.
    
    strName = StringUtil.gensym("AUID" + System.currentTimeMillis());    
    au = new MockArchivalUnit(strName);
                  
    return au;
  }

  /**
   * To be run at the (start and) end of every test: verify that no .lock file exists.
   */
  private void checkLockFile() {
    File fileLock;
    
    fileLock = new File(k_strDirectory + ".lock");
    assertFalse(".lock file was not removed.", fileLock.exists());
  }
}
