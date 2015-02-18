/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.georgthiemeverlag;

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/**
 * One of the articles used to get the html source for this plugin is:
 * https://www.thieme-connect.de/ejournals/abstract/10.1055/s-0029-1214947
 */
public class GeorgThiemeVerlagHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(GeorgThiemeVerlagHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
      throws PluginException {
    return new GeorgThiemeVerlagHtmlMetadataExtractor();
  }
  
  public static class GeorgThiemeVerlagHtmlMetadataExtractor 
    implements FileMetadataExtractor {
   
    private static MultiMap tagMap = new MultiValueMap();
    static {
      //<meta name="citation_doi" content="10.1055/s-0029-1214474"/>
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      //<meta name="citation_publication_date" content="2009/04/27"/>
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      //<meta name="citation_title" content="Medikamentöse Systemtherapien der Psoriasis"/>
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      //<meta name="citation_issn" content="0340-2541"/>
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      //<meta name="citation_volume" content="36"/>
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      //<meta name="citation_issue" content="04"/>
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      //<meta name="citation_firstpage" content="142"/>
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      //<meta name="citation_author" content="R. Mössner"/>
      tagMap.put("citation_author",MetadataField.FIELD_AUTHOR);
      //<meta name="citation_journal_title" content="Aktuelle Dermatologie" />
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      //<meta name="citation_publisher" content="..."/> 
      // FIELD_PUBLISHER value will be replaced below (PD-440)
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      //<meta name="citation_language" content="de" />
      tagMap.put("citation_language", MetadataField.FIELD_LANGUAGE);
    }
    
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am = new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);
      // PD-440 hardcode publisher value
      am.replace(MetadataField.FIELD_PUBLISHER, "Georg Thieme Verlag KG");
      emitter.emitMetadata(cu, am);
    }
    
  }
  
}
