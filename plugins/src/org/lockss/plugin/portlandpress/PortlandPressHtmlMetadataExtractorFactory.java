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

package org.lockss.plugin.portlandpress;

import java.io.*;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class PortlandPressHtmlMetadataExtractorFactory 
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("PortlandPressHtmlMetadataExtractorFactory");

  public FileMetadataExtractor 
    createFileMetadataExtractor(MetadataTarget target, String contentType)
      throws PluginException {
    return new PortlandPressHtmlMetadataExtractor();
  }

  public static class PortlandPressHtmlMetadataExtractor 
    implements FileMetadataExtractor {

    // Map Google Scholar HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("DC.Publisher", MetadataField.DC_FIELD_PUBLISHER);
      tagMap.put("DC.Publisher", MetadataField.FIELD_PUBLISHER);
      
      tagMap.put("DC.Date", MetadataField.FIELD_DATE);
      tagMap.put("DC.Date", MetadataField.DC_FIELD_DATE);
      
      tagMap.put("DC.Identifier", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("DC.Identifier", MetadataField.FIELD_ACCESS_URL);
      
      tagMap.put("DC.Language", MetadataField.DC_FIELD_LANGUAGE);
      
      tagMap.put("DC.Rights", MetadataField.DC_FIELD_RIGHTS);
      
      tagMap.put("DC.Title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("DC.Title", MetadataField.DC_FIELD_TITLE);      
      
      tagMap.put("DC.Creator",MetadataField.FIELD_AUTHOR);
      tagMap.put("DC.Creator", MetadataField.DC_FIELD_CREATOR);
      
      tagMap.put("DC.Keyword", MetadataField.FIELD_KEYWORDS);
      tagMap.put("Keywords", MetadataField.FIELD_KEYWORDS);
      
      tagMap.put("PPL.Volume", MetadataField.FIELD_VOLUME);
      tagMap.put("PPL.FirstPage", MetadataField.FIELD_START_PAGE);
      tagMap.put("PPL.LastPage", MetadataField.FIELD_END_PAGE);
      tagMap.put("PPL.DOI", MetadataField.FIELD_DOI);
    }
    
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am = 
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
 
      am.cook(tagMap);
      
      emitter.emitMetadata(cu, am);
    }
  }
}