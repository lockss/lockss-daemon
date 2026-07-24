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

package org.lockss.plugin.gigascience;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;

public class GigaScienceAPIXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(GigaScienceAPIXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper GigaScienceAPIHelper = null;

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new GigaScienceAPIHtmlMetadataExtractor();
    }

    public class GigaScienceAPIHtmlMetadataExtractor extends SourceXmlMetadataExtractor {

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            if (GigaScienceAPIHelper== null) {
                GigaScienceAPIHelper = (SourceXmlSchemaHelper) new GigaScienceAPIHelper();
                log.debug3("Setup GigaScienceAPIHelper Metadata Extractor");
            }
            return GigaScienceAPIHelper;
        }
        
        @Override
        protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {

            log.debug3("in GigaScienceAPI  postCookProcess");
            
            thisAM.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_FILE);
            thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_FILE);

            // Add a custom map to the generic am table 
            // Allow a child to override FileType
            Map<String, String> FILE_MAP = new HashMap<String,String>();

            //default is "file"
            FILE_MAP.put("FileType", MetadataField.ARTICLE_TYPE_FILE);
            // default is base filename
            FILE_MAP.put("FileIdentifier", getFileIdentifier(cu));
            FILE_MAP.put("FileSizeBytes", getFileSize(cu));
            FILE_MAP.put("FileMime", getFileMime(cu));
            // default is no additional k-v pairs; child can add specific items
            thisAM.putRaw(MetadataField.FIELD_MD_MAP.getKey(), FILE_MAP);

        }

        protected String getFileIdentifier(CachedUrl cu) {
            return FilenameUtils.getBaseName(cu.getUrl());
        }

        protected String getFileSize(CachedUrl cu) {
            long content_size = cu.getContentSize();
            return Long.toString(content_size);

        }
        
        protected String getFileMime(CachedUrl cu) {
            String content_mime = cu.getContentType();
            // needed after the getContentType
            AuUtil.safeRelease(cu);
            return content_mime;

        }

    }
}
