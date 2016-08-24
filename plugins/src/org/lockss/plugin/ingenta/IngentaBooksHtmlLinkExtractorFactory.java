/*
 * $Id:$
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