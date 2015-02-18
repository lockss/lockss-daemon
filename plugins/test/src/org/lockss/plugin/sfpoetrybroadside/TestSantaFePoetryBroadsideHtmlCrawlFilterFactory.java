/*
 * $Id$
 */
package org.lockss.plugin.sfpoetrybroadside;

import java.io.InputStream;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestSantaFePoetryBroadsideHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private SantaFePoetryBroadsideHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new SantaFePoetryBroadsideHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String bioPage =
      "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\" " +
       "   \"http://www.w3.org/TR/REC-html40/loose.dtd\"> " +
    "<html>" +
    "<head>" +
    "<title>Santa Fe Poetry Broadside... Bionotes, Issue #1</title>" +
    "</head>" +
    "<body bgcolor=\"ffffff\">" +
    "<a href=\"broadside.html\" title=\"Index of all issues\"><img alt=\"Santa Fe Poetry Broadside\"" +
    "src=\"smbanner.gif\"></a> " +
    "<center>" +
    "<h2>About the Poets</h2>" +
    "</center>" +
    "<dl>" +
    "<dt>" +
    "<a name=\"chavezc\"><b>Margo Chavez-Charles</b></a>" +
    "<dd>Sam Q Author teaches" +
    "<dd><small><b>poems in the Broadside, this issue...</b><a href=\"author.html\"><i>BOO</i></a></small>" +
    "<dd><small><b>poem in the Broadside, issue #36... </b><a href=\"author3.html\"><i>BOO2</i></a></small>" +
    "<p>" +
    "<dt>" +
    "<br>" +
    "</dl>" +
    "</body>" +
    "</html>";

  private static final String bioPageFiltered =
      "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\" " +
          "   \"http://www.w3.org/TR/REC-html40/loose.dtd\"> " +
       "<html>" +
       "<head>" +
       "<title>Santa Fe Poetry Broadside... Bionotes, Issue #1</title>" +
       "</head>" +
       "</html>";

  private static final String notBioPage =
      "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" " +
          "     \"http://www.w3.org/TR/REC-html4/loose.dtd\">  " +
          "<html>" +
          "<head>  " +
          "<title>Santa Fe Poetry Broadside... Poets of the Region and Beyond</title>  " +
          "<meta http-equiv=\"content-type\" content=\"text/html; charset=iso-8859-1\">  " +
          "<meta name=\"Description\" content=\"Santa Fe Poetry Broadside... Poets of the Region and Beyond\">  " +
          "<link rel=\"stylesheet\" href=\"broadside.css\" type=\"text/css\">  " +
          "</head> " +
          "<body>  " +
          "<div id=\"header\">  " +
          "<img alt=\"Santa Fe Poetry Broadside... Poets of the Region and Beyond\"  " +
          "src=\"banner.gif\"  width=\"681\" height=\"112\" />  " +
          "<div id=\"navmenu\">  " +
          "<ul>  " +
          "<li><a href=\"broadside.html\" title=\"Index of all issues\">All Issues</a></li>  " +
          "<li><a href=\"about.html\">About the <em>Broadside</em></a></li>  " +
          "<li><a href=\"bio46.html\">Bionotes</a></li> <li><a href=\"contact.html\">How to reach us</a></li>  " +
          "<li><a href=\"links.html\">Links</a></li> <li><a href=\"about.html#copy\">Copyright</a></li>  " +
          "</ul>  " +
          "</div>  " +
          "<hr width=\"70%\">   " +
          "Issue #46, June, 2006 :  " +
          "</div> " +
          "</body> " +
          "</html>  ";

  private static final String bioPageNoHtml =
      "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"" +
          "      \"http://www.w3.org/TR/REC-html4/loose.dtd\">" +
          "<head>" +
          "<title>Santa Fe Poetry Broadside... Bionotes</title>" +
          "<meta http-equiv=\"content-type\" content=\"text/html; charset=iso-8859-1\">" +
          "<meta name=\"Description\" content=\"Santa Fe Poetry Broadside... Poets of the Region and Beyond\">  " +
          "<link rel=\"stylesheet\" href=\"broadside.css\" type=\"text/css\">" +
          "</head>" +
          "<body>" +
          "<div id=\"header\">" +
          "<hr width=\"70%\">" +
          "<h4>" +
          "Santa Fe Poetry Broadside... Issue #47, August, 2006 :</h4>" +
          "</div>" +
          "" +
          "<a name=\"logghe\"></a><br />" +
          "<span class=\"author2\">Joan Logghe</span><br />" +
          "<div class=\"indent2\">" +
          "Joan Logghe of Espanola is a master performer and teacher of poetry. She has many books " +
          "and publications to her credit, including <span class=\"italic\">Blessed Resistance</span>, and " +
          "her latest, <span class=\"italic\">Rice</span> from Tres Chicas Books." +
          "<div class=\"bio2\">" +
          "Poem in the Broadside, this issue... <span class=\"italic\"><a href=\"whatis.html\">What Is</a></span><br />" +
          "Poems in the Broadside, issue #17: <a href=\"sept00.html\">After Horses</a> (twelve poems)<br />" +
          "Poems in the Broadside, issue #1... <a href=\"logghe1.html\"><i>from \"The Rice Sonnets\"</i></a>" +
          "-- <a href=\"logghe2.html\"><i>Velvet</i></a>" +
          "</div>" +
          "</div>" +
          "</body>" +
          "</html>";


  private static final String bioPageNoHtmlFiltered =
      "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"" +
          "      \"http://www.w3.org/TR/REC-html4/loose.dtd\">" +
          "<head>" +
          "<title>Santa Fe Poetry Broadside... Bionotes</title>" +
          "<meta http-equiv=\"content-type\" content=\"text/html; charset=iso-8859-1\">" +
          "<meta name=\"Description\" content=\"Santa Fe Poetry Broadside... Poets of the Region and Beyond\">  " +
          "<link rel=\"stylesheet\" href=\"broadside.css\" type=\"text/css\">" +
          "</head>" +
          "<body>" +
          "<div id=\"header\">" +
          "<hr width=\"70%\">" +
          "<h4>" +
          "Santa Fe Poetry Broadside... Issue #47, August, 2006 :</h4>" +
          "</div>" +
          "" +
          "<a name=\"logghe\"></a><br />" +
          "<span class=\"author2\">Joan Logghe</span><br />" +
          "<div class=\"indent2\">" +
          "Joan Logghe of Espanola is a master performer and teacher of poetry. She has many books " +
          "and publications to her credit, including <span class=\"italic\">Blessed Resistance</span>, and " +
          "her latest, <span class=\"italic\">Rice</span> from Tres Chicas Books." +
          "</div>" +
          "</body>" +
          "</html>";



  private static final String notBioPageNoHtml =
      "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" " +
          "     \"http://www.w3.org/TR/REC-html4/loose.dtd\">  " +
          "<head>  " +
          "<title>Santa Fe Poetry Broadside... Poets of the Region and Beyond</title>  " +
          "<meta http-equiv=\"content-type\" content=\"text/html; charset=iso-8859-1\">  " +
          "<meta name=\"Description\" content=\"Santa Fe Poetry Broadside... Poets of the Region and Beyond\">  " +
          "<link rel=\"stylesheet\" href=\"broadside.css\" type=\"text/css\">  " +
          "</head> " +
          "<body>  " +
          "<div id=\"header\">  " +
          "<img alt=\"Santa Fe Poetry Broadside... Poets of the Region and Beyond\"  " +
          "src=\"banner.gif\"  width=\"681\" height=\"112\" />  " +
          "<div id=\"navmenu\">  " +
          "<ul>  " +
          "<li><a href=\"broadside.html\" title=\"Index of all issues\">All Issues</a></li>  " +
          "<li><a href=\"about.html\">About the <em>Broadside</em></a></li>  " +
          "<li><a href=\"bio46.html\">Bionotes</a></li> <li><a href=\"contact.html\">How to reach us</a></li>  " +
          "<li><a href=\"links.html\">Links</a></li> <li><a href=\"about.html#copy\">Copyright</a></li>  " +
          "</ul>  " +
          "</div>  " +
          "<hr width=\"70%\">   " +
          "Issue #46, June, 2006 :  " +
          "</div> " +
          "</body> " +
          "</html>  ";


  public void testBioPageFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(bioPage),
        Constants.DEFAULT_ENCODING);
    assertEquals(bioPageFiltered, StringUtil.fromInputStream(actIn));

    actIn = fact.createFilteredInputStream(mau, new StringInputStream(notBioPage),
        Constants.DEFAULT_ENCODING);
    // this shouldn't change at all
    assertEquals(notBioPage, StringUtil.fromInputStream(actIn));

  }

  public void testLateIssueBioPageFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(bioPageNoHtml),
        Constants.DEFAULT_ENCODING);
    assertEquals(bioPageNoHtmlFiltered, StringUtil.fromInputStream(actIn));

    actIn = fact.createFilteredInputStream(mau, new StringInputStream(notBioPageNoHtml),
        Constants.DEFAULT_ENCODING);
    // this shouldn't change at all
    assertEquals(notBioPageNoHtml, StringUtil.fromInputStream(actIn));
  }

}
