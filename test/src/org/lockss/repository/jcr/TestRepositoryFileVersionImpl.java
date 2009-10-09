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
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.repository.v2.*;
import org.lockss.test.*;
import org.lockss.util.FileUtil;

import junit.framework.*;

/**
 * @author edwardsb
 *
 */
public class TestRepositoryFileVersionImpl extends LockssTestCase {
  
  // Constants
  private static final String k_nameXml = "LargeDatastore.xml";
  private static final String k_password = "password";
  private static final int k_sizeDeferredStream = 10240;
  private static final String k_stemFile = "TestRepository/Content";
  // NOTE: /tmp/ must exist on your machine for this test to work.
  private static final String k_stemFileAbsolute = "/tmp/stemfile";
  private static final String k_dirXml = "test/src/org/lockss/repository/jcr/TestRepository/";
  private static final String k_strFileMultipleFiles = "MultipleFiles";
  private static final String k_strFileNonexisting = "NonexistingFile";
  private static final String k_urlDefault = "http://www.example.com/example.html";
  private static final String k_username = "username";
  private static final long k_warcMax = 10000;
  
  // These constants can be anything, as long as they are not the same, and not
  // the same length.
  private final static String k_strContentCommit = "commit";
  private final static String k_strContentDiscard = "discard";
  
  // Mostly used by the threads test
  private static final int k_numThreads = 10;
  private static final int k_inCountRepositoriesInThread = 100;
  private static final int k_sizeBuffer = 30;

  // Static variables
  private static Random ms_random = new Random();
  
  // Member variables
  private IdentityManager m_idman;
  private MockLockssDaemon m_ld;
  private List<Event> m_lievents;
  private Node m_nodeRoot;
  private RepositoryImpl m_repos;
  private RepositoryFileImpl m_rfiParent;
  private SimpleBinarySemaphore m_sema;
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
    m_rfiParent = new RepositoryFileImpl(m_session, m_nodeRoot, k_stemFile, k_urlDefault);
    
    m_idman = new MockIdentityManager();
    
    m_ld = getMockLockssDaemon();
    m_ld.startDaemon();
    
    JcrRepositoryHelperFactory.preconstructor(k_warcMax, m_idman, m_ld);
  }

  /**
   * @see junit.framework.TestCase#tearDown()
   * @throws java.lang.Exception
   */
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
      
      // The file name comes from k_stemFile
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
    byte[] arbyMessage;
    byte[] arbyResult;
    Node nodeConstructor;
    Node nodeConstructor2;
    Node nodeConstructorAdd;
    RepositoryFileVersionHarness rfvhOrig;
    RepositoryFileVersionHarness rfvhOrig2;
    RepositoryFileVersionHarness rfvhTestNull;

    // Extreme oddity...
    // The result of 'addNode' is NOT the same as the result of 'getNode'.
    // If you have time, investigate why.
    nodeConstructorAdd = m_nodeRoot.addNode("testConstructor");
    m_session.save();
    m_session.refresh(true);
    nodeConstructor = m_nodeRoot.getNode("testConstructor");
    
    // Set each of the possible parameters to null. Each should cause an
    // exception.
    try {
      rfvhTestNull = new RepositoryFileVersionHarnessImpl(null, nodeConstructor,
          k_stemFile, m_rfiParent);
      fail("testConstructor(4): null for session");
    } catch (NullPointerException e) {
      // Pass.
    }

    try {
      rfvhTestNull = new RepositoryFileVersionHarnessImpl(m_session, null, k_stemFile,
          m_rfiParent);
      fail("testConstructor(4): null for node");
    } catch (NullPointerException e) {
      // Pass.
    }

    try {
      rfvhTestNull = new RepositoryFileVersionHarnessImpl(m_session, 
          nodeConstructor, null, m_rfiParent);
      fail("testConstructor(4): null for file");
    } catch (NullPointerException e) {
      // Pass.
    }

    try {
      rfvhTestNull = new RepositoryFileVersionHarnessImpl(null, nodeConstructor, 
          m_rfiParent);
      fail("testConstructor(3): null for session");
    } catch (NullPointerException e) {
      // Pass.
    }

    try {
      rfvhTestNull = new RepositoryFileVersionHarnessImpl(m_session, null,
          m_rfiParent);
      fail("testConstructor(3): null for node");
    } catch (NullPointerException e) {
      // Pass.
    }

    // This test verifies that the three-element constructor gets information
    // correctly from the four-element constructor.

    arbyMessage = (k_strContentCommit + "constructor").getBytes();
    rfvhOrig = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeConstructor, k_stemFile, m_rfiParent);
    rfvhOrig.setContent(arbyMessage);
    rfvhOrig.commit();

    rfvhOrig2 = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeConstructor, m_rfiParent);
    arbyResult = rfvhOrig2.getContent();
    compareByteArrays(arbyMessage, arbyResult);
        
    // Same test using a differently-created node.
    nodeConstructor2 = m_nodeRoot.getNode("testConstructor");
    
    // Verifying that our two nodes are likely to be the same...
    assertEquals(nodeConstructor.getUUID(), nodeConstructor2.getUUID());    
                
    rfvhOrig2 = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeConstructor2, m_rfiParent);
    arbyResult = rfvhOrig2.getContent();
    compareByteArrays(arbyMessage, arbyResult);
    
    // For simplicity's sake, make sure that we can't commit() the new 
    // repository version.
    try {
      rfvhOrig2.commit();
      fail("Committing twice on a committed node should have caused an " + 
          "exception.");
    } catch (LockssRepositoryException e) {
      // Pass.
    }
    
    // This test verifies that the two-element constructor can be added to,
    // if the original node was not committed.
    nodeConstructor2 = m_nodeRoot.addNode("testConstructor2");
    
    rfvhOrig = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeConstructor2, k_stemFile, m_rfiParent);
    rfvhOrig.setContent(arbyMessage);
    // Notice: No commit.
                
    rfvhOrig2 = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeConstructor2, m_rfiParent);
    rfvhOrig2.setContent(arbyMessage);
    rfvhOrig2.commit();  // This line is the real test.  It would generate an
                       // exception if it didn't know that the node wasn't
                       // committed yet.
    arbyResult = rfvhOrig2.getContent();
    compareByteArrays(arbyMessage, arbyResult);

    cleanRepositoryFile(rfvhOrig);
  }


  public final void testCommit() throws Exception {
    byte arbyContent[];
    Node nodeCommit;
    RepositoryFileVersionHarness rfvhCommit;

    // Many other tests use commit. This routine is for calls to commit() that
    // don't fall elsewhere.

    // Test that commit() with no content causes an exception.
    nodeCommit = m_nodeRoot.addNode("testCommit");
    m_nodeRoot.save();

    rfvhCommit = new RepositoryFileVersionHarnessImpl(m_session, nodeCommit, k_stemFile,
        m_rfiParent);

    // Notice that no content goes in...
    assertFalse(rfvhCommit.hasContent());

    // This should cause an exception.
    try {
      rfvhCommit.commit();
      fail("We should have seen a 'NoTextException' when we committed without text.");
    } catch (NoTextException e) {
      // Pass.
    }
    
    // Test that committing twice causes an exception on the second attempt.
    nodeCommit = m_nodeRoot.addNode("testCommit2");
    m_nodeRoot.save();
    
    arbyContent = new byte[1];
    arbyContent[0] = 5;
    
    rfvhCommit = new RepositoryFileVersionHarnessImpl(m_session, nodeCommit, 
        k_stemFile, m_rfiParent);
    rfvhCommit.setContent(arbyContent);
    
    // The first commit should cast no exception.
    rfvhCommit.commit();
    
    // The second commit should cast an exception.
    try {
      rfvhCommit.commit();
      fail("The second commit should have cast an exception.");
    } catch (LockssRepositoryException e) {
      // Pass.
    }
    
    cleanRepositoryFile(rfvhCommit);
  }

  
  public final void testDelete() throws Exception {
    Node nodeDelete;
    RepositoryFileVersionHarness rfvhDfelete;

    // Construct a RepositoryFile
    nodeDelete = m_nodeRoot.addNode("testDelete");
    m_nodeRoot.save();

    rfvhDfelete = new RepositoryFileVersionHarnessImpl(m_session, nodeDelete, 
        k_stemFile, m_rfiParent);

    // A RepositoryFileVersionHarness should start undeleted.
    assertFalse(rfvhDfelete.isDeleted());

    // A RepositoryFileVersionHarness, after delete is called, should be deleted.
    rfvhDfelete.delete();
    assertTrue(rfvhDfelete.isDeleted());

    // Try calling delete twice in a row. It should do nothing special.
    rfvhDfelete.delete();
    assertTrue(rfvhDfelete.isDeleted());

    // Further tests should include that most routines throw an exception (or
    // indicate an error) when they are called on a deleted node.

    cleanRepositoryFile(rfvhDfelete);
  }

  
  public final void testDiscard() throws Exception {
    Node nodeDiscard;
    Node nodeDiscard2;
    RepositoryFileVersionHarness rfvhDiscard;
    RepositoryFileVersionHarness rfvhDiscard2;

    nodeDiscard = m_nodeRoot.addNode("testDiscard");
    m_nodeRoot.save();

    rfvhDiscard = new RepositoryFileVersionHarnessImpl(m_session, nodeDiscard,
        k_stemFile, m_rfiParent);

    // Set information in the node, then roll back.
    rfvhDiscard.setContent((k_strContentCommit + "testDiscard-2").getBytes());
    rfvhDiscard.discard();

    // Verify that there's no content.
    assertFalse(rfvhDiscard.hasContent());
    
    cleanRepositoryFile(rfvhDiscard);

    // Many other tests use commit. This routine is for calls to discard() that
    // don't fall elsewhere.

    // Test that discard() with no content does nothing special: after the
    // commit, there's still no content.
    nodeDiscard2 = m_nodeRoot.addNode("testDiscard2");
    m_nodeRoot.save();

    rfvhDiscard2 = new RepositoryFileVersionHarnessImpl(m_session, nodeDiscard2,
        k_stemFile, m_rfiParent);

    // Add content -- but don't commit.
    rfvhDiscard2.setContent((k_strContentCommit + "testDiscard-3").getBytes());
    assertFalse(rfvhDiscard2.hasContent());

    rfvhDiscard2.discard();
    assertFalse(rfvhDiscard2.hasContent());
    
    // Test that discard() after a commit() causes an exception.
    rfvhDiscard = new RepositoryFileVersionHarnessImpl(m_session, nodeDiscard, 
        k_stemFile, m_rfiParent);
    rfvhDiscard.setContent((k_strContentCommit + "testDiscard-3").getBytes());
    rfvhDiscard.commit();
    
    try {
      rfvhDiscard.discard();
      fail("testDiscard: the discard() after a commit() should have caused an exception.");
    } catch (LockssRepositoryException e) {
      // Pass.
    }

    cleanRepositoryFile(rfvhDiscard2);
  }



  public final void testGetContent() throws Exception {
    byte[] arbyContent;
    byte[] arbyContent2;
    byte[] arbyResult;
    byte byContent;
    int i, j;
    Node nodeContent;
    RepositoryFileVersionHarness rfvhContent;

    // Construct a content node.
    nodeContent = m_nodeRoot.addNode("testGetContent");
    m_nodeRoot.save();

    // First: verify that we can save and remove an empty string.
    arbyContent = new byte[0];
    rfvhContent = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeContent, k_stemFile, m_rfiParent);
    rfvhContent.setContent(arbyContent);
    rfvhContent.commit();
    arbyResult = rfvhContent.getContent();
    assertEquals(0, arbyResult.length);

    // Now, run tests on saving and removing...
    arbyContent = new byte[10];
    for (i = 0; i <= 25; i++) {
      byContent = (byte) ('A' + i);
      for (j = 0; j < 10; j++) {
        arbyContent[j] = byContent;
      }

      nodeContent = m_nodeRoot.addNode("testGetContent" + i);
      m_nodeRoot.save();

      rfvhContent = new RepositoryFileVersionHarnessImpl(m_session, 
          nodeContent, k_stemFile, m_rfiParent);
      rfvhContent.setContent(arbyContent);
      rfvhContent.commit();
      arbyResult = rfvhContent.getContent();
      compareByteArrays(arbyContent, arbyResult);
    } // for i

    cleanRepositoryFile(rfvhContent);
  } // testGetContent

  
  public final void testGetContentSize() throws Exception {
    int i;
    long sizeTest;
    Node nodeContentSize;
    RepositoryFileVersionHarness rfvhContentSize = null;
    RepositoryFileVersionHarness rfvhContentSize2;    
    StringBuilder sbContent;

    nodeContentSize = m_nodeRoot.addNode("testGetContentSize");
    m_nodeRoot.save();

    // Check content size with contents from 0 to 50 long.
    for (sizeTest = 0; sizeTest < 50; sizeTest++) {
      sbContent = new StringBuilder();
      for (i = 0; i < sizeTest; i++) {
        sbContent.append("*");
      }
      rfvhContentSize = new RepositoryFileVersionHarnessImpl(m_session, 
          nodeContentSize, k_stemFile, m_rfiParent);
      rfvhContentSize.setContent(sbContent.toString().getBytes());
      rfvhContentSize.commit();
      assertEquals(sizeTest, rfvhContentSize.getContentSize());
    }

    // Now, test getting content size with commit and deletion...
    rfvhContentSize = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeContentSize, k_stemFile, m_rfiParent);
    rfvhContentSize.setContent(k_strContentCommit.getBytes());
    rfvhContentSize.commit();
    sizeTest = rfvhContentSize.getContentSize();
    assertEquals(k_strContentCommit.length(), sizeTest);

    // VERY IMPORTANT: deleting a node does NOT affect the
    // content size.
    rfvhContentSize.delete();
    sizeTest = rfvhContentSize.getContentSize();
    assertEquals(k_strContentCommit.length(), sizeTest);

    // Test against discard -- this should remove the content. 
    rfvhContentSize = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeContentSize, k_stemFile, m_rfiParent);
    rfvhContentSize.setContent(k_strContentDiscard.getBytes());
    rfvhContentSize.discard();
    sizeTest = rfvhContentSize.getContentSize();
    assertEquals(0, sizeTest);
    
    // Generate content, load with the 3-value
    // constructor, and verify that we get our content size.
    rfvhContentSize = new RepositoryFileVersionHarnessImpl(m_session,
        nodeContentSize, k_stemFile, m_rfiParent);
    rfvhContentSize.setContent(k_strContentCommit.getBytes());
    rfvhContentSize.commit();
    
    rfvhContentSize2 = new RepositoryFileVersionHarnessImpl(m_session,
        nodeContentSize, m_rfiParent);
    assertEquals(rfvhContentSize.getContentSize(), 
        rfvhContentSize2.getContentSize());
    
    cleanRepositoryFile(rfvhContentSize);
  }

  
  public final void testGetInputStream() throws Exception {
    Node nodeGetInputStream;
    RepositoryFileVersionHarness rfvhGetInputStream;
    String strContent;

    nodeGetInputStream = m_nodeRoot.addNode("testGetInputStream");
    m_nodeRoot.save();

    rfvhGetInputStream = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeGetInputStream, k_stemFile, m_rfiParent);

    // Set the content
    strContent = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    rfvhGetInputStream.setContent(strContent.getBytes());
    rfvhGetInputStream.commit();
    compareStreamAgainstString(rfvhGetInputStream, strContent.getBytes());

    cleanRepositoryFile(rfvhGetInputStream);
  }

  
  public final void testGetProperties() throws Exception {
    byte [] arbyContent;
    
    Node nodeGetProperties;
    String strProp1;
    String strProp2;
    Properties propsGetProperties1;
    Properties propsGetPropertiesTest;
    RepositoryFileVersionHarness rfvhGetProperties;

    nodeGetProperties = m_nodeRoot.addNode("testGetProperties");
    m_nodeRoot.save();

    rfvhGetProperties = new RepositoryFileVersionHarnessImpl(m_session,
        nodeGetProperties, k_stemFile, m_rfiParent);
    
    // Create simple content.
    arbyContent = new byte[3];

    // Create properties.
    propsGetProperties1 = new Properties();
    propsGetProperties1.setProperty("key1", "value1");
    propsGetProperties1.setProperty("key2", "value2");
    propsGetProperties1.setProperty("key3", "value3");

    rfvhGetProperties.setProperties(propsGetProperties1);
    rfvhGetProperties.setContent(arbyContent);
    rfvhGetProperties.commit();

    // Retrieve and test properties.
    propsGetPropertiesTest = rfvhGetProperties.getProperties();

    strProp1 = propsGetProperties1.getProperty("key1");
    strProp2 = propsGetPropertiesTest.getProperty("key1");
    assertEquals(strProp1, strProp2);
       
    strProp1 = propsGetProperties1.getProperty("key2");
    strProp2 = propsGetPropertiesTest.getProperty("key2"); 
    assertEquals(strProp1, strProp2);
        
    strProp1 = propsGetProperties1.getProperty("key3");
    strProp2 = propsGetPropertiesTest.getProperty("key3"); 
    assertEquals(strProp1, strProp2);
     
    cleanRepositoryFile(rfvhGetProperties);
  }

  
  public final void testHasContent() throws Exception {
    Node nodeHasContent;
    RepositoryFileVersionHarness rfvhHasContent;

    nodeHasContent = m_nodeRoot.addNode("testHasContent");
    m_nodeRoot.save();

    rfvhHasContent = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeHasContent, k_stemFile, m_rfiParent);

    // Verify that an unset node has no content.
    assertFalse(rfvhHasContent.hasContent());

    // Add content.  Verify that it does NOT report content before a 'commit'.
    rfvhHasContent.setContent("Yep, there's content.".getBytes());
    assertFalse(rfvhHasContent.hasContent());

    // Verify that after a discard, hasContent() reports no content.
    rfvhHasContent.discard();
    assertFalse(rfvhHasContent.hasContent());

    // Test that content remains after a call to commit(), then discard().
    rfvhHasContent.setContent((k_strContentCommit + "hasContent").getBytes());
    rfvhHasContent.commit();
    assertTrue(rfvhHasContent.hasContent());

    try {
      rfvhHasContent.discard();  // This should cause an exception.
      fail("testHasContent: a discard after a commit should have caused an exception.");
    } catch (LockssRepositoryException e) {
      // Pass...
    }
    assertTrue(rfvhHasContent.hasContent());

    cleanRepositoryFile(rfvhHasContent);
  }

  
  /* This method tests combinations of 'delete' and 'undelete' calls.
   */
  public final void testIsDeleted() throws Exception {
    Node nodeIsDeleted;
    RepositoryFileVersionHarness rfvhIsDeleted;

    nodeIsDeleted = m_nodeRoot.addNode("testIsDeleted");
    m_nodeRoot.save();

    rfvhIsDeleted = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeIsDeleted, k_stemFile, m_rfiParent);

    // Verify that the node doesn't start out deleted.
    assertFalse(rfvhIsDeleted.isDeleted());

    // Delete the node, and verify that it is deleted.
    rfvhIsDeleted.delete();
    assertTrue(rfvhIsDeleted.isDeleted());

    // Undelete the node and verify that it is no longer deleted.
    rfvhIsDeleted.undelete();
    assertFalse(rfvhIsDeleted.isDeleted());

    // Delete the node once more, and verify that it is deleted.
    rfvhIsDeleted.delete();
    assertTrue(rfvhIsDeleted.isDeleted());

    cleanRepositoryFile(rfvhIsDeleted);
  }

  private final byte[] k_arbyTestMoveText = "Test Move test text.".getBytes();
  private final String k_stemTestMove = "/tmp/stemfileTest";
  
  public final void testMove() throws Exception {
    Node nodeMove;
    RepositoryFileVersionHarness rfvhMove;
    
    nodeMove = m_nodeRoot.addNode("testMove");
    m_nodeRoot.save();
    
    rfvhMove = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeMove, k_stemFile, m_rfiParent);
    
    // Put some text in the node.
    rfvhMove.setContent(k_arbyTestMoveText);
    rfvhMove.commit();
    
    rfvhMove.move(k_stemTestMove);
    
    // Verify that we can get the node text back...
    assertTrue(rfvhMove.hasContent());
    compareStreamAgainstString(rfvhMove, k_arbyTestMoveText);
  }
  
  /*
   * Not too much can be done about the set... methods without their equivalent
   * get... method. However, one set of tests belongs here: What happens if the
   * set... method has a NULL argument?
   */

  /**
   * Setting the content to a null value causes a NullPointerException.
   * 
   * @throws Exception
   */
  public final void testSetContent() throws Exception {
    Node nodeSetContent;
    RepositoryFileVersionHarness rfvhSetContent;

    // Construct a RepositoryFileVersionHarness
    nodeSetContent = m_nodeRoot.addNode("testSetContent");
    m_nodeRoot.save();

    rfvhSetContent = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeSetContent, k_stemFile, m_rfiParent);

    // Setting the content with null should cause a NullPointerException.
    try {
      rfvhSetContent.setContent(null);
      fail("testSetContent: did not throw NullPointerException");
    } catch (NullPointerException e) {
      // Pass the test!
    }
  }

  
  public final void testSetInputStream() throws Exception {
    byte arbyContent[];
    InputStream istrContent;
    Node nodeSetInputStream;
    RepositoryFileVersionHarness rfvhSetInputStream;

    // Construct a RepositoryFileVersionHarness
    nodeSetInputStream = m_nodeRoot.addNode("testSetInputStream");
    m_nodeRoot.save();

    rfvhSetInputStream = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeSetInputStream, k_stemFile, m_rfiParent);

    // Setting the input stream with null should cause a NullPointerException.
    try {
      rfvhSetInputStream.setInputStream(null);
      fail("testSetInputStream: did not throw NullPointerException");
    } catch (NullPointerException e) {
      // Pass the test!
    }
    
    // Test that setInputStream() stops working, once commit() has
    // been called.
    arbyContent = new byte[1];
    arbyContent[0] = 99;
    istrContent = new ByteArrayInputStream(arbyContent);
    
    rfvhSetInputStream = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeSetInputStream, k_stemFile, m_rfiParent);
    rfvhSetInputStream.setInputStream(istrContent);
    rfvhSetInputStream.commit();
    
    try {
      rfvhSetInputStream.setInputStream(istrContent);
      fail("SetInputStream should not work after commit()");
    } catch (LockssRepositoryException e) {
      // Pass.
    }
    
    cleanRepositoryFile(rfvhSetInputStream);
  }


  /**
   * It should be acceptable to set a null value for properties.
   */
  public final void testSetProperties() throws Exception {
    byte arbyContent[];
    Node nodeSetProperties;
    Properties props;
    RepositoryFileVersionHarness rfvhSetProperties;

    // Construct a RepositoryFileVersionHarness
    nodeSetProperties = m_nodeRoot.addNode("testSetProperties");
    m_nodeRoot.save();

    rfvhSetProperties = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeSetProperties, k_stemFile, m_rfiParent);

    // Setting the properties with a null value is not okay.
    try {
      rfvhSetProperties.setProperties(null);
      fail("TestRepositoryFileImpl.testSetProperties: You may not set the " +
          "properties with a null value.");
    } catch (NullPointerException e) {
      // Pass!
    }
    
    // Verify that setProperties doesn't work after a commit().
    props = new Properties();
    props.put("Key1", "Value1");
    
    arbyContent = new byte[1];
    arbyContent[0] = 55;
    rfvhSetProperties.setContent(arbyContent);
    rfvhSetProperties.commit();
    
    try {
      rfvhSetProperties.setProperties(props);
      fail("You may not set properties after a commit().");
    } catch (LockssRepositoryException e) {
      // Pass!
    }
  }

  
  /**
   * This method tries several stem files, to make sure that it works 
   * both in directories and without directories.
   *  
   * @throws Exception
   */
  public final void testStemFile() throws Exception {
    byte[] arbyContent;
    byte[] arbyResult;
    Node nodeStemFile;
    RepositoryFileVersionHarness rfvhStemFile;

    // Construct a RepositoryFileVersionHarness
    nodeStemFile = m_nodeRoot.addNode("testSetProperties");
    m_nodeRoot.save();

    rfvhStemFile = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeStemFile, k_stemFileAbsolute, m_rfiParent);

    arbyContent = new byte[1];
    arbyContent[0] = 55;
    rfvhStemFile.setContent(arbyContent);
    rfvhStemFile.commit();
    
    arbyResult = rfvhStemFile.getContent();

    compareByteArrays(arbyContent, arbyResult);
    
    cleanRepositoryFile(rfvhStemFile);
  }

  
  public final void testUndelete() throws Exception {
    Node nodeUndelete;
    RepositoryFileVersionHarness rfvhUndelete;

    // Construct a RepositoryFileVersionHarness
    nodeUndelete = m_nodeRoot.addNode("testUndelete");
    m_nodeRoot.save();

    rfvhUndelete = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeUndelete, k_stemFile, m_rfiParent);

    // A RepositoryFileVersionHarness should start undeleted.
    assertFalse(rfvhUndelete.isDeleted());

    // A RepositoryFileVersionHarness, after delete is called, should be deleted.
    rfvhUndelete.delete();
    assertTrue(rfvhUndelete.isDeleted());

    rfvhUndelete.undelete();
    assertFalse(rfvhUndelete.isDeleted());

    // Try it twice in a row...
    rfvhUndelete.undelete();
    assertFalse(rfvhUndelete.isDeleted());

    cleanRepositoryFile(rfvhUndelete);
  }
  
  // The above tests came from the RepositoryFileVersion class.
  // The below tests are additions, that (originally) came later:
  // specific problems that occurred along the way.
    
  /* Early versions of this program had problems when the content file
   * didn't already exist, but could be written to.
   */
  public void testNonexistingContentFile() throws Exception {
    byte[] arbyMessage;
    byte[] arbyResult;
    File fileNonexisting;
    String stemNonexisting;
    Node nodeConstructor;
    RepositoryFileVersionHarness rfvhNonexisting;

    nodeConstructor = m_nodeRoot.addNode("testNonexistingContentFile");
    m_session.save();
    m_session.refresh(true);

    stemNonexisting = k_dirXml + k_strFileNonexisting;
    fileNonexisting = new File(stemNonexisting);
    if (fileNonexisting.exists()) {
      fileNonexisting.delete();
    }

    arbyMessage = (k_strContentCommit + "constructor").getBytes();

    rfvhNonexisting = new RepositoryFileVersionHarnessImpl(m_session, 
        nodeConstructor, stemNonexisting, m_rfiParent);
    rfvhNonexisting.setContent(arbyMessage);
    rfvhNonexisting.commit();

    arbyResult = rfvhNonexisting.getContent();
    
    compareByteArrays(arbyMessage, arbyResult);
    
    cleanRepositoryFile(rfvhNonexisting);
  }
  
  
  /**
   * Verify that the program generates, as expected, new files.
   */
  
  public void testMultipleFiles() throws Exception {
      byte[] arbyTestData;
      File fileMultipleFiles;
      int iByte;
      int iFile;
      long lFileParameter;
      Node nodeMultipleFiles;
      RepositoryFileVersionHarness rfvhMultipleFiles;
      String stemMultipleFiles;
      String strFilename;
      
      // Delete the files that we're hoping to generate.
      stemMultipleFiles = k_dirXml + k_strFileMultipleFiles;
      
      for (lFileParameter = 1; lFileParameter <= 5; lFileParameter++) {
          strFilename = RepositoryFileVersionImpl
              .createPermanentFileName(stemMultipleFiles, lFileParameter);
          fileMultipleFiles = new File(strFilename);
          fileMultipleFiles.delete();
      }
    
      // Construct a RepositoryFile that will hold the data, each file
      // with max size 10000 long.
      
      nodeMultipleFiles = m_nodeRoot.addNode("testMultipleFiles");
      m_session.save();
      m_session.refresh(true);
          
      // Put 3 files, each 4000 bytes long, into the RepositoryFile.
      arbyTestData = new byte[4000];
      for (iByte=0; iByte < 4000; iByte++) {
          arbyTestData[iByte] = (byte) ((iByte % 26) + 'A');
      }

      for (iFile=0; iFile < 3; iFile++) {
          rfvhMultipleFiles = new RepositoryFileVersionHarnessImpl(m_session, 
            nodeMultipleFiles,
            stemMultipleFiles, 
            m_rfiParent);
          rfvhMultipleFiles.setContent(arbyTestData);
          rfvhMultipleFiles.commit();
      }
    
      // Verify that two (not one) files were generated.
      for (lFileParameter = 1; lFileParameter <= 2; lFileParameter++) {
          strFilename = RepositoryFileVersionImpl
              .createPermanentFileName(stemMultipleFiles, lFileParameter);
          fileMultipleFiles = new File(strFilename);
          assertTrue(fileMultipleFiles.exists());
      }
  }
  

  // Test with many threads
  public void testThreads() {
    Thread[] arth;
    int i;
    
    // Start threads
    arth = new Thread[k_numThreads];
    m_lievents = new Vector<Event>();  // A vector is a synchronized list.
    m_sema = new SimpleBinarySemaphore();
    
    for (i = 0; i < k_numThreads; i++) {
      arth[i] = new Thread(new RepositoryFileTestThread());
      arth[i].start();
    }
    
    // Wait for everyone to be done...
    while (m_lievents.size() < k_numThreads) {
      m_sema.take();
    }
    
    // Examine the results.
    for (Event ev : m_lievents) {
      if (ev != null) {
        fail("testThreads2: A thread failed.  arbyResult = " + 
            ev.arbyResult().toString() + ", arbyTest = " + 
            ev.arbyTest().toString() + ",  rfTest = " + 
            ev.rfTest().toString());
      }
    }
  }

  
  // Extra methods, useful for testing...
  
  private void cleanRepositoryFile(RepositoryFileVersionHarness rfvh) 
  throws LockssRepositoryException, IOException, NoTextException {
    // Does nothing at the moment.
  }


  /**
   * Compares the values in two byte[].
   * 
   */
  private void compareByteArrays(byte[] arby1, byte[] arby2) {
    int i;
    
    if (arby1 == null && arby2 == null) {
      return;
    }
    
    if (arby1 == null || arby2 == null) {
      fail("One boolean array is null, the other is not.");
    }
    
    assertEquals(arby1.length, arby2.length);
    
    for (i=0; i<arby1.length; i++) {
      assertEquals(arby1[i], arby2[i]);
    }
  }
  
  /**
   * @param rfvhGetInputStream
   * @param strContent
   * @throws IOException
   */
  private void compareStreamAgainstString(RepositoryFileVersionHarness rfvhGetInputStream,
      byte[] arbyContent) throws IOException, LockssRepositoryException {
    byte[] arbyCompare = new byte[arbyContent.length];
    InputStream isContent;

    isContent = rfvhGetInputStream.getInputStream();
    isContent.read(arbyCompare);  // NOTE: this only works with small streams.
    compareByteArrays(arbyContent, arbyCompare);

    assertEquals(-1, isContent.read());
  }
  
  
  /**
   * To be run at the (start and) end of every test: verify that no .lock file exists.
   */
  private void checkLockFile() {
    File fileLock;
    
    fileLock = new File(k_dirXml + ".lock");
    assertFalse(".lock file was not removed.", fileLock.exists());
  }

  
  // Helper classes ------------------
  private class Event {
    private byte[] m_arbyResult;
    private byte[] m_arbyTest;
    private RepositoryFileVersion m_rfTest;
    
    public Event(byte[] arbyResult, byte[] arbyTest, RepositoryFileVersion rfTest) {
      m_arbyResult = arbyResult.clone();
      m_arbyTest = arbyTest.clone();
      m_rfTest = rfTest;
    }
    
    public byte[] arbyResult() {
      return m_arbyResult;
    }
    
    public byte[] arbyTest() {
      return m_arbyTest;
    }
    
    public RepositoryFileVersion rfTest() {
      return m_rfTest;
    }
  }
  
  
  // This class uses one RepositoryFileImpl per thread.
  private class RepositoryFileTestThread implements Runnable {
    public void run() {
      byte[] arbyResult;
      byte[] arbyTest;
      byte[] arbyThreadName;
      Event eventResult = null;
      int i;
      int j;
      int inRetrieveLength;
      InputStream istrResult;
      InputStream istrTest;
      Node nodeTestThread;
      RepositoryFileVersion rfTest;
      String strThreadName;
      
      try {
        // Name the thread with something (likely to be) unique...
        arbyThreadName = new byte[20];
        ms_random.nextBytes(arbyThreadName);
        strThreadName = new String(arbyThreadName);
        nodeTestThread = m_nodeRoot.addNode(strThreadName);
        
loop:
        for (i=0; i<k_inCountRepositoriesInThread; i++) {
          Thread.sleep(ms_random.nextInt(100));
          
          rfTest = new RepositoryFileVersionImpl(m_session, nodeTestThread, 
              k_stemFile, m_rfiParent, k_sizeDeferredStream);
          
          // Create some random content.
          arbyTest = new byte[k_sizeBuffer];        
          ms_random.nextBytes(arbyTest);
          
          // Set the content according to what a RepositoryFile uses...
          istrTest = new ByteArrayInputStream(arbyTest);
          rfTest.setInputStream(istrTest);
          rfTest.commit();
          
          // Wait, to allow the threads to interact...
          Thread.sleep(ms_random.nextInt(100));
          
          // Retrieve the text from the repository.
          arbyResult = new byte[k_sizeBuffer];
          istrResult = rfTest.getInputStream();         
          inRetrieveLength = istrResult.read(arbyResult);
          
          if (inRetrieveLength < 0) {
            System.err.println("RepositoryFileTestThread: Retrieved no data.");
          } else if (inRetrieveLength < k_sizeBuffer) {
            System.err.println("RepositoryFileTestThread: Did not retrieve all text.");
          }
          
          // Verify the variables.
          for (j=0; j<inRetrieveLength; j++) {
            if (arbyResult[j] != arbyTest[j]) {
              eventResult = new Event(arbyResult, arbyTest, rfTest);
              break loop;
            }
          }
        }
      } catch (Exception e) {
        System.err.println("RepositoryFileTestThread: Internal test error: " + e.getMessage());
      }
      
      m_lievents.add(eventResult);
      m_sema.give();
    } // public void run().
  } // class RepositoryFileTestThread1
}
