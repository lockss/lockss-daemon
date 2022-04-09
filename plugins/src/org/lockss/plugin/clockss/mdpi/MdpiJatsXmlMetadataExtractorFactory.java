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

package org.lockss.plugin.clockss.mdpi;

import java.util.ArrayList;
import java.util.List;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;

public class MdpiJatsXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(MdpiJatsXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper JatsPublishingHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new JatsPublishingSourceXmlMetadataExtractor();
  }

  public class JatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (JatsPublishingHelper == null) {
        JatsPublishingHelper = new JatsPublishingSchemaHelper();
      }
      return JatsPublishingHelper;
    }
    


    /* In this case, the filename is the same as the xml filename
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

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

      log.debug3("in MDPI postCookProcess");
      //If we didn't get a valid date value, use the copyright year if it's there
      if (thisAM.get(MetadataField.FIELD_DATE) == null) {
        if (thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date) != null) {
          thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date));
        } else {// last chance
          thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_edate));
        }
      }
    }

    @Override
    protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper,
                                   CachedUrl cu,
                                   ArticleMetadata am) {
      /*
       * Some articles were originally delivered with key metadata sufficiently
       * "off" to cause a starkly visible effect in Keepers/KBART reports, such
       * as years or volumes that are clearly out of sequence/range. Corrected
       * versions were subsequently delivered, but we still need to suppress the
       * incorrect ones. 
       */
      String doi = am.get(MetadataField.FIELD_DOI);
      if ("10.3390/rs61111444".equals(doi) && "61".equals(am.get(MetadataField.FIELD_VOLUME))) {
        return false;
      }
      if ("10.3390/polym10030328".equals(doi) && "2079-6439".equals(am.get(MetadataField.FIELD_EISSN))) {
        return false;
      }
      if ("10.3390/catal10070773".equals(doi) && "2411-5142".equals(am.get(MetadataField.FIELD_EISSN))) {
        return false;
      }
      if ("10.3390/nu12051459".equals(doi) && "2075-1729".equals(am.get(MetadataField.FIELD_EISSN))) {
        return false;
      }
      if ("10.3390/molecules25020281".equals(doi) && "1996-1944".equals(am.get(MetadataField.FIELD_EISSN))) {
        return false;
      }
      if ("10.3390/biochem1010004".equals(doi) && "2020".equals(am.get(MetadataField.FIELD_DATE))) {
        return false;
      }
      return super.preEmitCheck(schemaHelper, cu, am);
    }
    
  }
}
