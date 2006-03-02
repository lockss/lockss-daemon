/*
 * $Id: CrawlManagerStatus.java,v 1.29.8.1 2006-03-02 06:27:00 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.commons.collections.map.ReferenceMap;
import org.lockss.daemon.status.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class CrawlManagerStatus implements StatusAccessor {

  private static final String AU_COL_NAME = "au";
  private static final String CRAWL_TYPE = "crawl_type";
  private static final String START_TIME_COL_NAME = "start";
  private static final String END_TIME_COL_NAME = "end";
  private static final String CONTENT_BYTES_FETCHED = "content_bytes_fetched";
  private static final String NUM_URLS_PARSED = "num_urls_parsed";
  private static final String NUM_URLS_FETCHED = "num_urls_fetched";
  private static final String NUM_URLS_WITH_ERRORS = "num_urls_with_errors";
  private static final String NUM_URLS_NOT_MODIFIED = "num_urls_not_modified";
  private static final String START_URLS = "start_urls";
  private static final String CRAWL_STATUS = "crawl_status";
  private static final String SOURCES = "sources";

  private List sortRules = null;

  private List colDescs =
    ListUtil.fromArray(new ColumnDescriptor[] {
      new ColumnDescriptor(AU_COL_NAME, "Journal Volume",
			   ColumnDescriptor.TYPE_STRING)
      .setComparator(CatalogueOrderComparator.SINGLETON),
      new ColumnDescriptor(CRAWL_TYPE, "Crawl Type",
			   ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor(START_TIME_COL_NAME, "Start Time",
			   ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor(END_TIME_COL_NAME, "End Time",
			   ColumnDescriptor.TYPE_DATE),
      new ColumnDescriptor(CRAWL_STATUS, "Crawl Status",
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
      new ColumnDescriptor(NUM_URLS_NOT_MODIFIED, "Not Modified",
			   ColumnDescriptor.TYPE_INT,
			   "Number of pages for which we already had current content"),
      new ColumnDescriptor(NUM_URLS_WITH_ERRORS, "Errors",
			   ColumnDescriptor.TYPE_INT,
			   "Number of pages that could not be fetched"),
      new ColumnDescriptor(START_URLS, "Starting Url(s)",
			   ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor(SOURCES, "Source(s)",
			   ColumnDescriptor.TYPE_STRING),
    });


  private CrawlManager.StatusSource statusSource;
  private PluginManager pluginMgr;

  // Maps key to Crawler.Status.  Weak values allow Status obj to be
  // collected
  private Map statusMap = new ReferenceMap(ReferenceMap.HARD,
					   ReferenceMap.WEAK);

  public CrawlManagerStatus(CrawlManager.StatusSource statusSource) {
    this.statusSource = statusSource;
    this.pluginMgr = statusSource.getDaemon().getPluginManager();
  }

  protected CrawlManagerStatus() {
    //for testing
  }

  public String getDisplayName() {
    return "Crawl Status";
  }

  private List getRows(String key, boolean includeInternalAus) {
    List allCrawls = statusSource.getCrawlStatusList();
    List rows = new ArrayList();
    if (allCrawls != null) {

      for (Iterator it = allCrawls.iterator(); it.hasNext();) {
	Crawler.Status crawlStat = (Crawler.Status)it.next();
	if (!includeInternalAus &&
	    pluginMgr.isInternalAu(crawlStat.getAu())) {
	  continue;
	} else if (key != null && !key.equals(crawlStat.getAu().getAuId())) {
	  continue;
	}
	rows.add(makeRow(crawlStat));
      }
    }
    return rows;
  }

//   private void addCrawls(String key, List rows, boolean includeInternalAus) {
//     Collection crawls = statusSource.getCrawlStatus(key);
//     if (crawls != null) {
//       for (Iterator it = crawls.iterator(); it.hasNext();) {
// 	Crawler.Status crawlStat = (Crawler.Status)it.next();
// 	if (!includeInternalAus &&
// 	    pluginMgr.isInternalAu(crawlStat.getAu())) {
// 	  continue;
// 	}
// 	rows.add(makeRow(crawlStat));
//       }
//     }
//   }

//   private List getAllCrawls(boolean includeInternalAus) {
//     Collection aus = statusSource.getActiveAus();


//     List crawls = new ArrayList();
//     if (aus != null) {
//       for (Iterator it = aus.iterator(); it.hasNext();) {
// 	String auid = (String)it.next();
// 	addCrawls(auid, crawls, includeInternalAus);
//       }
//     }
//     return crawls;
//   }



  private Map makeRow(Crawler.Status status) {
    String key = status.getKey();
    statusMap.put(key, status);

    Map row = new HashMap();
    ArchivalUnit au = status.getAu();
    row.put(AU_COL_NAME, au.getName());
    row.put(CRAWL_TYPE, status.getType());
    row.put(START_TIME_COL_NAME, new Long(status.getStartTime()));
    row.put(END_TIME_COL_NAME, new Long(status.getEndTime()));
    row.put(CONTENT_BYTES_FETCHED, new Long(status.getContentBytesFetched()));
    row.put(NUM_URLS_FETCHED,
	    makeRef(status.getNumFetched(),
		    "single_crawl_status", "fetched."+key));

    row.put(NUM_URLS_WITH_ERRORS,
	    makeRef(status.getNumUrlsWithErrors(),
		    "single_crawl_status", "error."+key));

    row.put(NUM_URLS_NOT_MODIFIED,
	    makeRef(status.getNumNotModified(),
		    "single_crawl_status", "not-modified."+key));
    row.put(NUM_URLS_PARSED,
	    makeRef(status.getNumParsed(),
		    "single_crawl_status", "parsed."+key));
    row.put(START_URLS,
	    (StringUtil.separatedString(status.getStartUrls(), "\n")));
    row.put(SOURCES,
	    (StringUtil.separatedString(status.getSources(), "\n")));
    row.put(CRAWL_STATUS, status.getCrawlStatus());
    return row;
  }

  /**
   * Makes the proper reference object if value is != 0, otherwise just returns
   * a Long
   */
  private Object makeRef(long value, String tableName, String key) {
    return new StatusTable.Reference(new Long(value), tableName, key);
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
    String key = table.getKey();
    table.setColumnDescriptors(colDescs);
    boolean includeInternalAus =
      table.getOptions().get(StatusTable.OPTION_DEBUG_USER);
    table.setRows(getRows(key, includeInternalAus));

    table.setDefaultSortRules(makeSortRules());
  }

  private List makeSortRules() {
    if (sortRules == null) {
      sortRules = new ArrayList(2);
      sortRules.add(new StatusTable.SortRule(START_TIME_COL_NAME, false));
      sortRules.add(new StatusTable.SortRule(END_TIME_COL_NAME, false));
    }
    return sortRules;
  }

  public Crawler.Status getStatusObject(String key) {
    return (Crawler.Status)statusMap.get(key);
  }

}
