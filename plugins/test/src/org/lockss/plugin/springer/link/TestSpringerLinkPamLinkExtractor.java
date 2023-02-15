/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.springer.link;

import java.io.IOException;
import java.util.*;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.*;
import org.lockss.extractor.LinkExtractor.Callback;
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
