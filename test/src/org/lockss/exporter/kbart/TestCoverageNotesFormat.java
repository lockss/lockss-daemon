/*
 * $Id$
 */

/*

Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.exporter.biblio.BibliographicUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.util.*;


/**
 * Note that single title should reduce to summary.
 *
 * @author Neil Mayo
 */
public class TestCoverageNotesFormat extends LockssTestCase {

  KbartTitle title1, title2, title1novol, title2novol, titleNoRng;
  KbartTitle trng1a, trng1b, trng2a, trng2b, trng3, trng4;
  List<KbartTitle> allTitles, rangeTitles, restrictedRangeTitles;
  StringBuilder sb;
  EnumSet<CoverageNotesFormat> nonSfx =
      EnumSet.complementOf(EnumSet.of(CoverageNotesFormat.SFX));

  @Override
  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated("2014/6/1 00:00:00");
    sb = new StringBuilder();
    title1 = new KbartTitle()
        .setField(Field.PUBLICATION_TITLE, "My Journal Title")
        .setField(Field.DATE_FIRST_ISSUE_ONLINE, "1991")
        .setField(Field.DATE_LAST_ISSUE_ONLINE, "1999")
        .setField(Field.NUM_FIRST_VOL_ONLINE, "1")
        .setField(Field.NUM_LAST_VOL_ONLINE, "9")
       //.setField(Field.NUM_FIRST_ISSUE_ONLINE, TdbTestUtil.RANGE_1_START)
       //.setField(Field.NUM_LAST_ISSUE_ONLINE, TdbTestUtil.RANGE_1_END)
    ;
    // Add current year to title2
    title2 = new KbartTitle(title1)
        .setField(Field.DATE_FIRST_ISSUE_ONLINE, "2001")
        .setField(Field.DATE_LAST_ISSUE_ONLINE, ""+BibliographicUtil.getThisYear())
        .setField(Field.NUM_FIRST_VOL_ONLINE, "11")
        .setField(Field.NUM_LAST_VOL_ONLINE, "19")
    ;
    // Setup copies with no vol data
    title1novol = new KbartTitle(title1)
        .setField(Field.NUM_FIRST_VOL_ONLINE, "")
        .setField(Field.NUM_LAST_VOL_ONLINE, "")
    ;
    title2novol = new KbartTitle(title2)
        .setField(Field.NUM_FIRST_VOL_ONLINE, "")
        .setField(Field.NUM_LAST_VOL_ONLINE, "")
    ;
    // Setup a title with no range data
    titleNoRng = new KbartTitle().setField(Field.PUBLICATION_TITLE, "My Journal Title");

    // Add titles to list
    allTitles = Arrays.asList(title1, title2, title1novol, title2novol, titleNoRng);

    // Set up titles for restrictRanges test
    int gap = CoverageNotesFormat.DEFAULT_RANGE_REDUCTION_THRESHOLD;
    trng1a = new KbartTitle()
        .setField(Field.PUBLICATION_TITLE, "My Journal Title")
        .setField(Field.DATE_FIRST_ISSUE_ONLINE, "1991")
        .setField(Field.DATE_LAST_ISSUE_ONLINE, "1999");
    trng1b = new KbartTitle(trng1a)
        .setField(Field.DATE_FIRST_ISSUE_ONLINE, ""+(1999+gap))
        .setField(Field.DATE_LAST_ISSUE_ONLINE, "2003");
    trng2a = new KbartTitle(trng1a)
        .setField(Field.DATE_FIRST_ISSUE_ONLINE, ""+(2003+gap+1))
        .setField(Field.DATE_LAST_ISSUE_ONLINE, "2007");
    trng2b = new KbartTitle(trng2a)
        .setField(Field.DATE_FIRST_ISSUE_ONLINE, ""+(2007+gap-1))
        .setField(Field.DATE_LAST_ISSUE_ONLINE, ""+BibliographicUtil.getThisYear());
    // Not close enough
    trng3 = new KbartTitle(trng2a)
        .setField(Field.DATE_FIRST_ISSUE_ONLINE, ""+(2012+gap+3))
        .setField(Field.DATE_LAST_ISSUE_ONLINE, "2030");
    // No date data
    trng4 = new KbartTitle(trng2a)
        .setField(Field.DATE_FIRST_ISSUE_ONLINE, "")
        .setField(Field.DATE_LAST_ISSUE_ONLINE, "0");

    // Add titles to list
    rangeTitles = Arrays.asList(trng1a, trng1b, trng2a, trng2b, trng3, trng4);
    restrictedRangeTitles = Arrays.asList(
        new KbartTitle()
            .setField(Field.PUBLICATION_TITLE, "My Journal Title")
            .setField(Field.DATE_FIRST_ISSUE_ONLINE, "1991")
            .setField(Field.DATE_LAST_ISSUE_ONLINE, "2003"),
        new KbartTitle()
            .setField(Field.PUBLICATION_TITLE, "My Journal Title")
            .setField(Field.DATE_FIRST_ISSUE_ONLINE, ""+(2003+gap+1))
            .setField(Field.DATE_LAST_ISSUE_ONLINE, "2030"),
        trng3,
        trng4
    );
  }

  /**
   * Check each non-SFX format adds start val or appropriate length to the
   * string. Check SFX format adds the value somewhere along with the
   * appropriate operator.
   */
  public void testAppendStart() throws Exception {
    for (CoverageNotesFormat cnf : CoverageNotesFormat.values()) {
      // Renew the sb for every new cnf
      sb = new StringBuilder();
      for (KbartTitle title : allTitles) {
        cnf.appendStart(sb, title);
        assertFalse(StringUtil.isNullString(sb.toString()));
        String fldVal = title.getField(Field.DATE_FIRST_ISSUE_ONLINE);
        if (nonSfx.contains(cnf)) {
          int expPos = sb.length() - fldVal.length();
          // If showing vol and it exists, sub the length of the vol plus 2 for the bracket chars
          if (cnf.showVol) {
            int n = title.getField(Field.NUM_FIRST_VOL_ONLINE).length();
            if (n!=0) expPos -= n+2;
          }
          // Check the sb ends with an approp length string
          //System.out.format("Expecting %s in %s\n", fldVal, sb);
          assertEquals(expPos, sb.lastIndexOf(fldVal));
        } else {
          // SFX Format
          assertTrue(sb.indexOf(fldVal) >= 0);
          assertTrue(sb.indexOf(">=") > 0);
        }
      }
    }
  }


  /**
   * Check each non-SFX format adds end val or appropriate length to the
   * string. Check SFX format adds the value somewhere along with the
   * appropriate operator.
   */
  public void testAppendEnd() throws Exception {
    for (CoverageNotesFormat cnf : CoverageNotesFormat.values()) {
      // Renew the sb for every new cnf
      sb = new StringBuilder();
      for (KbartTitle title : allTitles) {
        cnf.appendEnd(sb, title);
        assertFalse(StringUtil.isNullString(sb.toString()));
        String fldVal = title.getField(Field.DATE_LAST_ISSUE_ONLINE);
        if (nonSfx.contains(cnf)) {
          int expPos = sb.length() - fldVal.length();
          // If showing vol and it exists, sub the length of the vol plus 2 for the bracket chars
          if (cnf.showVol) {
            int n = title.getField(Field.NUM_LAST_VOL_ONLINE).length();
            if (n!=0) expPos -= n+2;
          }
          // Check the sb ends with an approp length string
          //System.out.format("Expecting %s in %s\n", fldVal, sb);
          assertEquals(expPos, sb.lastIndexOf(fldVal));
        } else {
          // SFX Format
          assertTrue(sb.indexOf(fldVal) >= 0);
          assertTrue(sb.indexOf("<=") > 0);
        }
      }
    }
  }

  /**
   * Check each non-SFX format adds start val or appropriate length to the
   * string. Check SFX format adds the value somewhere along with the
   * appropriate operator.
   */
  public void testAppendEqualRange() throws Exception {
    for (CoverageNotesFormat cnf : CoverageNotesFormat.values()) {
      sb = new StringBuilder();
      for (KbartTitle title : allTitles) {
        cnf.appendEqualRange(sb, title);
        assertFalse(StringUtil.isNullString(sb.toString()));
        String fldVal = title.getField(Field.DATE_FIRST_ISSUE_ONLINE);
        if (nonSfx.contains(cnf)) {
          int expPos = sb.length() - fldVal.length();
          // If showing vol and it exists, sub the length of the vol plus 2 for the bracket chars
          if (cnf.showVol) {
            int n = title.getField(Field.NUM_FIRST_VOL_ONLINE).length();
            if (n!=0) expPos -= n+2;
          }
          // Check the sb ends with an approp length string
          assertEquals(expPos, sb.lastIndexOf(fldVal));
        } else {
          // SFX Format
          assertTrue(sb.indexOf(fldVal) >= 0);
          assertTrue(sb.indexOf("=") > 0);
          // Should not find either of the gt/lt chars with a =
          assertTrue(sb.indexOf(">=") == -1);
          assertTrue(sb.indexOf("<=") == -1);
          // Should not find the range join in the string
          // (NB Unless it is in the field value)
          assertTrue(sb.indexOf(cnf.rngJoin) == -1);
        }
      }
    }
  }

  /**
   * This method is mainly for the use of the SFX format, which needs to omit
   * the range join string if there is no end point.
   */
  public void testAppendRangeJoin_StringBuilder_Boolean() throws Exception {
    // With true, the results should be the same as appendRangeJoin with no boolean
    for (CoverageNotesFormat cnf : CoverageNotesFormat.values()) {
      sb = new StringBuilder();
      cnf.appendRangeJoin(sb, true);
      assertEquals(0, sb.indexOf(cnf.rngJoin));
    }
    // With false, the result is dependent on the format being used (SFX should
    // omit the join string)
    for (CoverageNotesFormat cnf : CoverageNotesFormat.values()) {
      sb = new StringBuilder();
      cnf.appendRangeJoin(sb, false);
      int pos = cnf==CoverageNotesFormat.SFX ? -1 : 0;
      // (NB Unless the rngJoin is in the field value)
      assertEquals(pos, sb.indexOf(cnf.rngJoin));
    }
  }

  public void testAppendRangeJoin() throws Exception {
    for (CoverageNotesFormat cnf : CoverageNotesFormat.values()) {
      sb = new StringBuilder();
      cnf.appendRangeJoin(sb);
      assertEquals(0, sb.indexOf(cnf.rngJoin));
    }
  }

  public void testAppendRangeSep() throws Exception {
    for (CoverageNotesFormat cnf : CoverageNotesFormat.values()) {
      sb = new StringBuilder();
      cnf.appendRangeSep(sb);
      assertEquals(0, sb.indexOf(cnf.rngSep));
    }
  }

  public void testConstructCoverageNote() throws Exception {
    for (CoverageNotesFormat cnf : CoverageNotesFormat.values()) {
      for (KbartTitle t : allTitles) {
        String s = cnf.constructCoverageNote(t);
        assertNotNull(s);
        // All titles should produce something except for titleNoRng
        if (t==titleNoRng) assertEquals(0, s.length());
        else assertNotEquals(0, s.length());
      }
    }

    // Note that for a single title, non-summary formats should reduce
    // to the equivalent summary format.
    for (KbartTitle t : allTitles) {
      assertEquals(CoverageNotesFormat.YEAR.constructCoverageNote(t),
          CoverageNotesFormat.YEAR_SUMMARY.constructCoverageNote(t));
      assertEquals(CoverageNotesFormat.YEAR_VOLUME.constructCoverageNote(t),
          CoverageNotesFormat.YEAR_VOLUME_SUMMARY.constructCoverageNote(t));
    }

    // Similarly, the YEAR_VOLUME format should reduce to the equivalent
    // YEAR format in the case that there is no volume data in the title
    for (KbartTitle t : Arrays.asList(title1novol, title2novol)) {
      assertEquals(CoverageNotesFormat.YEAR_VOLUME.constructCoverageNote(t),
          CoverageNotesFormat.YEAR.constructCoverageNote(t));
      assertEquals(CoverageNotesFormat.YEAR_VOLUME_SUMMARY.constructCoverageNote(t),
          CoverageNotesFormat.YEAR_SUMMARY.constructCoverageNote(t));
    }
  }

  public void testRestrictRanges() throws Exception {
    for (CoverageNotesFormat cnf : CoverageNotesFormat.values()) {
      assertIsomorphic(restrictedRangeTitles, cnf.restrictRanges(rangeTitles));
    }
  }

  public void testByName() throws Exception {
    // Check case insensitivity of byName()
    assertEquals(CoverageNotesFormat.SFX,
        CoverageNotesFormat.byName("SfX"));
    assertEquals(CoverageNotesFormat.SFX,
        CoverageNotesFormat.byName("sfx"));
    assertEquals(CoverageNotesFormat.SFX,
        CoverageNotesFormat.byName("SFX"));
    // Check default byName returns (null)
    assertNull(CoverageNotesFormat.byName(null));
    assertNull(CoverageNotesFormat.byName(" SFX "));
    assertNull(CoverageNotesFormat.byName("blah"));
  }

  public void testByName_String_CoverageNotesFormat() throws Exception {
    // Check default byName return (specified in call)
    assertEquals(CoverageNotesFormat.SFX,
        CoverageNotesFormat.byName("blah",
            CoverageNotesFormat.SFX));
    assertEquals(CoverageNotesFormat.SFX,
        CoverageNotesFormat.byName(null,
            CoverageNotesFormat.SFX));
  }

}
