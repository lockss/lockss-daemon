package org.lockss.exporter.kbart;

import java.util.Calendar;
import java.util.List;

import org.lockss.config.Tdb;
import org.lockss.config.TdbPublisher;
import org.lockss.config.TdbTestUtil;
import org.lockss.config.TdbTitle;
import org.lockss.config.Tdb.TdbException;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.test.LockssTestCase;

/**
 * Try some conversions from TdbTitles to KbartTitles. This is done using a single
 * TdbTitle via <code>TdbTestUtil.makeRangeTestTitle()</code>. The <code>extractAllTitles()</code> 
 * method is not exercised; instead we just run <code>createKbartTitles()</code> once per TdbTitle.
 * 
 * @author neil
 *
 */
public class TestKbartConverter extends LockssTestCase {

  private KbartConverter conv;
  private Tdb tdb;
  
  protected void setUp() throws Exception {
    super.setUp();
    //this.tdb = TdbTestUtil.makeTestTdb();
    this.conv = new KbartConverter(tdb);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    this.conv = null;
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

  public final void testCreateKbartTitles() {
    try {
      // Title without volume info; just year ranges leading to a coverage gap
      TdbTitle title = TdbTestUtil.makeRangeTestTitle(false);
      List<KbartTitle> titles = conv.createKbartTitles(title);
      assertEquals(2, titles.size());
      // Check the dates and vols have been correctly transferred in each title
      KbartTitle t = titles.get(0);
      assertEquals(t.getField(Field.DATE_FIRST_ISSUE_ONLINE), TdbTestUtil.RANGE_1_START);
      assertEquals(t.getField(Field.DATE_LAST_ISSUE_ONLINE), TdbTestUtil.RANGE_1_END);
      assertEquals(t.getField(Field.NUM_FIRST_VOL_ONLINE), "");
      assertEquals(t.getField(Field.NUM_LAST_VOL_ONLINE), "");
      assertEquals(t.getField(Field.PRINT_IDENTIFIER), TdbTestUtil.DEFAULT_ISSN_2);
      assertEquals(t.getField(Field.ONLINE_IDENTIFIER), TdbTestUtil.DEFAULT_EISSN_2);
      // Title 2
      t = titles.get(1);
      assertEquals(t.getField(Field.DATE_FIRST_ISSUE_ONLINE), TdbTestUtil.RANGE_2_START);
      assertEquals(t.getField(Field.DATE_LAST_ISSUE_ONLINE), TdbTestUtil.RANGE_2_END);
      assertEquals(t.getField(Field.NUM_FIRST_VOL_ONLINE), "");
      assertEquals(t.getField(Field.NUM_LAST_VOL_ONLINE), "");
      assertEquals(t.getField(Field.PRINT_IDENTIFIER), TdbTestUtil.DEFAULT_ISSN_2);
      assertEquals(t.getField(Field.ONLINE_IDENTIFIER), TdbTestUtil.DEFAULT_EISSN_2);
      
      // Test the method again with a range which goes to the present - end values should be empty
      title = TdbTestUtil.makeRangeToNowTestTitle();
      titles = conv.createKbartTitles(title);
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
      titles = conv.createKbartTitles(title);
      assertEquals("Coverage gap found when none exists in volume field; only years.", 1, titles.size());
      t = titles.get(0);
      assertEquals(t.getField(Field.DATE_FIRST_ISSUE_ONLINE), TdbTestUtil.RANGE_1_START);
      assertEquals(t.getField(Field.DATE_LAST_ISSUE_ONLINE), TdbTestUtil.RANGE_2_END);
      assertEquals(t.getField(Field.NUM_FIRST_VOL_ONLINE), TdbTestUtil.RANGE_1_START_VOL);
      assertEquals(t.getField(Field.NUM_LAST_VOL_ONLINE), TdbTestUtil.RANGE_2_END_VOL);
      assertEquals(t.getField(Field.PRINT_IDENTIFIER), TdbTestUtil.DEFAULT_ISSN_2);
      assertEquals(t.getField(Field.ONLINE_IDENTIFIER), TdbTestUtil.DEFAULT_EISSN_2);
      
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
