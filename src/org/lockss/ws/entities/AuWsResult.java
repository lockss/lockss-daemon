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
package org.lockss.ws.entities;

import java.util.List;

/**
 * Container for the information related to an archival unit that is the result
 * of a query.
 */
public class AuWsResult {
  private String auId;
  private String name;
  private String volume;
  private String pluginName;
  private String tdbYear;
  private String accessType;
  private Long contentSize;
  private Long diskUsage;
  private String repositoryPath;
  private Double recentPollAgreement;
  private Double highestPollAgreement;
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
  private List<PeerAgreementsWsResult> peerAgreements;
  private List<UrlWsResult> urls;
  private List<String> substanceUrls;
  private List<String> articleUrls;

  /**
   * Provides the Archival Unit identifier.
   * 
   * @return a String with the identifier.
   */
  public String getAuId() {
    return auId;
  }
  public void setAuId(String auId) {
    this.auId = auId;
  }

  /**
   * Provides the Archival Unit name.
   * 
   * @return a String with the name.
   */
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Provides the Archival Unit volume name.
   * 
   * @return a String with the volume name.
   */
  public String getVolume() {
    return volume;
  }
  public void setVolume(String volume) {
    this.volume = volume;
  }

  /**
   * Provides the Archival Unit plugin name.
   * 
   * @return a String with the plugin name.
   */
  public String getPluginName() {
    return pluginName;
  }
  public void setPluginName(String pluginName) {
    this.pluginName = pluginName;
  }

  /**
   * Provides the Archival Unit publication year from the TDB.
   * 
   * @return a String with the publication year from the TDB.
   */
  public String getTdbYear() {
    return tdbYear;
  }
  public void setTdbYear(String tdbYear) {
    this.tdbYear = tdbYear;
  }

  /**
   * Provides the Archival Unit access type.
   * 
   * @return a String with the access type.
   */
  public String getAccessType() {
    return accessType;
  }
  public void setAccessType(String accessType) {
    this.accessType = accessType;
  }

  /**
   * Provides the size of the Archival Unit.
   * 
   * @return a Long with the size in bytes.
   */
  public Long getContentSize() {
    return contentSize;
  }
  public void setContentSize(Long contentSize) {
    this.contentSize = contentSize;
  }

  /**
   * Provides the space occupied on disk by the Archival Unit.
   * 
   * @return a Long with the occupied space in bytes.
   */
  public Long getDiskUsage() {
    return diskUsage;
  }
  public void setDiskUsage(Long diskUsage) {
    this.diskUsage = diskUsage;
  }

  /**
   * Provides the Archival Unit repository path.
   * 
   * @return a String with the repository path.
   */
  public String getRepositoryPath() {
    return repositoryPath;
  }
  public void setRepositoryPath(String repositoryPath) {
    this.repositoryPath = repositoryPath;
  }

  /**
   * Provides the Archival Unit most recent poll agreement percentage.
   * 
   * @return a Double with the most recent poll agreement percentage.
   */
  public Double getRecentPollAgreement() {
    return recentPollAgreement;
  }
  public void setRecentPollAgreement(Double recentPollAgreement) {
    this.recentPollAgreement = recentPollAgreement;
  }

  /**
   * Provides the Archival Unit highest poll agreement percentage.
   * 
   * @return a Double with the highest poll agreement percentage.
   */
  public Double getHighestPollAgreement() {
    return highestPollAgreement;
  }
  public void setHighestPollAgreement(Double highestPollAgreement) {
    this.highestPollAgreement = highestPollAgreement;
  }

  /**
   * Provides the Archival Unit publishing platform name.
   * 
   * @return a String with the publishing platform name.
   */
  public String getPublishingPlatform() {
    return publishingPlatform;
  }
  public void setPublishingPlatform(String publishingPlatform) {
    this.publishingPlatform = publishingPlatform;
  }

  /**
   * Provides the Archival Unit publisher name from the TDB.
   * 
   * @return a String with the publisher name from the TDB.
   */
  public String getTdbPublisher() {
    return tdbPublisher;
  }
  public void setTdbPublisher(String tdbPublisher) {
    this.tdbPublisher = tdbPublisher;
  }

  /**
   * Provides an indication of whether the Archival Unit is available from the
   * publisher website.
   * 
   * @return a Boolean with the indication.
   */
  public Boolean getAvailableFromPublisher() {
    return availableFromPublisher;
  }
  public void setAvailableFromPublisher(Boolean availableFromPublisher) {
    this.availableFromPublisher = availableFromPublisher;
  }

  /**
   * Provides the Archival Unit substance state.
   * 
   * @return a String with the substance state.
   */
  public String getSubstanceState() {
    return substanceState;
  }
  public void setSubstanceState(String substanceState) {
    this.substanceState = substanceState;
  }

  /**
   * Provides the Archival Unit creation timestamp.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getCreationTime() {
    return creationTime;
  }
  public void setCreationTime(Long creationTime) {
    this.creationTime = creationTime;
  }

  /**
   * Provides the Archival Unit crawl proxy name.
   * 
   * @return a String with the crawl proxy name.
   */
  public String getCrawlProxy() {
    return crawlProxy;
  }
  public void setCrawlProxy(String crawlProxy) {
    this.crawlProxy = crawlProxy;
  }

  /**
   * Provides the Archival Unit crawl window.
   * 
   * @return a String with the crawl window.
   */
  public String getCrawlWindow() {
    return crawlWindow;
  }
  public void setCrawlWindow(String crawlWindow) {
    this.crawlWindow = crawlWindow;
  }

  /**
   * Provides the Archival Unit crawl pool name.
   * 
   * @return a String with the crawl pool name.
   */
  public String getCrawlPool() {
    return crawlPool;
  }
  public void setCrawlPool(String crawlPool) {
    this.crawlPool = crawlPool;
  }
  /**
   * Provides the timestamp of the last completed crawl of the Archival Unit.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getLastCompletedCrawl() {
    return lastCompletedCrawl;
  }
  public void setLastCompletedCrawl(Long lastCompletedCrawl) {
    this.lastCompletedCrawl = lastCompletedCrawl;
  }

  /**
   * Provides the timestamp of the last crawl of the Archival Unit.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getLastCrawl() {
    return lastCrawl;
  }
  public void setLastCrawl(Long lastCrawl) {
    this.lastCrawl = lastCrawl;
  }

  /**
   * Provides the result of the last crawl of the Archival Unit.
   * 
   * @return a String with the last crawl result.
   */
  public String getLastCrawlResult() {
    return lastCrawlResult;
  }
  public void setLastCrawlResult(String lastCrawlResult) {
    this.lastCrawlResult = lastCrawlResult;
  }

  /**
   * Provides the timestamp of the last completed poll of the Archival Unit.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getLastCompletedPoll() {
    return lastCompletedPoll;
  }
  public void setLastCompletedPoll(Long lastCompletedPoll) {
    this.lastCompletedPoll = lastCompletedPoll;
  }

  /**
   * Provides the timestamp of the last poll of the Archival Unit.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getLastPoll() {
    return lastPoll;
  }
  public void setLastPoll(Long lastPoll) {
    this.lastPoll = lastPoll;
  }

  /**
   * Provides the result of the last poll of the Archival Unit.
   * 
   * @return a String with the last poll result.
   */
  public String getLastPollResult() {
    return lastPollResult;
  }
  public void setLastPollResult(String lastPollResult) {
    this.lastPollResult = lastPollResult;
  }

  /**
   * Provides an indication of whether the Archival Unit is currently in the
   * process of crawling.
   * 
   * @return a Boolean with the indication.
   */
  public Boolean getCurrentlyCrawling() {
    return currentlyCrawling;
  }
  public void setCurrentlyCrawling(Boolean currentlyCrawling) {
    this.currentlyCrawling = currentlyCrawling;
  }

  /**
   * Provides an indication of whether the Archival Unit is currently in the
   * process of polling.
   * 
   * @return a Boolean with the indication.
   */
  public Boolean getCurrentlyPolling() {
    return currentlyPolling;
  }
  public void setCurrentlyPolling(Boolean currentlyPolling) {
    this.currentlyPolling = currentlyPolling;
  }

  /**
   * Provides the Archival Unit subscription status.
   * 
   * @return a String with the subscription status.
   */
  public String getSubscriptionStatus() {
    return subscriptionStatus;
  }
  public void setSubscriptionStatus(String subscriptionStatus) {
    this.subscriptionStatus = subscriptionStatus;
  }

  /**
   * Provides the Archival Unit configuration.
   * 
   * @return a AuConfigurationWsResult with the configuration.
   */
  public AuConfigurationWsResult getAuConfiguration() {
    return auConfiguration;
  }
  public void setAuConfiguration(AuConfigurationWsResult auConfiguration) {
    this.auConfiguration = auConfiguration;
  }

  /**
   * Provides the Archival Unit URLs to crawl new content.
   * 
   * @return a List<String> with the URLs to crawl new content.
   */
  public List<String> getNewContentCrawlUrls() {
    return newContentCrawlUrls;
  }
  public void setNewContentCrawlUrls(List<String> newContentCrawlUrls) {
    this.newContentCrawlUrls = newContentCrawlUrls;
  }

  /**
   * Provides the Archival Unit URL stems.
   * 
   * @return a List<String> with the URL stems.
   */
  public List<String> getUrlStems() {
    return urlStems;
  }
  public void setUrlStems(List<String> urlStems) {
    this.urlStems = urlStems;
  }

  /**
   * Provides an indication of whether the Archival Unit contains bulk content,
   * as opposed to harvested content.
   * 
   * @return a Boolean with the indication.
   */
  public Boolean getIsBulkContent() {
    return isBulkContent;
  }
  public void setIsBulkContent(Boolean isBulkContent) {
    this.isBulkContent = isBulkContent;
  }

  /**
   * Provides the Archival Unit peer agreements.
   * 
   * @return a List<PeerAgreementsWsResult> with the peer agreements.
   */
  public List<PeerAgreementsWsResult> getPeerAgreements() {
    return peerAgreements;
  }
  public void setPeerAgreements(List<PeerAgreementsWsResult> peerAgreements) {
    this.peerAgreements = peerAgreements;
  }

  /**
   * Provides the Archival Unit URLs.
   * 
   * @return a List<UrlWsResult> with the URLs.
   */
  public List<UrlWsResult> getUrls() {
    return urls;
  }
  public void setUrls(List<UrlWsResult> urls) {
    this.urls = urls;
  }

  /**
   * Provides the Archival Unit substance URLs.
   * 
   * @return a List<String> with the substance URLs.
   */
  public List<String> getSubstanceUrls() {
    return substanceUrls;
  }
  public void setSubstanceUrls(List<String> substanceUrls) {
    this.substanceUrls = substanceUrls;
  }

  /**
   * Provides the Archival Unit article URLs.
   * 
   * @return a List<String> with the article URLs.
   */
  public List<String> getArticleUrls() {
    return articleUrls;
  }
  public void setArticleUrls(List<String> articleUrls) {
    this.articleUrls = articleUrls;
  }

  @Override
  public String toString() {
    return "[AuWsResult auId=" + auId + ", name=" + name + ", volume=" + volume
	+ ", pluginName=" + pluginName + ", tdbYear=" + tdbYear
	+ ", accessType=" + accessType + ", contentSize=" + contentSize
	+ ", diskUsage=" + diskUsage + ", repositoryPath=" + repositoryPath
	+ ", recentPollAgreement=" + recentPollAgreement
	+ ", highestPollAgreement=" + highestPollAgreement
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
	+ urlStems + ", isBulkContent=" + isBulkContent + ", peerAgreements="
	+ peerAgreements + ", urls=" + urls + ", substanceUrls=" + substanceUrls
	+ ", articleUrls=" + articleUrls + "]";
  }
}
