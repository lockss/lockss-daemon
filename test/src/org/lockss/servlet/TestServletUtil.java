/*
 * $Id: TestServletUtil.java,v 1.7.14.1 2009-11-03 23:44:56 edwardsb1 Exp $
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
    assertEquals("http://host.edu:8081/BatchAuConfig?lockssAction=Backup",
                 ServletUtil.backupFileUrl("host.edu"));

    ConfigurationUtil.setFromArgs(AdminServletManager.PARAM_PORT, "1234");
    assertEquals("http://lib.edu:1234/BatchAuConfig?lockssAction=Backup",
                 ServletUtil.backupFileUrl("lib.edu"));

  }

  private MockArchivalUnit setUpAu(String name, String manifest,
                                   long lastCrawlTime) {
    MockLockssDaemon daemon = getMockLockssDaemon();
    Plugin pl = new MockPlugin(daemon);
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setName(name);
    mau.setPlugin(pl);
    mau.setCrawlSpec(new SpiderCrawlSpec(manifest, new MockCrawlRule()));
    PluginTestUtil.registerArchivalUnit(pl, mau);
    MockNodeManager nm = new MockNodeManager();
    daemon.setNodeManager(nm, mau);
    MockAuState aus = new MockAuState();
    nm.setAuState(aus);
    aus.setLastCrawlTime(lastCrawlTime);
    return mau;
  }

  public void testManifestIndexNotStarted() throws Exception {
    testManifestIndex(false);
  }

  public void testManifestIndex() throws Exception {
    testManifestIndex(true);
  }

  public void testManifestIndex(boolean started) throws Exception {
    MockLockssDaemon daemon = getMockLockssDaemon();
    daemon.setAusStarted(started);
    daemon.getPluginManager().setLoadablePluginsReady(started);

    String m1 = "http://foo.bar/manifest1.html";
    String m2 = "http://foo.bax/manifest2.html";
    String au1 = "Journal of Journalistics 1776";
    String au2 = "xyz";
    MockArchivalUnit mau = setUpAu(au1, m1, 1000);

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
      "<a href=\"" + m1 + "\">" + m1 + "</a></td></tr>" +
      "<tr><td align=\"left\">" + au2 + "</td>" + spacer +
      "<td align=\"left\">" +
      "<a href=\"" + m2 + "\">" + m2 + "</a> \\(not fully collected\\)" +
      "</td></tr></table>";
    Pattern pat =
      RegexpUtil.uncheckedCompile(pats, Perl5Compiler.MULTILINE_MASK);
    assertMatchesRE(pat, s);
  }
}
