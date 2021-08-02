/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.highwire.aps;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestAPSHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private APSHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new APSHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  private static final String withAuTT = "<div id=\"page\">\n" +
      "<div class=\"author-tooltip-name\">" +
      "<span class=\"nlm-given-names\">Nae</span>" +
      " <span class=\"nlm-surname\">Soup</span> </div>" +
      "</div>";
  
  private static final String withoutAuTT = "<div id=\"page\">" +
      "\n" +
      "</div>";
  
  private static final String withHwLink = "<div id=\"page\">" +
      "<a class=\"hw-link hw-abstract\"" +
      " href=\"http://ajpregu.physiology.org/content/304/3/R218.abstract\">View Abstract</a>" +
      "</div>";
  
  private static final String withoutHwLink = "<div id=\"page\">" +
      "</div>";
  
  
  public void testFiltering() throws Exception {
    InputStream inA;
    String a;
    
    // author tooltip
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withAuTT),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutAuTT, a);
    
    // HwLink
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withHwLink),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(withoutHwLink, a);
    
  }
  
}
