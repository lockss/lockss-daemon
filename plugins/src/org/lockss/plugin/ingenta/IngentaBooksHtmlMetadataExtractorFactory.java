/*
 * $Id$
 */

/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

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

