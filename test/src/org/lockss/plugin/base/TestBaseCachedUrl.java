/*
 * $Id: TestBaseCachedUrl.java,v 1.13 2004-10-13 23:07:20 clairegriffin Exp $
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
import org.lockss.plugin.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.repository.*;

public class TestBaseCachedUrl extends LockssTestCase {
  private static final String PARAM_SHOULD_FILTER_HASH_STREAM =
      Configuration.PREFIX+"baseCachedUrl.filterHashStream";

  private LockssRepository repo;
  private MockArchivalUnit mau;
  private MockLockssDaemon theDaemon;
  private CachedUrlSet cus;
  private MockPlugin plugin;

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
    CachedUrlSetSpec rSpec =
        new RangeCachedUrlSetSpec("http://www.example.com/testDir");
    cus = mau.makeCachedUrlSet(rSpec);
  }

  public void tearDown() throws Exception {
    repo.stopService();
    super.tearDown();
  }

  public void testFilterParamDefault() {
     MyCachedUrl cu = new MyCachedUrl(new MyAu(), null);
     cu.openForHashing();
     assertFalse(cu.gotUnfilteredStream());
   }

   public void testFilterParamFilterOn() throws IOException {
     String config = PARAM_SHOULD_FILTER_HASH_STREAM+"=true";
     ConfigurationUtil.setCurrentConfigFromString(config);
     MyCachedUrl cu = new MyCachedUrl(new MyAu(), null);
     cu.openForHashing();
     assertFalse(cu.gotUnfilteredStream());
   }

   public void testFilterParamFilterOff() throws IOException {
     String config = PARAM_SHOULD_FILTER_HASH_STREAM+"=false";
     ConfigurationUtil.setCurrentConfigFromString(config);
     MyCachedUrl cu = new MyCachedUrl(new MyAu(), null);
     cu.openForHashing();
     assertTrue(cu.gotUnfilteredStream());
   }

   public void testGetUrl() throws Exception {
     createLeaf("http://www.example.com/testDir/leaf1", "test stream", null);

     CachedUrl url =
       mau.makeCachedUrl("http://www.example.com/testDir/leaf1");
     assertEquals("http://www.example.com/testDir/leaf1", url.getUrl());
   }

   public void testIsLeaf() throws Exception {
     createLeaf("http://www.example.com/testDir/leaf1", "test stream", null);
     createLeaf("http://www.example.com/testDir/leaf2", null, null);

     CachedUrl url =
       mau.makeCachedUrl("http://www.example.com/testDir/leaf1");
     assertTrue(url.isLeaf());
     url = mau.makeCachedUrl("http://www.example.com/testDir/leaf2");
     assertTrue(url.isLeaf());
   }

   public void testGetContentSize() throws Exception {
     createLeaf("http://www.example.com/testDir/leaf1", "test stream", null);
     createLeaf("http://www.example.com/testDir/leaf2", "test stream2", null);
     createLeaf("http://www.example.com/testDir/leaf3", "", null);

     CachedUrl url =
       mau.makeCachedUrl("http://www.example.com/testDir/leaf1");
     BigInteger bi = new BigInteger(url.getUnfilteredContentSize());
     assertEquals(11, bi.intValue());

     url = mau.makeCachedUrl("http://www.example.com/testDir/leaf2");
     bi = new BigInteger(url.getUnfilteredContentSize());
     assertEquals(12, bi.intValue());

     url = mau.makeCachedUrl("http://www.example.com/testDir/leaf3");
     bi = new BigInteger(url.getUnfilteredContentSize());
     assertEquals(0, bi.intValue());
   }

   public void testOpenForReading() throws Exception {
     createLeaf("http://www.example.com/testDir/leaf1", "test stream", null);
     createLeaf("http://www.example.com/testDir/leaf2", "test stream2", null);
     createLeaf("http://www.example.com/testDir/leaf3", "", null);

     CachedUrl url =
       mau.makeCachedUrl("http://www.example.com/testDir/leaf1");
     InputStream urlIs = url.getUnfilteredInputStream();
     ByteArrayOutputStream baos = new ByteArrayOutputStream(11);
     StreamUtil.copy(urlIs, baos);
     assertEquals("test stream", baos.toString());

     url = mau.makeCachedUrl("http://www.example.com/testDir/leaf2");
     urlIs = url.getUnfilteredInputStream();
     baos = new ByteArrayOutputStream(12);
     StreamUtil.copy(urlIs, baos);
     assertEquals("test stream2", baos.toString());

     url = mau.makeCachedUrl("http://www.example.com/testDir/leaf3");
     urlIs = url.getUnfilteredInputStream();
     baos = new ByteArrayOutputStream(0);
     StreamUtil.copy(urlIs, baos);
     assertEquals("", baos.toString());
   }

  public void testOpenForHashingDefaultsToNoFiltering() throws Exception {
    createLeaf("http://www.example.com/testDir/leaf1", "<test stream>", null);
    String str = "This is a filtered stream";
    mau.setFilterRule(new MyMockFilterRule(new StringReader(str)));

    CachedUrl url =
      mau.makeCachedUrl("http://www.example.com/testDir/leaf1");
    InputStream urlIs = url.getUnfilteredInputStream();
    assertNotEquals(str, StringUtil.fromInputStream(urlIs));
  }

  public void testOpenForHashingWontFilterIfConfiguredNotTo()
      throws Exception {
    String config = PARAM_SHOULD_FILTER_HASH_STREAM+"=false";
    ConfigurationUtil.setCurrentConfigFromString(config);
    createLeaf("http://www.example.com/testDir/leaf1", "<test stream>", null);
    String str = "This is a filtered stream";
    mau.setFilterRule(new MyMockFilterRule(new StringReader(str)));

    CachedUrl url =
      mau.makeCachedUrl("http://www.example.com/testDir/leaf1");
    InputStream urlIs = url.openForHashing();
    assertNotEquals(str, StringUtil.fromInputStream(urlIs));
  }

   public void testOpenForHashingWillFilterIfConfiguredTo()
       throws Exception {
     String config = PARAM_SHOULD_FILTER_HASH_STREAM + "=true";
     ConfigurationUtil.setCurrentConfigFromString(config);
     createLeaf("http://www.example.com/testDir/leaf1", "blah <test stream>", null);
     String str = "This is a filtered stream";
     mau.setFilterRule(new MyMockFilterRule(new StringReader(str)));

     CachedUrl url =
         mau.makeCachedUrl("http://www.example.com/testDir/leaf1");
     InputStream urlIs = url.openForHashing();
     assertEquals(str, StringUtil.fromInputStream(urlIs));
   }

   public void testGetProperties() throws Exception {
     CIProperties newProps = new CIProperties();
     newProps.setProperty("test", "value");
     newProps.setProperty("test2", "value2");
     createLeaf("http://www.example.com/testDir/leaf1", null, newProps);

     CachedUrl url =
       mau.makeCachedUrl("http://www.example.com/testDir/leaf1");
     CIProperties urlProps = url.getProperties();
     assertEquals("value", urlProps.getProperty("test"));
     assertEquals("value2", urlProps.getProperty("test2"));
   }

    public void testGetReader() throws Exception {
     createLeaf("http://www.example.com/testDir/leaf1", "test stream", null);

     CachedUrl cu =
       mau.makeCachedUrl("http://www.example.com/testDir/leaf1");
     Reader reader = cu.openForReading();
     CharArrayWriter writer = new CharArrayWriter(11);
     StreamUtil.copy(reader, writer);
     assertEquals("test stream", writer.toString());
   }

   private RepositoryNode createLeaf(String url, String content,
                                     CIProperties props) throws Exception {
     return TestRepositoryNodeImpl.createLeaf(repo, url, content, props);
   }

   private class MyMockArchivalUnit extends MockArchivalUnit {
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

   private class MyMockFilterRule
       implements FilterRule {
     Reader reader;

     public MyMockFilterRule(Reader reader) {
       this.reader = reader;
     }

     public Reader createFilteredReader(Reader reader) {
       return this.reader;
     }
   }

    private class MyAu extends NullPlugin.ArchivalUnit {
     public FilterRule getFilterRule(String mimeType) {
       return new FilterRule() {
        public Reader createFilteredReader(Reader reader) {
          return reader;
        }
      };
     }
   }

   private class MyCachedUrl extends BaseCachedUrl {
     private boolean gotUnfilteredStream = false;
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

     public byte[] getUnfilteredContentSize() {
       throw new UnsupportedOperationException("Not implemented");
     }
   }

   public static void main(String[] argv) {
     String[] testCaseList = {
         TestBaseCachedUrl.class.getName()};
     junit.textui.TestRunner.main(testCaseList);
   }
 }
