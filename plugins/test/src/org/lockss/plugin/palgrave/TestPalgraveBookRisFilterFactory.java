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

public class TestPalgraveBookRisFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private PalgraveBookRisFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new PalgraveBookRisFilterFactory();
    mau = new MockArchivalUnit();
  }
  // this example is from an abstract
  private static final String HtmlHashA =
    "TY  - ECHAP\n" +
    "AU  - Goldsmith, Hilary A.\n" +
    "AU  - Goldsmith, Hilary A.\n" +
    "CY  - Basingstoke\n" +
    "DA  - 2014/07/23\n" +
    "DO  - 10.1057/9781137016768.0007\n" +
    "PB  - Palgrave Macmillan\n" +
    "PY  - 2012\n" +
    "SP  - 16\n" +
    "TI  - The Fact and Fiction of Darwinism\n" +
    "UR  - http://dx.doi.org/10.1057/9781137016768.0007\n" +
    "T2  - Cross-Cultural Connections in Crime Fictions\n" +
    "M1  - 2\n" +
    "ER  - \n";
 
  private static final String HtmlHashFiltered =
    "TY  - ECHAP\n" +
    "AU  - Goldsmith, Hilary A.\n" +
    "AU  - Goldsmith, Hilary A.\n" +
    "CY  - Basingstoke\n" +
    "DO  - 10.1057/9781137016768.0007\n" +
    "PB  - Palgrave Macmillan\n" +
    "PY  - 2012\n" +
    "SP  - 16\n" +
    "TI  - The Fact and Fiction of Darwinism\n" +
    "UR  - http://dx.doi.org/10.1057/9781137016768.0007\n" +
    "T2  - Cross-Cultural Connections in Crime Fictions\n" +
    "M1  - 2\n" +
    "ER  - \n";
 


  public void testFilterA() throws Exception {
    InputStream inA;

    inA = fact.createFilteredInputStream(mau, 
          new StringInputStream(HtmlHashA), ENC);
    String filtStrA = StringUtil.fromInputStream(inA);
    //System.out.println(filtStrA);

    assertEquals(HtmlHashFiltered, filtStrA);
   
  }
  
}