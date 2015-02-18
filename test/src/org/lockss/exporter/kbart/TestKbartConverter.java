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
package org.lockss.exporter.kbart;

import java.util.*;

import org.lockss.config.*;
import org.lockss.config.Tdb.TdbException;
import org.lockss.exporter.biblio.BibliographicUtil;
import static org.lockss.exporter.biblio.BibliographicUtil.*;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.Plugin;
import org.lockss.test.*;
import org.lockss.util.NumberUtil;
import org.lockss.util.StringUtil;


/**
 * Try some conversions from TdbTitles to KbartTitles. This is done using a
 * single TdbTitle via <code>TdbTestUtil.makeRangeTestTitle()</code>.
 * The <code>extractAllTitles()</code> method is not exercised; instead we just
 * run <code>convertTitleToKbartTitles()</code> once per TdbTitle.
 * <p>
 * Note that a lot of code in the converter class makes use of
 * {@link TdbAuOrderScorer}, which gets a good exercising in
 * {@link TestTdbAuOrderScorer}.
 *
 * @author neil
 */
public class TestKbartConverter extends LockssTestCase {

  // The base for an auid; note that if this is removed from the TDB files,
  // the tests may break.
  static final String auidAbsintheBase = 
    "org|lockss|plugin|absinthe|AbsinthePlugin&base_url~http%3A%2F%2Fabsintheliteraryreview%2Ecom%2F&year~";
  static final String auid1 = auidAbsintheBase+"2003";
  static final String auid2 = auidAbsintheBase+"2004";
  static final String auid3 = auidAbsintheBase+"2005";
  
  public final void testExtractAllTitles() {
    // extractAllTitles() just runs convertTitles on all TdbTitles
  }
  public final void testExtractTitles() {
    // extractTitles() just runs convertTitles on TdbTitles within a scope
  }

  /**
   * Check that the result of the wrapper method is the same as calling
   * convertTitleToKbartTitles on a single title.
   */
  public final void testConvertTitles() throws KbartConverter.ConversionException {
    List<TdbTitle> l = Collections.emptyList();
    assertEmpty(KbartConverter.convertTitles(null));
    assertEmpty(KbartConverter.convertTitles(l));
    try {
      final TdbTitle title = TdbTestUtil.makeRangeTestTitle(false);
      assertIsomorphic(KbartConverter.convertTitleToKbartTitles(title),
          KbartConverter.convertTitles(new Vector(){{ add(title); }})
      );
    } catch (TdbException e) {
      fail("Exception while making range test title: "+e);
    }
  }

  /**
   * Compare results of convertTitleAus and convertTitleToKbartTitles.
   */
  public final void testConvertAus() throws KbartConverter.ConversionException {
    boolean showHealth = true;
    boolean rangeFieldsIncluded = true;

    final List<ArchivalUnit> ausNull = null;
    final List<ArchivalUnit> ausEmpty = Collections.emptyList();
    final List<ArchivalUnit> ausMock = makeMockAuList();
    
    // Test null
    assertEmpty(KbartConverter.convertTitleToKbartTitles(
        ausNull, showHealth, rangeFieldsIncluded
    ));
    // Test null list, empty list, list of mock AUs
    //for (List<ArchivalUnit> lau : Arrays.asList(ausNull, ausEmpty, ausMock)) {
    for (List<ArchivalUnit> lau : Arrays.asList(ausMock)) {
      List<KbartTitle> result = KbartConverter.convertTitleToKbartTitles(
          lau, showHealth, rangeFieldsIncluded
      );
      assertIsomorphic(
          result,
          KbartConverter.convertTitleAus(
              TdbUtil.mapTitlesToAus(lau).values(), showHealth, rangeFieldsIncluded
          )
      );
    }
    
    // Test empty map against map with empty value lists
    try {
      Map<TdbTitle, List<ArchivalUnit>> emptyAusMap = new HashMap<TdbTitle, List<ArchivalUnit>>() {{
        put(TdbTestUtil.makeRangeTestTitle(false), ausEmpty);
        put(TdbTestUtil.makeRangeTestTitle(false), ausEmpty);
      }};
      assertIsomorphic(
          KbartConverter.convertTitleToKbartTitles(ausEmpty, showHealth, rangeFieldsIncluded),
          KbartConverter.convertTitleAus(emptyAusMap.values(), showHealth, rangeFieldsIncluded)
      );
    } catch (TdbException e) {
      fail("Exception while making range test title: "+e);
    }
  }

  /**
   * This is tested implicitly by the testCreateKbartTitles() methods.
   */
  public final void testGetAuCoverageRanges() {
    //fail("Not yet implemented");
    System.out.println("testGetAuCoverageRanges() not implemented.");
    // TODO Try running the method on some of the representative examples from
    // TestTdbAuOrderScorer, and compare the result against the ranges specified
    // there.
  }

  /**
   * Test extracting volume fields from a list of AUs.
   * The list of AUs must all have values for volume, and those
   * values must not be all the same (e.g. a list of placeholders like zero).
   * If the values are empty, all the same, any are unparseable, or they are not
   * available for all the AUs, null is returned.
   */
  public final void testGetAuVols() throws TdbException {
    String name     = "Monkeys Monthly";
    String yr       = "2011";
    TdbAu noVol     = TdbTestUtil.createBasicAu(name, yr, "");
    TdbAu nullVol   = TdbTestUtil.createBasicAu(name, yr, null);
    TdbAu vol1      = TdbTestUtil.createBasicAu(name, yr, "vol1");
    TdbAu vol2      = TdbTestUtil.createBasicAu(name, yr, "vol2");

    // Valid lists of Aus, with a variety of volumes
    assertEquals(2, KbartConverter.getAuVols(Arrays.asList(vol1, vol2)).size());
    assertEquals(3, KbartConverter.getAuVols(Arrays.asList(vol1, vol2, vol2)).size());

    // An empty list should return null as all the volumes are the same
    assertNull(KbartConverter.getAuVols(Collections.<TdbAu>emptyList()));
    // Null if any vols are null
    assertNull(KbartConverter.getAuVols(Arrays.asList(vol1, nullVol)));
    // Null if vols are all the same
    assertNull(KbartConverter.getAuVols(Arrays.asList(vol1, vol1)));
    // Null if any vols are empty
    assertNull(KbartConverter.getAuVols(Arrays.asList(vol1, noVol)));
  }

  /**
   * Test getAuYears().
   * If any year cannot be parsed, null is returned.
   * A list is only returned if a year could be parsed for every AU.
   */
  public final void testGetAuYears() throws TdbException {
    String name     = "Monkeys Monthly";
    TdbAu noYr     = TdbTestUtil.createBasicAu(name, "");
    TdbAu nullYr   = TdbTestUtil.createBasicAu(name, null);
    TdbAu badYr    = TdbTestUtil.createBasicAu(name, "HA!");
    TdbAu yr1      = TdbTestUtil.createBasicAu(name, "1976");
    TdbAu yr2      = TdbTestUtil.createBasicAu(name, "1984");

    // Valid lists of Aus, with a variety of yrs
    assertEquals(2, KbartConverter.getAuYears(Arrays.asList(yr1, yr1)).size());
    assertEquals(2, KbartConverter.getAuYears(Arrays.asList(yr1, yr2)).size());

    // Null if the au list is empty
    assertNull(KbartConverter.getAuYears(Collections.<TdbAu>emptyList()));
    // Null if yrs cannot be parsed
    assertNull(KbartConverter.getAuYears(Arrays.asList(yr1, badYr)));
    // Null if any yrs are null
    assertNull(KbartConverter.getAuYears(Arrays.asList(yr1, nullYr)));
    // Null if any yrs are empty
    assertNull(KbartConverter.getAuYears(Arrays.asList(yr1, noYr)));
  }

  /**
   * Test sortByVolumeYear() and sortByYearVolume().
   * Note these get well exercised in TestTdbAuOrderScorer.
   */
  public final void testSortTdbAus() throws TdbException {
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

  /**
   * Does the list of TdbAus contain mixed volume formats.
   * Note that there is no change of formats if one of the volume strings
   * can be parsed as a Roman number.
   */
  public final void testContainsMixedFormats() throws TdbException {
    TdbAu auStr       = TdbTestUtil.createBasicAu("", "", "s1");
    TdbAu auInt       = TdbTestUtil.createBasicAu("", "", "1");
    TdbAu auRom       = TdbTestUtil.createBasicAu("", "", "I");
    TdbAu auUnnormRom = TdbTestUtil.createBasicAu("", "", "VIV");
    // Single items do not have mixed formats
    assertFalse(KbartConverter.containsMixedFormats(Arrays.asList(auStr)));
    assertFalse(KbartConverter.containsMixedFormats(Arrays.asList(auInt)));
    assertFalse(KbartConverter.containsMixedFormats(Arrays.asList(auRom)));
    assertFalse(KbartConverter.containsMixedFormats(Arrays.asList(auUnnormRom)));
    // Not mixed formats
    assertFalse(KbartConverter.containsMixedFormats(Arrays.asList(auStr, auStr)));
    assertFalse(KbartConverter.containsMixedFormats(Arrays.asList(auInt, auInt)));
    assertFalse(KbartConverter.containsMixedFormats(Arrays.asList(auRom, auUnnormRom)));
    // Mixed formats
    assertTrue(KbartConverter.containsMixedFormats(Arrays.asList(auStr, auInt)));
    assertTrue(KbartConverter.containsMixedFormats(Arrays.asList(auInt, auStr)));
    assertTrue(KbartConverter.containsMixedFormats(Arrays.asList(auInt, auStr, auInt)));
    // Integers with Roman numeral strings are not considered to be mixed
    assertFalse(KbartConverter.containsMixedFormats(Arrays.asList(auInt, auRom)));
    assertFalse(KbartConverter.containsMixedFormats(Arrays.asList(auInt, auUnnormRom)));
    assertFalse(KbartConverter.containsMixedFormats(Arrays.asList(auRom, auInt)));
    assertFalse(KbartConverter.containsMixedFormats(Arrays.asList(auUnnormRom, auInt)));
    // Mixed strings with Roman numeral strings are considered to be mixed
    assertTrue(KbartConverter.containsMixedFormats(Arrays.asList(auStr, auRom)));
    assertTrue(KbartConverter.containsMixedFormats(Arrays.asList(auStr, auUnnormRom)));
    assertTrue(KbartConverter.containsMixedFormats(Arrays.asList(auRom, auStr)));
    assertTrue(KbartConverter.containsMixedFormats(Arrays.asList(auUnnormRom, auStr)));
  }

  /**
   *
   */
  public final void testSortKbartTitles() {
    //fail("Not yet implemented");
    System.out.println("testSortKbartTitles() not implemented.");
  }

  /**
   *
   */
  public final void testConvertTitleToKbartTitlesCollectionOfArchivalUnitBooleanBoolean() {
    //fail("Not yet implemented");
    System.out.println("testConvertTitleToKbartTitlesCollectionOfArchivalUnitBooleanBoolean() not implemented.");
  }

  /**
   *
   */
  public final void testConvertTitleToKbartTitlesListOfTdbAu() {
    //fail("Not yet implemented");
    System.out.println("testConvertTitleToKbartTitlesListOfTdbAu() not implemented.");
  }

  /**
   * Create a KbartTitle, copying these fields from the supplied TdbAu argument:
   * publisher name, publication title, ISSN identifiers and URL.
   */
  public final void testCreateBaseKbartTitle() {
    try {
      List<TdbAu> someAus = Arrays.asList(
          // Create TdbAu with name, year, volume
          TdbTestUtil.createBasicAu("Monkeys Monthly no. 1", "2011", "1"),
          // TdbAu with empty fields
          TdbTestUtil.createBasicAu("", "", "")
      );
      for (Collection<TdbAu> aus: Arrays.asList(
          someAus,
          TdbTestUtil.makeRangeTestTitle(true).getTdbAus(),
          TdbTestUtil.makeRangeToNowTestTitle().getTdbAus(),
          TdbTestUtil.makeVolumeTestTitle("Voluminous").getTdbAus(),
          TdbTestUtil.makeYearTestTitle("1994-1997").getTdbAus()
      )) {
        for (TdbAu example: aus) {
          // Add to a title if it is not already; the basic AUs have the same
          // default ids and so must be added to different titles
          try { TdbTestUtil.makeTitleWithNoAus("an id").addTdbAu(example); }
          catch (Exception e) { /* ignore exception - title already set */ }
          KbartTitle base = KbartConverter.createBaseKbartTitle(example);
          testBaseKbartTitleFields(base, example);
        }
      }
    } catch (TdbException e) {
      fail("Could not create example TdbAu.", e);
    }
  }

  /**
   * Test that a KbartTitle created with base field values has
   * the expected values filled in.
   * @param kbt a KbartTitle
   */
  private final void testBaseKbartTitleFields(final KbartTitle kbt, final TdbAu au) {
    final TdbTitle tdbt = au.getTdbTitle();
    // Only publisher name, publication title, ISSN identifiers and URL
    // should be filled. Map these to expected values.
    Map<Field, String> filled = new HashMap<Field, String>() {{
      put(Field.PUBLISHER_NAME,    au.getTdbPublisher().getName());
      put(Field.PUBLICATION_TITLE, tdbt.getName());
      // These fields are based on KbartTdbAuUtil.find*() methods,
      // rather than the exact au.get*() methods.
      /*put(Field.PRINT_IDENTIFIER,  KbartTdbAuUtil.findIssn(au));
      put(Field.ONLINE_IDENTIFIER, KbartTdbAuUtil.findEissn(au));
      put(Field.TITLE_ID,          KbartTdbAuUtil.findIssnL(au));*/
      put(Field.PRINT_IDENTIFIER,  au.getPrintIssn());
      put(Field.ONLINE_IDENTIFIER, au.getEissn());
      put(Field.TITLE_ID,          au.getIssnL());

      // The title URL is based on a very specific string - see KbartConverter
      put(Field.TITLE_URL,
          KbartConverter.DEFAULT_TITLE_URL_PREFIX +
              kbt.getResolverUrlParams());
    }};

    for (Field f : Field.values()) {
      if (filled.keySet().contains(f)) {
        //System.out.format("Comparing %s %s and %s\n", f, filled.get(f), kbt.getField(f));
        // Note that KbartTitles have empty string for unspecified params;
        // BibliographicItem/TdbAu have null.
        // If both are non-null/empty, compare them
        if (!StringUtil.isNullString(filled.get(f)) &&
            !StringUtil.isNullString(kbt.getField(f))) {
          assertEquals(filled.get(f), kbt.getField(f));
        }
      } else assertEquals("", kbt.getField(f));
    }
  }


  /**
   * Test the thin wrapper method which converts a Collection of AUs to a
   * List of TdbAus before passing to the main convertTitleToKbartTitles()
   */
  public final void testConvertTitleToKbartTitlesCollectionAus() {
    // If there are no AUs, an empty list should be returned
    Collection<ArchivalUnit> aus = Collections.emptyList();
    assertEmpty(KbartConverter.convertTitleToKbartTitles(aus, true, true));
    aus = makeMockAuList();
    boolean showHealth = true;
    List<KbartTitle> titles = KbartConverter.convertTitleToKbartTitles(aus, showHealth, true);
    // The result should be at most the size of the AU list 
    assertNotNull(titles);
    //assertNotEmpty(titles); // the result may be empty
    // Note that the result will in fact be empty using mock AUs which don't 
    // correspond to TDB entries.
    assertTrue(titles.size()<=aus.size());
    // If we asked for health values to be calculated, the list should contain 
    // wrapped KbartTitles:
    if (showHealth && titles.size()>0) assertTrue(titles.get(0) instanceof KbartTitleHealthWrapper);
  }

  /**
   * Test the thin wrapper method which gets a List of TdbAus from a TdbTitle
   * before passing to the main convertTitleToKbartTitles()
   */
  public final void testConvertTitleToKbartTitlesListTdbAus() {
    // If there are no TdbAus, an empty list should be returned
    List<TdbAu> noTdbAus = Collections.emptyList();
    assertEmpty(KbartConverter.convertTitleToKbartTitles(noTdbAus));
    // NOTE Testing with dummy TdbAus is handled by testCreateKbartTitlesTdbTitle() 
    // createKbartTitlesTdbTitle() merely extracts TdbAus from a dummy title 
  }

  /**
   * The convertTitleToKbartTitlesWithRanges() method is called by other convertTitleToKbartTitles()
   * methods and gets exercised by the tests for those methods. This test performs
   * some generic tests to ensure that the TitleRanges mapped to by the KbartTitles
   * have sensible parameters. 
   */
  public final void testConvertTitleToKbartTitlesWithRanges() {
    List<TdbAu> noAus = Collections.emptyList();
    assertEmpty(KbartConverter.convertTitleToKbartTitlesWithRanges(noAus));
    
    TdbTitle rangeTitle;
    TdbTitle rangeToNowTitle;
    try {
      rangeTitle = TdbTestUtil.makeRangeTestTitle(true);
      rangeToNowTitle = TdbTestUtil.makeRangeToNowTestTitle();
      for (TdbTitle title : Arrays.asList(rangeTitle, rangeToNowTitle)) {
        List<TdbAu> aus = new ArrayList<TdbAu>(title.getTdbAus());
        Map<KbartTitle, TitleRange> map = KbartConverter.convertTitleToKbartTitlesWithRanges(aus);
        // The map should have at least one title
        assertTrue(map.size()>=1);
        // Put the titles in order
        List<KbartTitle> sortedKeys = new ArrayList<KbartTitle>(map.keySet());
        KbartConverter.sortKbartTitles(sortedKeys); // This sort may not be necessery
        // Keep track of the ranges
        int totalRangeSize = 0;
        TitleRange prevRange = null;
        TitleRange currentRange;
        for (KbartTitle kbt : sortedKeys) {
          currentRange = map.get(kbt);
          int s = currentRange.items.size();
          // Each TitleRange should be of a size no bigger than the full set of aus
          assertTrue(s<=aus.size());
          assertTrue(s>0);
          // If there was a previous range, check the boundary TdbAus
          if (prevRange!=null) {
            // TdbAu following previous last should be the first of this one
            assertEquals(aus.get(totalRangeSize), currentRange.first);
          } else {
            // First TdbAu should be first of first range
            assertEquals(aus.get(0), currentRange.first);
          }
          // Set the previous range and add the range size to the total
          prevRange = currentRange;
          totalRangeSize += currentRange.items.size();
        }
        // The sum should be equal to the original list size
        assertEquals(aus.size(), totalRangeSize);
        // The last TdbAu should be the same as the last of the final range
        assertEquals(aus.get(aus.size()-1), prevRange.last);
      }
    } catch (TdbException e) {
      fail("Could not create TdbTitles: "+e);
    }
  }

  /**
   * Test the main convertTitleToKbartTitles() method
   */
  public final void testConvertTitleToKbartTitlesTdbTitle() {
    try {
      // If there is no TdbTitle, or no TdbAus in the title, an empty list should be returned
      TdbTitle nullTitle = null;
      assertEmpty(KbartConverter.convertTitleToKbartTitles(nullTitle));
      assertEmpty(KbartConverter.convertTitleToKbartTitles(TdbTestUtil.makeTitleWithNoAus("test title!")));

      // Title without volume info; just year ranges leading to a coverage gap
      TdbTitle title = TdbTestUtil.makeRangeTestTitle(false);
      List<KbartTitle> titles = KbartConverter.convertTitleToKbartTitles(title);
      // There should be two ranges based only on the dates
      assertEquals(2, titles.size());

      // Check the dates and vols have been correctly transferred in each title
      KbartTitle t = titles.get(0);
      assertEquals(NumberUtil.toArabicNumber(TdbTestUtil.RANGE_1_START),
          t.getField(Field.DATE_FIRST_ISSUE_ONLINE));
      assertEquals(NumberUtil.toArabicNumber(TdbTestUtil.RANGE_1_END), 
          t.getField(Field.DATE_LAST_ISSUE_ONLINE));
      assertEquals("", t.getField(Field.NUM_FIRST_VOL_ONLINE));
      assertEquals("", t.getField(Field.NUM_LAST_VOL_ONLINE));
      assertEquals(TdbTestUtil.DEFAULT_ISSN_2, t.getField(Field.PRINT_IDENTIFIER));
      assertEquals(TdbTestUtil.DEFAULT_EISSN_2, t.getField(Field.ONLINE_IDENTIFIER));
      // Title 2
      t = titles.get(1);
      assertEquals(NumberUtil.toArabicNumber(TdbTestUtil.RANGE_2_START),
          t.getField(Field.DATE_FIRST_ISSUE_ONLINE));
      assertEquals(NumberUtil.toArabicNumber(TdbTestUtil.RANGE_2_END),
          t.getField(Field.DATE_LAST_ISSUE_ONLINE));
      assertEquals("", t.getField(Field.NUM_FIRST_VOL_ONLINE));
      assertEquals("", t.getField(Field.NUM_LAST_VOL_ONLINE));
      assertEquals(TdbTestUtil.DEFAULT_ISSN_2, t.getField(Field.PRINT_IDENTIFIER));
      assertEquals(TdbTestUtil.DEFAULT_EISSN_2, t.getField(Field.ONLINE_IDENTIFIER));
      
      // Test the method again with a range which goes to the present - end values should be empty
      title = TdbTestUtil.makeRangeToNowTestTitle();
      titles = KbartConverter.convertTitleToKbartTitles(title);
      assertEquals("Range to now yields too many KBART titles", 1, titles.size());
      // Title with a range to now
      t = titles.get(0);
      assertEquals("First date wrong", TdbTestUtil.RANGE_TO_NOW_START, t.getField(Field.DATE_FIRST_ISSUE_ONLINE));
      assertEquals("Last date wrong", "", t.getField(Field.DATE_LAST_ISSUE_ONLINE));
      assertEquals("First vol wrong", TdbTestUtil.RANGE_TO_NOW_START_VOL, t.getField(Field.NUM_FIRST_VOL_ONLINE));
      assertEquals("Last vol wrong", "", t.getField(Field.NUM_LAST_VOL_ONLINE));
      assertEquals(TdbTestUtil.DEFAULT_ISSN_3, t.getField(Field.PRINT_IDENTIFIER));
      assertEquals(TdbTestUtil.DEFAULT_EISSN_3, t.getField(Field.ONLINE_IDENTIFIER));
      
      // Test with a title which has volume info as well as year ranges; no coverage gap by volume
      title = TdbTestUtil.makeRangeTestTitle(true);
      titles = KbartConverter.convertTitleToKbartTitles(title);
      assertEquals("Coverage gap found when none exists in volume field; only years.", 1, titles.size());
      t = titles.get(0);
      assertEquals(NumberUtil.toArabicNumber(TdbTestUtil.RANGE_1_START), 
          t.getField(Field.DATE_FIRST_ISSUE_ONLINE));
      assertEquals(NumberUtil.toArabicNumber(TdbTestUtil.RANGE_2_END),
                   t.getField(Field.DATE_LAST_ISSUE_ONLINE));
      assertEquals(TdbTestUtil.RANGE_1_START_VOL,
                   t.getField(Field.NUM_FIRST_VOL_ONLINE));
      assertEquals(TdbTestUtil.RANGE_2_END_VOL, 
                   t.getField(Field.NUM_LAST_VOL_ONLINE));
      assertEquals(TdbTestUtil.DEFAULT_ISSN_2, t.getField(Field.PRINT_IDENTIFIER));
      assertEquals(TdbTestUtil.DEFAULT_EISSN_2, t.getField(Field.ONLINE_IDENTIFIER));
      
    } catch (TdbException e) {
      fail("Exception while making range test title: "+e);
    }
  }

  /**
   * Test that valid/invalid publication dates are recognised.
   */
  public final void testIsPublicationDateString() {
    int now = Calendar.getInstance().get(Calendar.YEAR);
    String[] invalidDates = new String[] {
        ""+(KbartConverter.MIN_PUB_DATE-1),
        "1999a",
        "3000",
        "notdate",
        "1st September 200",
        "01/01/1950a",
        "200",
        ""+(now+1+KbartConverter.MAX_FUTURE_PUB_DATE)
    };
    for (String d : invalidDates) {
      assertFalse(d+" should not be a valid publication date", KbartConverter.isPublicationDate(d));
    }
    String[] validDates = new String[] {
        ""+KbartConverter.MIN_PUB_DATE,
        "1999",
        "2000",
        //"1st September 2000",
        //"01/01/1950",
        ""+now,
        ""+(now-1),
        ""+(now+KbartConverter.MAX_FUTURE_PUB_DATE)
    };
    for (String d : validDates) {
      assertTrue(d+" should be a valid publication date", KbartConverter.isPublicationDate(d));
    }
  }

  /**
   * Make a listof mock AUs.
   * @return
   */
  List<ArchivalUnit> makeMockAuList() {
    // Test with dummy AUs
    MockLockssDaemon daemon = getMockLockssDaemon();
    Plugin plugin = new MockPlugin();
    ArchivalUnit au1 = new MockArchivalUnit(plugin, auid1);
    ArchivalUnit au2 = new MockArchivalUnit(plugin, auid2);
    ArchivalUnit au3 = new MockArchivalUnit(plugin, auid3);
    List<ArchivalUnit> aus = Arrays.asList(au1, au2, au3);
    for (ArchivalUnit au : aus) {
      daemon.setNodeManager(new MockNodeManager(), au);
    }
    return aus;
  }

}
