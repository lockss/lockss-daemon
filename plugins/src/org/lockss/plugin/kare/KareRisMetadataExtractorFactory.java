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

package org.lockss.plugin.kare;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

/*
TY  - JOUR
T1  - Pain and pain behavior
AU  - Güleç, Gülcan
AU  - Güleç, Sacit
Y1  - 2006
PY  - 2006
DA  - 2006
N1  - doi:
DO  -
T2  - Agri
JF  - Agri
JO  - Ağrı
SP  - 5
EP  - 9
VL  - 18
IS  - 4
SN  -
M3  - PMID: 17457708
UR  - https://dx.doi.org/
Y2  - 2006
ER  -
 */
public class KareRisMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(KareRisMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new KareRisMetadataExtractor();
  }

  public static class KareRisMetadataExtractor
      extends RisMetadataExtractor {

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, FileMetadataExtractor.Emitter emitter)
        throws IOException, PluginException {

      // this extracts from the ris file and cooks the data according to the map
      ArticleMetadata am = extract(target, cu);

      // check for existence of doi, if doesnt exist, we are looking at some cover page, toc, etc, ignore it.
      String doi = am.get(MetadataField.FIELD_DOI);
      if(doi == null || doi.isEmpty()) {
        return; // do not emit, just return - no content
      }
      emitter.emitMetadata(cu, am);
    }

  }
}
