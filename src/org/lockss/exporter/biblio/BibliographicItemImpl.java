/*
 * $Id: BibliographicItemImpl.java,v 1.3 2012-07-19 11:54:42 easyonthemayo Exp $
 */

/*

Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * A basic implementation of the <code>BibliographicItem</code> interface which
 * is mutable. All bibliographic data is passed in at construction, though
 * values may be null. Extends the {@link BibliographicItemAdapter}, so has
 * setters which return the BibliographicItem so they can be chained.
 * Values are set in the constructors using the adapter's methods.
 *
 * @author Neil Mayo
 */
public class BibliographicItemImpl extends BibliographicItemAdapter {

  /**
   * Create a BibliographicItem with no properties.
   */
  public BibliographicItemImpl() {}

  /**
   * Create a BibliographicItem with the supplied field values. Values may
   * be null. Start and end values for volumes, years and issues are retrieved
   * from the full strings which may contain ranges.
   * @param printIsbn
   * @param printIssn
   * @param journalTitle
   * @param name
   * @param volume
   * @param year
   * @param issue
   */
  public BibliographicItemImpl(String printIsbn, String printIssn,
                               String journalTitle,
                               String publisherName, String name,
                               String volume, String year, String issue) {
    setPrintIsbn(printIsbn);
    setPrintIssn(printIssn);
    setJournalTitle(journalTitle);
    setPublisherName(publisherName);
    setName(name);
    setVolume(volume);
    setYear(year);
    setIssue(issue);
  }

  /**
   * Create a BibliographicItem with the supplied field values. Values may
   * be null. Start and end values for volumes, years and issues are explicitly
   * supplied.
   * @param printIsbn
   * @param printIssn
   * @param journalTitle
   * @param name
   * @param startVolume
   * @param endVolume
   * @param startYear
   * @param endYear
   * @param startIssue
   * @param endIssue
   */
  public BibliographicItemImpl(String printIsbn, String printIssn,
                               String journalTitle,
                               String publisherName, String name,
                               String startVolume, String endVolume,
                               String startYear, String endYear,
                               String startIssue, String endIssue) {
    setPrintIsbn(printIsbn);
    setPrintIssn(printIssn);
    setJournalTitle(journalTitle);
    setPublisherName(publisherName);
    setName(name);
    setStartVolume(startVolume);
    setEndVolume(endVolume);
    setStartYear(startYear);
    setEndYear(endYear);
    setStartIssue(startIssue);
    setEndIssue(endIssue);
  }

}
