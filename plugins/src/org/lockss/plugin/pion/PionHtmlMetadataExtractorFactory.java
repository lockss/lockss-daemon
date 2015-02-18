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
	  log.debug("createFME was called");
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
    	log.debug("The MetadataExtractor attempted to extract metadata from cu: "+cu);
    	ArticleMetadata am = 
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);

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
    	  log.debug(line + " : no " + issnFlag);
		return;
      }
      
      issnBegin += issnFlag.length();
      String issn = line.substring(issnBegin, issnBegin + 9);
      if (issn.length() < 9)
      {
    	log.debug(line + " : too short");
		return;
      }		
      
      ret.put(MetadataField.FIELD_EISSN, issn);
    }
  }
}
