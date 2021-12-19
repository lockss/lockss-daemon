/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
