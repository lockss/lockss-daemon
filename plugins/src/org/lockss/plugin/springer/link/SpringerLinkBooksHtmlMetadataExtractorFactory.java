/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.springer.link;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.text.StringEscapeUtils;
import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.SimpleFileMetadataExtractor;
import org.lockss.plugin.*;
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
    return new SpringerLinkBooksHtmlMetadataExtractor();
  }
  
  private static MultiMap tagMap = new MultiValueMap();
  static {
    tagMap.put("author", MetadataField.FIELD_AUTHOR);
    tagMap.put("abstract-about-book-chapter-copyright-year", MetadataField.FIELD_DATE);
    tagMap.put("abstract-about-title", MetadataField.FIELD_PUBLICATION_TITLE);
    tagMap.put("abstract-about-book-chapter-doi", MetadataField.FIELD_DOI);
    tagMap.put("abstract-about-book-print-isbn", MetadataField.FIELD_ISBN);
    tagMap.put("abstract-about-book-online-isbn", MetadataField.FIELD_EISBN);
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
      String url = ret.get(MetadataField.FIELD_ACCESS_URL);
      ArchivalUnit au = cu.getArchivalUnit();
      if (url == null || url.isEmpty() || !au.makeCachedUrl(url).hasContent()) {
        url = cu.getUrl();
      }
      ret.replace(MetadataField.FIELD_ACCESS_URL,
                 AuUtil.normalizeHttpHttpsFromBaseUrl(au, url));
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
