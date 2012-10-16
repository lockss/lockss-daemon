/*
 * $Id: TestNatureHtmlFilterFactory.java,v 1.1 2012-10-16 20:07:13 alexandraohlson Exp $
 */

package org.lockss.plugin.nature;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestNatureHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private NaturePublishingGroupHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new NaturePublishingGroupHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String breadcrumbHtmlHash =
      "<div><div id=\"breadcrumb\"><div><a href=\"/onc/index.html\">Journal home</a>" +
          "<span class=\"divider\"> &#x0003E; </span>" +
          "<a href=\"/onc/journal/v29/n50/index.html\"> Archive</a>" +
          "<span class=\"divider\"> &#x0003E; </span>" +
          "<a href=\"/onc/journal/v29/n50/index.html#oa\">Original Articles</a>" +
          "<span class=\"divider\"> &#x0003E; </span>" +
          "<span class=\"thisitem\">Full text</span></div></div></div>";
  private static final String breadcrumbHtmlHashFiltered =
      "<div></div>";
  
 
 
  private static final String WhiteSpace1 = "\n  <li><a href=\"/content/pdf/1477-7525-8-103.pdf\">PDF</a>\n (543KB)\n </li>";
  
  private static final String WhiteSpace2 = "\n\n      <li><a href=\"/content/pdf/1477-7525-8-103.pdf\">PDF</a>\n       (543KB)\n      </li>";

  
  public void testFiltering() throws Exception {
    InputStream inA;
    InputStream inB;
    
    /* impactFactor test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(breadcrumbHtmlHash),
        ENC);

    assertEquals(breadcrumbHtmlHashFiltered,StringUtil.fromInputStream(inA));

    /* whiteSpace test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(WhiteSpace1),
        ENC);
    
    inB = fact.createFilteredInputStream(mau, new StringInputStream(WhiteSpace2),
        ENC);

    assertEquals(StringUtil.fromInputStream(inA),StringUtil.fromInputStream(inB));
  }
}