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

package org.lockss.plugin.atypon.bloomsburyqatar;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;

/**
 * One of the articles used to get the html source for this plugin is:
 * view-source:http://www.qscience.com/doi/full/10.5339/nmejre.2011.2
 */
public class BloomsburyQatarHtmlMetadataExtractorFactory
extends BaseAtyponHtmlMetadataExtractorFactory {
  static Logger log = Logger.getLogger("BloomsburyQatarHtmlMetadataExtractorFactory");

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new BloomsburyQatarHtmlMetadataExtractor();
  }

  public static class BloomsburyQatarHtmlMetadataExtractor 
  extends BaseAtyponHtmlMetadataExtractor {

    /**
     * Use parent to extract raw metadata, map
     * to cooked fields, then do specific extract for extra tags by reading the file.
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {

      // extract but do some more processing before emitting
      ArticleMetadata am = 
          new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(getTagMap()); //parent set the tagMap

      getAdditionalMetadata(cu, am);

      emitter.emitMetadata(cu, am);
    }

    private void getAdditionalMetadata(CachedUrl cu, ArticleMetadata am) 
    {
      //Extracts doi from url (doi is included in file, but not formatted well)
      //metadata could come from either full text html or abstract - figure out which
      String doi;  
      if ( (cu.getUrl()).contains("abs/")) {
        doi = cu.getUrl().substring(cu.getUrl().indexOf("abs/")+4);
      } else 
        doi = cu.getUrl().substring(cu.getUrl().indexOf("full/")+5);
      if ( !(doi == null) && !(doi.isEmpty())) {
        am.put(MetadataField.FIELD_DOI,doi);
      }

      //Extracts the volume and issue number from the end of the doi
      String suffix = doi.substring(doi.indexOf("/"));
      am.put(MetadataField.FIELD_ISSUE, suffix.substring(suffix.lastIndexOf(".")+1));
      am.put(MetadataField.FIELD_VOLUME, suffix.substring(suffix.lastIndexOf(".", suffix.lastIndexOf(".")-1)+1, suffix.lastIndexOf(".")));

      // lastly, hardwire the publisher if it hasn't been set
      if (am.get(MetadataField.FIELD_PUBLISHER) == null) {
        am.put(MetadataField.FIELD_PUBLISHER, "Bloomsbury Qatar Foundation Journals");
      }

    }
  }
}