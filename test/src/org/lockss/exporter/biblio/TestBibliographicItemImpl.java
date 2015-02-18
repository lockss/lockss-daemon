/*
 * $Id$
 */

/*

Copyright (c) 2011-2014 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.test.LockssTestCase;

/**
 * @author Neil Mayo
 */
public class TestBibliographicItemImpl extends LockssTestCase {

  private final static String badIsbn = "54321";
  private final static String pIsbn = "978-1-58562-257-3";
  private final static String eIsbn = "1-58562-340-2";

  private final static String badIssn = "12345";
  private final static String pIssn = "0148-2076";
  private final static String eIssn = "1533-8606";
  private final static String issnL = "1556-326X";
  private final static String title = "A Journal";
  private final static String[] titleIds = {"A JournalId"};
  private final static String publisher = "A Publisher";
  private final static String name = "A Journal Volume X";
  private final static String volume = "s1-s10";
  private final static String year = "2001-2010";
  private final static String issue = "iss01-iss20";
  private final static String sVol = "s1";
  private final static String eVol = "s10";
  private final static String sYear = "2001";
  private final static String eYear = "2010";
  private final static String sIssue = "iss01";
  private final static String eIssue = "iss20";
  private final static String publicationType = "bookSeries";
  private final static String coverageDepth = "abstracts";
  
  // Secondary volume/year/isue for testing changes; different to above
  private final static String volume2 = "5-9";
  private final static String sVol2 = "5";
  private final static String eVol2 = "9";
  private final static String year2 = " 1976 - 1980 ";
  private final static String sYear2 = "1976";
  private final static String eYear2 = "1980";
  private final static String issue2 = "i1-i9";
  private final static String sIssue2 = "i1";
  private final static String eIssue2 = "i9";

  // A BibliographicItem constructed using vol/year/issue convenience strings
  private BibliographicItemImpl bibItem1 =
    (BibliographicItemImpl) new BibliographicItemImpl()
    .setPrintIsbn(pIsbn)
    .setPrintIssn(pIssn)
    .setPublicationTitle(title)
    .setProprietaryIds(titleIds)
    .setPublisherName(publisher)
    .setName(name)
    .setVolume(volume)
    .setYear(year)
    .setIssue(issue)
    .setPublicationType(publicationType)
    .setCoverageDepth(coverageDepth);
  
  // A BibliographicItem constructed using vol/year/issue start and end strings
  private BibliographicItemImpl bibItem2 = (
    BibliographicItemImpl) new BibliographicItemImpl()
    .setPrintIsbn(pIsbn)
    .setPrintIssn(pIssn)
    .setPublicationTitle(title)
    .setProprietaryIds(titleIds)
    .setPublisherName(publisher)
    .setName(name)
    .setStartVolume(sVol)
    .setEndVolume(eVol)
    .setStartYear(sYear) 
    .setEndYear(eYear)
    .setStartIssue(sIssue) 
    .setEndIssue(eIssue)
    .setPublicationType(publicationType)
    .setCoverageDepth(coverageDepth);      

  private BibliographicItemImpl bibItemCopy  = new BibliographicItemImpl(bibItem1);
  private BibliographicItemImpl bibItemClone = bibItem1.clone();

  /**
   * Check that the constructors have set appropriate dependent variables.
   */
  public void testConstructors() {
    assertEquals(publicationType, bibItem1.getPublicationType());
    assertEquals(coverageDepth, bibItem1.getCoverageDepth());
    
    // Start/end values should be set from the single strings
    assertEquals(sVol, bibItem1.getStartVolume());
    assertEquals(eVol, bibItem1.getEndVolume());
    assertEquals(sYear, bibItem1.getStartYear());
    assertEquals(eYear, bibItem1.getEndYear());
    assertEquals(sIssue, bibItem1.getStartIssue());
    assertEquals(eIssue, bibItem1.getEndIssue());

    // Start/end values should not inform the single strings, which are really
    // just a convenience
    assertNull(bibItem2.getVolume());
    assertNull(bibItem2.getYear());
    assertNull(bibItem2.getIssue());

    // Check that all the fields have been transferred, by checking generated
    // hashCode and equals methods. If these tests fail, you probably need to
    // regenerate the methods to account for new fields.
    assertEquals(bibItem1, bibItemCopy);
    assertEquals(bibItem1, bibItemClone);
    assertEquals(bibItemCopy, bibItemClone);
    assertTrue(bibItem1.equals(bibItemCopy));
    assertTrue(bibItem1.equals(bibItemClone));
    assertTrue(bibItemCopy.equals(bibItemClone));
    assertEquals(bibItem1.hashCode(), bibItemCopy.hashCode());
    assertEquals(bibItem1.hashCode(), bibItemClone.hashCode());
    assertEquals(bibItemCopy.hashCode(), bibItemClone.hashCode());
  }

  /**
   * ISBN should be the preferred available ISBN
   * (eISBN, then ISBN).
   */
  public void testGetIsbn() {
    assertEquals(pIsbn, bibItem1.getIsbn());
    ((BibliographicItemImpl)bibItem1).setEisbn(eIsbn);
    assertEquals(eIsbn, bibItem1.getIsbn());
  }

  /**
   * ISSN should be the preferred available ISSN
   * (ISSN-L, then eISSN, and finally print ISSN).
   */
  public void testGetIssn() {
    assertEquals(pIssn, bibItem1.getIssn());
    ((BibliographicItemImpl)bibItem1).setEissn(eIssn);
    assertEquals(eIssn, bibItem1.getIssn());
    ((BibliographicItemImpl)bibItem1).setIssnL(issnL);
    assertEquals(issnL, bibItem1.getIssn());
  }

  /**
   * Setting the volume, year or issue strings should affect the start and end
   * values.
   */
  public void testVolumeYearIssueSetters() {
    // Set a new volume/year/issue string and see that the start and end change
    ((BibliographicItemImpl)bibItem2).setVolume(volume2);
    assertEquals(sVol2, bibItem2.getStartVolume());
    assertEquals(eVol2, bibItem2.getEndVolume());

    ((BibliographicItemImpl)bibItem2).setYear(year2);
    assertEquals(sYear2, bibItem2.getStartYear());
    assertEquals(eYear2, bibItem2.getEndYear());

    ((BibliographicItemImpl)bibItem2).setIssue(issue2);
    assertEquals(sIssue2, bibItem2.getStartIssue());
    assertEquals(eIssue2, bibItem2.getEndIssue());
  }

}
