/*
 * $Id: TestSpringerApiPamLinkExtractor.java 40407 2015-03-11 01:28:09Z thib_gc $
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

package org.lockss.plugin.springer.link;

import java.io.IOException;
import java.util.*;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.*;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.springer.link.SpringerLinkPamLinkExtractor;
import org.lockss.test.*;

public class TestSpringerLinkPamLinkExtractor extends LockssTestCase {
  
  public void testGoodInput() throws Exception {
    String input =
        "<?xml-stylesheet type=\"text/xsl\" href=\"/resources/spdi-versioned-pam.xsl\"?>\n" +
        "<response>\n" +
        "  <query>issn:1234-567X volume:123</query>\n" +
        "  <result>\n" +
        "    <total>1</total>\n" +
        "    <start>1</start>\n" +
        "    <pageLength>100</pageLength>\n" +
        "    <recordsDisplayed>1</recordsDisplayed>\n" +
        "  </result>\n" +
        "  <records>\n" +
        "    <pam:message>\n" +
        "      <xhtml:head>\n" +
        "        <pam:article>\n" +
        "          <dc:identifier>doi:10.0000/s00125-014-3382-x</dc:identifier>\n" +
        "          <prism:url format=\"html\" platform=\"web\">http://www.example.com/openurl/fulltext?id=doi:10.0000/s00125-014-3382-x</prism:url>\n" +
        "          <prism:url format=\"pdf\" platform=\"web\">http://www.example.com/openurl/pdf?id=doi:10.0000/s00125-014-3382-x</prism:url>\n" +
        "          <dc:title>Chicken Chicken Chicken: Chicken Chicken Chicken Chicken Chicken</dc:title>\n" +
        "          <dc:creator>Chicken, C.</dc:creator>\n" +
        "          <prism:publicationName>Acta Chickenica</prism:publicationName>\n" +
        "          <prism:issn>1234-567X</prism:issn>\n" +
        "          <prism:genre>OriginalPaper</prism:genre>\n" +
        "          <journalId>111</journalId>\n" +
        "          <prism:volume>123</prism:volume>\n" +
        "          <prism:number>12</prism:number>\n" +
        "          <issueType>Regular</issueType>\n" +
        "          <topicalCollection />\n" +
        "          <prism:startingPage>2485</prism:startingPage>\n" +
        "          <openAccess>false</openAccess>\n" +
        "          <prism:doi>10.0000/s00125-014-3382-x</prism:doi>\n" +
        "          <dc:publisher>Chicken</dc:publisher>\n" +
        "          <prism:publicationDate>2014-12-01</prism:publicationDate>\n" +
        "          <prism:url>http://dx.doi.org/10.0000/s00125-014-3382-x</prism:url>\n" +
        "          <prism:copyright>©2014 Chicken</prism:copyright>\n" +
        "        </pam:article>\n" +
        "      </xhtml:head>\n" +
        "      <xhtml:body>\n" +
        "        <h1>Abstract</h1>\n" +
        "          <p>Aims/hypothesis</p>\n" +
        "          <p>Chicken chicken chicken chicken chicken.</p>\n" +
        "          <p>Methods</p>\n" +
        "          <p>Chicken chicken chicken chicken chicken chicken.</p>\n" +
        "          <p>Results</p>\n" +
        "          <p>Chicken chicken chicken chicken chicken chicken chicken.</p>\n" +
        "          <p>Conclusions/interpretation</p>\n" +
        "          <p>Chicken chicken chicken chicken chicken chicken chicken chicken.</p>\n" +
        "        </xhtml:body>\n" +
        "      </pam:message>\n" +
        "   </records>\n" +
        "</response>\n";
    
    SpringerLinkPamLinkExtractor ple = new SpringerLinkPamLinkExtractor();
    assertFalse(ple.isDone());
    List<String> out = doExtractUrls(ple, input);
    assertTrue(ple.isDone());
    assertEquals(100, ple.getPageLength());
    assertEquals(1, ple.getTotal());
    assertEquals(1, ple.getStart());
    assertTrue(out.size() != 0);
    assertIsomorphic(Arrays.asList("10.0000/s00125-014-3382-x"),
                     out);
  }
  
  public void testBadInput() throws Exception {
    String input =
        "<?xml-stylesheet type=\"text/xsl\" href=\"/resources/spdi-versioned-pam.xsl\"?>\n" +
        "<response>\n" +
        "  <query>issn:1234-567X volume:123</query>\n" +
        "  <result>\n" +
        "    <total>A</total>\n" +
        "    <start>B</start>\n" +
        "    <pageLength>C</pageLength>\n" +
        "    <recordsDisplayed>D</recordsDisplayed>\n" +
        "  </result>\n" +
        "</response>";
    
    SpringerLinkPamLinkExtractor ple = new SpringerLinkPamLinkExtractor();
    assertFalse(ple.isDone());
    try {
      List<String> out = doExtractUrls(ple, input);
      fail("Should have thrown IOException");
    }
    catch (IOException expected) {
      /*
       * This is not what we really want. Parsing the bad numbers should be the
       * cause of the exception, not the internal error subsequently encountered
       * by the results list having zero elements. This is part of the
       * boilerplate work to be done in XPathUtil.
       */
      assertMatchesRE("Internal error parsing results for http://api.example.com/", expected.getMessage());
    }
  }
  
  protected List<String> doExtractUrls(LinkExtractor le,
                                       String input)
      throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setConfiguration(ConfigurationUtil.fromArgs(
        ConfigParamDescr.BASE_URL.getKey(), "http://www.example.com/"));
    final List<String> out = new ArrayList<String>();
    le.extractUrls(mau,
                   new StringInputStream(input),
                   "UTF-8",
                   "http://api.example.com/meta/v1/pam?q=issn:1234-567X%20volume:123&p=100&s=1",
                   new Callback() {
                     @Override public void foundLink(String url) {
                       out.add(url);
                     }
                   });
    return out;
  }

}
