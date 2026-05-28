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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class BMPHtmlLinkExtractorFactory implements LinkExtractorFactory{
    
    protected static final Logger log = Logger.getLogger(BMPHtmlLinkExtractorFactory.class);

    @Override
    public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
        return new BMPHtmlLinkExtractor();
    }

    public static class BMPHtmlLinkExtractor extends GoslingHtmlLinkExtractor{

        public static final Pattern htmlPat = Pattern.compile("https://agridergisi\\.com/issue/[0-9]+");

        @Override
        public void extractUrls(final ArchivalUnit au, InputStream in, String encoding, final String srcUrl,
                final Callback cb) throws IOException {
                    //Baycinar provided manifest issue pages but we need to collect the issue pages on the actual website. But we do NOT want to 
                    //collect anything on these pages due to high risk of overcrawl.
                    Matcher htmlMat = htmlPat.matcher(srcUrl);
                    if(htmlMat.find()){
                        log.debug3("Source URL is " + srcUrl + " and is an issue page. No URLs on this page will be crawled.");
                        return;
                    }else{
                        super.extractUrls(au, in, encoding, srcUrl, cb);;
                    }
                }
    }
}
