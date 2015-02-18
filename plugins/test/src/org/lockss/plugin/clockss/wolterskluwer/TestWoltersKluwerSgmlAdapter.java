/* $Id$

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

package org.lockss.plugin.clockss.wolterskluwer;


import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;

import org.lockss.util.LineRewritingReader;
import java.io.StringReader;

/*
 * Test WoltersKluwerSgmlAdapter, which extends LineRewritingReader
 *   - this takes unclosed sgml tags and rewrites them, inline, to end in "/>"
 *     which the xml sax parser can then handle
 *   - we provide the unclosed sgml tags to this adapter
 */

public class TestWoltersKluwerSgmlAdapter
  extends LockssTestCase {
  
  static Logger log = 
      Logger.getLogger(TestWoltersKluwerSgmlAdapter.class);

  private static final String BEFORE = ">";
  private static final String AFTER = " />";
  private static final String TestCOVER_BEFORE = "<COVER NAME=\"G0132576-201403000-00000\"" + BEFORE;
  private static final String TestCOVER_AFTER  = "<COVER NAME=\"G0132576-201403000-00000\"" + AFTER;
  private static final String TestSPP_BEFORE = "<IS><SPP SUPNUM=\"Supplement 1\""+BEFORE+"<IP>1</IP></IS>";
  private static final String TestSPP_AFTER  = "<IS><SPP SUPNUM=\"Supplement 1\""+AFTER+"<IP>1</IP></IS>";
  private static final String TestTGP_BEFORE = "<TB ID=\"T1-3\"><TI>TABLE 1</TI><TGP FILE=\"G3TT1\""+BEFORE+"></TB></LV2>";
  private static final String TestTGP_AFTER  = "<TB ID=\"T1-3\"><TI>TABLE 1</TI><TGP FILE=\"G3TT1\""+AFTER+"></TB></LV2>";
  private static final String TestXUI_BEFORE = "<XUI XDB=\"pub-doi\" UI=\"10.1097/ADT.0b013e31827b4440\""+BEFORE+">";
  private static final String TestXUI_AFTER  = "<XUI XDB=\"pub-doi\" UI=\"10.1097/ADT.0b013e31827b4440\""+AFTER+">";
  private static final String TestMATH_BEFORE = "<MATH TEST=\"hello\"" + BEFORE;
  private static final String TestMATH_AFTER  = "<MATH TEST=\"hello\"" + AFTER;


  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  public TestWoltersKluwerSgmlAdapter() throws Exception {
    super.setUp();
  }

  public void testSgmlAdapter() throws Exception {
    ArrayList<String> beforeList = new ArrayList<String>(); 
    ArrayList<String> afterList = new ArrayList<String>(); 

    beforeList.add(TestCOVER_BEFORE);  afterList.add(TestCOVER_AFTER);
    beforeList.add(TestSPP_BEFORE);    afterList.add(TestSPP_AFTER);
    beforeList.add(TestTGP_BEFORE);    afterList.add(TestTGP_AFTER);
    beforeList.add(TestXUI_BEFORE);    afterList.add(TestXUI_AFTER);
    beforeList.add(TestMATH_BEFORE);    afterList.add(TestMATH_AFTER);


    LineRewritingReader sReader = new WoltersKluwerSgmlAdapter(new StringReader(TestCOVER_BEFORE));
    for (int i = 0; i < beforeList.size(); i++) {
      String filteredStr = sReader.rewriteLine(beforeList.get(i));
      //log.info(filteredStr);
      assertEquals(filteredStr, afterList.get(i));
      //log.info(afterList.get(i));
    }
  }
  
  public static final String SGML_REGEX = ".*/[^/]+\\.zip!/(.*)\\.[\\d]{1,2}$";

  public void testSgmlRegex() throws Exception {
    String inS0 = "hello/world.zip!/CHECKTHIS.";
    String s1 = null;
    // tests for digits (up to 2) as suffix
    for (int i = 0; i < 11; i++) {
      s1 = new String(inS0 + i);
      assertTrue(s1.matches(SGML_REGEX));
    }
    // checks a digit+char as suffix
    s1 = new String(inS0 + "3g");
    assertFalse(s1.matches(SGML_REGEX));
   
  }

}