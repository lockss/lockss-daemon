/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
