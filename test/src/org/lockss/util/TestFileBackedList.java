package org.lockss.util;

import java.io.*;
import java.util.*;

import org.lockss.test.LockssTestCase;

public class TestFileBackedList extends LockssTestCase {

  protected File file;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.file = FileBackedList.createTempFile();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    file.delete();
  }
  
  public void testUse() throws Exception {
    int size = 10000;
    FileBackedList<Object> list = new FileBackedList<Object>(giantListIterator(size), file);
    assertEquals(size, list.size());
    // Forward traversal
    for (int i = 0 ; i < size ; ++i) {
      assertEquals(giantListGet(i), list.get(i));
    }
    // Backward traversal
    for (int i = size - 1 ; i >= 0 ; --i) {
      assertEquals(giantListGet(i), list.get(i));
    }
    // Forward Iterator traversal
    Iterator<Object> iterator = list.iterator();
    for (int i = 0 ; i < size ; ++i) {
      assertTrue(iterator.hasNext());
      assertEquals(giantListGet(i), iterator.next());
    }
    // Backward ListIterator traversal
    ListIterator<Object> listIterator = list.listIterator(list.size());
    for (int i = size - 1 ; i >= 0 ; --i) {
      assertTrue(listIterator.hasPrevious());
      assertEquals(giantListGet(i), listIterator.previous());
    }
    // Negate all Integers
    for (int i = 0 ; i < size ; i += 6) {
      int num = i / 6;
      assertEquals(num, list.set(i, -((Integer)giantListGet(i)).intValue()));
    }
    for (int i = 0 ; i < size ; ++i) {
      assertEquals((i % 6 == 0) ? -((Integer)giantListGet(i)).intValue() : giantListGet(i), list.get(i));
    }
    // Add an element
    list.add(0, "hello");
    assertEquals(size + 1, list.size());
    assertEquals("hello", list.get(0));
    for (int i = 0 ; i < size ; ++i) {
      assertEquals((i % 6 == 0) ? -((Integer)giantListGet(i)).intValue() : giantListGet(i), list.get(i + 1));
    }
    // Remove an element
    list.remove(0);
    assertEquals(size, list.size());
    for (int i = 0 ; i < size ; ++i) {
      assertEquals((i % 6 == 0) ? -((Integer)giantListGet(i)).intValue() : giantListGet(i), list.get(i));
    }
    // Sublist
    List<Object> sublist = list.subList(6, 12);
    for (int i = 0 ; i < sublist.size() ; ++i) {
      assertEquals(list.get(i + 6), sublist.get(i));
    }
    sublist.clear();
    assertEquals(size - 6, list.size());
    for (int i = 0 ; i < 6 ; ++i) {
      assertEquals((i % 6 == 0) ? -((Integer)giantListGet(i)).intValue() : giantListGet(i), list.get(i));
    }
    for (int i = 6 ; i < size - 6 ; ++i) {
      assertEquals((i % 6 == 0) ? -((Integer)giantListGet(i + 6)).intValue() : giantListGet(i + 6), list.get(i));
    }
    // Other operations
    assertTrue(list.contains(new Float(2.1234f)));
    assertEquals(7, list.indexOf(new Float(2.1234f)));
  }
  
  public static Object giantListGet(int listIndex) {
    int num = listIndex / 6;
    int typ = listIndex % 6;
    switch (typ) {
      case 0: return new Integer(num);
      case 1: return new Float(0.1234f + num);
      case 2: return new Long(num);
      case 3: return new Double(0.1234 + num);
      case 4: return Integer.toString(num);
      case 5: return Arrays.asList(new Integer(num), new Float(0.1234f + num), Integer.toString(num));
    }
    fail(String.format("Should never happen; listIndex=%d, num=%d, typ=%d", listIndex, num, typ));
    return null;
  }

  public static Iterator<Object> giantListIterator(final int listSize) {
    return new Iterator<Object>() {
      int counter = 0;
      int size = listSize;
      @Override
      public boolean hasNext() {
        return (counter < size);
      }
      @Override
      public Object next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return giantListGet(counter++);
      }
    };
  }

}
