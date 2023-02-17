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

package org.lockss.plugin.palgrave;

import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.util.Logger;

  // Palgrave Book Citation Export:
  // TY  - BOOK
  // AU  - Larsson, Mats
  // DA  - 2014/08/01
  // PY  - 2012
  // TI  - The Business of Global Energy Transformation: Saving Billions through Sustainable Models
  // PB  - Palgrave Macmillan
  // CY  - Basingstoke
  // SN  - 9781137024497
  // DO  - 10.1057/9781137024497
  // UR  - http://dx.doi.org/10.1057/9781137024497
  // L1  - http://www.palgraveconnect.com/pc/busman2013/browse/inside/download/9781137024497.pdf
  // ER  -
  public class PalgraveBookRisMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
    
    static Logger log = Logger.getLogger(PalgraveBookRisMetadataExtractorFactory.class);
    
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
          String contentType) 
              throws PluginException {
          
      RisMetadataExtractor ris = new RisMetadataExtractor();
      // removing use of DA - PalgraveBook uses this for citation access date
      // and uses PY for publication year
      if (ris.containsRisTag("DA")) {
        ris.removeRisTag("DA");
      }
      log.debug3("ris: " + ris.toString());
      ris.addRisTag("PY", MetadataField.FIELD_DATE);
      ris.addRisTag("TI", MetadataField.FIELD_PUBLICATION_TITLE);
      ris.addRisTag("SN", MetadataField.FIELD_EISBN);
      ris.addRisTag("DO", MetadataField.FIELD_DOI);
      ris.addRisTag("L1", MetadataField.FIELD_ACCESS_URL);
    
    return ris;
  }

}
