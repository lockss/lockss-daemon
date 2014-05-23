/*
 * $Id: AuWsResult.java,v 1.2 2014-05-23 22:07:10 fergaloy-sf Exp $
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

/**
 * Container for the information related to an archival unit that is the result
 * of a query.
 */
package org.lockss.ws.entities;

import java.util.List;

public class AuWsResult {
  private String auId;
  private String name;
  private String volume;
  private String pluginName;
  private Integer tdbYear;
  private String accessType;
  private Long contentSize;
  private Long diskUsage;
  private String repositoryPath;
  private Double recentPollAgreement;
  private String publishingPlatform;
  private String tdbPublisher;
  private Boolean availableFromPublisher;
  private String substanceState;
  private Long creationTime;
  private String crawlProxy;
  private String crawlWindow;
  private String crawlPool;
  private Long lastCompletedCrawl;
  private Long lastCrawl;
  private String lastCrawlResult;
  private Long lastCompletedPoll;
  private Long lastPoll;
  private String lastPollResult;
  private Boolean currentlyCrawling;
  private Boolean currentlyPolling;
  private String subscriptionStatus;
  private AuConfigurationWsResult auConfiguration;
  private List<String> newContentCrawlUrls;
  private List<String> urlStems;
  private Boolean isBulkContent;

  public String getAuId() {
    return auId;
  }
  public void setAuId(String auId) {
    this.auId = auId;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getVolume() {
    return volume;
  }
  public void setVolume(String volume) {
    this.volume = volume;
  }
  public String getPluginName() {
    return pluginName;
  }
  public void setPluginName(String pluginName) {
    this.pluginName = pluginName;
  }
  public Integer getTdbYear() {
    return tdbYear;
  }
  public void setTdbYear(Integer tdbYear) {
    this.tdbYear = tdbYear;
  }
  public String getAccessType() {
    return accessType;
  }
  public void setAccessType(String accessType) {
    this.accessType = accessType;
  }
  public Long getContentSize() {
    return contentSize;
  }
  public void setContentSize(Long contentSize) {
    this.contentSize = contentSize;
  }
  public Long getDiskUsage() {
    return diskUsage;
  }
  public void setDiskUsage(Long diskUsage) {
    this.diskUsage = diskUsage;
  }
  public String getRepositoryPath() {
    return repositoryPath;
  }
  public void setRepositoryPath(String repositoryPath) {
    this.repositoryPath = repositoryPath;
  }
  public Double getRecentPollAgreement() {
    return recentPollAgreement;
  }
  public void setRecentPollAgreement(Double recentPollAgreement) {
    this.recentPollAgreement = recentPollAgreement;
  }
  public String getPublishingPlatform() {
    return publishingPlatform;
  }
  public void setPublishingPlatform(String publishingPlatform) {
    this.publishingPlatform = publishingPlatform;
  }
  public String getTdbPublisher() {
    return tdbPublisher;
  }
  public void setTdbPublisher(String tdbPublisher) {
    this.tdbPublisher = tdbPublisher;
  }
  public Boolean getAvailableFromPublisher() {
    return availableFromPublisher;
  }
  public void setAvailableFromPublisher(Boolean availableFromPublisher) {
    this.availableFromPublisher = availableFromPublisher;
  }
  public String getSubstanceState() {
    return substanceState;
  }
  public void setSubstanceState(String substanceState) {
    this.substanceState = substanceState;
  }
  public Long getCreationTime() {
    return creationTime;
  }
  public void setCreationTime(Long creationTime) {
    this.creationTime = creationTime;
  }
  public String getCrawlProxy() {
    return crawlProxy;
  }
  public void setCrawlProxy(String crawlProxy) {
    this.crawlProxy = crawlProxy;
  }
  public String getCrawlWindow() {
    return crawlWindow;
  }
  public void setCrawlWindow(String crawlWindow) {
    this.crawlWindow = crawlWindow;
  }
  public String getCrawlPool() {
    return crawlPool;
  }
  public void setCrawlPool(String crawlPool) {
    this.crawlPool = crawlPool;
  }
  public Long getLastCompletedCrawl() {
    return lastCompletedCrawl;
  }
  public void setLastCompletedCrawl(Long lastCompletedCrawl) {
    this.lastCompletedCrawl = lastCompletedCrawl;
  }
  public Long getLastCrawl() {
    return lastCrawl;
  }
  public void setLastCrawl(Long lastCrawl) {
    this.lastCrawl = lastCrawl;
  }
  public String getLastCrawlResult() {
    return lastCrawlResult;
  }
  public void setLastCrawlResult(String lastCrawlResult) {
    this.lastCrawlResult = lastCrawlResult;
  }
  public Long getLastCompletedPoll() {
    return lastCompletedPoll;
  }
  public void setLastCompletedPoll(Long lastCompletedPoll) {
    this.lastCompletedPoll = lastCompletedPoll;
  }
  public Long getLastPoll() {
    return lastPoll;
  }
  public void setLastPoll(Long lastPoll) {
    this.lastPoll = lastPoll;
  }
  public String getLastPollResult() {
    return lastPollResult;
  }
  public void setLastPollResult(String lastPollResult) {
    this.lastPollResult = lastPollResult;
  }
  public Boolean getCurrentlyCrawling() {
    return currentlyCrawling;
  }
  public void setCurrentlyCrawling(Boolean currentlyCrawling) {
    this.currentlyCrawling = currentlyCrawling;
  }
  public Boolean getCurrentlyPolling() {
    return currentlyPolling;
  }
  public void setCurrentlyPolling(Boolean currentlyPolling) {
    this.currentlyPolling = currentlyPolling;
  }
  public String getSubscriptionStatus() {
    return subscriptionStatus;
  }
  public void setSubscriptionStatus(String subscriptionStatus) {
    this.subscriptionStatus = subscriptionStatus;
  }
  public AuConfigurationWsResult getAuConfiguration() {
    return auConfiguration;
  }
  public void setAuConfiguration(AuConfigurationWsResult auConfiguration) {
    this.auConfiguration = auConfiguration;
  }
  public List<String> getNewContentCrawlUrls() {
    return newContentCrawlUrls;
  }
  public void setNewContentCrawlUrls(List<String> newContentCrawlUrls) {
    this.newContentCrawlUrls = newContentCrawlUrls;
  }
  public List<String> getUrlStems() {
    return urlStems;
  }
  public void setUrlStems(List<String> urlStems) {
    this.urlStems = urlStems;
  }
  public Boolean getIsBulkContent() {
    return isBulkContent;
  }
  public void setIsBulkContent(Boolean isBulkContent) {
    this.isBulkContent = isBulkContent;
  }

  @Override
  public String toString() {
    return "AuWsResult [auId=" + auId + ", name=" + name + ", volume=" + volume
	+ ", pluginName=" + pluginName + ", tdbYear=" + tdbYear
	+ ", accessType=" + accessType + ", contentSize=" + contentSize
	+ ", diskUsage=" + diskUsage + ", repositoryPath=" + repositoryPath
	+ ", recentPollAgreement=" + recentPollAgreement
	+ ", publishingPlatform=" + publishingPlatform + ", tdbPublisher="
	+ tdbPublisher + ", availableFromPublisher=" + availableFromPublisher
	+ ", substanceState=" + substanceState + ", creationTime="
	+ creationTime + ", crawlProxy=" + crawlProxy + ", crawlWindow="
	+ crawlWindow + ", crawlPool=" + crawlPool + ", lastCompletedCrawl="
	+ lastCompletedCrawl + ", lastCrawl=" + lastCrawl
	+ ", lastCrawlResult=" + lastCrawlResult + ", lastCompletedPoll="
	+ lastCompletedPoll + ", lastPoll=" + lastPoll + ", lastPollResult="
	+ lastPollResult + ", currentlyCrawling=" + currentlyCrawling
	+ ", currentlyPolling=" + currentlyPolling + ", subscriptionStatus="
	+ subscriptionStatus + ", auConfiguration=" + auConfiguration
	+ ", newContentCrawlUrls=" + newContentCrawlUrls + ", urlStems="
	+ urlStems + ", isBulkContent=" + isBulkContent + "]";
  }
}
