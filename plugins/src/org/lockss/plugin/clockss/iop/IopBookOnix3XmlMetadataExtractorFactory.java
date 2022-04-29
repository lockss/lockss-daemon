/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.clockss.iop;

import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.Onix3BooksSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

public class IopBookOnix3XmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(IopBookOnix3XmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper Onix3Helper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new IopBookOnix3XmlMetadataExtractor();
    }

    public class IopBookOnix3XmlMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {

            log.debug3("Fei: IopBook Onix3BooksSchemaHelper");


            if (Onix3Helper == null) {
                Onix3Helper = new Onix3BooksSchemaHelper();
            }
            return Onix3Helper;
        }

        /*
         * (non-Javadoc)
         * WARC XML files are a little non-standard in that they store the actual access.url
         * location in the "self-uri" field for Jats and the proprietary ID field
         * for ONIX
         * set the access_url depending on the schema
         * set the publisher as well. It may get replaced by the TDB value
         */
        @Override
        protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {

            log.debug3("Fei: in IopBook postCookProcess");

            String access = thisAM.getRaw(Onix3BooksSchemaHelper.ONIX_idtype_proprietary);
            if (access != null) {
                thisAM.replace(MetadataField.FIELD_ACCESS_URL, access);
            }
            if (thisAM.get(MetadataField.FIELD_DATE)== null) {
                String copydate = thisAM.getRaw(Onix3BooksSchemaHelper.ONIX_copy_date);
                if (copydate != null) {
                    thisAM.put(MetadataField.FIELD_DATE,copydate);
                }
            }
        }

        /* In this case, use the RecordReference + .pdf for the matching file */
        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                                ArticleMetadata oneAM) {

            log.debug3("Fei: in IopBook getFilenamesAssociatedWithRecord");

            String filenameValue = oneAM.getRaw(Onix3BooksSchemaHelper.ONIX_RR);
            String cuBase = FilenameUtils.getFullPath(cu.getUrl());

            if (filenameValue != null) {
                log.debug3("Fei: PDF file path is : " + cuBase + filenameValue);
            } else {
                log.debug3("Fei: PDF file path not found : " + cuBase);
            }

            String fullPathFile = UrlUtil.minimallyEncodeUrl(cuBase + filenameValue + ".pdf");
            List<String> returnList = new ArrayList<String>();
            returnList.add(fullPathFile);
            return returnList;
        }

    }
}


