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

package org.lockss.plugin.dividedsociety;

import java.io.*;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.config.TdbAu;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class DividedSocietyHtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  
  static Logger log = Logger.getLogger(
      DividedSocietyHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new DividedSocietyHtmlMetadataExtractor();
  }

  public static class DividedSocietyHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {
    
    // Map ACSESS Books HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();

    static {
    	
        tagMap.put("dcterms.publisher", MetadataField.DC_FIELD_PUBLISHER);
        //for now don't cook the publisher - wait to hear back from Kim, set to "Linen Hall Library"
        //tagMap.put("dcterms.publisher", MetadataField.FIELD_PUBLISHER);

        tagMap.put("dcterms.date", MetadataField.DC_FIELD_DATE);
        tagMap.put("dcterms.date", MetadataField.FIELD_DATE);

        tagMap.put("dcterms.title", MetadataField.DC_FIELD_TITLE);
        tagMap.put("dcterms.title", MetadataField.FIELD_ARTICLE_TITLE);
        
        tagMap.put("dcterms.identifier", MetadataField.DC_FIELD_IDENTIFIER);

        tagMap.put("dcterms.creator", MetadataField.DC_FIELD_CREATOR);
        tagMap.put("dcterms.creator",
                new MetadataField(MetadataField.FIELD_AUTHOR,
                    MetadataField.splitAt(";")));

        tagMap.put("dcterms.relation", MetadataField.DC_FIELD_RELATION);
        tagMap.put("dcterms.relation", MetadataField.FIELD_PUBLICATION_TITLE);

        } // static
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
      log.debug3("Metadata - cachedurl cu:" + cu.getUrl());
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      
      /*
       * Do some post-cook cleanup. Noted several pieces of content didn't get a publication title
       * And posters, outreach, and essays don't have one, so use the TDB name
       */
      //for now don't set the publisher and it will come from TDB value
      
      if (am.get(MetadataField.FIELD_PUBLICATION_TITLE) == null) {
    	  ArchivalUnit thisau = cu.getArchivalUnit();
    	  TdbAu tdbau = thisau.getTdbAu();
    	  String au_name = (tdbau == null) ? null : tdbau.getName();
    	  if (au_name != null) {
    		  am.put(MetadataField.FIELD_PUBLICATION_TITLE, au_name);
    	  }
      }
      // There are about 29 items that just don't give a date and we can't get it from tdb
      // because we're preserving an entire run of a topic or journal.
      // There is one case where it just needs cleaning up leading dash - "-1991"
      if ((am.get(MetadataField.FIELD_DATE) == null) || (am.get(MetadataField.FIELD_DATE).startsWith("-"))){
    	  String rawdate = am.getRaw("dcterms.date");
    	  if (rawdate != null) {
    		  rawdate.replace("-", "");
        	  am.put(MetadataField.FIELD_DATE, rawdate);
    	  }
      }
      // publisher will get set from the TDB file to Linen Hall Library if it wasn't pulled from the content
      return am;
    }
    
  }
  
}
 
