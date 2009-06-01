/*
 * $Id: TestNodeFilterHtmlLinkRewriterFactory.java,v 1.14 2009-06-01 07:33:05 tlipkis Exp $
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
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.servlet.*;
import java.util.*;
import java.io.*;

public class TestNodeFilterHtmlLinkRewriterFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestNodeFilterHtmlLinkRewriterFactory");

  private MockArchivalUnit au;
  private NodeFilterHtmlLinkRewriterFactory nfhlrf;
  private String encoding = Constants.DEFAULT_ENCODING;
  private static final String urlStem = "http://www.example.com/";
  private static final String urlSuffix = "content/index.html";
  private static final String url = urlStem + urlSuffix;

  private static final String orig =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "<title>example.com website</title>\n" +
    "<meta http-equiv=\"content-type\" " +
    "content=\"text/html; charset=ISO-8859-1\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1;url=http://www.example.com/page2.html\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1; url=http://www.example.com/page3.html\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1; \turl=http://www.example.com/page4.html\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1;url=page5.html\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1;url=../page6.html\">\n" +
    "</head>\n" +
    "<body>\n" +
    "<h1 align=\"center\">example.com website</h1>\n" +
    "<br>\n" +
    "<a href=\"http://www.example.com/content/index.html\">abs link</a>\n" +
    "<br>\n" +
    "<a href=\"path/index.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"4path/index.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"%2fpath/index.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"(content)/index.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"/more/path/index.html\">rel link</a>\n" +
    "<br>\n" +
    "<A HREF=\"../more/path/index.html\">rel link</A>\n" +
    "<br>\n" +
    "<A HREF=\"./more/path/index.html\">rel link</A>\n" +
    "<br>\n" +
    "<a href=\"?issn=123456789X\">rel query</a>\n" +
    "<br>\n" +
    "<a href=\"http://www.content.org/index.html\">abs link no rewrite</a>\n" +
    "<br>\n" +
    "Rel script" +
    "<script type=\"text/javascript\" src=\"/javascript/ajax/utility.js\"></script>\n" +
    "<br>\n" +
    "Abs script" +
    "<script type=\"text/javascript\" src=\"http://www.example.com/javascript/utility.js\"></script>\n" +
    "<br>\n" +
    "Rel stylesheet" +
    "<link rel=\"stylesheet\" href=\"/css/basic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel stylesheet" +
    "<link rel=\"stylesheet\" href=\"Basic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Abs stylesheet" +
    "<link rel=\"stylesheet\" href=\"http://www.example.com/css/extra.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel img" +
    "<img src=\"/icons/logo.gif\" alt=\"BMJ 1\" title=\"BMJ 1\" />\n" +
    "<br>\n" +
    "Abs img" +
    "<img src=\"http://www.example.com/icons/logo2.gif\" alt=\"BMJ 2\" title=\"BMJ 2\" />\n" +
    "<br>\n" +
    "Rel path CSS import" +
    "<style type=\"text/css\" media=\"screen,print\">@import url(css/common.css) @import url(common2.css);</style>\n" +
    "<br>\n" +
    "Rel CSS import" +
    "<style type=\"text/css\" media=\"screen,print\">@import url(/css/common.css) @import url(/css/common2.css);</style>\n" +
    "<br>\n" +
    "Abs CSS import" +
    "<style type=\"text/css\" media=\"screen,print\">@import url(http://www.example.com/css/extra.css) @import url(http://www.example.com/css/extra2.css);</style>\n" +
    "<br>\n" +
    "Mixed CSS import" +
    "<style type=\"text/css\" media=\"screen,print\">@import url(http://www.example.com/css/extra3.css) @import url(EXTRA4.css) @import url(../extra5.css);</style>\n" +
    "<br>\n" +
    "</body>\n" +
    "</HTML>\n";

  static String xformed =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "<title>example.com website</title>\n" +
    "<meta http-equiv=\"content-type\" content=\"text/html; charset=ISO-8859-1\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1;url=http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fpage2.html\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1; url=http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fpage3.html\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1; 	url=http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fpage4.html\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1;url=http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2Fpage5.html\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1;url=http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F..%2Fpage6.html\">\n" +
    "</head>\n" +
    "<body>\n" +
    "<h1 align=\"center\">example.com website</h1>\n" +
    "<br>\n" +
    "<a href=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2Findex.html\">abs link</a>\n" +
    "<br>\n" +
    "<a href=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2Fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F4path%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F%252fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F%28content%29%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fmore%2Fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<A HREF=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F..%2Fmore%2Fpath%2Findex.html\">rel link</A>\n" +
    "<br>\n" +
    "<A HREF=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F.%2Fmore%2Fpath%2Findex.html\">rel link</A>\n" +
    "<br>\n" +
    "<a href=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F%3Fissn%3D123456789X\">rel query</a>\n" +
    "<br>\n" +
    "<a href=\"http://www.content.org/index.html\">abs link no rewrite</a>\n" +
    "<br>\n" +
    "Rel script<script type=\"text/javascript\" src=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fjavascript%2Fajax%2Futility.js\"></script>\n" +
    "<br>\n" +
    "Abs script<script type=\"text/javascript\" src=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fjavascript%2Futility.js\"></script>\n" +
    "<br>\n" +
    "Rel stylesheet<link rel=\"stylesheet\" href=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fbasic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel stylesheet<link rel=\"stylesheet\" href=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2FBasic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Abs stylesheet<link rel=\"stylesheet\" href=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fextra.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel img<img src=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Ficons%2Flogo.gif\" alt=\"BMJ 1\" title=\"BMJ 1\" />\n" +
    "<br>\n" +
    "Abs img<img src=\"http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Ficons%2Flogo2.gif\" alt=\"BMJ 2\" title=\"BMJ 2\" />\n" +
    "<br>\n" +
    "Rel path CSS import<style type=\"text/css\" media=\"screen,print\">@import url(http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2Fcss%2Fcommon.css) @import url(http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2Fcommon2.css);</style>\n" +
    "<br>\n" +
    "Rel CSS import<style type=\"text/css\" media=\"screen,print\">@import url(http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fcommon.css) @import url(http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fcommon2.css);</style>\n" +
    "<br>\n" +
    "Abs CSS import<style type=\"text/css\" media=\"screen,print\">@import url(http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fextra.css) @import url(http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fextra2.css);</style>\n" +
    "<br>\n" +
    "Mixed CSS import<style type=\"text/css\" media=\"screen,print\">@import url(http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fextra3.css) @import url(http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2FEXTRA4.css) @import url(http://quark:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F..%2Fextra5.css);</style>\n" +
    "<br>\n" +
    "</body>\n" +
    "</HTML>\n";

  private ServletUtil.LinkTransform xform = null;
  private String testPort = "9524";

  public void setUp() throws Exception {
    super.setUp();
    au = new MockArchivalUnit();
    xform = new ServletUtil.LinkTransform() {
	public String rewrite(String url) {
	  return "http://" + PlatformUtil.getLocalHostname() +
	    ":" + testPort + "/ServeContent?url=" + url;
	}
      };
    List l = new ArrayList();
    l.add(urlStem);
    au.setUrlStems(l);
    nfhlrf = new NodeFilterHtmlLinkRewriterFactory();
  }

  public void testThrowsIfNotHtml() {
    Reader in = new StringReader(orig);
    try {
      Reader r = nfhlrf.createLinkRewriterReader("application/pdf", au, in,
						  encoding, url, xform);
      fail("createLinkRewriter should have thrown on non-html mime type");
    } catch (Exception ex) {
      if (ex instanceof PluginException) {
	return;
      }
      fail("createLinkRewriter should have thrown PluginException but threw " +
	   ex.toString());
    }
  }

  public void testRewriting() throws Exception {
    Reader r = nfhlrf.createLinkRewriterReader("text/html",
					       au,
					       new StringReader(orig),
					       encoding, url, xform);
    String out = StringUtil.fromReader(r);
    log.debug3("Original:\n" + orig);
    log.debug3("Transformed:\n" + out);
    assertEquals(xformed, out);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {
      TestNodeFilterHtmlLinkRewriterFactory.class.getName()
    };
    junit.textui.TestRunner.main(testCaseList);
  }

}
