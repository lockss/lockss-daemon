/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.iop;

import java.io.*;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.MetadataField.Cardinality;
import org.lockss.extractor.MetadataField.Validator;
import org.lockss.plugin.*;


/**
 * One of the articles used to get the html source for this plugin is:
 * http://iopscience.iop.org/2043-6262/1/4/043003
 */
public class IOPScienceHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(IOPScienceHtmlMetadataExtractorFactory.class);

  public static MetadataField IOP_ACCESS_URL = new MetadataField(
      MetadataField.KEY_ACCESS_URL, Cardinality.Single,
      new Validator() {
        public String validate(ArticleMetadata am,MetadataField field,String val)
            throws MetadataException.ValidationException {
          // trim trailing '/' from urls like http://iopscience.iop.org/0264-9381/29/9/097002/article/
          if( (val != null) && !val.isEmpty() && (val.endsWith("/"))) {
            val = val.substring(0, val.length()-1);
          }
          return val;
        }
      });

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new IOPScienceHtmlMetadataExtractor();
  }

  public static class IOPScienceHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {

   
    private static MultiMap tagMap = new MultiValueMap();
    static {
      //<meta name="citation_doi" content="10.1088/2043-6262/1/4/043003" />
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      //  <meta name="citation_publication_date" content="2011-01-25" />
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      //<meta name="citation_title" content="Polymer materials with spatially..." />
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      //<meta name="citation_issn" content="2043-6262"/>
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      //<meta name="citation_volume" content="1" />
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      //<meta name="citation_issue" content="4"/>
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      //<meta name="citation_firstpage" content="043003"/>
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      //  <meta name="citation_author" content="Daisuke Fujiki"/>
      tagMap.put("citation_author",MetadataField.FIELD_AUTHOR);
      //<meta name="citation_journal_title" content="Advances in Natural Sciences:
      //Nanoscience and Nanotechnology" />
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      // <meta name="citation_publisher" content="IOP Publishing" />
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      // XXX this map is so that the metadata url is not always the access_url
      // <meta name="citation_fulltext_html_url" content="http://iopscience.iop.org/...
      tagMap.put("citation_fulltext_html_url", IOP_ACCESS_URL);
    }

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
        throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      String url = am.get(MetadataField.FIELD_ACCESS_URL);
      if (url != null && !url.isEmpty()) {
        CachedUrl val = cu.getArchivalUnit().makeCachedUrl(url);
        if (!val.hasContent()) {
          am.replace(MetadataField.FIELD_ACCESS_URL, cu.getUrl());
        }
      } else {
        am.replace(MetadataField.FIELD_ACCESS_URL, cu.getUrl());
      }
      return am;
    }
  }

}
