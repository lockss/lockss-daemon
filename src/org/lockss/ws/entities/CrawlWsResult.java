/*
 * $Id$
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
package org.lockss.ws.entities;

import java.util.Collection;
import java.util.List;

/**
 * Container for the information related to a crawl that is the result of a
 * query.
 */
public class CrawlWsResult {
  private String auId;
  private String auName;
  private String crawlKey;
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
  public String getAuName() {
    return auName;
  }
  public void setAuName(String auName) {
    this.auName = auName;
  }

  /**
   * Provides the crawl key.
   * 
   * @return a String with the crawl key.
   */
public String getCrawlKey() {
    return crawlKey;
  }
  public void setCrawlKey(String crawlKey) {
    this.crawlKey = crawlKey;
  }

  /**
   * Provides the type of crawl.
   * 
   * @return a String with the crawl type.
   */
public String getCrawlType() {
    return crawlType;
  }
  public void setCrawlType(String crawlType) {
    this.crawlType = crawlType;
  }

  /**
   * Provides the crawl start timestamp.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getStartTime() {
    return startTime;
  }
  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  /**
   * Provides the crawl duration.
   * 
   * @return a Long with the duration in milliseconds.
   */
  public Long getDuration() {
    return duration;
  }
  public void setDuration(Long duration) {
    this.duration = duration;
  }

  /**
   * Provides the crawl status.
   * 
   * @return a String with the crawl status.
   */
  public String getCrawlStatus() {
    return crawlStatus;
  }
  public void setCrawlStatus(String crawlStatus) {
    this.crawlStatus = crawlStatus;
  }

  /**
   * Provides the count of bytes fetched during the crawl.
   * 
   * @return a Long with the byte count.
   */
  public Long getBytesFetchedCount() {
    return bytesFetchedCount;
  }
  public void setBytesFetchedCount(Long bytesFetchedCount) {
    this.bytesFetchedCount = bytesFetchedCount;
  }

  /**
   * Provides the count of pages fetched during the crawl.
   * 
   * @return an Integer with the page count.
   */
  public Integer getPagesFetchedCount() {
    return pagesFetchedCount;
  }
  public void setPagesFetchedCount(Integer pagesFetchedCount) {
    this.pagesFetchedCount = pagesFetchedCount;
  }

  /**
   * Provides the URLs of the pages fetched during the crawl.
   * 
   * @return a List<String> with the URLs.
   */
  public List<String> getPagesFetched() {
    return pagesFetched;
  }
  public void setPagesFetched(List<String> pagesFetched) {
    this.pagesFetched = pagesFetched;
  }

  /**
   * Provides the count of pages parsed during the crawl.
   * 
   * @return an Integer with the page count.
   */
  public Integer getPagesParsedCount() {
    return pagesParsedCount;
  }
  public void setPagesParsedCount(Integer pagesParsedCount) {
    this.pagesParsedCount = pagesParsedCount;
  }

  /**
   * Provides the URLs of the pages parsed during the crawl.
   * 
   * @return a List<String> with the URLs.
   */
  public List<String> getPagesParsed() {
    return pagesParsed;
  }
  public void setPagesParsed(List<String> pagesParsed) {
    this.pagesParsed = pagesParsed;
  }

  /**
   * Provides the count of pages pending to crawl.
   * 
   * @return an Integer with the page count.
   */
  public Integer getPagesPendingCount() {
    return pagesPendingCount;
  }
  public void setPagesPendingCount(Integer pagesPendingCount) {
    this.pagesPendingCount = pagesPendingCount;
  }

  /**
   * Provides the URLs of the pages pending to crawl.
   * 
   * @return a List<String> with the URLs.
   */
  public List<String> getPagesPending() {
    return pagesPending;
  }
  public void setPagesPending(List<String> pagesPending) {
    this.pagesPending = pagesPending;
  }

  /**
   * Provides the count of pages excluded from the crawl.
   * 
   * @return an Integer with the page count.
   */
  public Integer getPagesExcludedCount() {
    return pagesExcludedCount;
  }
  public void setPagesExcludedCount(Integer pagesExcludedCount) {
    this.pagesExcludedCount = pagesExcludedCount;
  }

  /**
   * Provides the URLs of the pages excluded from the crawl.
   * 
   * @return a List<String> with the URLs.
   */
  public List<String> getPagesExcluded() {
    return pagesExcluded;
  }
  public void setPagesExcluded(List<String> pagesExcluded) {
    this.pagesExcluded = pagesExcluded;
  }

  /**
   * Provides the count of off-site pages excluded from the crawl.
   * 
   * @return an Integer with the page count.
   */
  public Integer getOffSiteUrlsExcludedCount() {
    return offSiteUrlsExcludedCount;
  }
  public void setOffSiteUrlsExcludedCount(Integer offSiteUrlsExcludedCount) {
    this.offSiteUrlsExcludedCount = offSiteUrlsExcludedCount;
  }

  /**
   * Provides the count of not modified pages found during the crawl.
   * 
   * @return an Integer with the page count.
   */
  public Integer getPagesNotModifiedCount() {
    return pagesNotModifiedCount;
  }
  public void setPagesNotModifiedCount(Integer pagesNotModifiedCount) {
    this.pagesNotModifiedCount = pagesNotModifiedCount;
  }

  /**
   * Provides the URLs of the not modified pages found during the crawl.
   * 
   * @return a List<String> with the URLs.
   */
  public List<String> getPagesNotModified() {
    return pagesNotModified;
  }
  public void setPagesNotModified(List<String> pagesNotModified) {
    this.pagesNotModified = pagesNotModified;
  }

  /**
   * Provides the count of pages with errors found during the crawl.
   * 
   * @return an Integer with the page count.
   */
  public Integer getPagesWithErrorsCount() {
    return pagesWithErrorsCount;
  }
  public void setPagesWithErrorsCount(Integer pagesWithErrorsCount) {
    this.pagesWithErrorsCount = pagesWithErrorsCount;
  }

  /**
   * Provides data about the pages with errors found during the crawl.
   * 
   * @return a List<UrlErrorWsResult> with the data.
   */
  public List<UrlErrorWsResult> getPagesWithErrors() {
    return pagesWithErrors;
  }
  public void setPagesWithErrors(List<UrlErrorWsResult> pagesWithErrors) {
    this.pagesWithErrors = pagesWithErrors;
  }

  /**
   * Provides the count of MIME types found during the crawl.
   * 
   * @return an Integer with the count.
   */
  public Integer getMimeTypeCount() {
    return mimeTypeCount;
  }
  public void setMimeTypeCount(Integer mimeTypeCount) {
    this.mimeTypeCount = mimeTypeCount;
  }

  /**
   * Provides the MIME types found during the crawl.
   * 
   * @return a Collection<String> with the MIME types.
   */
  public Collection<String> getMimeTypes() {
    return mimeTypes;
  }
  public void setMimeTypes(Collection<String> mimeTypes) {
    this.mimeTypes = mimeTypes;
  }

  /**
   * Provides the sources of the crawl.
   * 
   * @return a List<String> with the sources.
   */
  public List<String> getSources() {
    return sources;
  }
  public void setSources(List<String> sources) {
    this.sources = sources;
  }

  /**
   * Provides the starting URLs of the crawl.
   * 
   * @return a Collection<String> with the URLs.
   */
  public Collection<String> getStartingUrls() {
    return startingUrls;
  }
  public void setStartingUrls(Collection<String> startingUrls) {
    this.startingUrls = startingUrls;
  }

  /**
   * Provides the crawl re-fetch depth.
   * 
   * @return an Integer with the re-fetch depth.
   */
  public Integer getRefetchDepth() {
    return refetchDepth;
  }
  public void setRefetchDepth(Integer refetchDepth) {
    this.refetchDepth = refetchDepth;
  }

  /**
   * Provides the crawl link depth.
   * 
   * @return an Integer with the link depth.
   */
  public Integer getLinkDepth() {
    return linkDepth;
  }
  public void setLinkDepth(Integer linkDepth) {
    this.linkDepth = linkDepth;
  }

  @Override
  public String toString() {
    return "CrawlWsResult [auId=" + auId + ", auName=" + auName + ", crawlKey="
	+ crawlKey + ", crawlType=" + crawlType + ", startTime=" + startTime
	+ ", duration=" + duration + ", crawlStatus=" + crawlStatus
	+ ", bytesFetchedCount=" + bytesFetchedCount + ", pagesFetchedCount="
	+ pagesFetchedCount + ", pagesFetched=" + pagesFetched
	+ ", pagesParsedCount=" + pagesParsedCount + ", pagesParsed="
	+ pagesParsed + ", pagesPendingCount=" + pagesPendingCount
	+ ", pagesPending=" + pagesPending + ", pagesExcludedCount="
	+ pagesExcludedCount + ", pagesExcluded=" + pagesExcluded
	+ ", offSiteUrlsExcludedCount="	+ offSiteUrlsExcludedCount
	+ ", pagesNotModifiedCount=" + pagesNotModifiedCount
	+ ", pagesNotModified=" + pagesNotModified + ", pagesWithErrorsCount="
	+ pagesWithErrorsCount + ", pagesWithErrors=" + pagesWithErrors
	+ ", mimeTypeCount=" + mimeTypeCount + ", mimeTypes=" + mimeTypes
	+ ", sources=" + sources + ", startingUrls=" + startingUrls
	+ ", refetchDepth=" + refetchDepth + ", linkDepth=" + linkDepth + "]";
  }
}
