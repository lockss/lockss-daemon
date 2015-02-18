/*
 * $Id$
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.protocol.psm;

import java.util.*;
import EDU.oswego.cs.dl.util.concurrent.*;

import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.app.*;

/**
 * PsmManager Manager the interpreter thread pool.
 */
public class PsmManager extends BaseLockssDaemonManager
  implements ConfigurableManager {

  static Logger log = Logger.getLogger("PsmManager");

  public static final String PREFIX = Configuration.PREFIX + "psm.";

  /** Min threads in interp thread pool */
  public static final String PARAM_THREAD_POOL_MIN = PREFIX + "threadPool.min";
  public static final int DEFAULT_THREAD_POOL_MIN = 3;

  /** Max threads in interp thread pool */
  public static final String PARAM_THREAD_POOL_MAX = PREFIX + "threadPool.max";
  public static final int DEFAULT_THREAD_POOL_MAX = 100;

  /** Duration after which idle threads will be terminated..  -1 = never */
  public static final String PARAM_THREAD_POOL_KEEPALIVE =
    PREFIX + "threadPool.keepAlive";
  public static final long DEFAULT_THREAD_POOL_KEEPALIVE =
    10 * Constants.MINUTE;

  /** Time after which idle runner will exit.  (Return thread to pool.) */
  public static final String PARAM_RUNNER_IDLE_TIME =
    PREFIX + "runnerIdleTime";
  public static final long DEFAULT_RUNNER_IDLE_TIME = 10 * Constants.SECOND;

  static final String PRIORITY_PARAM_PSM = "PSM";
  static final int PRIORITY_DEFAULT_PSM = -1;

  private int paramMinPoolSize = DEFAULT_THREAD_POOL_MIN;
  private int paramMaxPoolSize = DEFAULT_THREAD_POOL_MAX;
  private long paramPoolKeepaliveTime = DEFAULT_THREAD_POOL_KEEPALIVE;
  private long paramRunnerIdleTimeout = DEFAULT_RUNNER_IDLE_TIME;
  private PooledExecutor pool;

  public PsmManager() {
  }

  public void startService() {
    super.startService();
    LockssDaemon daemon = getDaemon();
    resetConfig();
//     daemon.getStatusService() registerStatusAccessor(new PsmManagerStatus());
    pool = new PooledExecutor(paramMaxPoolSize);
    pool.setMinimumPoolSize(paramMinPoolSize);
    pool.setKeepAliveTime(paramPoolKeepaliveTime);
    log.debug2("Interp thread pool min, max: " +
	      pool.getMinimumPoolSize() + ", " + pool.getMaximumPoolSize());
    pool.waitWhenBlocked();
  }

  public void stopService() {
//     getDaemon().getStatusService().unregisterStatusAccessor("Psm"));
    if (pool != null) {
      pool.shutdownNow();
    }
    super.stopService();
  }

  /**
   * Set communication parameters from configuration, once only.
   * This service currently cannot be reconfigured.
   * @param config the Configuration
   */
  public void setConfig(Configuration config,
			Configuration prevConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      paramMinPoolSize = config.getInt(PARAM_THREAD_POOL_MIN,
				       DEFAULT_THREAD_POOL_MIN);
      paramMaxPoolSize = config.getInt(PARAM_THREAD_POOL_MAX,
				       DEFAULT_THREAD_POOL_MAX);
      paramPoolKeepaliveTime =
	config.getTimeInterval(PARAM_THREAD_POOL_KEEPALIVE,
			       DEFAULT_THREAD_POOL_KEEPALIVE);
      paramRunnerIdleTimeout =
	config.getTimeInterval(PARAM_RUNNER_IDLE_TIME,
			       DEFAULT_RUNNER_IDLE_TIME);
    }
  }

  long getRunnerIdleTimeout() {
    return paramRunnerIdleTimeout;
  }

  public PsmInterp newPsmInterp(PsmMachine stateMachine, Object userData) {
    return new PsmInterp(this, stateMachine, userData);
  }

  /**
   * Execute the runnable in a pool thread
   * @param run the Runnable to be run
   * @throws RuntimeException if no pool thread is available
   */
  void execute(Runnable run) throws InterruptedException {
    if (run == null)
      log.warning("Executing null", new Throwable());
    pool.execute(run);
  }

//   private static final List statusColDescs =
//     ListUtil.list(
// 		  new ColumnDescriptor("Peer", "Peer",
// 				       ColumnDescriptor.TYPE_STRING)
// 		  );

//   private class Status implements StatusAccessor, StatusAccessor.DebugOnly {
//     // port, proto, u/m, direction, compressed, pkts, bytes
//     long start;

//     public String getDisplayName() {
//       return "Comm Statistics";
//     }

//     public boolean requiresKey() {
//       return false;
//     }

//     public void populateTable(StatusTable table) {
// //       table.setResortable(false);
// //       table.setDefaultSortRules(statusSortRules);
//       String key = table.getKey();
//       table.setColumnDescriptors(statusColDescs);
//       table.setRows(getRows(key));
//       table.setSummaryInfo(getSummaryInfo(key));
//     }

//     private List getSummaryInfo(String key) {
//       List res = new ArrayList();
//       res.add(new StatusTable.SummaryInfo("Max channels",
// 					  ColumnDescriptor.TYPE_INT,
// 					  channels.getMaxSize()));
      
//       res.add(new StatusTable.SummaryInfo("Max rcvChannels",
// 					  ColumnDescriptor.TYPE_INT,
// 					  rcvChannels.getMaxSize()));
//       return res;
//     }

//     private List getRows(String key) {
//       List table = new ArrayList();
//       synchronized (channels) {
// 	for (Iterator iter = channels.entrySet().iterator(); iter.hasNext();) {
// 	  Map.Entry ent = (Map.Entry)iter.next();
// 	  PeerIdentity pid = (PeerIdentity)ent.getKey();
// 	  BlockingPeerChannel chan = (BlockingPeerChannel)ent.getValue();
// 	  table.add(makeRow(pid, chan));
// 	}
//       }
//       return table;
//     }

//     private Map makeRow(PeerIdentity pid, BlockingPeerChannel chan) {
//       Map row = new HashMap();
//       row.put("Peer", pid.getIdString());
//       return row;
//     }
//   }

//   private static final List chanStatusColDescs =
//     ListUtil.list(
// 		  new ColumnDescriptor("Peer", "Peer",
// 				       ColumnDescriptor.TYPE_STRING),
// 		  new ColumnDescriptor("State", "State",
// 				       ColumnDescriptor.TYPE_STRING),
// 		  new ColumnDescriptor("Flags", "Flags",
// 				       ColumnDescriptor.TYPE_STRING),
// 		  new ColumnDescriptor("SendQ", "SendQ",
// 				       ColumnDescriptor.TYPE_INT),
// 		  new ColumnDescriptor("Sent", "Msgs Sent",
// 				       ColumnDescriptor.TYPE_INT),
// 		  new ColumnDescriptor("Rcvd", "Msgs Rcvd",
// 				       ColumnDescriptor.TYPE_INT),
// 		  new ColumnDescriptor("SentBytes", "Bytes Sent",
// 				       ColumnDescriptor.TYPE_INT),
// 		  new ColumnDescriptor("RcvdBytes", "Bytes Rcvd",
// 				       ColumnDescriptor.TYPE_INT),
// 		  new ColumnDescriptor("LastSend", "LastSend",
// 				       ColumnDescriptor.TYPE_STRING),
// 		  new ColumnDescriptor("LastRcv", "LastRcv",
// 				       ColumnDescriptor.TYPE_STRING),
// 		  new ColumnDescriptor("PrevState", "PrevState",
// 				       ColumnDescriptor.TYPE_STRING),
// 		  new ColumnDescriptor("PrevStateChange", "Change",
// 				       ColumnDescriptor.TYPE_STRING)
// 		  );

//   private class ChannelStatus implements StatusAccessor {
//     long start;

//     public String getDisplayName() {
//       return "Comm Channels";
//     }

//     public boolean requiresKey() {
//       return false;
//     }

//     public void populateTable(StatusTable table) {
// //       table.setResortable(false);
// //       table.setDefaultSortRules(statusSortRules);
//       String key = table.getKey();
//       ChannelStats cumulative = new ChannelStats();
//       table.setColumnDescriptors(chanStatusColDescs);
//       table.setRows(getRows(key, cumulative));
//       cumulative.add(globalStats);
//       table.setSummaryInfo(getSummaryInfo(key, cumulative));
//     }

//     private List getSummaryInfo(String key, ChannelStats stats) {
//       List res = new ArrayList();
//       res.add(new StatusTable.SummaryInfo("Channels",
// 					  ColumnDescriptor.TYPE_STRING,
// 					  channels.size() + "/"
// 					  + paramMaxChannels + ", "
// 					  + channels.getMaxSize() + " max"));
//       res.add(new StatusTable.SummaryInfo("RcvChannels",
// 					  ColumnDescriptor.TYPE_STRING,
// 					  rcvChannels.size() + ", "
// 					  + rcvChannels.getMaxSize() +" max"));
//       res.add(new StatusTable.SummaryInfo("Draining",
// 					  ColumnDescriptor.TYPE_STRING,
// 					  drainingChannels.size() + ", "
// 					  + maxDrainingChannels + " max"));
//       ChannelStats.Count count = stats.getInCount();
//       res.add(new StatusTable.SummaryInfo("Msgs Sent",
// 					  ColumnDescriptor.TYPE_INT,
// 					  count.getMsgs()));
//       res.add(new StatusTable.SummaryInfo("Bytes Sent",
// 					  ColumnDescriptor.TYPE_INT,
// 					  count.getBytes()));
//       count = stats.getOutCount();
//       res.add(new StatusTable.SummaryInfo("Msgs Rcvd",
// 					  ColumnDescriptor.TYPE_INT,
// 					  count.getMsgs()));
//       res.add(new StatusTable.SummaryInfo("Bytes Rcvd",
// 					  ColumnDescriptor.TYPE_INT,
// 					  count.getBytes()));
//       return res;
//     }

//     private List getRows(String key, ChannelStats cumulative) {
//       List table = new ArrayList();
//       synchronized (channels) {

// 	for (Iterator iter = channels.entrySet().iterator(); iter.hasNext();) {
// 	  Map.Entry ent = (Map.Entry)iter.next();
// 	  PeerIdentity pid = (PeerIdentity)ent.getKey();
// 	  BlockingPeerChannel chan = (BlockingPeerChannel)ent.getValue();
// 	  table.add(makeRow(pid, chan, "", cumulative));
// 	}
// 	for (Iterator iter = rcvChannels.entrySet().iterator();
// 	     iter.hasNext();) {
// 	  Map.Entry ent = (Map.Entry)iter.next();
// 	  PeerIdentity pid = (PeerIdentity)ent.getKey();
// 	  BlockingPeerChannel chan = (BlockingPeerChannel)ent.getValue();
// 	  table.add(makeRow(pid, chan, "2", cumulative));
// 	}
// 	for (BlockingPeerChannel chan : drainingChannels) {
// 	  table.add(makeRow(chan.getPeer(), chan, "D", cumulative));
// 	}
//       }
//       return table;
//     }

//     private Map makeRow(PeerIdentity pid, BlockingPeerChannel chan,
// 			String flags, ChannelStats cumulative) {
//       Map row = new HashMap();
//       row.put("Peer", pid.getIdString());
//       row.put("State", chan.getState());
//       row.put("SendQ", chan.getSendQueueSize());
//       ChannelStats stats = chan.getStats();
//       cumulative.add(stats);
//       ChannelStats.Count count = stats.getInCount();
//       row.put("Sent", count.getMsgs());
//       row.put("SentBytes", count.getBytes());
//       count = stats.getOutCount();
//       row.put("Rcvd", count.getMsgs());
//       row.put("RcvdBytes", count.getBytes());
//       StringBuilder sb = new StringBuilder(flags);
//       if (chan.isOriginate()) sb.append("O");
//       if (chan.hasConnecter()) sb.append("C");
//       if (chan.hasReader()) sb.append("R");
//       if (chan.hasWriter()) sb.append("W");
//       row.put("Flags", sb.toString());
//       row.put("LastSend", lastTime(chan.getLastSendTime()));
//       row.put("LastRcv", lastTime(chan.getLastRcvTime()));
//       if (chan.getPrevState() != BlockingPeerChannel.ChannelState.NONE) {
// 	row.put("PrevState", chan.getPrevState());
// 	row.put("PrevStateChange", lastTime(chan.getLastStateChange()));
//       }
//       return row;
//     }

//     String lastTime(long time) {
//       if (time <= 0) return "";
//       return StringUtil.timeIntervalToString(TimeBase.msSince(time));
//     }
//   }

}
