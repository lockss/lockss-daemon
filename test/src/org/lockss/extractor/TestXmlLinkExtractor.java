/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.extractor;

import java.util.*;

import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.test.*;
import org.lockss.uiapi.util.Constants;

/**
 * @since 1.68
 */
public class TestXmlLinkExtractor extends LockssTestCase {

  /**
   * @since 1.68
   */
  protected XmlLinkExtractor xle;
  
  /**
   * @since 1.68
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.xle = new XmlLinkExtractor();
  }
  
  /**
   * @since 1.68
   */
  public void testXmlLinkExtractor() throws Exception {
    // Examples inspired by http://www.w3.org/TR/xml-stylesheet/#introduction
    String srcUrl = "http://www.example.com/path/to/base";
    expectNoUrls("<greeting>Hello world</greeting>",
                 srcUrl);
    expectNoUrls("<?xml version=\"1.0\"?>\n" +
                 "<greeting>Hello world</greeting>\n",
                 srcUrl);
    expectNoUrls("<!-- comment -->\n" +
                 "<!-- comment -->\n" +
                 "<greeting>Hello world</greeting>\n",
                 srcUrl);
    expectOneUrl("<?xml version=\"1.0\"?>\n" +
                 "<?xml-stylesheet href=\"foo.css\"?>\n" +
                 "<greeting>Hello world</greeting>\n",
                 srcUrl,
                 "http://www.example.com/path/to/foo.css");
    expectOneUrl("<?xml version=\"1.0\"?>\n" +
                 "<?xml-stylesheet href='foo.css'?>\n" +
                 "<greeting>Hello world</greeting>\n",
                 srcUrl,
                 "http://www.example.com/path/to/foo.css");
    expectOneUrl("<?xml version=\"1.0\"?>\n" +
                 "<?xml-stylesheet href \t\n\r=\r\n\t \"foo.css\"?>\n" +
                 "<greeting>Hello world</greeting>\n",
                 srcUrl,
                 "http://www.example.com/path/to/foo.css");
    expectOneUrl("<?xml version=\"1.0\"?>\n" +
                 "<?xml-stylesheet href=\"/foo.css\"?>\n" +
                 "<greeting>Hello world</greeting>\n",
                 srcUrl,
                 "http://www.example.com/foo.css");
    expectOneUrl("<?xml version=\"1.0\"?>\n" +
                 "<?xml-stylesheet href=\"misc/foo.css\"?>\n" +
                 "<greeting>Hello world</greeting>\n",
                 srcUrl,
                 "http://www.example.com/path/to/misc/foo.css");
    expectOneUrl("<?xml version=\"1.0\"?>\n" +
                 "<?xml-stylesheet href=\"http://www.example.net/other/foo.css\"?>\n" +
                 "<greeting>Hello world</greeting>\n",
                 srcUrl,
                 "http://www.example.net/other/foo.css");
    expectUrls("<?xml version=\"1.0\"?>\n" +
               "<?xml-stylesheet href=\"common.css\"?>\n" +
               "<?xml-stylesheet href=\"default.css\" title=\"Default style\"?>\n" +
               "<?xml-stylesheet alternate=\"yes\" href=\"alt.css\" title=\"Alternative style\"?>" +
               "<?xml-stylesheet href=\"single-col.css\" media=\"all and (max-width: 30em)\"?>\n" +
               "<greeting>Hello world</greeting>\n",
               srcUrl,
               Arrays.asList("http://www.example.com/path/to/common.css",
                             "http://www.example.com/path/to/default.css",
                             "http://www.example.com/path/to/alt.css",
                             "http://www.example.com/path/to/single-col.css"));
  }
  
  /**
   * @since 1.68
   */
  protected void expectNoUrls(String srcStr,
                              String srcUrl)
      throws Exception {
    expectUrls(srcStr, srcUrl, Collections.<String>emptyList());
  }
  
  /**
   * @since 1.68
   */
  protected void expectOneUrl(String srcStr,
                              String srcUrl,
                              String expectedLink)
      throws Exception {
    expectUrls(srcStr, srcUrl, Arrays.asList(expectedLink));
  }
  
  /**
   * @since 1.68
   */
  protected void expectUrls(String srcStr,
                            String srcUrl,
                            List<String> expectedLinks)
      throws Exception {
    /*
     * LOCAL INNER CLASS
     */
    class MyCallback implements Callback {
      
      protected List<String> urls;
      
      MyCallback() {
        this.urls = new ArrayList<String>();
      }
      
      @Override
      public void foundLink(String url) {
        urls.add(url);
      }
      
      public List<String> getUrls() {
        return urls;
      }
    }
    
    MyCallback mcb = new MyCallback();
    xle.extractUrls(null, new StringInputStream(srcStr), Constants.DEFAULT_ENCODING, srcUrl, mcb);
    assertEquals(expectedLinks, mcb.getUrls());
  }
  
}
