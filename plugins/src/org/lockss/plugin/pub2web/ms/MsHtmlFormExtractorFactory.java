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

package org.lockss.plugin.pub2web.ms;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.HtmlFormExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.jstor.JstorHtmlFormExtractor;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MsHtmlFormExtractorFactory implements LinkExtractorFactory {
    
    public org.lockss.extractor.LinkExtractor createLinkExtractor(String mimeType) {

        // must turnon form processng which is off by default
        return new MsHtmlLinkExtractor(false,true,null,null);
    }

    public static class MsHtmlLinkExtractor extends JsoupHtmlLinkExtractor {

        private static Logger log = Logger.getLogger(MsHtmlLinkExtractor.class);


        public MsHtmlLinkExtractor(boolean enableStats, boolean processForms,
                                      Map<String,
                                              HtmlFormExtractor
                                                      .FormFieldRestrictions> restrictors,
                                      Map<String, HtmlFormExtractor.FieldIterator>
                                              generators) {
            super(enableStats, processForms, restrictors, generators);
        }


        @Override
        protected HtmlFormExtractor getFormExtractor(final ArchivalUnit au,
                                                     final String encoding,
                                                     final Callback cb) {
            log.debug3("Creating new MsHtmlFormExtractor");
            return new MsHtmlFormExtractor(au, cb, encoding,   getFormRestrictors(), getFormGenerators());
        }
    }
}
