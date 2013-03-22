/*
 * $Id: AuStatus.java,v 1.1 2013-03-22 04:47:32 fergaloy-sf Exp $
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
 * The status information of an archival unit.
 */
package org.lockss.ws.entities;

public class AuStatus {
  private String volume;
  private String journalTitle;
  private String pluginName;
  private Integer year;
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

  public String getVolume() {
    return volume;
  }

  public String getJournalTitle() {
    return journalTitle;
  }

  public String getPluginName() {
    return pluginName;
  }

  public Integer getYear() {
    return year;
  }

  public String getAccessType() {
    return accessType;
  }

  public Long getContentSize() {
    return contentSize;
  }

  public Long getDiskUsage() {
    return diskUsage;
  }

  public String getRepository() {
    return repository;
  }

  public String getStatus() {
    return status;
  }

  public Double getRecentPollAgreement() {
    return recentPollAgreement;
  }

  public String getPublishingPlatform() {
    return publishingPlatform;
  }

  public String getPublisher() {
    return publisher;
  }

  public Boolean isAvailableFromPublisher() {
    return availableFromPublisher;
  }

  public String getSubstanceState() {
    return substanceState;
  }

  public Long getCreationTime() {
    return creationTime;
  }

  public String getCrawlProxy() {
    return crawlProxy;
  }

  public String getCrawlWindow() {
    return crawlWindow;
  }

  public String getCrawlPool() {
    return crawlPool;
  }

  public Long getLastCompletedCrawl() {
    return lastCompletedCrawl;
  }

  public Long getLastCrawl() {
    return lastCrawl;
  }

  public String getLastCrawlResult() {
    return lastCrawlResult;
  }

  public Long getLastCompletedPoll() {
    return lastCompletedPoll;
  }

  public Long getLastPoll() {
    return lastPoll;
  }

  public String getLastPollResult() {
    return lastPollResult;
  }

  public Boolean isCurrentlyCrawling() {
    return currentlyCrawling;
  }

  public Boolean isCurrentlyPolling() {
    return currentlyPolling;
  }

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

  public void setYear(Integer year) {
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
