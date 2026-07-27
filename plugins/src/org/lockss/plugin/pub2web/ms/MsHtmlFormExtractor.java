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

import org.jsoup.nodes.Node;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.HtmlFormExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
PDF link used to be a href link
<a href="/deliver/fulltext/jmmcr/3/2/jmmcr000086.pdf?itemId=/content/journal/jmmcr/10.1099/jmmcr.0.000086&amp;mimeType=pdf&amp;isFastTrackArticle=" title="" rel="external" class="externallink pdf
list-group-item list-group-item-info" ><div class="fa fa-file-pdf-o full-text-icon"></div>PDF
<div class="fulltextsize ">
539.07
Kb
</div></a>


PDF link embedded into form action since 3/2020

<form action="/deliver/fulltext/micro/165/3/254_micro000750.pdf?itemId=%2Fcontent%2Fjournal%2Fmicro%2F10.1099%2Fmic.0.000750&mimeType=pdf&containerItemId=content/journal/micro"
target="/content/journal/micro/10.1099/mic.0.000750-pdf"
data-title="Download"
data-itemId="http://instance.metastore.ingenta.com/content/journal/micro/10.1099/mic.0.000750"
class="ft-download-content__form ft-download-content__form--pdf js-ft-download-form " >
<input type="hidden" name="pending" value="false" >
<i class="fa fa-file-pdf-o
access-options-icon" aria-hidden="true"></i>
<span class="hidden-xxs">PDF</span>
</form>
 */

public class MsHtmlFormExtractor extends HtmlFormExtractor {

    private static Logger log = Logger.getLogger(MsHtmlFormExtractor.class);

    public MsHtmlFormExtractor(ArchivalUnit au, LinkExtractor.Callback cb, String encoding,
                                  Map<String, FormFieldRestrictions> restrictions,
                                  Map<String, FieldIterator> generators) {
        super(au, cb, encoding, restrictions, generators);
    }

    @Override
    public FormElementLinkExtractor newTagsLinkExtractor() {
        return new MsFormElementLinkExtractor();
    }

    public static class MsFormElementLinkExtractor extends FormElementLinkExtractor {

        private static final String ACTION_ATTR = "action";
        
        private static Logger log = Logger.getLogger(MsFormElementLinkExtractor.class);

        /*
         * Extending
         */
        public void tagBegin(Node node, ArchivalUnit au, LinkExtractor.Callback cb) {

            String srcUrl = node.baseUri();

            log.debug3("MsHtmlFormExtractor custom tagBegin for" + srcUrl);

            if (node.hasAttr(ACTION_ATTR)) {
                if ("action".equalsIgnoreCase((node.attr(ACTION_ATTR)))) {
                    String pdfLink = node.attr(ACTION_ATTR);

                    String base = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
                    String newUrl = base + pdfLink;
                    log.debug3("MsHtmlFormExtractor PDF link: " + newUrl);
                    cb.foundLink(newUrl);
                }
            }
            log.debug3("now calling the super tagBegin");
            super.tagBegin(node, au, cb);
        }
    }
}



