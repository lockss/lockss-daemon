/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.heterocycles;

import java.io.*;
import java.util.regex.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.*;

public class HeterocyclesAltMetadataExtractorFactory implements FileMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(HeterocyclesAltMetadataExtractorFactory.class);
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new HeterocyclesAltMetadataExtractor();
  }
  
  public static class HeterocyclesAltMetadataExtractor implements FileMetadataExtractor {
    
    protected static final String CONTENT_BOXES_SELECTOR =
        "body > div#mainContainer > div#mainContent > div.contentBox";
    
    protected static final String DOI_DIV_SELECTOR =
        "div.contentBox > div:nth-child(4)";
    
    protected static final String TITLE_H5_SELECTOR =
        "div.contentBox > h5:nth-child(5)";
    
    protected static final String LINKROW_LINK_SELECTOR =
        "div.contentBox > a.linkrow";
    
    protected static final Pattern ISSUE_PATTERN =
        Pattern.compile("/libraries/journal/([^/]+)/([^/]+)$", Pattern.CASE_INSENSITIVE);

    @Override
    public void extract(MetadataTarget target,
                        CachedUrl cu,
                        Emitter emitter)
        throws IOException, PluginException {
      String tocUrl = cu.getUrl();
      log.debug2("Extracting from: " + tocUrl);
      
      String prefix = tocUrl.substring(0, tocUrl.indexOf("/clockss"));
      
      InputStream tocInputStream = null;
      String tocEncoding = null;
      try {
        tocInputStream = cu.getUnfilteredInputStream();
        tocEncoding = cu.getEncoding();
        Document doc = Jsoup.parse(tocInputStream, tocEncoding, tocUrl);
        Elements contentBoxes = doc.select(CONTENT_BOXES_SELECTOR);
        log.debug2("Content boxes found: " + contentBoxes.size());
        for (int i = 0 ; i < contentBoxes.size() ; ++i) {
          log.debug2("Content box at index: " + i);
          Element contentBox = contentBoxes.get(i);
          ArticleMetadata am = new ArticleMetadata();
          
          Elements linkrowLinks = contentBox.select(LINKROW_LINK_SELECTOR);
          if (linkrowLinks == null || linkrowLinks.size() == 0) {
            log.debug2("No 'linkrow' links found");
            continue;
          }
          // Find a 'PDFwithLinks' link
          for (Element linkrowLink : linkrowLinks) {
            String href = linkrowLink.attr("href").trim();
            if (href.toLowerCase().contains("/pdfwithlinks/")) {
              log.debug2("Found 'PDFwithLinks' link: " + prefix + href);
              am.put(MetadataField.FIELD_ACCESS_URL, prefix + href);
              break;
            }
          }
          // If no 'PDFwithLinks' link found, find a 'PDF' link
          if (am.get(MetadataField.FIELD_ACCESS_URL) == null) {
            for (Element linkrowLink : linkrowLinks) {
              String href = linkrowLink.attr("href").trim();
              if (href.toLowerCase().contains("/pdf/")) {
                log.debug2("Found 'PDF' link: " + prefix + href);
                am.put(MetadataField.FIELD_ACCESS_URL, prefix + href);
                break;
              }
            }
          }
          // If no 'PDFwithLinks' or 'PDF' link found, bail ('PDFsi' links are not suitable)
          if (am.get(MetadataField.FIELD_ACCESS_URL) == null) {
            log.debug2("No 'PDFwithLinks' or 'PDF' link found");
            continue;
          }

          Element doiDiv = contentBox.selectFirst(DOI_DIV_SELECTOR);
          if (doiDiv == null) {
            // Probably never happens
            log.debug2("No DOI found");
            continue;
          }
          log.debug3("Text of doiDiv: " + doiDiv.text());
          String doi = doiDiv.text().replace("doi:", "").replace("DOI:", "").trim();
          if (doi.length() == 0 || !doi.contains("/")) {
            log.debug2("Invalid DOI text: " + doiDiv.text());
            continue;
          }
          log.debug2("DOI found: " + doi);
          am.put(MetadataField.FIELD_DOI, doi);
          
          Element titleH5 = contentBox.selectFirst(TITLE_H5_SELECTOR);
          if (titleH5 == null) {
            // Probably never happens
            log.debug2("No article title found");
            continue;
          }
          String articleTitle = titleH5.text()/*.replace("<span>", "").replace("</span>", "")
                                              .replace("<i>", "").replace("</i>", "")
                                              .replace("<b>", "").replace("</b>", "")*/
                                              .replace("■", "").trim();
          if (articleTitle.length() == 0) {
            log.debug2("Invalid article title text: " + titleH5.text());
            continue;
          }
          log.debug2("Article title found: " + articleTitle);
          am.put(MetadataField.FIELD_ARTICLE_TITLE, articleTitle);
          
          Matcher mat = ISSUE_PATTERN.matcher(cu.getUrl());
          if (mat.find()) {
            String issue = mat.group(2);
            log.debug2("Issue number found: " + issue);
            am.put(MetadataField.FIELD_ISSUE, issue);
          }
          else {
            log.debug2("No issue number found");
          }

          emitter.emitMetadata(cu, am);
          log.debug2("End of content box at index: " + i);
        }
      }
      finally {
        IOUtil.safeClose(tocInputStream);
      }
    }
    
  }

}
