/*
 * $Id:$
 */
/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.pub2web.ms;

import java.io.InputStream;
import java.util.Set;

import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.IOUtil;
import org.lockss.util.SetUtil;
import org.lockss.util.StringUtil;


public class TestMsHtmlLinkExtractorFactory extends LockssTestCase {

  private MsHtmlLinkExtractorFactory msfact;
  private LinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  private final String BASE_URL = "http://www.asmscience.org/";
  private final String JID = "microbiolspec";

  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    log.setLevel("debug3");
       m_mau = new MockArchivalUnit();
      m_callback = new MyLinkExtractorCallback();

      msfact = new MsHtmlLinkExtractorFactory();
      //m_extractor = msfact.createLinkExtractor("html");
      m_extractor = new JsoupHtmlLinkExtractor(); 
  }
  
  public static final String htmlSnippet =
      "<html><head><title>Test Title</title>" +
          "<div></div>" +
          "</head><body>" +
          "insert html to pull links from here" +
          "</body>" +
          "</html>";
  
  
  public void testPlaceholder() throws Exception {
    InputStream inStream;
 //placeholder - took out real world file tests
    assertEquals(true,true);

  }
  
  private void testExpectedAgainstParsedUrls(Set<String> expectedUrls, 
      String source, String srcUrl) throws Exception {

    Set<String> result_strings = parseSingleSource(source, srcUrl);
    //assertEquals(expectedUrls.size(), result_strings.size());
    for (String url : result_strings) {
      log.debug3("URL: " + url);
      //assertTrue(expectedUrls.contains(url));
    }
  }
  private Set<String> parseSingleSource(String source, String srcUrl)
      throws Exception {

    m_callback.reset();
    m_extractor.extractUrls(m_mau,
        new org.lockss.test.StringInputStream(source), ENC,
        srcUrl, m_callback);
    return m_callback.getFoundUrls();
  }

  private static class MyLinkExtractorCallback implements
  LinkExtractor.Callback {

    Set<String> foundUrls = new java.util.HashSet<String>();

    public void foundLink(String url) {
      foundUrls.add(url);
    }

    public Set<String> getFoundUrls() {
      return foundUrls;
    }

    public void reset() {
      foundUrls = new java.util.HashSet<String>();
    }
  }

}
