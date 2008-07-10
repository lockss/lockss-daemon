/*
 * $Id: TestJavascriptHtmlLinkRewriterFactory.java,v 1.3 2008-07-10 03:50:32 dshr Exp $
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

public class TestJavascriptHtmlLinkRewriterFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("FuncArcExploder");

  private MockArchivalUnit au;
  private JavascriptHtmlLinkRewriterFactory jhlrf;
  private String encoding = null;
  private static final String urlStem = "http://www.example.com/";
  private static final String url = urlStem + "index.html";
  private static final String withHtmlTag =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "<title>example.com website</title>\n" +
    "<meta http-equiv=\"content-type\"" +
    "content=\"text/html; charset=ISO-8859-1\">" +
    "</head>" +
    "<body>" +
    "<h1 align=\"center\">example.com website</h1>" +
    "<br>" +
    "<a href=\"" + url + "\">a link</a>" +
    "<br>" +
    "</body>" +
    "</HTML>";
  private static final String withoutHtmlTag =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
    "<html>\n" +
    "<head>\n" +
    "<title>example.com website</title>\n" +
    "<meta http-equiv=\"content-type\"" +
    "content=\"text/html; charset=ISO-8859-1\">" +
    "</head>" +
    "<body>" +
    "<h1 align=\"center\">example.com website</h1>" +
    "<br>" +
    "<a href=\"" + url + "\">a link</a>" +
    "<br>" +
    "</body>" +
    "</foo>";
  private InputStream in;
  private static final String jsTag = "<SCRIPT language=\"Javascript\">";

  public void setUp() throws Exception {
    super.setUp();
    au = new MockArchivalUnit();
    List l = new ArrayList();
    l.add(urlStem);
    au.setUrlStems(l);
    jhlrf = new JavascriptHtmlLinkRewriterFactory();
  }

  public void testThrowsIfNotHtml() {
    in = new ReaderInputStream(new StringReader(withHtmlTag));
    setupConfig(true);
    try {
      InputStream ret = jhlrf.createLinkRewriter("application/pdf", au, in,
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
    in = new ReaderInputStream(new StringReader(withHtmlTag));
    setupConfig(false);
    try {
      InputStream ret = jhlrf.createLinkRewriter("text/html", au, in,
						 encoding, url);
      fail("createLinkRewriter should have thrown without port");
    } catch (Exception ex) {
      if (ex instanceof PluginException) {
	return;
      }
      fail("createLinkRewriter should have thrown PluginException but threw " +
	   ex.toString());
    }
  }

  public void testInsertsJavascriptIfHtmlTag() {
    in = new ReaderInputStream(new StringReader(withHtmlTag));
    setupConfig(true);
    try {
      InputStream ret = jhlrf.createLinkRewriter("text/html", au, in,
						 encoding, url);
      assertNotNull(ret);
      // Read from ret, look for javascript
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
      assertTrue(out.length() > withHtmlTag.length());
      assertTrue(out.indexOf(jsTag) > 0);
    } catch (Exception ex) {
      fail("createLinkRewriter should not have thrown " + ex +
	   " on html mime type");
    }
  }

  public void testCopiesUnchangedIfNoHtmlTag() {
    in = new ReaderInputStream(new StringReader(withoutHtmlTag));
    setupConfig(true);
    try {
      InputStream ret = jhlrf.createLinkRewriter("text/html", au, in,
						 encoding, url);
      assertNotNull(ret);
      // Read from ret, look for javascript
      Reader r = new InputStreamReader(ret);
      StringBuffer sb = new StringBuffer();
      char[] buf = new char[4096];
      int i;
      while ((i = r.read(buf)) > 0) {
	sb.append(buf, 0, i);
      }
      String out = sb.toString();
      assertNotNull(out);
      assertTrue(out.length() == withoutHtmlTag.length());
      assertTrue(out.indexOf(jsTag) < 0);
      assertTrue(out.equals(withoutHtmlTag));
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
      TestJavascriptHtmlLinkRewriterFactory.class.getName()
    };
    junit.textui.TestRunner.main(testCaseList);
  }

}
