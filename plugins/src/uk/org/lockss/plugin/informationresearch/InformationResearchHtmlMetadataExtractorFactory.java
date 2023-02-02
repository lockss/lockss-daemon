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

package uk.org.lockss.plugin.informationresearch;

import java.io.*;
import java.util.List;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringEscapeUtils;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class InformationResearchHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(InformationResearchHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new InformationResearchHtmlMetadataExtractor();
  }
  
  public static class InformationResearchHtmlMetadataExtractor 
    implements FileMetadataExtractor {

    // Map Taylor & Francis DublinCore HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("dc.date.available", MetadataField.FIELD_DATE);
      tagMap.put("dc.date.available", MetadataField.DC_FIELD_DATE);
      tagMap.put("dc.title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("dc.title", MetadataField.DC_FIELD_TITLE);
      tagMap.put("dc.creator", MetadataField.FIELD_AUTHOR);
      tagMap.put("dc.creator", MetadataField.DC_FIELD_CREATOR);
      tagMap.put("dc.creator", new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(",")));
      tagMap.put("dc.identifier", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("dc.subject", MetadataField.DC_FIELD_SUBJECT);
      tagMap.put("dc.description", MetadataField.DC_FIELD_DESCRIPTION);
      tagMap.put("dc.publisher", MetadataField.DC_FIELD_PUBLISHER);
      tagMap.put("dc.publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("dc.type", MetadataField.DC_FIELD_TYPE);
      tagMap.put("dc.format", MetadataField.DC_FIELD_FORMAT);
      tagMap.put("dc.Source", MetadataField.DC_FIELD_SOURCE);
      tagMap.put("dc.language", MetadataField.DC_FIELD_LANGUAGE);
      tagMap.put("dc.coverage.placename", MetadataField.DC_FIELD_COVERAGE);
      tagMap.put("dc.subject.keywords", new MetadataField(MetadataField.FIELD_KEYWORDS, MetadataField.splitAt(",")));
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am = 
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);
      
      // Strip the extra whitespace found in the HTML around and within the "dc.Creator" and "dc.Publisher" fields
      TrimWhitespace(am, MetadataField.DC_FIELD_CREATOR);
      TrimWhitespace(am, MetadataField.FIELD_AUTHOR);
      TrimWhitespace(am, MetadataField.DC_FIELD_PUBLISHER);
      TrimWhitespace(am, MetadataField.FIELD_PUBLISHER);
      
      // Parse the dc.Identifier fields with scheme values "publisher-id", "doi", and "coden".
      List<String> cookedIdentifierList = am.getList(MetadataField.DC_FIELD_IDENTIFIER);
      List<String> rawIdentifierList = am.getRawList("dc.Identifier");
      
      String journalTitle = "";
      String volume = "";
      String issue = "";
      String spage = "";
      String epage = "";
      String doi = "";
      
      for (int j = 0; j < cookedIdentifierList.size(); j++) {
    	  
    	  // If our dc.Identifier field has a comma in it, its content is a comma-delimited list of
    	  // the journal title, volume, issue, and page range associated with the article.
    	  // The journal title itself may contain commas, so the list is parsed backwards and all content
    	  // before ", Vol." is assumed to be part of the journal title.
    	  if (cookedIdentifierList.get(j).contains(", ")) {
    		  String content = cookedIdentifierList.get(j);
    		  String[] biblioInfo = content.split(", ");
    		  
    		  for (int k = biblioInfo.length-1; k >= 0; k--) {
    			  if (biblioInfo[k].startsWith("pp. ")) {
    				  spage = biblioInfo[k].substring("pp. ".length(), biblioInfo[k].indexOf('-'));
    				  epage = biblioInfo[k].substring(biblioInfo[k].indexOf('-'), biblioInfo[k].length());
    			  }
    			  else if (biblioInfo[k].startsWith("No. ")) {
    				 issue = biblioInfo[k].substring("No. ".length(), biblioInfo[k].length());
    			  }
    			  else if (biblioInfo[k].startsWith("Vol. ")) {
     				 volume = biblioInfo[k].substring("Vol. ".length(), biblioInfo[k].length());
    			  }
    			  else if (!volume.isEmpty()) {
    				  journalTitle = biblioInfo[k].concat(journalTitle);
    				  
    				  // If we're not at the beginning of the comma-separated list
    				  // (i.e. the journal title itself contains commas),
    				  // reinsert the comma that we lost in content.split(", ").
    				  if (k != 0) journalTitle = ", ".concat(journalTitle);
    			  }
    		  }
    		  
    		  // org.apache.commons.lang.StringEscapeUtils contains a method for unescaping HTML codes
    		  // (like &amp;) that may appear in the journal title
    		  journalTitle = StringEscapeUtils.unescapeHtml(journalTitle);
    		  
    		  am.put(MetadataField.FIELD_JOURNAL_TITLE, journalTitle);
    		  am.put(MetadataField.FIELD_VOLUME, volume);
              am.put(MetadataField.FIELD_ISSUE, issue);
              am.put(MetadataField.FIELD_START_PAGE, spage);
    	  }
    	  else if (MetadataUtil.isDOI(cookedIdentifierList.get(j))) {
    		  doi = cookedIdentifierList.get(j);
    		  am.put(MetadataField.FIELD_DOI, doi);
    	  }
    	  
    	  // vol. 16  no. 1, March, 2011
    	  // Information research //
      }
      emitter.emitMetadata(cu, am);
    }
    
    private void TrimWhitespace(ArticleMetadata am, MetadataField md) {
    	List<String> list = am.getList(md);
    	for (int i = 0; i < list.size(); i++) {
      	  String curEntry = list.get(i);
          curEntry = curEntry.trim();
      	  curEntry = curEntry.replace("   ", " ");
      	  list.set(i, curEntry);
    	}
    }
  }
}