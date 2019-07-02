/*

 * $Id$
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
 
