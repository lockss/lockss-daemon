/*

Copyright (c) 2001-2012 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.StyleTag;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FormUrlInput;
import org.lockss.plugin.FormUrlNormalizer;
import org.lockss.plugin.LinkExtractorStatisticsManager;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;

/**
 * An all purpose HTML Link extractor which attempts to collect a superset of
 * links extracted by {@link GoslingHtmlLinkExtractor}. Specifically, this
 * extractor provides an alternate {@link Parser} based implementation of
 * LinkExtractor interface. This dom traversal capability is suitable to extract
 * links from an HTML FORM that consists of radio buttons, checkboxes or select
 * options.
 *
 * @author vibhor
 */
public class HtmlParserLinkExtractor implements LinkExtractor {
  public static final String PREFIX = Configuration.PREFIX + "extractor" +
      ".htmlparser.";

  private static final Logger logger =
      Logger.getLogger("HtmlParserLinkExtractor");

  private boolean isBaseSet = false;
  // the AU

  public HtmlParserLinkExtractor() {

  }

  /**
   * Parse content on InputStream,  call cb.foundUrl() for each URL found
   *
   * @param au       the AU to pass to the extractor
   * @param in       the input stream to read from
   * @param encoding the streams text encoding
   * @param srcUrl   The URL at which the content lives.  Used as the base for
   *                 resolving relative URLs (unless/until base set otherwise by
   *                 content)
   * @param cb       The callback to call when a link is found.
   */
  @Override
  public void extractUrls(ArchivalUnit au, InputStream in, String encoding,
                          String srcUrl, Callback cb) throws IOException {

    if (in == null) {
      throw new IllegalArgumentException("Called with null InputStream");
    }
    else if (srcUrl == null) {
      throw new IllegalArgumentException("Called with null srcUrl");
    }
    else if (cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }
    isBaseSet = false;

    Callback current_cb = cb;
    LinkExtractorStatisticsManager stats = new LinkExtractorStatisticsManager();
    // Make a copy of input stream to be used with a fallback extractor (see
    // comment before Gosling).
    StringWriter w = new StringWriter();
    IOUtils.copy(in, w);
    // Restore the input stream consumed by StringWriter.
    in = new ReaderInputStream(new StringReader(w.toString()), encoding);
    // Make a copy.
    InputStream inCopy = new ReaderInputStream(new StringReader(w.toString())
        , encoding);

    Parser p = new Parser(new Lexer(new Page(in, encoding)));
    try {
      p.visitAllNodesWith(
          new LinkExtractorNodeVisitor(au, srcUrl, current_cb, encoding));
    }
    catch (ParserException e) {
      logger.warning("Unable to parse url: " + srcUrl, e);
    }
    catch (RuntimeException e) {
      e.printStackTrace();
      logger.warning("Encountered a runtime exception, " +
                         "continuing link extraction with Gosling", e);
    }

    // For legacy reasons, we want to ensure link extraction using a more
    // permissive Gosling parser.
    //
    new GoslingHtmlLinkExtractor().extractUrls(au, inCopy, encoding,
                                               srcUrl, current_cb);
  }

  /**
   * A factory of {@link TagLinkExtractor} objects.
   *
   * @param tag {@link Tag} object for which {@link TagLinkExtractor} is
   *            required.
   * @return {@link TagLinkExtractor} object.
   */
  private static TagLinkExtractor getTagLinkExtractor(Tag tag) {
    String tagName = tag.getTagName();
    if ("a".equalsIgnoreCase(tagName)) {
      return new HrefTagLinkExtractor(tag);
    }

    if ("area".equalsIgnoreCase(tagName)) {
      return new HrefTagLinkExtractor(tag);
    }

    if ("link".equalsIgnoreCase(tagName)) {
      return new HrefTagLinkExtractor(tag);
    }

    if ("script".equalsIgnoreCase(tagName)) {
      return new SrcTagLinkExtractor(tag);
    }

    if ("img".equalsIgnoreCase(tagName)) {
      return new SrcTagLinkExtractor(tag);
    }

    if ("embed".equalsIgnoreCase(tagName)) {
      return new SrcTagLinkExtractor(tag);
    }

    if ("frame".equalsIgnoreCase(tagName)) {
      return new SrcTagLinkExtractor(tag);
    }

    if ("applet".equalsIgnoreCase(tagName)) {
      return new CodeTagLinkExtractor(tag);
    }

    if ("object".equalsIgnoreCase(tagName)) {
      return new CodeBaseTagLinkExtractor(tag);
    }

    if ("body".equalsIgnoreCase(tagName)) {
      return new BackgroundTagLinkExtractor(tag);
    }

    if ("table".equalsIgnoreCase(tagName)) {
      return new BackgroundTagLinkExtractor(tag);
    }

    if ("tr".equalsIgnoreCase(tagName)) {
      return new BackgroundTagLinkExtractor(tag);
    }

    if ("td".equalsIgnoreCase(tagName)) {
      return new BackgroundTagLinkExtractor(tag);
    }

    if ("th".equalsIgnoreCase(tagName)) {
      return new BackgroundTagLinkExtractor(tag);
    }

    return null;
  }


  public interface FormInputWrapper {
    public FormUrlInput[] getUrlComponents();
  }

  /**
   * A link extractor interface that can parse a given html tag and extract
   * link(s) from it.
   *
   * @author nvibhor
   */
  private interface TagLinkExtractor {
    /**
     * Extract link(s) from this tag.
     *
     * @param au Current archival unit to which this html document belongs.
     * @param cb A callback to record extracted links.
     */
    public void extractLink(ArchivalUnit au, Callback cb);
  }


  /**
   * Implementation of {@link TagLinkExtractor} interface for html tags that
   * contain an 'href' attribute.
   *
   * @author nvibhor
   */
  private static class HrefTagLinkExtractor implements TagLinkExtractor {
    private Tag m_tag;

    /**
     * Constructor
     *
     * @param tag {@link Tag} object corresponding to this html tag.
     */
    public HrefTagLinkExtractor(Tag tag) {
      m_tag = tag;
    }

    @Override
    public void extractLink(ArchivalUnit au, Callback cb) {
      String target = m_tag.getAttribute("href");
      if (target == null) return;

      target = Translate.decode(target);
      cb.foundLink(target);
    }
  }

  /**
   * Implementation of {@link TagLinkExtractor} interface for html tags that
   * contain a 'src' attribute.
   *
   * @author nvibhor
   */
  private static class SrcTagLinkExtractor implements TagLinkExtractor {
    private Tag m_tag;

    /**
     * Constructor
     *
     * @param tag {@link Tag} object corresponding to this html tag.
     */
    public SrcTagLinkExtractor(Tag tag) {
      m_tag = tag;
    }

    @Override
    public void extractLink(ArchivalUnit au, Callback cb) {
      cb.foundLink(m_tag.getAttribute("src"));
    }
  }

  /**
   * Implementation of {@link TagLinkExtractor} interface for html tags that
   * contain a 'code' attribute.
   *
   * @author nvibhor
   */
  private static class CodeTagLinkExtractor implements TagLinkExtractor {
    private Tag m_tag;

    /**
     * Constructor
     *
     * @param tag {@link Tag} object corresponding to this html tag.
     */
    public CodeTagLinkExtractor(Tag tag) {
      m_tag = tag;
    }

    @Override
    public void extractLink(ArchivalUnit au, Callback cb) {
      cb.foundLink(m_tag.getAttribute("code"));
    }
  }

  /**
   * Implementation of {@link TagLinkExtractor} interface for html tags that
   * contain a 'codebase' attribute.
   *
   * @author nvibhor
   */
  private static class CodeBaseTagLinkExtractor implements TagLinkExtractor {
    private Tag m_tag;

    /**
     * Constructor
     *
     * @param tag {@link Tag} object corresponding to this html tag.
     */
    public CodeBaseTagLinkExtractor(Tag tag) {
      m_tag = tag;
    }

    @Override
    public void extractLink(ArchivalUnit au, Callback cb) {
      cb.foundLink(m_tag.getAttribute("codebase"));
    }
  }

  /**
   * Implementation of {@link TagLinkExtractor} interface for html tags that
   * contain a 'background' attribute.
   *
   * @author nvibhor
   */
  private static class BackgroundTagLinkExtractor implements TagLinkExtractor {
    private Tag m_tag;

    /**
     * Constructor
     *
     * @param tag {@link Tag} object corresponding to this html tag.
     */
    public BackgroundTagLinkExtractor(Tag tag) {
      m_tag = tag;
    }

    @Override
    public void extractLink(ArchivalUnit au, Callback cb) {
      cb.foundLink(m_tag.getAttribute("background"));
    }
  }

  /**
   * A custom NodeVisitor implementation that provides the support for link
   * extraction from the current document. An instance of this class is passed
   * to {@link Parser#visitAllNodesWith} which invokes visitTag & visitEngTag
   * for each tag in the document tree. For each tag, a corresponding {@link
   * TagLinkExtractor} object is used to emit links.
   *
   * @author nvibhor
   */
  private class LinkExtractorNodeVisitor extends NodeVisitor {
    private Callback m_cb;
    private ArchivalUnit m_au;
    private String m_srcUrl;
    private String m_encoding;
    private boolean m_isBaseUrlMalformed;
    private boolean m_inScriptMode;
    private boolean m_inFormMode;
    private Callback m_emit;
    private FormUrlNormalizer m_normalizer;

    /**
     * Constructor
     *
     * @param au       Current archival unit to which this html document
     *                 belongs.
     * @param srcUrl   The url of this html document.
     * @param cb       A callback to record extracted links.
     * @param encoding Encoding needed to read this html document off input
     *                 stream.
     */
    public LinkExtractorNodeVisitor(ArchivalUnit au, String srcUrl,
                                    Callback cb, String encoding) {
      m_cb = cb;
      m_au = au;
      m_srcUrl = srcUrl;
      m_encoding = encoding;
      m_isBaseUrlMalformed = false;
      m_inScriptMode = false;

      // TODO: Refactor this custom callback to its own class for better
      // readability.
      m_emit = new LinkExtractor.Callback() {

        @Override
        public void foundLink(String url) {
          if (url != null) {
            logger.debug3("Found link (before custom callback):" + url);
            try {
              if (m_isBaseUrlMalformed) {
                if (!UrlUtil.isAbsoluteUrl(url)) {
                  return;
                }
              }
              else {
                url = resolveUri(new URL(m_srcUrl), url);
                if (url == null) return;
              }
              logger.debug3("Found link (custom callback) after resolver:" +
                                url);
              logger.debug3("Found link (custom callback) after normalizer:"
                                + url);
              // emit the processed url
              m_cb.foundLink(url);
            }
            catch (MalformedURLException e) {
              //if the link is malformed, we can safely ignore it
            }
          }
        }
      };

    }

    /**
     * Called for each Tag visited that has an end tag.
     *
     * @param tag
     */
    @Override
    public void visitEndTag(Tag tag) {
      // If end script tag visited and we were in script mode, exit script mode.
      if ("script".equalsIgnoreCase(tag.getTagName())
          && tag.getStartPosition() != tag.getEndPosition()) {
        m_inScriptMode = false;
      }

      // If end form tag visited, we must have encountered all the form
      // inputs that will be needed to generate form
      // links.
      // Exit form mode, emit all form links and finish form processing for
      // this form.
      if ("form".equalsIgnoreCase(tag.getTagName())
          && tag.getStartPosition() != tag.getEndPosition()) {
        m_inFormMode = false;
      }
    }

    /**
     * Called for each Tag visited.
     *
     * @param tag
     */
    @Override
    public void visitTag(Tag tag) {
      if (tag instanceof FormTag) {
        if (m_inFormMode) {
          logger.error("Invalid HTML: Form inside a form");
          // Recover from this error by simply ignoring any child forms
          // (popular browser behavior)
          return;
        }
        // Visited a form tag, enter form mode and start form processing logic.
        m_inFormMode = true;
      }

      // We currently skip processing script tags.
      if (m_inScriptMode)
        return;

      // The following code for style tag processing is heavily derived from
      // GoslingHTmlLinkExtractor.
      if ("style".equalsIgnoreCase(tag.getTagName())) {
        StyleTag styleTag = (StyleTag) tag;
        InputStream in = new ReaderInputStream(new StringReader(
            styleTag.getStyleCode()), m_encoding);
        try {
          m_au.getLinkExtractor("text/css").extractUrls(m_au, in,
                                                        m_encoding, m_srcUrl,
                                                        m_cb);
          return;
        }
        catch (IOException e) {
        }
        catch (PluginException e) {
        }
      }

      // Visited a base tag, update the page url. All the relative links that
      // follow base tag will need to be resolved to the new page url.
      if ("base".equalsIgnoreCase(tag.getTagName()) && !isBaseSet) {
        String newBase = tag.getAttribute("href");
        if (newBase != null && !newBase.isEmpty()) {
          m_isBaseUrlMalformed = UrlUtil.isMalformedUrl(newBase);
          if (!m_isBaseUrlMalformed && UrlUtil.isAbsoluteUrl(newBase)) {
            m_srcUrl = newBase;
            isBaseSet = true;
          }
        }
        return;
      }

      // Visited a script tag, enter script mode.
      m_inScriptMode = "script".equalsIgnoreCase(tag.getTagName());

      // For everything else, we fallback to a TagLinkExtractor instance if
      // available for this tag.
      TagLinkExtractor tle = getTagLinkExtractor(tag);
      if (tle != null) {
        tle.extractLink(m_au, m_emit);
      }

    }

    /**
     * Resolves a url relative to given base url and returns an absolute url.
     * Also does some minor trasnformation (such as escaping). Derived from
     * {@link GoslingHtmlLinkExtractor#resolveUri(URL, String)}
     *
     * @param base     The base url
     * @param relative Url that needs to be resolved
     * @return The absolute url.
     * @throws MalformedURLException
     * @see UrlUtil#resolveUri(URL, String)
     */
    protected String resolveUri(URL base, String relative)
        throws MalformedURLException {
      String baseProto = null;
      if (base != null) {
        baseProto = base.getProtocol();
      }
      if ("javascript".equalsIgnoreCase(baseProto) || relative != null
          && StringUtil.startsWithIgnoreCase(relative, "javascript:")) {
        return null;
      }
      if ("mailto".equalsIgnoreCase(baseProto) || relative != null
          && StringUtil.startsWithIgnoreCase(relative, "mailto:")) {
        return null;
      }
      return UrlUtil.resolveUri(base, relative);
    }
  }


  public static class Factory implements LinkExtractorFactory {
    public LinkExtractor createLinkExtractor(String mimeType) {
      return new HtmlParserLinkExtractor();
    }
  }

}
