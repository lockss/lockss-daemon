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
import java.io.InputStream;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.SimpleHtmlMetaTagMetadataExtractor;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

public class BMPTOCMetadataExtractorFactory implements FileMetadataExtractorFactory{

    static Logger log = Logger.getLogger(BMPTOCMetadataExtractorFactory.class);

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target, String contentType) throws PluginException {
        //this is not really used, need to refactor code to remove extraneous factory
        return new BMPTOCMetadataExtractor(null);
    }

    public static class BMPTOCMetadataExtractor implements FileMetadataExtractor {
            private static MultiMap tagMap = new MultiValueMap();
            public String doi;

            public BMPTOCMetadataExtractor(String doi){
                this.doi = doi;
            }
            
            public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
                throws IOException, PluginException{
                InputStream in = cu.getUncompressedInputStream();
                ArticleMetadata am = new ArticleMetadata();
                if (in != null) {
                    try {

                        String title = null;
                        String author = null;

                        Elements element_title;
                        Elements element_author;
                        Elements element_doi;
                        String url = cu.getUrl();
                        try {
                            Document doc = Jsoup.parse(in, cu.getEncoding(), url);
                            Elements article = doc.select("div.span9>section[id*=cat]>div:has(>div>div.span8>a[href=\""+doi+"\"])");
                            element_title = article.select("h3>a");
                            element_author = article.select("p");
                            element_doi = article.select("div.row-fluid>div.span8>a");
                            author = checkElement(element_author, "Author");
                            title = checkElement(element_title, "Title");
                            doi = checkElement(element_doi, "DOI");
                        } catch (IOException e) {
                            log.debug3("Baycinar Medical Publishing: Error getting Metadata", e);
                        }
                        in.close();
                        am = fillMetadata(doi, MetadataField.FIELD_DOI, am);
                        am = fillMetadata(title, MetadataField.FIELD_ARTICLE_TITLE, am);
                        am = fillMetadata(author, MetadataField.FIELD_AUTHOR, am);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(doi != null && doi != ""){
                        emitter.emitMetadata(cu, am);
                    } 
                }
            }

            protected String checkElement(Elements element, String specifiedMetadata) {
                String cleanedUpElement = null;
                if ( element != null){
                    if( element.size() > 1){
                        throw new UnsupportedOperationException("Too many elements were found for metadata " + specifiedMetadata);
                    }else{
                        cleanedUpElement = element.text().trim();
                        log.debug3("Baycinar Medical Publishing: Element for " + specifiedMetadata + " is " + element);
                        if (cleanedUpElement != null) {
                            log.debug3("Baycinar Medical Publishing: Element cleaned for " + specifiedMetadata + " is " + cleanedUpElement);
                        } else {
                            log.debug3("Baycinar Medical Publishing: Element is null");
                        }
                    }
                }
                return cleanedUpElement;
            }

            protected ArticleMetadata fillMetadata(String metadata, MetadataField mf, ArticleMetadata am){
                if (metadata != null && metadata != "") {
                    log.debug3("Baycinar Medical Publishing: --------getAdditionalMetadata: " + metadata + "-------");
                    am.put(mf, metadata);
                } else {
                    log.debug3("Baycinar Medical Publishing: --------getAdditionalMetadata: " + metadata + " Failed-------");
                }
                return am;
            }
    }
    
}
