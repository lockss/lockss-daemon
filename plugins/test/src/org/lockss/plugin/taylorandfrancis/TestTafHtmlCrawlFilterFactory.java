/*  $Id$
 
 Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,

 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.taylorandfrancis;

import java.io.InputStream;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestTafHtmlCrawlFilterFactory extends LockssTestCase {
  private TaylorAndFrancisHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new TaylorAndFrancisHtmlCrawlFilterFactory();
  }
  

  private static final String overlay = 
      "<h2>Notes</h2>" +
          "<div class=\"summation-section\">" +
          "<a id=\"en0001\">" +
          "</a>" +
          "<p>1. Title and decription <a href=\"http://tandfonline.com/other\" target=\"_blank\"></a>http://tandfonline.com/other.</p>" +
          "<a id=\"en0002\">" +
          "</a>" +
          "</div>";
  private static final String overlayFiltered = 
      "<h2>Notes</h2>";  
  
  private static final String notes = 
      "<span class=\"ref-overlay scrollable-ref\">" +
          "<span class=\"ref-close\">" +
          "</span>" +
          "<span>34.  Author, Fred \"Title” <i>Periodical</i> 32, no. 2 (April 2009): pp. 39–61, " +
          "<a href=\"http://www.tandfonline.com/doi/abs/other\" target=\"_blank\">" +
          "http://www.tandfonline.com/doi/abs/other</a>; " +
          "<a href=\"#inline_frontnotes\">View all notes</a>" +
          "</span>";
  private static final String notesFiltered = 
      "";
  
  private static final String ref=
      "<ul class=\"references numeric-ordered-list\" id=\"references-Section\">" +
          "<h2 id=\"figures\">References</h2>" +
          "<li id=\"CIT0007\">" +
          "<span>" +
          "<span class=\"hlFld-ContribAuthor\">Forsyth, <span class=\"NLM+given-names\">A.</span>" +
          "</span> (<span class=\"NLM+year\">2001</span>). <i>" +
          "Planning Example</i> " +
          "<a href=\"http://www.tandfonline.com/doi/pdf/10.1080/2\" target=\"_blank\">http://www.tandfonline.com/doi/pdf/10.1080/2</a>" +
          "<div class=\"xlinks-container\">" +
          "</div> <div class=\"googleScholar-container\">" +
          "</div>" +
          "</span>" +
          "</li>" +
          "</ul>";   
  private static final String refFiltered=
      "";      
  

  /*
   *  Compare Html and HtmlHashFiltered
   */
  public void testOverlay() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(overlay), Constants.DEFAULT_ENCODING);
    assertEquals(overlayFiltered, StringUtil.fromInputStream(actIn));
  }
  
  public void testNotes() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(notes), Constants.DEFAULT_ENCODING);
    assertEquals(notesFiltered, StringUtil.fromInputStream(actIn));
  }
  
  public void testRef() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(ref), Constants.DEFAULT_ENCODING);
    assertEquals(refFiltered, StringUtil.fromInputStream(actIn));
  }
  

}
