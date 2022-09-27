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

package org.lockss.plugin.clockss.nzma;

import java.util.ArrayList;
import java.util.List;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.PubMedSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


/*
 * If the xml is at NZMJv130i1452.xml
 * then the pdf is at NZMJv130i1452.pdf
 */

public class PasifikaPubMedXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(PasifikaPubMedXmlMetadataExtractorFactory.class);
  private static final String Preferred_PUBLISHER = "Pasifika Medical Association Group";

  private static SourceXmlSchemaHelper PubMedHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new NzmaPubMedXmlMetadataExtractor();
  }

  public class NzmaPubMedXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (PubMedHelper == null) {
        PubMedHelper = new PubMedSchemaHelper();
      }
      return PubMedHelper;
    }


    /* 
     * filename is the same as the xml, just change the suffix 
     * For NZMA the PDF is a pdf of the entire issue so every article in the file will point
     * to the same PDF. 
     * One access.url for multiple articles
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
      return returnList;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

        String pname = thisAM.get(MetadataField.FIELD_PUBLISHER);
        if (!(Preferred_PUBLISHER.equals(pname))) {
          thisAM.replace(MetadataField.FIELD_PUBLISHER,Preferred_PUBLISHER);
        }

        // The following code change is need to create custom uniq access.url for reporting purpose
        String pageRange = "0";

        if (thisAM.get(MetadataField.FIELD_START_PAGE) != null) {
          pageRange = thisAM.get(MetadataField.FIELD_START_PAGE);
        } else {
           pageRange = "0";
        }

        pageRange = pageRange + "_";

        if (thisAM.get(MetadataField.FIELD_END_PAGE) != null) {
          pageRange = pageRange + thisAM.get(MetadataField.FIELD_END_PAGE);
        } else {
          pageRange = "9999";
        }


        String customAccessUrl = thisAM.get(MetadataField.FIELD_ACCESS_URL) + "?unique_record_id=" + pageRange;
        log.debug3("customAccessUrl  = " + customAccessUrl );

        thisAM.replace(MetadataField.FIELD_ACCESS_URL, customAccessUrl);
    }

  }
}
