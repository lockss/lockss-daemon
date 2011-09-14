/*
 * $Id: TestNodeFilterHtmlLinkRewriterFactory.java,v 1.21 2011-09-14 05:03:07 tlipkis Exp $
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
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.servlet.*;
import org.lockss.rewriter.RegexpCssLinkRewriterFactory.CssLinkRewriterUrlEncodeMode;

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
    "<meta http-equiv=\"refresh\" " +
    "content=\"1;url=http://www.content.org/page2.html\">\n" +
    "</head>\n" +
    "<body>\n" +
    "<h1 align=\"center\">example.com website</h1>\n" +
    "<br>\n" +
    "<a href=\"http://www.example.com/content/index.html\">abs link</a>\n" +
    "<br>\n" +
    "<a href=\"http://www.example.com/content/index.html#ref1\">abs link with ref</a>\n" +
    "<br>\n" +
    "<a href=\"path/index.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"path/index.html#ref2\">rel link with ref</a>\n" +
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
    "Rel style" +
    "<style type=\"text/css\" src=\"/css/utility.css\"></style>\n" +
    "<br>\n" +
    "Abs style" +
    "<style type=\"text/css\" src=\"http://www.example.com/css/utility.css\"></style>\n" +
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

    // repeat above after changing the base URL

    "<base href=\"http://www.example.com/otherdir/\" />\n" +
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
    "Rel style" +
    "<style type=\"text/css\" src=\"/css/utility.css\"></style>\n" +
    "<br>\n" +
    "Abs style" +
    "<style type=\"text/css\" src=\"http://www.example.com/css/utility.css\"></style>\n" +
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

    "</body>\n" +
    "</HTML>\n";

  static String xformed =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "<title>example.com website</title>\n" +
    "<meta http-equiv=\"content-type\" content=\"text/html; charset=ISO-8859-1\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1;url=http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fpage2.html\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1; url=http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fpage3.html\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1; 	url=http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fpage4.html\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1;url=http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2Fpage5.html\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1;url=http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F..%2Fpage6.html\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1;url=http://www.content.org/page2.html\">\n" +
    "</head>\n" +
    "<body>\n" +
    "<h1 align=\"center\">example.com website</h1>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2Findex.html\">abs link</a>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2Findex.html#ref1\">abs link with ref</a>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2Fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2Fpath%2Findex.html#ref2\">rel link with ref</a>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F4path%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F%252fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F%28content%29%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fmore%2Fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<A HREF=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F..%2Fmore%2Fpath%2Findex.html\">rel link</A>\n" +
    "<br>\n" +
    "<A HREF=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F.%2Fmore%2Fpath%2Findex.html\">rel link</A>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2F%3Fissn%3D123456789X\">rel query</a>\n" +
    "<br>\n" +
    "<a href=\"http://www.content.org/index.html\">abs link no rewrite</a>\n" +
    "<br>\n" +
    "Rel script<script type=\"text/javascript\" src=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fjavascript%2Fajax%2Futility.js\"></script>\n" +
    "<br>\n" +
    "Abs script<script type=\"text/javascript\" src=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fjavascript%2Futility.js\"></script>\n" +
    "<br>\n" +
    "Rel style<style type=\"text/css\" src=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Futility.css\"></style>\n" +
    "<br>\n" +
    "Abs style<style type=\"text/css\" src=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Futility.css\"></style>\n" +
    "<br>\n" +
    "Rel stylesheet<link rel=\"stylesheet\" href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fbasic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel stylesheet<link rel=\"stylesheet\" href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2FBasic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Abs stylesheet<link rel=\"stylesheet\" href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fextra.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel img<img src=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Ficons%2Flogo.gif\" alt=\"BMJ 1\" title=\"BMJ 1\" />\n" +
    "<br>\n" +
    "Abs img<img src=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Ficons%2Flogo2.gif\" alt=\"BMJ 2\" title=\"BMJ 2\" />\n" +
    "<br>\n" +

    // Base URL change
    "<base href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2F\" />\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2Findex.html\">abs link</a>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2Fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2F4path%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2F%252fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2F%28content%29%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fmore%2Fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<A HREF=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2F..%2Fmore%2Fpath%2Findex.html\">rel link</A>\n" +
    "<br>\n" +
    "<A HREF=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2F.%2Fmore%2Fpath%2Findex.html\">rel link</A>\n" +
    "<br>\n" +
    "<a href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2F%3Fissn%3D123456789X\">rel query</a>\n" +
    "<br>\n" +
    "<a href=\"http://www.content.org/index.html\">abs link no rewrite</a>\n" +
    "<br>\n" +
    "Rel script<script type=\"text/javascript\" src=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fjavascript%2Fajax%2Futility.js\"></script>\n" +
    "<br>\n" +
    "Abs script<script type=\"text/javascript\" src=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fjavascript%2Futility.js\"></script>\n" +
    "<br>\n" +
    "Rel style<style type=\"text/css\" src=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Futility.css\"></style>\n" +
    "<br>\n" +
    "Abs style<style type=\"text/css\" src=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Futility.css\"></style>\n" +
    "<br>\n" +
    "Rel stylesheet<link rel=\"stylesheet\" href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fbasic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel stylesheet<link rel=\"stylesheet\" href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2FBasic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Abs stylesheet<link rel=\"stylesheet\" href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fextra.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel img<img src=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Ficons%2Flogo.gif\" alt=\"BMJ 1\" title=\"BMJ 1\" />\n" +
    "<br>\n" +
    "Abs img<img src=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Ficons%2Flogo2.gif\" alt=\"BMJ 2\" title=\"BMJ 2\" />\n" +
    "<br>\n" +

    "</body>\n" +
    "</HTML>\n";

  private static final String origCss =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "</head>\n" +
    "<body>\n" +
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
    "Rel CSS import w/ bare url" +
    "<style type=\"text/css\" media=\"screen,print\">@import \"/css/common.css\"; @import 'common2.css';</style>\n" +
    "<br>\n" +

    // repeat above after changing the base URL

    "<base href=\"http://www.example.com/otherdir/\" />\n" +
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
    "Rel CSS import w/ bare url" +
    "<style type=\"text/css\" media=\"screen,print\">@import \"/css/common.css\"; @import 'common2.css';</style>\n" +
    "<br>\n" +

    "</body>\n" +
    "</HTML>\n";

  static String xformedCssFull =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "</head>\n" +
    "<body>\n" +
    "Rel path CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2Fcss%2Fcommon.css') @import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2Fcommon2.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fcommon.css') @import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fcommon2.css');</style>\n" +
    "<br>\n" +
    "Abs CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fextra.css') @import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fextra2.css');</style>\n" +
    "<br>\n" +
    "Mixed CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fextra3.css') @import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2FEXTRA4.css') @import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fextra5.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import w/ bare url<style type=\"text/css\" media=\"screen,print\">@import 'http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fcommon.css'; @import 'http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcontent%2Fcommon2.css';</style>\n" +
    "<br>\n" +

    // Base URL change
    "<base href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2F\" />\n" +
    "Rel path CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2Fcss%2Fcommon.css') @import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2Fcommon2.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fcommon.css') @import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fcommon2.css');</style>\n" +
    "<br>\n" +
    "Abs CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fextra.css') @import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fextra2.css');</style>\n" +
    "<br>\n" +
    "Mixed CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fextra3.css') @import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2FEXTRA4.css') @import url('http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fextra5.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import w/ bare url<style type=\"text/css\" media=\"screen,print\">@import 'http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fcss%2Fcommon.css'; @import 'http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2Fcommon2.css';</style>\n" +
    "<br>\n" +

    "</body>\n" +
    "</HTML>\n";

  static String xformedCssMinimal =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "</head>\n" +
    "<body>\n" +
    "Rel path CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/content/css/common.css') @import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/content/common2.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/css/common.css') @import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/css/common2.css');</style>\n" +
    "<br>\n" +
    "Abs CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/css/extra.css') @import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/css/extra2.css');</style>\n" +
    "<br>\n" +
    "Mixed CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/css/extra3.css') @import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/content/EXTRA4.css') @import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/extra5.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import w/ bare url<style type=\"text/css\" media=\"screen,print\">@import 'http://lockss.box:9524/ServeContent?url=http://www.example.com/css/common.css'; @import 'http://lockss.box:9524/ServeContent?url=http://www.example.com/content/common2.css';</style>\n" +
    "<br>\n" +

    // Base URL change
    "<base href=\"http://lockss.box:9524/ServeContent?url=http%3A%2F%2Fwww.example.com%2Fotherdir%2F\" />\n" +
    "Rel path CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/otherdir/css/common.css') @import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/otherdir/common2.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/css/common.css') @import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/css/common2.css');</style>\n" +
    "<br>\n" +
    "Abs CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/css/extra.css') @import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/css/extra2.css');</style>\n" +
    "<br>\n" +
    "Mixed CSS import<style type=\"text/css\" media=\"screen,print\">@import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/css/extra3.css') @import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/otherdir/EXTRA4.css') @import url('http://lockss.box:9524/ServeContent?url=http://www.example.com/extra5.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import w/ bare url<style type=\"text/css\" media=\"screen,print\">@import 'http://lockss.box:9524/ServeContent?url=http://www.example.com/css/common.css'; @import 'http://lockss.box:9524/ServeContent?url=http://www.example.com/otherdir/common2.css';</style>\n" +
    "<br>\n" +

    "</body>\n" +
    "</HTML>\n";

  private static final String origScript =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "</head>\n" +
    "<body>\n" +
    "<script type=\"text/javascript\">script test</script>\n" +
    "<br>\n" +
    "<script language=\"javascript\">test script</script>\n" +
    "<br>\n" +
    "</body>\n" +
    "</HTML>\n";

  private static final String xformedScript =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "</head>\n" +
    "<body>\n" +
    "<script type=\"text/javascript\">j123 script test 123j</script>\n" +
    "<br>\n" +
    "<script language=\"javascript\">j123 test script 123j</script>\n" +
    "<br>\n" +
    "</body>\n" +
    "</HTML>\n";



  private ServletUtil.LinkTransform xform = null;
  private String testPort = "9524";

  public void setUp() throws Exception {
    super.setUp();
    au = new MockArchivalUnit();
    au.setLinkRewriterFactory("text/css", new RegexpCssLinkRewriterFactory());
    au.setLinkRewriterFactory("text/javascript",
			      new MyLinkRewriterFactory("j123 ", " 123j"));

    List l = new ArrayList();
    l.add(urlStem);
    au.setUrlStems(l);
    nfhlrf = new NodeFilterHtmlLinkRewriterFactory();
  }

  public void testRewriting(String msg,
			    String src, String exp, boolean hostRel)
      throws Exception {
    if (hostRel) {
      xform = new ServletUtil.LinkTransform() {
	  public String rewrite(String url) {
	    return "/ServeContent?url=" + url;
	  }
	};
    } else {
      xform = new ServletUtil.LinkTransform() {
	  public String rewrite(String url) {
	    return "http://lockss.box:" + testPort + "/ServeContent?url=" + url;
	  }
	};
    }
    InputStream is = nfhlrf.createLinkRewriter("text/html", au,
					       new StringInputStream(src),
					       encoding, url, xform);
    String out = StringUtil.fromInputStream(is);
    log.debug3(msg + " original:\n" + orig);
    log.debug3(msg + " transformed:\n" + out);
    if (hostRel) {
      String relExp =
	StringUtil.replaceString(exp, "http://lockss.box:" + testPort, "");
      assertEquals(relExp, out);
    } else {
      assertEquals(exp, out);
    }
  }

  public void testAbsRewriting() throws Exception {
    testRewriting("Abs", orig, xformed, false);
  }

  public void testHostRelativeRewriting() throws Exception {
    testRewriting("Hostrel", orig, xformed, true);
  }

  public void testCssRewritingMinimal() throws Exception {
    ConfigurationUtil.addFromArgs(RegexpCssLinkRewriterFactory.PARAM_URL_ENCODE,
				  "Minimal");
    testRewriting("CSS abs minimal encoding", origCss, xformedCssMinimal,
		  false);
  }

  public void testCssRewritingFull() throws Exception {
    ConfigurationUtil.addFromArgs(RegexpCssLinkRewriterFactory.PARAM_URL_ENCODE,
				  "Full");
    testRewriting("CSS abs full encoding", origCss, xformedCssFull,
		  false);
  }

  public void testScriptRewritingMinimal() throws Exception {
    testRewriting("CSS abs minimal encoding", origScript, xformedScript,
		  false);
  }

  static class MyLinkRewriterFactory implements LinkRewriterFactory {

    private String prefix;
    private String suffix;

    public MyLinkRewriterFactory(String prefix, String suffix) {
      this.prefix = prefix;
      this.suffix = suffix;
      log.info("new MyLinkRewriterFactory(" + prefix + ", " + suffix + ")");
    }

    public InputStream createLinkRewriter(String mimeType,
					  ArchivalUnit au,
					  InputStream in,
					  String encoding,
					  String url,
					  ServletUtil.LinkTransform xform)
	throws PluginException {
      log.info("createLinkRewriter(" + prefix + ", " + suffix + ")");
      List<InputStream> lst = ListUtil.list(new StringInputStream(prefix),
					    in,
					    new StringInputStream(suffix));
      return new SequenceInputStream(Collections.enumeration(lst));
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {
      TestNodeFilterHtmlLinkRewriterFactory.class.getName()
    };
    junit.textui.TestRunner.main(testCaseList);
  }

}
