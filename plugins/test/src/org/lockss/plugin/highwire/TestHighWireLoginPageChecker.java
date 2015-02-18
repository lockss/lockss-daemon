/*
 * $Id$
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
package org.lockss.plugin.highwire;
import java.io.*;

import org.lockss.test.*;
import org.lockss.util.*;

public class TestHighWireLoginPageChecker extends LockssTestCase {

  public void testPropsParam() throws IOException {
    HighWireLoginPageChecker checker = new HighWireLoginPageChecker();
    try {
      checker.isLoginPage(null, new MyStringReader("blah"));
      fail("Calling isLoginPage w/ null CIProps should throw");
    } catch (NullPointerException ex) {
    }
  }

  public void testReaderParam() throws IOException {
    HighWireLoginPageChecker checker = new HighWireLoginPageChecker();
    try {
      checker.isLoginPage(new CIProperties(), null);
      fail("Calling isLoginPage w/ null reader should throw");
    } catch (NullPointerException ex) {
    }
  }

  public void testNotLoginPage() throws IOException {
    HighWireLoginPageChecker checker = new HighWireLoginPageChecker();
    assertFalse(checker.isLoginPage(new CIProperties(),
				    new MyStringReader("blah")));
  }

  public static String loginPageText =
    "This is a login page, blah blah\n"+
    "<!-- login page comment for LOCKSS-->\n"+
    "More random text...";

  public static String notLoginPageText =
    "This is not a login page, blah blah\n"+
    "More random text...";

  public void testHasIsLoginPage() throws IOException {
    HighWireLoginPageChecker checker = new HighWireLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Cache-Control", "no-store");

    MyStringReader reader = new MyStringReader(loginPageText);

    assertTrue(checker.isLoginPage(props, reader));
  }

  public void testHasIsLoginPageDiffCase() throws IOException {
    HighWireLoginPageChecker checker = new HighWireLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Cache-Control", "No-Store");

    MyStringReader reader = new MyStringReader(loginPageText);

    assertTrue(checker.isLoginPage(props, reader));
  }


  public void testHasNoCacheHeader() throws IOException {
    HighWireLoginPageChecker checker = new HighWireLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Cache-Control", "no-store");

    MyStringReader reader = new MyStringReader(notLoginPageText);

    assertFalse(checker.isLoginPage(props, reader));
  }

  public void testHasDifferentCacheControlHeader() throws IOException {
    HighWireLoginPageChecker checker = new HighWireLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Cache-Control", "blah");

    MyStringReader reader = new MyStringReader(notLoginPageText);

    assertFalse(checker.isLoginPage(props, reader));
    assertFalse(reader.readWasCalled());
  }

  public void testHasDifferentCacheControlHeaderButLoginPage()
      throws IOException {
    HighWireLoginPageChecker checker = new HighWireLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Cache-Control", "blah");

    MyStringReader reader = new MyStringReader(loginPageText);

    assertFalse(checker.isLoginPage(props, reader));
    assertFalse(reader.readWasCalled());
  }

  //other cache-control header
  //doesn't check reader if not header

  private static class MyStringReader extends StringReader {
    boolean readCalled = false;

    public MyStringReader(String str) {
      super(str);
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
      readCalled = true;
      return super.read(cbuf, off, len);
    }

    public boolean readWasCalled() {
      return readCalled;
    }
  }
}
