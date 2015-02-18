/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.filter.html;

import java.io.*;
import java.util.*;
import org.htmlparser.*;
import org.htmlparser.tags.*;
import org.htmlparser.util.*;

import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.filter.html.HtmlNodeFilters.AllExceptSubtreeNodeFilter;
import org.lockss.servlet.ServletUtil;

public class TestHtmlNodeFilters extends LockssTestCase {
  
  static Logger log = Logger.getLogger(TestHtmlNodeFilters.class);

  public void testAssumptions() throws Exception {
    NodeList nl = parse("<option value=\"val1\">blue 13</option>");
    Node node = nl.elementAt(0);
    assertTrue(node instanceof CompositeTag);
    assertEquals("blue 13", ((CompositeTag)node).getStringText());
    assertEquals("option value=\"val1\"", node.getText());

    nl = parse("some text");
    node = nl.elementAt(0);
    assertFalse(node instanceof CompositeTag);
    assertEquals("some text", node.getText());
  }

  public void testIll() {
    try {
      HtmlNodeFilters.tagWithAttribute(null, "attr", "aval");
      fail("null filter should throw");
    } catch(NullPointerException e) {
    }
    try {
      HtmlNodeFilters.tagWithAttribute("atag", null, "aval");
      fail("null filter should throw");
    } catch(NullPointerException e) {
    }
  }

  public void testTagWithAttributeWithValue() throws Exception {
    NodeFilter filt = HtmlNodeFilters.tagWithAttribute("div", "attr", "aval");
    assertFalse(filt.accept(divWithAttr("foo", "bar")));
    assertFalse(filt.accept(divWithAttr("attr", "bar")));
    assertFalse(filt.accept(divWithAttr("btag", "aval")));
    assertTrue(filt.accept(divWithAttr("attr", "aval")));
  }

  public void testTagWithAttributeWithoutValue() throws Exception {
    NodeFilter filt = HtmlNodeFilters.tagWithAttribute("div", "attr");
    assertFalse(filt.accept(divWithAttr("foo", "bar")));
    assertFalse(filt.accept(divWithAttr("btag", "aval")));
    assertTrue(filt.accept(divWithAttr("attr", "aval")));
    assertTrue(filt.accept(divWithAttr("attr", "bar")));
    assertTrue(filt.accept(divWithAttr("attr", "qux")));
  }

  public void testDivWithAttribute() throws Exception {
    NodeFilter filt = HtmlNodeFilters.divWithAttribute("attr", "aval");
    assertFalse(filt.accept(divWithAttr("attr", "bar")));
    assertTrue(filt.accept(divWithAttr("attr", "aval")));
  }

  public void testAttributeRegex() throws Exception {
    NodeFilter filt =
      HtmlNodeFilters.tagWithAttributeRegex("div", "attr", "a+b");
    assertFalse(filt.accept(divWithAttr("attr", "ba")));
    assertTrue(filt.accept(divWithAttr("attr", "ab")));
    assertTrue(filt.accept(divWithAttr("attr", "abb")));
    assertTrue(filt.accept(divWithAttr("attr", "abbb")));
    assertTrue(filt.accept(divWithAttr("attr", "abbbbc")));
    assertTrue(filt.accept(divWithAttr("attr", "xxabbbbc")));
    NodeFilter filt2 =
      HtmlNodeFilters.tagWithAttributeRegex("div", "attr", "^a+b$");
    assertTrue(filt.accept(divWithAttr("attr", "abbb")));
    assertFalse(filt2.accept(divWithAttr("attr", "xxabbbb")));
    assertFalse(filt2.accept(divWithAttr("attr", "abbbbc")));
  }

  public void testTagWithText() throws Exception {
    String opt = "This article is cited by the following articles in ...";
    NodeFilter filt = HtmlNodeFilters.tagWithText("option",
						  "article is cited by");
    NodeList nl = parse("<b><option value=\"#citart1\">" + opt + "</option>");
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals("Should have one element: " + nl, 1, nl.size());
    Node node = nl.elementAt(0);
    assertTrue(node instanceof OptionTag);
    assertEquals(opt, ((OptionTag)node).getStringText());

    nl = parse("<b><script value=\"#citart1\">" + opt + "</script>");
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals("should be empty: " + nl, 0, nl.size());

    nl = parse("some text");
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals("Should be empty: " + nl, 0, nl.size());

    nl = parse("<b><option value=\"#citart1\">this article isn't cited by anyone</option>");
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals("should be empty: " + nl, 0, nl.size());

  }

  public void testEmptyComposite() throws Exception {
    NodeFilter filt = HtmlNodeFilters.tagWithText("p",
						  "a paragraph text");
    NodeList nl = parse("foo<p class=\"cls\"/>bar");
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals(0, nl.size());
  }

  public void testTagWithTextRegex() throws Exception {
    String opt = "This article is cited by the following articles in ...";
    NodeFilter filt =
      HtmlNodeFilters.tagWithTextRegex("option",
				       "article [is]+ cited by.*ll.*l");
    NodeList nl = parse("<b><option value=\"#citart1\">" + opt + "</option>");
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals("Should have one element: " + nl, 1, nl.size());
    Node node = nl.elementAt(0);
    assertTrue(node instanceof OptionTag);
    assertEquals(opt, ((OptionTag)node).getStringText());

    nl = parse("<b><script value=\"#citart1\">" + opt + "</script>");
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals("should be empty: " + nl, 0, nl.size());

    nl = parse("some text");
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals("Should be empty: " + nl, 0, nl.size());

    nl = parse("<b><option value=\"#citart1\">this article isn't cited by anyone</option>");
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals("should be empty: " + nl, 0, nl.size());

  }

  public void testEmptyCompositeRegex() throws Exception {
    NodeFilter filt = HtmlNodeFilters.tagWithTextRegex("p",
						       ".*text");
    NodeList nl = parse("foo<p class=\"cls\"/>bar");
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals(0, nl.size());
  }

  public void testComment() throws Exception {
    NodeFilter filt = HtmlNodeFilters.comment();
    NodeList nl = parse("foo<b>bar baz qux <!-- comment1 -->");
    nl = parse("foo<b>bar<!-- comment -->baz qux<!-- \n multi \n line \n comment \n -->fred garply");
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals("Should have two elements: " + nl, 2, nl.size());
    Node node = nl.elementAt(0);
    assertTrue(node instanceof org.htmlparser.nodes.RemarkNode);
    assertEquals(" comment ", node.getText());
    node = nl.elementAt(1);
    assertTrue(node instanceof org.htmlparser.nodes.RemarkNode);
    assertEquals(" \n multi \n line \n comment \n ", node.getText());
  }

  public void testCommentWithString() throws Exception {
    NodeFilter filt = HtmlNodeFilters.commentWithString("sub String");
    NodeList nl = parse("foo<b>bar sub String <!-- dub String -->");
    assertEquals("Should be empty: " + nl,
		 0, nl.extractAllNodesThatMatch(filt).size());
    nl = parse("foo<b>bar<!-- sub String -->baz");
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals("Should have one element: " + nl, 1, nl.size());
    Node node = nl.elementAt(0);
    assertTrue(node instanceof org.htmlparser.nodes.RemarkNode);
    assertEquals(" sub String ", node.getText());
  }

  public void testCommentWithStringCase() throws Exception {
    NodeFilter filt = HtmlNodeFilters.commentWithString("sub String");
    NodeList nl = parse("foo<b>bar<!-- Sub string -->baz");
    nl = parse("foo<b>bar<!-- Sub string -->baz");
    assertEquals("Should be empty: " + nl,
		 0, nl.extractAllNodesThatMatch(filt).size());
    filt = HtmlNodeFilters.commentWithString("sub String", true);
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals("Should have one element: " + nl, 1, nl.size());
    Node node = nl.elementAt(0);
    assertTrue(node instanceof org.htmlparser.nodes.RemarkNode);
    assertEquals(" Sub string ", node.getText());
  }

  public void testCommentWithRegex() throws Exception {
    NodeFilter filt = HtmlNodeFilters.commentWithRegex("Begin ad [0-9]+");
    NodeList nl = parse("foo<b>bar Begin ad 27 <!-- Begin ad x -->");
    assertEquals("Should be empty: " + nl,
		 0, nl.extractAllNodesThatMatch(filt).size());
    nl = parse("foo<b>bar<!-- Begin ad 42 -->baz");
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals("Should have one element: " + nl, 1, nl.size());
    Node node = nl.elementAt(0);
    assertTrue(node instanceof org.htmlparser.nodes.RemarkNode);
    assertEquals(" Begin ad 42 ", node.getText());
  }

  public void testCommentWithRegexCase() throws Exception {
    NodeFilter filt = HtmlNodeFilters.commentWithRegex("begin Ad [0-9]+");
    NodeList nl = parse("foo<b>bar Begin ad 27 <!-- Begin ad 3 foo -->");
    assertEquals("Should be empty: " + nl,
		 0, nl.extractAllNodesThatMatch(filt).size());

    filt = HtmlNodeFilters.commentWithRegex("begin Ad [0-9]+", true);
    nl = nl.extractAllNodesThatMatch(filt);
    assertEquals("Should have one element: " + nl, 1, nl.size());
    Node node = nl.elementAt(0);
    assertTrue(""+node.getClass(),
	       node instanceof org.htmlparser.nodes.RemarkNode);
    assertEquals(" Begin ad 3 foo ", node.getText());
  }

  // Transforms

  public void testUrlEncode() throws Exception {
    HtmlNodeFilters.RefreshRegexXform xf =
      new HtmlNodeFilters.RefreshRegexXform("^http://", true, "^/", "foo");
    assertEquals("", xf.urlEncode(""));
    assertEquals("no url arg", xf.urlEncode("no url arg"));
    assertEquals("?url=http%3A%2F%2Ffoo.bar%2Fpath%2Ffile.html",
		 xf.urlEncode("?url=http://foo.bar/path/file.html"));
    assertEquals("?url=http%3A%2F%2Ffoo.bar%2Fpath%2Ffile.html#ref",
		 xf.urlEncode("?url=http://foo.bar/path/file.html#ref"));
    // end of a quoted url
    assertEquals("?url=http%3A%2F%2Ffoo.bar%2F\")oth/er",
		 xf.urlEncode("?url=http://foo.bar/\")oth/er"));
    // end of a css url expr
    assertEquals("?url=http%3A%2F%2Ffoo.bar%2F)oth/er",
		 xf.urlEncode("?url=http://foo.bar/)oth/er", true));
  }

  private static final String page =
    "foo<a href=\"http://www.example.com/index.html\">bar</a>baz";
  private static final String origUrl = "http://www.example.com/index.html";
  private static final String finalUrl = "http://foo.lockss.org/index.html";
  private static final  String[] attrs = {
    "href",
    "src",
  };

  public void testLinkRegexYesXformsNoMatch() throws Exception {
    NodeList nl = parse(page);
    String[] linkRegex = {
      "http://www.content.org/",
    };
    boolean[] ignoreCase = {
      true,
    };
    String[] rewriteRegex = {
      "http://www.content.org/",
    };
    String[] rewriteTarget = {
      "http://foo.lockss.org/",
    };
    NodeFilter filt = HtmlNodeFilters.linkRegexYesXforms(linkRegex,
							 ignoreCase,
							 rewriteRegex,
							 rewriteTarget,
							 attrs);
    assertEquals("Should be empty: " + nl,
		 0, nl.extractAllNodesThatMatch(filt).size());
    Node node = nl.elementAt(1);
    assertNotNull(node);
    assertTrue(""+node.getClass(),
	       node instanceof org.htmlparser.tags.LinkTag);
    assertEquals(origUrl, ((LinkTag)node).extractLink());
  }

  public void testLinkRegexYesXformsMatch() throws Exception {
    NodeList nl = parse(page);
    String[] linkRegex = {
      "http://www.example.com/",
    };
    boolean[] ignoreCase = {
      true,
    };
    String[] rewriteRegex = {
      "http://www.example.com/",
    };
    String[] rewriteTarget = {
      "http://foo.lockss.org/",
    };
    log.debug3("testLinkRegexYesXformsMatch before " + nl.toHtml());
    NodeFilter filt = HtmlNodeFilters.linkRegexYesXforms(linkRegex,
							 ignoreCase,
							 rewriteRegex,
							 rewriteTarget,
							 attrs);
    assertEquals("Should be empty: " + nl,
		 0, nl.extractAllNodesThatMatch(filt).size());
    // Don't use node.getLink() as it caches its result so doesn't reflect
    // the xform
    assertEquals(finalUrl, ((LinkTag)nl.elementAt(1)).extractLink());
  }

  static String[] arr(String... x) {
    return x;
  }

  static boolean[] arr(boolean... x) {
    return x;
  }

  public void testLinkRegexNoXformsMatch() throws Exception {
    NodeList nl = parse(page);
    String[] linkRegex = {
      "http://www.example.com/",
    };
    boolean[] ignoreCase = {
      true,
    };
    String[] rewriteRegex = {
      "http://www.example.com/",
    };
    String[] rewriteTarget = {
      "http://foo.lockss.org/",
    };
    NodeFilter filt = HtmlNodeFilters.linkRegexNoXforms(linkRegex,
							ignoreCase,
							rewriteRegex,
							rewriteTarget,
							attrs);
    assertEquals("Should be empty: " + nl,
		 0, nl.extractAllNodesThatMatch(filt).size());
    Node node = nl.elementAt(1);
    assertNotNull(node);
    assertTrue(""+node.getClass(),
	       node instanceof org.htmlparser.tags.LinkTag);
    assertEquals(origUrl, ((LinkTag)node).extractLink());
  }

  private static final String metapage =
    "<meta name=\"abc\" content=\"http://www.example.com/index.html\">\n" +
    "<meta name=\"def\" content=\"http://www.example42.com/index.html\">\n" +
    "<meta name=\"def\" content=\"http://www.example.com/index.html\">\n";

  public void testMetaTagRegexYesXforms() throws Exception {
    NodeFilter filt =
      HtmlNodeFilters.metaTagRegexYesXforms(arr("http://www.example.com/"),
					    arr(true),
					    arr("http://www.example.com/"),
					    arr("http://foo.lockss.org/"),
					    ListUtil.list("aaa", "def"));

    NodeList nl = parse(metapage);

    assertEquals("Should be empty: " + nl,
		 0, nl.extractAllNodesThatMatch(filt).size());
    // meta name not in names, no replace
    Node node = nl.elementAt(0);
    assertClass(org.htmlparser.tags.MetaTag.class, node);
    assertEquals("abc", ((MetaTag)node).getAttribute("name"));
    assertEquals(origUrl, ((MetaTag)node).getAttribute("content"));

    // orig URL doesn't match, no replace
    node = nl.elementAt(2);
    assertClass(org.htmlparser.tags.MetaTag.class, node);
    assertEquals("def", ((MetaTag)node).getAttribute("name"));
    assertEquals("http://www.example42.com/index.html",
		 ((MetaTag)node).getAttribute("content"));

    // name in names, URL matches, should get replaced
    node = nl.elementAt(4);
    assertClass(org.htmlparser.tags.MetaTag.class, node);
    assertEquals("def", ((MetaTag)node).getAttribute("name"));
    assertEquals("http://foo.lockss.org/index.html",
		 ((MetaTag)node).getAttribute("content"));
  }

  public void testEmptyStyleTagDispatch() throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    MockLinkRewriterFactory lrf = new MockLinkRewriterFactory();    
    String src =
      "foo <style type=\"text/css\" media=\"screen\"></style>\nbar\n";
    String exp =
      "foo <style type=\"text/css\" media=\"screen\"></style>\nbar\n";
    String base = "http://example.com/base/";
    ServletUtil.LinkTransform linkXform = new ServletUtil.LinkTransform() {
	public String rewrite(String url) {
	  return "rewritten";
	}};

    mau.setLinkRewriterFactory("text/css", lrf);

    HtmlNodeFilters.StyleTagXformDispatch xform =
      new HtmlNodeFilters.StyleTagXformDispatch(mau, null,
						base, linkXform);
    NodeList nl = parse(src);
    assertEquals(0, nl.extractAllNodesThatMatch(xform).size());
    assertEquals(exp, nl.toHtml());
    assertEmpty("LinkRewriterFactory should not have been invoked",
		lrf.getArgLists());
  }

  public void testStyleTagWithSrcNoDispatch() throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    MockLinkRewriterFactory lrf = new MockLinkRewriterFactory();    
    String src =
      "foo <style type=\"text/css\" src=\"foo.css\">xxx</style>\nbar\n";
    String exp =
      "foo <style type=\"text/css\" src=\"foo.css\">xxx</style>\nbar\n";
    String base = "http://example.com/base/";
    ServletUtil.LinkTransform linkXform = new ServletUtil.LinkTransform() {
	public String rewrite(String url) {
	  return "rewritten";
	}};

    mau.setLinkRewriterFactory("text/css", lrf);

    HtmlNodeFilters.StyleTagXformDispatch xform =
      new HtmlNodeFilters.StyleTagXformDispatch(mau, null,
						base, linkXform);
    lrf.setLinkRewriter(new StringInputStream("shouldn't"));
    NodeList nl = parse(src);
    assertEquals(0, nl.extractAllNodesThatMatch(xform).size());
    assertEquals(exp, nl.toHtml());
    assertEmpty("LinkRewriterFactory should not have been invoked",
		lrf.getArgLists());
  }

  public void testStyleTagDispatch(String charset) throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    MockLinkRewriterFactory lrf = new MockLinkRewriterFactory();    
    String src =
      "foo <style type=\"text/css\" media=\"screen\">\n@import \"/resource/css/hw.css\";\n@import \"/resource/css/btcint.css\";\n</style>\nbar\n";
    String exp =
      "foo <style type=\"text/css\" media=\"screen\">result string</style>\nbar\n";
    String res = "result string";
    String base = "http://example.com/base/";
    ServletUtil.LinkTransform linkXform = new ServletUtil.LinkTransform() {
	public String rewrite(String url) {
	  return "rewritten";
	}};

    mau.setLinkRewriterFactory("text/css", lrf);

    HtmlNodeFilters.StyleTagXformDispatch xform =
      new HtmlNodeFilters.StyleTagXformDispatch(mau, charset,
						base, linkXform);
    lrf.setLinkRewriter(new StringInputStream(res));
    NodeList nl = parse(src);
    assertEquals(0, nl.extractAllNodesThatMatch(xform).size());
    assertEquals(exp, nl.toHtml());
    List args = lrf.getArgLists().get(0);
    assertEquals("text/css", args.get(0));
    assertEquals(mau, args.get(1));
    assertEquals("\n@import \"/resource/css/hw.css\";\n@import \"/resource/css/btcint.css\";\n",
		 StringUtil.fromInputStream((InputStream)args.get(2)));
    assertEquals(charset == null ? Constants.DEFAULT_ENCODING : charset,
		 args.get(3));
  }

  public void testStyleTagDispatch() throws Exception {
    testStyleTagDispatch("UTF-8");
  }

  public void testStyleTagDispatchNoCharset() throws Exception {
    testStyleTagDispatch(null);
  }

  public void testStyleAttrDispatch() throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    MockLinkRewriterFactory lrf = new MockLinkRewriterFactory();    
    String src = "<span class=\"foo\" "
      + "style=\"background: url('/backg.png') no-repeat 0px -64px;\" />";
    String exp = "<span class=\"foo\" "
      + "style=\"result string\" />";
    String res = "result string";
    String base = "http://example.com/base/";
    ServletUtil.LinkTransform linkXform = new ServletUtil.LinkTransform() {
	public String rewrite(String url) {
	  return "rewritten";
	}};

    mau.setLinkRewriterFactory("text/css", lrf);

    HtmlNodeFilters.StyleAttrXformDispatch xform =
      new HtmlNodeFilters.StyleAttrXformDispatch(mau, null,
						 base, linkXform);
    lrf.setLinkRewriter(new StringInputStream(res));
    NodeList nl = parse(src);
    assertEquals(0, nl.extractAllNodesThatMatch(xform).size());
    assertEquals(exp, nl.toHtml());
    List args = lrf.getArgLists().get(0);
    assertEquals("text/css", args.get(0));
    assertEquals(mau, args.get(1));
    assertEquals("background: url('/backg.png') no-repeat 0px -64px;",
		 StringUtil.fromInputStream((InputStream)args.get(2)));
  }


  public void testEmptyScriptDispatch() throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    MockLinkRewriterFactory lrf = new MockLinkRewriterFactory();    
    String src =
      "foo <script type=\"text/javascript\"></script>\nbar\n";
    String exp =
      "foo <script type=\"text/javascript\"></script>\nbar\n";
    String base = "http://example.com/base/";
    ServletUtil.LinkTransform linkXform = new ServletUtil.LinkTransform() {
	public String rewrite(String url) {
	  return "rewritten";
	}};

    mau.setLinkRewriterFactory("text/css", lrf);

    HtmlNodeFilters.ScriptXformDispatch xform =
      new HtmlNodeFilters.ScriptXformDispatch(mau, null,
					     base, linkXform);
    NodeList nl = parse(src);
    assertEquals(0, nl.extractAllNodesThatMatch(xform).size());
    assertEquals(exp, nl.toHtml());
    assertEmpty("LinkRewriterFactory should not have been invoked",
		lrf.getArgLists());
  }

  public void testScriptDispatch(String src, String exp,
				 String charset) throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    MockLinkRewriterFactory lrf = new MockLinkRewriterFactory();    
    String res = "\nresult string\n";
    String base = "http://example.com/base/";
    ServletUtil.LinkTransform linkXform = new ServletUtil.LinkTransform() {
	public String rewrite(String url) {
	  return "rewritten";
	}};

    mau.setLinkRewriterFactory("text/javascript", lrf);

    HtmlNodeFilters.ScriptXformDispatch xform =
      new HtmlNodeFilters.ScriptXformDispatch(mau, charset,
					     base, linkXform);
    lrf.setLinkRewriter(new StringInputStream(res));
    NodeList nl = parse(src);
    assertEquals(0, nl.extractAllNodesThatMatch(xform).size());
    assertEquals(exp, nl.toHtml());
    List args = lrf.getArgLists().get(0);
    assertEquals("text/javascript", args.get(0));
    assertEquals(mau, args.get(1));
    assertEquals("\norig script;\n",
		 StringUtil.fromInputStream((InputStream)args.get(2)));
    assertEquals(charset == null ? Constants.DEFAULT_ENCODING : charset,
		 args.get(3));
  }

  public void testScriptDispatch(String src, String exp)
      throws Exception {
    testScriptDispatch(src, exp, "UTF-8");
    testScriptDispatch(src, exp, null);
  }

  public void testScriptDispatchWType(String src, String exp)
      throws Exception {
    testScriptDispatch("foo <script type=\"text/javascript\">\norig script;\n</script>\nbar\n",
		       "foo <script type=\"text/javascript\">\nresult string\n</script>\nbar\n");
  }

  public void testScriptDispatchWLang(String src, String exp)
      throws Exception {
    testScriptDispatch("foo <script language=\"javascript\">\norig script;\n</script>\nbar\n",
		       "foo <script language==\"javascript\">\nresult string\n</script>\nbar\n");
  }

  public void testScriptWithSrcNoDispatch() throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    MockLinkRewriterFactory lrf = new MockLinkRewriterFactory();    
    String src =
      "foo <script type=\"text/css\" src=\"foo.css\">xxx</script>\nbar\n";
    String exp =
      "foo <script type=\"text/css\" src=\"foo.css\">xxx</script>\nbar\n";
    String base = "http://example.com/base/";
    ServletUtil.LinkTransform linkXform = new ServletUtil.LinkTransform() {
	public String rewrite(String url) {
	  return "rewritten";
	}};

    mau.setLinkRewriterFactory("text/css", lrf);

    HtmlNodeFilters.ScriptXformDispatch xform =
      new HtmlNodeFilters.ScriptXformDispatch(mau, null,
					      base, linkXform);
    lrf.setLinkRewriter(new StringInputStream("shouldn't"));
    NodeList nl = parse(src);
    assertEquals(0, nl.extractAllNodesThatMatch(xform).size());
    assertEquals(exp, nl.toHtml());
    assertEmpty("LinkRewriterFactory should not have been invoked",
		lrf.getArgLists());
  }

  public void testLinkRegexNoXformsNoMatch() throws Exception {
    NodeList nl = parse(page);
    String[] linkRegex = {
      "http://www.content.org/",
    };
    boolean[] ignoreCase = {
      true,
    };
    String[] rewriteRegex = {
      "http://www.example.com/",
    };
    String[] rewriteTarget = {
      "http://foo.lockss.org/",
    };
    NodeFilter filt = HtmlNodeFilters.linkRegexNoXforms(linkRegex,
							ignoreCase,
							rewriteRegex,
							rewriteTarget,
							attrs);
    assertEquals("Should be empty: " + nl,
		 0, nl.extractAllNodesThatMatch(filt).size());
    // Don't use node.getLink() as it caches its result so doesn't reflect
    // the xform
    assertEquals(finalUrl, ((LinkTag)nl.elementAt(1)).extractLink());
  }

  NodeList parse(String in) throws Exception {
    Parser p = ParserUtils.createParserParsingAnInputString(in);
    NodeList nl = p.parse(null);
    if (log.isDebug3()) log.debug3("parsed (" + nl.size() + "):\n" +
				   HtmlFilterInputStream.nodeString(nl));
    return nl;
  }

  Node divWithAttr(String attr, String val) throws Exception {
    return tagWithAttr(Div.class, attr, val);
  }

  Node tagWithAttr(Class tagClass, String attr, String val) throws Exception {
    Tag tag = (Tag)tagClass.newInstance();
    tag.setAttribute(attr, val);
    return tag;
  }

  /*
   * This test follows the three examples from the Javadoc of AllExceptSubtreeNodeFilter.
   */
  public void testAllExceptSubtreeNodeFilter() throws Exception {
    final String inputStr =
        "<div id=\"a1\">" +
        "  <div id=\"a11\">" +
        "    <div id=\"a111\">...</div>" +
        "    <div id=\"a112\">...</div>" +
        "    <div id=\"a113\">...</div>" +
        "  </div>" +
        "  <div id=\"a12\">" +
        "    <div id=\"a121\">" +
        "      <div id=\"a1211\">...</div>" +
        "      <div id=\"a1212\">...</div>" +
        "      <div id=\"a1213\">...</div>" +
        "    </div>" +
        "    <div id=\"a122\">" +
        "      <div id=\"a1221\">...</div>" +
        "      <div id=\"a1222\">...</div>" +
        "      <div id=\"a1223\">...</div>" +
        "    </div>" +
        "    <div id=\"a123\">" +
        "      <div id=\"a1231\">...</div>" +
        "      <div id=\"a1232\">...</div>" +
        "      <div id=\"a1233\">...</div>" +
        "    </div>" +
        "  </div>" +
        "  <div id=\"a13\">" +
        "    <div id=\"a131\">...</div>" +
        "    <div id=\"a132\">...</div>" +
        "    <div id=\"a133\">...</div>" +
        "  </div>" +
        "</div>";
    
    NodeList nl = parse(inputStr);
    
    NodeList ret;
    NodeFilter nf;
    String[] expected;
    SimpleNodeIterator iter;
    
    ret = new NodeList();
    nf = HtmlNodeFilters.allExceptSubtree(
             HtmlNodeFilters.tagWithAttribute("div", "id", "a12"),
             HtmlNodeFilters.tagWithAttribute("div", "id", "a122"));
    nl.elementAt(0).collectInto(ret, nf);
    ret.keepAllNodesThatMatch(HtmlNodeFilters.tagWithAttribute("div", "id"));
    expected = new String[] {"a121", "a1211", "a1212", "a1213", "a123", "a1231",
        "a1232", "a1233"};
    assertEquals(expected.length, ret.size());
    iter = ret.elements();
    for (int i = 0 ; iter.hasMoreNodes() ; ++i) {
      assertEquals(expected[i], ((Tag)iter.nextNode()).getAttribute("id"));
    }
    
    ret = new NodeList();
    nf = HtmlNodeFilters.allExceptSubtree(
             HtmlNodeFilters.tagWithAttribute("div", "id", "a12"),
             HtmlNodeFilters.tagWithAttribute("div", "id", "a99"));
    nl.elementAt(0).collectInto(ret, nf);
    ret.keepAllNodesThatMatch(HtmlNodeFilters.tagWithAttribute("div", "id"));
    expected = new String[] {"a12", "a121", "a1211", "a1212", "a1213", "a122",
        "a1221", "a1222", "a1223", "a123", "a1231", "a1232", "a1233"};
    assertEquals(expected.length, ret.size());
    iter = ret.elements();
    for (int i = 0 ; iter.hasMoreNodes() ; ++i) {
      assertEquals(expected[i], ((Tag)iter.nextNode()).getAttribute("id"));
    }
    
    ret = new NodeList();
    nf = HtmlNodeFilters.allExceptSubtree(
             HtmlNodeFilters.tagWithAttribute("div", "id", "a99"),
             HtmlNodeFilters.tagWithAttribute("div", "id", "a122"));
    nl.elementAt(0).collectInto(ret, nf);
    ret.keepAllNodesThatMatch(HtmlNodeFilters.tagWithAttribute("div", "id"));
    assertEquals(0, ret.size());
  }

}
