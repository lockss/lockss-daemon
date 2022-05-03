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

package org.lockss.plugin.clockss.wiley;

import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;

import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

import java.io.IOException;
import java.util.Iterator;


public class WileyMRWSourceZipMetadataExtractorFactory implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(WileyMRWSourceZipMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
          throws PluginException {

    log.debug3("WileyMRWSourceZip: WileyMRWSourceZipMetadataExtractor is created" );

    return new WileyMRWSourceZipMetadataExtractor();
  }

  public static class WileyMRWSourceZipMetadataExtractor
          implements FileMetadataExtractor {

    // override this to do some additional atunzippedXmlCuts to get valid data before emitting
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, FileMetadataExtractor.Emitter emitter)
            throws IOException, PluginException {

      CachedUrlSet zipUrls = null;
      ArchivalUnit au = cu.getArchivalUnit();

      WileyMRWSourceXmlMetadataExtractorFactory.WileyMRWSourceXmlMetadataExtractor schemaHelper = null;


      //cu = https://clockss-test.lockss.org/sourcefiles/wileybooks-released/2022_01/9780470007259.zip
      log.debug3("WileyMRWSourceZip cu " + cu.getUrl());

      if (zipUrls == null) {
        String zipPath = cu.getUrl();
        if (zipPath.contains(".zip")) {
          log.debug3("WileyMRWSourceZip zip cu " + cu.getUrl());
          zipUrls = au.makeCachedUrlSet(new RangeCachedUrlSetSpec(zipPath));
        } else {
          log.debug3("WileyMRWSourceZip not zip file " + cu.getUrl());
        }
      }

      if (zipUrls != null) {

        Iterator<CachedUrl> iter = zipUrls.archiveMemberIterator();

        while (iter.hasNext()) {
          CachedUrl singleUrl = iter.next();

          String tryUrl = singleUrl.getUrl();
          log.debug3("WileyMRWSourceZip zipUrls not null, trycu =  " + singleUrl);

          if (tryUrl.contains(".xml")) {
            ArticleMetadata am;
            
            log.debug3("WileyMRWSourceZip tryUrl: "  + tryUrl);

            if ( schemaHelper == null) {

              CachedUrl unzippedXmlCu = au.makeCachedUrl(tryUrl);

              log.debug3("WileyMRWSourceZip making cachedUrl : "  + unzippedXmlCu.getUrl() + "");
              schemaHelper = new WileyMRWSourceXmlMetadataExtractorFactory.WileyMRWSourceXmlMetadataExtractor();
              schemaHelper.extract(target, unzippedXmlCu, emitter);
              if ( schemaHelper == null) {
                log.debug3("WileyMRWSourceZip setup schema failed.......");
              } else {
                log.debug3("WileyMRWSourceZip setup schema succeed.......");
              }
            }
            break;   //since all the xml file contains the same information, exit when found first
          }
        }

      } else {
        log.debug3("WileyMRWSourceZip zipUrls is null for cu " + cu.getUrl());
      }
    }
  }
}
