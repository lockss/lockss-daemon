/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.internationalunionofcrystallography;

import java.io.*;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.MetadataField.Cardinality;
import org.lockss.plugin.*;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;


public class InternationalUnionOfCrystallographyHtmlMetadataExtractorFactory 
    implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(
    InternationalUnionOfCrystallographyHtmlMetadataExtractorFactory.class.getName());

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new InternationalUnionOfCrystallographyMetadataExtractor();
  }
  
  public static class InternationalUnionOfCrystallographyMetadataExtractor 
    implements FileMetadataExtractor {
 
    // To do (PJG): Use the definitions from MetadataField once 1.59 is out
    private static final String KEY_LANGUAGE = "language";
    private static final MetadataField FIELD_LANGUAGE = new MetadataField(
        KEY_LANGUAGE, Cardinality.Single);
    private static final String KEY_FORMAT = "format";
    public static final MetadataField FIELD_FORMAT = new MetadataField(
        KEY_FORMAT, Cardinality.Single);
    public static final String KEY_PROPRIETARY_IDENTIFIER =
        "propietary_identifier";
    public static final MetadataField FIELD_PROPRIETARY_IDENTIFIER =
        new MetadataField(KEY_PROPRIETARY_IDENTIFIER, Cardinality.Single);

    
    // Map HighWire H20 HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("DC.Format", FIELD_FORMAT);
      tagMap.put("DC.Language", FIELD_LANGUAGE);
      tagMap.put("DC.Publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("citation_journal_title", MetadataField.FIELD_JOURNAL_TITLE);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
      tagMap.put("DC.Creator", MetadataField.FIELD_AUTHOR);
      tagMap.put("prism.issn", MetadataField.FIELD_ISSN);
      tagMap.put("prism.eissn", MetadataField.FIELD_EISSN);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("DC.Link", MetadataField.FIELD_ACCESS_URL);
      // typical field value: "acupmed;30/1/8": extract "acupmed"
      tagMap.put("citation_mjid", new MetadataField(
          FIELD_PROPRIETARY_IDENTIFIER, 
          MetadataField.extract("^([^;]+);", 1)));
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am = 
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);      
      //PostCook cleanup
      //If citation_doi missing, try dc.identifier: doi:10.1107/S160053680905394X
      if (am.get(MetadataField.FIELD_DOI) == null) {
        if (am.getRaw("dc.identifier") != null) {
          // this will remove the protocol "doi"" if it is there; probably don't need to check for !null 
          am.put(MetadataField.FIELD_DOI, am.getRaw("dc.identifier") );
        }
      }
      //If the citation_issue isn't there, get the issue from the prism.number
      //because the TDB version wil have leading 0
      if (am.get(MetadataField.FIELD_ISSUE) == null) {
        if (am.getRaw("prism.number") != null) {
          // probably don't need to check for !null - would just ignore? 
          am.put(MetadataField.FIELD_ISSUE, am.getRaw("prism.number") );
        }
      }
      emitter.emitMetadata(cu, am);
    }
  }
}