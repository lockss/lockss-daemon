/*
 * $Id: TestCrawlManagerStatus.java,v 1.4 2003-10-31 23:14:45 troberts Exp $
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
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

public class TestCrawlManagerStatus extends LockssTestCase {
  private MockCrawlManagerStatusSource statusSource;
  private CrawlManagerStatus cmStatus;

  private static final String AU_COL_NAME = "au";
  private static final String CRAWL_TYPE = "crawl_type";
  private static final String START_TIME_COL_NAME = "start";
  private static final String END_TIME_COL_NAME = "end";
  private static final String NUM_URLS_PARSED = "num_urls_parsed";
  private static final String NUM_URLS_FETCHED = "num_urls_fetched";
  private static final String NUM_CACHE_HITS = "num_cache_hits";
  private static final String CACHE_HITS_PERCENT = "cache_hits_percent";
  private static final String START_URLS = "start_urls";
  private static final String NC_TYPE = "New Content";
  private static final String REPAIR_TYPE = "Repair";

  private static List expectedColDescs =
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
		  new ColumnDescriptor(START_URLS, "starting url",
				       ColumnDescriptor.TYPE_STRING)
		  );

  public void setUp() {
    statusSource = new MockCrawlManagerStatusSource();
    cmStatus = new CrawlManagerStatus(statusSource);
  }

  public void testRequiresKeyReturnsFalse() {
    assertFalse(cmStatus.requiresKey());
  }

  public void testPopulateTableNullTable() {
    try {
      cmStatus.populateTable(null);
      fail("Should have thrown an IllegalArgumentException for a null table");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testPopulateTableNoCrawls() {
    StatusTable table = new StatusTable("test");
    cmStatus.populateTable(table);
    assertEquals("Crawl Status", table.getTitle());
    assertEquals(0, table.getSortedRows().size());
    assertEquals(expectedColDescs, table.getColumnDescriptors());
    assertEquals(new ArrayList(), table.getSortedRows());
  }

  private MockArchivalUnit makeMockAuWithId(String auid) {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setAuId(auid);
    return mau;
  }

  public void testPopulateTableWithKey() {
    StatusTable table = new StatusTable("test", "test_key");

    MockCrawler crawler = new MockCrawler();
    crawler.setStartTime(1);
    crawler.setEndTime(2);
    crawler.setNumFetched(3);
    crawler.setNumParsed(4);
     crawler.setAu(new MockArchivalUnit());

    MockCrawler crawler2 = new MockCrawler();
    crawler2.setStartTime(7);
    crawler2.setEndTime(8);
    crawler2.setNumFetched(9);
    crawler2.setNumParsed(10);
    crawler2.setAu(new MockArchivalUnit());
    statusSource.setCrawls(ListUtil.list(crawler), "test_key");
    statusSource.setCrawls(ListUtil.list(crawler2), "not_test_key");

    cmStatus.populateTable(table);
    assertEquals("Crawl Status", table.getTitle());
    assertEquals(expectedColDescs, table.getColumnDescriptors());
    List rows = table.getSortedRows();
    int expectedElements = 1;
    assertEquals(expectedElements, rows.size());

    Map map = (Map)rows.get(0);
    assertEquals(new Long(1), map.get(START_TIME_COL_NAME));
    assertEquals(new Long(2), map.get(END_TIME_COL_NAME));
    assertEquals(new Long(3), map.get(NUM_URLS_FETCHED));
    assertEquals(new Long(4), map.get(NUM_URLS_PARSED));
  }

  public void testPopulateTableAllAus() {
    StatusTable table = new StatusTable("test");
    MockCrawler crawler = new MockCrawler();
    crawler.setStartTime(1);
    crawler.setEndTime(2);
    crawler.setNumFetched(3);
    crawler.setNumParsed(4);
    crawler.setAu(new MockArchivalUnit());

    MockCrawler crawler2 = new MockCrawler();
    crawler2.setStartTime(7);
    crawler2.setEndTime(8);
    crawler2.setNumFetched(9);
    crawler2.setNumParsed(10);
    crawler2.setAu(new MockArchivalUnit());
    statusSource.setCrawls(ListUtil.list(crawler), "id1");
    statusSource.setCrawls(ListUtil.list(crawler2), "id2");
    statusSource.setActiveAus(ListUtil.list("id1", "id2"));

    cmStatus.populateTable(table);
    assertEquals("Crawl Status", table.getTitle());
    assertEquals(expectedColDescs, table.getColumnDescriptors());
    List rows = table.getSortedRows();
    int expectedElements = 2;
    assertEquals(expectedElements, rows.size());

    Map map = (Map)rows.get(0);
    assertEquals(new Long(1), map.get(START_TIME_COL_NAME));
    assertEquals(new Long(2), map.get(END_TIME_COL_NAME));
    assertEquals(new Long(3), map.get(NUM_URLS_FETCHED));
    assertEquals(new Long(4), map.get(NUM_URLS_PARSED));

    map = (Map)rows.get(1);
    assertEquals(new Long(7), map.get(START_TIME_COL_NAME));
    assertEquals(new Long(8), map.get(END_TIME_COL_NAME));
    assertEquals(new Long(9), map.get(NUM_URLS_FETCHED));
    assertEquals(new Long(10), map.get(NUM_URLS_PARSED));
  }


  public void testCrawlType() {
    StatusTable table = new StatusTable("test");

    MockCrawler crawler = new MockCrawler();
    crawler.setType(Crawler.NEW_CONTENT);
    crawler.setAu(new MockArchivalUnit());

    MockCrawler crawler2 = new MockCrawler();
    crawler2.setType(Crawler.REPAIR);
    crawler2.setAu(new MockArchivalUnit());

    statusSource.setCrawls(ListUtil.list(crawler), "key1");
    statusSource.setCrawls(ListUtil.list(crawler2), "key2");
    statusSource.setActiveAus(ListUtil.list("key1", "key2"));

    cmStatus.populateTable(table);
    assertEquals(expectedColDescs, table.getColumnDescriptors());
    List rows = table.getSortedRows();
    int expectedElements = 2;
    assertEquals(expectedElements, rows.size());

    Map map = (Map)rows.get(0);
    assertEquals(NC_TYPE, map.get(CRAWL_TYPE));

    map = (Map)rows.get(1);
    assertEquals(REPAIR_TYPE, map.get(CRAWL_TYPE));
  }

}
