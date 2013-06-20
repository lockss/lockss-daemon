/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"}, to deal
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

package org.lockss.plugin.ingenta;

import java.io.*;

import org.lockss.util.*;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.test.*;

/**
 * This class tests the IngentaJouranlPluginHtmlFilterFactory.
 * @author phil
 *
 */
public class TestIngentaJournalHtmlFilterFactory extends LockssTestCase {
  private IngentaJournalHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new IngentaJournalHtmlFilterFactory();
  }

  // block tags from IngentaJouranlHtmlFilterFactory
  String blockIds[][] = new String[][] {
    // filter out <div class="heading"> that encloses a statement with
    // the number of references and the number that can be referenced: 
    // number of reference links won't be the same because not all 
    // the referenced articles are available at a given institution.
    {"div", "class", "heading"},
    // filter out <li class="data"> that encloses a reference for the
    // article: reference links won't be the same because not all 
    // the referenced articles are available at a given institution.
    {"li", "class", "data"},
    // institution-specific subscription link section
    {"div", "id", "subscribe-links"},
    // Filter out <div id="links">...</div>
    {"div", "id", "links"},
    // Filter out <div id="footer">...</div>
    {"div", "id", "footer"},
    // Filter out <div id="top-ad-alignment">...</div>
    {"div", "id", "top-ad-alignment"},
    // Filter out <div id="top-ad">...</div>
    {"div", "id", "top-ad"},
    // Filter out <div id="ident">...</div>
    {"div", "id", "ident"},         
    // Filter out <div id="ad">...</div>
    {"div", "id", "ad"},
    // Filter out <div id="vertical-ad">...</div>
    {"div", "id", "vertical-ad"},      
    // institution-specific subscription link section
    {"div", "id", "subscribe-links"},
    // Filter out <div id="links">...</div>
    {"div", "id", "links"},
    // Filter out <div id="footer">...</div>
    {"div", "id", "footer"},
    // Filter out <div id="top-ad-alignment">...</div>
    {"div", "id", "top-ad-alignment"},
    // Filter out <div id="top-ad">...</div>
    {"div", "id", "top-ad"},
    // Filter out <div id="ident">...</div>
    {"div", "id", "ident"},         
    // Filter out <div id="ad">...</div>
    {"div", "id", "ad"},
    // Filter out <div id="vertical-ad">...</div>
    {"div", "id", "vertical-ad"},
    // Filter out <div class="right-col-download">...</div>
    {"div", "class", "right-col-download"},                                                               
    // Filter out <div id="cart-navbar">...</div>
    {"div", "id", "cart-navbar"},   
    //   // Filter out <div class="heading-macfix article-access-options">...</div>
    //  {"div", "class", "heading-macfix article-access-options"},                                                                           
    // Filter out <div id="baynote-recommendations">...</div>
    {"div", "id", "baynote-recommendations"},
    // Filter out <div id="bookmarks-container">...</div>
    {"div", "id", "bookmarks-container"},   
    // Filter out <div id="llb">...</div>
    {"div", "id", "llb"},   
    // Filter out <a href="...">...</a> where the href value includes "exitTargetId" as a parameter
    {"a", "href", "foo?exitTargetId=bar"},
    {"a", "href", "foo?parm=value&exitTargetId=bar"},
    // Icon on article reference page
    {"span", "class", "access-icon"},
  };
  
  // single tags from IngentaJouranlHtmlFilterFactory
  String[][] tagIds = new String[][] {
    // filter out <link rel="stylesheet" href="..."> because Ingenta has
    // bad habit of adding a version number to the CSS file name
    {"link", "rel", "stylesheet"},
    // Filter out <input name="exitTargetId">
    {"input", "name", "exitTargetId"},
  };
  
  public void testTagFiltering() throws Exception {
    // common filtered html results
    String filteredHtml =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"> "
            + "<html lang=\"en\"> <head> <body> "
            + "</body> </html> ";
    
    // html for block tags
    String blockHtml =
      "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n"
          + "<html lang=\"en\">\n<head>\n<body>\n"
          + "<%s %s=\"%s\">\n"
          + "chicken chicken chicken...\n"
          + "</%s>\n"
          + "</body>\n</html>\n\n\n";

    // test block tag ID filtering
    for (String[] id : blockIds) {
      InputStream htmlIn = fact.createFilteredInputStream(mau,
          new StringInputStream(String.format(blockHtml, id[0],id[1],id[2],id[0])),
          Constants.DEFAULT_ENCODING);
      assertEquals(filteredHtml, StringUtil.fromInputStream(htmlIn));
    }

    // html for single tags
    String tagHtml =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n"
            + "<html lang=\"en\">\n<head>\n<body>\n"
            + "<%s %s=\"%s\">\n"
            + "</body>\n</html>\n\n\n";

    // test single tag ID filtering
    for (String[] id : tagIds) {
      InputStream htmlIn = fact.createFilteredInputStream(mau,
          new StringInputStream(String.format(tagHtml, id[0],id[1],id[2])),
          Constants.DEFAULT_ENCODING);
      assertEquals(filteredHtml, StringUtil.fromInputStream(htmlIn));
    }
  }
  
    //test some straight html strings with explicit tests
    private static final String expireChecksumHtml =
        "<p>The chickens were decidedly cold.</p>" +
            "<p xmlns:f=\"http://www.example.org/functions\">" +
            "<a name=\"g002\"></a>" +
            "<div class=\"figure\">" +
            "<div class=\"image\">" +
            "<a class=\"table-popup\" href=\"javascript:popupImage('s4-ft1401-0065-g002.gif.html?expires=1355962274&id=72080579&titleid=6312&accname=Stanford+University&checksum=FA59636C74DD0E40E92BA6EFECB866E7')\">" +
            "<img alt=\"Figure 1\" border=\"0\" src=\"s4-ft1401-0065-g002_thmb.gif\">" +
           "<p>Figure 1<br>Click to view</p>" +
           "</a>" +
            "</div>" +
            "<div class=\"caption\">" +
            "<span class=\"captionLabel\"><span class=\"label\">The Chickens.</span></span>" +
            "</div>";
    

// NOTE - the two following lines below:
//   "<p>Figure 1<br>Click to view</p>" +
//   "</a>" +  
// need to be included in the filtered result because 
// the <a> tag hashing will stop when it finds an embedded composite tag,
// (in this case, <p>).  Once we update the daemon to allow modifying this in a subclass
// then we'll need to make the change for this plugin & update this test.
// HOWEVER - this still solves the hash problems since the item to remove is
// before the <p> tag
//
    private static final String expireChecksumFiltered =
        "<p>The chickens were decidedly cold.</p>" +
            "<p xmlns:f=\"http://www.example.org/functions\">" +
            "<a name=\"g002\"></a>" +
            "<div class=\"figure\">" +
            "<div class=\"image\">" +            
            "<p>Figure 1<br>Click to view</p>" +
            "</a>" +            
            "</div>" +
            "<div class=\"caption\">" +
            "<span class=\"captionLabel\"><span class=\"label\">The Chickens.</span></span>" +
            "</div>";
    
    
    public void testFiltering() throws Exception {
        InputStream inA;  
       
        inA = fact.createFilteredInputStream(mau, new StringInputStream(expireChecksumHtml),
            Constants.DEFAULT_ENCODING);

        assertEquals(expireChecksumFiltered,StringUtil.fromInputStream(inA));
      
    }
    
    private static final String onClickExitTargetID =
        "<div class=\"left-col-download\">View now:</div>" +
            "<div class=\"right-col-download contain\">" +
            "<span class=\"orangebutton\">" +
            "<span class=\"orangeleftside icbutton\">" +
            "<a onclick=\"javascript:popup('/search/download?pub=infobike%3a%2f%2flse%2fjtep%2f2001%2f00000035%2f00000001%2fart00001&mimetype=" +
            "application%2fpdf&exitTargetId=1371686277240','dowloadWindow','900','800')\" title=\"PDF download of Editorial\" class=\"no-underl" +
            "ine contain\" >PDF" +
            "</a></span></span>" +
            "</div>";
    private static final String onClickExitTargetIDFiltered =
        "<div class=\"left-col-download\">View now:</div>" +
            "<div class=\"right-col-download contain\">" +
            "<span class=\"orangebutton\">" +
            "<span class=\"orangeleftside icbutton\">" +
            "</span></span>" +
            "</div>";
    
    public void testonClickFiltering() throws Exception {
      InputStream inA;  
     
      inA = fact.createFilteredInputStream(mau, new StringInputStream(onClickExitTargetID),
          Constants.DEFAULT_ENCODING);

      assertEquals(onClickExitTargetIDFiltered,StringUtil.fromInputStream(inA));
    
  }

}
