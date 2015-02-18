/*
 * $Id$
 */

/*

 Copyright (c) 2013-2014 Board of Trustees of Leland Stanford Jr. University,
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
 * A journal used in COUNTER reports.
 * 
 * @version 1.0
 * 
 */
public class CounterReportsJournal extends BaseCounterReportsTitle {
  /**
   * Constructor.
   * 
   * @param name
   *          A String with the name of the journal.
   * @param publisherName
   *          A String with the name of the publisher of the journal.
   * @param publishingPlatform
   *          A String with the name of the publishing platform.
   * @param doi
   *          A String with the DOI of the journal.
   * @param proprietaryIds
   *          A Collection<String> with the proprietary identifiers.
   * @param printIssn
   *          A String with the print ISSN of the journal.
   * @param onlineIssn
   *          A String with the online ISSN of the journal.
   * @throws IllegalArgumentException
   *           if the name of the title is empty.
   */
  protected CounterReportsJournal(String name, String publisherName,
      String publishingPlatform, String doi, Collection<String> proprietaryIds,
      String printIssn, String onlineIssn) throws IllegalArgumentException {
    super(name, publisherName, publishingPlatform, doi, proprietaryIds);

    this.printIssn = printIssn;
    this.onlineIssn = onlineIssn;
  }
}
