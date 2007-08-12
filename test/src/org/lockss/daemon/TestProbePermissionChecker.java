/*
 * $Id: TestProbePermissionChecker.java,v 1.8 2007-08-12 01:48:05 tlipkis Exp $
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
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.urlconn.*;
import org.lockss.plugin.*;

public class TestProbePermissionChecker extends LockssTestCase {

  private ProbePermissionChecker pc;
  MockArchivalUnit mau;
  MyPermissionHelper helper;

  private static String htmlSourceWOLinkTag =
    "<html>\n"+
    "  <head>\n"+
    "    <title>\n"+
    "      Human Molecular Genetics Volume 14 LOCKSS Manifest Page\n"+
    "    </title>\n"+
    "  </head>\n"+
    "    <body>\n"+
    "      <h1>      Human Molecular Genetics Volume 14 LOCKSS Manifest Page</h1>\n"+
    "      <ul>\n"+
    "    </body>\n"+
    "</html>\n";

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


  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    helper = new MyPermissionHelper(mau);
  }




  public void testConstructorNullChecker() {
    try {
      new ProbePermissionChecker(null, new MockArchivalUnit());
      fail("Calling ProbePermissionChecker constructor with a null permission checker should throw");
    } catch (NullPointerException ex) {
    }
  }

  public void testConstructorNullArchivalUnit() {
    try {
      new ProbePermissionChecker(new MockLoginPageChecker(), null);
      fail("Calling ProbePermissionChecker constructor with a null Archival Unit should throw");
    } catch (NullPointerException ex) {
    }
  }

  public void testNoLinkTag() {
    String url = "http://www.example.com";
    mau.addUrl(url, true, true);
    mau.addContent(url, htmlSourceWOProbe);

    pc = new ProbePermissionChecker(new MockLoginPageChecker(), mau);
    assertFalse("Incorrectly gave permission when there was no probe",
		pc.checkPermission(helper,
				   new StringReader(htmlSourceWOLinkTag),
				   url));
  }


  public void testNoProbe() {
    String url = "http://www.example.com";
    mau.addUrl(url, true, true);
    mau.addContent(url, htmlSourceWOProbe);

    pc = new ProbePermissionChecker(new MockLoginPageChecker(), mau);
    assertFalse("Incorrectly gave permission when there was no probe",
		pc.checkPermission(helper, new StringReader(htmlSourceWOProbe),
				   url));
  }

  public void testProbeNoLoginPage() {
    String probeUrl = "http://www.example.com/cgi/content/full/14/9/1109";

    String url = "http://www.example.com";
    mau.addUrl(url, true, true);
    mau.addContent(url, htmlSourceWProbe);
    mau.addUrl(probeUrl, true, true);
    mau.addContent(probeUrl, "");

    MockLoginPageChecker mockLPC = new MockLoginPageChecker(false);

    pc = new ProbePermissionChecker(mockLPC, mau);
    pc = makeProbePC(new MockLoginPageChecker(false));
    assertTrue("Didn't give permission when there was a probe",
		pc.checkPermission(helper, new StringReader(htmlSourceWProbe),
				   "http://www.example.com"));
  }

  public void testProbeIsLoginPage() {
    String probeUrl = "http://www.example.com/cgi/content/full/14/9/1109";

    String url = "http://www.example.com";
    mau.addUrl(url, true, true);
    mau.addContent(url, htmlSourceWProbe);
    mau.addUrl(probeUrl, true, true);
    MockUrlCacher muc = (MockUrlCacher)mau.makeUrlCacher(probeUrl);
    muc.setCachingException(new CacheException.PermissionException("Found a login page"),
			    1);

    MockLoginPageChecker mockLPC = new MockLoginPageChecker(true);

    pc = new ProbePermissionChecker(mockLPC, mau);
    pc = makeProbePC(new MockLoginPageChecker(true));
    assertFalse("Gave permission when the nested checker denied it",
	       pc.checkPermission(helper, new StringReader(htmlSourceWProbe),
				  "http://www.example.com"));

  }

  private ProbePermissionChecker makeProbePC(LoginPageChecker lpc) {
    return new ProbePermissionChecker(lpc, mau);
  }

  static class MyPermissionHelper extends MockPermissionHelper {
    ArchivalUnit au;

    MyPermissionHelper(ArchivalUnit au) {
      this.au = au;
    }

    public UrlCacher makeUrlCacher(String url) {
      return au.makeUrlCacher(url);
    }
  }

  private static class MockLoginPageChecker implements LoginPageChecker {
    private boolean returnVal = false;

    public MockLoginPageChecker() {
      this(false);
    }

    public MockLoginPageChecker(boolean returnVal) {
      this.returnVal = returnVal;
    }


    public boolean isLoginPage(Properties props, Reader reader) {
      return returnVal;
    }
  }
}
