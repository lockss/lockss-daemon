/*
 * $Id: TestCacheResultHandlerWrapper.java,v 1.4 2010-02-11 10:05:40 tlipkis Exp $
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
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

public class TestCacheResultHandlerWrapper extends LockssTestCase {

  public void testWrapInit() throws PluginException, IOException {
    CacheResultHandler obj = new MockCacheResultHandler();
    CacheResultHandler wrapper =
      (CacheResultHandler)WrapperUtil.wrap(obj, CacheResultHandler.class);
    assertTrue(wrapper instanceof CacheResultHandlerWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MockCacheResultHandler);

    wrapper.init(null);
    MockCacheResultHandler mn = (MockCacheResultHandler)obj;
    assertEquals(ListUtil.list((Object)null), mn.args);
  }

  public void testWrapHandleResult() throws PluginException, IOException {
    CacheResultHandler obj = new MockCacheResultHandler();
    CacheResultHandler wrapper =
      (CacheResultHandler)WrapperUtil.wrap(obj, CacheResultHandler.class);
    assertTrue(wrapper instanceof CacheResultHandlerWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MockCacheResultHandler);

    String url = "foourl";
    wrapper.handleResult(null, url, 5);
    MockCacheResultHandler mn = (MockCacheResultHandler)obj;
    assertEquals(ListUtil.list(new Integer(5), url), mn.args);
  }

  public void testLinkageErrorInit() throws IOException {
    CacheResultHandler obj = new MockCacheResultHandler();
    CacheResultHandler wrapper =
      (CacheResultHandler)WrapperUtil.wrap(obj, CacheResultHandler.class);
    assertTrue(wrapper instanceof CacheResultHandlerWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MockCacheResultHandler);
    Error err = new LinkageError("bar");
    MockCacheResultHandler a = (MockCacheResultHandler)obj;
    a.setError(err);
    try {
      wrapper.init(null);
      fail("Should have thrown PluginException");
    } catch (PluginException e) {
      assertTrue(e instanceof PluginException.LinkageError);
    }
  }

  public void testLinkageErrorHandleResult() throws IOException {
    CacheResultHandler obj = new MockCacheResultHandler();
    CacheResultHandler wrapper =
      (CacheResultHandler)WrapperUtil.wrap(obj, CacheResultHandler.class);
    assertTrue(wrapper instanceof CacheResultHandlerWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MockCacheResultHandler);
    Error err = new LinkageError("bar");
    MockCacheResultHandler a = (MockCacheResultHandler)obj;
    a.setError(err);
    try {
      wrapper.handleResult(null, null, 3);
      fail("Should have thrown PluginException");
    } catch (PluginException e) {
      assertTrue(e instanceof PluginException.LinkageError);
    }
  }

  public static class MockCacheResultHandler implements CacheResultHandler {
    List args;
    Error error;

    void setError(Error error) {
      this.error = error;
    }

    public void init(CacheResultMap map) {
      args = ListUtil.list(map);
      if (error != null) {
	throw error;
      }
    }

    public CacheException handleResult(ArchivalUnit au,
				       String url,
				       int code) {
      args = ListUtil.list(new Integer(code), url);
      if (error != null) {
	throw error;
      }
      return new CacheException("bar");
    }

    public CacheException handleResult(ArchivalUnit au,
				       String url,
				       Exception ex) {
      args = ListUtil.list(ex, url);
      if (error != null) {
	throw error;
      }
      return new CacheException("bar");
    }
  }
}
