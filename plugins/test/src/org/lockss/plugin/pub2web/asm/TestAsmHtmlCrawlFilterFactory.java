/*
 * $Id:$
 */
package org.lockss.plugin.pub2web.asm;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestAsmHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private AsmHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AsmHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  private static final String withArticleUpdate = "<div><div class=\"consanguinityContainer\">" +
"<div class=\"consanguinityTitleContainer\">" +
"<h4>Article Version</h4>" +
"</div><div class=\"consanguinityBlurbContainer\">" +
"This article is an updated version of the following content:" +
"</div><a href=\"/content/journal/ecosalplus/10.1128/ecosalplus.5.5\" title=\"\" >" +
"titlegoeshere</span></a>" +
"</div></div>";

  private static final String withArticleUpdateFiltered = 
      "<div></div>";  


  private static final String withXreferencesInLine=
      "<p>" +
"The overview continues and updates previous reviews on the respiratory chain of blah blah" +
" detailed presentation of selected respiratory enzymes will be given in chapters" +
" NADH as Donor (<a target=\"xrefwindow\" " +
"href=\"http://www.asmscience.org/content/journal/ecosalplus/10.1128/ecosalplus.3.2.4\">" +
"http://www.asmscience.org/content/journal/ecosalplus/10.1128/ecosalplus.3.2.4</a>). " +
"And more is described below " +
"(<span class=\"xref\"><a href=\"#b5\">5</a></span>, <span class=\"xref\"><a href=\"#b6\">6</a></span>)." +
"</p>"; 
  private static final String withXreferencesInLineFiltered=
      "<p>" +
          "The overview continues and updates previous reviews on the respiratory chain of blah blah" +
          " detailed presentation of selected respiratory enzymes will be given in chapters" +
          " NADH as Donor (). " +
          "And more is described below " +
          "(<span class=\"xref\"><a href=\"#b5\">5</a></span>, <span class=\"xref\"><a href=\"#b6\">6</a></span>)." +
          "</p>"; 


  public void testXrefFiltering() throws Exception {
    InputStream inStream;
    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(withXreferencesInLine),
        Constants.DEFAULT_ENCODING);
    assertEquals(withXreferencesInLineFiltered, StringUtil.fromInputStream(inStream));
  }

  public void testArticleUpdateFiltering() throws Exception {
    InputStream inStream;
    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(withArticleUpdate),
        Constants.DEFAULT_ENCODING);
    assertEquals(withArticleUpdateFiltered, StringUtil.fromInputStream(inStream));


  }


}
