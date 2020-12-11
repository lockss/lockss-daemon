/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.copernicus;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

/*
 * Augment the standard RisMetadataExtractor to pull metadata from a .ris file
 * found: <base_url>/<vol#>/<issue#>/<year>/<abstract_base>.ris
 * The Copernicus RIS file is a little non-standard, so add to the map. It looks like this:
 * 
 * TY  - JOUR
 * A1  - Winkler, R.
 * A1  - Landais, A.
 * A1  - Sodemann, H.
 * A1  - Damgen, L.
 * A1  - Priac, F.
 * A1  - Masson-Delmotte, V.
 * A1  - Stenni, B.
 * A1  - Jouzel, J.
 * T1  - Deglaciation records of 17O-excess in East Antarctica:  reliable reconstruction of oceanic normalized relative humidity from coastal sites
 * JO  - Clim. Past
 * J1  - CP
 * VL  - 8
 * IS  - 1
 * SP  - 1
 * EP  - 16
 * Y1  - 2012/01/03
 * PB  - Copernicus Publications
 * SN  - 1814-9332
 * UR  - http://www.clim-past.net/8/1/2012/
 * L1  - http://www.clim-past.net/8/1/2012/cp-8-1-2012.pdf
 * DO  - 10.5194/cp-8-1-2012
 * ER  - 
 * 
 */
public class CopernicusRisMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(CopernicusRisMetadataExtractorFactory.class);
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    
    log.debug3("Inside Copernicus Metadata extractor factory");
    
    RisMetadataExtractor ris = new CopernicusRisMetadataExtractor();
    /* Copernicus seems to use different tags for these RIS fields */
    ris.addRisTag("JO", MetadataField.FIELD_JOURNAL_TITLE);
    ris.addRisTag("A1", MetadataField.FIELD_AUTHOR);
    ris.addRisTag("Y1", MetadataField.FIELD_DATE);
    // later in the extraction, we check if this is a valid value
    ris.addRisTag("UR", MetadataField.FIELD_ACCESS_URL);
    /* Our MetadataField doesn't yet support either of these field types */
    /*    ris.addRisTag("J1", MetadataField.FIELD_JOURNAL_ALTERNATE_NAME); */
    /*    ris.addRisTag("L1", MetadataField.FIELD_FILE); */

     return ris;
  }
   
  public static class CopernicusRisMetadataExtractor
  extends RisMetadataExtractor {
    
    private final static Pattern RIS_FILENAME_PATTERN = Pattern.compile("\\/([^/]+)\\.ris$");

    // override this to verify access_url before emitting
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, FileMetadataExtractor.Emitter emitter) 
        throws IOException, PluginException {
      ArticleMetadata am = extract(target, cu); 

      /*
       *  UR might be a url that isn't in the AU
       *  eg - a "suffixless" version of the filename
       */
      Boolean access_url_valid = false;
      // use the UR if it was set and exists
      if (am.getRaw("UR") != null) {
        String potential_access_url = am.getRaw("UR");
        CachedUrl potential_cu = cu.getArchivalUnit().makeCachedUrl(potential_access_url);
        if ( (potential_cu != null) && (potential_cu.hasContent()) ){     
          am.replace(MetadataField.FIELD_ACCESS_URL, potential_access_url);
          access_url_valid = true;
        }
      }
      // the UR wasn't set or wasn't preserved
      if (!access_url_valid) {
        // get the filename from this RIS file
        Matcher mat = RIS_FILENAME_PATTERN.matcher(cu.getUrl());
        if (mat.find()) {
          String new_access_url = mat.replaceFirst("/$1.pdf");
          CachedUrl potential_cu = cu.getArchivalUnit().makeCachedUrl(new_access_url);
          if ( (potential_cu != null) && (potential_cu.hasContent()) ){     
            am.replace(MetadataField.FIELD_ACCESS_URL, new_access_url);
            access_url_valid = true;
          }
        }
      }
      if (!access_url_valid){
        // perhaps add additional attempts here, or go without
        log.debug3("The access_url for this article isn't in the AU!");
      }
      emitter.emitMetadata(cu, am);
    }

  }
}
