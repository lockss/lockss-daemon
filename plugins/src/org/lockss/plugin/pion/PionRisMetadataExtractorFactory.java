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

package org.lockss.plugin.pion;

import java.io.IOException;

import org.lockss.config.TdbAu;
import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

/*
 * Augment the standard RisMetadataExtractor to pull metadata from a .ris file
 * found: <base_url>/<vol#>/<issue#>/<year>/<abstract_base>.ris
 * The Pion RIS file is a little non-standard, so add to the map. It looks like this:
 * 
 * TY  - JOUR
 * A1  - Lowe, N
 * A1  - Hagan, J
 * A1  - Iskander, N
 * Y1  - 2010
 * T1  - Revealing talent: informal skills intermediation as an emergent pathway to immigrant labor market incorporation
 * JO  - Environment and Planning A
 * SP  - 205
 * EP  - 222
 * VL  - 42
 * IS  - 1
 * UR  - http://www.envplan.com/abstract.cgi?id=a4238
 * PB  - Pion Ltd
 * SN  - 
 * N2  - This is a description of the article.
 * ER  -
 */
public class PionRisMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(PionRisMetadataExtractorFactory.class);
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    
    log.debug3("Inside Pion Metadata extractor factory");
    
    PionRisMetadataExtractor ris = new PionRisMetadataExtractor();

    /* Pion seems to use different tags for these RIS fields */
    ris.addRisTag("JO", MetadataField.FIELD_PUBLICATION_TITLE); // requires
    ris.addRisTag("A1", MetadataField.FIELD_AUTHOR);
    ris.addRisTag("Y1", MetadataField.FIELD_DATE);
    ris.addRisTag("UR", MetadataField.FIELD_ACCESS_URL);
    ris.addRisTag("N2", MetadataField.FIELD_ABSTRACT);
    /* Our MetadataField doesn't yet support either of these field types */
    /*    ris.addRisTag("J1", MetadataField.FIELD_JOURNAL_ALTERNATE_NAME); */
    /*    ris.addRisTag("L1", MetadataField.FIELD_FILE); */

     return ris;
  }
  public static class PionRisMetadataExtractor
  extends RisMetadataExtractor {

    // override this to do some additional attempts to get valid data before emitting
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
      ArticleMetadata md = extract(target, cu);
      
      if (md != null) {
        if (!md.hasValidValue(MetadataField.FIELD_DOI)) {
          // fill in DOI from accessURL
          // http://www.envplan.com/abstract.cgi?id=a42117
          // -> doi=10.1068/a42117
          String accessUrl = md.get(MetadataField.FIELD_ACCESS_URL);
          if ((accessUrl == null) || !accessUrl.startsWith("http")) {
            accessUrl = cu.getUrl();
          }
          int i = accessUrl.indexOf("id=");
          if (i > 0) {
            String doi = "10.1068/" +accessUrl.substring(i+3);
            md.put(MetadataField.FIELD_DOI, doi);
          }
          else {
            log.debug("accessUrl did not have id= " + accessUrl);
          }
        }
        completeMetadata(cu, md);
        emitter.emitMetadata(cu, md);
      }
    }
  };
  
  public static void completeMetadata(CachedUrl cu, ArticleMetadata am) {
    // Pick up some information from the TDB if not in the cooked data
    TdbAu tdbau = cu.getArchivalUnit().getTdbAu(); // returns null if titleConfig is null 
    if (tdbau != null) {
      if (am.get(MetadataField.FIELD_PUBLISHER) == null) {
        // We can try to get the publishger from the tdb file.  This would be the most accurate
        String publisher = tdbau.getPublisherName();
        if (publisher != null) {
          am.put(MetadataField.FIELD_PUBLISHER, publisher);
        }
      }
      if (am.get(MetadataField.FIELD_PUBLICATION_TITLE) == null) {
        String journal_title = tdbau.getPublicationTitle();
        if (journal_title != null) {
          am.put(MetadataField.FIELD_PUBLICATION_TITLE, journal_title);
        }
      }
    }
  }
}
