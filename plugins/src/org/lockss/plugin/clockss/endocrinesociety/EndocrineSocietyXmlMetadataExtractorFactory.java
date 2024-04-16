/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.endocrinesociety;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

public class EndocrineSocietyXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory{
    private static final Logger log = Logger.getLogger(EndocrineSocietyXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper schemaHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
        String contentType)
            throws PluginException {
      return new EndocrineSocietyXmlMetadataExtractor();
    }
    
    public class EndocrineSocietyXmlMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
        // Once you have it, just keep returning the same one. It won't change.
            if (schemaHelper == null) {
                schemaHelper = new EndocrineSocietyXmlSchemaHelper();
                log.info("setting up schema helper");
            }
            return schemaHelper;
        }

        /*
         * There is a one-to-many relationship between the xml and pdf.
         */
        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {
            String cuBase = FilenameUtils.getFullPath(cu.getUrl());
            String pdfName = "";
            log.debug3("CU Base is: " + cuBase);

            String title = oneAM.getRaw(EndocrineSocietyXmlSchemaHelper.article_title);
            String date = oneAM.getRaw(EndocrineSocietyXmlSchemaHelper.date);
            String id = oneAM.getRaw(EndocrineSocietyXmlSchemaHelper.id);
            //check if pdf is in REs or CMEs directory
            if(title != null){
                if(title.contains("Reference Edition")){
                    log.debug3("reference edition found! ");
                    pdfName = cuBase + "REs/";
                } else {
                    pdfName = cuBase + "CMEs/";
                }
            }
            if(title != null){
                if(title.contains("ESAP")){
                    pdfName += "(1)%20ESAP/";
                }else if(title.contains("Endocrine Case Management")){
                    pdfName += "(2)%20MTP/";
                }else if(title.contains("Endocrine Board Review")){
                    pdfName += "(3)%20EBR/";
                }else{
                    pdfName += "(4)%20PedESAP/";
                }
            }
            pdfName += date + "/" + id;
            log.debug3("The pdf is: " + pdfName);
            List<String> returnList = new ArrayList<String>();
            returnList.add(pdfName);
            return returnList;
        }
    }
}
