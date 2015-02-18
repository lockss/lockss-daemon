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

package org.lockss.plugin.massachusettsmedicalsociety;

import java.io.*;

import org.htmlparser.filters.TagNameFilter;
import org.lockss.util.*;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.test.*;

public class TestMassachusettsMedicalSocietyHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private MassachusettsMedicalSocietyHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new MassachusettsMedicalSocietyHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  String test1[] = {
		  "<html>     <div id = \"related\">abc</div></html>",
		  "<html>  <div id = \"trendsBox\">abc</div></html>",
	      "<html>   <div id = \"topAdBar\">abc</div></html>",
	      "<html>  <div class = \"Topbanner CM8\">abc</div></html>",
	      "<html>   <div class = \"audioTitle\">abc</div></html>",
	      "<html>  <div class = \"topLeftAniv\">abc</div></html>",
	      "<html>          <div class = \"ad\">abc</div></html>",
	      "<html> <div id = \"rightRailAd\">abc</div></html>",
	      "<html>  <div class = \"rightAd\">abc</div></html>",
	      "<html><div id = \"rightAd\">abc</div>  </html>",
	      "<html><div class = \"toolsAd\">abc</div>                </html>",
	      "<html><div id = \"bottomAdBar\">abc</div>       </html>",
	      "<html> <div class = \"bottomAd\">abc</div> </html>",
	      "<html>  <div class = \"bannerAdTower\">abc</div>        </html>",
	      "<html>  <dd id = \"comments\">abc</dd>  </html>",
	      "<html> <dd id = \"letters\">abc</dd>    </html>",
	      "<html>  <dd id = \"citedby\">abc</dd> </html>",
	      "<html> <div class = \"articleCorrection\">abc</div>     </html>",
  };
  String test2[] = {
	      "<html><div id = \"galleryContent\">abc</div></html>",
	      "<html><div class = \"discussion\">abc</div></html>",
	      "<html><dt id = \"citedbyTab\">abc</dt></html>",
	      "<html><div class = \"articleActivity\">abc</div></html>",
	      "<html><div id = \"institutionBox\">abc</div></html>",
	      "<html><div id = \"copyright\">abc</div></html>",
	      "<html><a class = \"issueArchive-recentIssue\">abc</a></html>",
	      "<html><div id = \"moreIn\">abc</div></html>"		  
  };
  
  String test3[] = {
  " <html><div id = \"layerPlayer\">abc</div></html>    ",
  "  <html><li class = \"submitLetter\">abc</div></html>        ",
  "     <html><p class = \"openUntilInfo\">abc</div></html>  ",
  "     <html><div class = \"poll\">abc</div></html>            ",
  "             <html><meta name=\"evt-ageContent\" content=\"Last-6-Months\" /></html> ",
  "  <html><div class=\"emailAlert\"></div></html>  ",
  " <html><div class=\"&#xA; jcarousel-skin-audio carousel-type-audiosummary\"><h3>More Weekly Audio Summaries (8)</h3></html> ",
  "     <html><div id=\"toolsBox\"><h3> Tools </h3></div></html>    ",
  " <html><a href=\"/servlet/linkout?suffix=r001&amp;dbid=16384&amp;url=http%3A%2F%2Fsfx.stanford.edu%2Flocal%3Fsid%3Dmms%26genre%3D%26aulast%3DSampson%26aufirst%3DHA%26aulast%3DMunoz-Furlong%26aufirst%3DA%26aulast%3DCampbell%26aufirst%3DRL%26atitle%3DSecond%2Bsymposium%2Bon%2Bthe%2Bdefinition%2Band%2Bmanagement%2Bof%2Banaphylaxis%253A%2Bsummary%2Breport%2B%252D%252D%2BSecond%2BNational%2BInstitute%2Bof%2BAllergy%2Band%2BInfectious%2BDisease%252FFood%2BAllergy%2Band%2BAnaphylaxis%2BNetwork%2BSymposium.%26stitle%3DJ%2BAllergy%2BClin%2BImmunol%26date%3D2006%26volume%3D117%26spage%3D391%26id%3Ddoi%3A10.1016%252Fj.jaci.2005.12.1303\" title=\"OpenURL STANFORD UNIVERSITY\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/templates/jsp/images/sfxbutton.gif\" alt=\"OpenURL STANFORD UNIVERSITY\"></a></html>  "
  };

  public void testFiltering() throws Exception {
    InputStream in;
    
    for (String t : test1){
    	in = fact.createFilteredInputStream(mau, new StringInputStream(t),
    	        ENC);
    	String str = StringUtil.fromInputStream(in);
    	assertEquals("<html> </html>", str);
    }
    for (String t : test2){
      in = fact.createFilteredInputStream(mau, new StringInputStream(t),
              ENC);
      String str = StringUtil.fromInputStream(in);
      assertEquals("<html></html>", str);
    }
    for (String t : test3){
      in = fact.createFilteredInputStream(mau, new StringInputStream(t),
              ENC);
      String str = StringUtil.fromInputStream(in);
      assertEquals(" <html></html> ", str);
    }
  }
}