/*
 * $Id$
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

package org.lockss.plugin.clockss.codeocean;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/**
 * We don't yet have access to a yaml library in the classic daemon
 * for the purposes of getting this plugin to go through now
 * do a manual line-reader that knows the limited set of things to look for
 * and the order they'll come in:
 * preservation.yml:
   metadata_version: 1
   capsule:
     doi: 10.24433/CO.4aaa25ae-2fb9-49fe-8379-7deb6bfb80e9
     publish_date: "2018-11-09"
   article:
     doi: 10.1038/d41586-018-07196-1
     publish_date: "2018-10-01"
     journal: Nature
 *
 * Loop over lines and make each line with a ^text: to be a context of text
 * and any line thereafter with leading spaces that has a key: value 
 * to get assigned as raw metadata of:
 * context_key value
 * eg
 * capsule_doi = 10.24433/CO.4aaa25ae-2fb9-49fe-8379-7deb6bfb80e9
 * or
 * article_publish_date = 2018-10-01
 *
 * TODO: ADD IN TRUE YAML PARSING ONCE AVAILABLE
 *
 */
public class AdHocYamlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(AdHocYamlMetadataExtractorFactory.class);
  
  private static final Pattern CONTEXT_PATTERN = Pattern.compile("^([a-z_]+):\\s*",Pattern.CASE_INSENSITIVE);
  // indented value - group1 = key, group 3 = value
  private static final Pattern VALUE_PATTERN = Pattern.compile("\\s+([a-z_]+):\\s*(\")?([^\"]+)(\")?",Pattern.CASE_INSENSITIVE);
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new AdHocYamlMetadataExtractor();
  }

  public static class AdHocYamlMetadataExtractor 
    implements FileMetadataExtractor {

    // Map code ocean items to cooked metadata fields
    private static MultiMap cookMap = new MultiValueMap();
    static {
      cookMap.put("capsule_doi", MetadataField.FIELD_DOI);
      cookMap.put("capsule_publish_date", MetadataField.FIELD_DATE);
    }

    /**
     * Read file line by line and look for regex for the "capsule:" related items which follow
     * and then the "article:" related items.
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
      throws IOException {
    	
      String context = null; 
      ArticleMetadata am = new ArticleMetadata();

      BufferedReader bReader = new BufferedReader(cu.openForReading());
      try {
        for (String line = bReader.readLine();
                line != null; line = bReader.readLine()) {
          //line = line.trim(); DO NOT TRIM - leading spaces matter in yaml
          Matcher contextMat = CONTEXT_PATTERN.matcher(line);
          if (contextMat.matches()) {
        	  // set a new context
        	  context = contextMat.group(1);
          } else if (context != null) {
        	  Matcher keyValMat = VALUE_PATTERN.matcher(line);
        	  if (keyValMat.matches()) {
        		  String key = context + "_" + keyValMat.group(1);
        		  String value = keyValMat.group(3);
        		  am.putRaw(key, value);
        	  }
           } 
       	  //otherwise keep going until get context, value or eof
        }
      } finally {
        IOUtil.safeClose(bReader);
      }
      am.cook(cookMap);
      emitter.emitMetadata(cu, am);
    }

  }
}
