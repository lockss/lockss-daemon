/*
 * $Id: AMetSocHtmlMetadataExtractorFactory.java,v 1.1 2013-02-08 00:19:42 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.americanmeteorologicalsociety;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class AMetSocHtmlMetadataExtractorFactory 
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("AMetSocHtmlMetadataExtractorFactory");

  public FileMetadataExtractor 
    createFileMetadataExtractor(MetadataTarget target, String contentType)
      throws PluginException {
    return new AMetSocHtmlMetadataExtractor();
  }

  public static class AMetSocHtmlMetadataExtractor 
    implements FileMetadataExtractor {

    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("dc.Identifier", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("dc.Identifier", MetadataField.FIELD_DOI);
      tagMap.put("dc.Date", MetadataField.FIELD_DATE);
      tagMap.put("dc.Date", MetadataField.DC_FIELD_DATE);
      tagMap.put("dc.Creator",
                 new MetadataField(MetadataField.FIELD_AUTHOR,
                                   MetadataField.splitAt(";")));
      tagMap.put("dc.Creator", MetadataField.DC_FIELD_CREATOR);
      
      tagMap.put("dc.Title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("dc.Title", MetadataField.DC_FIELD_TITLE);
      tagMap.put("dc.Publisher", MetadataField.DC_FIELD_PUBLISHER);
      tagMap.put("dc.Type", MetadataField.DC_FIELD_TYPE);
      tagMap.put("dc.Format", MetadataField.DC_FIELD_FORMAT);
      tagMap.put("dc.Language", MetadataField.DC_FIELD_LANGUAGE);
      tagMap.put("dc.Coverage",MetadataField.DC_FIELD_COVERAGE);
      tagMap.put("dc.Rights", MetadataField.DC_FIELD_RIGHTS);
    }
    
    private String base_url;
    
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am = 
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
 
      am.cook(tagMap);
      
      // publisher name does not appear anywhere on the page in this form
      am.put(MetadataField.FIELD_PUBLISHER, "American Meteorological Society");

      // if the doi isn't in the metadata, we can still get it from the filename
      if (am.get(MetadataField.FIELD_DOI) == null) {
        
        /*matches() is anchored so must create complete pattern or else use .finds() */
        /* URL is "<base>/doi/(abs|full)/<doi1st>/<doi2nd> */
        base_url = cu.getArchivalUnit().getConfiguration().get("base_url");
        String patternString = "^" + base_url + "doi/[^/]+/([^/]+)/([^/]+)$";
        Pattern ABSTRACT_PATTERN = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
        String url = cu.getUrl();
        Matcher mat = ABSTRACT_PATTERN.matcher(url);
        
        if (mat.matches()) {
          log.debug3("Pull DOI from URL " + mat.group(1) + "." + mat.group(2));
          am.put(MetadataField.FIELD_DOI, mat.group(1) + "/" + mat.group(2));
        }
      }

      // publisher name does not appear anywhere on the page in this form
      am.put(MetadataField.FIELD_PUBLISHER, "American Institute of Aeronautics and Astronautics");
      emitter.emitMetadata(cu, am);
    }
  }
}