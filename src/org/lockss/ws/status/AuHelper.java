/*
 * $Id$
 */

/*

 Copyright (c) 2014-2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.status;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lockss.app.LockssDaemon;
import org.lockss.daemon.CrawlWindow;
import org.lockss.daemon.TitleConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.poller.Poll;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.state.AuState;
import org.lockss.state.NodeManager;
import org.lockss.state.NodeState;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.ws.entities.AuStatus;
import org.lockss.ws.entities.AuWsResult;
import org.lockss.ws.entities.LockssWebServicesFault;
import org.lockss.ws.entities.LockssWebServicesFaultInfo;

/**
 * Helper of the DaemonStatus web service implementation of Archival Unit
 * queries.
 */
public class AuHelper {
  /**
   * The fully-qualified name of the class of the objects used as source in a
   * query.
   */
  static String SOURCE_FQCN = AuWsSource.class.getCanonicalName();

  /**
   * The fully-qualified name of the class of the objects returned by the query.
   */
  static String RESULT_FQCN = AuWsResult.class.getCanonicalName();

  //
  // Property names used in Archival Unit queries.
  //
  static String AU_ID = "auId";
  static String NAME = "name";
  static String VOLUME = "volume";
  static String PLUGIN_NAME = "pluginName";
  static String TDB_YEAR = "tdbYear";
  static String ACCESS_TYPE = "accessType";
  static String CONTENT_SIZE = "contentSize";
  static String DISK_USAGE = "diskUsage";
  static String REPOSITORY_PATH = "repositoryPath";
  static String RECENT_POLL_AGREEMENT = "recentPollAgreement";
  static String HIGHEST_POLL_AGREEMENT = "highestPollAgreement";
  static String PUBLISHING_PLATFORM = "publishingPlatform";
  static String TDB_PUBLISHER = "tdbPublisher";
  static String AVAILABLE_FROM_PUBLISHER = "availableFromPublisher";
  static String SUBSTANCE_STATE = "substanceState";
  static String CREATION_TIME = "creationTime";
  static String CRAWL_PROXY = "crawlProxy";
  static String CRAWL_WINDOW = "crawlWindow";
  static String CRAWL_POOL = "crawlPool";
  static String LAST_COMPLETED_CRAWL = "lastCompletedCrawl";
  static String LAST_CRAWL = "lastCrawl";
  static String LAST_CRAWL_RESULT = "lastCrawlResult";
  static String LAST_COMPLETED_POLL = "lastCompletedPoll";
  static String LAST_POLL = "lastPoll";
  static String LAST_POLL_RESULT = "lastPollResult";
  static String CURRENTLY_CRAWLING = "currentlyCrawling";
  static String CURRENTLY_POLLING = "currentlyPolling";
  static String SUBSCRIPTION_STATUS = "subscriptionStatus";
  static String AU_CONFIGURATION = "auConfiguration";
  static String NEW_CONTENT_CRAWL_URLS = "newContentCrawlUrls";
  static String URL_STEMS = "urlStems";
  static String IS_BULK_CONTENT = "isBulkContent";
  static String PEER_AGREEMENTS = "peerAgreements";
  static String URLS = "urls";
  static String SUBSTANCE_URLS = "substanceUrls";
  static String ARTICLE_URLS = "articleUrls";

  /**
   * All the property names used in Archival Unit queries.
   */
  @SuppressWarnings("serial")
  static final Set<String> PROPERTY_NAMES = new HashSet<String>() {
    {
      add(AU_ID);
      add(NAME);
      add(VOLUME);
      add(PLUGIN_NAME);
      add(TDB_YEAR);
      add(ACCESS_TYPE);
      add(CONTENT_SIZE);
      add(DISK_USAGE);
      add(REPOSITORY_PATH);
      add(RECENT_POLL_AGREEMENT);
      add(HIGHEST_POLL_AGREEMENT);
      add(PUBLISHING_PLATFORM);
      add(TDB_PUBLISHER);
      add(AVAILABLE_FROM_PUBLISHER);
      add(SUBSTANCE_STATE);
      add(CREATION_TIME);
      add(CRAWL_PROXY);
      add(CRAWL_WINDOW);
      add(CRAWL_POOL);
      add(LAST_COMPLETED_CRAWL);
      add(LAST_CRAWL);
      add(LAST_CRAWL_RESULT);
      add(LAST_COMPLETED_POLL);
      add(LAST_POLL);
      add(LAST_POLL_RESULT);
      add(CURRENTLY_CRAWLING);
      add(CURRENTLY_POLLING);
      add(SUBSCRIPTION_STATUS);
      add(AU_CONFIGURATION);
      add(NEW_CONTENT_CRAWL_URLS);
      add(URL_STEMS);
      add(IS_BULK_CONTENT);
      add(PEER_AGREEMENTS);
      add(URLS);
      add(SUBSTANCE_URLS);
      add(ARTICLE_URLS);
    }
  };

  private static Logger log = Logger.getLogger(AuHelper.class);

  /**
   * Provides the status information of an archival unit in the system.
   * 
   * @param auId
   *          A String with the identifier of the archival unit.
   * @return an AuStatus with the status information of the archival unit.
   * @throws LockssWebServicesFault
   */
  AuStatus getAuStatus(String auId) throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getAuStatus(): ";

    LockssDaemon theDaemon = LockssDaemon.getLockssDaemon();
    PluginManager pluginMgr = theDaemon.getPluginManager();
    ArchivalUnit au = pluginMgr.getAuFromId(auId);

    if (au == null) {
      throw new LockssWebServicesFault(
	  "No Archival Unit with provided identifier",
	  new LockssWebServicesFaultInfo("Archival Unit identifier = " + auId));
    }

    AuStatus result = new AuStatus();
    result.setVolume(au.getName());

    TitleConfig tc = au.getTitleConfig();
    if (tc != null) {
      result.setJournalTitle(tc.getJournalTitle());
    }

    Plugin plugin = au.getPlugin();
    result.setPluginName(plugin.getPluginName());

    result.setYear(AuUtil.getTitleAttribute(au, "year"));

    NodeManager nodeMgr = theDaemon.getNodeManager(au);
    AuState state = nodeMgr.getAuState();
    AuState.AccessType atype = state.getAccessType();

    if (atype != null) {
      result.setAccessType(atype.toString());
    }

    long contentSize = AuUtil.getAuContentSize(au, false);

    if (contentSize != -1) {
      result.setContentSize(contentSize);
    }

    long du = AuUtil.getAuDiskUsage(au, false);

    if (du != -1) {
      result.setDiskUsage(du);
    }

    String spec = LockssRepositoryImpl.getRepositorySpec(au);
    String repo = LockssRepositoryImpl.mapAuToFileLocation(
	LockssRepositoryImpl.getLocalRepositoryPath(spec), au);
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
      String disp = (aupinfo.getHost() == null
	  ? "Direct connection" : aupinfo.getHost() + ":" + aupinfo.getPort());
      result.setCrawlProxy(disp);
    }

    CrawlWindow window = au.getCrawlWindow();

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

    result.setCurrentlyCrawling(theDaemon.getCrawlManager().getStatusSource()
	.getStatus().isRunningNCCrawl(au));

    result.setCurrentlyPolling(theDaemon.getPollManager().isPollRunning(au));

    if (theDaemon.isDetectClockssSubscription()) {
      result.setSubscriptionStatus(AuUtil.getAuState(au)
	  .getClockssSubscriptionStatusString());
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides the universe of Archival Unit-related query objects used as the
   * source for a query.
   * 
   * @return a List<AuWsSource> with the universe.
   */
  List<AuWsSource> createUniverse() {
    final String DEBUG_HEADER = "createUniverse(): ";

    // Get all the configured Archival Units.
    List<ArchivalUnit> allAus =
	((PluginManager) LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER))
	.getAllAus();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "allAus.size() = " + allAus.size());

    // Initialize the universe.
    List<AuWsSource> universe = new ArrayList<AuWsSource>(allAus.size());

    // Loop through all the configured Archival Units.
    for (ArchivalUnit au : allAus) {
      // Add the object initialized with this Archival Unit to the universe of
      // objects.
      universe.add(new AuWsSource(au));
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "universe.size() = " + universe.size());
    return universe;
  }

  /**
   * Provides a printable copy of a collection of Archival Unit-related query
   * results.
   * 
   * @param results
   *          A Collection<AuWsResult> with the query results.
   * @return a String with the requested printable copy.
   */
  String nonDefaultToString(Collection<AuWsResult> results) {
    StringBuilder builder = new StringBuilder("[");
    boolean isFirst = true;

    // Loop through through all the results in the collection.
    for (AuWsResult result : results) {
      // Handle the first result differently.
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      // Add this result to the printable copy.
      builder.append(nonDefaultToString(result));
    }

    return builder.append("]").toString();
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

  /**
   * Provides a printable copy of an Archival Unit-related query result.
   * 
   * @param result
   *          An AuWsResult with the query result.
   * @return a String with the requested printable copy.
   */
  private String nonDefaultToString(AuWsResult result) {
    StringBuilder builder = new StringBuilder("AuWsResult [");
    boolean isFirst = true;

    if (result.getAuId() != null) {
      builder.append("auId=").append(result.getAuId());
      isFirst = false;
    }

    if (result.getName() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("name=").append(result.getName());
    }

    if (result.getVolume() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("volume=").append(result.getVolume());
    }

    if (result.getPluginName() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pluginName=").append(result.getPluginName());
    }

    if (result.getTdbYear() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("tdbYear=").append(result.getTdbYear());
    }

    if (result.getAccessType() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("accessType=").append(result.getAccessType());
    }

    if (result.getContentSize() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("contentSize=").append(result.getContentSize());
    }

    if (result.getDiskUsage() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("diskUsage=").append(result.getDiskUsage());
    }

    if (result.getRepositoryPath() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("repositoryPath=").append(result.getRepositoryPath());
    }

    if (result.getRecentPollAgreement() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("recentPollAgreement=")
      .append(result.getRecentPollAgreement());
    }

    if (result.getHighestPollAgreement() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("highestPollAgreement=")
      .append(result.getHighestPollAgreement());
    }

    if (result.getPublishingPlatform() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("publishingPlatform=")
      .append(result.getPublishingPlatform());
    }

    if (result.getTdbPublisher() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("tdbPublisher=").append(result.getTdbPublisher());
    }

    if (result.getAvailableFromPublisher() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("availableFromPublisher=")
      .append(result.getAvailableFromPublisher());
    }

    if (result.getSubstanceState() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("substanceState=").append(result.getSubstanceState());
    }

    if (result.getCreationTime() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("creationTime=").append(result.getCreationTime());
    }

    if (result.getCrawlProxy() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("crawlProxy=").append(result.getCrawlProxy());
    }

    if (result.getCrawlWindow() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("crawlWindow=").append(result.getCrawlWindow());
    }

    if (result.getCrawlPool() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("crawlPool=").append(result.getCrawlPool());
    }

    if (result.getLastCompletedCrawl() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastCompletedCrawl=")
      .append(result.getLastCompletedCrawl());
    }

    if (result.getLastCrawl() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastCrawl=").append(result.getLastCrawl());
    }

    if (result.getLastCrawlResult() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastCrawlResult=").append(result.getLastCrawlResult());
    }

    if (result.getLastCompletedPoll() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastCompletedPoll=")
      .append(result.getLastCompletedPoll());
    }

    if (result.getLastPoll() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastPoll=").append(result.getLastPoll());
    }

    if (result.getLastPollResult() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastPollResult=").append(result.getLastPollResult());
    }

    if (result.getCurrentlyCrawling() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("currentlyCrawling=")
      .append(result.getCurrentlyCrawling());
    }

    if (result.getCurrentlyPolling() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("currentlyPolling=").append(result.getCurrentlyPolling());
    }

    if (result.getSubscriptionStatus() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("subscriptionStatus=")
      .append(result.getSubscriptionStatus());
    }

    if (result.getAuConfiguration() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("auConfiguration=").append(result.getAuConfiguration());
    }

    if (result.getNewContentCrawlUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("newContentCrawlUrls=")
      .append(result.getNewContentCrawlUrls());
    }

    if (result.getUrlStems() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("urlStems=").append(result.getUrlStems());
    }

    if (result.getIsBulkContent() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("isBulkContent=").append(result.getIsBulkContent());
    }

    if (result.getPeerAgreements() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("peerAgreements=").append(result.getPeerAgreements());
    }

    if (result.getUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("urls=").append(result.getUrls());
    }

    if (result.getSubstanceUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("substanceUrls=").append(result.getSubstanceUrls());
    }

    if (result.getArticleUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("articleUrls=").append(result.getArticleUrls());
    }

    return builder.append("]").toString();
  }
}
