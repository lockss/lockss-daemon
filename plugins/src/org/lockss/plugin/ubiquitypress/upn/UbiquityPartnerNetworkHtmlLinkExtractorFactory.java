/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ubiquitypress.upn;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class UbiquityPartnerNetworkHtmlLinkExtractorFactory implements LinkExtractorFactory {

    protected static final Logger log = Logger.getLogger(UbiquityPartnerNetworkHtmlLinkExtractorFactory.class);

    @Override
    public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
        return new UbiquityPartnerNetworkHtmlLinkExtractor();
    }

    public static class UbiquityPartnerNetworkHtmlLinkExtractor extends GoslingHtmlLinkExtractor{

        // An image candidate string in a srcset/imagesrcset attribute (see: https://html.spec.whatwg.org/multipage/images.html#srcset-attributes)
        // Group 1: Zero or more ASCII whitespace (see https://infra.spec.whatwg.org/#ascii-whitespace)
        // Group 2: A valid non-empty URL that does not start or end with a U+002C COMMA character
        // Group 3: Zero or more ASCII whitespace
        // Group 4: Zero or one of either:
        //       4a: ASCII whitespace, a non-negative integer greater than zero, and a U+0077 LATIN SMALL LETTER W character
        //    or 4b: ASCII whitespace, a floating-point number greater than zero, and a U+0078 LATIN SMALL LETTER X character
        // Group 5: Zero or more ASCII whitespace
        public static final Pattern srcSetPat = Pattern.compile(  "([ \\t\\n\\r\\f]*)"
                                                                + "([^, \\t\\n\\r\\f]|[^, \\t\\n\\r\\f][^ \\t\\n\\r\\f]*[^, \\t\\n\\r\\f])" // hardly a thorough URL grammar (see https://html.spec.whatwg.org/multipage/urls-and-fetching.html#valid-non-empty-url)
                                                                + "([ \\t\\n\\r\\f]*)"
                                                                + "("
                                                                  + "[ \\t\\n\\r\\f]+[1-9][0-9]*[wW]" // allowing uppercase W
                                                                  + "|"
                                                                  + "[ \\t\\n\\r\\f]+[.0-9]+(?:[eE][+-]?[0-9]+)?[xX]" // allowing uppercase X; not a thorough floating point grammar (see https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#valid-floating-point-number)
                                                                + ")?"
                                                                + "([ \\t\\n\\r\\f]*)");

        public static final Pattern htmlPat = Pattern.compile("/files/submission/proof/.*\\.html");
      
        @Override
        protected String extractLinkFromTag(StringBuffer link,
                                        ArchivalUnit au,
                                        Callback cb)
        throws IOException {
            char ch = link.charAt(0);
            if ((ch == 'i' || ch == 'I') && beginsWithTag(link, "img")) {
                String srcSet = getAttributeValue("srcset", link);
                if (srcSet != null) {
                    String[] urls = getUrlsFromSrcSet(srcSet);
                    for (String url : urls){
                        if (StringUtils.isNotEmpty(url)) {
                            String resolved = resolveUri(baseUrl, url);
                            log.debug3("Image candidate: " + resolved);
                            cb.foundLink(resolved);
                        }
                    }
                }
            }
            else if ((ch == 'l' || ch == 'L') && beginsWithTag(link, "link")) {
                String imageSrcSet = getAttributeValue("imagesrcset", link);
                if (imageSrcSet != null) {
                    String[] urls = getUrlsFromSrcSet(imageSrcSet);
                    for (String url : urls){
                        if (StringUtils.isNotEmpty(url)) {
                            String resolved = resolveUri(baseUrl, url);
                            log.debug3("Image candidate: " + resolved);
                            cb.foundLink(resolved);
                        }
                    }
                }
            }
            else if ((ch == 's' || ch == 'S') && beginsWithTag(link, "source")) {
                String srcSet = getAttributeValue("srcset", link);
                if (srcSet != null) {
                    String[] urls = getUrlsFromSrcSet(srcSet);
                    for (String url : urls){
                        if (StringUtils.isNotEmpty(url)) {
                            String resolved = resolveUri(baseUrl, url);
                            log.debug3("Image candidate: " + resolved);
                            cb.foundLink(resolved);
                        }
                    }
                }
            }
            else if ((ch == 'u' || ch == 'U') && beginsWithTag(link, "use")) {
                // This platform has <svg> images that have <use> tags that are referenced
                String href = getAttributeValue("href", link);
                if (href != null){
                    String resolved = resolveUri(baseUrl, href);
                    log.debug3("The href is: " + resolved);
                    cb.foundLink(resolved);
                }
            }
            return super.extractLinkFromTag(link, au, cb);
        }

    
        public static String[] getUrlsFromSrcSet(String val) {
            String[] parts = StringUtils.split(val, ',');
            for (int i = 0 ; i < parts.length ; ++i) {
                Matcher srcSetMat = srcSetPat.matcher(parts[i]);
                parts[i] = srcSetMat.matches() ? srcSetMat.group(2) : "";
            }
            return parts;
        }


        @Override
        public void extractUrls(final ArchivalUnit au, InputStream in, String encoding, final String srcUrl,
                final Callback cb) throws IOException {
                    //UPN told us to NOT crawl any content on uploaded html version of an article since a lot of the content on them are broken. 
                    Matcher htmlMat = htmlPat.matcher(srcUrl);
                    if(htmlMat.find()){
                        log.debug3("Source URL is " + srcUrl + " and is an html page. No urls on this page wil be crawled.");
                        return;
                    }else{
                        super.extractUrls(au, in, encoding, srcUrl, cb);;
                    }
                }
    
    }
    
}
