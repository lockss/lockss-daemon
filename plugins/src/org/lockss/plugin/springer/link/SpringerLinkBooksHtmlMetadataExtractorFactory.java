/*
 * $Id: SimpleHtmlMetaTagMetadataExtractor.java 39864 2015-02-18 09:10:24Z thib_gc $
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

package org.lockss.plugin.springer.link;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringEscapeUtils;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.SimpleFileMetadataExtractor;
import org.lockss.plugin.*;
import org.lockss.plugin.springer.SpringerLinkBookMetadataExtractorFactory.SpringerLinkBookMetadataExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SpringerLinkBooksHtmlMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(SpringerLinkBooksHtmlMetadataExtractor.class);
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                               String contentType)
      throws PluginException {
    return new SpringerLinkBookMetadataExtractor();
  }
  
  private static MultiMap tagMap = new MultiValueMap();
  static {
    tagMap.put("author", MetadataField.FIELD_AUTHOR);
    tagMap.put("abstract-about-book-chapter-copyright-year", MetadataField.FIELD_DATE);
    tagMap.put("abstract-about-title", MetadataField.FIELD_PUBLICATION_TITLE);
    tagMap.put("abstract-about-book-chapter-doi", MetadataField.FIELD_DOI);
    tagMap.put("abstract-about-book-print-isbn", MetadataField.FIELD_ISBN);
    tagMap.put("abstract-about-book-online-isbn", MetadataField.FIELD_EISBN);
    tagMap.put("abstract-about-publisher", MetadataField.FIELD_PUBLISHER);
  }
    
  public class SpringerLinkBooksHtmlMetadataExtractor
    extends SimpleFileMetadataExtractor {
  
    private Pattern whiteSpacePat = Pattern.compile("\\s+");
  
    public SpringerLinkBooksHtmlMetadataExtractor() {
    }
    
    
    
    /*
     * Trying to pull metadata from tag of the form:
     * <dd id="abstract-about-title">Contemporary Turkey at a Glance</dd>
     * and 
     * <ul class="editors">
          <li itemprop="editor" itemscope="itemscope" itemtype="http://schema.org/Person">
            <a class="person" href="..." itemprop="name">Kristina Kamp</a>
            ...
          </li>
     */
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
        throws IOException {
      if (cu == null) {
        throw new IllegalArgumentException("extract() called with null CachedUrl");
      }
      ArticleMetadata ret = new ArticleMetadata();
      Document doc = Jsoup.parse(cu.getUnfilteredInputStream(), cu.getEncoding() ,cu.getUrl());
      Elements metadata = doc.select("dd[id]");
      Elements editors = doc.getElementsByClass("person");
      for(Element item:metadata) {
        putValue(ret, item.id(), item.text());
      }
      for(Element editor:editors) {
        putValue(ret, "author", editor.text());
      }
      ret.cook(tagMap);
      // Since we know it and since Metadata requires it, set it manually if necessary
      if (ret.get(MetadataField.FIELD_PUBLISHER) == null) {
        ret.put(MetadataField.FIELD_PUBLISHER, "Springer-Verlag");
      }
      return ret;
    }
    
    protected void putValue(ArticleMetadata ret, String name, String content) {
      // filter raw HTML tags embedded within content -- publishers get sloppy
      content = HtmlUtil.stripHtmlTags(content);
      // remove character entities from content
      content = StringEscapeUtils.unescapeHtml4(content);
      // normalize multiple whitespace characters to a single space character
      Matcher m = whiteSpacePat.matcher(content);
      content = m.replaceAll(" ");
      
      if (log.isDebug3()) log.debug3("Add: " + name + " = " + content);
      ret.putRaw(name, content);
    }
  }
}
