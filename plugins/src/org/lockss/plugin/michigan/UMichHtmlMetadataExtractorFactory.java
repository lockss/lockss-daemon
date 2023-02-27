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

package org.lockss.plugin.michigan;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;


//<meta name="citation_title" content="Printing and Prophecy: Prognostication and Media Change 1450-1550">
//<meta name="citation_author" content="Green, Jonathan">
//<meta name="citation_publication_date" content="2011">
//<meta name="citation_online_date" content="2011">
//<meta name="citation_isbn" content="978-0-472-11783-3">
//<meta name="citation_isbn" content="978-0-472-02758-3">
//<meta name="citation_isbn" content="978-0-472-90074-9">
//<meta name="citation_doi" content="doi:10.3998/mpub.3209249">
//<meta name="citation_publisher" content="University of Michigan Press">

public class UMichHtmlMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(
		  UMichHtmlMetadataExtractorFactory.class);
  
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
        String contentType)
      throws PluginException {
    return new UMichHtmlMetadataExtractor();
  }

  public static class UMichHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {
    
    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER); // XXX
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_isbn", MetadataField.FIELD_ISBN);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_author",
          new MetadataField(MetadataField.FIELD_AUTHOR,
                            MetadataField.splitAt(",")));
    }
    
    
  }
  
}
