/*
 * $Id$
 */
package org.lockss.plugin.ojs2;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestOJS2HtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private OJS2HtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new OJS2HtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  private static final String withNavbar = "<p align=\"center\" class=\"ads\">"
    + "<a href=\"http://okfestival.org/\"><img src=\"/up/ads/ad2.gif\" /></a></p>"
    + "<div id=\"navbar\">"
    + "<ul class=\"menu\">"
    + "<li id=\"home\"><a href=\"http://www.ancient-asia-journal.com/index\">Home</a></li>"
    + "<li id=\"about\"><a href=\"http://www.ancient-asia-journal.com/about\">About</a></li>"
    + "<li id=\"navItem\"><a href=\"/about/editorialTeam\">Editorial Board</a></li>"
    +	"<li id=\"current\"><a href=\"http://www.ancient-asia-journal.com/issue/current\">Current Issue</a></li>"
    + "<li id=\"archives\"><a href=\"http://www.ancient-asia-journal.com/issue/archive\">All Issues</a></li>"
    + "</ul>"
    + "</div>"
    + "<p align=\"center\" class=\"ads\"><a href=\"http://okfestival.org/\">"
    + "<img src=\"/up/ads/ad2.gif\" /></a></p>";
  
  private static final String withoutNavbar = "<p align=\"center\" class=\"ads\">"
    + "<a href=\"http://okfestival.org/\"><img src=\"/up/ads/ad2.gif\" /></a></p>"
    + "<p align=\"center\" class=\"ads\"><a href=\"http://okfestival.org/\">"
    + "<img src=\"/up/ads/ad2.gif\" /></a></p>";
  
  private static final String withBreadcrumb = "<div>"
    + "<div id=\"breadcrumb\">"
    + "<a href=\"http://www.ancient-asia-journal.com/index\">Home</a> &gt;"
    + "<a href=\"http://www.ancient-asia-journal.com/issue/archive\" class=\"hierarchyLink\">Archives</a> &gt;"
    + "Vol 1 (2006)</div>"
    + "</div>";

  private static final String withoutBreadcrumb = "<div>"
    + "</div>";

  private static final String withLinks = "<div>\n"
      + "<a href=\"https://scholarworks.iu.edu/journals/plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=https://scholarworks.iu.edu/journals/index.php/mar/article/viewFile/12838/26072\">Home1</a>\n"
      + "<a href=\"http://scholarworks.iu.edu/journals/plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=https://scholarworks.iu.edu/journals/index.php/mar/article/viewFile/12838/26072\">Home1</a>\n"
      + "<a href=\"https://scholarworks.iu.edu/journals/plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=https%3A%2F%2Fscholarworks.iu.edu%2Fjournals%2Findex.php%2Fmar%2Farticle%2FviewFile%2F12838%2F26072\">Home2</a>\n"
      + "<a href=\"http://scholarworks.iu.edu/journals/plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=https%3A%2F%2Fscholarworks.iu.edu%2Fjournals%2Findex.php%2Fmar%2Farticle%2FviewFile%2F12838%2F26072\">Home2</a>\n"
      + "<a href=\"https://scholarworks.iu.edu/journals/plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=https%253A%252F%252Fscholarworks.iu.edu%252Fjournals%252Findex.php%252Fmar%252Farticle%252FviewFile%252F12838%252F26072\">Home3</a>\n"
      + "<a href=\"http://scholarworks.iu.edu/journals/plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=https%253A%252F%252Fscholarworks.iu.edu%252Fjournals%252Findex.php%252Fmar%252Farticle%252FviewFile%252F12838%252F26072\">Home3</a>\n"
      + "</div>";

    private static final String withoutLinks = "<div>\n"
      + "<a href=\"https://scholarworks.iu.edu/journals/plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=https://scholarworks.iu.edu/journals/index.php/mar/article/viewFile/12838/26072\">Home1</a>\n"
      + "<a href=\"http://scholarworks.iu.edu/journals/plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=https://scholarworks.iu.edu/journals/index.php/mar/article/viewFile/12838/26072\">Home1</a>\n"
      + "<a href=\"https://scholarworks.iu.edu/journals/plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=https%3A%2F%2Fscholarworks.iu.edu%2Fjournals%2Findex.php%2Fmar%2Farticle%2FviewFile%2F12838%2F26072\">Home2</a>\n"
      + "<a href=\"http://scholarworks.iu.edu/journals/plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=https%3A%2F%2Fscholarworks.iu.edu%2Fjournals%2Findex.php%2Fmar%2Farticle%2FviewFile%2F12838%2F26072\">Home2</a>\n"
      + "\n\n"
      + "</div>";

  /*
   */
  
  public void testFiltering() throws Exception {
    InputStream inA, inB;
    
    // navbar
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withNavbar),
					 Constants.DEFAULT_ENCODING);
    assertEquals(withoutNavbar, StringUtil.fromInputStream(inA));
    
    // breadcrumb
    inB = fact.createFilteredInputStream(mau, new StringInputStream(withBreadcrumb),
					 Constants.DEFAULT_ENCODING);
    assertEquals(withoutBreadcrumb, StringUtil.fromInputStream(inB));
    
    // links
    inB = fact.createFilteredInputStream(mau, new StringInputStream(withLinks),
                                         Constants.DEFAULT_ENCODING);
    assertEquals(withoutLinks, StringUtil.fromInputStream(inB));

  }

}
