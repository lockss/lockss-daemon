/*
 * $Id: TestSilverchairHtmlLinkExtractor.java,v 1.1 2014-06-03 00:49:48 thib_gc Exp $
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

public class TestSilverchairHtmlLinkExtractor extends LockssTestCase {

  public void testImg() throws Exception {
    String input =
        "<img src=\"foosrc.jpg\" data-original=\"foodo.jpg\" />\n" +
        "<img data-original=\"bardo.jpg\" src=\"barsrc.jpg\" />\n";
    String srcUrl = "http://www.example.com/";
    LinkExtractor le = new SilverchairHtmlLinkExtractorFactory().createLinkExtractor(Constants.MIME_TYPE_HTML);
    final Set<String> emitted = new HashSet<String>();
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
    assertEquals(4, emitted.size());
    assertContains(emitted, srcUrl + "foosrc.jpg");
    assertContains(emitted, srcUrl + "foodo.jpg");
    assertContains(emitted, srcUrl + "barsrc.jpg");
    assertContains(emitted, srcUrl + "bardo.jpg");
  }
  
}
