/*
 * $Id$
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.exporter.biblio.BibliographicItemAdapter;

/**
 * A BibliographicItem that wraps a KbartTitle to provide read/write access to
 * its fields. Name, and compound fields for volume, issue and year, are not
 * recorded in KbartTitles, so the getters for these properties return null,
 * while their setters will set the internal value in the Adapter but also
 * update the KbartTitle's start and end values based on them.
 *
 * @author Neil Mayo
 */
public class BibliographicKbartTitle extends BibliographicItemAdapter {

  private final KbartTitle kbt;

  public BibliographicKbartTitle(KbartTitle kbt) {
    this.kbt = kbt;
  }

  @Override
  public String getPrintIssn() {
    return kbt.getField(KbartTitle.Field.PRINT_IDENTIFIER);
  }

  @Override
  public String getEissn() {
    return kbt.getField(KbartTitle.Field.ONLINE_IDENTIFIER);
  }

  @Override
  public String getPublicationTitle() {
    return kbt.getField(KbartTitle.Field.PUBLICATION_TITLE);
  }

  @Override
  public String getPublisherName() {
    return kbt.getField(KbartTitle.Field.PUBLISHER_NAME);
  }

  @Override
  public String getStartVolume() {
    return kbt.getField(KbartTitle.Field.NUM_FIRST_VOL_ONLINE);
  }

  @Override
  public String getEndVolume() {
    return kbt.getField(KbartTitle.Field.NUM_LAST_VOL_ONLINE);
  }

  @Override
  public String getStartYear() {
    return kbt.getField(KbartTitle.Field.DATE_FIRST_ISSUE_ONLINE);
  }

  @Override
  public String getEndYear() {
    return kbt.getField(KbartTitle.Field.DATE_LAST_ISSUE_ONLINE);
  }

  @Override
  public String getStartIssue() {
    return kbt.getField(KbartTitle.Field.NUM_FIRST_ISSUE_ONLINE);
  }

  @Override
  public String getEndIssue() {
    return kbt.getField(KbartTitle.Field.NUM_LAST_ISSUE_ONLINE);
  }

  @Override
  public String getCoverageDepth() {
    return kbt.getField(KbartTitle.Field.COVERAGE_DEPTH);
  }

  @Override
  public BibliographicItemAdapter setPrintIssn(String printIssn) {
    kbt.setField(KbartTitle.Field.PRINT_IDENTIFIER, printIssn);
    return this;
  }

  @Override
  public BibliographicItemAdapter setEissn(String eIssn) {
    kbt.setField(KbartTitle.Field.ONLINE_IDENTIFIER, eIssn);
    return this;
  }

  @Override
  public BibliographicItemAdapter setPublicationTitle(String publicationTitle) {
    kbt.setField(KbartTitle.Field.PUBLICATION_TITLE, publicationTitle);
    return this;
  }

  @Override
  public BibliographicItemAdapter setPublisherName(String publisherName) {
    kbt.setField(KbartTitle.Field.PUBLISHER_NAME, publisherName);
    return this;
  }

  @Override
  public BibliographicItemAdapter setStartVolume(String startVolume) {
    kbt.setField(KbartTitle.Field.NUM_FIRST_VOL_ONLINE, startVolume);
    return this;
  }

  @Override
  public BibliographicItemAdapter setEndVolume(String endVolume) {
    kbt.setField(KbartTitle.Field.NUM_LAST_VOL_ONLINE, endVolume);
    return this;
  }

  @Override
  public BibliographicItemAdapter setStartYear(String startYear) {
    kbt.setField(KbartTitle.Field.DATE_FIRST_ISSUE_ONLINE, startYear);
    return this;
  }

  @Override
  public BibliographicItemAdapter setEndYear(String endYear) {
    kbt.setField(KbartTitle.Field.DATE_LAST_ISSUE_ONLINE, endYear);
    return this;
  }

  @Override
  public BibliographicItemAdapter setStartIssue(String startIssue) {
    kbt.setField(KbartTitle.Field.NUM_FIRST_ISSUE_ONLINE, startIssue);
    return this;
  }

  @Override
  public BibliographicItemAdapter setEndIssue(String endIssue) {
    kbt.setField(KbartTitle.Field.NUM_LAST_ISSUE_ONLINE, endIssue);
    return this;
  }

  @Override
  public BibliographicItemAdapter setCoverageDepth(String coverageDepth) {
    kbt.setField(KbartTitle.Field.COVERAGE_DEPTH, coverageDepth);
    return this;
  }
}
