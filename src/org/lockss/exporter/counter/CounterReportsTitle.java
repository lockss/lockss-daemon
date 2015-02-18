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
 * Representation of a title used in COUNTER reports.
 * 
 * @version 1.0
 * 
 */
public interface CounterReportsTitle {

  /**
   * Provides the DOI of the title.
   * 
   * @return a String with the DOI of the title.
   */
  String getDoi();

  /**
   * Provides the ISBN of the title if it is a book.
   * 
   * @return a String with the ISBN of the title.
   */
  String getIsbn();

  /**
   * Provides the ISSN of the title if it is a book.
   * 
   * @return a String with the ISSN of the title.
   */
  String getIssn();

  /**
   * Provides the name of the title.
   * 
   * @return a String with the name of the title.
   */
  String getName();

  /**
   * Provides the online ISSN of the title if it is a journal.
   * 
   * @return a String with the online ISSN of the title.
   */
  String getOnlineIssn();

  /**
   * Provides the print ISSN of the title if it is a journal.
   * 
   * @return a String with the print ISSN of the title.
   */
  String getPrintIssn();

  /**
   * Provides the proprietary identifiers of the title.
   * 
   * @return a Collection<String> with the proprietary identifiers of the title.
   */
  Collection<String> getProprietaryIds();

  /**
   * Provides the publisher name of the title.
   * 
   * @return a String with the publisher name of the title.
   */
  String getPublisherName();

  /**
   * Provides the publishing platform name of the title.
   * 
   * @return a String with the publishing platform name of the title.
   */
  String getPublishingPlatform();
}
