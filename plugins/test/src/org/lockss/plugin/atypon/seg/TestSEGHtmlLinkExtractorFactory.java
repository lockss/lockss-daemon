/*
 * $Id: TestSEGHtmlLinkExtractorFactory.java,v 1.1 2014-09-04 03:16:36 ldoan Exp $
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

package org.lockss.plugin.atypon.seg;

import java.util.*;
import org.lockss.test.*;
import org.lockss.util.Constants;
import org.lockss.extractor.LinkExtractor;

/*
 * Looks for the javascript function 'popRefFull' in html 'a' tags, and
 * extracts the image id.  This image id, and the doi and article id extracted
 * from base uri are used to contruct the url for 'showFullPopup' page
 * where the large size images can be found.
 */
public class TestSEGHtmlLinkExtractorFactory extends LockssTestCase {

  public void testPopRefFull() throws Exception {
    String input =
        "<a href=\"javascript:popRefFull('fig1')\" class=\"ref\">" +
        "<img border=\"1\" align=\"bottom\" id=\"fig1image\" alt=\"\" " +
        "src=\"/imagesource/home/xxx/yyy/zzz/journals/content/jnamex" +
        "/2013/gabc.2013.99.issue-99/gabc2013-0099.9/20139999/images/small" +
        "/figure1.gif\">" +
        "<br><strong>View larger image </strong>(64K)<br><br></a>";
    String srcUrl = "http://www.example.com/doi/full/99.9999/gabc2013-0099.9";
    LinkExtractor le = new SEGHtmlLinkExtractorFactory()
                              .createLinkExtractor(Constants.MIME_TYPE_HTML);
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
    String expUrl = "http://www.example.com/action/showFullPopup" +
    		                    "?id=fig1&doi=99.9999%2Fgabc2013-0099.9";

    assertEquals(expUrl, emitted.get(0));
  }  
  
}
