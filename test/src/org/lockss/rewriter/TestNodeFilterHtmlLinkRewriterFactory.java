/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

import junit.framework.Test;

import org.htmlparser.NodeFilter;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.servlet.*;
import org.lockss.filter.html.*;
import org.lockss.filter.html.HtmlNodeFilters.*;

public class TestNodeFilterHtmlLinkRewriterFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestNodeFilterHtmlLinkRewriterFactory");

  static String ISO = "ISO-8859-1";
  static String UTF8 = "UTF-8";

  static final String charset_orig =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "<title>example.com website</title>\n" +
    "<meta http-equiv=\"content-type\" " +
    "content=\"text/html; charset=UTF-8\">\n" +
    "</head>\n" +
    "<body>\n" +
    "<br>\n" +
    "Euro sign: \u20AC \n" +
    "</body>\n" +
    "</HTML>\n";

  static final String charset_xformed_wrong =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "<title>example.com website</title>\n" +
    "<meta http-equiv=\"content-type\" " +
    "content=\"text/html; charset=UTF-8\">\n" +
    "</head>\n" +
    "<body>\n" +
    "<br>\n" +

    // If characters are re-encoded using ISO-8859, the Euro sign will be
    // unrepresentable and output as a question-mark.

    "Euro sign: ? \n" +
    "</body>\n" +
    "</HTML>\n";

  static final String charset_xformed_right =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "<title>example.com website</title>\n" +
    "<meta http-equiv=\"content-type\" " +
    "content=\"text/html; charset=UTF-8\">\n" +
    "</head>\n" +
    "<body>\n" +
    "<br>\n" +
    "Euro sign: \u20AC \n" +
    "</body>\n" +
    "</HTML>\n";


  // All the tests that change the baseurl with a <base> tag in the middle
  // are invalid, as <base> is only allowed inside <head>, and it should
  // apply to *all* relative links in the document, not just those
  // folliwing it lixecally.  The link rewriter doesn't implement this
  // correctly, and these tests document that behavior.

  /** 
   * The original HTML page; CPROTO is the protocol for the content server.
   */
  static final String orig =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "<title>example.com website</title>\n" +
    "<meta http-equiv=\"content-type\" " +
    "content=\"text/html; charset=ISO-8859-1\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1;url=CPROTO://www.example.com/page2.html\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1; url=/page3.html\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1; \turl=CPROTO://www.example.com/page4.html\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1;url=page5.html\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1;url=../page6.html\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1;url=CPROTO://www.content.org/page2.html\">\n" +
    "<meta name=\"citation_url\" " +
    "content=\"CPROTO://www.example.com/art01.pdf\">\n" +
    "</head>\n" +
    "<body>\n" +
    "<h1 align=\"center\">example.com website</h1>\n" +
    "<br>\n" +
    "<a href=\"CPROTO://www.example.com/content/index.html\">abs link</a>\n" +
    "<br>\n" +
    "<a href=\"CPROTO://www.example.com/content/index.html#ref1\">abs link with ref</a>\n" +
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
    "<a href=\"CPROTO://www.content.org/index.html\">abs link no rewrite</a>\n" +
    "<br>\n" +
    "Rel script" +
    "<script type=\"text/javascript\" src=\"/javascript/ajax/utility.js\"></script>\n" +
    "<br>\n" +
    "Protocol Rel script" +
    "<script type=\"text/javascript\" src=\"//protorel.com/path/ute.js\"></script>\n" +
    "Protocol rel href + site rel src" +
    "<a href=\"//protorel.com/path2/ute.html\" src=\"/siterel/path3/ute.foo\">xxx</a>\n" +
    "<br>\n" +
    "Abs script" +
    "<script type=\"text/javascript\" src=\"CPROTO://www.example.com/javascript/utility.js\"></script>\n" +
    "<br>\n" +
    "Rel style" +
    "<style type=\"text/css\" src=\"/css/utility.css\"></style>\n" +
    "<br>\n" +
    "Abs style" +
    "<style type=\"text/css\" src=\"CPROTO://www.example.com/css/utility.css\"></style>\n" +
    "<br>\n" +
    "Rel stylesheet" +
    "<link rel=\"stylesheet\" href=\"/css/basic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel stylesheet" +
    "<link rel=\"stylesheet\" href=\"Basic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Abs stylesheet" +
    "<link rel=\"stylesheet\" href=\"CPROTO://www.example.com/css/extra.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel img" +
    "<img src=\"/icons/logo.gif\" alt=\"BMJ 1\" title=\"BMJ 1\" />\n" +
    "<br>\n" +
    "Abs img" +
    "<img src=\"CPROTO://www.example.com/icons/logo2.gif\" alt=\"BMJ 2\" title=\"BMJ 2\" />\n" +
    "<br>\n" +
    "style attr" +
    "<span class=\"foo\" style=\"background: url('/images/bar.png') no-repeat 0px -64px;\" />\n" +
    "<br>\n" +

    // repeat above after changing the base URL

    "<base href=\"CPROTO://www.example.com/otherdir/\" />\n" +
    "<a href=\"CPROTO://www.example.com/content/index.html\">abs link</a>\n" +
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
    "<a href=\"CPROTO://www.content.org/index.html\">abs link no rewrite</a>\n" +
    "<br>\n" +
    "Rel script" +
    "<script type=\"text/javascript\" src=\"/javascript/ajax/utility.js\"></script>\n" +
    "<br>\n" +
    "Abs script" +
    "<script type=\"text/javascript\" src=\"CPROTO://www.example.com/javascript/utility.js\"></script>\n" +
    "<br>\n" +
    "Rel style" +
    "<style type=\"text/css\" src=\"/css/utility.css\"></style>\n" +
    "<br>\n" +
    "Abs style" +
    "<style type=\"text/css\" src=\"CPROTO://www.example.com/css/utility.css\"></style>\n" +
    "<br>\n" +
    "Rel stylesheet" +
    "<link rel=\"stylesheet\" href=\"/css/basic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel stylesheet" +
    "<link rel=\"stylesheet\" href=\"Basic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Abs stylesheet" +
    "<link rel=\"stylesheet\" href=\"CPROTO://www.example.com/css/extra.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel img" +
    "<img src=\"/icons/logo.gif\" alt=\"BMJ 1\" title=\"BMJ 1\" />\n" +
    "<br>\n" +
    "Abs img" +
    "<img src=\"CPROTO://www.example.com/icons/logo2.gif\" alt=\"BMJ 2\" title=\"BMJ 2\" />\n" +
    "<br>\n" +
    "style attr" +
    "<span class=\"foo\" style=\"background: url('images/bar.png') no-repeat 0px -64px;\" />\n" +
    "<br>\n" +

    "</body>\n" +
    "</HTML>\n";

  /** 
   * The transformed HTML page; CPROTO is the protocol for the content server,
   * and LPROTO is the protocol for the LOCKSS server
   */
  static String xformed =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "<title>example.com website</title>\n" +
    "<meta http-equiv=\"content-type\" content=\"text/html; charset=ISO-8859-1\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1;url=LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fpage2.html\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1; url=LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fpage3.html\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1; 	url=LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fpage4.html\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1;url=LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Fpage5.html\">\n" +
    "<meta http-equiv=\"refresh\" content=\"1;url=LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2F..%2Fpage6.html\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1;url=CPROTO://www.content.org/page2.html\">\n" +
    "<meta name=\"citation_url\" " +
    "content=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fart01.pdf\">\n" +
    "</head>\n" +
    "<body>\n" +
    "<h1 align=\"center\">example.com website</h1>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Findex.html\">abs link</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Findex.html#ref1\">abs link with ref</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Fpath%2Findex.html#ref2\">rel link with ref</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2F4path%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2F%252fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2F%28content%29%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fmore%2Fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<A HREF=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2F..%2Fmore%2Fpath%2Findex.html\">rel link</A>\n" +
    "<br>\n" +
    "<A HREF=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2F.%2Fmore%2Fpath%2Findex.html\">rel link</A>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2F%3Fissn%3D123456789X\">rel query</a>\n" +
    "<br>\n" +
    "<a href=\"CPROTO://www.content.org/index.html\">abs link no rewrite</a>\n" +
    "<br>\n" +
    "Rel script<script type=\"text/javascript\" src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fjavascript%2Fajax%2Futility.js\"></script>\n" +
    "<br>\n" +
    "Protocol Rel script<script type=\"text/javascript\" src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fprotorel.com%2Fpath%2Fute.js\"></script>\n" +
    "Protocol rel href + site rel src" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fprotorel.com%2Fpath2%2Fute.html\" src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fsiterel%2Fpath3%2Fute.foo\">xxx</a>\n" +
    "<br>\n" +
    "Abs script<script type=\"text/javascript\" src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fjavascript%2Futility.js\"></script>\n" +
    "<br>\n" +
    "Rel style<style type=\"text/css\" src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Futility.css\"></style>\n" +
    "<br>\n" +
    "Abs style<style type=\"text/css\" src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Futility.css\"></style>\n" +
    "<br>\n" +
    "Rel stylesheet<link rel=\"stylesheet\" href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fbasic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel stylesheet<link rel=\"stylesheet\" href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2FBasic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Abs stylesheet<link rel=\"stylesheet\" href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fextra.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel img<img src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Ficons%2Flogo.gif\" alt=\"BMJ 1\" title=\"BMJ 1\" />\n" +
    "<br>\n" +
    "Abs img<img src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Ficons%2Flogo2.gif\" alt=\"BMJ 2\" title=\"BMJ 2\" />\n" +
    "<br>\n" +
    "style attr<span class=\"foo\" style=\"background: url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/images/bar.png') no-repeat 0px -64px;\" />\n" +
    "<br>\n" +

    // Base URL change
    "<base href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2F\" />\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Findex.html\">abs link</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2Fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2F4path%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2F%252fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2F%28content%29%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fmore%2Fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<A HREF=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2F..%2Fmore%2Fpath%2Findex.html\">rel link</A>\n" +
    "<br>\n" +
    "<A HREF=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2F.%2Fmore%2Fpath%2Findex.html\">rel link</A>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2F%3Fissn%3D123456789X\">rel query</a>\n" +
    "<br>\n" +
    "<a href=\"CPROTO://www.content.org/index.html\">abs link no rewrite</a>\n" +
    "<br>\n" +
    "Rel script<script type=\"text/javascript\" src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fjavascript%2Fajax%2Futility.js\"></script>\n" +
    "<br>\n" +
    "Abs script<script type=\"text/javascript\" src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fjavascript%2Futility.js\"></script>\n" +
    "<br>\n" +
    "Rel style<style type=\"text/css\" src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Futility.css\"></style>\n" +
    "<br>\n" +
    "Abs style<style type=\"text/css\" src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Futility.css\"></style>\n" +
    "<br>\n" +
    "Rel stylesheet<link rel=\"stylesheet\" href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fbasic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel stylesheet<link rel=\"stylesheet\" href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2FBasic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Abs stylesheet<link rel=\"stylesheet\" href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fextra.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel img<img src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Ficons%2Flogo.gif\" alt=\"BMJ 1\" title=\"BMJ 1\" />\n" +
    "<br>\n" +
    "Abs img<img src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Ficons%2Flogo2.gif\" alt=\"BMJ 2\" title=\"BMJ 2\" />\n" +
    "<br>\n" +
    "style attr<span class=\"foo\" style=\"background: url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/otherdir/images/bar.png') no-repeat 0px -64px;\" />\n" +
    "<br>\n" +

    "</body>\n" +
    "</HTML>\n";

  // Similar to previous but <base> element has a relative href

  /**
   * The original HTML page; CPROTO is the protocol for the content server.
   */
  static final String orig_relbase =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "<title>example.com website</title>\n" +
    "</head>\n" +
    "<body>\n" +
    "<h1 align=\"center\">example.com website</h1>\n" +
    "<br>\n" +
    "<a href=\"CPROTO://www.example.com/content/index.html\">abs link</a>\n" +
    "<br>\n" +

    // repeat above after changing the base URL

    "<base href=\"/\" />\n" +
    "<a href=\"CPROTO://www.example.com/content/index.html\">abs link</a>\n" +
    "<br>\n" +
    "<a href=\"path/index.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"4path/index.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"%2fpath/index.html\">rel link</a>\n" +
    "<br>\n" +
    // Verify that a second <base> is ignored
    "<base href=\"/ignore/this/base/path/\" />\n" +
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
    "<a href=\"CPROTO://www.content.org/index.html\">abs link no rewrite</a>\n" +
    "<br>\n" +
    "Rel script" +
    "<script type=\"text/javascript\" src=\"/javascript/ajax/utility.js\"></script>\n" +
    "<br>\n" +
    "Abs script" +
    "<script type=\"text/javascript\" src=\"CPROTO://www.example.com/javascript/utility.js\"></script>\n" +
    "<br>\n" +
    "Rel style" +
    "<style type=\"text/css\" src=\"/css/utility.css\"></style>\n" +
    "<br>\n" +
    "Abs style" +
    "<style type=\"text/css\" src=\"CPROTO://www.example.com/css/utility.css\"></style>\n" +
    "<br>\n" +
    "Rel stylesheet" +
    "<link rel=\"stylesheet\" href=\"/css/basic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel stylesheet" +
    "<link rel=\"stylesheet\" href=\"Basic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Abs stylesheet" +
    "<link rel=\"stylesheet\" href=\"CPROTO://www.example.com/css/extra.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel img" +
    "<img src=\"/icons/logo.gif\" alt=\"BMJ 1\" title=\"BMJ 1\" />\n" +
    "<br>\n" +
    "Abs img" +
    "<img src=\"CPROTO://www.example.com/icons/logo2.gif\" alt=\"BMJ 2\" title=\"BMJ 2\" />\n" +
    "<br>\n" +
    "style attr" +
    "<span class=\"foo\" style=\"background: url('images/bar.png') no-repeat 0px -64px;\" />\n" +
    "<br>\n" +

    "</body>\n" +
    "</HTML>\n";

  /**
   * The transformed HTML page w/ relative <base> change.
   */
  static String xformed_relbase =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "<title>example.com website</title>\n" +
    "</head>\n" +
    "<body>\n" +
    "<h1 align=\"center\">example.com website</h1>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Findex.html\">abs link</a>\n" +
    "<br>\n" +

    // Base URL change
    "<base href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2F\" />\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Findex.html\">abs link</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2F4path%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2F%252fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<base href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fignore%2Fthis%2Fbase%2Fpath%2F\" />\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2F%28content%29%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fmore%2Fpath%2Findex.html\">rel link</a>\n" +
    "<br>\n" +
    "<A HREF=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2F..%2Fmore%2Fpath%2Findex.html\">rel link</A>\n" +
    "<br>\n" +
    "<A HREF=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2F.%2Fmore%2Fpath%2Findex.html\">rel link</A>\n" +
    "<br>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2F%3Fissn%3D123456789X\">rel query</a>\n" +
    "<br>\n" +
    "<a href=\"CPROTO://www.content.org/index.html\">abs link no rewrite</a>\n" +
    "<br>\n" +
    "Rel script<script type=\"text/javascript\" src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fjavascript%2Fajax%2Futility.js\"></script>\n" +
    "<br>\n" +
    "Abs script<script type=\"text/javascript\" src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fjavascript%2Futility.js\"></script>\n" +
    "<br>\n" +
    "Rel style<style type=\"text/css\" src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Futility.css\"></style>\n" +
    "<br>\n" +
    "Abs style<style type=\"text/css\" src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Futility.css\"></style>\n" +
    "<br>\n" +
    "Rel stylesheet<link rel=\"stylesheet\" href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fbasic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel stylesheet<link rel=\"stylesheet\" href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2FBasic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Abs stylesheet<link rel=\"stylesheet\" href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fextra.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Rel img<img src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Ficons%2Flogo.gif\" alt=\"BMJ 1\" title=\"BMJ 1\" />\n" +
    "<br>\n" +
    "Abs img<img src=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Ficons%2Flogo2.gif\" alt=\"BMJ 2\" title=\"BMJ 2\" />\n" +
    "<br>\n" +
    "style attr<span class=\"foo\" style=\"background: url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/images/bar.png') no-repeat 0px -64px;\" />\n" +
    "<br>\n" +

    "</body>\n" +
    "</HTML>\n";

  /** 
   * The original CSS page; CPROTO is the protocol for the content server.
   */
  static final String origCss =
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
    "<style type=\"text/css\" media=\"screen,print\">@import url(CPROTO://www.example.com/css/extra.css) @import url(CPROTO://www.example.com/css/extra2.css);</style>\n" +
    "<br>\n" +
    "Mixed CSS import" +
    "<style type=\"text/css\" media=\"screen,print\">@import url(CPROTO://www.example.com/css/extra3.css) @import url(EXTRA4.css) @import url(../extra5.css);</style>\n" +
    "<br>\n" +
    "Rel CSS import w/ bare url" +
    "<style type=\"text/css\" media=\"screen,print\">@import \"/css/common.css\"; @import 'common2.css';</style>\n" +
    "<br>\n" +

    // repeat above after changing the base URL

    "<base href=\"CPROTO://www.example.com/otherdir/\" />\n" +
    "Rel path CSS import" +
    "<style type=\"text/css\" media=\"screen,print\">@import url(css/common.css) @import url(common2.css);</style>\n" +
    "<br>\n" +
    "Rel CSS import" +
    "<style type=\"text/css\" media=\"screen,print\">@import url(/css/common.css) @import url(/css/common2.css);</style>\n" +
    "<br>\n" +
    "Abs CSS import" +
    "<style type=\"text/css\" media=\"screen,print\">@import url(CPROTO://www.example.com/css/extra.css) @import url(CPROTO://www.example.com/css/extra2.css);</style>\n" +
    "<br>\n" +
    "Mixed CSS import" +
    "<style type=\"text/css\" media=\"screen,print\">@import url(CPROTO://www.example.com/css/extra3.css) @import url(EXTRA4.css) @import url(../extra5.css);</style>\n" +
    "<br>\n" +
    "Rel CSS import w/ bare url" +
    "<style type=\"text/css\" media=\"screen,print\">@import \"/css/common.css\"; @import 'common2.css';</style>\n" +
    "<br>\n" +

    "</body>\n" +
    "</HTML>\n";

  /** 
   * The transformed full CSS page; CPROTO is the protocol for the content 
   * server, and LPROTO is the protocol of the LOCKSS server
   */
  static String xformedCssFull =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "</head>\n" +
    "<body>\n" +
    "Rel path CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Fcss%2Fcommon.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Fcommon2.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fcommon.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fcommon2.css');</style>\n" +
    "<br>\n" +
    "Abs CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fextra.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fextra2.css');</style>\n" +
    "<br>\n" +
    "Mixed CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fextra3.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2FEXTRA4.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fextra5.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import w/ bare url<style type=\"text/css\" media=\"screen,print\">@import 'LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fcommon.css'; @import 'LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Fcommon2.css';</style>\n" +
    "<br>\n" +

    // Base URL change
    "<base href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2F\" />\n" +
    "Rel path CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2Fcss%2Fcommon.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2Fcommon2.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fcommon.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fcommon2.css');</style>\n" +
    "<br>\n" +
    "Abs CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fextra.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fextra2.css');</style>\n" +
    "<br>\n" +
    "Mixed CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fextra3.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2FEXTRA4.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fextra5.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import w/ bare url<style type=\"text/css\" media=\"screen,print\">@import 'LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcss%2Fcommon.css'; @import 'LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2Fcommon2.css';</style>\n" +
    "<br>\n" +

    "</body>\n" +
    "</HTML>\n";

  /** 
   * The transformed minimal page; CPROTO is the protocol for the content server
   * and LPROTO is the protocol of the LOCKSS server
   */
  static String xformedCssMinimal =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "</head>\n" +
    "<body>\n" +
    "Rel path CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/content/css/common.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/content/common2.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/css/common.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/css/common2.css');</style>\n" +
    "<br>\n" +
    "Abs CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/css/extra.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/css/extra2.css');</style>\n" +
    "<br>\n" +
    "Mixed CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/css/extra3.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/content/EXTRA4.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/extra5.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import w/ bare url<style type=\"text/css\" media=\"screen,print\">@import 'LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/css/common.css'; @import 'LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/content/common2.css';</style>\n" +
    "<br>\n" +

    // Base URL change
    "<base href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fotherdir%2F\" />\n" +
    "Rel path CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/otherdir/css/common.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/otherdir/common2.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/css/common.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/css/common2.css');</style>\n" +
    "<br>\n" +
    "Abs CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/css/extra.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/css/extra2.css');</style>\n" +
    "<br>\n" +
    "Mixed CSS import<style type=\"text/css\" media=\"screen,print\">@import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/css/extra3.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/otherdir/EXTRA4.css') @import url('LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/extra5.css');</style>\n" +
    "<br>\n" +
    "Rel CSS import w/ bare url<style type=\"text/css\" media=\"screen,print\">@import 'LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/css/common.css'; @import 'LPROTO://lockss.box:9524/ServeContent?url=CPROTO://www.example.com/otherdir/common2.css';</style>\n" +
    "<br>\n" +

    "</body>\n" +
    "</HTML>\n";

  /** 
   * The original Javascript page.
   */
  static final String origScript =
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

  /** 
   * The transformed JavaScript page.
   */
  static final String xformedScript =
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

  static abstract class TestNodeFilterHtmlLinkRewriterFactoryItem
    extends TestNodeFilterHtmlLinkRewriterFactory {
    private MockArchivalUnit au;
    private MyNodeFilterHtmlLinkRewriterFactory nfhlrf;
    
    private ServletUtil.LinkTransform xform = null;
    private String testPort = "9524";
  
    // The LOCKSS protocol (http or https)
    String lproto;
    // The content protocol (http or https)
    String cproto;

    // URL info for content protocol
    String urlStem;
    String urlSuffix;
    String url;

  static final String meta_orig =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html><head>\n" +
    "<meta name=\"citation_url\" " +
    "content=\"CPROTO://www.example.com/foo.pdf\">\n" +
    "</head></html>\n";

  static final String meta_xformed_rel =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html><head>\n" +
    "<meta name=\"citation_url\" " +
    "content=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Ffoo.pdf\">\n" +
    "</head></html>\n";

  static final String meta_xformed_abs =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html><head>\n" +
    "<meta name=\"citation_url\" " +
    "content=\"http:triggered.clockss.orgLPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Ffoo.pdf\">\n" +
    "</head></html>\n";

  static final String assorted_attrs_orig =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html><head>\n" +
    "<a href=\"path/href.html\" " +
    "attr_2=\"path/attr_2.html\" " +
    "attr_3=\"path/attr_3.html\">rel link</a>\n" +
    "</head></html>\n";

  static final String assorted_attrs_xformed_href_only =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html><head>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Fpath%2Fhref.html\" " +
    "attr_2=\"path/attr_2.html\" " +
    "attr_3=\"path/attr_3.html\">rel link</a>\n" +
    "</head></html>\n";

  static final String assorted_attrs_xformed_href_attr_2 =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html><head>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Fpath%2Fhref.html\" " +
    "attr_2=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Fpath%2Fattr_2.html\" " +
    "attr_3=\"path/attr_3.html\">rel link</a>\n" +
    "</head></html>\n";

  static final String assorted_attrs_xformed_attr_2_attr_3 =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html><head>\n" +
    "<a href=\"path/href.html\" " +
    "attr_2=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Fpath%2Fattr_2.html\" " +
    "attr_3=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fcontent%2Fpath%2Fattr_3.html\">rel link</a>\n" +
    "</head></html>\n";

  static final String add_xform_orig =
    "<html><head>\n" +
    "<a href=\"CPROTO://www.example.com/path/pre1\">pre1</a>\n" +
    "<a href=\"CPROTO://www.example.com/path/pre2\">pre2</a>\n" +
    "<a href=\"CPROTO://www.example.com/path/post1\">post1</a>\n" +
    "<a href=\"CPROTO://www.example.com/path/post2\">post2</a>\n" +
    "</head></html>\n";

  static final String add_xform_xformed =
    "<html><head>\n" +
    "<a href=\"CPROTO://www.different.com/path/pre1\">pre1</a>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fpath%2Fpre2\">pre2</a>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.different.com%2Fpath%2Fpost1\">post1</a>\n" +
    "<a href=\"LPROTO://lockss.box:9524/ServeContent?url=CPROTO%3A%2F%2Fwww.example.com%2Fpath%2Fpost2\">post2</a>\n" +
    "</head></html>\n";


    public void setUp() throws Exception {
      super.setUp();
      au = new MockArchivalUnit();
      au.setLinkRewriterFactory("text/css", new RegexpCssLinkRewriterFactory());
      au.setLinkRewriterFactory("text/javascript",
  			      new MyLinkRewriterFactory("j123 ", " 123j"));
  
      List<String> l = new ArrayList<String>();
      l.add(urlStem);
      au.setUrlStems(l);
      nfhlrf = new MyNodeFilterHtmlLinkRewriterFactory();
      nfhlrf.setMetaNames(ListUtil.list("citation_url"));
    }
  
    public void testAddAttrs() throws Exception {
      assertEquals(ListUtil.list("href", "src", "action",
				 "background", "onclick"),
		   nfhlrf.getAttrList());
      assertEquals(ListUtil.list("href", "src", "action",
				 "background", "onclick").toArray(),
		   nfhlrf.getAttrsToRewrite());
      nfhlrf.addAttrToRewrite("new_attr");
      assertEquals(ListUtil.list("href", "src", "action",
				 "background", "onclick",
				 "new_attr").toArray(),
		   nfhlrf.getAttrsToRewrite());
      nfhlrf.addAttrToRewrite("new_attr_2");
      nfhlrf.addAttrToRewrite("new_attr");
      assertEquals(ListUtil.list("href", "src", "action",
				 "background", "onclick",
				 "new_attr", "new_attr_2").toArray(),
		   nfhlrf.getAttrsToRewrite());
    }
  
    public void testRemoveAttrs() throws Exception {
      assertEquals(ListUtil.list("href", "src", "action",
				 "background", "onclick"),
		   nfhlrf.getAttrList());
      assertEquals(ListUtil.list("href", "src", "action",
				 "background", "onclick").toArray(),
		   nfhlrf.getAttrsToRewrite());
      assertFalse(nfhlrf.removeAttrToRewrite("new_attr"));
      assertTrue(nfhlrf.removeAttrToRewrite("action"));
      assertEquals(ListUtil.list("href", "src",
				 "background", "onclick").toArray(),
		   nfhlrf.getAttrsToRewrite());
      assertFalse(nfhlrf.removeAttrToRewrite("action"));
      assertEquals(ListUtil.list("href", "src",
				 "background", "onclick").toArray(),
		   nfhlrf.getAttrsToRewrite());
    }
  
    public void testSetAttrs() throws Exception {
      assertEquals(ListUtil.list("href", "src", "action",
				 "background", "onclick"),
		   nfhlrf.getAttrList());
      assertEquals(ListUtil.list("href", "src", "action",
				 "background", "onclick").toArray(),
		   nfhlrf.getAttrsToRewrite());
      nfhlrf.setAttrsToRewrite(ListUtil.list("mostly", "different",
						 "src", "attrs"));
      assertEquals(ListUtil.list("mostly", "different",
				 "src", "attrs"),
		   nfhlrf.getAttrList());
      assertEquals(ListUtil.list("mostly", "different",
				 "src", "attrs").toArray(),
		   nfhlrf.getAttrsToRewrite());
    }
  

    public void testRewriting(String msg,
			      String src0, String srcCharset,
			      String exp0, boolean hostRel)
        throws Exception {

      String src = src0.replace("CPROTO", cproto);
      String exp = exp0.replace("CPROTO", cproto).replace("LPROTO", lproto);
      Collection stems = au.getUrlStems();
      stems.add(cproto + "://protorel.com");
      au.setUrlStems(new ArrayList(stems));

      if (hostRel) {
        xform = new ServletUtil.LinkTransform() {
  	  public String rewrite(String url) {
  	    return "/ServeContent?url=" + url;
  	  }
  	};
      } else {
        xform = new ServletUtil.LinkTransform() {
  	  public String rewrite(String url) {
  	    return lproto+"://lockss.box:" + testPort + "/ServeContent?url=" + url;
  	  }
  	};
      }
      // Read src string using specified charset
      InputStream srcIn =
	new ReaderInputStream(new StringReader(src), srcCharset);

      // Always pass ISO-8859-1 to rewriter - any non-ISO-8859-1 are after
      // <meta> that changes charset
      InputStream is = nfhlrf.createLinkRewriter("text/html", au,
						 srcIn,
						 ISO, url, xform);
      String out;
      if (is instanceof EncodedThing) {
	String filtCharset = ((EncodedThing)is).getCharset();
	out = StringUtil.fromReader(new InputStreamReader(is, filtCharset));
      } else {
	out = StringUtil.fromInputStream(is);
      }
      
      log.debug3(msg + " original:\n" + src);
      log.debug3(msg + " transformed:\n" + out);
      if (hostRel) {
        String relExp =
  	StringUtil.replaceString(exp, lproto+"://lockss.box:" + testPort, "");
        assertEquals(relExp, out);
      } else {
        assertEquals(exp, out);
      }
    }
  
    public void testAbsRewriting() throws Exception {
      ConfigurationUtil.addFromArgs(RegexpCssLinkRewriterFactory.PARAM_URL_ENCODE,
  				  "Minimal");
      testRewriting("Abs", orig, ISO, xformed, false);
    }
  
    public void testHostRelativeRewriting() throws Exception {
      ConfigurationUtil.addFromArgs(RegexpCssLinkRewriterFactory.PARAM_URL_ENCODE,
  				  "Minimal");
      testRewriting("Hostrel", orig, ISO, xformed, true);
    }
  
    public void testAbsRewritingRelBase() throws Exception {
      ConfigurationUtil.addFromArgs(RegexpCssLinkRewriterFactory.PARAM_URL_ENCODE,
  				  "Minimal");
      testRewriting("Abs", orig_relbase, ISO, xformed_relbase, false);
    }

    public void testHostRelativeRewritingRelBase() throws Exception {
      ConfigurationUtil.addFromArgs(RegexpCssLinkRewriterFactory.PARAM_URL_ENCODE,
  				  "Minimal");
      testRewriting("Hostrel", orig_relbase, ISO, xformed_relbase, true);
    }

    public void testCssRewritingMinimal() throws Exception {
      ConfigurationUtil.addFromArgs(RegexpCssLinkRewriterFactory.PARAM_URL_ENCODE,
  				  "Minimal");
      testRewriting("CSS abs minimal encoding", origCss, ISO, xformedCssMinimal,
  		  false);
    }
  
    public void testCssRewritingFull() throws Exception {
      ConfigurationUtil.addFromArgs(RegexpCssLinkRewriterFactory.PARAM_URL_ENCODE,
  				  "Full");
      testRewriting("CSS abs full encoding", origCss, ISO, xformedCssFull,
		    false);
    }
  
    public void testScriptRewritingMinimal() throws Exception {
      testRewriting("Script abs minimal encoding", origScript, ISO,
		    xformedScript, false);
    }
   
    public void testCharsetWrong() throws Exception {
      ConfigurationUtil.addFromArgs(HtmlFilterInputStream.PARAM_ADAPT_ENCODING,
  				  "false");
      testRewriting("Charset change, not handled correctly",
		    charset_orig, UTF8, charset_xformed_wrong, false);
    }
  
    public void testCharsetRight() throws Exception {
      testRewriting("Charset change, not handled correctly",
		    charset_orig, UTF8, charset_xformed_right, false);
    }
  
    public void testMetaRel() throws Exception {
      testRewriting("Meta tag rel", meta_orig, ISO, meta_xformed_rel, false);
    }
  
    public void testMetaAbsHack() throws Exception {
      ConfigurationUtil.addFromArgs(NodeFilterHtmlLinkRewriterFactory.PARAM_META_TAG_REWRITE_PREFIX,
  				  "http:triggered.clockss.org");

      testRewriting("Meta tag abs", meta_orig, ISO, meta_xformed_abs, true);
    }
  
    public void testAssortedAttrs_href() throws Exception {
      testRewriting("Default attrs", assorted_attrs_orig, ISO, assorted_attrs_xformed_href_only, true);
    }

    public void testAssortedAttrs_add_attr2() throws Exception {
      nfhlrf.addAttrToRewrite("attr_2");
      testRewriting("Add attr_2", assorted_attrs_orig, ISO, assorted_attrs_xformed_href_attr_2, true);
      try {
	nfhlrf.addAttrToRewrite("foo");
	fail("Adding attr to rewrite after rewriting has begun should throw");
      } catch (IllegalStateException e) {
      }
    }
  
    public void testAssortedAttrs_del_href_add_attr23() throws Exception {
      nfhlrf.removeAttrToRewrite("href");
      nfhlrf.addAttrToRewrite("attr_2");
      nfhlrf.addAttrToRewrite("attr_3");
      testRewriting("Del href, add attr_2, attr_3", assorted_attrs_orig, ISO, assorted_attrs_xformed_attr_2_attr_3, true);
      try {
	nfhlrf.removeAttrToRewrite("foo");
	fail("Removing attr to rewrite after rewriting has begun should throw");
      } catch (IllegalStateException e) {
      }
    }
  
    public void testAssortedAttrs_set_attr23() throws Exception {
      nfhlrf.setAttrsToRewrite(ListUtil.list("attr_2", "attr_3"));
      testRewriting("Set attr_2, attr_3", assorted_attrs_orig, ISO, assorted_attrs_xformed_attr_2_attr_3, true);
      try {
	nfhlrf.setAttrsToRewrite(ListUtil.list("foo"));
	fail("Setting attrs to rewrite after rewriting has begun should throw");
      } catch (IllegalStateException e) {
      }
    }

  static final String bare_ref_orig =
    "<html><head>\n" +
    "<a href=\"#refref\">there</a>\n" +
    "</head></html>\n";

    public void testNoRewriteRef() throws Exception {
      testRewriting("Abs", bare_ref_orig, ISO, bare_ref_orig, false);
    }

    public void testPrePostTransform() throws Exception {
      NodeFilter pre1 = new LinkRegexXform("pre1", false,
					 "example\\.com", "different.com",
					 new String[] {"href"});
      NodeFilter post1 = new LinkRegexXform("post1", false,
					    "example\\.com", "different.com",
					    new String[] {"href"});
      nfhlrf.addPreXform(pre1);
      nfhlrf.addPostXform(post1);
      testRewriting("Pre post", add_xform_orig, ISO, add_xform_xformed, true);
    }      


    void setupProtos(String cproto, String lproto) {
      this.cproto = cproto;
      this.lproto = lproto;
      
      urlStem = cproto + "://www.example.com/";
      urlSuffix = "content/index.html";
      url = urlStem + urlSuffix;
    }
  }

  
  static class MyLinkRewriterFactory implements LinkRewriterFactory {

    private String prefix;
    private String suffix;

    public MyLinkRewriterFactory(String prefix, String suffix) {
      this.prefix = prefix;
      this.suffix = suffix;
    }

    public InputStream createLinkRewriter(String mimeType,
					  ArchivalUnit au,
					  InputStream in,
					  String encoding,
					  String url,
					  ServletUtil.LinkTransform xform)
	throws PluginException {
      List<InputStream> lst = ListUtil.list(new StringInputStream(prefix),
					    in,
					    new StringInputStream(suffix));
      return new SequenceInputStream(Collections.enumeration(lst));
    }
  }

  static class MyNodeFilterHtmlLinkRewriterFactory
    extends NodeFilterHtmlLinkRewriterFactory {
    List<String> metaNames = Collections.EMPTY_LIST;

    void setMetaNames(List<String> metaNames) {
      this.metaNames = metaNames;
    }

    protected List<String> getMetaNamesToRewrite(ArchivalUnit au) {
      return metaNames;
    }
  }


  static public class TestNodeFilterHtmlLinkRewriterFactoryItem1 
    extends TestNodeFilterHtmlLinkRewriterFactoryItem {
    { setupProtos("http", "http"); }
  };

  static public class TestNodeFilterHtmlLinkRewriterFactoryItem2 
    extends TestNodeFilterHtmlLinkRewriterFactoryItem {
    { setupProtos("https", "http"); }
  };

  static public class TestNodeFilterHtmlLinkRewriterFactoryItem3 
    extends TestNodeFilterHtmlLinkRewriterFactoryItem {
    { setupProtos("http", "https"); }
  };

  static public class TestNodeFilterHtmlLinkRewriterFactoryItem4
    extends TestNodeFilterHtmlLinkRewriterFactoryItem {
    { setupProtos("https", "https"); }
  };

  public static Test suite() {
    return variantSuites(new Class[] {
        TestNodeFilterHtmlLinkRewriterFactoryItem1.class,
        TestNodeFilterHtmlLinkRewriterFactoryItem2.class,
        TestNodeFilterHtmlLinkRewriterFactoryItem3.class,
        TestNodeFilterHtmlLinkRewriterFactoryItem4.class,
    });
  }


  public static void main(String[] argv) {
    String[] testCaseList = {
      TestNodeFilterHtmlLinkRewriterFactory.class.getName()
    };
    junit.textui.TestRunner.main(testCaseList);
  }

}
