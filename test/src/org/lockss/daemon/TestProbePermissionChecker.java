/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.test.*;
import org.lockss.daemon.ProbePermissionChecker;

public class TestProbePermissionChecker extends LockssTestCase {

  private TestableProbePermissionChecker pc;
  MockArchivalUnit mau;

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
  }

  public void testNoLinkTag() {
    String url = "http://www.example.com";
    mau.addUrl(url, true, true);
    mau.addContent(url, htmlSourceWOProbe);
    
    pc = new TestableProbePermissionChecker();
    assertNull(pc.getProbeUrl());
  }

  public void testNoProbe() {
    String url = "http://www.example.com";
    mau.addUrl(url, true, true);
    mau.addContent(url, htmlSourceWOProbe);

    pc = new TestableProbePermissionChecker();
    assertNull(pc.getProbeUrl());
  }

  public void testProbeHasPermission() {
    String probeUrl = "http://www.example.com/cgi/content/full/14/9/1109";

    String url = "http://www.example.com";
    mau.addUrl(url, true, true);
    mau.addContent(url, htmlSourceWProbe);
    mau.addUrl(probeUrl, true, true);
    mau.addContent(probeUrl, "");

    pc = new TestableProbePermissionChecker();
    assertEquals(probeUrl, pc.getProbeUrl());
  }
  
  public void testProbeHasPermissionNotInCrawlRule() {
    String probeUrl = "http://www.example.com/cgi/content/full/14/9/1109";

    String url = "http://www.example.com";
    mau.addUrl(url, true, true);
    mau.addContent(url, htmlSourceWProbe);
    mau.addUrl(probeUrl, true, false);
    mau.addContent(probeUrl, "");

    pc = new TestableProbePermissionChecker();
    assertEquals(probeUrl, pc.getProbeUrl());
  }

  public void testProbeHasPermission2() {
    String probeUrl = "http://www.example.com/cgi/content/full/14/9/1109";

    String url = "http://www.example.com";
    mau.addUrl(url, true, true);
    mau.addContent(url, htmlSourceWProbe);
    mau.addUrl(probeUrl, true, true);
    mau.addContent(probeUrl, "");

    pc = new TestableProbePermissionChecker();
    assertEquals(probeUrl, pc.getProbeUrl());
  }

  
  public class TestableProbePermissionChecker extends ProbePermissionChecker {
    public String getProbeUrl() {
      return probeUrl;
    }
  }
}
