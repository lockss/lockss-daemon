/*
 * $Id: TestSingleCrawlStatus.java,v 1.3 2005-01-14 01:37:41 troberts Exp $
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
// import org.lockss.app.*;
import org.lockss.test.*;
import org.lockss.util.*;
// import org.lockss.crawler.*;
// import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
// import org.lockss.plugin.*;

public class TestSingleCrawlStatus extends LockssTestCase {
  MockCrawlManagerStatus cmStatus;
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
    cmStatus = new MockCrawlManagerStatus();
    cStatus = new SingleCrawlStatus(cmStatus);
  }

  public void testCrawlStatusDisplayName() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);

    try {
      cStatus.getDisplayName();
      fail("getDisplayName should throw");
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testRequiresKey() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    assertTrue(cStatus.requiresKey());
  }

  public void testPopulateTableNullTable() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    try {
      cStatus.populateTable(null);
      fail("Should have thrown an IllegalArgumentException for a null table");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testFetchedUrlsThrowsWOKey() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    StatusTable table = new StatusTable("test");
    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    try {
      cStatus.populateTable(table);
      fail("Should have thrown an IllegalArgumentException when called without"
	   +" a key");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testCrawlStatusFetchedUrlsNoUrls() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsFetched(SetUtil.set());
    status.setNumParsed(4);
    status.setAu(new MockArchivalUnit());
    
    cmStatus.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    StatusTable table = new StatusTable("test", "fetched;0");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsFetched, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testCrawlStatusParsedUrlsNoUrls() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setNumFetched(3);
    status.setUrlsParsed(SetUtil.set());
    status.setAu(new MockArchivalUnit());
    
    cmStatus.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    StatusTable table = new StatusTable("test", "parsed;0");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsParsed, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testCrawlStatusErrorUrlsNoUrls() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsWithErrors(new HashMap());
    status.setNumParsed(4);
    status.setAu(new MockArchivalUnit());
    
    cmStatus.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    StatusTable table = new StatusTable("test", "error;0");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsError, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testNotModifiedNoUrls() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsNotModified(new HashSet());
    status.setNumParsed(4);
    status.setAu(new MockArchivalUnit());
    
    cmStatus.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    StatusTable table = new StatusTable("test", "not-modified;0");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsNotModified, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testCrawlStatusFetchedUrls() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    
    MockCrawlStatus status = new MockCrawlStatus();
    MockArchivalUnit au = new MockArchivalUnit();
    au.setName("Mock name");

    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsFetched(SetUtil.set("http://www.example.com",
				      "http://www.example.com/blah.html"));
    status.setNumParsed(4);
    status.setAu(au);
    
    cmStatus.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    StatusTable table = new StatusTable("test", "fetched;0");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsFetched, table.getColumnDescriptors());
    assertEquals("URLs fetched during crawl of Mock name",
		 table.getTitle());

    List rows = table.getSortedRows();
    assertEquals(2, rows.size());

    Map map = (Map)rows.get(0);
    assertEquals("http://www.example.com", map.get(URL));

    map = (Map)rows.get(1);
    assertEquals("http://www.example.com/blah.html", map.get(URL));
  }

  public void testCrawlStatusParsedUrls() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    
    MockCrawlStatus status = new MockCrawlStatus();
    MockArchivalUnit au = new MockArchivalUnit();
    au.setName("Mock name");

    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsParsed(SetUtil.set("http://www.example.com",
				     "http://www.example.com/blah.html"));
//     status.setNumParsed(4);
    status.setAu(au);
    
    cmStatus.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    StatusTable table = new StatusTable("test", "parsed;0");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsParsed, table.getColumnDescriptors());
    assertEquals("URLs parsed during crawl of Mock name",
		 table.getTitle());

    List rows = table.getSortedRows();
    assertEquals(2, rows.size());

    Map map = (Map)rows.get(0);
    assertEquals("http://www.example.com", map.get(URL));

    map = (Map)rows.get(1);
    assertEquals("http://www.example.com/blah.html", map.get(URL));
  }

  public void testCrawlStatusNotModifiedUrls() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    
    MockCrawlStatus status = new MockCrawlStatus();
    MockArchivalUnit au = new MockArchivalUnit();
    au.setName("Mock name");

    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsNotModified(SetUtil.set("http://www.example.com",
					  "http://www.example.com/blah.html"));
    status.setNumParsed(4);
    status.setAu(au);
    
    cmStatus.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    StatusTable table = new StatusTable("test", "not-modified;0");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsNotModified, table.getColumnDescriptors());
    assertEquals("URLs not modified during crawl of Mock name",
		 table.getTitle());

    List rows = table.getSortedRows();
    assertEquals(2, rows.size());

    Map map = (Map)rows.get(0);
    assertEquals("http://www.example.com", map.get(URL));

    map = (Map)rows.get(1);
    assertEquals("http://www.example.com/blah.html", map.get(URL));
  }

  public void testCrawlStatusErrorUrls() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    
    MockCrawlStatus status = new MockCrawlStatus();
    MockArchivalUnit au = new MockArchivalUnit();
    au.setName("Mock name");
    
    Map errors = new HashMap();
    errors.put("http://www.example.com", "Generic error");
    errors.put("http://www.example.com/blah.html", "Generic error2");

    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsWithErrors(errors);
    status.setNumParsed(4);
    status.setAu(au);
    
    cmStatus.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    StatusTable table = new StatusTable("test", "error;0");

    cStatus.populateTable(table);
    assertEquals(expectedColDescsError, table.getColumnDescriptors());
    assertEquals("Errors during crawl of Mock name",
		 table.getTitle());

    List rows = table.getSortedRows();
    assertEquals(2, rows.size());

    Map map = (Map)rows.get(0);
    assertEquals("http://www.example.com", map.get(URL));
    assertEquals("Generic error", map.get(CRAWL_ERROR));

    map = (Map)rows.get(1);
    assertEquals("http://www.example.com/blah.html", map.get(URL));
    assertEquals("Generic error2", map.get(CRAWL_ERROR));
  }

}
