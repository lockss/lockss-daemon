/*
 * $Id$
 */
package org.lockss.plugin.atypon.americanmeteorologicalsociety;

import java.io.*;
import org.lockss.util.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;
import org.lockss.test.*;


public class TestAMetSocHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private BaseAtyponHtmlCrawlFilterFactory fact;
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
  
  private static final String againCorrig =
      "<td align=\"right\" valign=\"top\" width=\"18\" class=\"nowrap\">2360</td>" +
      "<td align=\"right\" valign=\"top\" width=\"18\" class=\"tocCheck\">" +
      "<input type=\"checkbox\" name=\"doi\" value=\"10.1175/2007JAS2421.1\"/>" +
      "<img src=\"/templates/jsp/_style2/_AP/images/access_free.gif\" alt=\"open access\" title=\"open access\" class=\"accessIcon\" /></td>" +
      "<td valign=\"top\" width=\"85%\">" +
      "<div class=\"art_title\">Foo</div>" +
      "<span class=\"author\">Marvin A. Geller</span>, <span class=\"author\">Tiehan Zhou</span>, <span class=\"author\">Kevin Hamilton</span><br />" +
      "<a class=\"ref nowrap \" href=\"/doi/abs/10.1175/2007JAS2421.1\">Abstract</a>" +
      "." +
      "<a class=\"ref nowrap\" href=\"/doi/full/10.1175/2007JAS2421.1\">Full Text</a>" +
      "    ." +
      "    <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1175/2007JAS2421.1\">PDF (1068 KB)</a>" +
      "<span class=\"linkDemarcator\"> | </span>" +
      "<a class=\"ref nowrap\" href=\"/doi/full/10.1175/JAS2898.1\">Corrigendum</a>" +
      "&nbsp;<a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl&amp;doi=10.1175/2007JAS2421.1&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dams%26id%3Ddoi%3A10.1175%2F2007JAS2421.1\" title=\"OpenURL STANFORD UNIV. GREEN LIBRARY\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\">" +
      "<img src=\"/userimages/2097/sfxbutton\" alt=\"OpenURL STANFORD UNIV. GREEN LIBRARY\" /></a>" +
      "</td>";
  
  private static final String filteredAgainCorrig =
      "<td align=\"right\" valign=\"top\" width=\"18\" class=\"nowrap\">2360</td>" +
      "<td align=\"right\" valign=\"top\" width=\"18\" class=\"tocCheck\">" +
      "<input type=\"checkbox\" name=\"doi\" value=\"10.1175/2007JAS2421.1\"/>" +
      "<img src=\"/templates/jsp/_style2/_AP/images/access_free.gif\" alt=\"open access\" title=\"open access\" class=\"accessIcon\" /></td>" +
      "<td valign=\"top\" width=\"85%\">" +
      "<div class=\"art_title\">Foo</div>" +
      "<span class=\"author\">Marvin A. Geller</span>, <span class=\"author\">Tiehan Zhou</span>, <span class=\"author\">Kevin Hamilton</span><br />" +
      "<a class=\"ref nowrap \" href=\"/doi/abs/10.1175/2007JAS2421.1\">Abstract</a>" +
      "." +
      "<a class=\"ref nowrap\" href=\"/doi/full/10.1175/2007JAS2421.1\">Full Text</a>" +
      "    ." +
      "    <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1175/2007JAS2421.1\">PDF (1068 KB)</a>" +
      "<span class=\"linkDemarcator\"> | </span>" +
      "" +
      "&nbsp;<a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl&amp;doi=10.1175/2007JAS2421.1&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dams%26id%3Ddoi%3A10.1175%2F2007JAS2421.1\" title=\"OpenURL STANFORD UNIV. GREEN LIBRARY\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\">" +
      "<img src=\"/userimages/2097/sfxbutton\" alt=\"OpenURL STANFORD UNIV. GREEN LIBRARY\" /></a>" +
      "</td>";

  private static final String inLineCrossRef =
      "<div class=\"NLM_author-notes\">" +
          "<a name=\"\">" +
          "</a>" +
          "<div class=\"NLM_corresp\">" +
          "<a name=\"cor1\">" +
          "</a>" +
          "<i>" +
          "Corresponding author address:</i>" +
          " Prof. Smarty Pants, 2455 Hayward St., E-mail: <a href=\"mailto:smarty@pants.edu\" class=\"email\">" +
          "smarty@pants.edu</a>" +
          "</div>" +
          "<a name=\"n301\">" +
          "</a>" +
          "<p class=\"first last\">" +
          "The original article that was the subject of this comment/reply can be found at " +
          "<a target=\"_blank\" href=\"http://journals.ametsoc.org/doi/full/10.1175/xxx\">" +
          "http://journals.ametsoc.org/doi/full/10.1175/xxx</a>" +
          ".</p>" +
          "</div>";


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
    
    actIn = fact.createFilteredInputStream(mau, new StringInputStream(againCorrig),
        Constants.DEFAULT_ENCODING);
    assertEquals(filteredAgainCorrig, StringUtil.fromInputStream(actIn));

    actIn = fact.createFilteredInputStream(mau, new StringInputStream(inLineCrossRef),
        Constants.DEFAULT_ENCODING);
    assertEquals("", StringUtil.fromInputStream(actIn));
    
    
  }


}
