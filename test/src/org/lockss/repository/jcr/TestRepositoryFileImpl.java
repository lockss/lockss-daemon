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

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.data.*;
import org.lockss.daemon.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.repository.v2.*;
import org.lockss.repository.v2.RepositoryNode;
import org.lockss.test.*;
import org.lockss.util.*;

import junit.framework.TestCase;

/**
 * @author edwardsb
 * 
 */
public class TestRepositoryFileImpl extends LockssTestCase {

  // Constants
  private static final String k_dirXml = "test/src/org/lockss/repository/jcr/TestRepository/";
  private static final String k_nameXml = "LargeDatastore.xml";
  private static final String k_password = "password";
  private static final int k_sizeDeferredStream = 10240;
  private static final int k_sizeMaxBuffer = 10000;
  private static final String k_stemFile = "TestRepository/Content";
  private static final String k_urlDefault = "http://www.example.com/example.html";
  private static final String k_username = "username";
  
  // Used by the "getPreferredVersion" and "createNewVersion" tests. 
  private static int k_numTestVersions = 10;
    
  // Used by the test for URLs
  private static int k_lenURL = 30;
  private static int k_numURL = 100;
  
  // Mostly used by the tests that make many versions.
  private static int k_numVersionsBad = 3;
  private static int k_numVersionsGood = 3;

  // Static variables
  private static Random ms_random = new Random();

  // Member variables
  private MockIdentityManager m_idman;
  MockLockssDaemon m_ld;
  private Node m_nodeRoot;
  private RepositoryImpl m_repos;
  private Session m_session;
  
  protected void setUp() throws Exception {
    super.setUp();
    
    RepositoryConfig repconfig;

    repconfig = RepositoryConfig.create(k_dirXml + k_nameXml,
        k_dirXml);
    m_repos = RepositoryImpl.create(repconfig);
    m_session = m_repos.login(new SimpleCredentials(k_username, k_password
        .toCharArray()));
    m_nodeRoot = m_session.getRootNode();
    
    m_idman = new MockIdentityManager();
    
    m_ld = getMockLockssDaemon();
    m_ld.startDaemon();
    
    JcrRepositoryHelperFactory.preconstructor(k_sizeMaxBuffer, m_idman, m_ld);
  }

  protected void tearDown() throws Exception {
    DataStore ds;
    
    JcrRepositoryHelperFactory.reset();

    m_ld.stopDaemon();
    
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

      checkLockFile();
      
      // The directory name comes from k_stemFile.
      FileUtil.delTree(new File("TestRepository"));
      
      super.tearDown();
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

    System.gc();

    super.tearDown();
  }

  public final void testConstructor() throws Exception {
    Node nodeConstructor;
    RepositoryFile rfConstructor1;
    RepositoryFile rfConstructor2;
    
    nodeConstructor = m_nodeRoot.addNode("testConstructor");
    m_nodeRoot.save();
    
    // Verify that no parameter may start as 'null'.
    try {
      new RepositoryFileImpl(null, nodeConstructor, k_stemFile, k_urlDefault);
      fail("When the session is null, it should throw a " + 
          "LockssRepositoryException. (four-parameter)");
    } catch (NullPointerException e) {
      // Pass.
    }
    
    try {
      new RepositoryFileImpl(null, nodeConstructor);
      fail("When the session is null, it should throw a " + 
          "LockssRepositoryException. (two-parameter)");
    } catch (NullPointerException e) {
      // Pass.
    }
    
    try {
      new RepositoryFileImpl(m_session, null, k_stemFile, k_urlDefault);
      fail("When the node is null, it should throw a " + 
          "LockssRepositoryException. (four-parameter)");
    } catch (NullPointerException e) {
      // Pass.
    }
    
    try {
      new RepositoryFileImpl(m_session, null);
      fail("When the node is null, it should throw a " + 
          "LockssRepositoryException. (two-parameter)");
    } catch (NullPointerException e) {
      // Pass.
    }
    
    try {
      new RepositoryFileImpl(m_session, nodeConstructor, null, k_urlDefault);
      fail("When the stem file is null, it should throw a " + 
          "LockssRepositoryException. (four-parameter)");
    } catch (NullPointerException e) {
      // Pass.
    }

    try {
      new RepositoryFileImpl(m_session, nodeConstructor, k_stemFile, null);
      fail("When the URL is null, it should throw a " + 
          "LockssRepositoryException.");
    } catch (NullPointerException e) {
      // Pass.
    }
    
    

    // Verify that constructing a node with the five-parameter constructor,
    // then running the two-parameter constructor, will get the original
    // node.
    rfConstructor1 = new RepositoryFileImpl(m_session, nodeConstructor,
        k_stemFile, k_urlDefault);
    rfConstructor2 = new RepositoryFileImpl(m_session, nodeConstructor);
    
    assertEquals(rfConstructor1, rfConstructor2);
  }
  
  
  // "createNewVersion" is also extensively used by many other tests...
  public final void testCreateNewVersion() throws Exception {
    RepositoryFileVersion[] arrfvCreated;
    int i;
    List<RepositoryFileVersion> lirfvVersions;
    Node nodeCreateNewVersion;
    RepositoryFile rfCreateNewVersion;
    int ver;
    
    nodeCreateNewVersion = m_nodeRoot.addNode("testCreateNewVersion");
    m_nodeRoot.save();
    
    rfCreateNewVersion = new RepositoryFileImpl(m_session, nodeCreateNewVersion,
        k_stemFile, k_urlDefault);
    
    arrfvCreated = new RepositoryFileVersion[k_numTestVersions];
    for (ver = 0; ver < k_numTestVersions; ver++) {
      arrfvCreated[ver] = rfCreateNewVersion.createNewVersion();
      lirfvVersions = rfCreateNewVersion.listVersions();
      for (i = 0; i <= ver; i++) {
        // Pop quiz time: why do I use "ver - i"?
        // Yep; that's right.  "listVersions" lists them from most
        // to least recent.
        assertEquals(arrfvCreated[i], lirfvVersions.get(ver - i));
      }
    }
  }
  

  public final void testCreateNewVersionBefore() throws Exception {
    RepositoryFileVersion[] arrfvCreated;
    List<RepositoryFileVersion> lirfvVersions;
    Node nodeCreateNewVersionBefore;
    RepositoryFile rfCreateNewVersionBefore;
    RepositoryFileVersion rfvAdded;
    int ver;
    
    nodeCreateNewVersionBefore = m_nodeRoot.addNode("createNewVersionBefore");
    m_nodeRoot.save();
    
    // Insert before a version in the middle.
    rfCreateNewVersionBefore = new RepositoryFileImpl(m_session, 
        nodeCreateNewVersionBefore, k_stemFile, k_urlDefault);
    
    arrfvCreated = new RepositoryFileVersion[4];
    for (ver = 0; ver < 4; ver++) {
      arrfvCreated[ver] = rfCreateNewVersionBefore.createNewVersion();
    }
    rfvAdded = rfCreateNewVersionBefore.createNewVersionBefore(arrfvCreated[2]);
    
    lirfvVersions = rfCreateNewVersionBefore.listVersions();
    
    assertEquals(arrfvCreated[3], lirfvVersions.get(0));
    assertEquals(arrfvCreated[2], lirfvVersions.get(1));
    assertEquals(rfvAdded, lirfvVersions.get(2));
    assertEquals(arrfvCreated[1], lirfvVersions.get(3));
    assertEquals(arrfvCreated[0], lirfvVersions.get(4));
        
    // Insert before the very first version.
    rfCreateNewVersionBefore = new RepositoryFileImpl(m_session, 
        nodeCreateNewVersionBefore, k_stemFile, k_urlDefault);
    
    arrfvCreated = new RepositoryFileVersion[4];
    for (ver = 0; ver < 4; ver++) {
      arrfvCreated[ver] = rfCreateNewVersionBefore.createNewVersion();
    }
    rfvAdded = rfCreateNewVersionBefore.createNewVersionBefore(arrfvCreated[0]);
    
    lirfvVersions = rfCreateNewVersionBefore.listVersions();
    
    assertEquals(arrfvCreated[3], lirfvVersions.get(0));
    assertEquals(arrfvCreated[2], lirfvVersions.get(1));
    assertEquals(arrfvCreated[1], lirfvVersions.get(2));
    assertEquals(arrfvCreated[0], lirfvVersions.get(3));
    assertEquals(rfvAdded, lirfvVersions.get(4));
  }
  
  
  public final void testDelete() throws Exception {
    byte[] arbyContent;
    InputStream istrContent;
    Node nodeDelete;
    RepositoryFileImpl rfiDelete;
    RepositoryFileVersion rfvDelete;
    
    nodeDelete = m_nodeRoot.addNode("testDelete");
    m_nodeRoot.save();

    rfiDelete = new RepositoryFileImpl(m_session, nodeDelete,
      k_stemFile, k_urlDefault);

    // When the code has no preferred version, delete() should throw
    // an exception.
    try {
      rfiDelete.delete();
      fail("Before a preferred version is set, delete() should throw an exception.");
    } catch (NullPointerException e) {
      // Pass
    }
    
    // When the code has a preferred version, then delete() should
    // change the status of 'isDeleted()'.
    arbyContent = new byte[1];
    arbyContent[0] = 15;
    istrContent = new ByteArrayInputStream(arbyContent);
    rfvDelete = rfiDelete.createNewVersion();
    rfvDelete.setInputStream(istrContent);
    rfvDelete.commit();
    rfiDelete.setPreferredVersion(rfvDelete);
    
    assertFalse(rfiDelete.isDeleted());
    rfiDelete.delete();
    assertTrue(rfiDelete.isDeleted());
  }
  
  private final String k_filenamePPIS = "PPIS.data";
  private final static String k_strPeerIdentityOne = "TCP:[127.0.0.2]:0";
  private final static String k_strPeerIdentityTwo = "TCP:[192.168.0.128]:0";

  public final void testGetAgreeingPeerIdSet() throws Exception {
    File filePPIS;
    Node nodePeerIdSet;
    PeerIdentity piOne;
    PeerIdentity piTwo;
    PersistentPeerIdSet ppisSource;
    PersistentPeerIdSet ppisRetrieve;
    RepositoryFile rfPeerIdSet;
    
    // Construct an identity manager.
    // Adapted from the TestPersistentPeerId class.
    m_idman.addPeerIdentity(k_strPeerIdentityOne, new MockPeerIdentity(k_strPeerIdentityOne));
    m_idman.addPeerIdentity(k_strPeerIdentityTwo, new MockPeerIdentity(k_strPeerIdentityTwo));
    piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    piTwo = m_idman.findPeerIdentity(k_strPeerIdentityTwo);
    
    // Construct and populate a PersistentPeerIdSet.
    filePPIS = FileTestUtil.tempFile(k_filenamePPIS);
    ppisSource = new PersistentPeerIdSetImpl(new StreamerFile(filePPIS), m_idman);
    ppisSource.add(piOne);
    ppisSource.add(piTwo);
    
    // Construct and populate the Repository Node.
    nodePeerIdSet = m_nodeRoot.addNode("testGetAgreeingPeerIdSet");
    rfPeerIdSet = new RepositoryFileImpl(m_session, nodePeerIdSet, 
        k_stemFile, k_urlDefault);
    rfPeerIdSet.setAgreeingPeerIdSet(ppisSource);
    
    // Test.
    ppisRetrieve = rfPeerIdSet.getAgreeingPeerIdSet();
    assertEquals(2, ppisRetrieve.size());
    assertTrue(ppisRetrieve.contains(piOne));
    assertTrue(ppisRetrieve.contains(piTwo));
  }

  
  /**
   * Important note: the child count is the number of NODES, not versions,
   * in a tree.  Adding more versions should not change the number of nodes.
   * @throws Exception
   */
  public final void testGetChildCount() throws Exception {
    byte [] arbyContent;
    InputStream istrContent;
    Node nodeGetChildCount;
    RepositoryFile rfGetChildCount;
    RepositoryFileVersion rfvGetChildCount;
    
    nodeGetChildCount = m_nodeRoot.addNode("testGetChildCount");
    m_nodeRoot.save();
    
    // Create content.
    arbyContent = new byte[1];
    arbyContent[0] = 125;
    istrContent = new ByteArrayInputStream(arbyContent);
    
    rfGetChildCount = new RepositoryFileImpl(m_session, nodeGetChildCount, 
        k_stemFile, k_urlDefault);
    // Required for 'getChildCount'.
    rfvGetChildCount = rfGetChildCount.createNewVersion();
    rfvGetChildCount.setInputStream(istrContent);
    rfvGetChildCount.commit();
    rfGetChildCount.setPreferredVersion(rfvGetChildCount);
    
    // No matter what happens, the 'getChildCount' should
    // remain 0.
    assertEquals(0, rfGetChildCount.getChildCount());
    
    rfvGetChildCount.delete();
    assertEquals(0, rfGetChildCount.getChildCount());

    rfvGetChildCount.undelete();
    assertEquals(0, rfGetChildCount.getChildCount());
    
    rfGetChildCount.createNewVersion();
    assertEquals(0, rfGetChildCount.getChildCount());
  }
  
  // I'm only testing the parameterized version of 'getContentSize'.
  
  public final void testGetContentSize() throws Exception {
    byte[] arbyContent;
    RepositoryFileVersion[] arrfvVersions;
    int b;
    InputStream istrContent;
    int lenContent;
    int lenTotal;
    Node nodeGetContentSize;
    RepositoryFile rfGetContentSize;
    int ver;
    
    // We assume that the RepositoryFileVersion.getContentSize()
    // works correctly.
    
    // Create a file with multiple versions.
    nodeGetContentSize = m_nodeRoot.addNode("testGetContentSize");
    m_session.save();
    m_session.refresh(true);
    
    arrfvVersions = new RepositoryFileVersion[k_numTestVersions];
    
    rfGetContentSize = new RepositoryFileImpl(m_session, nodeGetContentSize,
        k_stemFile, k_urlDefault);
    
    for (ver = 0; ver < k_numTestVersions; ver++) {
      arrfvVersions[ver] = rfGetContentSize.createNewVersion();
      
      lenContent = (int) (Math.random() * 200) + 1;
      arbyContent = new byte[lenContent];
      for (b = 0; b < lenContent; b++) {
        arbyContent[b] = (byte) (65 + Math.random() * 26);
      }
      
      istrContent = new ByteArrayInputStream(arbyContent);
      arrfvVersions[ver].setInputStream(istrContent);
      arrfvVersions[ver].commit();
    }
    
    // Test that 'getContentSize(true)' is the same length as what the
    // version reports.
    for (ver = 0; ver < k_numTestVersions; ver++) {
      rfGetContentSize.setPreferredVersion(arrfvVersions[ver]);
      assertEquals(arrfvVersions[ver].getContentSize(), rfGetContentSize.getContentSize(true));
    }
    
    // Test that 'getContentSize(false)' is the total of all lengths.
    lenTotal = 0;
    for (ver = 0; ver < k_numTestVersions; ver++) {
      lenTotal += arrfvVersions[ver].getContentSize();
    }
    // ...and that changing the preferred version makes no difference.
    for (ver = 0; ver < k_numTestVersions; ver++) {
      rfGetContentSize.setPreferredVersion(arrfvVersions[ver]);
      assertEquals(lenTotal, rfGetContentSize.getContentSize(false));
    }
  }
  
  // For simplicity's sake, I'm just testing parameters individually --
  // not in all combinations.   
  
  // For time's sake, I'm just testing the two-parameter version of
  // the getFileList() method.  The other methods are single-line methods,
  // and are better checked by inspection.
  public final void testGetFileList() throws Exception {
    byte[] arbyContent;
    CachedUrlSetSpec cussExcluded;  // PG rated
    CachedUrlSetSpec cussIncluded;  // PG-13 rated
    InputStream istrContent;
    Node nodeDeleted;
    Node nodeNotDeleted;
    RepositoryFile rfDeleted;
    RepositoryFile rfNotDeleted;
    List<RepositoryFile> lirfResult;
    RepositoryFileVersion rfvDeleted;
    RepositoryFileVersion rfvNotDeleted;
    
    nodeDeleted = m_nodeRoot.addNode("testGetFileListDeleted");
    nodeNotDeleted = m_nodeRoot.addNode("testGetFileListNotDeleted");
    m_session.save();
    m_session.refresh(true);
    
    arbyContent = new byte[1];
    arbyContent[0] = 23; 
    
    // Construct rfDeleted.
    istrContent = new ByteArrayInputStream(arbyContent);
    rfDeleted = new RepositoryFileImpl(m_session, nodeDeleted, 
        k_stemFile, k_urlDefault);
    rfvDeleted = rfDeleted.createNewVersion();
    rfvDeleted.setInputStream(istrContent);
    rfvDeleted.commit();
    rfDeleted.setPreferredVersion(rfvDeleted);
    rfvDeleted.delete();

    // Construct rfNotDeleted.
    istrContent = new ByteArrayInputStream(arbyContent);
    rfNotDeleted = new RepositoryFileImpl(m_session, nodeNotDeleted, 
        k_stemFile, k_urlDefault);
    rfvNotDeleted = rfNotDeleted.createNewVersion();
    rfvNotDeleted.setInputStream(istrContent);
    rfvNotDeleted.commit();
    rfNotDeleted.setPreferredVersion(rfvNotDeleted);

    // Test: filter == null
    lirfResult = rfDeleted.getFileList(null, false);
    assertEquals(0, lirfResult.size());
    
    lirfResult = rfNotDeleted.getFileList(null, false);
    assertEquals(1, lirfResult.size());
    assertEquals(rfNotDeleted, lirfResult.get(0));
    
    // filter includes url
    cussIncluded = new AuCachedUrlSetSpec();
    
    lirfResult = rfDeleted.getFileList(cussIncluded, false);
    assertEquals(0, lirfResult.size());
    
    lirfResult = rfNotDeleted.getFileList(cussIncluded, false);
    assertEquals(1, lirfResult.size());
    assertEquals(rfNotDeleted, lirfResult.get(0));
    
    // filter does not include url
    cussExcluded = new SingleNodeCachedUrlSetSpec("http://www.not-even-close.com");
    lirfResult = rfDeleted.getFileList(cussExcluded, false);
    assertEquals(0, lirfResult.size());
    
    lirfResult = rfNotDeleted.getFileList(cussExcluded, false);
    assertEquals(0, lirfResult.size());

    // includeDeleted = false
    lirfResult = rfDeleted.getFileList(null, false);
    assertEquals(0, lirfResult.size());
    
    lirfResult = rfNotDeleted.getFileList(null, false);
    assertEquals(1, lirfResult.size());
    assertEquals(rfNotDeleted, lirfResult.get(0));
    
    // includeDeleted = true
    lirfResult = rfDeleted.getFileList(null, true);
    assertEquals(1, lirfResult.size());
    assertEquals(rfDeleted, lirfResult.get(0));
    
    lirfResult = rfNotDeleted.getFileList(null, true);
    assertEquals(1, lirfResult.size());
    assertEquals(rfNotDeleted, lirfResult.get(0));   
  }

  // For simplicity's sake, I'm just testing parameters individually --
  // not in all combinations.   
  
  // For time's sake, I'm just testing the two-parameter version of
  // the getFiles() method.  The other methods are single-line methods,
  // and are better checked by inspection.
  public final void testGetFiles() throws Exception {
    byte[] arbyContent;
    InputStream istrContent;
    Node nodeDeleted;
    Node nodeNotDeleted;
    RepositoryFile rfDeleted;
    RepositoryFile rfNotDeleted;
    RepositoryFile[] arrfResult;
    RepositoryFileVersion rfvDeleted;
    RepositoryFileVersion rfvNotDeleted;
    
    nodeDeleted = m_nodeRoot.addNode("testGetFilesDeleted");
    nodeNotDeleted = m_nodeRoot.addNode("testGetFilesNotDeleted");
    m_session.save();
    m_session.refresh(true);
    
    arbyContent = new byte[1];
    arbyContent[0] = 4;  // http://xkcd.com/221/
    
    // Construct rfDeleted.
    istrContent = new ByteArrayInputStream(arbyContent);
    rfDeleted = new RepositoryFileImpl(m_session, nodeDeleted, 
        k_stemFile, k_urlDefault);
    rfvDeleted = rfDeleted.createNewVersion();
    rfvDeleted.setInputStream(istrContent);
    rfvDeleted.commit();
    rfDeleted.setPreferredVersion(rfvDeleted);
    rfvDeleted.delete();

    // Construct rfNotDeleted.
    istrContent = new ByteArrayInputStream(arbyContent);
    rfNotDeleted = new RepositoryFileImpl(m_session, nodeNotDeleted, 
        k_stemFile, k_urlDefault);
    rfvNotDeleted = rfNotDeleted.createNewVersion();
    rfvNotDeleted.setInputStream(istrContent);
    rfvNotDeleted.commit();
    rfNotDeleted.setPreferredVersion(rfvNotDeleted);

    // Test: maxVersions > 1
    arrfResult = rfDeleted.getFiles(53, false);
    assertEquals(0, arrfResult.length);
    
    arrfResult = rfNotDeleted.getFiles(67, false);
    assertEquals(1, arrfResult.length);
    assertEquals(rfNotDeleted, arrfResult[0]);
    
    // maxVersions == 1
    arrfResult = rfDeleted.getFiles(1, false);
    assertEquals(0, arrfResult.length);
    
    arrfResult = rfNotDeleted.getFiles(1, false);
    assertEquals(1, arrfResult.length);
    assertEquals(rfNotDeleted, arrfResult[0]);
    
    // maxVersions < 1
    arrfResult = rfDeleted.getFiles(0, false);
    assertEquals(0, arrfResult.length);
    
    arrfResult = rfNotDeleted.getFiles(0, false);
    assertEquals(0, arrfResult.length);

    // includeDeleted = false
    arrfResult = rfDeleted.getFiles(1, false);
    assertEquals(0, arrfResult.length);
    
    arrfResult = rfNotDeleted.getFiles(1, false);
    assertEquals(1, arrfResult.length);
    assertEquals(rfNotDeleted, arrfResult[0]);
    
    // includeDeleted = true
    arrfResult = rfDeleted.getFiles(1, true);
    assertEquals(1, arrfResult.length);
    assertEquals(rfDeleted, arrfResult[0]);
    
    arrfResult = rfNotDeleted.getFiles(1, true);
    assertEquals(1, arrfResult.length);
    assertEquals(rfNotDeleted, arrfResult[0]);   
  }
  
  public final void testGetNodeUrl() throws Exception {
    int countChar;
    int countURL;
    Node nodeGetNodeUrl;
    org.lockss.repository.v2.RepositoryFile rfGetNodeUrl;
    StringBuffer sbUrl;
    String url = null;
    
    nodeGetNodeUrl = m_nodeRoot.addNode("testGetNodeUrl");
    m_nodeRoot.save();
    
    for (countURL = 0; countURL < k_numURL; countURL++) {
      sbUrl = new StringBuffer();
      sbUrl.append("http://");
      
      for (countChar = 0; countChar < k_lenURL; countChar++) {
        sbUrl.append((char) (ms_random.nextInt(26) + 'A'));
      }
      
      url = sbUrl.toString();
      
      rfGetNodeUrl = new RepositoryFileImpl(m_session, 
          nodeGetNodeUrl, k_stemFile, url);
      
      assertEquals(url, rfGetNodeUrl.getNodeUrl());
    }
    
    // And verify that we get the last URL when we restart...
    rfGetNodeUrl = new RepositoryFileImpl(m_session, nodeGetNodeUrl);
    assertEquals(url, rfGetNodeUrl.getNodeUrl());
  }

  public final void testGetPollHistories() {
    // This method has been stubbed out.  Therefore, no
    // tests are performed.
  }

  public final void testGetPreferredVersion() throws Exception {
    byte[] arbyContent;
    InputStream istrContent;
    Node nodePreferredVersion;
    RepositoryFile rfPreferredVersion;
    RepositoryFileVersion[] arrfvVersions;
    int ver;

    // Test the setPreferredVersion / getPreferredVersion combination.
    nodePreferredVersion = m_nodeRoot.addNode("testGetPreferredVersion");
    m_nodeRoot.save();
    
    rfPreferredVersion = new RepositoryFileImpl(m_session, nodePreferredVersion,
        k_stemFile, k_urlDefault);
    
    arbyContent = new byte[1];
    arbyContent[0] = 33;
    
    arrfvVersions = new RepositoryFileVersion[k_numTestVersions];
    for (ver = 0; ver < k_numTestVersions; ver++) {
      arrfvVersions[ver] = rfPreferredVersion.createNewVersion();
      
      istrContent = new ByteArrayInputStream(arbyContent);
      arrfvVersions[ver].setInputStream(istrContent);
      arrfvVersions[ver].commit();
      
      istrContent.close();
    }
    
    for (ver = 0; ver < k_numTestVersions; ver++) {
      rfPreferredVersion.setPreferredVersion(arrfvVersions[ver]);
      assertEquals(arrfvVersions[ver], rfPreferredVersion.getPreferredVersion());
    }
    
    // Test without setPreferredVersion called:
    nodePreferredVersion = m_nodeRoot.addNode("testGetPreferredVersion-2");
    m_nodeRoot.save();
    
    rfPreferredVersion = new RepositoryFileImpl(m_session, nodePreferredVersion,
        k_stemFile, k_urlDefault);
    
    // If no versions have been added, then return null.
    assertEquals(null, rfPreferredVersion.getPreferredVersion());

    arrfvVersions = new RepositoryFileVersion[k_numTestVersions];
    for (ver = 0; ver < k_numTestVersions; ver++) {
      arrfvVersions[ver] = rfPreferredVersion.createNewVersion();
      
      istrContent = new ByteArrayInputStream(arbyContent);
      arrfvVersions[ver].setInputStream(istrContent);
      arrfvVersions[ver].commit();
      
      istrContent.close();
    }

    // If no nodes are deleted, then return the most recent node.
    assertEquals(arrfvVersions[k_numTestVersions - 1], rfPreferredVersion.getPreferredVersion());
    
    // If some nodes are deleted, then return the most recent node
    // that was not deleted.
    for (ver = k_numTestVersions - 1; ver > 0; ver--) {
      arrfvVersions[ver].delete();
      assertEquals(arrfvVersions[ver - 1], rfPreferredVersion.getPreferredVersion());
    }
    
    // If all nodes are deleted, then return null.
    arrfvVersions[0].delete();
    assertEquals(null, rfPreferredVersion.getPreferredVersion());
  }
  
  public final void testGetProperties() throws Exception {
    Node nodeGetProperties;
    String strProp1;
    String strProp2;
    Properties propsGetProperties1;
    Properties propsGetPropertiesTest;
    org.lockss.repository.v2.RepositoryFile rfGetProperties;

    nodeGetProperties = m_nodeRoot.addNode("testGetUnversionedProperties");
    m_nodeRoot.save();

    rfGetProperties = new RepositoryFileImpl(m_session, nodeGetProperties,
        k_stemFile, k_urlDefault);
    
    // Create properties.
    propsGetProperties1 = new Properties();
    propsGetProperties1.setProperty("key1", "value1");
    propsGetProperties1.setProperty("key2", "value2");
    propsGetProperties1.setProperty("key3", "value3");

    rfGetProperties.setProperties(propsGetProperties1);

    // Retrieve and test properties.
    propsGetPropertiesTest = rfGetProperties.getProperties();

    strProp1 = propsGetProperties1.getProperty("key1");
    strProp2 = propsGetPropertiesTest.getProperty("key1");
    assertEquals(strProp1, strProp2);
       
    strProp1 = propsGetProperties1.getProperty("key2");
    strProp2 = propsGetPropertiesTest.getProperty("key2"); 
    assertEquals(strProp1, strProp2);
        
    strProp1 = propsGetProperties1.getProperty("key3");
    strProp2 = propsGetPropertiesTest.getProperty("key3"); 
    assertEquals(strProp1, strProp2);
  }
  
  
  // As usual with these tests, I am only testing the version of
  // getTreeContentSize with all parameters.  

  // For this test, I'm doing the cross-product of CachedUrlSetSpec
  // and preferredOnly.
  
  public final void testGetTreeContentSize() throws Exception {
    byte [] arbyContent;
    RepositoryFileVersion[] arrfvVersions;
    int b;
    CachedUrlSetSpec cussExcluded;
    CachedUrlSetSpec cussIncluded;
    InputStream istrContent;
    Node nodeGetTreeContentSize;
    RepositoryFile rfGetTreeContentSize;
    int size;
    int sizeTotal;
    int ver;
    
    nodeGetTreeContentSize = m_nodeRoot.addNode("testGetTreeContentSize");
    m_nodeRoot.save();
    
    rfGetTreeContentSize = new RepositoryFileImpl(m_session, 
        nodeGetTreeContentSize, k_stemFile, k_urlDefault);
      
    // Check that when no versions are set, that the tree content size is 0.
    assertEquals(0, rfGetTreeContentSize.getTreeContentSize(null, true, true));
    assertEquals(0, rfGetTreeContentSize.getTreeContentSize(null, true, false));
 
    arrfvVersions = new RepositoryFileVersion[k_numTestVersions];
    sizeTotal = 0;
    for (ver = 0; ver < k_numTestVersions; ver++) {
      size = (int) (Math.random() * 100);
      sizeTotal += size;
      arbyContent = new byte[size];
      
      for (b = 0; b < size; b++) {
        arbyContent[b] = (byte) (65 + Math.random() * 26);
      }
      istrContent = new ByteArrayInputStream(arbyContent);
      
      arrfvVersions[ver] = rfGetTreeContentSize.createNewVersion();
      arrfvVersions[ver].setInputStream(istrContent);
      arrfvVersions[ver].commit();
    }
    rfGetTreeContentSize.setPreferredVersion(arrfvVersions[0]);
    
    // Check filter: filter is null.
    assertEquals(arrfvVersions[0].getContentSize(), 
        rfGetTreeContentSize.getTreeContentSize(null, false, true));
    assertEquals(sizeTotal, 
        rfGetTreeContentSize.getTreeContentSize(null, false, false));
    
    // Check filter: filter matches the URL
    cussIncluded = new AuCachedUrlSetSpec();
    assertEquals(arrfvVersions[0].getContentSize(), 
        rfGetTreeContentSize.getTreeContentSize(cussIncluded, false, true));
    assertEquals(sizeTotal, 
        rfGetTreeContentSize.getTreeContentSize(cussIncluded, false, false));
   
    // Check filter: filter does not match the URL
    cussExcluded = new SingleNodeCachedUrlSetSpec("http://this.url.is.bad.com");
    assertEquals(0, 
        rfGetTreeContentSize.getTreeContentSize(cussExcluded, false, true));
    assertEquals(0,
        rfGetTreeContentSize.getTreeContentSize(cussExcluded, false, false));
  }
  

  public final void testHasContent() throws Exception {
    byte[] arbyContent;
    InputStream istrContent;
    Node nodeHasContent;
    RepositoryFile rfHasContent;
    RepositoryFileVersion rfvHasContent;
    
    nodeHasContent = m_nodeRoot.addNode("testHasContent");
    rfHasContent = new RepositoryFileImpl(m_session, nodeHasContent,
        k_stemFile, k_urlDefault);
    
    // Test the RepositoryFile before it has content.
    assertFalse(rfHasContent.hasContent());
    
    // Test the RepositoryFile after it has content.
    arbyContent = new byte[1];
    arbyContent[0] = 56;
    istrContent = new ByteArrayInputStream(arbyContent);
    
    rfvHasContent = rfHasContent.createNewVersion();
    rfvHasContent.setInputStream(istrContent);
    rfvHasContent.commit();
    rfHasContent.setPreferredVersion(rfvHasContent);
    
    assertTrue(rfHasContent.hasContent());
  }

  public final void testIsDeleted() throws Exception {
    byte[] arbyContent;
    InputStream istrContent;
    Node nodeIsDeleted;
    RepositoryFileImpl rfiIsDeleted;
    RepositoryFileVersion rfvIsDeleted;
    
    nodeIsDeleted = m_nodeRoot.addNode("testIsDeleted");
    m_nodeRoot.save();

    rfiIsDeleted = new RepositoryFileImpl(m_session, nodeIsDeleted,
        k_stemFile, k_urlDefault);

    // When the code has no preferred version, isDeleted() should 
    // return false.
    assertFalse(rfiIsDeleted.isDeleted());
    
    arbyContent = new byte[1];
    arbyContent[0] = 15;
    istrContent = new ByteArrayInputStream(arbyContent);
    rfvIsDeleted = rfiIsDeleted.createNewVersion();
    rfvIsDeleted.setInputStream(istrContent);
    rfvIsDeleted.commit();
    rfiIsDeleted.setPreferredVersion(rfvIsDeleted);
    
    // When the code has a preferred version, then delete() should
    // change the status of 'isDeleted()'.
    assertFalse(rfiIsDeleted.isDeleted());
    rfiIsDeleted.delete();
    assertTrue(rfiIsDeleted.isDeleted());
    
    // Undelete() should change the status of 'isDeleted()'.
    rfiIsDeleted.undelete();
    assertFalse(rfiIsDeleted.isDeleted());
  }
  
  public final void testListVersions() throws Exception {
    byte[] arbyContent;
    RepositoryFileVersion[] arrfvBadVersions;
    RepositoryFileVersion[] arrfvGoodVersions;
    Collection<RepositoryFileVersion> colVersions;
    int i;
    boolean isFound;
    InputStream istrContent;
    Iterator<RepositoryFileVersion> iterVersions;
    Node nodeListVersions;
    int numVersions;
    org.lockss.repository.jcr.RepositoryFileImpl rfListVersions;
    RepositoryFileVersion rfvBadVersion;
    RepositoryFileVersion rfvGoodVersion;
    RepositoryFileVersion rfvVersion;

    arbyContent = new byte[1];

    // Generate the sets of versions...
    nodeListVersions = m_nodeRoot.addNode("testListVersions");
    rfListVersions = new RepositoryFileImpl(m_session, nodeListVersions, 
        k_stemFile, k_urlDefault);
    
    // Generate real versions that won't be part of the final
    // rfvGoodVersion...
    arrfvBadVersions = new RepositoryFileVersion[k_numVersionsBad];
    for (i = 0; i < k_numVersionsBad; i++) {
      arbyContent[0] = (byte) i;
      istrContent = new ByteArrayInputStream(arbyContent);
      
      rfvBadVersion = new RepositoryFileVersionHarnessImpl(m_session, nodeListVersions,
          k_stemFile, rfListVersions);
      rfvBadVersion.setInputStream(istrContent);
      rfvBadVersion.commit();
      arrfvBadVersions[i] = rfvBadVersion;
    }

    // Generate versions that will be part of the final rfvGoodVersion.
    arrfvGoodVersions = new RepositoryFileVersion[k_numVersionsGood];
    
    for (i = 0; i < k_numVersionsGood; i++) {
      arbyContent[0] = (byte) i;
      istrContent = new ByteArrayInputStream(arbyContent);

      rfvGoodVersion = rfListVersions.createNewVersion();
      
      rfvGoodVersion.setInputStream(istrContent);
      rfvGoodVersion.commit();
      arrfvGoodVersions[i] = rfvGoodVersion;
    }
    
    // Compare the versions returned against the known set of versions.
    colVersions = rfListVersions.listVersions();
    for (i = 0; i < k_numVersionsBad; i++) {
      assertFalse(colVersions.contains(arrfvBadVersions[i]));
    }
    
    for (i = 0; i < k_numVersionsGood; i++) {
      assertTrue(colVersions.contains(arrfvGoodVersions[i]));
    }
    
    iterVersions = colVersions.iterator();
    while (iterVersions.hasNext()) {
      rfvVersion = iterVersions.next();

      // Sadly, there's no "arrfvGoodVersions.contains(lVersion)"
      isFound = false;
      for (i = 0; i < k_numVersionsGood; i++) {
        if (rfvVersion.equals(arrfvGoodVersions[i])) {
          isFound = true;
          break;  // Out of the 'for' loop.
        }
      }
      
      assertTrue(isFound);
    }
    
    // The following tests are for listVersions(int).
    for (numVersions = 1; numVersions < k_numVersionsGood; numVersions++) {
      colVersions = rfListVersions.listVersions(numVersions);
      
      for (i = 0; i < k_numVersionsBad; i++) {
        assertFalse(colVersions.contains(arrfvBadVersions[i]));
      }
      
      for (i = k_numVersionsBad - numVersions; i < k_numVersionsGood; i++) {
        assertTrue(colVersions.contains(arrfvGoodVersions[i]));
      }
    }
  }
  
  private final String k_strMove = "/tmp/stemfileTestMoveA";
  
  public final void testMove() throws Exception {
    InputStream istrContent;
    List<RepositoryFileVersion> lirfvReturned;
    Node nodeTestMove;
    RepositoryFile rfTestMove;
    RepositoryFileVersion rfvTestMove1;
    RepositoryFileVersion rfvTestMove2;
    String strText1;
    String strText2;
    
    nodeTestMove = m_nodeRoot.addNode("testMove");
    rfTestMove = new RepositoryFileImpl(m_session, nodeTestMove, 
        k_stemFile, k_urlDefault);
    
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
    rfTestMove.move(k_strMove);
    
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
  }
  
  
  public final void testSetAgreeingPeerIdSet() throws Exception {
    Node nodeAgreeingPeerIDSet;
    org.lockss.repository.v2.RepositoryFile rfAgreeingPeerIDSet;
    PersistentPeerIdSet ppisNull;

    // Construct a RepositoryFileVersionHarness
    nodeAgreeingPeerIDSet = m_nodeRoot.addNode("testSetAgreeingPeerIdSet");
    m_nodeRoot.save();

    rfAgreeingPeerIDSet = new RepositoryFileImpl(m_session,
        nodeAgreeingPeerIDSet, k_stemFile, k_urlDefault);

    // Try setting the peer id set with null.
    rfAgreeingPeerIDSet.setAgreeingPeerIdSet(null);
    ppisNull = rfAgreeingPeerIDSet.getAgreeingPeerIdSet();

    assertEquals(null, ppisNull);
  }
  
  
  public final void testSetPollHistories() throws Exception {
    // Because the method has been stubbed out, no tests are performed.
  }
  
  
  public final void testSetPreferredVersion() throws Exception {
    byte[] arbyContent;
    InputStream isContent;
    Node nodeSetPreferredVersion1;
    Node nodeSetPreferredVersion2;
    RepositoryFileImpl rfiFalse;
    RepositoryFileImpl rfiTrue;
    RepositoryFileVersion rfviFalse;
    RepositoryFileVersion rfviTrue;
    
    nodeSetPreferredVersion1 = m_nodeRoot.addNode("testSetPreferredVersion-1");
    nodeSetPreferredVersion2 = m_nodeRoot.addNode("testSetPreferredVersion-2");
    m_nodeRoot.save();
    
    rfiTrue = new RepositoryFileImpl(m_session, nodeSetPreferredVersion1, 
        k_stemFile, k_urlDefault);
    rfiFalse = new RepositoryFileImpl(m_session, nodeSetPreferredVersion2, 
        k_stemFile, k_urlDefault);
    
    arbyContent = new byte[1];
    arbyContent[0] = 45;
    
    // Test: setPreferredVersion should accept a new version that is committed.
    rfviTrue = rfiTrue.createNewVersion();
    
    isContent = new ByteArrayInputStream(arbyContent);
    rfviTrue.setInputStream(isContent);
    rfviTrue.commit();
    
    // This line is the test; if there's an exception, it fails.
    rfiTrue.setPreferredVersion(rfviTrue);
    
    // Test: setPreferredVersion should reject a version that is not 
    // of the same file (even if it is committed).
    rfviFalse = new RepositoryFileVersionImpl(m_session, nodeSetPreferredVersion2,
        k_stemFile, rfiFalse, k_sizeDeferredStream);

    isContent = new ByteArrayInputStream(arbyContent);
    rfviFalse.setInputStream(isContent);
    rfviFalse.commit();
    
    try {
      rfiTrue.setPreferredVersion(rfviFalse);
      fail("setPreferredVersion should have failed when given a version from " + 
          "a different file.");
    } catch (LockssRepositoryException e) {
      // Pass.
    }

    // Test: setPreferredVersion should reject a version that is from
    // the same file, but is not committed.
    rfviFalse = rfiTrue.createNewVersion();
    
    isContent = new ByteArrayInputStream(arbyContent);
    rfviFalse.setInputStream(isContent);
    // Notice: No commit!
    
    try {
      rfiTrue.setPreferredVersion(rfviFalse);
      fail("setPreferredVersion should have failed when given a version " + 
          "that was not committed.");
    } catch (LockssRepositoryException e) {
      // Pass.
    }
  }

  // The following tests come about from specific problems.
  
  // It is a feature, NOT a bug, that creating a new version 
  // does not copy the properties from the previous version.
  
  public void testNewVersionsDontCopyProperties() throws Exception {
    byte[] arbyContent;
    InputStream istrContent;
    Node nodeNewVersionsAreEmpty;
    String strProp1;
    String strProp2;
    Properties propsNewVersionsAreEmpty1;
    Properties propsNewVersionsAreEmpty2;
    Properties propsNewVersionsAreEmptyTest;
    org.lockss.repository.v2.RepositoryFile rfNewVersionsAreEmpty;
    RepositoryFileVersion rfvhNVAE1;
    RepositoryFileVersion rfvhNVAE2;
        
    arbyContent = new byte[3];

    nodeNewVersionsAreEmpty = m_nodeRoot.addNode("testNewVersionsAreEmpty");
    m_nodeRoot.save();

    rfNewVersionsAreEmpty = new RepositoryFileImpl(m_session, nodeNewVersionsAreEmpty,
        k_stemFile, "http://www.example.com/my-file.html");
    rfvhNVAE1 = rfNewVersionsAreEmpty.createNewVersion();

    // Create properties.
    propsNewVersionsAreEmpty1 = new Properties();
    propsNewVersionsAreEmpty1.setProperty("key1", "value1");
    propsNewVersionsAreEmpty1.setProperty("key2", "value2");
    propsNewVersionsAreEmpty1.setProperty("key3", "value3");
    
    istrContent = new ByteArrayInputStream(arbyContent);

    rfvhNVAE1.setProperties(propsNewVersionsAreEmpty1);
    rfvhNVAE1.setInputStream(istrContent);
    rfvhNVAE1.commit();

    // We know from 'testGetProperties' that the properties were set.
    // Now, set new properties that do NOT match the original...
    
    propsNewVersionsAreEmpty2 = new Properties();
    propsNewVersionsAreEmpty2.setProperty("alt1", "alternate1");
    propsNewVersionsAreEmpty2.setProperty("alt2", "alternate2");
    propsNewVersionsAreEmpty2.setProperty("alt3", "alternate3");

    rfvhNVAE2 = rfNewVersionsAreEmpty.createNewVersion();
    
    istrContent = new ByteArrayInputStream(arbyContent);
    
    rfvhNVAE2.setProperties(propsNewVersionsAreEmpty2);
    rfvhNVAE2.setInputStream(istrContent);
    rfvhNVAE2.commit();
    
    propsNewVersionsAreEmptyTest = rfvhNVAE2.getProperties();
    
    // Test that the old properties are NOT in the second version...
    assertNull(propsNewVersionsAreEmptyTest.getProperty("key1"));
    assertNull(propsNewVersionsAreEmptyTest.getProperty("key2"));
    assertNull(propsNewVersionsAreEmptyTest.getProperty("key3"));

    // Test that the new properties are being stored...
    strProp1 = propsNewVersionsAreEmpty2.getProperty("alt1");  // '2', this time.
    strProp2 = propsNewVersionsAreEmptyTest.getProperty("alt1");
    assertEquals(strProp1, strProp2);
       
    strProp1 = propsNewVersionsAreEmpty2.getProperty("alt2");
    strProp2 = propsNewVersionsAreEmptyTest.getProperty("alt2"); 
    assertEquals(strProp1, strProp2);
        
    strProp1 = propsNewVersionsAreEmpty2.getProperty("alt3");
    strProp2 = propsNewVersionsAreEmptyTest.getProperty("alt3"); 
    assertEquals(strProp1, strProp2);
  }
  
  
  public void testUndelete() throws Exception {
    byte[] arbyContent;
    InputStream istrContent;
    Node nodeUndelete;
    RepositoryFileImpl rfiUndelete;
    RepositoryFileVersion rfvUndelete;
    
    nodeUndelete = m_nodeRoot.addNode("testUndelete");
    m_nodeRoot.save();

    rfiUndelete = new RepositoryFileImpl(m_session, nodeUndelete,
        k_stemFile, k_urlDefault);

    // When the code has no preferred version, undelete() should throw
    // an exception.
    try {
      rfiUndelete.undelete();
      fail("Before a preferred version is set, undelete() should throw an exception.");
    } catch (NullPointerException e) {
      // Pass
    }
    
    arbyContent = new byte[1];
    arbyContent[0] = 15;
    istrContent = new ByteArrayInputStream(arbyContent);
    rfvUndelete = rfiUndelete.createNewVersion();
    rfvUndelete.setInputStream(istrContent);
    rfvUndelete.commit();
    rfiUndelete.setPreferredVersion(rfvUndelete);
    
    assertFalse(rfiUndelete.isDeleted());
    
    // Calling undelete() on a default (undeleted) file
    // should not change anything.
    rfiUndelete.undelete();
    assertFalse(rfiUndelete.isDeleted());
    
    // When the code has a preferred version, then delete() should
    // change the status of 'isDeleted()'.
    rfiUndelete.delete();
    assertTrue(rfiUndelete.isDeleted());
    
    // Undelete() should revert the status of 'isDeleted()'.
    rfiUndelete.undelete();
    assertFalse(rfiUndelete.isDeleted());
  }

  
  /** ****** Helper methods ******** */

  private void cleanRepositoryFile(
      RepositoryFileVersionHarness rfGetProperties) {
    // Do nothing, for now.
  }

  /**
   * Useful for debugging only.
   * 
   * @param nodeConstructor2
   * @throws RepositoryException
   * @throws ValueFormatException
   */
  private void outputProperties(Node nodeConstructor2)
      throws RepositoryException, ValueFormatException {
    PropertyIterator pi;
    Property prop;
    pi = nodeConstructor2.getProperties();
    while (pi.hasNext())  {
      prop = pi.nextProperty();
      System.out.println(prop.getName() + "-->" + prop.getString());
    }
  }
  
  private String createRandomText(int size) {
    int numChar;
    StringBuilder sbRandom;
    
    sbRandom = new StringBuilder(size);
    
    for (numChar = 0; numChar < size; numChar++) {
      sbRandom.append((char) (ms_random.nextInt(26) + 65));
    }
    
    return sbRandom.toString();
  }
  
  /**
   * To be run at the (start and) end of every test: verify that no .lock file exists.
   */
  private void checkLockFile() {
    File fileLock;
    
    fileLock = new File(k_dirXml + ".lock");
    assertFalse(".lock file was not removed.", fileLock.exists());
  }

}
