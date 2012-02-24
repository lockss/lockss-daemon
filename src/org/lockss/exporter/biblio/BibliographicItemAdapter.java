/*
 * $Id: BibliographicItemAdapter.java,v 1.4 2012-02-24 15:39:57 easyonthemayo Exp $
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

import org.lockss.util.MetadataUtil;
import org.lockss.util.NumberUtil;

/**
 * A partial implementation of the <code>BibliographicItem</code> interface, to
 * simplify basic implementations. Defines a String variable for each property,
 * all of which are initialised to null by default, along with getters. Setters
 * are supplied which return the BibliographicItem so they can be chained.
 * There is also a default implementation of {@link #getIssn} which prioritises
 * available ISSNs in the preferred order by default. Additionally, ISSNs are
 * validated before they are set.
 * <p>
 * The default method implementations consult and update the String variables
 * as the definitive source of data. If extensions of this class take a
 * different approach, for example by wrapping an object, they should be careful
 * to provide all the appropriate plumbing in the setters and getters so that
 * the object is kept up to date and consistent.
 * <p>
 * In order to maintain consistency between full volume/year/issue strings and
 * start and end values, the start and end values are set within the setter of
 * each full string. This means the getter can just return the internal value.
 * Volume, year and issue strings can include comma/semicolon-separated lists
 * of ranges.
 *
 * @author Neil Mayo
 */
public abstract class BibliographicItemAdapter implements BibliographicItem {

  protected String printIssn = null;
  protected String eIssn = null;
  protected String issnL = null;
  protected String journalTitle = null;
  protected String publisherName = null;
  protected String name = null;
  protected String volume = null;
  protected String year = null;
  protected String issue = null;
  protected String startVolume = null;
  protected String endVolume = null;
  protected String startYear = null;
  protected String endYear = null;
  protected String startIssue = null;
  protected String endIssue = null;

  // Getters (implementing the interface)

  /**
   * Returns a representative ISSN for the bibliographic item. This may be any
   * available ISSN, but the order of preference is ISSN-L, then eISSN, and
   * finally print ISSN.
   *
   * @return an ISSN for the bibliographic item
   */
  public String getIssn() {
    String theIssn = getIssnL();
    if (theIssn == null) {
      theIssn = getEissn();
      if (theIssn == null) {
        theIssn = getPrintIssn();
      }
    }
    return theIssn;
  }

  public String getPrintIssn() {
    return printIssn;
  }

  public String getEissn() {
    return eIssn;
  }

  public String getIssnL() {
    return issnL;
  }

  public String getJournalTitle() {
    return journalTitle;
  }

  public String getPublisherName() {
    return publisherName;
  }

  public String getName() {
    return name;
  }

  public String getVolume() {
    return volume;
  }

  public String getYear() {
    return year;
  }

  public String getIssue() {
    return issue;
  }

  public String getStartVolume() {
    // Return the start volume if available, or get it from the volume string.
    //return startVolume == null ? NumberUtil.getRangeStart(volume) : startVolume;
    return startVolume;
  }

  public String getEndVolume() {
    // Return the end volume if available, or get it from the volume string.
    //return endVolume == null ? NumberUtil.getRangeEnd(volume) : endVolume;
    return endVolume;
  }

  public String getStartYear() {
    return startYear;
  }

  public String getEndYear() {
    return endYear;
  }

  public String getStartIssue() {
    return startIssue;
  }

  public String getEndIssue() {
    return endIssue;
  }


  // Setters (chainable)
  public BibliographicItemAdapter setPrintIssn(String printIssn) {
    if (MetadataUtil.isIssn(printIssn)) this.printIssn = printIssn;
    return this;
  }

  public BibliographicItemAdapter setEissn(String eIssn) {
    if (MetadataUtil.isIssn(eIssn)) this.eIssn = eIssn;
    return this;
  }

  public BibliographicItemAdapter setIssnL(String issnL) {
    if (MetadataUtil.isIssn(issnL)) this.issnL = issnL;
    return this;
  }

  public BibliographicItemAdapter setJournalTitle(String journalTitle) {
    this.journalTitle = journalTitle;
    return this;
  }

  public BibliographicItemAdapter setPublisherName(String publisherName) {
    this.publisherName = publisherName;
    return this;
  }

  public BibliographicItemAdapter setName(String name) {
    this.name = name;
    return this;
  }

  public BibliographicItemAdapter setVolume(String volume) {
    this.volume = volume;
    // Set the start and end volumes from this string to maintain internal consistency
    if (volume!=null) {
      this.setStartVolume(BibliographicUtil.getRangeSetStart(volume));
      this.setEndVolume(BibliographicUtil.getRangeSetEnd(volume));
    }
    return this;
  }

  public BibliographicItemAdapter setYear(String year) {
    this.year = year;
    // Set the start and end years from this string to maintain internal consistency
    if (year!=null) {
      this.setStartYear(BibliographicUtil.getRangeSetStart(year));
      this.setEndYear(BibliographicUtil.getRangeSetEnd(year));
    }
    return this;
  }

  public BibliographicItemAdapter setIssue(String issue) {
    this.issue = issue;
    // Set the start and end issues from this string to maintain internal consistency
    if (issue!=null) {
      this.setStartIssue(BibliographicUtil.getRangeSetStart(issue));
      this.setEndIssue(BibliographicUtil.getRangeSetEnd(issue));
    }
    return this;
  }

  public BibliographicItemAdapter setStartVolume(String startVolume) {
    this.startVolume = startVolume;
    return this;
  }

  public BibliographicItemAdapter setEndVolume(String endVolume) {
    this.endVolume = endVolume;
    return this;
  }

  public BibliographicItemAdapter setStartYear(String startYear) {
    this.startYear = startYear;
    return this;
  }

  public BibliographicItemAdapter setEndYear(String endYear) {
    this.endYear = endYear;
    return this;
  }

  public BibliographicItemAdapter setStartIssue(String startIssue) {
    this.startIssue = startIssue;
    return this;
  }

  public BibliographicItemAdapter setEndIssue(String endIssue) {
    this.endIssue = endIssue;
    return this;
  }

}
