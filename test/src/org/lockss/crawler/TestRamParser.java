/*
 * $Id: TestRamParser.java,v 1.1 2004-03-01 23:21:35 troberts Exp $
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

package org.lockss.crawler;
import java.io.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestRamParser extends LockssTestCase {

  private RamParser parser = null;
  private MyFoundUrlCallback cb = null;

  public void setUp() throws Exception {
    super.setUp();
    MockArchivalUnit mau = new MockArchivalUnit();
    parser = new RamParser(mau);
    cb = new MyFoundUrlCallback();
  }

  public void testThrowsOnNullCachedUrl() throws IOException {
    try {
      parser.parseForUrls(null, new MyFoundUrlCallback());
      fail("Calling parseForUrls with a null CachedUrl should have thrown");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testThrowsOnNullCallback() throws IOException {
    try {
      parser.parseForUrls(new MockCachedUrl("http://www.example.com/"), null);
      fail("Calling parseForUrls with a null FoundUrlCallback should have thrown");
    } catch (IllegalArgumentException iae) {
    }
  }
  
  private Set parseSourceForUrls(String source) throws IOException {
    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com/blah.ram");
    mcu.setContent(source);

    parser.parseForUrls(mcu, cb);
    return cb.getFoundUrls();
  }

  public void testEmptyFileReturnsNoLinks() throws IOException {
    assertEquals(SetUtil.set(), parseSourceForUrls(""));
  }

  public void testSingleLink() throws IOException {
    assertEquals(SetUtil.set("http://www.example.com/blah.rm"),
		 parseSourceForUrls("http://www.example.com/blah.rm"));
  }

  public void testSingleLinkWithSpaces() throws IOException {
    assertEquals(SetUtil.set("http://www.example.com/blah.rm"),
		 parseSourceForUrls(" \t  http://www.example.com/blah.rm  "));
  }

  public void testBadLink() throws IOException {
    assertEquals(SetUtil.set(),
		 parseSourceForUrls("rtsp://www.example.com/blah.rm"));
  }

  public void testIgnoresCase() throws IOException {
    assertEquals(SetUtil.set("http://www.example.com/blah.rm"),
		 parseSourceForUrls("HTTP://www.example.com/blah.rm"));
  }

  public void testMultipleLinks() throws IOException {
    Set expected = SetUtil.set("http://www.example.com/blah.rm",
			       "http://www.example.com/blah2.rm",
			       "http://www.example.com/blah3.rm"); 
    Set actual = parseSourceForUrls("http://www.example.com/blah.rm\n"+
				    "http://www.example.com/blah2.rm\n\n"+
				    "http://www.example.com/blah3.rm\n");
    assertEquals(expected, actual);
  }

  private class MyFoundUrlCallback implements ContentParser.FoundUrlCallback {
    Set foundUrls = new HashSet();

    public void foundUrl(String url) {
      foundUrls.add(url);
    }

    public Set getFoundUrls() {
      return foundUrls;
    }
  }
}
