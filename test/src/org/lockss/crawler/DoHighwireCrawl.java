/*
 * $Id: DoHighwireCrawl.java,v 1.30 2005-03-15 07:42:42 tlipkis Exp $
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
import java.net.*;
import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.base.*;

public class DoHighwireCrawl {
  static MockLockssDaemon theDaemon;

  private static DefinableArchivalUnit makeAu(URL url, int volume, int year)
      throws Exception {
    String baseUrlKey = ConfigParamDescr.BASE_URL.getKey();
    String yearKey = ConfigParamDescr.YEAR.getKey();
    String volKey = ConfigParamDescr.VOLUME_NUMBER.getKey();

    Properties props = new Properties();
    props.setProperty(volKey, Integer.toString(volume));
    props.setProperty(baseUrlKey, url.toString());
    props.setProperty(yearKey, String.valueOf(year));
    props.setProperty(BaseArchivalUnit.USE_CRAWL_WINDOW, ""+true);
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon, "org.lockss.plugin.highwire.HighWirePlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
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
    theDaemon = new MockLockssDaemon(null);
    theDaemon.startDaemon();

    ArchivalUnit au = makeAu(base, volume,2004);
    PluginUtil.registerArchivalUnit(au);
    if (proxyFlg) {
//  This should already be started now.
//      daemon.getProxyHandler().startProxy();
      System.err.println("Proxy started");

    }
    if (crawlFlg) {
      Crawler crawler = new NewContentCrawler(au, au.getCrawlSpec(), null);
      crawler.doCrawl();
    }
  }
}
