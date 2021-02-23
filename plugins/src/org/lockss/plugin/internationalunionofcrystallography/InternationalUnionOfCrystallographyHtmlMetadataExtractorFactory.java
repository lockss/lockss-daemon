/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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