/*
 * $Id: TestExploderHelperWrapper.java,v 1.1 2007-09-29 12:42:32 dshr Exp $
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

public class TestExploderHelperWrapper extends LockssTestCase {

  public void testWrap() throws PluginException {
    ExploderHelper obj = new MockExploderHelper();
    ExploderHelper wrapper =
      (ExploderHelper)WrapperUtil.wrap(obj, ExploderHelper.class);
    assertTrue(wrapper instanceof ExploderHelperWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MockExploderHelper);

    ArchiveEntry ae = new ArchiveEntry("foo", 1, 0, null, null);
    wrapper.process(ae);
    MockExploderHelper mn = (MockExploderHelper)obj;
    assertEquals(ListUtil.list(ae), mn.args);
  }

  public void testLinkageError() {
    ExploderHelper obj = new MockExploderHelper();
    ExploderHelper wrapper =
      (ExploderHelper)WrapperUtil.wrap(obj, ExploderHelper.class);
    assertTrue(wrapper instanceof ExploderHelperWrapper);
    assertTrue(WrapperUtil.unwrap(wrapper) instanceof MockExploderHelper);
    Error err = new LinkageError("bar");
    MockExploderHelper a = (MockExploderHelper)obj;
    a.setError(err);
    ExploderHelperWrapper ehw = (ExploderHelperWrapper)wrapper;
    try {
	ehw.process(new ArchiveEntry("foo", 1, 0, null, null));
      fail("Should have thrown PluginException");
    } catch (Throwable e) {
      assertTrue(e instanceof PluginException.LinkageError);
    }
  }

  public static class MockExploderHelper implements ExploderHelper {
      List args;
    Error error;

    void setError(Error error) {
      this.error = error;
    }

      public void process(ArchiveEntry ae) {
	  args = ListUtil.list(ae);
	  if (error != null) {
	      throw error;
	  }
      }
  }
}
