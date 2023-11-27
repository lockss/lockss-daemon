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

package org.lockss.plugin.scielo;

import java.util.regex.*;

import org.jsoup.nodes.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.StringUtil;

public class SciEloHtmlLinkExtractor extends JsoupHtmlLinkExtractor {

  public SciEloHtmlLinkExtractor() {
    super();
    registerAOnclickTagExtractor();
    registerScriptTagExtractor();
  }

  protected void registerAOnclickTagExtractor() {
    registerTagExtractor("a", new SimpleTagLinkExtractor(new String[] {"href", "download"}) {
      @Override
      public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
        String onclick = node.attr("onclick");
        if (!StringUtil.isNullString(onclick)) {
          String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
          Pattern onclickPat = Pattern.compile(String.format("[\"'][ \t]*(%s[^ \t\"']*)[ \t]*[\"']", baseUrl),
                                               Pattern.CASE_INSENSITIVE);
          Matcher onclickMat = onclickPat.matcher(onclick);
          if (onclickMat.find()) {
            cb.foundLink(onclickMat.group(1).trim());
          }
        }
        super.tagBegin(node, au, cb);
      }
    });
  }
  
  protected void registerScriptTagExtractor() {
    registerTagExtractor("script", new ScriptTagLinkExtractor() {
      @Override
      public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
        if (node.baseUri().contains("scielo.php?script=sci_pdf")) {
          String scriptHtml = ((Element)node).html();
          if (!StringUtil.isNullString(scriptHtml)) {
            String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
            Pattern pat = Pattern.compile(String.format("window\\.location[ \t]*=[ \t]*[\"'][ \t]*(%s[^ \t\"']*)[ \t]*[\"'][ \t]*;", baseUrl),
                                                 Pattern.CASE_INSENSITIVE);
            Matcher mat = pat.matcher(scriptHtml);
            if (mat.find()) {
              cb.foundLink(mat.group(1));
            }
          }
        }
        super.tagBegin(node, au, cb);
      }
    });

  }
  
}
