/*
 * $Id: TestKbartTdbAuUtil.java,v 1.8 2011-09-23 13:23:15 easyonthemayo Exp $
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

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.Tdb;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbTestUtil;
import org.lockss.config.TdbTitle;
import org.lockss.config.Tdb.TdbException;
import org.lockss.exporter.kbart.KbartTdbAuUtil.AuInfoType;
import org.lockss.exporter.kbart.KbartTdbAuUtil.IssueFormat;
import org.lockss.test.LockssTestCase;
import org.lockss.util.NumberUtil;

public class TestKbartTdbAuUtil extends LockssTestCase {

  TdbAu au1;
  Tdb tdb;  
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
  private TdbAu afrTod, afrTodEquiv, afrTodDiffVol, afrTodDiffYear, 
      afrTodDiffName, afrTodDiffUrl, afrTodNullYear;
  private List<TdbAu> afrTodAus = Arrays.asList(afrTod, afrTodEquiv,
      afrTodDiffVol, afrTodDiffYear, afrTodDiffName, afrTodDiffUrl,
      afrTodNullYear);
  
  /**
   * Create a test Tdb structure.
   */
  protected void setUp() throws Exception {
    super.setUp();
    tdb = TdbTestUtil.makeTestTdb();
    assertNotNull("Tdb is null", tdb);
    // Set up some AUs for the equivalence tests
    try {
      // 2 equivalent AUs
      afrTod  = TdbTestUtil.createBasicAu("Africa Today",     "1999", "46");
      afrTodEquiv  = TdbTestUtil.createBasicAu("Africa Today",     "1999", "46");
      // 2 AUs differing from the previous 2 by one field each
      afrTodDiffVol  = TdbTestUtil.createBasicAu("Africa Today",     "1999", "47");
      afrTodDiffYear  = TdbTestUtil.createBasicAu("Africa Today",     "2000", "46");
      afrTodDiffName   = TdbTestUtil.createBasicAu("Africa Yesterday", "1999", "46");
      // An AU differing from the first pair on a non-primary field
      afrTodDiffUrl   = TdbTestUtil.createBasicAu("Africa Today",     "1999", "46");
      TdbTestUtil.setParam(afrTodDiffUrl, KbartTdbAuUtil.DEFAULT_TITLE_URL_ATTR, "http://whocares.com");
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
    tdb = null;
  }

  public void testAreEquivalent() {
    assertTrue(KbartTdbAuUtil.areEquivalent(afrTod, afrTod));
    assertTrue(KbartTdbAuUtil.areEquivalent(afrTod, afrTodEquiv));
    assertTrue(KbartTdbAuUtil.areEquivalent(afrTod, afrTodDiffUrl));
    assertTrue(KbartTdbAuUtil.areEquivalent(afrTodDiffUrl, afrTod));
    // Should return false if any field or arg is null
    assertFalse(KbartTdbAuUtil.areEquivalent(null, null));
    assertFalse(KbartTdbAuUtil.areEquivalent(afrTod, afrTodNullYear));
    assertFalse(KbartTdbAuUtil.areEquivalent(afrTodNullYear, afrTod));
    
    assertFalse(KbartTdbAuUtil.areEquivalent(afrTod, afrTodDiffVol));
    assertFalse(KbartTdbAuUtil.areEquivalent(afrTod, afrTodDiffYear));
    
    assertFalse(KbartTdbAuUtil.areEquivalent(afrTod, afrTodDiffName));
  }
 
  /**
   * Either the ISSNs or the names are the same. In this case all the ISSNs 
   * are the same.
   */
  public void testHaveSameIdentity() {
    assertTrue(KbartTdbAuUtil.haveSameIdentity(afrTod, afrTod));
    assertTrue(KbartTdbAuUtil.haveSameIdentity(afrTod, afrTodEquiv));
    assertTrue(KbartTdbAuUtil.haveSameIdentity(afrTod, afrTodDiffUrl));
    // Should throw exception if any arg is null
    try {
      KbartTdbAuUtil.haveSameIdentity(null, null);
      fail("Should throw exception with null TdbAus.");
    } catch (NullPointerException e) {
      // expected
    }
    // The null field is irrelevant to the comparison 
    assertTrue(KbartTdbAuUtil.haveSameIdentity(afrTod, afrTodNullYear));
    // These have the same ISSN, despite the other difference
    assertTrue(KbartTdbAuUtil.haveSameIdentity(afrTod, afrTodDiffName));
    assertTrue(KbartTdbAuUtil.haveSameIdentity(afrTod, afrTodDiffVol));
    assertTrue(KbartTdbAuUtil.haveSameIdentity(afrTod, afrTodDiffYear));
    assertTrue(KbartTdbAuUtil.haveSameIdentity(afrTodDiffYear, afrTodDiffVol));
    assertTrue(KbartTdbAuUtil.haveSameIdentity(afrTodDiffName, afrTodDiffYear));
    // Null field discounts year; issns still match
    assertTrue(KbartTdbAuUtil.haveSameIdentity(afrTodNullYear, afrTodDiffYear));
    assertTrue(KbartTdbAuUtil.haveSameIdentity(afrTodNullYear, afrTodDiffVol));
  }

  /**
   * Check volume and year. Each available pair of non-null values must match. 
   */
  public void testHaveSameIndex() {
    assertTrue(KbartTdbAuUtil.haveSameIndex(afrTod, afrTod));
    assertTrue(KbartTdbAuUtil.haveSameIndex(afrTod, afrTodEquiv));
    assertTrue(KbartTdbAuUtil.haveSameIndex(afrTod, afrTodDiffUrl));
    // Should throw exception if any arg is null
    try {
      KbartTdbAuUtil.haveSameIndex(null, null);
      fail("Should throw exception with null TdbAus.");
    } catch (NullPointerException e) {
      // expected
    }
    // The null field should be omitted from the comparison
    assertTrue(KbartTdbAuUtil.haveSameIndex(afrTod, afrTodNullYear));
    // These only differ on id fields
    assertTrue(KbartTdbAuUtil.haveSameIndex(afrTod, afrTodDiffName));
    // These differ on one of the two available index fields
    assertFalse(KbartTdbAuUtil.haveSameIndex(afrTod, afrTodDiffVol));
    assertFalse(KbartTdbAuUtil.haveSameIndex(afrTod, afrTodDiffYear));
    // These differ on available fields
    assertFalse(KbartTdbAuUtil.haveSameIndex(afrTodDiffYear, afrTodDiffVol));
    assertFalse(KbartTdbAuUtil.haveSameIndex(afrTodDiffName, afrTodDiffYear));
    // Null fields discount year; volumes match
    assertTrue(KbartTdbAuUtil.haveSameIndex(afrTodNullYear, afrTodDiffYear));
    // Null fields discount year; volumes differ
    assertFalse(KbartTdbAuUtil.haveSameIndex(afrTodNullYear, afrTodDiffVol));
  }

  /**
   * The names or ISSNs match, and any of the volume and year fields that are
   * available (non-null) must match.
   */
  public void testAreApparentlyEquivalent() {
    assertTrue(KbartTdbAuUtil.areApparentlyEquivalent(afrTod, afrTod));
    assertTrue(KbartTdbAuUtil.areApparentlyEquivalent(afrTod, afrTodEquiv));
    assertTrue(KbartTdbAuUtil.areApparentlyEquivalent(afrTod, afrTodDiffUrl));
    // Should throw exception if any arg is null
    try {
      KbartTdbAuUtil.areApparentlyEquivalent(null, null);
      fail("Should throw exception with null TdbAus.");
    } catch (NullPointerException e) {
      // expected
    }
    // The null field should be omitted from the comparison
    assertTrue(KbartTdbAuUtil.areApparentlyEquivalent(afrTod, afrTodNullYear));
    // These have the same ISSN, despite the name difference
    assertTrue(KbartTdbAuUtil.areApparentlyEquivalent(afrTod, afrTodDiffName));
    // These differ on one of the two available index fields
    assertFalse(KbartTdbAuUtil.areApparentlyEquivalent(afrTod, afrTodDiffVol));
    assertFalse(KbartTdbAuUtil.areApparentlyEquivalent(afrTod, afrTodDiffYear));
    // These differ on available fields    
    assertFalse(KbartTdbAuUtil.areApparentlyEquivalent(afrTodDiffYear, afrTodDiffVol));
    assertFalse(KbartTdbAuUtil.areApparentlyEquivalent(afrTodDiffName, afrTodDiffYear));
    // Null field discounts year; issn and volume match
    assertTrue(KbartTdbAuUtil.areApparentlyEquivalent(afrTodNullYear, afrTodDiffYear));
    // Null field discounts year; vols differ
    assertFalse(KbartTdbAuUtil.areApparentlyEquivalent(afrTodNullYear, afrTodDiffVol));
  }

  
  public void testCompareStringYears() {
    // Check that the result is right; check for NFE?
    String yr1 = "2000";
    String yr2 = "1999";
    String yr3 = "MMI"; // 2001
    String yr4 = "MCM"; // 1900
    assertTrue(yr1+" is not greater than "+yr2, KbartTdbAuUtil.compareStringYears(yr1, yr2) > 0);
    assertTrue(yr1+" is not less than "+yr2, KbartTdbAuUtil.compareStringYears(yr2, yr1) < 0);
    assertTrue(yr1+" is not equal to "+yr1, KbartTdbAuUtil.compareStringYears(yr1, yr1) == 0);
    assertTrue(yr2+" is not equal to "+yr2, KbartTdbAuUtil.compareStringYears(yr2, yr2) == 0);

    assertTrue(yr3+" is not greater than "+yr2, KbartTdbAuUtil.compareStringYears(yr3, yr2) > 0);
    assertTrue(yr4+" is not less than "+yr2, KbartTdbAuUtil.compareStringYears(yr4, yr1) < 0);
    assertTrue(yr1+" is not equal to "+yr1, KbartTdbAuUtil.compareStringYears(yr1, yr1) == 0);
    assertTrue(yr2+" is not equal to "+yr2, KbartTdbAuUtil.compareStringYears(yr2, yr2) == 0);
}

  /**
   * The AU's property maps are searched for the following, in the order given: 
   * an attribute called "volume"; a parameter called "volume"; a parameter called "volume_name"
   */
  public void testFindVolume() {
    String vol = "Volume 221B";
    try {
      TdbTitle title = TdbTestUtil.makeVolumeTestTitle(vol);

      for (TdbAu au: title.getTdbAus()) {
        String foundVol = KbartTdbAuUtil.findVolume(au);
        assertNotNull(foundVol);
        // The return should be a non-empty string with the correct value
        assertFalse(au+" Volume not found", foundVol.equals(""));
        assertTrue(au+" Wrong volume found: "+foundVol, foundVol.equals(vol));
      }
    } catch (TdbException e) {
      fail("Exception encountered in testFindVolume() "+e);
      return;
    }
  }

  /**
   * Year should be value or empty string, not null.
   */
  public void testFindYear() {
    // returns attr, param, empty
    TdbTitle title = tdb.getTdbTitleById(TdbTestUtil.DEFAULT_TITLE_ID);
    assertNotNull(title);
    TdbAu au;
    
    // test empty AU
    au = title.getTdbAusByName("basicTitleEmptyAu").iterator().next();
    String foundYear = KbartTdbAuUtil.findYear(au);
    assertNotNull(foundYear);
    assertTrue(foundYear.equals(""));
    
    // test basic AU, which should have DEFAULT_YEAR
    au = title.getTdbAusByName("basicTitleAu").iterator().next();
    foundYear= KbartTdbAuUtil.findYear(au);
    assertNotNull(foundYear);
    assertFalse(foundYear.equals(""));
    assertTrue(foundYear.equals(TdbTestUtil.DEFAULT_YEAR));
  }

  
  /**
   * Test on AUs which have an ISSN, which fall back to the title id, or which are empty.
   * AUs should now have nothing recorded if they lack a specified ISSN (NM 17-01-11).
   */
  public void testFindIssn() {
    TdbTitle title = tdb.getTdbTitleById(TdbTestUtil.DEFAULT_TITLE_ID);
    assertNotNull(title);
    TdbAu au;

    // test empty AU, which should default to id
    au = title.getTdbAusByName("basicTitleEmptyAu").iterator().next();
    String foundIssn = KbartTdbAuUtil.findIssn(au);
    assertNotNull(foundIssn);
    //assertFalse(foundIssn.equals(""));
    //assertTrue(foundIssn.equals(au.getTdbTitle().getId()));

    // test basic AU, which should have DEFAULT_ISSN_1
    au = title.getTdbAusByName("basicTitleAu").iterator().next();
    foundIssn = KbartTdbAuUtil.findIssn(au);
    assertNotNull(foundIssn);
    //assertFalse(foundIssn.equals(""));
    assertTrue(foundIssn.equals(TdbTestUtil.DEFAULT_ISSN_1));
  }

  /**
   * Test on AUs which have an EISSN, which fall back to the title id, or which are empty.
   * AUs should now have nothing recorded if they lack a specified ISSN (NM 17-01-11).
   */
  public void testFindEissn() {
    TdbTitle title = tdb.getTdbTitleById(TdbTestUtil.DEFAULT_TITLE_ID);
    assertNotNull(title);
    TdbAu au;

    // test empty AU, which should default to id
    au = title.getTdbAusByName("basicTitleEmptyAu").iterator().next();
    String foundEissn = KbartTdbAuUtil.findEissn(au);
    assertNotNull(foundEissn);
    //assertFalse(foundEissn.equals(""));
    //assertTrue(foundEissn.equals(au.getTdbTitle().getId()));

    // test basic AU, which should have DEFAULT_ISSN_1
    au = title.getTdbAusByName("basicTitleAu").iterator().next();
    foundEissn = KbartTdbAuUtil.findEissn(au);
    assertNotNull(foundEissn);
    //assertFalse(foundEissn.equals(""));
    assertTrue(foundEissn.equals(TdbTestUtil.DEFAULT_EISSN_1));
  }

  /**
   * This is exercised as part of the other findThing() methods. 
   */
  public void testFindAuInfo() {
    //fail("Not yet implemented");
    // calls findMapValue on the appropriate map
    //String findAuInfo(TdbAu au, String key, AuInfoType type) {
  }

  /**
   * Attempts to find a map key by searching for the supplied key in 
   * each AuInfoType's map, in the order they are enumerated.
   */
  public void testFindAuInfoType() {
    String vol = "a volume";
    TdbTitle title;
    try {
      title = TdbTestUtil.makeVolumeTestTitle(vol);
    } catch (TdbException e) {
      fail("Exception encountered making volume test title: "+e);
      return;
    }

    // Try and find AU info type for each AU
    for (TdbAu au: title.getTdbAus()) {
      AuInfoType type1 = KbartTdbAuUtil.findAuInfoType(au, TdbTestUtil.DEFAULT_VOLUME_KEY);
      AuInfoType type2 = KbartTdbAuUtil.findAuInfoType(au, TdbTestUtil.DEFAULT_VOLUME_NAME_KEY);
      //assertNotNull("AuInfoType for "+au+" is "+type, type);
      assertTrue(type1!=null || type2!=null);
    }

    // Now test empty AU
    title = tdb.getTdbTitleById(TdbTestUtil.DEFAULT_TITLE_ID);
    assertNotNull(title);
    // test empty AU
    TdbAu au = title.getTdbAusByName("basicTitleEmptyAu").iterator().next();
    assertNull(KbartTdbAuUtil.findAuInfoType(au, "volume"));
    assertNull(KbartTdbAuUtil.findAuInfoType(au, "issue"));
    assertNull(KbartTdbAuUtil.findAuInfoType(au, "blabla"));
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
                   ""+KbartTdbAuUtil.getFirstYearAsInt(au));
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
          ""+KbartTdbAuUtil.getLastYearAsInt(au));
    }
  }

  /**
   *
   */
  public void testGetLatestYear() {
    try {
      String name = "A Title";
      // Single AU
      assertEquals(1, KbartTdbAuUtil.getLatestYear(
          Arrays.asList(
              TdbTestUtil.createBasicAu(name, "1")
          )
      ));
      // Several AUs, highest year at end
      assertEquals(3, KbartTdbAuUtil.getLatestYear(
          Arrays.asList(
              TdbTestUtil.createBasicAu(name, "1"),
              TdbTestUtil.createBasicAu(name, "2"),
              TdbTestUtil.createBasicAu(name, "3")
          )
      ));
      // Several AUs, duplicated highest
      assertEquals(1, KbartTdbAuUtil.getLatestYear(
          Arrays.asList(
              TdbTestUtil.createBasicAu(name, "1"),
              TdbTestUtil.createBasicAu(name, "1"),
              TdbTestUtil.createBasicAu(name, "1")
          )
      ));
      assertEquals(10, KbartTdbAuUtil.getLatestYear(
          Arrays.asList(
              TdbTestUtil.createBasicAu(name, "1"),
              TdbTestUtil.createBasicAu(name, "10"),
              TdbTestUtil.createBasicAu(name, "9"),
              TdbTestUtil.createBasicAu(name, "8"),
              TdbTestUtil.createBasicAu(name, "7")
          )
      ));
      // Several AUs, highest year in middle
      assertEquals(3000, KbartTdbAuUtil.getLatestYear(
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
   * Exercise the IssueFormat enum - for each format, create a matching AU and try to extract the 
   * issue string and parse it for first and last years.
   * Also tests identifyIssueFormat(). 
   */
  public void testIssueFormat() {
    // test extractFirstLastIssues, getFirstIssue, getLastIssue
    // with different aus - create one with each type of key, one with spurious key (yielding null format)
    TdbAu au;

    try {
      for(IssueFormatTest fmt : IssueFormatTest.values()) {
        String s = fmt.issueString;
        IssueFormat ifmt = fmt.issueFormat;
        au = TdbTestUtil.makeIssueTestAu(ifmt.getKey(), s);
        // Get an issue format and check it is not null and matches what is expected
        IssueFormat foundIfmt = KbartTdbAuUtil.identifyIssueFormat(au);
        String fmtName = foundIfmt.name();
        assertNotNull(foundIfmt);
        assertEquals(ifmt, foundIfmt);
        assertEquals(foundIfmt.extractFirstLastIssues(au).length, 2);
        assertEquals(fmtName+" IssueFormat problem.", fmt.firstIssue, foundIfmt.getFirstIssue(au));
        assertEquals(fmtName+" IssueFormat problem.", fmt.lastIssue, foundIfmt.getLastIssue(au));
      }      
      // Now test one with an incompatible issue format
      au = TdbTestUtil.makeIssueTestAu("not_a_real_issue_key", "not_a_real_issue_format");
      assertNull( KbartTdbAuUtil.identifyIssueFormat(au) );
    } catch (TdbException e) {
      fail("Exception encountered while making AU with issue parameter "+e);
      return;    
    }
  }

  /**
   * A utility enum for testing - groups together an IssueFormat, an issue string on which to test it,
   * and the expected start and end issue strings that should be parsed from the string.
   * 
   * @author Neil Mayo
   *
   */
  static enum IssueFormatTest {
    
    IFT2  (IssueFormat.ISSUE_FORMAT_2,  issNum1, issNum2, issNumRng),
    IFT3  (IssueFormat.ISSUE_FORMAT_3,  iss1,    iss2,    issLst),
    IFT4  (IssueFormat.ISSUE_FORMAT_4,  issNum1, issNum2, issNumRng),
    IFT5  (IssueFormat.ISSUE_FORMAT_5,  iss1,    iss2,    issLst),
    IFT6a (IssueFormat.ISSUE_FORMAT_6a, issNum1, issNum2, issNumRng),
    IFT6b (IssueFormat.ISSUE_FORMAT_6b, issStr,  issStr,  issStr);
    
    IssueFormatTest(IssueFormat i, String f, String l, String s) {
      this.issueFormat = i;
      this.firstIssue = f;
      this.lastIssue = l;
      this.issueString = s;
    }

    IssueFormat issueFormat;
    String firstIssue, lastIssue;
    String issueString;
    
  }
  
}
