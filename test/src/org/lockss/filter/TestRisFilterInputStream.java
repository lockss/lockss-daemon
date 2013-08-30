/*
 * $Id: TestRisFilterInputStream.java,v 1.1 2013-08-30 17:10:37 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.filter;

import java.io.*;
import java.util.ArrayList;

import org.lockss.test.*;
import org.lockss.util.*;

public class TestRisFilterInputStream extends LockssTestCase {
  
  public void testCanNotCreateWithNullInputStream() {
    try {
      RisFilterInputStream filter =
        new RisFilterInputStream(null, Constants.DEFAULT_ENCODING, "");
      fail("Trying to create a RisFilterInputStream with a null InputStream should throw "+
           "an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }
  
  public void testBasicFilterFunctionality() throws Exception {
    InputStream actIn;
    //InputStream expIn;
    String filterOut;
    
    // remove arbitrary information
    actIn = new StringInputStream("blah");
    filterOut = "blah";
    assertEquals("", StringUtil.fromInputStream(new RisFilterInputStream(actIn, Constants.DEFAULT_ENCODING, filterOut)));
    
    // remove and leave the rest of the lines
    actIn = new StringInputStream("\nblah  \nGOOD\n");
    assertEquals("\nGOOD\n", StringUtil.fromInputStream(new RisFilterInputStream(actIn, Constants.DEFAULT_ENCODING, filterOut)));

    // must be first thing on line (other than white space) to be a match and get removed
    actIn = new StringInputStream("\nxblah  \nGOOD\n");
    assertEquals("\nxblah  \nGOOD\n", StringUtil.fromInputStream(new RisFilterInputStream(actIn, Constants.DEFAULT_ENCODING, filterOut)));

    // empty filter will match every single line. It's weird but correct (see String.startsWith())
    filterOut = ""; 
    actIn = new StringInputStream("\nFour score and \nseven years ago our fathers\n brought forth on this continent...\n");
    assertEquals("", StringUtil.fromInputStream(new RisFilterInputStream(actIn, Constants.DEFAULT_ENCODING, filterOut)));
    
    IOUtil.safeClose(actIn);
  }
  
  private static final String risData =
"\n" +
"\n" +
"\n" +
"      \n" + // some extra white spaces
"\n" +
"\n" +
"\n" +
"TY  - JOUR\n" +
"T1  - The Article Title\n" +
"AU  - Author, Sven\n" +
"AU  - Writer, Martin\n" +
"Y1  - 2011/05/31\n" +
"PY  - 2011\n" +
"DA  - 2011/06/01\n" +
"N1  - doi: 11.1111/XXX.YYY\n" +
"DO  - 11.1111/XXX.YYY\n" +
"T2  - Full Journal Name\n" +
"JF  - Full Journal Name\n" +
"JO  - Full Journal Name\n" +
"SP  - 87\n" + 
"  EP  - 95\n" + // some white space before item
"VL  - 8\n" +
"IS  - 1\n" +
"PB  - Routledge\n" +
"SN  - 1744-9480\n" +
"M3  - doi: 11.1111/XXX.YYY\n" +
"UR  - http://dx.doi.org/11.1111/XXX.YYY\n" +
"Y2  - 2013/08/29\n" +
"ER  - \n" +
"0\n" +
"";
  
  private static final String risDataNoPublisher = 
      "\n" +
          "\n" +
          "\n" +
          "      \n" +
          "\n" +
          "\n" +
          "\n" +
          "TY  - JOUR\n" +
          "T1  - The Article Title\n" +
          "AU  - Author, Sven\n" +
          "AU  - Writer, Martin\n" +
          "Y1  - 2011/05/31\n" +
          "PY  - 2011\n" +
          "DA  - 2011/06/01\n" +
          "N1  - doi: 11.1111/XXX.YYY\n" +
          "DO  - 11.1111/XXX.YYY\n" +
          "T2  - Full Journal Name\n" +
          "JF  - Full Journal Name\n" +
          "JO  - Full Journal Name\n" +
          "SP  - 87\n" +
          "  EP  - 95\n" +
          "VL  - 8\n" +
          "IS  - 1\n" +
          "SN  - 1744-9480\n" +
          "M3  - doi: 11.1111/XXX.YYY\n" +
          "UR  - http://dx.doi.org/11.1111/XXX.YYY\n" +
          "Y2  - 2013/08/29\n" +
          "ER  - \n" +
          "0\n" +
          "";
  
  private static final String risDataModified =
      "\n" +
          "\n" +
          "\n" +
          "      \n" +
          "\n" +
          "\n" +
          "\n" +
          "TY  - JOUR\n" +
          "T1  - The Article Title\n" +
          "AU  - Author, Sven\n" +
          "AU  - Writer, Martin\n" +
          "Y1  - 2011/05/31\n" +
          "PY  - 2011\n" +
          "DA  - 2011/06/01\n" +
          "N1  - doi: 11.1111/XXX.YYY\n" +
          "DO  - 11.1111/XXX.YYY\n" +
          "T2  - Full Journal Name\n" +
          "JF  - Full Journal Name\n" +
          "SP  - 87\n" +
          "VL  - 8\n" +
          "IS  - 1\n" +
          "PB  - Routledge\n" +
          "SN  - 1744-9480\n" +
          "M3  - doi: 11.1111/XXX.YYY\n" +
          "UR  - http://dx.doi.org/11.1111/XXX.YYY\n" +
          "ER  - \n" +
          "0\n" +
          "";
      
  
public void testBasisRisFiltering() throws Exception {
  InputStream actIn;
  String filterOut;
  ArrayList<String> filterList = new ArrayList<String>();  

  // remove publisher
  actIn = new StringInputStream(risData); // filter out publisher
  filterOut = "PB";
  assertEquals(risDataNoPublisher, StringUtil.fromInputStream(new RisFilterInputStream(actIn, Constants.DEFAULT_ENCODING, filterOut)));
  
  // remove 3 items: Y2, J0; EP
  actIn = new StringInputStream(risData); // filter out publisher
  filterList.add("Y2");
  filterList.add("JO");
  filterList.add("EP");
  assertEquals(risDataModified, StringUtil.fromInputStream(new RisFilterInputStream(actIn, Constants.DEFAULT_ENCODING, filterList)));
  
  // case dependent??
  filterList.remove("JO");
  filterList.add("jO");
  actIn = new StringInputStream(risData);
  // RIS tags must be upper case, therefore case dependent
  assertNotEquals(risDataModified, StringUtil.fromInputStream(new RisFilterInputStream(actIn, Constants.DEFAULT_ENCODING, filterList)));
  
  IOUtil.safeClose(actIn);
}


}
