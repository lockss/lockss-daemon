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
import org.lockss.extractor.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestArticleMetadataExtractorFactoryWrapper extends LockssTestCase {

  MetadataTarget target1 = new MetadataTarget("one");

  public void testWrap() throws PluginException, IOException {
    ArticleMetadataExtractorFactory obj =
      new MockArticleMetadataExtractorFactory();
    ArticleMetadataExtractorFactory wrapper =
      WrapperUtil.wrap(obj, ArticleMetadataExtractorFactory.class);
    assertTrue(wrapper instanceof ArticleMetadataExtractorFactoryWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper)
	       instanceof MockArticleMetadataExtractorFactory);

    wrapper.createArticleMetadataExtractor(target1);
    MockArticleMetadataExtractorFactory mn =
      (MockArticleMetadataExtractorFactory)obj;
    assertEquals(ListUtil.list(target1), mn.args);
  }

  public void testLinkageError() throws IOException {
    ArticleMetadataExtractorFactory obj =
      new MockArticleMetadataExtractorFactory();
    ArticleMetadataExtractorFactory wrapper =
      WrapperUtil.wrap(obj, ArticleMetadataExtractorFactory.class);
    assertTrue(wrapper instanceof ArticleMetadataExtractorFactoryWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper)
	       instanceof MockArticleMetadataExtractorFactory);
    Error err = new LinkageError("bar");
    MockArticleMetadataExtractorFactory a =
      (MockArticleMetadataExtractorFactory)obj;
    a.setError(err);
    try {
      wrapper.createArticleMetadataExtractor(MetadataTarget.Any());
      fail("Should have thrown PluginException");
    } catch (PluginException e) {
      assertTrue(e instanceof PluginException.LinkageError);
    }
  }

  public static class MockArticleMetadataExtractorFactory
    implements ArticleMetadataExtractorFactory {

    List args;
    Error error;

    void setError(Error error) {
      this.error = error;
    }

    public ArticleMetadataExtractor
      createArticleMetadataExtractor(MetadataTarget target)
	throws PluginException {
      args = ListUtil.list(target);
      if (error != null) {
	throw error;
      }
      return new MockArticleMetadataExtractor();
    }
  }
}
