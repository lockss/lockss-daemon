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

package org.lockss.plugin.iumj;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestIUMJHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private IUMJHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new IUMJHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
 
  // timestampHtmlHashA should be filtered to become timestampHashAFiltered
  private static final String timestampHtmlHashA =
    "<div id=\"container\">" +
    "<div id=\"footer\">" +
    "<p>Hello World Journal - 2013-01-23 18:57:08</p>" +
    "</div>" +
    "</div>";
 
  private static final String timestampHtmlHashAFiltered =
    "<div id=\"container\"></div>";
  
  // timestampHtmlHashB should be filtered same as 'A'
  private static final String timestampHtmlHashB =
    "<div id=\"container\">" +
    "<div id=\"footer\">" +
    "<p>Hello World - 2013-01-23 18:57:08</p>" +
    "</div>" +
    "</div>";
  
  // timestampHtmlHashC should not get filtered at all (stays the same)
  private static final String timestampHtmlHashC =
    "<div id=\"container\">" +
    "<div id=\"header\">" +
    "<p>Hello World Journal - 2013-01-23 18:57:08</p>" +
    "</div>" +
    "</div>";
 
  public void testFiltering() throws Exception {
    InputStream inA, inB, inC;

    /* timestamp test */
    inA = fact.createFilteredInputStream(mau, 
          new StringInputStream(timestampHtmlHashA), ENC);
    String filtStrA = StringUtil.fromInputStream(inA);
    assertEquals(timestampHtmlHashAFiltered, filtStrA);
    
    inB = fact.createFilteredInputStream(mau, 
        new StringInputStream(timestampHtmlHashB), ENC);
    String filtStrB = StringUtil.fromInputStream(inB);
    assertNotEquals(timestampHtmlHashB, filtStrB);

    inC = fact.createFilteredInputStream(mau, 
        new StringInputStream(timestampHtmlHashC), ENC);
    String filtStrC = StringUtil.fromInputStream(inC);
    assertEquals(timestampHtmlHashC, filtStrC);    
  }
}