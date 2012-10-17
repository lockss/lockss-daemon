/*
 * $Id: TestProjectMuseFilterFactory.java,v 1.1 2012-10-17 19:04:58 alexandraohlson Exp $
 */

package org.lockss.plugin.projmuse;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestProjectMuseFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private ProjectMuseHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new ProjectMuseHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String frequentHtml =
          "<h2 class=\"rightnav box-nav\" id=\"freq_downloaded\"><a title=\"Frequently Downloaded\" id=\"freq_downloaded\">Frequently Downloaded</a></h2>" +
          "<ul class=\"rightnav\" id=\"freq_downloaded-box\">" +
          "<li><a> The Rentier State and National Oil Companies: An Economic and Political Perspective</a></li>" +   
          "<li><a> Democratic Islamization in Pakistan and Turkey: Lessons for the Post-Arab Spring Muslim World</a></li>" +   
          "<li><a> Lebanon after the Civil War: Peace or the Illusion of Peace?</a></li>" +    
          "</ul>" +
          "<div class=\"legend\">THIS CONTENT STAYS</div>";
// All html tags get removed by the projmuse filter rule after hash filtering
  private static final String frequentHtmlFiltered =
      "THIS CONTENT STAYS";
  
  public void testFiltering() throws Exception {
    InputStream inA;
//    InputStream inB;
    
    /* impactFactor test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(frequentHtml),
        ENC);

    assertEquals(frequentHtmlFiltered,StringUtil.fromInputStream(inA));


  }
}