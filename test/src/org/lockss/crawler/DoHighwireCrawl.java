/*
 * $Id: DoHighwireCrawl.java,v 1.2 2002-10-08 01:08:31 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import java.net.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.highwire.*;
import org.lockss.proxy.*;

public class DoHighwireCrawl {

  public static void main(String args[]){
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
    String start = args[i];

    ArchivalUnit au = new HighWirePlugin("http://shadow3.stanford.edu", 324);
    CachedUrlSetSpec cuss = new GenericCachedUrlSetSpec(start, "");
    au.addToList(cuss);
    if (proxyFlg) {
      org.lockss.plugin.Plugin.registerArchivalUnit(au);
      ProxyHandler.startProxy();
      System.err.println("Proxy started");

    }
    if (crawlFlg) {
      Crawler.doCrawl(au);
    }
  }
}
