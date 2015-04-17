/*
 * $Id$
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.knowledge;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


public class KnowledgeUnlatchedSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  static Logger log = Logger.getLogger(KnowledgeUnlatchedSourceXmlMetadataExtractorFactory.class);
  private static final String KNOWLEDGE_UNLATCHED_PROVIDER = "Knowledge Unlatched";
  
  private static SourceXmlSchemaHelper MARCHelper = null;
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new KnowledgeSourceXmlMetadataExtractor();
  }

  public class KnowledgeSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema() {
      // this version of this routine is abstract, but should not get called 
      // because we have the other implementation (with the CachedUrl argument)
      return null; // will cause a plugin exception to get thrown
    }

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (MARCHelper != null) {
        return MARCHelper;
      }
      MARCHelper = new MarcXmlSchemaHelper();
      return MARCHelper;
    }

    
    // TODO - if we get full text XML without a matching pdf we must still emit
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      // filename is just the same a the XML filename but with
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
      //If we didn't get a valid ISBN, use the filename
      if (thisAM.get(MetadataField.FIELD_ISBN) == null) {
        String url_string = cu.getUrl();
        String filenameValue = FilenameUtils.getBaseName(url_string);
        thisAM.put(MetadataField.FIELD_ISBN,  filenameValue);
      }
      if (thisAM.get(MetadataField.FIELD_PROVIDER)== null ) {
        // this plugin delivers content from Knowledge Unlatched 
        thisAM.put(MetadataField.FIELD_PROVIDER,  KNOWLEDGE_UNLATCHED_PROVIDER);
        log.debug3("Provider is now set to  " + thisAM.get(MetadataField.FIELD_PROVIDER));
      }
      log.debug3("in KnowledgeUnlatched postEmitProcess");
    }

  }
}
