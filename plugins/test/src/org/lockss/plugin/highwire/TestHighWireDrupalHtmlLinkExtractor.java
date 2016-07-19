/*
 * $Id: $
 */

/*

Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.util.*;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.*;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.test.*;

public class TestHighWireDrupalHtmlLinkExtractor extends LockssTestCase {
  
  private static final MockArchivalUnit mau = new MockArchivalUnit();
  private static final String BASE_URL1 = "http://www.example.com/";
  private static final String BASE_URL2 = "https://www.example.com/";
  
  public void testGoodInput() throws Exception {
    String input =
        "<HTML>\n" + 
        "<head>\n" + 
        "<title>An Anthropology| Advertising &amp; Society Review 7:1</title>\n" + 
        "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">\n" + 
        "<meta name=\"robots\" content=\"noarchive\">\n" + 
        "\n" + 
        "<meta name=\"citation_journal_title\" content=\"Advertising & Society Review\">\n" + 
        "<meta name=\"citation_volume\" content=\"7\">\n" + 
        "<meta name=\"citation_issue\" content=\"1\">\n" + 
        "<meta name=\"citation_fulltext_html_url\" content=\"" + BASE_URL1 + "journals/american_imago/v007/7.11s.html\">\n" + 
        "<meta name=\"citation_issn\" content=\"1534-7311\">\n" + 
        "</head>\n" + 
        "<body>  \n" + 
        "<table width=\"800\" border=\"0\">\n" + 
        "  <tr> \n" + 
        "    <td valign=\"top\"><img src=\"" + BASE_URL2 + "images/journals/banners/asr/logo.gif\" width=\"141\" height=\"251\" align=\"right\"></td>\n" + 
        "    <td valign=\"top\" align=\"center\"> \n" + 
        "    </td>\n" +
        "    <td><a href=\"" + BASE_URL1 + "content/123/789/100.1\">HTML</a></td>\n" + 
        "    <td><a href=\"" + BASE_URL2 + "content/123/abc/14.pdf\">PDF</a></td>\n" + 
        "    <td><a href=\"" + BASE_URL1 + "content/os-86/1_suppl_2/103\">HTML</a></td>\n" + 
        "    <td><a href=\"" + BASE_URL2 + "content/os-86/12/e03\">HTML</a></td>\n" + 
        "    <td><a href=\"" + BASE_URL1 + "content/123/bmj.f4270\">HTML</a></td>\n" + 
        "    <td><a href=\"" + BASE_URL2 + "content/95/1\">ISSUE</a></td>\n" + 
        "    <td><a href=\"" + BASE_URL1 + "content/95/1.toc\">ISSUE</a></td>\n" + 
        "    <td><a href=\"" + BASE_URL2 + "content/123/789\">ISSUE</a></td>\n" + 
        "  </tr>\n" + 
        "</table>\n" + 
        "</body></html>";
    // content/os-86/1_suppl_2/103
    // content/95/1
    HighWireDrupalHtmlLinkExtractorFactory hlef = new HighWireDrupalHtmlLinkExtractorFactory();
    LinkExtractor ple = hlef.createLinkExtractor("any");
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL1);
    props.setProperty(ConfigParamDescr.VOLUME_NAME.getKey(), "123");
    mau.setConfiguration(ConfigurationUtil.fromProps(props));
    List<String> out = doExtractUrls1(ple, input, mau);
    assertTrue(out.size() != 0);
    assertIsomorphic(Arrays.asList(BASE_URL1 + "images/journals/banners/asr/logo.gif",
                                   BASE_URL1 + "content/123/789/100.1.full.pdf",
                                   BASE_URL1 + "content/123/789/100.1",
                                   BASE_URL1 + "content/123/abc/14.pdf",
                                   BASE_URL1 + "content/os-86/1_suppl_2/103.full.pdf",
                                   BASE_URL1 + "content/os-86/1_suppl_2/103",
                                   BASE_URL1 + "content/os-86/12/e03.full.pdf",
                                   BASE_URL1 + "content/os-86/12/e03",
                                   BASE_URL1 + "content/123/bmj.f4270.full.pdf",
                                   BASE_URL1 + "content/123/bmj.f4270",
                                   BASE_URL1 + "content/123/789"),
                     out);
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL1);
    props.setProperty(ConfigParamDescr.VOLUME_NAME.getKey(), "os-86");
    mau.setConfiguration(ConfigurationUtil.fromProps(props));
    out = doExtractUrls2(ple, input, mau);
    assertTrue(out.size() != 0);
    assertIsomorphic(Arrays.asList(BASE_URL1 + "images/journals/banners/asr/logo.gif",
                                   BASE_URL1 + "content/123/789/100.1.full.pdf",
                                   BASE_URL1 + "content/123/789/100.1",
                                   BASE_URL1 + "content/123/abc/14.pdf",
                                   BASE_URL1 + "content/os-86/1_suppl_2/103.full.pdf",
                                   BASE_URL1 + "content/os-86/1_suppl_2/103",
                                   BASE_URL1 + "content/os-86/12/e03.full.pdf",
                                   BASE_URL1 + "content/os-86/12/e03",
                                   BASE_URL1 + "content/123/bmj.f4270.full.pdf",
                                   BASE_URL1 + "content/123/bmj.f4270"),
        out);
    
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL2);
    props.setProperty(ConfigParamDescr.VOLUME_NAME.getKey(), "123");
    mau.setConfiguration(ConfigurationUtil.fromProps(props));
    out = doExtractUrls2(ple, input, mau);
    assertTrue(out.size() != 0);
    assertIsomorphic(Arrays.asList(BASE_URL2 + "images/journals/banners/asr/logo.gif",
                                   BASE_URL2 + "content/123/789/100.1.full.pdf",
                                   BASE_URL2 + "content/123/789/100.1",
                                   BASE_URL2 + "content/123/abc/14.pdf",
                                   BASE_URL2 + "content/os-86/1_suppl_2/103.full.pdf",
                                   BASE_URL2 + "content/os-86/1_suppl_2/103",
                                   BASE_URL2 + "content/os-86/12/e03.full.pdf",
                                   BASE_URL2 + "content/os-86/12/e03",
                                   BASE_URL2 + "content/123/bmj.f4270.full.pdf",
                                   BASE_URL2 + "content/123/bmj.f4270",
                                   BASE_URL2 + "content/123/789"),
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
    
    HighWireDrupalHtmlLinkExtractorFactory hlef = new HighWireDrupalHtmlLinkExtractorFactory();
    LinkExtractor ple = hlef.createLinkExtractor("mimetype");
    mau.setConfiguration(ConfigurationUtil.fromArgs(
        ConfigParamDescr.BASE_URL.getKey(), BASE_URL1));
    try {
      List<String> out = doExtractUrls2(ple, input, mau);
      assertTrue(out.size() == 0);
    }
    catch (Exception unexpected) {
      fail("Exception ");
    }
    mau.setConfiguration(ConfigurationUtil.fromArgs(
        ConfigParamDescr.BASE_URL.getKey(), BASE_URL2));
    List<String> out = doExtractUrls2(ple, input, mau);
    assertTrue(out.size() == 0);
  }
  
  protected List<String> doExtractUrls1(LinkExtractor le,
                                        String input,
                                        MockArchivalUnit mau)
      throws Exception {
    final List<String> out = new ArrayList<String>();
    le.extractUrls(mau,
                   new StringInputStream(input),
                   "UTF-8",
                   BASE_URL1 + "journals/american_imago/v007/7.11s.foo",
                   new Callback() {
                     @Override public void foundLink(String url) {
                       out.add(url);
                     }
                   });
    return out;
  }
  
  
  protected List<String> doExtractUrls2(LinkExtractor le,
                                        String input,
                                        MockArchivalUnit mau)
      throws Exception {
    final List<String> out = new ArrayList<String>();
    le.extractUrls(mau,
                   new StringInputStream(input),
                   "UTF-8",
                   BASE_URL2 + "journals/american_imago/v007/7.11s.foo",
                   new Callback() {
                     @Override public void foundLink(String url) {
                       out.add(url);
                     }
                   });
    return out;
  }

}
