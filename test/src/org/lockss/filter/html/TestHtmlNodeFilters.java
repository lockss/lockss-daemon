/*
 * $Id: TestHtmlNodeFilters.java,v 1.6 2009-08-27 01:59:44 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.util.*;
import org.lockss.test.*;
import org.htmlparser.*;
import org.htmlparser.tags.*;
import org.htmlparser.util.*;

public class TestHtmlNodeFilters extends LockssTestCase {
  static Logger log = Logger.getLogger("TestHtmlNodeFilters");

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
    assertEquals(origUrl, ((LinkTag)node).getLink());
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
    // XXX this is wierd - the NodeList has been rewritten
    // XXX but node.getLink() returns the old value.
    log.debug3("testLinkRegexYesXformsMatch after " + nl.toHtml());
    log.debug3("Node is " + nl.elementAt(1).toHtml());
    log.debug3("Link is " + ((LinkTag)nl.elementAt(1)).getLink());
    NodeList nl2 = parse(nl.toHtml());
    Node node = nl2.elementAt(1);
    log.debug3("Node is " + node.toHtml());
    log.debug3("Link is " + ((LinkTag)node).getLink());
    assertNotNull(node);
    assertTrue(""+node.getClass(),
	       node instanceof org.htmlparser.tags.LinkTag);
    assertEquals(finalUrl, ((LinkTag)node).getLink());
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
    assertEquals(origUrl, ((LinkTag)node).getLink());
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
    // XXX this is wierd - the NodeList has been rewritten
    // XXX but node.getLink() returns the old value.
    log.debug3("testLinkRegexYesXformsMatch after " + nl.toHtml());
    log.debug3("Node is " + nl.elementAt(1).toHtml());
    log.debug3("Link is " + ((LinkTag)nl.elementAt(1)).getLink());
    NodeList nl2 = parse(nl.toHtml());
    Node node = nl2.elementAt(1);
    log.debug3("Node is " + node.toHtml());
    log.debug3("Link is " + ((LinkTag)node).getLink());
    assertNotNull(node);
    assertTrue(""+node.getClass(),
	       node instanceof org.htmlparser.tags.LinkTag);
    assertEquals(finalUrl, ((LinkTag)node).getLink());
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

}
