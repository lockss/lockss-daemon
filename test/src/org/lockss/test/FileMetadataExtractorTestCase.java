/*
 * $Id: FileMetadataExtractorTestCase.java,v 1.3 2011-01-10 09:12:40 tlipkis Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.io.*;
import java.util.*;
import org.lockss.extractor.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/** Framework for FileMetadataExtractor tests.  Subs must implement only
 * {@link #getFactory()} and {@link #getMimeType()}, and tests which call
 * {@link #extractFrom(String)}. */
public abstract class FileMetadataExtractorTestCase extends LockssTestCase {
  public static String URL = "http://www.example.com/";

  public static String MIME_TYPE_HTML = "text/html";
  public static String MIME_TYPE_XML = "application/xml";
  public static String MIME_TYPE_RAM = "audio/x-pn-realaudio";

  protected FileMetadataListExtractor extractor = null;
  protected String encoding;
  protected MockArchivalUnit mau;
  protected MockCachedUrl cu;

  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    cu = new MockCachedUrl(getUrl(), mau);
    FileMetadataExtractor fme =
      getFactory().createFileMetadataExtractor(getMimeType());
    extractor = new FileMetadataListExtractor(fme);
    encoding = getEncoding();
  }

  public abstract String getMimeType();
  public abstract FileMetadataExtractorFactory getFactory();

  public String getEncoding() {
    return Constants.DEFAULT_ENCODING;
  }

  public String getUrl() {
    return URL;
  }

  protected void assertMdEmpty(String text) {
    assertEquals(0, extractFrom(text).rawSize());
  }

  protected void assertMdEquals(String expkey1, String expval1,
				String text) {
    ArticleMetadata md = extractFrom(text);
    assertEquals(expval1, md.getRaw(expkey1));
    assertEquals(1, extractFrom(text).rawSize());
  }

  protected void assertMdEquals(String expkey1, String expval1,
				String expkey2, String expval2,
				String text) {
    ArticleMetadata md = extractFrom(text);
    assertEquals(expval1, md.getRaw(expkey1));
    assertEquals(expval2, md.getRaw(expkey2));
    assertEquals(2, extractFrom(text).rawSize());
  }

  protected void assertMdEquals(List<String> keyvaluepairs,
				String text) {
    assertEquals("Invalid call to assertMdEquals: odd length key/value list",
		 0, keyvaluepairs.size() % 1);

    ArticleMetadata md = extractFrom(text);
    Iterator<String> iter = keyvaluepairs.iterator();
    while (iter.hasNext()) {
      String key = iter.next();
      String val = iter.next();
      assertEquals(val, md.getRaw(key));
    }
    assertEquals(keyvaluepairs.size() / 2, md.rawSize());
  }

  public void testEmptyFileReturnsEmptyMetadata() throws Exception {
    List<ArticleMetadata> lst = extractor.extract(cu);
    assertEquals(1, lst.size());
    ArticleMetadata md = lst.get(0);
    assertEquals(0, md.rawSize());
  }

  public void testThrows() throws IOException, PluginException {
    try {
      extractor.extract(null);
      fail("Calling extract with a null InputStream should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  protected ArticleMetadata extractFrom(String content) {
    try {
      List<ArticleMetadata> lst = extractor.extract(cu.addVersion(content));
      return lst.get(0);
    } catch (Exception e) {
      fail("extract threw " + e);
    }
    return null;			// impossible
  }
      
}
