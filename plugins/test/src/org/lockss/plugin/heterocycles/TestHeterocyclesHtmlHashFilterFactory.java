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

package org.lockss.plugin.heterocycles;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestHeterocyclesHtmlHashFilterFactory extends LockssTestCase {
  private HeterocyclesHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new HeterocyclesHtmlHashFilterFactory();
  }

 private static final String withScript =
     "<div class=\"block\">"
       + "<script type=\"text/javascript\" "
       + "src=\"/js/jquery-1.4.2.min.js\"></script>"
       + "<script type=\"text/javascript\""
       + "src=\"/js/checkboxes.js\"></script>"
       + "<script type=\"text/javascript\""
       + "src=\"/js/pub.js\"></script>"
       + "<script  type=\"text/javascript\" charset=\"utf-8\">"
       + "$(function(){"
       + " $('input[type=checkbox],input[type=radio]').checkboxes();"
       + "});"
       + "</script>"
       + "</div>";
  private static final String withoutScript = "<div class=\"block\"></div>";
  
  private static final String withComments =
      "<div class=\"block\">"
        + "<!-- comment comment comment -->"
        + "</div>";
  private static final String withoutComments = "<div class=\"block\"></div>";

  private static final String withHeader =
      "<div class=\"block\">"
        + "<div id=\"header\">"
        + "<div class=\"logobox clearfix\">"
        + "<img src=\"/clockss/img/cycle.gif\" border=\"0\" height=\"100\""
        + "width=\"100\" class=\"logo\" alt=\"logo2\">"
        + "The journal title"
        + "<span class=\"red\">Web Edition</span> ISSN: 1111-1111 <br>"
        + "<span class=\"copy\">Published online</span>"
        + "</div>"
        + "<br class=\"clearfloat\">"
        + "<div id=\"menu2\" class=\"menu1\">"
        + "<br class=\"clearfloat\">"
        + "</div>"
        + "<br class=\"clearfloat\">"
        + "</div>"
        + "</div>";
  private static final String withoutHeader = 
      "<div class=\"block\"></div>";
  
  private static final String withFooter =
      "<div class=\"block\">"
        + "<div id=\"footer\">"
        + "<div class=\"footerBar\"> The journal title</div>"
        + "<br class=\"clearfloat\">"
        + "<span class=\"copy\">Copyright ⓒ 2013 publisher name</span>"
        + "<p></p>"
        + "</div>"
	+ "</div>";
  private static final String withoutFooter =
      "<div class=\"block\"></div>";

  public void testScriptFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withScript), Constants.DEFAULT_ENCODING);
    assertEquals(withoutScript, StringUtil.fromInputStream(actIn));
  }

  public void testCommentsFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withComments), Constants.DEFAULT_ENCODING);
    assertEquals(withoutComments, StringUtil.fromInputStream(actIn));
  }

  public void testHeaderFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withHeader), Constants.DEFAULT_ENCODING);
    assertEquals(withoutHeader, StringUtil.fromInputStream(actIn));
  }

  public void testFooterFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withFooter), Constants.DEFAULT_ENCODING);
    assertEquals(withoutFooter, StringUtil.fromInputStream(actIn));
  }

}
