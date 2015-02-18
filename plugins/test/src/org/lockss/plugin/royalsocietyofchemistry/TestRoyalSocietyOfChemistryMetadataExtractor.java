/*
 * $Id$
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

package org.lockss.plugin.royalsocietyofchemistry;


import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.plugin.simulated.SimulatedPlugin;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;
import java.util.*;

/**
 * Two of the articles used to get the html source for this plugin is:
 * http://www.rsc.org/publishing/journals/JC/article.asp?doi=a700024c
 * http://www.rsc.org/publishing/journals/FT/article.asp?doi=a706359h
 * Need to proxy content through beta2.lockss.org or another LOCKSS box.
 * The content online is NOT relevant to this plugin.
 *
 */
public class TestRoyalSocietyOfChemistryMetadataExtractor extends LockssTestCase {
  
  static Logger log = Logger.getLogger(TestRoyalSocietyOfChemistryMetadataExtractor.class);
  
  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit bau;		// RSC AU
  
  private static String PLUGIN_NAME = "org.lockss.plugin.royalsocietyofchemistry.ClockssRoyalSocietyOfChemistryPlugin";
  
  private static String BASE_URL = "http://www.rsc.org/";
  private static String RESOLVER_URL = "http://www.rsc.org/";
  private static String JOURNAL_CODE = "analComm";
  private static String YEAR = "1998";
  private static String VOLUME_NAME = "Analytical Communications";
  
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    
    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class, simAuConfig(tempDirPath));
    bau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, rscAuConfig());
  }
  
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("resolver_url", RESOLVER_URL);
    conf.put("journal_code", JOURNAL_CODE);
    conf.put("year", YEAR);
    conf.put("volume_name", VOLUME_NAME);
    conf.put("depth", "2");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "application/html");
    return conf;
  }
  
  Configuration rscAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("resolver_url", RESOLVER_URL);
    conf.put("journal_code", JOURNAL_CODE);
    conf.put("year", YEAR);
    conf.put("volume_name", VOLUME_NAME);
    conf.put("volume", "1");
    conf.put("journal_abbr", "xyzjn");
    return conf;
  }
  
  String goodDoiPrefix = "10.1039";
  String goodDoiSuffix = "c0sm90012e";
  String goodVolume = "35";
  String goodStartPage = "179";
  String goodEndPage = "182";
  String goodISSN = "1234-5679";
  String goodDate = "1991";
  String goodAuthors = "Makoto Mizoguchi and Munetaka Ishiyama";
  String goodArticleTitle = "Studies with 3-Oxoalkanenitriles: Synthesis of New" +
      " Pyrazolo[1,5-a]pyrimidines and Pyrazolo[5,1-c]-1,2,4-triazines and Reactivity" +
      " of 4-Phenyl-3-oxobutanenitrile Derivatives";
  String goodJournalTitle = "Analytical Communications";
  
  String goodContent = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
      "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">\n" +
      "<head>\n" +
      "<title>"+goodJournalTitle+" articles</title>\n"+
      "</head>\n"+
      "<!--CMS XSL V.1.6 13-Jun-2007--><div class=\"onecol\"><a href=\"http://pubs." +
      "rsc.org/en/Content/ArticleLanding/1997/JC/a700024c?utm_source=rsc.org&amp;" +
      "utm_medium=banners&amp;utm_content=Article%2Btoggle&amp;utm_campaign=Try%2Bbeta%20site\">" +
      "<img align=\"top\" alt=\"Select to view this article on our beta website\"" +
      " src=\"Journal%20of%20Chemical%20Research%20articles_files/trybeta6.gif\" /></a>" +
      "<p> </p><div class=\"hilite\"><div class=\"header\"><div class=\"header-inner\">" +
      "</div></div><h3>Free access</h3><ul><li><a target=\"_blank\" href=\"http://www." +
      "rsc.org/delivery/_ArticleLinking/DisplayArticleForFree.cfm?doi=a700024c&amp;JournalCode=JC\"" +
      " title=\"Select to access full article in PDF format&#xA;\t\t\t\t\t\t\t(ID: a700024c)\">PDF</a>" +
      "</li><li><a target=\"_blank\" title=\"Select to access reference linked citations" +
      " for this article  (ID: a700024c)\" href=\"http://www.rsc.org/delivery/_ArticleLinking/" +
      "ArticleLinking.cfm?JournalCode=JC&amp;Year=1997&amp;ManuscriptID=a700024c&amp;type=citonly\">" +
      "Reference linked article citations</a></li><li><a href=\"http://www.rsc.org/publishing/" +
      "journals/JC/article.asp?DOI=a700024c&amp;type=ForwardLink\" title=\"Select to search for" +
      " other articles that cite this article (ID:a700024c)\">Search for citing articles</a></li></ul>" +
      "<div class=\"footer\"></div></div><div class=\"footer\"></div><br class=\"cl\" /></div>" +
      "<br class=\"cl\" /><h3>Paper</h3><p><strong><i>J. Chem. Res. (S)</i></strong>, " +
      goodDate+", "+goodStartPage+" - "+goodEndPage+", <strong>DOI:</strong> 10.1039/" +
      goodDoiSuffix+"</p><div class=\"hr\"></div><hr /><span style=\"font-size:150%;\">" +
      "<strong><font color=\"#9C0000\">Studies with 3-Oxoalkanenitriles: Synthesis of New\n" +
      "\n" +
      "Pyrazolo[1,5-a]pyrimidines and\n" +
      "\n" +
      "Pyrazolo[5,1-c]-1,2,4-triazines and Reactivity of\n" +
      "\n" +
      "4-Phenyl-3-oxobutanenitrile Derivatives</font></strong><br class=\"cl\" />" +
      "<br /></span><p><strong>"+goodAuthors+"</strong></p><div class=\"hr\"></div>" +
      "<hr />4-Phenyl-3-oxobutanenitrile is synthesized\n" +
      "\n" +
      "via the reaction of ethyl phenylacetate with\n" +
      "\n" +
      "acetonitrile in the presence of sodium hydride and identified by\n" +
      "\n" +
      "isolating its 2-phenylhydrazone and dimethylaminomethylidene\n" +
      "\n" +
      "derivatives; both the hydrazone and dimethylaminomethylidene\n" +
      "\n" +
      "derivatives prove versatile starting materials for the synthesis\n" +
      "\n" +
      "of a variety of polyfunctionally substituted heterocycles.\n" +
      "\n" +
      "\n" +
      "       <br class=\"cl\" /> <!-- Stops #footer floating up in NS 6 -->\n" +
      "\n" +
      "       </div><!-- end #content -->\n"+
      "</body>\n" +
      "</html>";
  
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/publishing/journals/AC/article.asp?doi=" +
        goodDoiSuffix;
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new RoyalSocietyOfChemistryHtmlMetadataExtractorFactory.
        RoyalSocietyOfChemistryHtmlExtractor();
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(goodDoiPrefix+"/"+goodDoiSuffix,
        md.get(MetadataField.FIELD_DOI));
    // year is the volume where volume is not present.
    assertEquals(goodDate, md.get(MetadataField.FIELD_VOLUME));
    //assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    //assertEquals(goodISSN, md.get(MetadataField.FIELD_ISSN));
    
    assertEquals(goodAuthors.replaceAll(" and", ","),
        md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle,
        md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle,
        md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
  }
  
  String badContent = "<HTML><HEAD><TITLE>" + goodArticleTitle + "</TITLE></HEAD><BODY>\n" +
      "<meta name=\"foo\"" +  " content=\"bar\">\n" +
      "  <div id=\"issn\">" +
      "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
      goodISSN + " </div>\n";
  
  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/publishing/journals/AC/article.asp";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new RoyalSocietyOfChemistryHtmlMetadataExtractorFactory.
        RoyalSocietyOfChemistryHtmlExtractor();
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertEquals(1, md.rawSize());
    assertEquals("bar", md.getRaw("foo"));
  }
  
  public static class MySimulatedPlugin extends SimulatedPlugin {
    public ArchivalUnit createAu0(Configuration auConfig)
        throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }
    
    public SimulatedContentGenerator getContentGenerator(Configuration cf, String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }
    
  }
  
  public static class MySimulatedContentGenerator extends SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }
    
    public String getHtmlFileContent(String filename, int fileNum, int depth, int branchNum, boolean isAbnormal) {
      String file_content = "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>\n";
      
      file_content += "  <meta name=\"lockss.filenum\" content=\"" + fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";
      
      file_content += getHtmlContent(fileNum, depth, branchNum, isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
          + file_content);
      
      return file_content;
    }
  }
}
