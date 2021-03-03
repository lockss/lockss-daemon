/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.atypon;

import java.io.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/**
 * A metadata extractor so we can NOT emit when an article is found for which
 * there is no existing metadata extractor. 
 * Currently the daemon just emits the appropriate tdb defaults.
 * For Atypon, we hit this case if we have ONLY a pdf mime-type associated
 * with an articlefiles.  This occurs with a dead-end overcrawl due to an 
 * in-line link to a PDF (usually of an original article from a corrigendum)
 * @author alexohlson
 *
 */
public class BaseAtyponNullMetadataExtractorFactory 
  implements FileMetadataExtractorFactory {
  
  public FileMetadataExtractor 
    createFileMetadataExtractor(MetadataTarget target, String contentType)
      throws PluginException {
    return new BaseAtyponNullMetadataExtractor();
  }

  public static class BaseAtyponNullMetadataExtractor
    implements FileMetadataExtractor {
    static Logger log = Logger.getLogger(BaseAtyponNullMetadataExtractor.class);
 

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      log.debug("Suppressing emit of metadata in Null extractor: " + cu.getUrl());
      // do nothing, do not allow TDB info to get used as default
      // by not emitting
    }
    
  }
}