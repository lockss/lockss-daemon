/*
 * $Id$
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.palgrave;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestPalgraveBookHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private PalgraveBookHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new PalgraveBookHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
  // this example is from an abstract
  private static final String HtmlHashA =
    "<div id=\"constrain\">" +
    "<div id=\"constrain-header\" class=\"cleared\">" +
    "<header><div class=\"wrapper\">" +
    "</div></header>" +
    "</div>" +  
    "</div>";
 
  private static final String HtmlHashFiltered =
    "<div id=\"constrain\">" +
    "</div>"; 
  
  // this example is a little different; it's from an article
  private static final String HtmlHashB =
    "<div id=\"constrain\">" +
    "<div id=\"constrain-footer\" class=\"jello\">" +
    "<header><div class=\"wrapper\">" +
    "</div></header>" +
    "</div>" +  
    "</div>";

  private static final String HtmlHashC =
    "<div id=\"constrain\">" +
    "<div class=\"column-width-sidebar column-r\">" +
    "</div>" +  
    "<meta name=\"keywords\" content=\"\" />" +
    "<meta name=\"description\" content=\"Hello World is a global academic and business publisher.\" />" +
    "<meta name=\"robots\" content=\"noarchive\" />" +
    "<meta name=\"emanating\" content=\"171X66X236X16\" />" +
    "<meta name=\"HW.z_cg_cat\" content=\"Business & Management\" />" +
    "<meta name=\"HW.z_cg_ecollection\" content=\"Business & Management Collection 2011\" />" +
    "<meta name=\"HW.z_cg_topic\" content=\"Jetson Studies in Leadership\" />" +
    "<meta name=\"HW.z_prod_title\" content=\"For the Greater Good of All\" />" +
    "<meta name=\"HW.cg_n\" content=\"Hello World Connect\" />" +
    "<meta name=\"HW.cg_s\" content=\"Book\" />" +
    "</div>";

  private static final String HtmlHashD = ".          .\t\t\t\t\t\t\t.";
  private static final String HtmlHashDFiltered = ". . .";

  private static final String HtmlHashEFiltered = "Hello World";
  private static final String HtmlHashE = 
    "<div class=\"similar-content cleared\">" +
    "<h3>related content</h3></div>" + HtmlHashEFiltered;

  public void testFilterA() throws Exception {
    InputStream inA;

    inA = fact.createFilteredInputStream(mau, 
          new StringInputStream(HtmlHashA), ENC);
    String filtStrA = StringUtil.fromInputStream(inA);

    assertEquals(HtmlHashFiltered, filtStrA);
   
  }

  public void testFilterB() throws Exception {
    InputStream inB;

    inB = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashB), ENC);    
    String filtStrB = StringUtil.fromInputStream(inB);
    assertEquals(HtmlHashFiltered, filtStrB);
   
  }
  public void testFilterC() throws Exception {
    InputStream in;

    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashC), ENC);    
    String filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashFiltered, filtStr);
   
  }
  public void testFilterD() throws Exception {
    InputStream in;

    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashD), ENC);    
    String filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashDFiltered, filtStr);
   
  }
  public void testFilterE() throws Exception {
    InputStream in;

    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashE), ENC);    
    String filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
   
  }

/*  
  String realHtmlFile = "TestHtmlFile.html";

  String realFilteredFile = "TestFiltered.html";

  String BASE_URL = "http://palgraveconnect.com/";
  public void testRealFile() throws Exception {
    //CIProperties xmlHeader = new CIProperties();
    InputStream file_input = null;
    PrintStream filtered_output = null;
    try {
      file_input = getResourceAsStream(realHtmlFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      InputStream inA;

      // visual test for book 
      inA = fact.createFilteredInputStream(mau, 
            new StringInputStream(string_input), ENC);
      String filtStrA = StringUtil.fromInputStream(inA);
      OutputStream outS = new FileOutputStream(realFilteredFile);
      filtered_output = new PrintStream(outS);
      filtered_output.print(filtStrA);
      IOUtil.safeClose(filtered_output);
      
    }finally {
      IOUtil.safeClose(file_input);
      IOUtil.safeClose(filtered_output);
    }

  }
  */
  
}