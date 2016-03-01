/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
    registerTagExtractor("a", new SimpleTagLinkExtractor("onclick") {
      @Override
      public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
        String onclick = node.attr("onclick");
        if (!StringUtil.isNullString(onclick)) {
          String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
          Pattern onclickPat = Pattern.compile(String.format("[\"'][ \t]*(%s[^ \t\"']*)[ \t]*[\"']", baseUrl),
                                               Pattern.CASE_INSENSITIVE);
          Matcher onclickMat = onclickPat.matcher(onclick);
          if (onclickMat.find()) {
            cb.foundLink(onclickMat.group(1));
            return;
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
              return;
            }
          }
        }
        super.tagBegin(node, au, cb);
      }
    });

  }
  
}
