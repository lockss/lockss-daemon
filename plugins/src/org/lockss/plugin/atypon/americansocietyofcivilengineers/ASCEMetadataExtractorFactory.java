/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.atypon.americansocietyofcivilengineers;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.atypon.BaseAtyponRisMetadataExtractorFactory.BaseAtyponRisMetadataExtractor;
import org.lockss.util.Logger;

public class ASCEMetadataExtractorFactory implements FileMetadataExtractorFactory{

      private static final Logger log = Logger.getLogger(ASCEMetadataExtractorFactory.class);

      public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)throws PluginException {

        log.debug3("Inside Base Atypon Metadata extractor factory for RIS files");

        ASCEMetadataExtractor ASCE_ris = new ASCEMetadataExtractor();
        return ASCE_ris;
    }

     public static class ASCEMetadataExtractor extends BaseAtyponRisMetadataExtractor {
            @Override
            protected void postCookProcess(CachedUrl cu, ArticleMetadata am, String ris_type)  {
                super.postCookProcess(cu,am,ris_type);

               // am.put(MetadataField.FIELD_EISBN,  am.getRaw("SN"));
               // am.put(MetadataField.FIELD_ISSN, null);
            }
    }
}
