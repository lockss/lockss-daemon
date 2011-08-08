package org.lockss.exporter.kbart;

import java.util.*;

import org.lockss.config.*;
import org.lockss.config.Tdb.TdbException;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.test.LockssTestCase;
import org.lockss.util.NumberUtil;


/**
 * Try some conversions from TdbTitles to KbartTitles. This is done using a single
 * TdbTitle via <code>TdbTestUtil.makeRangeTestTitle()</code>. The <code>extractAllTitles()</code> 
 * method is not exercised; instead we just run <code>createKbartTitles()</code> once per TdbTitle.
 * 
 * @author neil
 *
 */
public class TestKbartConverter extends LockssTestCase {

  //private KbartConverter conv;
  //private Tdb tdb;
  
  protected void setUp() throws Exception {
    super.setUp();
    //this.tdb = TdbTestUtil.makeTestTdb();
    //this.conv = new KbartConverter(tdb);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    //this.conv = null;
  }

  // TODO Unwritten test
  public final void testExtractAllTitles() {
    //fail("Not yet implemented");
    // extractAllTitles() loops through titles for each publisher and runs createKbartTitles()
  }

  public final void testGetAuCoverageRanges() {
    // TODO Unwritten test for private method
  }
  
  public final void testGetAuVols() {
    // TODO Unwritten test for private method
  }
  
  public final void testGetAuYears() {
    // TODO Unwritten test for private method 
  }

  // Check that the result of the wrapper method is the same as calling createKbartTitles
  // on a single title.
  public final void testConvertTitles() {
    List<TdbTitle> l = Collections.emptyList();
    assertEmpty(KbartConverter.convertTitles(null));
    assertEmpty(KbartConverter.convertTitles(l));
    try {
      final TdbTitle title = TdbTestUtil.makeRangeTestTitle(false);
      assertIsomorphic(KbartConverter.createKbartTitles(title), 
	  KbartConverter.convertTitles(new Vector(){{ add(title); }})
      );
    } catch (TdbException e) {
      fail("Exception while making range test title: "+e);
    }
  }

  public final void testConvertAus() {
    Map<TdbTitle, List<ArchivalUnit>> emptyMap = Collections.emptyMap();
    assertEmpty(KbartConverter.convertAus(null));
    assertEmpty(KbartConverter.convertAus(emptyMap));

    // TODO Test a list of AUs (how to create dummy AUs?)
    final List<ArchivalUnit> ausNull = null;
    final List<ArchivalUnit> ausEmpty = Collections.emptyList();
    // Test null
    assertIsomorphic(KbartConverter.createKbartTitles(ausNull), 
	KbartConverter.convertAus(TdbUtil.mapTitlesToAus(ausNull))
    );
    assertIsomorphic(KbartConverter.createKbartTitles(ausNull), 
	KbartConverter.convertAus(null)
    );
    // Test empty list
    assertIsomorphic(KbartConverter.createKbartTitles(ausEmpty), 
	KbartConverter.convertAus(TdbUtil.mapTitlesToAus(ausEmpty))
    );
    // Test empty map against map with empty value lists
    try {
      Map<TdbTitle, List<ArchivalUnit>> emptyAusMap = new HashMap<TdbTitle, List<ArchivalUnit>>() {{
	put(TdbTestUtil.makeRangeTestTitle(false), ausEmpty);
	put(TdbTestUtil.makeRangeTestTitle(false), ausEmpty);
      }};
      assertIsomorphic(KbartConverter.createKbartTitles(ausEmpty), 
	  KbartConverter.convertAus(emptyAusMap)
      );
    } catch (TdbException e) {
      fail("Exception while making range test title: "+e);
    }
  }

  // Test the thin wrapper method which converts a Collection of AUs to a 
  // List of TdbAus before passing to the main createKbartTitles()
  public final void testCreateKbartTitlesCollectionAus() {
    // If there are no AUs, an empty list should be returned
    Collection<ArchivalUnit> aus = Collections.emptyList();
    assertEmpty(KbartConverter.createKbartTitles(aus));
    // TODO Test with dummy AUs
  }

  // Test the thin wrapper method which gets a List of TdbAus from a TdbTitle  
  // before passing to the main createKbartTitles()
  public final void testCreateKbartTitlesListTdbAus() {
    // If there are no TdbAus, an empty list should be returned
    List<TdbAu> noTdbAus = Collections.emptyList();
    assertEmpty(KbartConverter.createKbartTitles(noTdbAus));
    // NOTE Testing with dummy TdbAus is handled by testCreateKbartTitlesTdbTitle() 
    // createKbartTitlesTdbTitle() merely extracts TdbAus from a dummy title 
  }

  // Test the main createKbartTitles() method
  public final void testCreateKbartTitlesTdbTitle() {
    try {
      // If there is no TdbTitle, or no TdbAus in the title, an empty list should be returned
      TdbTitle nullTitle = null;
      assertEmpty(KbartConverter.createKbartTitles(nullTitle));
      assertEmpty(KbartConverter.createKbartTitles(TdbTestUtil.makeTitleWithNoAus("test title!")));

      // Title without volume info; just year ranges leading to a coverage gap
      TdbTitle title = TdbTestUtil.makeRangeTestTitle(false);
      List<KbartTitle> titles = KbartConverter.createKbartTitles(title);
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
      log.critical("first issue: " + t.getField(Field.DATE_FIRST_ISSUE_ONLINE));
      log.critical("last issue: " + t.getField(Field.DATE_LAST_ISSUE_ONLINE));
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
      titles = KbartConverter.createKbartTitles(title);
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
      titles = KbartConverter.createKbartTitles(title);
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

  public final void testIsPublicationDate() {
    int now = Calendar.getInstance().get(Calendar.YEAR);
    String[] invalidDates = new String[] {""+(KbartConverter.MIN_PUB_DATE-1), "1999a", "3000", "notdate", ""+(now+1)};
    for (String d : invalidDates) {
      assertFalse(d+" should not be a valid publication date", KbartConverter.isPublicationDate(d));
    }
    String[] validDates = new String[] {""+KbartConverter.MIN_PUB_DATE, "1999", "2000", ""+now, ""+(now-1)};
    for (String d : validDates) {
      assertTrue(d+" should be a valid publication date", KbartConverter.isPublicationDate(d));
    }
  }  
  
}
