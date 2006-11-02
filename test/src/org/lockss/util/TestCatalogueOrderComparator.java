/*
 * $Id: TestCatalogueOrderComparator.java,v 1.8 2006-11-02 04:19:38 tlipkis Exp $
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
  
  public void testPadNumbers() {
     assertEquals("Volume of the World History",
		   coc.padNumbers("Volume of the World History", 6));  
     assertEquals("1 initial number",
		   coc.padNumbers("1 initial number", 0));
     assertEquals("1 initial number",
		   coc.padNumbers("1 initial number", 1));
     assertEquals("000001 initial number",
		   coc.padNumbers("1 initial number", 6));
     assertEquals("234 initial number",
		   coc.padNumbers("234 initial number", 0));
     assertEquals("234 initial number",
		   coc.padNumbers("234 initial number", 1));
     assertEquals("000234 initial number",
		   coc.padNumbers("234 initial number", 6));
     assertEquals("final number 4",
		   coc.padNumbers("final number 4", 0));
     assertEquals("final number 4",
		   coc.padNumbers("final number 4", 1));
     assertEquals("final number 000004",
		   coc.padNumbers("final number 4", 6));
     assertEquals("final number 234",
		   coc.padNumbers("final number 234", 0));
     assertEquals("final number 234",
		   coc.padNumbers("final number 234", 1));
     assertEquals("final number 000234",
		   coc.padNumbers("final number 234", 6));
     assertEquals("embedded 4 number",
		   coc.padNumbers("embedded 4 number", 0));
     assertEquals("embedded 4 number",
		   coc.padNumbers("embedded 4 number", 1));
     assertEquals("embedded 000004 number",
		   coc.padNumbers("embedded 4 number", 6));
     assertEquals("embedded 234 number",
		   coc.padNumbers("embedded 234 number", 0));
     assertEquals("embedded 234 number",
		   coc.padNumbers("embedded 234 number", 1));
     assertEquals("embedded 000234 number",
		   coc.padNumbers("embedded 234 number", 6));
     assertEquals("000123 leading and embedded 000234 number",
		   coc.padNumbers("123 leading and embedded 234 number", 6));
     assertEquals("000123 leading and trailing 000234",
		   coc.padNumbers("123 leading and trailing 234", 6));
     assertEquals("embedded 00234 and trailing 00321",
		   coc.padNumbers("embedded 234 and trailing 321", 5));
     assertEquals("12345 longer 4321 or equal to pad 666777",
		   coc.padNumbers("12345 longer 4321 or equal to pad 666777",
				  4));
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
