/*
 * $Id: TestArticleIteratorFactoryWrapper.java,v 1.1 2009-05-19 03:49:09 dshr Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.test.*;
import org.lockss.util.*;

public class TestArticleIteratorFactoryWrapper extends LockssTestCase {

  public void testWrap() throws PluginException, IOException {
    ArticleIteratorFactory obj = new MockArticleIteratorFactory();
    ArticleIteratorFactory wrapper =
      (ArticleIteratorFactory)WrapperUtil.wrap(obj, ArticleIteratorFactory.class);
    assertTrue(wrapper instanceof ArticleIteratorFactoryWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper)
	       instanceof MockArticleIteratorFactory);

    wrapper.createArticleIterator("foomime", new MockArchivalUnit());
    MockArticleIteratorFactory mn = (MockArticleIteratorFactory)obj;
    assertEquals(ListUtil.list("foomime"), mn.args);
  }

  public void testLinkageError() throws IOException {
    ArticleIteratorFactory obj = new MockArticleIteratorFactory();
    ArticleIteratorFactory wrapper =
      (ArticleIteratorFactory)WrapperUtil.wrap(obj, ArticleIteratorFactory.class);
    assertTrue(wrapper instanceof ArticleIteratorFactoryWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper)
	       instanceof MockArticleIteratorFactory);
    Error err = new LinkageError("bar");
    MockArticleIteratorFactory a = (MockArticleIteratorFactory)obj;
    a.setError(err);
    try {
      wrapper.createArticleIterator(null, new MockArchivalUnit());
      fail("Should have thrown PluginException");
    } catch (PluginException e) {
      assertTrue(e instanceof PluginException.LinkageError);
    }
  }

  public static class MockArticleIteratorFactory
    implements ArticleIteratorFactory {

    List args;
    Error error;

    void setError(Error error) {
      this.error = error;
    }

      public Iterator createArticleIterator(String mimeType, ArchivalUnit au)
	throws PluginException {
      args = ListUtil.list(mimeType);
      if (error != null) {
	throw error;
      }
      return CollectionUtil.EMPTY_ITERATOR;
    }
  }
}
