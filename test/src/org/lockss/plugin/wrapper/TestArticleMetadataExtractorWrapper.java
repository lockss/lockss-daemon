/*
 * $Id$
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
package org.lockss.plugin.wrapper;

import java.io.*;
import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.crawler.*;
import org.lockss.test.*;
import org.lockss.extractor.*;
import org.lockss.util.*;

public class TestArticleMetadataExtractorWrapper extends LockssTestCase {

  private static final String url = "http://www.example.com/";
  public void testWrap() throws PluginException, IOException {
    ArticleMetadataExtractor obj = new MyArticleMetadataExtractor();
    ArticleMetadataExtractor wrapper =
      WrapperUtil.wrap(obj, ArticleMetadataExtractor.class);
    assertTrue(wrapper instanceof ArticleMetadataExtractorWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MyArticleMetadataExtractor);

    CachedUrl cu = new MockCachedUrl(url);
    ArticleFiles af = new ArticleFiles();
    wrapper.extract(MetadataTarget.Any(), af, null);
    MyArticleMetadataExtractor mn = (MyArticleMetadataExtractor)obj;
    assertEquals(ListUtil.list(af), mn.args);
  }

  public void testLinkageError() throws IOException {
    ArticleMetadataExtractor obj = new MyArticleMetadataExtractor();
    ArticleMetadataExtractor wrapper =
      WrapperUtil.wrap(obj, ArticleMetadataExtractor.class);
    assertTrue(wrapper instanceof ArticleMetadataExtractorWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MyArticleMetadataExtractor);
    Error err = new LinkageError("bar");
    MyArticleMetadataExtractor a = (MyArticleMetadataExtractor)obj;
    a.setError(err);
    ArticleFiles af = new ArticleFiles();
    CachedUrl cu = new MockCachedUrl(url);
    try {
      wrapper.extract(MetadataTarget.Any(), af, null);
      fail("Should have thrown PluginException");
    } catch (PluginException e) {
      assertTrue(e instanceof PluginException.LinkageError);
    }
  }

  public static class MyArticleMetadataExtractor
    implements ArticleMetadataExtractor {
    List args;
    Error error;

    void setError(Error error) {
      this.error = error;
    }

    @Override
    public void extract(MetadataTarget target,
			ArticleFiles af,
			ArticleMetadataExtractor.Emitter emitter)
	throws IOException {
      args = ListUtil.list(af);
      if (error != null) {
	throw error;
      }
    }
  }
}
