/*
 * $Id: TestSingleCrawlStatus.java,v 1.6 2006-04-11 08:33:33 tlipkis Exp $
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
import org.lockss.daemon.status.*;

public class TestSingleCrawlStatus extends LockssTestCase {
  MockCrawlManagerStatusAccessor cmStatusAcc;
  SingleCrawlStatus cStatus;


  private static final String URL = "url";
  private static final String CRAWL_ERROR = "crawl_error";

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
    ListUtil.list(new ColumnDescriptor(URL, "URL",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor(CRAWL_ERROR, "Error",
				       ColumnDescriptor.TYPE_STRING));


  public void setUp() throws Exception {
    super.setUp();
    cmStatusAcc = new MockCrawlManagerStatusAccessor();
    cStatus = new SingleCrawlStatus(cmStatusAcc);
    MockCrawlManagerStatusAccessor cmStatusAcc =
      new MockCrawlManagerStatusAccessor();
  }

  public void testCrawlStatusDisplayName() {
    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatusAcc);
    try {
      cStatus.getDisplayName();
      fail("getDisplayName should throw");
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testRequiresKey() {
    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatusAcc);
    assertTrue(cStatus.requiresKey());
  }

  public void testPopulateTableNullTable() throws Exception {
    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatusAcc);
    try {
      cStatus.populateTable(null);
      fail("Should have thrown an IllegalArgumentException for a null table");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testFetchedUrlsThrowsWOKey() throws Exception {
    StatusTable table = new StatusTable("test");
    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatusAcc);
    try {
      cStatus.populateTable(table);
      fail("Should have thrown an IllegalArgumentException when called without"
	   +" a key");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testCrawlStatusFetchedUrlsNoUrls() throws Exception {
    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsFetched(ListUtil.list());
    status.setNumParsed(4);
    status.setAu(new MockArchivalUnit());

    cmStatusAcc.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatusAcc);
    StatusTable table = new StatusTable("test", "fetched."+status.getKey());

    cStatus.populateTable(table);
    assertEquals(expectedColDescsFetched, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testCrawlStatusParsedUrlsNoUrls() throws Exception {
    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setNumFetched(3);
    status.setUrlsParsed(ListUtil.list());
    status.setAu(new MockArchivalUnit());

    cmStatusAcc.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatusAcc);
    StatusTable table = new StatusTable("test", "parsed."+status.getKey());

    cStatus.populateTable(table);
    assertEquals(expectedColDescsParsed, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testCrawlStatusErrorUrlsNoUrls() throws Exception {
    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsWithErrors(new HashMap());
    status.setNumParsed(4);
    status.setAu(new MockArchivalUnit());

    cmStatusAcc.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatusAcc);
    StatusTable table = new StatusTable("test", "error."+status.getKey());

    cStatus.populateTable(table);
    assertEquals(expectedColDescsError, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testNotModifiedNoUrls() throws Exception {
    MockCrawlManagerStatusAccessor cmStatusAcc = new MockCrawlManagerStatusAccessor();
    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsNotModified(new HashSet());
    status.setNumParsed(4);
    status.setAu(new MockArchivalUnit());

    cmStatusAcc.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatusAcc);
    StatusTable table = new StatusTable("test", "not-modified."+status.getKey());

    cStatus.populateTable(table);
    assertEquals(expectedColDescsNotModified, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testCrawlStatusFetchedUrls() throws Exception {
    MockCrawlManagerStatusAccessor cmStatusAcc = new MockCrawlManagerStatusAccessor();

    MockCrawlStatus status = new MockCrawlStatus();
    MockArchivalUnit au = new MockArchivalUnit();
    au.setName("Mock name");

    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsFetched(ListUtil.list("http://www.example.com",
					"http://www.example.com/blah.html"));
    status.setNumParsed(4);
    status.setAu(au);

    cmStatusAcc.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatusAcc);
    StatusTable table = new StatusTable("test", "fetched."+status.getKey());

    cStatus.populateTable(table);
    assertEquals(expectedColDescsFetched, table.getColumnDescriptors());
    assertEquals("URLs fetched during crawl of Mock name",
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

  public void testCrawlStatusParsedUrls() throws Exception {
    MockCrawlManagerStatusAccessor cmStatusAcc = new MockCrawlManagerStatusAccessor();

    MockCrawlStatus status = new MockCrawlStatus();
    MockArchivalUnit au = new MockArchivalUnit();
    au.setName("Mock name");

    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsParsed(ListUtil.list("http://www.example.com",
				       "http://www.example.com/blah.html"));
//     status.setNumParsed(4);
    status.setAu(au);

    cmStatusAcc.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatusAcc);
    StatusTable table = new StatusTable("test", "parsed."+status.getKey());

    cStatus.populateTable(table);
    assertEquals(expectedColDescsParsed, table.getColumnDescriptors());
    assertEquals("URLs parsed during crawl of Mock name",
		 table.getTitle());

    List rows = table.getSortedRows();
    assertEquals(2, rows.size());

    assertEquals(ListUtil.list("http://www.example.com",
			       "http://www.example.com/blah.html"),
		 rowUrls(rows));
  }

  public void testCrawlStatusNotModifiedUrls() throws Exception {
    MockCrawlManagerStatusAccessor cmStatusAcc = new MockCrawlManagerStatusAccessor();

    MockCrawlStatus status = new MockCrawlStatus();
    MockArchivalUnit au = new MockArchivalUnit();
    au.setName("Mock name");

    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsNotModified(ListUtil.list("http://www.example.com",
					    "http://www.example.com/blah.html"));
    status.setNumParsed(4);
    status.setAu(au);

    cmStatusAcc.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatusAcc);
    StatusTable table = new StatusTable("test", "not-modified."+status.getKey());

    cStatus.populateTable(table);
    assertEquals(expectedColDescsNotModified, table.getColumnDescriptors());
    assertEquals("URLs not modified during crawl of Mock name",
		 table.getTitle());

    List rows = table.getSortedRows();
    assertEquals(2, rows.size());

    assertEquals(ListUtil.list("http://www.example.com",
			       "http://www.example.com/blah.html"),
		 rowUrls(rows));
  }

  public void testCrawlStatusErrorUrls() throws Exception {
    MockCrawlManagerStatusAccessor cmStatusAcc = new MockCrawlManagerStatusAccessor();

    MockCrawlStatus status = new MockCrawlStatus();
    MockArchivalUnit au = new MockArchivalUnit();
    au.setName("Mock name");

    Map errors = new LinkedMap();
    errors.put("http://www.example.com", "Generic error");
    errors.put("http://www.example.com/blah.html", "Generic error2");

    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsWithErrors(errors);
    status.setNumParsed(4);
    status.setAu(au);

    cmStatusAcc.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatusAcc);
    StatusTable table = new StatusTable("test", "error."+status.getKey());

    cStatus.populateTable(table);
    assertEquals(expectedColDescsError, table.getColumnDescriptors());
    assertEquals("Errors during crawl of Mock name",
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
