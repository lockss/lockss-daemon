/*
 * $Id$
 */

/* Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University, all rights reserved.

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
package org.lockss.plugin.atypon.apha;


import java.io.InputStream;

import junit.framework.Test;

import org.lockss.plugin.FilterFactory;
import org.lockss.plugin.bioone.BioOneAtyponHtmlHashFilterFactory;
import org.lockss.plugin.bioone.TestBioOneAtyponHtmlFilterFactory;
import org.lockss.test.*;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestAmPublicHealthAssocHtmlFilterFactory extends LockssTestCase{
  static String ENC = Constants.DEFAULT_ENCODING;

  public FilterFactory fact;
  public MockArchivalUnit mau;
  
  public static class TestCrawl extends TestAmPublicHealthAssocHtmlFilterFactory {
    
 
     // toc to original article
    private static final String tocErrataHtml1 = 
        "<table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
        "<tbody><tr><td valign=\"top\">" +
        "<div class=\"art_title\">ERRATUM</div>" +
        "<div class=\"art_meta\">American Journal of Public Health: January 2011, Vol. 101, No. 1:  5-5.</div>" +
        "<a class=\"ref nowrap\" href=\"/doi/abs/10.2105/AJPH.2009.blahe\">Citation</a> |" + 
        "<a class=\"ref\" href=\"/doi/abs/10.2105/AJPH.2009.blah\">Original Article</a>&nbsp;" +
        "</td></tr></tbody></table>";
    private static final String tocErrataHtml1Filtered = 
        "<table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
        "<tbody><tr><td valign=\"top\">" +
        "<div class=\"art_title\">ERRATUM</div>" +
        "<div class=\"art_meta\">American Journal of Public Health: January 2011, Vol. 101, No. 1:  5-5.</div>" +
        "<a class=\"ref nowrap\" href=\"/doi/abs/10.2105/AJPH.2009.blahe\">Citation</a> |" + 
        "&nbsp;" +
        "</td></tr></tbody></table>";
       
    // toc to erratum
   private static final String tocErrataHtml4=
   "<div class=\"art_title\">Examination of Stuff</div>" +
   "<span class=\"linkDemarcator\"> | </span>" +
   "<a class=\"ref nowrap\" href=\"/doi/abs/10.2105/AJPH.2009.blahe\">Erratum</a>";
   private static final String tocErrataHtml4Filtered=
   "<div class=\"art_title\">Examination of Stuff</div>" +
   "<span class=\"linkDemarcator\"> | </span>";
   
   
     // article page to erratum
    private static final String articleErrataHtml2 =
        "<ul id=\"articleToolsFormats\"><li>" +
            "<a href=\"/doi/full/10.2105/AJPH.2009.blahe\">" +
            "             Erratum" +
            "</a>" +
            "</li></ul>";
    private static final String articleErrataHtml2Filtered =
        "<ul id=\"articleToolsFormats\"><li>" +
            "</li></ul>";

       // article page to original 
    private static final String articleErrataHtml3 =
        "<ul id=\"articleToolsFormats\">" +
        "<li><a href=\"/doi/pdf/10.2105/ajph.2009.blahe\">PDF</a></li>" +
        "<li><a href=\"/doi/full/10.2105/ajph.2009.blah\">Original</a></li>" +
        "</ul>";
    private static final String articleErrataHtml3Filtered =
        "<ul id=\"articleToolsFormats\">" +
        "<li><a href=\"/doi/pdf/10.2105/ajph.2009.blahe\">PDF</a></li>" +
            "<li></li>" +
        "</ul>";
     


    public void setUp() throws Exception {
      super.setUp();
      fact = new AmPublicHealthAssocHtmlCrawlFilterFactory();
    }

    public void testTOCCrawlFiltering() throws Exception {
      InputStream inA;
      InputStream inB;


      inA = fact.createFilteredInputStream(mau, new StringInputStream(tocErrataHtml1),
          ENC);
      assertEquals(tocErrataHtml1Filtered,StringUtil.fromInputStream(inA));

      inA = fact.createFilteredInputStream(mau, new StringInputStream(tocErrataHtml4),
          ENC);
            assertEquals(tocErrataHtml4Filtered,StringUtil.fromInputStream(inA));
    }

    public void testArticleCrawlFiltering() throws Exception {
      InputStream inA;
      InputStream inB;


      inA = fact.createFilteredInputStream(mau, new StringInputStream(articleErrataHtml2),
          ENC);
      assertEquals(articleErrataHtml2Filtered,StringUtil.fromInputStream(inA));

      inA = fact.createFilteredInputStream(mau, new StringInputStream(articleErrataHtml3),
          ENC);
            assertEquals(articleErrataHtml3Filtered,StringUtil.fromInputStream(inA));
    }
    
  }
  
  public static class TestHash extends TestAmPublicHealthAssocHtmlFilterFactory {

    private static final String footerHtml =
        "<start><div id=\"footer\">" +
            "<!-- ============= start snippet ============= -->" +
            "<div><cite>American Journal of Public Health<span class=\"fontSize1\"></div>" +
            "<div>Print ISSN: 0090-0036 | Electronic ISSN: 1541-0048</div>" +
            "<div>Copyright © 2012 by the <a class=\"inserted\" target=\"_blank\" title=\"APHA home\" " +
            "href=\"http://www.apha.org\">American Public Health Association</a><span class=\"fontSize1\"></div>" +
            "<!-- ============= end snippet ============= -->" +
            "<div id=\"atyponNote\">" +
            "    Powered by <a href=\"http://www.atypon.com\">Atypon&reg; Literatum</a>" +
            "</div>" +
            "</div>" +
            "</div>" +
            "</body><end>";

    private static final String footerHtmlFiltered =
        "<start></div>" +
            "</body><end>";
    public void setUp() throws Exception {
      super.setUp();
      fact = new AmPublicHealthAssocHtmlHashFilterFactory();
    }


    
    
    public void testHashFiltering() throws Exception {
      InputStream actIn = fact.createFilteredInputStream(mau,
          new StringInputStream(footerHtml),
          Constants.DEFAULT_ENCODING);

      assertEquals(footerHtmlFiltered, StringUtil.fromInputStream(actIn));
    }
      
  }
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
    });
  }

}

