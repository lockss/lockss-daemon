/*
 * $Id: TestGPOFDSysHtmlFilterFactory.java,v 1.2 2012-03-23 05:46:10 thib_gc Exp $
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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.test.*;

public class TestGPOFDSysHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private GPOFDSysHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new GPOFDSysHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String inst1 = 
      "<a href=\"search/notificationPage.action?emailBody=" +
      "http%3A%2F%2Fwww.gpo.gov%3A80%2Ffdsys%2Fgranule%2" +
      "FWCPD-2005-01-03%2FWCPD-2005-01-03-Pg3017-2%2Fcontent-detail.html" +
      "%3Fnull\">Email a link to this page</a>";
  
  private static final String inst2 =
	  "<a href=\"search/notificationPage.action?emailBody=" +
      "http%3A%2F%2Fwww.gpo.gov%3A80%2Ffdsys%2Fpkg%2" +
      "FERIC-ED465240%2Fcontent-detail.html%3Fnull\">" +
      "Email a link to this page</a>";

  public void testFiltering() throws PluginException, IOException {
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