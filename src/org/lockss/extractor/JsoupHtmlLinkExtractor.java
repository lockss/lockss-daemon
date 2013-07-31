/*
 * $Id: JsoupHtmlLinkExtractor.java,v 1.3.2.1 2013-07-31 20:49:06 clairegriffin Exp $
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.LinkExtractorStatisticsManager;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JsoupHtmlLinkExtractor implements LinkExtractor {
  public static final String PREFIX =
    Configuration.PREFIX + "extractor.jsoup.";
  public static final String PARAM_ENABLE_STATS =
    PREFIX + "enable_stats";
  public static final boolean DEFAULT_ENABLE_STATS = false;
  public static final String PARAM_PROCESS_FORMS =
    PREFIX + "process_forms";
  public static final boolean DEFAULT_PROCESS_FORMS = true;
  /**
   * the theLog for this class
   */
  private static final Logger theLog =
    Logger.getLogger(JsoupHtmlLinkExtractor.class);
  /**
   * the class map of tags -> link extractors
   */
  private static HashMap<String, LinkExtractor> theTagTable;

  static {
    theTagTable = new HashMap<String, LinkExtractor>();
    theTagTable.put("a", new SimpleTagLinkExtractor("href"));
    theTagTable.put("applet",
                    new SimpleTagLinkExtractor(new String[]
                                                 {
                                                   "archive",
                                                   "code",
                                                   "codebase"
                                                 }));
    theTagTable.put("area", new SimpleTagLinkExtractor("href"));
    theTagTable.put("audio", new SimpleTagLinkExtractor("src"));
    theTagTable.put("blockquote", new SimpleTagLinkExtractor("cite"));
    theTagTable.put("body", new SimpleTagLinkExtractor("background"));
    theTagTable.put("command", new SimpleTagLinkExtractor("icon"));
    theTagTable.put("del", new SimpleTagLinkExtractor("cite"));
    theTagTable.put("embed", new SimpleTagLinkExtractor("src"));
    theTagTable.put("frame",
                    new SimpleTagLinkExtractor(new String[]
                                                 {"src", "longdesc"}));
    theTagTable.put("head", new SimpleTagLinkExtractor("profile"));
    theTagTable.put("html", new SimpleTagLinkExtractor("manifest"));
    theTagTable.put("iframe",
                    new SimpleTagLinkExtractor(new String[]
                                                 {"src", "longdesc"}));
    theTagTable.put("img",
                    new SimpleTagLinkExtractor(new String[]
                                                 {"src", "longdesc"}));
    theTagTable.put("ins", new SimpleTagLinkExtractor("cite"));
    theTagTable.put("link", new SimpleTagLinkExtractor("href"));
    theTagTable.put("meta", new MetaTagLinkExtractor());
    theTagTable.put("object", new SimpleTagLinkExtractor(new String[]
                                                           {"archive", "data",
                                                            "codebase"}));
    theTagTable.put("q", new SimpleTagLinkExtractor("cite"));
    theTagTable.put("table", new SimpleTagLinkExtractor("background"));
    theTagTable.put("tr", new SimpleTagLinkExtractor("background"));
    theTagTable.put("th", new SimpleTagLinkExtractor("background"));
    theTagTable.put("td", new SimpleTagLinkExtractor("background"));
    theTagTable.put("video",
                    new SimpleTagLinkExtractor(new String[]
                                                 {"poster", "src"}));
    theTagTable.put("param",
                    new SimpleTagLinkExtractor(new String[]
                                                 {"url", "src", "filename"}));


  }

  /**
   * true if we should log link extraction statistics
   */
  private boolean m_enableStats;
  /**
   * true if we should use a form processor to process forms
   */
  private boolean m_processForms;
  /**
   * the statistics manager for logging links extracted
   */
  private LinkExtractorStatisticsManager m_statsMgr;
  /**
   * the instance map of tags -> link extractors
   */
  private HashMap<String, LinkExtractor> m_tagTable =
    new HashMap<String, LinkExtractor>();

  /**
   * The table of FormFieldRestrictions by field name
   * key = field name,
   * value = restrictions
   */
  private Map<String,
              HtmlFormExtractor.FormFieldRestrictions> m_formRestrictors;


  /**
   * The table of FormFieldGenerators key = field name, value generator
   */

  private Map<String,
              HtmlFormExtractor.FieldIterator> m_formGenerators;

  /**
   * constructor used for the jsoup link extractor.
   */
  public JsoupHtmlLinkExtractor() {
    // set statistics on/off - def off
    m_enableStats =
      CurrentConfig.getBooleanParam(PARAM_ENABLE_STATS,
                                    DEFAULT_ENABLE_STATS);
    if(m_enableStats) {
      m_statsMgr = new LinkExtractorStatisticsManager();

    }
    m_processForms =
      CurrentConfig.getBooleanParam(PARAM_PROCESS_FORMS,
                                    DEFAULT_PROCESS_FORMS);
    m_tagTable.putAll(theTagTable);
  }

  /**
   * constructor used by test code
   *
   * @param enableStats true to turn on statistics logging
   */
  public JsoupHtmlLinkExtractor(boolean enableStats, boolean processForms,
      Map<String,HtmlFormExtractor.FormFieldRestrictions> restrictors,
      Map<String,HtmlFormExtractor.FieldIterator> generators) {
    m_enableStats = enableStats;
    m_processForms = processForms;
    m_formRestrictors = restrictors;
    m_formGenerators = generators;
    m_tagTable.putAll(theTagTable);
  }

  public void setFormRestrictors(final Map<String,
      HtmlFormExtractor.FormFieldRestrictions> formRestrictors) {
    m_formRestrictors = formRestrictors;
  }

  public void setFormGenerators(final Map<String,
      HtmlFormExtractor.FieldIterator> formGenerators) {
    m_formGenerators = formGenerators;
  }

  public Map<String, HtmlFormExtractor.FormFieldRestrictions>
      getFormRestrictors() {
    return m_formRestrictors;
  }

  public Map<String, HtmlFormExtractor.FieldIterator> getFormGenerators() {
    return m_formGenerators;
  }


  /**
   * Parse content on InputStream,  call cb.foundUrl() for each URL found
   *
   * @param au       the archival unit
   * @param in       the input stream
   * @param encoding the character encoding for the input stream url
   * @param srcUrl   The URL at which the content lives.  Used as the base for
   *                 resolving relative URLs (unless/until base set otherwise by content)
   * @param cb       the callback used to forward all found urls
   */
  @Override
  public void extractUrls(final ArchivalUnit au, final InputStream in,
                          final String encoding, final String srcUrl,
                          final Callback cb)
    throws IOException, PluginException {

    // validate input
    if(in == null) {
      throw new IllegalArgumentException("Called with null InputStream");
    }
    else if(srcUrl == null) {
      throw new IllegalArgumentException("Called with null srcUrl");
    }
    else if(cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }
    // our base url is the same as source when we start
    // the wrapper is only needed for the stats manager
    org.lockss.extractor.LinkExtractor.Callback wrapped_cb = cb;
    if(m_enableStats) {
      m_statsMgr.startMeasurement("JsoupHtmlLinkExtractor");
      wrapped_cb = m_statsMgr.wrapCallback(cb, "JsoupHtmlLinkExtractor");
    }
    // Parse our file
    Document doc = Jsoup.parse(in, encoding, srcUrl);

    HtmlFormExtractor formExtractor = null;
    if(m_processForms) {
      // create a form processor
      formExtractor = new HtmlFormExtractor(au, cb, encoding,
          m_formRestrictors, m_formGenerators);
      // To allow for HTML 5 forms which allow free form getFieldIterator we grab all
      // forms with ids. When we encounter an input elements with the 'form'
      // attribute we'll add it to the correct form.
      Elements forms_with_ids = doc.select("form[id]");
      formExtractor.initProcessor(this, forms_with_ids);
    }
    // Now we walk the document as usual and process found urls
    doc.traverse(new LinkExtractorNodeVisitor(au, srcUrl, wrapped_cb,
                                              encoding));
    if(formExtractor != null) {
      // Now we're ready to process any unprocessed forms
      formExtractor.processForms();
    }
    if(m_enableStats) {
      m_statsMgr.stopMeasurement();
    }
  }

  /**
   * Node Visitor for JSoup parser
   */
  protected class LinkExtractorNodeVisitor implements NodeVisitor,
                                                      Callback {
    private Callback m_cb;
    private ArchivalUnit m_au;
    private URL m_baseUrl;
    private String m_encoding;
    private boolean m_inScript;

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
      try {
        m_baseUrl = new URL(srcUrl);
      }
      catch(MalformedURLException e) {
        theLog.warning("Malformed Base URL: all doc urls must be absolute.");
      }
      m_encoding = encoding;
      m_inScript = false;
      m_tagTable.put("script", new ScriptTagLinkExtractor(this));
      m_tagTable.put("base", new BaseTagLinkExtractor(this));
      m_tagTable.put("style", new StyleTagLinkExtractor(m_encoding));
    }

    /**
     * Callback used by the jsoup parser for when a node is first visited.
     *
     * @param node  the node being visited.
     * @param depth the depth of the node, relative to the root node. E.g., the
     *              root node has depth 0, and a child node of that will have depth 1.
     */
    @Override
    public void head(final Node node, final int depth) {
      LinkExtractor tle = m_tagTable.get(node.nodeName());
      if(tle != null && !m_inScript) {
        tle.tagBegin(node, m_au, this);
      }
    }

    /**
     * Callback used by jsoup for when a node is last visited,
     * after all of its descendants have been visited.
     *
     * @param node  the node being visited.
     * @param depth the depth of the node, relative to the root node. E.g., the
     *              root node has depth 0, and a child node of that will have depth 1.
     */
    @Override
    public void tail(final Node node, final int depth) {
      String name = node.nodeName();
      LinkExtractor tle = m_tagTable.get(name);
      if(tle != null && ("script".equalsIgnoreCase(name) || !m_inScript)) {
        tle.tagEnd(node, m_au, this);
      }
    }

    /**
     * Notify the callback when a link has been found by a link extractor.
     *
     * @param url the url which has been found.
     */
    @Override
    public void foundLink(String url) {
      if(!StringUtil.isNullString(url)) {
        theLog.debug3("FoundLink (before resolver):" + url);
        try {
          url = resolveUri(m_baseUrl, url);
          if(url == null) {
            return;
          }
          // emit the processed url
          m_cb.foundLink(url);
        }
        catch(MalformedURLException e) {
          //if the link is malformed, we can safely ignore it
        }
      }
    }

    /**
     * Resolves a url relative to given base url and returns an absolute url.
     * Also does some minor transformation (such as escaping). Derived from
     * {@link GoslingHtmlLinkExtractor#resolveUri(URL, String)}
     *
     * @param base     The base url
     * @param relative Url that needs to be resolved
     *
     * @return The absolute url.
     *
     * @throws MalformedURLException
     * @see UrlUtil#resolveUri(URL, String)
     */
    protected String resolveUri(URL base, String relative)
      throws MalformedURLException {
      String baseProto = null;
      if(base != null) {
        baseProto = base.getProtocol();
      }
      if("javascript".equalsIgnoreCase(baseProto) || relative != null
        && StringUtil.startsWithIgnoreCase(relative, "javascript:")) {
        return null;
      }
      if("mailto".equalsIgnoreCase(baseProto) || relative != null
        && StringUtil.startsWithIgnoreCase(relative, "mailto:")) {
        return null;
      }
      return UrlUtil.resolveUri(base, relative);
    }

    public void setInScript(final boolean inScript) {
      m_inScript = inScript;
    }

    public URL getBaseUrl() {
      return m_baseUrl;
    }

    public void setBaseUrl(final URL baseUrl) {
      m_baseUrl = baseUrl;
    }
  }

  /**
   * register a tag link extractor to handle processing a html tag.  This
   * will replace the existing link extractor.
   *
   * @param tagName   the name of the html tag
   * @param extractor the LinkExtractor to call when entering and exiting
   *                  a tag
   */
  public void registerTagExtractor(String tagName,
                                   LinkExtractor extractor) {
    m_tagTable.put(tagName, extractor);
  }

  /**
   * unregister a tag link extractor to handle processing a html tag.  This
   * will replace the existing link extractor.
   *
   * @param tagName the name of the html tag
   *
   * @return the LinkExtractor to called when entering and exiting
   *         a tag
   */
  public LinkExtractor unregisterTagExtractor(String tagName) {
    return m_tagTable.remove(tagName);
  }


  /**
   * The interface which wraps the jsoup callbacks to tagBegin
   * and tagEnd.
   */
  public interface LinkExtractor {
    /**
     * Extract link(s) from this tag.
     *
     * @param node the node containing the tag info
     * @param au   Current archival unit to which this html document belongs.
     * @param cb   A callback to record extracted links.
     */
    public void tagBegin(Node node, ArchivalUnit au, Callback cb);

    /**
     * Perform any extractions based on end tag processing.
     *
     * @param node the node containing the tag info
     * @param au   Current archival unit to which this html document belongs.
     * @param cb   A callback to record extracted links.
     */
    public void tagEnd(Node node, ArchivalUnit au, Callback cb);

  }

  /**
   * Base Class for all Tag Link extractors which implements tagBegin
   * and tagEnd which simple logs to trace node entry and exit.
   * Subclasses must override tagBegin &/or tagEnd to obtain correct
   * tag behaviour.
   */
  public static class BaseLinkExtractor implements LinkExtractor {

    /**
     * Extract link(s) from this tag.
     *
     * @param node the node containing the link
     * @param au   Current archival unit to which this html document belongs.
     * @param cb   A callback to record extracted links.
     */
    @Override
    public void tagBegin(final Node node, final ArchivalUnit au, final
    Callback cb) {
      theLog.debug3("begin tag: " + node.nodeName());
    }

    /**
     * Perform any extractions based on end tag processing.
     *
     * @param node the node containing the link
     * @param au   Current archival unit to which this html document belongs.
     * @param cb   A callback to record extracted links.
     */
    @Override
    public void tagEnd(final Node node, final ArchivalUnit au,
                       final Callback cb) {
      theLog.debug3("end tag: " + node.nodeName());

    }
  }

  /**
   * Link Extractor for the html "base" tag
   */
  public static class BaseTagLinkExtractor extends BaseLinkExtractor {
    private boolean m_baseSet = false;
    private LinkExtractorNodeVisitor m_nodeVisitor;

    BaseTagLinkExtractor(LinkExtractorNodeVisitor nodeVisitor)
    {
      m_nodeVisitor = nodeVisitor;
    }

    @Override
    public void tagBegin(final Node node,
                         final ArchivalUnit au,
                         final Callback cb) {
      super.tagBegin(node, au, cb);
      if(!m_baseSet && node.hasAttr("href")) {
        String href = node.attr("href");
        try {
          String newBase =
            m_nodeVisitor.resolveUri(m_nodeVisitor.getBaseUrl(), href);
          m_nodeVisitor.setBaseUrl(new URL(newBase));
          m_baseSet = true;
        }
        catch(MalformedURLException e) {
          // ignore it, we don't change the base
        }
      }
    }
  }

  /**
   * Link Extractor for meta tag http-equiv url
   */
  public static class MetaTagLinkExtractor extends BaseLinkExtractor {
    static private String CONTENT_ATTR = "content";
    static private String URL_PREFIX = "url=";
    static private String EQUIV_ATTR = "http-equiv";
    static private String EQUIV_REFRESH = "refresh";

    /**
     * Extract link(s) from this tag.
     *
     * @param node the node containing the link
     * @param au   Current archival unit to which this html document belongs.
     * @param cb   A callback to record extracted links.
     */
    @Override
    public void tagBegin(final Node node, final ArchivalUnit au,
                         final Callback cb) {
      super.tagBegin(node, au, cb);
      if(node.hasAttr(EQUIV_ATTR) &&
        node.attr(EQUIV_ATTR).equalsIgnoreCase(EQUIV_REFRESH)) {
        if(node.hasAttr(CONTENT_ATTR)) {
          String value = node.attr(CONTENT_ATTR);
          int pos = value.indexOf(URL_PREFIX) + URL_PREFIX.length();
          if(pos > 0) {
            cb.foundLink(value.substring(pos));
          }
        }
      }
    }
  }

  /**
   * A link extractor interface that can parse a given html tag and extract
   * link(s) from one or more attrs within it.
   */
  public static class SimpleTagLinkExtractor extends BaseLinkExtractor {

    ArrayList<String> m_Attrs = new ArrayList<String>();

    public SimpleTagLinkExtractor(String[] attr) {
      if(attr != null) {
        java.util.Collections.addAll(m_Attrs, attr);
      }
    }

    public SimpleTagLinkExtractor(String attr) {
      if(attr != null) {
        m_Attrs.add(attr);
      }
    }

    /**
     * Extract link(s) from this tag.
     *
     * @param node the node containing the link
     * @param au   Current archival unit to which this html document belongs.
     * @param cb   A callback to record extracted links.
     */
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      super.tagBegin(node, au, cb);
      for(String attr : m_Attrs) {
        if(node.hasAttr(attr)) {
          cb.foundLink(node.attr(attr));
        }
      }

    }
  }

  /**
   * Link Extractor for the script tag
   */
  public static class ScriptTagLinkExtractor extends BaseLinkExtractor {
    private LinkExtractorNodeVisitor m_nodeVisitor;

    ScriptTagLinkExtractor(LinkExtractorNodeVisitor nodeVisitor)
    {
      m_nodeVisitor = nodeVisitor;
    }

    @Override
    public void tagBegin(final Node node,
                         final ArchivalUnit au,
                         final Callback cb) {
      super.tagBegin(node, au, cb);
      m_nodeVisitor.setInScript(true);
      if(node.hasAttr("src")) {
        cb.foundLink(node.attr("src"));
      }

    }

    @Override
    public void tagEnd(final Node node,
                       final ArchivalUnit au,
                       final Callback cb) {
      super.tagEnd(node, au, cb);
      m_nodeVisitor.setInScript(false);
    }
  }

  /**
   * Link Extractor for the css tag
   */
  public static class StyleTagLinkExtractor extends BaseLinkExtractor {

    String m_encoding;

    public StyleTagLinkExtractor(String encoding) {
      m_encoding = encoding;
    }

    /**
     * Extract link(s) from this tag.
     *
     * @param node the node containing the link
     * @param au   Current archival unit to which this html document belongs.
     * @param cb   A callback to record extracted links.
     */
    @Override
    public void tagBegin(final Node node, final ArchivalUnit au,
                         final Callback cb) {
      super.tagBegin(node, au, cb);
      InputStream in =
        new ReaderInputStream(new StringReader(node.outerHtml()),
                              m_encoding);
      try {
        au.getLinkExtractor("text/css").extractUrls(au,
                                                    in,
                                                    m_encoding,
                                                    node.baseUri(),
                                                    cb);
      }
      catch(IOException e) {
        theLog.debug3("css:tagbegin: io exception should not happen");
      }
      catch(PluginException e) {
        theLog.debug3("css:tagbegin: plugin exception");
      }
    }
  }


  public static class Factory implements LinkExtractorFactory {
    public org.lockss.extractor.LinkExtractor createLinkExtractor(String mimeType) {
      return new JsoupHtmlLinkExtractor();
    }
  }
}
