/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

/*
 * No metadata found from the publisher's website.
 * BaseArticleMetadataExtractor provides some default metadata from tdb file.
 * Issue number can be extracted from the pdf url:
 *      <heterocyclesbase>.com/clockss/downloads/PDF/23208/83/1
 * The cached url is pdf.
 * 
 * A sample metadata from daemon:
 * ArticleFiles
 *  Full text CU:  http://www.heterocycles.jp/clockss/downloads/PDF/22574/87/4
 *  Abstract:  http://www.heterocycles.jp/clockss/libraries/fulltext/22574/87/4
 *  ArticleMetadata:  http://www.heterocycles.jp/clockss/libraries/fulltext/22574/87/4
 *  FullTextHtml:  http://www.heterocycles.jp/clockss/libraries/fulltext/22574/87/4
 *  FullTextPdfFile:  http://www.heterocycles.jp/clockss/downloads/PDF/22574/87/4
 *  PdfWithLinks:  http://www.heterocycles.jp/clockss/downloads/PDFwithLinks/22574/87/4
 * Metadata
 *  access.url: http://www.heterocycles.jp/clockss/downloads/PDF/22574/87/4
 *  date: 2013
 *  eissn: 1881-0942
 *  issn: 0385-5414
 *  issue: 4
 *  journal.title: An International Journal for Reviews and Communications in Heterocyclic Chemistry
 *  publisher: The Japan Institute of Heterocyclic Chemistry
 *  volume: 87    
 * Raw Metadata (empty)
 */
public class HeterocyclesMetadataExtractorFactory 
  implements FileMetadataExtractorFactory {
  
  static Logger log = Logger.getLogger(HeterocyclesMetadataExtractorFactory.class);
  
  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new HeterocyclesMetadataExtractor();
  }

  // Gets default metadata from tdb: date, journal.title, publisher, 
  public static class HeterocyclesMetadataExtractor 
    implements FileMetadataExtractor {

    // get issue number from pdf url
    // <heterocyclesbase>.com/clockss/downloads/PDF/23208/83/1
    private Pattern ISSUE_PATTERN = Pattern.compile(
        "/downloads/PDF/[^/]+/[^/]+/([^/]+)$", Pattern.CASE_INSENSITIVE);

    @Override
    public void extract(MetadataTarget target, 
        CachedUrl pdfCu, Emitter emitter) throws IOException {
      
      log.debug3("Metadata - cachedurl pdf cu:" + pdfCu.getUrl());
            
      ArticleMetadata am = new ArticleMetadata();
      Matcher mat = ISSUE_PATTERN.matcher(pdfCu.getUrl());
      if (mat.find()) {
        String issue = mat.group(1);
        am.put(MetadataField.FIELD_ISSUE, issue);
      }

      emitter.emitMetadata(pdfCu, am);
    }
  }
  
}
 
