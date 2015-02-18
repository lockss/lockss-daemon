/*
 * $Id$
 */

/*

Copyright (c) 2000-201 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.edinburgh;

import java.io.*;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;


public class EdinburghHtmlMetadataExtractorFactory 
  extends BaseAtyponHtmlMetadataExtractorFactory {
  static Logger log = Logger.getLogger("EdinburghHtmlMetadataExtractorFactory");

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target, String contentType)
      throws PluginException {
    return new EdinburghHtmlMetadataExtractor();
  }

  public static class EdinburghHtmlMetadataExtractor 
    extends BaseAtyponHtmlMetadataExtractor {
    
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      // extract but do some more processing before emitting
      ArticleMetadata am = 
          new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(getTagMap()); //parent set the tagMap
      
      // publisher name does not appear anywhere on the page in this form
      am.put(MetadataField.FIELD_PUBLISHER, "Edinburgh University Press");
      
      // Get the content
      BufferedReader bReader = new BufferedReader(cu.openForReading());
      try {
        // go through the cached URL content line by line
        for (String line = bReader.readLine(); 
             line != null; line = bReader.readLine()) {
          line = line.trim();
          
          if (line.matches(".*>Issue: Volume [0-9]+, Number [0-9]+<.*")) {
            int i = line.indexOf("Volume ");
            if (i >= 0) {
              i+= "Volume ".length();
              int j = line.indexOf(",", i);
              if (j > i) {
                String volume = line.substring(i,j);
                am.put(MetadataField.FIELD_VOLUME, volume);
                am.put(MetadataField.DC_FIELD_CITATION_VOLUME, volume);
                i = line.indexOf("Number ", j);
                if (i >= 0) {
                  i += "Number ".length();
                  j = line.indexOf("<", i);
                  if (j > i) {
                    String issue = line.substring(i,j);
                    am.put(MetadataField.FIELD_ISSUE, issue);
                    am.put(MetadataField.DC_FIELD_CITATION_ISSUE, issue);
                  }
                }
              }
            }
          } else if (line.matches(".*>E-ISSN:<.*")) {
            String eissn = line.substring(line.length()-9);
            am.put(MetadataField.FIELD_EISSN, eissn);
            am.put(MetadataField.DC_FIELD_IDENTIFIER_EISSN, eissn);
          } else if (line.matches(".*>ISSN:<.*")) {
            String issn = line.substring(line.length()-9);
            am.put(MetadataField.FIELD_ISSN, issn);
            am.put(MetadataField.DC_FIELD_IDENTIFIER_ISSN, issn);
          } else if (line.matches("Page [1-9]+-[0-9]+")) {
            String spage = 
              line.substring(line.lastIndexOf(' ')+1, line.lastIndexOf('-')); 
            am.put(MetadataField.FIELD_START_PAGE, spage);
            am.put(MetadataField.DC_FIELD_CITATION_SPAGE, spage);
              
          } else if (line.matches(".*<journal-title.*>.*</journal-title>.*")) {
            int i = line.indexOf('>', line.indexOf("<journal-title"));
            String value = line.substring(i+1, line.indexOf('<',i+1));
            am.put(MetadataField.FIELD_JOURNAL_TITLE, value);
            am.put(MetadataField.DC_FIELD_RELATION_ISPARTOF, value);
          }
        }
      } finally {
        IOUtil.safeClose(bReader);
      }

      emitter.emitMetadata(cu, am);
    }
  }
}