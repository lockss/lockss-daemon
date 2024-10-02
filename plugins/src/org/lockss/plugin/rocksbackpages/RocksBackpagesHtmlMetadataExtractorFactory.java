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

package org.lockss.plugin.rocksbackpages;

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
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

public class RocksBackpagesHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory{
    static Logger log = Logger.getLogger(RocksBackpagesHtmlMetadataExtractorFactory.class);

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target, String contentType) throws PluginException {
        return new RocksBackpagesMetadataExtractor();
    }
    public static class RocksBackpagesMetadataExtractor extends SimpleHtmlMetaTagMetadataExtractor {
            private static MultiMap tagMap = new MultiValueMap();

            @Override
            public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
            throws IOException {
                ArticleMetadata am = super.extract(target, cu);
                am.cook(tagMap);
                getAdditionalMetadata(cu, am);

                return am;
            }

            private void getAdditionalMetadata(CachedUrl cu, ArticleMetadata am){
                InputStream in = cu.getUnfilteredInputStream();
                if (in != null) {
                    try {
                        String publication = null;
                        String author = null;
                        publication = getPublication(in, cu.getEncoding(), cu.getUrl());
                        //author = getAuthor(in, cu.getEncoding(), cu.getUrl());
                        in.close();
                        if (publication != null && publication != "") {
                            log.info("Rocks Backpages: Volume--------getAdditionalMetadata: publication-------");
                            am.put(MetadataField.FIELD_PUBLICATION_TITLE, publication);
                        } else {
                            log.info("Rocks Backpages: Volume--------getAdditionalMetadata: publication Failed-------");
                        }
                        if (author != null && author != "") {
                            log.info("Rocks Backpages: Author--------getAdditionalMetadata: author-------");
                            am.put(MetadataField.FIELD_AUTHOR, author);
                        } else {
                            log.info("Rocks Backpages: Author--------getAdditionalMetadata: author Failed-------");
                        }
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return;
            }

            protected String getPublication(InputStream in, String encoding, String url) {
                Elements span_element;
                try {
                    Document doc = Jsoup.parse(in, encoding, url);

                    span_element = doc.select("span[class=\"publication\"]"); //<span class="publication">Rolling Stone</span>,
                    log.info("Rocks Backpages Publication");
                    String publication = null;
                    if ( span_element != null){
                        publication = span_element.text().trim();
                        log.info("Rocks Backpages: publication: = " + span_element + ", url = " + url);
                        if (publication != null) {
                            log.info("Rocks Backpages: Publication cleaned: = " + publication + ", url = " + url);
                            return publication.trim();
                        } else {
                            log.info("Rocks Backpages: Volume is null" + ", url = " + url);
                        }
                        return null;
                    }
                } catch (IOException e) {
                    log.info("Rocks Backpages: Publication Error getPublication", e);
                    return null;
                }
                return null;
            }

            protected String getAuthor(InputStream in, String encoding, String url) {
                Elements span_element;
                try {
                    Document doc = Jsoup.parse(in, encoding, url);
                    span_element = doc.select("span[class=\"writer\"]"); //<span class="writer">Michael Lydon</span>,
                    log.info("Rocks Backpages Author");
                    String author = null;
                    if ( span_element != null){
                        author = span_element.text().trim();
                        log.info("Rocks Backpages: author: = " + span_element + ", url = " + url);
                        if (author != null) {
                            log.info("Rocks Backpages: Author cleaned: = " + author + ", url = " + url);
                            return author.trim();
                        } else {
                            log.info("Rocks Backpages: Author is null" + ", url = " + url);
                        }
                        return null;
                    }
                } catch (IOException e) {
                    log.info("Rocks Backpages: Author Error getAuthor", e);
                    return null;
                }
                return null;
            }
    }
}
