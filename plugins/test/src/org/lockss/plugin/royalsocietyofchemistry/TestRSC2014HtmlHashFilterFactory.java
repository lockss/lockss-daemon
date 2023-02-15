/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.royalsocietyofchemistry;

import java.io.InputStream;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestRSC2014HtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private RSC2014HtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new RSC2014HtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String withScript = "" +
      "<html>" +
      "  	<head>" +
      "<script type=\"text/javascript\">\n" + 
      "  var _gaq = _gaq || [];\n" + 
      "</script>" +
      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>" +
      "</head>\n" +
      "<script type=\"text/javascript\" src=\"/camcos/etc/cover.js\"></script>" +
      "</html>";
  
  private static final String withoutScript = " ";
  
  private static final String withStuff = "" +
      "<html lang=\"en\" xml:lang=\"en\" xmlns:rsc=\"urn:rsc.org\" xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\" xmlns:art=\"http://www.rsc.org/schema/rscart38\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:epub=\"http://www.idpf.org/2007/ops\">" +
      "<head><!--v5_4_9--></head>" +
      "<body>" +
      "<div class=\"header\">" +
      " <div class=\"state_version\"> Lite Version<br> </div>" +
      "</div> " +
      "<div class=\"footer\">\n" + 
      "        <div class=\"links\">\n" + 
      "            <a title=\"Login to RSC Publishing\" tabindex=\"5\" href=\"/en/account/logon?returnurl=http%3A%2F%2Fpubs.rsc.org%2Fen%2FContent%2FArticleLanding%2F2008%2FGC%2FB710041H\" accesskey=\"l\">Login</a> \n" + 
      "<span class=\"seperator\">|</span>\n" + 
      "        </div>\n" + 
      "    </div>" +
      "" +
      "</body>" +
      "</html>";
  
  private static final String withoutStuff = " ";
  
  private static final String withLinks = "" +
      "A. A, A. K, H. S, and R. A, Native changes in <span class=\"italic\">Bacillus subtilis</span>" +
      ", <span class=\"italic\">Jnl.</span>, year, <span class=\"bold\">2</span>(1), 80 87" +
      "<a target=\"_blank\" class=\"Links\" href=\"http://www.rsc.org/\" " +
      "title=\"Left in text\">Left</a>" +
      "<img src=\"http://sod-a.rsc-cdn.org/pubs-core/rel_ver/content/NewImages/pdf_icon_small.gif\">" +
      "<img src=\"https://sod-a.rsc-cdn.org/pubs-core/rel_ver/content/NewImages/pdf_icon_small.gif\">" +
      "<a target=\"_blank\" class=\"URLLinks\" href=\"http://pubs.rsc.org/\" " +
      "title=\"Link via OpenURL Resolver\"><img src=\"http://pubs.rsc.org/en\"></a>.";
  
  private static final String withoutLinks = "" +
      "A. A, A. K, H. S, and R. A, Native changes in Bacillus subtilis " +
      ", Jnl. , year, 2 (1), 80 87" +
      " Left .";
  
  private static final String withNav = "" +
      "<html>\n" +
      " <div id=\"top\" class=\"navigation\" style=\"color: rgb(0, 0, 0);\">\n" + 
      "  <h1>\nGap in\n</h1>\n" + 
      "  <div class=\"open_access\">\n</div>" +
      " </div>" +
      "</html>";
  
  private static final String withoutNav = " ";
  
  private static final String genError = "" +
      "<html><body>" +
      "<span id=\"1\" />" +
      "</body></html>";
  
  private static final String noError = " " +
      "<html><body>" +
      "<span/>" +
      "</body></html>";
  
  
  public void testFiltering() throws Exception {
    try {
      assertFilterToSame(genError, noError);
      fail("Didn't throw Exception");
    } catch (Exception e) {
    }
    assertFilterToSame(withScript, withoutScript);
    assertFilterToSame(withStuff, withoutStuff);
    assertFilterToSame(withLinks, withoutLinks);
    assertFilterToSame(withNav, withoutNav);
  }
  
  private void assertFilterToSame(String str1, String str2) throws Exception {
    InputStream inA = fact.createFilteredInputStream(mau, new StringInputStream(str1),
        Constants.DEFAULT_ENCODING);
    InputStream inB = fact.createFilteredInputStream(mau, new StringInputStream(str2),
        Constants.DEFAULT_ENCODING);
    String a = StringUtil.fromInputStream(inA);
    String b = StringUtil.fromInputStream(inB);
    assertEquals(b, a);
    assertEquals(str2, a);
  }
  
}
