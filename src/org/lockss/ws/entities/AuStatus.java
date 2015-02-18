/*
 * $Id$
 */

/*

 Copyright (c) 2013-2014 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * The status information of an Archival Unit.
 */
public class AuStatus {
  private String volume;
  private String journalTitle;
  private String pluginName;
  private String year;
  private String accessType;
  private Long contentSize;
  private Long diskUsage;
  private String repository;
  private String status;
  private Double recentPollAgreement;
  private String publishingPlatform;
  private String publisher;
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

  /**
   * Provides the Archival Unit volume name.
   * 
   * @return a String with the volume name.
   */
  public String getVolume() {
    return volume;
  }

  /**
   * Provides the Archival Unit journal title.
   * 
   * @return a String with the journal title.
   */
  public String getJournalTitle() {
    return journalTitle;
  }

  /**
   * Provides the Archival Unit plugin name.
   * 
   * @return a String with the plugin name.
   */
  public String getPluginName() {
    return pluginName;
  }

  /**
   * Provides the Archival Unit publication year.
   * 
   * @return an String with the publication year.
   */
  public String getYear() {
    return year;
  }

  /**
   * Provides the Archival Unit access type.
   * 
   * @return a String with the access type.
   */
  public String getAccessType() {
    return accessType;
  }

  /**
   * Provides the size of the Archival Unit.
   * 
   * @return a Long with the size in bytes.
   */
  public Long getContentSize() {
    return contentSize;
  }

  /**
   * Provides the space occupied on disk by the Archival Unit.
   * 
   * @return a Long with the occupied space in bytes.
   */
  public Long getDiskUsage() {
    return diskUsage;
  }

  /**
   * Provides the Archival Unit repository name.
   * 
   * @return a String with the repository name.
   */
  public String getRepository() {
    return repository;
  }

  /**
   * Provides the Archival Unit status.
   * 
   * @return a String with the status.
   */
  public String getStatus() {
    return status;
  }

  /**
   * Provides the Archival Unit most recent poll agreement percentage.
   * 
   * @return a Double with the most recent poll agreement percentage.
   */
  public Double getRecentPollAgreement() {
    return recentPollAgreement;
  }

  /**
   * Provides the Archival Unit publishing platform name.
   * 
   * @return a String with the publishing platform name.
   */
  public String getPublishingPlatform() {
    return publishingPlatform;
  }

  /**
   * Provides the Archival Unit publisher name.
   * 
   * @return a String with the publisher name.
   */
  public String getPublisher() {
    return publisher;
  }

  /**
   * Provides an indication of whether the Archival Unit is available from the
   * publisher website.
   * 
   * @return a Boolean with the indication.
   */
  public Boolean isAvailableFromPublisher() {
    return availableFromPublisher;
  }

  /**
   * Provides the Archival Unit substance state.
   * 
   * @return a String with the substance state.
   */
  public String getSubstanceState() {
    return substanceState;
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

  /**
   * Provides the Archival Unit crawl proxy name.
   * 
   * @return a String with the crawl proxy name.
   */
  public String getCrawlProxy() {
    return crawlProxy;
  }

  /**
   * Provides the Archival Unit crawl window.
   * 
   * @return a String with the crawl window.
   */
  public String getCrawlWindow() {
    return crawlWindow;
  }

  /**
   * Provides the Archival Unit crawl pool name.
   * 
   * @return a String with the crawl pool name.
   */
  public String getCrawlPool() {
    return crawlPool;
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

  /**
   * Provides the timestamp of the last crawl of the Archival Unit.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getLastCrawl() {
    return lastCrawl;
  }

  /**
   * Provides the result of the last crawl of the Archival Unit.
   * 
   * @return a String with the last crawl result.
   */
  public String getLastCrawlResult() {
    return lastCrawlResult;
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

  /**
   * Provides the timestamp of the last poll of the Archival Unit.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getLastPoll() {
    return lastPoll;
  }

  /**
   * Provides the result of the last poll of the Archival Unit.
   * 
   * @return a String with the last poll result.
   */
  public String getLastPollResult() {
    return lastPollResult;
  }

  /**
   * Provides an indication of whether the Archival Unit is currently in the
   * process of crawling.
   * 
   * @return a Boolean with the indication.
   */
  public Boolean isCurrentlyCrawling() {
    return currentlyCrawling;
  }

  /**
   * Provides an indication of whether the Archival Unit is currently in the
   * process of polling.
   * 
   * @return a Boolean with the indication.
   */
  public Boolean isCurrentlyPolling() {
    return currentlyPolling;
  }

  /**
   * Provides the Archival Unit subscription status.
   * 
   * @return a String with the subscription status.
   */
  public String getSubscriptionStatus() {
    return subscriptionStatus;
  }

  public void setVolume(String volume) {
    this.volume = volume;
  }

  public void setJournalTitle(String journalTitle) {
    this.journalTitle = journalTitle;
  }

  public void setPluginName(String pluginName) {
    this.pluginName = pluginName;
  }

  public void setYear(String year) {
    this.year = year;
  }

  public void setAccessType(String accessType) {
    this.accessType = accessType;
  }

  public void setContentSize(Long contentSize) {
    this.contentSize = contentSize;
  }

  public void setDiskUsage(Long diskUsage) {
    this.diskUsage = diskUsage;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setRecentPollAgreement(Double recentPollAgreement) {
    this.recentPollAgreement = recentPollAgreement;
  }

  public void setPublishingPlatform(String publishingPlatform) {
    this.publishingPlatform = publishingPlatform;
  }

  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }

  public void setAvailableFromPublisher(Boolean availableFromPublisher) {
    this.availableFromPublisher = availableFromPublisher;
  }

  public void setSubstanceState(String substanceState) {
    this.substanceState = substanceState;
  }

  public void setCreationTime(Long creationTime) {
    this.creationTime = creationTime;
  }

  public void setCrawlProxy(String crawlProxy) {
    this.crawlProxy = crawlProxy;
  }

  public void setCrawlWindow(String crawlWindow) {
    this.crawlWindow = crawlWindow;
  }

  public void setCrawlPool(String crawlPool) {
    this.crawlPool = crawlPool;
  }

  public void setLastCompletedCrawl(Long lastCompletedCrawl) {
    this.lastCompletedCrawl = lastCompletedCrawl;
  }

  public void setLastCrawl(Long lastCrawl) {
    this.lastCrawl = lastCrawl;
  }

  public void setLastCrawlResult(String lastCrawlResult) {
    this.lastCrawlResult = lastCrawlResult;
  }

  public void setLastCompletedPoll(Long lastCompletedPoll) {
    this.lastCompletedPoll = lastCompletedPoll;
  }

  public void setLastPoll(Long lastPoll) {
    this.lastPoll = lastPoll;
  }

  public void setLastPollResult(String lastPollResult) {
    this.lastPollResult = lastPollResult;
  }

  public void setCurrentlyCrawling(Boolean currentlyCrawling) {
    this.currentlyCrawling = currentlyCrawling;
  }

  public void setCurrentlyPolling(Boolean currentlyPolling) {
    this.currentlyPolling = currentlyPolling;
  }

  public void setSubscriptionStatus(String subscriptionStatus) {
    this.subscriptionStatus = subscriptionStatus;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("AuStatus [volume=");
    builder.append(volume);
    builder.append(", journalTitle=");
    builder.append(journalTitle);
    builder.append(", pluginName=");
    builder.append(pluginName);
    builder.append(", year=");
    builder.append(year);
    builder.append(", accessType=");
    builder.append(accessType);
    builder.append(", contentSize=");
    builder.append(contentSize);
    builder.append(", diskUsage=");
    builder.append(diskUsage);
    builder.append(", repository=");
    builder.append(repository);
    builder.append(", status=");
    builder.append(status);
    builder.append(", recentPollAgreement=");
    builder.append(recentPollAgreement);
    builder.append(", publishingPlatform=");
    builder.append(publishingPlatform);
    builder.append(", publisher=");
    builder.append(publisher);
    builder.append(", availableFromPublisher=");
    builder.append(availableFromPublisher);
    builder.append(", substanceState=");
    builder.append(substanceState);
    builder.append(", creationTime=");
    builder.append(creationTime);
    builder.append(", crawlProxy=");
    builder.append(crawlProxy);
    builder.append(", crawlWindow=");
    builder.append(crawlWindow);
    builder.append(", crawlPool=");
    builder.append(crawlPool);
    builder.append(", lastCompletedCrawl=");
    builder.append(lastCompletedCrawl);
    builder.append(", lastCrawl=");
    builder.append(lastCrawl);
    builder.append(", lastCrawlResult=");
    builder.append(lastCrawlResult);
    builder.append(", lastCompletedPoll=");
    builder.append(lastCompletedPoll);
    builder.append(", lastPoll=");
    builder.append(lastPoll);
    builder.append(", lastPollResult=");
    builder.append(lastPollResult);
    builder.append(", currentlyCrawling=");
    builder.append(currentlyCrawling);
    builder.append(", currentlyPolling=");
    builder.append(currentlyPolling);
    builder.append(", subscriptionStatus=");
    builder.append(subscriptionStatus);
    builder.append("]");
    return builder.toString();
  }
}
