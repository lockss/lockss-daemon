/*
 * $Id$
 */

/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.base;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

import org.lockss.plugin.*;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.repository.*;
import org.lockss.crawler.*;
import org.lockss.crawler.PermissionRecord.PermissionStatus;
import org.lockss.config.*;

public class TestSimpleUrlConsumer extends LockssTestCase {

  protected static Logger logger = Logger.getLogger("TestBaseUrlFetcher");


  private MockLockssDaemon theDaemon;
  private MockPlugin plugin;
  private MockNodeManager nodeMgr = new MockNodeManager();
  private MockArchivalUnit mau;
  private MockAuState aus;
  private MySimpleUrlConsumerFactory ucfact = new MySimpleUrlConsumerFactory();

  private static final String TEST_URL = "http://www.example.com/testDir/leaf1";

  public void setUp() throws Exception {
    super.setUp();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
				  tempDirPath);
    theDaemon = getMockLockssDaemon();

    mau = new MockArchivalUnit();

    plugin = new MockPlugin();
    plugin.initPlugin(theDaemon);
    mau.setPlugin(plugin);

    nodeMgr = new MockNodeManager();
    theDaemon.setNodeManager(nodeMgr, mau);
    aus = new MockAuState(mau);
    nodeMgr.setAuState(aus);
  }

  // Ensure SimpleUrlConsumer uses the CrawlFacade's makeUrlCacher()
  public void testUrlCacherCreation() throws IOException {
    mau.addUrl(TEST_URL);
    TestableBaseCrawler crawler = new TestableBaseCrawler(mau, aus);
    Crawler.CrawlerFacade crawlFacade =
      new BaseCrawler.BaseCrawlerFacade(crawler);
    LockssWatchdog wdog = new MockLockssWatchdog();
    crawler.setWatchdog(wdog);
    FetchedUrlData fud = 
      new FetchedUrlData(TEST_URL, TEST_URL,
			 new StringInputStream("test stream"),
			 new CIProperties(), null, null/*this*/);
    MySimpleUrlConsumer con =
      (MySimpleUrlConsumer)ucfact.createUrlConsumer(crawlFacade, fud);
    con.consume();
    UrlCacher uc = con.getUrlCacher();
    assertSame(wdog, uc.getWatchdog());
  }


  private class MyMockLockssUrlConnection extends MockLockssUrlConnection {
    String proxyHost = null;
    int proxyPort = -1;
    IPAddr localAddr = null;
    String username;
    String password;
    String cpolicy;
    String headerCharset;
    List<List<String>> cookies = new ArrayList<List<String>>();

    public MyMockLockssUrlConnection() throws IOException {
      super();
    }

    public MyMockLockssUrlConnection(String url) throws IOException {
      super(url);
    }

    public void setProxy(String host, int port) {
      proxyHost = host;
      proxyPort = port;
    }

    public void setLocalAddress(IPAddr addr) {
      localAddr = addr;
    }

    public void setCredentials(String username, String password) {
      this.username = username;
      this.password = password;
    }

    public void setCookiePolicy(String policy) {
      cpolicy = policy;
    }

    String getCookiePolicy() {
      return cpolicy;
    }

    public void addCookie(String domain, String path,
			  String name, String value) {
      cookies.add(ListUtil.list(domain, path, name, value));
    }

    public List<List<String>> getCookies() {
      return cookies;
    }

    public void setHeaderCharset(String charset) {
      headerCharset = charset;
    }

  }

  private class ThrowingMockLockssUrlConnection extends MockLockssUrlConnection {
    IOException ex;

    public ThrowingMockLockssUrlConnection(IOException ex) throws IOException {
      super();
      this.ex = ex;
    }

    public void execute() throws IOException {
      throw ex;
    }
  }

  class MyStringInputStreamMarkNotSupported extends MyStringInputStream {
    public MyStringInputStreamMarkNotSupported(String str) {
      super(str);
    }

    public boolean markSupported() {
      return false;
    }
  }

  class MyStringInputStream extends StringInputStream {
    private boolean resetWasCalled = false;
    private boolean markWasCalled = false;
    private boolean closeWasCalled = false;
    private IOException resetEx;

    private int buffSize = -1;

    public MyStringInputStream(String str) {
      super(str);
    }

    /**
     * @param str String to read from
     * @param resetEx IOException to throw when reset is called
     *
     * Same as one arg constructor, but can provide an exception that is thrown
     * when reset is called
     */
    public MyStringInputStream(String str, IOException resetEx) {
      super(str);
      this.resetEx = resetEx;
    }

    public void reset() throws IOException {
      resetWasCalled = true;
      if (resetEx != null) {
        throw resetEx;
      }
      super.reset();
    }

    public boolean resetWasCalled() {
      return resetWasCalled;
    }

    public void mark(int buffSize) {
      markWasCalled = true;
      this.buffSize = buffSize;
      super.mark(buffSize);
    }

    public boolean markWasCalled() {
      return markWasCalled;
    }

    public int getMarkBufferSize() {
      return this.buffSize;
    }

    public void close() throws IOException {
      Exception ex = new Exception("Blah");
      logger.debug3("Close called on " + this, ex);
      closeWasCalled = true;
      super.close();
    }

    public boolean closeWasCalled() {
      return closeWasCalled;
    }

  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestBaseUrlFetcher.class.getName() };
    junit.swingui.TestRunner.main(testCaseList);
  }

  private static class MockPermissionMap extends PermissionMap {
    public MockPermissionMap() {
      super(new MockCrawler().new MockCrawlerFacade(),
	    new ArrayList(), new ArrayList(), null);
    }

    protected void putStatus(String permissionUrl, PermissionStatus status)
            throws MalformedURLException {
      super.createRecord(permissionUrl).setStatus(status);
    }

  }

  private class TestableBaseCrawler extends BaseCrawler {

    protected TestableBaseCrawler(ArchivalUnit au, AuState aus) {
      super(au, aus);
      crawlStatus = new MockCrawlStatus();
    }

    public Crawler.Type getType() {
      throw new UnsupportedOperationException("not implemented");
    }

    public String getTypeString() {
      return "Testable";
    }

    public boolean isWholeAU() {
      return true;
    }

    protected boolean doCrawl0() {
      return true;
    }
  }

  static class MySimpleUrlConsumer extends SimpleUrlConsumer {
    public MySimpleUrlConsumer(Crawler.CrawlerFacade crawlFacade,
			       FetchedUrlData fud){
      super(crawlFacade, fud);
    }

    UrlCacher getUrlCacher() {
      return cacher;
    }

  }

  static class MySimpleUrlConsumerFactory implements UrlConsumerFactory {
    public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade crawlFacade,
					 FetchedUrlData fud) {
      return new MySimpleUrlConsumer(crawlFacade, fud);
    }
  }
}
