/*
 * $Id$
 */
package org.lockss.plugin.metapress;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestMetaPressHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private MetaPressHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new MetaPressHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  private static final String withRef = "" +
      "<h5 class=\"references\"> References </h5>" +
      "<div id=\"References\">\n" + 
      "  <p> Auth, One (2000) ‘Under’, Retrieved September 2001, http://www.foo.org/under.htm " +
      "    <a href=\"http://www.foo.org/under.htm\" target=\"_blank\">http://www.foo.org/under.htm</a></p>" +
      "  <p> more stuff</p>\n" + 
      "</div>";
  
  private static final String withoutRef = "" +
      "<h5 class=\"references\"> References </h5>";
  
  
  public void testFiltering() throws Exception {
    InputStream inA;
    
    // navbar
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withRef),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutRef, StringUtil.fromInputStream(inA));
    
  }
  
}
