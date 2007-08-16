/*
 * $Id: TestServletUtil.java,v 1.4 2007-08-16 02:38:54 tlipkis Exp $
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

    ConfigurationUtil.setFromArgs(LocalServletManager.PARAM_PORT, "1234");
    assertEquals("http://lib.edu:1234/BatchAuConfig?lockssAction=Backup",
		 ServletUtil.backupFileUrl("lib.edu"));

  }

  public void testManifestIndexNotStarted() throws Exception {
    testManifestIndex(false);
  }

  public void testManifestIndex() throws Exception {
    testManifestIndex(true);
  }

  public void testManifestIndex(boolean started) throws Exception {
    MockLockssDaemon daemon = getMockLockssDaemon();
    daemon.getPluginManager().setLoadablePluginsReady(started);

    Plugin pl = new MockPlugin();
    String m1 = "http://foo.bar/manifest1.html";
    String m2 = "http://foo.bax/manifest2.html";
    String au1 = "Journal of Journalistics 1776";
    String au2 = "xyz";
    MockArchivalUnit mau;
    mau = new MockArchivalUnit();
    mau.setName(au1);
    mau.setPlugin(pl);
    mau.setCrawlSpec(new SpiderCrawlSpec(m1, new MockCrawlRule()));
    PluginTestUtil.registerArchivalUnit(pl, mau);
    mau.addUrl(m1, true, true);

    mau = new MockArchivalUnit();
    mau.setName(au2);
    mau.setPlugin(pl);
    mau.setCrawlSpec(new SpiderCrawlSpec(m2, new MockCrawlRule()));
    PluginTestUtil.registerArchivalUnit(pl, mau);

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
      "<tr><th>Archival Unit</th>" + spacer +
      "<th>Manifest</th></tr>" +
      "<tr><td align=\"left\">" + au1 + "</td.*>" + spacer +
      "<td align=\"left\">" +
      "<a href=\"" + m1 + "\">" + m1 + "</a></td></tr>" +
      "<tr><td align=\"left\">" + au2 + "</td>" + spacer +
      "<td align=\"left\">" +
      "<a href=\"" + m2 + "\">" + m2 + "</a> \\(not yet collected\\)" +
      "</td></tr></table>";
    Pattern pat =
      RegexpUtil.uncheckedCompile(pats, Perl5Compiler.MULTILINE_MASK);
    assertMatchesRE(pat, s);
  }
}
