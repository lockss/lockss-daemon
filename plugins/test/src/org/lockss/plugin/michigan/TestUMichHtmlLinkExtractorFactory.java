/*
 * $Id:$
 */
/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.michigan;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Pattern;

import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.extractor.RegexpCssLinkExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.Constants;


public class TestUMichHtmlLinkExtractorFactory extends LockssTestCase {
  UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();

  private UMichHtmlLinkExtractorFactory fact;
  private LinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  

  private static final String scriptHtmlPartial =
		  "<script type=\"text/javascript\"> "+
		  "if ( true ) {\r\n" + 
		  "        //$(\"body\").addClass(\"reading\");\r\n" + 
		  "        var reader = cozy.reader('reader', {\r\n" + 
		  "          href: \"https://www.fulcrum.org/epubs/9s161681f/\",\r\n" + 
		  "          skipLink: '.skip',\r\n" + 
		  "          useArchive: false,\r\n" + 
		  "          download_links: [{\"format\":\"EPUB\",\"size\":\"1.99 MB\",\"href\":\"/downloads/9s161681f\"}],\r\n" + 
		  "          loader_template: '<div class=\"fulcrum-loading\"><div class=\"rect rect1\"></div><div class=\"circle circ1\"></div><div class=\"rect rect2\"></div><div class=\"circle circ2\"></div></div>',\r\n" + 
		  "          metadata: {\r\n" + 
		  "            doi: 'https://hdl.handle.net/2027/fulcrum.9s161681f',\r\n" + 
		  "            location: 'Ann Arbor, MI'\r\n" + 
		  "          }\r\n" + 
		  "        });\r\n" + 
		  "\r\n</script>";

  @Override
  public void setUp() throws Exception {
      super.setUp();
      m_mau = new MockArchivalUnit();
      m_callback = new MyLinkExtractorCallback();
      fact = new UMichHtmlLinkExtractorFactory();
      m_extractor = fact.createLinkExtractor("text/html");
 
    }
 
  
  private Set<String> parseSingleSource(String source)
      throws Exception {
    MockArchivalUnit m_mau = new MockArchivalUnit();
    LinkExtractor ue = new RegexpCssLinkExtractor(); // FIXME why a CSS link extractor?
    m_mau.setLinkExtractor("text/html", ue);
    MockCachedUrl mcu =
      new org.lockss.test.MockCachedUrl("https://www.fulcrum.org/epubs/9s161681f", m_mau);
    mcu.setContent(source);

    m_callback.reset();
    m_extractor.extractUrls(m_mau,
                            new org.lockss.test.StringInputStream(source), ENC,
                            mcu.getUrl(), m_callback);
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
  


/*
  * Optionally use a real html page
 */

  //static final String input_1 = "/tmp/testLinks.html";
  public void testDownloadsFromFormFile() throws Exception {
    // FIXME this test does not seem to test anything
	  /*
    InputStream input1 = new FileInputStream(input_1);
    String s_input1;
    try {
    s_input1 = StringUtil.fromInputStream(input1);
    } finally {
      IOUtil.safeClose(input1);
    }
    Set<String> result_strings = parseSingleSource(s_input1);
    */
    Set<String> result_strings = parseSingleSource(scriptHtmlPartial);
    for (String url : result_strings) {
      log.debug3("from URL: " + url);
    }
  }

  public static final String leafletTileLayerIiifAbsolute =
      "<script>" +
      "\r\n" + 
      "    $().ready(function() {\r\n" + 
      "        var map, layer;\r\n" + 
      "        map = L.map('image', {\r\n" + 
      "            center: [0, 0],\r\n" + 
      "            crs: L.CRS.Simple,\r\n" + 
      "            zoom: 0,\r\n" + 
      "            scrollWheelZoom: false,\r\n" + 
      "        });\r\n" + 
      "        layer = L.tileLayer.iiif(\"http://www.foo.com/image-service/123456789/info.json?1555623447\", { bestFit: true } );\r\n" + 
      "        layer.addTo(map);\r\n" + 
      "        L.control.pan({ panOffset: 150 }).addTo(map);\r\n" + 
      "        // Detect fullscreen toggling\r\n" + 
      "        // Doesn't zoom in/out unless the browser has had a chance\r\n" + 
      "        // to enter fullscreen, hence the timeout.\r\n" + 
      "        // \"TypeError: The expression cannot be converted to return the specified type.\"\r\n" + 
      "        // that prevents fullscreen toggle predates the code below,\r\n" + 
      "        // I suspect a bug in vendor/leaflet.fullscreen-1.5.1/Control.FullScreen.js\r\n" + 
      "        map.on('enterFullscreen', function() {\r\n" + 
      "          setTimeout(function() {\r\n" + 
      "            try { layer._fitBounds(); } catch (err) {}\r\n" + 
      "            }, 1000);\r\n" + 
      "        });\r\n" + 
      "        map.on('exitFullscreen', function() {\r\n" + 
      "          setTimeout(function() {\r\n" + 
      "            try { layer._fitBounds(); } catch (err) {}\r\n" + 
      "            }, 1000);\r\n" + 
      "        });\r\n" + 
      "    });\r\n" +
      "</script>";
  
  public static final String leafletTileLayerIiifBaseUrl =
      "<script>" +
      "        layer = L.tileLayer.iiif(\"/image-service/123456789/info.json?1555623447\", { bestFit: true } );\r\n" + 
      "</script>";
  
  public static final String leafletTileLayerIiifRelative =
      "<script>" +
      "        layer = L.tileLayer.iiif(\"abcdefgh\", { bestFit: true } );\r\n" + 
      "</script>";
  
  public void testScriptTagLeafletTileLayerIiif() throws Exception {
    LinkExtractor le = new UMichHtmlLinkExtractorFactory().createLinkExtractor(Constants.MIME_TYPE_HTML);
    final List<String> res = new ArrayList<String>();
    Callback cb = new Callback() {
      @Override
      public void foundLink(String url) {
        res.add(url);
      }
    };
    
    // Absolute
    le.extractUrls(null,
                   new ByteArrayInputStream(leafletTileLayerIiifAbsolute.getBytes(Constants.ENCODING_UTF_8)),
                   Constants.ENCODING_UTF_8,
                   "http://www.example.com/concern/file_sets/111111111",
                   cb);
    assertEquals(Arrays.asList("http://www.foo.com/image-service/123456789/info.json"),
                 res);
    res.clear();
    
    // Base URL
    le.extractUrls(null,
                   new ByteArrayInputStream(leafletTileLayerIiifBaseUrl.getBytes(Constants.ENCODING_UTF_8)),
                   Constants.ENCODING_UTF_8,
                   "http://www.example.com/concern/file_sets/111111111",
                   cb);
    assertEquals(Arrays.asList("http://www.example.com/image-service/123456789/info.json"),
                 res);
    res.clear();
    
    // Relative
    le.extractUrls(null,
                   new ByteArrayInputStream(leafletTileLayerIiifRelative.getBytes(Constants.ENCODING_UTF_8)),
                   Constants.ENCODING_UTF_8,
                   "http://www.example.com/concern/file_sets/111111111",
                   cb);
    assertEquals(Arrays.asList("http://www.example.com/concern/file_sets/abcdefgh"),
                 res);
    res.clear();
  }
  
}
