/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.tdb;

import java.util.*;

import org.lockss.test.LockssTestCase;

public class TestTdb extends LockssTestCase {

  public void testEmpty() throws Exception {
    Tdb tdb = new Tdb();
    assertEmpty(tdb.getPublishers());
    assertEmpty(tdb.getTitles());
    assertEmpty(tdb.getAus());
  }
  
  public void testTdb() throws Exception {
    Tdb tdb = new Tdb();
    
    Map<String, String> publisher1Map = new HashMap<String, String>();
    publisher1Map.put(Publisher.NAME, "Publisher 1");
    Publisher publisher1 = new Publisher();
    tdb.addPublisher(publisher1);
    
    Map<String, String> title11Map = new HashMap<String, String>();
    title11Map.put(Title.NAME, "Title 11");
    Title title11 = new Title(publisher1);
    tdb.addTitle(title11);
    
    Au au111 = new Au(title11);
    au111.put(Au.NAME, "AU 111");
    tdb.addAu(au111);
    Au au112 = new Au(title11);
    au112.put(Au.NAME, "AU 112");
    tdb.addAu(au112);
    Au au113 = new Au(title11);
    au113.put(Au.NAME, "AU 113");
    tdb.addAu(au113);
    
    Map<String, String> title12Map = new HashMap<String, String>();
    title12Map.put(Title.NAME, "Title 12");
    Title title12 = new Title(publisher1);
    tdb.addTitle(title12);
    
    Au au121 = new Au(title12);
    au121.put(Au.NAME, "AU 121");
    tdb.addAu(au121);
    Au au122 = new Au(title12);
    au122.put(Au.NAME, "AU 122");
    tdb.addAu(au122);
    Au au123 = new Au(title12);
    au123.put(Au.NAME, "AU 123");
    tdb.addAu(au123);
    
    List<Publisher> publishers = tdb.getPublishers();
    assertEquals(1, publishers.size());
    assertSame(publisher1, publishers.get(0));
    try {
      publishers.add(publisher1);
      fail("Collection should be unmodifiable");
    }
    catch (UnsupportedOperationException expected) {
      // Expected
    }

    List<Title> titles = tdb.getTitles();
    assertEquals(2, titles.size());
    assertSame(title11, titles.get(0));
    assertSame(title12, titles.get(1));
    try {
      titles.add(title11);
      fail("Collection should be unmodifiable");
    }
    catch (UnsupportedOperationException expected) {
      // Expected
    }
    
    List<Au> aus = tdb.getAus();
    assertEquals(6, aus.size());
    assertSame(au111, aus.get(0));
    assertSame(au112, aus.get(1));
    assertSame(au113, aus.get(2));
    assertSame(au121, aus.get(3));
    assertSame(au122, aus.get(4));
    assertSame(au123, aus.get(5));
    try {
      aus.add(au111);
      fail("Collection should be unmodifiable");
    }
    catch (UnsupportedOperationException expected) {
      // Expected
    }
  }
  
}
