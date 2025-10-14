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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class UbiquityPartnerNetworkHtmlLinkExtractorFactory implements LinkExtractorFactory {

    Logger log = Logger.getLogger(UbiquityPartnerNetworkHtmlLinkExtractorFactory.class);
    static Pattern spritesheetPattern = Pattern.compile("/api/[0-9\\.]+/spritesheet");

    @Override
    public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
        return new UbiquityPartnerNetworkHtmlLinkExtractor();
    }

    public class UbiquityPartnerNetworkHtmlLinkExtractor extends GoslingHtmlLinkExtractor{

        @Override
        protected String extractLinkFromTag(StringBuffer link,
                                        ArchivalUnit au,
                                        Callback cb)
        throws IOException {
            char ch = link.charAt(0);
            if ((ch == 'u' || ch == 'U') && beginsWithTag(link, "use")) {
                String href = getAttributeValue("href", link);
                if(href != null){
                    Matcher spritesheetMatcher = spritesheetPattern.matcher(href);
                    if(spritesheetMatcher.find()){
                        cb.foundLink(resolveUri(baseUrl, href));
                        log.debug3("The href is :" + resolveUri(baseUrl, href));
                    }
                }
                return href;
            } else{
                return super.extractLinkFromTag(link, au, cb);
            }
        }
    }
    
}
