/*
 * $Id: RunDaemon.java,v 1.38 2003-06-25 21:19:58 eaalto Exp $
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

import java.util.*;
import java.io.*;
import java.net.*;

import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.hasher.HashService;
import org.lockss.protocol.LcapComm;
import org.lockss.plugin.simulated.*;
import org.lockss.test.*;
import org.lockss.protocol.*;
import org.lockss.poller.*;
import org.lockss.util.*;
import org.lockss.crawler.*;
import org.lockss.repository.*;
import org.lockss.app.*;
import org.lockss.plugin.*;
import org.lockss.state.*;

public class RunDaemon
    extends LockssDaemon {
  private static final String DEFAULT_DIR_PATH = "./";

  static final String PARAM_CACHE_LOCATION =
      LockssRepositoryImpl.PARAM_CACHE_LOCATION;

  static final String PARAM_REG_SIMUL_STATUS_ACCESSOR =
    Configuration.PREFIX + "shouldRegisterSimSA";

  static final String PARAM_CALL_POLL = Configuration.PREFIX
      + "test.doPoll";
  static final String PARAM_RUN_TREEWALK = Configuration.PREFIX
      + "test.doTreewalk";
  static final String PARAM_TREEWALK_AUID = Configuration.PREFIX
      + "test.treewalk.auId";
  static final String PARAM_POLL_TYPE = Configuration.PREFIX
      + "test.polltype";
  static final String PARAM_PS_AUID = Configuration.PREFIX
      + "test.pollspec.auId";
  static final String PARAM_PS_URL = Configuration.PREFIX
      + "test.pollspec.url";
  static final String PARAM_PS_LWRBND = Configuration.PREFIX
      + "test.pollspec.lwrBound";
  static final String PARAM_PS_UPRBND = Configuration.PREFIX
      + "test.pollspec.uprBound";

  private static Logger log = Logger.getLogger("RunDaemon");

  public static void main(String argv[]) {
    Vector urls = new Vector();
    for (int i = 0; i < argv.length; i++) {
      urls.add(argv[i]);
    }
    try {
      RunDaemon daemon = new RunDaemon(urls);
      daemon.runDaemon();
    }
    catch (Throwable e) {
      System.err.println("Exception thrown in main loop:");
      e.printStackTrace();
    }
  }

  protected RunDaemon(List propUrls) {
    super(propUrls);
  }

  public void runDaemon() throws Exception {
    super.runDaemon();

    boolean testPoll = Configuration.getBooleanParam(PARAM_CALL_POLL,
        false);

    boolean testTreeWalk = Configuration.getBooleanParam(PARAM_RUN_TREEWALK,
        false);

    boolean registerSimulatedStatusAccessor =
      Configuration.getBooleanParam(PARAM_REG_SIMUL_STATUS_ACCESSOR, false);

    if (registerSimulatedStatusAccessor) {
      SimulatedStatusAccessor.register(this);
    }

    if(testTreeWalk) {
      runTreeWalk();
    }

    if (testPoll) {
      callPoll();
    }
  }

  private void runTreeWalk() {
    ArchivalUnit au;
    String auId = Configuration.getParam(PARAM_TREEWALK_AUID);
    PluginManager pluginMgr = getPluginManager();
    if(auId != null) {
      au = pluginMgr.getAuFromId(auId);
      if (au != null) {
	log.info("starting tree walk for auId " + auId);
	startWalk(au);
      } else {
	log.error("No AU with id " + auId);
      }
    }
    else {
      Iterator iter = pluginMgr.getAllAUs().iterator();
      while(iter.hasNext()) {
        au = (ArchivalUnit) iter.next();
        startWalk(au);
      }
    }

  }

  private void startWalk(ArchivalUnit au) {
    NodeManager nodeMgr = getNodeManager(au);
    nodeMgr.forceTreeWalk();
  }

  private void callPoll() {
    int poll_type = Configuration.getIntParam(PARAM_POLL_TYPE,
                                              LcapMessage.CONTENT_POLL_REQ);
    String auId = Configuration.getParam(PARAM_PS_AUID);
    String url = Configuration.getParam(PARAM_PS_URL, "LOCKSSAU:");
    String lwrBound = Configuration.getParam(PARAM_PS_LWRBND);
    String uprBound = Configuration.getParam(PARAM_PS_UPRBND);

    PollSpec spec = new PollSpec(auId, url,lwrBound,uprBound, null);

    CachedUrlSet cus = getPluginManager().findCachedUrlSet(spec);
    try {
      Thread.currentThread().sleep(10000);
      getPollManager().sendPollRequest(poll_type, new PollSpec(cus));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
