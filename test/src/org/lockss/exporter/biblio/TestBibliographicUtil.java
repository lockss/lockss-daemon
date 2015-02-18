/*
 * $Id$
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

import static org.lockss.exporter.biblio.BibliographicUtil.*;

import org.lockss.config.*;
import org.lockss.config.Tdb.TdbException;
import org.lockss.test.LockssTestCase;
import org.lockss.util.NumberUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Many tests are performed using TdbAu objects, though perhaps the more generic
 * BibliographicItemImpl should be used.
 * // XXX NOTE: we don't incorporate test for ISBNs or ISSN-Ls or eIS[BS]Ns yet
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
  private BibliographicItemAdapter afrTod, afrTodEquiv, afrTodDiffVol, afrTodDiffYear,
      afrTodDiffName, afrTodDiffJournalTitle, afrTodDiffIssn, afrTodDiffPublisher, afrTodNullYear;
  private List<BibliographicItemAdapter> afrTodAus;


  // OUP's "The Library..." has consecutive volumes with different identifier formats
  private BibliographicItem theLib1 = new BibliographicItemImpl()
      .setPrintIssn(TdbTestUtil.DEFAULT_ISSN_1)
      .setName("The Library")
      .setYear("1981")
      .setVolume("s6-3");
  private BibliographicItem theLib2 = new BibliographicItemImpl()
      .setPrintIssn(TdbTestUtil.DEFAULT_ISSN_1)
      .setName("The Library")
      .setYear("1981")
      .setVolume("s6-III");


  /**
   * Create a test Tdb structure.
   */
  protected void setUp() throws Exception {
    super.setUp();
    // Set up some AUs for the equivalence tests
    // Basic cloneable AU
    afrTod = new BibliographicItemImpl()
        .setPublicationTitle("Africa Today")
        .setName("Africa Today")
        .setYear("1999").setVolume("46")
            // Note we have to set these default ISSN/EISSN for compatibility with Tdb created below
        .setPrintIssn(TdbTestUtil.DEFAULT_ISSN_1)
        .setEissn(TdbTestUtil.DEFAULT_EISSN_1);
    // equivalent AU
    afrTodEquiv = new BibliographicItemImpl(afrTod);
    // 2 AUs differing from the previous 2 by one field each
    afrTodDiffVol  = new BibliographicItemImpl(afrTod).setVolume("47");
    afrTodDiffYear = new BibliographicItemImpl(afrTod).setYear("2000");
    afrTodDiffName = new BibliographicItemImpl(afrTod).setName("Africa Yesterday");
    afrTodDiffJournalTitle = new BibliographicItemImpl(afrTod).setPublicationTitle("Africa Yesterday");
    afrTodDiffIssn = new BibliographicItemImpl(afrTod).setPrintIssn(TdbTestUtil.DEFAULT_ISSN_4);
    afrTodDiffPublisher = new BibliographicItemImpl(afrTod).setPublisherName(TdbTestUtil.DEFAULT_PUBLISHER);

    // ----------------------------------------------------------------------
    // The following is not equivalent to BibItems as it is a TdbAu,
    // having a null journal title by default, dependent on title.
    // Instead all BibItems are now created as fully comparable BibItemImpl;
    // it is up to implementors to provide appropriate values, and test them
    // separately against the BibliographicItem interface.
    // ----------------------------------------------------------------------
    // An AU differing from the first pair on a non-primary field
    /*TdbAu auDiffUrl = TdbTestUtil.createBasicAu("Africa Today", "1999", "46");
      TdbTestUtil.setParam(auDiffUrl, "base_url", "http://whocares.com");
      afrTodDiffUrl  = auDiffUrl;*/
    // ----------------------------------------------------------------------

    // An AU with a null year
    afrTodNullYear = new BibliographicItemImpl(afrTod).setYear(null);

    afrTodAus = Arrays.asList(afrTod, afrTodEquiv,
        afrTodDiffVol, afrTodDiffYear, afrTodDiffName, afrTodDiffJournalTitle,
        afrTodDiffIssn, afrTodDiffPublisher, afrTodNullYear);
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
    assertTrue(BibliographicUtil.areEquivalent(afrTod, afrTodDiffPublisher));
    assertTrue(BibliographicUtil.areEquivalent(afrTodDiffPublisher, afrTod));
    assertTrue(BibliographicUtil.areEquivalent(theLib1, theLib2));

    // Should return false if any arg is null or one of the fields is null
    assertFalse(BibliographicUtil.areEquivalent(null, null));
    assertFalse(BibliographicUtil.areEquivalent(afrTod, afrTodNullYear));
    assertFalse(BibliographicUtil.areEquivalent(afrTodNullYear, afrTod));

    assertFalse(BibliographicUtil.areEquivalent(afrTod, afrTodDiffVol));
    assertFalse(BibliographicUtil.areEquivalent(afrTod, afrTodDiffYear));

    assertFalse(BibliographicUtil.areEquivalent(afrTod, afrTodDiffName));
    assertFalse(BibliographicUtil.areEquivalent(afrTod, afrTodDiffJournalTitle));
  }

  /**
   * BibliographicItems come from the same title if they have same identifying
   * fields. If either argument is null, an exception should be thrown.
   */
  public void testAreFromSameTitle() throws Exception {
    // All titles should be from the same title as themselves
    for (BibliographicItem au : afrTodAus) {
      assertTrue(BibliographicUtil.areFromSameTitle(au, au));
    }
    assertTrue(BibliographicUtil.areFromSameTitle(theLib1, theLib2));

    // All Africa Today example titles should be from same title
    // except afrTodDiffJournalTitle, afrTodDiffIssn, afrTodDiffPublisher
    List<BibliographicItemAdapter> sameTitleAfrTodAus = Arrays.asList(
        afrTod, afrTodEquiv, afrTodDiffVol, afrTodDiffYear, afrTodNullYear);
    // XXX NOTE: we don;t incorporate test for ISBNs or ISSN-Ls or eIS[BS]Ns yet

    for (BibliographicItem bi1 : sameTitleAfrTodAus) {
      // Should be same as item with diff name
      assertTrue(BibliographicUtil.areFromSameTitle(bi1, afrTodDiffName));
      // Should differ from item with diff journal title
      assertFalse(BibliographicUtil.areFromSameTitle(bi1, afrTodDiffJournalTitle));
      // Should differ from item with diff ISSN
      assertFalse(BibliographicUtil.areFromSameTitle(bi1, afrTodDiffIssn));
      // Should differ from item with diff publisher
      assertFalse(BibliographicUtil.areFromSameTitle(bi1, afrTodDiffPublisher));
      // Should match all other items
      for (BibliographicItem bi2 : sameTitleAfrTodAus) {
        assertTrue(BibliographicUtil.areFromSameTitle(bi1, bi2));
      }
    }

    // Should return false if any arg is null
    try {
      BibliographicUtil.areFromSameTitle(null, null);
      fail("Should throw exception with null BibliographicItems.");
    } catch (NullPointerException e) {
      // expected
    }
  }


  /**
   * Either the ISSNs (if available) or the journal names are the same. In this
   * case all the ISSNs are the same.
   */
  public void testHaveSameIdentity() {
    assertTrue(BibliographicUtil.haveSameIdentity(afrTod, afrTod));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTod, afrTodEquiv));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTod, afrTodDiffPublisher));
    // Should return false if any arg is null
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
    assertTrue(BibliographicUtil.haveSameIdentity(afrTod, afrTodDiffJournalTitle));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTod, afrTodDiffVol));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTod, afrTodDiffYear));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTodDiffYear, afrTodDiffVol));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTodDiffName, afrTodDiffYear));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTodDiffJournalTitle, afrTodDiffYear));
    // Null field discounts year; issns still match
    assertTrue(BibliographicUtil.haveSameIdentity(afrTodNullYear, afrTodDiffYear));
    assertTrue(BibliographicUtil.haveSameIdentity(afrTodNullYear, afrTodDiffVol));
  }

  /**
   * Check volume and year. Each available pair of values must match.
   */
  public void testHaveSameIndex() {
    assertTrue(BibliographicUtil.haveSameIndex(afrTod, afrTod));
    assertTrue(BibliographicUtil.haveSameIndex(afrTod, afrTodEquiv));
    assertTrue(BibliographicUtil.haveSameIndex(afrTod, afrTodDiffPublisher));
    // Should throw exception if any arg is null
    try {
      BibliographicUtil.haveSameIndex(null, null);
      fail("Should throw exception with null BibliographicItems.");
    } catch (NullPointerException e) {
      // expected
    }
    // The null field differs from the year which is present
    assertFalse(BibliographicUtil.haveSameIndex(afrTod, afrTodNullYear));
    // These only differ on id fields
    assertTrue(BibliographicUtil.haveSameIndex(afrTod, afrTodDiffName));
    assertTrue(BibliographicUtil.haveSameIndex(afrTod, afrTodDiffJournalTitle));
    // These differ on one of the two available index fields
    assertFalse(BibliographicUtil.haveSameIndex(afrTod, afrTodDiffVol));
    assertFalse(BibliographicUtil.haveSameIndex(afrTod, afrTodDiffYear));
    // These differ on available fields
    assertFalse(BibliographicUtil.haveSameIndex(afrTodDiffYear, afrTodDiffVol));
    assertFalse(BibliographicUtil.haveSameIndex(afrTodDiffName, afrTodDiffYear));
    assertFalse(BibliographicUtil.haveSameIndex(afrTodDiffJournalTitle, afrTodDiffYear));
    // Null field differs from year; volumes match
    assertFalse(BibliographicUtil.haveSameIndex(afrTodNullYear, afrTodDiffYear));
    // Years and volumes differ
    assertFalse(BibliographicUtil.haveSameIndex(afrTodNullYear, afrTodDiffVol));
  }

  /**
   * The names or ISSNs match, and volume and year fields must match.
   */
  public void testAreApparentlyEquivalent() {
    assertTrue(BibliographicUtil.areApparentlyEquivalent(theLib1, theLib2));
    assertTrue(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTod));
    assertTrue(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTodEquiv));
    assertTrue(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTodDiffPublisher));
    // Should throw exception if any arg is null
    try {
      BibliographicUtil.areApparentlyEquivalent(null, null);
      fail("Should throw exception with null BibliographicItems.");
    } catch (NullPointerException e) {
      // expected
    }
    // The null field should be omitted from the comparison
    assertFalse(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTodNullYear));
    // These have the same ISSN, despite the name/title difference
    assertTrue(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTodDiffName));
    assertTrue(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTodDiffJournalTitle));
    // These differ on one of the two available index fields
    assertFalse(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTodDiffVol));
    assertFalse(BibliographicUtil.areApparentlyEquivalent(afrTod, afrTodDiffYear));
    // These differ on available fields
    assertFalse(BibliographicUtil.areApparentlyEquivalent(afrTodDiffYear, afrTodDiffVol));
    assertFalse(BibliographicUtil.areApparentlyEquivalent(afrTodDiffName, afrTodDiffYear));
    assertFalse(BibliographicUtil.areApparentlyEquivalent(afrTodDiffJournalTitle, afrTodDiffYear));
    // Null field differs from year; issn and volume match
    assertFalse(BibliographicUtil.areApparentlyEquivalent(afrTodNullYear, afrTodDiffYear));
    // Years and volumes differ
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

  /**
   * Determines whether a String appears to represent a range. In general, a
   * range is considered to be <i>either</i> two numeric strings <i>or</i> two
   * non-numeric strings, separated by a hyphen '-' with optional whitespace.
   * The second value is expected to be numerically or lexically greater than
   * the first. For example, "s1-4" would not qualify as either a numeric or a
   * non-numeric range, while "I-4" ("I" being a Roman number) and the string
   * range "a-baa" would.
   * <p>
   * "s1-4" and "1-s4" are invalid ranges
   * Both sides if the hyphen must either be parseable as integers, or both
   * involve non-numerical tokens.
   * <p>
   * To allow for identifers that themselves incorporate a hyphen, the input
   * string is only split around the centremost hyphen. If there is an even
   * number of hyphens, the input string is assumed not to represent a parseable
   * range.
   */
  public final void testIsRange() {
    // Invalid input
    assertFalse(isRange(null));
    assertFalse(isRange(""));

    // -------------------------------------------------------------------------
    // A variety of strings not valid as ranges
    // -------------------------------------------------------------------------
    // Invalid ranges
    assertFalse(isRange("s1-4"));
    assertFalse(isRange("1-s4"));
    assertFalse(isRange("s1-s2-s3"));
    assertFalse(isRange("s1-2-3"));
    assertFalse(isRange("s123"));
    assertFalse(isRange("1-2-3"));
    assertFalse(isRange("123"));
    assertFalse(isRange("a-1"));
    assertFalse(isRange("1-two"));
    // Invalid downward ranges
    assertFalse(isRange("II-I"));
    assertFalse(isRange("2-1"));
    assertFalse(isRange("bb-aa"));
    // Mixed-format string not representing range
    assertFalse(isRange("os-1"));
    assertFalse(isRange("os-I"));
    assertFalse(isRange("os-V"));

    // -------------------------------------------------------------------------
    // Examples where one side of the hyphen can be interpreted as a Roman
    // number, and we already know that it is not a numerical range where both
    // sides are numerical.
    //
    // These are considered non-ranges where the tokens either side of the
    // hyphen are different lengths, or they suggest a descending alphabetical
    // range.
    // -------------------------------------------------------------------------
    // Topical range, as the tokens are the same length and alphabetically increasing
    assertTrue(isRange("va-vi"));
    assertTrue(isRange("il-iv"));
    // non-normalized roman number is part of s numerical range
    assertTrue(isRange("i-ixi"));
    // This is not a topical range as it decreases; it is a single id
    assertFalse(isRange("vi-va"));
    // This is a single id; it is not a topic range because of the case difference
    assertFalse(isRange("os-VI"));
    // These are considered to be non-ranges (single ids) with numerical and
    // non-numerical parts of different lengths
    assertFalse(isRange("i-ion"));
    assertTrue(isRange("vin-viv"));
    // This is a range as the vii can represent a normalised Roman number,
    // and the tokens are the same length and alphabetically increasing
    assertTrue(isRange("vii-vin"));
    // This is not a range; the vii can represent a normalised Roman number,
    // but the range would be descending
    assertFalse(isRange("vin-vii"));

    // -------------------------------------------------------------------------
    // A variety of valid ranges
    // -------------------------------------------------------------------------
    assertTrue(isRange("i-iv"));
    assertTrue(isRange("I-II"));
    assertTrue(isRange("a-aa"));
    assertTrue(isRange("a-b"));
    assertTrue(isRange("a-baa"));
    assertTrue(isRange("aardvark - bat"));
    assertTrue(isRange("s2 - s10"));
    assertTrue(isRange("a-1 - b-1"));
    assertTrue(isRange("1-2-3 - 2-3-4"));
    // Individual non-numerical tokens are not compared
    assertTrue(isRange("s1-t4"));
    // Valid mixed-format ranges
    assertTrue(isRange("I-4"));
    assertTrue(isRange("1-IV"));
    assertTrue(isRange("os-1 - os-V"));
    // Currently we allow non-increasing ranges (but not descedning ones)
    assertTrue(isRange("I-I"));
    assertTrue(isRange("a-a"));
    assertTrue(isRange("hello-hello"));
    assertTrue(isRange("1-1"));
    assertTrue(isRange("1-a -1-a"));
    assertTrue(isRange("s1-I-s1-IIII"));
    assertTrue(isRange("s1-I-s1-4"));
    assertTrue(isRange("s1-II-s1-4"));
    assertTrue(isRange("s1-II - s1-4"));
    // The following will not work because the boundary between the s/v prefix
    // and the Roman number is a boundary btw lower/upper case.
    // TODO It would be good to incorporate this in the regexp
    assertFalse(isRange("sII-s4"));
    assertFalse(isRange("vII-v4"));
  }

  /**
   * Test numeric and text ranges specified as comma/semicolon-separated lists.
   */
  public final void testCoverageIncludes() {
    assertFalse(BibliographicUtil.coverageIncludes(null, ""));
    assertFalse(BibliographicUtil.coverageIncludes("", null));
    assertFalse(BibliographicUtil.coverageIncludes(null, null));
    // Empty coverage string does not include the empty string
    assertFalse(BibliographicUtil.coverageIncludes("", ""));

    assertTrue(BibliographicUtil.coverageIncludes("1994,1996", "1994"));
    assertFalse(BibliographicUtil.coverageIncludes("1994,1996", "1995"));

    String yrRng = "1994,1996-1998,1999";
    assertTrue(BibliographicUtil.coverageIncludes(yrRng, "1994"));
    assertTrue(BibliographicUtil.coverageIncludes(yrRng, "1997"));
    assertTrue(BibliographicUtil.coverageIncludes(yrRng, "1999"));

    String alphaRng = "s1-3;s1-4;s1-5-s1-9";
    assertTrue(BibliographicUtil.coverageIncludes(alphaRng, "s1-7"));
    assertFalse(BibliographicUtil.coverageIncludes(alphaRng, "s4"));
    assertFalse(BibliographicUtil.coverageIncludes(alphaRng, "4"));

    String romanRng = "ii,iii-x;xii-XII";
    // The mixed case range should be normalised and yield a single value,
    // but the test will also normalise the search value by
    // recognising and testing it as a Roman number.
    assertTrue(BibliographicUtil.coverageIncludes(romanRng, "XII"));
    assertTrue(BibliographicUtil.coverageIncludes(romanRng, "xii"));
    assertFalse(BibliographicUtil.coverageIncludes(romanRng, "xiii"));
    assertFalse(BibliographicUtil.coverageIncludes(romanRng, "XIII"));
    // The iii-x range includes 4 and 8, in whatever format
    assertTrue(BibliographicUtil.coverageIncludes(romanRng, "iv"));
    assertTrue(BibliographicUtil.coverageIncludes(romanRng, "iiii"));
    assertTrue(BibliographicUtil.coverageIncludes(romanRng, "viii"));
    assertTrue(BibliographicUtil.coverageIncludes(romanRng, "iix"));

    // Test that Roman tokens get interpreted as Roman tokens when necessary
    // Do not contain values from an alphabetic interpretation (topic ranges)
    // (1) s1v-s1x to include s1vi and s1ix (should be interpreted as Roman nums)
    assertTrue(BibliographicUtil.coverageIncludes("s1v-s1x", "s1vi"));
    assertTrue(BibliographicUtil.coverageIncludes("s1v-s1x", "s1ix"));
    // (2) s1v-s1x to also include s1w - interpret as alphabetic not Roman,
    // given the context provided by the search value
    assertTrue(BibliographicUtil.coverageIncludes("s1v-s1x", "s1w"));
    // (3) s1ii to not include s1iii (should be interpreted as Roman nums)
    assertFalse(BibliographicUtil.coverageIncludes("s1i, s1ii", "s1iii"));

    // Alphabetic/topic range examples
    assertTrue(BibliographicUtil.coverageIncludes("a-c", "b"));
    assertTrue(BibliographicUtil.coverageIncludes("a-c,d,e,f;g-t", "b"));
    assertTrue(BibliographicUtil.coverageIncludes("a-c,d,e,f;g-t,", "s"));

    // Identifiers with hyphens
    assertTrue(BibliographicUtil.coverageIncludes("a-a - a-y", "a-s"));
    assertFalse(BibliographicUtil.coverageIncludes("a-a - a-y", "s"));

    assertTrue(BibliographicUtil.coverageIncludes("s1-2-a;s1-2-c - s1-2-g", "s1-2-f"));
    assertFalse(BibliographicUtil.coverageIncludes("s1-2-a;s1-2-c - s1-2-g", "s1-2-b"));
    assertFalse(BibliographicUtil.coverageIncludes("s1-2,s1-4", "s1-3"));

    // Other mixed examples
    assertTrue(BibliographicUtil.coverageIncludes("a-c;aa-cc", "bb"));
    assertTrue(BibliographicUtil.coverageIncludes("a-c;aa-cc", "bbb"));
    // Mixed identifier types, multiple delimiters
    String weirdMix = "a-c,;,,;g-t,s1-s3;v-x,vol001-vol090";
    assertTrue(BibliographicUtil.coverageIncludes(weirdMix, "s"));
    assertTrue(BibliographicUtil.coverageIncludes(weirdMix, "s1"));
    assertTrue(BibliographicUtil.coverageIncludes(weirdMix, "s2"));
    assertTrue(BibliographicUtil.coverageIncludes(weirdMix, "vi"));
    assertTrue(BibliographicUtil.coverageIncludes(weirdMix, "w"));
    assertTrue(BibliographicUtil.coverageIncludes(weirdMix, "vol040"));
  }


  /**
   * Test individual numeric and text ranges. Note that a change of formats
   * between start and end indicates a non-range.
   */
  public final void testRangeIncludes() {
    assertFalse(BibliographicUtil.rangeIncludes(null, ""));
    assertFalse(BibliographicUtil.rangeIncludes("", null));
    assertFalse(BibliographicUtil.rangeIncludes(null, null));
    assertTrue(BibliographicUtil.rangeIncludes("", ""));

    // rangeIncludes takes only a range string, not list
    assertFalse(BibliographicUtil.rangeIncludes("1994,1996", "1994"));
    assertTrue(BibliographicUtil.rangeIncludes("1994,1996", "1994,1996"));

    // Year ranges
    assertTrue(BibliographicUtil.rangeIncludes("1994", "1994"));
    assertTrue(BibliographicUtil.rangeIncludes("1997-1999", "1997"));
    assertTrue(BibliographicUtil.rangeIncludes("1997-1999", "1998"));
    assertTrue(BibliographicUtil.rangeIncludes("1997-1999", "1999"));

    // Ranges of identifiers with hyphen
    assertTrue(BibliographicUtil.rangeIncludes("s1-5-s1-9", "s1-5"));
    assertTrue(BibliographicUtil.rangeIncludes("s1-5-s1-9", "s1-7"));
    assertTrue(BibliographicUtil.rangeIncludes("a-1", "a-1"));
    assertFalse(BibliographicUtil.rangeIncludes("a-1", "a"));
    assertFalse(BibliographicUtil.rangeIncludes("a-1", "1"));

    // Alphabetical base-26 ranges
    assertTrue(BibliographicUtil.rangeIncludes("a-c", "b"));
    assertTrue(BibliographicUtil.rangeIncludes("a-c", "bb"));
    // Value and range endpoints interpreted as base-26 as same length
    assertTrue(BibliographicUtil.rangeIncludes("aa-cc", "bb"));
    // Range interpreted as topic range as diff lengths
    assertTrue(BibliographicUtil.rangeIncludes("aa-cc", "bbb"));
   
    // Roman ranges
    assertTrue(BibliographicUtil.rangeIncludes("v-x", "vii"));
    // Potentially alphabetical range
    assertTrue(BibliographicUtil.rangeIncludes("v-x", "w"));
    // Mixed cases should be normalised
    assertTrue(BibliographicUtil.rangeIncludes("i-x", "VII"));
    assertTrue(BibliographicUtil.rangeIncludes("i-X", "Vii"));
    assertTrue(BibliographicUtil.rangeIncludes("i-x", "ii"));
    assertTrue(BibliographicUtil.rangeIncludes("i-X", "II"));
    assertTrue(BibliographicUtil.rangeIncludes("i-X", "ii"));
    // With single value; spurious range
    assertTrue(BibliographicUtil.rangeIncludes("xii-XII", "XII"));
    assertTrue(BibliographicUtil.rangeIncludes("xii-XII", "xii"));
    // With single value; no range
    assertTrue(BibliographicUtil.rangeIncludes("xii", "XII"));
    assertTrue(BibliographicUtil.rangeIncludes("XII", "xii"));

    // Zero-padded number components
    assertTrue(BibliographicUtil.rangeIncludes("vol001-vol090", "vol040"));

    // Textual numbers are not recognised - they are interpreted alphabetically
    assertFalse(BibliographicUtil.rangeIncludes("one-three", "two"));
    assertFalse(BibliographicUtil.rangeIncludes("one-seven", "three"));

    // Test that Roman tokens get interpreted as Roman tokens when necessary
    // Do not contain values from an alphabetic interpretation (topic ranges)
    // (1) s1v-s1x to include s1vi and s1ix (should be interpreted as Roman nums)
    assertTrue(BibliographicUtil.rangeIncludes("s1v-s1x", "s1vi"));
    assertTrue(BibliographicUtil.rangeIncludes("s1v-s1x", "s1ix"));
    // (2) s1v-s1x to also include s1w - interpret as alphabetic not Roman,
    // given the context provided by the search value
    assertTrue(BibliographicUtil.rangeIncludes("s1v-s1x", "s1w"));
    // (3) s1ii to not include s1iii (should be interpreted as Roman nums)
    assertFalse(BibliographicUtil.rangeIncludes("s1i, s1ii", "s1iii"));

    // Identifiers with hyphens
    assertTrue(BibliographicUtil.rangeIncludes("a-a - a-y", "a-s"));
    assertFalse(BibliographicUtil.rangeIncludes("a-a - a-y", "s"));
    assertTrue(BibliographicUtil.rangeIncludes("s1-2-c - s1-2-g", "s1-2-f"));
  }

  public final void testCommonTokenBasedPrefix() {
    // Empty strings
    assertEquals("", BibliographicUtil.commonTokenBasedPrefix("", ""));
    assertEquals("", BibliographicUtil.commonTokenBasedPrefix("", "123"));
    assertEquals("", BibliographicUtil.commonTokenBasedPrefix("123", ""));

    // Alphabetic substr
    assertEquals("s", BibliographicUtil.commonTokenBasedPrefix("s100", "s101"));
    // Numerical substr
    assertEquals("123", BibliographicUtil.commonTokenBasedPrefix("123abc", "123abcd"));
    // Mixed substr
    assertEquals("123abc", BibliographicUtil.commonTokenBasedPrefix("123abc", "123abc1"));
    assertEquals("123abc", BibliographicUtil.commonTokenBasedPrefix("123abc", "123abc321cba"));

    // Empty string in common
    assertEquals("", BibliographicUtil.commonTokenBasedPrefix("100", "101"));
    assertEquals("", BibliographicUtil.commonTokenBasedPrefix("123", "abc"));

    // One string substr of the other
    assertEquals("s", BibliographicUtil.commonTokenBasedPrefix("s", "s1"));
    assertEquals("1a2b3c",
        BibliographicUtil.commonTokenBasedPrefix("1a2b3c4", "1a2b3c")
    );

    // Full string in common
    assertEquals("1a2b3c",
        BibliographicUtil.commonTokenBasedPrefix("1a2b3c", "1a2b3c")
    );
    // Check no common substr which is not at start
    assertEquals("",
        BibliographicUtil.commonTokenBasedPrefix("a123", "b123")
    );

    // Roman numeral token examples
    // Full string in common
    assertEquals("sI-",
        BibliographicUtil.commonTokenBasedPrefix("sI-2", "sI-II")
    );
    assertEquals("sI-",
        BibliographicUtil.commonTokenBasedPrefix("sI-2", "sI-24")
    );

  }


  /**
   * Returns true if one string contains only digits while the other is not
   * parseable as a number. Note this method is also exercised in
   * TestBibliographicOrderScorer.
   */
  public final void testChangeOfFormats() {
    assertTrue(  changeOfFormats("1", null) );
    assertTrue(  changeOfFormats(null, "1") );
    // No change of formats where neither value can be parsed as a number
    assertFalse( changeOfFormats("string", null) );
    assertFalse( changeOfFormats(null, "string") );
    assertFalse( changeOfFormats(null, null) );

    assertTrue(  changeOfFormats("string", "1") );
    assertTrue(  changeOfFormats("1", "string") );
    assertFalse( changeOfFormats("1", "1") );
    assertFalse( changeOfFormats("string", "string") );
    // No change of formats when either string can be parsed as a Roman number
    assertFalse( changeOfFormats("1", "II") );
    assertFalse( changeOfFormats("II", "3") );
    // The following are currently false but we might want to do some proper
    // regular expression work to find and rate string commonalities, or
    // use something like Levenstein distance.
    assertFalse( changeOfFormats("s1-1", "volume 8") );
    assertFalse( changeOfFormats("99", "ill") );
  }


  /**
   *
   */
  public final void testTranslateRomanTokens() {
    assertEquals("s1-2", BibliographicUtil.translateRomanTokens("s1-II"));
    assertEquals("sI-2", BibliographicUtil.translateRomanTokens("sI-II"));
    assertEquals("os:2-8", BibliographicUtil.translateRomanTokens("os:2-VIII"));
    assertEquals("os:2-8", BibliographicUtil.translateRomanTokens("os:2-Viii"));
    assertEquals("os:2-8", BibliographicUtil.translateRomanTokens("os:II-viii"));
    assertEquals("6", BibliographicUtil.translateRomanTokens("VI"));
    // Check start and end tokens
    assertEquals("5-vol", BibliographicUtil.translateRomanTokens("V-vol"));
    assertEquals("vol;5", BibliographicUtil.translateRomanTokens("vol;V"));
    // Roman tokens must be normalised to get translated
    assertEquals("viv", BibliographicUtil.translateRomanTokens("viv"));
    // Check non-Roman tokens remain unchanged, including null
    assertEquals(null, BibliographicUtil.translateRomanTokens(null));
    assertEquals("", BibliographicUtil.translateRomanTokens(""));
    assertEquals("6", BibliographicUtil.translateRomanTokens("6"));
    assertEquals("-", BibliographicUtil.translateRomanTokens("-"));

    checkPerl5MatcherUsageWithTask(new Runnable() {
      public void run() { BibliographicUtil.translateRomanTokens("s1-II"); }
    });
  }


  /**
   * Test the construction of string sequences.
   */
  public final void testConstructSequenceStrings() {

    // vol08-vol10 zero-padded
    assertIsomorphic(
        Arrays.asList(new String[]{"vol08", "vol09", "vol10"}),
        BibliographicUtil.constructSequence("vol08", "vol10")
    );
    // vol8-vol10
    assertIsomorphic(
        Arrays.asList(new String[]{"vol8", "vol9", "vol10"}),
        BibliographicUtil.constructSequence("vol8", "vol10")
    );
    // Complex volume strings
    assertIsomorphic(
        Arrays.asList(new String[]{"Volume s1-10", "Volume s1-11", "Volume s1-12"}),
        BibliographicUtil.constructSequence("Volume s1-10", "Volume s1-12")
    );
    // Volume strings with Roman component
    assertIsomorphic(
        Arrays.asList(new String[]{"Volume s1-III", "Volume s1-IV", "Volume s1-V"}),
        BibliographicUtil.constructSequence("Volume s1-III", "Volume s1-V")
    );
    // Backwards sequence, Roman component, default delta (magnitude 1)
    assertIsomorphic(
        Arrays.asList(new String[]{"Volume s1-V", "Volume s1-IV", "Volume s1-III"}),
        BibliographicUtil.constructSequence("Volume s1-V", "Volume s1-III", 1)
    );
    // Backwards sequence, Roman component, delta magnitude and sign
    assertIsomorphic(
        Arrays.asList(new String[]{"Volume s1-V", "Volume s1-IV", "Volume s1-III"}),
        BibliographicUtil.constructSequence("Volume s1-V", "Volume s1-III", -1)
    );

    //
    /*assertIsomorphic(
        Arrays.asList(new String[]{"vol10v", "vol10w", "vol10x"}),
        BibliographicUtil.constructSequence("vol10v", "vol10x")
    );*/
    // Lower case Roman - could be alphabetic but we assume Roman first if it parses
    assertIsomorphic(
        Arrays.asList(new String[]{"vol10v", "vol10vi", "vol10vii", "vol10viii", "vol10ix", "vol10x"}),
        BibliographicUtil.constructSequence("vol10v", "vol10x")
    );

    // Lower case Roman - we assume alphabetic with non-standard Roman format
    assertEquals(20, BibliographicUtil.constructSequence("vic", "viv").size());
    // Roman sequences with unclear boundaries based on case are not tokenised,
    // so will result in alphabetical sequence
    assertEquals(14, BibliographicUtil.constructSequence("sII", "sIV").size());

    // Unclear boundary before Roman component - requires case-change tokenisation
    /*assertIsomorphic(
        Arrays.asList(new String[]{"volumeII", "volumeIII", "volumeIV"}),
        BibliographicUtil.constructSequence("volumeII", "volumeIV")
    );*/

    // Empty strings
    assertIsomorphic(
        Arrays.asList(new String[]{""}),
        BibliographicUtil.constructSequence("", "")
    );

    // Failures
    // Mixed zero-padding
    checkConstructSequenceFailure("s1-01", "s1-9");
    // Unsupported sequences - not enough info to decide an increment scheme
    checkConstructSequenceFailure("1-1", "2-1");
    checkConstructSequenceFailure("alpha", "beta");
  }


  private boolean checkConstructSequenceFailure(String s, String e) {
    try {
      BibliographicUtil.constructSequence(s, e);
      fail("Should throw exception");
      return false;
    } catch (Exception ex) {
      /* Expected exception */
      return true;
    }
  }


  /**
   * The tokenisation should result in separate tokens for digits, non-digits,
   * and symbols.
   */
  public final void testAlphanumericTokenisation() {
    checkAlphanumericTokenisation("", 0);
    checkAlphanumericTokenisation("a1b2c3d4", 8);
    checkAlphanumericTokenisation("1a2b3c4d", 8);
    checkAlphanumericTokenisation("s1-2", 4);
    checkAlphanumericTokenisation("a1-2 - b2-4", 9);
    checkAlphanumericTokenisation("abc$%^123", 3);

    // The method does not currently distinguish tokens based on case
    checkAlphanumericTokenisation("aIV", 1);

    checkPerl5MatcherUsageWithTask(new Runnable() {
      public void run() { checkAlphanumericTokenisation("a1-2 - b2-4", 9); }
    });
  }

  /**
   * Try parallel usage of a class or method that uses Perl5Matcher, via an
   * ExecutorService; the thread-local Perl5Matcher should be used carefully,
   * and not shared among threads (for example, by making it static).
   * @param r
   */
  public final void checkPerl5MatcherUsageWithTask(Runnable r) {
    ExecutorService executorService = Executors.newFixedThreadPool(3);
    List<Future> futures = new ArrayList<Future>();
    for (int i=0; i < 100; i++) futures.add( executorService.submit(r) );
    try {
      for (Future f : futures) f.get();
    } catch (InterruptedException e) {
      fail("Thread should not be interrupted "+e);
    } catch (ExecutionException e) {
      fail("Runnable task should not throw exception "+e);
    }
  }

  /**
   * Check that the AlphanumericTokenisation of the given string displays the
   * expected characteristics.
   * @param at
   * @param expectedNumTokens
   * @param numFirst
   */
  private final void checkAlphanumericTokenisation(String s,
                                                   int expectedNumTokens) {
    AlphanumericTokenisation at = new AlphanumericTokenisation(s);
    // Total size of tokens should equal size of string
    int n = 0;
    for (String tok : at.tokens) n += tok.length();
    assertEquals(s.length(), n);
    // Expected number of tokens
    assertEquals(expectedNumTokens, at.numTokens());
    // Expected isNumFirst
    assertEquals(ptn.matcher(s).find(), at.isNumFirst);
  }
  /** A pattern to match digits at start of string. */
  Pattern ptn = Pattern.compile("^\\d");


  /**
   * Check the behaviour of RangeIterator and VolumeIterator.
   */
  public final void testRangeVolumeIterators() {
    // Year ranges
    String yrRng = "1994,1996-1998,1999";
    assertIsomorphic(
                     Arrays.asList(new String[]{"1994", "1996-1998", "1999"}),
                     getArrayFromIterator(new RangeIterator(yrRng))
                     );
    assertIsomorphic(
                     Arrays.asList(new String[]{"1994", "1996", "1997", "1998", "1999"}),
                     getArrayFromIterator(new VolumeIterator(yrRng))
    );

    // Alphabetic ranges
    String alphaRng = "s1-3;s1-4;s1-5-s1-9";
    assertIsomorphic(
                     Arrays.asList(new String[]{"s1-3", "s1-4", "s1-5-s1-9"}),
                     getArrayFromIterator(new RangeIterator(alphaRng))
    );
    assertIsomorphic(
                     Arrays.asList(new String[]{"s1-3", "s1-4", "s1-5", "s1-6", "s1-7", "s1-8", "s1-9"}),
                     getArrayFromIterator(new VolumeIterator(alphaRng))
    );

    // Roman ranges
    String romanRng = "ii,iii-x;xii-XII";
    assertIsomorphic(
                     Arrays.asList(new String[]{"ii", "iii-x", "xii-XII"}),
                     getArrayFromIterator(new RangeIterator(romanRng))
    );
    assertIsomorphic(
                     Arrays.asList(new String[]{"ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x", "XII"}),
                                   getArrayFromIterator(new VolumeIterator(romanRng))
    );

    // Mixed ranges
    String weirdMix = "a-c,;,,;g-i,s1-s3;v-x,vol008-vol010";
    assertIsomorphic(
                     Arrays.asList(new String[]{"a-c", "g-i", "s1-s3", "v-x", "vol008-vol010"}),
                     getArrayFromIterator(new RangeIterator(weirdMix))
                     );
    assertIsomorphic(
                     Arrays.asList(new String[]{"a", "b", "c", "g", "h", "i",
                                                "s1", "s2", "s3",
                                                "v", "vi", "vii", "viii", "ix", "x",
                                                "vol008", "vol009", "vol010"}),
                     getArrayFromIterator(new VolumeIterator(weirdMix))
                     );
  }
  
  /**
   * Check the behaviour of SequenceIterators.
   */
  public final void testSequenceIterators() {
    System.out.println("testSequenceIterators() - iterators exercised in testRangeVolumeIterators");
  }

  private List<String> getArrayFromIterator(final Iterator<String> it) {
    return new ArrayList() {{
      while (it.hasNext()) add(it.next());
    }};
  }

}
