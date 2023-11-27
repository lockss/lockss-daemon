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

package org.lockss.plugin.ingenta;



import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/* an addition to JsoupHtmlLinkExtractor  */
/* 
 * Finds the full-text PDF download button which is in the onclick portion of
 * a link tag <a>
 * Be sure to allow the standard jsoup to pull from href, download 
 * as normal...
 * 
 * <a class="fulltext pdf btn btn-general icbutton" 
 *    onclick="javascript:popup('/search/download?pub=infobike%3a%2f%2fbkpub%2f2ouacs%2f2015%2f00000001%2f00000001%2fart00001&mimetype=application%2fpdf&exitTargetId=1463607913143','downloadWindow','900','800')" title="PDF download of Dare to Serve" class="no-underline contain" >
 * and extracts from the javascript the link
 *   <base>/search/download?pub=infobike%3a%2f%2fbkpub%2f2ouacs%2f2015%2f00000001%2f00000001%2fart00001&mimetype=application%2fpdf&exitTargetId=1463607913143
 * which is URL normalized to the crawler stable version of the PDF
 * 
 */
public class IngentaBooksHtmlLinkExtractorFactory 
implements LinkExtractorFactory {

  private static final Logger log = 
      Logger.getLogger(IngentaBooksHtmlLinkExtractorFactory.class);

  //javascript:popup('/sea...',...)
  // group1 is whichever quote is being used just inside the open-paren
  // group2 will be everything between the first quotemark and then matching quotemark
 protected static final Pattern onClickPdfPattern =
 Pattern.compile("^javascript:popup\\(([\"'])([^\"',]*)\\1,", 
     Pattern.CASE_INSENSITIVE);
      
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) {
    return new IngentaBooksHtmlLinkExtractor();
  }

  public static class IngentaBooksHtmlLinkExtractor extends JsoupHtmlLinkExtractor {

    public IngentaBooksHtmlLinkExtractor() {
      super();
      registerAOnclickTagExtractor();
    }

    // call this with href, download so that standard extraction occurs
    // but pick up any onclick that exist and fit the pattern as well
    protected void registerAOnclickTagExtractor() {
      registerTagExtractor("a", new SimpleTagLinkExtractor(new String[] {"href", "download"}) {
        @Override
        public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
          String onclick = node.attr("onclick");
          if (!StringUtil.isNullString(onclick)) { 
            Matcher onclickMat = onClickPdfPattern.matcher(onclick);            
            if (onclickMat.find()) {
              log.debug3("Found an <a> tag with pdf onclick");
              String newUrl = onclickMat.group(2);
              log.debug3("create pdf URL: " + newUrl);
              cb.foundLink(newUrl);           
            }
          }
          // need to handle href & download as per usual
          super.tagBegin(node, au, cb);
        }
      });
    }
  }
}