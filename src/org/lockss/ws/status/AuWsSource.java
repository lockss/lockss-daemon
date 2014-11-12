/*
 * $Id: AuWsSource.java,v 1.7 2014-11-12 20:11:45 wkwilson Exp $
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lockss.app.LockssDaemon;
import org.lockss.crawler.CrawlManagerStatus;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.CrawlWindow;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.Plugin;
import org.lockss.poller.Poll;
import org.lockss.protocol.AgreementType;
import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.PeerAgreement;
import org.lockss.protocol.PeerIdentity;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.state.AuState;
import org.lockss.state.NodeManager;
import org.lockss.util.StringUtil;
import org.lockss.util.TypedEntryMap;
import org.lockss.ws.entities.AgreementTypeWsResult;
import org.lockss.ws.entities.AuConfigurationWsResult;
import org.lockss.ws.entities.AuWsResult;
import org.lockss.ws.entities.PeerAgreementWsResult;
import org.lockss.ws.entities.PeerAgreementsWsResult;

/**
 * Container for the information that is used as the source for a query related
 * to Archival Units.
 */
public class AuWsSource extends AuWsResult {
  private ArchivalUnit au;

  private boolean auIdPopulated = false;
  private boolean namePopulated = false;
  private boolean volumePopulated = false;
  private boolean pluginNamePopulated = false;
  private boolean tdbYearPopulated = false;
  private boolean accessTypePopulated = false;
  private boolean contentSizePopulated = false;
  private boolean diskUsagePopulated = false;
  private boolean repositoryPathPopulated = false;
  private boolean recentPollAgreementPopulated = false;
  private boolean publishingPlatformPopulated = false;
  private boolean tdbPublisherPopulated = false;
  private boolean availableFromPublisherPopulated = false;
  private boolean substanceStatePopulated = false;
  private boolean creationTimePopulated = false;
  private boolean crawlProxyPopulated = false;
  private boolean crawlWindowPopulated = false;
  private boolean crawlPoolPopulated = false;
  private boolean lastCompletedCrawlPopulated = false;
  private boolean lastCrawlPopulated = false;
  private boolean lastCrawlResultPopulated = false;
  private boolean lastCompletedPollPopulated = false;
  private boolean lastPollPopulated = false;
  private boolean lastPollResultPopulated = false;
  private boolean currentlyCrawlingPopulated = false;
  private boolean currentlyPollingPopulated = false;
  private boolean subscriptionStatusPopulated = false;
  private boolean auConfigurationPopulated = false;
  private boolean newContentCrawlUrlsPopulated = false;
  private boolean urlStemsPopulated = false;
  private boolean isBulkContentPopulated = false;
  private boolean peerAgreementsPopulated = false;

  private LockssDaemon theDaemon = null;
  private Plugin plugin = null;
  private NodeManager nodeMgr = null;
  private AuState state = null;

  public AuWsSource(ArchivalUnit au) {
    this.au = au;
  }

  @Override
  public String getAuId() {
    if (!auIdPopulated) {
      setAuId(au.getAuId());
      auIdPopulated = true;
    }

    return super.getAuId();
  }

  @Override
  public String getName() {
    if (!namePopulated) {
      setName(au.getName());
      namePopulated = true;
    }

    return super.getName();
  }

  @Override
  public String getVolume() {
    if (!volumePopulated) {
      setVolume(au.getName());
      volumePopulated = true;
    }

    return super.getVolume();
  }

  @Override
  public String getPluginName() {
    if (!pluginNamePopulated) {
      setPluginName(getPlugin().getPluginId());
      pluginNamePopulated = true;
    }

    return super.getPluginName();
  }

  @Override
  public String getTdbYear() {
    if (!tdbYearPopulated) {
      setTdbYear(AuUtil.getTitleAttribute(au, "year"));
      tdbYearPopulated = true;
    }

    return super.getTdbYear();
  }

  @Override
  public String getAccessType() {
    if (!accessTypePopulated) {
      AuState.AccessType atype = getState().getAccessType();

      if (atype != null) {
	setAccessType(atype.toString());
      }

      accessTypePopulated = true;
    }

    return super.getAccessType();
  }

  @Override
  public Long getContentSize() {
    if (!contentSizePopulated) {
      long auContentSize = AuUtil.getAuContentSize(au, true);

      if (auContentSize != -1) {
  	setContentSize(Long.valueOf(auContentSize));
      }

      contentSizePopulated = true;
    }

    return super.getContentSize();
  }

  @Override
  public Long getDiskUsage() {
    if (!diskUsagePopulated) {
      long auDiskUsage = AuUtil.getAuDiskUsage(au, true);

      if (auDiskUsage != -1) {
	setDiskUsage(Long.valueOf(auDiskUsage));
      }

      diskUsagePopulated = true;
    }

    return super.getDiskUsage();
  }

  @Override
  public String getRepositoryPath() {
    if (!repositoryPathPopulated) {
      String spec = LockssRepositoryImpl.getRepositorySpec(au);
      setRepositoryPath(LockssRepositoryImpl
	  .mapAuToFileLocation(LockssRepositoryImpl
  	      .getLocalRepositoryPath(spec), au));
      repositoryPathPopulated = true;
    }

    return super.getRepositoryPath();
  }

  @Override
  public Double getRecentPollAgreement() {
    if (!recentPollAgreementPopulated) {
      if (AuUtil.getProtocolVersion(au) == Poll.V3_PROTOCOL) {
  	if (getState().getV3Agreement() >= 0) {
  	  if (state.getHighestV3Agreement() != state.getV3Agreement()) {
  	    setRecentPollAgreement(Double.valueOf(state.getV3Agreement()));
  	  }
  	}
      }

      recentPollAgreementPopulated = true;
    }

    return super.getRecentPollAgreement();
  }

  @Override
  public String getPublishingPlatform() {
    if (!publishingPlatformPopulated) {
      String platform = getPlugin().getPublishingPlatform();

      if (!StringUtil.isNullString(platform)) {
	setPublishingPlatform(platform);
      }

      publishingPlatformPopulated = true;
    }

    return super.getPublishingPlatform();
  }

  @Override
  public String getTdbPublisher() {
    if (!tdbPublisherPopulated) {
      String publisherName = AuUtil.getTitleAttribute(au, "publisher");

      if (!StringUtil.isNullString(publisherName)) {
	setTdbPublisher(publisherName);
      }

      tdbPublisherPopulated = true;
    }

    return super.getTdbPublisher();
  }

  @Override
  public Boolean getAvailableFromPublisher() {
    if (!availableFromPublisherPopulated) {
      setAvailableFromPublisher(Boolean.valueOf(!AuUtil.isPubDown(au)));
      availableFromPublisherPopulated = true;
    }

    return super.getAvailableFromPublisher();
  }

  @Override
  public String getSubstanceState() {
    if (!substanceStatePopulated) {
      setSubstanceState(getState().getSubstanceState().toString());
      substanceStatePopulated = true;
    }

    return super.getSubstanceState();
  }

  @Override
  public Long getCreationTime() {
    if (!creationTimePopulated) {
      setCreationTime(Long.valueOf(getState().getAuCreationTime()));
      creationTimePopulated = true;
    }

    return super.getCreationTime();
  }

  @Override
  public String getCrawlProxy() {
    if (!crawlProxyPopulated) {
      AuUtil.AuProxyInfo aupinfo = AuUtil.getAuProxyInfo(au);

      if (aupinfo.isAuOverride()) {
  	setCrawlProxy((aupinfo.getHost() == null ? "Direct connection"
  	    : aupinfo.getHost() + ":" + aupinfo.getPort()));
      }

      crawlProxyPopulated = true;
    }

    return super.getCrawlProxy();
  }

  @Override
  public String getCrawlWindow() {
    if (!crawlWindowPopulated) {
      CrawlWindow window = au.getCrawlWindow();

      if (window != null) {
  	String wmsg = window.toString();

  	if (wmsg.length() > 140) {
  	  wmsg = "(not displayable)";
  	}

  	if (!window.canCrawl()) {
  	  wmsg = "Currently closed: " + wmsg;
  	}

  	setCrawlWindow(wmsg);
      }
      crawlWindowPopulated = true;
    }

    return super.getCrawlWindow();
  }

  @Override
  public String getCrawlPool() {
    if (!crawlPoolPopulated) {
      String frlk = au.getFetchRateLimiterKey();

      if (frlk == null) {
  	setCrawlPool("(none)");
      } else {
  	setCrawlPool(frlk);
      }

      crawlPoolPopulated = true;
    }

    return super.getCrawlPool();
  }

  @Override
  public Long getLastCompletedCrawl() {
    if (!lastCompletedCrawlPopulated) {
      setLastCompletedCrawl(Long.valueOf(getState().getLastCrawlTime()));
      lastCompletedCrawlPopulated = true;
    }

    return super.getLastCompletedCrawl();
  }

  @Override
  public Long getLastCrawl() {
    if (!lastCrawlPopulated) {
      long lastCrawlAttempt = getState().getLastCrawlAttempt();

      if (lastCrawlAttempt > 0) {
	setLastCrawl(Long.valueOf(lastCrawlAttempt));
      }

      lastCrawlPopulated = true;
    }

    return super.getLastCrawl();
  }

  @Override
  public String getLastCrawlResult() {
    if (!lastCrawlResultPopulated) {
      long lastCrawlAttempt = getState().getLastCrawlAttempt();

      if (lastCrawlAttempt > 0) {
	setLastCrawlResult(state.getLastCrawlResultMsg());
      }

      lastCrawlResultPopulated = true;
    }

    return super.getLastCrawlResult();
  }

  @Override
  public Long getLastCompletedPoll() {
    if (!lastCompletedPollPopulated) {
      long lastTopLevelPollTime = getState().getLastTopLevelPollTime();

      if (lastTopLevelPollTime > 0) {
	setLastCompletedPoll(Long.valueOf(lastTopLevelPollTime));
      }

      lastCompletedPollPopulated = true;
    }

    return super.getLastCompletedPoll();
  }

  @Override
  public Long getLastPoll() {
    if (!lastPollPopulated) {
      long lastPollStart = getState().getLastPollStart();

      if (lastPollStart > 0) {
	setLastPoll(Long.valueOf(lastPollStart));
      }

      lastPollPopulated = true;
    }

    return super.getLastPoll();
  }

  @Override
  public String getLastPollResult() {
    if (!lastPollResultPopulated) {
      long lastPollStart = getState().getLastPollStart();

      if (lastPollStart > 0) {
  	String pollResult = state.getLastPollResultMsg();

  	if (!StringUtil.isNullString(pollResult)) {
  	  setLastPollResult(state.getLastPollResultMsg());
  	}
      }

      lastPollResultPopulated = true;
    }

    return super.getLastPollResult();
  }

  @Override
  public Boolean getCurrentlyCrawling() {
    if (!currentlyCrawlingPopulated) {
      CrawlManagerStatus cms =
	  getTheDaemon().getCrawlManager().getStatusSource().getStatus();

      if (cms != null) {
	setCurrentlyCrawling(Boolean.valueOf(cms.isRunningNCCrawl(au)));
      }

      currentlyCrawlingPopulated = true;
    }

    return super.getCurrentlyCrawling();
  }

  @Override
  public Boolean getCurrentlyPolling() {
    if (!currentlyPollingPopulated) {
      setCurrentlyPolling(Boolean.valueOf(getTheDaemon().getPollManager()
	  .isPollRunning(au)));
      currentlyPollingPopulated = true;
    }

    return super.getCurrentlyPolling();
  }

  @Override
  public String getSubscriptionStatus() {
    if (!subscriptionStatusPopulated) {
      if (getTheDaemon().isDetectClockssSubscription()) {
	setSubscriptionStatus(AuUtil.getAuState(au)
	    .getClockssSubscriptionStatusString());
      }

      subscriptionStatusPopulated = true;
    }

    return super.getSubscriptionStatus();
  }

  @Override
  public AuConfigurationWsResult getAuConfiguration() {
    if (!auConfigurationPopulated) {
      // Get the properties of the Archival Unit.
      TypedEntryMap auProperties = au.getProperties();

      if (auProperties.size() > 0) {
	// Initialize the result.
	AuConfigurationWsResult result = new AuConfigurationWsResult();

	Map<String, String> defParams = new HashMap<String, String>();
	result.setDefParams(defParams);

	Map<String, String> nonDefParams = new HashMap<String, String>();
	result.setNonDefParams(nonDefParams);

	// Loop through all the properties of the Archival Unit.
	for (Map.Entry entry : auProperties.entrySet()) {
	  // Get the key and value of this property.
	  String key = (String)entry.getKey();
	  String val = null;

	  // Handle only non-null values.
	  if (entry.getValue() != null) {
	    val = entry.getValue().toString();
	  }

	  // Find the property type from the Archival Unit configuration.
	  ConfigParamDescr descr = getPlugin().findAuConfigDescr(key);

	  // Check whether the property is not in the configuration.
	  if (descr == null || !au.getConfiguration().containsKey(key)) {
	    // Yes: Ignore it.
	    // No: Check whether the property is definitional.
	  } else if (descr.isDefinitional()) {
	    // Yes: Place it in the appropriate list.
	    defParams.put(key, val);
	  } else {
	    // No: Place it in the appropriate list.
	    nonDefParams.put(key, val);
	  }
	}

	setAuConfiguration(result);
      }
      
      auConfigurationPopulated = true;
    }

    return super.getAuConfiguration();
  }

  @Override
  public List<String> getNewContentCrawlUrls() {
    if (!newContentCrawlUrlsPopulated) {
      setNewContentCrawlUrls(new ArrayList<String>(au.getStartUrls()));
      newContentCrawlUrlsPopulated = true;
    }

    return super.getNewContentCrawlUrls();
  }

  @Override
  public List<String> getUrlStems() {
    if (!urlStemsPopulated) {
      setUrlStems((List<String>)au.getUrlStems());
      urlStemsPopulated = true;
    }

    return super.getUrlStems();
  }

  @Override
  public Boolean getIsBulkContent() {
    if (!isBulkContentPopulated) {
      setIsBulkContent(au.isBulkContent());
      isBulkContentPopulated = true;
    }

    return super.getIsBulkContent();
  }

  @Override
  public List<PeerAgreementsWsResult> getPeerAgreements() {
    if (!peerAgreementsPopulated) {
      IdentityManager idManager = getTheDaemon().getIdentityManager();

      // Initialize the the map of agreements by type by peer.
      Map<String, Map<AgreementType, PeerAgreement>>
      allAgreementsByPeer =
	  new HashMap<String, Map<AgreementType, PeerAgreement>>();

      // Loop through all the types of agreements.
      for (AgreementType type : AgreementType.values()) {
	// Get the agreements for this type.
	Map<PeerIdentity, PeerAgreement> agreementsByPeer =
	    idManager.getAgreements(au, type);

	// Loop through the peers for the agreements.
	for (PeerIdentity pid : agreementsByPeer.keySet()) {
	  // Get the agreement of this type for this peer.
	  PeerAgreement agreement = agreementsByPeer.get(pid);
	  // Check whether there has been an agreement at some point in the
	  // past.
	  if (agreement != null
	      && agreement.getHighestPercentAgreement() >= 0.0) {
	    // Yes: Create the map of agreements for this peer in the map of
	    // agreements by type by peer if it does not exist already.
	    if (!allAgreementsByPeer.containsKey(pid.getIdString())) {
	      allAgreementsByPeer.put(pid.getIdString(),
		  new HashMap<AgreementType, PeerAgreement>());
	    }

	    // Add this type of agreement to the map of agreements for this peer
	    // in the map of by type by peer.
	    allAgreementsByPeer.get(pid.getIdString()).put(type, agreement);
	  }
	}
      }

      // Initialize the results.
      List<PeerAgreementsWsResult> results =
	  new ArrayList<PeerAgreementsWsResult>();

      // Loop through all the peer identifiers.
      for (String pidid : allAgreementsByPeer.keySet()) {
	// Create the result agreements by type for this peer identifier.
	Map<AgreementTypeWsResult, PeerAgreementWsResult> resultAgreements =
	    new HashMap<AgreementTypeWsResult, PeerAgreementWsResult>();

	// Loop through all the types for this peer identifier.
	for (AgreementType type : allAgreementsByPeer.get(pidid).keySet()) {
	  // Get the corresponding result type.
	  AgreementTypeWsResult resultType =
	      AgreementTypeWsResult.values()[type.ordinal()];

	  // Get the agreement for this type for this peer identifier.
	  PeerAgreement agreement = allAgreementsByPeer.get(pidid).get(type);

	  // Get the corresponding result agreement.
	  PeerAgreementWsResult resultAgreement = new PeerAgreementWsResult(
		Float.valueOf(agreement.getPercentAgreement()),
		Long.valueOf(agreement.getPercentAgreementTime()),
		Float.valueOf(agreement.getHighestPercentAgreement()),
		Long.valueOf(agreement.getHighestPercentAgreementTime()));

	  // Save this type/agreement pair for this peer identifier.
	  resultAgreements.put(resultType, resultAgreement);
	}

	// Save the result for this peer identifier.
	PeerAgreementsWsResult result = new PeerAgreementsWsResult();
	result.setPeerId(pidid);
	result.setAgreements(resultAgreements);
	results.add(result);
      }

      setPeerAgreements(results);
      peerAgreementsPopulated = true;
    }

    return super.getPeerAgreements();
  }

  /**
   * Provides the daemon, initializing it if necessary.
   * 
   * @return a LockssDaemon with the daemon.
   */
  private LockssDaemon getTheDaemon() {
    if (theDaemon == null) {
      theDaemon = LockssDaemon.getLockssDaemon();
    }

    return theDaemon;
  }

  /**
   * Provides the plugin, initializing it if necessary.
   * 
   * @return a Plugin with the plugin.
   */
  private Plugin getPlugin() {
    if (plugin == null) {
      plugin = au.getPlugin();
    }

    return plugin;
  }

  /**
   * Provides the node manager, initializing it if necessary.
   * 
   * @return a NodeManager with the node manager.
   */
  private NodeManager getNodeManager() {
    if (nodeMgr == null) {
      nodeMgr = getTheDaemon().getNodeManager(au);
    }

    return nodeMgr;
  }

  /**
   * Provides the Archival Unit state, initializing it if necessary.
   * 
   * @return a AuState with the Archival Unit state.
   */
  private AuState getState() {
    if (state == null) {
      state = getNodeManager().getAuState();
    }

    return state;
  }
}
