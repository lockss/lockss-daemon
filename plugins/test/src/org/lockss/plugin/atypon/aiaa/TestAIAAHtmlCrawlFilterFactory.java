/*
 * $Id$
 */
package org.lockss.plugin.atypon.aiaa;

import java.io.*;
import org.lockss.util.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;
import org.lockss.test.*;

/* AIAA inherits most of its filtering from BaseAtypon and that is tested there
 * but use this to test customized bits
 */
public class TestAIAAHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private AIAAHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AIAAHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  private static final String tocLink=
      "<div class=\"widget type-publication-tools ui-helper-clearfix\" id=\"widget-3168\">" +
          "<div class=\"header thin\">" +
          "<div class=\"head-left\">" +
          "<a class=\"articleToolsNav\" href=\"/doi/full/10.1111/1.xx\">" +
          "<button>" +
          "  Previous Article" +
          "</button>" +
          "</a>" +
          "</div>" +
          "<div class=\"head-right\">" +
          "<a class=\"articleToolsNav\" href=\"/doi/full/10.1111/1.xx\">" +
          "<button>" +
          "  Next Article" +
          "</button>" +
          "</a>" +
          "</div>" +
          "<div class=\"head-middle\">" +
          "<h3>" +
          "<a href=\"http://test/toc/testj/51/7\">" +
          "            Volume 51, Issue 7 (July)" +
          "</a>" +
          "</h3>" +
          "</div>" +
          "</div>" +
          "<div class=\"body\">" +
          "  links to other aspects of article" +
          "</div>" +
          "</div>";
  private static final String tocLinkFiltered=
      "<div class=\"widget type-publication-tools ui-helper-clearfix\" id=\"widget-3168\">" +
          "<div class=\"header thin\">" +
          "<div class=\"head-left\">" +
          "</div>" +
          "<div class=\"head-right\">" +
          "</div>" +
          "</div>" +
          "<div class=\"body\">" +
          "  links to other aspects of article" +
          "</div>" +
          "</div>";


  public void testAIAAFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(tocLink),
        Constants.DEFAULT_ENCODING);
    assertEquals(tocLinkFiltered, StringUtil.fromInputStream(actIn));
    

  }


}
