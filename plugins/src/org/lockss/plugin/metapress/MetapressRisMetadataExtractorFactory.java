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

package org.lockss.plugin.metapress;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.util.Logger;

/*
 * TY  - JOUR
JF  - Electronic Government, an International Journal 
T1  - Evaluating usability, user satisfaction and intention to revisit for successful e-government websites
VL  - 8
IS  - 1
SP  - 1
EP  - 19
PY  - 2011/01/01/
UR  - http://dx.doi.org/10.1504/EG.2011.037694
DO  - 10.1504/EG.2011.037694
AU  - Byun, Dae-Ho
AU  - Finnie, Gavin
 */
public class MetapressRisMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(MetapressRisMetadataExtractorFactory.class);
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                 String contentType)
      throws PluginException {
  
    log.debug3("In createFileMetadataExtractor");
    
    //TODO 1.70 - remove LocalRisMetadataExtractor and go back to using daemon class RisMetadataExtarctor
    // newer version handles multi-line values
    LocalRisMetadataExtractor ris = new LocalRisMetadataExtractor();
    ris.addRisTag("PY", MetadataField.FIELD_DATE);
    
    return ris;
  }
}
