/*
 * $Id: PollSpec.java,v 1.19 2003-11-11 20:33:31 tlipkis Exp $
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

package org.lockss.poller;

import java.io.*;
import java.net.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.app.*;

/**
 * <p>Class implementing the concept of the set of URLs covered by a poll.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class PollSpec {
  /**
   * A lower bound value which indicates the poll should use a
   * {@link SingleNodeCachedUrlSetSpec} instead of a
   * {@link RangeCachedUrlSetSpec}.
   */
  public static final String SINGLE_NODE_LWRBOUND = ".";

  public static final String PARAM_USE_PROTOCOL_VERSION = Configuration.PREFIX
      +      "protocol.useProtocolVersion";

  public static final int DEFAULT_USE_PROTOCOL_VERSION = 1;

  private static Logger theLog=Logger.getLogger("PollSpec");
  private static LockssRandom theRandom = new LockssRandom();
  private String auId;
  private String url;
  private String uprBound = null;
  private String lwrBound = null;
  private CachedUrlSet cus = null;
  private PluginManager pluginMgr = null;
  private int version; // poll protocol version

  /**
   * constructor for a "mock" poll spec
   * @param auId the archival unit id
   * @param url the url
   * @param lwrBound the lower bound of the url
   * @param uprBound the upper bound of the url
   * @param cus the cached url set
   */
  public PollSpec(String auId, String url,
                  String lwrBound, String uprBound, CachedUrlSet cus) {
    this.auId = auId;
    this.url = url;
    this.uprBound = uprBound;
    this.lwrBound = lwrBound;
    this.cus = cus;
    this.version = Configuration.getIntParam(PARAM_USE_PROTOCOL_VERSION,
					  DEFAULT_USE_PROTOCOL_VERSION);
  }

  /**
   * construct a pollspec from an existing pollspec but change the
   * upper and lower boundary of the RangedCachedUrlSetSpec
   * @param cus the existing cached url set
   * @param lwrBound the new lower boundary
   * @param uprBound the new upper boundary
   */
  public PollSpec(CachedUrlSet cus, String lwrBound, String uprBound) {
    this.cus = cus;
    ArchivalUnit au = cus.getArchivalUnit();
    auId = au.getAuId();
    CachedUrlSetSpec cuss = cus.getSpec();
    url = cuss.getUrl();
    this.lwrBound = lwrBound;
    this.uprBound = uprBound;
    this.version = Configuration.getIntParam(PARAM_USE_PROTOCOL_VERSION,
					  DEFAULT_USE_PROTOCOL_VERSION);
  }

  /**
   * construct a pollspec from an existing pollspec but change the
   * upper and lower boundary of the RangedCachedUrlSetSpec
   * @param cus the existing cached url set
   * @param lwrBound the new lower boundary
   * @param uprBound the new upper boundary
   * @param version the protocol version to use
   */
  public PollSpec(CachedUrlSet cus,
		  String lwrBound,
		  String uprBound,
		  int version) {
    this.cus = cus;
    ArchivalUnit au = cus.getArchivalUnit();
    auId = au.getAuId();
    CachedUrlSetSpec cuss = cus.getSpec();
    url = cuss.getUrl();
    this.lwrBound = lwrBound;
    this.uprBound = uprBound;
    this.version = version;
  }

  /**
   * Construct a PollSpec from a CachedUrlSet
   * @param cus the CachedUrlSpec which defines the range of interest
   */
  public PollSpec(CachedUrlSet cus) {
    this.cus = cus;
    ArchivalUnit au = cus.getArchivalUnit();
    auId = au.getAuId();
    CachedUrlSetSpec cuss = cus.getSpec();
    url = cuss.getUrl();
    if (cuss instanceof RangeCachedUrlSetSpec) {
      RangeCachedUrlSetSpec rcuss = (RangeCachedUrlSetSpec)cuss;
      lwrBound = rcuss.getLowerBound();
      uprBound = rcuss.getUpperBound();
    } else if (cuss.isSingleNode()) {
      // not used, but needs to be set to allow this poll to overlap with
      // other ranged polls
      lwrBound = SINGLE_NODE_LWRBOUND;
    }
    this.version = Configuration.getIntParam(PARAM_USE_PROTOCOL_VERSION,
					  DEFAULT_USE_PROTOCOL_VERSION);
  }

  /**
   * Construct a PollSpec from a LcapMessage
   * @param msg the LcapMessage which defines the range of interest
   */
  public PollSpec(LcapMessage msg) {
    auId = msg.getArchivalId();
    url = msg.getTargetUrl();
    uprBound = msg.getUprBound();
    lwrBound = msg.getLwrBound();
    version = msg.getVersion();
    cus = getPluginManager().findCachedUrlSet(this);
  }

  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  public String getAuId() {
    return auId;
  }

  public String getUrl() {
    return url;
  }

  public String getLwrBound() {
    return lwrBound;
  }

  public String getUprBound() {
    return uprBound;
  }

  public String getRangeString() {
    if ((lwrBound!=null) && (lwrBound.equals(SINGLE_NODE_LWRBOUND))) {
      return "single node";
    }
    String lwrDisplay = lwrBound;
    String uprDisplay = uprBound;
    if (lwrBound != null || uprBound != null) {
      if (lwrBound != null && lwrBound.startsWith("/")) {
        lwrDisplay = lwrBound.substring(1);
      }
      if (uprBound != null && uprBound.startsWith("/")) {
        uprDisplay = uprBound.substring(1);
      }
      return lwrDisplay + " - " + uprDisplay;
    }
    return null;
  }

  public int getVersion() {
    return version;
  }

  long calcDuration(int opcode, CachedUrlSet cus, PollManager pm) {
    long ret = 0;
    int quorum = pm.getQuorum();
    switch (opcode) {
      case LcapMessage.NAME_POLL_REQ:
      case LcapMessage.NAME_POLL_REP:
        ret = pm.m_minNamePollDuration +
            theRandom.nextLong(pm.m_maxNamePollDuration -
			       pm.m_minNamePollDuration);
        theLog.debug2("Name Poll duration: " +
                      StringUtil.timeIntervalToString(ret));
        break;

      case LcapMessage.CONTENT_POLL_REQ:
      case LcapMessage.CONTENT_POLL_REP:
        long estTime = cus.estimatedHashDuration();
        theLog.debug3("CUS estimated hash duration = " + estTime);

        estTime = getAdjustedEstimate(estTime, pm);
        theLog.debug3("My adjusted hash duration = " + estTime);

        ret = estTime * pm.m_durationMultiplier * (quorum + 1);
        theLog.debug3("I think the poll should take: " + ret);

        if (ret < pm.m_minContentPollDuration) {
          theLog.info("My poll estimate (" + ret
                       + ") is too low, adjusting to the minimum: "
                       + pm.m_minContentPollDuration);
          ret = pm.m_minContentPollDuration;
        }
        else if (ret > pm.m_maxContentPollDuration) {
          theLog.warning("My poll estimate (" + ret
                       + ") is too high, adjusting to the maximum: "
                       + pm.m_maxContentPollDuration);
          ret = pm.m_maxContentPollDuration;
        }
        if(!canSchedulePoll(ret, estTime * (quorum+1), pm)) {
           ret = -1;
           theLog.info("Can't schedule this poll returning -1");
        }
        else {
          theLog.debug2("Content Poll duration: " +
                        StringUtil.timeIntervalToString(ret));
        }
        break;

      default:
    }

    return ret;
  }

  boolean canSchedulePoll(long pollTime, long neededTime, PollManager pm) {
    Deadline when = Deadline.in(pollTime);
    return pm.canHashBeScheduledBefore(neededTime, when);
  }

  long getAdjustedEstimate(long estTime, PollManager pm) {
    long my_estimate = estTime;
    long my_rate;
    long slow_rate = pm.getSlowestHashSpeed();
    try {
      my_rate = pm.getBytesPerMsHashEstimate();
    }
    catch (SystemMetrics.NoHashEstimateAvailableException e) {
      // if can't get my rate, use slowest rate to prevent adjustment
      theLog.warning("No hash estimate available, " +
                     "not adjusting poll for slow machines");
      my_rate = slow_rate;
    }
    theLog.debug3("My hash speed is " + my_rate
                  + ". Slow speed is " + slow_rate);


    if (my_rate > slow_rate) {
      my_estimate = estTime * my_rate / slow_rate;
      theLog.debug3("I've corrected the hash estimate to " + my_estimate);
    }
    return my_estimate;
  }

  private PluginManager getPluginManager() {
    if(pluginMgr == null) {
      pluginMgr = (PluginManager)LockssDaemon.getManager(
          LockssDaemon.PLUGIN_MANAGER);
    }
    return pluginMgr;
  }

  public String toString() {
    return "[PS: pid=" + "auid=" + auId + ", url=" + url
      + ", l=" + lwrBound + ", u=" + uprBound + ", version=" + version + "]";
  }
}


