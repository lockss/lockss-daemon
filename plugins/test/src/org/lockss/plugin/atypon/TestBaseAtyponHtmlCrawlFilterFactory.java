/*
 * $Id: TestBaseAtyponHtmlCrawlFilterFactory.java,v 1.1 2013-08-06 21:24:24 aishizaki Exp $
 */
package org.lockss.plugin.atypon;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestBaseAtyponHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private BaseAtyponHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new BaseAtyponHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  private static final String withCitations = "</div><div class=\"citedBySection\">" +
  "<a name=\"citedBySection\"></a><h2>Cited by</h2>" +
  "<div class=\"citedByEntry\"><span class=\"author\">Robert X</span>, <span class=\"author\">Kenneth X</span>.BIG TITLE HERE. <i>" +
  "<span class=\"NLM_source\">Risk Analysis</span></i>no" +
  "-no<br />Online publication date: 1-Dec-2012.<br /><span class=\"CbLinks\"></span></div>"+
  "</div><!-- /fulltext content --></div>"+
  "       </div></div><div class=\"clearfix\">&nbsp;</div></div>";

  private static final String withoutCitations = "</div><!-- /fulltext content --></div>"+
  "       </div></div><div class=\"clearfix\">&nbsp;</div></div>";  

  
  public void testFiltering() throws Exception {
    InputStream inStream;
    inStream = fact.createFilteredInputStream(mau,
                                              new StringInputStream(withCitations),
                                              Constants.DEFAULT_ENCODING);
    assertEquals(withoutCitations, StringUtil.fromInputStream(inStream));
  }

}
