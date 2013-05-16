/*
 * $Id: TestNRCResearchPressHtmlHashFilterFactory.java,v 1.1 2013-04-19 22:49:44 alexandraohlson Exp $
 */

/* Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University, all rights reserved.

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
package org.lockss.plugin.atypon.nrcresearchpress;


import java.io.InputStream;
import org.lockss.test.*;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class TestNRCResearchPressHtmlHashFilterFactory extends LockssTestCase{
  private ClockssNRCResearchPressHtmlHashFilterFactory filt;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    filt = new ClockssNRCResearchPressHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  // testing removal of the institution-banner-text line and the stuff within <script> </script>
  String testContent1 =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
    "<html>\n" +
    "<head>\n" +        
    "<div class=\"institutionBanner\" >Subscriber access provided by STANFORD UNIV. </div>" +
    "\n</head>\n" +
    "</html>";
  String resultingContent1 =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
    "<html>\n" +
    "<head>\n" +
    "\n</head>\n" +
    "</html>";

  String testContent2 =
    "<script type=\"text/javascript\">"+
    "<!-- // hide it from old browsers" +
    "var prefix3 = \"http%3A%2F%2Flibrary.stanford.edu%2Fsfx\";" +
    "function genSfxLink3(id, url, doi) {" +
    "   var href = \"javascript:popSfxLink(prefix3,'\"+id+\"','\"+url+\"','\"+doi+\"')\""+
    "   var name =  \"null\";"+
    "    if( name == null || name == \"\" || name == \"null\") name = \"OpenURL STANFORD UNIVERSITY\";"+
    "   var height = 14;"+
    "   var width = 72;"+
    "   document.write('<a href=\"'+href+'\" title=\"'+name+'\">');"+
    "       document.write('<img src=\"/userimages/57853/sfxbutton\" alt=\"'+name+'\" border=\"0\" valign=\"bottom\" height=\"' + height + '\" width=\"' + width + '\" />');"+
    "       document.write('</a>');"+
    "}"+
    "// stop hiding -->"+
    "</script>Hello";
  String resultingContent2 =
    "Hello";
  
  String testContent3 =
    "<a href=\"/servlet/linkout?suffix=CIT0001_1&amp;dbid=16384&amp;doi=10.5558/tfc2013-035&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dnrc%26aulast%3DBarth%25C3%25A8s%26aufirst%3DB.%26atitle%3DAggregate%2Bstability%2Bas%2Bindicator%2Bof%2Bsoil%2Bsusceptibility%2Bto%2Brunoff%2Band%2Berosion%253A%2Bvalidation%2Bat%2Bseveral%2Blevels%26stitle%3DCatena%26date%3D2002%26volume%3D47%26spage%3D133%26id%3Ddoi%3A10.1016%252FS0341-8162%252801%252900180-1\" title=\"OpenURL STANFORD UNIVERSITY\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/57853/sfxbutton\" alt=\"OpenURL STANFORD UNIVERSITY\" />"+
    "</a>World";
  String resultingContent3 = "World";

  public void testFilter() throws Exception {
    InputStream in1, in2, in3;

    in1 = filt.createFilteredInputStream(mau,
        new StringInputStream(testContent1), Constants.DEFAULT_ENCODING);
    in2 = filt.createFilteredInputStream(mau,
        new StringInputStream(testContent2), Constants.DEFAULT_ENCODING);
    in3 = filt.createFilteredInputStream(mau,
        new StringInputStream(testContent3), Constants.DEFAULT_ENCODING);

    assertEquals( resultingContent1, StringUtil.fromInputStream(in1));
    assertEquals( resultingContent2, StringUtil.fromInputStream(in2));
    assertEquals( resultingContent3, StringUtil.fromInputStream(in3));

  }     

}

