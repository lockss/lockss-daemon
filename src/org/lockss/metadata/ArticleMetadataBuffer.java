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
package org.lockss.metadata;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import org.lockss.daemon.PublicationDate;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.MetadataField;
import org.lockss.util.CloseCallbackInputStream.DeleteFileOnCloseInputStream;
import org.lockss.util.FileUtil;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * This class is a buffer of ArticleMetadata records 
 * that will be stored in the database once the metadata
 * extraction process has completed.
 * 
 * @author Philip Gust
 *
 */
class ArticleMetadataBuffer {
  private static Logger log = Logger.getLogger(ArticleMetadataBuffer.class);

  File collectedMetadataFile = null;
  ObjectOutputStream outstream = null;
  ObjectInputStream instream = null;
  long infoCount = 0;
  
  /**
   * This class encapsulates the values extracted from metadata
   * records that will be stored in the metadatabase once the
   * extraction process has completed.
   * 
   * @author Philip Gust
   *
   */
  static class ArticleMetadataInfo implements Serializable {
    private static final long serialVersionUID = -2372571567706061080L;
    String publisher;
    String provider;
    String seriesTitle;
    String proprietarySeriesIdentifier;
    String publicationTitle;
    String publicationType;
    String isbn;
    String eisbn;
    String issn;
    String eissn;
    String volume;
    String issue;
    String startPage;
    String pubDate; 
    final String pubYear;
    String articleTitle;
    String articleType;
    Collection<String> authors;
    String doi;
    String accessUrl;
    Map<String, String> featuredUrlMap;
    Collection<String> keywords;
    String endPage;
    String coverage;
    String itemNumber;
    String proprietaryIdentifier;
    String fetchTime;

    /**
     * Extract the information from the ArticleMetadata
     * that will be stored in the database.
     * 
     * @param md the ArticleMetadata
     */
    public ArticleMetadataInfo(ArticleMetadata md) {
      publisher = md.get(MetadataField.FIELD_PUBLISHER);
      provider = md.get(MetadataField.FIELD_PROVIDER);
      seriesTitle = md.get(MetadataField.FIELD_SERIES_TITLE);
      proprietarySeriesIdentifier =
          md.get(MetadataField.FIELD_PROPRIETARY_SERIES_IDENTIFIER);
      publicationTitle = md.get(MetadataField.FIELD_PUBLICATION_TITLE);
      isbn = md.get(MetadataField.FIELD_ISBN);
      eisbn = md.get(MetadataField.FIELD_EISBN);
      issn = md.get(MetadataField.FIELD_ISSN);
      eissn = md.get(MetadataField.FIELD_EISSN);
      volume = md.get(MetadataField.FIELD_VOLUME);
      issue = md.get(MetadataField.FIELD_ISSUE);
      String allPages = md.get(MetadataField.FIELD_START_PAGE);
      if (allPages != null) {
        String[] pages = allPages.split("\\D");
        if (pages.length == 2) {
          startPage = pages[0];
          if (StringUtil.isNullString(md.get(MetadataField.FIELD_END_PAGE))) {
            endPage = pages[1];
          } else {
            endPage = md.get(MetadataField.FIELD_END_PAGE);
          }
        } else {
          startPage = allPages;
          endPage = md.get(MetadataField.FIELD_END_PAGE);
        }
      } else {
        startPage = null;
        endPage = md.get(MetadataField.FIELD_END_PAGE);
      }
      PublicationDate pd = getDateField(md);
      if (pd == null) {
        pubDate = pubYear = null;
      } else {
        pubDate = pd.toString();
        pubYear = Integer.toString(pd.getYear());
      }
      articleTitle = md.get(MetadataField.FIELD_ARTICLE_TITLE);
      authors = md.getList(MetadataField.FIELD_AUTHOR);
      doi = md.get(MetadataField.FIELD_DOI);
      accessUrl = md.get(MetadataField.FIELD_ACCESS_URL);
      featuredUrlMap = md.getRawMap(MetadataField.FIELD_FEATURED_URL_MAP);
      log.debug3("featuredUrlMap = " + featuredUrlMap);
      keywords = md.getList(MetadataField.FIELD_KEYWORDS);;
      coverage = md.get(MetadataField.FIELD_COVERAGE);
      itemNumber = md.get(MetadataField.FIELD_ITEM_NUMBER);
      proprietaryIdentifier =
          md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER);
      fetchTime = md.get(MetadataField.FIELD_FETCH_TIME);
      
      // get publication type from metadata or infer it if not set
      publicationType = md.get(MetadataField.FIELD_PUBLICATION_TYPE);
      if (StringUtil.isNullString(publicationType)) {
        if (MetadataManager.isBookSeries(
            issn, eissn, isbn, eisbn, seriesTitle, volume)) {
          // book series if e/isbn and either e/issn or volume fields present
          publicationType = MetadataField.PUBLICATION_TYPE_BOOKSERIES;
        } else if (MetadataManager.isBook(isbn, eisbn)) {
          // book if e/isbn present
          publicationType = MetadataField.PUBLICATION_TYPE_BOOK;
        } else {
          // journal if not book or bookSeries
          publicationType = MetadataField.PUBLICATION_TYPE_JOURNAL;
        }
      }
      
      // get article type from metadata or infer it if not set
      articleType = md.get(MetadataField.FIELD_ARTICLE_TYPE);
      if (StringUtil.isNullString(articleType)) {
        if (   MetadataField.PUBLICATION_TYPE_BOOK.equals(publicationType)
            || MetadataField.PUBLICATION_TYPE_BOOKSERIES.equals(publicationType)) {
          if (    StringUtil.isNullString(startPage)
              || !StringUtil.isNullString(endPage)
              || !StringUtil.isNullString(itemNumber)) {
            // assume book chapter if startPage, endPage, or itemNumber present
            articleType = MetadataField.ARTICLE_TYPE_BOOKCHAPTER;
          } else {
            // assume book volume if none of these fields are present
            articleType = MetadataField.ARTICLE_TYPE_BOOKVOLUME;
          }
        } else if (MetadataField.PUBLICATION_TYPE_JOURNAL.equals(publicationType)) {
          // assume article for journal
          articleType = MetadataField.ARTICLE_TYPE_JOURNALARTICLE;          
        }
      }
    }
    
    /**
     * Return the date field to store in the database. The date field can be
     * nearly anything a MetaData extractor chooses to provide, making it a near
     * certainty that this method will be unable to parse it, even with the help
     * of locale information.
     * 
     * @param md the ArticleMetadata
     * @return the publication date or <code>null</code> if none specified 
     *    or one cannot be parsed from the metadata information
     */
    static private PublicationDate getDateField(ArticleMetadata md) {
      PublicationDate pubDate = null;
      String dateStr = md.get(MetadataField.FIELD_DATE);
      if (dateStr != null) {
        Locale locale = md.getLocale();
        if (locale == null) {
          locale = Locale.getDefault();
        }
        try {
          pubDate = new PublicationDate(dateStr, locale);
        } catch (ParseException ex) {}
      }
      return pubDate;
    }

    /**
     * Provides a printable version of this object.
     * 
     * @return a String with the printable version of the object.
     */
    @Override
    public String toString() {
      return "ArticleMetadataInfo "
          + "[publisher=" + publisher
          + ", seriesTitle=" + seriesTitle
          + ", proprietarySeriesIdentifier=" + proprietarySeriesIdentifier
          + ", publicationTitle=" + publicationTitle
          + ", isbn=" + isbn
          + ", eisbn=" + eisbn
          + ", issn=" + issn
          + ", eissn=" + eissn
          + ", volume=" + volume
          + ", issue=" + issue
          + ", startPage=" + startPage
          + ", pubDate=" + pubDate
          + ", pubYear=" + pubYear
          + ", articleTitle=" + articleTitle
          + ", articleType=" + articleType
          + ", authorSet=" + authors
          + ", doi="+ doi
          + ", accessUrl=" + accessUrl
          + ", featuredUrlMap=" + featuredUrlMap
          + ", keywordSet=" + keywords
          + ", endPage=" + endPage
          + ", coverage=" + coverage
          + ", itemNumber=" + itemNumber
          + ", proprietaryIdentifier=" + proprietaryIdentifier + "]";
    }
  }

  public ArticleMetadataBuffer(File tmpdir) throws IOException {
    collectedMetadataFile = 
      FileUtil.createTempFile("MetadataManager", "md", tmpdir);
    outstream =
        new ObjectOutputStream(
            new BufferedOutputStream(
                new FileOutputStream(collectedMetadataFile)));
  }

  /** 
   * Add the information from the specified metadata record
   * to the buffer
   * 
   * @param md the metadata record
   * @throws IllegalStateException if no more items can be added
   *   because the iterator has already been obtained
   */
  public void add(ArticleMetadata md) throws IOException {
    if (outstream == null) {
      throw new IllegalStateException("collectedMetadataOutputStream closed");
    }
    ArticleMetadataInfo mdinfo = new ArticleMetadataInfo(md);
    outstream.writeObject(mdinfo);
    infoCount++;
  }

  /**
   * Return an iterator to the buffered metadata information.
   * Only one iterator per buffer is available.
   * 
   * @return an iterator to the buffered metadata information
   * @throws IllegalStateException if the iterator cannot be obtained
   *   because the buffer is closed
   */
  public Iterator<ArticleMetadataInfo> iterator() {
    if (!isOpen()) {
      throw new IllegalStateException("Buffer is closed");
    }
    
    if (instream != null) {
      throw new IllegalStateException("Iterator already obtained.");
    }

    // buffer is closed for adding once iterator is obtained.
    IOUtil.safeClose(outstream);
    outstream = null;

    return new Iterator<ArticleMetadataInfo>() {
      {
        try {
          instream = 
            new ObjectInputStream(
                new BufferedInputStream(
                    new DeleteFileOnCloseInputStream(collectedMetadataFile)));
        } catch (IOException ex) {
          log.warning("Error opening input stream", ex);
        }
      }
      
      @Override
      public boolean hasNext() {
        return (instream != null) && (infoCount > 0);
      }
      
      @Override
      public ArticleMetadataInfo next() throws NoSuchElementException {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        try {
          ArticleMetadataInfo info = (ArticleMetadataInfo)instream.readObject();
          infoCount--;
          return info;
        } catch (ClassNotFoundException ex) {
          NoSuchElementException ex2 = 
              new NoSuchElementException("Error reading next element");
          ex2.initCause(ex);
          throw ex2;
        } catch (IOException ex) {
          NoSuchElementException ex2 = 
              new NoSuchElementException("Error reading next element");
          ex2.initCause(ex);
          throw ex2;
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * Determines whether buffer is open.
   * @return <code>true</code> if buffer is open
   */
  public boolean isOpen() {
    return (collectedMetadataFile != null);
  }
  
  /**
   * Release the collected metadata.
   */
  public void close() {
    // collectedMetadataOutputStream automatically deleted on close 
    IOUtil.safeClose(outstream);
    IOUtil.safeClose(instream);
    outstream = null;
    instream = null;
    collectedMetadataFile = null;
  }
}
