/*
 * $Id: HtmlFilterInputStream.java,v 1.9 2010-02-04 06:53:00 tlipkis Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
import org.htmlparser.lexer.*;
import org.htmlparser.util.*;

import org.lockss.config.*;
import org.lockss.util.*;

/**
 * InputStream that parses HTML input, applies a user-supplied
 * transformation to the parse tree, then makes the regenerated HTML text
 * available to be read.  <i>Eg</i> to exclude all <code>div</code> nodes
 * with a certain attribute, (<i>ie</i> sections of the html input matching
 * <code>&lt;div someattr="someval" ...&gt; ... &lt;/div&gt;</code>):
 *
 * <pre>   NodeFilter filter = HtmlNodeFilters.divWithAttribute("someattr", "someval");
 *   HtmlTransform xform = HtmlNodeFilterTransform.exclude(filter);
 *   InputStream filtered = new HtmlFilterInputStream(reader, xform);</pre>

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
public class HtmlFilterInputStream extends InputStream {
  private static Logger log = Logger.getLogger("HtmlFilterInputStream");

  /** The readlimit used to mark() the input stream in case of charset
   * changes requiring reset() */
  public static final String PARAM_MARK_SIZE =
    Configuration.PREFIX + "filter.html.mark";
  public static final int DEFAULT_MARK_SIZE = 16 * 1024;

  /** If true, html output will be as close as possible to the input.  If
   * false, missing end tags will be inserted.  */
  public static final String PARAM_VERBATIM =
    Configuration.PREFIX + "filter.html.verbatim";
  public static final boolean DEFAULT_VERBATIM = true;

  private FeedbackLogger fl = new FeedbackLogger();

  private InputStream in;
  private String charset;
  private InputStream out = null;
  private HtmlTransform xform;

  /**
   * Create an HtmlFilterInputStream that applies the given transform
   * @param in InputStream to filter from
   * @param xform HtmlTransform to apply to parsed NodeList
   */
  public HtmlFilterInputStream(InputStream in, HtmlTransform xform) {
    this(in, null, xform);
  }

  public HtmlFilterInputStream(InputStream in, String charset,
			       HtmlTransform xform) {
    if (in == null || xform == null) {
      throw new IllegalArgumentException("Called with a null argument");
    }
    this.in = in;
    this.charset = charset;
    this.xform = xform;
  }

  /** Parse the input, apply the transform, generate output string and
   * InputStream */
  void parse() throws IOException {
    try {
      Configuration config = ConfigManager.getCurrentConfig();
      int mark = config.getInt(PARAM_MARK_SIZE, DEFAULT_MARK_SIZE);
      boolean verbatim = config.getBoolean(PARAM_VERBATIM, DEFAULT_VERBATIM);

      Parser parser = makeParser(mark);
      NodeList nl = parser.parse(null);
      if (nl.size() <= 0) {
	log.warning("nl.size(): " + nl.size());
	out = new ReaderInputStream(new StringReader(""));
      }
      if (log.isDebug3()) log.debug3("parsed (" + nl.size() + "):\n" +
				     nodeString(nl));
      nl = xform.transform(nl);
      if (log.isDebug3()) log.debug3("xformed (" + nl.size() + "):\n" +
				     nodeString(nl));
      String h = nl.toHtml(verbatim);
      out = new ReaderInputStream(new StringReader(h));
    } catch (ParserException e) {
      log.warning("read()", e);
      IOException ioe = new IOException();
      ioe.initCause(e);
      throw ioe;
    }
  }

  /** Make a parser, register our extra nodes */
  Parser makeParser(int mark) throws UnsupportedEncodingException {
    // InputStreamSource may reset() the stream if it encounters a charset
    // change.  It expects the stream already to have been mark()ed.
    if (mark > 0) {
      in.mark(mark);
    }
    Page pg = new Page(new InputStreamSource(in, charset));
    setupHtmlParser();

    Lexer lx = new Lexer(pg);
    Parser parser = new Parser(lx, fl);

    PrototypicalNodeFactory factory = new PrototypicalNodeFactory();
    factory.registerTag(new HtmlTags.Iframe());
    factory.registerTag(new HtmlTags.Noscript());
    factory.registerTag(new HtmlTags.Font());
    factory.registerTag(new HtmlTags.MyTableRow());
    parser.setNodeFactory(factory);
    return parser;
  }

  void setupHtmlParser() {
    // Tell HtmlParser to accept common html comment variants.
    Lexer.STRICT_REMARKS = false;
  }

  public static String nodeString(NodeList nl) {
    return StringUtil.separatedString(nl.toNodeArray(), "\n----------\n");
  }

  InputStream getOut() throws IOException {
    if (in == null) {
      throw new IOException("Attempting to read from a closed InputStream");
    }
    if (out == null) {
      parse();
    }
    return out;
  }

  public int read() throws IOException {
    return getOut().read();
  }
  public int read(byte b[]) throws IOException {
    return read(b, 0, b.length);
  }
  public int read(byte b[], int off, int len) throws IOException {
    return getOut().read(b, off, len);
  }
  public long skip(long n) throws IOException {
    return getOut().skip(n);
  }
  public int available() throws IOException {
    return getOut().available();
  }
  public void mark(int readlimit) {
    try {
      getOut().mark(readlimit);
    } catch (IOException e) {
      throw new RuntimeException("", e);
    }
  }
  public void reset() throws IOException {
    getOut().reset();
  }
  public boolean markSupported() {
    try {
      return getOut().markSupported();
    } catch (IOException e) {
      throw new RuntimeException("", e);
    }
  }

  public void close() throws IOException {
    in.close();
    in = null;
  }

  static class FeedbackLogger implements ParserFeedback{
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
