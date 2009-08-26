/**

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
import java.sql.*;
import java.util.*;

import javax.jcr.*;

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.config.*;
import org.apache.jackrabbit.core.data.*;
import org.lockss.app.*;
import org.lockss.plugin.*;
import org.lockss.poller.v3.V3Poller;
import org.lockss.protocol.*;
import org.lockss.protocol.IdentityManager.*;
import org.lockss.repository.*;
import org.lockss.repository.v2.*;
import org.lockss.repository.v2.RepositoryFile;
import org.lockss.repository.v2.RepositoryNode;
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import sun.security.action.GetLongAction;

import junit.framework.*;

/**
 * @author edwardsb
 *
 */
public class TestLockssAuRepositoryImpl extends LockssTestCase {
  // Constants
  private static final String k_nameXml = "LargeDatastore.xml";
  private static final String k_password = "password";
  private static final long k_sizeWarcMax = 10000;
  private static final String k_stemFile = "stem";
  private static final String k_strAuId = "AUID";
  private static final String k_strDirectory = "TestRepository/";
  private static final String k_strPeerID = "TCP:[192.168.0.1]:9723";
  private static final String k_url = "http://www.example.com/";
  private static final String k_urlGetFile = "http://www.example.com/cgi-bin?foo=bar";
  private static final String k_urlGetFileNoPath = "http://www.example3.com";
  private static final String k_urlGetFileNoQuery = "http://www.example3.com/cgi-bin";
  private static final String k_urlGetNode = "http://www.example2.com/directory/";
  private static final String k_username = "username";
  
  // Static variables
  private static Logger logger = Logger.getLoggerWithInitialLevel("TestLockssAuRepositoryImpl", 3);
  
  // Member variables
  private IdentityManager m_idman;
  private MockLockssDaemon m_ldTest;
  private RepositoryImpl m_repos;
  private Session m_session;
  
  /**
   * @param name
   */
  public TestLockssAuRepositoryImpl(String name) throws Exception {
    super(name);
    
  }

  /**
   * @see junit.framework.TestCase#setUp()
   * @throws java.lang.Exception
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    if (!LockssJackrabbitHelper.isConstructed()) {
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
       
      LockssJackrabbitHelper.preconstructor(k_strDirectory, k_nameXml, 
          k_strDirectory + "/" + k_stemFile, k_sizeWarcMax, k_url, m_idman, m_ldTest,
          k_strAuId);

      m_repos = LockssJackrabbitHelper.getRepository();
      m_session = LockssJackrabbitHelper.getSession();    
    } else { // isConstructed
      LockssJackrabbitHelper ljh;
      
      ljh = LockssJackrabbitHelper.constructor();
      
      m_ldTest = (MockLockssDaemon) ljh.getDaemon();
      m_idman = ljh.getIdentityManager();
      m_repos = LockssJackrabbitHelper.getRepository();
      m_session = LockssJackrabbitHelper.getSession();
    }
  }

  /**
   * @see junit.framework.TestCase#tearDown()
   * @throws java.lang.Exception
   */
  protected void tearDown() throws Exception {
    // We have only one repository.  The repository needs to be saved throughout 
    // the tests.  Therefore, we do not tear down the repository with every test.
    
//    DataStore ds;
//
//    if (m_repos != null) {
//      ds = m_repos.getDataStore();
//      if (ds != null) {
//        try {
//          ds.clearInUse();
//          ds.close();
//        } catch (DataStoreException e) {
//          e.printStackTrace();
//        }
//      }
//
//      m_repos.shutdown();
//      m_repos = null;
      
//      super.tearDown();
//    }

    // In order to shut down the Derby database, we need to do this...
    // See:
    // http://publib.boulder.ibm.com/infocenter/cldscp10/index.jsp?topic=/com.ibm.cloudscape.doc/develop15.htm

//    try {
//      DriverManager.getConnection("jdbc:derby:;shutdown=true");
//    } catch (SQLException e) {
      // From the documentation:

      // A successful shutdown always results in an SQLException to indicate
      // that Cloudscape [Derby]
      // has shut down and that there is no other exception.
//    }
    
    if (m_idman != null) {
      m_idman.stopService();
      m_idman = null;
    }

    System.gc();

    super.tearDown();
  }

  /**
   * All other tests in this class use the constructor.  This one just
   * verifies that the constructor works.
   *
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#LockssAuRepositoryImpl(org.lockss.plugin.ArchivalUnit)}.
   */
  public final void testLockssAuRepositoryImpl() throws Exception {
    ArchivalUnit au;
    
    au = new MockArchivalUnit("TestAu");
    new LockssAuRepositoryImpl(au);
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#getAuCreationTime()}.
   */
  public final void testGetAuCreationTime() throws Exception {
    ArchivalUnit au;
    LockssAuRepositoryImpl lari;
    long timeCreation;
    long timeEnd;
    long timeStart;
    
    timeStart = System.currentTimeMillis();
    
    au = createAu();
    lari = new LockssAuRepositoryImpl(au);
    
    timeCreation = lari.getAuCreationTime();
    timeEnd = System.currentTimeMillis();
    
    if (timeCreation < timeStart || timeCreation > timeEnd) {
      fail("Creation time is impossible.");
    }
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#getAuStateRawContents()}.
   */
  public final void testGetAuStateRawContents() throws Exception {
    ArchivalUnit auOther;
    ArchivalUnit auTest;
    AuState ausOther;
    AuState ausTest;
    InputStream istrAuState1;
    InputStream istrAuState2;
    InputStream istrAuState3;
    InputStream istrOther;
    LockssAuRepositoryImpl lariTest;
    String strAuOther;
    String strAuState1;
    String strAuState2;
    String strAuState3;
    
    // Entering the same AuState multiple times should give the same contents.
    auTest = createAu();
    ausTest = new MockAuState(auTest);
    
    lariTest = new LockssAuRepositoryImpl(auTest);
    
    // --- istrAuState1: store the Au State.
    lariTest.storeAuState(ausTest);    
    istrAuState1 = lariTest.getAuStateRawContents();
    strAuState1 = retrieveStream(istrAuState1);
    
    // --- istrAuState2: Reuse the same Au State (no set in-between)
    istrAuState2 = lariTest.getAuStateRawContents();
    strAuState2 = retrieveStream(istrAuState2);
    
    // -- istrAuState3: Store the Au state once more.
    lariTest.storeAuState(ausTest);
    istrAuState3 = lariTest.getAuStateRawContents();
    strAuState3 = retrieveStream(istrAuState3);
    
    assertEquals(strAuState1, strAuState2);
    assertEquals(strAuState1, strAuState3);
    
    // Entering a DIFFERENT AuState should give different contents.
    auOther = createAu();
    ausOther = new MockAuState(auOther);
    
    // -- istrOther: store the other state.
    lariTest.storeAuState(ausOther);
    istrOther = lariTest.getAuStateRawContents();
    strAuOther = retrieveStream(istrOther);
    
    assertNotSame(strAuState1, strAuOther);
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#getFile(java.lang.String, boolean)}.
   */
  
  public final void testGetFile() throws Exception {
    ArchivalUnit auTest;
    AuState ausTest;
    byte [] arbyContent;
    InputStream istrContent;
    Node nodeGetFile;
    LockssAuRepositoryImpl lariTest;
    RepositoryFile rfGetFile1;
    RepositoryFile rfGetFile2;
    RepositoryFile rfGetFile3;    
    RepositoryFileVersion rfvGetFile;
    
    // Generate a LockssAuRepository.
    auTest = createAu();
    ausTest = new MockAuState(auTest);
    
    lariTest = new LockssAuRepositoryImpl(auTest);
    
    // Get its associated RepositoryFile at a spot.
    rfGetFile1 = lariTest.getFile(k_urlGetFile, true);
    rfGetFile2 = lariTest.getFile(k_urlGetFile, false);
    
    assertEquals(rfGetFile1.getNodeUrl(), rfGetFile2.getNodeUrl());
    assertEquals(k_urlGetFile, rfGetFile1.getNodeUrl());
    
    // Add a version to the RepositoryFile, get another copy, and make sure
    // that the added file is in the RepositoryFile.
    
    rfvGetFile = rfGetFile1.createNewVersion();
    arbyContent = new byte[1];
    arbyContent[0] = 99;
    istrContent = new ByteArrayInputStream(arbyContent);
    rfvGetFile.setInputStream(istrContent);
    rfvGetFile.commit();
    rfGetFile1.setPreferredVersion(rfvGetFile);
    
    rfGetFile3 = lariTest.getFile(k_urlGetFile, false);
    
    assertEquals(rfGetFile3.getPreferredVersion(), rfvGetFile);
  }

  /**
   * Additional tests on getFile
   */
  public final void testGetFile2() throws Exception {
    ArchivalUnit auTest;
    AuState ausTest;
    byte [] arbyContent;
    InputStream istrContent;
    Node nodeGetFile;
    LockssAuRepositoryImpl lariTest;
    RepositoryFile rfGetFile1;
    RepositoryFile rfGetFile2;
    RepositoryFile rfGetFile3;    
    RepositoryFileVersion rfvGetFile;
    
    // Generate a LockssAuRepository.
    auTest = createAu();
    ausTest = new MockAuState(auTest);
    
    lariTest = new LockssAuRepositoryImpl(auTest);

    // TEST: GetFile with empty path, file, and query.
    
    // Get its associated RepositoryFile at a spot.
    rfGetFile1 = lariTest.getFile(k_urlGetFileNoPath, true);
    rfGetFile2 = lariTest.getFile(k_urlGetFileNoPath, false);
    
    assertEquals(rfGetFile1.getNodeUrl(), rfGetFile2.getNodeUrl());
    assertEquals(k_urlGetFileNoPath, rfGetFile1.getNodeUrl());
    
    // Add a version to the RepositoryFile, get another copy, and make sure
    // that the added file is in the RepositoryFile.
    
    rfvGetFile = rfGetFile1.createNewVersion();
    arbyContent = new byte[1];
    arbyContent[0] = 99;
    istrContent = new ByteArrayInputStream(arbyContent);
    rfvGetFile.setInputStream(istrContent);
    rfvGetFile.commit();
    rfGetFile1.setPreferredVersion(rfvGetFile);
    
    rfGetFile3 = lariTest.getFile(k_urlGetFileNoPath, false);
    
    assertEquals(rfGetFile3.getPreferredVersion(), rfvGetFile);

    // TEST: GetFile with empty query.
    // Get its associated RepositoryFile at a spot.
    rfGetFile1 = lariTest.getFile(k_urlGetFileNoQuery, true);
    rfGetFile2 = lariTest.getFile(k_urlGetFileNoQuery, false);
    
    assertEquals(rfGetFile1.getNodeUrl(), rfGetFile2.getNodeUrl());
    assertEquals(k_urlGetFileNoQuery, rfGetFile1.getNodeUrl());
    
    // Add a version to the RepositoryFile, get another copy, and make sure
    // that the added file is in the RepositoryFile.
    
    rfvGetFile = rfGetFile1.createNewVersion();
    arbyContent = new byte[1];
    arbyContent[0] = 99;
    istrContent = new ByteArrayInputStream(arbyContent);
    rfvGetFile.setInputStream(istrContent);
    rfvGetFile.commit();
    rfGetFile1.setPreferredVersion(rfvGetFile);
    
    rfGetFile3 = lariTest.getFile(k_urlGetFileNoQuery, false);
    
    assertEquals(rfGetFile3.getPreferredVersion(), rfvGetFile);

  }
  
  /**
   * 
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#getIdentityAgreementRawContents()}.
   */
  public final void testGetIdentityAgreementRawContents() throws Exception {
    ArchivalUnit auTest;
    IdentityAgreement idag1;
    IdentityAgreement idag2;
    InputStream istrIdentityAgreement1;
    InputStream istrIdentityAgreement2;
    InputStream istrIdentityAgreement3;
    LockssAuRepositoryImpl lariTest;
    List<IdentityAgreement> liidag1;
    List<IdentityAgreement> liidag2;
    MockPeerIdentity mpid1;
    MockPeerIdentity mpid2;
    String strIdentityAgreement1;
    String strIdentityAgreement2;
    String strIdentityAgreement3;
    
    // Generate a LockssAuRepository.
    auTest = createAu();
    
    lariTest = new LockssAuRepositoryImpl(auTest);
    
    // Create and store one identity agreement.
    mpid1 = new MockPeerIdentity("foobar1");
    idag1 = new IdentityManager.IdentityAgreement(mpid1);
    
    liidag1 = new ArrayList<IdentityAgreement>();
    liidag1.add(idag1);
    
    lariTest.storeIdentityAgreements(liidag1);
    
    // Get the identity agreement twice, and compare.
    istrIdentityAgreement1 = lariTest.getIdentityAgreementRawContents();
    strIdentityAgreement1 = retrieveStream(istrIdentityAgreement1);
    
    istrIdentityAgreement2 = lariTest.getIdentityAgreementRawContents();
    strIdentityAgreement2 = retrieveStream(istrIdentityAgreement2);
    
    assertEquals(strIdentityAgreement1, strIdentityAgreement2);
    
    // Set a second identity agreement.
    mpid2 = new MockPeerIdentity("yarf2");
    idag2 = new IdentityManager.IdentityAgreement(mpid2);
    
    liidag2 = new ArrayList<IdentityAgreement>();
    liidag2.add(idag2);
    
    lariTest.storeIdentityAgreements(liidag2);
    
    // Make sure that a different identity agreement is not the same as the first.
    istrIdentityAgreement3 = lariTest.getIdentityAgreementRawContents();
    strIdentityAgreement3 = retrieveStream(istrIdentityAgreement3);
    
    assertNotSame(strIdentityAgreement1, strIdentityAgreement3);
  }

  
  /**
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#getNoAuPeerSet()}.
   */
  public final void testGetNoAuPeerSet() throws Exception {
    // Changes to the DatedPeerIdSet should be passed among different copies
    // of the DatedPeerIdSet.
    
    ArchivalUnit auTest;
    DatedPeerIdSet dpis1;
    DatedPeerIdSet dpis2;
    LockssAuRepositoryImpl lariTest;
    PeerIdentity mpid;

    // Create the LockssAuRepositoryImpl
    auTest = createAu();    
    lariTest = new LockssAuRepositoryImpl(auTest);
    
    // Insert info into the DatedPeerIdSet.
    mpid = m_idman.findPeerIdentity(k_strPeerID);
    
    dpis1 = lariTest.getNoAuPeerSet();    
    dpis1.add(mpid);
    dpis1.store(true);
    
    // Get another DatedPeerIdSet.
    dpis2 = lariTest.getNoAuPeerSet();
        
    assertTrue(dpis2.contains(mpid));
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#getNode(java.lang.String, boolean)}.
   */
  public final void testGetNode() throws Exception {
    ArchivalUnit auTest;
    LockssAuRepositoryImpl lariTest;
    RepositoryNode rnGetFile1;
    RepositoryNode rnGetFile2;
    RepositoryNode rnGetFile3;    
    RepositoryNode rnNewAddition;
    
    // Generate a LockssAuRepository.
    auTest = createAu();
    lariTest = new LockssAuRepositoryImpl(auTest);

    // Get its associated RepositoryNode at a spot.
    rnGetFile1 = lariTest.getNode(k_urlGetNode, true);
    rnGetFile2 = lariTest.getNode(k_urlGetNode, false);
    
    assertEquals(rnGetFile1.getNodeUrl(), rnGetFile2.getNodeUrl());
    assertEquals(rnGetFile1.getNodeUrl(), k_urlGetNode);
    
    // Add a version to the RepositoryFile, get another copy, and make sure
    // that the added file is in the RepositoryFile.
    
    rnNewAddition = rnGetFile1.makeNewRepositoryNode("test1");
    m_session.save();
    
    rnGetFile3 = lariTest.getNode(k_urlGetNode, false);    
    assertEquals(rnGetFile3.getChildCount(), 1);
  }

  
  public final void testGetPersistentPeerIdRawContents() throws Exception {
    
  }
  
  /**
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#loadAuState()}.
   */
  public final void testLoadAuState() throws Exception {
    ArchivalUnit auTest;
    AuState ausOriginal;
    AuState ausRetrieved1;
    AuState ausRetrieved2;
    LockssAuRepositoryImpl lariTest;
    
    auTest = createAu();
    lariTest = new LockssAuRepositoryImpl(auTest);
    
    // Save an AU state, and retrieve it.
    ausOriginal = new MockAuState(auTest);

    lariTest.storeAuState(ausOriginal);
    ausRetrieved1 = lariTest.loadAuState();
    ausRetrieved2 = lariTest.loadAuState();
    
    // The following lines can't be used: the AuStates are clones,
    // not identical.
    
    // assertEquals(ausOriginal, ausRetrieved1);
    // assertEquals(ausOriginal, ausRetrieved2);
    
    assertEquals(ausOriginal.getLastCrawlAttempt(), ausRetrieved1.getLastCrawlAttempt());
    assertEquals(ausOriginal.getLastPollAttempt(), ausRetrieved1.getLastPollAttempt());

    assertEquals(ausOriginal.getLastCrawlAttempt(), ausRetrieved2.getLastCrawlAttempt());
    assertEquals(ausOriginal.getLastPollAttempt(), ausRetrieved2.getLastPollAttempt());

  }

  
  /**
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#loadIdentityAgreements()}.
   */
  public final void testLoadIdentityAgreements() throws Exception {
    ArchivalUnit auTest;
    IdentityManager.IdentityAgreement idag;
    List<IdentityManager.IdentityAgreement> liidag;
    List<IdentityManager.IdentityAgreement> liidagReturned;
    LockssAuRepositoryImpl lariTest;
    PeerIdentity pid;
    
    auTest = createAu();
    lariTest = new LockssAuRepositoryImpl(auTest);
    
    // Create a list of identity agreements.
    pid = new MockPeerIdentity(k_strPeerID);
    idag = new IdentityAgreement(pid);
    
    liidag = new ArrayList<IdentityManager.IdentityAgreement>();
    liidag.add(idag);
    
    // Set it, retrieve it, and verify it.
    lariTest.storeIdentityAgreements(liidag);
    liidagReturned = lariTest.loadIdentityAgreements();
    
    assertEquals(liidagReturned.size(), 1);
    assertEquals(liidagReturned.get(0), idag);
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#queueSizeCalc(org.lockss.repository.v2.RepositoryNode)}.
   */
  public final void testQueueSizeCalcRepositoryNode() {
    // This method will be difficult to test.
    // fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#storeAuStateRawContents(java.io.InputStream)}.
   */
  public final void testStoreAuStateRawContents() throws Exception {
    ArchivalUnit auTest;
    ArchivalUnit auTest2;
    AuState ausOriginal1;
    AuState ausOriginal2;
    AuState ausRetrieved;
    InputStream istrAuState;
    InputStream istrAuState2;
    LockssAuRepositoryImpl lariTest;
    String strAuState;
    
    auTest = createAu();
    lariTest = new LockssAuRepositoryImpl(auTest);
    
    // Store an Au State, and retrieve the raw contents.
    ausOriginal1 = new MockAuState(auTest);

    lariTest.storeAuState(ausOriginal1);
    istrAuState = lariTest.getAuStateRawContents();
    strAuState = retrieveStream(istrAuState);
    
    // Store a different Au State.
    auTest2 = createAu();
    ausOriginal2 = new MockAuState(auTest2);
    lariTest.storeAuState(ausOriginal2);
    
    // Use StoreAuStateRawContents.  Verify that the Au State is the first one.
    istrAuState2 = new ByteArrayInputStream(strAuState.getBytes());
    lariTest.storeAuStateRawContents(istrAuState2);
    ausRetrieved = lariTest.loadAuState();
    
    // The following lines can't be used: the AuStates are clones,
    // not identical.

    // assertEquals(ausRetrieved, ausOriginal1);
    
    assertEquals(ausOriginal1.getLastCrawlAttempt(), ausRetrieved.getLastCrawlAttempt());
    assertEquals(ausOriginal1.getLastPollAttempt(), ausRetrieved.getLastPollAttempt());

  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#storeIdentityAgreementsRawContents(java.io.InputStream)}.
   */
  public final void testStoreIdentityAgreementsRawContents() throws Exception {
    ArchivalUnit auTest;
    IdentityManager.IdentityAgreement idagreeOriginal1;
    IdentityManager.IdentityAgreement idagreeOriginal2;
    InputStream istrIdentityAgreements1;
    InputStream istrIdentityAgreements2;
    List<IdentityManager.IdentityAgreement> liidagreeOriginal1;
    List<IdentityManager.IdentityAgreement> liidagreeOriginal2;
    List<IdentityManager.IdentityAgreement> liidagreeReceived1;
    List<IdentityManager.IdentityAgreement> liidagreeReceived2;
    LockssAuRepositoryImpl lariTest;
    PeerIdentity pid1;
    PeerIdentity pid2;
    String strIdentityAgreements;
    
    auTest = createAu();
    lariTest = new LockssAuRepositoryImpl(auTest);
    
    // Store an Identity Agreement, and retrieve the raw contents.
    pid1 = new MockPeerIdentity(k_strPeerID);
    idagreeOriginal1 = new IdentityManager.IdentityAgreement(pid1);
    liidagreeOriginal1 = new ArrayList<IdentityAgreement>();
    liidagreeOriginal1.add(idagreeOriginal1);
    
    lariTest.storeIdentityAgreements(liidagreeOriginal1);
    istrIdentityAgreements1 = lariTest.getIdentityAgreementRawContents();
    strIdentityAgreements = retrieveStream(istrIdentityAgreements1);
        
    // Store a different Identity Agreement.
    pid2 = new MockPeerIdentity("TCP:[127.0.0.1]:9723");
    idagreeOriginal2 = new IdentityManager.IdentityAgreement(pid2);
    liidagreeOriginal2 = new ArrayList<IdentityAgreement>();
    liidagreeOriginal2.add(idagreeOriginal2);
    
    lariTest.storeIdentityAgreements(liidagreeOriginal2);
    
    // Use StoreIdentityAgreementsRawContents.  Verify that the Identity Agreement is the first one.
    istrIdentityAgreements1 = new ByteArrayInputStream(strIdentityAgreements.getBytes());
    lariTest.storeIdentityAgreementsRawContents(istrIdentityAgreements1);
    liidagreeReceived2 = lariTest.loadIdentityAgreements();
    
    assertEquals(liidagreeReceived2.size(), 1);
    assertEquals(liidagreeReceived2.get(0).getId(), idagreeOriginal1.getId());
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#storeAuState(org.lockss.state.AuState)}.
   */
  public final void testStoreAuState() throws Exception {
    ArchivalUnit auTest;
    ArchivalUnit auTest2;
    AuState ausOriginal1;
    AuState ausOriginal2;
    AuState ausRetrieved;
    LockssAuRepositoryImpl lariTest;
    
    auTest = createAu();
    lariTest = new LockssAuRepositoryImpl(auTest);
    
    // Store an Au State, and retrieve the raw contents.
    ausOriginal1 = new MockAuState(auTest);

    lariTest.storeAuState(ausOriginal1);
    ausRetrieved = lariTest.loadAuState();

    // The following lines can't be used: the AuStates are clones,
    // not identical.

    // assertEquals(ausOriginal1, ausRetrieved);

    assertEquals(ausOriginal1.getLastCrawlAttempt(), ausRetrieved.getLastCrawlAttempt());
    assertEquals(ausOriginal1.getLastPollAttempt(), ausRetrieved.getLastPollAttempt());

    // Store a different Au State.
    auTest2 = createAu();
    ausOriginal2 = new MockAuState(auTest2);
    lariTest.storeAuState(ausOriginal2);
    ausRetrieved = lariTest.loadAuState();
    // assertEquals(ausOriginal2, ausRetrieved);
    
    assertEquals(ausOriginal2.getLastCrawlAttempt(), ausRetrieved.getLastCrawlAttempt());
    assertEquals(ausOriginal2.getLastPollAttempt(), ausRetrieved.getLastPollAttempt());

  }

  
  /**
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#storeIdentityAgreements(java.util.List)}.
   */
  public final void testStoreIdentityAgreements() throws Exception {
    ArchivalUnit auTest;
    IdentityManager.IdentityAgreement idagreeOriginal1;
    IdentityManager.IdentityAgreement idagreeOriginal2;
    List<IdentityManager.IdentityAgreement> liidagreeOriginal1;
    List<IdentityManager.IdentityAgreement> liidagreeOriginal2;
    List<IdentityManager.IdentityAgreement> liidagreeReceived1;
    List<IdentityManager.IdentityAgreement> liidagreeReceived2;
    LockssAuRepositoryImpl lariTest;
    PeerIdentity pid1;
    PeerIdentity pid2;
    
    auTest = createAu();
    lariTest = new LockssAuRepositoryImpl(auTest);
    
    // Store an Identity Agreement, and retrieve the raw contents.
    pid1 = new MockPeerIdentity(k_strPeerID);
    idagreeOriginal1 = new IdentityManager.IdentityAgreement(pid1);
    liidagreeOriginal1 = new ArrayList<IdentityAgreement>();
    liidagreeOriginal1.add(idagreeOriginal1);
    
    lariTest.storeIdentityAgreements(liidagreeOriginal1);
    liidagreeReceived1 = lariTest.loadIdentityAgreements();

    assertEquals(liidagreeReceived1.size(), 1);
    assertEquals(liidagreeReceived1.get(0), idagreeOriginal1);

    // Store a different Identity Agreement.
    pid2 = new MockPeerIdentity("TCP:[127.0.0.1]:9723");
    idagreeOriginal2 = new IdentityManager.IdentityAgreement(pid2);
    liidagreeOriginal2 = new ArrayList<IdentityAgreement>();
    liidagreeOriginal2.add(idagreeOriginal2);
    
    lariTest.storeIdentityAgreements(liidagreeOriginal2);
    liidagreeReceived2 = lariTest.loadIdentityAgreements();
    
    assertEquals(liidagreeReceived2.size(), 1);
    assertEquals(liidagreeReceived2.get(0), idagreeOriginal2);
  }
  
  /**
   * Test method for {@link org.lockss.repository.jcr.LockssAuRepositoryImpl#getRepoDiskUsage}
   */
  public final void testGetRepoDiskUsage() throws Exception {
    ArchivalUnit auTest;
    LockssAuRepositoryImpl lariTest;
    long lDiskAfter;
    long lDiskBefore;
    
    auTest = createAu();
    lariTest = new LockssAuRepositoryImpl(auTest);

    lDiskBefore = lariTest.getRepoDiskUsage(true);
    
    // TODO: I don't have a good idea for a test...
  }
  
  
  
  /***********************
   * Private methods, useful for the above methods.
   */
  
  /**
   * @return a new Archival Unit.
   */
  private ArchivalUnit createAu() throws LockssRepositoryException, FileNotFoundException {
    ArchivalUnit au;
    String strName;

    // gensym creates an AUID that's unique per run.
    // The combination of gensym and the current time should be unique
    // across runs.
    
    strName = StringUtil.gensym("AUID" + System.currentTimeMillis());    
    au = new MockArchivalUnit(strName);
    
    // So that the requests have a place to go in the helper.
    LockssJackrabbitHelper.addRepository(au.getAuId(), k_strDirectory);
      
        
    return au;
  }

  /**
   * @param istr
   * @throws IOException
   */
  private String retrieveStream(InputStream istr) throws IOException {
    int inChar;
    StringBuilder sbReturn;
    
    sbReturn = new StringBuilder();
    for (;;) {  // FOREVER
      inChar = istr.read();
      if (inChar < 0) {
        break;  // out of the FOREVER loop
      }
      
      sbReturn.append((char) inChar);
    }
    
    istr.close();
    
    return sbReturn.toString();
  }

  /**
   * @param istr
   * @throws IOException
   */
  private void storeStream(OutputStream istr, String str) throws IOException {
    int inChar;
    
    for (inChar = 0; inChar < str.length(); inChar++) {
      istr.write((byte) str.charAt(inChar)); 
    }
  }

}
