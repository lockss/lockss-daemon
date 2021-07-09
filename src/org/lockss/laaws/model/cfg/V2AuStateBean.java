/*

Copyright (c) 2000-2020 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.laaws.model.cfg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.Plugin.Feature;
import org.lockss.state.AuState;
import org.lockss.state.AuState.AccessType;
import org.lockss.state.SubstanceChecker;

public class V2AuStateBean {

  // Persistent state vars
  protected long auCreationTime = -1;
  // Note that WS exposes lastCrawlTime as lastCompletedCrawl, and
  // lastCrawlAttempt as lastCrawl.
  protected long lastCrawlTime = -1;  // last successful crawl
  protected long lastCrawlAttempt = -1;
  protected String lastCrawlResultMsg = null;
  protected int lastCrawlResult = -1;
  protected long lastDeepCrawlTime = -1; // last successful deep crawl finish
  protected long lastDeepCrawlAttempt = -1; // last deep crawl start
  protected String lastDeepCrawlResultMsg;
  protected int lastDeepCrawlResult = -1;
  protected int lastDeepCrawlDepth = -1;// requested depth of last
  // successful deep crawl
  protected long lastTopLevelPollTime = -1;  // last completed PoR poll time
  protected long lastPollStart = -1;  // last time a poll started
  protected int lastPollResult = -1;  // ditto
  protected long pollDuration = 0; // average of last two PoRpoll durations
  protected long averageHashDuration = -1;
  protected int clockssSubscriptionStatus = AuState.CLOCKSS_SUB_UNKNOWN;
  protected double v3Agreement = -1.0;
  protected double highestV3Agreement = -1.0;
  protected AccessType accessType = null;
  protected SubstanceChecker.State hasSubstance =
    SubstanceChecker.State.Unknown;
  protected String substanceVersion = null;
  protected String metadataVersion = null;
  protected long lastMetadataIndex = -1; // last time metadata extraction
  // completed
  protected long lastContentChange = 0;   // last time a new URL version created
  protected long lastPoPPoll = -1;   // last completed PoP poll time
  protected int lastPoPPollResult = -1;   // result of last PoP poll
  protected long lastLocalHashScan = -1; // last completed local hash scan
  protected int numAgreePeersLastPoR = -1; // Number of agreeing peers last PoR
  protected int numWillingRepairers = 0;  // Number of willing repairers
  protected int numCurrentSuspectVersions = 0; // # URLs w/ current version suspect
  protected List<String> cdnStems = null; // URL stems of content on hosts
  // not predicted by permission URLs
  // The indication of whether metadata extraction is enabled for this AU.
  protected boolean isMetadataExtractionEnabled = true;

  protected String auId = null;

  public V2AuStateBean(AuState v1State) {
    ArchivalUnit au = v1State.getArchivalUnit();
    accessType = v1State.getAccessType();
    auCreationTime = v1State.getAuCreationTime();
    auId = au.getAuId();
    //averageHashDuration /* ignore */
    cdnStems = v1State.getCdnStems();
    clockssSubscriptionStatus = v1State.getClockssSubscriptionStatus();
    hasSubstance = v1State.getSubstanceState();
    highestV3Agreement = v1State.getHighestV3Agreement();
    //isMetadataExtractionEnable /* Use default value */
    lastContentChange = v1State.getLastContentChange();
    lastCrawlAttempt = v1State.getLastCrawlAttempt();
    lastCrawlResult = v1State.getLastCrawlResult();
    lastCrawlResultMsg = v1State.getLastCrawlResultMsg();
    lastCrawlTime = v1State.getLastCrawlTime();
    lastDeepCrawlAttempt = v1State.getLastDeepCrawlAttempt();
    lastDeepCrawlDepth = v1State.getLastDeepCrawlDepth();
    lastDeepCrawlResult = v1State.getLastDeepCrawlResult();
    lastDeepCrawlResultMsg = v1State.getLastDeepCrawlResultMsg();
    lastDeepCrawlTime = v1State.getLastDeepCrawlTime();
    lastLocalHashScan = v1State.getLastLocalHashScan();
    lastMetadataIndex = v1State.getLastMetadataIndex();
    lastPollResult = v1State.getLastPollResult();
    lastPollStart = v1State.getLastPollStart();
    lastPoPPoll = v1State.getLastPoPPoll();
    lastPoPPollResult = v1State.getLastPoPPollResult();
    lastTopLevelPollTime = v1State.getLastTopLevelPollTime();
    metadataVersion = v1State.getFeatureVersion(Feature.Metadata);
    numAgreePeersLastPoR = v1State.getNumAgreePeersLastPoR();
    numCurrentSuspectVersions = v1State.getNumCurrentSuspectVersions();
    numWillingRepairers = v1State.getNumWillingRepairers();
    pollDuration = v1State.getPollDuration();
    substanceVersion = v1State.getFeatureVersion(Feature.Substance);
    v3Agreement = v1State.getV3Agreement();
  }

  /**
   * @return the auCreationTime
   */
  public long getAuCreationTime() {
    return auCreationTime;
  }

  /**
   * @param auCreationTime the auCreationTime to set
   */
  public void setAuCreationTime(long auCreationTime) {
    this.auCreationTime = auCreationTime;
  }

  /**
   * @return the lastCrawlTime
   */
  public long getLastCrawlTime() {
    return lastCrawlTime;
  }

  /**
   * @param lastCrawlTime the lastCrawlTime to set
   */
  public void setLastCrawlTime(long lastCrawlTime) {
    this.lastCrawlTime = lastCrawlTime;
  }

  /**
   * @return the lastCrawlAttempt
   */
  public long getLastCrawlAttempt() {
    return lastCrawlAttempt;
  }

  /**
   * @param lastCrawlAttempt the lastCrawlAttempt to set
   */
  public void setLastCrawlAttempt(long lastCrawlAttempt) {
    this.lastCrawlAttempt = lastCrawlAttempt;
  }

  /**
   * @return the lastCrawlResultMsg
   */
  public String getLastCrawlResultMsg() {
    return lastCrawlResultMsg;
  }

  /**
   * @param lastCrawlResultMsg the lastCrawlResultMsg to set
   */
  public void setLastCrawlResultMsg(String lastCrawlResultMsg) {
    this.lastCrawlResultMsg = lastCrawlResultMsg;
  }

  /**
   * @return the lastCrawlResult
   */
  public int getLastCrawlResult() {
    return lastCrawlResult;
  }

  /**
   * @param lastCrawlResult the lastCrawlResult to set
   */
  public void setLastCrawlResult(int lastCrawlResult) {
    this.lastCrawlResult = lastCrawlResult;
  }

  /**
   * @return the lastDeepCrawlTime
   */
  public long getLastDeepCrawlTime() {
    return lastDeepCrawlTime;
  }

  /**
   * @param lastDeepCrawlTime the lastDeepCrawlTime to set
   */
  public void setLastDeepCrawlTime(long lastDeepCrawlTime) {
    this.lastDeepCrawlTime = lastDeepCrawlTime;
  }

  /**
   * @return the lastDeepCrawlAttempt
   */
  public long getLastDeepCrawlAttempt() {
    return lastDeepCrawlAttempt;
  }

  /**
   * @param lastDeepCrawlAttempt the lastDeepCrawlAttempt to set
   */
  public void setLastDeepCrawlAttempt(long lastDeepCrawlAttempt) {
    this.lastDeepCrawlAttempt = lastDeepCrawlAttempt;
  }

  /**
   * @return the lastDeepCrawlResultMsg
   */
  public String getLastDeepCrawlResultMsg() {
    return lastDeepCrawlResultMsg;
  }

  /**
   * @param lastDeepCrawlResultMsg the lastDeepCrawlResultMsg to set
   */
  public void setLastDeepCrawlResultMsg(String lastDeepCrawlResultMsg) {
    this.lastDeepCrawlResultMsg = lastDeepCrawlResultMsg;
  }

  /**
   * @return the lastDeepCrawlResult
   */
  public int getLastDeepCrawlResult() {
    return lastDeepCrawlResult;
  }

  /**
   * @param lastDeepCrawlResult the lastDeepCrawlResult to set
   */
  public void setLastDeepCrawlResult(int lastDeepCrawlResult) {
    this.lastDeepCrawlResult = lastDeepCrawlResult;
  }

  /**
   * @return the lastDeepCrawlDepth
   */
  public int getLastDeepCrawlDepth() {
    return lastDeepCrawlDepth;
  }

  /**
   * @param lastDeepCrawlDepth the lastDeepCrawlDepth to set
   */
  public void setLastDeepCrawlDepth(int lastDeepCrawlDepth) {
    this.lastDeepCrawlDepth = lastDeepCrawlDepth;
  }

  /**
   * @return the lastTopLevelPollTime
   */
  public long getLastTopLevelPollTime() {
    return lastTopLevelPollTime;
  }

  /**
   * @param lastTopLevelPollTime the lastTopLevelPollTime to set
   */
  public void setLastTopLevelPollTime(long lastTopLevelPollTime) {
    this.lastTopLevelPollTime = lastTopLevelPollTime;
  }

  /**
   * @return the lastPollStart
   */
  public long getLastPollStart() {
    return lastPollStart;
  }

  /**
   * @param lastPollStart the lastPollStart to set
   */
  public void setLastPollStart(long lastPollStart) {
    this.lastPollStart = lastPollStart;
  }

  /**
   * @return the lastPollResult
   */
  public int getLastPollResult() {
    return lastPollResult;
  }

  /**
   * @param lastPollResult the lastPollResult to set
   */
  public void setLastPollResult(int lastPollResult) {
    this.lastPollResult = lastPollResult;
  }

  /**
   * @return the pollDuration
   */
  public long getPollDuration() {
    return pollDuration;
  }

  /**
   * @param pollDuration the pollDuration to set
   */
  public void setPollDuration(long pollDuration) {
    this.pollDuration = pollDuration;
  }

  /**
   * @return the averageHashDuration
   */
  public long getAverageHashDuration() {
    return averageHashDuration;
  }

  /**
   * @param averageHashDuration the averageHashDuration to set
   */
  public void setAverageHashDuration(long averageHashDuration) {
    this.averageHashDuration = averageHashDuration;
  }

  /**
   * @return the clockssSubscriptionStatus
   */
  public int getClockssSubscriptionStatus() {
    return clockssSubscriptionStatus;
  }

  /**
   * @param clockssSubscriptionStatus the clockssSubscriptionStatus to set
   */
  public void setClockssSubscriptionStatus(int clockssSubscriptionStatus) {
    this.clockssSubscriptionStatus = clockssSubscriptionStatus;
  }

  /**
   * @return the v3Agreement
   */
  public double getV3Agreement() {
    return v3Agreement;
  }

  /**
   * @param v3Agreement the v3Agreement to set
   */
  public void setV3Agreement(double v3Agreement) {
    this.v3Agreement = v3Agreement;
  }

  /**
   * @return the highestV3Agreement
   */
  public double getHighestV3Agreement() {
    return highestV3Agreement;
  }

  /**
   * @param highestV3Agreement the highestV3Agreement to set
   */
  public void setHighestV3Agreement(double highestV3Agreement) {
    this.highestV3Agreement = highestV3Agreement;
  }

  /**
   * @return the accessType
   */
  public AccessType getAccessType() {
    return accessType;
  }

  /**
   * @param accessType the accessType to set
   */
  public void setAccessType(AccessType accessType) {
    this.accessType = accessType;
  }

  /**
   * @return the hasSubstance
   */
  public SubstanceChecker.State getHasSubstance() {
    return hasSubstance;
  }

  /**
   * @param hasSubstance the hasSubstance to set
   */
  public void setHasSubstance(SubstanceChecker.State hasSubstance) {
    this.hasSubstance = hasSubstance;
  }

  /**
   * @return the substanceVersion
   */
  public String getSubstanceVersion() {
    return substanceVersion;
  }

  /**
   * @param substanceVersion the substanceVersion to set
   */
  public void setSubstanceVersion(String substanceVersion) {
    this.substanceVersion = substanceVersion;
  }

  /**
   * @return the metadataVersion
   */
  public String getMetadataVersion() {
    return metadataVersion;
  }

  /**
   * @param metadataVersion the metadataVersion to set
   */
  public void setMetadataVersion(String metadataVersion) {
    this.metadataVersion = metadataVersion;
  }

  /**
   * @return the lastMetadataIndex
   */
  public long getLastMetadataIndex() {
    return lastMetadataIndex;
  }

  /**
   * @param lastMetadataIndex the lastMetadataIndex to set
   */
  public void setLastMetadataIndex(long lastMetadataIndex) {
    this.lastMetadataIndex = lastMetadataIndex;
  }

  /**
   * @return the lastContentChange
   */
  public long getLastContentChange() {
    return lastContentChange;
  }

  /**
   * @param lastContentChange the lastContentChange to set
   */
  public void setLastContentChange(long lastContentChange) {
    this.lastContentChange = lastContentChange;
  }

  /**
   * @return the lastPoPPoll
   */
  public long getLastPoPPoll() {
    return lastPoPPoll;
  }

  /**
   * @param lastPoPPoll the lastPoPPoll to set
   */
  public void setLastPoPPoll(long lastPoPPoll) {
    this.lastPoPPoll = lastPoPPoll;
  }

  /**
   * @return the lastPoPPollResult
   */
  public int getLastPoPPollResult() {
    return lastPoPPollResult;
  }

  /**
   * @param lastPoPPollResult the lastPoPPollResult to set
   */
  public void setLastPoPPollResult(int lastPoPPollResult) {
    this.lastPoPPollResult = lastPoPPollResult;
  }

  /**
   * @return the lastLocalHashScan
   */
  public long getLastLocalHashScan() {
    return lastLocalHashScan;
  }

  /**
   * @param lastLocalHashScan the lastLocalHashScan to set
   */
  public void setLastLocalHashScan(long lastLocalHashScan) {
    this.lastLocalHashScan = lastLocalHashScan;
  }

  /**
   * @return the numAgreePeersLastPoR
   */
  public int getNumAgreePeersLastPoR() {
    return numAgreePeersLastPoR;
  }

  /**
   * @param numAgreePeersLastPoR the numAgreePeersLastPoR to set
   */
  public void setNumAgreePeersLastPoR(int numAgreePeersLastPoR) {
    this.numAgreePeersLastPoR = numAgreePeersLastPoR;
  }

  /**
   * @return the numWillingRepairers
   */
  public int getNumWillingRepairers() {
    return numWillingRepairers;
  }

  /**
   * @param numWillingRepairers the numWillingRepairers to set
   */
  public void setNumWillingRepairers(int numWillingRepairers) {
    this.numWillingRepairers = numWillingRepairers;
  }

  /**
   * @return the numCurrentSuspectVersions
   */
  public int getNumCurrentSuspectVersions() {
    return numCurrentSuspectVersions;
  }

  /**
   * @param numCurrentSuspectVersions the numCurrentSuspectVersions to set
   */
  public void setNumCurrentSuspectVersions(int numCurrentSuspectVersions) {
    this.numCurrentSuspectVersions = numCurrentSuspectVersions;
  }

  /**
   * @return the cdnStems
   */
  public List<String> getCdnStems() {
    return cdnStems;
  }

  /**
   * @param cdnStems the cdnStems to set
   */
  public void setCdnStems(List<String> cdnStems) {
    this.cdnStems = cdnStems;
  }

  /**
   * @return the isMetadataExtractionEnabled
   */
  public boolean isMetadataExtractionEnabled() {
    return isMetadataExtractionEnabled;
  }

  /**
   * @param isMetadataExtractionEnabled the isMetadataExtractionEnabled to set
   */
  public void setMetadataExtractionEnabled(boolean isMetadataExtractionEnabled) {
    this.isMetadataExtractionEnabled = isMetadataExtractionEnabled;
  }

  /**
   * @return the AUID
   */
  public String getAuId() {
    return auId;
  }

  /**
   * @param auId the auid to set
   */
  public void setAuId(String auId) {
    this.auId = auId;
  }


  /**
   * Serialize entire object to json string
   */
  public String toJson() {
    Gson gson = new GsonBuilder().create();
    return gson.toJson(toMap());
  }

  public Map<String,Object> toMap() {
    Map<String, Object> propMap = new HashMap<>();
    propMap.put("accessType", accessType);
    propMap.put("auCreationTime", auCreationTime);
    propMap.put("auId", auId);
    propMap.put("averageHashDuration", averageHashDuration);
    propMap.put("cdnStems", cdnStems);
    propMap.put("clockssSubscriptionStatus", clockssSubscriptionStatus);
    propMap.put("hasSubstance", hasSubstance);
    propMap.put("highestV3Agreement", highestV3Agreement);
    propMap.put("isMetadataExtractionEnabled", isMetadataExtractionEnabled);
    propMap.put("lastContentChange", lastContentChange);
    propMap.put("lastCrawlAttempt", lastCrawlAttempt);
    propMap.put("lastCrawlResult", lastCrawlResult);
    propMap.put("lastCrawlResultMsg", lastCrawlResultMsg);
    propMap.put("lastCrawlTime", lastCrawlTime);
    propMap.put("lastDeepCrawlAttempt", lastDeepCrawlAttempt);
    propMap.put("lastDeepCrawlDepth", lastDeepCrawlDepth);
    propMap.put("lastDeepCrawlResult", lastDeepCrawlResult);
    propMap.put("lastDeepCrawlResultMsg", lastDeepCrawlResultMsg);
    propMap.put("lastDeepCrawlTime", lastDeepCrawlTime);
    propMap.put("lastLocalHashScan", lastLocalHashScan);
    propMap.put("lastMetadataIndex", lastMetadataIndex);
    propMap.put("lastPollResult", lastPollResult);
    propMap.put("lastPollStart", lastPollStart);
    propMap.put("lastPoPPoll", lastPoPPoll);
    propMap.put("lastPoPPollResult", lastPoPPollResult);
    propMap.put("lastTopLevelPollTime", lastTopLevelPollTime);
    propMap.put("metadataVersion", metadataVersion);
    propMap.put("numAgreePeersLastPoR", numAgreePeersLastPoR);
    propMap.put("numCurrentSuspectVersions", numCurrentSuspectVersions);
    propMap.put("numWillingRepairers", numWillingRepairers);
    propMap.put("pollDuration", pollDuration);
    propMap.put("substanceVersion", substanceVersion);
    propMap.put("v3Agreement", v3Agreement);
    return propMap;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("accessType", accessType)
      .append("auCreationTime", auCreationTime)
      .append("auId", auId)
      .append("averageHashDuration", averageHashDuration)
      .append("cdnStems", cdnStems)
      .append("clockssSubscriptionStatus", clockssSubscriptionStatus)
      .append("hasSubstance", hasSubstance)
      .append("highestV3Agreement", highestV3Agreement)
      .append("isMetadataExtractionEnabled", isMetadataExtractionEnabled)
      .append("lastContentChange", lastContentChange)
      .append("lastCrawlAttempt", lastCrawlAttempt)
      .append("lastCrawlResult", lastCrawlResult)
      .append("lastCrawlResultMsg", lastCrawlResultMsg)
      .append("lastCrawlTime", lastCrawlTime)
      .append("lastDeepCrawlAttempt", lastDeepCrawlAttempt)
      .append("lastDeepCrawlDepth", lastDeepCrawlDepth)
      .append("lastDeepCrawlResult", lastDeepCrawlResult)
      .append("lastDeepCrawlResultMsg", lastDeepCrawlResultMsg)
      .append("lastDeepCrawlTime", lastDeepCrawlTime)
      .append("lastLocalHashScan", lastLocalHashScan)
      .append("lastMetadataIndex", lastMetadataIndex)
      .append("lastPollResult", lastPollResult)
      .append("lastPollStart", lastPollStart)
      .append("lastPoPPoll", lastPoPPoll)
      .append("lastPoPPollResult", lastPoPPollResult)
      .append("lastTopLevelPollTime", lastTopLevelPollTime)
      .append("metadataVersion", metadataVersion)
      .append("numAgreePeersLastPoR", numAgreePeersLastPoR)
      .append("numCurrentSuspectVersions", numCurrentSuspectVersions)
      .append("numWillingRepairers", numWillingRepairers)
      .append("pollDuration", pollDuration)
      .append("substanceVersion", substanceVersion)
      .append("v3Agreement", v3Agreement)
      .toString();
  }
}
