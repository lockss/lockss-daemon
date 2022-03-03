/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.regex.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.servlet.*;
import sun.security.action.OpenFileInputStreamAction;

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
    "/* Enough random chars to push what follows over a buffer boundary in tests */\n" +
    "@import url(http://www.example.com/file1.css) /* abs link to rewrite */\n" +
    "@import url( http://auhost.com/ddd/phylum.css) /* 2nd host abs link to rewrite */\n" +
    "@import url(http://www.content.org/file2.css  ) /* abs link no rewrite */\n" +
    "@import url('http://www.content.org/file2.css') /* abs link no rewrite */\n" +
    "@import url(\"http://www.content.org/fi\\(le\\)2.css\") /* abs link no rewrite */\n" +
    "@import url(file3.css) /* rel link to rewrite */\n" +
    "@import url('bar/quote.css') /* rel link to rewrite */\n" +
    "@import url(\"bar/dquote.css\") /* rel link to rewrite */\n" +
    "@import url(../style/file4.css) /* rel link to rewrite */\n" +
    "@import url(/style/file5.css) /* site rel link to rewrite */\n" +
    "@import url('rel\\(dir\\)/f\\'il\\'e5.c ss') /* rel with quoting */\n" +
    "@import url(/style/cgi?foo=bar&a=b) /* query args to rewrite */\n" +
    "@import url()\n" +
    "@import file3.css; /* rel link to rewrite */\n" +
    "@import 'bar/quote.css'; /* rel link to rewrite */\n" +
    "@import \"bar/dquote.css\"; /* rel link to rewrite */\n" +
    "@import ../style/file4.css; /* rel link to rewrite */\n" +
    "@import /style/file5.css  ; /* site rel link to rewrite */\n" +
    "@import 'rel\\(dir\\)/f\\'il\\'e5.c ss'; /* rel with quoting */\n" +
    "@import ;\n" +
    "#banner, #top-banner {" +
    " padding: 0;\n" +
    " height: 120px;\n" +
    " background-image: url('../images/top_banner.gif');\n" +
    " background-fog: url(\"to/images/fog_banner.gif\");\n" +
    " background-fog: url(so/images/fog_banner.gif);\n" +
    " background-image: url('http://www.example.com/images/banner.gif');\n" +
    "}\n";

  private static final String xformed =
    "table.gallery {\n" +
    "border:  1px solid #cccccc;\n" +
    "margin:  2px;\n" +
    "padding: 2px;\n" +
    "background-color:#ffffff;\n" +
    "}\n" +
    "/* Enough random chars to push what follows over a buffer boundary in tests */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/file1.css') /* abs link to rewrite */\n" +
    "@import url( 'http://serve.host:1234/ServeContent?url=http://auhost.com/ddd/phylum.css') /* 2nd host abs link to rewrite */\n" +
    "@import url(http://www.content.org/file2.css  ) /* abs link no rewrite */\n" +
    "@import url('http://www.content.org/file2.css') /* abs link no rewrite */\n" +
    "@import url(\"http://www.content.org/fi\\(le\\)2.css\") /* abs link no rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/file3.css') /* rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/bar/quote.css') /* rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/bar/dquote.css') /* rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/style/file4.css') /* rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/style/file5.css') /* site rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/rel\\(dir\\)/f\\'il\\'e5.c%20ss') /* rel with quoting */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/style/cgi%3Ffoo%3Dbar%26a%3Db') /* query args to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/file.css')\n" +
    "@import 'http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/file3.css'; /* rel link to rewrite */\n" +
    "@import 'http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/bar/quote.css'; /* rel link to rewrite */\n" +
    "@import 'http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/bar/dquote.css'; /* rel link to rewrite */\n" +
    "@import 'http://serve.host:1234/ServeContent?url=http://www.example.com/dir/style/file4.css'; /* rel link to rewrite */\n" +
    "@import 'http://serve.host:1234/ServeContent?url=http://www.example.com/style/file5.css'  ; /* site rel link to rewrite */\n" +
    "@import 'http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/rel\\(dir\\)/f\\'il\\'e5.c%20ss'; /* rel with quoting */\n" +
    "@import 'http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/file.css';\n" +
    "#banner, #top-banner { padding: 0;\n" +
    " height: 120px;\n" +
    " background-image: url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/images/top_banner.gif');\n" +
    " background-fog: url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/to/images/fog_banner.gif');\n" +
    " background-fog: url('http://serve.host:1234/ServeContent?url=http://www.example.com/dir/path/so/images/fog_banner.gif');\n" +
    " background-image: url('http://serve.host:1234/ServeContent?url=http://www.example.com/images/banner.gif');\n" +
    "}\n";


  private static final String xformedEncoded =
    "table.gallery {\n" +
    "border:  1px solid #cccccc;\n" +
    "margin:  2px;\n" +
    "padding: 2px;\n" +
    "background-color:#ffffff;\n" +
    "}\n" +
    "/* Enough random chars to push what follows over a buffer boundary in tests */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Ffile1.css') /* abs link to rewrite */\n" +
    "@import url( 'http://serve.host:1234/ServeContent?url=http%3A%2F%2Fauhost.com%2Fddd%2Fphylum.css') /* 2nd host abs link to rewrite */\n" +
    "@import url(http://www.content.org/file2.css  ) /* abs link no rewrite */\n" +
    "@import url('http://www.content.org/file2.css') /* abs link no rewrite */\n" +
    "@import url(\"http://www.content.org/fi\\(le\\)2.css\") /* abs link no rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fpath%2Ffile3.css') /* rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fpath%2Fbar%2Fquote.css') /* rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fpath%2Fbar%2Fdquote.css') /* rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fstyle%2Ffile4.css') /* rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fstyle%2Ffile5.css') /* site rel link to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fpath%2Frel%28dir%29%2Ff%27il%27e5.c+ss') /* rel with quoting */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fstyle%2Fcgi%3Ffoo%3Dbar%26a%3Db') /* query args to rewrite */\n" +
    "@import url('http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fpath%2Ffile.css')\n" +
    "@import 'http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fpath%2Ffile3.css'; /* rel link to rewrite */\n" +
    "@import 'http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fpath%2Fbar%2Fquote.css'; /* rel link to rewrite */\n" +
    "@import 'http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fpath%2Fbar%2Fdquote.css'; /* rel link to rewrite */\n" +
    "@import 'http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fstyle%2Ffile4.css'; /* rel link to rewrite */\n" +
    "@import 'http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fstyle%2Ffile5.css'  ; /* site rel link to rewrite */\n" +
    "@import 'http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fpath%2Frel%28dir%29%2Ff%27il%27e5.c+ss'; /* rel with quoting */\n" +
    "@import 'http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fpath%2Ffile.css';\n" +
    "#banner, #top-banner { padding: 0;\n" +
    " height: 120px;\n" +
    " background-image: url('http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fimages%2Ftop_banner.gif');\n" +
    " background-fog: url('http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fpath%2Fto%2Fimages%2Ffog_banner.gif');\n" +
    " background-fog: url('http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fdir%2Fpath%2Fso%2Fimages%2Ffog_banner.gif');\n" +
    " background-image: url('http://serve.host:1234/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fimages%2Fbanner.gif');\n" +
    "}\n";

  public void setUp() throws Exception {
    super.setUp();
    au = new MockArchivalUnit();
    xform = new ServletUtil.LinkTransform() {
	public String rewrite(String url) {
	  return "http://serve.host:1234" + "/ServeContent?url=" + url;
	}
      };
    au.setUrlStems(stems);
    rclrf = new RegexpCssLinkRewriterFactory();
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

  void assertMatch(String exp, String str, int group) {
    Matcher m1 = RegexpCssLinkRewriterFactory.CSS_URL_PAT.matcher(str);
    assertTrue(m1.find());
    assertEquals(exp, m1.group(group));
  }

  public void testPat() throws Exception {
    assertMatch("http://foo/bar", "@import http://foo/bar;", 2);
    assertMatch("http://foo/bar", "@import   'http://foo/bar';", 2);
    assertMatch("http://foo/bar", "@import \"http://foo/bar\"  ;", 2);
    assertMatch("http://fo'o/bar", "@import \"http://fo'o/bar\";", 2);

    assertMatch("http://foo/bar", "@import url( http://foo/bar );", 2);
    assertMatch("http://foo/bar", "@import url('http://foo/bar');", 2);
    assertMatch("http://foo/bar", "@import url(\"http://foo/bar\");", 2);
    assertMatch("http://fo'o/bar", "@import url( \"http://fo'o/bar\" );", 2);

    assertMatch("http://foo/bar", "url(  http://foo/bar )", 2);
    assertMatch("http://foo/bar", "url('http://foo/bar')", 2);
    assertMatch("http://f o)o/bar", "url(\"http://f o)o/bar\")", 2);
    assertMatch("http://foo/b\"ar", "url('http://foo/b\"ar')", 2);
    assertMatch("http://foo/b'ar", "url(\"http://foo/b'ar\")", 2);

    // ensure match isn't greedy
    assertMatch("http://foo/bar",
		"a:url('http://foo/bar'); b:url('http://bar/foo')", 2);
  }

  public void testUrlEscape() {
    assertEquals("foo", rclrf.urlEscape("foo"));
    assertEquals("f\\ oo", rclrf.urlEscape("f oo"));
    assertEquals("f\\'o\\\"o", rclrf.urlEscape("f'o\"o"));
    assertEquals("f\\(o\\)o\\\\b", rclrf.urlEscape("f(o)o\\b"));
  }

  public void testRewritingMinimalEncoding() throws Exception {
    ConfigurationUtil.addFromArgs(RegexpCssLinkRewriterFactory.PARAM_URL_ENCODE,
				  "Minimal");
    InputStream is = rclrf.createLinkRewriter("text/css", au,
					       new StringInputStream(orig),
					       encoding, srcUrl, xform);
    String out = StringUtil.fromInputStream(is);
    log.debug3("Original:\n" + orig);
    log.debug3("Transformed:\n" + out);
    assertEquals(xformed, out);
  }

  public void testRewritingLargeDataUri() throws Exception {
    ConfigurationUtil.addFromArgs(RegexpCssLinkRewriterFactory.PARAM_URL_ENCODE,
        "Minimal");
    rclrf = new RegexpCssLinkRewriterFactory(2097152, 2097152 / 16 , 2097152);
    String large_data_uri = StringUtil.fromInputStream(getResourceAsStream("2MB_data_uri.css"));
    InputStream is = rclrf.createLinkRewriter("text/css", au,
        getResourceAsStream("2MB_data_uri.css"),
        encoding, srcUrl, xform);
    String out = StringUtil.fromInputStream(is);
    //log.debug3("Original:\n" + large_data_uri);
    //log.info("Transformed:\n" + out);
    assertEquals(large_data_uri, out);
  }

  public void testRewritingFullEncoding() throws Exception {
    ConfigurationUtil.addFromArgs(RegexpCssLinkRewriterFactory.PARAM_URL_ENCODE,
				  "Full");
    InputStream is = rclrf.createLinkRewriter("text/css", au,
					       new StringInputStream(orig),
					       encoding, srcUrl, xform);
    String out = StringUtil.fromInputStream(is);
    log.debug3("Original:\n" + orig);
    log.debug3("Transformed:\n" + out);
    assertEquals(xformedEncoded, out);
  }

  public void testBufferSize(int bufsize, int overlap) throws Exception {
    rclrf = new RegexpCssLinkRewriterFactory(bufsize, overlap);
    testRewritingMinimalEncoding();
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
