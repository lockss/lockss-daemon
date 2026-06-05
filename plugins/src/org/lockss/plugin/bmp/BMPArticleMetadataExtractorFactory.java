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

package org.lockss.plugin.bmp;

import java.io.IOException;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.bmp.BMPTOCMetadataExtractorFactory;
import org.lockss.plugin.bmp.BMPAbstractMetadataExtractorFactory;
import org.lockss.util.Logger;

public class BMPArticleMetadataExtractorFactory implements ArticleMetadataExtractorFactory{

    static Logger log = Logger.getLogger(BMPArticleMetadataExtractorFactory.class);

    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
        throws PluginException{
            return new BMPArticleMetadataExtractor();
        }

    public class BMPArticleMetadataExtractor implements ArticleMetadataExtractor{

        public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
            throws IOException, PluginException{
                ArticleMetadata am = new ArticleMetadata();
                CachedUrl metadataUrl = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
                FileMetadataExtractor me = null;
                if(metadataUrl.toString().contains("issue")){
                    String doi = af.getRoleAsString(ArticleFiles.ROLE_ARTICLE_HANDLE);
                    if(!doi.isEmpty()){
                        me = new BMPTOCMetadataExtractorFactory.BMPTOCMetadataExtractor(doi);
                    }
                }else{
                    me = new BMPAbstractMetadataExtractorFactory.BMPAbstractMetadataExtractor();
                }
                if(me != null){
                    me.extract(target, metadataUrl, new FileMetadataExtractor.Emitter() {
                        public void emitMetadata(CachedUrl cu, ArticleMetadata am) {
                            emitter.emitMetadata(af, am);
                        }
                    });
                }
        }
    } 
}