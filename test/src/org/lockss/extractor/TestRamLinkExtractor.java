/*
 * $Id: TestRamLinkExtractor.java,v 1.4 2008-09-09 07:53:07 tlipkis Exp $
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
import junit.framework.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

public abstract class TestRamLinkExtractor extends LinkExtractorTestCase {


  public String getMimeType() {
    return MIME_TYPE_RAM;
  }

  public static class TestSimple extends TestRamLinkExtractor {
    public LinkExtractorFactory getFactory() {
      return new LinkExtractorFactory() {
	  public LinkExtractor createLinkExtractor(String mimeType) {
	    return RamLinkExtractor.makeBasicRamLinkExtractor();
	  }
	};
    }

    public void testSingleLink() throws Exception {
      assertEquals(SetUtil.set("http://www.example.com/blah.rm"),
		   extractUrls("http://www.example.com/blah.rm"));
    }

    public void testSingleLinkWithSpaces() throws Exception {
      assertEquals(SetUtil.set("http://www.example.com/blah.rm"),
		   extractUrls(" \t  http://www.example.com/blah.rm  "));
    }

    //verify that we don't fetch links that start with rtsp:// by default
    public void testBadLink() throws Exception {
      assertEquals(SetUtil.set(),
		   extractUrls("rtsp://www.example.com/blah.rm"));
    }

    public void testIgnoresCaseInProtocol() throws Exception {
      assertEquals(SetUtil.set("http://www.example.com/blah.rm"),
		   extractUrls("HTTP://www.example.com/blah.rm"));
    }

    public void testIgnoresCaseInHost() throws Exception {
      assertEquals(SetUtil.set("http://www.EXAMPLE.com/blah.rm"),
		   extractUrls("http://www.EXAMPLE.com/blah.rm"));
    }

    public void testDoesntIgnoreCaseInPath() throws Exception {
      assertEquals(SetUtil.set("http://www.example.com/BLAH.rm"),
		   extractUrls("http://www.example.com/BLAH.rm"));
    }

    public void testIgnoresComments() throws Exception {
      Set expected = SetUtil.set("http://www.example.com/blah.rm",
				 "http://www.example.com/blah3.rm");
      Collection actual = extractUrls("http://www.example.com/blah.rm\n"+
				      "#http://www.example.com/blah2.rm\n\n"+
				      "http://www.example.com/blah3.rm\n");
      assertEquals(expected, SetUtil.theSet(actual));
    }

    public void testStripsParams() throws Exception {
      Set expected = SetUtil.set("http://www.example.com/blah.rm",
				 "http://www.example.com/blah3.rm");
      Collection actual =
	extractUrls("http://www.example.com/blah.rm?blah=blah\n"+
		    "http://www.example.com/blah3.rm?blah2=blah2&blah3=asdf\n");
      assertEquals(expected, SetUtil.theSet(actual));
    }

    public void testMultipleLinks() throws Exception {
      Set expected = SetUtil.set("http://www.example.com/blah.rm",
				 "http://www.example.com/blah2.rm",
				 "http://www.example.com/blah3.rm");
      Collection actual = extractUrls("http://www.example.com/blah.rm\n"+
				      "http://www.example.com/blah2.rm\n\n"+
				      "http://www.example.com/blah3.rm\n");
      assertEquals(expected, SetUtil.theSet(actual));
    }
  }

  public static class TestTranslating extends TestRamLinkExtractor {
    static String FROM_URL = "rtsp://www.example.com/";
    static String TO_URL = "http://www.example.com/media/";

    public LinkExtractorFactory getFactory() {
      return new LinkExtractorFactory() {
	  public LinkExtractor createLinkExtractor(String mimeType) {
	    return
	      RamLinkExtractor.makeTranslatingRamLinkExtractor(FROM_URL,
							       TO_URL);
	  }
	};
    }

    public void testTranslateUrls() throws Exception {
      Set expected = SetUtil.set("http://www.example.com/media/blah.rm",
				 "http://www.example.com/blah2.rm",
				 "http://www.example.com/media/blah3.rm");
      Collection actual = extractUrls("rtsp://www.example.com/blah.rm\n"+
				      "http://www.example.com/blah2.rm\n\n"+
				      "rtsp://www.example.com/blah3.rm\n");
      assertEquals(expected, SetUtil.theSet(actual));
    }

    public void testTranslateUrlsHandlesCase() throws Exception {
      Set expected = SetUtil.set("http://www.example.com/media/blah.rm",
				 "http://www.example.com/media/blah2.rm",
				 "http://www.example.com/media/blah3.rm");
      Collection actual = extractUrls("rtsp://www.example.com/blah.rm\n"+
				      "rtsp://www.EXAMPLE.com/blah2.rm\n\n"+
				      "RTSP://www.example.com/blah3.rm\n");
      assertEquals(expected, SetUtil.theSet(actual));
    }
  }

  public static Test suite() {
    return variantSuites(new Class[] {TestSimple.class,
				      TestTranslating.class});
  }
}
