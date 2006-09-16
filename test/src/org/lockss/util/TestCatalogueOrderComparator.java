/*
 * $Id: TestCatalogueOrderComparator.java,v 1.7 2006-09-16 07:19:48 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * Test class for org.lockss.util.CatalogueOrderComparator
 */
public class TestCatalogueOrderComparator extends LockssTestCase {
  CatalogueOrderComparator coc;

  public void setUp() {
    coc = new CatalogueOrderComparator();
  }

  public void testDeleteInitial() {
    assertSame("foo", coc.deleteInitial("foo", "bar"));
    assertEquals("foo", coc.deleteInitial("bar foo", "bar"));
    assertEquals("foo", coc.deleteInitial("bar  foo", "bar"));
    assertEquals("barfoo", coc.deleteInitial("barfoo", "bar"));
  }

  public void testDeleteAll() {
    assertSame("foo", coc.deleteAll("foo", "bar"));
    assertEquals("foo", coc.deleteAll("f.o,o..", ".,"));
    assertEquals("foo", coc.deleteAll("fo--o", "--!"));
  }

  public void testDeleteSpaceBetweenInitials() {
    assertEquals("Journal of IBM 2004", coc.deleteSpaceBetweenInitials("Journal of I B M 2004"));
    assertEquals("UPS manuel", coc.deleteSpaceBetweenInitials("U P S manuel"));
    assertEquals("Journal of HP", coc.deleteSpaceBetweenInitials("Journal of H P"));
    assertEquals("Who am I magazine", coc.deleteSpaceBetweenInitials("Who am I magazine"));
    assertEquals("Wild Space", coc.deleteSpaceBetweenInitials("Wild         Space"));
  }

  public void testReplaceAccentedChar(){
    assertEquals("toe", coc.replaceAccentedChar("to\u00eb"));
    assertEquals("Apple", coc.replaceAccentedChar("\u00c1pple"));
    assertEquals("low", coc.replaceAccentedChar("l\u00f4w"));
    assertEquals("coupe", coc.replaceAccentedChar("coup\u00e9"));
    assertEquals("naive", coc.replaceAccentedChar("na\u00efve"));
    assertEquals("garcon", coc.replaceAccentedChar("gar\u00e7on"));
  }

  public void testXlate() {
    assertEquals("Tao of Pooh", coc.xlate("The Tao of Pooh"));
    assertEquals("Tao of Pooh", coc.xlate("A  Tao of Pooh"));
    assertEquals("Tao of Pooh", coc.xlate("An Tao of Pooh"));
    assertEquals("IBM Tech Journal", coc.xlate("I.B.M. Tech Journal"));
    assertEquals("Journal of IBM", coc.xlate("Journal of I. B. M."));
  }
  
  public void testfindNumsPadZero() {
     assertEquals( "Volume of the World History", coc.findNumsPadZero("Volume of the World History", "0123456789",6) );  
     assertEquals( 3, coc.getNumStrLen("the 455 World 88 History", 4) );
     assertEquals( 2, coc.getNumStrLen("the 455 World 88 History", 14) );
     assertEquals( "Volume 1 of the World History",  coc.findNumsPadZero("Volume 1 of the World History", "0123456789", 0) );
     assertEquals( "Volume 001 of the World History", coc.findNumsPadZero("Volume 1 of the World History", "0123456789", 3) );
     assertEquals( "Volume 00002 of the World 00008 History",coc.findNumsPadZero("Volume 2 of the World 8 History", "0123456789", 5) );
     assertEquals( "Volume 11223 of the World 11228 History",coc.findNumsPadZero("Volume 11223 of the World 11228 History", "0123456789", 3) );
     assertEquals( "Volume 1 of the World 28 History of 8 men and 7 women",
                   coc.findNumsPadZero("Volume 1 of the World 28 History of 8 men and 7 women", "0123456789", 1) );         
     assertEquals( "Volume 1 of the World 28 History of 8 men and 7 women",
                   coc.findNumsPadZero("Volume 1 of the World 28 History of 8 men and 7 women", "0123456789", 0) );     
     assertEquals( "Volume 00001234 of the World 00001111 History",
                   coc.findNumsPadZero("Volume 1234 of the World 1111 History", "0123456789", 8) );
     assertEquals( "Volume 00012345678 of the World 00000224466 History",
                   coc.findNumsPadZero("Volume 12345678 of the World 224466 History", "0123456789", 11) );
  }

  public void testOrder() {
    // titles in sorted order
    String[] titles = {
      "The Aardvark of the Baskervilles",
      "An Apple and its Eve",
      "Applied Semiotics / Sémiotique appliquée Volume 1",
      "Applied Semiotics / Sémiotique appliquée Volume 2",
      "Applied Semiotics / Sémiotique appliquée Volume 3",
      "Applied Semiotics / Sémiotique appliquée Volume 4",
      "Applied Semiotics / Sémiotique appliquée Volume 5",
      "Applied Semiotics / Sémiotique appliquée Volume 6.00-7",
      "Applied Semiotics / Sémiotique appliquée Volume 6.2-7",
      "Applied Semiotics / Sémiotique appliquée Volume 6-7",
      "Applied Semiotics / Sémiotique appliquée Volume 6.15-7",
      "Applied Semiotics / Sémiotique appliquée Volume 6.20-7",
      "Applied Semiotics / Sémiotique appliquée Volume 6.25-7",
      "Applied Semiotics / Sémiotique appliquée Volume 8",
      "Applied Semiotics / Sémiotique appliquée Volume 9",
      "Applied Semiotics / Sémiotique appliquée Volume 10",
      "Applied Semiotics / Sémiotique appliquée Volume 11.0-12",
      "Applied Semiotics / Sémiotique appliquée Volume 11.0-12.5",
      "Applied Semiotics / Sémiotique appliquée Volume 11.5-12",
      "Applied Semiotics / Sémiotique appliquée Volume 11.6-12",
      "Applied Semiotics / Sémiotique appliquée Volume 11-12",
      "Applied Semiotics / Sémiotique appliquée Volume 11-12.5",
      "Applied Semiotics / Sémiotique appliquée Volume 11.50-12",
      "Applied Semiotics / Sémiotique appliquée Volume 13",
      "Applied Semiotics / Sémiotique appliquée Volume 14",
      "a boy and his bog",
      "A Boy and his Dog",
      "Gar\u00e7on Magazine",
      "IBM Tech Journak",
      "I.B.M. Tech. Journal",
      "IBM Tech Journam",
      "Journal of I B M 2004",
      "The Volume 1 of the World 11 History 532",
      "A Volume 2 of the World 1 History 532",
      "A Volume 2 of the World 2 History 532",
      "The Volume 2 of the World 11 History 532",
      "The Volume 2 of the World 11 History 4210",
      "A Volume 2 of the World 21 History 532",
      "A Volume 2 of the World 113 History 532",
      "a Volume 10 of the World 1 History 532",
      "the Volume 11 of the World 1 History 532"
     };
    List tl = ListUtil.fromArray(titles);
    Collections.reverse(tl);
    assertFalse(CollectionUtil.isIsomorphic(titles, tl));
    assertIsomorphic(titles, sort(tl));
    Collections.shuffle(tl);
    assertIsomorphic(titles, sort(tl));
  }

  public void testOrderWithNull() {
    // titles in sorted order
    String[] titles = {
      null,
      "The Aardvark of the Baskervilles",
      "An Apple and its Eve",
      "a boy and his bog",
      "A Boy and his Dog",
      "IBM Tech Journak",
      "I.B.M. Tech. Journal",
      "IBM Tech Journam",
      "Journal of I B M 2004"
    };
    List tl = ListUtil.fromArray(titles);
    Collections.reverse(tl);
    assertFalse(CollectionUtil.isIsomorphic(titles, tl));
    assertIsomorphic(titles, sort(tl));
    Collections.shuffle(tl);
    assertIsomorphic(titles, sort(tl));
  }

  public void testIllType() {
    try {
      sort(ListUtil.list("foo", new Integer(1)));
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testCache() {
    CountingCOC ccoc = new CountingCOC();

    List lst = ListUtil.list("1",
			     "2",
			     "the 3",
			     "a 4",
			     "an 5",
			     "6 I B M Journal",
			     " 7 Gar\u00e7on Magazine");

    ccoc.xlateCnt = 0;
    Collections.sort(lst, ccoc);
    // should have xlated each item once
    assertEquals(lst.size(), ccoc.xlateCnt);
    lst.add("a partridge");
    Collections.shuffle(lst);
    Collections.sort(lst, ccoc);
    assertEquals(lst.size(), ccoc.xlateCnt);
  }

  List sort(List l) {
    Collections.sort(l, new CatalogueOrderComparator());
    return l;
  }

  class CountingCOC extends CatalogueOrderComparator {
    int xlateCnt = 0;
    String xlate(String s) {
      xlateCnt++;
      return super.xlate(s);
    }
  }

}
