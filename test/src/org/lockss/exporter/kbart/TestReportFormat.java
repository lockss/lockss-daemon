/*
 * $Id $
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.test.LockssTestCase;
import org.lockss.util.StringUtil;

import java.util.*;

/**
 * @author Neil Mayo
 */
public class TestReportFormat extends LockssTestCase {

  KbartTitle title1, title2, title3, title1diffEnd;
  List<KbartTitle> titles;
  private static final String SPLIT_TITLE = "Multiple Range Title";
  private static final String SINGLE_RNG_TITLE = "Single Unbroken Range Title";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // First title range
    this.title1 = TestKbartTitle.createKbartTitle(
        new HashMap<Field, String>() {{
          put(Field.PUBLICATION_TITLE, SPLIT_TITLE);
          put(Field.DATE_FIRST_ISSUE_ONLINE, "2001");
          put(Field.DATE_LAST_ISSUE_ONLINE, "2003");
          put(Field.NUM_FIRST_VOL_ONLINE, "1");
          put(Field.NUM_LAST_VOL_ONLINE, "3");
          //put(Field.NUM_FIRST_ISSUE_ONLINE, TdbTestUtil.RANGE_1_START);
          //put(Field.NUM_LAST_ISSUE_ONLINE, TdbTestUtil.RANGE_1_END);
        }}
    );
    // Duplicate with different end values
    title1diffEnd = new KbartTitle(title1)
        .setField(Field.DATE_LAST_ISSUE_ONLINE, "2009")
        .setField(Field.NUM_LAST_VOL_ONLINE, "9")
    ;
    // Second range for same title
    title2 = new KbartTitle(title1)
        .setField(Field.DATE_FIRST_ISSUE_ONLINE, "2007")
        .setField(Field.DATE_LAST_ISSUE_ONLINE, "2009")
        .setField(Field.NUM_FIRST_VOL_ONLINE, "7")
        .setField(Field.NUM_LAST_VOL_ONLINE, "9")
    ;
    // Third range, for distinct title
    this.title3 = TestKbartTitle.createKbartTitle(
        new HashMap<Field, String>() {{
          put(Field.PUBLICATION_TITLE, SINGLE_RNG_TITLE);
          put(Field.DATE_FIRST_ISSUE_ONLINE, "2011");
          put(Field.DATE_LAST_ISSUE_ONLINE, "2014");
          put(Field.NUM_FIRST_VOL_ONLINE, "5");
          put(Field.NUM_LAST_VOL_ONLINE, "8");
        }}
    );
    // List of titles - one broken range and one whole range
    titles = Arrays.asList(title1, title2, title3);
  }

  public void testProcess() throws Exception {
    List<KbartTitle> processed;

    // KBART process should yield same number of titles regardless of the
    // coverage_notes format
    for (CoverageNotesFormat cnf : CoverageNotesFormat.values()) {
      processed = ReportFormat.process(titles, cnf, ReportFormat.ReportDataFormat.KBART);
      assertEquals(titles.size(), processed.size());
      // Covnotes field should contain no range seps in any format
      for (KbartTitle kbt : processed) {
        String covnotes = kbt.getField(Field.COVERAGE_NOTES);
        assertEquals(-1, covnotes.indexOf(cnf.rngSep));
      }
    }

    // Titles processes should yield 1 less title
    for (ReportFormat.ReportDataFormat rdf : EnumSet.of(
        ReportFormat.ReportDataFormat.TITLES/*,
        ReportFormat.ReportDataFormat.SFX*/
        // XXX Temp disabled SFX
    )) {
      for (CoverageNotesFormat cnf : CoverageNotesFormat.values()) {
        processed = ReportFormat.process(titles, cnf, rdf);
        assertEquals(titles.size()-1, processed.size());
        final String rngSep = cnf.rngSep;
        // Summary covnotes format should contain no range seps; otherwise, the
        // SPLIT_TITLE should have 1 sep and 2 ranges
        for (KbartTitle kbt : processed) {
          String covnotes = kbt.getField(Field.COVERAGE_NOTES);
          // NOTE If ReportDataFormat is
          if (cnf.isSummary ||
              kbt.getField(Field.PUBLICATION_TITLE).equals(SINGLE_RNG_TITLE)) {
            assertEquals(-1, covnotes.indexOf(rngSep));
          } else if (kbt!=title3) {
            // There should be exactly 1 range sep and 2 ranges
            assertTrue(covnotes.indexOf(rngSep) >= 0);
          } else {
            // There should be exactly 1 range and no seps for title3
            assertEquals(-1, covnotes.indexOf(rngSep));
          }
        }
      }
    }

    // TODO Check for bracketed volumes where necessary (and lack of them when not available)
    // TODO Check SFX format
    // TODO Check that contiguous title ranges when amalgamated yield a single cov_notes range
    // TODO Check that title ranges in wrong order are not successfully amalgamated
  }


  public void testUpdateEndValues() throws Exception {
    // Titles 1 and 1diffEnd differ only on start/end values
    ReportFormat.updateEndValues(title1diffEnd, title1);
    assertEquals(title1diffEnd, title1);
    // Titles 1 and 2 differ on start values too
    ReportFormat.updateEndValues(title2, title1);
    assertNotEquals(title2, title1);
    ReportFormat.updateEndValues(title1, title2);
    assertNotEquals(title1, title2);
  }

  /**
   * Test ReportDataFormat.byName.
   */
  public void testReportDataFormat() throws Exception {
    // Check case insensitivity of byName()
    assertEquals(ReportFormat.ReportDataFormat.KBART,
        ReportFormat.ReportDataFormat.byName("KbArT"));
    assertEquals(ReportFormat.ReportDataFormat.KBART,
        ReportFormat.ReportDataFormat.byName("kbart"));
    assertEquals(ReportFormat.ReportDataFormat.KBART,
        ReportFormat.ReportDataFormat.byName("KBART"));
    // Check default byName return (null)
    assertNull(ReportFormat.ReportDataFormat.byName("blah"));
    // Check default byName return (specified in call)
    assertEquals(ReportFormat.ReportDataFormat.KBART,
        ReportFormat.ReportDataFormat.byName("blah",
            ReportFormat.ReportDataFormat.KBART));
  }

  // TitleCoverageRanges.add some titles, reset, constructCoverageNotes
  public void testTitleCoverageRanges() throws Exception {
    KbartTitle combinedTitle, prevCombinedTitle;
    EnumSet<Field> startFields = EnumSet.of(Field.DATE_FIRST_ISSUE_ONLINE,
        Field.NUM_FIRST_VOL_ONLINE, Field.NUM_FIRST_ISSUE_ONLINE);
    EnumSet<Field> endFields = EnumSet.of(Field.DATE_LAST_ISSUE_ONLINE,
        Field.NUM_LAST_VOL_ONLINE, Field.NUM_LAST_ISSUE_ONLINE);

    // Fields to check when comparing a single title with a combined title
    // from TitleCoverageRanges object
    EnumSet<Field> combinedCheckFields = EnumSet.of(
        Field.DATE_FIRST_ISSUE_ONLINE,
        Field.DATE_LAST_ISSUE_ONLINE,
        Field.NUM_FIRST_ISSUE_ONLINE,
        Field.NUM_LAST_ISSUE_ONLINE,
        Field.NUM_FIRST_VOL_ONLINE,
        Field.NUM_LAST_VOL_ONLINE,
        Field.PUBLICATION_TITLE,
        Field.PUBLISHER_NAME
    );

    // Create with format (which format does not matter unless we are using the
    // coverageNotes field via the constructCoverageNotes() or
    // getCombinedTitle() methods
    ReportFormat.TitleCoverageRanges tcr =
        new ReportFormat.TitleCoverageRanges(CoverageNotesFormat.YEAR);

    // Add first title; id and range vals should equal those in the combined
    // title. Note the combined title also has a coverage_notes field value
    // which is not shared by the first title.
    tcr.add(title1);
    combinedTitle = tcr.getCombinedTitle();
    for (Field f : combinedCheckFields) {
      assertEquals(title1.getFieldValue(f), combinedTitle.getFieldValue(f));
    }


    // Add other titles; end fields should equal the combined title's end fields
    // while the start fields match those of the first title
    for (KbartTitle title : Arrays.asList(title2, title3)) {
      // Add the title to the TCR and retrieve the new combinedTitle
      tcr.add(title);
      prevCombinedTitle = combinedTitle;
      combinedTitle = tcr.getCombinedTitle();
      // Update the end values in prev title, and compare whole thing to the
      // new combined title
      ReportFormat.updateEndValues(title, prevCombinedTitle);
      assertEquals(prevCombinedTitle, combinedTitle);
      // Check each start field matches title1
      for (Field f : startFields)
        assertEquals(title1.getField(f), combinedTitle.getField(f));
      // Check each end field matches the recently added title
      for (Field f : endFields)
        assertEquals(title.getField(f), combinedTitle.getField(f));
    }

    // Reset and add a title
    tcr.reset();
    assertNull(tcr.getCombinedTitle());
    tcr.add(title1);
    assertNotNull(tcr.getCombinedTitle());
    combinedTitle = tcr.getCombinedTitle();
    for (Field f : combinedCheckFields) {
      assertEquals(title1.getFieldValue(f), combinedTitle.getFieldValue(f));
    }

    // Reset with base title
    tcr.reset(title3);
    assertNotNull(tcr.getCombinedTitle());
    combinedTitle = tcr.getCombinedTitle();
    for (Field f : combinedCheckFields) {
      assertEquals(title3.getField(f), combinedTitle.getField(f));
      assertEquals(title3.getFieldValue(f), combinedTitle.getFieldValue(f));
    }
  }

}
