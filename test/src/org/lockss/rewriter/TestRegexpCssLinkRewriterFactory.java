/*
 * $Id: TestRegexpCssLinkRewriterFactory.java,v 1.1.2.2 2011-02-14 00:16:49 tlipkis Exp $
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

package org.lockss.rewriter;

import java.io.*;
import java.net.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.servlet.*;

public class TestRegexpCssLinkRewriterFactory extends LockssTestCase {
  static Logger log =
      Logger.getLogger("TestRegexpCssLinkRewriterFactory");

  private MockArchivalUnit au;
  private RegexpCssLinkRewriterFactory rclrf;
  private String encoding = null;

  private InputStream in;
  private ServletUtil.LinkTransform xform = null;
  static final String srcUrl = "http://www.example.com/dir/path/file.css";

  static List stems = ListUtil.list("http://www.example.com/",
				    "http://auhost.com/");

  private static final String orig =
    "table.gallery {\n" +
      "border:  1px solid #cccccc;\n" +
      "margin:  2px;\n" +
      "padding: 2px;\n" +
      "background-color:#ffffff;\n" +
    "}\n" +
      /* "Some random crap\n" + */
      /* "Much more random crap then the earlier tests used" + */
    "@import url(http://www.example.com/file1.css) /* abs link to rewrite */\n" +
    "@import url(http://auhost.com/ddd/phylum.css) /* 2nd host abs link to rewrite */\n" +
    "@import url(http://www.content.org/file2.css) /* abs link no rewrite */\n" +
    "@import url('http://www.content.org/file2.css') /* abs link no rewrite */\n" +
    "@import url(\"http://www.content.org/fi\\(le\\)2.css\") /* abs link no rewrite */\n" +
    "@import url(file3.css) /* rel link to rewrite */\n" +
    "@import url('bar/quote.css') /* rel link to rewrite */\n" +
    "@import url(\"bar/dquote.css\") /* rel link to rewrite */\n" +
    "@import url(../style/file4.css) /* rel link to rewrite */\n" +
    "@import url(/style/file5.css) /* site rel link to rewrite */\n" +
    "@import url('rel\\(dir\\)/f\\'il\\'e5.c ss') /* rel with quoting */\n" +
    "@import url()\n" +
    "#banner, #top-banner {" +
    " padding: 0;\n" +
    " height: 120px;\n" +
    " background-image: url('../images/top_banner.gif');\n" +
    " background-fog: url('to/images/fog_banner.gif');\n" +
    " background-image: url('http://www.example.com/images/banner.gif');\n" +
    "}\n";

  private static final String xformed =
    "table.gallery {\n" +
    "border:  1px solid #cccccc;\n" +
    "margin:  2px;\n" +
    "padding: 2px;\n" +
    "background-color:#ffffff;\n" +
    "}\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/file1.css') /* abs link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://auhost.com/ddd/phylum.css') /* 2nd host abs link to rewrite */\n" +
    "@import url(http://www.content.org/file2.css) /* abs link no rewrite */\n" +
    "@import url('http://www.content.org/file2.css') /* abs link no rewrite */\n" +
    "@import url(\"http://www.content.org/fi\\(le\\)2.css\") /* abs link no rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/file3.css') /* rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/bar/quote.css') /* rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/bar/dquote.css') /* rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/style/file4.css') /* rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/style/file5.css') /* site rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/rel\\(dir\\)/f\\'il\\'e5.c%20ss') /* rel with quoting */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/file.css')\n" +
    "#banner, #top-banner { padding: 0;\n" +
    " height: 120px;\n" +
    " background-image: url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/images/top_banner.gif');\n" +
    " background-fog: url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/to/images/fog_banner.gif');\n" +
    " background-image: url('http://serve.host:1234/ServeContent?url=http://www.example.com/images/banner.gif');\n" +
    "}\n";


  public void setUp() throws Exception {
    super.setUp();
    au = new MockArchivalUnit();
    xform = new ServletUtil.LinkTransform() {
	public String rewrite(String url) {
	  return "http://serve.host:1234" + "/ServeContent?url=" + url;
	}
      };
//     List l = new ArrayList();
//     l.add(urlStem);
//     au.setUrlStems(l);
    au.setUrlStems(stems);
    rclrf = new RegexpCssLinkRewriterFactory();
  }

  public void testThrowsIfNotCss() {
    in = new StringInputStream("foo");
    try {
      rclrf.createLinkRewriter("text/html", au, in,
				encoding, srcUrl, xform);
      fail("createLinkRewriter should have thrown on non-css mime type");
    } catch (Exception ex) {
      if (ex instanceof PluginException) {
	return;
      }
      fail("createLinkRewriter should have thrown PluginException but threw " +
	   ex.toString());
    }
  }

  public void testMalformedUrl() throws Exception {
    in = new StringInputStream("bar");
    try {
      rclrf.createLinkRewriter("text/css", au, in,
				encoding, "garbage://xx.foo.bar/123", xform);
      fail("Should have thrown MalformedURLException");
    } catch (IllegalArgumentException ex) {
    }
  }

  private static final String CSS_ESCAPE_CHARS = "\\() '\"";

  public void testUrlEscape() {
    assertEquals("foo", rclrf.urlEscape("foo"));
    assertEquals("f\\ oo", rclrf.urlEscape("f oo"));
    assertEquals("f\\'o\\\"o", rclrf.urlEscape("f'o\"o"));
    assertEquals("f\\(o\\)o\\\\b", rclrf.urlEscape("f(o)o\\b"));
  }

  public void testRewriting() throws Exception {
    InputStream is = rclrf.createLinkRewriter("text/css", au,
					       new StringInputStream(orig),
					       encoding, srcUrl, xform);
    String out = StringUtil.fromInputStream(is);
    log.debug3("Original:\n" + orig);
    log.debug3("Transformed:\n" + out);
    assertEquals(xformed, out);
  }


  public void testBufferSize(int bufsize, int overlap) throws Exception {
    rclrf = new RegexpCssLinkRewriterFactory(bufsize, overlap);
    testRewriting();
   }

   public void testBuffer1() throws Exception {
     testBufferSize(200, 100);
   }

   public void testBuffer2() throws Exception {
     testBufferSize(200, 50);
   }

   public void testBuffer3() throws Exception {
     testBufferSize(100, 50);
   }

   public void testBuffer4() throws Exception {
     testBufferSize(80, 30);
   }
}
