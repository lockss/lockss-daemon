/*
 * $Id: TestBMJDrupalHtmlHashFilterFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
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

package org.lockss.plugin.highwire.bmj;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestBMJDrupalHtmlHashFilterFactory extends LockssTestCase {
//  private static String ENC = Constants.DEFAULT_ENCODING;
  
  private BMJDrupalHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new BMJDrupalHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  
  // No relevant content in header/footer
  // new TagNameFilter("header"),
  // new TagNameFilter("footer"),
  private static final String withArticle = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML+RDFa 1.0//EN\">\n" + 
      "<html lang=\"en\" xml:lang=\"en\" style=\"\">\n" +
      "<head profile=\"http://www.w3.org/1999/xhtml/vocab\">\n" + 
      "  <meta charset=\"utf-8\">\n" + 
      "</head>\n" +
      "<body>\n" +
      "    <div id=\"wrapper-outer\">\n" + 
      "  <div id=\"wrapper\">\n" + 
      "    <header>\n" + 
      "     </header><!-- /header -->\n" + 
      "    <div id=\"page\" class=\"main-container container\">\n" + 
      "<div class=\"region region-content\">\n" + 
      "    <section class=\"block block-system clearfix\" id=\"block-system-main\">\n" + 

      "  <div id=\"issue-toc-page\" class=\"panel-display\">\n" + 
      "    <div class=\"row\">\n" + 
      "        <article>\n" + 
      "          <div class=\"panel-pane pane-highwire-markup\">\n" +
      "          </div>\n" + 
      "        </article>\n" + 
      "      <div class=\"right-nav col-xs-12 col-sm-4 col-md-4 col-lg-4\">\n" + 
      "        <aside>\n" + 
      "          <div class=\"panel-pane pane-bmj-issue-pager\">\n" + 
      "          </div>\n" + 
      "        </aside>\n" + 
      "      </div>\n" + 
      "    </div>\n" + 
      "  </div>\n" + 
      "</section> <!-- /.block -->\n" + 
      "  </div>\n" + 
      "    </div><!-- /main-container -->\n" + 

      "          <div class=\"footer-wrapper\">\n" + 
      "        <div class=\"container\">\n" + 
      "          <footer id=\"footer\">\n" + 
      "          </footer> <!-- /footer -->\n" + 
      "        </div>\n" + 
      "          </div><!-- /footer-wrapper -->\n" + 
      "  </div> <!-- /wrapper -->\n" + 
      "</div> <!-- /wrapper-outer -->\n" + 
      "  <script>\n" + 
      "setTimeout(function(){var a=document.createElement(\"script\");\n" + 
      "var b=document.getElementsByTagName(\"script\")[0];\n" + 
      "a.async=true;a.type=\"text/javascript\";b.parentNode.insertBefore(a,b)}, 1);</script>\n" + 
      "</body>\n" +
      "</html>\n";
  private static final String filteredArticle = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML+RDFa 1.0//EN\">\n" + 
      "<html lang=\"en\" xml:lang=\"en\" style=\"\">\n" +
      "\n<body>" +
      "<div id=\"wrapper-outer\">" + 
      "<div id=\"wrapper\">" + 
      "<div id=\"page\" class=\"main-container container\">" + 
      "<div class=\"region region-content\">" + 
      "<section class=\"block block-system clearfix\" id=\"block-system-main\">" + 
      "<div id=\"issue-toc-page\" class=\"panel-display\">" + 
      "<div class=\"row\">" + 
      "<article>\n" + 
      "          <div class=\"panel-pane pane-highwire-markup\">\n" +
      "          </div>\n" + 
      "        </article>" +
      "</div></div></section></div></div></div></div>" +
      "</body>\n" +
      "</html>\n";
  
  
  public void testFiltering() throws Exception {
    InputStream inA;
    String a;
    
    // article
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withArticle),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(filteredArticle, a);
    
  }
  
}
