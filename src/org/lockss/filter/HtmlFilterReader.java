/*
 * $Id: HtmlFilterReader.java,v 1.1 2006-07-31 06:47:26 tlipkis Exp $
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

import java.io.*;
import java.util.List;

import org.htmlparser.*;
import org.htmlparser.lexer.*;
import org.htmlparser.filters.*;
import org.htmlparser.util.*;

import org.lockss.config.*;
import org.lockss.util.*;

/**
 * Reader that parses HTML input, applies a user-supplied transformation to
 * the parse tree, then makes the regenerated HTML text available to be
 * read.  <i>Eg</i> to exclude all <code>div</code> nodes with a certain
 * attribute, (<i>ie</i> sections of the html input matching <code>&lt;div
 * someattr="someval" ...&gt; ... &lt;/div&gt;</code>):
 *
 * <pre>   NodeFilter filter = HtmlNodeFilters.divWithAttribute("someattr", "someval");
 *   HtmlTransform xform = HtmlNodeFilterTransform.exclude(filter);
 *   Reader filtered = new HtmlFilterReader(reader, xform);</pre>

 * <p>Uses org.htmlparser.* to parse HTML.  Registers additional tags with
 * PrototypicalNodeFactory to cause them to be treated as a CompositeTag.
 * If you find that the construct you expected to be a subtree is instead
 * spliced into its parent nodelist as a sequence, you probably need to
 * create a new subclass of CompositeTag and register it.  <i>Eg</i>,
 * registering such a class for &lt;font&gt; would cause this:
 *
 * <pre>    Tag (35[0,35],41[0,41]): font
 *    Txt (44[0,44],48[0,48]): here
 *    End (52[0,52],59[0,59]): /font</pre>to instead parse as
 * <pre>    Tag (35[0,35],41[0,41]): font
 *      Txt (44[0,44],48[0,48]): here
 *      End (52[0,52],59[0,59]): /font</pre>
 *
 * In the first case, filtering out (excluding) nodes matching the
 * &lt;font&gt; tags would remove only the tag itself; in the second case
 * the tag, text and end tag would be removed.
 *
 * @see HtmlTransform
 * @see HtmlNodeFilterTransform
 * @see HtmlNodeFilters
 * @see HtmlTags
 */
public class HtmlFilterReader extends Reader {
  private static Logger log = Logger.getLogger("HtmlFilterReader");

  private FeedbackLogger fl = new FeedbackLogger();

  private Reader in;
  private Reader out = null;
  private HtmlTransform xform;

  /**
   * Create an HtmlFilterReader that applies the given transform
   * @param reader reader to filter from
   * @param xform HtmlTransform to apply to parsed NodeList
   */
  public HtmlFilterReader(Reader reader, HtmlTransform xform) {
    if (reader == null || xform == null) {
      throw new IllegalArgumentException("Called with a null argument");
    }
    in = reader;
    this.xform = xform;
  }

  void parse() throws IOException {
    try {
      Parser parser = makeParser();
      NodeList nl = parser.parse(null);
      if (nl.size() <= 0) {
	log.warning("nl.size(): " + nl.size());
	out = new StringReader("");
      }
      if (log.isDebug3()) log.debug3("parsed (" + nl.size() + "):\n" +
				     nodeString(nl));
      nl = xform.transform(nl);
      if (log.isDebug3()) log.debug3("xformed (" + nl.size() + "):\n" +
				     nodeString(nl));
      String h = nl.toHtml();
      out = new StringReader(h);
    } catch (ParserException e) {
      log.warning("read()", e);
      IOException ioe = new IOException();
      ioe.initCause(e);
      throw ioe;
    }
  }

  /** Make a parser, register our extra nodes */
  Parser makeParser() throws UnsupportedEncodingException { 
    Page pg = new Page(new InputStreamSource(new ReaderInputStream(in)));
    Lexer lx = new Lexer(pg);
    Parser parser = new Parser(lx, fl);

    PrototypicalNodeFactory factory = new PrototypicalNodeFactory();
    factory.registerTag(new HtmlTags.Iframe());
    parser.setNodeFactory(factory);
    return parser;
  }

  String nodeString(NodeList nl) {
    return StringUtil.separatedString(nl.toNodeArray(), "\n----------\n");
  }

  public int read(char[] outputBuf, int off, int bufSize) throws IOException {
    if (in == null) {
      throw new IOException("Attempting to read from a closed Reader");
    }
    if (out == null) {
      parse();
    }
    return out.read(outputBuf, off, bufSize);
  }

  public void close() throws IOException {
    in.close();
    in = null;
  }

  class FeedbackLogger implements ParserFeedback{
    public FeedbackLogger() {
    }
    public void warning(String message) {
      log.warning(message);
    }
    public void info(String message) {
      log.info(message);
    }
    public void error(String message, ParserException e) {
      log.error(message, e);
    }
  }
}
