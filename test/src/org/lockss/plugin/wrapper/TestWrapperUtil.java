/*
 * $Id: TestWrapperUtil.java,v 1.2 2007-02-06 01:03:07 tlipkis Exp $
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

import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestWrapperUtil extends LockssTestCase {

  public void testNoWrap() throws PluginException {
    AnInterface obj = new AClass();
    AnInterface wrapper =
      (AnInterface)WrapperUtil.wrap(obj, AnInterface.class);
    assertEquals("foo0", wrapper.op("foo"));
    AClass a = (AClass)obj;
    assertEquals("foo", a.arg);
    assertTrue(wrapper instanceof AClass);
  }

  public void testWrap() throws PluginException {
    WrapperUtil.registerWrapperFactory(AnInterface.class,
				       new AWrapper.Factory());
    AnInterface obj = new AClass();
    AnInterface wrapper =
      (AnInterface)WrapperUtil.wrap(obj, AnInterface.class);
    assertTrue(wrapper instanceof AWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof AClass);

    assertEquals("foo0", wrapper.op("foo"));
    AClass a = (AClass)obj;
    assertEquals("foo", a.arg);
  }

  public void testLinkageError() {
    WrapperUtil.registerWrapperFactory(AnInterface.class,
				       new AWrapper.Factory());
    AnInterface obj = new AClass();
    AnInterface wrapper =
      (AnInterface)WrapperUtil.wrap(obj, AnInterface.class);
    assertTrue(wrapper instanceof AWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof AClass);
    Error err = new LinkageError("bar");
    AClass a = (AClass)obj;
    a.setError(err);
    try {
      wrapper.op("foo");
      fail("Should have thrown PluginException");
    } catch (PluginException e) {
      assertTrue(e instanceof PluginException.LinkageError);
    }
  }

  
  void assertRegistered(Class inter) {
    assertTrue(inter + " missing",
	       WrapperUtil.getWrapperFactories().get(inter) != null);
  }

  public void testReg() throws PluginException {
    assertRegistered(org.lockss.plugin.FilterFactory.class);
    assertRegistered(org.lockss.plugin.FilterRule.class);
    assertRegistered(org.lockss.plugin.UrlNormalizer.class);
    assertRegistered(org.lockss.extractor.LinkExtractor.class);
    assertRegistered(LoginPageChecker.class);
    assertRegistered(PermissionCheckerFactory.class);
    assertRegistered(org.lockss.plugin.definable.DefinableArchivalUnit.ConfigurableCrawlWindow.class);
    assertRegistered(org.lockss.util.urlconn.CacheResultHandler.class);
  }

  public interface AnInterface {
    String op(String arg) throws PluginException.LinkageError;
  }

  public static class AClass implements AnInterface {
    String arg;
    List args = new ArrayList();
    Error error;

    void setError(Error error) {
      this.error = error;
    }

    public String op(String arg) {
      this.arg = arg;
      args.add(arg);
      if (error != null) {
	throw error;
      }
      return arg + "0";
    }
  }

  public static class AWrapper implements AnInterface, PluginCodeWrapper {
    AnInterface inst;

    public AWrapper(AnInterface inst) {
      this.inst = inst;
    }

    public Object getWrappedObj() {
      return inst;
    }

    public String op(String arg) throws PluginException.LinkageError {
      try {
	return inst.op(arg);
      } catch (LinkageError e) {
	throw new PluginException.LinkageError(e);
      }
    }

    static class Factory implements WrapperFactory {
      public Object wrap(Object obj) {
	return new AWrapper((AnInterface)obj);
      }
    }
  }
}
