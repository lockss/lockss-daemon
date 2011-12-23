/*
 * $Id: TestUrlToChecksumMapper.java,v 1.1.2.1 2011-12-23 19:30:15 nchondros Exp $
 */

/*

 Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.catalog;

import java.util.*;
import java.io.StringWriter;
import java.security.*;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.test.*;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class TestUrlToChecksumMapper extends LockssTestCase {
  private static final String checksumAlgorithm = "SHA-1";

  private MockLockssDaemon theDaemon;
  private ArchivalUnit testau;
  private MessageDigest checksumProducer = null;
  
  private static final String BASE_URL = "http://www.test.org/";
  
  private static String[] urls = {
    "lockssau:",
    BASE_URL,
    BASE_URL + "index.html",
    BASE_URL + "file1.html",
    BASE_URL + "file2.html",
    BASE_URL + "branch1/",
    BASE_URL + "branch1/index.html",
    BASE_URL + "branch1/file1.html",
    BASE_URL + "branch1/file2.html",
    BASE_URL + "branch2/",
    BASE_URL + "branch2/index.html",
    BASE_URL + "branch2/file1.html",
    BASE_URL + "branch2/file2.html",
  };

  public void setUp() throws Exception {
    super.setUp();
    checksumProducer = MessageDigest.getInstance(checksumAlgorithm);
    theDaemon = getMockLockssDaemon();
    TimeBase.setSimulated();
    this.testau = setupAu();
    setupRepo(testau);
  }

  private MockArchivalUnit setupAu() {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setAuId("mock");
    MockPlugin plug = new MockPlugin(theDaemon);
    mau.setPlugin(plug);
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.setEstimatedHashDuration(1000);
    List files = new ArrayList();
    for (int ix = 0; ix < urls.length; ix++) {
      MockCachedUrl cu = (MockCachedUrl)mau.addUrl(urls[ix], "This is content for CUS file " + ix);
      cu.getProperties().put(CachedUrl.PROPERTY_CHECKSUM, checksum(urls[ix]));
      files.add(cu);
    }
    cus.setHashItSource(files);
    return mau;
  }
  
  private void setupRepo(ArchivalUnit au) throws Exception {
    MockLockssRepository repo = new MockLockssRepository("/foo", au);
    for (int ix =  0; ix < urls.length; ix++) {
      repo.createNewNode(urls[ix]);
    }
    ((MockLockssDaemon)theDaemon).setLockssRepository(repo, au);
  }
  
  public void tearDown() throws Exception {
    theDaemon.getLockssRepository(testau).stopService();
    TimeBase.setReal();
    super.tearDown();
  }

  public void xtestDumpCachedUrls() throws Exception {
    CachedUrlSet cus = this.testau.getAuCachedUrlSet();
    Iterator iter = cus.contentHashIterator();
    while (iter.hasNext()) {
      CachedUrlSetNode node = (CachedUrlSetNode) iter.next();
      System.err.println(node);
      switch (node.getType()) {
      case CachedUrlSetNode.TYPE_CACHED_URL_SET:
        // Do nothing: No properties attached to a CachedUrlSet
        break;
      case CachedUrlSetNode.TYPE_CACHED_URL:
        // only process entries with content
        if (node.hasContent()) {
          CachedUrl cu = (CachedUrl) node;
          for (CachedUrl version : cu.getCuVersions()) {
            CIProperties headers = version.getProperties();
            String checksum = headers.getProperty(CachedUrl.PROPERTY_CHECKSUM);
            System.err.println("  version: " + cu.getVersion() + " checksum: "
                + checksum);
          }
        }
        break;
      default:
        fail("Unknown node type: " + node.getType());
      }
    }
  }
  
  public void testMapper() throws Exception {
    //anticipate a sorted list of the test urls
    String[] sortedUrls = urls.clone();
    Arrays.sort(sortedUrls);
    UrlToChecksumMap map = UrlToChecksumMapper.generateMap(testau);
    int iSortedUrls = 0;
    for( String key : map.keySet() ) {
      assertEquals(key, sortedUrls[iSortedUrls++]);
      assertEquals(map.get(key), checksum(key));
    }
    assertEquals(iSortedUrls, sortedUrls.length);
  }
  
  public void testXMLMarshaling() throws Exception {
    UrlToChecksumMap originalMap = UrlToChecksumMapper.generateMap(testau);
    XStream xStream = new XStream(new DomDriver());
    xStream.alias("UrlToChecksumMap",UrlToChecksumMap.class);
    xStream.registerConverter(new UrlToChecksumMapConverter());
    String xml = xStream.toXML(originalMap);
    
    UrlToChecksumMap unmarshalledMap = (UrlToChecksumMap) xStream.fromXML(xml);
    assertEquals(originalMap, unmarshalledMap);
  }
  
  public void xtestDumpXMLMapper() throws Exception {
    StringWriter sw = new StringWriter();
    UrlToChecksumMapper.generateXMLMap(testau, sw);
    System.out.println(sw.toString());
  }
  
  public void xtestDumpMapper() throws Exception {
    UrlToChecksumMap map = UrlToChecksumMapper.generateMap(testau);
    for( String key : map.keySet() ) {
      System.out.println(String.format("  url %s -> %s ", key, map.get(key)));
    }
  }
  
  private String checksum(String url) {
    //for lack of content, just do a checksum of the url itself
    checksumProducer.reset();
    checksumProducer.update(url.getBytes());
    byte[] bchecksum = checksumProducer.digest();
    return ByteArray.toHexString(bchecksum);
  }
}
