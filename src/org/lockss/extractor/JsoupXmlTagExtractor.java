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
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * XML Tag Extractor which uses jsoup parser
 */
public class JsoupXmlTagExtractor extends SimpleFileMetadataExtractor {
  
  private static final Logger theLog = Logger.getLogger(JsoupXmlTagExtractor.class);
  
  protected Collection<String> m_tags;
  Parser m_parser = Parser.xmlParser();

  JsoupXmlTagExtractor()
  {

  }

  /**
   * Create an extractor what will extract the value(s) of the xml tags in
   * <code>tags</code>
   * @param tags the list of XML tags whose value to extract
   */
  public JsoupXmlTagExtractor(Collection<String> tags) {
    m_tags = tags;
  }

  /**
   * Create an extractor that will extract the value(s) of the xml tags in
   * <code>tagMap.keySet()</code>
   * @param tagMap a map from XML tags to cooked keys.  (Only the set of
   * tags is used by this object.)
   */
  public JsoupXmlTagExtractor(Map tagMap) {
    m_tags = tagMap.keySet();
  }

  /**
   * set the tags for extraction
   * @param tagMap the map with keys to to be used as the tags for extraction
   */
  public void setTags(Map tagMap)
  {
    m_tags = tagMap.keySet();
  }

  /**
   * set the tags for extraction
   * @param tags the collection of tags we will extract data from
   */
  public void setTags(Collection<String> tags)
  {
    m_tags = tags;
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
    if(cu.hasContent()) {
      InputStream in = cu.getUnfilteredInputStream();
      Document doc = Jsoup.parse(in, null, cu.getUrl(), m_parser);
      extractTags(doc, am_ret);
    }
    return am_ret;
  }

  /**
   * extract the values for the desired tags and store them in article metadata
   * @param doc the jsoup parsed doc
   * @param articleMeta the ArticleMetadata in which to store the tag/value(s)
   */
  void extractTags(Document doc, ArticleMetadata articleMeta)
  {
    // if we don't have any tags, there is nothing to do so we return
    if(m_tags == null || m_tags.isEmpty()) return;
    for(String tag : m_tags) {
      String value;
      Elements tag_elements = doc.select(tag);
      for(Element tag_el : tag_elements) {
        if(tag_el.hasText()) {
          value = processXml(tag, tag_el.text());
          articleMeta.putRaw(tag, value);
        }
      }
    }
  }

  /**
   * take the value for a tag from an xml page and perform the necessary
   * transformations to regularize it for storing in the article metadata.
   * this will unescape any escaped xml and remove any extra spaces.
   *
   * @param name the tag name
   * @param value the value
   * @return the regularized value
   */
  private String processXml(final String name, String value) {
    // remove character entities from content
    value = StringEscapeUtils.unescapeXml(value);
    // normalize multiple whitespaces to a single space character
    value = value.replaceAll("\\s+", " ");
    if (theLog.isDebug3()) theLog.debug3("Add: " + name + " = " + value);
    return value;
  }

}
