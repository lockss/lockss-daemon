/*
 * $Id: TestAMetSocHtmlCrawlFilterFactory.java,v 1.1 2013-07-31 21:43:59 alexandraohlson Exp $
 */
package org.lockss.plugin.atypon.americanmeteorologicalsociety;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestAMetSocHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private AMetSocHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AMetSocHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String withCitations = "</div><div class=\"citedBySection\">" +
      "<a name=\"citedBySection\"></a><h2>Cited by</h2>" +
      "<div class=\"citedByEntry\"><span class=\"author\">Robert X</span>, <span class=\"author\">Kenneth X</span>.BIG TITLE HERE. <i>" +
      "<span class=\"NLM_source\">Risk Analysis</span></i>no" +
      "-no<br />Online publication date: 1-Dec-2012.<br /><span class=\"CbLinks\"></span></div>"+
      "</div><!-- /fulltext content --></div>"+
      "       </div></div><div class=\"clearfix\">&nbsp;</div></div>";

  private static final String withoutCitations = "</div><!-- /fulltext content --></div>"+
      "       </div></div><div class=\"clearfix\">&nbsp;</div></div>";
  
  private static final String withOrigArticle = 
      "<h2 class=\"tocHeading\"><span class=\"subj-group\">CORRIGENDUM </span></h2>"+
          "<td valign=\"top\" width=\"85%\">"+
          "<div class=\"art_title\">CORRIGENDUM</div>"+
          "<span class=\"author\">Todd X</span><br />"+
          "<a class=\"ref nowrap\" href=\"/doi/blah\">Citation</a> ."+
          "<a class=\"ref nowrap\" href=\"/doi/blah\">Full Text</a> ."+
          "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/blah1\">PDF (444 KB)</a>"+
          "<span class=\"linkDemarcator\"> | </span>"+
          "<a class=\"ref\" href=\"/doi/abs/blah\">Original Article</a>&nbsp;"+
          "<script type=\"text/javascript\">genSfxLinks('s0', '', 'blah');</script></td>";
  private static final String withoutOrigArticle = 
      "<h2 class=\"tocHeading\"><span class=\"subj-group\">CORRIGENDUM </span></h2>"+
          "<td valign=\"top\" width=\"85%\">"+
          "<div class=\"art_title\">CORRIGENDUM</div>"+
          "<span class=\"author\">Todd X</span><br />"+
          "<a class=\"ref nowrap\" href=\"/doi/blah\">Citation</a> ."+
          "<a class=\"ref nowrap\" href=\"/doi/blah\">Full Text</a> ."+
          "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/blah1\">PDF (444 KB)</a>"+
          "<span class=\"linkDemarcator\"> | </span>"+
          "&nbsp;"+
          "<script type=\"text/javascript\">genSfxLinks('s0', '', 'blah');</script></td>";

  private static final String withCorrigendum =
      "<ul id=\"articleToolsFormats\">"+
          "        <li>"+
          "            <a href=\"/doi/full/blah.1\">"+
          "                Full-text"+
          "            </a>"+
          "        </li>"+
          "        <li>"+
          "            <a href=\"/doi/full/blah.1\" class=\"errata\">"+
          "                Corrigendum&nbsp;"+
          "            </a>"+
          "       </li>"+
          "</ul>";
  private static final String withoutCorrigendum =
      "<ul id=\"articleToolsFormats\">"+
          "        <li>"+
          "            <a href=\"/doi/full/blah.1\">"+
          "                Full-text"+
          "            </a>"+
          "        </li>"+
          "        <li>"+
          "            "+
          "       </li>"+
          "</ul>";


  public void testCitationsFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(withCitations),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutCitations, StringUtil.fromInputStream(actIn));
    
    actIn = fact.createFilteredInputStream(mau, new StringInputStream(withOrigArticle),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutOrigArticle, StringUtil.fromInputStream(actIn));
    
     actIn = fact.createFilteredInputStream(mau, new StringInputStream(withCorrigendum),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutCorrigendum, StringUtil.fromInputStream(actIn));
  }


}
