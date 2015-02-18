/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.secrecynews;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
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