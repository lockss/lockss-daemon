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
  public static final String PARAM_MAX_FORM_URLS =
      PREFIX + "max_form_urls";
  public static final int DEFAULT_MAX_FORM_URLS = 1000000;

  public static final String PARAM_STATISTICS_ENABLED =
      PREFIX + "statistics_enabled";
  public static final boolean DEFAULT_STATISTICS_ENABLED = false;

  public static final String PARAM_NORMALIZE_FORM_URLS =
      PREFIX + "statistics_enabled";
  public static final boolean DEFAULT_NORMALIZE_FORM_URLS = false;

  private static final Logger logger =
      Logger.getLogger("HtmlParserLinkExtractor");

  private int m_maxFormUrls;
  private boolean m_statisticsEnabled;
  private boolean m_normalizeFormUrls;
  private boolean isBaseSet = false;
  // the AU

  // For testing
  public HtmlParserLinkExtractor(int maxFormUrls,
                                 boolean enableStatistics,
                                 boolean normalizeFormUrls) {
    m_maxFormUrls = maxFormUrls;
    m_statisticsEnabled = enableStatistics;
    m_normalizeFormUrls = normalizeFormUrls;
  }

  public HtmlParserLinkExtractor() {
    // set max form urls
    m_maxFormUrls = CurrentConfig.getIntParam(PARAM_MAX_FORM_URLS,
                                              DEFAULT_MAX_FORM_URLS);

    // set statistics on/off - def off
    m_statisticsEnabled = CurrentConfig.getBooleanParam
        (PARAM_STATISTICS_ENABLED,
         DEFAULT_STATISTICS_ENABLED);

    // set the normalization - def off
    m_normalizeFormUrls = CurrentConfig.getBooleanParam
        (PARAM_NORMALIZE_FORM_URLS, DEFAULT_NORMALIZE_FORM_URLS);

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
    if (m_statisticsEnabled) {
      stats.startMeasurement("HtmlParser");
      current_cb = stats.wrapCallback(cb, "HtmlParser");
    }
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
    // TODO(vibhor): Instead of copying the IOStream,
    // we should be able to specify pass multiple
    // link extractors in the plugin (for same mime type) and reopen stream
    // for each.
    if (m_statisticsEnabled) {
      stats.startMeasurement("Gosling");
      current_cb = stats.wrapCallback(cb, "Gosling");
    }

    new GoslingHtmlLinkExtractor().extractUrls(au, inCopy, encoding,
                                               srcUrl, current_cb);
    if (m_statisticsEnabled) {
      stats.stopMeasurement();
      stats.compareExtractors("Gosling", "HtmlParser", "AU: " + au.toString()
          + " src URL=" + srcUrl);
    }
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
   * @author mlanken, vibhor, fred all possible links are emitted from
   *         emitLinks
   */
  public class FormProcessor {

    Vector<FormInputWrapper> m_orderedTags;
    Map<String, RadioFormInput> m_nameToRadioInput;
    private FormTag m_formTag;
    private boolean m_isSubmitSeen;

    public FormProcessor(FormTag formTag) {
      this.m_formTag = formTag;
      m_orderedTags = new Vector<HtmlParserLinkExtractor.FormInputWrapper>();
      m_nameToRadioInput = new HashMap<String, HtmlParserLinkExtractor
          .FormProcessor.RadioFormInput>();
      m_isSubmitSeen = false;
    }

    public void submitSeen() {
      m_isSubmitSeen = true;
    }

    public void addTag(Tag tag) {
      if ("submit".equalsIgnoreCase(tag.getAttribute("type"))) {
        // HACK(vibhor): If the submit button has a value,
        // browser will send its value in a POST form.
        m_orderedTags.add(new HiddenFormInput(tag));
        submitSeen();
        return;
      }
      String name = tag.getAttribute("name");
      if (name == null || name.isEmpty()) {
        logger.warning("Form input tag with no name found. Skipping");
        return;
      }

      if (tag instanceof SelectTag) {
        m_orderedTags.add(new SingleSelectFormInput((SelectTag) tag));
      }
      else if (tag instanceof InputTag) {
        String type = tag.getAttribute("type");
        if (type.equalsIgnoreCase("hidden")) {
          m_orderedTags.add(new HiddenFormInput(tag));
        }
        else if (type.equalsIgnoreCase("checkbox")) {
          m_orderedTags.add(new CheckboxFormInput(tag));
        }
        else if (type.equalsIgnoreCase("radio")) {
          RadioFormInput in = m_nameToRadioInput.get(name);
          if (in != null) {
            in.add(tag);
            return;
          }
          in = new RadioFormInput();
          m_nameToRadioInput.put(name, in);
          in.add(tag);
          m_orderedTags.add(in);
        }
      }
    }

    public void emitLinks(ArchivalUnit au, Callback cb) {
      // Do not extract links if submit button is not seen in the form.
      if (!m_isSubmitSeen)
        return;

      // Get the absolute base url from action attribute.
      String baseUrl = m_formTag.extractFormLocn();

      FormUrlIterator iter = new FormUrlIterator(m_orderedTags, baseUrl);
      iter.initialize();

      // TODO(fkautz): Instead of using a custom normalizer,
      // investigate and use PluginManager to normalize the form
      // urls. This
      // way we can share the logic between crawler and proxyhandler. (We do
      // a similar normalization in ProxyHandler
      // .java)
      // ***NOTE: We only need to use a normalizer if the task to use proxy
      // request header fails.***
      FormUrlNormalizer normalizer = new FormUrlNormalizer(true, null);
      boolean isPost = m_formTag.getFormMethod().equalsIgnoreCase("post");
      while (iter.hasMore()) {
        String link = iter.nextUrl();
        if (isPost) {
          try {
            link = normalizer.normalizeUrl(link, au);
          }
          catch (PluginException e) {
            // TODO Auto-generated catch block   - what should we do here?
            e.printStackTrace();
          }
        }
        cb.foundLink(link);
      }
    }

    private class HiddenFormInput implements FormInputWrapper {
      private Tag m_tag;

      public HiddenFormInput(Tag tag) {
        m_tag = tag;
      }

      @Override
      public FormUrlInput[] getUrlComponents() {
        FormUrlInput[] l = new FormUrlInput[1];
        String name = m_tag.getAttribute("name");
        if (name == null || name.isEmpty()) {
          return null; // should never reach this, return null for defense
        }
        l[0] = new FormUrlInput(name, m_tag.getAttribute("value"));
        return l;
      }
    }

    private class RadioFormInput implements FormInputWrapper {
      // Assumed to be all input type=radio of same name.
      Vector<InputTag> m_inputs;
      String m_name;

      public RadioFormInput() {
        m_inputs = new Vector<InputTag>();
        m_name = null;
      }

      public void add(Tag tag) {
        String tagName = tag.getAttribute("name");
        if (m_name == null) {
          m_name = tagName;
          if (m_name.isEmpty()) {
            // shouldn't ever reach this
            logger.warning("Radio button with no name. Skipping.");
            return;
          }
        }
        if (!tagName.equalsIgnoreCase(m_name)) {
          // should never reach this
          logger.error("Radio button for different group. Skipping.");
          return;
        }

        if (!(tag instanceof InputTag) || !tag.getAttribute("type")
            .equalsIgnoreCase("radio")) {
          // should never reach this
          logger.error("Not a radio button. Skipping.");
          return;
        }

        m_inputs.add((InputTag) tag);
      }

      @Override
      public FormUrlInput[] getUrlComponents() {
        if (m_name == null || m_name.isEmpty()) {
          // should never reach this
          logger.error("Not a radio button. Skipping");
          return null;
        }

        FormUrlInput[] l = new FormUrlInput[m_inputs.size()];
        int i = 0;
        // Like single select, radio allows ONLY one value at a time
        // (unlike multi-select or checkbox).
        for (InputTag in : m_inputs) {
          l[i++] = new FormUrlInput(m_name, in.getAttribute("value"));
        }
        return l;
      }
    }

    private class CheckboxFormInput implements FormInputWrapper {
      // Assumed to be all input type=radio of same name.
      private Tag m_tag;

      public CheckboxFormInput(Tag tag) {
        m_tag = tag;
      }

      @Override
      public FormUrlInput[] getUrlComponents() {
        String name = m_tag.getAttribute("name");
        if (name == null || name.isEmpty())
          return null; // shouldn't ever reach this, defensive
        // Only 2 possible values on/off (value sent or empty)
        FormUrlInput[] l = new FormUrlInput[2];
        String value = m_tag.getAttribute("value");
        if (value == null || value.isEmpty())
          value = "on";
        l[0] = new FormUrlInput(name, value);
        l[1] = new FormUrlInput(name, "");
        return l;
      }
    }

    private class SingleSelectFormInput implements FormInputWrapper {
      private SelectTag m_selectTag;

      public SingleSelectFormInput(SelectTag tag) {
        m_selectTag = tag;
      }

      @Override
      public FormUrlInput[] getUrlComponents() {
        FormUrlInput l[] = new FormUrlInput[m_selectTag.getOptionTags().length];
        String name = m_selectTag.getAttribute("name");
        if (name == null || name.isEmpty()) {
          // shouldn't ever reach this, defensive
          return null;
        }
        OptionTag[] options = m_selectTag.getOptionTags();
        int i = 0;
        for (OptionTag option : options) {
          l[i++] = new FormUrlInput(name, option.getAttribute("value"));
        }
        return l;
      }
    }

  }

  public class FormUrlIterator {
    private Vector<FormInputWrapper> m_tags;
    private Vector<FormUrlInput[]> m_components;
    private int[] m_currentPositions;
    private int m_totalUrls;
    private int m_numUrlSeen;
    private String m_baseUrl;

    public FormUrlIterator(Vector<FormInputWrapper> tags, String baseUrl) {
      m_tags = tags;
      m_components = new Vector<FormUrlInput[]>();
      m_totalUrls = 1;
      m_currentPositions = null;
      m_numUrlSeen = 0;
      m_baseUrl = baseUrl;
    }

    public void initialize() {
      for (FormInputWrapper tag : m_tags) {
        FormUrlInput[] urlComponents = tag.getUrlComponents();
        if (urlComponents != null && urlComponents.length > 0) {
          if (m_maxFormUrls > m_totalUrls * urlComponents.length) {
            m_totalUrls *= urlComponents.length;
          }
          else {
            m_totalUrls = m_maxFormUrls;
          }
          m_components.add(tag.getUrlComponents());
        }
      }
      m_currentPositions = new int[m_components.size()];
      for (int i = 0; i < m_currentPositions.length; ++i) {
        m_currentPositions[i] = 0;
      }

      if (m_totalUrls > m_maxFormUrls)
        m_totalUrls = m_maxFormUrls;
    }

    public boolean hasMore() {
      return m_numUrlSeen < m_totalUrls;
    }

    public String nextUrl() {
      if (!hasMore()) return null;

      boolean isFirstArgSeen = false;
      String url = m_baseUrl;
      int i = 0;
      for (FormUrlInput[] components : m_components) {
        url += (isFirstArgSeen ? '&' : '?') + components[this
            .m_currentPositions[i++]].toString();
        isFirstArgSeen = true;
      }
      incrementPositions_();
      return url;
    }

    private boolean isLastComponent(int i) {
      return (m_currentPositions[i] + 1) >= m_components.get(i).length;
    }

    private void incrementPositions_() {
      if (!hasMore()) return;

      m_numUrlSeen++;

      // If we have 3 select-option values, 1 checkbox and 2 radiobuttons,
      // we can have 3 X 2 X 2 combinations:
      // This is how the iteration works:
      // <0,0,0> <0,0,1> <0,1,0> <0,1, 1>....<2,1,1>
      for (int i = 0; i < m_currentPositions.length; ++i) {
        if (isLastComponent(i)) {
          if (i + 1 == m_currentPositions.length) break;
          m_currentPositions[i] = 0;
        }
        else {
          m_currentPositions[i]++;
          break;
        }
      }
    }

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
   * TagLinkExtractor} object is used to emit links or in case of forms, a
   * {@link FormProcessor} is used.
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
    private FormProcessor m_formProcessor;

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
      m_normalizer = new FormUrlNormalizer();
      m_formProcessor = null;

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
              //previously, a length check was done here
              // sort form parameters if enabled
              if (m_normalizeFormUrls) {
                url = m_normalizer.normalizeUrl(url, m_au);
              }
              logger.debug3("Found link (custom callback) after normalizer:"
                                + url);
              // emit the processed url
              m_cb.foundLink(url);
            }
            catch (MalformedURLException e) {
              //if the link is malformed, we can safely ignore it
            }
            catch (PluginException e) {
              //If a PluginException results,  it can be safely ignored
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
        if (m_formProcessor == null) {
          logger.error("Null FormProcessor while trying to emit links.");
          return;
        }
        m_formProcessor.emitLinks(m_au, m_emit);
        // Cleanup form processor
        m_formProcessor = null;
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
        if (m_formProcessor != null) {
          logger.error("Internal inconsistency for formprocessor_");
          logger.error(Thread.currentThread().getStackTrace().toString());
        }
        // Initialize form processor
        m_formProcessor = new FormProcessor((FormTag) tag);
      }

      // An input/select tag inside a form mode should be handled by form
      // processor.
      if (m_inFormMode
          && (tag instanceof InputTag || tag instanceof SelectTag)) {
        m_formProcessor.addTag(tag);
        return;
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
