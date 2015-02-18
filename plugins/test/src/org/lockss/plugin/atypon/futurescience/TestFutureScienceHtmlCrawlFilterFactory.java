/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.futurescience;

import java.io.*;

import junit.framework.Test;

import org.lockss.util.*;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.*;

public class TestFutureScienceHtmlCrawlFilterFactory extends LockssTestCase {
  private static FilterFactory fact;
  private static MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new FutureScienceHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String alsoReadHtml =
      "<td><div class=\"full_text\">" +
          "<div class=\"header_divide\"><h3>Users who read this article also read:</h3></div>" +
          "<table border=\"0\" width=\"100%\" class=\"articleEntry\"><tr>" +
          "<td align=\"right\" valign=\"top\" width=\"18\"><input type=\"checkbox\" name=\"doi\" value=\"10.4155/tde.12.122\"/><br /></td>" +
          "<td valign=\"top\"><div class=\"art_title\">Title </div>" +
          "<a class=\"ref nowrap\" href=\"/doi/abs/10.4155/tde.12.122\">Citation</a>" +
          "    | <a class=\"ref nowrap\" href=\"/doi/full/10.4155/tde.12.122\">Full Text</a>" +
          "    | <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.4155/tde.12.122\">PDF (1093 KB)</a>" +
          "    | <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdfplus/10.4155/tde.12.122\">PDF Plus (1103 KB)</a>" +
          "    | <a class=\"ref\" href=\"/personalize/addFavoriteArticle?doi=10.4155%2Ftde.12.122\">Add to Favorites</a>" +
          "    | <a class=\"ref\" href=\"/action/doSearch?doi=10.4155%2Ftde.12.122&amp;target=related\">Related</a>&nbsp;<!-- ${xml_link: 10.4155%2Ftde.12.122} -->" +
          "   | <a class=\"ref nowrap\" href=\"javascript:void(0)\" title=\"Opens new window\" onclick=\"window.open('/action/showReprints\">" +
          " Reprints &amp; Permissions </a>" +
          " <script type=\"text/javascript\"> genSfxLinks('s0', '', '10.4155/tde.12.122');</script></td><td valign=\"top\"></td></tr></table>" +
          "  </div></td><td width=\"10\">&nbsp;</td>";
  private static final String alsoReadHtmlFiltered =
      "<td></td><td width=\"10\">&nbsp;</td>";
  
  private static final String citedBySection =
      "</div><!-- /abstract content --><!-- fulltext content -->" +
      "<div class=\"citedBySection\"><a name=\"citedBySection\"></a><h2>Cited by</h2>" +
      "<div class=\"citedByEntry\">" +
      "<a href=\"/action/foo\">Author</a>.  (2012) Recent Title. <i><span class=\"NLM_source\">Bioanalysis</span></i> <b>4</b>:9, 1123-1140<br />" +
      "Online publication date: 1-May-2012.<br /><span class=\"CbLinks\"><a class=\"ref nowrap\" href=\"/doi/abs/10.4155/bio.xx.73\">Summary</a>" +
      " | <a class=\"ref nowrap\" href=\"/doi/full/10.4155/bio.xx.73\">Full Text</a>" +
      " | <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.4155/bio.xx.73\">PDF (1157 KB)</a>" +
      " | <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdfplus/10.4155/bio.xx.73\">PDF Plus (1157 KB)</a>" +
      "&nbsp;<!-- ${cadmus-articleworks: 10.4155%2Fbio.12.73 class='ref' } --><!-- ${xml_link: 10.4155%2Fbio.12.73} -->" +
      " | <a class=\"ref nowrap\" href=\"javascript:void(0)\" title=\"Opens new window\" onclick=\"window.open('/action/showReprints', '_blank', 'width=950,height=800')\">" +
      " Reprints &amp; Permissions" +
      "</a></span></div>" +
      "</div><!-- /fulltext content -->" +
      "<div class=\"article_link\">"; 
      private static final String citedBySectionFiltered =
      "</div><!-- /abstract content --><!-- fulltext content -->" +
      "<!-- /fulltext content -->" +
      "<div class=\"article_link\">"; 
      
      private static final String referencesHtml =
          "<img src=\"/templates/jsp/images/arrow_up.gif\" width=\"11\" height=\"9\" border=\"0\" hspace=\"5\" alt=\"Previous section\"/>" +
              "</a>" +
              "</td>" +
              "</tr>" +
              "</table>" +
              "<table border=\"0\" class=\"references\">" +
              "<tr>" +
              "<td class=\"refnumber\" id=\"ref-1\">1 .</td>" +
              "<td valign=\"top\">" +
              "<span class=\"name noWrap\">Heuck</span>" +
              "<span class=\"wbr\">&#8203;&#8204;</span> G. Zwei auf Deutsch. <span class=\"citation_source-journal\">" +
              "<i>Virchows Arch.</i>" +
              "</span>78,<span class=\"NLM_fpage\">475</span> (<span class=\"NLM_year\">1879</span>). " +
              "<script type=\"text/javascript\">genRefLink(16, 'ref-1', '10.1007%2FBF01878089');</script> </td></tr>" +
              "<tr>" +
              "<td class=\"refnumber\" id=\"ref-2\">2 .</td>" +
              "<td valign=\"top\">" +
              "<span class=\"name noWrap\">Vardiman</span>" +
              "<span class=\"wbr\">&#8203;&#8204;</span> JW, Thiele J, Arber DA <i>et al.</i> Another Title. <span class=\"citation_source-journal\">" +
              "<i>Blood</i>" +
              "</span>114(5),<span class=\"NLM_fpage\">937</span>–951 (<span class=\"NLM_year\">2009</span>). " +
              "<script type=\"text/javascript\">genRefLink(16, 'ref-2', '10.1182%2Fblood-2009-03-209262');</script></td></tr>" +
              "</table>" +
              "DON'T REMOVE" +
              "<span class=\"title2\" id=\"d634692e2883\">Website</span> <table border=\"0\" class=\"references\">" +
              "<tr>" +
              "<td class=\"refnumber\" id=\"ref-101\">101 .</td>" +
              "<td valign=\"top\">RESPONSE trial. <a href=\"http://www.example.com\" target=\"_blank\">www.example.com</a> </td>" +
              "</tr>" +
              "</table>" +
              "<h2 style=\"margin-top:1em\">Affiliations</h2>";
      
      private static final String referencesHtmlFiltered =
          "<img src=\"/templates/jsp/images/arrow_up.gif\" width=\"11\" height=\"9\" border=\"0\" hspace=\"5\" alt=\"Previous section\"/>" +
              "</a>" +
              "</td>" +
              "</tr>" +
              "</table>" +
              "DON'T REMOVE" +
              "<span class=\"title2\" id=\"d634692e2883\">Website</span> " +
              "<h2 style=\"margin-top:1em\">Affiliations</h2>";
      
      private static final String articleNav=
          "<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" class=\"breadcrumbs\">" +
              "<tbody><tr>" +
              "<td><a href=\"/\">Home</a></td>" +
              "<td>&nbsp;&gt;</td><td></td>" +
              "<td nowrap=\"nowrap\"><a href=\"/loi/test\">Journal home</a></td>" +
              "<td>&nbsp;&gt;</td><td></td>" +
              "<td nowrap=\"nowrap\"><a href=\"/toc/test/6/2\">TOC</a></td>" +
              "<td>&nbsp;&gt;</td><td></td>" +
              "<td nowrap=\"nowrap\">Full Text</td>" +
              "</tr></tbody></table>";
      private static final String articleNavFiltered=
          "";
      private static final String breadcrumb=
          "<br>" +
              "<a href=\"/doi/full/10.1111/test.13.197\">Prev. Article</a>" +
              "|" +
              "<a href=\"/doi/full/10.1111/test.13.201\">Next Article</a>" +
              "<br>"; 
      private static final String breadcrumbFiltered=
          "<br>" +
              "|" +
              "<br>"; 
      


 //Variant to test with Crawl Filter
 public static class TestCrawl extends TestFutureScienceHtmlCrawlFilterFactory {
          
          public void setUp() throws Exception {
                  super.setUp();
                  fact = new FutureScienceHtmlCrawlFilterFactory();
          }

  }
 
  
  public void testHtmlCitedBy() throws Exception {
    InputStream actIn1 = fact.createFilteredInputStream(mau,
        new StringInputStream(citedBySection), Constants.DEFAULT_ENCODING);

    assertEquals(citedBySectionFiltered, StringUtil.fromInputStream(actIn1));
  }
  
  public void testLinks() throws Exception {
    InputStream actIn1 = fact.createFilteredInputStream(mau,
        new StringInputStream(articleNav), Constants.DEFAULT_ENCODING);

    assertEquals(articleNavFiltered, StringUtil.fromInputStream(actIn1));

    actIn1 = fact.createFilteredInputStream(mau,
        new StringInputStream(breadcrumb), Constants.DEFAULT_ENCODING);

    assertEquals(breadcrumbFiltered, StringUtil.fromInputStream(actIn1));
    
  }
  
  public void testAlsoRead() throws Exception {
    InputStream actIn1 = fact.createFilteredInputStream(mau,
        new StringInputStream(alsoReadHtml), Constants.DEFAULT_ENCODING);

    assertEquals(alsoReadHtmlFiltered, StringUtil.fromInputStream(actIn1));
  }
  
  public void testReferences() throws Exception {
    InputStream actIn1 = fact.createFilteredInputStream(mau,
        new StringInputStream(referencesHtml), Constants.DEFAULT_ENCODING);

    assertEquals(referencesHtmlFiltered, StringUtil.fromInputStream(actIn1));
  }
  
}
