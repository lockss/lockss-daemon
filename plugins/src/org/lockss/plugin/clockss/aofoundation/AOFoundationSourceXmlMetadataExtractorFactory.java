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

package org.lockss.plugin.clockss.aofoundation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.CrossRefSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;


public class AOFoundationSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(AOFoundationSourceXmlMetadataExtractorFactory.class);
  
  private static final String ECM_PUBLISHER = "AO Foundation";
  private static final String ECM_TITLE = "European Cells and Materials";
  private static SourceXmlSchemaHelper CrossRefHelper = null;
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new AOFoundationSourceXmlMetadataExtractor();
  }

  public class AOFoundationSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (CrossRefHelper != null) {
        return CrossRefHelper;
      }
      CrossRefHelper = new CrossRefSchemaHelper();
      return CrossRefHelper;
    }

    
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      // filename is just the same a the resource filename with this apth
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      String resource = oneAM.getRaw(CrossRefSchemaHelper.art_resource);
      String pdfName = cuBase  + FilenameUtils.getName(resource);
      log.debug3("looking for pdfName of " + pdfName);
      List<String> returnList = new ArrayList<String>();
      returnList.add(pdfName);
      return returnList;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
      log.debug("in AOFoundation postcook");
      // In the AOFoundation metadata, the registrant is incorrectly set to WEB-FORM
      String pname = thisAM.get(MetadataField.FIELD_PUBLISHER);
      // they cannot seem to avoid spelling errors in the publication name. I'm going to manually set it 
      // after doing a basic check.  The variants seen so far are:
      // European Cells and Material,European Cells and Materials,European Cells and Matherials, European Cells a¨nd Materials
      // European cells amd Material,European cells amd Materials, Europen Cells and Materials,etc
      String jname = thisAM.get(MetadataField.FIELD_PUBLICATION_TITLE);
      if (jname == null) {
        thisAM.put(MetadataField.FIELD_PUBLICATION_TITLE, ECM_TITLE);
      }
      else if (jname.startsWith("Euro")) {
        thisAM.replace(MetadataField.FIELD_PUBLICATION_TITLE, ECM_TITLE);
      }
      if ("WEB-FORM".equals(pname)) {
        // for now this is the only journal handled by this plugin
        // if ("European Cells and Materials".equals(jname)) {
          thisAM.replace(MetadataField.FIELD_PUBLISHER,ECM_PUBLISHER);
      }
    }

  }
}
