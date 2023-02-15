/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.psychiatryonline;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestPsychiatryOnlineHtmlFilterFactory extends LockssTestCase {
  private PsychiatryOnlineHtmlFilterFactory fact;
  private MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new PsychiatryOnlineHtmlFilterFactory();
  }
  
  // block tags from PsychiatryOnlineHtmlFilterFactory
  String blockIds[][] = new String[][] {
      // only tests the constructed tag rather than actual example from page
      {"span", "id", "lblSeeAlso"},
  };
  
  // single tags from PsychiatryOnlineHtmlFilterFactory
  String[][] tagIds = new String[][] {
      {"input", "type", "hidden"},
  };
  
  public void testTagFiltering() throws Exception {
    // common filtered html results
    String filteredHtml = "" +
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"> "
        + "<html lang=\"en\"> <body>\n"
        + "\n</body>\n</html>\n\n";
    
    // html for block tags
    String blockHtml = "" +
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"> "
        + "<html lang=\"en\"> <body>\n"
        + "<%s %s=\"%s\">\n"
        + "chicken chicken chicken...\n"
        + "</%s>"
        + "\n</body>\n</html>\n\n";
    
    // test block tag ID filtering
    for (String[] id : blockIds) {
      InputStream htmlIn = fact.createFilteredInputStream(mau,
          new StringInputStream(String.format(blockHtml, id[0],id[1],id[2],id[0])),
          Constants.DEFAULT_ENCODING);
      assertEquals(filteredHtml, StringUtil.fromInputStream(htmlIn));
    }
  }
  //test meta tag with explicit tests
  private static final String MetaTagHtml = "" +
      "<p>The chickens were decidedly cold." +
      "<META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-16\">" +
      "</p>";
  
  private static final String MetaTagHtmlFiltered =
      "<p>The chickens were decidedly cold.</p>";
  
  public void testMetaTag() throws Exception {
    InputStream inA;
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(MetaTagHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(MetaTagHtmlFiltered,StringUtil.fromInputStream(inA));
  }
  
}
