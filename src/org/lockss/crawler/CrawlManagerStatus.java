/*
 * $Id: CrawlManagerStatus.java,v 1.1 2003-07-02 00:56:04 troberts Exp $
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
  private static final String NUM_CACHE_HITS = "num_cache_hits";
  private static final String CACHE_HITS_PERCENT = "cache_hits_percent";
  private static final String START_URLS = "start_urls";

  private List colDescs =
    ListUtil.list(
		  new ColumnDescriptor(AU_COL_NAME, "Journal Volume",
				       ColumnDescriptor.TYPE_STRING),
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
		  new ColumnDescriptor(NUM_CACHE_HITS, "Cache hits",
				       ColumnDescriptor.TYPE_INT),
		  new ColumnDescriptor(CACHE_HITS_PERCENT, "percent",
				       ColumnDescriptor.TYPE_PERCENT),
		  new ColumnDescriptor(START_URLS, "starting url",
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

  private List getRows(String key) {
    if (key == null) {
      return getAllCrawls();
    }

//     addCrawlsFromMap(key, newContentCrawls, NC_TYPE, rows);
//     addCrawlsFromMap(key, repairCrawls, REPAIR_TYPE, rows);
    List rows = new ArrayList();
    addCrawls(key, rows);
    return rows;
  }

  public void addCrawls(String key, List rows) {
    Collection crawls = statusSource.getCrawls(key);
    if (crawls != null) {
      for (Iterator it = crawls.iterator(); it.hasNext();) {
	rows.add(makeRow((Crawler) it.next()));
      }
    }
  }

  private void addCrawlsFromMap(String key, Map crawlMap,
				String type, List rows) {
    synchronized(crawlMap) {
      List crawlsForAu = (List) crawlMap.get(key);
      if (crawlsForAu != null) {
	Iterator it = crawlsForAu.iterator();
	while (it.hasNext()) {
	  rows.add(makeRow(type, (Crawler) it.next()));
	}
      }
    }
  }

  private List getAllCrawls() {
    Collection aus = statusSource.getActiveAUs();
    List crawls = new ArrayList();
    if (aus != null) {
      for (Iterator it = aus.iterator(); it.hasNext();) {
	String auid = (String)it.next();
	addCrawls(auid, crawls);
      }
    }
    return crawls;
  }

  

  private void getAllCrawlsFromMap(List rows, String type, Map crawlMap) {
    synchronized(crawlMap) {
      Iterator keys = crawlMap.keySet().iterator();
      while (keys.hasNext()) {
	List crawls = (List)crawlMap.get((String)keys.next());
	Iterator it = crawls.iterator();
	while (it.hasNext()) {
	  rows.add(makeRow(type, (Crawler)it.next()));
	}
      }
    }
  }

  private String getTypeString(int type) {
    return "blah";
  }

  private Map makeRow(Crawler crawler) {
    String type = getTypeString(crawler.getType());
    Map row = new HashMap();
    ArchivalUnit au = crawler.getAU();
    row.put(AU_COL_NAME, au.getName());
    row.put(CRAWL_TYPE, type);
    row.put(START_TIME_COL_NAME, new Long(crawler.getStartTime()));
    row.put(END_TIME_COL_NAME, new Long(crawler.getEndTime()));
    row.put(NUM_URLS_FETCHED, new Long(crawler.getNumFetched()));
    row.put(NUM_URLS_PARSED, new Long(crawler.getNumParsed()));
    row.put(START_URLS,
	    (StringUtil.separatedString(crawler.getStartUrls(), "\n")));
    if (au instanceof BaseArchivalUnit) {
      BaseArchivalUnit bau = (BaseArchivalUnit)au;
      row.put(NUM_CACHE_HITS, new Long(bau.getCrawlSpecCacheHits()));
      double per = ((float)bau.getCrawlSpecCacheHits() /
		    ((float)bau.getCrawlSpecCacheHits() +
		     (float)bau.getCrawlSpecCacheMisses()));
      row.put(CACHE_HITS_PERCENT, new Double(per));
    }
    return row;
  }

  private Map makeRow(String type, Crawler crawler) {
    Map row = new HashMap();
    ArchivalUnit au = crawler.getAU();
    row.put(AU_COL_NAME, au.getName());
    row.put(CRAWL_TYPE, type);
    row.put(START_TIME_COL_NAME, new Long(crawler.getStartTime()));
    row.put(END_TIME_COL_NAME, new Long(crawler.getEndTime()));
    row.put(NUM_URLS_FETCHED, new Long(crawler.getNumFetched()));
    row.put(NUM_URLS_PARSED, new Long(crawler.getNumParsed()));
    row.put(START_URLS,
	    (StringUtil.separatedString(crawler.getStartUrls(), "\n")));
    if (au instanceof BaseArchivalUnit) {
      BaseArchivalUnit bau = (BaseArchivalUnit)au;
      row.put(NUM_CACHE_HITS, new Long(bau.getCrawlSpecCacheHits()));
      double per = ((float)bau.getCrawlSpecCacheHits() /
		    ((float)bau.getCrawlSpecCacheHits() +
		     (float)bau.getCrawlSpecCacheMisses()));
      row.put(CACHE_HITS_PERCENT, new Double(per));
    }
    return row;
  }

  public boolean requiresKey() {
    return false;
  }

  /**
   * @param table StatusTable to populate
   * @throws IllegalArgumentException if called with a null StatusTable
   */
  public void populateTable(StatusTable table) {
    if (table == null) {
      throw new IllegalArgumentException("Called with null StatusTable");
    }
    String key = table.getKey();
    table.setTitle("Crawl Status");
    table.setColumnDescriptors(colDescs);
    table.setRows(getRows(key));
  }
}
