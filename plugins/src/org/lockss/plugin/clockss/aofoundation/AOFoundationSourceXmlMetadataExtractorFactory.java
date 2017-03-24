/*
 * $Id$
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
