/*

 * $Id: TestCollectionOfAuRepositoriesImpl.java,v 1.1.2.2 2009-11-03 23:44:56 edwardsb1 Exp $
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
import org.lockss.repository.v2.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.PlatformUtil.DF;

import junit.framework.TestCase;

/**
 * @author edwardsb
 *
 */
public class TestCollectionOfAuRepositoriesImpl extends LockssTestCase {
  // Constants...
  private static final String k_dirNonexistent = "/ueato7au/htsdt/tsthndfeu/tsthsnheaou/tstuoehautsha/sseuthanseu/tsateuhons/teusateuhas/";  // This directory should not exist!
  private static final String k_dirSubdirectory = "subdirectory/";
  private static final long k_sizeWarcMax = 10000;
  private static final String k_stemFile = "stem";
  private static final String k_strAuid = "AUID";
  private static final String k_strDirectory = "TestCollectionOfAuRepositoriesImpl/";
  private static final String k_strPeerID = "TCP:[192.168.0.1]:9723";
  private static final String k_url = "http://www.example.com/";
  
  // Member variables
  private ArchivalUnit m_au1;
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
    
    jhrf.createRepositoryHelper("TestCollectionOfAuRepositoriesImpl", new File("TestCollectionOfAuRepositoriesImpl"));
    m_au1 = new MockArchivalUnit(k_strAuid);

  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    JcrRepositoryHelperFactory.reset();
    
    // This check verifies that the test shut down correctly.
    checkLockFile();
    
    // Clean up everything! 
    FileUtil.delTree(new File(k_strDirectory));
    
    super.tearDown();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.CollectionOfAuRepositoriesImpl#CollectionOfAuRepositoriesImpl(java.io.File)}.
   * All other tests will verify the methods of a COAR; this test examines
   * only the constructor's parameters. 
   */
  public final void testCollectionOfAuRepositoriesImpl() throws Exception {
    File fileDir;
    File fileFile;
    File fileNonexistent;
    
    // Test: Create a COAR with a nonexistent directory...
    fileNonexistent = new File(k_dirNonexistent);
    try {
      new CollectionOfAuRepositoriesImpl(fileNonexistent);
      fail("The nonexistent directory should have thrown an exception.");
    } catch (IOException e) {
      // Pass test!
    }
    
    // Test: Create a temp file (not a directory!) and try to put a COAR in it.
    fileFile = FileUtil.createTempFile("testCOARImpl", "foo");
    try {
      new CollectionOfAuRepositoriesImpl(fileFile);
      fail("Creating a COAR on a file should have thrown an exception.");
    } catch (IOException e) {
      // Pass test!
    }
    fileFile.delete();
    
    // Test: Create a temp directory with nothing in it, and try to put a COAR in it.
    fileDir = FileUtil.createTempDir("testCOARImpl", "dir");
    new CollectionOfAuRepositoriesImpl(fileDir);
    FileUtil.delTree(fileDir);
  }
  
  /**
   * Test method for {@link org.lockss.repository.jcr.CollectionOfAuRepositoriesImpl#generateAuRepository(java.io.File)}.
   */
  public final void testGenerateAuRepository() throws Exception {
    ArchivalUnit au;
    File dirTest;
    File fileDatastore;
    CollectionOfAuRepositoriesImpl jcTest;
    
    dirTest = FileUtil.createTempDir("test", "generateAuRepository");
    
    au = new MockArchivalUnit(k_strAuid);
    jcTest = new CollectionOfAuRepositoriesImpl(dirTest);
    jcTest.generateAuRepository(au, dirTest);
    
    // Verify it...
    fileDatastore = new File(dirTest, CollectionOfAuRepositoriesImpl.k_FILENAME_DATASTORE);
    assertTrue(fileDatastore.exists());
    
    // Delete everything.
    fileDatastore.delete();
    dirTest.delete();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.CollectionOfAuRepositoriesImpl#generateAuRepository(java.io.File)}.
   * This method was created independently of the above test, but they're both useful.
   */
  public final void testGenerateAuRepository2() throws Exception {
    CollectionOfAuRepositories coarGenerate;
    File fileGenerate;
    File fileFile;
    File fileUnused;
    File fileSubdirectory;
    
    fileGenerate = FileUtil.createTempDir("testGenerateAuRepository", null);
    coarGenerate = new CollectionOfAuRepositoriesImpl(fileGenerate);

    // Test: Verify that a null AU causes an error.
    try {
      coarGenerate.generateAuRepository(null, fileGenerate);
      fail("A null AU should have caused an exception.");
    } catch (LockssRepositoryException e) {
      // Pass test!
    }
    
    // Test: create an Au Repository with an unused directory.
    fileUnused = new File(k_dirNonexistent);
    try {
      coarGenerate.generateAuRepository(m_au1, fileUnused);
      fail("The nonexistent directory should have thrown an exception.");
    } catch (IOException e) {
      // Pass test!
    }
    
    // Test: Create a temp file (not a directory!) and try to put an Au Repository in it.
    fileFile = FileUtil.createTempFile("testCOARImpl", "foo");
    try {
      coarGenerate.generateAuRepository(m_au1, fileFile);
      fail("Creating a COAR on a file should have thrown an exception.");
    } catch (IOException e) {
      // Pass test!
    }
    fileFile.delete();

    // Test: Create a directory under fileGenerate, and finally create a real AU Repository.
    fileSubdirectory = new File(fileGenerate, k_dirSubdirectory);
    coarGenerate.generateAuRepository(m_au1, fileSubdirectory);
    assertTrue(fileSubdirectory.list().length > 0);
    
    // This command also deletes fileSubdirectory.
    FileUtil.delTree(fileGenerate);
  }

  

  
  
  /**
   * Test method for {@link org.lockss.repository.jcr.CollectionOfAuRepositoriesImpl#listAuRepositories(java.io.File)}.
   */
  public final void testListAuRepositories() throws Exception {
    ArchivalUnit au1;
    ArchivalUnit au2;
    File dir1;
    File dir2;
    File dirParent;
    CollectionOfAuRepositoriesImpl coarTest;
    List<String> lifilenameResult;
    
    // Create two directories that will have an AU
    dirParent = FileUtil.createTempDir("test", "listAuRepositories");
    
    dir1 = new File(dirParent, "child1");
    dir1.mkdir();        
    dir2 = new File(dirParent, "child2");
    dir2.mkdir();
    
    coarTest = new CollectionOfAuRepositoriesImpl(dirParent);
    au1 = new MockArchivalUnit("auid1");
    coarTest.generateAuRepository(au1, dir1);
    au2 = new MockArchivalUnit("auid2");
    coarTest.generateAuRepository(au2, dir2);
    
    // Setup is done.  Get the results.
    lifilenameResult = coarTest.listAuRepositories();
    
    assertTrue(lifilenameResult.contains("auid1"));
    assertTrue(lifilenameResult.contains("auid2"));
    
    // Clean up.
    dir2.delete();
    dir1.delete();
  }

  
  /**
   * Test CollectionOfAuRepositoriesImpl.openAuRepository()
   */
  public final void testOpenAuRepository() throws Exception {
    ArchivalUnit auGood;
    CollectionOfAuRepositoriesImpl coarTest;
    File dirTest;    
    File dirLocation;
    LockssAuRepository larTest;
    
    dirTest = FileUtil.createTempDir("test", "OpenAuRepository");
    coarTest = new CollectionOfAuRepositoriesImpl(dirTest);
    
    auGood = createAu(dirTest);    
    larTest = coarTest.openAuRepository(auGood);
    
    // TODO: Test the larTest.  I'm not sure what to do here.

    // Clean up.
    FileUtil.delTree(dirTest);
  }
  
  
  /**
   * Test CollectionOfAuRepositoriesImpl.getDF()
   */
  public final void testGetDF() throws Exception {
    DF df1;
    DF df2;
    File dirTest;
    File fileFiller;
    int i;
    CollectionOfAuRepositoriesImpl jcTest;
    OutputStream ostrFiller;
    
    
    dirTest = FileUtil.createTempDir("test", "getDF");
    jcTest = new CollectionOfAuRepositoriesImpl(dirTest);
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
