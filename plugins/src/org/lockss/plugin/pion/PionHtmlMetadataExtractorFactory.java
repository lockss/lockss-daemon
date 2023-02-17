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

package org.lockss.plugin.pion;

import java.io.*;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * view-source:http://i-perception.perceptionweb.com/journal/I/volume/1/article/i0402
 *
 */
public class PionHtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(PionHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new PionHtmlMetadataExtractor();
  }

  public static class PionHtmlMetadataExtractor 
    implements FileMetadataExtractor {

    // Map Pion-specific HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_doi", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_date", MetadataField.DC_FIELD_DATE);
      tagMap.put("citation_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_volume", MetadataField.DC_FIELD_CITATION_VOLUME);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.DC_FIELD_CITATION_ISSUE);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.DC_FIELD_CITATION_SPAGE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_authors",
              new MetadataField(MetadataField.DC_FIELD_CONTRIBUTOR,
                                MetadataField.splitAt(";")));
      tagMap.put("citation_authors",
                 new MetadataField(MetadataField.FIELD_AUTHOR,
                                   MetadataField.splitAt(";")));
      tagMap.put("citation_title", MetadataField.DC_FIELD_TITLE);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      
      tagMap.put("citation_lastpage", MetadataField.DC_FIELD_CITATION_EPAGE);
      tagMap.put("citation_abstract_html_url", MetadataField.FIELD_ACCESS_URL);
      // Pion returns "Pion Ltd" in metadata, our tdb returns "Pion"
      tagMap.put("citation_publisher", MetadataField.DC_FIELD_PUBLISHER);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
    }
    
    /**
     * Use SimpleHtmlMetaTagMetadataExtractor to extract raw metadata, map
     * to cooked fields, then extract the ISSN by reading the file.
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
      throws IOException {
      log.debug3("The MetadataExtractor attempted to extract metadata from cu: "+cu);
      ArticleMetadata am = 
          new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      String curl = cu.getUrl();
      if (am != null) {
        String url = am.get(MetadataField.FIELD_ACCESS_URL);
        if (url != null && !url.isEmpty()) {
          CachedUrl val = cu.getArchivalUnit().makeCachedUrl(url);
          if (!val.hasContent()) {
            am.replace(MetadataField.FIELD_ACCESS_URL, curl);
          }
        }
        am.cook(tagMap);
        if (!am.hasValidValue(MetadataField.FIELD_DOI)) {
          // fill in DOI from accessURL
          // http://www.envplan.com/abstract.cgi?id=a42117
          // -> doi=10.1068/a42117
          if (curl != null) {
            int i = curl.lastIndexOf("id=");
            if (i > 0) {
              String doi = "10.1068/" + curl.substring(i+3);
              am.put(MetadataField.FIELD_DOI, doi);
            } 
            else {
              log.debug3("Using alternate match for DOI :" + curl);
              i = curl.lastIndexOf('/');
              if (i > 0) {
                String doi = "10.1068/" + curl.substring(i+1).replace(".pdf", "");
                am.put(MetadataField.FIELD_DOI, doi);
              }
            }
          }
        }
      }

      // XXX Should this be conditional on
      //     !am.hasValidValue(MetadataField.FIELD_ISSN) ?

      // The ISSN is not in a meta tag but we can find it in the source
      // code. Here we need to some string manipulation and use some REGEX
      // to find the right index where the ISSN number is located.
      BufferedReader bReader = new BufferedReader(cu.openForReading());
      try {
        for (String line = bReader.readLine();
             line != null; line = bReader.readLine()) {
          line = line.trim();
          if (StringUtil.startsWithIgnoreCase(line, "<p class=\"ref\"><b>ISSN:")) 
          {
        	  log.debug2("Line: " + line);
        	  addEISSN(line, am);
          }
        }
      } finally {
        IOUtil.safeClose(bReader);
      }
      emitter.emitMetadata(cu, am);
    }
		
    /**
     * Extract the ISSN from a line of HTML source code.
     * @param line The HTML source code that contains the ISSN and should have the following form:
     * <p class="ref"><b>ISSN:</b> 2041-6695 (electronic only)</p> 
     * @param ret The ArticleMetadat object used to put the ISSN in extracted metadata
     */
    protected void addEISSN(String line, ArticleMetadata ret) {
      String issnFlag = "ISSN:</b> ";
      int issnBegin = StringUtil.indexOfIgnoreCase(line, issnFlag);
     
      if (issnBegin <= 0) 
      {
    	  log.debug3(line + " : no " + issnFlag);
		return;
      }
      
      issnBegin += issnFlag.length();
      String issn = line.substring(issnBegin, issnBegin + 9);
      if (issn.length() < 9)
      {
    	log.debug3(line + " : too short");
		return;
      }		
      
      ret.put(MetadataField.FIELD_EISSN, issn);
    }
  }
}
