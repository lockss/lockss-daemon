/*
 * $Id: DoHighwireCrawl.java,v 1.19 2003-09-26 23:50:38 eaalto Exp $
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

package org.lockss.crawler;
import java.util.*;
import java.net.URL;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.highwire.*;
import org.lockss.proxy.ProxyHandler;
import org.lockss.util.Deadline;
import org.lockss.test.*;

public class DoHighwireCrawl {

  private static HighWireArchivalUnit makeAu(URL url, int volume)
      throws ArchivalUnit.ConfigurationException, ClassNotFoundException {
    Properties props = new Properties();
    props.setProperty(HighWirePlugin.AUPARAM_VOL, Integer.toString(volume));
    props.setProperty(HighWirePlugin.AUPARAM_BASE_URL, url.toString());
    Configuration config = ConfigurationUtil.fromProps(props);
    HighWireArchivalUnit au = new HighWireArchivalUnit(null);
    au.setConfiguration(config);
    return au;
  }

  public static void main(String args[]) throws Exception {
    boolean proxyFlg = false;
    boolean crawlFlg = false;
    int i = 0;
    while (i < args.length && args[i].startsWith("-")) {
      if (args[i].equals("-p")) {
	proxyFlg = true;
      }
      if (args[i].equals("-c")) {
	crawlFlg = true;
      }
      i++;
    }
    if (i >= args.length) {
      System.err.println("Base URL arg required");
      System.exit(-1);
    }
    URL base = new URL(args[i]);
    int volume = Integer.parseInt(args[i+1]);

    ArchivalUnit au = makeAu(base, volume);
    MockLockssDaemon daemon = new MockLockssDaemon(null);
    daemon.startDaemon();
    PluginUtil.registerArchivalUnit(au);
    if (proxyFlg) {
//  This should already be started now.
//      daemon.getProxyHandler().startProxy();
      System.err.println("Proxy started");

    }
    if (crawlFlg) {
      Crawler crawler = 
	new GoslingCrawlerImpl(au, au.getCrawlSpec().getStartingUrls(),
			       Crawler.NEW_CONTENT, true);
      crawler.doCrawl(Deadline.MAX);
    }
  }
}
