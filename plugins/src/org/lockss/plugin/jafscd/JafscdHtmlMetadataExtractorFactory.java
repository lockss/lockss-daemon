/*
 * $Id: MaffeyHtmlMetadataExtractorFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
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

package org.lockss.plugin.jafscd;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class JafscdHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(JafscdHtmlCrawlFilterFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new MaffeyHtmlMetadataExtractor();
  }

  public static class MaffeyHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {

    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("author",
              new MetadataField(MetadataField.FIELD_AUTHOR,
                                MetadataField.splitAt(";")));
      tagMap.put("title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("keywords", MetadataField.FIELD_KEYWORDS);
      tagMap.put("doi", MetadataField.FIELD_DOI);
    }

    protected static final Pattern DOI_PAT = 
            Pattern.compile(".+http://dx.doi.org/([^\\s]+),?\\s.+");
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
	throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      if (cu == null) {
		throw new IllegalArgumentException("extract(null)");
      }									
      BufferedReader bReader = new BufferedReader(cu.openForReading());			
      try {		
		
		// go through the cached URL content line by line
		for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {										
			line = line.trim();										
			
			// title is of form: <title>MyJournalTitle articles</title>
			if(line.contains("http://dx.doi.org/")){
				log.debug("found DOI line:" + line);
				Matcher m = DOI_PAT.matcher(line);
				if(m.matches()){
					am.putRaw("doi", m.group(1));
					break;
				}
			}
		}
	} finally {
		IOUtil.safeClose(bReader);
	}
    am.cook(tagMap);
    return am;
    }
  }

}
