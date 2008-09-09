/*
 * $Id: CrawlManagerStatusAccessor.java,v 1.20 2008-09-09 07:52:07 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;

import java.util.*;

import org.lockss.daemon.status.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.state.ArchivalUnitStatus;
import org.lockss.util.*;

public class CrawlManagerStatusAccessor implements StatusAccessor {

  private static final String AU_COL_NAME = "au";
  private static final String CRAWL_TYPE = "crawl_type";
  private static final String START_TIME_COL_NAME = "start";
//   private static final String END_TIME_COL_NAME = "end";
  private static final String DURATION_COL_NAME = "dur";
  private static final String CONTENT_BYTES_FETCHED = "content_bytes_fetched";
  private static final String NUM_URLS_PARSED = "num_urls_parsed";
  private static final String NUM_URLS_FETCHED = "num_urls_fetched";
  private static final String NUM_URLS_EXCLUDED = "num_urls_excluded";
  private static final String NUM_URLS_PENDING = "num_urls_pending";
  private static final String NUM_URLS_WITH_ERRORS = "num_urls_with_errors";
  private static final String NUM_URLS_NOT_MODIFIED = "num_urls_not_modified";
  private static final String CRAWL_URLS_STATUS_ACCESSOR =
                                CrawlManagerImpl.CRAWL_URLS_STATUS_TABLE;
  private static final String SINGLE_CRAWL_STATUS_ACCESSOR =
                                CrawlManagerImpl.SINGLE_CRAWL_STATUS_TABLE;
  private static final String NUM_OF_MIME_TYPES = "num_of_mime_types";

//   private static final String START_URLS = "start_urls";
  private static final String CRAWL_STATUS = "crawl_status";
//   private static final String SOURCES = "sources";
  // Sort key, not a visible column
  private static final String SORT_KEY = "sort";

  private static int SORT_BASE_ACTIVE = 0;
  private static int SORT_BASE_WAITING = 1000000;
  private static int SORT_BASE_DONE = 2000000;

  private List sortRules = null;

  private List<ColumnDescriptor> colDescs =
    ListUtil.fromArray(new ColumnDescriptor[] {
      new ColumnDescriptor(AU_COL_NAME, "Journal Volume",
			   ColumnDescriptor.TYPE_STRING)
      .setComparator(CatalogueOrderComparator.SINGLETON),
      new ColumnDescriptor(CRAWL_TYPE, "Crawl Type",
			   ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor(START_TIME_COL_NAME, "Start Time",
			   ColumnDescriptor.TYPE_DATE),
//       new ColumnDescriptor(END_TIME_COL_NAME, "End Time",
// 			   ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor(DURATION_COL_NAME, "Duration",
			   ColumnDescriptor.TYPE_TIME_INTERVAL),
      new ColumnDescriptor(CRAWL_STATUS, "Status",
			   ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor(CONTENT_BYTES_FETCHED, "Bytes Fetched",
			   ColumnDescriptor.TYPE_INT,
			   "Number of content bytes collected from server " +
			   "during crawl.  Does not include HTTP headers " +
			   "or other network overhead."),
      new ColumnDescriptor(NUM_URLS_FETCHED, "Pages Fetched",
			   ColumnDescriptor.TYPE_INT,
			   "Number of pages successfully fetched from server"),
      new ColumnDescriptor(NUM_URLS_PARSED, "Pages Parsed",
			   ColumnDescriptor.TYPE_INT,
			   "Number of (html, etc.) pages scanned for URLs"),
      new ColumnDescriptor(NUM_URLS_PENDING, "Pages Pending",
                           ColumnDescriptor.TYPE_INT,
                           "Number of pages waiting to be fetched"),
      new ColumnDescriptor(NUM_URLS_EXCLUDED, "Pages Excluded",
			   ColumnDescriptor.TYPE_INT,
			   "Number of pages that didn't match the crawl rules"),
      new ColumnDescriptor(NUM_URLS_NOT_MODIFIED, "Not Modified",
			   ColumnDescriptor.TYPE_INT,
			   "Number of pages for which we already had current content"),
      new ColumnDescriptor(NUM_URLS_WITH_ERRORS, "Errors",
			   ColumnDescriptor.TYPE_INT,
			   "Number of pages that could not be fetched"),
      new ColumnDescriptor(NUM_OF_MIME_TYPES, "Mime Types",
                           ColumnDescriptor.TYPE_INT,
                           "Number of different content types"),
//       new ColumnDescriptor(START_URLS, "Starting Url(s)",
// 			   ColumnDescriptor.TYPE_STRING),
//       new ColumnDescriptor(SOURCES, "Source(s)",
// 			   ColumnDescriptor.TYPE_STRING),
    });


  private CrawlManager.StatusSource statusSource;
  private PluginManager pluginMgr;

  public CrawlManagerStatusAccessor(CrawlManager.StatusSource statusSource) {
    this.statusSource = statusSource;
    this.pluginMgr = statusSource.getDaemon().getPluginManager();
  }

  protected CrawlManagerStatusAccessor() {
    //for testing
  }

  public String getDisplayName() {
    return "Crawl Status";
  }


  public boolean requiresKey() {
    return false;
  }

  /**
   * Fill in the crawl status table.
   * @param table StatusTable to populate
   * @throws IllegalArgumentException if called with a null StatusTable
   */
  public void populateTable(StatusTable table) {
    if (table == null) {
      throw new IllegalArgumentException("Called with null StatusTable");
    }
    CrawlManagerStatus cms = statusSource.getStatus();
    String key = table.getKey();
    Counts ct = new Counts();
    boolean includeInternalAus =
      table.getOptions().get(StatusTable.OPTION_DEBUG_USER);
    table.setRows(getRows(cms, key, includeInternalAus, ct));
    table.setDefaultSortRules(makeSortRules());
    table.setColumnDescriptors(getColDescs(cms, ct));
    table.setSummaryInfo(getSummaryInfo(cms, ct));
  }

  static final String ODC_PENDING_FOOTNOTE =
    "Only the next few pending crawls are shown, " +
    "and only in the approximate order they'll run.";

  private List getColDescs(CrawlManagerStatus cms, Counts ct) {
    boolean includePendingFoot = cms.isOdc() && ct.waiting > 0;
    List res = new ArrayList(colDescs.size());
    for (ColumnDescriptor desc : colDescs) {
      if (includePendingFoot && desc.getColumnName() == CRAWL_STATUS) {
	res.add(new ColumnDescriptor(desc.getColumnName(),
				     desc.getTitle(),
				     desc.getType(),
				     ODC_PENDING_FOOTNOTE));
      } else {
	res.add(desc);
      }
    }
    return res;
  }

  private List getRows(CrawlManagerStatus cms, String key,
		       boolean includeInternalAus, Counts ct) {
    List allCrawls = cms.getCrawlerStatusList();
    List rows = new ArrayList();
    int rowNum = 0;
    if (allCrawls != null) {
      for (Iterator it = allCrawls.iterator(); it.hasNext();) {
	CrawlerStatus crawlStat = (CrawlerStatus)it.next();
	if (!includeInternalAus &&
	    pluginMgr.isInternalAu(crawlStat.getAu())) {
	  continue;
	} else if (key != null && !key.equals(crawlStat.getAu().getAuId())) {
	  continue;
	}
	rows.add(makeRow(crawlStat, ct, rowNum++));
      }
    }
    Collection<CrawlReq> pendingQ = statusSource.getPendingQueue();
    if (pendingQ != null) {
      for (CrawlReq req : pendingQ) {
	if (!includeInternalAus && pluginMgr.isInternalAu(req.au)) {
	  continue;
	} else if (key != null && !key.equals(req.au.getAuId())) {
	  continue;
	}
	ct.waiting++;
	rows.add(makeRow(req, ct, rowNum++));
      }
    }
    return rows;
  }

  private Map makeRow(CrawlReq req, Counts ct, int rowNum) {
    Map row = new HashMap();
    ArchivalUnit au = req.au;
    row.put(AU_COL_NAME,
	    new StatusTable.Reference(au.getName(),
				      ArchivalUnitStatus.AU_STATUS_TABLE_NAME,
				      au.getAuId()));
    row.put(CRAWL_TYPE, "New Content");
    row.put(CRAWL_STATUS, "Pending");
    row.put(SORT_KEY, new Integer(rowNum + SORT_BASE_WAITING));
    ct.waiting++;
    return row;
  }

  private Map makeRow(CrawlerStatus status, Counts ct, int rowNum) {
    String key = status.getKey();

    Map row = new HashMap();
    Object statusColRef = null;
    ArchivalUnit au = status.getAu();
    row.put(AU_COL_NAME,
	    new StatusTable.Reference(au.getName(),
				      ArchivalUnitStatus.AU_STATUS_TABLE_NAME,
				      au.getAuId()));
    row.put(CRAWL_TYPE, status.getType());
    if (status.getStartTime() > 0) {
      row.put(START_TIME_COL_NAME, new Long(status.getStartTime()));
      row.put(CONTENT_BYTES_FETCHED,
	      new Long(status.getContentBytesFetched()));
      row.put(NUM_URLS_FETCHED,
	      makeRefIfColl(status.getFetchedCtr(), key, "fetched"));
      row.put(NUM_URLS_WITH_ERRORS,
	      makeRefIfColl(status.getErrorCtr(), key, "error"));
      row.put(NUM_URLS_NOT_MODIFIED,
	      makeRefIfColl(status.getNotModifiedCtr(), key, "not-modified"));
      row.put(NUM_URLS_PARSED,
	      makeRefIfColl(status.getParsedCtr(), key, "parsed"));
      row.put(NUM_URLS_PENDING,
              makeRefIfColl(status.getPendingCtr(), key, "pending"));
      row.put(NUM_URLS_EXCLUDED,
	      makeRefIfColl(status.getExcludedCtr(), key, "excluded"));
      row.put(NUM_OF_MIME_TYPES,
	      makeRef(status.getNumOfMimeTypes(),
		      SINGLE_CRAWL_STATUS_ACCESSOR, key));
      if (status.getEndTime() > 0) {
	row.put(DURATION_COL_NAME, new Long(status.getEndTime() -
					    status.getStartTime()));
	row.put(SORT_KEY, new Integer(SORT_BASE_DONE));
	if (status.getErrorCtr().getCount() > 0) {
	  switch (status.getCrawlStatus()) {
	  case Crawler.STATUS_ERROR:
	  case Crawler.STATUS_ABORTED:
	  case Crawler.STATUS_FETCH_ERROR:
	  case Crawler.STATUS_EXTRACTOR_ERROR:
	  case Crawler.STATUS_NO_PUB_PERMISSION:
	  case Crawler.STATUS_PLUGIN_ERROR:
	  case Crawler.STATUS_REPO_ERR:
	    statusColRef = makeRef(status.getCrawlStatusMsg(),
				   CRAWL_URLS_STATUS_ACCESSOR, key + ".error");
	  }
	}
      } else {
	row.put(DURATION_COL_NAME, new Long(TimeBase.nowMs() -
					    status.getStartTime()));
	row.put(SORT_KEY, new Integer(SORT_BASE_ACTIVE));
	ct.active++;
      }
    } else {
      ct.waiting++;
      row.put(SORT_KEY, new Integer(rowNum + SORT_BASE_WAITING));
    }

//     row.put(START_URLS,
// 	    (StringUtil.separatedString(status.getStartUrls(), "\n")));
//     row.put(SOURCES,
// 	    (StringUtil.separatedString(status.getSources(), "\n")));
    if (statusColRef == null) {
      statusColRef = makeRef(status.getCrawlStatusMsg(),
			     SINGLE_CRAWL_STATUS_ACCESSOR, key);
    }
    row.put(CRAWL_STATUS, statusColRef);
    return row;
  }

  /**
   * If the UrlCounter has a collection, return a reference to it, else
   * just the count
   */
  Object makeRefIfColl(CrawlerStatus.UrlCount ctr, String crawlKey,
		       String subkey) {
    if (ctr.hasCollection()) {
      return makeRef(ctr.getCount(),
		     CRAWL_URLS_STATUS_ACCESSOR, crawlKey + "." + subkey);
    }
    return new Long(ctr.getCount());
  }

  /**
   * Return a reference object to the table, displaying the value
   */
  private Object makeRef(long value, String tableName, String key) {
    return new StatusTable.Reference(new Long(value), tableName, key);
  }

  /**
   * Return a reference object to the table, displaying the value
   */
  private Object makeRef(Object value, String tableName, String key) {
    return new StatusTable.Reference(value, tableName, key);
  }

  private List getSummaryInfo(CrawlManagerStatus cms, Counts ct) {
    List res = new ArrayList();
    long totalTime = 0;
    addIfNonZero(res, "Active Crawls", ct.active);
    addIfNonZero(res, "Pending Crawls", ct.waiting);
    addIfNonZero(res, "Successful Crawls", cms.getSuccessCount());
    addIfNonZero(res, "Failed Crawls", cms.getFailedCount());
    Deadline nextStarter = cms.getNextCrawlStarter();
    if (!statusSource.isCrawlerEnabled()) {
      res.add(new StatusTable.SummaryInfo("Crawler is disabled",
					  ColumnDescriptor.TYPE_STRING,
					  null));
    } else if (nextStarter != null) {
      String instr;
      long in = TimeBase.msUntil(nextStarter.getExpirationTime());
      if (in > 0) {
	instr = StringUtil.timeIntervalToString(in);
      } else {
	instr = "running";
      }
      res.add(new StatusTable.SummaryInfo("Crawl Starter",
					  ColumnDescriptor.TYPE_STRING,
					  instr));
    }
    return res;
  }

  private void addIfNonZero(List res, String head, int val) {
    if (val != 0) {
      res.add(new StatusTable.SummaryInfo(head,
					  ColumnDescriptor.TYPE_INT,
					  new Long(val)));
    }
  }

  private List makeSortRules() {
    if (sortRules == null) {
      sortRules = new ArrayList(3);
      sortRules.add(new StatusTable.SortRule(SORT_KEY, true));
      sortRules.add(new StatusTable.SortRule(START_TIME_COL_NAME, false));
      sortRules.add(new StatusTable.SortRule(DURATION_COL_NAME, false));
    }
    return sortRules;
  }

  static class CrawlOverview implements OverviewAccessor {

    private CrawlManager.StatusSource statusSource;
    private PluginManager pluginMgr;

    public CrawlOverview(CrawlManager.StatusSource statusSource) {
      this.statusSource = statusSource;
      this.pluginMgr = statusSource.getDaemon().getPluginManager();
    }

    public Object getOverview(String tableName, BitSet options) {
      if (!statusSource.isCrawlerEnabled()) {
	return "Crawler disabled";
      }
      List res = new ArrayList();
      boolean includeInternalAus = options.get(StatusTable.OPTION_DEBUG_USER);
      CrawlManagerStatus cms = statusSource.getStatus();
      List<CrawlerStatus> allCrawls = cms.getCrawlerStatusList();
      if (allCrawls != null) {
	int active = 0;
	for (CrawlerStatus crawlStat : allCrawls) {
	  if (!includeInternalAus &&
	      pluginMgr.isInternalAu(crawlStat.getAu())) {
	    continue;
	  }
	  if (crawlStat.isCrawlActive()) {
	    active++;
	  }
	}
	String s =
	  StringUtil.numberOfUnits(active, "active crawl", "active crawls");
	res.add(new StatusTable.Reference(s, CrawlManagerImpl.CRAWL_STATUS_TABLE_NAME));
      }
      return res;
    }
  }

  static class Counts {
    int active = 0;
    int waiting = 0;
  }
}
