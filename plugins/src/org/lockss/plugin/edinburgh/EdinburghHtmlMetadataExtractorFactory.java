/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.edinburgh;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;


public class EdinburghHtmlMetadataExtractorFactory 
  extends BaseAtyponHtmlMetadataExtractorFactory {
  static Logger log = Logger.getLogger(EdinburghHtmlMetadataExtractorFactory.class);

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