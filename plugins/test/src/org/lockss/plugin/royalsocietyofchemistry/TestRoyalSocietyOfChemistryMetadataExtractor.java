package org.lockss.plugin.royalsocietyofchemistry;


import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleMetadata;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.plugin.simulated.SimulatedPlugin;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Two of the articles used to get the html source for this plugin is:
 * http://www.rsc.org/publishing/journals/JC/article.asp?doi=a700024c
 * http://www.rsc.org/publishing/journals/FT/article.asp?doi=a706359h
 * Need to proxy content through beta2.lockss.org or another LOCKSS box.
 * The content online is NOT relevant to this plugin.
 *
 */
public class TestRoyalSocietyOfChemistryMetadataExtractor extends LockssTestCase {

	  static Logger log = Logger.getLogger("TestRoyalSocietyOfChemistryMetadataExtractor");

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
	    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
	    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);

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
	  String goodArticleTitle = "Studies with 3-Oxoalkanenitriles: Synthesis of New Pyrazolo[1,5-a]pyrimidines and Pyrazolo[5,1-c]-1,2,4-triazines and Reactivity of 4-Phenyl-3-oxobutanenitrile Derivatives";
	  String goodJournalTitle = "Analytical Communications";	 
	  
	  String goodContent = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
					      "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
					      "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">\n" +
					      "<head>\n" +
					      "<title>"+goodJournalTitle+" articles</title>\n"+
					      "</head>\n"+
					      "<!--CMS XSL V.1.6 13-Jun-2007--><div class=\"onecol\"><a href=\"http://pubs.rsc.org/en/Content/ArticleLanding/1997/JC/a700024c?utm_source=rsc.org&amp;utm_medium=banners&amp;utm_content=Article%2Btoggle&amp;utm_campaign=Try%2Bbeta%20site\"><img align=\"top\" alt=\"Select to view this article on our beta website\" src=\"Journal%20of%20Chemical%20Research%20articles_files/trybeta6.gif\" /></a><p> </p><div class=\"hilite\"><div class=\"header\"><div class=\"header-inner\"></div></div><h3>Free access</h3><ul><li><a target=\"_blank\" href=\"http://www.rsc.org/delivery/_ArticleLinking/DisplayArticleForFree.cfm?doi=a700024c&amp;JournalCode=JC\" title=\"Select to access full article in PDF format&#xA;\t\t\t\t\t\t\t(ID: a700024c)\">PDF</a></li><li><a target=\"_blank\" title=\"Select to access reference linked citations for this article  (ID: a700024c)\" href=\"http://www.rsc.org/delivery/_ArticleLinking/ArticleLinking.cfm?JournalCode=JC&amp;Year=1997&amp;ManuscriptID=a700024c&amp;type=citonly\">Reference linked article citations</a></li><li><a href=\"http://www.rsc.org/publishing/journals/JC/article.asp?DOI=a700024c&amp;type=ForwardLink\" title=\"Select to search for other articles that cite this article (ID:a700024c)\">Search for citing articles</a></li></ul><div class=\"footer\"></div></div><div class=\"footer\"></div><br class=\"cl\" /></div><br class=\"cl\" /><h3>Paper</h3><p><strong><i>J. Chem. Res. (S)</i></strong>, "+goodDate+", "+goodStartPage+" - "+goodEndPage+", <strong>DOI:</strong> 10.1039/"+goodDoiSuffix+"</p><div class=\"hr\"></div><hr /><span style=\"font-size:150%;\"><strong><font color=\"#9C0000\">Studies with 3-Oxoalkanenitriles: Synthesis of New\n" +
					      "\n" +
					      "Pyrazolo[1,5-a]pyrimidines and\n" +
					      "\n" +
					      "Pyrazolo[5,1-c]-1,2,4-triazines and Reactivity of\n" +
					      "\n" +
					      "4-Phenyl-3-oxobutanenitrile Derivatives</font></strong><br class=\"cl\" /><br /></span><p><strong>"+goodAuthors+"</strong></p><div class=\"hr\"></div><hr />4-Phenyl-3-oxobutanenitrile is synthesized\n" +
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
	    String url = "http://www.example.com/publishing/journals/AC/article.asp?doi="+goodDoiSuffix;
	    MockCachedUrl cu = new MockCachedUrl(url, bau);
	    cu.setContent(goodContent);
	    cu.setContentSize(goodContent.length());
	    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
	    FileMetadataExtractor me = new RoyalSocietyOfChemistryHtmlMetadataExtractorFactory.RoyalSocietyOfChemistryHtmlExtractor(); 
	    ArticleMetadata md = me.extract(cu);
	    assertNotNull(md);	    
	    
	    assertEquals(goodDoiPrefix+"/"+goodDoiSuffix, md.getDOI());
	    assertEquals(goodDate, md.getVolume()); // year is the volume where volume is not present.
	    //assertEquals(goodIssue, md.getIssue());
	    assertEquals(goodStartPage, md.getStartPage());
	    //assertEquals(goodISSN, md.getISSN());    	    
	    
	    assertEquals(goodAuthors.replaceAll(" and", ","), md.getAuthor());    
	    assertEquals(goodArticleTitle, md.getArticleTitle());
	    assertEquals(goodJournalTitle, md.getJournalTitle());
	    assertEquals(goodDate, md.getDate());    
	  }

	  String badContent =
	    "<HTML><HEAD><TITLE>" + goodArticleTitle + "</TITLE></HEAD><BODY>\n" + 
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
	    FileMetadataExtractor me = new RoyalSocietyOfChemistryHtmlMetadataExtractorFactory.RoyalSocietyOfChemistryHtmlExtractor();	     
	    ArticleMetadata md = me.extract(cu);
	    assertNotNull(md);
	    assertNull(md.getDOI());
	    assertNull(md.getVolume());
	    assertNull(md.getIssue());
	    assertNull(md.getStartPage());
	    assertNull(md.getISSN());	    
	    assertEquals(1, md.size());
	    assertEquals("bar", md.getProperty("foo"));
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
