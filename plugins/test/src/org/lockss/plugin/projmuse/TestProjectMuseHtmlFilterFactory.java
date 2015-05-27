/*
 * $Id$
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

package org.lockss.plugin.projmuse;

import java.io.*;
import java.net.URL;
import java.util.Properties;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.*;

public class TestProjectMuseHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private ProjectMuseHtmlHashFilterFactory fact;
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JRNL_KEY = ConfigParamDescr.JOURNAL_DIR.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();
  
  private MockLockssDaemon theDaemon;
  
  static final String HTTP_ROOT = "http://muse.jhu.edu/";
  static final String HTTPS_ROOT = "https://muse.jhu.edu/";
  static final String DIR = "american_imago";

  public void setUp() throws Exception {
    super.setUp();
    fact = new ProjectMuseHtmlHashFilterFactory();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }
  private DefinableArchivalUnit makeAu(URL baseUrl, String journalDir, int volume)
      throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
    if (baseUrl!=null) {
      props.setProperty(BASE_URL_KEY, baseUrl.toString());
    }
    if (journalDir!=null) {
      props.setProperty(JRNL_KEY, journalDir);
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon,"org.lockss.plugin.projmuse.ProjectMusePlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  private static final String frequentHtml =
          "<HTML>\n" + 
          "<head>\n" + 
          "<title>An Anthropology| Advertising &amp; Society Review 7:1</title>\n" + 
          "<script language=\"JavaScript\">\n" + 
          "<!--\n" + 
          "function MM_swapImgRestore() { //v3.0\n" + 
          "  var i,x,a=document.MM_sr; for(i=0;a&&i<a.length&&(x=a[i])&&x.oSrc;i++) x.src=x.oSrc;\n" + 
          "}\n" + 
          "//-->\n" + 
          "</script>\n" + 
          "\n" + 
          "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">\n" + 
          "<meta name=\"robots\" content=\"noarchive\">\n" + 
          "\n" + 
          "<!-- headmeta -->\n" + 
          "<meta name=\"citation_journal_title\" content=\"Advertising & Society Review\">\n" + 
          "<meta name=\"citation_volume\" content=\"7\">\n" + 
          "<meta name=\"citation_issue\" content=\"1\">\n" + 
          "<meta name=\"citation_fulltext_html_url\" content=\"https://muse.jhu.edu/journals/american_imago/v007/7.11s.html\">\n" + 
          "<meta name=\"citation_issn\" content=\"1534-7311\">\n" + 
          "<!-- /headmeta -->\n" + 
          "</head>\n" + 
          "<body>  \n" + 
          "<div class=\"header\">THIS CONTENT GOES</div>\n" +
          "<table width=\"800\" border=\"0\">\n" + 
          "  <tr> \n" + 
          "    <td valign=\"top\"><img src=\"http://muse.jhu.edu/images/journals/banners/asr/logo.gif\" width=\"141\" height=\"251\" align=\"right\"></td>\n" + 
          "    <td valign=\"top\" align=\"center\"> \n" + 
          "<div class=\"right_nav\">THIS CONTENT GOES</div>\n" +
          "    </td>\n" +
          "  </tr>\n" + 
          "  <tr> \n" + 
          "    <td valign=\"top\">\n" + 
          "     <a href=\"http://www.aef.com\"\" onMouseOut=\"MM_swapImgRestore()\"" +
          "     onMouseOver=\"MM_swapImage('Image12','','/images/journals/navigational/asr/nav_aef_logo_on.gif',1)\">" +
          "     <img name=\"Image12\" border=\"0\" src=\"/images/journals/navigational/asr/nav_aef_logo_off.gif\" width=\"142\"></a>\n" + 
          "     </td>\n" + 
          "    <td valign=\"top\">\n" + 
          "        <table width=\"550\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" align=\"center\">\n" + 
          "    <tr>\n" + 
          "          <td class=\"content\"> \n" + 
          "            <!--TITLE-->\n" + 
          "           <p class=\"style1\">An Anthropology</p>\n" + 
          "            <p><a href=\"#S\" name=\"author\">M S</a></p><br />\n" + 
          "            <p>in the store.<a name=\"NOTE1\" href=\"#FOOT1\"><sup>1</sup></a></p>\n" + 
          "            <hr>\n" + 
          "<div id=\"sidebar2\">THIS CONTENT GOES</div>\n" +
          "<!--copyright-->\n" + 
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
          "<div class=\"footer\">THIS CONTENT GOES</div>\n" +
          "<div class=\"legend\">THIS CONTENT STAYS</div>\n" +
          "</body></html>";
  // All html tags get removed by the projmuse filter rule after hash filtering
  private static final String frequentHtmlFiltered =
      "<HTML> " + 
      "<body> " + 
      "<table width=\"800\" border=\"0\"> " + 
      "<tr> " + 
      // http no change
      "<td valign=\"top\"><img src=\"http://muse.jhu.edu/images/journals/banners/asr/logo.gif\" width=\"141\" height=\"251\" align=\"right\"></td> " + 
      "<td valign=\"top\" align=\"center\"> " + 
      "</td> " +
      "</tr> " + 
      "<tr> " + 
      "<td valign=\"top\"> " + 
      "<a href=\"http://www.aef.com\"\" onMouseOut=\"MM_swapImgRestore()\"" +
      " onMouseOver=\"MM_swapImage('Image12','','/images/journals/navigational/asr/nav_aef_logo_on.gif',1)\"> " +
      "<img name=\"Image12\" border=\"0\" src=\"/images/journals/navigational/asr/nav_aef_logo_off.gif\" width=\"142\"></a> " + 
      "</td> " + 
      "<td valign=\"top\"> " + 
      "<table width=\"550\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" align=\"center\"> " + 
      "<tr> " + 
      "<td class=\"content\"> " + 
      "<p class=\"style1\">An Anthropology</p> " + 
      "<p><a href=\"#S\" name=\"author\">M S</a></p><br /> " + 
      "<p>in the store.<a name=\"NOTE1\" href=\"#FOOT1\"><sup>1</sup></a></p> " + 
      "<hr> " + 
      "<hr> " + 
      "<BR> " + 
      "<IMG SRC=\"/images/journals/navigational/asr/nav_bottom.gif\" WIDTH=\"480\" HEIGHT=\"16\" USEMAP=\"#nav_bottom\" ALIGN=\"BOTTOM\" NATURALSIZEFLAG=\"3\" BORDER=\"0\" ISMAP> " + 
      "</td> " + 
      "</tr> " + 
      "</table> " + 
      "</td> " + 
      "</tr> " + 
      "</table> " + 
      "<div class=\"legend\">THIS CONTENT STAYS</div> " +
      "</body></html>";
  
  public void testFiltering() throws Exception {
    InputStream inA;
    URL base = new URL(HTTP_ROOT);
    int volume = 60;
    ArchivalUnit au = makeAu(base, DIR, volume);
    
    // filters test (also http/s mods)
    inA = fact.createFilteredInputStream(au, new StringInputStream(frequentHtml), ENC);
    
    assertEquals(frequentHtmlFiltered,StringUtil.fromInputStream(inA));
  }
  
}