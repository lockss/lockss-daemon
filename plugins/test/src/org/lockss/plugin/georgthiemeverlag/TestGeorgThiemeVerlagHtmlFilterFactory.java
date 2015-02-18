/*
/    * $Id$
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.georgthiemeverlag;

import java.io.*;

import org.htmlparser.filters.TagNameFilter;
import org.lockss.util.*;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.test.StringInputStream;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.test.*;


public class TestGeorgThiemeVerlagHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.ENCODING_UTF_8;

  private GeorgThiemeVerlagHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new GeorgThiemeVerlagHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String withStuff = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" + 
    "<meta node=\"phvmthieme02\">\n" + 
    "<meta charset=\"utf-8\">\n" + 
    "<title>Thieme E-Journals - Full Text</title>\n" + 
    "<script src=\"/js/libs/modernizr-2.0.6.min.js\"></script><script src=\"/js/libs/jquery-1.6.2.min.js\">" +
    "</script>\n" + 
    "</head>" +
"<body>" +
    "<header id=\"pageHeader\">\n" + 
    "<div id=\"topHeaderBar\">\n" + 
    "<ul role=\"navigation\" id=\"metaNavigation\">\n" + 
    "<li class=\"langSwitch2German\">\n" + 
    "<a href=\"/ejournals/html/10.1055/s-0032-1331310?locale=de&amp;LgSwitch=1\">DE</a>\n" + 
    "</li>\n" + 
    "<li class=\"langSwitch2English\">\n" + 
    "<span>EN</span>\n" + 
    "</li>\n" + 
    "<li>\n" + 
    "<a href=\"/ejournals/home.html\">Home</a>\n" + 
    "</li>\n" + 
    "<li class=\"hasChildren\">\n" + 
    "<span>Journals</span>\n" + 
    "<ul class=\"linkList dropMenu\">\n" + 
    "<li>\n" + 
    "<a href=\"/ejournals/subjects\">Subject Collection</a>\n" + 
    "</li>\n" + 
    "<li>\n" + 
    "<a href=\"/ejournals/journals\">Alphabetical List</a>\n" + 
    "</li>\n" + 
    "</ul>\n" + 
    "<div id=\"institutionName\">CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit.</div>\n" + 
    "</header>" +
    "<a name=\"N65771\"></a>" +
    "nonstuff\n" +
    "<footer>\n" + 
    "<div id=\"pageEnd\">\n" + 
    "<a href=\"#top\">Top of Page</a>\n" + 
    "</div>\n" + 
    "<div class=\"clearfix\">\n" + 
    "<div style=\"margin-left:40px\" id=\"footerCenter\">&copy; 2012 Georg Thieme Verlag KG | " +
    "<a href=\"/ejournals/impressum\">Impressum</a> | <a href=\"/ejournals/datenschutz\">Privacy</a>\n" + 
    "</div>\n" + 
    "<div id=\"footerRight\"></div>\n" + 
    "</div>\n" +
    "</footer>" +
    "<script type=\"text/javascript\"> $(function () { initTabBar({ open: 0}); }); </script>\n" + 
"</body></html>";

  private static final String withoutStuff = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
"<body>" +
  "nonstuff " +
"</body></html>";
  
  private static final String withAnchorHref = 
"<ul class=\"articleTocList\">" +
"<li>\n" + 
"<a href=\"#N65623\">Zusammenfassung</a>\n" + 
"</li>\n" + 
"</ul>\n" +
"<a href=\"#N65623\">Zusammenfassung</a>\n";

  private static final String withoutAnchorHref = 
"\n<a href=\"#\">Zusammenfassung</a>\n";
  
  public void testFiltering() throws Exception {
    assertFilterTo(withoutStuff, withStuff);
    assertFilterTo(withoutAnchorHref, withAnchorHref);
  }

  private void assertFilterTo(String expected, String str) throws Exception {
    StringInputStream mis = new StringInputStream(expected);
    InputStream filteredStream = new HtmlFilterInputStream (mis, Constants.ENCODING_UTF_8,
         HtmlNodeFilterTransform.exclude(new TagNameFilter("fake")));
    InputStream inA = new ReaderInputStream(new WhiteSpaceFilter(FilterUtil.getReader(
        filteredStream, Constants.ENCODING_UTF_8)));
    String a = StringUtil.fromInputStream(inA);
    InputStream inB = fact.createFilteredInputStream(mau, new StringInputStream(str),
        Constants.ENCODING_UTF_8);
    String b = StringUtil.fromInputStream(inB);
    assertEquals(a,b);
  }

}
