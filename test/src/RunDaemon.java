/*
 * $Id: RunDaemon.java,v 1.4 2002-11-27 00:26:18 troberts Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.io.*;
import java.net.*;
import org.lockss.daemon.*;
import org.lockss.hasher.HashService;
import org.lockss.protocol.LcapComm;
import org.lockss.plugin.simulated.*;
import org.lockss.test.*;
import org.lockss.protocol.*;
import org.lockss.poller.*;
import org.lockss.util.*;
import org.lockss.crawler.*;

public class RunDaemon {
  private static final String DEFAULT_DIR_PATH = "./";
  private static Logger log = Logger.getLogger("RunDaemon");

  private SimulatedArchivalUnit sau = null;
  private List propUrls = null;
  private String dirPath = null;

  public static void main(String argv[]) {
    Vector urls = new Vector();
    for (int i=0; i<argv.length; i++) {
      urls.add(argv[i]);
    }
    try {
      RunDaemon daemon = new RunDaemon(urls);
      daemon.runDaemon();
    } catch (Throwable e) {
      System.err.println("Exception thrown in main loop:");
      e.printStackTrace();
    } finally {
//        System.err.println("Exiting");
//        System.exit(0);
    }
  }

  RunDaemon(List propUrls){
    this.propUrls = propUrls;
  }

  public void runDaemon() {
    Configuration.startHandler(propUrls);
    System.err.println("Sleeping so config can get set");
    Configuration.waitConfig();
    System.err.println("Awake");

    HashService.start();
    LcapComm.startComm();

    dirPath = Configuration.getParam(Configuration.PREFIX+"cache.location",
				     DEFAULT_DIR_PATH);
    boolean shouldCallPoll =
      Configuration.getBooleanParam(Configuration.PREFIX+"shouldCallPoll",
				    false);

    sau = new SimulatedArchivalUnit(dirPath);
    org.lockss.plugin.Plugin.registerArchivalUnit(sau);

    int poll_type = Configuration.getIntParam(Configuration.PREFIX+"poll.type",
        LcapMessage.CONTENT_POLL_REQ);

    createContent();
    crawlContent();
    if (shouldCallPoll) {
      try {
	Thread.currentThread().sleep(1000);
	PollManager.makePollRequest("http://www.example.com/", ".*",
				    poll_type,
				    5, InetAddress.getByName("239.4.5.6"),
				    1 * 60 * 1000, 60 * 1000);
      } catch (Exception e) {
	e.printStackTrace();
      }
    }
    else {
    }
  }

  private void createContent() {
    SimulatedContentGenerator scgen = sau.getContentGenerator();
    scgen.setTreeDepth(2);
    scgen.setNumBranches(2);
    scgen.setNumFilesPerBranch(2);
    scgen.setFileTypes(scgen.FILE_TYPE_HTML+scgen.FILE_TYPE_TXT);
    scgen.setAbnormalFile("1,1", 1);

    sau.generateContentTree();
  }

  private void crawlContent() {
    CrawlSpec spec = new CrawlSpec(sau.SIMULATED_URL_START, null);
    org.lockss.crawler.GoslingCrawlerImpl.doCrawl(sau, spec);
  }
}
