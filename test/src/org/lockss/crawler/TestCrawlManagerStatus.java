/*
 * $Id: TestCrawlManagerStatus.java,v 1.9 2004-03-22 22:06:06 troberts Exp $
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
  private static final String START_URLS = "start_urls";
  private static final String CRAWL_STATUS = "crawl_status";
  private static final String NC_TYPE = "New Content";
  private static final String REPAIR_TYPE = "Repair";

  private static final String STATUS_INCOMPLETE = "Active";
  private static final String STATUS_ERROR = "Error";
  private static final String STATUS_SUCCESSFUL = "Successful";

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
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor(CRAWL_STATUS, "Crawl Status",
				       ColumnDescriptor.TYPE_STRING)
		  );

  public void setUp() {
    statusSource = new MockCrawlManagerStatusSource();
    cmStatus = new CrawlManagerStatus(statusSource);
  }

  public void testRequiresKeyReturnsFalse() {
    assertFalse(cmStatus.requiresKey());
  }

  public void testDisplayName() {
    assertEquals("Crawl Status", cmStatus.getDisplayName());
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

    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setNumFetched(3);
    status.setNumParsed(4);
    status.setAu(new MockArchivalUnit());


    MockCrawlStatus status2 = new MockCrawlStatus();
    status2.setStartTime(7);
    status2.setEndTime(8);
    status2.setNumFetched(9);
    status2.setNumParsed(10);
    status2.setAu(new MockArchivalUnit());
    statusSource.setCrawlStatus(ListUtil.list(status), "test_key");
    statusSource.setCrawlStatus(ListUtil.list(status2), "not_test_key");


    cmStatus.populateTable(table);
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

    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setNumFetched(3);
    status.setNumParsed(4);
    status.setAu(new MockArchivalUnit());

    MockCrawlStatus status2 = new MockCrawlStatus();
    status2.setStartTime(7);
    status2.setEndTime(8);
    status2.setNumFetched(9);
    status2.setNumParsed(10);
    status2.setAu(new MockArchivalUnit());
    statusSource.setCrawlStatus(ListUtil.list(status), "id1");
    statusSource.setCrawlStatus(ListUtil.list(status2), "id2");
    statusSource.setActiveAus(ListUtil.list("id1", "id2"));

    cmStatus.populateTable(table);
    assertEquals(expectedColDescs, table.getColumnDescriptors());
    List rows = table.getSortedRows();
    int expectedElements = 2;
    assertEquals(expectedElements, rows.size());

    Map map = (Map)rows.get(1);
    assertEquals(new Long(1), map.get(START_TIME_COL_NAME));
    assertEquals(new Long(2), map.get(END_TIME_COL_NAME));
    assertEquals(new Long(3), map.get(NUM_URLS_FETCHED));
    assertEquals(new Long(4), map.get(NUM_URLS_PARSED));

    map = (Map)rows.get(0);
    assertEquals(new Long(7), map.get(START_TIME_COL_NAME));
    assertEquals(new Long(8), map.get(END_TIME_COL_NAME));
    assertEquals(new Long(9), map.get(NUM_URLS_FETCHED));
    assertEquals(new Long(10), map.get(NUM_URLS_PARSED));
  }


  public void testCrawlType() {
    StatusTable table = new StatusTable("test");

    MockCrawlStatus status = makeStatus(Crawler.NEW_CONTENT, -1);
    MockCrawlStatus status2 = makeStatus(Crawler.REPAIR, -1);

    statusSource.setCrawlStatus(ListUtil.list(status), "key1");
    statusSource.setCrawlStatus(ListUtil.list(status2), "key2");
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
  
  public void testCrawlStatus() {
    StatusTable table = new StatusTable("test");

    MockCrawlStatus status = makeStatus(Crawler.NEW_CONTENT,
					Crawler.STATUS_INCOMPLETE);

    MockCrawlStatus status2 = makeStatus(Crawler.REPAIR,
					Crawler.STATUS_SUCCESSFUL);

    MockCrawlStatus status3 = makeStatus(Crawler.REPAIR,
					Crawler.STATUS_ERROR);

    statusSource.setCrawlStatus(ListUtil.list(status), "key1");
    statusSource.setCrawlStatus(ListUtil.list(status2), "key2");
    statusSource.setCrawlStatus(ListUtil.list(status3), "key3");
    statusSource.setActiveAus(ListUtil.list("key1", "key2", "key3"));

    cmStatus.populateTable(table);
    assertEquals(expectedColDescs, table.getColumnDescriptors());
    List rows = table.getSortedRows();
    int expectedElements = 3;
    assertEquals(expectedElements, rows.size());

    Map map = (Map)rows.get(0);
    assertEquals(STATUS_INCOMPLETE, map.get(CRAWL_STATUS));

    map = (Map)rows.get(1);
    assertEquals(STATUS_SUCCESSFUL, map.get(CRAWL_STATUS));

    map = (Map)rows.get(2);
    assertEquals(STATUS_ERROR, map.get(CRAWL_STATUS));
  }

  public void testSortOrder() {
    StatusTable table = new StatusTable("test");

    MockCrawlStatus status = makeStatus(Crawler.NEW_CONTENT, 1, 2);

    MockCrawlStatus status2 = makeStatus(Crawler.REPAIR, 2, 2);

    MockCrawlStatus status3 = makeStatus(Crawler.REPAIR, 2, 4);

    statusSource.setCrawlStatus(ListUtil.list(status), "key1");
    statusSource.setCrawlStatus(ListUtil.list(status2), "key2");
    statusSource.setCrawlStatus(ListUtil.list(status3), "key3");
    statusSource.setActiveAus(ListUtil.list("key1", "key2", "key3"));

    cmStatus.populateTable(table);
    assertEquals(expectedColDescs, table.getColumnDescriptors());
    List rows = table.getSortedRows();
    int expectedElements = 3;
    assertEquals(expectedElements, rows.size());

    Map map = (Map)rows.get(0);
    assertEquals(new Long(2), map.get(START_TIME_COL_NAME));
    assertEquals(new Long(4), map.get(END_TIME_COL_NAME));

    map = (Map)rows.get(1);
    assertEquals(new Long(2), map.get(START_TIME_COL_NAME));
    assertEquals(new Long(2), map.get(END_TIME_COL_NAME));

    map = (Map)rows.get(2);
    assertEquals(new Long(1), map.get(START_TIME_COL_NAME));
    assertEquals(new Long(2), map.get(END_TIME_COL_NAME));
  }

  private static MockCrawlStatus makeStatus(int type, long start, long end) {
    MockCrawlStatus status = makeStatus(type, -1);
    status.setStartTime(start);
    status.setEndTime(end);
    return status;
  }

  private static MockCrawlStatus makeStatus(int type, int crawlStatus) {
    MockCrawlStatus status = new MockCrawlStatus();
    status.setType(type);
    status.setAu(new MockArchivalUnit());
    status.setCrawlStatus(crawlStatus);
    return status;
  }
}
