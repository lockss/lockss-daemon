/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.servlet;

import java.util.*;
import java.io.*;

import org.mortbay.html.*;
import org.apache.oro.text.regex.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;

/**
 * Test class for org.lockss.servlet.ServletUtil
 */
public class TestServletUtil extends LockssTestCase {

  public void testBackupFileUrl() throws Exception {
    assertEquals("http://host.edu:8081/BatchAuConfig?lockssAction=SelectBackup",
		 ServletUtil.backupFileUrl("host.edu"));

    ConfigurationUtil.setFromArgs(AdminServletManager.PARAM_PORT, "1234");
    assertEquals("http://lib.edu:1234/BatchAuConfig?lockssAction=SelectBackup",
		 ServletUtil.backupFileUrl("lib.edu"));

  }

  private MockArchivalUnit setUpAu(String name, String manifest,
				   long lastCrawlTime) {
    MockLockssDaemon daemon = getMockLockssDaemon();
    Plugin pl = new MockPlugin(daemon);
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setName(name);
    mau.setPlugin(pl);
    mau.setStartUrls(ListUtil.list(manifest));
    mau.setCrawlRule(new MockCrawlRule());
    PluginTestUtil.registerArchivalUnit(pl, mau);
    MockNodeManager nm = new MockNodeManager();
    daemon.setNodeManager(nm, mau);
    MockAuState aus = new MockAuState();
    nm.setAuState(aus);
    aus.setLastCrawlTime(lastCrawlTime);
    return mau;
  }

  public void testManifestIndexNotStarted() throws Exception {
    testManifestIndex(false, false);
  }

  public void testManifestIndex() throws Exception {
    testManifestIndex(true, false);
  }

  public void testManifestIndexAccessUrl() throws Exception {
    testManifestIndex(true, true);
  }

  public void testManifestIndex(boolean started, boolean useAccessUrls)
      throws Exception {
    MockLockssDaemon daemon = getMockLockssDaemon();
    daemon.setAusStarted(started);
    daemon.getPluginManager().setLoadablePluginsReady(started);

    String au1 = "Journal of Journalistics 1776";
    String au2 = "xyz";

    String m1 = "http://foo.bar/manifest1.html";
    String m1a = "http://foo.bar/access1.html";
    String m2 = "http://foo.bax/manifest2.html";
    MockArchivalUnit mau = setUpAu(au1, m1, 1000);
    if (useAccessUrls) {
      mau.setAccessUrls(ListUtil.list(m1a));
    } else {
      m1a = m1;
    }

    mau = setUpAu(au2, m2, -1);

    Element ele = ServletUtil.manifestIndex(daemon, "host.edu");
    StringWriter sw = new StringWriter();
    ele.write(sw);
    sw.flush();
    String s0 = sw.toString();
    String s = StringUtil.trimNewlinesAndLeadingWhitespace(s0);
    String spacer = "<td width=8>&nbsp;</td>";
    String pats = "<table.*><tr><td align=\"center\" colspan=\"3\">" +
      "<font size=\"\\+2\"><b>Volume Manifests on host.edu</b></font>" +
      "</td></tr>" +
      (!started ? ("<tr><td align=\"center\" colspan=\"3\"><center>" +
		     "<font color=red size=\\+1>" +
		     "This LOCKSS box is still starting.  " +
		     "Table contents may be incomplete.</font></center>" +
		     "<br></td></tr>")
       : "") +
      "<tr><th align=left>Archival Unit</th>" + spacer +
      "<th align=left>Manifest</th></tr>" +
      "<tr><td align=\"left\">" + au1 + "</td.*>" + spacer +
      "<td align=\"left\">" +
      "<a href=\"" + m1a + "\">" + m1a + "</a></td></tr>" +
      "<tr><td align=\"left\">" + au2 + "</td>" + spacer +
      "<td align=\"left\">" +
      "<a href=\"" + m2 + "\">" + m2 + "</a> \\(not fully collected\\)" +
      "</td></tr></table>";
    Pattern pat =
      RegexpUtil.uncheckedCompile(pats, Perl5Compiler.MULTILINE_MASK);
    assertMatchesRE(pat, s);
  }

  public void testGetContentOriginalFilename() {
    CachedUrl cu;
    cu = new MockCachedUrl("http://example.com/foo/file.pdf");
    assertEquals("file.pdf", ServletUtil.getContentOriginalFilename(cu, false));
    assertEquals("\"file.pdf\"",
		 ServletUtil.getContentOriginalFilename(cu, true));
    cu = new MockCachedUrl("http://example.com/foo/file.html?query=foo");
    assertEquals("file.html",
		 ServletUtil.getContentOriginalFilename(cu, false));
    assertEquals("\"file.html\"",
		 ServletUtil.getContentOriginalFilename(cu, true));

    cu = new MockCachedUrl("https://www.here.there/articles/10.18352/ts.327/galley/319/download/");
    assertEquals("\"download\"",
		 ServletUtil.getContentOriginalFilename(cu, true));
  }

  public void testIsTabPopulated() {
    Map<String, Boolean> tabLetterPopulationMap =
	new HashMap<String, Boolean>();

    assertFalse(ServletUtil.isTabPopulated(1, 'A', tabLetterPopulationMap));
    assertFalse(ServletUtil.isTabPopulated(2, 'A', tabLetterPopulationMap));
    assertFalse(ServletUtil.isTabPopulated(26, 'A', tabLetterPopulationMap));

    tabLetterPopulationMap.put("A", Boolean.TRUE);
    tabLetterPopulationMap.put("C", Boolean.TRUE);
    tabLetterPopulationMap.put("D", Boolean.TRUE);
    tabLetterPopulationMap.put("G", Boolean.TRUE);
    tabLetterPopulationMap.put("H", Boolean.TRUE);
    tabLetterPopulationMap.put("I", Boolean.TRUE);
    tabLetterPopulationMap.put("M", Boolean.TRUE);
    tabLetterPopulationMap.put("N", Boolean.TRUE);
    tabLetterPopulationMap.put("O", Boolean.TRUE);
    tabLetterPopulationMap.put("P", Boolean.TRUE);
    tabLetterPopulationMap.put("U", Boolean.TRUE);
    tabLetterPopulationMap.put("V", Boolean.TRUE);
    tabLetterPopulationMap.put("W", Boolean.TRUE);
    tabLetterPopulationMap.put("X", Boolean.TRUE);
    tabLetterPopulationMap.put("Y", Boolean.TRUE);

    assertTrue(ServletUtil.isTabPopulated(1, 'A', tabLetterPopulationMap));
    assertTrue(ServletUtil.isTabPopulated(2, 'A', tabLetterPopulationMap));
    assertTrue(ServletUtil.isTabPopulated(26, 'A', tabLetterPopulationMap));
    assertFalse(ServletUtil.isTabPopulated(1, 'B', tabLetterPopulationMap));
    assertTrue(ServletUtil.isTabPopulated(2, 'B', tabLetterPopulationMap));
    assertTrue(ServletUtil.isTabPopulated(2, 'C', tabLetterPopulationMap));
    assertFalse(ServletUtil.isTabPopulated(2, 'E', tabLetterPopulationMap));
    assertTrue(ServletUtil.isTabPopulated(3, 'E', tabLetterPopulationMap));
    assertTrue(ServletUtil.isTabPopulated(3, 'G', tabLetterPopulationMap));
    assertTrue(ServletUtil.isTabPopulated(3, 'I', tabLetterPopulationMap));
    assertFalse(ServletUtil.isTabPopulated(3, 'J', tabLetterPopulationMap));
    assertTrue(ServletUtil.isTabPopulated(4, 'L', tabLetterPopulationMap));
    assertTrue(ServletUtil.isTabPopulated(4, 'M', tabLetterPopulationMap));
    assertTrue(ServletUtil.isTabPopulated(4, 'N', tabLetterPopulationMap));
    assertFalse(ServletUtil.isTabPopulated(4, 'Q', tabLetterPopulationMap));
  }
}
