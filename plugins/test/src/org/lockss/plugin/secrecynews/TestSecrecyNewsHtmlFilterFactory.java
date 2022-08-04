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

package org.lockss.plugin.secrecynews;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestSecrecyNewsHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private SecrecyNewsHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new SecrecyNewsHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String inst1 =
	  "<p style=\"display: none;\"> " +
	  "<input type=\"hidden\" " +
	  "id=\"akismet_comment_nonce\" " +
	  "name=\"akismet_comment_nonce\" " +
	  "value=\"abcde12345\" " +
	  "/></p>" +
    "<!-- Advertising Manager v3.4.19 (0.123 seconds.) -->" +
	  "<!-- 40 queries. 0.595 seconds. -->" +
    "<!-- All in One SEO Pack 1.6.14.3 by Michael Torbert of Semper Fi Web Design[307,357] -->" +
    "<li><h2>Archives</h2><ul><li><a href='http://www.fas.org/blog/secrecy/2012/06' title='June 2012'>June 2012</a></li></ul></li>" +
    "<li class=\"categories\"><h2><Categories</h2><ul><li class=\"cat-item cat-item-3\"><a href=\"http://www.fas.org/blog/secrecy/category/crs\" title=\"View all posts filed under CRS\">CRS</a> (126)</li></ul>";
  
  private static final String inst2 =
	  "<p style=\"display: none;\"> " +
	  "<input type=\"hidden\" " +
	  "id=\"akismet_comment_nonce\" " +
	  "name=\"akismet_comment_nonce\" " +
	  "value=\"fghij67890\" " +
	  "/></p>" +
    "<!-- Advertising Manager v3.4.19 (0.456 seconds.) -->" +
    "<!-- 40 queries. 0.648 seconds. -->" +
    "<!-- All in One SEO Pack 1.6.14.2 by Michael Torbert of Semper Fi Web Design[307,357] -->" +
    "<li><h2>Archives</h2><ul><li><a href='http://www.fas.org/blog/secrecy/2012/05' title='May 2012'>May 2012</a></li></ul></li>" +
    "<li class=\"categories\"><h2><Categories</h2><ul><li class=\"cat-item cat-item-3\"><a href=\"http://www.fas.org/blog/secrecy/category/crs\" title=\"View all posts filed under CRS\">CRS</a> 117)</li></ul>";

  public void testFiltering() throws Exception {
    InputStream inA;
    InputStream inB;

    inA = fact.createFilteredInputStream(mau,
					 new StringInputStream(inst1), ENC);
    inB = fact.createFilteredInputStream(mau,
					 new StringInputStream(inst2), ENC);
    assertEquals(StringUtil.fromInputStream(inA),
                 StringUtil.fromInputStream(inB));
  }
}