/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.highwire;

import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.*;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.*;

public class TestHighWireJCoreHtmlLinkExtractor extends LockssTestCase {
  
  // private static final MockArchivalUnit mau = new MockArchivalUnit();
  private static final String BASE_URL1 = "http://www.example.com/";
  private static final String BASE_URL2 = "https://www.example.com/";
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;
  DefinableArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    plugin = new DefinablePlugin();
    plugin.initPlugin(theDaemon,
        "org.lockss.plugin.highwire.HighWireJCorePlugin");
    
  }
  
  private DefinableArchivalUnit makeAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return (DefinableArchivalUnit)plugin.configureAu(config, null);
  }
  
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
        "    <td><a href=\"" + BASE_URL1 + "content/os-86/1_suppl_2/103.2\">HTML</a></td>\n" + 
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
    HighWireJCoreHtmlLinkExtractorFactory hlef = new HighWireJCoreHtmlLinkExtractorFactory();
    LinkExtractor ple = hlef.createLinkExtractor("any");
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL1);
    props.setProperty(ConfigParamDescr.VOLUME_NAME.getKey(), "123");
    mau = makeAuFromProps(props);
    List<String> out = doExtractUrls1(ple, input, mau);
    assertTrue(out.size() != 0);
    assertIsomorphic(Arrays.asList(BASE_URL1 + "images/journals/banners/asr/logo.gif",
//                                   BASE_URL1 + "content/123/789/100.1.full.pdf",
                                   BASE_URL1 + "content/123/789/100.1",
                                   BASE_URL1 + "content/123/abc/14.pdf",
//                                   BASE_URL1 + "content/os-86/1_suppl_2/103.2.full.pdf",
                                   BASE_URL1 + "content/os-86/1_suppl_2/103.2",
//                                   BASE_URL1 + "content/os-86/12/e03.full.pdf",
                                   BASE_URL1 + "content/os-86/12/e03",
//                                   BASE_URL1 + "content/123/bmj.f4270.full.pdf",
                                   BASE_URL1 + "content/123/bmj.f4270",
                                   BASE_URL1 + "content/123/789"),
                     out);
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL1);
    props.setProperty(ConfigParamDescr.VOLUME_NAME.getKey(), "os-86");
    mau = makeAuFromProps(props);
    out = doExtractUrls2(ple, input, mau);
    assertTrue(out.size() != 0);
    assertIsomorphic(Arrays.asList(BASE_URL1 + "images/journals/banners/asr/logo.gif",
//                                   BASE_URL1 + "content/123/789/100.1.full.pdf",
                                   BASE_URL1 + "content/123/789/100.1",
                                   BASE_URL1 + "content/123/abc/14.pdf",
//                                   BASE_URL1 + "content/os-86/1_suppl_2/103.2.full.pdf",
                                   BASE_URL1 + "content/os-86/1_suppl_2/103.2",
//                                   BASE_URL1 + "content/os-86/12/e03.full.pdf",
                                   BASE_URL1 + "content/os-86/12/e03",
//                                   BASE_URL1 + "content/123/bmj.f4270.full.pdf",
                                   BASE_URL1 + "content/123/bmj.f4270"),
        out);
    
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL2);
    props.setProperty(ConfigParamDescr.VOLUME_NAME.getKey(), "123");
    mau = makeAuFromProps(props);
    out = doExtractUrls2(ple, input, mau);
    assertTrue(out.size() != 0);
    assertIsomorphic(Arrays.asList(BASE_URL2 + "images/journals/banners/asr/logo.gif",
//                                   BASE_URL2 + "content/123/789/100.1.full.pdf",
                                   BASE_URL2 + "content/123/789/100.1",
                                   BASE_URL2 + "content/123/abc/14.pdf",
//                                   BASE_URL2 + "content/os-86/1_suppl_2/103.2.full.pdf",
                                   BASE_URL2 + "content/os-86/1_suppl_2/103.2",
//                                   BASE_URL2 + "content/os-86/12/e03.full.pdf",
                                   BASE_URL2 + "content/os-86/12/e03",
//                                   BASE_URL2 + "content/123/bmj.f4270.full.pdf",
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
    
    HighWireJCoreHtmlLinkExtractorFactory hlef = new HighWireJCoreHtmlLinkExtractorFactory();
    LinkExtractor ple = hlef.createLinkExtractor("mimetype");
    Properties props = new Properties();
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL1);
    props.setProperty(ConfigParamDescr.VOLUME_NAME.getKey(), "123");
    mau = makeAuFromProps(props);
    
    try {
      List<String> out = doExtractUrls2(ple, input, mau);
      assertTrue(out.size() == 0);
    }
    catch (Exception unexpected) {
      fail("Exception ");
    }
    props.setProperty(ConfigParamDescr.BASE_URL.getKey(), BASE_URL2);
    mau = makeAuFromProps(props);
    List<String> out = doExtractUrls2(ple, input, mau);
    assertTrue(out.size() == 0);
  }
  
  protected List<String> doExtractUrls1(LinkExtractor le,
                                        String input,
                                        DefinableArchivalUnit mau)
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
                                        DefinableArchivalUnit mau)
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
