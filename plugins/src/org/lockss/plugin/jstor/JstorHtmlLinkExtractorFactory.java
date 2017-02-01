/* $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

 */

package org.lockss.plugin.jstor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.lockss.extractor.HtmlFormExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

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
      
      // TODO Auto-generated constructor stub
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
              cb.foundLink(mat.group(1));
            }
          }
          super.tagBegin(node, au, cb);
        }
      });
    }
    
  }


}
