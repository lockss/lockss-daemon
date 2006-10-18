/*
 * $Id: TestCrawlManagerStatusAccessor.java,v 1.9 2006-10-18 17:06:30 adriz Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.app.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.plugin.*;

public class TestCrawlManagerStatusAccessor extends LockssTestCase {
  private MockCrawlManagerStatusSource statusSource;
  private CrawlManagerStatusAccessor cmStatusAcc;

  private static final String AU_COL_NAME = "au";
  private static final String CRAWL_TYPE = "crawl_type";
  private static final String START_TIME_COL_NAME = "start";
  private static final String END_TIME_COL_NAME = "end";
  private static final String DURATION_COL_NAME = "dur";
  private static final String CONTENT_BYTES_FETCHED = "content_bytes_fetched";
  private static final String NUM_URLS_PARSED = "num_urls_parsed";
  private static final String NUM_URLS_PENDING = "num_urls_pending";
  private static final String NUM_URLS_FETCHED = "num_urls_fetched";
  private static final String NUM_URLS_EXCLUDED = "num_urls_excluded";
  private static final String NUM_URLS_WITH_ERRORS = "num_urls_with_errors";
  private static final String NUM_URLS_NOT_MODIFIED = "num_urls_not_modified";
  private static final String NUM_OF_MIME_TYPES = "num_of_mime_types"; 
  private static final String START_URLS = "start_urls";
  private static final String CRAWL_STATUS = "crawl_status";
  private static final String NC_TYPE = "New Content";
  private static final String REPAIR_TYPE = "Repair";
  private static final String SOURCES = "sources";

  private static final String STATUS_INCOMPLETE = "Active";
  private static final String STATUS_ERROR = "Error";
  private static final String STATUS_SUCCESSFUL = "Successful";

  private static final String CRAWL_URLS_TABLE = "crawl_urls";

  private static List expectedColDescs =
    ListUtil.fromArray(new ColumnDescriptor[] {
      new ColumnDescriptor(AU_COL_NAME, "Journal Volume",
			   ColumnDescriptor.TYPE_STRING),
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
			   ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor(NUM_URLS_PARSED, "Pages Parsed",
			   ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor(NUM_URLS_PENDING, "Pages Pending",
                           ColumnDescriptor.TYPE_INT),                           
      new ColumnDescriptor(NUM_URLS_EXCLUDED, "Pages Excluded",
			   ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor(NUM_URLS_NOT_MODIFIED, "Not Modified",
			   ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor(NUM_URLS_WITH_ERRORS, "Errors",
			   ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor(NUM_OF_MIME_TYPES, "Mime Types",
                           ColumnDescriptor.TYPE_INT,
                           "Number of different content types"),                                                             
      new ColumnDescriptor(START_URLS, "Starting Url(s)",
			   ColumnDescriptor.TYPE_STRING),
      new ColumnDescriptor(SOURCES, "Source(s)",
			   ColumnDescriptor.TYPE_STRING),
    });


  public void setUp() throws Exception {
    // Must set up a plugin manager for these tests, otherwise
    // nullpointerexception is thrown when the CrawlManagerStatusAccessor
    // checks if AUs are internal or not (using
    // PluginManager.isInternalAu(foo))
    super.setUp();
    MockLockssDaemon theDaemon = getMockLockssDaemon();
    theDaemon.setPluginManager(new PluginManager());

    statusSource = new MockCrawlManagerStatusSource(theDaemon);
    cmStatusAcc = new CrawlManagerStatusAccessor(statusSource);
  }

  public void testRequiresKeyReturnsFalse() {
    assertFalse(cmStatusAcc.requiresKey());
  }

  public void testDisplayName() {
    assertEquals("Crawl Status", cmStatusAcc.getDisplayName());
  }

  public void testPopulateTableNullTable() {
    try {
      cmStatusAcc.populateTable(null);
      fail("Should have thrown an IllegalArgumentException for a null table");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testPopulateTableNoCrawls() {
    StatusTable table = new StatusTable("test");
    cmStatusAcc.populateTable(table);
    assertEquals(0, table.getSortedRows().size());
    assertEquals(expectedColDescs, table.getColumnDescriptors());
    assertEquals(new ArrayList(), table.getSortedRows());
  }

  private MockArchivalUnit makeMockAuWithId(String auid) {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setAuId(auid);
    return mau;
  }

  public void testZerosReturnNoLink() {
    StatusTable table = new StatusTable("test");

    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setNumFetched(0);
    status.setNumParsed(0);
    status.setNumPending(0);
    status.setNumNotModified(0);
    status.setNumUrlsWithErrors(0);
    status.setAu(makeMockAuWithId("test_key"));

    statusSource.setCrawlStatusList(ListUtil.list(status));

    cmStatusAcc.populateTable(table);
    assertEquals(expectedColDescs, table.getColumnDescriptors());
    List rows = table.getSortedRows();
    int expectedElements = 1;
    assertEquals(expectedElements, rows.size());

//     Map map = (Map)rows.get(0);
//     assertEquals(new Long(0), map.get(NUM_URLS_FETCHED));
//     assertEquals(new Long(0), map.get(NUM_URLS_PARSED));
//     assertEquals(new Long(0), map.get(NUM_URLS_NOT_MODIFIED));
//     assertEquals(new Long(0), map.get(NUM_URLS_WITH_ERRORS));
  }

  public void testPopulateTableWithKey() {
    StatusTable table = new StatusTable("test", "test_key");

    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setNumFetched(3);
    status.setNumParsed(4);    
    status.setNumPending(4);
    status.setNumExcluded(5);
    status.setAu(makeMockAuWithId("test_key"));


    MockCrawlStatus status2 = new MockCrawlStatus();
    status2.setStartTime(7);
    status2.setEndTime(8);
    status2.setNumFetched(9);
    status2.setNumParsed(10);
    status2.setNumPending(10);
    status2.setAu(makeMockAuWithId("not_test_key"));
    statusSource.setCrawlStatusList(ListUtil.list(status, status2));


    cmStatusAcc.populateTable(table);
    assertEquals(expectedColDescs, table.getColumnDescriptors());
    List rows = table.getSortedRows();
    int expectedElements = 1;
    assertEquals(expectedElements, rows.size());

    Map map = (Map)rows.get(0);
    assertEquals(new Long(1), map.get(START_TIME_COL_NAME));
//     assertEquals(new Long(2), map.get(END_TIME_COL_NAME));
    assertEquals(new Long(1), map.get(DURATION_COL_NAME));

    StatusTable.Reference ref  =
      (StatusTable.Reference)map.get(NUM_URLS_FETCHED);
    assertEquals(new Long(3), ref.getValue());

    ref = (StatusTable.Reference)map.get(NUM_URLS_EXCLUDED);
    assertEquals(new Long(5), ref.getValue());

    assertEquals(CRAWL_URLS_TABLE, ref.getTableName());
  }

  public void testPopulateTableAllAus() {
    StatusTable table = new StatusTable("test");

    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setNumFetched(3);
    status.setNumParsed(4);
    status.setNumPending(4);
    status.setNumUrlsWithErrors(5);
    status.setNumNotModified(6);
    status.setNumExcluded(7);
    status.setAu(makeMockAuWithId("id1"));

    MockCrawlStatus status2 = new MockCrawlStatus();
    status2.setStartTime(7);
    status2.setEndTime(8);
    status2.setNumFetched(9);
    status2.setNumParsed(10);
    status2.setNumPending(10);
    status2.setNumUrlsWithErrors(11);
    status2.setNumNotModified(12);
    status2.setAu(makeMockAuWithId("id2"));

    statusSource.setCrawlStatusList(ListUtil.list(status, status2));

    cmStatusAcc.populateTable(table);
    assertEquals(expectedColDescs, table.getColumnDescriptors());
    List rows = table.getSortedRows();
    int expectedElements = 2;
    assertEquals(expectedElements, rows.size());

    Map map = (Map)rows.get(1);
    assertEquals(new Long(1), map.get(START_TIME_COL_NAME));
//     assertEquals(new Long(2), map.get(END_TIME_COL_NAME));
    assertEquals(new Long(1), map.get(DURATION_COL_NAME));

    StatusTable.Reference ref  =
      (StatusTable.Reference)map.get(NUM_URLS_FETCHED);
    assertEquals(new Long(3), ref.getValue());
    assertEquals(CRAWL_URLS_TABLE, ref.getTableName());
    assertEquals(status.getKey()+".fetched", ref.getKey());

    ref = (StatusTable.Reference)map.get(NUM_URLS_PARSED);
    assertEquals(new Long(4), ref.getValue());
    assertEquals(CRAWL_URLS_TABLE, ref.getTableName());
    assertEquals(status.getKey()+".parsed", ref.getKey());

    ref = (StatusTable.Reference)map.get(NUM_URLS_PENDING);
    assertEquals(new Long(4), ref.getValue());
    assertEquals(CRAWL_URLS_TABLE, ref.getTableName());
    assertEquals(status.getKey()+".pending", ref.getKey());

    ref = (StatusTable.Reference)map.get(NUM_URLS_WITH_ERRORS);
    assertEquals(new Long(5), ref.getValue());
    assertEquals(CRAWL_URLS_TABLE, ref.getTableName());
    assertEquals(status.getKey()+".error", ref.getKey());

    ref = (StatusTable.Reference)map.get(NUM_URLS_NOT_MODIFIED);
    assertEquals(new Long(6), ref.getValue());
    assertEquals(CRAWL_URLS_TABLE, ref.getTableName());
    assertEquals(status.getKey()+".not-modified", ref.getKey());

    ref = (StatusTable.Reference)map.get(NUM_URLS_EXCLUDED);
    assertEquals(new Long(7), ref.getValue());
    assertEquals(CRAWL_URLS_TABLE, ref.getTableName());
    assertEquals(status.getKey()+".excluded", ref.getKey());



    map = (Map)rows.get(0);
    assertEquals(new Long(7), map.get(START_TIME_COL_NAME));
//     assertEquals(new Long(8), map.get(END_TIME_COL_NAME));
    assertEquals(new Long(1), map.get(DURATION_COL_NAME));

    ref = (StatusTable.Reference)map.get(NUM_URLS_FETCHED);
    assertEquals(new Long(9), ref.getValue());
    assertEquals(CRAWL_URLS_TABLE, ref.getTableName());
  }


  public void testCrawlType() {
    StatusTable table = new StatusTable("test");

    MockCrawlStatus status = makeStatus(NC_TYPE, "Unknown");
    MockCrawlStatus status2 = makeStatus(REPAIR_TYPE, "Unknown");

    statusSource.setCrawlStatusList(ListUtil.list(status, status2));

    cmStatusAcc.populateTable(table);
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

    MockCrawlStatus status = makeStatus(NC_TYPE,
					Crawler.STATUS_ACTIVE);

    MockCrawlStatus status2 = makeStatus(REPAIR_TYPE,
					Crawler.STATUS_SUCCESSFUL);

    MockCrawlStatus status3 = makeStatus(REPAIR_TYPE,
					Crawler.STATUS_ERROR);

    statusSource.setCrawlStatusList(ListUtil.list(status, status2, status3));

    cmStatusAcc.populateTable(table);
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

    MockCrawlStatus status = makeStatus(NC_TYPE, 1, 2);

    MockCrawlStatus status2 = makeStatus(REPAIR_TYPE, 2, 2);

    MockCrawlStatus status3 = makeStatus(REPAIR_TYPE, 2, 4);

    statusSource.setCrawlStatusList(ListUtil.list(status, status2, status3));

    cmStatusAcc.populateTable(table);
    assertEquals(expectedColDescs, table.getColumnDescriptors());
    List rows = table.getSortedRows();
    int expectedElements = 3;
    assertEquals(expectedElements, rows.size());

    Map map = (Map)rows.get(0);
    assertEquals(new Long(2), map.get(START_TIME_COL_NAME));
//     assertEquals(new Long(4), map.get(END_TIME_COL_NAME));
    assertEquals(new Long(2), map.get(DURATION_COL_NAME));

    map = (Map)rows.get(1);
    assertEquals(new Long(2), map.get(START_TIME_COL_NAME));
//     assertEquals(new Long(2), map.get(END_TIME_COL_NAME));
    assertEquals(new Long(0), map.get(DURATION_COL_NAME));

    map = (Map)rows.get(2);
    assertEquals(new Long(1), map.get(START_TIME_COL_NAME));
//     assertEquals(new Long(2), map.get(END_TIME_COL_NAME));
    assertEquals(new Long(1), map.get(DURATION_COL_NAME));
  }



  /** @deprecated */
  private static MockCrawlStatus makeStatus(String type, long start, long end) {
    MockCrawlStatus status = makeStatus(type, "Unknown");
    status.setStartTime(start);
    status.setEndTime(end);
    return status;
  }

  private static MockCrawlStatus makeStatus(String type, String crawlStatus) {
    MockCrawlStatus status = new MockCrawlStatus();
    status.setType(type);
    status.setAu(new MockArchivalUnit());
    status.setCrawlStatus(crawlStatus);
    return status;
  }
}
