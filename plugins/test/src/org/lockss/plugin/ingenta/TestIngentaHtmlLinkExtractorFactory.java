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

package org.lockss.plugin.ingenta;

import java.util.Set;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.Constants;
import org.lockss.util.SetUtil;


public class TestIngentaHtmlLinkExtractorFactory extends LockssTestCase {

  private LinkExtractorFactory factJS;
  private LinkExtractorFactory factG;
  private LinkExtractorFactory factIng;
  private LinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  private final static String BASE_URL = "http://www.ingentaconnect.com/";
  Set<String> expectedUrls;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    m_mau = new MockArchivalUnit();
    m_callback = new MyLinkExtractorCallback();
        
    factJS = new JsoupHtmlLinkExtractor.Factory();
    factG = new GoslingHtmlLinkExtractor.Factory();
    factIng = new IngentaBooksHtmlLinkExtractorFactory();

    // MODIFY HERE TO CHANGE EXTRACTORS (1 of 2)
    //m_extractor = factJS.createLinkExtractor("html");
    //m_extractor = factG.createLinkExtractor("html");
    m_extractor = factIng.createLinkExtractor("html");

  }

  
  
  
  private static final String toc_url = "";
  private static final String toc_html = 
    "<html><body>" +
    "</body></html>";
  
  private static final String book_url = BASE_URL + "content/bkpub/2ouacs/2015/00000001/00000001/art00001";
  private static final String book_html = 
      "<html><body>" +
          "<script src=\"ingentabookpage_files/bootstrap.js\"></script>" +
          "<link rel=\"stylesheet\" href=\"ingentabookpage_files/jquery.css\" type=\"text/css\" media=\"screen\">" +
          "<a href=\"http://library.stanford.edu/\">" +
          "<img src=\"ingentabookpage_files/stanford.txt\" alt=\"\"> " +
          "</a>" +
          "<ul><li class=\"social\">" +
          "<a href=\"https://www.linkedin.com/company/ingenta/shareArticle?mini=true&amp;url=http://www.ingentaconnect.com/content/bkpub/2ouacs/2015/00000001/00000001/art00001\" " +
          " class=\"socialIcons\" target=\"_blank\" title=\"link to linkedIn\">" + 
          "<span class=\"fa fa-linkedin-square\"></span></a></li></ul>" +
          "<div class=\"right-col-download contain\">" +
          "<a class=\"fulltext pdf btn btn-general icbutton\" " +
          "onclick=\"javascript:popup('/search/download?pub=infobike%3a%2f%2fbkpub%2f2ouacs%2f2015%2f00000001%2f00000001%2fart00001&amp;mimetype=application%2fpdf&amp;exitTargetId=1463607913143'" +
          ",'downloadWindow','900','800')\" title=\"PDF download of Dare to Serve\">" +
          "<i class=\"fa fa-arrow-circle-o-down\"></i></a>&nbsp;<span class=\"rust\">" +
          "<strong>Download</strong> <br>(PDF 1,664.2 kb)</span>&nbsp;</div>" +
          "</body></html>";
  
     //This test makes sure other base link extraction continues to work
     public void testBookHtml() throws Exception {
   
      Set<String> result_strings = parseSingleSource(book_html, book_url);
      expectedUrls = SetUtil.set(
          "http://www.ingentaconnect.com/content/bkpub/2ouacs/2015/00000001/00000001/ingentabookpage_files/stanford.txt",
          "http://www.ingentaconnect.com/content/bkpub/2ouacs/2015/00000001/00000001/ingentabookpage_files/bootstrap.js",
          "https://www.linkedin.com/company/ingenta/shareArticle?mini=true&url=http://www.ingentaconnect.com/content/bkpub/2ouacs/2015/00000001/00000001/art00001",
          "http://library.stanford.edu/",
          "http://www.ingentaconnect.com/content/bkpub/2ouacs/2015/00000001/00000001/ingentabookpage_files/jquery.css",
          "http://www.ingentaconnect.com/search/download?pub=infobike%3a%2f%2fbkpub%2f2ouacs%2f2015%2f00000001%2f00000001%2fart00001&mimetype=application%2fpdf&exitTargetId=1463607913143"
          );

      assertEquals(6, result_strings.size());
      for (String url : result_strings) {
        log.debug3("URL: " + url);
        assertTrue(expectedUrls.contains(url));
      }
    }
    
    
   /*
     public void testTocHtml() throws Exception {
       
       Set<String> result_strings = parseSingleSource(toc_html, toc_url);
       expectedUrls = SetUtil.set(
       );

       assertEquals(0, result_strings.size());
       for (String url : result_strings) {
         log.debug3("URL: " + url);
         assertTrue(expectedUrls.contains(url));
       }
       
     }
     */

  
  /*------------------SUPPORT FUNCTIONS --------------------- */

       private Set<String> parseSingleSource(String source, String srcUrl)
           throws Exception {
      
         MockArchivalUnit m_mau = new MockArchivalUnit();
         // MODIFY HERE TO CHANGE EXTRACTORS (2 of 2)
         //LinkExtractor ue = new JsoupHtmlLinkExtractor();
         //LinkExtractor ue = new GoslingHtmlLinkExtractor();
         LinkExtractor ue = factIng.createLinkExtractor("html");
         m_mau.setLinkExtractor("html", ue);
         MockCachedUrl mcu =
             new org.lockss.test.MockCachedUrl(srcUrl, m_mau);
         mcu.setContent(source);

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
