/*
 * $Id: TestProjectMuseLinkExtractor.java 40407 2015-03-11 01:28:09Z thib_gc $
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

package org.lockss.plugin.projmuse;

import java.util.*;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.*;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.test.*;

public class TestProjectMuseLinkExtractor extends LockssTestCase {
  
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
        "<meta name=\"citation_fulltext_html_url\" content=\"https://muse.jhu.edu/journals/american_imago/v007/7.11s.html\">\n" + 
        "<meta name=\"citation_issn\" content=\"1534-7311\">\n" + 
        "</head>\n" + 
        "<body>  \n" + 
        "<table width=\"800\" border=\"0\">\n" + 
        "  <tr> \n" + 
        "    <td valign=\"top\"><img src=\"https://muse.jhu.edu/images/journals/banners/asr/logo.gif\" width=\"141\" height=\"251\" align=\"right\"></td>\n" + 
        "    <td valign=\"top\" align=\"center\"> \n" + 
        "    </td>\n" +
        "  </tr>\n" + 
        "  <tr> \n" + 
        "    <td valign=\"top\">\n" + 
        "     </td>\n" + 
        "    <td valign=\"top\">\n" + 
        "        <table width=\"550\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" align=\"center\">\n" + 
        "    <tr>\n" + 
        "          <td class=\"content\"> \n" + 
        "            <!--TITLE-->\n" + 
        "           <p class=\"style1\">An Anthropology</p>\n" + 
        "            <p><a href=\"#S\" name=\"author\">M S</a></p><br />\n" + 
        "            <p>in the store.<a name=\"NOTE1\" href=\"#FOOT1\"><sup>1</sup></a></p>\n" + 
        " <hr>\n" + 
        "<hr>\n" + 
        "<BR>\n" + 
        "<IMG SRC=\"/images/journals/navigational/asr/nav_bottom.gif\" WIDTH=\"480\" HEIGHT=\"16\" USEMAP=\"#nav_bottom\" ALIGN=\"BOTTOM\" NATURALSIZEFLAG=\"3\" BORDER=\"0\" ISMAP>\n" + 
        "\n" + 
        "        </td>\n" + 
        "        </tr>\n" + 
        "        </table>\n" + 
        "</td>\n" + 
        "</tr>\n" + 
        "</table>\n" + 
        "</body></html>";
    
    ProjectMuseHtmlLinkExtractorFactory plef = new ProjectMuseHtmlLinkExtractorFactory();
    LinkExtractor ple = plef.createLinkExtractor("any");
    List<String> out = doExtractUrls(ple, input);
    assertTrue(out.size() != 0);
    assertIsomorphic(Arrays.asList("http://muse.jhu.edu/images/journals/banners/asr/logo.gif",
                                   "http://muse.jhu.edu/journals/american_imago/v007/7.11s.html#S",
                                   "http://muse.jhu.edu/journals/american_imago/v007/7.11s.html#FOOT1",
                                   "http://muse.jhu.edu/images/journals/navigational/asr/nav_bottom.gif"),
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
    
    ProjectMuseHtmlLinkExtractorFactory plef = new ProjectMuseHtmlLinkExtractorFactory();
    LinkExtractor ple = plef.createLinkExtractor("mimetype");
    try {
      List<String> out = doExtractUrls(ple, input);
      assertTrue(out.size() == 0);
    }
    catch (Exception unexpected) {
      fail("Exception ");
    }
  }
  
  protected List<String> doExtractUrls(LinkExtractor le,
                                       String input)
      throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setConfiguration(ConfigurationUtil.fromArgs(
        ConfigParamDescr.BASE_URL.getKey(), "http://muse.jhu.edu/"));
    final List<String> out = new ArrayList<String>();
    le.extractUrls(mau,
                   new StringInputStream(input),
                   "UTF-8",
                   "http://muse.jhu.edu/journals/american_imago/v007/7.11s.html",
                   new Callback() {
                     @Override public void foundLink(String url) {
                       out.add(url);
                     }
                   });
    return out;
  }

}
