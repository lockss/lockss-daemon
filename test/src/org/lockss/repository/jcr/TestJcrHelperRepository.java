/*
 * $Id: TestJcrHelperRepository.java,v 1.1.2.2 2009-09-30 23:02:33 edwardsb1 Exp $
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
import java.sql.*;
import java.util.Properties;

import javax.jcr.*;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.data.*;
import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.poller.v3.V3Poller;
import org.lockss.protocol.IdentityManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.repository.v2.RepositoryNode;
import org.lockss.test.*;
import org.lockss.util.*;

import junit.framework.TestCase;

/**
 * @author edwardsb
 *
 */
public class TestJcrHelperRepository extends LockssTestCase {

  // Constants
  private static final File k_directory = new File("TestJcrHelperRepository/");
  private static final String k_dirXml = "test/src/org/lockss/repository/jcr/TestRepository/";
  private static final String k_nameXml = "LargeDatastore.xml";
  private static final String k_password = "password";
  private static final long k_sizeWarcMax = 50000;
  private static final String k_sizestrWarcMax = "50000";
  private static final long k_sizeWarcMaxChanged = 30000;
  private static final String k_sizestrWarcMaxChanged = "30000";
  private static final String k_stemFile = "stemFile";
  private static final String k_stemFileMoved = "stemFileMoved";
  private static final String k_strAuId = "AUID";
  private static final String k_strUrl = "http://www.example.com/foo.html?bar=baz";
  private static final String k_username = "username";

  // Variables
  private IdentityManager m_idman;
  private MockLockssDaemon m_ldTest;
  private Node m_nodeRoot;
  private RepositoryImpl m_repos;
  private Session m_session;

  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    RepositoryConfig repconfig;
    JcrRepositoryHelperFactory jhrf;

    m_ldTest = getMockLockssDaemon();
    m_ldTest.startDaemon();
  
    // Taken from org.lockss.poller.v3.TestBlockTally
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, k_directory + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, k_directory.toString());
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(V3Poller.PARAM_V3_VOTE_MARGIN, "73");
    p.setProperty(V3Poller.PARAM_V3_TRUSTED_WEIGHT, "300");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    
    m_idman = m_ldTest.getIdentityManager();
    m_idman.startService();
    
    repconfig = RepositoryConfig.create(k_dirXml + k_nameXml,
        k_dirXml);
    m_repos = RepositoryImpl.create(repconfig);
    m_session = m_repos.login(new SimpleCredentials(k_username, k_password
        .toCharArray()));
    m_nodeRoot = m_session.getRootNode();

    JcrRepositoryHelperFactory.preconstructor(k_sizeWarcMax, m_idman, m_ldTest);    
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {    
    DataStore ds;
    
    if (m_session != null) {
      m_session.save();
      m_session.logout();
    }
        
    if (m_repos != null) {
      ds = m_repos.getDataStore();
      if (ds != null) {
        try {
          ds.clearInUse();
          ds.close();
        } catch (DataStoreException e) {
          e.printStackTrace();
        }
      }

      m_repos.shutdown();       
      m_repos = null;
    }
    
    // In order to shut down the Derby database, we need to do this...
    // See:
    // http://publib.boulder.ibm.com/infocenter/cldscp10/index.jsp?topic=/com.ibm.cloudscape.doc/develop15.htm

    try {
      DriverManager.getConnection("jdbc:derby:;shutdown=true");
    } catch (SQLException e) {
      // From the documentation:

      // A successful shutdown always results in an SQLException to indicate
      // that Cloudscape [Derby]
      // has shut down and that there is no other exception.
    }
    
    JcrRepositoryHelperFactory.reset();

    checkLockFile();
    
    FileUtil.delTree(k_directory);

    super.tearDown();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.JcrRepositoryHelper#JcrHelperRepository(java.lang.String, long, org.lockss.protocol.IdentityManager, org.lockss.app.LockssDaemon)}.
   */
  public final void testJcrRepositoryHelper() throws Exception {
    JcrRepositoryHelper jhrTest = null;
  
    // Other tests will run with the helper repository after the constructor.
    jhrTest = new JcrRepositoryHelper(k_directory, k_sizeWarcMax, m_idman, m_ldTest);
    jhrTest.reset();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.JcrRepositoryHelper#getIdentityManager()}.
   */
  public final void testGetIdentityManager() throws Exception {
    JcrRepositoryHelper jhrTest = null;
    IdentityManager idmanTest;
    
    jhrTest = new JcrRepositoryHelper(k_directory, k_sizeWarcMax, m_idman, m_ldTest);
    idmanTest = jhrTest.getIdentityManager();
    
    // Even if they're different objects, this should test equality.
    assertEquals(m_idman.hashCode(), idmanTest.hashCode());
    jhrTest.reset();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.JcrRepositoryHelper#getRepositoryConfig()}.
   */
  public final void testGetRepositoryConfig() throws Exception {
    JcrRepositoryHelper jhrTest = null;
    RepositoryConfig rcTest;

    jhrTest = new JcrRepositoryHelper(k_directory, k_sizeWarcMax, m_idman, m_ldTest);
    rcTest = jhrTest.getRepositoryConfig();
    
    assertTrue(rcTest.getHomeDir().contains(k_directory.getName()));
    jhrTest.reset();
  }
  
  
  /**
   * Test method for {@link org.lockss.repository.jcr.JcrRepositoryHelper#getRepositoryNode()}.
   */
  public final void testGetRepositoryNode() throws Exception {
    JcrRepositoryHelper jhrTest;
    JcrRepositoryHelperFactory jhrf;
    RepositoryNode rn;
    
    jhrf = JcrRepositoryHelperFactory.getSingleton();
    
    jhrTest = jhrf.createHelperRepository("test", k_directory);
    
    assertNull(jhrTest.getRepositoryNode("foobar"));
    
    rn = new RepositoryNodeImpl(m_session, m_nodeRoot, k_stemFile, k_sizeWarcMax, 
        k_strUrl, m_idman);
    jhrTest.addRepositoryNode("foobar", rn);
    
    assertNotNull(jhrTest.getRepositoryNode("foobar"));
    
    jhrTest.reset();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.JcrRepositoryHelper#getRootNode(java.lang.String)}.
   */
  public final void testGetRootNode() throws Exception {
    JcrRepositoryHelper jhrTest = null;
    Node nodeRoot;
    
    jhrTest = new JcrRepositoryHelper(k_directory, k_sizeWarcMax, m_idman, m_ldTest);
    nodeRoot = jhrTest.getRootNode();
    
    // I'm not sure what to test about the node...

    jhrTest.reset();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.JcrRepositoryHelper#getSession()}.
   */
  public final void testGetSession() throws Exception {
    JcrRepositoryHelper jhrTest = null;
    Session sessionRoot;
    
    jhrTest = new JcrRepositoryHelper(k_directory, k_sizeWarcMax, m_idman, m_ldTest);
    sessionRoot = jhrTest.getSession();
    
    // I'm not sure what to test about the session...
    jhrTest.reset();
  
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.JcrRepositoryHelper#getSizeWarcMax()}.
   */
  public final void testGetSizeWarcMax() throws Exception {
    JcrRepositoryHelper jhrTest = null;
    long sizeWarc;
    
    jhrTest = new JcrRepositoryHelper(k_directory, k_sizeWarcMax, m_idman, m_ldTest);
    sizeWarc = jhrTest.getSizeWarcMax();

    assertEquals(sizeWarc, k_sizeWarcMax);

    jhrTest.reset();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.JcrRepositoryHelper#moveRepository(java.lang.String, java.lang.String)}.
   */
  public final void testMoveRepository() throws Exception {
    JcrRepositoryHelper jhrTest = null;
    
    jhrTest = new JcrRepositoryHelper(k_directory, k_sizeWarcMax, m_idman, m_ldTest);
    jhrTest.moveRepository(k_strAuId, k_stemFileMoved);
    
    // I'm not sure what to test.
    jhrTest.reset();
  }

  /**
   * Test method for {@link org.lockss.repository.jcr.JcrRepositoryHelper#setConfig(org.lockss.config.Configuration, org.lockss.config.Configuration, org.lockss.config.Configuration.Differences)}.
   */
  public final void testSetConfig() throws Exception {
    Configuration conf1;
    Configuration conf2;
    Configuration.Differences confdiff;
    JcrRepositoryHelper jhrTest = null;
    Properties props1;
    Properties props2;
    
    props1 = new PropertyTree();
    props1.put(JcrRepositoryHelper.k_SIZE_WARC_MAX, k_sizestrWarcMax);
    conf1 = ConfigurationUtil.fromProps(props1);
    
    props2 = new PropertyTree();
    props2.put(JcrRepositoryHelper.k_SIZE_WARC_MAX, k_sizestrWarcMaxChanged);
    conf2 = ConfigurationUtil.fromProps(props2);
    
    confdiff = conf2.differences(conf1);

    jhrTest = new JcrRepositoryHelper(k_directory, k_sizeWarcMax, m_idman, m_ldTest);
    jhrTest.setConfig(conf2, conf1, confdiff);
    
    assertEquals(jhrTest.getSizeWarcMax(), k_sizeWarcMaxChanged);
    
    jhrTest.reset();
  }

  /**
   * To be run at the (start and) end of every test: verify that no .lock file exists.
   */
  private void checkLockFile() {
    File fileLock;
    
    fileLock = new File(k_directory + ".lock");
    assertFalse(".lock file was not removed.", fileLock.exists());
  }
}
