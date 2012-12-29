/*
 * $Id: HtmlNodeFilters.java,v 1.21 2012-12-29 04:04:25 pgust Exp $
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

import java.io.*;

import org.htmlparser.*;
import org.htmlparser.nodes.*;
import org.htmlparser.tags.*;
import org.htmlparser.filters.*;
import org.htmlparser.util.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.rewriter.*;
import org.lockss.servlet.ServletUtil;

/** Factory methods for making various useful combinations of NodeFilters,
 * and additional  {@link NodeFilter}s to supplement those in
 * {@link org.htmlparser.filters}
 */
public class HtmlNodeFilters {
  static Logger log = Logger.getLogger("HtmlNodeFilters");

  /** No instances */
  private HtmlNodeFilters() {
  }

  /** Create a NodeFilter that matches tags. Equivalant to
   * <pre>new TagNameFilter(tag)</pre> */
  public static NodeFilter tag(String tag) {
    return new TagNameFilter(tag);
  }

  /** Create a NodeFilter that matches tags with a specified tagname and
   * attribute value.  Equivalant to
   * <pre>new AndFilter(new TagNameFilter(tag),
   new HasAttributeFilter(attr, val))</pre> */
  public static NodeFilter tagWithAttribute(String tag,
					    String attr, String val) {
    return new AndFilter(new TagNameFilter(tag),
			 new HasAttributeFilter(attr, val));
  }

  /** Create a NodeFilter that matches tags with a specified tagname and
   * that define a given attribute.  Equivalant to
   * <pre>new AndFilter(new TagNameFilter(tag),
   new HasAttributeFilter(attr))</pre>
   * @since 1.32.0
   */
  public static NodeFilter tagWithAttribute(String tag,
                                            String attr) {
    return new AndFilter(new TagNameFilter(tag),
                         new HasAttributeFilter(attr));
  }

  /** Create a NodeFilter that matches div tags with a specified
   * attribute value.  Equivalant to
   * <pre>new AndFilter(new TagNameFilter("div"),
   new HasAttributeFilter(attr, val))</pre> */
  public static NodeFilter divWithAttribute(String attr, String val) {
    return tagWithAttribute("div", attr, val);
  }

  /** Create a NodeFilter that matches tags with a specified tagname and
   * an attribute value matching a regex.
   * @param tag The tagname.
   * @param attr The attribute name.
   * @param valRegexp The pattern to match against the attribute value.
   */
  public static NodeFilter tagWithAttributeRegex(String tag,
						 String attr,
						 String valRegexp) {
    return new AndFilter(new TagNameFilter(tag),
			 new HasAttributeRegexFilter(attr, valRegexp));
  }

  /** Create a NodeFilter that matches tags with a specified tagname and
   * an attribute value matching a regex.
   * @param tag The tagname.
   * @param attr The attribute name.
   * @param valRegexp The pattern to match against the attribute value.
   * @param ignoreCase If true, match is case insensitive
   */
  public static NodeFilter tagWithAttributeRegex(String tag,
						 String attr,
						 String valRegexp,
						 boolean ignoreCase) {
    return new AndFilter(new TagNameFilter(tag),
			 new HasAttributeRegexFilter(attr, valRegexp,
						     ignoreCase));
  }

  /** Create a NodeFilter that matches tags with a specified tagname and
   * nested text containing a substring.  <i>Eg,</i> in <code>
   * &lt;select&gt;&lt;option value="1"&gt;Option text
   * 1&lt;/option&gt;...&lt;/select&gt;</code>, <code>tagWithText("option", "Option text") </code>would match the <code>&lt;option&gt; </code>node.
   * @param tag The tagname.
   * @param text The nested text to match.
   */
  public static NodeFilter tagWithText(String tag, String text) {
    return new AndFilter(new TagNameFilter(tag),
			 new CompositeStringFilter(text));
  }

  /** Create a NodeFilter that matches tags with a specified tagname and
   * nested text containing a substring.
   * @param tag The tagname.
   * @param text The nested text to match.
   * @param ignoreCase If true, match is case insensitive
   */
  public static NodeFilter tagWithText(String tag, String text,
				       boolean ignoreCase) {
    return new AndFilter(new TagNameFilter(tag),
			 new CompositeStringFilter(text, ignoreCase));
  }

  /** Create a NodeFilter that matches composite tags with a specified
   * tagname and nested text matching a regex.  <i>Eg</i>,  <code>
   * @param tag The tagname.
   * @param regex The pattern to match against the nested text.
   */
  public static NodeFilter tagWithTextRegex(String tag, String regex) {
    return new AndFilter(new TagNameFilter(tag),
			 new CompositeRegexFilter(regex));
  }

  /** Create a NodeFilter that matches tags with a specified tagname and
   * nested text matching a regex.  Equivalant to
   * <pre>new AndFilter(new TagNameFilter(tag),
              new RegexFilter(regex))</pre>
   * @param tag The tagname.
   * @param regex The pattern to match against the nested text.
   * @param ignoreCase If true, match is case insensitive
   */
  public static NodeFilter tagWithTextRegex(String tag,
					    String regex,
					    boolean ignoreCase) {
    return new AndFilter(new TagNameFilter(tag),
			 new CompositeRegexFilter(regex, ignoreCase));
  }

  /**
   * Creates a NodeFilter that accepts comment nodes containing the
   * specified string.  The match is case sensitive.
   * @param string The string to look for
   */
  public static NodeFilter commentWithString(String string) {
    return new CommentStringFilter(string);
  }

  /**
   * Creates a NodeFilter that accepts comment nodes containing the
   * specified string.
   * @param string The string to look for
   * @param ignoreCase If true, match is case insensitive
   */
  public static NodeFilter commentWithString(String string,
					     boolean ignoreCase) {
    return new CommentStringFilter(string, ignoreCase);
  }

  /** Create a NodeFilter that matches html comments containing a match for
   * a specified regex.  The match is case sensitive.
   * @param regex The pattern to match.
   */
  public static NodeFilter commentWithRegex(String regex) {
    return new CommentRegexFilter(regex);
  }

  /** Create a NodeFilter that matches html comments containing a match for
   * a specified regex.
   * @param regex The pattern to match.
   * @param ignoreCase If true, match is case insensitive
   */
  public static NodeFilter commentWithRegex(String regex, boolean ignoreCase) {
    return new CommentRegexFilter(regex, ignoreCase);
  }

  /** Create a NodeFilter that matches the lowest level node that matches the
   * specified filter.  This is useful for searching for text within a tag,
   * because the default is to match parent nodes as well.
   */
  public static NodeFilter lowestLevelMatchFilter(NodeFilter filter) {
    return new AndFilter(filter,
			 new NotFilter(new HasChildFilter(filter, true)));
  }

  /** Create a NodeFilter that applies all of an array of LinkRegexYesXforms
   */
  public static NodeFilter linkRegexYesXforms(String[] regex,
					      boolean[] ignoreCase,
					      String[] target,
					      String[] replace,
					      String[] attrs) {
    NodeFilter[] filters = new NodeFilter[regex.length];
    for (int i = 0; i < regex.length; i++) {
      filters[i] = new LinkRegexXform(regex[i], ignoreCase[i],
				      target[i], replace[i], attrs);
    }
    return new OrFilter(filters);
  }

  /** Create a NodeFilter that applies all of an array of LinkRegexNoXforms
   */
  public static NodeFilter linkRegexNoXforms(String[] regex,
					     boolean[] ignoreCase,
					     String[] target,
					     String[] replace,
					     String[] attrs) {
    NodeFilter[] filters = new NodeFilter[regex.length];
    for (int i = 0; i < regex.length; i++) {
      filters[i] = new LinkRegexXform(regex[i], ignoreCase[i],
				      target[i], replace[i], attrs)
	.setNegateFilter(true);
    }
    return new OrFilter(filters);
  }

  /** Create a NodeFilter that applies all of an array of StyleRegexYesXforms
   */
  public static NodeFilter styleRegexYesXforms(String[] regex,
					       boolean[] ignoreCase,
					       String[] target,
					       String[] replace) {
    NodeFilter[] filters = new NodeFilter[regex.length];
    for (int i = 0; i < regex.length; i++) {
      filters[i] = new StyleRegexXform(regex[i], ignoreCase[i],
				       target[i], replace[i]);
    }
    return new OrFilter(filters);
  }

  /** Create a NodeFilter that applies all of an array of StyleRegexNoXforms
   */
  public static NodeFilter styleRegexNoXforms(String[] regex,
					      boolean[] ignoreCase,
					      String[] target,
					      String[] replace) {
    NodeFilter[] filters = new NodeFilter[regex.length];
    for (int i = 0; i < regex.length; i++) {
      filters[i] = new StyleRegexXform(regex[i], ignoreCase[i],
				       target[i], replace[i])
	.setNegateFilter(true);
    }
    return new OrFilter(filters);
  }

  /** Create a NodeFilter that applies all of an array of RefreshRegexYesXforms
   */
  public static NodeFilter refreshRegexYesXforms(String[] regex,
						 boolean[] ignoreCase,
						 String[] target,
						 String[] replace) {
    NodeFilter[] filters = new NodeFilter[regex.length];
    for (int i = 0; i < regex.length; i++) {
      if (log.isDebug3()) {
	log.debug3("Build meta yes" + regex[i] + " targ " + target[i] +
		   " repl " + replace[i]);
      }
      filters[i] = new RefreshRegexXform(regex[i], ignoreCase[i],
					 target[i], replace[i]);
    }
    return new OrFilter(filters);
  }

  /** Create a NodeFilter that applies all of an array of RefreshRegexNoXforms
   */
  public static NodeFilter refreshRegexNoXforms(String[] regex,
						boolean[] ignoreCase,
						String[] target,
						String[] replace) {
    NodeFilter[] filters = new NodeFilter[regex.length];
    for (int i = 0; i < regex.length; i++) {
      if (log.isDebug3()) {
	log.debug3("Build meta no" + regex[i] + " targ " + target[i] +
		   " repl " + replace[i]);
      }
      filters[i] = new RefreshRegexXform(regex[i], ignoreCase[i],
					target[i], replace[i])
	.setNegateFilter(true);
    }
    return new OrFilter(filters);
  }

  static String getCompositeStringText(CompositeTag node) {
    if (node.getEndTag() != null &&
	node.getEndPosition() < node.getEndTag().getStartPosition()) {
      return node.getStringText();
    }
    return "";
  }


  /**
   * This class accepts all comment nodes containing the given string.
   */
  public static class CommentStringFilter implements NodeFilter {
    private String string;
    private boolean ignoreCase = false;

    /**
     * Creates a CommentStringFilter that accepts comment nodes containing
     * the specified string.  The match is case sensitive.
     * @param substring The string to look for
     */
    public CommentStringFilter(String substring) {
      this(substring, false);
    }

    /**
     * Creates a CommentStringFilter that accepts comment nodes containing
     * the specified string.
     * @param substring The string to look for
     * @param ignoreCase If true, match is case insensitive
     */
    public CommentStringFilter(String substring, boolean ignoreCase) {
      this.string = substring;
      this.ignoreCase = ignoreCase;
    }

    public boolean accept(Node node) {
      if (node instanceof Remark) {
	String nodestr = ((Remark)node).getText();
	return -1 != (ignoreCase
		      ? StringUtil.indexOfIgnoreCase(nodestr, string)
		      : nodestr.indexOf(string));
      }
      return false;
    }
  }

  /**
   * This class accepts all composite nodes containing the given string.
   */
  public static class CompositeStringFilter implements NodeFilter {
    private String string;
    private boolean ignoreCase = false;

    /**
     * Creates a CompositeStringFilter that accepts composite nodes
     * containing the specified string.  The match is case sensitive.
     * @param substring The string to look for
     */
    public CompositeStringFilter(String substring) {
      this(substring, false);
    }

    /**
     * Creates a CompositeStringFilter that accepts composite nodes
     * containing the specified string.
     * @param substring The string to look for
     * @param ignoreCase If true, match is case insensitive
     */
    public CompositeStringFilter(String substring, boolean ignoreCase) {
      this.string = substring;
      this.ignoreCase = ignoreCase;
    }

    public boolean accept(Node node) {
      if (node instanceof CompositeTag) {
	String nodestr = getCompositeStringText((CompositeTag)node);
	return -1 != (ignoreCase
		      ? StringUtil.indexOfIgnoreCase(nodestr, string)
		      : nodestr.indexOf(string));
      }
      return false;
    }
  }

  /**
   * Abstract class for regex filters
   */
  public abstract static class BaseRegexFilter implements NodeFilter {
    protected CachedPattern pat;
    protected boolean negateFilter = false;

    /**
     * Creates a BaseRegexFilter that performs a case sensitive match for
     * the specified regex
     * @param regex The pattern to match.
     */
    public BaseRegexFilter(String regex) {
      this(regex, false);
    }

    /**
     * Creates a BaseRegexFilter that performs a match for
     * the specified regex
     * @param regex The pattern to match.
     * @param ignoreCase If true, match is case insensitive
     */
    public BaseRegexFilter(String regex, boolean ignoreCase) {
      this.pat = new CachedPattern(regex);
      if (ignoreCase) {
	pat.setIgnoreCase(true);
      }
//       pat.getPattern();
    }

    public BaseRegexFilter setNegateFilter(boolean val) {
      negateFilter = val;
      return this;
    }

    protected boolean isFilterMatch(String str, CachedPattern pat) {
      boolean isMatch = pat.getMatcher(str).find();
      return negateFilter ? !isMatch : isMatch;
    }

    public abstract boolean accept(Node node);

    /**
     * URL encode the part of url that represents the original URL
     * @param url the string including the rewritten URL
     * @return the content of url with the original url encoded
     */
    private static final String tag = "?url=";
    protected String urlEncode(String url) {
      return urlEncode(url, false);
    }

    protected String cssEncode(String url) {
      return urlEncode(url, true);
    }

    protected String urlEncode(String url, boolean isCss) {
      int startIx = url.indexOf(tag);
      if (startIx < 0) {
	log.warning("urlEncode: no tag (" + tag + ") in " + url);
	return url;
      }
      startIx += tag.length();
      String oldUrl = url.substring(startIx);
      // If it is already encoded,  leave it alone
      String oldUrlLow = oldUrl.toLowerCase();
      if (oldUrlLow.startsWith("http%") ||
	  oldUrlLow.startsWith("ftp%") ||
	  oldUrlLow.startsWith("https%")) {
	log.debug3("not encoding " + url);
	return url;
      }
      int endIx = url.indexOf('"', startIx);
      if (endIx > startIx) {
	// meta tag content attribute
	oldUrl = url.substring(startIx, endIx);
      } else if (isCss && (endIx = url.indexOf(')', startIx)) > startIx) {
	// CSS @import
	oldUrl = url.substring(startIx, endIx);
      } else {
	// Normal tag attribute
	endIx = -1;
      }
      int hashpos = oldUrl.indexOf('#');
      String hashref = null;
      if (hashpos >= 0) {
	hashref = oldUrl.substring(hashpos);
	oldUrl = oldUrl.substring(0, hashpos);
      }
      String newUrl = UrlUtil.encodeUrl(oldUrl);
      if (log.isDebug3()) log.debug3("urlEncode: " + oldUrl + " -> " + newUrl);
      StringBuilder sb = new StringBuilder();
      sb.append(url, 0, startIx);
      sb.append(newUrl);
      if (hashref != null) {
	sb.append(hashref);
      }
      if (endIx >= 0) {
	sb.append(url.substring(endIx, url.length()));
      }
      return sb.toString();
//       return url.substring(0, startIx) + newUrl +
// 	(endIx < 0 ? "" : url.substring(endIx));
    }
  }

  /**
   * This class accepts all comment nodes whose text contains a match for
   * the regex.
   */
  public static class CommentRegexFilter extends BaseRegexFilter {
    /**
     * Creates a CommentRegexFilter that accepts comment nodes whose text
     * contains a match for the regex.  The match is case sensitive.
     * @param regex The pattern to match.
     */
    public CommentRegexFilter(String regex) {
      super(regex);
    }

    /**
     * Creates a CommentRegexFilter that accepts comment nodes containing
     * the specified string.
     * @param regex The pattern to match.
     * @param ignoreCase If true, match is case insensitive
     */
    public CommentRegexFilter(String regex, boolean ignoreCase) {
      super(regex, ignoreCase);
    }

    public boolean accept(Node node) {
      if (node instanceof Remark) {
	String nodestr = ((Remark)node).getText();
	return isFilterMatch(nodestr, pat);
      }
      return false;
    }
  }

  /** A regex filter than performs a regex replacement on matching nodes. */
  public interface RegexXform {
    void setReplace(String replace);
  }

  abstract static class BaseRegexXform
    extends BaseRegexFilter
    implements RegexXform {

    protected CachedPattern targetPat;
    protected String replace;

    public BaseRegexXform(String regex, String target, String replace) {
      this(regex, false, target, replace);
    }

    public BaseRegexXform(String regex, boolean ignoreCase,
			  String target, String replace) {
      super(regex, ignoreCase);
      this.targetPat = new CachedPattern(target);
      this.replace = replace;
    }

    public void setReplace(String replace) {
      this.replace = replace;
    }
  }

  /**
   * This class accepts everything but applies a transform to
   * links that match the regex.
   */
  public static class LinkRegexXform extends BaseRegexXform {
    /**
     * Creates a LinkRegexXform that rejects everything but applies
     * a transform to nodes whose text
     * contains a match for the regex.
     * @param regex The pattern to match.
     * @param ignoreCase If true, match is case insensitive
     * @param target Regex to replace
     * @param replace Text to replace it with
     * @param attrs Attributes to process
     */
    private String[] attrs;
    public LinkRegexXform(String regex, boolean ignoreCase,
			  String target, String replace, String[] attrs) {
      super(regex, ignoreCase, target, replace);
      this.attrs = attrs;
    }

    public boolean accept(Node node) {
      if (node instanceof TagNode &&
	  !(node instanceof MetaTag)) {
	for (int i = 0; i < attrs.length; i++) {
	  Attribute attribute = ((TagNode)node).getAttributeEx(attrs[i]);
	  if (attribute != null) {
	    // Rewrite this attribute
	    String url = attribute.getValue();
 	    if (isFilterMatch(url, pat)) {
	      if (log.isDebug3()) {
		log.debug3("Attribute " + attribute.getName() + " old " + url +
			   " target " + targetPat.getPattern() +
			   " replace " + replace);
	      }
	      String newUrl = targetPat.getMatcher(url).replaceFirst(replace);
	      if (!newUrl.equals(url)) {
		String encoded = urlEncode(newUrl);
		attribute.setValue(encoded);
		((TagNode)node).setAttributeEx(attribute);
		if (log.isDebug3()) log.debug3("new " + encoded);
	      }
	    }
// 	    return false;
	  }
	}
      }
      return false;
    }
  }

  private static final String importTag = "@import";

  /**
   * This class rejects everything but applies a transform to
   * links is Style tags that match the regex.
   */
  public static class StyleRegexXform extends BaseRegexXform {
    /**
     * Creates a StyleRegexXform that rejects everything but applies
     * a transform to style nodes whose child text
     * contains a match for the regex.
     * @param regex The pattern to match.
     * @param ignoreCase If true, match is case insensitive
     * @param target Regex to replace
     * @param replace Text to replace it with
     */
    public StyleRegexXform(String regex, boolean ignoreCase,
			   String target, String replace) {
      super(regex, ignoreCase, target, replace);
    }

    public boolean accept(Node node) {
      if (node instanceof StyleTag) {
	try {
	  NodeIterator it;
	  nodeLoop:
	  for (it = ((StyleTag)node).children(); it.hasMoreNodes(); ) {
	    Node child = it.nextNode();
	    if (child instanceof TextNode) {
	      // Find each instance of @import.*)
	      String text = ((TextNode)child).getText();
	      int startIx = 0;
	      while ((startIx = text.indexOf(importTag, startIx)) >= 0) {
		int endIx = text.indexOf(')', startIx);
		int delta = 0;
		if (endIx < 0) {
		  log.error("Can't find close paren in " + text);
		  continue nodeLoop;
// 		  return false;
		}
		String oldImport = text.substring(startIx, endIx + 1);
		if (isFilterMatch(oldImport, pat)) {
		  String newImport = targetPat.getMatcher(oldImport).replaceFirst(replace);
		  if (!newImport.equals(oldImport)) {
		    String encoded = cssEncode(newImport);
		    delta = encoded.length() - oldImport.length();
		    String newText = text.substring(0, startIx) + 
		      encoded + text.substring(endIx + 1);
		    if (log.isDebug3())
		      log.debug3("Import rewritten " + newText);
		    text = newText;
		    ((TextNode)child).setText(text);
		  }
		}
		startIx = endIx + 1 + delta;
	      }
	    }
	  }
	} catch (ParserException ex) {
	  log.error("Node " + node.toString() + " threw " + ex);
	}
      }
      return false;
    }
  }

  /**
   * Rejects everything and applies a CSS LinkRewriter to the text in
   * style tags
   */
  public static class StyleXformDispatch implements NodeFilter {

    private ArchivalUnit au;
    private String charset;
    private String baseUrl;
    private ServletUtil.LinkTransform xform;

    private LinkRewriterFactory lrf;

    public StyleXformDispatch(ArchivalUnit au,
			      String charset,
			      String baseUrl,
			      ServletUtil.LinkTransform xform) {
      this.au = au;
      if (charset == null) {
	this.charset = Constants.DEFAULT_ENCODING;
      } else {
	this.charset = charset;
      }
      this.baseUrl = baseUrl;
      this.xform = xform;
    }

    public void setBaseUrl(String newBase) {
      baseUrl = newBase;
    }

    static String DEFAULT_STYLE_MIME_TYPE = "text/css";
  
    public boolean accept(Node node) {
      if (node instanceof StyleTag) {
	StyleTag tag = (StyleTag)node;
	if (tag.getAttribute("src") != null) {
	  return false;
	}
	String mime = tag.getAttribute("type");
	if (mime == null) {
	  // shouldn't happen - type attr is required
	  log.warning("<style> tag with no type attribute");
	  mime = DEFAULT_STYLE_MIME_TYPE;
	}
	LinkRewriterFactory lrf = au.getLinkRewriterFactory(mime);
	if (lrf != null) {
	  try {
	    for (NodeIterator it = (tag).children();
		 it.hasMoreNodes(); ) {
	      Node child = it.nextNode();
	      if (child instanceof TextNode) {
		TextNode textChild = (TextNode)child;
		String source = textChild.getText();
		if (!StringUtil.isNullString(source)) {
		  InputStream rewritten = null;
		  try {
		    rewritten =
		      lrf.createLinkRewriter(mime,
					     au,
					     new ReaderInputStream(new StringReader(source),
								   charset),
					     charset,
					     baseUrl,
					     xform);
		    String res =
		      StringUtil.fromReader(new InputStreamReader(rewritten,
								  charset));
		    if (!res.equals(source)) {
		      if (log.isDebug3()) log.debug3("Style rewritten " + res);
		      textChild.setText(res);
		    }
		  } catch (PluginException e) {
		    log.error("Can't create link rewriter, not rewriting", e);
		  } catch (IOException e) {
		    log.error("Can't create link rewriter, not rewriting", e);
		  } finally {
		    IOUtil.safeClose(rewritten);
		  }
		}
	      }
	    }
	  } catch (ParserException ex) {
	    log.error("Node " + node.toString() + " threw " + ex);
	  }
	}
      }
      return false;
    }
  }

  /**
   * Rejects everything and applies a CSS LinkRewriter to the text in
   * script tags
   */
  public static class ScriptXformDispatch implements NodeFilter {

    private ArchivalUnit au;
    private String charset;
    private String baseUrl;
    private ServletUtil.LinkTransform xform;

    private LinkRewriterFactory lrf;

    public ScriptXformDispatch(ArchivalUnit au,
			       String charset,
			       String baseUrl,
			       ServletUtil.LinkTransform xform) {
      this.au = au;
      if (charset == null) {
	this.charset = Constants.DEFAULT_ENCODING;
      } else {
	this.charset = charset;
      }
      this.baseUrl = baseUrl;
      this.xform = xform;
    }

    public void setBaseUrl(String newBase) {
      baseUrl = newBase;
    }

    static String DEFAULT_SCRIPT_MIME_TYPE = "text/javascript";
  
    public boolean accept(Node node) {
      if (node instanceof ScriptTag) {
	ScriptTag tag = (ScriptTag)node;
	if (tag.getAttribute("src") != null) {
	  return false;
	}
	String mime = tag.getAttribute("type");
	if (mime == null) {
	  mime = tag.getAttribute("language");
	  if (mime != null) {
	    if (mime.indexOf("/") < 0) {
	      mime = "text/" + mime;
	    }
	  } else {
	    // shouldn't happen - type attr is required
	    log.warning("<script> tag with no type or language attribute");
	    mime = DEFAULT_SCRIPT_MIME_TYPE;
	  }
	}
	LinkRewriterFactory lrf = au.getLinkRewriterFactory(mime);
	if (lrf != null) {
	  try {
	    for (NodeIterator it = (tag).children();
		 it.hasMoreNodes(); ) {
	      Node child = it.nextNode();
	      if (child instanceof TextNode) {
		TextNode textChild = (TextNode)child;
		String source = textChild.getText();
		if (!StringUtil.isNullString(source)) {
		  InputStream rewritten = null;
		  try {
		    rewritten =
		      lrf.createLinkRewriter(mime,
					     au,
					     new ReaderInputStream(new StringReader(source),
								   charset),
					     charset,
					     baseUrl,
					     xform);
		    String res =
		      StringUtil.fromReader(new InputStreamReader(rewritten,
								  charset));
		    if (!res.equals(source)) {
		      if (log.isDebug3()) log.debug3("Script rewritten " + res);
		      textChild.setText(res);
		    }
		  } catch (PluginException e) {
		    log.error("Can't create link rewriter, not rewriting", e);
		  } catch (IOException e) {
		    log.error("Can't create link rewriter, not rewriting", e);
		  } finally {
		    IOUtil.safeClose(rewritten);
		  }
		}
	      }
	    }
	  } catch (ParserException ex) {
	    log.error("Node " + node.toString() + " threw " + ex);
	  }
	}
      }
      return false;
    }
  }

  /**
   * This class rejects everything but applies a transform to
   * meta refresh tags that match the regex.
   */
  public static class RefreshRegexXform extends BaseRegexXform {
    /**
     * Creates a RefreshRegexXform that rejects everything but applies
     * a transform to nodes whose text
     * contains a match for the regex.
     * @param regex The pattern to match.
     * @param ignoreCase If true, match is case insensitive
     * @param target Regex to replace
     * @param replace Text to replace it with
     */
    public RefreshRegexXform(String regex, boolean ignoreCase,
			     String target, String replace) {
      super(regex, ignoreCase, target, replace);
    }

    public boolean accept(Node node) {
      if (node instanceof MetaTag) {
	String equiv = ((MetaTag)node).getAttribute("http-equiv");
	if ("refresh".equalsIgnoreCase(equiv)) {
	  String contentVal = ((MetaTag)node).getAttribute("content");
	  if (log.isDebug3()) log.debug3("RefreshRegexXform: " + contentVal);
	  if (contentVal != null && isFilterMatch(contentVal, pat)) {
	    // Rewrite the attribute
	    if (log.isDebug3()) {
	      log.debug3("Refresh old " + contentVal +
			 " target " + targetPat.getPattern() +
			 " replace " + replace);
	    }
	    String newVal = targetPat.getMatcher(contentVal).replaceFirst(replace);
	    if (!newVal.equals(contentVal)) {
	      String encoded = urlEncode(newVal);
	      ((MetaTag)node).setAttribute("content", encoded);
	      if (log.isDebug3()) log.debug3("new " + encoded);
	    }
	  }
	}
      }
      return false;
    }
  }

  /**
   * This class accepts all composite nodes whose text contains a match for
   * the regex.
   */
  public static class CompositeRegexFilter extends BaseRegexFilter {
    /**
     * Creates a CompositeRegexFilter that accepts composite nodes whose
     * text contains a match for the regex.  The match is case sensitive.
     * @param regex The pattern to match.
     */
    public CompositeRegexFilter(String regex) {
      super(regex);
    }

    /**
     * Creates a CompositeRegexFilter that accepts composite nodes whose
     * text contains a match for the regex.
     * @param regex The pattern to match.
     * @param ignoreCase If true, match is case insensitive
     */
    public CompositeRegexFilter(String regex, boolean ignoreCase) {
      super(regex, ignoreCase);
    }

    public boolean accept(Node node) {
      if (node instanceof CompositeTag) {
	String nodestr = getCompositeStringText((CompositeTag)node);
	return isFilterMatch(nodestr, pat);
      }
      return false;
    }
  }

  /**
   * A filter that matches tags that have a specified attribute whose value
   * matches a regex
   */
  public static class HasAttributeRegexFilter extends BaseRegexFilter {
    private String attr;

    /**
     * Creates a HasAttributeRegexFilter that accepts nodes whose specified
     * attribute value matches a regex.
     * The match is case insensitive.
     * @param attr The attribute name
     * @param regex The regex to match against the attribute value.
     */
    public HasAttributeRegexFilter(String attr, String regex) {
      this(attr, regex, true);
    }

    /**
     * Creates a HasAttributeRegexFilter that accepts nodes whose specified
     * attribute value matches a regex.
     * @param attr The attribute name
     * @param regex The regex to match against the attribute value.
     * @param ignoreCase if true match is case insensitive
     */
    public HasAttributeRegexFilter(String attr, String regex,
				   boolean ignoreCase) {
      super(regex, ignoreCase);
      this.attr = attr;
    }

    public boolean accept(Node node) {
      if (node instanceof Tag) {
	Tag tag = (Tag)node;
	String attribute = tag.getAttribute(attr);
	if (attribute != null) {
	  return isFilterMatch(attribute, pat);
	}
      }
      return false;
    }
  }
}
