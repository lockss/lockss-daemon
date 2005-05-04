/*
 * $Id: TestProbePermissionChecker.java,v 1.1 2005-05-04 00:23:20 troberts Exp $
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
package org.lockss.daemon;

import java.io.*;
import org.lockss.test.*;

public class TestProbePermissionChecker extends LockssTestCase {

  private ProbePermissionChecker pc;

  private static String htmlSourceWProbe =
    "<html>\n"+
    "  <head>\n"+
    "    <title>\n"+
    "      Human Molecular Genetics Volume 14 LOCKSS Manifest Page\n"+
    "    </title>\n"+
    "    <link href=\"/cgi/content/full/14/9/1109\" lockss-probe=\"true\" />\n"+
    "  </head>\n"+
    "    <body>\n"+
    "      <h1>      Human Molecular Genetics Volume 14 LOCKSS Manifest Page</h1>\n"+
    "      <ul>\n"+
    "    </body>\n"+
    "</html>\n";
    
  private static String htmlSourceWOProbe =
    "<html>\n"+
    "  <head>\n"+
    "    <title>\n"+
    "      Human Molecular Genetics Volume 14 LOCKSS Manifest Page\n"+
    "    </title>\n"+
    "    <link href=\"/cgi/content/full/14/9/1109\" />\n"+
    "  </head>\n"+
    "    <body>\n"+
    "      <h1>      Human Molecular Genetics Volume 14 LOCKSS Manifest Page</h1>\n"+
    "      <ul>\n"+
    "    </body>\n"+
    "</html>\n";
		
	

	
		
		

  public void testConstructorNullChecker() {
    try {
      new ProbePermissionChecker(null, new MockArchivalUnit());
      fail("Calling ProbePermissionChecker constructor with a null permission checker should throw");
    } catch (NullPointerException ex) {
    }
  }

  public void testConstructorNullArchivalUnit() {
    try {
      new ProbePermissionChecker(new MockPermissionChecker(100), null);
      fail("Calling ProbePermissionChecker constructor with a null Archival Unit should throw");
    } catch (NullPointerException ex) {
    }
  }

  public void testNoProbe() {
    MockArchivalUnit mau = new MockArchivalUnit();
    String url = "http://www.example.com";
    mau.addUrl(url, true, true);
    mau.addContent(url, htmlSourceWOProbe);

    pc = new ProbePermissionChecker(new MockPermissionChecker(100),
				    mau);
    assertFalse("Incorrectly gave permission when there was no probe",
		pc.checkPermission(new StringReader(htmlSourceWOProbe),
				   url));
  }

  public void testProbe() {
    MockArchivalUnit mau = new MockArchivalUnit();
    String url = "http://www.example.com";
    mau.addUrl(url, true, true);
    mau.addContent(url, htmlSourceWProbe);

    pc = new ProbePermissionChecker(new MockPermissionChecker(100),
				    mau); 
    assertTrue("Gave permission when there was no probe",
		pc.checkPermission(new StringReader(htmlSourceWProbe),
				   "http://www.example.com"));
  }

  public void testProbeCheckerRefuses() {
    MockArchivalUnit mau = new MockArchivalUnit();
    String url = "http://www.example.com";
    mau.addUrl(url, true, true);
    mau.addContent(url, htmlSourceWProbe);

    pc = new ProbePermissionChecker(new MockPermissionChecker(0),
				    mau); 
    assertFalse("Gave permission when the nested checker denied it",
	       pc.checkPermission(new StringReader(htmlSourceWProbe),
				  "http://www.example.com"));
    
  }
}
