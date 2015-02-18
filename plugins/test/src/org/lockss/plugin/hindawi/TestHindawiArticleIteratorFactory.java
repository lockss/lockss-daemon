/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.hindawi;

import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestHindawiArticleIteratorFactory extends ArticleIteratorTestCase {

  private final String PLUGIN_NAME = "org.lockss.plugin.hindawi.HindawiPublishingCorporationPlugin";

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String DOWNLOAD_URL_KEY = "download_url";
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();

  private final String BASE_URL = "http://www.example.com/";
  private final String DOWNLOAD_URL = "http://download.example.com/";
  private final String VOLUME_NAME = "2008";
  private final String JOURNAL_ID = "jid";

  private final Configuration AU_CONFIG =
      ConfigurationUtil.fromArgs(BASE_URL_KEY, BASE_URL,
                                 DOWNLOAD_URL_KEY, DOWNLOAD_URL,
                                 VOLUME_NAME_KEY, VOLUME_NAME,
                                 JOURNAL_ID_KEY, JOURNAL_ID);

  public void setUp() throws Exception {
    super.setUp();
    au = PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }

  public void testArticleIterator() throws Exception {
    final int FLAG_FULL_TEXT_HTML = 0x1;
    final int FLAG_FULL_TEXT_PDF = 0x2;
    final int FLAG_ABSTRACT = 0x4;
    final int FLAG_REFERENCES = 0x8;
    final int FLAG_CITATION = 0x10;
    final int FLAG_FULL_TEXT_EPUB = 0x20;
    final int MAX = 0x40;
    
    // Create all combinations of URLs that may or may not amount to articles
    for (int counter = 0 ; counter < MAX ; ++counter) {
      for (int flag = 0x1 ; flag < MAX ; flag <<= 1) {
        if ((counter & flag) != 0) {
          String url = null;
          switch (flag) {
            case FLAG_FULL_TEXT_HTML: url = String.format("%sjournals/%s/%s/%08d", BASE_URL, JOURNAL_ID, VOLUME_NAME, counter); break;
            case FLAG_FULL_TEXT_PDF: url = String.format("%sjournals/%s/%s/%08d.pdf", DOWNLOAD_URL, JOURNAL_ID, VOLUME_NAME, counter); break;
            case FLAG_ABSTRACT: url = String.format("%sjournals/%s/%s/%08d/abs", BASE_URL, JOURNAL_ID, VOLUME_NAME, counter); break;
            case FLAG_REFERENCES: url = String.format("%sjournals/%s/%s/%08d/ref", BASE_URL, JOURNAL_ID, VOLUME_NAME, counter); break;
            case FLAG_CITATION: url = String.format("%sjournals/%s/%s/%08d/cta", BASE_URL, JOURNAL_ID, VOLUME_NAME, counter); break;
            case FLAG_FULL_TEXT_EPUB: url = String.format("%sjournals/%s/%s/%08d.epub", DOWNLOAD_URL, JOURNAL_ID, VOLUME_NAME, counter); break;
            default: fail(String.format("Internal error: counter=0x%x, flag=0x%x", counter, flag));
          }
          au.makeUrlCacher(new UrlData(new StringInputStream(url), new CIProperties(), url)).storeContent();
        }
      }
    }

    // Collect all articles seen by the article iterator
    Map<String, ArticleFiles> map = new HashMap<String, ArticleFiles>();
    SubTreeArticleIterator iter = createSubTreeIter();
    while (iter.hasNext()) {
      ArticleFiles af = iter.next();
      String url = af.getFullTextUrl();
      if (url.endsWith(".pdf")) {
        map.put(url.substring(url.length() - 12, url.length() - 4), af);
      }
      else {
        map.put(url.substring(url.length() - 8), af);
      }
    }
    
    // Check that only the expected articles came out as an ArticleFiles
    // and that each has the expected traits
    for (int counter = 0 ; counter < MAX ; ++counter) {
      String counterStr = String.format("%08d", counter);
      String counterHex = Integer.toHexString(counter);
      ArticleFiles af = map.get(counterStr);
      // No ArticleFiles if no full text HTML and no full text PDF
      if ((counter & (FLAG_FULL_TEXT_HTML | FLAG_FULL_TEXT_PDF)) == 0) {
        assertNull(counterHex, af);
        continue;
      }
      assertNotNull(counterHex, af);
      for (int flag = 0x1 ; flag < MAX ; flag <<= 1) {
        if ((counter & flag) != 0) {
          String url = null;
          String role = null;
          switch (flag) {
            case FLAG_FULL_TEXT_HTML: {
              url = String.format("%sjournals/%s/%s/%08d", BASE_URL, JOURNAL_ID, VOLUME_NAME, counter);
              role = ArticleFiles.ROLE_FULL_TEXT_HTML;
              assertEquals(url, af.getFullTextUrl());
            } break;
            case FLAG_FULL_TEXT_PDF: {
              url = String.format("%sjournals/%s/%s/%08d.pdf", DOWNLOAD_URL, JOURNAL_ID, VOLUME_NAME, counter);
              role = ArticleFiles.ROLE_FULL_TEXT_PDF;
              if ((counter & FLAG_FULL_TEXT_HTML) == 0) {
                assertEquals(url, af.getFullTextUrl());
              }
              else {
                assertEquals(String.format("%sjournals/%s/%s/%08d", BASE_URL, JOURNAL_ID, VOLUME_NAME, counter), af.getFullTextUrl());
              }
            } break;
            case FLAG_ABSTRACT: {
              url = String.format("%sjournals/%s/%s/%08d/abs", BASE_URL, JOURNAL_ID, VOLUME_NAME, counter);
              role = ArticleFiles.ROLE_ABSTRACT;
            } break;
            case FLAG_REFERENCES: {
              url = String.format("%sjournals/%s/%s/%08d/ref", BASE_URL, JOURNAL_ID, VOLUME_NAME, counter);
              role = ArticleFiles.ROLE_REFERENCES;
            } break;
            case FLAG_CITATION: {
              url = String.format("%sjournals/%s/%s/%08d/cta", BASE_URL, JOURNAL_ID, VOLUME_NAME, counter);
              role = ArticleFiles.ROLE_CITATION;
            } break;
            case FLAG_FULL_TEXT_EPUB: {
              url = String.format("%sjournals/%s/%s/%08d.epub", DOWNLOAD_URL, JOURNAL_ID, VOLUME_NAME, counter);
              role = ArticleFiles.ROLE_FULL_TEXT_EPUB;
            } break;
            default: fail(String.format("Internal error: counter=0x%x, flag=0x%x", counter, flag));
          }
          assertEquals(url, af.getRoleUrl(role));
        }
      }
    }
    
  }
  
}