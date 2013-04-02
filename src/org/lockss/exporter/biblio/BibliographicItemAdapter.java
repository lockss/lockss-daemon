/*
 * $Id: BibliographicItemAdapter.java,v 1.9 2013-04-02 11:01:14 easyonthemayo Exp $
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

  protected String printIsbn = null;
  protected String eIsbn = null;
  protected String printIssn = null;
  protected String eIssn = null;
  protected String issnL = null;
  protected String journalTitle = null;
  protected String proprietaryId = null;
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
  protected String publicationType = null;
  protected String coverageDepth = null;

  //////////////////////////////////////////////////////////////////////////////
  // Getters (implementing the interface)
  //////////////////////////////////////////////////////////////////////////////

  public String getPrintIsbn() {
    return printIsbn;
  }

  public String getEisbn() {
    return eIsbn;
  }

  public String getIsbn() {
    String isbn = getEisbn();
    if (!MetadataUtil.isIsbn(isbn)) {
      isbn = getPrintIsbn();
      if (!MetadataUtil.isIsbn(isbn)) {
        isbn = null;
      }
    }
    return isbn;
  }

  /**
   * Returns a representative ISSN for the bibliographic item. This may be any
   * available ISSN, but the order of preference is ISSN-L, then eISSN, and
   * finally print ISSN.
   *
   * @return an ISSN for the bibliographic item
   */
  @Override
  public String getIssn() {
    String theIssn = getIssnL();
    if (!MetadataUtil.isIssn(theIssn)) {
      theIssn = getEissn();
      if (!MetadataUtil.isIssn(theIssn)) {
        theIssn = getPrintIssn();
        if (!MetadataUtil.isIssn(theIssn)) {
          theIssn = null;
        }
      }
    }
    return theIssn;
  }
  
  @Override
  public String getPublicationType() {
    return (publicationType == null) ? "journal" : publicationType;
  }
  
  @Override
  public String getCoverageDepth() {
    return (coverageDepth == null) ? "fulltext" : coverageDepth;
  }
  

  @Override
  public String getPrintIssn() {
    return printIssn;
  }

  @Override
  public String getEissn() {
    return eIssn;
  }

  @Override
  public String getIssnL() {
    return issnL;
  }

  @Override
  public String getProprietaryId() {
    return proprietaryId;
  }

  @Override
  public String getJournalTitle() {
    return journalTitle;
  }

  @Override
  public String getPublisherName() {
    return publisherName;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getVolume() {
    return volume;
  }

  @Override
  public String getYear() {
    return year;
  }

  @Override
  public String getIssue() {
    return issue;
  }

  @Override
  public String getStartVolume() {
    // Return the start volume if available, or get it from the volume string.
    //return startVolume == null ? NumberUtil.getRangeStart(volume) : startVolume;
    return startVolume;
  }

  @Override
  public String getEndVolume() {
    // Return the end volume if available, or get it from the volume string.
    //return endVolume == null ? NumberUtil.getRangeEnd(volume) : endVolume;
    return endVolume;
  }

  @Override
  public String getStartYear() {
    return startYear;
  }

  @Override
  public String getEndYear() {
    return endYear;
  }

  @Override
  public String getStartIssue() {
    return startIssue;
  }

  @Override
  public String getEndIssue() {
    return endIssue;
  }

  //////////////////////////////////////////////////////////////////////////////
  // Setters (chainable)
  //////////////////////////////////////////////////////////////////////////////
  public BibliographicItemAdapter setPrintIsbn(String printIsbn) {
    if (MetadataUtil.isIsbn(printIsbn)) this.printIsbn = printIsbn;
    return this;
  }

  public BibliographicItemAdapter setEisbn(String eIsbn) {
    if (MetadataUtil.isIsbn(eIsbn)) this.eIsbn = eIsbn;
    return this;
  }

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

  public BibliographicItemAdapter setProprietaryId(String proprietaryId) {
    this.proprietaryId = proprietaryId;
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

  public BibliographicItemAdapter setPublicationType(String publicationType) {
    this.publicationType = publicationType;
    return this;
  }

  public BibliographicItemAdapter setCoverageDepth(String coverageDepth) {
    this.coverageDepth = coverageDepth;
    return this;
  }

  //////////////////////////////////////////////////////////////////////////////
  // Automatically generated equals and hashCode methods.
  // These should be regenerated if more fields are added.
  //////////////////////////////////////////////////////////////////////////////

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BibliographicItemAdapter that = (BibliographicItemAdapter) o;

    if (publicationType != null ? !publicationType.equals(that.publicationType) : that.publicationType != null)
      return false;
    if (coverageDepth != null ? !coverageDepth.equals(that.coverageDepth) : that.coverageDepth != null)
      return false;
    if (eIsbn != null ? !eIsbn.equals(that.eIsbn) : that.eIsbn != null)
      return false;
    if (eIssn != null ? !eIssn.equals(that.eIssn) : that.eIssn != null)
      return false;
    if (endIssue != null ? !endIssue.equals(that.endIssue) : that.endIssue != null)
      return false;
    if (endVolume != null ? !endVolume.equals(that.endVolume) : that.endVolume != null)
      return false;
    if (endYear != null ? !endYear.equals(that.endYear) : that.endYear != null)
      return false;
    if (issnL != null ? !issnL.equals(that.issnL) : that.issnL != null)
      return false;
    if (issue != null ? !issue.equals(that.issue) : that.issue != null)
      return false;
    if (journalTitle != null ? !journalTitle.equals(that.journalTitle) : that.journalTitle != null)
      return false;
    if (name != null ? !name.equals(that.name) : that.name != null)
      return false;
    if (printIsbn != null ? !printIsbn.equals(that.printIsbn) : that.printIsbn != null)
      return false;
    if (printIssn != null ? !printIssn.equals(that.printIssn) : that.printIssn != null)
      return false;
    if (publisherName != null ? !publisherName.equals(that.publisherName) : that.publisherName != null)
      return false;
    if (startIssue != null ? !startIssue.equals(that.startIssue) : that.startIssue != null)
      return false;
    if (startVolume != null ? !startVolume.equals(that.startVolume) : that.startVolume != null)
      return false;
    if (startYear != null ? !startYear.equals(that.startYear) : that.startYear != null)
      return false;
    if (volume != null ? !volume.equals(that.volume) : that.volume != null)
      return false;
    if (year != null ? !year.equals(that.year) : that.year != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = printIsbn != null ? printIsbn.hashCode() : 0;
    result = 31 * result + (eIsbn != null ? eIsbn.hashCode() : 0);
    result = 31 * result + (printIssn != null ? printIssn.hashCode() : 0);
    result = 31 * result + (eIssn != null ? eIssn.hashCode() : 0);
    result = 31 * result + (issnL != null ? issnL.hashCode() : 0);
    result = 31 * result + (journalTitle != null ? journalTitle.hashCode() : 0);
    result = 31 * result + (publisherName != null ? publisherName.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (volume != null ? volume.hashCode() : 0);
    result = 31 * result + (year != null ? year.hashCode() : 0);
    result = 31 * result + (issue != null ? issue.hashCode() : 0);
    result = 31 * result + (startVolume != null ? startVolume.hashCode() : 0);
    result = 31 * result + (endVolume != null ? endVolume.hashCode() : 0);
    result = 31 * result + (startYear != null ? startYear.hashCode() : 0);
    result = 31 * result + (endYear != null ? endYear.hashCode() : 0);
    result = 31 * result + (startIssue != null ? startIssue.hashCode() : 0);
    result = 31 * result + (endIssue != null ? endIssue.hashCode() : 0);
    result = 31 * result + (publicationType != null ? publicationType.hashCode() : 0);
    result = 31 * result + (coverageDepth != null ? coverageDepth.hashCode() : 0);
    return result;
  }
}
