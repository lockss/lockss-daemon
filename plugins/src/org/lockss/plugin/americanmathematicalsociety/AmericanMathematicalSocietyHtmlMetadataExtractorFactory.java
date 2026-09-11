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

package org.lockss.plugin.americanmathematicalsociety;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.MetadataField.Cardinality;
import org.lockss.extractor.MetadataField.Validator;
import org.lockss.plugin.*;

/* In 2026, there was a complete overhaul of the AMS site and all meta tags were removed.
   We will now be using JSoup to pull metadata from the html source. */

public class AmericanMathematicalSocietyHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
    static Logger log = Logger.getLogger(AmericanMathematicalSocietyHtmlMetadataExtractorFactory.class);

    //The DOI on the webpage usually looks like this: 
    //       DOI: https://doi.org/10.1090/jams/929
    protected static Pattern doiPattern = Pattern.compile("(.*https://doi\\.org/)(/?10\\.[\\d]{4,}/.*)", Pattern.CASE_INSENSITIVE);
    private static final String DOI_REPL = "$2";

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target, String contentType) throws PluginException {
        return new AmericanMathematicalSocietyHtmlMetadataExtractor();
    }
    public static class AmericanMathematicalSocietyHtmlMetadataExtractor extends SimpleHtmlMetaTagMetadataExtractor {
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
                        String volume = null;
                        String doi = null;

                        Elements div_element_title;
                        Elements header_element_publication;
                        Elements div_element_author;
                        Elements div_element_volume;
                        Elements div_element_doi;
                        String url = cu.getUrl();
                        try {
                            Document doc = Jsoup.parse(in, cu.getEncoding(), url);
                            div_element_title = doc.select("div[id=\"productDetailSubscriptionSelectedArticleContainer\"]>div[class=\"productDetailSubscriptionTabHeader\"]");
                            header_element_publication = doc.select("header[class=\"journalHomeHeader\"]>h2[class=\"headerInfo\"]"); 
                            div_element_author = doc.select("div[class=\"productDetailSubscriptionTabInfo\"]>div:contains(by)>a");
                            div_element_volume = doc.select("div[class=\"productDetailArticleInfoContent\"]>div>strong");
                            div_element_doi = doc.select("div[id=\"articleBibliographyContent\"]>div>ul>li:contains(DOI:)");
                            title = checkElement(div_element_title);
                            publication = checkElement(header_element_publication);
                            author = checkElement(div_element_author);
                            volume = checkElement(div_element_volume);
                            doi = cleanDoi(div_element_doi);
                        } catch (IOException e) {
                            log.debug3("American Mathematical Society: Error getting Metadata", e);
                        }
                        in.close();
                        am = fillMetadata(title, MetadataField.FIELD_ARTICLE_TITLE, am);
                        am = fillMetadata(publication, MetadataField.FIELD_PUBLICATION_TITLE, am);
                        am = fillMetadata(author, MetadataField.FIELD_AUTHOR, am);
                        am = fillMetadata(volume, MetadataField.FIELD_VOLUME, am);
                        am = fillMetadata(doi, MetadataField.FIELD_DOI, am);
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return;
              }

            private static String cleanDoi(Elements doi) {
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

            protected String checkElement(Elements element) {
                String cleanedUpElement = null;
                if ( element != null){
                    cleanedUpElement = element.text().trim();
                    log.debug3("American Mathematical Society: Element is " + element);
                    if (cleanedUpElement != null) {
                        log.debug3("American Mathematical Society: Element cleaned is " + cleanedUpElement);
                    } else {
                        log.debug3("American Mathematical Society: Element is null");
                    }
                }
                return cleanedUpElement;
            }

            protected ArticleMetadata fillMetadata(String metadata, MetadataField mf, ArticleMetadata am){
                if (metadata != null && metadata != "") {
                    log.debug3("American Mathematical Society: --------getAdditionalMetadata: " + metadata + "-------");
                    am.put(mf, metadata);
                } else {
                    log.debug3("American Mathematical Society: --------getAdditionalMetadata: " + metadata + " Failed-------");
                }
                return am;
            }
    }
  
}
