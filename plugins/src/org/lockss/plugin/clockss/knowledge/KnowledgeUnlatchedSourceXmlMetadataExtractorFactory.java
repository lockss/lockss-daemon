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
  private static final Logger log = Logger.getLogger(KnowledgeUnlatchedSourceXmlMetadataExtractorFactory.class);
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
