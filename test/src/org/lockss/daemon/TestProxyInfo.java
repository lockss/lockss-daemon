/*
 * $Id: TestProxyInfo.java,v 1.3 2003-09-23 07:49:58 eaalto Exp $
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

import java.io.IOException;
import java.util.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.protocol.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import gnu.regexp.RE;
import gnu.regexp.REException;

/**
 * Test class for ProxyInfo.
 */
public class TestProxyInfo extends LockssTestCase {
  static final String HOST = "host.org";

  private ProxyInfo pi;

  public void setUp() throws Exception {
    super.setUp();
    pi = new ProxyInfo(HOST);
  }

  public void testGetProxyHost() {
    String h = "1.3.4.22";
    Properties p = new Properties();
    p.put(IdentityManager.PARAM_LOCAL_IP, h);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    assertEquals(h, new ProxyInfo().getProxyHost());
    assertEquals("foo", new ProxyInfo("foo").getProxyHost());
  }

  // platform param should supersede local ip
  public void testGetProxyHostFromPlatform() {
    String h = "fq.dn.org";
    Properties p = new Properties();
    p.put(ConfigManager.PARAM_PLATFORM_FQDN, h);
    p.put(IdentityManager.PARAM_LOCAL_IP, "superseded.by.platform");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    assertEquals(h, new ProxyInfo().getProxyHost());
    assertEquals("foo", new ProxyInfo("foo").getProxyHost());
  }

  String ifsRE =
    " if \\(shExpMatch\\(url, \\\"http://foo\\.bar/\\*\\\"\\)\\)\\n" +
    " { return \\\"PROXY host\\.org:9090\\\"; }\\n\\n" +
    " if \\(shExpMatch\\(url, \\\"http://x\\.com/\\*\\\"\\)\\)\\n" +
    " { return \\\"PROXY host\\.org:9090\\\"; }\\n\\n";

  List urlStems = ListUtil.list("http://foo.bar", "http://x.com");

  Map makeUrlStemMap() {
    Map map = new HashMap();
    for (Iterator iter = urlStems.iterator(); iter.hasNext(); ) {
      String urlStem = (String)iter.next();
      ArchivalUnit au = new MockArchivalUnit();
      map.put(urlStem, au);
    }
    return map;
  }

  public void testGeneratePacEntry() throws Exception {
    RE re = new RE(ifsRE);
    StringBuffer sb = new StringBuffer();
    for (Iterator iter = urlStems.iterator(); iter.hasNext(); ) {
      String urlStem = (String)iter.next();
      pi.generatePacEntry(sb, urlStem);
    }
    assertTrue("Fragments didn't match RE:\n" + sb.toString(),
	       re.isMatch(sb.toString()));
  }

  public void testGeneratePacFile() throws Exception {
    String headRE =
      "// PAC file generated .* by LOCKSS cache .*\\n\\n" +
      "function FindProxyForURL\\(url, host\\) {\\n";
    String tailRE = " return \\\"DIRECT\\\";\\n}\\n";
    RE re = new RE(headRE + ifsRE + tailRE);
    String pf = pi.generatePacFile(makeUrlStemMap());
    assertTrue("PAC file didn't match RE.  File contents:\n" + pf,
	       re.isMatch(pf));
  }

  String frag = "Proxy host.org:9090\n" +
    "Title foo\n" +
    "URL http://foo.bar\n" +
    "Domain foo.bar\n\n";

  public void testGenerateEZProxyEntry() throws Exception {
    StringBuffer sb = new StringBuffer();
    pi.generateEZProxyEntry(sb, "http://foo.bar", "foo");
    assertEquals(frag, sb.toString());
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestProxyInfo.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
