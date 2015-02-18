/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

import static org.lockss.util.CachingComparator.NormalisationOption.*;

import java.util.*;

import org.lockss.exporter.kbart.KbartFieldOrderComparator;
import org.lockss.test.*;
import org.lockss.util.CachingComparator.NormalisationOption;

/**
 * Test class for org.lockss.util.CachingComparator
 */
public class TestCachingComparator extends LockssTestCase {
  CachingComparator<String> scc;
  CachingComparator<CustomObject> cocc;

  public void setUp() {
    // Standard CC, with options false, true, true
    scc = new CachingComparator<String>();
    // CC on a CustomObject which defines its own comparison string
    // Options case sensitive, don't translate accents or remove determiners
    cocc = new CachingComparator<CustomObject>(
	new HashMap<NormalisationOption, Boolean>() {{
	  put(CASE_SENSITIVE, true);
	  put(TRANSLATE_ACCENTS, false);
	  put(REMOVE_INITIAL_DETERMINERS, false);
	}}) {
      protected String getComparisonString(CustomObject obj) { return obj.name; }
    };
       
  }

  public void testDeleteInitial() {
    assertSame("foo", scc.deleteInitial("foo", "bar"));
    assertEquals("foo", scc.deleteInitial("bar foo", "bar"));
    assertEquals("foo", scc.deleteInitial("bar  foo", "bar"));
    assertEquals("barfoo", scc.deleteInitial("barfoo", "bar"));
  }

  public void testDeleteAll() {
    assertSame("foo", scc.deleteAll("foo", "bar"));
    assertEquals("foo", scc.deleteAll("f.o,o..", ".,"));
    assertEquals("foo", scc.deleteAll("fo--o", "--!"));
  }

  public void testDeleteSpaceBetweenInitials() {
    assertEquals("Journal of IBM 2004", scc.deleteSpaceBetweenInitials("Journal of I B M 2004"));
    assertEquals("UPS manuel", scc.deleteSpaceBetweenInitials("U P S manuel"));
    assertEquals("Journal of HP", scc.deleteSpaceBetweenInitials("Journal of H P"));
    assertEquals("Who am I magazine", scc.deleteSpaceBetweenInitials("Who am I magazine"));
    assertEquals("Wild Space", scc.deleteSpaceBetweenInitials("Wild         Space"));
  }

  public void testReplaceAccentedChar(){
    assertEquals("toe", scc.replaceAccentedChar("to\u00eb"));
    assertEquals("Apple", scc.replaceAccentedChar("\u00c1pple"));
    assertEquals("low", scc.replaceAccentedChar("l\u00f4w"));
    assertEquals("coupe", scc.replaceAccentedChar("coup\u00e9"));
    assertEquals("naive", scc.replaceAccentedChar("na\u00efve"));
    assertEquals("garcon", scc.replaceAccentedChar("gar\u00e7on"));
  }

  public void testXlate() {
    assertEquals("Tao of Pooh", scc.xlate("The Tao of Pooh"));
    assertEquals("Tao of Pooh", scc.xlate("A  Tao of Pooh"));
    assertEquals("Tao of Pooh", scc.xlate("An Tao of Pooh"));
    assertEquals("IBM Tech Journal", scc.xlate("I.B.M. Tech Journal"));
    assertEquals("Journal of IBM", scc.xlate("Journal of I. B. M."));

    assertEquals("The Tao of Pooh", cocc.xlate("The Tao of Pooh"));
    assertEquals("A Tao of Pooh", cocc.xlate("A  Tao of Pooh"));
    assertEquals("An Tao of Pooh", cocc.xlate("An Tao of Pooh"));
    // I.B.M. Têch Journal
    assertEquals("IBM T\u00EAch Journal", cocc.xlate("I.B.M. T\u00EAch Journal"));
    // Journal øf I. B. M.
    assertEquals("Journal \u00F8f IBM", cocc.xlate("Journal \u00F8f I. B. M."));
  }
  
  public void testPadNumbers() {
     assertEquals("Volume of the World History",
		   scc.padNumbers("Volume of the World History", 6));  
     assertEquals("1 initial number",
		   scc.padNumbers("1 initial number", 0));
     assertEquals("1 initial number",
		   scc.padNumbers("1 initial number", 1));
     assertEquals("000001 initial number",
		   scc.padNumbers("1 initial number", 6));
     assertEquals("234 initial number",
		   scc.padNumbers("234 initial number", 0));
     assertEquals("234 initial number",
		   scc.padNumbers("234 initial number", 1));
     assertEquals("000234 initial number",
		   scc.padNumbers("234 initial number", 6));
     assertEquals("final number 4",
		   scc.padNumbers("final number 4", 0));
     assertEquals("final number 4",
		   scc.padNumbers("final number 4", 1));
     assertEquals("final number 000004",
		   scc.padNumbers("final number 4", 6));
     assertEquals("final number 234",
		   scc.padNumbers("final number 234", 0));
     assertEquals("final number 234",
		   scc.padNumbers("final number 234", 1));
     assertEquals("final number 000234",
		   scc.padNumbers("final number 234", 6));
     assertEquals("embedded 4 number",
		   scc.padNumbers("embedded 4 number", 0));
     assertEquals("embedded 4 number",
		   scc.padNumbers("embedded 4 number", 1));
     assertEquals("embedded 000004 number",
		   scc.padNumbers("embedded 4 number", 6));
     assertEquals("embedded 234 number",
		   scc.padNumbers("embedded 234 number", 0));
     assertEquals("embedded 234 number",
		   scc.padNumbers("embedded 234 number", 1));
     assertEquals("embedded 000234 number",
		   scc.padNumbers("embedded 234 number", 6));
     assertEquals("000123 leading and embedded 000234 number",
		   scc.padNumbers("123 leading and embedded 234 number", 6));
     assertEquals("000123 leading and trailing 000234",
		   scc.padNumbers("123 leading and trailing 234", 6));
     assertEquals("embedded 00234 and trailing 00321",
		   scc.padNumbers("embedded 234 and trailing 321", 5));
     assertEquals("12345 longer 4321 or equal to pad 666777",
		   scc.padNumbers("12345 longer 4321 or equal to pad 666777",
				  4));
  }

  public void testOrder() {
    // titles in sorted order
    String[] titles = {
      "The Aardvark of the Baskervilles",
      "An Apple and its Eve",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 1",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 2",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 3",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 4",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 5",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 6.00-7",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 6.2-7",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 6-7",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 6.15-7",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 6.20-7",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 6.25-7",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 8",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 9",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 10",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 11.0-12",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 11.0-12.5",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 11.5-12",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 11.6-12",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 11-12",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 11-12.5",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 11.50-12",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 13",
      "Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 14",
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
    
    ccListSortingTest(scc, titles);
    
    // Titles in sorted order for CC options true, false, false.
    // We don't make too much effort here to stress the number formatting, as this
    // is common to the CC regardless of initialisation options. We do test the 
    // ordering of different cases and of accented versus unaccented characters,
    // and also ensure that initial determiners are not removed.
    CustomObject[] cotitles = {
	new CustomObject("An Apple and its Eve"),
	new CustomObject("An Apple and its eve"),
	new CustomObject("An Applied Semiotics / Une S\u00E9miotique appliqu\u00E9e Volume 3"),
	new CustomObject("An apple and its Eve"),
	new CustomObject("Applied Semiotics / Semiotique appliquee Volume 1"),
	new CustomObject("Applied Semiotics / Semiotique appliqu\u00E9e Volume 6.00-7.00008"),
	new CustomObject("Applied Semiotics / S\u00E9miotique appliquee Volume 6.00-7.00002"),
	new CustomObject("Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 2"),
	new CustomObject("Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 4"),
	new CustomObject("Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 5"),
	new CustomObject("Applied Semiotics / S\u00E9miotique appliqu\u00E9e Volume 6.00-7"),
	new CustomObject("Garcon Magazine"),
	new CustomObject("Gar\u00e7on Magazine"),
	new CustomObject("The Aardvark of the Baskervilles"),
	new CustomObject("a Boy and his dog"),
	new CustomObject("a boy and his bog"),
	new CustomObject("an Apple and its Eve"),
	new CustomObject("an apple and its Eve"),
    };
    ccListSortingTest(cocc, cotitles);
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
    ccListSortingTest(scc, titles);
    
    // test custom objects with null
    CustomObject[] cotitles = new CustomObject[] {
	null,
	new CustomObject("A Boy and his Dog"),
	new CustomObject("An Apple and its Eve"),
	new CustomObject("IBM Tech Journak"),
	new CustomObject("I.B.M. Tech. Journal"),
	new CustomObject("IBM Tech Journam"),
	new CustomObject("Journal of I B M 2004"),
	new CustomObject("The Aardvark of the Baskervilles"),
	new CustomObject("a boy and his bog"),
    };
    ccListSortingTest(cocc, cotitles);
  }
  
  public void testIllType() {
    // Create a homogeneous list
    List homogList = ListUtil.list("foo", "noo");
    // Create a heterogeneous list
    List heteroList = ListUtil.list(new CustomObject("foo"), "noo");
    List heteroList2 = ListUtil.list("foo", new Integer(1));
    
    // Attempt to sort a string list using an unparameterized comparator
    try {
      sort(homogList, new CachingComparator());
    } catch (ClassCastException e) {
      fail("Should not throw ClassCastException doing unparameterized sort on String list");
    }
    // Use a homogeneous string list with a parameterized Comparator
    try {
      sort(homogList, new CachingComparator<String>());
    } catch (ClassCastException e) {
      fail("Should not throw ClassCastException doing sort with correct parameterization (String).");
    }

    // Unparameterized sort of hetero list
    try {
      sort(heteroList, new CachingComparator());
      fail("Should throw ClassCastException doing unparameterized sort of heterogeneous list.");
    } catch (ClassCastException e) {
      // Expected exception
    }
    // Appropriately-parameterized sort but with a mixed-type list
    try {
      sort(heteroList, new CachingComparator<Object>());
      fail("Should throw ClassCastException doing sort of heterogeneous list with parameterization (Object).");
    } catch (ClassCastException e) {
      // Expected exception
    }
  }

  // The comparator will have to be consistent with equals if it used to order Sets; this one is not
  /*public void testConsistencyWithEquals() {
    CustomObject co1 = new CustomObject("Name");
    CustomObject co2 = new CustomObject("Name");
    String cos1 = co1.toString();
    String cos2 = co2.toString();
    String cos = "CustomObject Name";
    assertEquals(co1.equals(co2),   cocc.compare(co1, co2)==0); 
    assertEquals(cos1.equals(cos2), scc.compare(cos1, cos2)==0);
    assertEquals(cos1.equals(cos),  scc.compare(cos1, cos)==0);
  }*/
  
  public void testCache() {
    CountingCC<String> ccoc = new CountingCC<String>();
    List<String> lst = ListUtil.list("1",
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
    
    // test cache with different CC options
    // \u00FC is umlauted lower case 'u' ü
    CountingCC<CustomObject> coccc = new CountingCC<CustomObject>();
    List<CustomObject> colst = ListUtil.list(
	new CustomObject("The Pigeon by Patrick S\u00FCskind"),
	new CustomObject("Pigeon by Patrick S\u00FCskind"),
	new CustomObject("The Pigeon by Patrick Suskind"),
	new CustomObject("A Pigeon by Patrick S\u00FCskind"),
	new CustomObject("pigeon by Patrick Suskind"),
	new CustomObject("The pigeon by Patrick Suskind"),
	new CustomObject("The Pigeon by Patrick Sueskind")
    );
    ccoc.xlateCnt = 0;
    Collections.sort(colst, coccc);
    // should have xlated each item once
    assertEquals(colst.size(), coccc.xlateCnt);
    // add a pigeon
    colst.add(new CustomObject("a pigeon"));
    Collections.shuffle(colst);
    Collections.sort(colst, coccc);
    assertEquals(colst.size(), coccc.xlateCnt);
    // do it again
    colst.add(new CustomObject("a pigeon"));
    Collections.shuffle(colst);
    Collections.sort(colst, coccc);
    assertEquals(colst.size()-1, coccc.xlateCnt);
  }

  void ccListSortingTest(CachingComparator cc, Object[] titles) {
    List<Object> tl = ListUtil.fromArray(titles);
    Collections.reverse(tl);
    assertFalse(CollectionUtil.isIsomorphic(titles, tl));
    assertIsomorphic(titles, sort(tl, cc));
    Collections.shuffle(tl);
    assertIsomorphic(titles, sort(tl, cc));
  }

  // Sort a parameterized list using a parameterized CachingComparator
  <T> List<T> sort(List<T> l, CachingComparator<T> acc) {
    Collections.sort(l, acc);
    return l;
  }

  class CountingCC<T> extends CachingComparator<T> {
    int xlateCnt = 0;
    protected String xlate(String s) {
      xlateCnt++;
      return super.xlate(s);
    }
  }

  class CustomObject {
    public String name;
    public CustomObject(String name) { this.name = name; }
    public String toString() { return "CustomObject "+name; }
  }

  // Default getSingleton should throw exception
  public final void testGetSingleton() {
    try {
      CachingComparator.getSingleton();
      fail("Should throw OperationNotSupportedException .");
    } catch (Exception e) {
      // Expected exception
    }
  }

  public void testGenerateDefaultNormalisationOptions() {
    Map<NormalisationOption, Boolean> normOpts = 
      CachingComparator.generateDefaultNormalisationOptions();
    assertEquals(NormalisationOption.values().length, normOpts.size());
    assertContainsAll(normOpts.keySet(), NormalisationOption.values());
  }
  
}
