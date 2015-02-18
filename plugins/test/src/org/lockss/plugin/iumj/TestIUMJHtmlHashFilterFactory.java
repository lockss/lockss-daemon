/*
 * $Id$
 */

/*

 Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.iumj;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.MetadataField;
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