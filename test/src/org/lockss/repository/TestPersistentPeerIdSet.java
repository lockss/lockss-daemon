/**
 * 
 */
package org.lockss.repository;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.IdentityManagerImpl;
import org.lockss.protocol.PeerIdentity;
import org.lockss.test.LockssTestCase;

/**
 * @author edwardsb
 *
 */
public class TestPersistentPeerIdSet extends LockssTestCase {
  // Constants used by testLoad.
  private final int k_countIpElements = 4;
  private final int k_countPeerIdentities = 5;
  private final static String k_filenameEmpty = "empty.ppis";
  private final static String k_filenameMany = "many.ppis";
  private final static String k_filenameNonexistent = "/ahtae/gcjau/etuha";
  private final static String k_filenameOne = "one.ppis";
  private final static String k_filenameTest = "test.ppis";
  private final static String k_strPeerIdentityOne = "TCP:[127.0.0.2]:0";
  private final static String k_strPeerIdentityTwo = "TCP:[192.168.0.128]:0";

  // Variables used throughout the program...
  private IdentityManager m_idman;
  
  private File m_fileEmpty;
  private File m_fileMany;
  private File m_fileNonexistent;
  private File m_fileOne;
  private File m_fileTest;
  
  /* (non-Javadoc)
   * @see org.lockss.test.LockssTestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();

    m_idman = new IdentityManagerImpl();

    m_fileEmpty = new File(k_filenameEmpty);
    m_fileMany = new File(k_filenameMany);
    m_fileNonexistent = new File(k_filenameNonexistent);
    m_fileOne = new File(k_filenameOne);
    m_fileTest = new File(k_filenameTest);
    
    // Delete files from any previous runs.
    m_fileEmpty.delete();
    m_fileMany.delete();
    m_fileOne.delete();
    m_fileTest.delete();
  }

  /* (non-Javadoc)
   * @see org.lockss.test.LockssTestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#load()}.
   */
  public void testLoad() {
    m_fileEmpty = new File(k_filenameEmpty);
    m_fileMany = new File(k_filenameMany);
    m_fileNonexistent = new File(k_filenameNonexistent);
    m_fileOne = new File(k_filenameOne);
    PeerIdentity peeridentityOne;
    PeerIdentity peeridentityMany;
    PersistentPeerIdSet ppisLoad;
    PersistentPeerIdSet ppisStore;
    Set<PeerIdentity> setStore;
    
    
    // TEST: Return an empty set if we load from a non-existent file.
    ppisLoad = new PersistentPeerIdSetImpl(m_fileNonexistent, m_idman);
    
    try {
      assertTrue(ppisLoad.isEmpty());
      fail("isEmpty() should have caused an IO Exception when the directory cannot exist. ");
    } catch (IOException e) {
      /* Test passes */
    }
    
    
    // TEST: Save and load an empty set.
    ppisStore = new PersistentPeerIdSetImpl(m_fileEmpty, m_idman);
    
    try {
      ppisStore.store();
    } catch (IOException e) {
      fail("store() caused IO Exception: " + e.getMessage());
    }
    
    // Load the empty set.
    ppisLoad = new PersistentPeerIdSetImpl(m_fileEmpty, m_idman);
    
    try {
      ppisLoad.load();
    } catch (IOException e) {
      fail("load() caused IO Exception: " + e.getMessage());
    }
    
    // Verify that the loaded set is empty.
    try {
      assertTrue(ppisLoad.isEmpty());
    } catch (IOException e) {
      fail("isEmpty() caused IO Exception: " + e.getMessage());
    }
        
    
    // TEST: Save a set with one element.
    ppisStore = new PersistentPeerIdSetImpl(m_fileOne, m_idman);
    peeridentityOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    try {
      ppisStore.add(peeridentityOne);
      ppisStore.store();
    } catch (IOException e) {
      fail("store() caused IO Exception: " + e.getMessage());
    }
       
    // Load the one-element set.
    ppisLoad = new PersistentPeerIdSetImpl(m_fileOne, m_idman);
    
    try {
      ppisLoad.load();
    } catch (IOException e) {
      fail("load() caused IO Exception: " + e.getMessage());
    }
    
    // Verify that the loaded set has the correct element.
    try {
      assertTrue(!ppisLoad.isEmpty());
      assertEquals(1, ppisLoad.size());
      assertTrue(ppisLoad.contains(peeridentityOne));
    } catch (IOException e) {
      fail("Verifying that a one-element set has the correct element failed with IO Exception: " + e.getMessage());
    }
    
    // TEST: Save a set with many elements.
    ppisStore = new PersistentPeerIdSetImpl(m_fileMany, m_idman);
    setStore = createSetPeerIdentities();
    
    try {
      ppisStore.addAll(setStore);
    } catch (IOException e) {
      fail("Adding to ppisStore caused an IO Exception: "+ e.getMessage());
    }

    // Load the many-elements set.
    ppisLoad = new PersistentPeerIdSetImpl(m_fileMany, m_idman);
    
    try {
      ppisLoad.load();
    } catch (IOException e) {
      fail("load() caused IO Exception: " + e.getMessage());
    }
    
    // Verify that the loaded set has the correct elements.
    try {
      assertTrue(!ppisLoad.isEmpty());
      assertEquals(k_countPeerIdentities, ppisLoad.size());
      assertTrue(ppisLoad.containsAll(setStore));
    } catch (IOException e) {
      fail("Verifying that a many-element set has the correct elements failed with IO Exception: " + e.getMessage());
    }
    
    // Delete created files.
    m_fileEmpty.delete();
    m_fileMany.delete();
    m_fileOne.delete();
  }

  /**
   * Additional test method for {@link org.lockss.repository.PersistentPeerIdSet#load()}
   * 
   * Multiple instances of 'load()' should not change anything.  This test method 
   * expressly verifies that calling 'load()' multiple times does not cause the
   * source to lose data.
   */
  public void testDoubleLoad()
  {
    PeerIdentity piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    PersistentPeerIdSet ppisTest = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    
    try {
      ppisTest.load();  // This should be empty.
    
      ppisTest.add(piOne);
    
      ppisTest.load();  // This operation should be a no-op.
      
      assertEquals(1, ppisTest.size());
    } catch (IOException e) {
      fail("testDoubleLoad caused an IO Exception: " + e.getMessage());
    }
    
    m_fileTest.delete();
  }
  
  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#store()}.
   * 
   * save() has no return value, making it hard to test on its own.
   * 
   * The below test attempts to verify that nothing happens if store() is called
   * many times in a row.
   * 
   * One other test would examine the files created by store(). Write that test if 
   * you have lots and lots of time.
   */
  public void testStore() {
    PeerIdentity piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    PeerIdentity piTwo = m_idman.findPeerIdentity(k_strPeerIdentityTwo);
    PersistentPeerIdSet ppisTest = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    
    try {
      ppisTest.load();  // This should be empty.
    
      // Add one to the value, then store.
      ppisTest.add(piOne);
      ppisTest.store();      
      assertEquals(1, ppisTest.size());
      
      ppisTest.add(piTwo);
      ppisTest.store();      
      assertEquals(2, ppisTest.size());
      
    } catch (IOException e) {
      fail("testStore caused an IO Exception: " + e.getMessage());
    }
    
    m_fileTest.delete();
  }

  /* The following tests all do things twice:
   *    once without using 'load()' and 'save()', so that all is done by files.
   *    once with 'load()' and 'save', so that all is done by memory.
   */
  
  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#add(org.lockss.protocol.PeerIdentity)}.
   */
  public void testAdd() {
    m_fileTest = new File(k_filenameTest);
    PersistentPeerIdSet ppisAdd;
    PeerIdentity piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test add() without load() and save()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      assertTrue(ppisAdd.isEmpty());
      ppisAdd.add(piOne);
      assertFalse(ppisAdd.isEmpty());
      assertEquals(1, ppisAdd.size());
      assertTrue(ppisAdd.contains(piOne));
    } catch (IOException e) {
      fail("testAdd caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();
    
    // Test add() WITH load() and store().
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.load();
      assertTrue(ppisAdd.isEmpty());
      ppisAdd.add(piOne);
      assertFalse(ppisAdd.isEmpty());
      assertEquals(1, ppisAdd.size());
      assertTrue(ppisAdd.contains(piOne));
      ppisAdd.store();
    } catch (IOException e) {
      fail("testAdd caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();
  }

  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#addAll(java.util.Collection)}.
   */
  public void testAddAll() {
    m_fileTest = new File(k_filenameTest);
    PersistentPeerIdSet ppisAdd;
    Set<PeerIdentity> sepiAdd;
    
    sepiAdd = createSetPeerIdentities();
    
    // Test add() without load() and save()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      assertTrue(ppisAdd.isEmpty());
      ppisAdd.addAll(sepiAdd);
      assertFalse(ppisAdd.isEmpty());
      assertEquals(k_countPeerIdentities, ppisAdd.size());
      assertTrue(ppisAdd.containsAll(sepiAdd));
    } catch (IOException e) {
      fail("testAddAll caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();
    
    // Test add() WITH load() and store().
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.load();
      assertTrue(ppisAdd.isEmpty());
      ppisAdd.addAll(sepiAdd);
      assertFalse(ppisAdd.isEmpty());
      assertEquals(k_countPeerIdentities, ppisAdd.size());
      assertTrue(ppisAdd.containsAll(sepiAdd));
      ppisAdd.store();
    } catch (IOException e) {
      fail("testAddAll caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();  
  }

  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#clear()}.
   */
  public void testClear() {
    // Test clear() without load() and store()
    m_fileTest = new File(k_filenameTest);
    PersistentPeerIdSet ppisAdd;
    PeerIdentity piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test add() without load() and save()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      assertTrue(ppisAdd.isEmpty());
      ppisAdd.add(piOne);
      assertFalse(ppisAdd.isEmpty());
      assertEquals(1, ppisAdd.size());
      ppisAdd.clear();
      assertTrue(ppisAdd.isEmpty());
      assertEquals(0, ppisAdd.size());
    } catch (IOException e) {
      fail("testClear caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();
     
    // Test clear() with load() and store()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.load();
      assertTrue(ppisAdd.isEmpty());
      ppisAdd.add(piOne);
      assertFalse(ppisAdd.isEmpty());
      assertEquals(1, ppisAdd.size());
      ppisAdd.clear();
      assertTrue(ppisAdd.isEmpty());
      assertEquals(0, ppisAdd.size());
      ppisAdd.store();
    } catch (IOException e) {
      fail("testClear caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();

  }

  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#contains(java.lang.Object)}.
   * 
   * At the time that I wrote this, this test was identical to testAdd.  If new tests on .contains()
   * are needed, add them to the end of this method.
   */
  public void testContains() {
    m_fileTest = new File(k_filenameTest);
    PersistentPeerIdSet ppisAdd;
    PeerIdentity piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test add() without load() and save()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      assertTrue(ppisAdd.isEmpty());
      ppisAdd.add(piOne);
      assertFalse(ppisAdd.isEmpty());
      assertEquals(1, ppisAdd.size());
      assertTrue(ppisAdd.contains(piOne));
    } catch (IOException e) {
      fail("testContains caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();
    
    // Test add() WITH load() and store().
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.load();
      assertTrue(ppisAdd.isEmpty());
      ppisAdd.add(piOne);
      assertFalse(ppisAdd.isEmpty());
      assertEquals(1, ppisAdd.size());
      assertTrue(ppisAdd.contains(piOne));
      ppisAdd.store();
    } catch (IOException e) {
      fail("testContains caused IO Exception: " + e.getMessage());
    }    
    m_fileTest.delete();
  }

  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#containsAll(java.util.Collection)}.
   * 
   * At the time that I wrote this, this test was identical to testAddAll.  If new tests on .containsAll()
   * are needed, add them to the end of this method.
   */
  public void testContainsAll() {
    m_fileTest = new File(k_filenameTest);
    PersistentPeerIdSet ppisAdd;
    Set<PeerIdentity> sepiAdd;
    
    sepiAdd = createSetPeerIdentities();
    
    // Test add() without load() and save()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      assertTrue(ppisAdd.isEmpty());
      ppisAdd.addAll(sepiAdd);
      assertFalse(ppisAdd.isEmpty());
      assertEquals(k_countPeerIdentities, ppisAdd.size());
      assertTrue(ppisAdd.containsAll(sepiAdd));
    } catch (IOException e) {
      fail("testContainsAll caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();
    
    // Test add() WITH load() and store().
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.load();
      assertTrue(ppisAdd.isEmpty());
      ppisAdd.addAll(sepiAdd);
      assertFalse(ppisAdd.isEmpty());
      assertEquals(k_countPeerIdentities, ppisAdd.size());
      assertTrue(ppisAdd.containsAll(sepiAdd));
      ppisAdd.store();
    } catch (IOException e) {
      fail("testContainsAll caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();  
  }

  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#equals(java.lang.Object)}.
   */
  public void testEquals() {
    // Note: equals doesn't examine the disk, so I'm not writing both 'load'/'store' and no-
    // 'load'/'store' values.
    File fileTest1 = new File(k_filenameTest);
    File fileTest2 = new File(k_filenameTest);
    File fileTest3 = new File("different.ppis");
    PersistentPeerIdSet ppisEquals1;
    PersistentPeerIdSet ppisEquals1b;
    PersistentPeerIdSet ppisEquals2;
    PersistentPeerIdSet ppisEquals3;
    
    ppisEquals1 = new PersistentPeerIdSetImpl(fileTest1, m_idman);
    ppisEquals1b = new PersistentPeerIdSetImpl(fileTest1, m_idman);    
    ppisEquals2 = new PersistentPeerIdSetImpl(fileTest2, m_idman);
    ppisEquals3 = new PersistentPeerIdSetImpl(fileTest3, m_idman);
    
    // Here come 16 tests: the product of the two sets of PPIS.
    assertTrue(ppisEquals1.equals(ppisEquals1));
    assertTrue(ppisEquals1.equals(ppisEquals1b));
    assertTrue(ppisEquals1.equals(ppisEquals2));
    assertFalse(ppisEquals1.equals(ppisEquals3));

    assertTrue(ppisEquals1b.equals(ppisEquals1));
    assertTrue(ppisEquals1b.equals(ppisEquals1b));
    assertTrue(ppisEquals1b.equals(ppisEquals2));
    assertFalse(ppisEquals1b.equals(ppisEquals3));

    assertTrue(ppisEquals2.equals(ppisEquals1));
    assertTrue(ppisEquals2.equals(ppisEquals1b));
    assertTrue(ppisEquals2.equals(ppisEquals2));
    assertFalse(ppisEquals2.equals(ppisEquals3));

    assertFalse(ppisEquals3.equals(ppisEquals1));
    assertFalse(ppisEquals3.equals(ppisEquals1b));
    assertFalse(ppisEquals3.equals(ppisEquals2));
    assertTrue(ppisEquals3.equals(ppisEquals3));
  }

  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#hashCode()}.
   * 
   * Removed because I can't think of a sane test on hash codes.
   */
//  public void testHashCode() {
//    fail("Not yet implemented");
//  }

  
  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#isEmpty()}.
   */
  public void testIsEmpty() {
    m_fileTest = new File(k_filenameTest);
    PersistentPeerIdSet ppisAdd;
    PeerIdentity piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test add() without load() and save()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      assertTrue(ppisAdd.isEmpty());
      ppisAdd.add(piOne);
      assertFalse(ppisAdd.isEmpty());
    } catch (IOException e) {
      fail("testIsEmpty caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();
    
    // Test add() WITH load() and store().
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.load();
      assertTrue(ppisAdd.isEmpty());
      ppisAdd.add(piOne);
      assertFalse(ppisAdd.isEmpty());
      ppisAdd.store();
    } catch (IOException e) {
      fail("testIsEmpty caused IO Exception: " + e.getMessage());
    }    
    m_fileTest.delete();
  }

  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#iterator()}.
   * 
   * Notice that calling an iterator on something that has not been load()ed is automatically
   * an exception.
   */
  public void testIterator() {
    m_fileTest = new File(k_filenameTest);
    Iterator<PeerIdentity> iterAdd;
    PeerIdentity piTest;
    PersistentPeerIdSet ppisAdd;
    Set<PeerIdentity> sepiAdd;
    
    // Verify that if it's not in memory, it's an UnsupportedOperationException.
    sepiAdd = createSetPeerIdentities();
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.addAll(sepiAdd);
      iterAdd = ppisAdd.iterator();
      fail("testIterator failed: it should have thrown an UnsupportedOperationException.");
    } catch (UnsupportedOperationException e1) {
      /* Passes the test. */
    } catch (IOException e2) {
     fail("testIterator caused IO Exception: " + e2.getMessage()); 
    }
    m_fileTest.delete();

    // Verify that if it is in memory, the iterator exists.
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.load();
      ppisAdd.addAll(sepiAdd);
      iterAdd = ppisAdd.iterator();
      
      while (iterAdd.hasNext()) {
        piTest = iterAdd.next();
        assertTrue(ppisAdd.contains(piTest));
      }
    } catch (UnsupportedOperationException e1) {
      fail("testIterator failed: it should not have thrown an UnsupportedOperationException.  Information: " + e1.getMessage());
    } catch (IOException e2) {
     fail("testIterator caused IO Exception: " + e2.getMessage()); 
    }
    m_fileTest.delete();
    
  }

  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#remove(java.lang.Object)}.
   */
  public void testRemove() {
    m_fileTest = new File(k_filenameTest);
    PeerIdentity piOne;
    PersistentPeerIdSet ppisAdd;
    Set<PeerIdentity> sepiAdd;
    
    sepiAdd = createSetPeerIdentities();
    piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test remove() without load() and save()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      assertTrue(ppisAdd.isEmpty());
      
      ppisAdd.add(piOne);
      ppisAdd.addAll(sepiAdd);
      assertFalse(ppisAdd.isEmpty());
      assertTrue(ppisAdd.contains(piOne));
      
      ppisAdd.remove(piOne);
      assertFalse(ppisAdd.contains(piOne));
    } catch (IOException e) {
      fail("testRemove caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();

    // Test remove() with load() and store()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.load();
      
      assertTrue(ppisAdd.isEmpty());
      
      ppisAdd.add(piOne);
      ppisAdd.addAll(sepiAdd);
      assertFalse(ppisAdd.isEmpty());
      assertTrue(ppisAdd.contains(piOne));
      
      ppisAdd.remove(piOne);
      assertFalse(ppisAdd.contains(piOne));
      
      ppisAdd.store();
    } catch (IOException e) {
      fail("testRemove caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();

  }

  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#removeAll(java.util.Collection)}.
   */
  public void testRemoveAll() {
    m_fileTest = new File(k_filenameTest);
    PeerIdentity piOne;
    PersistentPeerIdSet ppisAdd;
    Set<PeerIdentity> sepiAdd;
    
    sepiAdd = createSetPeerIdentities();
    piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test remove() without load() and save()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      assertTrue(ppisAdd.isEmpty());
      
      ppisAdd.add(piOne);
      ppisAdd.addAll(sepiAdd);
      assertFalse(ppisAdd.isEmpty());
      assertTrue(ppisAdd.containsAll(sepiAdd));
      
      ppisAdd.removeAll(sepiAdd);
      assertFalse(ppisAdd.containsAll(sepiAdd));
    } catch (IOException e) {
      fail("testRemoveAll caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();

    // Test remove() with load() and store()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.load();
      
      assertTrue(ppisAdd.isEmpty());
      
      ppisAdd.add(piOne);
      ppisAdd.addAll(sepiAdd);
      assertFalse(ppisAdd.isEmpty());
      assertTrue(ppisAdd.containsAll(sepiAdd));
      
      ppisAdd.removeAll(sepiAdd);
      assertFalse(ppisAdd.containsAll(sepiAdd));
      
      ppisAdd.store();
    } catch (IOException e) {
      fail("testRemoveAll caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();

  }

  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#retainAll(java.util.Collection)}.
   */
  public void testRetainAll() {
    m_fileTest = new File(k_filenameTest);
    PeerIdentity piOne;
    PersistentPeerIdSet ppisAdd;
    Set<PeerIdentity> sepiAdd;
    
    sepiAdd = createSetPeerIdentities();
    piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test retainAll() without load() and save()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      assertTrue(ppisAdd.isEmpty());
      
      ppisAdd.add(piOne);
      ppisAdd.addAll(sepiAdd);
      assertFalse(ppisAdd.isEmpty());
      assertTrue(ppisAdd.containsAll(sepiAdd));
      
      ppisAdd.retainAll(sepiAdd);
      assertTrue(ppisAdd.containsAll(sepiAdd));
    } catch (IOException e) {
      fail("testRemoveAll caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();

    // Test retainAll() with load() and store()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.load();
      
      assertTrue(ppisAdd.isEmpty());
      
      ppisAdd.add(piOne);
      ppisAdd.addAll(sepiAdd);
      assertFalse(ppisAdd.isEmpty());
      assertTrue(ppisAdd.containsAll(sepiAdd));
      
      ppisAdd.retainAll(sepiAdd);
      assertTrue(ppisAdd.containsAll(sepiAdd));
      
      ppisAdd.store();
    } catch (IOException e) {
      fail("testRetainAll caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();
  }

  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#size()}.
   */
  public void testSize() {
    m_fileTest = new File(k_filenameTest);
    PeerIdentity piOne;
    PersistentPeerIdSet ppisAdd;
    Set<PeerIdentity> sepiAdd;
    
    sepiAdd = createSetPeerIdentities();
    piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test testSize() without load() and save()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      assertEquals(0, ppisAdd.size());
      
      ppisAdd.add(piOne);
      assertEquals(1, ppisAdd.size());
      
      ppisAdd.addAll(sepiAdd);
      assertEquals(1 + k_countPeerIdentities, ppisAdd.size());
    } catch (IOException e) {
      fail("testRemoveAll caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();

    // Test retainAll() with load() and store()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.load();
      
      assertEquals(0, ppisAdd.size());
      
      ppisAdd.add(piOne);
      assertEquals(1, ppisAdd.size());
      
      ppisAdd.addAll(sepiAdd);
      assertEquals(1 + k_countPeerIdentities, ppisAdd.size());
      
      ppisAdd.store();
    } catch (IOException e) {
      fail("testRemoveAll caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();
  }

  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#toArray()}.
   */
  public void testToArray() {
    m_fileTest = new File(k_filenameTest);
    int index;
    PersistentPeerIdSet ppisAdd;
    Set<PeerIdentity> sepiAdd;
    Object[] arpeerid;
    
    sepiAdd = createSetPeerIdentities();
    
    // Test toArray() without load() and save()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.addAll(sepiAdd);
      arpeerid = ppisAdd.toArray();
      
      for (index = 0; index < arpeerid.length; index++) {
        assertTrue(ppisAdd.contains(arpeerid[index]));
      }
    } catch (IOException e) {
      fail("testContainsAll caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();

    // Test toArray() with load() and store()
    ppisAdd = new PersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.load();
      
      ppisAdd.addAll(sepiAdd);
      arpeerid = ppisAdd.toArray();
      
      for (index = 0; index < arpeerid.length; index++) {
        assertTrue(ppisAdd.contains(arpeerid[index]));
      }
      
      ppisAdd.store();
    } catch (IOException e) {
      fail("testContainsAll caused IO Exception: " + e.getMessage());
    }
    m_fileTest.delete();
  }

  /**
   * Test method for {@link org.lockss.repository.PersistentPeerIdSet#toArray(T[])}.
   * 
   * I am not testing this, because the only data type that implements PeerIdentity is PeerIdentityImpl --
   * and that is tested with testToArray.  
   * 
   * If you use toArray(T[]), then you should add tests to this function.
   */
//  public void testToArrayTArray() {
//    fail("Not yet implemented");
//  }
  
  
  // ------ Private methods --------
  private Set<PeerIdentity> createSetPeerIdentities() {
    int numIpElement;
    int numPeerIdentity;
    PeerIdentity peeridentityMany;
    StringBuilder sbIpAddress;
    Set<PeerIdentity> setStore;
    
    setStore = new HashSet<PeerIdentity>();
    for (numPeerIdentity = 0; numPeerIdentity < k_countPeerIdentities; numPeerIdentity++) {
    
      // Create a random address of the form 'TCP:[AAA.AAA.AAA.AAA]:BBBBB', where AAA is between 
      // 1 and 254 and B is between 0 and 65536.  (I prefer not to use either 0 or 255 in these addresses; 
      // often, they have special meanings.  For example, 127.0.0.1 is the source machine, and 
      // addresses ending in 255 often are multicast.)
    
      sbIpAddress = new StringBuilder();
      sbIpAddress.append("TCP:[");
      
      for (numIpElement = 0; numIpElement < k_countIpElements; numIpElement++) {
        if (numIpElement != 0) {
          sbIpAddress.append(".");
        }
      
        sbIpAddress.append(Integer.toString((int) (Math.random() * 253 + 1)));
      }
 
      sbIpAddress.append("]:");
      sbIpAddress.append(Integer.toString((int) (Math.random() * 65536)));
      
      peeridentityMany = m_idman.findPeerIdentity(sbIpAddress.toString());
    
      setStore.add(peeridentityMany);
    }
    
    return setStore;
  }


}
