/*
 * $Id: DaemonStatusServiceImpl.java,v 1.1 2013-03-22 04:47:32 fergaloy-sf Exp $
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * The DaemonStatus web service implementation.
 */
package org.lockss.ws.status;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import javax.jws.WebService;
import org.lockss.app.LockssDaemon;
import org.lockss.crawler.CrawlManager;
import org.lockss.daemon.CrawlWindow;
import org.lockss.daemon.TitleConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.poller.Poll;
import org.lockss.poller.PollManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.state.AuState;
import org.lockss.state.NodeManager;
import org.lockss.state.NodeState;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.ws.entities.AuStatus;
import org.lockss.ws.entities.IdNamePair;
import org.lockss.ws.entities.LockssWebServicesFault;
import org.lockss.ws.entities.LockssWebServicesFaultInfo;
import org.lockss.ws.status.DaemonStatusService;

@WebService
public class DaemonStatusServiceImpl implements DaemonStatusService {
  private static Logger log = Logger.getLogger(DaemonStatusServiceImpl.class);

  /**
   * Provides an indication of whether the daemon is ready.
   * 
   * @return a boolean with the indication.
   * @throws LockssWebServicesFault
   */
  @Override
  public boolean isDaemonReady() throws LockssWebServicesFault {
    final String DEBUG_HEADER = "isDaemonReady(): ";

    try {
      log.debug2(DEBUG_HEADER + "Invoked.");
      PluginManager pluginMgr =
	  (PluginManager) LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER);
      boolean areAusStarted = pluginMgr.areAusStarted();
      log.debug2(DEBUG_HEADER + "areAusStarted = " + areAusStarted);

      return areAusStarted;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides a list of the identifier/name pairs of the archival units in the
   * system.
   * 
   * @return a List<IdNamePair> with the identifier/name pairs of the archival
   *         units in the system.
   * @throws LockssWebServicesFault
   */
  @Override
  public Collection<IdNamePair> getAuIds() throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getAuIds(): ";

    try {
      log.debug2(DEBUG_HEADER + "Invoked.");
      Collection<IdNamePair> result = new ArrayList<IdNamePair>();
      PluginManager pluginMgr =
	  (PluginManager) LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER);

      for (ArchivalUnit au : pluginMgr.getAllAus()) {
	log.debug2(DEBUG_HEADER + "au = " + au);
	result.add(new IdNamePair(au.getAuId(), au.getName()));
      }

      log.debug2(DEBUG_HEADER + "result.size() = " + result.size());
      return result;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the status information of an archival unit in the system.
   * 
   * @param auId
   *          A String with the identifier of the archival unit.
   * @return an AuStatus with the status information of the archival unit.
   * @throws LockssWebServicesFault
   */
  @Override
  public AuStatus getAuStatus(String auId) throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getAuStatus(): ";

    try {
      log.debug2(DEBUG_HEADER + "auId = " + auId);
      if (StringUtil.isNullString(auId)) {
	throw new LockssWebServicesFault(
	    new IllegalArgumentException("Invalid Archival Unit identifier"),
	    new LockssWebServicesFaultInfo("Archival Unit identifier = "
		+ auId));
      }

      LockssDaemon theDaemon = LockssDaemon.getLockssDaemon();
      PluginManager pluginMgr = theDaemon.getPluginManager();
      ArchivalUnit au = pluginMgr.getAuFromId(auId);
      AuStatus result = new AuStatus();

      result.setVolume(au.getName());

      TitleConfig tc = au.getTitleConfig();
      if (tc != null) {
	result.setJournalTitle(tc.getJournalTitle());
      }

      Plugin plugin = au.getPlugin();
      result.setPluginName(plugin.getPluginName());

      String yearAsString = null;
      try {
	yearAsString = AuUtil.getTitleAttribute(au, "year");
	if (yearAsString != null) {
	  result.setYear(Integer.valueOf(yearAsString));
	}
      } catch (NumberFormatException nfe) {
	log.warning("Invalid year title attribute '" + yearAsString
	    + "' for auId '" + auId + "'", nfe);
      }

      NodeManager nodeMgr = theDaemon.getNodeManager(au);
      AuState state = nodeMgr.getAuState();

      result.setAccessType(state.getAccessType().toString());
      long contentSize = AuUtil.getAuContentSize(au, false);
      if (contentSize != -1) {
	result.setContentSize(contentSize);
      }

      long du = AuUtil.getAuDiskUsage(au, false);
      if (du != -1) {
	result.setDiskUsage(du);
      }

      String spec = LockssRepositoryImpl.getRepositorySpec(au);
      String repo =
	  LockssRepositoryImpl.mapAuToFileLocation(LockssRepositoryImpl
	      .getLocalRepositoryPath(spec), au);
      result.setRepository(repo);

      CachedUrlSet auCus = au.getAuCachedUrlSet();
      NodeState topNode = nodeMgr.getNodeState(auCus);

      if (AuUtil.getProtocolVersion(au) == Poll.V3_PROTOCOL) {
	if (state.getV3Agreement() < 0) {
	  if (state.getLastCrawlTime() < 0) {
	    result.setStatus("Waiting for Crawl");
	  } else {
	    result.setStatus("Waiting for Poll");
	  }
	} else {
	  result.setStatus(doubleToPercent(state.getHighestV3Agreement())
	      + "% Agreement");
	  if (state.getHighestV3Agreement() != state.getV3Agreement()) {
	    result.setRecentPollAgreement(state.getV3Agreement());
	  }
	}
      } else {
	result.setStatus(topNode.hasDamage() ? "Repairing" : "Ok");
      }

      String publishingPlatform = plugin.getPublishingPlatform();
      if (!StringUtil.isNullString(publishingPlatform)) {
	result.setPublishingPlatform(publishingPlatform);
      }

      String publisher = AuUtil.getTitleAttribute(au, "publisher");
      if (!StringUtil.isNullString(publisher)) {
	result.setPublisher(publisher);
      }

      result.setAvailableFromPublisher(!AuUtil.isPubDown(au));
      result.setSubstanceState(state.getSubstanceState().toString());
      result.setCreationTime(state.getAuCreationTime());

      AuUtil.AuProxyInfo aupinfo = AuUtil.getAuProxyInfo(au);
      if (aupinfo.isAuOverride()) {
	String disp =
	    (aupinfo.getHost() == null ? "Direct connection" : aupinfo
		.getHost() + ":" + aupinfo.getPort());
	result.setCrawlProxy(disp);
      }

      CrawlWindow window = au.getCrawlSpec().getCrawlWindow();
      if (window != null) {
	String wmsg = window.toString();
	if (wmsg.length() > 140) {
	  wmsg = "(not displayable)";
	}
	if (!window.canCrawl()) {
	  wmsg = "Currently closed: " + wmsg;
	}
	result.setCrawlWindow(wmsg);
      }

      String crawlPool = au.getFetchRateLimiterKey();
      if (crawlPool == null) {
	crawlPool = "(none)";
      }
      result.setCrawlPool(crawlPool);

      result.setLastCompletedCrawl(state.getLastCrawlTime());

      long lastCrawlAttempt = state.getLastCrawlAttempt();
      if (lastCrawlAttempt > 0) {
	result.setLastCrawl(lastCrawlAttempt);
	result.setLastCrawlResult(state.getLastCrawlResultMsg());
      }

      long lastTopLevelPollTime = state.getLastTopLevelPollTime();
      if (lastTopLevelPollTime > 0) {
	result.setLastCompletedPoll(lastTopLevelPollTime);
      }

      long lastPollStart = state.getLastPollStart();
      if (lastPollStart > 0) {
	result.setLastPoll(lastPollStart);
	String pollResult = state.getLastPollResultMsg();
	if (!StringUtil.isNullString(pollResult)) {
	  result.setLastPollResult(state.getLastPollResultMsg());
	}
      }

      CrawlManager crawlMgr = theDaemon.getCrawlManager();
      result.setCurrentlyCrawling(crawlMgr.getStatusSource().getStatus()
	  .isRunningNCCrawl(au));

      PollManager pollMgr = theDaemon.getPollManager();
      result.setCurrentlyPolling(pollMgr.isPollRunning(au));

      if (theDaemon.isDetectClockssSubscription()) {
	result.setSubscriptionStatus(AuUtil.getAuState(au)
	    .getClockssSubscriptionStatusString());
      }

      if (log.isDebug2()) {
	log.debug2(DEBUG_HEADER + "result = " + result);
      }

      return result;
    } catch (LockssWebServicesFault wsf) {
      throw wsf;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Formats 100 times a double to print as a percentage. An input value of 1.0
   * will produce "100.00".
   * 
   * @param d
   *          A double with the value to convert.
   * @return a String representing the double.
   */
  private String doubleToPercent(double d) {
    int i = (int) (d * 10000);
    double pc = i / 100.0;
    return new DecimalFormat("0.00").format(pc);
  }
}
