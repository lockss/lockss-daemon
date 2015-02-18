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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lockss.app.LockssDaemon;
import org.lockss.crawler.CrawlManager;
import org.lockss.crawler.CrawlManager.StatusSource;
import org.lockss.crawler.CrawlReq;
import org.lockss.crawler.CrawlerStatus;
import org.lockss.util.Logger;
import org.lockss.ws.entities.CrawlWsResult;

/**
 * Helper of the DaemonStatus web service implementation of crawl queries.
 */
public class CrawlHelper {
  /**
   * The fully-qualified name of the class of the objects used as source in a
   * query.
   */
  static String SOURCE_FQCN = CrawlWsSource.class.getCanonicalName();

  /**
   * The fully-qualified name of the class of the objects returned by the query.
   */
  static String RESULT_FQCN = CrawlWsResult.class.getCanonicalName();

  //
  // Property names used in peer queries.
  //
  static String AU_ID = "auId";
  static String AU_NAME = "auName";
  static String CRAWL_KEY = "crawlKey";
  static String CRAWL_TYPE = "crawlType";
  static String START_TIME = "startTime";
  static String DURATION = "duration";
  static String CRAWL_STATUS = "crawlStatus";
  static String BYTES_FETCHED_COUNT = "bytesFetchedCount";
  static String PAGES_FETCHED_COUNT = "pagesFetchedCount";
  static String PAGES_FETCHED = "pagesFetched";
  static String PAGES_PARSED_COUNT = "pagesParsedCount";
  static String PAGES_PARSED = "pagesParsed";
  static String PAGES_PENDING_COUNT = "pagesPendingCount";
  static String PAGES_PENDING = "pagesPending";
  static String PAGES_EXCLUDED_COUNT = "pagesExcludedCount";
  static String PAGES_EXCLUDED = "pagesExcluded";
  static String OFF_SITE_URLS_EXCLUDED_COUNT = "offSiteUrlsExcludedCount";
  static String PAGES_NOT_MODIFIED_COUNT = "pagesNotModifiedCount";
  static String PAGES_NOT_MODIFIED = "pagesNotModified";
  static String PAGES_WITH_ERRORS_COUNT = "pagesWithErrorsCount";
  static String PAGES_WITH_ERRORS = "pagesWithErrors";
  static String MIME_TYPE_COUNT = "mimeTypeCount";
  static String MIME_TYPES = "mimeTypes";
  static String SOURCES = "sources";
  static String STARTING_URLS = "startingUrls";
  static String REFETCH_DEPTH = "refetchDepth";
  static String LINK_DEPTH = "linkDepth";

  /**
   * All the property names used in peer queries.
   */
  @SuppressWarnings("serial")
  static final Set<String> PROPERTY_NAMES = new HashSet<String>() {
    {
      add(AU_ID);
      add(AU_NAME);
      add(CRAWL_KEY);
      add(CRAWL_TYPE);
      add(START_TIME);
      add(DURATION);
      add(CRAWL_STATUS);
      add(BYTES_FETCHED_COUNT);
      add(PAGES_FETCHED_COUNT);
      add(PAGES_FETCHED);
      add(PAGES_PARSED_COUNT);
      add(PAGES_PARSED);
      add(PAGES_PENDING_COUNT);
      add(PAGES_PENDING);
      add(PAGES_EXCLUDED_COUNT);
      add(PAGES_EXCLUDED);
      add(OFF_SITE_URLS_EXCLUDED_COUNT);
      add(PAGES_NOT_MODIFIED_COUNT);
      add(PAGES_NOT_MODIFIED);
      add(PAGES_WITH_ERRORS_COUNT);
      add(PAGES_WITH_ERRORS);
      add(MIME_TYPE_COUNT);
      add(MIME_TYPES);
      add(SOURCES);
      add(STARTING_URLS);
      add(REFETCH_DEPTH);
      add(LINK_DEPTH);
    }
  };

  private static Logger log = Logger.getLogger(CrawlHelper.class);

  /**
   * Provides the universe of peer-related objects used as the source for a
   * query.
   * 
   * @return a List<CrawlWsProxy> with the universe.
   */
  List<CrawlWsSource> createUniverse() {
    final String DEBUG_HEADER = "createUniverse(): ";

    // Initialize the universe.
    List<CrawlWsSource> universe = new ArrayList<CrawlWsSource>();

    // Get the crawling status source.
    StatusSource statusSource =
	((CrawlManager)LockssDaemon.getManager(LockssDaemon.CRAWL_MANAGER))
	.getStatusSource();

    // Get all the crawls that are not pending.
    List<CrawlerStatus> nonPendingCrawls =
	statusSource.getStatus().getCrawlerStatusList();

    // Get all the crawls that are pending.
    Collection<CrawlReq> pendingCrawls = statusSource.getPendingQueue();

    // Add to the universe all the non-pending crawls.
    if (nonPendingCrawls != null) {
      for (CrawlerStatus crawlerStatus : nonPendingCrawls) {
	universe.add(new CrawlWsSource(crawlerStatus));
      }
    }

    // Add to the universe all the pending crawls.
    if (pendingCrawls != null) {
      for (CrawlReq crawlerRequest : pendingCrawls) {
	universe.add(new CrawlWsSource(crawlerRequest));
      }
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "universe.size() = " + universe.size());
    return universe;
  }

  /**
   * Provides a printable copy of a collection of peer-related query results.
   * 
   * @param results
   *          A Collection<CrawlWsResult> with the query results.
   * @return a String with the requested printable copy.
   */
  String nonDefaultToString(Collection<CrawlWsResult> results) {
    StringBuilder builder = new StringBuilder("[");
    boolean isFirst = true;

    // Loop through through all the results in the collection.
    for (CrawlWsResult result : results) {
      // Handle the first result differently.
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append(nonDefaultToString(result));
    }

    // Add this result to the printable copy.
    return builder.append("]").toString();
  }

  /**
   * Provides a printable copy of a peer-related query result.
   * 
   * @param result
   *          A CrawlWsResult with the query result.
   * @return a String with the requested printable copy.
   */
  private String nonDefaultToString(CrawlWsResult result) {
    StringBuilder builder = new StringBuilder("CrawlWsResult [");
    boolean isFirst = true;

    if (result.getAuId() != null) {
      builder.append("auId=").append(result.getAuId());
      isFirst = false;
    }

    if (result.getAuName() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("auName=").append(result.getAuName());
    }

    if (result.getCrawlKey() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("crawlKey=").append(result.getCrawlKey());
    }

    if (result.getCrawlType() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("crawlType=").append(result.getCrawlType());
    }

    if (result.getStartTime() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("startTime=").append(result.getStartTime());
    }

    if (result.getDuration() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("duration=").append(result.getDuration());
    }

    if (result.getCrawlStatus() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("crawlStatus=").append(result.getCrawlStatus());
    }

    if (result.getBytesFetchedCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("bytesFetchedCount=")
      .append(result.getBytesFetchedCount());
    }

    if (result.getPagesFetchedCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pagesFetchedCount=")
      .append(result.getPagesFetchedCount());
    }

    if (result.getPagesParsedCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pagesParsedCount=").append(result.getPagesParsedCount());
    }

    if (result.getPagesParsed() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pagesParsed=").append(result.getPagesParsed());
    }

    if (result.getPagesPendingCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pagesPendingCount=")
      .append(result.getPagesPendingCount());
    }

    if (result.getPagesPending() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pagesPending=").append(result.getPagesPending());
    }

    if (result.getPagesExcludedCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pagesExcludedCount=")
      .append(result.getPagesExcludedCount());
    }

    if (result.getPagesExcluded() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pagesExcluded=").append(result.getPagesExcluded());
    }

    if (result.getOffSiteUrlsExcludedCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("offSiteUrlsExcludedCount=")
      .append(result.getOffSiteUrlsExcludedCount());
    }

    if (result.getPagesNotModifiedCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pagesNotModifiedCount=")
      .append(result.getPagesNotModifiedCount());
    }

    if (result.getPagesNotModified() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pagesNotModified=").append(result.getPagesNotModified());
    }

    if (result.getPagesWithErrorsCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pagesWithErrorsCount=")
      .append(result.getPagesWithErrorsCount());
    }

    if (result.getPagesWithErrors() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pagesWithErrors=").append(result.getPagesWithErrors());
    }

    if (result.getMimeTypeCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("mimeTypeCount=").append(result.getMimeTypeCount());
    }

    if (result.getMimeTypes() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("mimeTypes=").append(result.getMimeTypes());
    }

    if (result.getSources() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("sources=").append(result.getSources());
    }

    if (result.getStartingUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("startingUrls=").append(result.getStartingUrls());
    }

    if (result.getRefetchDepth() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("refetchDepth=").append(result.getRefetchDepth());
    }

    if (result.getLinkDepth() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("linkDepth=").append(result.getLinkDepth());
    }

    return builder.append("]").toString();
  }

}
