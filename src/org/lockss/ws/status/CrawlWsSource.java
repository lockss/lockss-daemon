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

package org.lockss.ws.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.lockss.crawler.CrawlReq;
import org.lockss.crawler.CrawlerStatus;
import org.lockss.crawler.CrawlerStatus.UrlErrorInfo;
import org.lockss.util.TimeBase;
import org.lockss.ws.entities.CrawlWsResult;
import org.lockss.ws.entities.UrlErrorWsResult;

/**
 * Container for the information that is used as the source for a query related
 * to crawls.
 */
public class CrawlWsSource extends CrawlWsResult {
  private CrawlerStatus crawlerStatus = null;
  private CrawlReq crawlerRequest = null;

  private boolean auIdPopulated = false;
  private boolean auNamePopulated = false;
  private boolean crawlKeyPopulated = false;
  private boolean crawlTypePopulated = false;
  private boolean startTimePopulated = false;
  private boolean durationPopulated = false;
  private boolean crawlStatusPopulated = false;
  private boolean bytesFetchedCountPopulated = false;
  private boolean pagesFetchedCountPopulated = false;
  private boolean pagesFetchedPopulated = false;
  private boolean pagesParsedCountPopulated = false;
  private boolean pagesParsedPopulated = false;
  private boolean pagesPendingCountPopulated = false;
  private boolean pagesPendingPopulated = false;
  private boolean pagesExcludedCountPopulated = false;
  private boolean pagesExcludedPopulated = false;
  private boolean offSiteUrlsExcludedCountPopulated = false;
  private boolean pagesNotModifiedCountPopulated = false;
  private boolean pagesNotModifiedPopulated = false;
  private boolean pagesWithErrorsCountPopulated = false;
  private boolean pagesWithErrorsPopulated = false;
  private boolean mimeTypeCountPopulated = false;
  private boolean mimeTypesPopulated = false;
  private boolean sourcesPopulated = false;
  private boolean startingUrlsPopulated = false;
  private boolean refetchDepthPopulated = false;
  private boolean linkDepthPopulated = false;

  private boolean isStatus;

  public CrawlWsSource(CrawlReq crawlerRequest) {
    this.crawlerRequest = crawlerRequest;
    isStatus = false;
  }

  public CrawlWsSource(CrawlerStatus crawlerStatus) {
    this.crawlerStatus = crawlerStatus;
    isStatus = true;
  }

  @Override
  public String getAuId() {
    if (!auIdPopulated) {
      if (isStatus) {
	setAuId(crawlerStatus.getAuId());
      } else {
	setAuId(crawlerRequest.getAu().getAuId());
      }

      auIdPopulated = true;
    }

    return super.getAuId();
  }

  @Override
  public String getAuName() {
    if (!auNamePopulated) {
      if (isStatus) {
	setAuName(crawlerStatus.getAuName());
      } else {
	setAuName(crawlerRequest.getAu().getName());
      }

      auNamePopulated = true;
    }

    return super.getAuName();
  }

  @Override
  public String getCrawlKey() {
    if (!crawlKeyPopulated) {
      if (isStatus) {
	setCrawlKey(crawlerStatus.getKey());
      }

      crawlKeyPopulated = true;
    }

    return super.getCrawlKey();
  }

  @Override
  public String getCrawlType() {
    if (!crawlTypePopulated) {
      if (isStatus) {
	setCrawlType(crawlerStatus.getType());
      } else {
	setCrawlType("New Content");
      }

      crawlTypePopulated = true;
    }

    return super.getCrawlType();
  }

  @Override
  public Long getStartTime() {
    if (!startTimePopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setStartTime(Long.valueOf(crawlerStatus.getStartTime()));
	}
      }

      startTimePopulated = true;
    }

    return super.getStartTime();
  }

  @Override
  public Long getDuration() {
    if (!durationPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  if (crawlerStatus.getEndTime() > 0) {
	    setDuration(Long
		.valueOf(crawlerStatus.getEndTime() - getStartTime()));
	  } else {
	    setDuration(Long.valueOf(TimeBase.nowMs() - getStartTime()));
	  }
	}
      }

      durationPopulated = true;
    }

    return super.getDuration();
  }

  @Override
  public String getCrawlStatus() {
    if (!crawlStatusPopulated) {
      if (isStatus) {
	setCrawlStatus(crawlerStatus.getCrawlStatusMsg());
      } else {
	setCrawlStatus("Pending");
      }

      crawlStatusPopulated = true;
    }

    return super.getCrawlStatus();
  }

  @Override
  public Long getBytesFetchedCount() {
    if (!bytesFetchedCountPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setBytesFetchedCount(Long
	      .valueOf(crawlerStatus.getContentBytesFetched()));
	}
      }

      bytesFetchedCountPopulated = true;
    }

    return super.getBytesFetchedCount();
  }

  @Override
  public Integer getPagesFetchedCount() {
    if (!pagesFetchedCountPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setPagesFetchedCount(Integer
	      .valueOf(crawlerStatus.getFetchedCtr().getCount()));
	}
      }

      pagesFetchedCountPopulated = true;
    }

    return super.getPagesFetchedCount();
  }

  @Override
  public List<String> getPagesFetched() {
    if (!pagesFetchedPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setPagesFetched((List<String>)crawlerStatus.getUrlsFetched());
	}
      }

      pagesFetchedPopulated = true;
    }

    return super.getPagesFetched();
  }

  @Override
  public Integer getPagesParsedCount() {
    if (!pagesParsedCountPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setPagesParsedCount(Integer
	      .valueOf(crawlerStatus.getParsedCtr().getCount()));
	}
      }

      pagesParsedCountPopulated = true;
    }

    return super.getPagesParsedCount();
  }

  @Override
  public List<String> getPagesParsed() {
    if (!pagesParsedPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setPagesParsed((List<String>)crawlerStatus.getUrlsParsed());
	}
      }

      pagesParsedPopulated = true;
    }

    return super.getPagesParsed();
  }

  @Override
  public Integer getPagesPendingCount() {
    if (!pagesPendingCountPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setPagesPendingCount(Integer
	      .valueOf(crawlerStatus.getPendingCtr().getCount()));
	}
      }

      pagesPendingCountPopulated = true;
    }

    return super.getPagesPendingCount();
  }

  @Override
  public List<String> getPagesPending() {
    if (!pagesPendingPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setPagesPending((List<String>)crawlerStatus.getUrlsPending());
	}
      }

      pagesPendingPopulated = true;
    }

    return super.getPagesPending();
  }

  @Override
  public Integer getPagesExcludedCount() {
    if (!pagesExcludedCountPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setPagesExcludedCount(Integer
	      .valueOf(crawlerStatus.getExcludedCtr().getCount()));
	}
      }

      pagesExcludedCountPopulated = true;
    }

    return super.getPagesExcludedCount();
  }

  @Override
  public List<String> getPagesExcluded() {
    if (!pagesExcludedPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setPagesExcluded((List<String>)crawlerStatus.getUrlsExcluded());
	}
      }

      pagesExcludedPopulated = true;
    }

    return super.getPagesExcluded();
  }

  @Override
  public Integer getOffSiteUrlsExcludedCount() {
    if (!offSiteUrlsExcludedCountPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setPagesExcludedCount(Integer
	      .valueOf(crawlerStatus.getNumExcludedExcludes()));
	}
      }

      offSiteUrlsExcludedCountPopulated = true;
    }

    return super.getOffSiteUrlsExcludedCount();
  }

  @Override
  public Integer getPagesNotModifiedCount() {
    if (!pagesNotModifiedCountPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setPagesNotModifiedCount(Integer
	      .valueOf(crawlerStatus.getNotModifiedCtr().getCount()));
	}
      }

      pagesNotModifiedCountPopulated = true;
    }

    return super.getPagesNotModifiedCount();
  }

  @Override
  public List<String> getPagesNotModified() {
    if (!pagesNotModifiedPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setPagesNotModified((List<String>)crawlerStatus.getUrlsNotModified());
	}
      }

      pagesNotModifiedPopulated = true;
    }

    return super.getPagesNotModified();
  }

  @Override
  public Integer getPagesWithErrorsCount() {
    if (!pagesWithErrorsCountPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setPagesWithErrorsCount(Integer
	      .valueOf(crawlerStatus.getErrorCtr().getCount()));
	}
      }

      pagesWithErrorsCountPopulated = true;
    }

    return super.getPagesWithErrorsCount();
  }

  @Override
  public List<UrlErrorWsResult> getPagesWithErrors() {
    if (!pagesWithErrorsPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  Map<String, UrlErrorInfo> errorMap = crawlerStatus.getUrlsErrorMap();

	  List<UrlErrorWsResult> errorPages =
	      new ArrayList<UrlErrorWsResult>(errorMap.keySet().size());

	  for (Map.Entry<String, UrlErrorInfo> entry : errorMap.entrySet()) {
	    UrlErrorInfo uei = entry.getValue();

	    UrlErrorWsResult result = new UrlErrorWsResult();
	    result.setUrl(entry.getKey());
	    result.setSeverity(uei.getSeverity().toString());
	    result.setMessage(uei.getMessage());

	    errorPages.add(result);
	  }

	  setPagesWithErrors(errorPages);
	}
      }

      pagesWithErrorsPopulated = true;
    }

    return super.getPagesWithErrors();
  }

  @Override
  public Integer getMimeTypeCount() {
    if (!mimeTypeCountPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setMimeTypeCount(Integer.valueOf(crawlerStatus.getNumOfMimeTypes()));
	}
      }

      mimeTypeCountPopulated = true;
    }

    return super.getMimeTypeCount();
  }

  @Override
  public Collection<String> getMimeTypes() {
    if (!mimeTypesPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setMimeTypes(crawlerStatus.getMimeTypes());
	}
      }

      mimeTypesPopulated = true;
    }

    return super.getMimeTypes();
  }

  @Override
  public List<String> getSources() {
    if (!sourcesPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setSources((List<String>)crawlerStatus.getSources());
	}
      }

      sourcesPopulated = true;
    }

    return super.getSources();
  }

  @Override
  public Collection<String> getStartingUrls() {
    if (!startingUrlsPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  setStartingUrls(crawlerStatus.getStartUrls());
	}
      }

      startingUrlsPopulated = true;
    }

    return super.getStartingUrls();
  }

  @Override
  public Integer getRefetchDepth() {
    if (!refetchDepthPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  if (crawlerStatus.getRefetchDepth() >= 0) {
	    setRefetchDepth(Integer.valueOf(crawlerStatus.getRefetchDepth()));
	  }
	}
      }

      refetchDepthPopulated = true;
    }

    return super.getRefetchDepth();
  }

  @Override
  public Integer getLinkDepth() {
    if (!linkDepthPopulated) {
      if (isStatus) {
	if (crawlerStatus.getStartTime() > 0) {
	  if (crawlerStatus.getDepth() >= 0) {
	    setLinkDepth(Integer.valueOf(crawlerStatus.getDepth()));
	  }
	}
      }

      linkDepthPopulated = true;
    }

    return super.getLinkDepth();
  }
}
