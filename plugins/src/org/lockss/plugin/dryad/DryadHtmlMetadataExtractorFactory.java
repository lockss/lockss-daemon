/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.dryad;

import java.io.IOException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/*
 * Metadata on an abstract page http://http://datadryad.org/resource/doi:<doi>
 * in the form of:
 * <meta content="MolEcol-<uid>" name="DC.identifier">
 */

public class DryadHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(DryadHtmlMetadataExtractorFactory.class);
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
        String contentType)
      throws PluginException {
    return new DryadHtmlMetadataExtractor();
  }
  
  public static class DryadHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {
    
    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("dc.title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("dc.title", MetadataField.DC_FIELD_TITLE);
      tagMap.put("dc.contributor", MetadataField.FIELD_AUTHOR);
      tagMap.put("dc.creator", MetadataField.DC_FIELD_CREATOR);
      
      //content="doi:<doi>"/>
      tagMap.put("dc.identifier", new MetadataField(
          MetadataField.FIELD_DOI, MetadataField.extract("doi:(.*)",1)));
      tagMap.put("dc.identifier", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("dcterms.dateAccepted", MetadataField.FIELD_DATE);
      tagMap.put("dcterms.dateAccepted", MetadataField.DC_FIELD_DATE);
      tagMap.put("dc.subject", 
          new MetadataField(MetadataField.FIELD_KEYWORDS,
              MetadataField.splitAt(";")));
      tagMap.put("dc.description", MetadataField.DC_FIELD_DESCRIPTION);
      tagMap.put("dc.type", MetadataField.DC_FIELD_TYPE);
      tagMap.put("dcterms.haspart", MetadataField.DC_FIELD_RELATION);
    }
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
        throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      
      am.cook(tagMap);
      
      return am;
    }
  }
}
