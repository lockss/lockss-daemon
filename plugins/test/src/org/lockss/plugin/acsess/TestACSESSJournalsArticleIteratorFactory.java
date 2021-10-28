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

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.ConfigurationUtil;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.daemon.SingleNodeCachedUrlSetSpec;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;

/*
 Article files:
- abs: https://dl.sciencesocieties.org/publications/aj/abstracts/106/1/57
       https://dl.sciencesocieties.org/publications/jeq/abstracts/27/5/JEQ0270051094
- preview html landing: https://dl.sciencesocieties.org/publications/cns/abstracts/47/1/20/preview
- html full text: https://dl.sciencesocieties.org/publications/aj/articles/106/1/57
                  https://dl.sciencesocieties.org/publications/aj/articles/106/3/1070a
- pdf: https://dl.sciencesocieties.org/publications/aj/pdfs/106/1/57
- tables only: https://dl.sciencesocieties.org/publications/aj/articles/106/1/57?show-t-f=tables&wrapper=no
- figures only: https://dl.sciencesocieties.org/publications/aj/articles/106/1/57?show-t-f=figures&wrapper=no
- supplement: https://dl.sciencesocieties.org/publications/jeq/supplements/43/177-supplement.pdf
              https://dl.sciencesocieties.org/publications/aj/supplements/106/645-supplement1.xlsx
              https://dl.sciencesocieties.org/publications/aj/supplements/106/645-supplement2.pdf
- EndNote: https://dl.sciencesocieties.org/publications/citation-manager/down/en/aj/106/5/1677
- ProCite Ris: https://dl.sciencesocieties.org/publications/citation-manager/down/pc/aj/106/5/1677
- MARC: https://dl.sciencesocieties.org/publications/citation-manager/down/marc/aj/106/5/1677
- RefWorks: https://dl.sciencesocieties.org/publications/citation-manager/down/refworks/aj/106/5/1677
 */
public class TestACSESSJournalsArticleIteratorFactory 
  extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
        
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.acsess.ClockssACSESSJournalsPlugin";
   
  private static final String BASE_URL = "https://www.example.com/";
  private static final String JID = "xxxjid";
  private static final String VOL = "106";
  
  private static final int DEFAULT_FILESIZE = 3000;
  
  private static final int EXP_DELETED_FILE_COUNT = 8;
  private static final int EXP_FULL_TEXT_COUNT = 12; // full text
  private static final int EXP_HTML_COUNT = 8; // after deleteBlock
  private static final int EXP_PDF_COUNT = 8; // after deleteBlock
  private static final int EXP_ABS_COUNT = 12; 
  private static final int EXP_PREVIEW_HTML_LANDING_COUNT = 12; 
  private static final int EXP_RIS_COUNT = 12;

  public void setUp() throws Exception {
    super.setUp();    
    // au is protected archival unit from super class ArticleIteratorTestCase
    au = createAu(); 
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));

    ConfigurationUtil.addFromArgs(CachedUrl.PARAM_ALLOW_DELETE, "true");
  }
  
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    super.tearDown();
  }
  
  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_ID,  acsessAuConfig());
  }
  
  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "4");
    conf.put("numFiles", "3");
    conf.put("fileTypes",
        "" + (  SimulatedContentGenerator.FILE_TYPE_PDF        
            | SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }

  private Configuration acsessAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", JID);
    conf.put("volume_name", VOL);
    return conf;
  }
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL + "publications/"), getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    // PATTERN_TEMPLATE = 
    //  "\"^%spublications/([^/])?/(articles|citation-manager)(/down/[^/]/[^/])?/\\d+/\\d+/\\d+$\", base_url";  
 
    // ex: https://dl.sciencesocieties.org/publications/aj/articles/106/1/57
    assertMatchesRE(pat, 
        BASE_URL + "publications/" + JID + "/abstracts/" + VOL + "/1/57");
    assertMatchesRE(pat, 
        BASE_URL + "publications/" + JID + "/articles/" + VOL + "/1/57");
    assertMatchesRE(pat, 
        BASE_URL + "publications/" + JID + "/pdfs/" + VOL + "/1/57");
    assertNotMatchesRE(pat, 
        BASE_URL + "publications/" + JID + "/pdfsbad/" + VOL + "/1/57");    
  }
  
  /* simulated cached urls:
     total number files = 48; // 1 depth, 4 branches, 3 files
     /1/001 means branch #1 and file  #1
     there are 4 primary aspects

	https://www.example.com/publications/xxxjid/abstracts/106/1/001
	https://www.example.com/publications/xxxjid/abstracts/106/1/001/preview
	https://www.example.com/publications/xxxjid/abstracts/106/1/002
	https://www.example.com/publications/xxxjid/abstracts/106/1/002/preview
	https://www.example.com/publications/xxxjid/abstracts/106/1/003
	https://www.example.com/publications/xxxjid/abstracts/106/1/003/preview
	https://www.example.com/publications/xxxjid/abstracts/106/2/001
	https://www.example.com/publications/xxxjid/abstracts/106/2/001/preview
	https://www.example.com/publications/xxxjid/abstracts/106/2/002
	https://www.example.com/publications/xxxjid/abstracts/106/2/002/preview
	https://www.example.com/publications/xxxjid/abstracts/106/2/003
	https://www.example.com/publications/xxxjid/abstracts/106/2/003/preview
	https://www.example.com/publications/xxxjid/abstracts/106/3/001
	https://www.example.com/publications/xxxjid/abstracts/106/3/001/preview
	https://www.example.com/publications/xxxjid/abstracts/106/3/002
	https://www.example.com/publications/xxxjid/abstracts/106/3/002/preview
	https://www.example.com/publications/xxxjid/abstracts/106/3/003
	https://www.example.com/publications/xxxjid/abstracts/106/3/003/preview
	https://www.example.com/publications/xxxjid/abstracts/106/4/001
	https://www.example.com/publications/xxxjid/abstracts/106/4/001/preview
	https://www.example.com/publications/xxxjid/abstracts/106/4/002
	https://www.example.com/publications/xxxjid/abstracts/106/4/002/preview
	https://www.example.com/publications/xxxjid/abstracts/106/4/003
	https://www.example.com/publications/xxxjid/abstracts/106/4/003/preview
	https://www.example.com/publications/xxxjid/articles/106/1/001
	https://www.example.com/publications/xxxjid/articles/106/1/002
	https://www.example.com/publications/xxxjid/articles/106/1/003
	https://www.example.com/publications/xxxjid/articles/106/2/001
	https://www.example.com/publications/xxxjid/articles/106/2/002
	https://www.example.com/publications/xxxjid/articles/106/2/003
	https://www.example.com/publications/xxxjid/articles/106/3/001
	https://www.example.com/publications/xxxjid/articles/106/3/002
	https://www.example.com/publications/xxxjid/articles/106/3/003
	https://www.example.com/publications/xxxjid/articles/106/4/001
	https://www.example.com/publications/xxxjid/articles/106/4/002
	https://www.example.com/publications/xxxjid/articles/106/4/003
	https://www.example.com/publications/xxxjid/pdfs/106/1/001
	https://www.example.com/publications/xxxjid/pdfs/106/1/002
	https://www.example.com/publications/xxxjid/pdfs/106/1/003
	https://www.example.com/publications/xxxjid/pdfs/106/2/001
	https://www.example.com/publications/xxxjid/pdfs/106/2/002
	https://www.example.com/publications/xxxjid/pdfs/106/2/003
	https://www.example.com/publications/xxxjid/pdfs/106/3/001
	https://www.example.com/publications/xxxjid/pdfs/106/3/002
	https://www.example.com/publications/xxxjid/pdfs/106/3/003
	https://www.example.com/publications/xxxjid/pdfs/106/4/001
	https://www.example.com/publications/xxxjid/pdfs/106/4/002
	https://www.example.com/publications/xxxjid/pdfs/106/4/003
	https://www.example.com/publications/citation-manager/down/pc/xxxjid/106/1/001
	https://www.example.com/publications/citation-manager/down/pc/xxxjid/106/1/002
	https://www.example.com/publications/citation-manager/down/pc/xxxjid/106/1/003
	https://www.example.com/publications/citation-manager/down/pc/xxxjid/106/2/001
	https://www.example.com/publications/citation-manager/down/pc/xxxjid/106/2/002
	https://www.example.com/publications/citation-manager/down/pc/xxxjid/106/2/003
	https://www.example.com/publications/citation-manager/down/pc/xxxjid/106/3/001
	https://www.example.com/publications/citation-manager/down/pc/xxxjid/106/3/002
	https://www.example.com/publications/citation-manager/down/pc/xxxjid/106/3/003
	https://www.example.com/publications/citation-manager/down/pc/xxxjid/106/4/001
	https://www.example.com/publications/citation-manager/down/pc/xxxjid/106/4/002
	https://www.example.com/publications/citation-manager/down/pc/xxxjid/106/4/003
	https://www.example.com/publications/xxxjid/supplements/106/001-supplement1.xlsx
	https://www.example.com/publications/xxxjid/supplements/106/002-supplement1.xlsx
	https://www.example.com/publications/xxxjid/supplements/106/003-supplement1.xlsx
   */
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);

    // html full text: https://dl.sciencesocieties.org/publications/aj/articles/106/1/57
    String htmlPat = "branch(\\d+)/(\\d+)file\\.html";
    String htmlRep = "publications/xxxjid/articles/106/$1/$2";
    String absRep = "publications/xxxjid/abstracts/106/$1/$2";
    String previewHtmlLandingRep = "publications/xxxjid/abstracts/106/$1/$2/preview";
    String pdfRep = "publications/xxxjid/pdfs/106/$1/$2";
    String risRep = "publications/citation-manager/down/pc/xxxjid/106/$1/$2";
    String xlsxSupplement1Rep = "publications/xxxjid/supplements/106/$2-supplement1.xlsx";
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", htmlPat, htmlRep);
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", htmlPat, absRep); 
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", htmlPat, previewHtmlLandingRep); 
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", htmlPat, pdfRep);  
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", htmlPat, risRep); 
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", htmlPat, xlsxSupplement1Rep); 
   
    // Remove some URLs:
    // https://www.example.com/publications/xxxjid/articles/106/1/001
    // https://www.example.com/publications/xxxjid/articles/106/2/001
    // https://www.example.com/publications/xxxjid/articles/106/3/001
    // https://www.example.com/publications/xxxjid/articles/106/4/001
    // https://www.example.com/publications/xxxjid/pdfs/106/1/002
    // https://www.example.com/publications/xxxjid/pdfs/106/2/002
    // https://www.example.com/publications/xxxjid/pdfs/106/3/002
    // https://www.example.com/publications/xxxjid/pdfs/106/4/002
    int deletedFileCount = 0; 
    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
        String url = cu.getUrl();
        log.info("au cached url: " + url);
        if ((url.contains("/articles/") && url.endsWith("1")) 
            || (url.contains("/pdfs/") && url.endsWith("2"))) {
          deleteBlock(cu);
          ++deletedFileCount;
        }
    }
    assertEquals(EXP_DELETED_FILE_COUNT, deletedFileCount);
    
    // au should now match the aspects that the SubTreeArticleIteratorBuilder
    // builds in ACSESSJournalsArticleIteratorFactory
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    
    int count = 0;
    int countHtmlOnly = 0;
    int countPdfOnly = 0;
    int countAbsOnly = 0;
    int countPreviewHtmlLandingOnly = 0;
    int countRisOnly = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.info(af.toString());
      CachedUrl cu = af.getFullTextCu();
      String url = cu.getUrl();
      assertNotNull(cu);
      String contentType = cu.getContentType();
      log.info("count " + count + " url " + url + " " + contentType);
      count++;
      url = af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML);
      if (!StringUtil.isNullString(url) && url.contains("/articles/")) {
        ++countHtmlOnly;
      }
      url = af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF);
      if (!StringUtil.isNullString(url) && url.contains("/pdfs/")) {
        ++countPdfOnly;
      }
      url = af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT);
      if (!StringUtil.isNullString(url) && url.contains("/abstracts/")) {
        ++countAbsOnly;
      }
      url = af.getRoleUrl(ACSESSJournalsArticleIteratorFactory.ROLE_PREVIEW_HTML_LANDING);
      if (!StringUtil.isNullString(url) && url.contains("/preview")) {
        ++countPreviewHtmlLandingOnly;
      }  
      url = af.getRoleUrl(ArticleFiles.ROLE_CITATION_RIS);
      if (!StringUtil.isNullString(url) && url.contains("/down/pc/")) {
        ++countRisOnly;
      }    
    }
    
    log.info("Full text Article count is " + count);
    assertEquals(EXP_FULL_TEXT_COUNT, count);
    assertEquals(EXP_HTML_COUNT, countHtmlOnly);
    assertEquals(EXP_PDF_COUNT, countPdfOnly);
    assertEquals(EXP_ABS_COUNT, countAbsOnly);
    assertEquals(EXP_PREVIEW_HTML_LANDING_COUNT, countPreviewHtmlLandingOnly);
    assertEquals(EXP_RIS_COUNT, countRisOnly);
   }
 
  private void deleteBlock(CachedUrl cu) throws IOException {
    log.info("deleting " + cu.getUrl());
    cu.delete();
  }

}
