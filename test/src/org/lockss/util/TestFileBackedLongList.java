package org.lockss.util;

import java.io.File;

import org.apache.commons.collections.primitives.*;
import org.lockss.test.LockssTestCase;

public class TestFileBackedLongList extends LockssTestCase {

  protected File file;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.file = File.createTempFile(getClass().getSimpleName(), ".bin");
    this.file.deleteOnExit();
  }

  @Override
  protected void tearDown() throws Exception {
    this.file.delete();
    super.tearDown();
  }
  
  public void testPopulateAndTraverse() throws Exception {
    int size = 10000;
    FileBackedLongList list = null;
    try {
      list = new FileBackedLongList(file);
      assertTrue(list.isEmpty());
      assertEquals(0, list.size());
      for (int i = 0 ; i < size ; ++i) {
        assertTrue(list.add(i));
        assertFalse(list.isEmpty());
        assertEquals(i + 1, list.size());
      }
      // Traverse forward
      for (int i = 0 ; i < size ; ++i) {
        assertEquals(i, list.get(i));
      }
      // Traverse backward
      for (int i = size - 1 ; i >= 0 ; --i) {
        assertEquals(i, list.get(i));
      }
      // Traverse forward with Iterator
      LongIterator iter = list.iterator();
      for (int i = 0 ; iter.hasNext() ; ++i) {
        assertEquals(i, iter.next());
      }
      // Traverse backward with ListIterator
      LongListIterator listIter = list.listIterator(list.size());
      for (int i = size - 1 ; listIter.hasPrevious() ; --i) {
        assertEquals(i, listIter.previous());
      }
    }
    finally {
      if (list != null) {
        list.release();
      }
    }
  }
  
  public void testAddAndRemove() throws Exception {
    int size = 10000;
    FileBackedLongList list = null;
    try {
      list = new FileBackedLongList(file);
      for (int i = 0 ; i < size ; ++i) {
        list.add(i);
      }
      list.add(999, -1L);
      assertEquals(size + 1, list.size());
      for (int i = 1 ; i < 999 ; ++i) {
        assertEquals(i, list.get(i));
      }
      assertEquals(-1L, list.get(999));
      for (int i = 1000 ; i < list.size() ; ++i) {
        assertEquals(i - 1, list.get(i));
      }
      list.add(list.size(), -2L);
      assertEquals(size + 2, list.size());
      assertEquals(-2L, list.get(list.size() - 1));
      
      assertEquals(-2L, list.removeElementAt(list.size() - 1));
      assertEquals(size + 1, list.size());
      for (int i = 1 ; i < 999 ; ++i) {
        assertEquals(i, list.get(i));
      }
      assertEquals(-1L, list.get(999));
      for (int i = 1000 ; i < list.size() ; ++i) {
        assertEquals(i - 1, list.get(i));
      }
      assertEquals(-1L, list.removeElementAt(999));
      for (int i = 0 ; i < size ; ++i) {
        assertEquals(i, list.get(i));
      }
    }
    finally {
      if (list != null) {
        list.release();
      }
    }
  }
  
}
