/*
 * $Id: TestArcCrawler.java,v 1.4 2007-09-24 18:37:13 dshr Exp $
 */

/*

Copyright (c) 2007 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;
import java.io.*;

import org.apache.commons.collections.set.ListOrderedSet;
import org.archive.io.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.state.*;
import org.lockss.util.urlconn.*;
import org.lockss.extractor.*;

/**
 * Unit tests for the ARC file crawler.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

/*
 * XXX temporarily extends TestNewContentCrawler to make
 * XXX sure I don't break anything for non-ARC files.
 * XXX Eventually will be stand-alone and only test
 * XXX ARC file functionality.
 *
 * XXX Mostly disabled until MockArchiveReader works
 * XXX enough to make unit tests meaningful.
 */

public class TestArcCrawler extends TestNewContentCrawler {

  private static final String BASE_URL = "http://www.example.com/foo/";
  private String[] urls = {
    BASE_URL,
    BASE_URL + "one.html",
    BASE_URL + "two.txt",
    BASE_URL + "three.jpg",
    BASE_URL + "bar/four.html",
    BASE_URL + "bar/five.txt",
    BASE_URL + "bar/six.jpg",
  };
  private String[] mimeTypes = {
    "text/html",
    "text/html",
    "text/plain",
    "image/jpg",
    "text/html",
    "text/plain",
    "image/jpg",
  };
  private String[] contents = {
    "0 zero, zero",
    "1 one, one, one",
    "2 two, two, two, two",
    "3 three, three, three, three, three",
    "4 four, four, four, four, four, four",
    "5 five, five, five, five, five, five, five",
    "6 six, six, six, six, six, six, six, six",
  };

  protected BaseCrawler crawler = null;
  protected boolean streamWrapped = false;
  protected Exploder realExploder = null;
  private boolean mockArchiveReader = false;

  public void setUp() throws Exception {
    super.setUp();
    crawler = new MyArcCrawler(mau, spec, aus);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MockPermissionChecker(1));
    streamWrapped = false;
  }

  public void testMnccThrowsForNullAu() {
    try {
      crawler = new MyArcCrawler(null, spec, new MockAuState());
      fail("Constructing a ArcCrawler with a null ArchivalUnit"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMnccThrowsForNullCrawlSpec() {
    try {
      crawler = new MyArcCrawler(mau, null, new MockAuState());
      fail("Calling makeArcCrawler with a null CrawlSpec"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMnccThrowsForNullAuState() {
    try {
      crawler = new MyArcCrawler(mau, spec, null);
      fail("Calling makeArcCrawler with a null AuState"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testReturnsProperType() {
    assertEquals(Crawler.ARC, crawler.getType());
    assertEquals("ARC", crawler.getTypeString());
  }

  public void testDoCrawlOnePageWithOneArcLinkSuccessful() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.arc.gz";
    mau.addUrl(startUrl, false, true);
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1));
    mau.addUrl(url1, false, true);
    Properties p = new Properties();
    p.setProperty(ArcCrawler.PARAM_EXPLODE_ARC_FILES, "false");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    crawler.setCrawlConfig(CurrentConfig.getCurrentConfig());
    crawlRule.addUrlToCrawl(url1);
    ((MyArcCrawler)crawler).setMock(false);

    assertFalse(streamWrapped);
    assertTrue(crawler.doCrawl0());
    Set expected = SetUtil.set(permissionPage, startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
    assertTrue(streamWrapped);
  }

  public void testDoCrawlOnePageWithoutArcLinkSuccessful() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.arc.gz.foo";
    mau.addUrl(startUrl, false, true);
    extractor.addUrlsToReturn(startUrl, SetUtil.set(url1));
    mau.addUrl(url1, false, true);
    Properties p = new Properties();
    p.setProperty(ArcCrawler.PARAM_EXPLODE_ARC_FILES, "false");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    crawler.setCrawlConfig(CurrentConfig.getCurrentConfig());
    crawlRule.addUrlToCrawl(url1);
    ((MyArcCrawler)crawler).setMock(true);

    assertFalse(streamWrapped);
    assertTrue(crawler.doCrawl0());
    Set expected = SetUtil.set(permissionPage, startUrl, url1);
    assertEquals(expected, cus.getCachedUrls());
    assertFalse(streamWrapped);
  }

  // XXX should test ArchiveReader throwing, also during iteration

  private class MyArcCrawler extends ArcCrawler {

    protected MyArcCrawler(ArchivalUnit au, CrawlSpec spec,
			   AuState aus) {
      super(au, spec, aus);
      crawlStatus =
	new MyCrawlerStatus(au, ((SpiderCrawlSpec)spec).getStartingUrls(),
			    getTypeString());
      mockArchiveReader = false;
    }

    // Ordered set makes results easier to check
    protected Set newSet() {
      return new ListOrderedSet();
    }

    /** suppress these actions */
    protected void doCrawlEndActions() {
    }

    protected void setMock(boolean mock) {
      mockArchiveReader = mock;
    }

    protected Exploder getExploder(UrlCacher uc, int maxRetries) {
      Exploder ret = null;
      if (uc.getUrl().endsWith(".arc.gz")) {
	ret = new MyArcExploder(uc, maxRetries, crawlSpec, this, explodeFiles,
				storeArchive);
      }
      return ret;
    }
  }

  protected class MyArcExploder extends ArcExploder {

    protected MyArcExploder(UrlCacher uc, int maxRetries, CrawlSpec crawlSpec,
			    BaseCrawler crawler, boolean explode, boolean store) {
      super(uc, maxRetries, crawlSpec, crawler, explode, store);
    }

    protected ArchiveReader wrapStream(UrlCacher uc, InputStream arcStream)
	throws IOException {
      streamWrapped = true;
      ArchiveReader ret = null;
      if (mockArchiveReader) {
	ret = new MockArchiveReader(uc, arcStream);
	for (int i = 0; i < urls.length; i++) {
	  Map temp = new HashMap();
	  temp.put("Content-type", mimeTypes[i]);
	  try {
	    ((MockArchiveReader)ret).addArchiveRecord(urls[i],
						      contents[i],
						      temp);
	  } catch (IOException ex) {
	    fail("addArchivalRecord threw " + ex);
	  }
	}
      } else {
	ret = super.wrapStream(uc, arcStream);
      }
      return (ret);
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestArcCrawler.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }

}

