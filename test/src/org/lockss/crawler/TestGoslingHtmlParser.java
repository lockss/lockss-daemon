/*
 * $Id: TestGoslingHtmlParser.java,v 1.1 2004-01-13 00:45:29 troberts Exp $
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
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.crawler.GoslingCrawlerImpl
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */
public class TestGoslingHtmlParser extends LockssTestCase {

  GoslingHtmlParser parser = null;

  public void setUp() throws Exception {
    super.setUp();
    MockArchivalUnit mau = new MockArchivalUnit();
    parser = new GoslingHtmlParser(mau);
  }

  public void testThrowsOnNullCachedUrl() throws IOException {
//     GoslingHtmlParser parser = new GoslingHtmlParser(new MockArchivalUnit());
    try {
      parser.parseForUrls(null, new HashSet(), null);
      fail("Calling parseForUrls with a null CachedUrl should have thrown");
    } catch (IllegalArgumentException iae) {
    } 
  }

  public void testThrowsOnNullSet() throws IOException {
//     GoslingHtmlParser parser = new GoslingHtmlParser(new MockArchivalUnit());
    try {
      parser.parseForUrls(new MockCachedUrl("http://www.example.com/"),
			  null, null);
      fail("Calling parseForUrls with a null Set should have thrown");
    } catch (IllegalArgumentException iae) {
    } 
  }

  public void testThrowsOnNullAu() {
    try {
      GoslingHtmlParser parser = new GoslingHtmlParser(null);
      fail("Trying to construct a GoslingHtmlParser with a null AU should throw");
    } catch (IllegalArgumentException iae) {
    }
  }


//   public void testParsesHref() throws IOException {
//     singleTagShouldParse("http://www.example.com/web_link.html",
// 			 "<a href=", "</a>");
//   }

//   private void singleTagShouldParse(String url,
// 				    String startTag, String endTag)
//       throws IOException {
//     MockCachedUrl mcu = new MockCachedUrl("http://www.example.com");
//     mcu.setContent(startTag+url+endTag);
    
//     Collection parsedUrls = new HashSet();
//     parser.parseForUrls(mcu, parsedUrls, new HashSet());

//     Set expected = SetUtil.set(url);
//     assertEquals(expected, parsedUrls);
//   }

}
