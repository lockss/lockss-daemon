/*
 * $Id: TestRamLinkExtractor.java,v 1.1 2007-02-06 00:37:58 tlipkis Exp $
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

package org.lockss.extractor;

import java.io.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

public class TestRamLinkExtractor extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private RamLinkExtractor extractor = null;
  private MyFoundUrlCallback cb = null;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    extractor = RamLinkExtractor.makeBasicRamLinkExtractor();
    cb = new MyFoundUrlCallback();
  }

  public void testThrows() throws IOException {
    try {
      extractor.extractUrls(mau, null, ENC, "http://www.example.com/",
			    new MyFoundUrlCallback());
      fail("Calling extractUrls with a null InputStream should have thrown");
    } catch (IllegalArgumentException iae) {
    }
    try {
      extractor.extractUrls(mau, new StringInputStream("blah"), ENC,
			    "http://www.example.com/", null);
      fail("Calling extractUrls with a null callback should have thrown");
    } catch (IllegalArgumentException iae) {
    }
  }

  private Set parseSourceForUrls(String source) throws IOException {
    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com/blah.ram");
    mcu.setContent(source);

    String enc = Constants.DEFAULT_ENCODING;

    extractor.extractUrls(new MockArchivalUnit(),
			  mcu.getUnfilteredInputStream(),
			  enc, mcu.getUrl(), cb);
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

  //verify that we don't fetch links that start with rtsp:// by default
  public void testBadLink() throws IOException {
    assertEquals(SetUtil.set(),
		 parseSourceForUrls("rtsp://www.example.com/blah.rm"));
  }

  public void testIgnoresCaseInProtocol() throws IOException {
    assertEquals(SetUtil.set("http://www.example.com/blah.rm"),
		 parseSourceForUrls("HTTP://www.example.com/blah.rm"));
  }

  public void testIgnoresCaseInHost() throws IOException {
    assertEquals(SetUtil.set("http://www.EXAMPLE.com/blah.rm"),
		 parseSourceForUrls("http://www.EXAMPLE.com/blah.rm"));
  }
  public void testDoesntIgnoreCaseInPath() throws IOException {
    assertEquals(SetUtil.set("http://www.example.com/BLAH.rm"),
		 parseSourceForUrls("http://www.example.com/BLAH.rm"));
  }

  public void testIgnoresComments() throws IOException {
    Set expected = SetUtil.set("http://www.example.com/blah.rm",
			       "http://www.example.com/blah3.rm");
    Set actual = parseSourceForUrls("http://www.example.com/blah.rm\n"+
				    "#http://www.example.com/blah2.rm\n\n"+
				    "http://www.example.com/blah3.rm\n");
    assertEquals(expected, actual);
  }

  public void testStripsParams() throws IOException {
    Set expected = SetUtil.set("http://www.example.com/blah.rm",
			       "http://www.example.com/blah3.rm");
    Set actual =
      parseSourceForUrls("http://www.example.com/blah.rm?blah=blah\n"+
			 "http://www.example.com/blah3.rm?blah2=blah2&blah3=asdf\n");
    assertEquals(expected, actual);
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

  public void testTranslateUrls() throws IOException {
    extractor =
      RamLinkExtractor.makeTranslatingRamLinkExtractor("rtsp://www.example.com/",
						       "http://www.example.com/media/");

    Set expected = SetUtil.set("http://www.example.com/media/blah.rm",
			       "http://www.example.com/blah2.rm",
			       "http://www.example.com/media/blah3.rm");
    Set actual = parseSourceForUrls("rtsp://www.example.com/blah.rm\n"+
				    "http://www.example.com/blah2.rm\n\n"+
				    "rtsp://www.example.com/blah3.rm\n");
    assertEquals(expected, actual);
  }

  public void testTranslateUrlsHandlesCase() throws IOException {
    extractor =
      RamLinkExtractor.makeTranslatingRamLinkExtractor("rtsp://www.example.com/",
						       "http://www.example.com/media/");

    Set expected = SetUtil.set("http://www.example.com/media/blah.rm",
			       "http://www.example.com/media/blah2.rm",
			       "http://www.example.com/media/blah3.rm");
    Set actual = parseSourceForUrls("rtsp://www.example.com/blah.rm\n"+
				    "rtsp://www.EXAMPLE.com/blah2.rm\n\n"+
				    "RTSP://www.example.com/blah3.rm\n");
    assertEquals(expected, actual);
  }

  private class MyFoundUrlCallback implements LinkExtractor.Callback {
    Set foundUrls = new HashSet();

    public void foundUrl(String url) {
      foundUrls.add(url);
    }

    public Set getFoundUrls() {
      return foundUrls;
    }
  }
}
