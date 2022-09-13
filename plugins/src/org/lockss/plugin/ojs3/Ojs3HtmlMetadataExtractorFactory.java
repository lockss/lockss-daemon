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

package org.lockss.plugin.ojs3;

import java.io.*;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/*
 * Metadata on an abstract page - there is additional 
 * <meta name="citation_journal_title" content="Economic Development in Higher Education"/>
 * <meta name="citation_author" content="G. Jason Jolley"/>
 * <meta name="citation_title" content="U.S. Economic Development Administration University Centers: Leveraging Federal Dollars Toward Best Practices"/>
 * <meta name="citation_date" content="2016/12/12"/>
 * <meta name="citation_volume" content="1"/>
 * <meta name="citation_volume" content="1"/>
 * <meta name="citation_abstract_html_url" content="https://scholarworks.iu.edu/journals/index.php/jedhe/article/view/19369"/>
 * <meta name="citation_pdf_url" content="https://scholarworks.iu.edu/journals/index.php/jedhe/article/download/19369/28943"/>
 * <meta name="DC.Creator.PersonalName" content="G. Jason Jolley"/>
 * <meta name="DC.Date.created" scheme="ISO8601" content="2016-12-12"/>
 * <meta name="DC.Identifier" content="19369"/>
 * <meta name="DC.Identifier.URI" content="https://scholarworks.iu.edu/journals/index.php/jedhe/article/view/19369"/>
 * <meta name="DC.Source" content="Economic Development in Higher Education"/>
 * <meta name="DC.Source.Volume" content="1"/>
 * <meta name="DC.Source.URI" content="https://scholarworks.iu.edu/journals/index.php/jedhe"/>
 * <meta name="DC.Title" content="U.S. Economic Development Administration University Centers: Leveraging Federal Dollars Toward Best Practices"/>
 */

public class Ojs3HtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(Ojs3HtmlMetadataExtractorFactory.class);
  private static final SimpleHtmlMetaTagMetadataExtractor shtmmde = new SimpleHtmlMetaTagMetadataExtractor();
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new Ojs3HtmlMetadataExtractor();
  }

  public static class Ojs3HtmlMetadataExtractor
      implements FileMetadataExtractor {

    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static  {
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_author",
        new MetadataField(MetadataField.FIELD_AUTHOR,
                          MetadataField.splitAt(";")));
      tagMap.put("citation_keywords",
        new MetadataField(MetadataField.FIELD_KEYWORDS,
            MetadataField.splitAt(";")));
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_doi",  MetadataField.FIELD_DOI);
      tagMap.put("citation_issn",  MetadataField.FIELD_ISSN);
      tagMap.put("citation_language",  MetadataField.FIELD_LANGUAGE);
    }


    /** Call the SimplifiedHtmlMetaTagMetadataExtractor extract() method,
     * iterate over the pdfs in citation_pdf_url, and emit an ArticleMetadata
     * for each pdf url in the metadata.
     * We overwrite because we want to emit more than one ArticleMetadata. */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
       //
      ArticleMetadata am = shtmmde.extract(target, cu);
      am.cook(tagMap);
      if (am != null) {
        emitter.emitMetadata(cu, am);
        // if there are multiple pdfs (from multiple languages) the publisher
        // would like this represented in the metadata count
        processMultiplePdfs(am, cu, emitter);
      }
    }

    public void processMultiplePdfs(ArticleMetadata am, CachedUrl cu, Emitter emitter) {
      List<String> pdfUrls = am.getRawList("citation_pdf_url");
      List<String> altTitles = am.getRawList("dc.title.alternative");
      if (pdfUrls.size() > 1) {
        ArchivalUnit au = cu.getArchivalUnit();
        for (int i = 1; i < pdfUrls.size(); i++) {
          String actualPdfUrl = null;
          CachedUrl pdfCu = au.makeCachedUrl(pdfUrls.get(i));
          if (pdfCu != null && pdfCu.hasContent()) {
            actualPdfUrl = pdfUrls.get(i);
          } else {
            // search for the actual pdf url, which can have appended dirs/args
            for (CachedUrl acu : au.getAuCachedUrlSet().getCuIterable()) {
              String cuUrl = acu.getUrl();
              if (cuUrl.contains(pdfUrls.get(i))) {
                actualPdfUrl = cuUrl;
                break;
              }
            }
          }
          if (actualPdfUrl != null) {
            ArticleMetadata multiAm = new ArticleMetadata();
            //for (Object mf : tagMap.values()) {
            //  multiAm.put(((MetadataField) mf), am.get((MetadataField) mf));
            //}
            multiAm.put(MetadataField.FIELD_ACCESS_URL, actualPdfUrl);
            if (altTitles.size()>=i-1) {
              multiAm.put(MetadataField.FIELD_ARTICLE_TITLE, altTitles.get(i-1));
            }
            //multiAm.replace(MetadataField.FIELD_LANGUAGE, "en");
            emitter.emitMetadata(cu, multiAm);
          }
        }
      }
    }

  }

}
