/*
 * $Id$
 */

/*

 Copyright (c) 2012-2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.exporter.counter;

import java.util.Collection;

/**
 * A title used in COUNTER reports.
 * 
 * @version 1.0
 * 
 */
public abstract class BaseCounterReportsTitle implements CounterReportsTitle {
  // The DOI of the title.
  private final String doi;

  // The name of the title.
  private final String name;

  // The name of the publisher of the title.
  private final String publisherName;

  // The name of the publishing platform.
  private final String publishingPlatform;

  // The proprietary identifiers.
  private final Collection<String> proprietaryIds;

  // The ISBN of a book.
  protected String isbn = null;

  // The ISSN of a book.
  protected String issn = null;

  // The online ISSN of a journal.
  protected String onlineIssn = null;

  // The print ISSN of a journal.
  protected String printIssn = null;

  /**
   * Constructor.
   * 
   * @param name
   *          A String with the name of the title.
   * @param publisherName
   *          A String with the name of the publisher of the title.
   * @param publishingPlatform
   *          A String with the name of the publishing platform.
   * @param doi
   *          A String with the DOI of the title.
   * @param proprietaryIds
   *          A Collection<String> with the proprietary identifiers.
   * @throws IllegalArgumentException
   *           if the name of the title is empty.
   */
  protected BaseCounterReportsTitle(String name, String publisherName,
      String publishingPlatform, String doi, Collection<String> proprietaryIds)
      throws IllegalArgumentException {
    if (name == null || name.trim().length() == 0) {
      throw new IllegalArgumentException("Name cannot be empty.");
    }

    this.name = name;
    this.publisherName = publisherName;
    this.publishingPlatform = publishingPlatform;
    this.doi = doi;
    this.proprietaryIds = proprietaryIds;
  }

  /**
   * Provides the DOI of the title.
   * 
   * @return a String with the DOI of the title.
   */
  @Override
  public String getDoi() {
    return doi;
  }

  /**
   * Provides the ISBN of the title if it is a book.
   * 
   * @return a String with the ISBN of the title.
   */
  @Override
  public String getIsbn() {
    return isbn;
  }

  /**
   * Provides the ISSN of the title if it is a book.
   * 
   * @return a String with the ISSN of the title.
   */
  @Override
  public String getIssn() {
    return issn;
  }

  /**
   * Provides the name of the title.
   * 
   * @return a String with the name of the title.
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Provides the online ISSN of the title if it is a journal.
   * 
   * @return a String with the online ISSN of the title.
   */
  @Override
  public String getOnlineIssn() {
    return onlineIssn;
  }

  /**
   * Provides the print ISSN of the title if it is a journal.
   * 
   * @return a String with the print ISSN of the title.
   */
  @Override
  public String getPrintIssn() {
    return printIssn;
  }

  /**
   * Provides the proprietary identifiers of the title.
   * 
   * @return a Collection<String> with the proprietary identifiers of the title.
   */
  @Override
  public Collection<String> getProprietaryIds() {
    return proprietaryIds;
  }

  /**
   * Provides the publisher name of the title.
   * 
   * @return a String with the publisher name of the title.
   */
  @Override
  public String getPublisherName() {
    return publisherName;
  }

  /**
   * Provides the publishing platform name of the title.
   * 
   * @return a String with the publishing platform name of the title.
   */
  @Override
  public String getPublishingPlatform() {
    return publishingPlatform;
  }

  @Override
  public String toString() {
    return "BaseCounterReportsTitle [doi=" + doi + ", name=" + name
	+ ", publisherName=" + publisherName + ", publishingPlatform="
	+ publishingPlatform + ", proprietaryIds=" + proprietaryIds + ", isbn="
	+ isbn + ", issn=" + issn + ", onlineIssn=" + onlineIssn
	+ ", printIssn=" + printIssn + "]";
  }
}
