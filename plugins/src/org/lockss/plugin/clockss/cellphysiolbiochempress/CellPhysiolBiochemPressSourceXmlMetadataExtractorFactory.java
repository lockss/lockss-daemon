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

package org.lockss.plugin.clockss.cellphysiolbiochempress;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;


public class CellPhysiolBiochemPressSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final String unmappedMetadata = "CellPhysiolBiochemPressUnmappedMetadata";

  private static final Logger log = Logger.getLogger(CellPhysiolBiochemPressSourceXmlMetadataExtractorFactory.class);
  static Logger upmappedlog = Logger.getLogger(unmappedMetadata);

  private static SourceXmlSchemaHelper  schemaHelper = null;
  private static final String TITLE_SEPARATOR = ":";

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new CellPhysiolBiochemPressSourceXmlMetadataExtractor();
  }

  public static class CellPhysiolBiochemPressSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
    // Once you have it, just keep returning the same one. It won't change.
      if ( schemaHelper != null) {
        return  schemaHelper;
      }
       schemaHelper = new CellPhysiolBiochemPressSourceXmlSchemaHelper();
      return  schemaHelper;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
    		CachedUrl cu, ArticleMetadata thisAM) {

    	log.debug3("setting publication type in postcook process");
        log.debug3("get cu url = " + cu.getUrl());
        StringBuilder titleVal = new StringBuilder();

        String page = thisAM.getRaw(CellPhysiolBiochemPressSourceXmlSchemaHelper.start_page);
        String page_alt = thisAM.getRaw(CellPhysiolBiochemPressSourceXmlSchemaHelper.start_page_alt);
        if (page != null && page.contains("-"))  {
            thisAM.put(MetadataField.FIELD_START_PAGE,page.substring(0, page.indexOf("-")));
            thisAM.put(MetadataField.FIELD_END_PAGE,page.substring(page.indexOf("-") + 1));

        } else if (page != null) {
            //in cuhk-released/2022_01/LPJ/i45a.zip!/i45a_cover.xml, the start page is not a range
            // so set it to '0'
            thisAM.put(MetadataField.FIELD_START_PAGE,"1");
        } else {
            if (page_alt != null && page_alt.contains("-")) {
                thisAM.put(MetadataField.FIELD_START_PAGE,page_alt.substring(0, page_alt.indexOf("-")));
                thisAM.put(MetadataField.FIELD_END_PAGE,page_alt.substring(page_alt.indexOf("-") + 1));

            } else if (page_alt != null) {
                //in cuhk-released/2022_01/LPJ/i45a.zip!/i45a_cover.xml, the start page is not a range
                // so set it to '0'
                thisAM.put(MetadataField.FIELD_START_PAGE,"1");
            } else {
                upmappedlog.debug2("missing data: startpage && startpage_alt, cu = " + cu.getUrl());
            }
        }

        if ( (thisAM.getRaw(CellPhysiolBiochemPressSourceXmlSchemaHelper.article_title) == null) &&
                (thisAM.getRaw(CellPhysiolBiochemPressSourceXmlSchemaHelper.article_title_alt) == null)) {
            upmappedlog.debug2("missing data: article_title, cu = " + cu.getUrl());
        }

        if ( (thisAM.getRaw(CellPhysiolBiochemPressSourceXmlSchemaHelper.journal_title) == null) &&
                (thisAM.getRaw(CellPhysiolBiochemPressSourceXmlSchemaHelper.journal_title_alt) == null)) {
            upmappedlog.debug2("missing data: journal_title, cu = " + cu.getUrl());
        }

    	thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE,MetadataField.PUBLICATION_TYPE_JOURNAL);
    	thisAM.put(MetadataField.FIELD_ARTICLE_TYPE,MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
    }
    
  }
}
