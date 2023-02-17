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

package org.lockss.plugin.clockss.frontiers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;



public class FrontiersXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(FrontiersXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper JatsPublishingHelper = null;
  private static SourceXmlSchemaHelper WorksheetHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new FrontiersJatsPublishingSourceXmlMetadataExtractor();
  }

  public class FrontiersJatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      String url = cu.getUrl();
      // TODO - we need to see the naming convention to do this
      if ((url != null) && url.contains("Ebooks")) {
        // Once you have it, just keep returning the same one. It won't change.
        if (WorksheetHelper == null) {
          WorksheetHelper = new FrontiersBooksWorksheetXmlSchemaHelper();
        }
        return WorksheetHelper;
      } else {      
      if (JatsPublishingHelper == null) {
        JatsPublishingHelper = new JatsPublishingSchemaHelper();
      }
      return JatsPublishingHelper;
      }
    }
    


    /* In this case, the filename is the same as the xml filename
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      List<String> returnList = new ArrayList<String>();
      String pdfBase;
      if (helper == WorksheetHelper) {
        //book layout
        String isbn_val = oneAM.getRaw(FrontiersBooksWorksheetXmlSchemaHelper.FEB_isbn);
        String cuBase = FilenameUtils.getFullPath(cu.getUrl());
        pdfBase = cuBase + isbn_val;
      } else { 
        // journal layout
        String url_string = cu.getUrl();
        pdfBase = url_string.substring(0,url_string.length() - 4);
      }
      log.debug3("pdfName is " + pdfBase + ".pdf (or .PDF)" );
      returnList.add(pdfBase + ".pdf");
      returnList.add(pdfBase + ".PDF");
      return returnList;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in Frontiers postCookProcess");
      if (schemaHelper == WorksheetHelper) {
        //books
        return;
      }
      //If we didn't get a valid date value, use the copyright year if it's there
      if (thisAM.get(MetadataField.FIELD_DATE) == null) {
        if (thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date) != null) {
          thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date));
        } else {// last chance
          thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_edate));
        }
      }
    }

  }
}
