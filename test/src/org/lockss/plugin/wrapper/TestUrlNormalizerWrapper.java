/*
 * $Id: TestUrlNormalizerWrapper.java,v 1.1 2006-12-09 07:09:00 tlipkis Exp $
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

import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestUrlNormalizerWrapper extends LockssTestCase {

  public void testWrap() throws PluginException {
    UrlNormalizer obj = new MockUrlNormalizer();
    UrlNormalizer wrapper =
      (UrlNormalizer)WrapperUtil.wrap(obj, UrlNormalizer.class);
    assertTrue(wrapper instanceof UrlNormalizerWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MockUrlNormalizer);

    MockArchivalUnit mau = new MockArchivalUnit();
    assertEquals("foo0", wrapper.normalizeUrl("foo", mau));
    MockUrlNormalizer mn = (MockUrlNormalizer)obj;
    assertEquals(ListUtil.list("foo", mau), mn.args);
  }

  public void testLinkageError() {
    UrlNormalizer obj = new MockUrlNormalizer();
    UrlNormalizer wrapper =
      (UrlNormalizer)WrapperUtil.wrap(obj, UrlNormalizer.class);
    assertTrue(wrapper instanceof UrlNormalizerWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MockUrlNormalizer);
    Error err = new LinkageError("bar");
    MockUrlNormalizer a = (MockUrlNormalizer)obj;
    a.setError(err);
    try {
      wrapper.normalizeUrl("foo", new MockArchivalUnit());
      fail("Should have thrown PluginException");
    } catch (PluginException e) {
      assertTrue(e instanceof PluginException.LinkageError);
    }
  }

  public static class MockUrlNormalizer implements UrlNormalizer {
    List args;
    Error error;

    void setError(Error error) {
      this.error = error;
    }

    public String normalizeUrl(String url, ArchivalUnit au) {
      args = ListUtil.list(url, au);
      if (error != null) {
	throw error;
      }
      return url + "0";
    }
  }
}
