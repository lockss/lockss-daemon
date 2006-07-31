/*
 * $Id: HtmlNodeFilters.java,v 1.1 2006-07-31 06:47:26 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.filter;

import org.apache.oro.text.regex.*;
import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.filters.StringFilter;
import org.lockss.util.*;

/** Factory methods for making various useful combinations of NodeFilters,
 * and additional  {@link NodeFilter}s to supplement those in
 * {@link org.htmlparser.filters}
 */
public class HtmlNodeFilters {
  static Logger log = Logger.getLogger("HtmlNodeFilters");

  /** No instances */
  private HtmlNodeFilters() {
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
   * @param regex The pattern to match against the attribute value.
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
   * @param regex The pattern to match against the attribute value.
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
   * Abstract class for regex filters
   */
  public abstract static class BaseRegexFilter implements NodeFilter {
    protected Pattern pat;
    protected Perl5Matcher matcher;

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
      int flags = 0;
      if (ignoreCase) {
	flags |= Perl5Compiler.CASE_INSENSITIVE_MASK;
      }
      try {
	pat = RegexpUtil.getCompiler().compile(regex, flags);
	matcher = RegexpUtil.getMatcher();
      } catch (MalformedPatternException e) {
	throw new RuntimeException(e);
      }
    }

    public abstract boolean accept(Node node);
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
	return matcher.contains(nodestr, pat);
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
	  return matcher.contains(attribute, pat);
	}
      }
      return false;
    }
  }
}
