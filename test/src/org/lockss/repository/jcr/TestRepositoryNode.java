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

import com.sun.org.apache.xalan.internal.xsltc.dom.*;

import java.io.*;
import java.sql.*;
import java.util.*;

import javax.jcr.*;

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.config.*;
import org.apache.jackrabbit.core.data.*;
import org.lockss.daemon.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.repository.v2.*;
import org.lockss.repository.v2.RepositoryNode;
import org.lockss.test.*;
import org.lockss.util.*;


import junit.framework.*;

/**
 * @author edwardsb
 *
 */
public class TestRepositoryNode extends LockssTestCase {
  
  // Constants
  private static final String k_dirXml = "test/src/org/lockss/repository/jcr/TestRepository/";
  private static final int k_maxChildren = 50;
  private static final String k_nameXml = "LargeDatastore.xml";
  private static final int k_numNodes = 10;
  private static final int k_numFilesPerNode = 10;
  private static final int k_numVersionsPerFile = 3;
  private static final String k_password = "password";
  private static final int k_sizeMaxBuffer = 10000;
  private static final String k_stemFile = "TestRepository/Content";
  private static final String k_urlDefault = "http://www.example.com/example.html";
  private static final String k_username = "username";
  private static final int k_sizeFile = 1000;
  
  
  // Static member variables
  private static Random ms_random = new Random();

  // Member variables
  private MockIdentityManager m_idman;
  private MockLockssDaemon m_ld;
  private Node m_nodeRoot;
  private RepositoryImpl m_repos;
  private Session m_session;

  /**
   * @see junit.framework.TestCase#setUp()
   * @throws java.lang.Exception
   */
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
    
    JcrRepositoryHelperFactory.preconstructor(k_sizeFile, m_idman, m_ld);
  }

  
  /**
   * @see junit.framework.TestCase#tearDown()
   * @throws java.lang.Exception
   */
  protected void tearDown() throws Exception {
    DataStore ds;

    JcrRepositoryHelperFactory.reset();
    
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

    System.gc();

    checkLockFile();
    FileUtil.delTree(new File("TestRepository"));
    
    super.tearDown();
  }

  /**
   * Test method for the constructor.
   */
  public final void testConstructor() throws Exception {
    Node nodeConstructor;
    RepositoryNode rnConstructor;
    RepositoryNode rnDuplicate;
    
    // Verify all of the nulls for the constructor.
    nodeConstructor = m_nodeRoot.addNode("testConstructor");
    
    try {
      new RepositoryNodeImpl(null, nodeConstructor, k_stemFile, k_urlDefault);
      fail("A null session should have caused a failure.");
    } catch (NullPointerException e) {
      // Pass.
    }
    
    try {
      new RepositoryNodeImpl(m_session, null, k_stemFile, k_urlDefault);
      fail("A null node should have caused a failure.");
    } catch (NullPointerException e) {
      // Pass.
    }
    
    try {
      new RepositoryNodeImpl(m_session, nodeConstructor, null, k_urlDefault);
      fail("A null stem file should have caused a failure.");
    } catch (NullPointerException e) {
      // Pass.
    }

    try {
      new RepositoryNodeImpl(m_session, nodeConstructor, k_stemFile, null);
      fail("A null URL should have caused a failure.");
    } catch (NullPointerException e) {
      // Pass.
    }
    
    try {
      new RepositoryNodeImpl(null, nodeConstructor);
      fail("A null session should have caused a failure. (2)");
    } catch (NullPointerException e) {
      // Pass
    }
    
    try {
      new RepositoryNodeImpl(m_session, null);
      fail("A null node should have caused a failure. (2)");
    } catch (NullPointerException e) {
      // Pass
    }

    // Verify that constructing a node, then reconstructing it via the 
    // 2-parameter constructor, generates the same node.
    rnConstructor = new RepositoryNodeImpl(m_session, nodeConstructor,
        k_stemFile, k_urlDefault);
    rnDuplicate = new RepositoryNodeImpl(m_session, nodeConstructor);
    
    assertEquals(rnConstructor, rnDuplicate);
  }
  
  
  // Aside from the real constructor, there is also the method called 
  // 'constructor', which is supposed to generate either RepositoryNode
  // or RepositoryFile.
  
  public final void testConstructor2() throws Exception {
    Node nodeTestConstructor2File;
    Node nodeTestConstructor2Node;    
    RepositoryFile rfOrig;
    RepositoryNode rnOrig;
    RepositoryNode rfTest;
    
    nodeTestConstructor2File = m_nodeRoot.addNode("testConstructor2File");
    nodeTestConstructor2Node = m_nodeRoot.addNode("testConstructor2Node");
    m_nodeRoot.save();
    
    // Verify that, given a file, we get back a file.
    rfOrig = new RepositoryFileImpl(m_session, 
        nodeTestConstructor2File, k_stemFile, k_urlDefault);
    rfTest = RepositoryNodeImpl.constructor(m_session, 
        nodeTestConstructor2File, k_stemFile, k_urlDefault);
    assertTrue(rfTest instanceof RepositoryFileImpl);
    
    // Verify that, given a node, we get back a node.
    rnOrig = new RepositoryNodeImpl(m_session, 
        nodeTestConstructor2Node, k_stemFile, k_urlDefault);
    rfTest = RepositoryNodeImpl.constructor(m_session, 
        nodeTestConstructor2Node, k_stemFile, k_urlDefault);
    assertTrue(rfTest instanceof RepositoryNodeImpl);    
  }
  
  /**
   * Test method for {@link org.lockss.repository.v2.RepositoryNode#getChildCount(boolean, boolean)}.
   */
  public final void testGetChildCountBooleanBoolean() throws Exception {
    byte [] arbyContent;
    RepositoryFile[] arrfGetChildCountFiles;
    RepositoryNode[] arrnGetChildCountChildren;
    InputStream istrContent;
    Node nodeGetChildCount;
    Node nodeGetChildCount2;
    Node nodeGetChildCount3;
    int numChildren;
    RepositoryFileVersion rfvContent;
    RepositoryNode rnGetChildCount;
    
    nodeGetChildCount = m_nodeRoot.addNode("testGetChildCount");
    m_nodeRoot.save();
    
    // Create content.
    arbyContent = new byte[1];
    arbyContent[0] = 125;
    
    // Test for just one level of children nodes.
    rnGetChildCount = new RepositoryNodeImpl(m_session, nodeGetChildCount, 
        k_stemFile, k_urlDefault);
    
    assertEquals(0, rnGetChildCount.getChildCount());
    
    arrnGetChildCountChildren = new RepositoryNode[k_maxChildren];
    for (numChildren = 1; numChildren <= k_maxChildren; numChildren++) {
      arrnGetChildCountChildren[numChildren - 1] = rnGetChildCount.
        makeNewRepositoryNode("Child" + numChildren);
      
      assertEquals(numChildren, rnGetChildCount.getChildCount());
    }

    // Test multiple levels of children nodes.
    nodeGetChildCount2 = m_nodeRoot.addNode("testGetChildCount2");
    m_nodeRoot.save();
    rnGetChildCount = new RepositoryNodeImpl(m_session, nodeGetChildCount2, 
        k_stemFile, k_urlDefault);
    
    arrnGetChildCountChildren = new RepositoryNode[k_maxChildren + 1];
    arrnGetChildCountChildren[0] = rnGetChildCount.makeNewRepositoryNode("root");
    
    for (numChildren = 1; numChildren <= k_maxChildren; numChildren++) {
      arrnGetChildCountChildren[numChildren] = 
        arrnGetChildCountChildren[numChildren - 1].
        makeNewRepositoryNode("Child" + numChildren);
      
      assertEquals(1, rnGetChildCount.getChildCount());
    }

    // Test multiple files under this node.
    nodeGetChildCount3 = m_nodeRoot.addNode("testGetChildCount3");
    m_nodeRoot.save();
    rnGetChildCount = new RepositoryNodeImpl(m_session, nodeGetChildCount3, 
        k_stemFile, k_urlDefault);
    arrfGetChildCountFiles = new RepositoryFileImpl[k_maxChildren];
    
    for (numChildren = 1; numChildren <= k_maxChildren; numChildren++) {
      arrfGetChildCountFiles[numChildren - 1] = 
        rnGetChildCount.makeNewRepositoryFile("Child" + numChildren);
      
      istrContent = new ByteArrayInputStream(arbyContent);      
      rfvContent = arrfGetChildCountFiles[numChildren - 1].createNewVersion();
      rfvContent.setInputStream(istrContent);
      rfvContent.commit();
      arrfGetChildCountFiles[numChildren - 1].setPreferredVersion(rfvContent);
     
      assertEquals(numChildren, rnGetChildCount.getChildCount());
      
      istrContent.close();
    }
    
    // Now, delete some of the files.
    // Note that deleting files does NOT change the number of nodes.
    for (numChildren = 1; numChildren <= k_maxChildren; numChildren++) {
      arrfGetChildCountFiles[numChildren - 1].getPreferredVersion().delete();
      
      assertEquals(k_maxChildren, rnGetChildCount.getChildCount());
    }
    
    // Future test: combine both files and nodes, have many multi-level 
    // trees.  
  }

  /**
   * Test method for {@link org.lockss.repository.v2.RepositoryNode#getTreeContentSize(org.lockss.daemon.CachedUrlSetSpec, boolean, boolean)}.
   */
  
  public final void testGetTreeContentSizeCachedUrlSetSpecBooleanBoolean() throws Exception {
    byte [] arbyContent;
    int countFilePerNode;
    int countNode;
    int countVersionPerFile;
    CachedUrlSetSpec cussExcluded;
    CachedUrlSetSpec cussIncluded;
    int i;
    InputStream istrContent;
    Node nodeGetTreeContentSize;
    RepositoryFile rfIter;
    RepositoryFileVersion rfvIter;
    RepositoryNode rnGetTreeContentSize;
    RepositoryNode rnIter;
    
    nodeGetTreeContentSize = m_nodeRoot.addNode("getTreeContentSize");
    m_nodeRoot.save();
    
    arbyContent = new byte[k_sizeFile];
    for (i=0; i < k_sizeFile; i++) {
      arbyContent[i] = (byte) (65 + (i % 26));   // "ABCD...YZAB...YZAB..."
    }
    
    rnGetTreeContentSize = new RepositoryNodeImpl(m_session, nodeGetTreeContentSize, 
        k_stemFile, k_urlDefault);
    
    for (countNode = 0; countNode < k_numNodes; countNode++) {
      rnIter = rnGetTreeContentSize.makeNewRepositoryNode("Node" + countNode);
      
      for (countFilePerNode = 0; countFilePerNode < k_numFilesPerNode; countFilePerNode++) {
        rfIter = rnIter.makeNewRepositoryFile("File" + countFilePerNode);
        
        for (countVersionPerFile = 0; countVersionPerFile < k_numVersionsPerFile; countVersionPerFile++) {
          istrContent = new ByteArrayInputStream(arbyContent);
          
          rfvIter = rfIter.createNewVersion();
          rfvIter.setInputStream(istrContent);
          rfvIter.commit();
        } // countVersionPerFile
      }  // countFilePerNode
    }  // countNode
    
    assertEquals(k_numNodes * k_numFilesPerNode * k_numVersionsPerFile * k_sizeFile, 
        rnGetTreeContentSize.getTreeContentSize(null, true, false));
    
    cussIncluded = new AuCachedUrlSetSpec();
    assertEquals(k_numNodes * k_numFilesPerNode * k_numVersionsPerFile * k_sizeFile,
        rnGetTreeContentSize.getTreeContentSize(cussIncluded, true, false));
    
    cussExcluded = new SingleNodeCachedUrlSetSpec("http://not.the.right.url.com");
    assertEquals(0,
        rnGetTreeContentSize.getTreeContentSize(cussExcluded, true, false));
    
    // Future tests should include some, but not all, of the elements in the
    // repository node.
  }

  /**
   * Test method for {@link org.lockss.repository.v2.RepositoryNode#getFileList(org.lockss.daemon.CachedUrlSetSpec, boolean)}.
   */
  public final void testGetFileListCachedUrlSetSpecBoolean() throws Exception {
    byte[] arbyContent;
    CachedUrlSetSpec cussExcluded;  // PG rated
    CachedUrlSetSpec cussIncluded;  // PG-13 rated
    InputStream istrContent;
    Node nodeDeleted;
    Node nodeNotDeleted;
    RepositoryFile rfDeleted;
    RepositoryFile rfNotDeleted;
    RepositoryNode rnDeleted;
    RepositoryNode rnNotDeleted;
    List<RepositoryFile> lirfResult;
    RepositoryFileVersion rfvDeleted;
    RepositoryFileVersion rfvNotDeleted;
    
    nodeDeleted = m_nodeRoot.addNode("testGetFileListDeleted");
    nodeNotDeleted = m_nodeRoot.addNode("testGetFileListNotDeleted");
    m_session.save();
    m_session.refresh(true);
    
    arbyContent = new byte[1];
    arbyContent[0] = 23; 
    
    // Construct rnDeleted.
    istrContent = new ByteArrayInputStream(arbyContent);
    rnDeleted = new RepositoryNodeImpl(m_session, nodeDeleted,
        k_stemFile, k_urlDefault+"/Deleted");
    rfDeleted = rnDeleted.makeNewRepositoryFile("test1");
    rfvDeleted = rfDeleted.createNewVersion();
    rfvDeleted.setInputStream(istrContent);
    rfvDeleted.commit();
    rfDeleted.setPreferredVersion(rfvDeleted);
    rfvDeleted.delete();  /* The significant difference */
    
    // Construct rnNotDeleted.
    
    istrContent = new ByteArrayInputStream(arbyContent);
    rnNotDeleted = new RepositoryNodeImpl(m_session, nodeNotDeleted, 
        k_stemFile, k_urlDefault+"/NotDeleted");
    rfNotDeleted = rnNotDeleted.makeNewRepositoryFile("test2");
    rfvNotDeleted = rfNotDeleted.createNewVersion();
    rfvNotDeleted.setInputStream(istrContent);
    rfvNotDeleted.commit();
    rfNotDeleted.setPreferredVersion(rfvNotDeleted);

    // Test: filter == null
    lirfResult = rnDeleted.getFileList(null, false);
    assertEquals(0, lirfResult.size());
    
    lirfResult = rnNotDeleted.getFileList(null, false);
    assertEquals(1, lirfResult.size());
    assertEquals(rfNotDeleted, lirfResult.get(0));
    
    // filter includes url
    cussIncluded = new AuCachedUrlSetSpec();
    
    lirfResult = rnDeleted.getFileList(cussIncluded, false);
    assertEquals(0, lirfResult.size());
    
    lirfResult = rnNotDeleted.getFileList(cussIncluded, false);
    assertEquals(1, lirfResult.size());
    assertEquals(rfNotDeleted, lirfResult.get(0));
    
    // filter does not include url
    cussExcluded = new SingleNodeCachedUrlSetSpec("http://www.not-even-close.com");
    lirfResult = rnDeleted.getFileList(cussExcluded, false);
    assertEquals(0, lirfResult.size());
    
    lirfResult = rnNotDeleted.getFileList(cussExcluded, false);
    assertEquals(0, lirfResult.size());

    // includeDeleted = false
    lirfResult = rnDeleted.getFileList(null, false);
    assertEquals(0, lirfResult.size());
    
    lirfResult = rnNotDeleted.getFileList(null, false);
    assertEquals(1, lirfResult.size());
    assertEquals(rfNotDeleted, lirfResult.get(0));
    
    // includeDeleted = true
    lirfResult = rnDeleted.getFileList(null, true);
    assertEquals(1, lirfResult.size());
    assertEquals(rfDeleted, lirfResult.get(0));
    
    lirfResult = rfNotDeleted.getFileList(null, true);
    assertEquals(1, lirfResult.size());
    assertEquals(rfNotDeleted, lirfResult.get(0));
    
    // Future tests should have multiple nodes, and multiple files per
    // node.
  }

  /**
   * Test method for {@link org.lockss.repository.v2.RepositoryNode#getNodeUrl()}.
   */
  // Used by the test for URLs
  private static int k_lenUrl = 30;
  private static int k_numURL = 100;
  
  public final void testGetNodeUrl() throws Exception {
    // Repeat the tests from RepositoryFile.    
    int countURL;
    Node nodeGetNodeUrl;
    RepositoryNode rnChild;
    RepositoryNode rnGetNodeUrl;
    StringBuffer sbUrl;
    String url = null;
    String url2;
    String urlCombined;
    
    nodeGetNodeUrl = m_nodeRoot.addNode("testGetNodeUrl");
    m_nodeRoot.save();
    
    for (countURL = 0; countURL < k_numURL; countURL++) {
      sbUrl = new StringBuffer();
      sbUrl.append("http://");
      
      sbUrl.append(createRandomText(k_lenUrl));
      url = sbUrl.toString();
      
      rnGetNodeUrl = new RepositoryNodeImpl(m_session, 
          nodeGetNodeUrl, k_stemFile, url);
      
      assertEquals(url, rnGetNodeUrl.getNodeUrl());
    }
    
    // And verify that we get the last URL when we restart...
    rnGetNodeUrl = new RepositoryNodeImpl(m_session, nodeGetNodeUrl);
    assertEquals(url, rnGetNodeUrl.getNodeUrl());

    // New test: a child node of a parent node should concatenate
    // the new nodes into the old ones.
    url2 = createRandomText(k_lenUrl);
    rnChild = rnGetNodeUrl.makeNewRepositoryNode(url2);

    urlCombined = url + "/" + url2;
    assertEquals(urlCombined, rnChild.getNodeUrl());
  }


  /**
   * Test method for {@link org.lockss.repository.v2.RepositoryNode#getFiles(int, boolean)}.
   */
  public final void testGetFilesIntBoolean() throws Exception {
    byte[] arbyContent;
    InputStream istrContent;
    Node nodeDeleted;
    Node nodeNotDeleted;
    RepositoryFile rfDeleted;
    RepositoryFile rfNotDeleted;
    RepositoryFile[] arrfResult;
    RepositoryFileVersion rfvDeleted;
    RepositoryFileVersion rfvNotDeleted;
    RepositoryNode rnDeleted;
    RepositoryNode rnNotDeleted;
    
    nodeDeleted = m_nodeRoot.addNode("testGetFilesDeleted");
    nodeNotDeleted = m_nodeRoot.addNode("testGetFilesNotDeleted");
    m_session.save();
    m_session.refresh(true);
    
    arbyContent = new byte[1];
    arbyContent[0] = 4;  // http://xkcd.com/221/
    
    // Construct rfDeleted.
    istrContent = new ByteArrayInputStream(arbyContent);
    rnDeleted = new RepositoryNodeImpl(m_session, nodeDeleted, 
        k_stemFile, k_urlDefault+"/Deleted");
    rfDeleted = rnDeleted.makeNewRepositoryFile("deleted");
    rfvDeleted = rfDeleted.createNewVersion();
    rfvDeleted.setInputStream(istrContent);
    rfvDeleted.commit();
    rfDeleted.setPreferredVersion(rfvDeleted);
    rfvDeleted.delete();

    // Construct rfNotDeleted.
    istrContent = new ByteArrayInputStream(arbyContent);
    rnNotDeleted = new RepositoryNodeImpl(m_session, nodeNotDeleted, 
        k_stemFile, k_urlDefault+"/NotDeleted");
    rfNotDeleted = rnNotDeleted.makeNewRepositoryFile("notDeleted");
    rfvNotDeleted = rfNotDeleted.createNewVersion();
    rfvNotDeleted.setInputStream(istrContent);
    rfvNotDeleted.commit();
    rfNotDeleted.setPreferredVersion(rfvNotDeleted);

    // Test: maxVersions > 1
    arrfResult = rnDeleted.getFiles(53, false);
    assertEquals(0, arrfResult.length);
    
    arrfResult = rnNotDeleted.getFiles(67, false);
    assertEquals(1, arrfResult.length);
    assertEquals(rfNotDeleted, arrfResult[0]);
    
    // maxVersions == 1
    arrfResult = rnDeleted.getFiles(1, false);
    assertEquals(0, arrfResult.length);
    
    arrfResult = rnNotDeleted.getFiles(1, false);
    assertEquals(1, arrfResult.length);
    assertEquals(rfNotDeleted, arrfResult[0]);
    
    // maxVersions < 1
    arrfResult = rnDeleted.getFiles(0, false);
    assertEquals(0, arrfResult.length);
    
    arrfResult = rnNotDeleted.getFiles(0, false);
    assertEquals(0, arrfResult.length);

    // includeDeleted = false
    arrfResult = rnDeleted.getFiles(1, false);
    assertEquals(0, arrfResult.length);
    
    arrfResult = rnNotDeleted.getFiles(1, false);
    assertEquals(1, arrfResult.length);
    assertEquals(rfNotDeleted, arrfResult[0]);
    
    // includeDeleted = true
    arrfResult = rnDeleted.getFiles(1, true);
    assertEquals(1, arrfResult.length);
    assertEquals(rfDeleted, arrfResult[0]);
    
    arrfResult = rnNotDeleted.getFiles(1, true);
    assertEquals(1, arrfResult.length);
    assertEquals(rfNotDeleted, arrfResult[0]);   
  }

  
  /**
   * Test method for {@link org.lockss.repository.v2.RepositoryNode#makeNewRepositoryFile(java.lang.String)}.
   */
  public final void testMakeNewRepositoryFile() throws Exception {
    List<RepositoryFile> lirfFiles;
    Node nodeNewRepositoryFile;
    RepositoryFile rfChild;
    RepositoryNode rnNewRepositoryFile;
    String strActualUrl;
    String strExpectedUrl;
    
    nodeNewRepositoryFile = m_nodeRoot.addNode("testMakeNewRepositoryFile");
    rnNewRepositoryFile = new RepositoryNodeImpl(m_session, nodeNewRepositoryFile, 
        k_stemFile, k_urlDefault);
    
    // Construct and test the new repository file...
    rfChild = rnNewRepositoryFile.makeNewRepositoryFile("child");
    
    strExpectedUrl = k_urlDefault + "/child";
    strActualUrl = rfChild.getNodeUrl();
    assertEquals(strExpectedUrl, strActualUrl);
    
    lirfFiles = rnNewRepositoryFile.getFileList(null);
    assertEquals(1, lirfFiles.size());
    assertTrue(rfChild.equals(lirfFiles.get(0)));
    assertEquals(rfChild, lirfFiles.get(0));
  }

  /**
   * Test method for {@link org.lockss.repository.v2.RepositoryNode#makeNewRepositoryNode(java.lang.String)}.
   */
  public final void testMakeNewRepositoryNode() throws Exception {
    Node nodeNewRepositoryFile;
    RepositoryNode rnChild;
    RepositoryNode rnNewRepositoryFile;
    String strActualUrl;
    String strExpectedUrl;
    
    nodeNewRepositoryFile = m_nodeRoot.addNode("testMakeNewRepositoryFile");
    rnNewRepositoryFile = new RepositoryNodeImpl(m_session, nodeNewRepositoryFile, 
        k_stemFile, k_urlDefault);
    
    // Construct and test the new repository file...
    rnChild = rnNewRepositoryFile.makeNewRepositoryFile("child");
    
    strExpectedUrl = k_urlDefault + "/child";
    strActualUrl = rnChild.getNodeUrl();
    assertEquals(strExpectedUrl, strActualUrl);
  }
  
  
  private final String k_strMove = "/tmp/stemfileTestMoveB";
  
  public final void testMove() throws Exception {
    InputStream istrContent;
    List<RepositoryFileVersion> lirfvReturned;
    Node nodeTestMove;
    RepositoryFile rfTestMove;
    RepositoryFileVersion rfvTestMove1;
    RepositoryFileVersion rfvTestMove2;
    RepositoryNode rnTestMove;
    String strText1;
    String strText2;
    
    nodeTestMove = m_nodeRoot.addNode("testMove");
    rnTestMove = new RepositoryNodeImpl(m_session, nodeTestMove, 
        k_stemFile, k_urlDefault);
    
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
    rnTestMove.move(k_strMove);
    
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


  /**
   * Test method for {@link org.lockss.repository.v2.RepositoryNode#setProperties(java.util.Properties)}.
   */
  public final void testSetProperties() throws Exception {
    // The 'properties' are the headers for the WARC file.
    // They only affect the WARC file, and are not (currently) read back.
    // For now, they're not important enough to test -- and they may
    // not be important enough to keep in the program.
  }

  /**
   * Test method for {@link org.lockss.repository.v2.RepositoryNode#loadNodeState(org.lockss.plugin.CachedUrlSet)}.
   */
  public final void testLoadNodeState() throws Exception {
    // Sadly, NodeStateImpl has a private constructor, so can't be tested without
    // either changing that to a public constructor or finding another way
    // to generate NodeStateImpl.  I couldn't easily find a way to generate
    // NodeStateImpl.
  }


  // Private methods, used by the above tests.
  
  /**
   * @param sbUrl
   * @return
   */
  private String createRandomText(int lenUrl) {
    StringBuffer sbUrl;
    int countChar;
    
    sbUrl = new StringBuffer();
    for (countChar = 0; countChar < lenUrl; countChar++) {
      sbUrl.append((char) (ms_random.nextInt(26) + 'A'));
    }
    
    return sbUrl.toString();
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
