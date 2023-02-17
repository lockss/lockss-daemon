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

package org.lockss.plugin.ingenta;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/*
 * A much simpler html extractor for books
 * I am not reworking the existing journals extractor in order not to destabilize
 * a heavily used plugin
 */
public class IngentaBooksHtmlMetadataExtractorFactory implements
    FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(IngentaBooksHtmlMetadataExtractorFactory.class);

  private static Pattern isbnPattern = Pattern.compile("urn:ISSN:(.*)");

  
  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new IngentaHtmlMetadataExtractor();
  }

  public static class IngentaHtmlMetadataExtractor 
    extends SimpleHtmlMetaTagMetadataExtractor {
    
    private static MultiMap tagMap = new MultiValueMap();
    static {
      
      tagMap.put("DC.creator", MetadataField.FIELD_AUTHOR);
      tagMap.put("DCTERMSissued", MetadataField.FIELD_DATE);
      tagMap.put("DC.title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("DCTERMS.isPartOf", new MetadataField(
          MetadataField.FIELD_ISBN, MetadataField.extract(isbnPattern,1)));
      tagMap.put("DC.publisher", MetadataField.DC_FIELD_PUBLISHER);
    }
    
    // publication, volume, issue, spage-epage(totalpages)
    // This is very specific, but give it a try
    //<meta name="DCTERMS.bibliographicCitation" content="A Simpler Way, , , 56-64(9)"/>
    private static final Pattern fullPattern = Pattern.compile(
        "(.*)[,] [,] [,]([^-]+)[-]([^-()]+)", Pattern.CASE_INSENSITIVE);
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
      
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      
     // This is by definition defined for this AU
     if (am.get(MetadataField.FIELD_ISBN) == null) {
       //the isbn is available from the params
       ArchivalUnit au = cu.getArchivalUnit();
       TypedEntryMap tfProps = au.getProperties();
       String AU_isbn = tfProps.getString("book_isbn");
       am.put(MetadataField.FIELD_ISBN, AU_isbn);
     }
      
      //entire book: <meta name="DCTERMS.bibliographicCitation" content="A Simpler Way, , , 1-168(168)"/>
      //chapter: <meta name="DCTERMS.bibliographicCitation" content="A Simpler Way, , , 56-64(9)"/>
      String raw_biblio = am.getRaw("DCTERMS.bibliographicCitation");
      if (raw_biblio != null && !(raw_biblio.isEmpty())) {
        Matcher m = fullPattern.matcher(raw_biblio);
        if (m.find()) { 
          // use what we did find with the matcher - we know we at least had 3 groups... 
          if (!(m.group(1)).isEmpty()) { am.put(MetadataField.FIELD_PUBLICATION_TITLE, m.group(1)); }
          if (!(m.group(2)).isEmpty()) { am.put(MetadataField.FIELD_START_PAGE,  m.group(2));}
          if (!(m.group(3)).isEmpty()) { am.put(MetadataField.FIELD_END_PAGE,  m.group(3));}
        }
      }
      return am;
    }
  }
}

