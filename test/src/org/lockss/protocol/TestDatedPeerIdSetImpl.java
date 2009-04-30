package org.lockss.protocol;

import java.io.File;
import java.io.IOException;

import org.lockss.test.*;
import org.lockss.util.*;

public class TestDatedPeerIdSetImpl extends LockssTestCase {
  // Constants 
  private final static String k_strPeerIdentityOne = "TCP:[127.0.0.2]:0";
  private final static String k_strPeerIdentityTwo = "TCP:[192.168.0.128]:0";

  // Member Variables
  private MockIdentityManager m_idman;

  private File m_fileOne;
  private File m_fileTest;
  private File m_fileTest2;
  private File m_fileTest3;
  private File m_fileNotExist;

  private Streamer m_strfileOne;
  private Streamer m_strfileTest;
  private Streamer m_strfileTest2;
  private Streamer m_strfileTest3;
  private Streamer m_strfileNotExist;


  protected void setUp() throws Exception {
    super.setUp();
    
    m_idman = new MockIdentityManager();
    m_idman.addPeerIdentity(k_strPeerIdentityOne, new MockPeerIdentity(k_strPeerIdentityOne));
    m_idman.addPeerIdentity(k_strPeerIdentityTwo, new MockPeerIdentity(k_strPeerIdentityTwo));

    m_fileOne = FileTestUtil.tempFile("ppis");
    m_strfileOne = new StreamerFile(m_fileOne);
    
    m_fileTest = FileTestUtil.tempFile("ppis");
    m_strfileTest = new StreamerFile(m_fileTest);

    m_fileTest2 = FileTestUtil.tempFile("ppis");
    m_strfileTest2 = new StreamerFile(m_fileTest2);

    m_fileTest3 = FileTestUtil.tempFile("ppis");
    m_strfileTest3 = new StreamerFile(m_fileTest3);

    m_fileNotExist = FileTestUtil.tempFile("ppis");
    m_fileNotExist.delete();
    m_strfileNotExist = new StreamerFile(m_fileNotExist);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testLoadAndStore() throws IOException {
    DatedPeerIdSet dpisStore;
    DatedPeerIdSet dpisLoad;
    long dateRandom;
    PeerIdentity peeridentityOne;
        
    // Create a DatedPeerIdSet with a random date.  Verify that it stores and retrieves without exception,
    // and that the dpis2 has the right number of elements.
    dateRandom = (long) (Math.random() * Long.MAX_VALUE);
    
    assertFalse(m_fileNotExist.exists());
    dpisStore = new DatedPeerIdSetImpl(m_strfileNotExist, m_idman);
    assertFalse(m_fileNotExist.exists());
    dpisStore.setDate(dateRandom);
    dpisStore.store();
    
    dpisLoad = new DatedPeerIdSetImpl(m_strfileNotExist, m_idman);
    dpisLoad.load();
    assertEquals(dateRandom, dpisLoad.getDate());
    assertEquals(0, dpisLoad.size());
    
    
    // Create a DatedPeerIdSet without a date.  Verify that it stores and retrieves without exception, and
    // that the retrieved date is the default (-1).
    dpisStore = new DatedPeerIdSetImpl(m_strfileTest2, m_idman);
    dpisStore.clear();
    dpisStore.store();
    
    dpisLoad = new DatedPeerIdSetImpl(m_strfileTest2, m_idman);
    dpisLoad.load();
    assertEquals(DatedPeerIdSetImpl.k_dateDefault, dpisLoad.getDate());
    assertEquals(0, dpisLoad.size());
    
    
    // Create a DatedPeerIdSet with a random date and a PeerId.  Verify that the stored peer id and
    // date are retrieved.
    dateRandom = (long) (Math.random() * Long.MAX_VALUE); // Just in case I had a bad choice last time.
    peeridentityOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    dpisStore = new DatedPeerIdSetImpl(m_strfileTest3, m_idman);
    dpisStore.add(peeridentityOne);
    dpisStore.setDate(dateRandom);
    dpisStore.store();
       
    // Load the one-element set.
    dpisLoad = new DatedPeerIdSetImpl(m_strfileTest3, m_idman);
    dpisLoad.load();
    
    // Verify that the loaded set has the correct element.
    assertTrue(!dpisLoad.isEmpty());
    assertEquals(1, dpisLoad.size());
    assertTrue(dpisLoad.contains(peeridentityOne));
    assertEquals(dateRandom, dpisLoad.getDate());
  }

  
  final static int k_numRepeat = 1000;
  
  public void testGetAndSetDate() throws IOException {
    DatedPeerIdSet dpisTest;
    int i;
    long dateRandom;
    
    dpisTest = new DatedPeerIdSetImpl(m_strfileTest, m_idman);
    
    for (i=0; i<k_numRepeat; i++) {
      dateRandom = (long) (Math.random() * Long.MAX_VALUE);
      
      dpisTest.setDate(dateRandom);
      assertEquals(dateRandom, dpisTest.getDate());
    }
  }

}
