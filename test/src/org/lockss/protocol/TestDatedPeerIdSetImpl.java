package org.lockss.protocol;

import java.io.*;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileObject;


import org.lockss.test.*;
import org.lockss.repository.*;

public class TestDatedPeerIdSetImpl extends LockssTestCase {
  // Constants 
  private final static String k_strPeerIdentityOne = "TCP:[127.0.0.2]:0";
  private final static String k_strPeerIdentityTwo = "TCP:[192.168.0.128]:0";

  // Member Variables
  private MockIdentityManager m_idman;
  private MockRepositoryNode m_node;

  private final static String k_fileOne = "fileOne.ppis";
  private final static String k_fileTest = "fileTest.ppis";
  private final static String k_fileTest2 = "fileTest2.ppis";
  private final static String k_fileTest3 = "fileTest3.ppis";
  private final static String k_fileNotExist = "fileNotExist.ppis";
  private static String tempDirPath;
  private static String url = "http://www.example.com/";

  protected void setUp() throws Exception {
    super.setUp();
    
    m_idman = new MockIdentityManager();
    m_idman.addPeerIdentity(k_strPeerIdentityOne, new MockPeerIdentity(k_strPeerIdentityOne));
    m_idman.addPeerIdentity(k_strPeerIdentityTwo, new MockPeerIdentity(k_strPeerIdentityTwo));
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    m_node = new MockRepositoryNode(url, tempDirPath);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testLoadAndStore() throws IOException {
    MyDatedPeerIdSetImpl dpisStore;
    MyDatedPeerIdSetImpl dpisLoad;
    long dateRandom;
    PeerIdentity peeridentityOne;
        
    // Create a DatedPeerIdSet with a random date.  Verify that it stores and retrieves without exception,
    // and that the dpis2 has the right number of elements.
    dateRandom = (long) (Math.random() * Long.MAX_VALUE);

    // assertFalse(m_fileNotExist.exists());
    dpisStore = new MyDatedPeerIdSetImpl(m_node, k_fileNotExist, m_idman);
    dpisStore.deletePeerIdFile(k_fileNotExist);
    assertFalse(dpisStore.existsPeerIdFile(k_fileNotExist));
    dpisStore.setDate(dateRandom);
    dpisStore.store();
    
    dpisLoad = new MyDatedPeerIdSetImpl(m_node, k_fileNotExist, m_idman);
    dpisLoad.load();
    assertEquals(dateRandom, dpisLoad.getDate());
    assertEquals(0, dpisLoad.size());
    dpisLoad.deletePeerIdFile(k_fileNotExist);
    dpisStore.deletePeerIdFile(k_fileNotExist);
    
    
    // Create a DatedPeerIdSet without a date.  Verify that it stores and retrieves without exception, and
    // that the retrieved date is the default (-1).
    dpisStore = new MyDatedPeerIdSetImpl(m_node, k_fileTest2, m_idman);
    dpisStore.clear();
    dpisStore.store();
    
    dpisLoad = new MyDatedPeerIdSetImpl(m_node, k_fileTest2, m_idman);
    dpisLoad.load();
    assertEquals(DatedPeerIdSetImpl.k_dateDefault, dpisLoad.getDate());
    assertEquals(0, dpisLoad.size());
    dpisStore.deletePeerIdFile(k_fileTest2);
    dpisLoad.deletePeerIdFile(k_fileTest2);
    
    
    // Create a DatedPeerIdSet with a random date and a PeerId.  Verify that the stored peer id and
    // date are retrieved.
    dateRandom = (long) (Math.random() * Long.MAX_VALUE); // Just in case I had a bad choice last time.
    peeridentityOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    dpisStore = new MyDatedPeerIdSetImpl(m_node, k_fileTest3, m_idman);
    dpisStore.add(peeridentityOne);
    dpisStore.setDate(dateRandom);
    dpisStore.store();
       
    // Load the one-element set.
    dpisLoad = new MyDatedPeerIdSetImpl(m_node, k_fileTest3, m_idman);
    dpisLoad.load();
    
    // Verify that the loaded set has the correct element.
    assertTrue(!dpisLoad.isEmpty());
    assertEquals(1, dpisLoad.size());
    assertTrue(dpisLoad.contains(peeridentityOne));
    assertEquals(dateRandom, dpisLoad.getDate());
    dpisStore.deletePeerIdFile(k_fileTest3);
    dpisLoad.deletePeerIdFile(k_fileTest3);
  }

  
  final static int k_numRepeat = 1000;
  
  public void testGetAndSetDate() throws IOException {
    MyDatedPeerIdSetImpl dpisTest;
    int i;
    long dateRandom;
    
    dpisTest = new MyDatedPeerIdSetImpl(m_node, k_fileTest, m_idman);
    
    for (i=0; i<k_numRepeat; i++) {
      dateRandom = (long) (Math.random() * Long.MAX_VALUE);
      
      dpisTest.setDate(dateRandom);
      assertEquals(dateRandom, dpisTest.getDate());
    }
    dpisTest.deletePeerIdFile(k_fileTest);
  }

  static class MyDatedPeerIdSetImpl extends DatedPeerIdSetImpl {
    boolean didStore = false;
    public MyDatedPeerIdSetImpl(RepositoryNode node,
				String fileName,
				IdentityManager identityManager) {
      super(node, fileName, identityManager);
    }

    @Override
    protected void encode(DataOutputStream dos) throws IOException {
      didStore = true;
      super.encode(dos);
    }
    boolean didStore() {
      return didStore;
    }
    boolean isInMemory() {
      return m_isInMemory;
    }
    public boolean deletePeerIdFile(String fileName) {
      try {
        return m_node.getPeerIdFileObject(fileName).delete();
      } catch (FileSystemException ex) {
	fail(ex.toString());
      }
      return false;
    }
    public boolean existsPeerIdFile(String fileName) {
      try {
	return m_node.getPeerIdFileObject(fileName).exists();
      } catch (FileSystemException ex) {
	fail(ex.toString());
      }
      return false;
    }
  }
}

