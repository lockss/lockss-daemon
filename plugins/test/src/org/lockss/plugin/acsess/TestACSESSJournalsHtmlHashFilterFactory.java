/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.acsess;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.*;

public class TestACSESSJournalsHtmlHashFilterFactory extends LockssTestCase {
  private ACSESSJournalsHtmlHashFilterFactory fact;
  private MockArchivalUnit aau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new ACSESSJournalsHtmlHashFilterFactory();
  }

  private static final String filteredStr = 
      "<div class=\"acsMarkLogicWrapper\">" +
      "</div>";
  
  private void doFilterTest(ArchivalUnit au, 
      FilterFactory fact, String nameToHash, String expectedStr) 
          throws PluginException, IOException {
    InputStream actIn; 
    actIn = fact.createFilteredInputStream(au, 
        new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);
    // for debug
    // String actualStr = StringUtil.fromInputStream(actIn);
    // assertEquals(expectedStr, actualStr);
    assertEquals(expectedStr, StringUtil.fromInputStream(actIn));
  }
  
  private static final String withScript =
      "<div class=\"block\">" +
          "<div class=\"acsMarkLogicWrapper\">" +
          "<script type=\"text/javascript\">" +
          "var _gaq = _gaq || [];" +
          "blah blah;" +
          "</script>" +
          "</div>" +
          "</div>";
 
  private static final String withComments =
      "<div class=\"block\">" +
          "<div class=\"acsMarkLogicWrapper\">" +
          "<!-- comment comment comment -->" +
          "</div>" +
          "</div>";
  
  private static final String withNoPrint =
      "<div class=\"block\">" +
          "<div class=\"acsMarkLogicWrapper\">" +
          "<div class=\"noPrint\" style=\"display: clear: both;\">" +
          "<div>" +
          "<a rel=\"nofollow\" href=\"http://www.facebook.com/links\">" +
          "<img src=\"/files/images/icons/share-fb.png\">" +
          "</a>" +
          "<a rel=\"nofollow\" href=\"http://twitter.com/links\">" +
          "<img src=\"/files/images/icons/tweet.png\">" +
          "</a>" +
          "</div></div></div>" +
          "</div>";
  
  private static final String withCbContents =
      "<div class=\"block\">" +
          "<div id=\"article-cb-main\" class=\"content-box\">" +
          "<div class=\"cb-contents\" style=\"text-align: left;\">" +
          "<h3 class=\"cb-contents-header\">" +
          "<span>This article in AJ</span>" +
          "</h3>" +
          "<div style=\"background-color: #eee; font-size: 12px; margin:0; " +
          "padding: 5px; font-weight: bold;\"> View</div>" +
          "<div style=\"margin: 0px; padding: 0px 5px\">" +
          "<a href=\"/publications/aj/abstracts/106/1/24\">»Abstract</a>" +
          "</div>" +
          "<div style=\"margin: 0px; padding: 0px 5px\">" +
          "<a href=\"/publications/aj/articles/106/1/24\">»Full Text</a>" +
          "</div>" +
          "<div style=\"background-color: #eee; font-size: 12px; margin:0; " +
          "padding: 5px; font-weight: bold;\"> Download</div>" +
          "<div style=\"margin: 0px; padding: 0px 0px 0px 5px;\">" +
          "<a href=\"/publications/citation-manager/prev/zt/aj/106/1/24\">Citation</a>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>";
  
  private static final String cbContentsFilteredStr =
      "<div id=\"article-cb-main\" class=\"content-box\">" +
          "<div class=\"cb-contents\" style=\"text-align: left;\">" +
          "<div style=\"margin: 0px; padding: 0px 0px 0px 5px;\">" +
          "<a href=\"/publications/citation-manager/prev/zt/aj/106/1/24\">Citation</a>" +
          "</div>" +
          "</div>" +
          "</div>";

    private static final String withCitationFooter =
        "<body class=\"not-front page-publications no-sidebars\">" +
            "<div id=\"content\">" +
            "<h1>Citation Manager Download</h1>" +
            "</div>" +
            "<div id=\"footer\">" +
            "<div style=\"margin: 10px;\">" +
            "<a href=\"/copyright/\">Copyright Information, and Terms of Use</a>" +
            "</div>" +
            "</div>" +
            "</body>";
    private static final String citationFooterFilteredStr =
        "<body class=\"not-front page-publications no-sidebars\">" +
            "<div id=\"content\">" +
            "<h1>Citation Manager Download</h1>" +
            "</div>" +
            "</body>";
    
    private static final String withOpenAccess =
        "<div class=\"block\">" +
            "<div class=\"acsMarkLogicWrapper\">" +
            "<div class=\"openAccess\">OPEN ACCESS</div>" +
            "</div>" +
            "</div>";
    
    private static final String withArticleFootnotes =
        "<div class=\"block\">" +
            "<div class=\"acsMarkLogicWrapper\">" +
            "<div id=\"articleFootnotes\">" +
            "<h2>Footnotes</h2>" +
            "<ul style=\"margin-top: 0; padding-top: 0;\">" +
            "<li class=\"copyright-statement\">" +
            "<span>Copyright © 2014 by the A Journal Inc.All rights reserved." +
            "<div style=\"clear: both;\"></div></span>" +
            "</li></ul></div>" +
            "</div>" +
            "</div>";
        
  public void testFiltering() throws Exception {
    doFilterTest(aau, fact, withScript, filteredStr);
    doFilterTest(aau, fact, withComments, filteredStr);
    doFilterTest(aau, fact, withNoPrint, filteredStr);
    doFilterTest(aau, fact, withCbContents, cbContentsFilteredStr);
    doFilterTest(aau, fact, withCitationFooter, citationFooterFilteredStr);
    doFilterTest(aau, fact, withOpenAccess, filteredStr);
    doFilterTest(aau, fact, withArticleFootnotes, filteredStr);
  }

}
