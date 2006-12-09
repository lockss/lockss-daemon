/*
 * $Id: TestLoginPageCheckerWrapper.java,v 1.1 2006-12-09 07:09:00 tlipkis Exp $
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
import org.lockss.util.*;

public class TestLoginPageCheckerWrapper extends LockssTestCase {

  public void testWrap() throws PluginException, IOException {
    LoginPageChecker obj = new MockLoginPageChecker();
    LoginPageChecker wrapper =
      (LoginPageChecker)WrapperUtil.wrap(obj, LoginPageChecker.class);
    assertTrue(wrapper instanceof LoginPageCheckerWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MockLoginPageChecker);
    Properties props = new Properties();
    Reader rdr = new StringReader("foo");
    assertTrue(wrapper.isLoginPage(props, rdr));
    MockLoginPageChecker mn = (MockLoginPageChecker)obj;
    assertEquals(ListUtil.list(props, rdr), mn.args);
  }

  public void testLinkageError() throws IOException {
    LoginPageChecker obj = new MockLoginPageChecker();
    LoginPageChecker wrapper =
      (LoginPageChecker)WrapperUtil.wrap(obj, LoginPageChecker.class);
    assertTrue(wrapper instanceof LoginPageCheckerWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MockLoginPageChecker);
    Error err = new LinkageError("bar");
    MockLoginPageChecker a = (MockLoginPageChecker)obj;
    a.setError(err);
    try {
      wrapper.isLoginPage(null, null);
      fail("Should have thrown PluginException");
    } catch (PluginException e) {
      assertTrue(e instanceof PluginException.LinkageError);
    }
  }

  public static class MockLoginPageChecker implements LoginPageChecker {
    List args;
    Error error;

    void setError(Error error) {
      this.error = error;
    }

    public boolean isLoginPage(Properties props, Reader reader)
	throws IOException {
      args = ListUtil.list(props, reader);
      if (error != null) {
	throw error;
      }
      return true;
    }
  }
}
