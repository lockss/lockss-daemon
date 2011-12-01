/*
 * $Id: TestBibliographicUtil.java,v 1.1 2011-12-01 17:39:32 easyonthemayo Exp $
 */

/*

Copyright (c) 2010-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.exporter.biblio;

import org.lockss.config.*;
import org.lockss.config.Tdb.TdbException;
import org.lockss.test.LockssTestCase;
import org.lockss.util.NumberUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Testing is performed using TdbAu objects, though perhaps the more generic
 * BibliographicItemImpl should be used.
 */
public class TestBibliographicUtil extends LockssTestCase {

  BibliographicItem au1;
  ConfigManager mgr;
  Configuration config;

  // String-based formats
  private static final String iss1 = "iss7";
  private static final String iss2 = "iss11";
  private static final String issRng = iss1+"-"+iss2;
  private static final String issLst = iss1+", 8, 89, bgt5, test, 088, "+iss2;
  // A string with an unknown structure 
  private static final String issStr = "A string representing an issue";
      
  // Number-based formats
  private static final String issNum1 = "007";
  private static final String issNum2 = "011";
  private static final String issNumRng = issNum1+"-"+issNum2;
  
  // AUs for the equivalence tests
  private BibliographicItem afrTod, afrTodEquiv, afrTodDiffVol, afrTodDiffYear,
      afrTodDiffName, afrTodDiffUrl, afrTodNullYear;
  private List<BibliographicItem> afrTodAus = Arrays.asList(afrTod, afrTodEquiv,
      afrTodDiffVol, afrTodDiffYear, afrTodDiffName, afrTodDiffUrl,
      afrTodNullYear);
  
  /**
   * Create a test Tdb structure.
   */
  protected void setUp() throws Exception {
    super.setUp();
    // Set up some AUs for the equivalence tests
    try {
      // 2 equivalent AUs
      afrTod         = TdbTestUtil.createBasicAu("Africa Today",     "1999", "46");
      afrTodEquiv    = TdbTestUtil.createBasicAu("Africa Today",     "1999", "46");
      // 2 AUs differing from the previous 2 by one field each
      afrTodDiffVol  = TdbTestUtil.createBasicAu("Africa Today",     "1999", "47");
      afrTodDiffYear = TdbTestUtil.createBasicAu("Africa Today",     "2000", "46");
      afrTodDiffName = TdbTestUtil.createBasicAu("Africa Yesterday", "1999", "46");
      // An AU differing from the first pair on a non-primary field
      TdbAu au = TdbTestUtil.createBasicAu("Africa Today",     "1999", "46");
      TdbTestUtil.setParam(au, "base_url", "http://whocares.com");
      afrTodDiffUrl  = au;
      // An AU with a null year
      afrTodNullYear = TdbTestUtil.createBasicAu("Africa Today", null, "46");
    } catch (TdbException e) {
      fail("Error setting up test TdbAus.");
    }
  }

  /**
   * Nullify everything.
   */
  protected void tearDown() throws Exception {
    super.tearDown();
    mgr = null;
    config = null;
  }

  /**
   * Test sortByVolumeYear() and sortByYearVolume().
   * Note these get well exercised in TestBibliographicOrderScorer.
   */
  public final void testSortItemsAus() throws TdbException {
    String name     = "Monkeys Monthly";
    // Create a set of TdbAus with both multiple volumes to a year,
    // and multiple years to a volume.
    TdbAu au1     = TdbTestUtil.createBasicAu(name, "1980", "vol1");
    TdbAu au2     = TdbTestUtil.createBasicAu(name, "1980", "vol2");
    TdbAu au3     = TdbTestUtil.createBasicAu(name, "1981", "vol3");
    TdbAu au4     = TdbTestUtil.createBasicAu(name, "1982", "vol3");
    TdbAu au5     = TdbTestUtil.createBasicAu(name, "1983", "vol4");
    List<TdbAu> list = Arrays.asList(au1, au2, au3, au4, au5);
    final List<TdbAu> expectedVolYearOrder = Arrays.asList(au1, au2, au3, au4, au5);
    final List<TdbAu> expectedYearVolOrder = Arrays.asList(au1, au2, au3, au4, au5);

    // Shuffle then sort vol-year
    Collections.shuffle(list);
    BibliographicUtil.sortByVolumeYear(list);
    assertIsomorphic(expectedVolYearOrder, list);
    // Shuffle then sort year-vol
    Collections.shuffle(list);
    BibliographicUtil.sortByYearVolume(list);
    assertIsomorphic(expectedYearVolOrder, list);

    // TODO try incorporating name variations where other fields are equivalent
  }

  public void testAreEquivalent() {
    assertTrue(BibliographicUtil.areEquivalent(afrTod, afrTod));
    assertTrue(BibliographicUtil.areEquivalent(afrTod, afrTodEquiv));
    assertTrue(BibliographicUtil.areEquivalent(afrTod, afrTodDiffUrl));
    assertTrue(BibliographicUtil.areEquivalent(afrTodDiffUrl, afrTod));
    // Should return false if any field or arg is null
    assertFalse(BibliographicUtil.areEquivalent(null, null));
    assertFalse(BibliographicUtil.areEquivalent(afrTod, afrTodNullYear));
    assertFalse(BibliographicUtil.areEquivalent(afrTodNullYear, afrTod));

    assertFalse(BibliographicUtil.areEquivalent(afrTod, afrTodDiffVol));
    assertFalse(BibliographicUtil.areEquivalent(afrTod, afrTodDiffYear));

    assertFalse(BibliographicUtil.areEquivalent(afrTod, afrTodDiffName));
  }

  /**
   * Either the ISSNs or the names are the same. In this case all the ISSNs
   * are the same.
   */
  public void testHaveSameIdentity() {
    assertTrue(BibliographicUtil.haveSameIdentity(afrTod, afrTod));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTod, afrTodEquiv));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTod, afrTodDiffUrl));
    // Should throw exception if any arg is null
    try {
      BibliographicUtil.haveSameIdentity(null, null);
      fail("Should throw exception with null BibliographicItems.");
    } catch (NullPointerException e) {
      // expected
    }
    // The null field is irrelevant to the comparison
    assertTrue(BibliographicUtil.haveSameIdentity(afrTod, afrTodNullYear));
    // These have the same ISSN, despite the other difference
    assertTrue(BibliographicUtil.haveSameIdentity(afrTod, afrTodDiffName));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTod, afrTodDiffVol));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTod, afrTodDiffYear));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTodDiffYear, afrTodDiffVol));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTodDiffName, afrTodDiffYear));
    // Null field discounts year; issns still match
    assertTrue(BibliographicUtil.haveSameIdentity(afrTodNullYear, afrTodDiffYear));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTodNullYear, afrTodDiffVol));
  }

  /**
   * Check volume and year. Each available pair of non-null values must match.
   */
  public void testHaveSameIndex() {
    assertTrue(BibliographicUtil.haveSameIndex(afrTod, afrTod));
    assertTrue(BibliographicUtil.haveSameIndex(afrTod, afrTodEquiv));
    assertTrue(BibliographicUtil.haveSameIndex(afrTod, afrTodDiffUrl));
    // Should throw exception if any arg is null
    try {
      BibliographicUtil.haveSameIndex(null, null);
      fail("Should throw exception with null BibliographicItems.");
    } catch (NullPointerException e) {
      // expected
    }
    // The null field should be omitted from the comparison
    assertTrue(BibliographicUtil.haveSameIndex(afrTod, afrTodNullYear));
    // These only differ on id fields
    assertTrue(BibliographicUtil.haveSameIndex(afrTod, afrTodDiffName));
    // These differ on one of the two available index fields
    assertFalse(BibliographicUtil.haveSameIndex(afrTod, afrTodDiffVol));
    assertFalse(BibliographicUtil.haveSameIndex(afrTod, afrTodDiffYear));
    // These differ on available fields
    assertFalse(BibliographicUtil.haveSameIndex(afrTodDiffYear, afrTodDiffVol));
    assertFalse(BibliographicUtil.haveSameIndex(afrTodDiffName, afrTodDiffYear));
    // Null fields discount year; volumes match
    assertTrue(BibliographicUtil.haveSameIndex(afrTodNullYear, afrTodDiffYear));
    // Null fields discount year; volumes differ
    assertFalse(BibliographicUtil.haveSameIndex(afrTodNullYear, afrTodDiffVol));
  }

  /**
   * The names or ISSNs match, and any of the volume and year fields that are
   * available (non-null) must match.
   */
  public void testAreApparentlyEquivalent() {
    assertTrue(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTod));
    assertTrue(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTodEquiv));
    assertTrue(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTodDiffUrl));
    // Should throw exception if any arg is null
    try {
      BibliographicUtil.areApparentlyEquivalent(null, null);
      fail("Should throw exception with null BibliographicItems.");
    } catch (NullPointerException e) {
      // expected
    }
    // The null field should be omitted from the comparison
    assertTrue(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTodNullYear));
    // These have the same ISSN, despite the name difference
    assertTrue(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTodDiffName));
    // These differ on one of the two available index fields
    assertFalse(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTodDiffVol));
    assertFalse(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTodDiffYear));
    // These differ on available fields
    assertFalse(BibliographicUtil.areApparentlyEquivalent(afrTodDiffYear, afrTodDiffVol));
    assertFalse(BibliographicUtil.areApparentlyEquivalent(afrTodDiffName, afrTodDiffYear));
    // Null field discounts year; issn and volume match
    assertTrue(BibliographicUtil.areApparentlyEquivalent(afrTodNullYear, afrTodDiffYear));
    // Null field discounts year; vols differ
    assertFalse(BibliographicUtil.areApparentlyEquivalent(afrTodNullYear, afrTodDiffVol));
  }


  public void testCompareStringYears() {
    // Check that the result is right; check for NFE?
    String yr1 = "2000";
    String yr2 = "1999";
    String yr3 = "MMI"; // 2001
    String yr4 = "MCM"; // 1900
    assertTrue(yr1+" is not greater than "+yr2, BibliographicUtil.compareStringYears(yr1, yr2) > 0);
    assertTrue(yr1+" is not less than "+yr2, BibliographicUtil.compareStringYears(yr2, yr1) < 0);
    assertTrue(yr1+" is not equal to "+yr1, BibliographicUtil.compareStringYears(yr1, yr1) == 0);
    assertTrue(yr2+" is not equal to "+yr2, BibliographicUtil.compareStringYears(yr2, yr2) == 0);

    assertTrue(yr3+" is not greater than "+yr2, BibliographicUtil.compareStringYears(yr3, yr2) > 0);
    assertTrue(yr4+" is not less than "+yr2, BibliographicUtil.compareStringYears(yr4, yr1) < 0);
    assertTrue(yr1+" is not equal to "+yr1, BibliographicUtil.compareStringYears(yr1, yr1) == 0);
    assertTrue(yr2+" is not equal to "+yr2, BibliographicUtil.compareStringYears(yr2, yr2) == 0);
  }

  /**
   * Should work with a year or a range of years; returns null if not parsable.
   */
  public void testGetFirstYear() {
    TdbTitle title;
    try {
      title = TdbTestUtil.makeYearTestTitle("2000", "2002-MMV", "2006", "MMVIII-2011");
    } catch (TdbException e) {
      fail("Exception encountered making year test title: "+e);
      return;
    }

    // Try and find AU info type for each AU
    for (TdbAu au: title.getTdbAus()) {
      // Various valid ranges
      assertEquals(NumberUtil.toArabicNumber(au.getStartYear()),
                   ""+ BibliographicUtil.getFirstYearAsInt(au));
    }
  }

  /**
   * Should work with a year or a range of years; returns null if not parsable.
   */
  public void testGetLastYear() {
    TdbTitle title;
    try {
      title = TdbTestUtil.makeYearTestTitle("2000", "2002-MMV", "2006", "MMVIII-2011");
    } catch (TdbException e) {
      fail("Exception encountered making year test title: "+e);
      return;
    }

    // Try and find AU info type for each AU
    for (TdbAu au: title.getTdbAus()) {
      // Various valid ranges
      assertEquals(NumberUtil.toArabicNumber(au.getEndYear()),
          ""+ BibliographicUtil.getLastYearAsInt(au));
    }
  }

  /**
   *
   */
  public void testGetLatestYear() {
    try {
      String name = "A Title";
      // Single AU
      assertEquals(1, BibliographicUtil.getLatestYear(
          Arrays.asList(
              TdbTestUtil.createBasicAu(name, "1")
          )
      ));
      // Several AUs, highest year at end
      assertEquals(3, BibliographicUtil.getLatestYear(
          Arrays.asList(
              TdbTestUtil.createBasicAu(name, "1"),
              TdbTestUtil.createBasicAu(name, "2"),
              TdbTestUtil.createBasicAu(name, "3")
          )
      ));
      // Several AUs, duplicated highest
      assertEquals(1, BibliographicUtil.getLatestYear(
          Arrays.asList(
              TdbTestUtil.createBasicAu(name, "1"),
              TdbTestUtil.createBasicAu(name, "1"),
              TdbTestUtil.createBasicAu(name, "1")
          )
      ));
      assertEquals(10, BibliographicUtil.getLatestYear(
          Arrays.asList(
              TdbTestUtil.createBasicAu(name, "1"),
              TdbTestUtil.createBasicAu(name, "10"),
              TdbTestUtil.createBasicAu(name, "9"),
              TdbTestUtil.createBasicAu(name, "8"),
              TdbTestUtil.createBasicAu(name, "7")
          )
      ));
      // Several AUs, highest year in middle
      assertEquals(3000, BibliographicUtil.getLatestYear(
          Arrays.asList(
              TdbTestUtil.createBasicAu(name, "1999"),
              TdbTestUtil.createBasicAu(name, "2999"),
              TdbTestUtil.createBasicAu(name, "3000"),
              TdbTestUtil.createBasicAu(name, "1999"),
              TdbTestUtil.createBasicAu(name, "2999")
          )
      ));
    } catch (TdbException e) {
      fail("Error setting up test TdbAus.");
    }
  }


}
