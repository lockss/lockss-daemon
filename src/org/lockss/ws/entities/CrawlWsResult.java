/*
 * $Id: CrawlWsResult.java,v 1.1.2.2 2014-05-05 17:32:30 wkwilson Exp $
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
 * Container for the information related to a crawl that is the result of a
 * query.
 */
package org.lockss.ws.entities;

import java.util.Collection;
import java.util.List;

public class CrawlWsResult {
  private String auName;
  private String crawlType;
  private Long startTime;
  private Long duration;
  private String crawlStatus;
  private Long bytesFetchedCount;
  private Integer pagesFetchedCount;
  private List<String> pagesFetched;
  private Integer pagesParsedCount;
  private List<String> pagesParsed;
  private Integer pagesPendingCount;
  private List<String> pagesPending;
  private Integer pagesExcludedCount;
  private List<String> pagesExcluded;
  private Integer offSiteUrlsExcludedCount;
  private Integer pagesNotModifiedCount;
  private List<String> pagesNotModified;
  private Integer pagesWithErrorsCount;
  private List<UrlErrorWsResult> pagesWithErrors;
  private Integer mimeTypeCount;
  private Collection<String> mimeTypes;
  private List<String> sources;
  private Collection<String> startingUrls;
  private Integer refetchDepth;
  private Integer linkDepth;

  public String getAuName() {
    return auName;
  }
  public void setAuName(String auName) {
    this.auName = auName;
  }
  public String getCrawlType() {
    return crawlType;
  }
  public void setCrawlType(String crawlType) {
    this.crawlType = crawlType;
  }
  public Long getStartTime() {
    return startTime;
  }
  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }
  public Long getDuration() {
    return duration;
  }
  public void setDuration(Long duration) {
    this.duration = duration;
  }
  public String getCrawlStatus() {
    return crawlStatus;
  }
  public void setCrawlStatus(String crawlStatus) {
    this.crawlStatus = crawlStatus;
  }
  public Long getBytesFetchedCount() {
    return bytesFetchedCount;
  }
  public void setBytesFetchedCount(Long bytesFetchedCount) {
    this.bytesFetchedCount = bytesFetchedCount;
  }
  public Integer getPagesFetchedCount() {
    return pagesFetchedCount;
  }
  public void setPagesFetchedCount(Integer pagesFetchedCount) {
    this.pagesFetchedCount = pagesFetchedCount;
  }
  public List<String> getPagesFetched() {
    return pagesFetched;
  }
  public void setPagesFetched(List<String> pagesFetched) {
    this.pagesFetched = pagesFetched;
  }
  public Integer getPagesParsedCount() {
    return pagesParsedCount;
  }
  public void setPagesParsedCount(Integer pagesParsedCount) {
    this.pagesParsedCount = pagesParsedCount;
  }
  public List<String> getPagesParsed() {
    return pagesParsed;
  }
  public void setPagesParsed(List<String> pagesParsed) {
    this.pagesParsed = pagesParsed;
  }
  public Integer getPagesPendingCount() {
    return pagesPendingCount;
  }
  public void setPagesPendingCount(Integer pagesPendingCount) {
    this.pagesPendingCount = pagesPendingCount;
  }
  public List<String> getPagesPending() {
    return pagesPending;
  }
  public void setPagesPending(List<String> pagesPending) {
    this.pagesPending = pagesPending;
  }
  public Integer getPagesExcludedCount() {
    return pagesExcludedCount;
  }
  public void setPagesExcludedCount(Integer pagesExcludedCount) {
    this.pagesExcludedCount = pagesExcludedCount;
  }
  public List<String> getPagesExcluded() {
    return pagesExcluded;
  }
  public void setPagesExcluded(List<String> pagesExcluded) {
    this.pagesExcluded = pagesExcluded;
  }
  public Integer getOffSiteUrlsExcludedCount() {
    return offSiteUrlsExcludedCount;
  }
  public void setOffSiteUrlsExcludedCount(Integer offSiteUrlsExcludedCount) {
    this.offSiteUrlsExcludedCount = offSiteUrlsExcludedCount;
  }
  public Integer getPagesNotModifiedCount() {
    return pagesNotModifiedCount;
  }
  public void setPagesNotModifiedCount(Integer pagesNotModifiedCount) {
    this.pagesNotModifiedCount = pagesNotModifiedCount;
  }
  public List<String> getPagesNotModified() {
    return pagesNotModified;
  }
  public void setPagesNotModified(List<String> pagesNotModified) {
    this.pagesNotModified = pagesNotModified;
  }
  public Integer getPagesWithErrorsCount() {
    return pagesWithErrorsCount;
  }
  public void setPagesWithErrorsCount(Integer pagesWithErrorsCount) {
    this.pagesWithErrorsCount = pagesWithErrorsCount;
  }
  public List<UrlErrorWsResult> getPagesWithErrors() {
    return pagesWithErrors;
  }
  public void setPagesWithErrors(List<UrlErrorWsResult> pagesWithErrors) {
    this.pagesWithErrors = pagesWithErrors;
  }
  public Integer getMimeTypeCount() {
    return mimeTypeCount;
  }
  public void setMimeTypeCount(Integer mimeTypeCount) {
    this.mimeTypeCount = mimeTypeCount;
  }
  public Collection<String> getMimeTypes() {
    return mimeTypes;
  }
  public void setMimeTypes(Collection<String> mimeTypes) {
    this.mimeTypes = mimeTypes;
  }
  public List<String> getSources() {
    return sources;
  }
  public void setSources(List<String> sources) {
    this.sources = sources;
  }
  public Collection<String> getStartingUrls() {
    return startingUrls;
  }
  public void setStartingUrls(Collection<String> startingUrls) {
    this.startingUrls = startingUrls;
  }
  public Integer getRefetchDepth() {
    return refetchDepth;
  }
  public void setRefetchDepth(Integer refetchDepth) {
    this.refetchDepth = refetchDepth;
  }
  public Integer getLinkDepth() {
    return linkDepth;
  }
  public void setLinkDepth(Integer linkDepth) {
    this.linkDepth = linkDepth;
  }

  @Override
  public String toString() {
    return "CrawlWsResult [auName=" + auName + ", crawlType=" + crawlType
	+ ", startTime=" + startTime + ", duration=" + duration
	+ ", crawlStatus=" + crawlStatus + ", bytesFetchedCount="
	+ bytesFetchedCount + ", pagesFetchedCount=" + pagesFetchedCount
	+ ", pagesFetched=" + pagesFetched + ", pagesParsedCount="
	+ pagesParsedCount + ", pagesParsed=" + pagesParsed
	+ ", pagesPendingCount=" + pagesPendingCount + ", pagesPending="
	+ pagesPending + ", pagesExcludedCount=" + pagesExcludedCount
	+ ", pagesExcluded=" + pagesExcluded + ", offSiteUrlsExcludedCount="
	+ offSiteUrlsExcludedCount + ", pagesNotModifiedCount="
	+ pagesNotModifiedCount + ", pagesNotModified=" + pagesNotModified
	+ ", pagesWithErrorsCount=" + pagesWithErrorsCount
	+ ", pagesWithErrors=" + pagesWithErrors + ", mimeTypeCount="
	+ mimeTypeCount + ", mimeTypes=" + mimeTypes + ", sources=" + sources
	+ ", startingUrls=" + startingUrls + ", refetchDepth=" + refetchDepth
	+ ", linkDepth=" + linkDepth + "]";
  }
}
