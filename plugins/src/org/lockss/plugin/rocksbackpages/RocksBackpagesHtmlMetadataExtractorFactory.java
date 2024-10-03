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

/*
 * There is very little metadata in article pages so we
 * have to use JSoup to comb through the article to find
 * some. So far, the metadata found is the following: 
    <h1 class="article"> Where’s the Money from Monterey Pop?</h1>

    <p class="article-details">
        <span class="writer">Michael Lydon</span>,
        <span class="publication">Rolling Stone</span>,
        <span class="date">9 November 1967</span>
    </p>
 */

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
                        String title = null;
                        String publication = null;
                        String author = null;
                        String date = null;

                        Elements h1_element_title;
                        Elements span_element_publication;
                        Elements span_element_author;
                        Elements span_element_date;
                        String url = cu.getUrl();
                        try {
                            Document doc = Jsoup.parse(in, cu.getEncoding(), url);
                            h1_element_title = doc.select("h1[class=\"article\"]");
                            span_element_publication = doc.select("span[class=\"publication\"]"); 
                            span_element_author = doc.select("span[class=\"writer\"]");
                            span_element_date = doc.select("span[class=\"date\"]");
                            title = checkElement(h1_element_title);
                            publication = checkElement(span_element_publication);
                            author = checkElement(span_element_author);
                            date = checkElement(span_element_date);
                        } catch (IOException e) {
                            log.debug3("Rocks Backpages: Error getting Metadata", e);
                        }
                        in.close();
                        am = fillMetadata(title, MetadataField.FIELD_ARTICLE_TITLE, am);
                        am = fillMetadata(publication, MetadataField.FIELD_PUBLICATION_TITLE, am);
                        am = fillMetadata(author, MetadataField.FIELD_AUTHOR, am);
                        am = fillMetadata(date, MetadataField.FIELD_DATE, am);
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return;
            }

            protected String checkElement(Elements element) {
                String cleanedUpElement = null;
                if ( element != null){
                    cleanedUpElement = element.text().trim();
                    log.debug3("Rock's Backpages: Element is " + element);
                    if (cleanedUpElement != null) {
                        log.debug3("Rock's Backpages: Element cleaned is " + cleanedUpElement);
                    } else {
                        log.debug3("Rock's Backpages: Element is null");
                    }
                }
                return cleanedUpElement;
            }

            protected ArticleMetadata fillMetadata(String metadata, MetadataField mf, ArticleMetadata am){
                if (metadata != null && metadata != "") {
                    log.debug3("Rock's Backpages: --------getAdditionalMetadata: " + metadata + "-------");
                    am.put(mf, metadata);
                } else {
                    log.debug3("Rock's Backpages: --------getAdditionalMetadata: " + metadata + " Failed-------");
                }
                return am;
            }
    }
}
