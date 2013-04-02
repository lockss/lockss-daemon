/*
 * $Id: BibliographicItem.java,v 1.6 2013-04-02 11:01:14 easyonthemayo Exp $
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
 * This is the interface for a bibliographic item which is to be used in the
 * <tt>org.lockss.exporter.biblio</tt> package. A bibliographic item represents
 * a volume or range of volumes in a particular title. Clients with proprietary
 * objects representing their bibliographic items can implement this interface
 * either directly or through an adapter class.
 * <p>
 * Implementations may return <tt>null</tt> where values are unavailable.
 * Note that there are accessors for volume, year and issue strings, which may
 * represent ranges, and also for individual start and end values. If the
 * single strings are specified, the start and end accessors should use these
 * strings to provide their data, to maintain consistency. However, the volume,
 * year and issue accessors should return null if no such value is set, rather
 * than attempting to concatenate available start and end values in an arbitrary
 * way. These data are linked and should, as far as possible, not be managed
 * independently. The start and end values should be dependent upon the full
 * values where possible, but this dependency should be unidirectional. It is
 * intended that <code>getStart*</code> and <code>getEnd*</code> methods will be
 * the primary methods used for accessing range data.
 * <p>
 * Note that this interface is currently journal-specific, and may need to be
 * expanded into a set of interfaces or a hierarchy, in order to handle books.
 * Methods have been added to establish the "type" of a BibliographicItem,
 * which may be mapped externally to an enumeration. Currently this is left open
 * so as not to impose an application-specific usage.
 *
 * @author Neil Mayo
 */
public interface BibliographicItem {

  /**
   * Returns a representative ISBN for the bibliographic item. This should be
   * whatever available ISBN is considered most appropriate. This may be any
   * available ISBN, but the order of preference is eISBN, then print ISBN.
   *
   * @return an ISBN for the bibliographic item
   */
  public String getIsbn();

  /**
   * Returns print ISBN for this item. This is what is usually just called "ISBN".
   *
   * @return the print ISBN for this title or <code>null</code> if not specified
   */
  public String getPrintIsbn();

  /**
   * Returns eISBN for this item.
   *
   * @return the eISBN for this title or <code>null</code> if not specified
   */
  public String getEisbn();

  /**
   * Returns a representative ISSN for the bibliographic item. This should be
   * whatever available ISSN is considered most appropriate. This may be any
   * available ISSN, but the order of preference is ISSN-L, then eISSN, and
   * finally print ISSN.
   *
   * @return an ISSN for the bibliographic item
   */
  public String getIssn();

  /**
   * Returns print ISSN for this item. This is what is usually just called "ISSN".
   *
   * @return the print ISSN for this title or <code>null</code> if not specified
   */
  public String getPrintIssn();

  /**
   * Returns eISSN for this item.
   *
   * @return the eISSN for this title or <code>null</code> if not specified
   */
  public String getEissn();

  /**
   * Returns ISSN-L for this item.
   *
   * @return the ISSN-L for this title or <code>null</code> if not specified
   */
  public String getIssnL();

  /**
   * Returns the proprietary ID of of the journal of which the bibliographic 
   * item is a part.
   * @return the proprietary ID of the bibliographic item's journal
   */
  public String getProprietaryId();

  /**
   * Return publication type this AU. Values include "journal" for a journal,
   * "book" if each AU is an individual book that is not part of a series, and
   * "bookSeries" if each AU is an individual book that is part of a series.
   * For a "bookSeries" the journalTitle() is returns the name of the series.
   * For a "book" the journalTitle() is simply descriptive of the collection
   * of books (e.g. "Springer Books") and can be ignored for bibliographic
   * purposes.
   * 
   * @return publication type this title or "journal" if not specified
   */
  public String getPublicationType();
  
  /**
   * Return coverage depth of the content in this AU. Values include "fulltext"
   * for full-text coverage, and "abstracts" for primarily or only "abstracts"
   * coverage.
   * 
   * @return coverage depth this AU or "fulltext" if not specified
   */
  public String getCoverageDepth();
  
  /**
   * Returns the title of the journal of which the bibliographic item is a part.
   * @return the title of the bibliographic item's journal
   */
  public String getJournalTitle();

  /**
   * Returns the name of the publisher who publishes the journal of which the
   * bibliographic item is a part.
   * @return the name of the bibliographic item's publisher
   */
  public String getPublisherName();

  /**
   * Returns a name for the bibliographic item. The name may include volume
   * information as it is intended to represent the specific item.
   * @return the name of the bibliographic item
   */
  public String getName();

  /**
   * Returns a volume for the bibliographic item. This is the full unparsed
   * volume string, for example "1-12" or "vol 1".
   * @return the volume string for the bibliographic item
   */
  public String getVolume();

  /**
   * Returns a year for the bibliographic item. This is a string in the format
   * of four digits, optionally followed by a hyphen and a further four digits,
   * in which case the string represents a range. Whitespace can be included
   * around and between the years and hyphen. For example, "2011", "1999-2001",
   * " 1976 - 2011 ". If the string deviates from this format, it will not
   * be successfully parsed as a date range.
   * @return the year string for the bibliographic item
   */
  public String getYear();

  /**
   * Returns an issue for the bibliographic item. This is the full unparsed
   * issue string, for example "1-12" or "issue 1".
   * @return the issue string for the bibliographic item
   */
  public String getIssue();

  /**
   * Returns a start volume for the bibliographic item. This is the first volume
   * in a range, or the only volume if the bibliographic item represents a
   * single volume. It should match the start of the range available through
   * {@link #getVolume}.
   * @return the start volume of the bibliographic item's range
   */
  public String getStartVolume();

  /**
   * Returns an end volume for the bibliographic item. This is the last volume
   * in a range, or the only volume if the bibliographic item represents a
   * single volume. It should match the end of the range available through
   * {@link #getVolume}.
   * @return the end volume of the bibliographic item's range
   */
  public String getEndVolume();

  /**
   * Returns a start year for the bibliographic item. This is the first year
   * in a range, or the only year if the bibliographic item represents a
   * single volume. It should match the start of the range available through
   * {@link #getYear}.
   * @return the start year of the bibliographic item's range
   */
  public String getStartYear();

  /**
   * Returns an end year for the bibliographic item. This is the first year
   * in a range, or the only year if the bibliographic item represents a
   * single volume. It should match the end of the range available through
   * {@link #getYear}.
   * @return the end year of the bibliographic item's range
   */
  public String getEndYear();

  /**
   * Returns a start issue for the bibliographic item. This is the first issue
   * in a range, or the only issue if the bibliographic item represents a
   * single volume. It should match the start of the range available through
   * {@link #getIssue}.
   * @return the start issue of the bibliographic item's range
   */
  public String getStartIssue();

  /**
   * Returns an end issue for the bibliographic item. This is the first issue
   * in a range, or the only issue if the bibliographic item represents a
   * single volume. It should match the end of the range available through
   * {@link #getIssue}.
   * @return the end issue of the bibliographic item's range
   */
  public String getEndIssue();

}
