/*
 * $Id: TestProxyInfo.java,v 1.13 2006-03-13 22:35:54 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;

import org.lockss.config.ConfigManager;
import org.lockss.plugin.*;
import org.lockss.protocol.IdentityManager;
import org.lockss.test.*;
import org.lockss.util.*;

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
    Map map = new TreeMap();
    for (Iterator iter = urlStems.iterator(); iter.hasNext(); ) {
      String urlStem = (String)iter.next();
      ArchivalUnit au = new MockArchivalUnit();
      map.put(urlStem, au);
    }
    return map;
  }

  public void testGetUrlStemMap() throws Exception {
    String stem1 = "http://foo1/";
    String stem2 = "http://foo2/";
    String stem3 = "http://foo3/";
    getMockLockssDaemon().getPluginManager();
    MyMockArchivalUnit au1 = new MyMockArchivalUnit();
    au1.setUrlStems(ListUtil.list(stem1, stem2));
    MyRegistryArchivalUnit au2 =
      new MyRegistryArchivalUnit(new RegistryPlugin());
    au2.setUrlStems(ListUtil.list(stem3));
    Map map = pi.getUrlStemMap(ListUtil.list(au1, au2));
    assertSame(au1, map.get(stem1));
    assertSame(au1, map.get(stem2));
    assertEquals(2, map.size());
  }

  class MyMockArchivalUnit extends MockArchivalUnit {
    private Collection urlStems;
    public Collection getUrlStems() {
      return urlStems;
    }
    void setUrlStems(Collection urlStems) {
      this.urlStems = urlStems;
    }
  }

  class MyRegistryArchivalUnit extends RegistryArchivalUnit {
    private Collection urlStems;
    public MyRegistryArchivalUnit(RegistryPlugin plugin) {
      super(plugin);
    }
    public Collection getUrlStems() {
      return urlStems;
    }
    void setUrlStems(Collection urlStems) {
      this.urlStems = urlStems;
    }
  }

  public void testGeneratePacEntry() throws Exception {
    StringBuffer sb = new StringBuffer();
    for (Iterator iter = urlStems.iterator(); iter.hasNext(); ) {
      String urlStem = (String)iter.next();
      pi.generatePacEntry(sb, urlStem);
    }
    assertMatchesRE("Fragments didn't match RE:\n" + sb.toString(),
		    ifsRE, sb.toString());
  }

  public void testGeneratePacFile() throws Exception {
    String headRE =
      "// PAC file generated .* by LOCKSS cache .*\\n\\n" +
      "function FindProxyForURL\\(url, host\\) {\\n";
    String tailRE = " return \\\"DIRECT\\\";\\n}\\n";
    String pf = pi.generatePacFile(makeUrlStemMap());
    assertMatchesRE("PAC file didn't match RE.  File contents:\n" + pf,
		    headRE + ifsRE + tailRE, pf);
  }

  public void testGenerateEncapsulatedPacFile() throws Exception {
    String oldfile = "# foo\n" +
      "function FindProxyForURL(url, host) {\n" +
      "return some_logic(url, host);\n}\n";
    String encapsulated = "# foo\n" +
      "function FindProxyForURL_0(url, host) {\n" +
      "return some_logic(url, host);\n}\n";

    String headRE =
      "// PAC file generated .* by LOCKSS cache .*\\n\\n" +
      "function FindProxyForURL\\(url, host\\) {\\n";
    String tailRE = " return FindProxyForURL_0\\(url, host\\);\\n}\\n" +
      "// Encapsulated PAC file follows \\(msg\\)\\n\\n";
    String pat = headRE + ifsRE + tailRE +
      StringUtil.escapeNonAlphaNum(encapsulated);
    String pf = pi.encapsulatePacFile(makeUrlStemMap(), oldfile, " (msg)");
    assertMatchesRE("PAC file didn't match RE.  File contents:\n" + pf,
		    pat, pf);
  }

  String entry = "Title foo\n" +
    "URL http://foo.bar\n" +
    "Domain foo.bar\n\n";

  String frag =
    "Proxy host.org:9090\n" +
    "\n" +
    "Title MockAU\n" +
    "URL http://foo.bar\n" +
    "Domain foo.bar\n" +
    "\n" +
    "Title MockAU\n" +
    "URL http://x.com\n" +
    "Domain x.com\n" +
    "\n" +
    "Proxy\n";


  public void testFindUnusedName() throws Exception {
    String js1 = "function func0(foo, bar) { stmt; }\n";
    String js2 = "function func1(foo, bar) { stmt; }\n";
    String js3 = "function func00(foo, bar) { stmt; }\n";
    assertEquals("newname0", pi.findUnusedName(js1, "newname"));
    assertEquals("func1", pi.findUnusedName(js1, "func"));
    assertEquals("func2", pi.findUnusedName(js1 + js2, "func"));
    assertEquals("func0", pi.findUnusedName(js3, "func"));
    assertEquals("func01", pi.findUnusedName(js3, "func0"));
  }

  public void testJSReplace() throws Exception {
    String js1 =
      "function func(foo, bar) { func(bar, foo); func0(1,2); func_3(1) }\n";
    String exp =
      "function func1(foo, bar) { func1(bar, foo); func0(1,2); func_3(1) }\n";
    assertEquals(exp, pi.jsReplace(js1, "func", "func1"));
  }

  public void testRemoveCommentLines() {
    assertEquals("", removeCommentLines(""));
    assertEquals("foo", removeCommentLines("#bar\nfoo\n####"));
    assertEquals("foo\n", removeCommentLines("#bar\nfoo\n####\n"));
  }


  public void testGenerateEZProxyEntry() throws Exception {
    StringBuffer sb = new StringBuffer();
    pi.generateEZProxyEntry(sb, "http://foo.bar", "foo");
    assertEquals(entry, sb.toString());
  }

  public void testGenerateEZProxyFragment() throws Exception {
    String s = pi.generateEZProxyFragment(makeUrlStemMap());
    assertTrue(s.startsWith("#"));
    assertEquals(frag, removeCommentLines(s));
  }

  public void testGenerateSquidEntry() throws Exception {
    final String PROTOCOL = "anyproto://";
    final String DOT = ".";
    final String JOURNALX_DOT_COM = "journalx.com";

    assertEquals(DOT + JOURNALX_DOT_COM,
                 pi.generateSquidEntry(PROTOCOL + JOURNALX_DOT_COM));
  }

  String removeCommentLines(String s) {
    List lines = StringUtil.breakAt(s, '\n');
    for (ListIterator iter = lines.listIterator(); iter.hasNext(); ) {
      if (((String)iter.next()).startsWith("#")) {
	iter.remove();
      }
    }
    return StringUtil.separatedString(lines, "\n");
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestProxyInfo.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
