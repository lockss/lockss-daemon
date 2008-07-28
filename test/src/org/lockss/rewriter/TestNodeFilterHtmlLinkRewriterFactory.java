/*
 * $Id: TestNodeFilterHtmlLinkRewriterFactory.java,v 1.4.2.7 2008-07-28 01:23:30 dshr Exp $
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
  private String encoding = null;
  private static final String urlStem = "http://www.example.com/";
  private static final String urlSuffix = "content/index.html";
  private static final String url = urlStem + urlSuffix;
  private static final String page =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "<title>example.com website</title>\n" +
    "<meta http-equiv=\"content-type\" " +
    "content=\"text/html; charset=ISO-8859-1\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1;url=" + urlStem + "page2.html" + "\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1; url=" + urlStem + "page3.html" + "\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1; \turl=" + urlStem + "page4.html" + "\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1;url=" + "page5.html" + "\">\n" +
    "<meta http-equiv=\"refresh\" " +
    "content=\"1;url=" + "../page6.html" + "\">\n" +
    "</head>\n" +
    "<body>\n" +
    "<h1 align=\"center\">example.com website</h1>\n" +
    "<br>\n" +
    "<a href=\"" + url + "\">an absolute link to rewrite</a>\n" +
    "<br>\n" +
    "<a href=\"" + urlSuffix + "\">a relative link to rewrite</a>\n" +
    "<br>\n" +
    "<a href=\"" + "/more/" + urlSuffix + "\">a relative link to rewrite</a>\n" +
    "<br>\n" +
    "<A HREF=\"" + "../more/" + urlSuffix + "\">a relative link to rewrite</A>\n" +
    "<br>\n" +
    "<a href=\"" + "?issn=123456789X" + "\">a relative query to rewrite</a>\n" +
    "<br>\n" +
    "<a href=\"http://www.content.org/index.html\">an absolute link not to rewrite</a>\n" +
    "<br>\n" +
    "A relative script" +
    "<script type=\"text/javascript\" src=\"/javascript/ajax/utility.js\"></script>\n" +
    "<br>\n" +
    "An absolute script" +
    "<script type=\"text/javascript\" src=\"" + urlStem + "javascript/utility.js\"></script>\n" +
    "<br>\n" +
    "A relative stylesheet" +
    "<link rel=\"stylesheet\" href=\"/css/basic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "Another relative stylesheet" +
    "<link rel=\"stylesheet\" href=\"Basic.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "An absolute stylesheet" +
    "<link rel=\"stylesheet\" href=\"" + urlStem + "css/extra.css\" type=\"text/css\" media=\"all\">\n" +
    "<br>\n" +
    "A relative img" +
    "<img src=\"/icons/logo.gif\" alt=\"BMJ 1\" title=\"BMJ 1\" />\n" +
    "<br>\n" +
    "An absolute img" +
    "<img src=\"" + urlStem + "icons/logo2.gif\" alt=\"BMJ 2\" title=\"BMJ 2\" />\n" +
    "<br>\n" +
    "A relative CSS import" +
    "<style type=\"text/css\" media=\"screen,print\">@import url(/css/common.css) @import url(/css/common2.css);</style>\n" +
    "<br>\n" +
    "An absolute CSS import" +
    "<style type=\"text/css\" media=\"screen,print\">@import url(" + urlStem + "css/extra.css) @import url(" + urlStem + "css/extra2.css);</style>\n" +
    "<br>\n" +
    "A mixed CSS import" +
    "<style type=\"text/css\" media=\"screen,print\">@import url(" + urlStem + "css/extra3.css) @import url(EXTRA4.css) @import url(../extra5.css);</style>\n" +
    "<br>\n" +
    "</body>\n" +
    "</HTML>\n";

  // XXX need a better way to determine that the rewrite is correct than
  // XXX counting tag instances
  private static final int linkCount = 17;
  private static final int importCount = 7;
  private InputStream in;

  public void setUp() throws Exception {
    super.setUp();
    au = new MockArchivalUnit();
    List l = new ArrayList();
    l.add(urlStem);
    au.setUrlStems(l);
    nfhlrf = new NodeFilterHtmlLinkRewriterFactory();
  }

  public void testThrowsIfNotHtml() {
    in = new ReaderInputStream(new StringReader(page));
    setupConfig(true);
    try {
      InputStream ret = nfhlrf.createLinkRewriter("application/pdf", au, in,
						  encoding, url);
      fail("createLinkRewriter should have thrown on non-html mime type");
    } catch (Exception ex) {
      if (ex instanceof PluginException) {
	return;
      }
      fail("createLinkRewriter should have thrown PluginException but threw " +
	   ex.toString());
    }
  }

  public void testThrowsIfNoPort() {
    in = new ReaderInputStream(new StringReader(page));
    setupConfig(false);
    try {
      InputStream ret = nfhlrf.createLinkRewriter("text/html", au, in,
						  encoding, url);
      fail("createLinkRewriter should have thrown if no port");
    } catch (Exception ex) {
      if (ex instanceof PluginException) {
	return;
      }
      fail("createLinkRewriter should have thrown PluginException but threw " +
	   ex.toString());
    }
  }

  public void testRewriting() {
    in = new ReaderInputStream(new StringReader(page));
    setupConfig(true);
    try {
      InputStream ret = nfhlrf.createLinkRewriter("text/html", au, in,
						 encoding, url);
      assertNotNull(ret);
      // Read from ret, make String
      Reader r = new InputStreamReader(ret);
      StringBuffer sb = new StringBuffer();
      char[] buf = new char[4096];
      int i;
      while ((i = r.read(buf)) > 0) {
	sb.append(buf, 0, i);
      }
      String out = sb.toString();
      assertNotNull(out);
      log.debug3(out);
      String rewriteTag = "ServeContent?url=";
      // Now check the rewriting
      int ix = 0;
      for (i = 0; i < linkCount; i++) {
	int nix = out.indexOf(rewriteTag, ix);
	assertTrue("Start of rewritten url not found", nix > ix);
	int endix = out.indexOf("\"", nix);
	assertTrue("End of rewritten url not found", endix > nix);
	String rewritten = out.substring(nix, endix);
	log.debug3("Link " + i + " rewritten: " + rewritten);
	// Make sure no double rewrites
	assertEquals("Multiple rewrite " + rewritten,
		     rewritten.indexOf(rewriteTag),
		     rewritten.lastIndexOf(rewriteTag));
	ix = endix;
      }
      log.debug3("End of links at " + out.substring(ix, ix+32));
      for (i = 0; i < importCount; i++) {
	int nix = out.indexOf(rewriteTag, ix);
	assertTrue("Start of rewritten import not found", nix > ix);
	int endix = out.indexOf(")", nix);
	assertTrue("End of rewritten import not found", endix > nix);
	String rewritten = out.substring(nix, endix);
	log.debug3("Import rewritten " + i + " : " + rewritten);
	// Make sure no double rewrites
	assertEquals("Multiple rewrite " + rewritten,
		     rewritten.indexOf(rewriteTag),
		     rewritten.lastIndexOf(rewriteTag));
	ix = endix;
      }
      ix = out.indexOf(rewriteTag + urlStem, ix);
      if (ix >= 0) {
	log.error(out.substring(ix));
      }
      assertTrue("wrong url rewritten", ix < 0);
    } catch (Exception ex) {
      fail("createLinkRewriter should not have thrown " + ex +
	   " on html mime type");
    }
  }

  private void setupConfig(boolean good) {
    Properties props = new Properties();
    if (good) {
      props.setProperty(ContentServletManager.PARAM_PORT, "9524");
      ConfigurationUtil.setCurrentConfigFromProps(props);
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {
      TestNodeFilterHtmlLinkRewriterFactory.class.getName()
    };
    junit.textui.TestRunner.main(testCaseList);
  }

}
