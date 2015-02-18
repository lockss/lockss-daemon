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

package org.lockss.plugin.projmuse;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestProjectMuseHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private ProjectMuseHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new ProjectMuseHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String frequentHtml =
          "<h2 class=\"rightnav box-nav\" id=\"freq_downloaded\"><a title=\"Frequently Downloaded\" id=\"freq_downloaded\">Frequently Downloaded</a></h2>" +
          "<ul class=\"rightnav\" id=\"freq_downloaded-box\">" +
          "<li><a> The Rentier State and National Oil Companies: An Economic and Political Perspective</a></li>" +   
          "<li><a> Democratic Islamization in Pakistan and Turkey: Lessons for the Post-Arab Spring Muslim World</a></li>" +   
          "<li><a> Lebanon after the Civil War: Peace or the Illusion of Peace?</a></li>" +    
          "</ul>" +
          "<div class=\"legend\">THIS CONTENT STAYS</div>";
// All html tags get removed by the projmuse filter rule after hash filtering
  private static final String frequentHtmlFiltered =
      "THIS CONTENT STAYS";
  
  public void testFiltering() throws Exception {
    InputStream inA;
//    InputStream inB;
    
    /* impactFactor test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(frequentHtml),
        ENC);

    assertEquals(frequentHtmlFiltered,StringUtil.fromInputStream(inA));


  }
}