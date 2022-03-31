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

package org.lockss.plugin.clockss.eastview;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;



public class EastviewXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(EastviewXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper EastviewHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new EastviewXmlMetadataExtractor();
  }

  public class EastviewXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (EastviewHelper == null) {
        EastviewHelper = new EastviewSchemaHelper();
      }
      return EastviewHelper;
    }


    /* 
     * a PDF file may or may not exist, but assume the XML is full text
     * when it does not
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      // filename is just the same a the XML filename but with .pdf 
      // instead of .xml
      String url_string = cu.getUrl();
      String pdfName = url_string.substring(0,url_string.length() - 3) + "pdf";
      log.debug3("pdfName is " + pdfName);
      List<String> returnList = new ArrayList<String>();
      returnList.add(pdfName);
      returnList.add(url_string); // xml file
      return returnList;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      String raw_title = thisAM.getRaw(EastviewSchemaHelper.ART_RAW_TITLE);
      log.debug3(String.format("Eastview metadata raw title parsed: %s", raw_title));

      Pattern pattern =  Pattern.compile("\\d\\d-\\d\\d-\\d\\d\\d\\d\\(([^)]+)-([^(]+)\\)\\s+(.*)");

      Matcher m = pattern.matcher(raw_title);

      String publisher_shortcut = null;
      String publisher_mapped = null;
      String volume = null;
      String title = null;

      if(m.matches()){
        publisher_shortcut = m.group(1).trim();
        publisher_mapped = EastViewPublisherNameMappingHelper.canonical.get(publisher_shortcut);
        volume = m.group(2);
        title = m.group(2);
      }

      log.debug3(String.format("Eastview metadata raw title parsed = %s | " +
                      "publisher_shortcut = %s | publisher_mapped = %s | volume = %s | title = %s",
              raw_title,
              publisher_shortcut,
              publisher_mapped,
              volume,
              title));

      if (publisher_mapped != null) {
        thisAM.put(MetadataField.FIELD_PUBLISHER, publisher_mapped);
      }  else {
        log.debug3(String.format("Eastview metadata raw title parsed = %s | " +
                        "publisher_shortcut = %s | Null publisher_mapped = %s | volume = %s | title = %s",
                raw_title,
                publisher_shortcut,
                publisher_mapped,
                volume,
                title));
      }
 
    }

  }
}
