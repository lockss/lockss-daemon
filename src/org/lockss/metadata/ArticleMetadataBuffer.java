/*
 * $Id: ArticleMetadataBuffer.java,v 1.2 2013-03-26 22:15:07 fergaloy-sf Exp $
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
package org.lockss.metadata;

import static org.lockss.db.DbManager.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
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
    String journalTitle;
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
    final String authors;
    Set<String> authorSet;
    String doi;
    String accessUrl;
    Map<String, String> featuredUrlMap;
    Set<String> keywordSet;
    String endPage;
    String coverage;
    String itemNumber;
    String proprietaryIdentifier;

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
      articleTitle = getArticleTitleField(md);
      authors = getAuthorField(md);
      authorSet = getAuthorSet(md);
      doi = md.get(MetadataField.FIELD_DOI);
      accessUrl = md.get(MetadataField.FIELD_ACCESS_URL);
      featuredUrlMap =
	  new HashMap<String, String>(md.getRawMap(MetadataField
	                                           .FIELD_FEATURED_URL_MAP));
      log.debug3("featuredUrlMap = " + featuredUrlMap);
      keywordSet = getKeywordSet(md);
      coverage = md.get(MetadataField.FIELD_COVERAGE);
      itemNumber = md.get(MetadataField.FIELD_ITEM_NUMBER);
      proprietaryIdentifier =
	  md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER);
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

      // create author field as semicolon-separated list of authors from
      // metadata
      if (authors != null) {
        for (String a : authors) {
          if (sb.length() == 0) {
            // check first author to see if it's too long -- probably caused 
            // by metadata extractor not properly splitting a list of authors
            if (a.length() >= MAX_AUTHOR_COLUMN) {
              // just truncate first author to max length
              log.warning("Author metadata too long -- truncating:'" + a + "'");
              a = a.substring(0,MAX_AUTHOR_COLUMN);
              break;
            }
          } else {
            // include as many authors as will fit in the field
            if (sb.length()+a.length()+1 > MAX_AUTHOR_COLUMN) {
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
     * Return the authors as a set.
     * 
     * @param md the ArticleMetadata
     * @return the authors or <code>null</code> if none specified
     */
    private static Set<String> getAuthorSet(ArticleMetadata md) {
      return new HashSet<String>(md.getList(MetadataField.FIELD_AUTHOR));
    }
    
    /**
     * Return the keywords as a set.
     * 
     * @param md the ArticleMetadata
     * @return the keywords or <code>null</code> if none specified
     */
    private static Set<String> getKeywordSet(ArticleMetadata md) {
      Set<String> keywords = new HashSet<String>();
      List<String> original = md.getList(MetadataField.FIELD_KEYWORDS);

      if (original == null || original.size() == 0) {
	return keywords;
      }
      
      String [] splitKeywords;
      
      for (String unsplitKeywords : original) {
	splitKeywords = unsplitKeywords.split(",");

	for (String keyword : splitKeywords) {
	  keywords.add(keyword.trim());
	}
      }
      
      return keywords;
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
      if (articleTitle != null
	  && (articleTitle.length() > MAX_NAME_COLUMN)) {
	articleTitle =
	    articleTitle.substring(0, MAX_NAME_COLUMN);
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

    /**
     * Provides a printable version of this object.
     * 
     * @return a String with the printable version of the object.
     */
    @Override
    public String toString() {
      return "ArticleMetadataInfo [publisher=" + publisher
	  + ", journalTitle=" + journalTitle
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
	  + ", authors=" + authors
	  + ", authorSet=" + authorSet
	  + ", doi="+ doi
	  + ", accessUrl=" + accessUrl
	  + ", featuredUrlMap=" + featuredUrlMap
	  + ", keywordSet=" + keywordSet
	  + ", endPage=" + endPage
	  + ", coverage=" + coverage
	  + ", itemNumber=" + itemNumber
	  + ", proprietaryIdentifier=" + proprietaryIdentifier + "]";
    }
  }

  public ArticleMetadataBuffer() throws IOException {
    if (outstream == null) {
      collectedMetadataFile = 
          FileUtil.createTempFile("MetadataManager", "md");
      outstream =
          new ObjectOutputStream(
              new BufferedOutputStream(
                  new FileOutputStream(collectedMetadataFile)));
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
