/*
 * $Id: TestScHtmlLinkExtractor.java,v 1.1 2015-02-03 03:07:34 thib_gc Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair;

import java.util.*;

import org.lockss.extractor.LinkExtractor;
import org.lockss.test.*;
import org.lockss.util.Constants;

public class TestScHtmlLinkExtractor extends LockssTestCase {

  public void testImg() throws Exception {
    String input =
        "<img src=\"foosrc.jpg\" data-original=\"foodo.jpg\" />\n" +
        "<img data-original=\"bardo.jpg\" src=\"barsrc.jpg\" />\n" +
        "<img src=\"bazsrc.jpg\" />\n" +
        "<img data-original=\"quxdo.jpg\" />\n";
    String srcUrl = "http://www.example.com/";
    LinkExtractor le = new ScHtmlLinkExtractorFactory().createLinkExtractor(Constants.MIME_TYPE_HTML);
    final List<String> emitted = new ArrayList<String>();
    le.extractUrls(null,
                   new StringInputStream(input),
                   Constants.ENCODING_UTF_8,
                   srcUrl,
                   new LinkExtractor.Callback() {
                       @Override
                       public void foundLink(String url) {
                         emitted.add(url);
                       }
                   });
    assertEquals(6, emitted.size()); // 3 x "src" + 3 x "data-original"
    assertContains(emitted, srcUrl + "foodo.jpg");
    assertContains(emitted, srcUrl + "bardo.jpg");
    assertContains(emitted, srcUrl + "quxdo.jpg");
  }
  
  public void testDownloadFile() throws Exception {
    String input =
        "<a onclick=\"javascript:downloadFile('/data/Journals/JOURN/12345/journ_11_222_edboard.pdf')\">Foo</a>\n";
    String srcUrl = "http://www.example.com/";
    LinkExtractor le = new ScHtmlLinkExtractorFactory().createLinkExtractor(Constants.MIME_TYPE_HTML);
    final List<String> emitted = new ArrayList<String>();
    le.extractUrls(null,
                   new StringInputStream(input),
                   Constants.ENCODING_UTF_8,
                   srcUrl,
                   new LinkExtractor.Callback() {
                       @Override
                       public void foundLink(String url) {
                         emitted.add(url);
                       }
                   });
    assertEquals(1, emitted.size());
    assertContains(emitted, srcUrl + "data/Journals/JOURN/12345/journ_11_222_edboard.pdf");
  }
  
  // This applies only when the citation URL doesn't have the article ID in it
  public void testCitation() throws Exception {
    String input =
        "<a target=\"_blank\" onclick=\"detailsHandler(this.href); return false;\" href=\"../downloadCitation.aspx?format=ris\">RIS</a>\n" +
        "<a target=\"_blank\" onclick=\"detailsHandler(this.href); return false;\" href=\"../downloadCitation.aspx?\">RefWorks</a>\n";
    String baseUrl = "http://www.example.com/";
    String srcUrl = baseUrl + "article.aspx?articleid=1234567";
    LinkExtractor le = new ScHtmlLinkExtractorFactory().createLinkExtractor(Constants.MIME_TYPE_HTML);
    final List<String> emitted = new ArrayList<String>();
    le.extractUrls(null,
                   new StringInputStream(input),
                   Constants.ENCODING_UTF_8,
                   srcUrl,
                   new LinkExtractor.Callback() {
                       @Override
                       public void foundLink(String url) {
                         emitted.add(url);
                       }
                   });
    System.out.println(emitted);
    assertEquals(4, emitted.size()); // 2 x without "articleid" + 2 x with "articleid"
    assertContains(emitted, baseUrl + "downloadCitation.aspx?format=ris&articleid=1234567");
    assertContains(emitted, baseUrl + "downloadCitation.aspx?articleid=1234567");
  }
  
}
