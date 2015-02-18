/*
 * $Id$
 */
package org.lockss.plugin.atypon.americansocietyofcivilengineers;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestASCEHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private ASCEHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new ASCEHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  private static final String withSessionViewed =
      "<p align=\"center\" class=\"leftColumn\">"
        + "<div class=\"sessionViewed\">"
        + "<div class=\"label\">Recently Viewed</div>"
        + "<ul class=\"sessionHistory\">"
        + "<li><a href=\"/doi/abs/10.1061/%28ASCE%29CO.1943-7862.0000402\">"
        + "Modified Time Impact Analysis Method</a></li>"
        + "<li><a href=\"/doi/abs/10.1061/%28ASCE%29CO.1943-7862.0000372\">"
        + "Do Perceptions of Supervisors’ Safety Responses Mediate "
        + "the Relationship between Perceptions of the Organizational Safety "
        + "Climate and Incident Rates in the Construction Supply Chain?</a></li>"
        + "<li><a href=\"/doi/abs/10.1061/%28ASCE%291076-0431%282007%2913%3A2"
        + "%2872%29\">Tedesko’s Philadelphia Skating Club: Refinement of "
        + "an Idea</a></li>"
        + "</ul>"
        + "</div>"
        + "</p>";
  
  private static final String withoutSessionViewed =
      "<p align=\"center\" class=\"leftColumn\">"
      + "</p>";
  
  public void testFiltering() throws Exception {
    InputStream inStream;
    inStream = fact.createFilteredInputStream(mau,
                                              new StringInputStream(withSessionViewed),
                                              Constants.DEFAULT_ENCODING);
    assertEquals(withoutSessionViewed, StringUtil.fromInputStream(inStream));
  }

}
