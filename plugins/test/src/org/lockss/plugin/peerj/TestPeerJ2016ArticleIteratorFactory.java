/* 
 * $Id: $ 
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

package org.lockss.plugin.peerj;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.Iterator;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestPeerJ2016ArticleIteratorFactory extends ArticleIteratorTestCase {

  private final String PATTERN_FAIL_MSG = "Article file URL pattern changed or incorrect";
  private final String PLUGIN_NAME = "org.lockss.plugin.peerj.ClockssPeerJ2016Plugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String BASE_URL = "https://peerj.com/";
  private final String VOLUME_NAME = "2016";
  private final String JOURNAL_ID = "cs";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      VOLUME_NAME_KEY, VOLUME_NAME,
      JOURNAL_ID_KEY, JOURNAL_ID);
  private CIProperties pdfHeader = new CIProperties();    
  private CIProperties textHeader = new CIProperties();
  private static final String ContentString = "foo blah";
  InputStream random_content_stream;

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();

    au = createAu();
    // set up headers for creating mock CU's of the appropriate type
    pdfHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");
    textHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    // the content in the urls doesn't really matter for the test
    random_content_stream = new ByteArrayInputStream(ContentString.getBytes(Constants.ENCODING_UTF_8));
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Configuration method. 
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", JOURNAL_ID);
    conf.put("volume_name", VOLUME_NAME);

    return conf;
  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals("Article file root URL pattern changed or incorrect" ,ListUtil.list( BASE_URL + "articles/"),
        getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);

    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://www.wrong.com/articles/250/");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "articles/cs-/");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "articles/cs-25.xml");
    //these should be good    
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "articles/cs-25.pdf");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "articles/cs-25");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "articles/250");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "articles/250.pdf");
  }


}
