/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.extractor;


/**
 * Describes the type, purpose, format, etc. of the metadata the client
 * would like extracted.  Passed to ArticleIterator and MetadataExtractor
 * factories.
 */
public class MetadataTarget {

  /** @deprecated Use {@link #isAny()} or {@link #Any()} instead. */
  public static MetadataTarget Any = new MetadataTarget("Any");
  /** @deprecated Use {@link #isDoi()} or {@link #Doi()} instead. */
  public static MetadataTarget DOI = new MetadataTarget("DOI");
  /** @deprecated Use {@link #isOpenURL()} or {@link #OpenURL()} instead. */
  public static MetadataTarget OpenURL = new MetadataTarget("OpenURL");
  /** @deprecated Use {@link #isArticle()} or {@link #Article()} instead. */
  public static MetadataTarget Article = new MetadataTarget("Article");

  /** Use when no knowledge of the particular type of metadata needed. */
  public static final String PURPOSE_ANY = "Any";
  /** Extract DOIs. */
  public static final String PURPOSE_DOI = "DOI";
  /** Extract DOIs. */
  public static final String PURPOSE_OPENURL = "OpenURL";
  /** Extract only the list of article URLs.  (Usually alleviates need to
   * parse content files.) */
  public static final String PURPOSE_ARTICLE = "Article";

  private String format;
  private String purpose;
  private long includeAfterDate = 0;


  public MetadataTarget() {
  }

  public MetadataTarget(String purpose) {
    this.purpose = purpose;
  }

  public MetadataTarget setFormat(String format) {
    this.format = format;
    return this;
  }

  public String getFormat() {
    return format;
  }

  public MetadataTarget setPurpose(String purpose) {
    this.purpose = purpose;
    return this;
  }

  public String getPurpose() {
    return purpose;
  }

  /**
   * Express interest in files that have changed after the specified date.
   * Allows article iterators to skip over entire archive files, avoid the
   * (large) expense of expanding them.  There is no guarantee that no
   * older files will be included; currently, <i>only</i> archive files are
   * skipped.
   */
  public MetadataTarget setIncludeFilesChangedAfter(long date) {
    this.includeAfterDate = date;
    return this;
  }

  public long getIncludeFilesChangedAfter() {
    return includeAfterDate;
  }

  /** Return true if the purpose of the metadata extraction is unspecified. */
  public boolean isAny() {
    return PURPOSE_ANY.equals(purpose);
  }

  /** Return true if the purpose of the metadata extraction is to find DOIs. */
  public boolean isDoi() {
    return PURPOSE_DOI.equals(purpose);
  }

  /** Return true if the purpose of the metadata extraction is to find
   * bibliographic metadata necessary to resolve OpenURLs. */
  public boolean isOpenURL() {
    return PURPOSE_OPENURL.equals(purpose);
  }

  /** Return true if the purpose of the metadata extraction is to find
   * article URLs. */
  public boolean isArticle() {
    return PURPOSE_ARTICLE.equals(purpose);
  }

  /** Return a new MetadataTarget whose purpose is {@value #PURPOSE_ANY} */
  public static MetadataTarget Any() {
    return new MetadataTarget(PURPOSE_ANY);
  }

  /** Return a new MetadataTarget whose purpose is {@value #PURPOSE_DOI} */
  public static MetadataTarget Doi() {
    return new MetadataTarget(PURPOSE_DOI);
  }

  /** Return a new MetadataTarget whose purpose is {@value #PURPOSE_OPENURL} */
  public static MetadataTarget OpenURL() {
    return new MetadataTarget(PURPOSE_OPENURL);
  }

  /** Return a new MetadataTarget whose purpose is {@value #PURPOSE_ARTICLE} */
  public static MetadataTarget Article() {
    return new MetadataTarget(PURPOSE_ARTICLE);
  }

}
