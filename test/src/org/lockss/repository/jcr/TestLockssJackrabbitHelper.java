/**
 * 
 */
package org.lockss.repository.jcr;

import java.io.*;
import java.util.*;

import javax.jcr.*;

import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.poller.v3.V3Poller;
import org.lockss.protocol.IdentityManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.repository.v2.*;
import org.lockss.repository.v2.RepositoryFile;
import org.lockss.repository.v2.RepositoryNode;
import org.lockss.test.*;
import org.lockss.util.*;

import junit.framework.TestCase;

/**
 * @author edwardsb
 *
 */
public class TestLockssJackrabbitHelper extends LockssTestCase {

  // Constants
  private static final String k_auId = "AUID225";
  private static final String k_nameXml = "LargeDatastore.xml";
  private static final String k_nodeTest = "test1";
  private static final long k_sizeWarcMax = 10000;
  private static final long k_sizeWarcMax2 = 55555;
  private static final String k_sizestrWarcMax = "10000";
  private static final String k_sizestrWarcMax2 = "55555";
  private static final String k_stemFile = "stem";
  private static final String k_stemFile2 = "stemX";
  private static final String k_strAuId = "AUID";
  private static final String k_strDirectory = "TestRepository/";
  private static final String k_url = "http://www.example.com/";


  // Member variables...
  private Random m_rand;
  private IdentityManager m_idman;
  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    m_rand = new Random();
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssJackrabbitHelper#preconstructor(java.lang.String, java.lang.String, java.lang.String, long, java.lang.String, org.lockss.protocol.IdentityManager, org.lockss.app.LockssDaemon)}.
   */
  public final void testPreconstructor() throws Exception {
    LockssJackrabbitHelper ljhTest;
    long sizeWarcMax;
    String stemFile;
    String strUrl;
    
    try {
      // Test that what's entered is what's returned.
      stemFile = StringUtil.gensym("stemFile");
      sizeWarcMax = Math.abs(m_rand.nextLong());
      strUrl = StringUtil.gensym("url");
      
      callPreconstructor(stemFile, sizeWarcMax, strUrl);
      
      ljhTest = LockssJackrabbitHelper.constructor();
      
      assertEquals(stemFile, ljhTest.getStemFile());
      assertEquals(sizeWarcMax, ljhTest.getSizeWarcMax());
  
      // Test: Make sure that calling the preconstructor twice 
      // changes nothing.
      
      LockssJackrabbitHelper.preconstructor("badDirectory", "badFilename", 
          "badStemfile", 1, "badUrl", null, null, k_strAuId);
  
      assertEquals(stemFile, ljhTest.getStemFile());
      assertEquals(sizeWarcMax, ljhTest.getSizeWarcMax());
    } finally {
      LockssJackrabbitHelper.reset();
    }
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssJackrabbitHelper#isConstructed()}.
   */
  public final void testIsConstructed() throws Exception {
    try {
      // Reseting the constructor turns off "isConstructed".
      LockssJackrabbitHelper.reset();
      assertFalse(LockssJackrabbitHelper.isConstructed());
      
      callPreconstructor(k_stemFile, k_sizeWarcMax, k_url);
      assertTrue(LockssJackrabbitHelper.isConstructed());
    } finally {
      LockssJackrabbitHelper.reset();
    }
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssJackrabbitHelper#getRepositoryNode(java.lang.String)}.
   */
  public final void testGetRepositoryNode() throws Exception {
    LockssJackrabbitHelper ljh;
    RepositoryNode rnTest1;
    RepositoryNode rnTest2;
    
    try {
      callPreconstructor(k_stemFile, k_sizeWarcMax, k_url);
      ljh = LockssJackrabbitHelper.constructor();
      
      // This line is now required: the initial string (before a /)
      // determines where repository nodes are set.
      ljh.addRepository(k_nodeTest, k_strDirectory);
      
      // Test that we can get a node.
      rnTest1 = ljh.getRepositoryNode(k_nodeTest);      
      rnTest2 = ljh.getRepositoryNode(k_nodeTest);
      
      // Verify that they seem to be the same.
      assertTrue(rnTest1.equals(rnTest2));
    } finally {
      LockssJackrabbitHelper.reset();
    }
    
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssJackrabbitHelper#getRepositoryConfig()}.
   */
  public final void testGetRepositoryConfig() throws Exception {
    RepositoryConfig repconfig;
    
    // Verify that the repository config set is the repository config retrieved.
    try {
      callPreconstructor(k_stemFile, k_sizeWarcMax, k_url);
      repconfig = LockssJackrabbitHelper.getRepositoryConfig();
      
      assertEquals(k_strDirectory, repconfig.getHomeDir());
    } finally {
      LockssJackrabbitHelper.reset();
    }
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssJackrabbitHelper#getRootNode(java.lang.String)}.
   */
  public final void testGetRootNode() throws Exception {
    LockssJackrabbitHelper ljh;
    Node rnTest1;
    Node rnTest2;
    
    try {
      callPreconstructor(k_stemFile, k_sizeWarcMax, k_url);
      ljh = LockssJackrabbitHelper.constructor();
      
      // Test that we can get a node.
      rnTest1 = ljh.getRootNode(k_auId);      
      rnTest2 = ljh.getRootNode(k_auId);
      
      // Verify that they seem to be the same.
      assertTrue(rnTest1.isSame(rnTest2));
    } finally {
      LockssJackrabbitHelper.reset();
    }
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssJackrabbitHelper#getSession()}.
   */
  public final void testGetSession() throws Exception {
    Session session;
    
    try {
      callPreconstructor(k_stemFile, k_sizeWarcMax, k_url);
      
      session = LockssJackrabbitHelper.getSession();
      assertEquals(LockssJackrabbitHelper.k_USERNAME, session.getUserID());
    } finally {
      LockssJackrabbitHelper.reset();
    }

  }

  /**
   * Test method for {@link org.lockss.repository.jcr.LockssJackrabbitHelper#getStemFile()}.
   */
  public final void testGetStemFile() throws Exception {
    LockssJackrabbitHelper ljh;
    String sfTest;
    
    try {
      callPreconstructor(k_stemFile, k_sizeWarcMax, k_url);
      ljh = LockssJackrabbitHelper.constructor();
      
      // Test that we can get a node.
      sfTest = ljh.getStemFile();
      
      // Verify that they seem to be the same.
      assertEquals(k_stemFile, sfTest);
    } finally {
      LockssJackrabbitHelper.reset();
    }
  }

  
  private final String k_strMove = "/tmp/stemfileTestMoveC";
  
  public final void testMove() throws Exception {
    InputStream istrContent;
    List<RepositoryFileVersion> lirfvReturned;
    LockssJackrabbitHelper ljh;
    Node nodeTestMove;
    RepositoryFile rfTestMove;
    RepositoryFileVersion rfvTestMove1;
    RepositoryFileVersion rfvTestMove2;
    RepositoryNode rnTestMove;
    String strText1;
    String strText2;
    
    try {
      callPreconstructor(k_stemFile, k_sizeWarcMax, k_url);
      ljh = LockssJackrabbitHelper.constructor();
      LockssJackrabbitHelper.addRepository(k_nodeTest, k_strDirectory);
      
      rnTestMove = ljh.getRepositoryNode(k_nodeTest);
        
      rfTestMove = rnTestMove.makeNewRepositoryFile("TestMove");
      
      // Create two nodes with random text.
      strText1 = createRandomText(256);
      istrContent = new ByteArrayInputStream(strText1.getBytes());
      
      rfvTestMove1 = rfTestMove.createNewVersion();
      rfvTestMove1.setInputStream(istrContent);
      rfvTestMove1.commit();
      
      strText2 = createRandomText(256);
      istrContent = new ByteArrayInputStream(strText2.getBytes());
      
      rfvTestMove2 = rfTestMove.createNewVersion();
      rfvTestMove2.setInputStream(istrContent);
      rfvTestMove2.commit();
      
      // Move the parent node.
      LockssJackrabbitHelper.moveRepository(k_nodeTest, k_strDirectory);
      
      // Verify that we can still read the nodes.
      // Is it a bug that we're matching strText2 against version 0,
      // and strText1 against version 1?  Nope.  Remember that the
      // list is first in, first out.
      lirfvReturned = rfTestMove.listVersions();
      istrContent = new ByteArrayInputStream(strText2.getBytes());
      assertTrue(StreamUtil.compare(lirfvReturned.get(0).getInputStream(), 
          istrContent));
      
      istrContent = new ByteArrayInputStream(strText1.getBytes());
      assertTrue(StreamUtil.compare(lirfvReturned.get(1).getInputStream(), 
          istrContent));
    } finally {
      LockssJackrabbitHelper.reset();
    }
  }

  
  /**
   * Test method for {@link org.lockss.repository.jcr.LockssJackrabbitHelper#setConfig(org.lockss.config.Configuration, org.lockss.config.Configuration, org.lockss.config.Configuration.Differences)}.
   */
  public final void testSetConfig() throws Exception {
    Configuration conf1;
    Configuration conf2;
    Configuration.Differences confdiff;
    LockssJackrabbitHelper ljh;
    Properties props1;
    Properties props2;
    
    try {
      callPreconstructor(k_stemFile, k_sizeWarcMax, k_url);

      props1 = new PropertyTree();
      props1.put(LockssJackrabbitHelper.k_STEM_FILE, k_stemFile);
      props1.put(LockssJackrabbitHelper.k_SIZE_WARC_MAX, k_sizestrWarcMax);
      conf1 = ConfigurationUtil.fromProps(props1);
      
      props2 = new PropertyTree();
      props2.put(LockssJackrabbitHelper.k_STEM_FILE, k_stemFile2);
      props2.put(LockssJackrabbitHelper.k_SIZE_WARC_MAX, k_sizestrWarcMax2);
      conf2 = ConfigurationUtil.fromProps(props2);
      
      confdiff = conf2.differences(conf1);
      
      ljh = LockssJackrabbitHelper.constructor();
      ljh.setConfig(conf2, conf1, confdiff);
      
      assertEquals(ljh.getStemFile(), k_stemFile2);
      assertEquals(ljh.getSizeWarcMax(), k_sizeWarcMax2);
    } finally {
      LockssJackrabbitHelper.reset();
    }
  }
  
  // Private, helper methods...
  
  private void callPreconstructor(String stemFile, long sizeWarcMax,
      String strUrl) throws Exception {
    MockLockssDaemon ldTest;

    ldTest = getMockLockssDaemon();
    ldTest.startDaemon();
  
    // Taken from org.lockss.poller.v3.TestBlockTally
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, k_strDirectory + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, k_strDirectory);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(V3Poller.PARAM_V3_VOTE_MARGIN, "73");
    p.setProperty(V3Poller.PARAM_V3_TRUSTED_WEIGHT, "300");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    
    m_idman = ldTest.getIdentityManager();
    m_idman.startService();
    
    LockssJackrabbitHelper.preconstructor(k_strDirectory, k_nameXml, 
        stemFile, sizeWarcMax, strUrl, m_idman, ldTest, k_strAuId);
  }
  
  
  /**
   * @param sbUrl
   * @return
   */
  private String createRandomText(int lenUrl) {
    StringBuffer sbUrl;
    int countChar;
    
    sbUrl = new StringBuffer();
    for (countChar = 0; countChar < lenUrl; countChar++) {
      sbUrl.append((char) (m_rand.nextInt(26) + 'A'));
    }
    
    return sbUrl.toString();
  }
}
