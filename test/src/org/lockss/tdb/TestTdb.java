/*

Copyright (c) 2000-2017, Board of Trustees of Leland Stanford Jr. University,
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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
    
    Map<String, String> publisher1Map = new LinkedHashMap<String, String>();
    publisher1Map.put(Publisher.NAME, "Publisher 1");
    Publisher publisher1 = new Publisher();
    tdb.addPublisher(publisher1);
    
    Map<String, String> title11Map = new LinkedHashMap<String, String>();
    title11Map.put(Title.NAME, "Title 11");
    Title title11 = new Title(publisher1);
    tdb.addTitle(title11);
    
    Au au111 = new Au(null, title11);
    au111.put(Au.NAME, "AU 111");
    tdb.addAu(au111);
    Au au112 = new Au(null, title11);
    au112.put(Au.NAME, "AU 112");
    tdb.addAu(au112);
    Au au113 = new Au(null, title11);
    au113.put(Au.NAME, "AU 113");
    tdb.addAu(au113);
    
    Map<String, String> title12Map = new LinkedHashMap<String, String>();
    title12Map.put(Title.NAME, "Title 12");
    Title title12 = new Title(publisher1);
    tdb.addTitle(title12);
    
    Au au121 = new Au(null, title12);
    au121.put(Au.NAME, "AU 121");
    tdb.addAu(au121);
    Au au122 = new Au(null, title12);
    au122.put(Au.NAME, "AU 122");
    tdb.addAu(au122);
    Au au123 = new Au(null, title12);
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
