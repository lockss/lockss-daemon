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

package org.lockss.extractor;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.HtmlUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

public class JsoupTagExtractor extends SimpleFileMetadataExtractor {
  
  private static final Logger theLog = Logger.getLogger(JsoupTagExtractor.class);

  static final String DEFAULT_META_TAG = "meta";
  protected Collection<String> m_selectors;
  String m_mimeType;
  Parser m_parser = Parser.htmlParser();
  boolean m_isHtml = true;

  /**
   * default constructor which will process meta selectors only
   * kept for backwards compatibility
   */
  public JsoupTagExtractor(String mimeType ) {
    m_mimeType = mimeType;

    if ("text/xml".equalsIgnoreCase(mimeType) ||
        "application/xml".equalsIgnoreCase(mimeType) ||
        "application/xhtml+xml".equalsIgnoreCase(mimeType))
    {
      m_parser = Parser.xmlParser();
      m_isHtml = false;
    }
  }

  /**
   * Create an extractor that will extract the value(s) of the xml selectors in
   * <code>selMap.keySet()</code>
   * @param selMap a map from XML selectors to cooked keys.  (Only the set of
   * selectors is used by this object.)
   */
  public JsoupTagExtractor(Map selMap) {
    m_selectors = selMap.keySet();
  }

  /**
   * set the selectors for extraction
   * @param selMap the map with keys to to be used as the selectors for extraction
   */
  public void setSelectors(Map selMap)
  {
    m_selectors = selMap.keySet();
  }

  /**
   * set the selectors for extraction
   * @param selectors the collection of selectors we will extract data from
   */
  public void setSelectors(Collection<String> selectors)
  {
    m_selectors = selectors;
  }

  public String getMimeType()
  {
    return m_mimeType;
  }

  @Override
  public ArticleMetadata extract(final MetadataTarget target,
                                 final CachedUrl cu) throws
      IOException, PluginException {
    // validate input
    if (cu == null) {
      throw new IllegalArgumentException("extract() called with null CachedUrl");
    }
    ArticleMetadata am_ret = new ArticleMetadata();
    if(cu.getContentSize() > 0) {
      InputStream in = cu.getUnfilteredInputStream();
      // we pass in null for charset to determine from http-equiv meta selector
      Document doc = Jsoup.parse(in, null, cu.getUrl(), m_parser);
      if(m_isHtml && (m_selectors == null || m_selectors.isEmpty())) {
        // just use the default "meta" tag (backwards compatible)
        extractMetaTags(doc, am_ret);
      }
      else {
        // extract all selectors in m_selectors
        extractSelectors(doc, am_ret);
      }
    }
    return am_ret;
  }

  /**
   * extract the <meta...></meta> selectors
   * @param doc the parsed jsoup document
   * @param articleMeta the ArticleMetadata to store the name/content pairs
   */
  void extractMetaTags(Document doc, ArticleMetadata articleMeta)
  {
    Elements metas = doc.select(DEFAULT_META_TAG);
    String name;
    String content;
    for (Element meta : metas)
    {
      name = meta.attr("name");
      content = meta.attr("content");
      if(!StringUtil.isNullString(content) && !StringUtil.isNullString(name))
      {
        content = processHtml(name, content);
        if (theLog.isDebug3()) theLog.debug3("Add: "+ name + " = " + content);
        articleMeta.putRaw(name, content);
      }
    }
   }


  /**
   * extract the values for the as defined by the selectors and store them in
   * article. These can be selectors or they can be css/jquery selection strings
   * metadata
   * @param doc the jsoup parsed doc
   * @param articleMeta the ArticleMetadata in which to store the selector/value(s)
   */
  void extractSelectors(Document doc, ArticleMetadata articleMeta)
  {

    // if we don't have any selectors there is nothing to do, so we return
    if(m_selectors == null || m_selectors.isEmpty()) return;

    for(String selector : m_selectors) {
      String val;
      Elements elements = doc.select(selector);
      for(Element element : elements) {
        if(element.hasText()) {
          if(m_isHtml)  {
            val = processHtml(selector, element.text());
          }
          else {
            val = processXml(selector, element.text());
          }
          if (theLog.isDebug3()) theLog.debug3("Add: "+ selector + " = " +val);
          articleMeta.putRaw(selector, val);
        }
      }
    }
  }

  /**
   * take the value for a selector from an html page and perform the necessary
   * transformations to regularize it for storing in the article metadata.
   * this will strip embedded html selectors, unescape any escaped html and remove
   * any extra spaces.
   *
   * @param name the selector name
   * @param value the value
   * @return the regularized value
   */
  private String processHtml(final String name, String value) {
    value = HtmlUtil.stripHtmlTags(value);
    // remove character entities from content
    value = StringEscapeUtils.unescapeHtml4(value);
    // normalize multiple whitespaces to a single space character
    value = value.replaceAll("\\s+", " ");
    return value;
  }

  /**
   * take the value for a selector from an xml page and perform the necessary
   * transformations to regularize it for storing in the article metadata.
   * this will unescape any escaped xml and remove any extra spaces.
   *
   * @param name the selector name
   * @param value the value
   * @return the regularized value
   */
  private String processXml(final String name, String value) {
    // remove character entities from content
    value = StringEscapeUtils.unescapeXml(value);
    // normalize multiple whitespaces to a single space character
    value = value.replaceAll("\\s+", " ");
    return value;
  }

}
