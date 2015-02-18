/*
 * $Id$
 */

/*

  Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
  all rights reserved.

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.bioone;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.repository.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * Now modified to use the archived content instead of the new Atypon version. 
 * Articles used as HTML source examples (from beta7.lockss.org) were taken from these TOCs:
 * http://www.bioone.org/perlserv/?request=get-toc&issn=0097-4463&volume=74&issue=4
 * http://www.bioone.org/perlserv/?request=get-toc&issn=0002-8444&volume=91&issue=1&ct=1
 * The individual articles have URLs like:
 * http://www.bioone.org/perlserv/?request=get-document&doi=10.1640 %2F 0002-8444 %28 2001 %29 091[0001 %3A TGOAGI]2.0.CO%3B2
 * http://www.bioone.org/perlserv/?request=get-document&doi=10.2992 %2F 0097-4463 %28 2005 %29 74[217 %3A NSAROT]2.0.CO%3B2
 * (spaces inserted to illustrate some of the metadata encoded therein)
 * 
 * @author Neil Mayo
 */
public class TestBioOneAllenPressMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger("TestBioOneMetadataExtractor");

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit bau;		// Bioone AU
  private MockLockssDaemon theDaemon;
  private static int exceptionCount;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;
  private static final String issnTemplate = "%1%2%3%1-%3%1%2%3";
  private static final String volumeTemplate = "%1%3";
  private static final String issueTemplate = "%2";
  private static final String fPageTemplate = "%2%3";
  private static final String lPageTemplate = "%3%1%2";  

  private static String PLUGIN_NAME = "org.lockss.plugin.bioone.BioOnePlugin";

  private static String BASE_URL = "http://www.bioone.org/";

  /**
   * Method to set up the daemon and create the simulated Archival Unit (AU). To be copied as it is to all future plugins
   */
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    PluginManager pluginMgr = theDaemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginMgr.startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,
					     simAuConfig(tempDirPath));
    bau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, biooneAuConfig());
  }


  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  /**
   * Configure the simulated archival unit.
   * @param rootPath
   * @return
   */
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "2");
    conf.put("branch", "2");
    conf.put("numFiles", "4");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
    //     conf.put("default_article_mime_type", "application/pdf");
    return conf;
  }

  /**
   * Configure the bioone AU
   * @return
   */
  Configuration biooneAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", "0002-8444");
    conf.put("volume", "91");
    return conf;
  }

  public void testExtraction() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    PluginTestUtil.copyAu(sau, bau);
    exceptionCount = 0;
    int count = 0;
    for (Iterator<ArticleFiles> it = bau.getArticleIterator(); it.hasNext();) {
      CachedUrl cu = it.next().getFullTextCu();
      assertNotNull(cu);
      log.debug3("count " + count + " url " + cu.getUrl());
      ArticleMetadataExtractor me =
	bau.getPlugin().getArticleMetadataExtractor(MetadataTarget.Any, bau);
      log.debug3("Extractor: " + me.toString());
      ArticleMetadataListExtractor mle = new ArticleMetadataListExtractor(me);
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(cu);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, af);
      assertNotEmpty(mdlist);
      ArticleMetadata md = mdlist.get(0);
      assertNotNull(md);
      checkMetadata(md);
      count++;
    }
    log.debug("Article count is " + count);
    //assertEquals(28, count); // XXX Uncomment when iterators and extractors are back
  }

  String goodDOI = "10.1640/0002-8444(2001)091[0001:TGOAGI]2.0.CO;2";
  String urlEncodedDOI = "10.1640%2F0002-8444%282001%29091%5B0001%3ATGOAGI%5D2.0.CO%3B2";
  String goodVolume = "91";
  String goodIssue = "1";
  String goodStartPage = "1";
  String goodEndPage = "8";
  String goodOnlineISSN = "0002-8444";
  // There is no Print ISSN available
  String goodDate = "January 2001";
  String[] goodAuthors = new String[] {"David B. Lellinger", "Jefferson Prado"};
  String goodATitle1 = "The Group of ";
  String goodATitle2 = "Adiantum gracile";
  String goodATitle3 = " in Brazil and Environs";
  String goodArticleTitle = goodATitle1 + goodATitle2 + goodATitle3; //"The Group of Adiantum gracile in Brazil and Environs";
  String goodJournalTitle = "American Fern Journal";
    
  // A String holding a chunk of relevant HTML of a Bioone article. Only relevant chunks are added to minimise the length of the string and
  // maximise readability. 
  String goodContent = 
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html lang=\"en\">\n" +
    "       <head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"/>\n" +
    "         <title>BIOONE Online Journals - "+goodArticleTitle+"</title>\n" +
    "       </head>\n" +
    "       <body>\n" +
    "         \n" +
    "               <div id=\"pageTitle\">\n" +
    "                       <h1>Full Text View</h1>\n" +
    "                       <p><a href=\"http://www.bioone.org/perlserv/?request=get-toc&#38;issn="+goodOnlineISSN+"&#38;volume="+goodVolume+"&#38;issue="+goodIssue+"\">Volume "+goodVolume+", Issue "+goodIssue+" ("+goodDate+")</a></p>\n" +
    "               </div>\n" +
    "\n" +
    "               <h2 style=\"margin-bottom: 0px\">" + goodJournalTitle + "</h2>\n" +
    "\n" +
    "               <p style=\"margin-top: 0px\">Article: pp. "+goodStartPage+"&#8211;"+goodEndPage+" | <a href=\"http://www.bioone.org/perlserv/?request=get-abstract&#38;doi="+urlEncodedDOI+"\">Abstract</a> &#124; <a href=\"http://www.bioone.org/perlserv/?request=res-loc&#38;uri=urn%3Aap%3Apdf%3Adoi%3A"+urlEncodedDOI+"\">PDF (498K)</a></p>\n" +
    "               \n" +
    "         <h1>"+goodATitle1+"<a href=\"http://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=all&#38;search_value=Adiantum+gracile&#38;search_kingdom=every&#38;search_span=exactly_for&#38;categories=All&#38;source=html&#38;search_credRating=All\" TARGET=\"itis_window\"><em>"+goodATitle2+"</em></a>"+goodATitle3+"</h1>\n" +
    "         <p class=\"authors\">"+goodAuthors[0]+"<sup><a href=\"#AFF1\" class=\"aff-auth\">A</a></sup> and "+goodAuthors[1]+"<sup><a href=\"#AFF2\" class=\"aff-auth\">B</a></sup> </p>\n" +
    "         <p class=\"affiliation\"><span class=\"aff aff-label\" id=\"AFF1\">A.</span> Department of Botany, National Museum of Natural History, Smithsonian Institution, Washington, DC 20560-0166, <span class=\"aff aff-label\" id=\"AFF2\">B.</span> Se&#231;&#227;o de Briologia e Pteridologia, Instituto de Bot&#226;nica, Caixa Postal 4005, 01061-970 S&#227;o Paulo, SP, Brasil       </p>\n" +
    "    \n" +
    "    <div class=\"abstract\"><p class=\"abstract\">An abstract</p></div><p class=\"info\">DOI: "+goodDOI+"</p>\n" +
    "    \n" +
    "  </body>\n" +
    "</html>\n";
    
 
  /**
   * Method that asserts all metadata extracted from the html. This is the method where we test whether the BePressHtmlMetadataExtractor can extract
   * the data that should be extracted
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    ArticleMetadata md = extractFromTestContent(goodContent);

    assertTrue(MetadataUtil.isDoi(md.get(MetadataField.FIELD_DOI)));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));				
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    //assertEquals(goodEndPage, md.getEndPage());
    assertTrue(MetadataUtil.isIssn(md.get(MetadataField.FIELD_ISSN)));
    assertEquals(goodOnlineISSN, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodAuthors[0], md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
  }

  String[] badAuthors = new String[] {"David B. Lellinger &amp; Jefferson Prado" };
  
  // A string representing bad html markup for a bioone article. Metadata from this code should NOT be extracted
  String badContent = 
    "<html>\n" +
    "       <head><title>BIOONE Online Journals - "+goodArticleTitle+"</title></head>\n" +
    "       <body>\n" +
    "               <div id=\"pageTitle\">\n" +
    "                       <h1>Full Text View</h1>\n" +
    // Next line has URL params in an unexpected order
    "                       <p><a href=\"http://www.bioone.org/perlserv/?request=get-toc&#38;volume="+goodVolume+"&#38;issue="+goodIssue+"&#38;issn="+goodOnlineISSN+"\">Volume "+goodVolume+", Issue "+goodIssue+" ("+goodDate+")</a></p>\n" +
    "               </div>\n" +
    // Wrong margin in h2 style
    "               <h2 style=\"margin-bottom: 10px\">" + goodJournalTitle + "</h2>\n" +
    // Unexpected pp format
    "               <p style=\"margin-top: 0px\">Article: pages "+goodStartPage+" - "+goodEndPage+" | <a href=\"http://www.bioone.org/perlserv/?request=get-abstract&#38;doi="+urlEncodedDOI+"\">Abstract</a> &#124; <a href=\"http://www.bioone.org/perlserv/?request=res-loc&#38;uri=urn%3Aap%3Apdf%3Adoi%3A"+urlEncodedDOI+"\">PDF (498K)</a></p>\n" +
    "               \n" +
    // Wrong title header type
    "         <h2>"+goodArticleTitle+"</h2>\n" +
    // Unexpected author layout
    "         <p class=\"authors\">"+goodAuthors[0]+" &amp; "+goodAuthors[1]+" </p>\n" +
    // Bad DOI with spaces
    "    <div class=\"abstract\"><p class=\"abstract\">An abstract</p></div><p class=\"info\">DOI: "+goodDOI+" doiend</p>\n" +
    "    \n" +
    "  </body>\n" +
    "</html>\n";
    

  /**
   * Method that asserts that data is NOT extracted from the badContent string
   * @throws Exception
   */
  public void testExtractFromBadContent() throws Exception {
    ArticleMetadata md = extractFromTestContent(badContent);

    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    //assertNull(md.getEndPage());
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertEquals(badAuthors[0], md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
    assertNotNull(md.get(MetadataField.FIELD_ACCESS_URL));
    assertEquals(2, md.size()); // only author and access field
  }

  /**
   * Method that makes use of the BioOneHtmlMetadataExtractor to extract the metadata of the html content passed as a parameter.
   * @param content The html code we want to extract the metadata from
   * @return An articleMetadata object where we can retreive all relevant metadata.
   * @throws Exception
   */
  private ArticleMetadata extractFromTestContent(String content) throws Exception {
    String url = "http://www.example.org/doi/abs/10.1640/0002-8444-00.2.61";
    MockCachedUrl cu = new MockCachedUrl(url, sau);
    cu.setContent(content);
    cu.setContentSize(content.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    cu.setFileMetadataExtractor(new BioOneAllenPressHtmlMetadataExtractorFactory.BioOneAllenPressHtmlMetadataExtractor());
    ArticleMetadataExtractorFactory mef = new BioOneAllenPressArticleIteratorFactory();
    ArticleMetadataExtractor me =
      mef.createArticleMetadataExtractor(MetadataTarget.Any);
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    ArticleMetadataListExtractor mle = new ArticleMetadataListExtractor(me);
    ArticleFiles af = new ArticleFiles();
    af.setFullTextCu(cu);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, af);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    return md;
  }

  /**
   * Method to replace strings containing for example "%1%3" with ints according to the fileNum, depth and branchNum
   * @param content The string to replace
   * @param fileNum
   * @param depth
   * @param branchNum
   * @return
   */
  private static String getFieldContent(String content, int fileNum, int depth, int branchNum) {
    content = StringUtil.replaceString(content, "%1", "" + fileNum);
    content = StringUtil.replaceString(content, "%2", "" + depth);
    content = StringUtil.replaceString(content, "%3", "" + branchNum);
    return content;
  }

  public void checkMetadata(ArticleMetadata md) {
    String temp = null;
    temp = (String) md.getRaw("lockss.filenum");
    int fileNum = -1;
    try {
      fileNum = Integer.parseInt(temp);
    } catch (NumberFormatException ex) {
      log.error(temp + " caused " + ex);
      fail();
    }
    temp = (String) md.getRaw("lockss.depth");
    int depth = -1;
    try {
      depth = Integer.parseInt(temp);
    } catch (NumberFormatException ex) {
      log.error(temp + " caused " + ex);
      fail();
    }
    temp = (String) md.getRaw("lockss.branchnum");
    int branchNum = -1;
    try {
      branchNum = Integer.parseInt(temp);
    } catch (NumberFormatException ex) {
      log.error(temp + " caused " + ex);
      fail();
    }

    // Do the accessors return the expected values?
    assertEquals(getFieldContent(volumeTemplate, fileNum, depth, branchNum),
		 md.get(MetadataField.FIELD_VOLUME));
    assertEquals(getFieldContent(issnTemplate, fileNum, depth, branchNum),
		 md.get(MetadataField.FIELD_ISSN));
    assertEquals(getFieldContent(issueTemplate, fileNum, depth, branchNum),
		 md.get(MetadataField.FIELD_ISSUE));
    assertEquals(getFieldContent(fPageTemplate, fileNum, depth, branchNum),
		 md.get(MetadataField.FIELD_START_PAGE));
    // can't assert doi here as it's extracted from URL
  }

  public static class MySimulatedPlugin extends SimulatedPlugin {

    public SimulatedContentGenerator getContentGenerator(Configuration cf, String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

  }

  /**
   * Inner class that generates simulated content. It is not an essential part of this test class. In this case the content representing the AU does
   * not even resembles actual BioOne html article content.
   *    
   */
  public static class MySimulatedContentGenerator extends SimulatedContentGenerator {

    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum, int depth, int branchNum, boolean isAbnormal) {
      String file_content = "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>\n";
      file_content += "  <meta name=\"lockss.filenum\" content=\"" + fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";
      file_content += "<p><strong>Print ISSN: </strong>" + getFieldContent(issnTemplate, fileNum, depth, branchNum) + " </p>\n";
      if (fileNum % 2 == 0) {
	file_content += "<p><strong>Online ISSN: </strong>" + getFieldContent(issnTemplate, fileNum, depth, branchNum) + "</p>\n";
      }
      file_content += "<p><strong>Current: </strong>Apr 2009 : Volume " + getFieldContent(volumeTemplate, fileNum, depth, branchNum) + " Issue " + getFieldContent(issueTemplate, fileNum, depth, branchNum) + "</p>\n";
      file_content += "<span class=\"title\">\n" +
	"        \n" +
	"                pg(s) " + getFieldContent(fPageTemplate, fileNum, depth, branchNum) + "-" + getFieldContent(lPageTemplate, fileNum, depth, branchNum) + "\n" +
	"            \n" +
	"            </span>\n" +
	"    ";

      file_content += getHtmlContent(fileNum, depth, branchNum, isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug3("MySimulatedContentGenerator.getHtmlFileContent: " + file_content);

      return file_content;
    }
  }

}
