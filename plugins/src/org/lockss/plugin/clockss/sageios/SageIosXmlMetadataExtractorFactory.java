/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.sageios;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;

public class SageIosXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory{
    static Logger log = Logger.getLogger(SageIosXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper SageIosBitsSchemaHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
        String contentType)
            throws PluginException {
        return new SageIosXmlMetadataExtractor();
    }

    public class SageIosXmlMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            if (SageIosBitsSchemaHelper == null) {
                SageIosBitsSchemaHelper = new SageIosBitsSchemaHelper();
            }
            return SageIosBitsSchemaHelper;
        }

        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
            ArticleMetadata oneAM) {
            String url_string = cu.getUrl();
            List<String> returnList = new ArrayList<String>();
            String pdfName;
            pdfName = url_string.substring(0,url_string.length() - 3) + "pdf";
            log.debug3("pdfName is " + pdfName);
            returnList.add(pdfName);
            return returnList;        
        }

        @Override
        protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper,
                                    CachedUrl cu, ArticleMetadata thisAM) {

            String cuBase = FilenameUtils.getFullPath(cu.getUrl());

            List<String> filesToCheck;

            // If no files get returned in the list, nothing to check
            if ((filesToCheck = getFilenamesAssociatedWithRecord(schemaHelper, cu,thisAM)) == null) {
                return true;
            }
            ArchivalUnit B_au = cu.getArchivalUnit();
            CachedUrl fileCu;
            for (int i=0; i < filesToCheck.size(); i++){
                //Find eisbn to use in generated pdf url, Unfortunately, a couple of the frontmatter metadata doesn't contain the eisbn, 
                //so just hardcode them. 
                String eisbn = thisAM.getRaw(schemaHelper.getFilenameXPathKey());
                if(eisbn != null){
                    eisbn = eisbn.replaceAll("-","");
                }else if(filesToCheck.get(i).contains("FAIA131-fm-i")){
                    eisbn = "9781586035600";
                }else if(filesToCheck.get(i).contains("FAIA146-fm-i")){
                    eisbn = "9781586036638";
                }
                String currentUrl = filesToCheck.get(i);
                int indexOfSecondToLastSlash = currentUrl.substring(0, currentUrl.lastIndexOf("/")).lastIndexOf("/");
                String fulltextPdf = currentUrl.substring(0,indexOfSecondToLastSlash)+"/fulltext/"+eisbn+".pdf";
                log.debug3("Generated pdf url is " + fulltextPdf);
                fileCu = B_au.makeCachedUrl(currentUrl);
                if(fileCu != null && (fileCu.hasContent())) {
                    //If fulltext xml exists, use that. 
                    if(fileCu.toString().contains("fulltext")){
                        thisAM.put(MetadataField.FIELD_ACCESS_URL, fileCu.getUrl());
                        log.debug3("Check for existence of " + filesToCheck.get(i));
                        return true;
                    //The following five articles don't have
                    //fullltext metadata so we must use the frontmatter metadata. 
                    }else if(fileCu.toString().contains("FAIA131-fm-i")){
                        thisAM.put(MetadataField.FIELD_ACCESS_URL, B_au.makeCachedUrl(fulltextPdf).getUrl());
                        return true;
                    }else if(fileCu.toString().contains("FAIA146-fm-i")){
                        thisAM.put(MetadataField.FIELD_ACCESS_URL, B_au.makeCachedUrl(fulltextPdf).getUrl());
                        return true;
                    }else if(fileCu.toString().contains("NICSP40-fm-i")){
                        thisAM.put(MetadataField.FIELD_ACCESS_URL, B_au.makeCachedUrl(fulltextPdf).getUrl());
                        return true;
                    }else if(fileCu.toString().contains("NICSP45-fm-i")){
                        thisAM.put(MetadataField.FIELD_ACCESS_URL, B_au.makeCachedUrl(fulltextPdf).getUrl());
                        return true;
                    }else if(fileCu.toString().contains("DAI-349-DAI200003-fm")){
                        thisAM.put(MetadataField.FIELD_ACCESS_URL, B_au.makeCachedUrl(fulltextPdf).getUrl());
                        return true;
                    }else{
                    //Any other frontmatter metadata is superfluous, we don't want to emit extra metadata. 
                        log.debug3("DO NOT EMIT. This is extraneous frontmatter metadata.");
                        return false;
                    }
                }
                else {
                    log.debug3("Check for failed existence of " + filesToCheck.get(i));
                }
            }
            log.debug3("No file exists associated with this record");
            return false; //No files found that match this record
        }
    }
}
