/*
 * $Id: TestSingleCrawlStatus.java,v 1.1 2005-01-11 01:56:43 troberts Exp $
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

  private static List expectedColDescs =
    ListUtil.list(new ColumnDescriptor(URL, "URL Fetched",
				       ColumnDescriptor.TYPE_STRING));



  public void setUp() throws Exception {
    super.setUp();
    cmStatus = new MockCrawlManagerStatus();
    cStatus = new SingleCrawlStatus(cmStatus);
  }


  public void testCrawlStatusFetchedUrlsRequiresKey() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    assertTrue(cStatus.requiresKey());
  }

  public void testCrawlStatusFetchedUrlsDisplayName() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    assertEquals("URLs Fetched", cStatus.getDisplayName());
  }

  public void testFetchedUrlsPopulateTableNullTable() {
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
    assertEquals(expectedColDescs, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testCrawlStatusErrorUrlsNoUrls() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsFetched(SetUtil.set());
    status.setNumParsed(4);
    status.setAu(new MockArchivalUnit());
    
    cmStatus.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    StatusTable table = new StatusTable("test", "error;0");

    cStatus.populateTable(table);
    assertEquals(expectedColDescs, table.getColumnDescriptors());

    assertEquals(0, table.getSortedRows().size());
  }

  public void testCrawlStatusFetchedUrls() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    
    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setUrlsFetched(SetUtil.set("http://www.example.com",
				      "http://www.example.com/blah.html"));
    status.setNumParsed(4);
    status.setAu(new MockArchivalUnit());
    
    cmStatus.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    StatusTable table = new StatusTable("test", "fetched;0");

    cStatus.populateTable(table);
    assertEquals(expectedColDescs, table.getColumnDescriptors());

    List rows = table.getSortedRows();
    assertEquals(2, rows.size());

    Map map = (Map)rows.get(0);
    assertEquals("http://www.example.com", map.get(URL));

    map = (Map)rows.get(1);
    assertEquals("http://www.example.com/blah.html", map.get(URL));
  }
  /*
  public void testCrawlStatusFetchedUrls() {
    MockCrawlManagerStatus cmStatus = new MockCrawlManagerStatus();
    
    MockCrawlStatus status = new MockCrawlStatus();
    status.setStartTime(1);
    status.setEndTime(2);
    status.setErrorUrls(SetUtil.set("http://www.example.com",
    "http://www.example.com/blah.html"));
    status.setNumParsed(4);
    status.setAu(new MockArchivalUnit());
    
    cmStatus.addStatusObject(status);

    SingleCrawlStatus cStatus = new SingleCrawlStatus(cmStatus);
    StatusTable table = new StatusTable("test", "fetched;0");

    cStatus.populateTable(table);
    assertEquals(expectedColDescs, table.getColumnDescriptors());

    List rows = table.getSortedRows();
    assertEquals(2, rows.size());

    Map map = (Map)rows.get(0);
    assertEquals("http://www.example.com", map.get(URL));

    map = (Map)rows.get(1);
    assertEquals("http://www.example.com/blah.html", map.get(URL));
  }
  */
}
