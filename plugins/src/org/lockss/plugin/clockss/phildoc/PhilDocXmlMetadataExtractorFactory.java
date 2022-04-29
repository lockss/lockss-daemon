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

package org.lockss.plugin.clockss.phildoc;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


public class PhilDocXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(PhilDocXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper PhilDocHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new PhilDocXmlMetadataExtractor();
  }

  public class PhilDocXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (PhilDocHelper == null) {
        PhilDocHelper = new PhilDocSchemaHelper();
      }
      return PhilDocHelper;
    }


    
    // The sample used pdf subdir but the later deposit had the pdf in the same
    // directory as the XML file - try both
    protected List<String> getFilenamesAssociatedWithRecord(PhilDocSchemaHelper helper, 
        CachedUrl cu,
        ArticleMetadata oneAM) {
      // Note the altered method signature: PhilDocSchemaHelper instead of SourceXmlSchemaHelper
      String imuse_id = oneAM.getRaw(helper.art_imuse_id);
      String pdfname = oneAM.getRaw(helper.art_pdfname);
      String xmlname = oneAM.getRaw(helper.art_xmlname);
      
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      ArrayList<String> returnList = new ArrayList<String>();
//      log.debug3("looking for filename of: " + cuBase + "pdf/" + filenameValue + ".pdf");
      returnList.add(cuBase + imuse_id + ".pdf");
      returnList.add(cuBase + "pdf/" + imuse_id + ".pdf");
      returnList.add(cuBase + pdfname + ".pdf");
      returnList.add(cuBase + "pdf/" + pdfname + ".pdf");
      return returnList;
    }    
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
    
      thisAM.put(MetadataField.FIELD_PUBLISHER,"Philosophy Documentation Center");
      // Now build up a full title if that is necessary
      String subT = thisAM.getRaw(PhilDocSchemaHelper.art_subtitle);  
      if (subT != null) {
        StringBuilder title_br = new StringBuilder(thisAM.get(MetadataField.FIELD_ARTICLE_TITLE));
        title_br.append(": ");
        title_br.append(subT);
        thisAM.replace(MetadataField.FIELD_ARTICLE_TITLE,  title_br.toString());
      }
      log.debug3("in PhilDoc postCookProcess");
    }

  }
}

