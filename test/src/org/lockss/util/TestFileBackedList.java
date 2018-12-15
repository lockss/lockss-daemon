/*

Copyright (c) 2000-2018, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.util;

import java.util.*;

import org.lockss.test.LockssTestCase;

/**
 * <p>
 * Unit tests for the {@link FileBackedList} class.
 * </p>
 * 
 * @since 1.74.4
 */
public class TestFileBackedList extends LockssTestCase {

  public void testUse() throws Exception {
    try (FileBackedList<Object> list = new FileBackedList<Object>(giantListIterator(SIZE))) {
      assertEquals(SIZE, list.size());
      // Forward traversal
      for (int i = 0 ; i < SIZE ; ++i) {
        assertEquals(giantListGet(i), list.get(i));
      }
      // Backward traversal
      for (int i = SIZE - 1 ; i >= 0 ; --i) {
        assertEquals(giantListGet(i), list.get(i));
      }
      // Forward Iterator traversal
      Iterator<Object> iterator = list.iterator();
      for (int i = 0 ; i < SIZE ; ++i) {
        assertTrue(iterator.hasNext());
        assertEquals(giantListGet(i), iterator.next());
      }
      // Backward ListIterator traversal
      ListIterator<Object> listIterator = list.listIterator(list.size());
      for (int i = SIZE - 1 ; i >= 0 ; --i) {
        assertTrue(listIterator.hasPrevious());
        assertEquals(giantListGet(i), listIterator.previous());
      }
      // Negate all Integers
      for (int i = 0 ; i < SIZE ; i += 6) {
        int num = i / 6;
        assertEquals(num, list.set(i, -((Integer)giantListGet(i)).intValue()));
      }
      for (int i = 0 ; i < SIZE ; ++i) {
        assertEquals((i % 6 == 0) ? -((Integer)giantListGet(i)).intValue() : giantListGet(i), list.get(i));
      }
      // Add an element
      list.add(0, "hello");
      assertEquals(SIZE + 1, list.size());
      assertEquals("hello", list.get(0));
      for (int i = 0 ; i < SIZE ; ++i) {
        assertEquals((i % 6 == 0) ? -((Integer)giantListGet(i)).intValue() : giantListGet(i), list.get(i + 1));
      }
      // Remove an element
      list.remove(0);
      assertEquals(SIZE, list.size());
      for (int i = 0 ; i < SIZE ; ++i) {
        assertEquals((i % 6 == 0) ? -((Integer)giantListGet(i)).intValue() : giantListGet(i), list.get(i));
      }
      // Sublist
      List<Object> sublist = list.subList(6, 12);
      for (int i = 0 ; i < sublist.size() ; ++i) {
        assertEquals(list.get(i + 6), sublist.get(i));
      }
      sublist.clear();
      assertEquals(SIZE - 6, list.size());
      for (int i = 0 ; i < 6 ; ++i) {
        assertEquals((i % 6 == 0) ? -((Integer)giantListGet(i)).intValue() : giantListGet(i), list.get(i));
      }
      for (int i = 6 ; i < SIZE - 6 ; ++i) {
        assertEquals((i % 6 == 0) ? -((Integer)giantListGet(i + 6)).intValue() : giantListGet(i + 6), list.get(i));
      }
      // Other operations
      assertTrue(list.contains(new Float(2.1234f)));
      assertEquals(7, list.indexOf(new Float(2.1234f)));
    }
  }
  
  protected static final int SIZE = 100_000;
  
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
      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

}
