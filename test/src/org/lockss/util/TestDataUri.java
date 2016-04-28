/*
 * Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
 * all rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Stanford University shall not
 * be used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Stanford University.
 *
 */

package org.lockss.util;

import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Test class for DataUri
 */
public class TestDataUri extends LockssTestCase {

  private static final String HTML_MIME_TYPE = "text/html";

  private static final String IMAGE_LINK =
      "<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA" +
          "AAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO" +
          "9TXL0Y4OHwAAAABJRU5ErkJggg==\" alt=\"Red dot\" />";

  private static final String SIMPLE_TXTDATA = // Hello, World! - html encoding
      "data:,Hello%2C%20World!";


  private static final String BASE64_TXTDATA = // Hello, World! - Base64 encoding
      "data:text/plain;base64,SGVsbG8sIFdvcmxkIQ%3D%3D";

  private static final String SIMPLE_HTMLDATA = // <h1>Hello, World!</h1> - html encoding
      "data:text/html,%3Ch1%3EHello%2C%20World!%3C%2Fh1%3E";

  private static final String SIMPLE_JAVASCRIPT =
      "data:text/html,<script>alert('hi');</script>";

  private static final String BASE64_IMGDATA =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA" +
          "AAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO" +
          "9TXL0Y4OHwAAAABJRU5ErkJggg==";

  private static final String MULTI_PARAM_DATA =
      "data:text/html;charset=utf-8;param2,%3Ch1%3EHello%2C%20World!%3C%2Fh1%3E";
  private static final String SVG_DATA =
      "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' "+
          "width='10' height='10'><linearGradient id='gradient'>"+
          "<stop offset='10%' stop-color='#F00'/><stop offset='90%' stop-color='#fcc'/>"+
          " </linearGradient><rect fill='url(#gradient)' x='0' y='0' "+
          "width='100%' height='100%'/></svg>";

  private static final String HTML_WITH_LINKS =
      "data:text/html;charset=utf-8," +
          "%3Ca+href%3D%22http%3A%2F%2Fwww.w3schools" +
          ".com%2Fhtml%2F%22%3EVisit+our+HTML+tutorial%3C%2Fa%3E";

  private File tempDir;
  private MockArchivalUnit mockAu;
  private String baseUri = "http://www.example.com";
  private MyLinkExtractorCallback extractorCallback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private LinkExtractor jsoupExtractor = new JsoupHtmlLinkExtractor(false, false, null,
      null);
  private LinkExtractor goslingExtractor = new GoslingHtmlLinkExtractor();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    mockAu = new MockArchivalUnit("datauri");
    tempDir = getTempDir("datauri");
    extractorCallback = new MyLinkExtractorCallback();
  }

  @Override
  public void tearDown() throws Exception {
    FileUtil.delTree(tempDir);
    super.tearDown();
  }

  public void testNewBuilder() throws Exception {
    DataUri dataUri;
    DataUri.Builder builder = DataUri.newBuilder();
    assertNotNull(builder);
    try {
      dataUri = builder.build();
      fail("empty builder should throw");
    }
    catch (Exception ex)  {
      // expected
    }
    builder.data("12345");
    dataUri = builder.build();
    assertNotNull(dataUri);
    assertEquals(DataUri.DEFAULT_CHARSET,dataUri.getCharset());
    assertEquals(DataUri.DEFAULT_MIMETYPE,dataUri.getMimeType());
    assertEquals("12345",dataUri.getData());
  }

  public void testIsDataUri() throws Exception {
    // these should all good.
    assertTrue(DataUri.isDataUri(SIMPLE_TXTDATA));
    assertTrue(DataUri.isDataUri(BASE64_TXTDATA));
    assertTrue(DataUri.isDataUri(SIMPLE_HTMLDATA));
    assertTrue(DataUri.isDataUri(BASE64_IMGDATA));

    // standard url string
    String test_str = "http://www.example.com";
    assertFalse(DataUri.isDataUri(test_str));

    // mispelled data
    test_str = "daat:blahdeblah";
    assertFalse(DataUri.isDataUri(test_str));

    // uppercase data should be ok
    test_str = "DATA:blahdeblah";
    assertTrue(DataUri.isDataUri(test_str));

    // uppercase data should w/ comma
    test_str = "DATA:,blahdeblah";
    assertTrue(DataUri.isDataUri(test_str));
  }

  public void testIsValidDataUri() throws Exception {
    // these should all good.
    assertTrue(DataUri.isValidDataUri(SIMPLE_TXTDATA));
    assertTrue(DataUri.isValidDataUri(BASE64_TXTDATA));
    assertTrue(DataUri.isValidDataUri(SIMPLE_HTMLDATA));
    assertTrue(DataUri.isValidDataUri(BASE64_IMGDATA));

    // standard url string
    String test_str = "http://www.example.com";
    assertFalse(DataUri.isValidDataUri(test_str));

    // mispelled data
    test_str = "daat:blahdeblah";
    assertFalse(DataUri.isValidDataUri(test_str));

    // uppercase data should but no comma
    test_str = "DATA:blahdeblah";
    assertFalse(DataUri.isValidDataUri(test_str));

    // uppercase data should w/ comma
    test_str = "DATA:,blahdeblah";
    assertTrue(DataUri.isValidDataUri(test_str));
  }

  public void testMakeDataUri() throws Exception {
    // simple string "data:,Hello%2C%20World!"
    DataUri dataUri = DataUri.makeDataUri(SIMPLE_TXTDATA);
    assertEquals(DataUri.DEFAULT_MIMETYPE,dataUri.getMimeType());
    assertEquals(DataUri.DEFAULT_CHARSET,dataUri.getCharset());
    assertFalse(dataUri.usesBase64());
    assertEquals("Hello%2C%20World!",dataUri.getData());

    // simple html "data:text/html,%3Ch1%3EHello%2C%20World!%3C%2Fh1%3E"
    dataUri = DataUri.makeDataUri(SIMPLE_HTMLDATA);
    assertEquals(HTML_MIME_TYPE,dataUri.getMimeType());
    assertEquals(DataUri.DEFAULT_CHARSET, dataUri.getCharset());
    assertFalse(dataUri.usesBase64());
    assertEquals("%3Ch1%3EHello%2C%20World!%3C%2Fh1%3E",dataUri.getData());

    // simple javascript: "data:text/html,<script>alert('hi');</script>"
    dataUri = DataUri.makeDataUri(SIMPLE_JAVASCRIPT);
    assertEquals(HTML_MIME_TYPE,dataUri.getMimeType());
    assertEquals(DataUri.DEFAULT_CHARSET, dataUri.getCharset());
    assertFalse(dataUri.usesBase64());
    assertEquals("<script>alert('hi');</script>",dataUri.getData());

    // test base64 img data
    dataUri = DataUri.makeDataUri(BASE64_IMGDATA);
    assertEquals("image/png",dataUri.getMimeType());
    assertEquals(DataUri.DEFAULT_CHARSET,dataUri.getCharset());
    assertTrue(dataUri.usesBase64());
    assertEquals("iVBORw0KGgoAAAANSUhEUgAAAAUA" +
        "AAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO" +
            "9TXL0Y4OHwAAAABJRU5ErkJggg==", dataUri.getData());


    // test multiple media parameters
    dataUri = DataUri.makeDataUri(MULTI_PARAM_DATA);
    assertEquals(HTML_MIME_TYPE,dataUri.getMimeType());
    assertEquals("utf-8", dataUri.getCharset());
    assertNotNull(dataUri.getMediaParams().getProperty("param2"));
    assertFalse(dataUri.usesBase64());
    assertEquals("%3Ch1%3EHello%2C%20World!%3C%2Fh1%3E",dataUri.getData());

    // svg data: data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg'
    dataUri = DataUri.makeDataUri(SVG_DATA);
    assertEquals("image/svg+xml",dataUri.getMimeType());
    assertEquals(Constants.ENCODING_UTF_8, dataUri.getCharset());
    assertFalse(dataUri.usesBase64());
    assertEquals("<svg xmlns='http://www.w3.org/2000/svg' "+
    "width='10' height='10'><linearGradient id='gradient'>"+
        "<stop offset='10%' stop-color='#F00'/><stop offset='90%' stop-color='#fcc'/>"+
        " </linearGradient><rect fill='url(#gradient)' x='0' y='0' "+
        "width='100%' height='100%'/></svg>",dataUri.getData());

  }

  public void testDecodeToFile() throws Exception {
    checkDecodedFile(SIMPLE_TXTDATA,"Hello, World!");
    checkDecodedFile(BASE64_TXTDATA,"Hello, World!");
    checkDecodedFile(SIMPLE_HTMLDATA, "<h1>Hello, World!</h1>");
    checkDecodedFile(SIMPLE_JAVASCRIPT,"<script>alert('hi');</script>");
  }

  public void testDispatchToLinkExtractor() throws Exception {
    // test jsoup extracts
    mockAu.setLinkExtractor(HTML_MIME_TYPE, jsoupExtractor);
    DataUri.dispatchToLinkExtractor(SIMPLE_HTMLDATA,baseUri,mockAu,extractorCallback);
    assertEmpty(extractorCallback.getFoundUrls());
    extractorCallback.reset();
    DataUri.dispatchToLinkExtractor(SIMPLE_JAVASCRIPT,baseUri,mockAu,extractorCallback);
    assertEmpty(extractorCallback.getFoundUrls());
    extractorCallback.reset();
    DataUri.dispatchToLinkExtractor(HTML_WITH_LINKS,baseUri,mockAu,extractorCallback);
    assertEquals(SetUtil.set("http://www.w3schools.com/html/"),extractorCallback.getFoundUrls());
    extractorCallback.reset();
    // test gosling extracts
    mockAu.setLinkExtractor(HTML_MIME_TYPE, goslingExtractor);
    DataUri.dispatchToLinkExtractor(SIMPLE_HTMLDATA,baseUri,mockAu,extractorCallback);
    assertEmpty(extractorCallback.getFoundUrls());
    extractorCallback.reset();
    DataUri.dispatchToLinkExtractor(SIMPLE_JAVASCRIPT,baseUri,mockAu,extractorCallback);
    assertEmpty(extractorCallback.getFoundUrls());
    extractorCallback.reset();
    DataUri.dispatchToLinkExtractor(HTML_WITH_LINKS,baseUri,mockAu,extractorCallback);
    assertEquals(SetUtil.set("http://www.w3schools.com/html/"),extractorCallback.getFoundUrls());
    extractorCallback.reset();
  }

  private void checkDecodedFile(String datauri, String decodeString) throws IOException {
    File tempFile = FileUtil.createTempFile("decode",".html", tempDir);
    String fname = tempFile.getCanonicalPath();
    OutputStream os = new BufferedOutputStream(new FileOutputStream(tempFile));
    DataUri.decodeToStream(datauri, os);
    os.close();
    BufferedReader in
        = new BufferedReader(new FileReader(fname));
    assertEquals(decodeString, in.readLine());

  }

  private static class MyLinkExtractorCallback implements
      LinkExtractor.Callback {
    java.util.Set<String> foundUrls = new java.util.HashSet<String>();

    public void foundLink(String url) {
      foundUrls.add(url);
    }

    public java.util.Set<String> getFoundUrls() {
      return foundUrls;
    }

    public void reset() {
      foundUrls = new java.util.HashSet<String>();
    }
  }

}
