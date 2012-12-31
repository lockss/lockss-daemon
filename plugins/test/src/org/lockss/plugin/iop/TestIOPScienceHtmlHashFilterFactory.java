/*
 * $Id: TestIOPScienceHtmlHashFilterFactory.java,v 1.1 2012-12-31 20:51:00 pgust Exp $
 */

package org.lockss.plugin.iop;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestIOPScienceHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private IOPScienceHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new IOPScienceHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  // test removal of tags by the hash filter
  private static final String tagsHtmlHash =
      "<div>" +
      "<div class=\"alsoRead\"><br></div>" +
      "<div class=\"tabs javascripted\"><br></div>" +
      "<div id=\"banner\"><br></div>" +
      "<div id=\"footer\"><br></div>" +
      "<link type=\"text/css\"/>" +
      "<script type=\"javascript\"/>var x=0;</script>" +
      "<form action=\"foo?jsessionId=bar\"><br></form>" +
      "<div id=\"tacticalBanners\"><br></div>" +
      "</div>";
  // only outer div should remain
  private static final String tagsHtmlHashFiltered =
      "<div></div>";
  
 
  private static final String WhiteSpace1 = "\n  <li><a href=\"/content/pdf/1477-7525-8-103.pdf\">PDF</a>\n (543KB)\n </li>";
  
  private static final String WhiteSpace2 = "\n\n      <li><a href=\"/content/pdf/1477-7525-8-103.pdf\">PDF</a>\n       (543KB)\n      </li>";

  
  public void testFiltering() throws Exception {
    InputStream inA;
    InputStream inB;
    
    /* impactFactor test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(tagsHtmlHash),
        ENC);

    assertEquals(tagsHtmlHashFiltered,StringUtil.fromInputStream(inA));

    /* whiteSpace test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(WhiteSpace1),
        ENC);
    
    inB = fact.createFilteredInputStream(mau, new StringInputStream(WhiteSpace2),
        ENC);

    assertEquals(StringUtil.fromInputStream(inA),StringUtil.fromInputStream(inB));
  }
}