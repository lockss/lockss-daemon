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

package org.lockss.plugin.jstor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.HtmlFormExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;

/* Jstor needs its own LinkExtractor so that it can generate a link
 * for citation download from the DOI information stored on TOC pages.
 * Because the information is stored on the TOC page in a checkbox <input> tag
 * the html link extractor itself cannot do the work. It must use a custom
 * Jstor FORM element link extractor. 
 * This plugin version of the link extractor is needed to specify use of the 
 * custom HtmlFormExtractor which has the necessary custom FormElementLinkExtractor
 * 
 * This custom JsoupHtmlLinkExtractor could also be used to set up additional
 * restrictions for form processing but at least for now this isn't needed because
 * Jstor only allows us such limited crawling (because of redirection) that we
 * control the creation of the citation URLs anyway.
 */

public class JstorHtmlLinkExtractorFactory 
implements LinkExtractorFactory {

  Pattern SCRIPT_SRC_PAT = Pattern.compile("\\.src[ \t]*=[ \t]*[\"'][ \t]*(/[^ \t\"']*)[ \t]*[\"'][ \t]*;",
      Pattern.CASE_INSENSITIVE);

  
  public org.lockss.extractor.LinkExtractor createLinkExtractor(String mimeType) {

    // must turnon form processng which is off by default
    return new JstorHtmlLinkExtractor(false,true,null,null);
  }

  public static class JstorHtmlLinkExtractor extends JsoupHtmlLinkExtractor {
    public static Pattern SCRIPT_SRC_PAT = Pattern.compile("\\.src[ \t]*=[ \t]*[\"'][ \t]*(/[^ \t\"']*)[ \t]*[\"'][ \t]*;",
        Pattern.CASE_INSENSITIVE);

    
    
    private static Logger log = Logger.getLogger(JstorHtmlLinkExtractor.class);

    
    public JstorHtmlLinkExtractor(boolean enableStats, boolean processForms,
                                Map<String,
                                       HtmlFormExtractor
                                           .FormFieldRestrictions> restrictors,
                                Map<String, HtmlFormExtractor.FieldIterator>
                                    generators) {
      super(enableStats, processForms, restrictors, generators);
      registerScriptTagExtractor();
    }


    @Override
    protected HtmlFormExtractor getFormExtractor(final ArchivalUnit au,
        final String encoding,
        final Callback cb) {
      log.debug3("Creating new JstorHtmlFormExtractor");
      return new JstorHtmlFormExtractor(au, cb, encoding,   getFormRestrictors(), getFormGenerators());
    }
    
    // This is needed by the Jstor Current Scholarship plugin to find the js support 
    // at base_url/px/client/main.min.js
    // in <script (function() {....s.src='/px/client/main.min.js'...
    protected void registerScriptTagExtractor() {
      registerTagExtractor("script", new ScriptTagLinkExtractor() {
        @Override
        public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
          String scriptHtml = ((Element)node).html();
          if (!StringUtil.isNullString(scriptHtml)) {
            Matcher mat = SCRIPT_SRC_PAT.matcher(scriptHtml);
            if (mat.find()) {
              String url = mat.group(1);
              if (UrlUtil.isSameHost(au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()), url)) {
                url = AuUtil.normalizeHttpHttpsFromBaseUrl(au, url);
              }
              cb.foundLink(url);
            }
          }
          super.tagBegin(node, au, cb);
        }
      });
    }
    
  }


}
