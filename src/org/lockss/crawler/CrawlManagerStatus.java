/*
 * $Id: CrawlManagerStatus.java,v 1.14 2004-07-12 23:01:53 smorabito Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.plugin.base.*;
import org.lockss.util.*;

public class CrawlManagerStatus implements StatusAccessor {

  private static final String AU_COL_NAME = "au";
  private static final String CRAWL_TYPE = "crawl_type";
  private static final String START_TIME_COL_NAME = "start";
  private static final String END_TIME_COL_NAME = "end";
  private static final String NUM_URLS_PARSED = "num_urls_parsed";
  private static final String NUM_URLS_FETCHED = "num_urls_fetched";
  private static final String START_URLS = "start_urls";
  private static final String CRAWL_STATUS = "crawl_status";

//   public static final String INCOMPLETE_STRING = "Active";
//   public static final String SUCCESSFUL_STRING = "Successful";
//   public static final String ERROR_STRING = "Error";
//   public static final String FETCH_ERROR_STRING = "Fetch error";
//   public static final String PUB_PERMISSION_STRING =
//     "No permission from publisher";
//   public static final String WINDOW_CLOSED_STRING = "Crawl window closed";
//   public static final String UNKNOWN_STRING = "Unknown";

  private List sortRules = null;

  private List colDescs =
    ListUtil.list(
		  new ColumnDescriptor(AU_COL_NAME, "Journal Volume",
				       ColumnDescriptor.TYPE_STRING)
		  .setComparator(CatalogueOrderComparator.SINGLETON),
		  new ColumnDescriptor(CRAWL_TYPE, "Crawl Type",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor(START_TIME_COL_NAME, "Start Time",
				       ColumnDescriptor.TYPE_DATE),
		  new ColumnDescriptor(END_TIME_COL_NAME, "End Time",
				       ColumnDescriptor.TYPE_DATE),
		  new ColumnDescriptor(NUM_URLS_FETCHED, "URLs fetched",
				       ColumnDescriptor.TYPE_INT),
		  new ColumnDescriptor(NUM_URLS_PARSED, "URLs parsed",
				       ColumnDescriptor.TYPE_INT),
		  new ColumnDescriptor(START_URLS, "starting url",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor(CRAWL_STATUS, "Crawl Status",
				       ColumnDescriptor.TYPE_STRING)
		  );


  private static final String NC_TYPE = "New Content";
  private static final String REPAIR_TYPE = "Repair";
  private Map newContentCrawls = null;
  private Map repairCrawls = null;
  private CrawlManager.StatusSource statusSource;

  public CrawlManagerStatus(CrawlManager.StatusSource statusSource) {
    this.statusSource = statusSource;
  }

  public String getDisplayName() {
    return "Crawl Status";
  }

  private List getRows(String key, boolean includeInternalAus) {
    if (key == null) {
      return getAllCrawls(includeInternalAus);
    }

    List rows = new ArrayList();
    addCrawls(key, rows, includeInternalAus);
    return rows;
  }

  public void addCrawls(String key, List rows, boolean includeInternalAus) {
    Collection crawls = statusSource.getCrawlStatus(key);
    if (crawls != null) {
      for (Iterator it = crawls.iterator(); it.hasNext();) {
	Crawler.Status crawlStat = (Crawler.Status)it.next();
	if (!includeInternalAus &&
	    (crawlStat.getAu() instanceof RegistryArchivalUnit)) {
	  continue;
	}
	rows.add(makeRow(crawlStat));
      }
    }
  }

  private List getAllCrawls(boolean includeInternalAus) {
    Collection aus = statusSource.getActiveAus();
    List crawls = new ArrayList();
    if (aus != null) {
      for (Iterator it = aus.iterator(); it.hasNext();) {
	String auid = (String)it.next();
	addCrawls(auid, crawls, includeInternalAus);
      }
    }
    return crawls;
  }

  

  private String getTypeString(int type) {
    switch(type) {
      case Crawler.NEW_CONTENT:
	return NC_TYPE;
      case Crawler.REPAIR:
	return REPAIR_TYPE;
    }
    return "Unknown";
  }

  private Map makeRow(Crawler.Status status) {
    String type = getTypeString(status.getType());
    Map row = new HashMap();
    ArchivalUnit au = status.getAu();
    row.put(AU_COL_NAME, au.getName());
    row.put(CRAWL_TYPE, type);
    row.put(START_TIME_COL_NAME, new Long(status.getStartTime()));
    row.put(END_TIME_COL_NAME, new Long(status.getEndTime()));
    row.put(NUM_URLS_FETCHED, new Long(status.getNumFetched()));
    row.put(NUM_URLS_PARSED, new Long(status.getNumParsed()));
    row.put(START_URLS,
	    (StringUtil.separatedString(status.getStartUrls(), "\n")));
    row.put(CRAWL_STATUS, status.getCrawlStatus());
    return row;
  }

//   private Map makeRow(String type, Crawler crawler) {
//     Map row = new HashMap();
//     ArchivalUnit au = crawler.getAu();
//     row.put(AU_COL_NAME, au.getName());
//     row.put(CRAWL_TYPE, type);
//     row.put(START_TIME_COL_NAME, new Long(crawler.getStartTime()));
//     row.put(END_TIME_COL_NAME, new Long(crawler.getEndTime()));
//     row.put(NUM_URLS_FETCHED, new Long(crawler.getNumFetched()));
//     row.put(NUM_URLS_PARSED, new Long(crawler.getNumParsed()));
//     row.put(START_URLS,
// 	    (StringUtil.separatedString(crawler.getStartUrls(), "\n")));
//     row.put(CRAWL_STATUS, statusToString(crawler.getStatus()));
//     return row;
//   }

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
    table.setRows(getRows(key, table.getOptions().get(StatusTable.OPTION_INCLUDE_INTERNAL_AUS)));

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

//   private String statusToString(int status) {
//     switch (status) {
//     case Crawler.STATUS_SUCCESSFUL : 
//       return SUCCESSFUL_STRING;
//     case Crawler.STATUS_INCOMPLETE : 
//       return INCOMPLETE_STRING;
//     case Crawler.STATUS_ERROR : 
//       return ERROR_STRING;
//     case Crawler.STATUS_FETCH_ERROR : 
//       return FETCH_ERROR_STRING;
//     case Crawler.STATUS_PUB_PERMISSION : 
//       return PUB_PERMISSION_STRING;
//     case Crawler.STATUS_WINDOW_CLOSED : 
//       return WINDOW_CLOSED_STRING;
//     default : 
//       return UNKNOWN_STRING;
//     }
//   }
}
