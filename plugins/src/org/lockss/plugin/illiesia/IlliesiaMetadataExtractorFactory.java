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

package org.lockss.plugin.illiesia;

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
 * Each article consists of a full text pdf and nothing else.  All abstracts
 * are listed in one page. ROLE_ARTICLE_METADATA can not be set.
 * BaseArticleMetadataExtractor provides default metadata from tdb file.
 *   Issue toc containing abstracts - <illiesiabase>/html/2013.html
 *   pdf - <illiesiabase>/papers/Illiesia09-01.pdf
 * The cached url is pdf.
 * 
 * A sample metadata from daemon:
 * ArticleFiles
 *   Full text CU:  http://www2.pms-lj.si/illiesia/papers/Illiesia09-01.pdf
 *   FullTextPdfFile:  http://www2.pms-lj.si/illiesia/papers/Illiesia09-01.pdf
 * Metadata
 *   access.url: http://www2.pms-lj.si/illiesia/papers/Illiesia09-01.pdf
 *   date: 2013
 *   eissn: 1854-0392
 *   issn: 1855-5810
 *   item_number: 01
 *   publication.title: Illesia International Journal of Stonefly Research
 *   publisher: Illiesia
 *   pubtype: journal
 *   volume: 9
 * Raw Metadata (empty)
 */
public class IlliesiaMetadataExtractorFactory 
  implements FileMetadataExtractorFactory {
  
  static Logger log = Logger.getLogger(IlliesiaMetadataExtractorFactory.class);
  
  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new IlliesiaMetadataExtractor();
  }

  // Gets default metadata from tdb: date, journal.title, publisher, 
  public static class IlliesiaMetadataExtractor 
    implements FileMetadataExtractor {

    // get article number from pdf url
    // Thib suggests putting "\\.pdf" instead of ".pdf"
    private Pattern ARTICLE_NUM_PATTERN = 
      Pattern.compile("/papers/Illiesia[0-9]+-([0-9]+).pdf$");      

    @Override
    public void extract(MetadataTarget target, 
        CachedUrl pdfCu, Emitter emitter) throws IOException {
      
      log.debug3("Metadata - cachedurl pdf cu:" + pdfCu.getUrl());
            
      ArticleMetadata am = new ArticleMetadata();
      Matcher mat = ARTICLE_NUM_PATTERN.matcher(pdfCu.getUrl());
      if (mat.find()) {
        String articleNum = mat.group(1);
        am.put(MetadataField.FIELD_ITEM_NUMBER, articleNum);
      }

      emitter.emitMetadata(pdfCu, am);
    }
  }
  
}
 
