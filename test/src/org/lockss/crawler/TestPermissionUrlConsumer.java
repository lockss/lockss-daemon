/*
 * $Id: TestPermissionUrlConsumer.java 45054 2015-11-30 23:27:00Z tlipkis $
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

package org.lockss.crawler;
import java.util.*;
import java.io.*;
import java.net.*;
import junit.framework.*;

import org.lockss.crawler.PermissionRecord.PermissionStatus;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.test.MockCrawler.MockCrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.util.*;

// Tests are in two variant subclass trees: UncompressedTests and
// CompressedTests.  The latter has a subclass per compression scheme,
// currently GZipTests and DeflateTests.

public abstract class TestPermissionUrlConsumer extends LockssTestCase {

  protected static String url1 = "http://www.example.com/perm.html";

  protected static final String permContent =
    "xuzxuzxuz " +
    LockssPermission.LOCKSS_PERMISSION_STRING +
    "xuzxuzxuz";

  protected static final String notFirstPermContent =
    "xuzxuzxuz " +
    LockssPermission.LOCKSS_OJS_PERMISSION_STRING +
    "xuzxuzxuz";

  protected static final String noPermContent = "not a permission statement";


  protected MockLockssDaemon daemon;
  protected MockPlugin mplug;

  protected MockArchivalUnit mau;
  protected MockCrawlerFacade mcf;
  
  protected PermissionMap pMap;
  protected PermissionUrlConsumer puc;
  protected MockUrlFetcher muf;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    daemon.getAlertManager(); //populates AlertManager
    
    mplug = new MockPlugin(daemon);
    mplug.initPlugin(daemon);
    mau = new MockArchivalUnit(mplug, "testau");
    MockNodeManager nodeManager = new MockNodeManager();
    daemon.setNodeManager(nodeManager, mau);
    MockAuState aus = new MockAuState(mau);
    nodeManager.setAuState(aus);

    mau.addUrl(url1);
    mcf = new MockCrawler().new MockCrawlerFacade();
    mcf.setAu(mau);
    mcf.setCrawlerStatus(new MockCrawlStatus());

    muf = new MockUrlFetcher(mcf, url1);

    mcf.setPermissionUrlFetcher(muf);
    pMap = new MyPermissionMap(mcf, new LockssPermission().getCheckers(),
			       new ArrayList(), new ArrayList());
  }

  protected CIProperties encHdr() {
    return
      CIProperties.fromProperties(PropUtil.fromArgs("Content-Encoding",
						    getEncoding()));
  }


  protected abstract String getEncoding();

  protected abstract InputStream makeCompressedStream(String content)
      throws IOException;


  protected FetchedUrlData makeFud(String content) {
    return makeFud(new StringInputStream(content));
  }

  protected FetchedUrlData makeFud(String content, CIProperties props) {
    return makeFud(new StringInputStream(content), props);
  }

  protected FetchedUrlData makeFud(InputStream contentStream) {
    return makeFud(contentStream, new CIProperties());
  }

  protected FetchedUrlData makeFud(InputStream contentStream,
				   CIProperties props) {
    muf.setUncachedInputStream(contentStream);
    return new FetchedUrlData(url1, url1,
			      new BufferedInputStream(contentStream), props,
			      null, muf);
  }

  protected abstract static class CompressedTests
    extends TestPermissionUrlConsumer {

    public void testCompressedOk() throws Exception {
      FetchedUrlData fud = makeFud(makeCompressedStream(permContent), encHdr());
      PermissionUrlConsumerFactory pucf =
	new PermissionUrlConsumerFactory(pMap);
      MockUrlCacher muc1 = (MockUrlCacher)mau.makeUrlCacher(fud.getUrlData());
      muc1.setReadContent(true);
      puc = (PermissionUrlConsumer)pucf.createUrlConsumer(mcf, fud);
      puc.consume();
      assertEquals(PermissionStatus.PERMISSION_OK, pMap.getStatus(url1));
      // ensure file got stored as originally compressed
      assertSameBytes(makeCompressedStream(permContent),
		      muc1.getStoredContentStream());
    }

    public void testCompressedOkNotFirst() throws Exception {
      FetchedUrlData fud = makeFud(makeCompressedStream(notFirstPermContent),
				   encHdr());
      PermissionUrlConsumerFactory pucf =
	new PermissionUrlConsumerFactory(pMap);
      MockUrlCacher muc1 = (MockUrlCacher)mau.makeUrlCacher(fud.getUrlData());
      muc1.setReadContent(true);
      puc = (PermissionUrlConsumer)pucf.createUrlConsumer(mcf, fud);
      puc.consume();
      assertEquals(PermissionStatus.PERMISSION_OK, pMap.getStatus(url1));
      assertSameBytes(makeCompressedStream(notFirstPermContent),
		      muc1.getStoredContentStream());
    }

    public void testCompressedOkNotFirstPlusPluginPermissionChecker()
	throws Exception {
      List<PermissionChecker> plugPerms =
	ListUtil.list(new StringPermissionChecker(notFirstPermContent));
      pMap = new MyPermissionMap(mcf, new LockssPermission().getCheckers(),
				 plugPerms, new ArrayList());
      FetchedUrlData fud = makeFud(makeCompressedStream(notFirstPermContent),
				   encHdr());
      PermissionUrlConsumerFactory pucf =
	new PermissionUrlConsumerFactory(pMap);
      MockUrlCacher muc1 = (MockUrlCacher)mau.makeUrlCacher(fud.getUrlData());
      muc1.setReadContent(true);
      puc = (PermissionUrlConsumer)pucf.createUrlConsumer(mcf, fud);
      puc.consume();
      assertEquals(PermissionStatus.PERMISSION_OK, pMap.getStatus(url1));
      assertSameBytes(makeCompressedStream(notFirstPermContent),
		      muc1.getStoredContentStream());
    }

    public void testCompressedOkNotFirstPlusPluginPermissionCheckerNotFirst()
	throws Exception {
      List<PermissionChecker> plugPerms =
	ListUtil.list(new StringPermissionChecker(notFirstPermContent),
		      new StringPermissionChecker(notFirstPermContent));
      pMap = new MyPermissionMap(mcf, new LockssPermission().getCheckers(),
				 plugPerms, new ArrayList());
      FetchedUrlData fud = makeFud(makeCompressedStream(notFirstPermContent),
				   encHdr());
      PermissionUrlConsumerFactory pucf =
	new PermissionUrlConsumerFactory(pMap);
      MockUrlCacher muc1 = (MockUrlCacher)mau.makeUrlCacher(fud.getUrlData());
      muc1.setReadContent(true);
      puc = (PermissionUrlConsumer)pucf.createUrlConsumer(mcf, fud);
      puc.consume();
      assertEquals(PermissionStatus.PERMISSION_OK, pMap.getStatus(url1));
      assertSameBytes(makeCompressedStream(notFirstPermContent),
		      muc1.getStoredContentStream());
    }

    public void testCompressedNoPerm() throws Exception {
      FetchedUrlData fud =
	new FetchedUrlData(url1, url1,
			   makeCompressedStream(noPermContent),
			   encHdr(),
			   null, null);
      PermissionUrlConsumerFactory pucf =
	new PermissionUrlConsumerFactory(pMap);
      MockUrlCacher muc1 = (MockUrlCacher)mau.makeUrlCacher(fud.getUrlData());
      muc1.setReadContent(true);
      puc = (PermissionUrlConsumer)pucf.createUrlConsumer(mcf, fud);
      puc.consume();
      assertEquals(PermissionStatus.PERMISSION_NOT_OK, pMap.getStatus(url1));
      assertNull(muc1.getStoredContentBytes());
    }

    public void testCompressedNoHdr() throws Exception {
      FetchedUrlData fud =
	new FetchedUrlData(url1, url1,
			   makeCompressedStream(permContent),
			   new org.lockss.util.CIProperties(),
			   null, null);
      PermissionUrlConsumerFactory pucf =
	new PermissionUrlConsumerFactory(pMap);
      MockUrlCacher muc1 = (MockUrlCacher)mau.makeUrlCacher(fud.getUrlData());
      muc1.setReadContent(true);
      puc = (PermissionUrlConsumer)pucf.createUrlConsumer(mcf, fud);
      puc.consume();
      assertEquals(PermissionStatus.PERMISSION_NOT_OK, pMap.getStatus(url1));
      assertNull(muc1.getStoredContentBytes());
    }

    // PermissionUrlConsumer now handles spurious Content-Encoding header
    // on non-compressed response.
    public void testNotCompressedWithHdr() throws Exception {
      FetchedUrlData fud =
	new FetchedUrlData(url1, url1,
			   new StringInputStream(permContent),
			   encHdr(),
			   null, null);
      PermissionUrlConsumerFactory pucf =
	new PermissionUrlConsumerFactory(pMap);
      puc = (PermissionUrlConsumer)pucf.createUrlConsumer(mcf, fud);
      puc.consume();
      assertEquals(PermissionStatus.PERMISSION_OK, pMap.getStatus(url1));
    }

  }

  protected static class MyPermissionMap extends PermissionMap {
    public MyPermissionMap(Crawler.CrawlerFacade crawlFacade,
			 Collection<PermissionChecker> daemonPermissionCheckers,
			 Collection<PermissionChecker> pluginPermissionCheckers,
			 Collection<String> permUrls) {
      super(crawlFacade,
	    daemonPermissionCheckers,
	    pluginPermissionCheckers,
	    permUrls);
    }

    protected PermissionRecord get(String url) throws MalformedURLException{
      PermissionRecord res = super.get(url);
      if (res == null) {
	res = createRecord(url);
      }
      return res;
    }
  }

  public static class UncompressedTests extends TestPermissionUrlConsumer {

    public UncompressedTests() {
    }

    // Don't need these but they need to be defined
    protected String getEncoding() {
      throw new IllegalStateException();
    }

    protected InputStream makeCompressedStream(String content) {
      throw new IllegalStateException();
    }

    public void testUncompressedOk() throws Exception {
      FetchedUrlData fud = makeFud(permContent);
      PermissionUrlConsumerFactory pucf =
	new PermissionUrlConsumerFactory(pMap);
      log.critical("fud: " + fud);
      log.critical("urldata: " + fud.getUrlData());
      log.critical("input: " + fud.getUrlData().input);
      MockUrlCacher muc1 = (MockUrlCacher)mau.makeUrlCacher(fud.getUrlData());
      muc1.setReadContent(true);

      puc = (PermissionUrlConsumer)pucf.createUrlConsumer(mcf, fud);
      puc.consume();
      assertEquals(PermissionStatus.PERMISSION_OK, pMap.getStatus(url1));
      assertInputStreamMatchesString(permContent,
				     muc1.getStoredContentStream());
    }

    public void testUncompressedOkNotFirst() throws Exception {
      FetchedUrlData fud = makeFud(notFirstPermContent);
      PermissionUrlConsumerFactory pucf =
	new PermissionUrlConsumerFactory(pMap);
      MockUrlCacher muc1 = (MockUrlCacher)mau.makeUrlCacher(fud.getUrlData());
      muc1.setReadContent(true);

      puc = (PermissionUrlConsumer)pucf.createUrlConsumer(mcf, fud);
      puc.consume();
      assertEquals(PermissionStatus.PERMISSION_OK, pMap.getStatus(url1));
      assertInputStreamMatchesString(notFirstPermContent,
				     muc1.getStoredContentStream());
    }

    public void testUncompressedOkNotFirstPlusPluginPermissionChecker()
	throws Exception {
      List<PermissionChecker> plugPerms =
	ListUtil.list(new StringPermissionChecker(notFirstPermContent));
      pMap = new MyPermissionMap(mcf, new LockssPermission().getCheckers(),
				 plugPerms, new ArrayList());
      FetchedUrlData fud = makeFud(notFirstPermContent);
      PermissionUrlConsumerFactory pucf =
	new PermissionUrlConsumerFactory(pMap);
      MockUrlCacher muc1 = (MockUrlCacher)mau.makeUrlCacher(fud.getUrlData());
      muc1.setReadContent(true);

      puc = (PermissionUrlConsumer)pucf.createUrlConsumer(mcf, fud);
      puc.consume();
      assertEquals(PermissionStatus.PERMISSION_OK, pMap.getStatus(url1));
      assertInputStreamMatchesString(notFirstPermContent,
				     muc1.getStoredContentStream());
    }

    public void testUncompressedOkNotFirstPlusPluginPermissionCheckerNotFirst()
	throws Exception {
      List<PermissionChecker> plugPerms =
	ListUtil.list(new StringPermissionChecker(notFirstPermContent),
		      new StringPermissionChecker(notFirstPermContent));
      pMap = new MyPermissionMap(mcf, new LockssPermission().getCheckers(),
				 plugPerms, new ArrayList());
      FetchedUrlData fud = makeFud(notFirstPermContent);
      PermissionUrlConsumerFactory pucf =
	new PermissionUrlConsumerFactory(pMap);
      MockUrlCacher muc1 = (MockUrlCacher)mau.makeUrlCacher(fud.getUrlData());
      muc1.setReadContent(true);

      puc = (PermissionUrlConsumer)pucf.createUrlConsumer(mcf, fud);
      puc.consume();
      assertEquals(PermissionStatus.PERMISSION_OK, pMap.getStatus(url1));
      assertInputStreamMatchesString(notFirstPermContent,
				     muc1.getStoredContentStream());
    }

    public void testUncompressedNoPerm() throws Exception {
      FetchedUrlData fud = makeFud(new StringInputStream(noPermContent));
      PermissionUrlConsumerFactory pucf =
	new PermissionUrlConsumerFactory(pMap);
      MockUrlCacher muc1 = (MockUrlCacher)mau.makeUrlCacher(fud.getUrlData());
      muc1.setReadContent(true);
      puc = (PermissionUrlConsumer)pucf.createUrlConsumer(mcf, fud);
      puc.consume();
      assertEquals(PermissionStatus.PERMISSION_NOT_OK, pMap.getStatus(url1));
      assertNull(muc1.getStoredContentBytes());
    }
  }

  public static class GZipTests extends CompressedTests  {
    public GZipTests() {
    }

    protected String getEncoding() {
      return "gzip";
    }

    protected InputStream makeCompressedStream(String content)
	throws IOException {
      return new GZIPpedInputStream(content);
    }
  }

  public static class DeflateTests extends CompressedTests {
    public DeflateTests() {
    }

    protected String getEncoding() {
      return "deflate";
    }

    protected InputStream makeCompressedStream(String content)
	throws IOException {
      return new DeflatedInputStream(content);
    }
  }


  public static Test suite() {
    return variantSuites(new Class[] {UncompressedTests.class,
				      GZipTests.class,
				      DeflateTests.class});
  }
}
