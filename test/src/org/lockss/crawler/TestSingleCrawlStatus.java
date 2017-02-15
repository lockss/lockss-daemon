/*
 * $Id$
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
import org.apache.commons.collections.map.LinkedMap;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.crawler.CrawlerStatus.*;

public class TestSingleCrawlStatus extends LockssTestCase {
  MockCrawlManagerStatusSource statusSource;
  CrawlUrlsStatusAccessor cStatus;
  MockCrawlStatus mcStatus = new MockCrawlStatus();

  private static final String URL = "url";
  private static final String CRAWL_ERROR = "crawl_error";
  private static final String CRAWL_SEVERITY = "crawl_severity";

  private static List expectedColDescsFetched =
    ListUtil.list(new ColumnDescriptor(URL, "URL Fetched",
				       ColumnDescriptor.TYPE_STRING));

  private static List expectedColDescsParsed =
    ListUtil.list(new ColumnDescriptor(URL, "URL Parsed",
				       ColumnDescriptor.TYPE_STRING));

  private static List expectedColDescsNotModified =
    ListUtil.list(new ColumnDescriptor(URL, "URL Not-Modified",
				       ColumnDescriptor.TYPE_STRING));

  private static List expectedColDescsError =
    ListUtil.list(new ColumnDescriptor(CRAWL_SEVERITY, "Severity",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor(URL, "URL",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor(CRAWL_ERROR, "Error",
				       ColumnDescriptor.TYPE_STRING));

  private static List expectedColDescsExcluded =
    ListUtil.list(new ColumnDescriptor(URL, "URL Excluded",
				       ColumnDescriptor.TYPE_STRING));

  private static List expectedColDescsExcludedWithReason =
    ListUtil.list(new ColumnDescriptor(URL, "URL Excluded",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("reason", "Reason",
				       ColumnDescriptor.TYPE_STRING));

  public void setUp() throws Exception {
    super.setUp();
    statusSource = new MockCrawlManagerStatusSource(null);
    statusSource.setStatus(new CrawlManagerStatus(10));
    cStatus = new CrawlUrlsStatusAccessor(statusSource);
  }

  public void testCrawlStatusDisplayName() {
    try {
      cStatus.getDisplayName();
      fail("getDisplayName should throw");
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testRequiresKey() {
    assertTrue(cStatus.requiresKey());
  }

  public void testPopulateTableNullTable() throws Exception {
    try {
      cStatus.populateTable(null);
      fail("Should have thrown an IllegalArgumentException for a null table");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testFetchedUrlsThrowsWOKey() throws Exception {
    StatusTable table = new StatusTable("test");
    try {
      cStatus.populateTable(table);
      fail("Should have thrown an IllegalArgumentException when called without"
	   +" a key");
    } catch (IllegalArgumentException e) {
    }
  }

  private void addCrawlStatus(CrawlerStatus status) {
    statusSource.getStatus().addCrawlStatus(status);
  }

  public void testCrawlStatusFetchedUrlsNoUrls() throws Exception {
    mcStatus.setStartTime(1);
    mcStatus.setEndTime(2);
    mcStatus.setUrlsFetched(ListUtil.list());
    mcStatus.setNumParsed(4);
    mcStatus.setAu(new MockArchivalUnit());

    addCrawlStatus(mcStatus);

    StatusTable table = new StatusTable("test", mcStatus.getKey()+".fetched");
    //out: StatusTable table = new StatusTable("test", "fetched."+mcStatus.getKey());
    cStatus.populateTable(table);
    assertEquals(expectedColDescsFetched, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testCrawlStatusParsedUrlsNoUrls() throws Exception {
    mcStatus.setStartTime(1);
    mcStatus.setEndTime(2);
    mcStatus.setNumFetched(3);
    mcStatus.setUrlsParsed(ListUtil.list());
    mcStatus.setAu(new MockArchivalUnit());

    addCrawlStatus(mcStatus);

    StatusTable table = new StatusTable("test", mcStatus.getKey()+".parsed");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsParsed, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testCrawlStatusErrorUrlsNoUrls() throws Exception {
    mcStatus.setStartTime(1);
    mcStatus.setEndTime(2);
    mcStatus.setUrlsWithErrors(new HashMap());
    mcStatus.setNumParsed(4);
    mcStatus.setAu(new MockArchivalUnit());

    addCrawlStatus(mcStatus);

    StatusTable table = new StatusTable("test", mcStatus.getKey()+".error");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsError, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testNotModifiedNoUrls() throws Exception {
    mcStatus.setStartTime(1);
    mcStatus.setEndTime(2);
    mcStatus.setUrlsNotModified(Collections.EMPTY_LIST);
    mcStatus.setNumParsed(4);
    mcStatus.setAu(new MockArchivalUnit());

    addCrawlStatus(mcStatus);

    StatusTable table = new StatusTable("test", mcStatus.getKey()+".not-modified");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsNotModified, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testExcludedNoUrls() throws Exception {
    mcStatus.setStartTime(1);
    mcStatus.setEndTime(2);
    mcStatus.setNumNotModified(3);
    mcStatus.setNumParsed(4);
    mcStatus.setUrlsExcluded(Collections.EMPTY_LIST);
    mcStatus.setAu(new MockArchivalUnit());

    addCrawlStatus(mcStatus);

    StatusTable table = new StatusTable("test", mcStatus.getKey()+".excluded");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsExcluded, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testCrawlStatusFetchedUrls() throws Exception {
    MockArchivalUnit au = new MockArchivalUnit();

    mcStatus.setStartTime(1);
    mcStatus.setEndTime(2);
    mcStatus.setUrlsFetched(ListUtil.list("http://www.example.com",
					"http://www.example.com/blah.html"));
    mcStatus.setNumParsed(4);
    mcStatus.setAu(au);

    addCrawlStatus(mcStatus);

    StatusTable table = new StatusTable("test", mcStatus.getKey()+".fetched");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsFetched, table.getColumnDescriptors());
    assertEquals("URLs fetched during crawl of MockAU",
		 table.getTitle());

    List rows = table.getSortedRows();
    assertEquals(2, rows.size());

    assertEquals(ListUtil.list("http://www.example.com",
			       "http://www.example.com/blah.html"),
		 rowUrls(rows));
  }

  List rowUrls(Collection rows) {
    List res = new ArrayList();
    for (Iterator iter = rows.iterator(); iter.hasNext(); ) {
      Map map = (Map)iter.next();
      res.add(map.get(URL));
    }
    return res;
  }

  public void testCrawlStatusExcluded() throws Exception {
    MockArchivalUnit au = new MockArchivalUnit();

    mcStatus.setStartTime(1);
    mcStatus.setEndTime(2);
    mcStatus.setUrlsExcluded(ListUtil.list("http://www.example.com",
					"http://www.example.com/blah.html"));
    mcStatus.setNumParsed(4);
    mcStatus.setAu(au);

    addCrawlStatus(mcStatus);

    StatusTable table = new StatusTable("test", mcStatus.getKey()+".excluded");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsExcluded, table.getColumnDescriptors());
    assertEquals("URLs excluded during crawl of MockAU",
		 table.getTitle());

    List rows = table.getSortedRows();
    assertEquals(2, rows.size());

    assertEquals(ListUtil.list("http://www.example.com",
			       "http://www.example.com/blah.html"),
		 rowUrls(rows));
  }

  public void testCrawlStatusExcludedWithReason() throws Exception {
    MockArchivalUnit au = new MockArchivalUnit();

    mcStatus.setStartTime(1);
    mcStatus.setEndTime(2);
    mcStatus.setUrlsExcluded(MapUtil.map("http://www.example.com",
					 "example reason",
					 "http://www.example.com/blah.html",
					 null));
    mcStatus.setNumParsed(4);
    mcStatus.setAu(au);

    addCrawlStatus(mcStatus);

    StatusTable table = new StatusTable("test", mcStatus.getKey()+".excluded");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsExcludedWithReason,
		 table.getColumnDescriptors());
    assertEquals("URLs excluded during crawl of MockAU",
		 table.getTitle());

    List<Map> rows = table.getSortedRows();
    assertEquals(2, rows.size());

    List<Map> exp =
      ListUtil.list(MapUtil.map("url", "http://www.example.com",
				"reason", "example reason",
				"ix", 1),
		    MapUtil.map("url", "http://www.example.com/blah.html",
				"ix", 2));
    assertEquals(exp, rows);
  }

  public void testCrawlStatusParsedUrls() throws Exception {
    MockArchivalUnit au = new MockArchivalUnit();

    mcStatus.setStartTime(1);
    mcStatus.setEndTime(2);
    mcStatus.setUrlsParsed(ListUtil.list("http://www.example.com",
				       "http://www.example.com/blah.html"));
//     mcStatus.setNumParsed(4);
    mcStatus.setAu(au);

    addCrawlStatus(mcStatus);

    StatusTable table = new StatusTable("test", mcStatus.getKey()+".parsed");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsParsed, table.getColumnDescriptors());
    assertEquals("URLs parsed during crawl of MockAU",
		 table.getTitle());

    List rows = table.getSortedRows();
    assertEquals(2, rows.size());

    assertEquals(ListUtil.list("http://www.example.com",
			       "http://www.example.com/blah.html"),
		 rowUrls(rows));
  }

  public void testCrawlStatusNotModifiedUrls() throws Exception {
    MockArchivalUnit au = new MockArchivalUnit();

    mcStatus.setStartTime(1);
    mcStatus.setEndTime(2);
    mcStatus.setUrlsNotModified(ListUtil.list("http://www.example.com",
					    "http://www.example.com/blah.html"));
    mcStatus.setNumParsed(4);
    mcStatus.setAu(au);

    addCrawlStatus(mcStatus);

    StatusTable table = new StatusTable("test", mcStatus.getKey()+".not-modified");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsNotModified, table.getColumnDescriptors());
    assertEquals("URLs not modified during crawl of MockAU",
		 table.getTitle());

    List rows = table.getSortedRows();
    assertEquals(2, rows.size());

    assertEquals(ListUtil.list("http://www.example.com",
			       "http://www.example.com/blah.html"),
		 rowUrls(rows));
  }

  public void testCrawlStatusErrorUrls() throws Exception {
    MockArchivalUnit au = new MockArchivalUnit();

    Map errors = new LinkedMap();
    errors.put("http://www.example.com",
	       new UrlErrorInfo("Generic error", Severity.Warning));
    errors.put("http://www.example.com/blah.html",
	       new UrlErrorInfo("Generic error2", Severity.Warning));

    mcStatus.setStartTime(1);
    mcStatus.setEndTime(2);
    mcStatus.setUrlsWithErrors(errors);
    mcStatus.setNumParsed(4);
    mcStatus.setAu(au);

    addCrawlStatus(mcStatus);

    StatusTable table = new StatusTable("test", mcStatus.getKey()+".error");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsError, table.getColumnDescriptors());
    assertEquals("Errors during crawl of MockAU",
		 table.getTitle());

    List rows = table.getSortedRows();
    assertEquals(2, rows.size());

    List res = new ArrayList();
    for (Iterator iter = rows.iterator(); iter.hasNext(); ) {
      Map map = (Map)iter.next();
      res.add(ListUtil.list(map.get(URL), map.get(CRAWL_ERROR)));
    }
    assertEquals(ListUtil.list(ListUtil.list("http://www.example.com",
					     "Generic error"),
			       ListUtil.list("http://www.example.com/blah.html",
					     "Generic error2")),
		 res);
  }
}
