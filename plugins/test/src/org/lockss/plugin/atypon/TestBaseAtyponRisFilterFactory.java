/*
 * $Id: TestBaseAtyponRisFilterFactory.java,v 1.6 2014-01-23 18:51:26 alexandraohlson Exp $
 */
/*

/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.atypon;

import java.io.InputStream;
import org.lockss.test.*;
import org.lockss.util.*;


public class TestBaseAtyponRisFilterFactory extends LockssTestCase {

  static Logger log = Logger.getLogger("TestBaseAtyponRisFilterFactory");

  private BaseAtyponRisFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new BaseAtyponRisFilterFactory();
  }

  
  public void testEmptyContent() throws Exception {
    InputStream actIn;
    // no content at all
    actIn = fact.createFilteredInputStream(mau,  new StringInputStream(""), Constants.DEFAULT_ENCODING);
    assertEquals("", StringUtil.fromInputStream(actIn));
    // one newline 
    actIn = fact.createFilteredInputStream(mau,  new StringInputStream("\n"), Constants.DEFAULT_ENCODING);
    assertEquals("\n", StringUtil.fromInputStream(actIn));
    // empty line and then empty space and then nothing
    actIn = fact.createFilteredInputStream(mau,  new StringInputStream("     \n  "), Constants.DEFAULT_ENCODING);
    assertEquals("     \n  ", StringUtil.fromInputStream(actIn));
    
  }
  
  private static final String notContent1 =
      "\n  \n   \n\n NOT RIS\nTY - JOUR\nER  - \n"; // not first item in file
  private static final String notContent2 = 
      "\n\n\n\nTY : JOUR\nY1 - gooddate  \nY2 - baddate\n  - ER \n"; //missing the delimiter
  
  private static final String yesContent1 = 
      "\n\n\n\nTY - JOUR\nY1 - gooddate  \nY2  - baddate\nER  - \n";
  private static final String yesContent1_expected =      
      "\n\n\n\nTY - JOUR\nY1 - gooddate  \nER  - \n";

  private static final String yesContent2 =      
      "\n\n\n\n    TY - JOUR\nY1 - gooddate  \nY2  - baddate\nER  - \n"; // extra spaces before TY
  private static final String yesContent2_expected =      
      "\n\n\n\n    TY - JOUR\nY1 - gooddate  \nER  - \n"; // extra spaces before TY
  
  
  public void testBasicContent() throws Exception {
    InputStream actIn;

    // shouldn't get filtered
    actIn = fact.createFilteredInputStream(mau,  new StringInputStream(notContent1), Constants.DEFAULT_ENCODING);
    assertEquals(notContent1, StringUtil.fromInputStream(actIn));

    // shouldn't get filtered
    actIn = fact.createFilteredInputStream(mau,  new StringInputStream(notContent2), Constants.DEFAULT_ENCODING);
    assertEquals(notContent2, StringUtil.fromInputStream(actIn));

    //should get filtered
    actIn = fact.createFilteredInputStream(mau,  new StringInputStream(yesContent1), Constants.DEFAULT_ENCODING);
    assertEquals(yesContent1_expected, StringUtil.fromInputStream(actIn));
    
    //should get filtered
    actIn = fact.createFilteredInputStream(mau,  new StringInputStream(yesContent2), Constants.DEFAULT_ENCODING);
    assertEquals(yesContent2_expected, StringUtil.fromInputStream(actIn));

  }

  // Using the getResourceAsStream() will find these in the current directory
  private static final String realFileRis = "Test_ris.txt";
  private static final String realFileRisExpected = "Test_risExpected.txt";
  private static final String realFileBib = "Test_bib.txt";
  private static final String realFileMedlars = "Test_medlars.txt";


  public void testFromFilesFiltering() throws Exception {
    InputStream file_input = null;
    InputStream file_expected_input = null;
    InputStream filteredInput = null;
    String string_expected;
    String string_filtered;

    try {
      // RIS FILE - should filter out the Y2 

      file_input = getResourceAsStream(realFileRis);
      file_expected_input = getResourceAsStream(realFileRisExpected); // no Y2 in this file
      filteredInput = fact.createFilteredInputStream(mau, file_input, Constants.DEFAULT_ENCODING); //run through filter
      string_expected = StringUtil.fromInputStream(file_expected_input);
      string_filtered = StringUtil.fromInputStream(filteredInput);  
      assertEquals(string_expected, string_filtered);
      IOUtil.safeClose(file_input);
      IOUtil.safeClose(file_expected_input);
      IOUtil.safeClose(filteredInput);
      
      // BIB FILE - should be unchanged 
      file_input = getResourceAsStream(realFileBib);
      file_expected_input = getResourceAsStream(realFileBib); //identical
      filteredInput = fact.createFilteredInputStream(mau, file_input, Constants.DEFAULT_ENCODING);
      string_expected = StringUtil.fromInputStream(file_expected_input); // unchanged from original
      string_filtered = StringUtil.fromInputStream(filteredInput); 
      assertEquals(string_expected, string_filtered);
      IOUtil.safeClose(file_input);
      IOUtil.safeClose(file_expected_input);
      
      // MEDLARS FILE - should be unchanged 
      file_input = getResourceAsStream(realFileMedlars);
      file_expected_input = getResourceAsStream(realFileMedlars); //identical
      filteredInput = fact.createFilteredInputStream(mau, file_input, Constants.DEFAULT_ENCODING);
      string_expected = StringUtil.fromInputStream(file_expected_input); //unchanged from original
      string_filtered = StringUtil.fromInputStream(filteredInput); 
      assertEquals(string_expected, string_filtered);
      IOUtil.safeClose(file_input);
      IOUtil.safeClose(file_expected_input);

    }finally {
      IOUtil.safeClose(file_input);
      IOUtil.safeClose(file_expected_input);
      IOUtil.safeClose(filteredInput);
    }
   

  }


}
