/*
 * $Id: PollSpec.java,v 1.37 2008-06-03 22:25:28 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;

/**
 * Class implementing the concept of the set of URLs covered by a poll.
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
  public static final String DEFAULT_PLUGIN_VERSION = "1";

  private static Logger theLog = Logger.getLogger("PollSpec");

  private String auId;
  private String pluginVersion;
  private String url;
  private String uprBound = null;
  private String lwrBound = null;
  private CachedUrlSet cus = null;
  private PluginManager pluginMgr = null;
  private int protocolVersion; // poll protocol version
  private int pollType; // One of the types defined by Poll

  /**
   * Construct a PollSpec from a CachedUrlSet and an upper and lower bound
   * @param cus the CachedUrlSet
   * @param lwrBound the lower boundary
   * @param uprBound the upper boundary
   * @param pollType one of the types defined by Poll
   */
  public PollSpec(CachedUrlSet cus, String lwrBound,
                  String uprBound, int pollType) {
    commonSetup(cus, lwrBound, uprBound, pollType);
  }


  /**
   * Construct a PollSpec from a CachedUrlSet.
   * @param cus the CachedUrlSpec which defines the range of interest
   * @param pollType one of the types defined by Poll
   */
  public PollSpec(CachedUrlSet cus, int pollType) {
    CachedUrlSetSpec cuss = cus.getSpec();
    if (cuss instanceof RangeCachedUrlSetSpec) {
      RangeCachedUrlSetSpec rcuss = (RangeCachedUrlSetSpec)cuss;
      commonSetup(cus, rcuss.getLowerBound(), rcuss.getUpperBound(), pollType);
    } else if (cuss.isSingleNode()) {
      commonSetup(cus, SINGLE_NODE_LWRBOUND, null, pollType);
    } else {
      commonSetup(cus, null, null, pollType);
    }
  }

  /**
   * Construct a PollSpec from a V1 LcapMessage
   * @param msg the LcapMessage which defines the range of interest
   */
  public PollSpec(V1LcapMessage msg) {
    auId = msg.getArchivalId();
    pluginVersion = msg.getPluginVersion();
    url = msg.getTargetUrl();
    uprBound = msg.getUprBound();
    lwrBound = msg.getLwrBound();
    protocolVersion = msg.getProtocolVersion();
    if (msg.isContentPoll()) {
      pollType = Poll.V1_CONTENT_POLL;
    } else if (msg.isNamePoll()) {
      pollType = Poll.V1_NAME_POLL;
    } else if (msg.isVerifyPoll()) {
      pollType = Poll.V1_VERIFY_POLL;
    } else {
      pollType = -1;
    }
    cus = getPluginManager().findCachedUrlSet(this);
  }

  public PollSpec(V3LcapMessage msg) {
    this(msg.getArchivalId(),
	 (msg.getTargetUrl() == null) ? "lockssau:" : msg.getTargetUrl(),
	 null,
	 null,
	 Poll.V3_POLL);
    protocolVersion = msg.getProtocolVersion();
    pluginVersion = msg.getPluginVersion();
  }

  /**
   * Construct a PollSpec from explicit args
   */
  public PollSpec(String auId, String url, String lower,
                  String upper, int pollType) {
    this.auId = auId;
    this.url = url;
    uprBound = upper;
    lwrBound = lower;
    this.pollType = pollType;
    cus = getPluginManager().findCachedUrlSet(this);
    this.protocolVersion = protocolVersionFromPollType(pollType);
  }

  /**
   * Construct a PollSpec from another PollSpec and a poll type
   * XXX it seems that other constructors are not setting all fields
   */
  public PollSpec(PollSpec ps, int pollType) {
    this.auId = ps.auId;
    this.pluginVersion = ps.pluginVersion;
    this.url = ps.url;
    this.uprBound = ps.uprBound;
    this.lwrBound = ps.lwrBound;
    this.cus = ps.cus;
    this.pluginMgr = ps.pluginMgr;
    this.protocolVersion = ps.protocolVersion;
    this.pollType = pollType;
  }

  /** Setup common to most constructors */
  private void commonSetup(CachedUrlSet cus, String lwrBound,
                           String uprBound, int pollType) {
    this.cus = cus;
    ArchivalUnit au = cus.getArchivalUnit();
    auId = au.getAuId();
    this.pluginVersion = au.getPlugin().getVersion();
    CachedUrlSetSpec cuss = cus.getSpec();
    url = cuss.getUrl();
    this.lwrBound = lwrBound;
    this.uprBound = uprBound;
    this.protocolVersion = protocolVersionFromPollType(pollType);
    this.pollType = pollType;
  }

  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  public String getAuId() {
    return auId;
  }

  public String getPluginVersion() {
    return (pluginVersion != null) ? pluginVersion : DEFAULT_PLUGIN_VERSION;
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
    if (StringUtil.equalStrings(lwrBound, SINGLE_NODE_LWRBOUND)) {
      return "single node";
    }
    if (lwrBound != null || uprBound != null) {
      String lwrDisplay = lwrBound;
      String uprDisplay = uprBound;
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

  public int getProtocolVersion() {
    return protocolVersion;
  }

  public int getPollType() {
    return pollType;
  }

  private PluginManager getPluginManager() {
    if (pluginMgr == null) {
      pluginMgr =
	(PluginManager)LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER);
    }
    return pluginMgr;
  }

  /**
   * Given a poll type, return the correct version of the protocol to use.
   *
   * @param pollType
   * @return The protocol version to use
   */
  private int protocolVersionFromPollType(int pollType) {
    switch(pollType) {
    case Poll.V1_CONTENT_POLL:
    case Poll.V1_NAME_POLL:
    case Poll.V1_VERIFY_POLL:
      return Poll.V1_PROTOCOL;
    case Poll.V3_POLL:
      return Poll.V3_PROTOCOL;
    default:
      return Poll.UNDEFINED_PROTOCOL;
    }
  }

  public String toString() {
    return "[PS: " + Poll.POLL_NAME[pollType]
      + " auid=" + auId
      + ", url=" + url
      + ", l=" + lwrBound
      + ", u=" + uprBound
      + ", type=" + pollType
      + ", plugVer=" + getPluginVersion()
      + ", protocol=" + protocolVersion + "]";
  }
}
