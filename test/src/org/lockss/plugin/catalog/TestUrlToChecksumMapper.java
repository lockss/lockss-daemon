/*
 * $Id: TestUrlToChecksumMapper.java,v 1.1.2.2 2012-02-11 17:44:53 nchondros Exp $
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

import org.custommonkey.xmlunit.XMLAssert;
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
  
  public void testBufferedMapper() throws Exception {
    //anticipate a sorted list of the test urls
    String[] sortedUrls = urls.clone();
    Arrays.sort(sortedUrls);
    UrlToChecksumMapperBuffered mapper = new UrlToChecksumMapperBuffered();
    UrlToChecksumMap map = mapper.generateMap(testau);
    int iSortedUrls = 0;
    for( String key : map.keySet() ) {
      assertEquals(key, sortedUrls[iSortedUrls++]);
      assertEquals(map.get(key), checksum(key));
    }
    assertEquals(iSortedUrls, sortedUrls.length);
  }
  
  public void testBufferedMapperXMLMarshaling() throws Exception {
    UrlToChecksumMapperBuffered mapper = new UrlToChecksumMapperBuffered();
    UrlToChecksumMap originalMap = mapper.generateMap(testau);
    XStream xStream = new XStream(new DomDriver());
    xStream.alias("UrlToChecksumMap",UrlToChecksumMap.class);
    xStream.registerConverter(new UrlToChecksumMapConverter());
    String xml = xStream.toXML(originalMap);
    
    UrlToChecksumMap unmarshalledMap = (UrlToChecksumMap) xStream.fromXML(xml);
    assertEquals(originalMap, unmarshalledMap);
  }
  
  public void xtestDumpXMLBufferedMapper() throws Exception {
    StringWriter sw = new StringWriter();
    UrlToChecksumMapper mapper = new UrlToChecksumMapperBuffered();
    mapper.generateXMLMap(testau, sw);
    System.out.println("Buffered:" + sw.toString());
  }
  
  public void testDumpXMLDirectMapper() throws Exception {
    StringWriter sw = new StringWriter();
    UrlToChecksumMapper mapper = new UrlToChecksumMapperDirect();
    mapper.generateXMLMap(testau, sw);
    //System.out.println("Direct:" + sw.toString());
    XMLAssert.assertXMLEqual(sw.toString(), expectedDirectXML);
  }
  
  public void xtestDumpMapper() throws Exception {
    UrlToChecksumMapperBuffered mapper = new UrlToChecksumMapperBuffered();
    UrlToChecksumMap map = mapper.generateMap(testau);
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
  
  private String expectedDirectXML =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
          "<UrlToChecksumMap>\n" +
          "    <entry>\n" +
          "        <url>lockssau:</url>\n" +
          "        <checksum>594D83A41AB4304997A8E3CBED187DDA1FF3997D</checksum>\n" +
          "    </entry>\n" +
          "    <entry>\n" +
          "        <url>http://www.test.org/</url>\n" +
          "        <checksum>BCC964D42C957F9D64D8EC47B1BF31A1DD3AB5CB</checksum>\n" +
          "    </entry>\n" +
          "    <entry>\n" +
          "        <url>http://www.test.org/index.html</url>\n" +
          "        <checksum>273B27ECA7DBBFB8215BE707ECEDC87E07687B47</checksum>\n" +
          "    </entry>\n" +
          "    <entry>\n" +
          "        <url>http://www.test.org/file1.html</url>\n" +
          "        <checksum>9C2038ACDD61740198D6CF72E3912E8F2960E2AD</checksum>\n" +
          "    </entry>\n" +
          "    <entry>\n" +
          "        <url>http://www.test.org/file2.html</url>\n" +
          "        <checksum>2A195FBF19F1B71DEF081A8F2B41CF8D8F8665B2</checksum>\n" +
          "    </entry>\n" +
          "    <entry>\n" +
          "        <url>http://www.test.org/branch1/</url>\n" +
          "        <checksum>6ED5FC900B1B2562AF2BCDFEA4994BBD75EC421A</checksum>\n" +
          "    </entry>\n" +
          "    <entry>\n" +
          "        <url>http://www.test.org/branch1/index.html</url>\n" +
          "        <checksum>B41F8A3E8D90344BFF0845247504509BB3F2F205</checksum>\n" +
          "    </entry>\n" +
          "    <entry>\n" +
          "        <url>http://www.test.org/branch1/file1.html</url>\n" +
          "        <checksum>39F2435E05740276E84D61E5CFA2638A16BEE160</checksum>\n" +
          "    </entry>\n" +
          "    <entry>\n" +
          "        <url>http://www.test.org/branch1/file2.html</url>\n" +
          "        <checksum>ED33BB9C800E8049B9D1D8C1E093850E04A1ADB1</checksum>\n" +
          "    </entry>\n" +
          "    <entry>\n" +
          "        <url>http://www.test.org/branch2/</url>\n" +
          "        <checksum>908CE4FC2554DCF2534F146D41C6DC06BB3C3D25</checksum>\n" +
          "    </entry>\n" +
          "    <entry>\n" +
          "        <url>http://www.test.org/branch2/index.html</url>\n" +
          "        <checksum>FF3CB6108D2847321EF01861C346C59E81DF71F3</checksum>\n" +
          "    </entry>\n" +
          "    <entry>\n" +
          "        <url>http://www.test.org/branch2/file1.html</url>\n" +
          "        <checksum>9A32BA6F1FB1142220B2F8F310B221482BC8587A</checksum>\n" +
          "    </entry>\n" +
          "    <entry>\n" +
          "        <url>http://www.test.org/branch2/file2.html</url>\n" +
          "        <checksum>2AF5D83D2C62C802B319B5A6D157305F248F214D</checksum>\n" +
          "    </entry>\n" +
          "</UrlToChecksumMap>";
}
