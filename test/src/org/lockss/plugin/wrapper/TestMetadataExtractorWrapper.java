/*
 * $Id: TestMetadataExtractorWrapper.java,v 1.1 2009-05-22 19:14:55 dshr Exp $
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

public class TestMetadataExtractorWrapper extends LockssTestCase {

  private static final String url = "http://www.example.com/";
  public void testWrap() throws PluginException, IOException {
    MetadataExtractor obj = new MockMetadataExtractor();
    MetadataExtractor wrapper =
      (MetadataExtractor)WrapperUtil.wrap(obj, MetadataExtractor.class);
    assertTrue(wrapper instanceof MetadataExtractorWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MockMetadataExtractor);

    CachedUrl cu = new MockCachedUrl(url);
    wrapper.extract(cu);
    MockMetadataExtractor mn = (MockMetadataExtractor)obj;
    assertEquals(ListUtil.list(cu), mn.args);
  }

  public void testLinkageError() throws IOException {
    MetadataExtractor obj = new MockMetadataExtractor();
    MetadataExtractor wrapper =
      (MetadataExtractor)WrapperUtil.wrap(obj, MetadataExtractor.class);
    assertTrue(wrapper instanceof MetadataExtractorWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MockMetadataExtractor);
    Error err = new LinkageError("bar");
    MockMetadataExtractor a = (MockMetadataExtractor)obj;
    a.setError(err);
    CachedUrl cu = new MockCachedUrl(url);
    try {
      wrapper.extract(cu);
      fail("Should have thrown PluginException");
    } catch (PluginException e) {
      assertTrue(e instanceof PluginException.LinkageError);
    }
  }

  public static class MockMetadataExtractor implements MetadataExtractor {
    List args;
    Error error;

    void setError(Error error) {
      this.error = error;
    }

    public Metadata extract(CachedUrl cu)
	throws IOException {
      args = ListUtil.list(cu);
      if (error != null) {
	throw error;
      }
      return new Metadata(new Properties());
    }
  }
}
