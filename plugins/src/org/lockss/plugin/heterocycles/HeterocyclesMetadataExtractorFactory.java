/*

 * $Id$
 */

/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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
 
