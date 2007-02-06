/*
 * $Id: TestLinkExtractorWrapper.java,v 1.1 2007-02-06 01:03:07 tlipkis Exp $
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
import org.lockss.crawler.*;
import org.lockss.test.*;
import org.lockss.extractor.*;
import org.lockss.util.*;

public class TestLinkExtractorWrapper extends LockssTestCase {

  public void testWrap() throws PluginException, IOException {
    LinkExtractor obj = new MockLinkExtractor();
    LinkExtractor wrapper =
      (LinkExtractor)WrapperUtil.wrap(obj, LinkExtractor.class);
    assertTrue(wrapper instanceof LinkExtractorWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MockLinkExtractor);

    InputStream in = new StringInputStream("foo");
    wrapper.extractUrls(null, in, null, "foo", null);
    MockLinkExtractor mn = (MockLinkExtractor)obj;
    assertEquals(ListUtil.list(null, in, null, "foo", null), mn.args);
  }

  public void testLinkageError() throws IOException {
    LinkExtractor obj = new MockLinkExtractor();
    LinkExtractor wrapper =
      (LinkExtractor)WrapperUtil.wrap(obj, LinkExtractor.class);
    assertTrue(wrapper instanceof LinkExtractorWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MockLinkExtractor);
    Error err = new LinkageError("bar");
    MockLinkExtractor a = (MockLinkExtractor)obj;
    a.setError(err);
    try {
      wrapper.extractUrls(null, null, null, "foo", null);
      fail("Should have thrown PluginException");
    } catch (PluginException e) {
      assertTrue(e instanceof PluginException.LinkageError);
    }
  }

  public static class MockLinkExtractor implements LinkExtractor {
    List args;
    Error error;

    void setError(Error error) {
      this.error = error;
    }

    public void extractUrls(ArchivalUnit au, InputStream in, String encoding,
			    String srcUrl, LinkExtractor.Callback cb)
	throws IOException {
      args = ListUtil.list(au, in, encoding, srcUrl, cb);
      if (error != null) {
	throw error;
      }
    }
  }
}
