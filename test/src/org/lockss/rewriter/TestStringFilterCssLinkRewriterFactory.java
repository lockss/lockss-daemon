/*
 * $Id: TestStringFilterCssLinkRewriterFactory.java,v 1.2 2008-09-18 02:10:23 dshr Exp $
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

public class TestStringFilterCssLinkRewriterFactory extends LockssTestCase {
  static Logger log =
      Logger.getLogger("TestStringFilterCssLinkRewriterFactory");

  private MockArchivalUnit au;
  private StringFilterCssLinkRewriterFactory sfclrf;
  private String encoding = null;
  private static final String urlStem = "http://www.example.com/site/";
  private static final String urlSuffix = "css/";
  private static final String url = urlStem + "test/example.css";
  private static final String page =
    "table.gallery {\n" +
      "border:  1px solid #cccccc;\n" +
      "margin:  2px;\n" +
      "padding: 2px;\n" +
      "background-color:#ffffff;\n" +
    "}\n" +
      /* "Some random crap\n" + */
      /* "Much more random crap then the earlier tests used" + */
    "@import url(" + urlStem + urlSuffix + "file1.css) /* absolute link to rewrite */\n" +
    "@import url(http://www.content.org/file2.css) /* absolute link no rewrite */\n" +
    "@import url(" + urlSuffix + "file3.css) /* relative link to rewrite */\n" +
    "@import url(../style/file4.css) /* relative link to rewrite */\n" +
    "@import url(/style/file5.css) /* site relative link to rewrite */\n" +
    "@import url()\n";
  private static final String servlet = "ServeContent?url=";    
  private static final int linkCount = 4;
    private static final String testFile = "file0.css";
  private static final String absImportBefore =
      "@import url(" + urlStem + urlSuffix + testFile + ")";
  private static final String importAfter =
      servlet + urlStem + urlSuffix + testFile;
  private static final String pageRelImportBefore =
      "@import url(" + urlSuffix + testFile + ")";
  private static final String pageRelImportAfter =
      servlet + urlStem + "test/" + urlSuffix+ testFile ;
  private static final String dotDotRelImportBefore =
      "@import url(../" + urlSuffix + testFile + ")";
  private static final String dotDotRelImportAfter =
      servlet + urlStem + "test/../" + urlSuffix + testFile ;
  private static final String siteRelImportBefore =
      "@import url(/" + urlSuffix + testFile + ")";
  private static final String siteRelImportAfter =
      servlet + urlStem + "test/../../" + urlSuffix + testFile;
  private Reader in;
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
    sfclrf = new StringFilterCssLinkRewriterFactory();
  }

  public void testThrowsIfNotCss() {
    in = new StringReader(page);
    setupConfig(true);
    try {
      Reader r = sfclrf.createLinkRewriterReader("text/html", au, in,
						  encoding, url, xform);
      fail("createLinkRewriter should have thrown on non-css mime type");
    } catch (Exception ex) {
      if (ex instanceof PluginException) {
	return;
      }
      fail("createLinkRewriter should have thrown PluginException but threw " +
	   ex.toString());
    }
  }

  public void testThrowsIfNoPort() {
    in = new StringReader(page);
    setupConfig(false);
    try {
      Reader r = sfclrf.createLinkRewriterReader("text/css", au, in,
						  encoding, url, xform);
      fail("createLinkRewriter should have thrown if no port");
    } catch (Exception ex) {
      if (ex instanceof PluginException) {
	return;
      }
      fail("createLinkRewriter should have thrown PluginException but threw " +
	   ex.toString());
    }
  }

  public void testAbsRewriting() {
    in = new StringReader(absImportBefore);
    log.debug3("page is " + absImportBefore);
    setupConfig(true);
    try {
      Reader r = sfclrf.createLinkRewriterReader("text/css", au, in,
						 encoding, url, xform);
      assertNotNull(r);
      // Read from r, make String
      StringBuffer sb = new StringBuffer();
      char[] buf = new char[4096];
      int i;
      while ((i = r.read(buf)) > 0) {
	sb.append(buf, 0, i);
      }
      String out = sb.toString();
      assertNotNull(out);
      log.debug3("rewritten page is\n" + out);
      // Now check the rewriting
      assertTrue("Abs import bad rewrite " + out, out.indexOf(importAfter) > 0);
    } catch (Exception ex) {
      fail("createLinkRewriter should not have thrown " + ex +
	   " on css mime type");
    }
  }

  public void testPageRelRewriting() {
    in = new StringReader(pageRelImportBefore);
    log.debug3("page is " + pageRelImportBefore + " url is " + url);
    setupConfig(true);
    try {
      Reader r = sfclrf.createLinkRewriterReader("text/css", au, in,
						 encoding, url, xform);
      assertNotNull(r);
      // Read from r, make String
      StringBuffer sb = new StringBuffer();
      char[] buf = new char[4096];
      int i;
      while ((i = r.read(buf)) > 0) {
	sb.append(buf, 0, i);
      }
      String out = sb.toString();
      assertNotNull(out);
      log.debug3("rewritten page is\n" + out);
      log.debug3("should match\n" + pageRelImportAfter);
      // Now check the rewriting
      assertTrue("Rel import bad rewrite " + out,
		 out.indexOf(pageRelImportAfter) > 0);
    } catch (Exception ex) {
      fail("createLinkRewriter should not have thrown " + ex +
	   " on css mime type");
    }
  }

  public void testDotDotRelRewriting() {
    in = new StringReader(dotDotRelImportBefore);
    log.debug3("page is " + dotDotRelImportBefore + " url is " + url);
    setupConfig(true);
    try {
      Reader r = sfclrf.createLinkRewriterReader("text/css", au, in,
						 encoding, url, xform);
      assertNotNull(r);
      // Read from r, make String
      StringBuffer sb = new StringBuffer();
      char[] buf = new char[4096];
      int i;
      while ((i = r.read(buf)) > 0) {
	sb.append(buf, 0, i);
      }
      String out = sb.toString();
      assertNotNull(out);
      log.debug3("rewritten page is\n" + out);
      log.debug3("should match\n" + dotDotRelImportAfter);
      // Now check the rewriting
      assertTrue("Rel import bad rewrite " + out,
		 out.indexOf(dotDotRelImportAfter) > 0);
    } catch (Exception ex) {
      fail("createLinkRewriter should not have thrown " + ex +
	   " on css mime type");
    }
  }

  public void testSiteRelRewriting() {
    in = new StringReader(siteRelImportBefore);
    log.debug3("page is " + siteRelImportBefore + " url is " + url);
    setupConfig(true);
    try {
      Reader r = sfclrf.createLinkRewriterReader("text/css", au, in,
						 encoding, url, xform);
      assertNotNull(r);
      // Read from r, make String
      StringBuffer sb = new StringBuffer();
      char[] buf = new char[4096];
      int i;
      while ((i = r.read(buf)) > 0) {
	sb.append(buf, 0, i);
      }
      String out = sb.toString();
      assertNotNull(out);
      log.debug3("rewritten page is\n" + out);
      log.debug3("should match\n" + siteRelImportAfter);
      // Now check the rewriting
      assertTrue("Rel import bad rewrite " + out,
		 out.indexOf(siteRelImportAfter) > 0);
    } catch (Exception ex) {
      fail("createLinkRewriter should not have thrown " + ex +
	   " on css mime type");
    }
  }

  public void testRewriting() {
    in = new StringReader(page);
    setupConfig(true);
    log.debug3("page is \n" + page);
    try {
      Reader r = sfclrf.createLinkRewriterReader("text/css", au, in,
						 encoding, url, xform);
      assertNotNull(r);
      // Read from r, make String
      StringBuffer sb = new StringBuffer();
      char[] buf = new char[4096];
      int i;
      while ((i = r.read(buf)) > 0) {
	sb.append(buf, 0, i);
      }
      String out = sb.toString();
      assertNotNull(out);
      log.debug3("rewritten page is\n" + out);
      // Now check the rewriting
      int ix = 0;
      for (i = 0; i < linkCount; i++) {
	int nix = out.indexOf(servlet + urlStem, ix);
	assertTrue("Start of rewritten url not found", nix > ix);
	int endix = out.indexOf(")", nix);
	assertTrue("End of rewritten url not found", endix > nix);
	log.debug3("Link rewritten: " + out.substring(nix, endix));
	ix = endix;
      }
      ix = out.indexOf("ServeContent?url=" + urlStem, ix);
      if (ix >= 0) {
	log.error(out.substring(ix));
      }
      assertTrue("wrong url rewritten", ix < 0);
    } catch (Exception ex) {
      fail("createLinkRewriter should not have thrown " + ex +
	   " on css mime type");
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
      TestStringFilterCssLinkRewriterFactory.class.getName()
    };
    junit.textui.TestRunner.main(testCaseList);
  }

}
