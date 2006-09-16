/*
 * $Id: TestBaseCachedUrl.java,v 1.17 2006-09-16 22:55:22 tlipkis Exp $
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

package org.lockss.plugin.base;

import java.io.*;
import java.util.*;
import java.math.BigInteger;
import junit.framework.*;
import org.lockss.plugin.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.repository.*;

/** Variants test "current" BaseCachedUrl and version-specific
 * BaseCachedUrl instances */
public class TestBaseCachedUrl extends LockssTestCase {
  private static final String PARAM_SHOULD_FILTER_HASH_STREAM =
    Configuration.PREFIX+"baseCachedUrl.filterHashStream";

  protected LockssRepository repo;
  protected MockArchivalUnit mau;
  protected MockLockssDaemon theDaemon;
  protected MockPlugin plugin;

  String url1 = "http://www.example.com/testDir/leaf1";
  String url2 = "http://www.example.com/testDir/leaf2";
  String url3 = "http://www.example.com/testDir/leaf3";
  String urlparent = "http://www.example.com/testDir";
  String content1 = "test content 1";
  String content2 = "test content 2 longer";
  String badcontent = "this is the wrong content string";

  TestBaseCachedUrl() {
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    CIProperties props = new CIProperties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();

    mau = new MyMockArchivalUnit();
    plugin = new MockPlugin();
    plugin.initPlugin(theDaemon);
    mau.setPlugin(plugin);

    repo = theDaemon.getLockssRepository(mau);
    theDaemon.getNodeManager(mau);
  }

  public void tearDown() throws Exception {
    repo.stopService();
    super.tearDown();
  }

  /** Tests that are independent of versioning */
  public static class NotVersionedTests extends TestBaseCachedUrl {
    public NotVersionedTests() {
    }

    public void testFilterParamDefault() {
      MyCachedUrl cu = new MyCachedUrl(new MyAu(), null);
      cu.openForHashing();
      assertTrue(cu.gotFilteredStream);
    }

    public void testFilterParamFilterOn() throws IOException {
      String config = PARAM_SHOULD_FILTER_HASH_STREAM+"=true";
      ConfigurationUtil.setCurrentConfigFromString(config);
      MyCachedUrl cu = new MyCachedUrl(new MyAu(), null);
      cu.openForHashing();
      assertTrue(cu.gotFilteredStream);
    }

    public void testFilterParamFilterOff() throws IOException {
      String config = PARAM_SHOULD_FILTER_HASH_STREAM+"=false";
      ConfigurationUtil.setCurrentConfigFromString(config);
      MyCachedUrl cu = new MyCachedUrl(new MyAu(), null);
      cu.openForHashing();
      assertFalse(cu.gotFilteredStream);
    }
  }

  // helper for above tests
  private class MyCachedUrl extends BaseCachedUrl {
    private boolean gotUnfilteredStream = false;
    private boolean gotFilteredStream = false;
    private CIProperties props = new CIProperties();

    public MyCachedUrl(ArchivalUnit au, String url) {
      super(au, url);
      props.setProperty(PROPERTY_CONTENT_TYPE, "text/html");
    }


    public InputStream getUnfilteredInputStream() {
      gotUnfilteredStream = true;
      return null;
    }

    public boolean gotUnfilteredStream() {
      return gotUnfilteredStream;
    }

    protected InputStream getFilteredStream() {
      gotFilteredStream = true;
      return super.getFilteredStream();
    }

    public boolean hasContent() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public Reader openForReading() {
      return new StringReader("Test");
    }

    public CIProperties getProperties() {
      return props;
    }

    public void setProperties(CIProperties props) {
      this.props = props;
    }
  }

  /** Tests that run with the current version and with an older version */
  public abstract static class VersionedTests extends TestBaseCachedUrl {
    public VersionedTests() {
    }

    /** Concrete class must create either a single version or multiple
     * versions here */
    abstract void createLeaf(String url, String content, CIProperties props)
	throws Exception;

    /** Concrete class must return either the current version or an older
     * version here */
    abstract CachedUrl getTestCu(String url);

    public void testGetUrl() throws Exception {
      createLeaf(url1, content1, null);

      CachedUrl cu = getTestCu(url1);
      assertEquals(url1, cu.getUrl());
    }

    public void testIsLeaf() throws Exception {
      createLeaf(url1, content1, null);
      createLeaf(url2, null, null);

      CachedUrl cu = getTestCu(url1);
      assertTrue(cu.isLeaf());
      cu = getTestCu(url2);
      assertTrue(cu.isLeaf());
    }

    public void testOpenForReading() throws Exception {
      createLeaf(url1, content1, null);
      createLeaf(url2, content2, null);
      createLeaf(url3, "", null);

      CachedUrl cu = getTestCu(url1);
      InputStream urlIs = cu.getUnfilteredInputStream();
      assertEquals(content1, StringUtil.fromInputStream(urlIs));

      cu = getTestCu(url2);
      urlIs = cu.getUnfilteredInputStream();
      assertEquals(content2, StringUtil.fromInputStream(urlIs));

      cu = getTestCu(url3);
      urlIs = cu.getUnfilteredInputStream();
      assertEquals("", StringUtil.fromInputStream(urlIs));
    }

    public void testOpenForHashingDefaultsToNoFiltering() throws Exception {
      createLeaf(url1, "<test stream>", null);
      String str = "This is a filtered stream";
      mau.setFilterRule(new MyMockFilterRule(new StringReader(str)));

      CachedUrl cu = getTestCu(url1);
      InputStream urlIs = cu.getUnfilteredInputStream();
      assertNotEquals(str, StringUtil.fromInputStream(urlIs));

      mau.setFilterFactory(new MyMockFilterFactory(new StringInputStream(str)));
      urlIs = cu.getUnfilteredInputStream();
      assertNotEquals(str, StringUtil.fromInputStream(urlIs));
    }

    public void testOpenForHashingWontFilterIfConfiguredNotTo()
	throws Exception {
      String config = PARAM_SHOULD_FILTER_HASH_STREAM+"=false";
      ConfigurationUtil.setCurrentConfigFromString(config);
      createLeaf(url1, "<test stream>", null);
      String str = "This is a filtered stream";
      mau.setFilterRule(new MyMockFilterRule(new StringReader(str)));

      CachedUrl cu = getTestCu(url1);
      InputStream urlIs = cu.openForHashing();
      assertNotEquals(str, StringUtil.fromInputStream(urlIs));

      mau.setFilterFactory(new MyMockFilterFactory(new StringInputStream(str)));
      urlIs = cu.openForHashing();
      assertNotEquals(str, StringUtil.fromInputStream(urlIs));
    }

    public void testOpenForHashingUsesFilterRule()
	throws Exception {
      String config = PARAM_SHOULD_FILTER_HASH_STREAM + "=true";
      ConfigurationUtil.setCurrentConfigFromString(config);
      createLeaf(url1, "blah <test stream>", null);
      String str = "This is a filtered stream";
      mau.setFilterRule(new MyMockFilterRule(new StringReader(str)));

      CachedUrl cu = getTestCu(url1);
      InputStream urlIs = cu.openForHashing();
      assertEquals(str, StringUtil.fromInputStream(urlIs));
    }

    public void testOpenForHashingUsesFilterFactory()
	throws Exception {
      String config = PARAM_SHOULD_FILTER_HASH_STREAM + "=true";
      ConfigurationUtil.setCurrentConfigFromString(config);
      createLeaf(url1, "blah <test stream>", null);
      String str = "This is a filtered stream";
      mau.setFilterFactory(new MyMockFilterFactory(new StringInputStream(str)));

      CachedUrl cu = getTestCu(url1);
      InputStream urlIs = cu.openForHashing();
      assertEquals(str, StringUtil.fromInputStream(urlIs));
    }

    public void testOpenForHashingUsesFilterFactoryBeforeRule()
	throws Exception {
      String config = PARAM_SHOULD_FILTER_HASH_STREAM + "=true";
      ConfigurationUtil.setCurrentConfigFromString(config);
      createLeaf(url1, "blah <test stream>", null);
      String strRule = "This is a filtered stream";
      mau.setFilterRule(new MyMockFilterRule(new StringReader(strRule)));
      String strFact = "This is a filtered stream";
      mau.setFilterFactory(new MyMockFilterFactory(new StringInputStream(strFact)));

      CachedUrl cu = getTestCu(url1);
      InputStream urlIs = cu.openForHashing();
      assertEquals(strFact, StringUtil.fromInputStream(urlIs));
    }

    public void testGetContentSize() throws Exception {
      createLeaf(url1, content1, null);
      createLeaf(url2, content2, null);
      createLeaf(url3, "", null);

      CachedUrl cu = getTestCu(url1);
      assertEquals(content1.length(), cu.getContentSize());

      cu = getTestCu(url2);
      assertEquals(content2.length(), cu.getContentSize());

      cu = getTestCu(url3);
      assertEquals(0, cu.getContentSize());
    }

    public void testGetProperties() throws Exception {
      CIProperties newProps = new CIProperties();
      newProps.setProperty("test", "value");
      newProps.setProperty("test2", "value2");
      createLeaf(url1, null, newProps);

      CachedUrl cu = getTestCu(url1);
      CIProperties urlProps = cu.getProperties();
      assertEquals("value", urlProps.getProperty("test"));
      assertEquals("value2", urlProps.getProperty("test2"));
    }

    public void testGetReader() throws Exception {
      createLeaf(url1, content1, null);

      CachedUrl cu = getTestCu(url1);
      Reader reader = cu.openForReading();
      assertEquals(content1, StringUtil.fromReader(reader));
    }
  }

  class MyMockArchivalUnit extends MockArchivalUnit {
    public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
      return new BaseCachedUrlSet(this, cuss);
    }

    public CachedUrl makeCachedUrl(String url) {
      return new BaseCachedUrl(this, url);
    }

    public UrlCacher makeUrlCacher(String url) {
      return new BaseUrlCacher(this, url);
    }
  }

  class MyMockFilterRule
    implements FilterRule {
    Reader reader;

    public MyMockFilterRule(Reader reader) {
      this.reader = reader;
    }

    public Reader createFilteredReader(Reader reader) {
      return this.reader;
    }
  }

  class MyMockFilterFactory implements FilterFactory {
    InputStream in;

    public MyMockFilterFactory(InputStream in) {
      this.in = in;
    }

    public InputStream createFilteredInputStream(ArchivalUnit au,
						 InputStream unfilteredIn,
						 String encoding) {
      return this.in;
    }
  }

  class MyAu extends NullPlugin.ArchivalUnit {
    public FilterRule getFilterRule(String mimeType) {
      return new FilterRule() {
	  public Reader createFilteredReader(Reader reader) {
	    return reader;
	  }
	};
    }
    public FilterFactory getFilterFactory(String mimeType) {
      return new FilterFactory() {
	  public InputStream createFilteredInputStream(ArchivalUnit au,
						       InputStream in,
						       String encoding) {
	    return in;
	  }
	};
    }
  }

  /** Varient that performs the tests when there's only a single version */
  public static class OnlyVersion extends VersionedTests {
    public OnlyVersion() {
    }

    protected void createLeaf(String url, String content,
			      CIProperties props) throws Exception {
      TestRepositoryNodeImpl.createLeaf(repo, url, content, props);
    }

    CachedUrl getTestCu(String url) {
      return mau.makeCachedUrl(url);
    }

    public void testVersionNumber() throws Exception {
      createLeaf(url1, content1, null);
      CachedUrl cu = getTestCu(url1);
      assertEquals(1, cu.getVersion());
    }
  }

  /** Variant that performs the tests with the current version when there's
   * a previous version */
  public static class CurrentVersion extends VersionedTests {
    public CurrentVersion() {
    }

    protected void createLeaf(String url, String content,
			      CIProperties props) throws Exception {
      Properties p = new Properties();
      p.put("wrongkey", "wrongval");
      RepositoryNode node =
	TestRepositoryNodeImpl.createLeaf(repo, url, badcontent+"1", p);
      TestRepositoryNodeImpl.createContentVersion(node, content, props);
    }

    CachedUrl getTestCu(String url) {
      CachedUrl cu = mau.makeCachedUrl(url);
      return cu;
    }

    public void testVersionNumber() throws Exception {
      createLeaf(url1, content1, null);
      CachedUrl cu = getTestCu(url1);
      CachedUrl curcu = mau.makeCachedUrl(url1);
      assertEquals(2, cu.getVersion());
      assertEquals(2, curcu.getVersion());
    }
  }

  /** Varient that performs the tests with version 2 of 3 versions */
  public static class PreviousVersion extends VersionedTests {
    public PreviousVersion() {
    }

    protected void createLeaf(String url, String content,
			      CIProperties props) throws Exception {
      Properties p = new Properties();
      p.put("wrongkey", "wrongval");
      RepositoryNode node =
	TestRepositoryNodeImpl.createLeaf(repo, url, badcontent+"1", p);
      TestRepositoryNodeImpl.createContentVersion(node, content, props);
      TestRepositoryNodeImpl.createContentVersion(node, badcontent+"3", p);
    }

    CachedUrl getTestCu(String url) {
      CachedUrl cu = mau.makeCachedUrl(url);
      CachedUrl[] all = cu.getCuVersions();
      assertEquals(3, all.length);

      return all[1];
    }

    public void testVersionNumber() throws Exception {
      createLeaf(url1, content1, null);
      CachedUrl cu = getTestCu(url1);
      CachedUrl curcu = mau.makeCachedUrl(url1);
      assertEquals(2, cu.getVersion());
      assertEquals(3, curcu.getVersion());
    }

    public void testGetCuVersion() throws Exception {
      createLeaf(url1, content1, null);
      CachedUrl cu = mau.makeCachedUrl(url1);
      assertEquals(1, cu.getCuVersion(1).getVersion());
      assertEquals(2, cu.getCuVersion(2).getVersion());
      assertEquals(3, cu.getCuVersion(3).getVersion());
      CachedUrl noncu = cu.getCuVersion(4);
      assertEquals(4, noncu.getVersion());
      try {
	noncu.getContentSize();
	fail("No version 4, getContentSize() should throw");
      } catch (UnsupportedOperationException e) { }
      try {
	noncu.getUnfilteredInputStream();
	fail("No version 4, getUnfilteredInputStream() should throw");
      } catch (UnsupportedOperationException e) { }
    }

    public void testGetCuVersions() throws Exception {
      createLeaf(url1, content1, null);
      CachedUrl cu = mau.makeCachedUrl(url1);
      CachedUrl[] all = cu.getCuVersions();
      assertEquals(3, all.length);
      assertEquals(3, all[0].getVersion());
      assertEquals(2, all[1].getVersion());
      assertEquals(1, all[2].getVersion());
    }

    public void testGetCuVersionsMax() throws Exception {
      createLeaf(url1, content1, null);
      CachedUrl cu = mau.makeCachedUrl(url1);
      CachedUrl[] all = cu.getCuVersions(2);
      assertEquals(2, all.length);
      assertEquals(3, all[0].getVersion());
      assertEquals(2, all[1].getVersion());
    }



  }

  public static Test suite() {
    return variantSuites(new Class[] {NotVersionedTests.class,
				      OnlyVersion.class,
				      CurrentVersion.class,
				      PreviousVersion.class,
				      });
  }

}
