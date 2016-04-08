/*
 * $Id:$
 */
package org.lockss.plugin.pub2web.ms;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestMsHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private MsHtmlCrawlFilterFactory cfact;
  private MsHtmlHashFilterFactory hfact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    cfact = new MsHtmlCrawlFilterFactory();
    hfact = new MsHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String toc_bit = "";
  private static final String toc_bit_hashfiltered = "";
  private static final String toc_bit_crawlfiltered = "";

  private static final String article_bit = "";
  private static final String article_bit_hashfiltered = "";
  private static final String article_bit_crawlfiltered = "";


  public void testTOCFiltering() throws Exception {
    InputStream inStream;
    //crawl-filter
    inStream = cfact.createFilteredInputStream(mau,
        new StringInputStream(toc_bit),
        Constants.DEFAULT_ENCODING);
    assertEquals(toc_bit_crawlfiltered, StringUtil.fromInputStream(inStream));
    //hash-filter
    inStream = hfact.createFilteredInputStream(mau,
        new StringInputStream(toc_bit),
        Constants.DEFAULT_ENCODING);
    assertEquals(toc_bit_hashfiltered, StringUtil.fromInputStream(inStream));

  }

  public void testArticleLandingFiltering() throws Exception {
    InputStream inStream;
    //crawl-filter
    inStream = cfact.createFilteredInputStream(mau,
        new StringInputStream(article_bit),
        Constants.DEFAULT_ENCODING);
    assertEquals(article_bit_crawlfiltered, StringUtil.fromInputStream(inStream));
    //hash-filter
    inStream = hfact.createFilteredInputStream(mau,
        new StringInputStream(article_bit),
        Constants.DEFAULT_ENCODING);
    assertEquals(article_bit_hashfiltered, StringUtil.fromInputStream(inStream));

  }
}
