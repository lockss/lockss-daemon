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

package org.lockss.plugin.bepress;

import java.io.IOException;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

/*
 * There is no correlation in url patterns for the article aspects,
 * pdfs and abstracts. Hence, we can not guess the abstract urls 
 * from pdf urls. The abstract pages contain metadata; however, since 
 * the abstract urls can not be guessed, ROLE_ARTICLE_METADATA can not be set. 
 * By default, BaseArticleMetadataExtractor extracts some metadata from the 
 * tdb files. The cached url is pdf.
 * 
 * abstract - <dcrbase>.edu/xxxdept/122
 * pdf - <dcrbase>.edu/cgi/viewcontent.cgi?article=1108&context=xxxdept
 *                                      
 * A sample metadata from daemon:
 * ArticleFiles
 *  Full text CU: 
 *      <dcrbase>.edu/cgi/viewcontent.cgi?article=1046&context=xxxdept
 *  FullTextPdfFile:  
 *      <dcrbase>.edu/cgi/viewcontent.cgi?article=1046&context=xxxdept
 * Metadata
 *  access.url: <dcrbase>.edu/cgi/viewcontent.cgi?article=1046&context=xxxdept
 *  date: 2012
 *  journal.title: Department of xxxdept
 *  publisher: Carnegie Mellon University Libraries
 * Raw Metadata (empty)
 */
public class DigitalCommonsRepositoryMetadataExtractorFactory 
  implements FileMetadataExtractorFactory {
  
  static Logger log = Logger.getLogger(
      DigitalCommonsRepositoryMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new DigitalCommonsRepositoryMetadataExtractor();
  }

  // Gets default metadata from tdb: date, journal.title, publisher, 
  public static class DigitalCommonsRepositoryMetadataExtractor 
    implements FileMetadataExtractor {

    @Override
    public void extract(MetadataTarget target, 
        CachedUrl pdfCu, Emitter emitter) throws IOException {
      
      log.debug3("Metadata - cachedurl pdf cu:" + pdfCu.getUrl());
            
      ArticleMetadata am = new ArticleMetadata();
      emitter.emitMetadata(pdfCu, am);
    }
  }
  
}
 
