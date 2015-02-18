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

package org.lockss.metadata;

import java.util.*;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.MetadataField;
import org.lockss.metadata.ArticleMetadataBuffer.ArticleMetadataInfo;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * Test class for org.lockss.daemon.ArticleMetadataBuffer.
 *
 * @author  Philip Gust
 * @version 1.0
 */
public class TestArticleMetadataBuffer extends LockssTestCase {
  static Logger log = Logger.getLogger(TestArticleMetadataBuffer.class);

  ArticleMetadataBuffer amBuffer;

  public void setUp() throws Exception {
    super.setUp();
    amBuffer = new ArticleMetadataBuffer(getTempDir());
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Variant with ISBN, ISSN, and item number metadata specified.
   * Should result in 'bookSeries' and 'book_chapter' being in metadata item.
   * @throws Exception
   */
  public void testArticleMetadtataBuffer1() throws Exception {
    for (int i = 1; i < 10; i++) {
      ArticleMetadata am = new ArticleMetadata();
      am.put(MetadataField.FIELD_PUBLICATION_TITLE, "Journal Title" + i);
      am.put(MetadataField.FIELD_DATE, "2012-12-0"+i);
      am.put(MetadataField.FIELD_ISSN,"1234-567" + i);
      am.put(MetadataField.FIELD_EISSN,"4321-765" + i);
      am.put(MetadataField.FIELD_ARTICLE_TITLE, "Article Title" + i);
      am.put(MetadataField.FIELD_PUBLISHER, "Publisher"+i);
      am.put(MetadataField.FIELD_AUTHOR, "Author,First"+i);
      am.put(MetadataField.FIELD_AUTHOR, "Author,Second"+i);
      am.put(MetadataField.FIELD_ACCESS_URL, "http://xyz.com/" + i);
      am.put(MetadataField.FIELD_ISBN, "0123456789");
      am.put(MetadataField.FIELD_EISBN, "9876543210");
      am.put(MetadataField.FIELD_ITEM_NUMBER, "3");
      amBuffer.add(am);
    }
    int count = 0;
    Iterator<ArticleMetadataInfo> amitr = amBuffer.iterator();
    while (amitr.hasNext()) {
      ArticleMetadataInfo aminfo = amitr.next();
      assertNotNull(aminfo);
      count++;
      assertEquals("2012",aminfo.pubYear);
      assertEquals("Journal Title"+count,aminfo.publicationTitle);
      assertEquals("2012-12-0"+count,aminfo.pubDate);
      assertEquals("1234-567"+count,aminfo.issn);
      assertEquals("4321-765"+count,aminfo.eissn);
      assertEquals("Article Title"+count,aminfo.articleTitle);
      assertEquals("Publisher"+count,aminfo.publisher);
      assertSameElements(
          new String[] {"Author,First"+count, "Author,Second"+count}, 
          aminfo.authors);
      assertEquals("http://xyz.com/"+count,aminfo.accessUrl);
      assertEquals("0123456789",aminfo.isbn);
      assertEquals("9876543210",aminfo.eisbn);
      assertEquals("3", aminfo.itemNumber);
      assertEquals(MetadataField.PUBLICATION_TYPE_BOOKSERIES, aminfo.publicationType);
      assertEquals(MetadataField.ARTICLE_TYPE_BOOKCHAPTER, aminfo.articleType);
    }
    assertEquals(9, count);
    amBuffer.close();
  }
  
  /**
   * Variant with 'bookSeries' publication type and 'book_chapter' article type.
   * Should result in 'bookSeries' and 'book_chapter' being in metadata item.
   * @throws Exception
   */
  public void testArticleMetadtataBuffer2() throws Exception {
    for (int i = 1; i < 10; i++) {
      ArticleMetadata am = new ArticleMetadata();
      am.put(MetadataField.FIELD_PUBLICATION_TITLE, "Journal Title" + i);
      am.put(MetadataField.FIELD_DATE, "2012-12-0"+i);
      am.put(MetadataField.FIELD_ARTICLE_TITLE, "Article Title" + i);
      am.put(MetadataField.FIELD_PUBLISHER, "Publisher"+i);
      am.put(MetadataField.FIELD_AUTHOR, "Author,First"+i);
      am.put(MetadataField.FIELD_AUTHOR, "Author,Second"+i);
      am.put(MetadataField.FIELD_ACCESS_URL, "http://xyz.com/" + i);
      am.put(MetadataField.FIELD_PUBLICATION_TYPE, "bookSeries");
      am.put(MetadataField.FIELD_ARTICLE_TYPE, "book_chapter");
      amBuffer.add(am);
    }
    int count = 0;
    Iterator<ArticleMetadataInfo> amitr = amBuffer.iterator();
    while (amitr.hasNext()) {
      ArticleMetadataInfo aminfo = amitr.next();
      assertNotNull(aminfo);
      count++;
      assertEquals("2012",aminfo.pubYear);
      assertEquals("Journal Title"+count,aminfo.publicationTitle);
      assertEquals("2012-12-0"+count,aminfo.pubDate);
      assertEquals("Article Title"+count,aminfo.articleTitle);
      assertEquals("Publisher"+count,aminfo.publisher);
      assertSameElements(
          new String[] {"Author,First"+count, "Author,Second"+count}, 
          aminfo.authors);
      assertEquals("http://xyz.com/"+count,aminfo.accessUrl);
      assertEquals(MetadataField.PUBLICATION_TYPE_BOOKSERIES, aminfo.publicationType);
      assertEquals(MetadataField.ARTICLE_TYPE_BOOKCHAPTER, aminfo.articleType);
    }
    assertEquals(9, count);
    amBuffer.close();
  }

  /**
   * Variant with 'bookSeries' publication type and item number metadata.
   * Should result in 'bookSeries' and 'book_chapter' being in metadata item.
   * @throws Exception
   */
  public void testArticleMetadtataBuffer3() throws Exception {
    for (int i = 1; i < 10; i++) {
      ArticleMetadata am = new ArticleMetadata();
      am.put(MetadataField.FIELD_PUBLICATION_TITLE, "Journal Title" + i);
      am.put(MetadataField.FIELD_DATE, "2012-12-0"+i);
      am.put(MetadataField.FIELD_ARTICLE_TITLE, "Article Title" + i);
      am.put(MetadataField.FIELD_PUBLISHER, "Publisher"+i);
      am.put(MetadataField.FIELD_AUTHOR, "Author,First"+i);
      am.put(MetadataField.FIELD_AUTHOR, "Author,Second"+i);
      am.put(MetadataField.FIELD_ACCESS_URL, "http://xyz.com/" + i);
      am.put(MetadataField.FIELD_PUBLICATION_TYPE, "bookSeries");
      am.put(MetadataField.FIELD_ITEM_NUMBER, "3");
      amBuffer.add(am);
    }
    int count = 0;
    Iterator<ArticleMetadataInfo> amitr = amBuffer.iterator();
    while (amitr.hasNext()) {
      ArticleMetadataInfo aminfo = amitr.next();
      assertNotNull(aminfo);
      count++;
      assertEquals("2012",aminfo.pubYear);
      assertEquals("Journal Title"+count,aminfo.publicationTitle);
      assertEquals("2012-12-0"+count,aminfo.pubDate);
      assertEquals("Article Title"+count,aminfo.articleTitle);
      assertEquals("Publisher"+count,aminfo.publisher);
      assertSameElements(
          new String[] {"Author,First"+count, "Author,Second"+count}, 
          aminfo.authors);
      assertEquals("http://xyz.com/"+count,aminfo.accessUrl);
      assertEquals("3", aminfo.itemNumber);
      assertEquals(MetadataField.PUBLICATION_TYPE_BOOKSERIES, aminfo.publicationType);
      assertEquals(MetadataField.ARTICLE_TYPE_BOOKCHAPTER, aminfo.articleType);
    }
    assertEquals(9, count);
    amBuffer.close();
  }

  public void testEmptyArticleMetadtataBuffer() throws Exception {
    Iterator<ArticleMetadataInfo> amitr = amBuffer.iterator();
    assertFalse(amitr.hasNext());
    try {
      amitr.next();
      fail("Should have thrown NoSuchElementException");
    } catch (NoSuchElementException ex) {
    }
    try {
      // cannot get iterator twice
      amBuffer.iterator();
      fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException ex) {
    }
    amBuffer.close();
  }

  public void testClosedArticleMetadtataBuffer() throws Exception {
    amBuffer.close();
    try {
      // cannot add after closing
      amBuffer.add(new ArticleMetadata());
      fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException ex) {      
    }
    try {
      // cannot obtain iterator after closing
      @SuppressWarnings("unused")
      Iterator<ArticleMetadataInfo> amitr = amBuffer.iterator();
      fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException ex) {
    }
    
  }
}
