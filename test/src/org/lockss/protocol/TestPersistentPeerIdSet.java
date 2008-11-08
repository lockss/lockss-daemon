/**
 * $Id
 */

/*
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
package org.lockss.protocol;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.lockss.test.*;
import org.lockss.util.*;

/**
 * @author edwardsb
 *
 */
public class TestPersistentPeerIdSet extends LockssTestCase {
  // Constants used by testLoad.
  private final int k_countIpElements = 4;
  private final int k_countPeerIdentities = 5;
  private final static String k_filenameCantBeCreated = "/ahtae/gcjau/etuha";
  private final static String k_strPeerIdentityOne = "TCP:[127.0.0.2]:0";
  private final static String k_strPeerIdentityTwo = "TCP:[192.168.0.128]:0";

  // Variables used throughout the program...
  private MockIdentityManager m_idman;
  
  private File m_fileEmpty;
  private File m_fileMany;
  private File m_fileCantBeCreated;
  private File m_fileOne;
  private File m_fileTest;
  private File m_fileNotExist;
  
  /* (non-Javadoc)
   * @see org.lockss.test.LockssTestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();

    m_idman = new MockIdentityManager();
    m_idman.addPeerIdentity(k_strPeerIdentityOne, new MockPeerIdentity(k_strPeerIdentityOne));
    m_idman.addPeerIdentity(k_strPeerIdentityTwo, new MockPeerIdentity(k_strPeerIdentityTwo));

    m_fileEmpty = FileTestUtil.tempFile("ppis");
    m_fileMany = FileTestUtil.tempFile("ppis");
    m_fileCantBeCreated = new File(k_filenameCantBeCreated);
    m_fileOne = FileTestUtil.tempFile("ppis");
    m_fileTest = FileTestUtil.tempFile("ppis");
    m_fileNotExist = FileTestUtil.tempFile("ppis");
    m_fileNotExist.delete();
  }

  /* (non-Javadoc)
   * @see org.lockss.test.LockssTestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#load()}.
   * 
   * @throws IOException
   */
  public void testLoad() throws IOException {
    PeerIdentity peeridentityOne =
      m_idman.findPeerIdentity(k_strPeerIdentityOne);
    MyPersistentPeerIdSetImpl ppisLoad;
    MyPersistentPeerIdSetImpl ppisStore;
    Set<PeerIdentity> setStore;
    
    // TEST: Return an empty set if we load from a non-existent file.
    ppisLoad = new MyPersistentPeerIdSetImpl(m_fileCantBeCreated, m_idman);

    ppisLoad.load();
    assertTrue(ppisLoad.isEmpty());
    // Not changed, so store() shouldn't try to save, so no error
    ppisLoad.store();
    ppisLoad.add(peeridentityOne);
    // now should cause an error
    try {
      ppisLoad.store();
      fail("store() should have caused an IO Exception when the directory cannot exist. ");
    } catch (IOException e) {
      /* Test passes */
    }
    
    
    // TEST: Save and load an empty set.
    ppisStore = new MyPersistentPeerIdSetImpl(m_fileEmpty, m_idman);
    ppisStore.store();
    
    // Load the empty set.
    ppisLoad = new MyPersistentPeerIdSetImpl(m_fileEmpty, m_idman);
    ppisLoad.load();
    
    // Verify that the loaded set is empty.
    assertTrue(ppisLoad.isEmpty());
    
    m_fileEmpty.delete();
    
    
    // TEST: Save a set with one element.
    ppisStore = new MyPersistentPeerIdSetImpl(m_fileOne, m_idman);
    
    ppisStore.add(peeridentityOne);
    ppisStore.store();
       
    // Load the one-element set.
    ppisLoad = new MyPersistentPeerIdSetImpl(m_fileOne, m_idman);
    ppisLoad.load();
    
    // Verify that the loaded set has the correct element.
    assertTrue(!ppisLoad.isEmpty());
    assertEquals(1, ppisLoad.size());
    assertTrue(ppisLoad.contains(peeridentityOne));
    
    m_fileOne.delete();

    
    // TEST: Save a set with many elements.
    ppisStore = new MyPersistentPeerIdSetImpl(m_fileMany, m_idman);
    setStore = createSetPeerIdentities();
    ppisStore.addAll(setStore);

    // Load the many-elements set.
    ppisLoad = new MyPersistentPeerIdSetImpl(m_fileMany, m_idman);
    ppisLoad.load();
    
    // Verify that the loaded set has the correct elements.
    assertTrue(!ppisLoad.isEmpty());
    assertEquals(k_countPeerIdentities, ppisLoad.size());
    assertTrue(ppisLoad.containsAll(setStore));
    
    m_fileMany.delete();
  }

  public void testLoadNonExistentFile() throws IOException {
    PeerIdentity peeridentityOne =
      m_idman.findPeerIdentity(k_strPeerIdentityOne);
    MyPersistentPeerIdSetImpl ppisOne;
    MyPersistentPeerIdSetImpl ppisTwo;
    Set<PeerIdentity> setStore;
    
    // TEST: Return an empty set if we load from a non-existent file.
    ppisOne = new MyPersistentPeerIdSetImpl(m_fileNotExist, m_idman);
    assertFalse(m_fileNotExist.exists());

    ppisOne.load();
    assertFalse(m_fileNotExist.exists());
    assertTrue(ppisOne.isEmpty());
    ppisOne.add(peeridentityOne);
    ppisOne.store();
    assertTrue(m_fileNotExist.exists());
    
    // Load the one-element set.
    ppisTwo = new MyPersistentPeerIdSetImpl(m_fileNotExist, m_idman);
    ppisTwo.load();
    
    // Verify that the loaded set has the correct element.
    assertFalse(ppisTwo.isEmpty());
    assertEquals(1, ppisTwo.size());
    assertTrue(ppisTwo.contains(peeridentityOne));
  }

  /**
   * Additional test method for {@link org.lockss.protocol.PersistentPeerIdSet#load()}
   * 
   * Multiple instances of 'load()' should not change anything.  This test method 
   * expressly verifies that calling 'load()' multiple times does not cause the
   * source to lose data.
   * 
   * @throws IOException
   */
  public void testDoubleLoad() throws IOException
  {
    PeerIdentity piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    MyPersistentPeerIdSetImpl ppisTest = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    
    ppisTest.load();  // This should be empty.   
    ppisTest.add(piOne);
    ppisTest.load();  // This operation should be a no-op.
      
    assertEquals(1, ppisTest.size());
    
    m_fileTest.delete();
  }
  
  
  /**
   * Additional test method for {@link org.lockss.protocol.PersistentPeerIdSet#load()}
   * 
   * If we store a file at one location, then store an empty set, the empty set
   * should have precedence.
   */
  public void testEmptySetEmptiesFile() throws IOException
  {
    PeerIdentity piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    MyPersistentPeerIdSetImpl ppisTest = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisTest.add(piOne);
    ppisTest.store();
    
    MyPersistentPeerIdSetImpl ppisTest2 = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisTest2.clear();
    ppisTest2.store();
    assertTrue(m_fileTest.exists());
    
    // Verify that loading will give the empty file.
    MyPersistentPeerIdSetImpl ppisTest3 = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisTest3.load();
    assertEquals(0, ppisTest3.size());
  }
  
  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#store()}.
   * 
   * save() has no return value, making it hard to test on its own.
   * 
   * The below test attempts to verify that nothing happens if store() is called
   * many times in a row.
   * 
   * One other test would examine the files created by store(). Write that test if 
   * you have lots and lots of time.
   * 
   * @throws IOException
   */
  public void testStore() throws IOException {
    PeerIdentity piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    PeerIdentity piTwo = m_idman.findPeerIdentity(k_strPeerIdentityTwo);
    MyPersistentPeerIdSetImpl ppisTest = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    
    // Test multiple stores.
    ppisTest.load();  // This should be empty.
    assertTrue(ppisTest.isEmpty());
  
    // Add one to the value, then store.
    ppisTest.add(piOne);
    ppisTest.store();      
    assertEquals(1, ppisTest.size());
    assertTrue(ppisTest.didStore());
    assertTrue(ppisTest.isInMemory());
    ppisTest.didStore = false;
    
    ppisTest.add(piTwo);
    ppisTest.store();      
    assertEquals(2, ppisTest.size());
    assertTrue(ppisTest.didStore());
    ppisTest.didStore = false;

    ppisTest.store();      
    assertFalse(ppisTest.didStore());
      
    ppisTest.remove(piTwo);
    ppisTest.store(true);      
    assertTrue(ppisTest.didStore());
    assertFalse(ppisTest.isInMemory());

    m_fileTest.delete();
    
    // Test store without calling "load" first.
    
    ppisTest = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    
    ppisTest.add(piOne);
    ppisTest.store();
    assertEquals(1, ppisTest.size());
    
    m_fileTest.delete();
  }

  /* The following tests all do things twice:
   *    once without using 'load()' and 'save()', so that all is done by files.
   *    once with 'load()' and 'save', so that all is done by memory.
   */
  
  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#add(org.lockss.protocol.PeerIdentity)}.
   * 
   * @rows IOException
   */
  public void testAdd() throws IOException {
    MyPersistentPeerIdSetImpl ppisAdd;
    PeerIdentity piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test add() without load() and save()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    assertTrue(ppisAdd.isEmpty());
    ppisAdd.add(piOne);
    assertFalse(ppisAdd.isEmpty());
    assertEquals(1, ppisAdd.size());
    assertTrue(ppisAdd.contains(piOne));
    m_fileTest.delete();
    
    // Test add() WITH load() and store().
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisAdd.load();
    assertTrue(ppisAdd.isEmpty());
    ppisAdd.add(piOne);
    assertFalse(ppisAdd.isEmpty());
    assertEquals(1, ppisAdd.size());
    assertTrue(ppisAdd.contains(piOne));
    ppisAdd.store();
    m_fileTest.delete();
  }

  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#addAll(java.util.Collection)}.
   * 
   * @throws IOException
   */
  public void testAddAll() throws IOException {
    MyPersistentPeerIdSetImpl ppisAdd;
    Set<PeerIdentity> sepiAdd;
    
    sepiAdd = createSetPeerIdentities();
    
    // Test add() without load() and save()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    assertTrue(ppisAdd.isEmpty());
    ppisAdd.addAll(sepiAdd);
    assertFalse(ppisAdd.isEmpty());
    assertEquals(k_countPeerIdentities, ppisAdd.size());
    assertTrue(ppisAdd.containsAll(sepiAdd));
    m_fileTest.delete();
    
    // Test add() WITH load() and store().
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisAdd.load();
    assertTrue(ppisAdd.isEmpty());
    ppisAdd.addAll(sepiAdd);
    assertFalse(ppisAdd.isEmpty());
    assertEquals(k_countPeerIdentities, ppisAdd.size());
    assertTrue(ppisAdd.containsAll(sepiAdd));
    ppisAdd.store();
    m_fileTest.delete();  
  }

  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#clear()}.
   */
  public void testClear() throws IOException {
    // Test clear() without load() and store()
    MyPersistentPeerIdSetImpl ppisAdd;
    PeerIdentity piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test add() without load() and save()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    assertTrue(ppisAdd.isEmpty());
    ppisAdd.add(piOne);
    assertFalse(ppisAdd.isEmpty());
    assertEquals(1, ppisAdd.size());
    ppisAdd.clear();
    assertTrue(ppisAdd.isEmpty());
    assertEquals(0, ppisAdd.size());
    m_fileTest.delete();
     
    // Test clear() with load() and store()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisAdd.load();
    assertTrue(ppisAdd.isEmpty());
    ppisAdd.add(piOne);
    assertFalse(ppisAdd.isEmpty());
    assertEquals(1, ppisAdd.size());
    ppisAdd.clear();
    assertTrue(ppisAdd.isEmpty());
    assertEquals(0, ppisAdd.size());
    ppisAdd.store();
    m_fileTest.delete();

  }

  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#contains(java.lang.Object)}.
   * 
   * At the time that I wrote this, this test was identical to testAdd.  If new tests on .contains()
   * are needed, add them to the end of this method.
   * 
   * @throws IOException
   */
  public void testContains() throws IOException {
    MyPersistentPeerIdSetImpl ppisAdd;
    PeerIdentity piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    PeerIdentity piTwo = m_idman.findPeerIdentity(k_strPeerIdentityTwo);
    
    // Test add() without load() and save()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    assertTrue(ppisAdd.isEmpty());
    ppisAdd.add(piOne);
    assertFalse(ppisAdd.isEmpty());
    assertEquals(1, ppisAdd.size());
    assertTrue(ppisAdd.contains(piOne));
    assertFalse(ppisAdd.contains(piTwo));
    m_fileTest.delete();
    
    // Test add() WITH load() and store().
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisAdd.load();
    assertTrue(ppisAdd.isEmpty());
    ppisAdd.add(piOne);
    assertFalse(ppisAdd.isEmpty());
    assertEquals(1, ppisAdd.size());
    assertTrue(ppisAdd.contains(piOne));
    assertFalse(ppisAdd.contains(piTwo));
    ppisAdd.store();
    m_fileTest.delete();
  }

  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#containsAll(java.util.Collection)}.
   * 
   * At the time that I wrote this, this test was identical to testAddAll.  If new tests on .containsAll()
   * are needed, add them to the end of this method.
   * 
   * @throws IOException
   */
  public void testContainsAll() throws IOException {
    MyPersistentPeerIdSetImpl ppisAdd;
    Set<PeerIdentity> sepiAdd;
    
    sepiAdd = createSetPeerIdentities();
    
    // Test add() without load() and save()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    assertTrue(ppisAdd.isEmpty());
    ppisAdd.addAll(sepiAdd);
    assertFalse(ppisAdd.isEmpty());
    assertEquals(k_countPeerIdentities, ppisAdd.size());
    assertTrue(ppisAdd.containsAll(sepiAdd));
    m_fileTest.delete();
    
    // Test add() WITH load() and store().
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisAdd.load();
    assertTrue(ppisAdd.isEmpty());
    ppisAdd.addAll(sepiAdd);
    assertFalse(ppisAdd.isEmpty());
    assertEquals(k_countPeerIdentities, ppisAdd.size());
    assertTrue(ppisAdd.containsAll(sepiAdd));
    ppisAdd.store();
    m_fileTest.delete();  
  }

  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#equals(java.lang.Object)}.
   * 
   * @throws IOException
   */
  public void testEquals() throws IOException {
    // Note: equals doesn't examine the disk, so I'm not writing both 'load'/'store' and no-
    // 'load'/'store' values.
    File fileTest1 = FileTestUtil.tempFile("ppis");
    File fileTest2 = fileTest1;
    File fileTest3 = FileTestUtil.tempFile("ppis");
    MyPersistentPeerIdSetImpl ppisEquals1;
    MyPersistentPeerIdSetImpl ppisEquals1b;
    MyPersistentPeerIdSetImpl ppisEquals2;
    MyPersistentPeerIdSetImpl ppisEquals3;
    
    ppisEquals1 = new MyPersistentPeerIdSetImpl(fileTest1, m_idman);
    ppisEquals1b = new MyPersistentPeerIdSetImpl(fileTest1, m_idman);    
    ppisEquals2 = new MyPersistentPeerIdSetImpl(fileTest2, m_idman);
    ppisEquals3 = new MyPersistentPeerIdSetImpl(fileTest3, m_idman);
    
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
   * The only sane test on hash code is to verify that two hashes that should be the same
   * actually are the same.
   * 
   * Important note: any tests that verify that two PPIDs with different values create different 
   * hashes can only be probably (not definitely) true.  Therefore, I'm not including that test.
   * 
   * @throws IOException
   */
  public void testHashCode() throws IOException {
    File fileTest1 = FileTestUtil.tempFile("ppis");
    File fileTest2 = fileTest1;
    PeerIdentity piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    MyPersistentPeerIdSetImpl ppisEquals1 = new MyPersistentPeerIdSetImpl(fileTest1, m_idman);
    ppisEquals1.add(piOne);
    
    MyPersistentPeerIdSetImpl ppisEquals2 = new MyPersistentPeerIdSetImpl(fileTest2, m_idman);;
    ppisEquals2.add(piOne);
    
    assertEquals(ppisEquals1.hashCode(), ppisEquals2.hashCode());
  }

  
  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#isEmpty()}.
   * 
   * @throws IOException
   */
  public void testIsEmpty() throws IOException {
    MyPersistentPeerIdSetImpl ppisAdd;
    PeerIdentity piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test add() without load() and save()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    assertTrue(ppisAdd.isEmpty());
    ppisAdd.add(piOne);
    assertFalse(ppisAdd.isEmpty());
    m_fileTest.delete();
    
    // Test add() WITH load() and store().
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisAdd.load();
    assertTrue(ppisAdd.isEmpty());
    ppisAdd.add(piOne);
    assertFalse(ppisAdd.isEmpty());
    ppisAdd.store();
    m_fileTest.delete();
  }

  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#iterator()}.
   * 
   * Notice that calling an iterator on something that has not been load()ed is automatically
   * an exception.
   * 
   * @throws IOException
   */
  public void testIterator() throws IOException {
    Iterator<PeerIdentity> iterAdd;
    PeerIdentity piTest;
    MyPersistentPeerIdSetImpl ppisAdd;
    Set<PeerIdentity> sepiAdd;
    
    // Verify that if it's not in memory, it's an UnsupportedOperationException.
    sepiAdd = createSetPeerIdentities();
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    try {
      ppisAdd.addAll(sepiAdd);
      iterAdd = ppisAdd.iterator();
      fail("testIterator failed: it should have thrown an UnsupportedOperationException.");
    } catch (UnsupportedOperationException e1) {
      /* Passes the test. */
    }
    m_fileTest.delete();

    // Verify that if it is in memory, the iterator exists and works
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisAdd.load();
    ppisAdd.addAll(sepiAdd);
    iterAdd = ppisAdd.iterator();
    assertSameElements(sepiAdd, SetUtil.fromIterator(iterAdd));
      
    m_fileTest.delete();
    
  }

  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#remove(java.lang.Object)}.
   * 
   * @throws IOException
   */
  public void testRemove() throws IOException {
    PeerIdentity piOne;
    MyPersistentPeerIdSetImpl ppisAdd;
    Set<PeerIdentity> sepiAdd;
    
    sepiAdd = createSetPeerIdentities();
    piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test remove() without load() and save()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    assertTrue(ppisAdd.isEmpty());
    
    ppisAdd.add(piOne);
    ppisAdd.addAll(sepiAdd);
    assertFalse(ppisAdd.isEmpty());
    assertTrue(ppisAdd.contains(piOne));
    
    ppisAdd.remove(piOne);
    assertFalse(ppisAdd.contains(piOne));
    m_fileTest.delete();

    // Test remove() with load() and store()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisAdd.load();
    
    assertTrue(ppisAdd.isEmpty());
    
    ppisAdd.add(piOne);
    ppisAdd.addAll(sepiAdd);
    assertFalse(ppisAdd.isEmpty());
    assertTrue(ppisAdd.contains(piOne));
    
    ppisAdd.remove(piOne);
    assertFalse(ppisAdd.contains(piOne));
    
    ppisAdd.store();
    m_fileTest.delete();
  }

  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#removeAll(java.util.Collection)}.
   * 
   * @throws IOException
   */
  public void testRemoveAll() throws IOException {
    PeerIdentity piOne;
    MyPersistentPeerIdSetImpl ppisAdd;
    Set<PeerIdentity> sepiAdd;
    
    sepiAdd = createSetPeerIdentities();
    piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test remove() without load() and save()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    assertTrue(ppisAdd.isEmpty());
    
    ppisAdd.add(piOne);
    ppisAdd.addAll(sepiAdd);
    assertFalse(ppisAdd.isEmpty());
    assertTrue(ppisAdd.containsAll(sepiAdd));
    
    ppisAdd.removeAll(sepiAdd);
    assertFalse(ppisAdd.containsAll(sepiAdd));
    m_fileTest.delete();

    // Test remove() with load() and store()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisAdd.load();
    
    assertTrue(ppisAdd.isEmpty());
    
    ppisAdd.add(piOne);
    ppisAdd.addAll(sepiAdd);
    assertFalse(ppisAdd.isEmpty());
    assertTrue(ppisAdd.containsAll(sepiAdd));
    
    ppisAdd.removeAll(sepiAdd);
    assertFalse(ppisAdd.containsAll(sepiAdd));
    
    ppisAdd.store();
    m_fileTest.delete();

  }

  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#retainAll(java.util.Collection)}.
   * 
   * @throws IOException
   */
  public void testRetainAll() throws IOException {
    PeerIdentity piOne;
    MyPersistentPeerIdSetImpl ppisAdd;
    Set<PeerIdentity> sepiAdd;
    
    sepiAdd = createSetPeerIdentities();
    piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test retainAll() without load() and save()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    assertTrue(ppisAdd.isEmpty());
    
    ppisAdd.add(piOne);
    ppisAdd.addAll(sepiAdd);
    assertFalse(ppisAdd.isEmpty());
    assertTrue(ppisAdd.containsAll(sepiAdd));
    
    ppisAdd.retainAll(sepiAdd);
    assertTrue(ppisAdd.containsAll(sepiAdd));
    m_fileTest.delete();

    // Test retainAll() with load() and store()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisAdd.load();
    
    assertTrue(ppisAdd.isEmpty());
    
    ppisAdd.add(piOne);
    ppisAdd.addAll(sepiAdd);
    assertFalse(ppisAdd.isEmpty());
    assertTrue(ppisAdd.containsAll(sepiAdd));
    
    ppisAdd.retainAll(sepiAdd);
    assertTrue(ppisAdd.containsAll(sepiAdd));
    
    ppisAdd.store();
    m_fileTest.delete();
  }

  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#size()}.
   * 
   * @throws IOException
   */
  public void testSize() throws IOException {
    PeerIdentity piOne;
    MyPersistentPeerIdSetImpl ppisAdd;
    Set<PeerIdentity> sepiAdd;
    
    sepiAdd = createSetPeerIdentities();
    piOne = m_idman.findPeerIdentity(k_strPeerIdentityOne);
    
    // Test testSize() without load() and save()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    assertEquals(0, ppisAdd.size());
    
    ppisAdd.add(piOne);
    assertEquals(1, ppisAdd.size());
    
    ppisAdd.addAll(sepiAdd);
    assertEquals(1 + k_countPeerIdentities, ppisAdd.size());
    m_fileTest.delete();

    // Test retainAll() with load() and store()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisAdd.load();
    
    assertEquals(0, ppisAdd.size());
    
    ppisAdd.add(piOne);
    assertEquals(1, ppisAdd.size());
    
    ppisAdd.addAll(sepiAdd);
    assertEquals(1 + k_countPeerIdentities, ppisAdd.size());
    
    ppisAdd.store();
    m_fileTest.delete();
  }

  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#toArray()}.
   * 
   * @throws IOException
   */
  public void testToArray() throws IOException {
    int index;
    MyPersistentPeerIdSetImpl ppisAdd;
    Set<PeerIdentity> sepiAdd;
    Object[] arpeerid;
    
    sepiAdd = createSetPeerIdentities();
    
    // Test toArray() without load() and save()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisAdd.addAll(sepiAdd);
    arpeerid = ppisAdd.toArray();
    
    for (index = 0; index < arpeerid.length; index++) {
      assertTrue(ppisAdd.contains(arpeerid[index]));
    }
    m_fileTest.delete();

    // Test toArray() with load() and store()
    ppisAdd = new MyPersistentPeerIdSetImpl(m_fileTest, m_idman);
    ppisAdd.load();
    
    ppisAdd.addAll(sepiAdd);
    arpeerid = ppisAdd.toArray();
    
    for (index = 0; index < arpeerid.length; index++) {
      assertTrue(ppisAdd.contains(arpeerid[index]));
    }
    
    ppisAdd.store();
    m_fileTest.delete();
  }

  /**
   * Test method for {@link org.lockss.protocol.PersistentPeerIdSet#toArray(T[])}.
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
  private Set<PeerIdentity> createSetPeerIdentities()
      throws IdentityManager.MalformedIdentityKeyException {
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

      peeridentityMany = new MockPeerIdentity(sbIpAddress.toString());
      m_idman.addPeerIdentity(sbIpAddress.toString(), peeridentityMany);    
      setStore.add(peeridentityMany);
    }
    
    return setStore;
  }

  static class MyPersistentPeerIdSetImpl extends PersistentPeerIdSetImpl {
    boolean didStore = false;
    public MyPersistentPeerIdSetImpl(File filePeerId,
				     IdentityManager identityManager) {
      super(filePeerId, identityManager);
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
  }
}
