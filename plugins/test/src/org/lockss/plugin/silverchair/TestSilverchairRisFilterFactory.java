/*
 * $Id$
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

package org.lockss.plugin.silverchair;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;


public class TestSilverchairRisFilterFactory extends LockssTestCase {

  static Logger log = Logger.getLogger(TestSilverchairRisFilterFactory.class);

  private SilverchairRisFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new SilverchairRisFilterFactory();
  }


  // Using the getResourceAsStream() will find these in the current directory
  private static final String realFileRis = "Test_ris.txt";
  private static final String realFileRisExpected = "Test_risExpected.txt";

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

    }finally {
      IOUtil.safeClose(file_input);
      IOUtil.safeClose(file_expected_input);
      IOUtil.safeClose(filteredInput);
    }
  }
}
