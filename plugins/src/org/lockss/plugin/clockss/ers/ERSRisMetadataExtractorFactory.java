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

package org.lockss.plugin.clockss.ers;

import java.io.IOException;
import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

/*
 * The following are the default tags:
 *     T1, AU, JF,DO, PB, VL, IS, SP,EP, DA
 * Should the use of RIS become more common, we can generalize this
 *  NOTE: for European Respiratory Society I have found the following
 * TY  - BOOK
 * AU  - 
 * A2  - Bankier, A.
 * Gevenois, P.A.
 * PY  - 2004
 * T1  - Imaging (out of print)
 * PB  - European Respiratory Society
 * SP  - 363
 * DA  - 2004-12-01 00:00:00
 * SN  - 9781904097907
 * M3  - 10.1183/1025448x.erm3004
 * N2  - This line can be quite long... 
 * UR  - http://erspublications.com/content/9781904097907/9781904097907
 * ER  - 
 */
public class ERSRisMetadataExtractorFactory
implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(ERSRisMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {

    log.debug3("Inside ERS Metadata extractor factory for RIS files");

    ERSRisMetadataExtractor ers_ris = new ERSRisMetadataExtractor();

    ers_ris.addRisTag("A1", MetadataField.FIELD_AUTHOR);
    ers_ris.addRisTag("A2", MetadataField.FIELD_AUTHOR);
    ers_ris.addRisTag("SN", MetadataField.FIELD_ISBN);
    ers_ris.addRisTag("M3", MetadataField.FIELD_DOI);
    // Do not use UR listed in the ris file! It will get set to full text CU by daemon
    return ers_ris;
  }

  public static class ERSRisMetadataExtractor
  extends RisMetadataExtractor {

    // override this to do some additional attempts to get valid data before emitting
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, FileMetadataExtractor.Emitter emitter) 
        throws IOException, PluginException {
      
      // this extracts from th file and cooks the data according to the map
      ArticleMetadata am = extract(target, cu); 

      // post-cook processing...
      // check for existence of content file - return without emitting if not there
      String url_string = cu.getUrl();
      String pdfName = url_string.substring(0,url_string.length() - 3) + "pdf";
      ArchivalUnit au = cu.getArchivalUnit();
      CachedUrl fileCu = au.makeCachedUrl(pdfName);
      log.debug3("Check for existence of " + pdfName);
      if(fileCu == null || !(fileCu.hasContent())) {
        log.debug3(pdfName + " was not in cu");
        return; // do not emit, just return - no content
      }

      // correction or fallback values in to cooked map here
      
      // These are books, we know that. So set the publication title to the title
      am.put(MetadataField.FIELD_PUBLICATION_TITLE,  am.getRaw("T1"));
      am.put(MetadataField.FIELD_ACCESS_URL, pdfName);
 
      emitter.emitMetadata(cu, am);
    }
    
  }

}
