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
package org.lockss.plugin.highwire.isass;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

public class IsassDrupalHtmlMetadataExtractorFactory
implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(IsassDrupalHtmlMetadataExtractorFactory.class);

    //The DOI on the webpage usually looks like this: 
    //       DOI: https://doi.org/10.14444/8654
    protected static Pattern doiPattern = Pattern.compile("(.*https://doi\\.org/)(/?10\\.[\\d]{4,}/.*)", Pattern.CASE_INSENSITIVE);
    //url example: https://www.ijssurgery.com/content/19/1/2
    protected static Pattern urlPattern = Pattern.compile("https://www\\.ijssurgery\\.com/content/([^/]+)/([^/]+)/([^/]+)", Pattern.CASE_INSENSITIVE);
    private static final String DOI_REPL = "$2";
  
    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target, String contentType) throws PluginException {
        return new IsassDrupalHtmlMetadataExtractor();
    }
        public static class IsassDrupalHtmlMetadataExtractor extends SimpleHtmlMetaTagMetadataExtractor {
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
                        String doi = null;
                        String volume = null;
                        String issue = null;
                        String start_page = null;

                        Elements h1_element_title;
                        Elements span_element_doi;
                        String url = cu.getUrl();
                        try {
                            Document doc = Jsoup.parse(in, cu.getEncoding(), url);
                            h1_element_title = doc.select("h1");
                            span_element_doi = doc.select("li[class=\"journal-doi-info\"]>span[class=\"doi-info\"]"); 
                            title = checkElement(h1_element_title.first());
                            doi = cleanDoi(span_element_doi.first());
                            Matcher urlMatcher = urlPattern.matcher(url);
                            if(urlMatcher.matches()){
                              volume = urlMatcher.group(1);
                              issue = urlMatcher.group(2);
                              start_page = urlMatcher.group(3);
                            }
                        } catch (IOException e) {
                            log.debug3("Rocks Backpages: Error getting Metadata", e);
                        }
                        in.close();
                        am = fillMetadata(title, MetadataField.FIELD_ARTICLE_TITLE, am);
                        am = fillMetadata(doi, MetadataField.FIELD_DOI, am);
                        am = fillMetadata(volume, MetadataField.FIELD_VOLUME, am);
                        am = fillMetadata(issue, MetadataField.FIELD_ISSUE, am);
                        am = fillMetadata(start_page, MetadataField.FIELD_START_PAGE, am);
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return;
            }

            private static String cleanDoi(Element doi) {
              String cleanedDoi = null;
              if(doi != null){
                cleanedDoi = doi.text().trim();
                if(cleanedDoi != null){
                  log.debug3("raw doi: = " + cleanedDoi);
                  Matcher doiMatcher = doiPattern.matcher(cleanedDoi);
                  if (doiMatcher.matches()) {
                    cleanedDoi = doiMatcher.replaceFirst(DOI_REPL);
                    log.debug3("raw doi cleaned: = " + cleanedDoi);
                    return cleanedDoi;
                  } else {
                    log.debug3("raw doi not cleaned: = " + cleanedDoi);
                  }
                  return cleanedDoi;
                }
              }
              return cleanedDoi;
            }

            protected String checkElement(Element element) {
                String cleanedUpElement = null;
                if ( element != null){
                    cleanedUpElement = element.text().trim();
                    log.debug3("Isass Drupal: Element is " + element);
                    if (cleanedUpElement != null) {
                        log.debug3("Isass Drupal: Element cleaned is " + cleanedUpElement);
                    } else {
                        log.debug3("Isass Drupal: Element is null");
                    }
                }
                return cleanedUpElement;
            }

            protected ArticleMetadata fillMetadata(String metadata, MetadataField mf, ArticleMetadata am){
                if (metadata != null && metadata != "") {
                    log.debug3("Isass Drupal: --------getAdditionalMetadata: " + metadata + "-------");
                    am.put(mf, metadata);
                } else {
                    log.debug3("Isass Drupal: --------getAdditionalMetadata: " + metadata + " Failed-------");
                }
                return am;
            }
    }
}