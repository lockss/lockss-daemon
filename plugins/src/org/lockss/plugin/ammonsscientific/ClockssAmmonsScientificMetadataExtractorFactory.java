/*
 * $Id: ClockssAmmonsScientificMetadataExtractorFactory.java,v 1.1 2012-03-16 00:40:00 thib_gc Exp $
 */

/*

 Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ammonsscientific;

import java.io.*;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * view-source:http://www.amsciepub.com/doi/abs/10.2466/07.17.21.PMS.113.6.703-714
 *
 */
public class ClockssAmmonsScientificMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("ClockssAmmonsScientificMetadataExtractorFactory");
  private static final int DOI_LEN = 7;

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new ClockssAmmonsMetadataExtractor();
  }
  
  public static class ClockssAmmonsMetadataExtractor 
    implements FileMetadataExtractor {

    // Map Ammons Scientific-specific HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static { 
      tagMap.put("dc.Title", MetadataField.DC_FIELD_TITLE);
      tagMap.put("dc.Title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("dc.Creator",
              new MetadataField(MetadataField.DC_FIELD_CONTRIBUTOR,
                                MetadataField.splitAt(";")));
      tagMap.put("dc.Creator",
                 new MetadataField(MetadataField.FIELD_AUTHOR,
                                   MetadataField.splitAt(";")));
      tagMap.put("dc.Description", MetadataField.DC_FIELD_DESCRIPTION);
      tagMap.put("dc.Publisher", MetadataField.DC_FIELD_PUBLISHER);
      tagMap.put("dc.Publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("dc.Date", MetadataField.DC_FIELD_DATE);
      tagMap.put("dc.Date", MetadataField.FIELD_DATE);
      tagMap.put("dc.Format", MetadataField.DC_FIELD_FORMAT);
      tagMap.put("dc.Type", MetadataField.DC_FIELD_TYPE);
      tagMap.put("dc.Language", MetadataField.DC_FIELD_LANGUAGE);
      tagMap.put("dc.Source", MetadataField.DC_FIELD_SOURCE);
      tagMap.put("dc.Rights", MetadataField.DC_FIELD_RIGHTS);
      tagMap.put("dc.Coverage", MetadataField.DC_FIELD_COVERAGE);
    }
    
    /**
     * Use SimpleHtmlMetaTagMetadataExtractor to extract raw metadata, map
     * to cooked fields, then extract extra tags by reading the file.
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
      throws IOException {
    	log.debug("The MetadataExtractor attempted to extract metadata from cu: "+cu);
      ArticleMetadata am = 
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);

      //Extracts author, issue number, and doi information not properly encoded in tags
      BufferedReader bReader = new BufferedReader(cu.openForReading());
      try {
        for (String line = bReader.readLine();
             line != null; line = bReader.readLine()) {
          line = line.trim();
          
          if (StringUtil.startsWithIgnoreCase(line, "<link rel=\"schema.DC\" href=\"http://purl.org/DC/elements/1.0/\"></link>")) 
        	  addExtraTags(line, am);
        }
      } finally {
        IOUtil.safeClose(bReader);
      }
      emitter.emitMetadata(cu, am);
    }
		
    /**
     * Extract issue number, multiple authors, and doi from a line of HTML source code.
     * @param line The HTML source code that contains these tags 
     * @param ret The ArticleMetadata object used to put the tags in extracted metadata
     */
    
    protected void addExtraTags(String line, ArticleMetadata ret) {
      String doiFlag = "<meta name=\"dc.Identifier\" scheme=\"doi\" content=\"";
      int doiBegin = StringUtil.indexOfIgnoreCase(line, doiFlag);
      
      if (doiBegin <= 0) 
    	  return;
      
      doiBegin += doiFlag.length();
      String doi = line.substring(doiBegin, line.indexOf('"',doiBegin+1));
      if (doi.length() < DOI_LEN)
		return;
      
      ret.put(MetadataField.FIELD_DOI, doi);
    }
  }
}