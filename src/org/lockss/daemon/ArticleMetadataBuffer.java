/*
 * $Id: ArticleMetadataBuffer.java,v 1.1 2012-08-02 18:51:50 pgust Exp $
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
package org.lockss.daemon;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.MetadataField;
import org.lockss.util.CloseCallbackInputStream.DeleteFileOnCloseInputStream;
import org.lockss.util.FileUtil;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;

/**
 * This class is a buffer of ArticleMetadata records 
 * that will be stored in the database once the metadata
 * extraction process has completed.
 * 
 * @author Philip Gust
 *
 */
class ArticleMetadataBuffer {
  private static Logger log = Logger.getLogger("MetadataManager");

  File collectedMetadataFile = null;
  ObjectOutputStream collectedMetadataOutputStream = null;
  ObjectInputStream collectedMetadataInputStream = null;
  
  /**
   * This class encapsulates the values extracted from metadata
   * records that will be stored in the metadatabase once the
   * extraction process has completed.
   * 
   * @author Philip Gust
   *
   */
  @SuppressWarnings("serial")
  static class ArticleMetadataInfo implements Serializable {
    final String publisher;
    final String journalTitle;
    final String isbn;
    final String eisbn;
    final String issn;
    final String eissn;
    final String volume;
    final String issue;
    final String startPage;
    final String pubDate; 
    final String pubYear;
    final String articleTitle;
    final String authors;
    final String doi;
    final String accessUrl;
    
    /**
     * Extract the information from the ArticleMetadata
     * that will be stored in the database.
     * 
     * @param md the ArticleMetadata
     */
    public ArticleMetadataInfo(ArticleMetadata md) {
      publisher=md.get(MetadataField.FIELD_PUBLISHER);
      journalTitle = md.get(MetadataField.FIELD_JOURNAL_TITLE);
      isbn = md.get(MetadataField.FIELD_ISBN);
      eisbn = md.get(MetadataField.FIELD_EISBN);
      issn = md.get(MetadataField.FIELD_ISSN);
      eissn = md.get(MetadataField.FIELD_EISSN);
      volume = md.get(MetadataField.FIELD_VOLUME);
      issue = md.get(MetadataField.FIELD_ISSUE);
      startPage = md.get(MetadataField.FIELD_START_PAGE);
      PublicationDate pd = getDateField(md);
      if (pd == null) {
        pubDate = pubYear = null;
      } else {
        pubDate = pd.toString();
        pubYear = Integer.toString(pd.getYear());
      }
      articleTitle = getArticleTitleField(md);
      authors = getAuthorField(md);
      doi = md.get(MetadataField.FIELD_DOI);
      accessUrl = md.get(MetadataField.FIELD_ACCESS_URL);
    }
    /**
     * Return the author field to store in database. The field is comprised of a
     * semicolon separated list of as many authors as will fit in the database
     * author field.
     * 
     * @param md the ArticleMetadata
     * @return the author or <code>null</code> if none specified
     */
    private static String getAuthorField(ArticleMetadata md) {
      StringBuilder sb = new StringBuilder();
      List<String> authors = md.getList(MetadataField.FIELD_AUTHOR);

      // create author field as semicolon-separated list of authors from metadata
      if (authors != null) {
        for (String a : authors) {
          if (sb.length() == 0) {
            // check first author to see if it's too long -- probably caused 
            // by metadata extractor not properly splitting a list of authors
            if (a.length() >= MetadataManager.MAX_AUTHOR_FIELD) {
              // just truncate first author to max length
              log.warning("Author metadata too long -- truncating:'" + a + "'");
              a = a.substring(0,MetadataManager.MAX_AUTHOR_FIELD);
              break;
            }
          } else {
            // include as many authors as will fit in the field
            if (sb.length()+a.length()+1 > MetadataManager.MAX_AUTHOR_FIELD) {
              break;
            }
            sb.append(';');
          }
          sb.append(a);
        }
      }
      return (sb.length() == 0) ? null : sb.toString();
    }

    /**
     * Return the article title field to store in the database. The field is
     * truncated to the size of the article title field.
     * 
     * @param md the ArticleMetadata
     * @return the articleTitleField or <code>null</code> if none specified
     */
    static private String getArticleTitleField(ArticleMetadata md) {
      // elide title field
      String articleTitle = md.get(MetadataField.FIELD_ARTICLE_TITLE);
      if (   (articleTitle != null) 
          && (articleTitle.length() > MetadataManager.MAX_ATITLE_FIELD)) {
        articleTitle = articleTitle.substring(0, MetadataManager.MAX_ATITLE_FIELD);
      }
      return articleTitle;
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
        pubDate = new PublicationDate(dateStr, locale);
      }
      return pubDate;
    }
  }

  public ArticleMetadataBuffer()  throws IOException {
    if (collectedMetadataOutputStream == null) {
      collectedMetadataFile = 
          FileUtil.createTempFile("MetadataManager", "md");
      collectedMetadataOutputStream = 
          new ObjectOutputStream(
              new FileOutputStream(collectedMetadataFile));
    }
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
    if (collectedMetadataOutputStream == null) {
      throw new IllegalStateException("collectedMetadataOutputStream closed");
    }
    ArticleMetadataInfo mdinfo = new ArticleMetadataInfo(md);
    collectedMetadataOutputStream.writeObject(mdinfo);
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
    
    if (collectedMetadataInputStream != null) {
      throw new IllegalStateException("Iterator already obtained.");
    }

    // buffer is closed for adding once iterator is obtained.
    IOUtil.safeClose(collectedMetadataOutputStream);
    collectedMetadataOutputStream = null;

    return new Iterator<ArticleMetadataInfo>() {
      ArticleMetadataInfo nextInfo = null;
      {
        try {
          collectedMetadataInputStream = 
            new ObjectInputStream(
                new DeleteFileOnCloseInputStream(collectedMetadataFile));
          doNext();
        } catch (IOException ex) {
          collectedMetadataInputStream = null;
          log.warning("Error opening input stream", ex);
        }
      }
      
      private void doNext() throws IOException{
        if (!isOpen()) { 
          throw new IllegalStateException("Buffer is closed");
        }
        nextInfo = null;
        if (collectedMetadataInputStream != null) {
          try {
              nextInfo = 
                (ArticleMetadataInfo)collectedMetadataInputStream.readObject();
          } catch (EOFException ex) {
          } catch (ClassNotFoundException ex) {
            log.warning("Unexpected error reading metadata info", ex);
          }
        }
      }
      
      @Override
      public boolean hasNext() {
        return isOpen() && nextInfo != null;
      }
      
      @Override
      public ArticleMetadataInfo next() throws NoSuchElementException {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        ArticleMetadataInfo info = nextInfo;
        nextInfo = null;
        try {
          doNext();
        } catch (IOException ex) {
          log.warning("Error reading next metadata info", ex);
        }
        return info;
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
    IOUtil.safeClose(collectedMetadataOutputStream);
    IOUtil.safeClose(collectedMetadataInputStream);
    collectedMetadataOutputStream = null;
    collectedMetadataInputStream = null;
    collectedMetadataFile = null;
  }
}
