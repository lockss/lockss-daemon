/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

/*
*public class UbiquityPartnerNetworkXmlLinkExtractorFactory
*    extends XmlLinkExtractorFactory {
*
*    private static Logger logger =
*            Logger.getLogger("UbiquityPartnerNetworkXmlLinkExtractorFactory");
*
*    XmlLinkExtractor UbiquityPartnerNetworkXmlLinkExtractor =
*            new XmlLinkExtractor;
*
*
*
*}
*/

public class UbiquityPartnerNetworkXmlLinkExtractorFactory
        implements LinkExtractorFactory { /* tried extends XmlLinkExtractorFactory but no luck. */
    protected static final Logger log = Logger.getLogger("UbiquityPartnerNetworkXmlLinkExtractorFactory");

    @Override
    public LinkExtractor createLinkExtractor(String mimeType)
            throws PluginException {
        return new UbiquityPartnerNetworkXmlLinkExtractor();
    }
    /* static class is stylistic but is used because it doesnt need the implicit pointer to the parent class */
    protected static class UbiquityPartnerNetworkXmlLinkExtractor
            extends XmlLinkExtractor {

        @Override
        public void extractUrls(ArchivalUnit au,
                                InputStream in,
                                String encoding,
                                String srcUrl,
                                Callback cb)
                throws IOException, PluginException {
            String badUrl = new String("https://www.gewina-studium.nl/articles/10.18352/studium.10198/galley/10893/download/");
            if (srcUrl.equals(badUrl)) {
                log.debug3("NOT extracting from "+srcUrl);
                return;
            } /* This works for some reason... */
            super.extractUrls(au,
                        in,
                        encoding,
                        srcUrl,
                        cb);
        }

    }

}