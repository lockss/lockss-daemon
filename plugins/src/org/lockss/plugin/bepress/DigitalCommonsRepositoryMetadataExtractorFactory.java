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
 
