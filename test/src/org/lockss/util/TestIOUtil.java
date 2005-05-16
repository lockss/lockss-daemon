/*
 * $Id: TestIOUtil.java,v 1.1 2005-05-16 21:35:53 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.*;
import java.io.*;
import junit.framework.TestCase;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.util.IOUtil
 */
public class TestIOUtil extends LockssTestCase {

  // XXX Need to define mock objects to test other variants

  public void testSafeCloseInputStream() throws IOException {
    IOUtil.safeClose((InputStream)null);
    MockInputStream is = new MockInputStream();
    assertFalse(is.isClosed());
    IOUtil.safeClose(is);
    assertTrue(is.isClosed());
    IOUtil.safeClose(is);
    assertTrue(is.isClosed());
    MyMockInputStream mmis = new MyMockInputStream();
    try {
      mmis.close();
      fail("MMIS.close() should throw");
    } catch (IOException e) {
    }
    IOUtil.safeClose(mmis);
  }

  class MyMockInputStream extends MockInputStream {
    public void close() throws IOException {
      throw new ExpectedIOException("test exception");
    }
  }
}
