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