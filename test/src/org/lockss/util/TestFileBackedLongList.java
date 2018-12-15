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

import org.apache.commons.collections.primitives.*;
import org.lockss.test.LockssTestCase;

/**
 * <p>
 * Unit tests for the {@link FileBackedLongList} class.
 * </p>
 * 
 * @since 1.74.4
 * @see FileBackedLongList
 */
public class TestFileBackedLongList extends LockssTestCase {

  public void testPopulateAndTraverse() throws Exception {
    try (FileBackedLongList list = new FileBackedLongList()) {
      // Populate the list
      assertTrue(list.isEmpty());
      assertEquals(0, list.size());
      for (int i = 0 ; i < SIZE ; ++i) {
        assertTrue(list.add(i));
        assertFalse(list.isEmpty());
        assertEquals(i + 1, list.size());
      }
      // Traverse forward
      for (int i = 0 ; i < SIZE ; ++i) {
        assertEquals(i, list.get(i));
      }
      // Traverse backward
      for (int i = SIZE - 1 ; i >= 0 ; --i) {
        assertEquals(i, list.get(i));
      }
      // Traverse forward with Iterator
      LongIterator iter = list.iterator();
      for (int i = 0 ; iter.hasNext() ; ++i) {
        assertEquals(i, iter.next());
      }
      // Traverse backward with ListIterator
      LongListIterator listIter = list.listIterator(list.size());
      for (int i = SIZE - 1 ; listIter.hasPrevious() ; --i) {
        assertEquals(i, listIter.previous());
      }
      // Traverse sublist
      LongList sublist = list.subList(10, 30);
      assertEquals(20, sublist.size());
      for (int i = 0 ; i < 20 ; ++i) {
        assertEquals(i + 10, sublist.get(i));
      }
    }
  }
  
  public void testAddAndRemove() throws Exception {
    try (FileBackedLongList list = new FileBackedLongList()) {
      // Populate list
      // 0:0, ..., SIZE-1:SIZE-1
      for (int i = 0 ; i < SIZE ; ++i) {
        list.add(i);
      }
      // Insert 1,000th element
      // 0:0, ..., 998:998, 999:-1, 1000:999, ..., SIZE:SIZE-1
      list.add(999, -1L);
      assertEquals(SIZE + 1, list.size());
      for (int i = 1 ; i < 999 ; ++i) {
        assertEquals(i, list.get(i));
      }
      assertEquals(-1L, list.get(999));
      for (int i = 1000 ; i < list.size() ; ++i) {
        assertEquals(i - 1, list.get(i));
      }
      // Test that add works with edge case
      // 0:0, ..., 998:998, 999:-1, 1000:999, ..., SIZE:SIZE-1, SIZE+1:-2
      list.add(list.size(), -2L);
      assertEquals(SIZE + 2, list.size());
      for (int i = 1 ; i < 999 ; ++i) {
        assertEquals(i, list.get(i));
      }
      assertEquals(-1L, list.get(999));
      for (int i = 1000 ; i < list.size() - 1 ; ++i) {
        assertEquals(i - 1, list.get(i));
      }
      assertEquals(-2L, list.get(list.size() - 1));
      // Remove last element
      // 0:0, ..., 998:998, 999:-1, 1000:999, ..., SIZE:SIZE-1
      assertEquals(-2L, list.removeElementAt(list.size() - 1));
      assertEquals(SIZE + 1, list.size());
      for (int i = 1 ; i < 999 ; ++i) {
        assertEquals(i, list.get(i));
      }
      assertEquals(-1L, list.get(999));
      for (int i = 1000 ; i < list.size() ; ++i) {
        assertEquals(i - 1, list.get(i));
      }
      // Remove 1,000th element
      // 0:0, ..., SIZE-1:SIZE-1
      assertEquals(-1L, list.removeElementAt(999));
      for (int i = 0 ; i < SIZE ; ++i) {
        assertEquals(i, list.get(i));
      }
      // Put removed 1,000th element back and delete it using a sublist
      list.add(999, -1L);
      list.subList(999, 1000).clear();
      for (int i = 0 ; i < SIZE ; ++i) {
        assertEquals(i, list.get(i));
      }
    }
  }

  protected static final int SIZE = 500_000;
  
}
