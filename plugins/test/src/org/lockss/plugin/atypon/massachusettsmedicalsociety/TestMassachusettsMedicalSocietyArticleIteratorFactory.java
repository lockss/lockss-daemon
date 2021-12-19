/* 
 * $Id: $ 
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

package org.lockss.plugin.atypon.massachusettsmedicalsociety;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.Iterator;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

/*
 * HTML Full Text: http://www.nejm.org/doi/full/10.1056/NEJMoa042957
 * PDF Full Text: http://www.nejm.org/doi/pdf/10.1056/NEJMoa042957
 * Citation (containing metadata): www.nejm.org/action/downloadCitation?format=(ris|endnote|bibTex|medlars|procite|referenceManager)&doi=10.1056%2FNEJMoa042957&include=cit&direct=checked
 * Supplemental Materials page: http://www.nejm.org/action/showSupplements?doi=10.1056%2FNEJMc1304053
 */
public class TestMassachusettsMedicalSocietyArticleIteratorFactory extends ArticleIteratorTestCase {

  private final String PATTERN_FAIL_MSG = "Article file URL pattern changed or incorrect";
  private final String PLUGIN_NAME = "org.lockss.plugin.atypon.massachusettsmedicalsociety.MassachusettsMedicalSocietyPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String BASE_URL = "http://www.example.com/";
  //private final String BASE_URL2 = "http:/cdn.example.com/";
  private final String VOLUME_NAME = "352";
  private final String JOURNAL_ID = "nejm";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      VOLUME_NAME_KEY, VOLUME_NAME,
      JOURNAL_ID_KEY, JOURNAL_ID);
  private CIProperties pdfHeader = new CIProperties();    
  private CIProperties textHeader = new CIProperties();
  private static final String ContentString = "foo blah";
  InputStream random_content_stream;

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();

    au = createAu();
    // set up headers for creating mock CU's of the appropriate type
    pdfHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");
    textHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    // the content in the urls doesn't really matter for the test
    random_content_stream = new ByteArrayInputStream(ContentString.getBytes(Constants.ENCODING_UTF_8));
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Configuration method. 
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", JOURNAL_ID);
    conf.put("volume_name", VOLUME_NAME);

    return conf;
  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals("Article file root URL pattern changed or incorrect" ,ListUtil.list( BASE_URL + "doi/"),
        getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);

    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://www.wrong.com/doi/full/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "dooi/full/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "/full/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi1/full/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "//10.5339/nejm12315");
    //assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/pdfplus/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/ful/10.5339/nejm12315");
    //assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/124/10.5339/nejm12315"); // this is now allowed
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://www.wrong.com/doi/pdf/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "dooi/pdf/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "/pdf/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi1/pdf/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/abs/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "action/showSupplements?doi=10.1056%2FNEJMc1304053");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/full/10.5339/nejm12315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/full/10.533329/nejm123324315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/full/1023.5339/nejmb123b315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/full/10232.533339/nejm12315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/pdf/10.5339/nejm12315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/pdf/10.533329/nejm123324315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/pdf/1023.5339/nejmb123b315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/pdf/10232.533339/nejm12315");
    //assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL2 + "6.0/img/blueDownArrow.gif");
  }

  public void testCreateArticleFiles() throws Exception {
    // key the expected content to the fullTextUrl for the ArticleFiles
    HashMap<String, ArticleFiles> fullUrlToAF = new HashMap<String, ArticleFiles>();
    // add the following URLs to the AU
    String[] au_urls = {
        // article "NEJM1231" - abs, pdf, full, supplements and citation
        BASE_URL + "doi/abs/10.1056/NEJM1231",
        BASE_URL + "doi/pdf/10.5339/NEJM1231",
        BASE_URL + "doi/full/10.5339/NEJM1231",
        BASE_URL + "action/showSupplements?doi=10.5339%2FNEJM1231",
        BASE_URL + "action/downloadCitation?doi=10.5339%2FNEJM1231&format=ris&include=cit",
        // article nejm12345  - full text only
        BASE_URL + "doi/full/10.5339/nejm12345",
        // article nejm1231113 - pdf, full, ris
        BASE_URL + "doi/full/10.5339/nejm1231113",
        BASE_URL + "doi/pdf/10.5339/nejm1231113",
        //BASE_URL + "doi/image/10.5339/nejm1231113",
        BASE_URL + "action/downloadCitation?doi=10.5339%2FNEJM1231113&format=ris&include=cit",
        //  article nejm12315002, pdf, supp, ris
        BASE_URL + "doi/pdf/10.5339/nejm12315002",
        BASE_URL + "action/showSupplements?doi=10.5339%2Fnejm12315002",
        BASE_URL + "action/downloadCitation?doi=10.5339%2FNEJM12315002&format=ris&include=cit",
        // article nejm123456 pdf, full, supp, ris
        BASE_URL + "doi/pdf/10.5339/NEJM123456",
        BASE_URL + "doi/full/10.5339/NEJM123456",
        BASE_URL + "action/showSupplements?doi=10.5339%2FNEJM123456",
        BASE_URL + "action/downloadCitation?doi=10.5339%2FNEJM123456&format=ris&include=cit",
        // randoms
        BASE_URL + "doi/",
        BASE_URL + "bq/352/12"
        // cdn
    };
    // the content is never checked so just use a random input stream with the
    //correct header
    for(String url : au_urls) {
      if(url.contains("pdf")){
        storeContent(random_content_stream,pdfHeader, url);
      }
      else {
        storeContent(random_content_stream,textHeader, url);
      }
    }
    // article NEJM1231 - mostly full complement
    ArticleFiles af1 = new ArticleFiles();
    af1.setRoleString(ArticleFiles.ROLE_ABSTRACT,BASE_URL + "doi/abs/10.5339/NEJM1231");
    af1.setRoleString(ArticleFiles.ROLE_FULL_TEXT_HTML,BASE_URL + "doi/full/10.5339/NEJM1231");
    af1.setRoleString(ArticleFiles.ROLE_FULL_TEXT_PDF,BASE_URL + "doi/pdf/10.5339/NEJM1231");
    af1.setRoleString(ArticleFiles.ROLE_CITATION_RIS,BASE_URL + "action/downloadCitation?doi=10.5339%2FNEJM1231&format=ris&include=cit");
    af1.setRoleString(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS,BASE_URL + "action/showSupplements?doi=10.5339%2FNEJM1231");
    //log.info(af1.toString()); 
    fullUrlToAF.put(BASE_URL + "doi/full/10.5339/NEJM1231", af1);
    
    // article nejm12345
    ArticleFiles af2 = new ArticleFiles();
    af2.setRoleString(ArticleFiles.ROLE_FULL_TEXT_HTML, BASE_URL + "doi/full/10.5339/nejm12345");
    //log.info(af2.toString()); 
    fullUrlToAF.put(BASE_URL + "doi/full/10.5339/nejm12345", af2);
   
    // article nejm1231113
    ArticleFiles af3 = new ArticleFiles();
    af3.setRoleString(ArticleFiles.ROLE_FULL_TEXT_PDF, BASE_URL + "doi/pdf/10.5339/nejm1231113");
    af3.setRoleString(ArticleFiles.ROLE_FULL_TEXT_HTML, BASE_URL + "doi/full/10.5339/nejm1231113");
    af3.setRoleString(ArticleFiles.ROLE_CITATION_RIS, BASE_URL + "action/downloadCitation?doi=10.5339%2FNEJM1231113&format=ris&include=cit");
    //log.info(af3.toString());  
    fullUrlToAF.put(BASE_URL + "doi/full/10.5339/nejm1231113", af3);

    // article nejm12315002
    ArticleFiles af4 = new ArticleFiles();
    af4.setRoleString(ArticleFiles.ROLE_FULL_TEXT_PDF,BASE_URL + "doi/pdf/10.5339/nejm12315002");
    af4.setRoleString(ArticleFiles.ROLE_CITATION_RIS, BASE_URL + "action/downloadCitation?doi=10.5339%2FNEJM12315002&format=ris&include=cit");
    af4.setRoleString(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS,BASE_URL + "action/showSupplements?doi=10.5339%2Fnejm12315002");
    //log.info(af4.toString()); 
    fullUrlToAF.put(BASE_URL + "doi/pdf/10.5339/nejm12315002", af4);

    // article nejm123456
    ArticleFiles af5 = new ArticleFiles();
    af5.setRoleString(ArticleFiles.ROLE_FULL_TEXT_PDF,BASE_URL + "doi/pdf/10.5339/NEJM123456");
    af5.setRoleString(ArticleFiles.ROLE_FULL_TEXT_HTML,BASE_URL + "doi/full/10.5339/NEJM123456");
    af5.setRoleString(ArticleFiles.ROLE_CITATION_RIS,BASE_URL + "action/downloadCitation?doi=10.5339%2FNEJM123456&format=ris&include=cit");
    af5.setRoleString(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS,BASE_URL + "action/showSupplements?doi=10.5339%2FNEJM123456");
    //log.info(af5.toString());    
    fullUrlToAF.put(BASE_URL + "doi/full/10.5339/NEJM123456", af5);

    for ( Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any()); it.hasNext(); ) {
      ArticleFiles af = it.next();
      log.info("next AF: " + af.getFullTextUrl());
      ArticleFiles exp= fullUrlToAF.get(af.getFullTextUrl());
      assertNotNull(exp);
      compareArticleFiles(exp, af);
    }
    
  }
  
  public void testCitationPattern() {

  }

  private void compareArticleFiles(ArticleFiles exp, ArticleFiles act) {
    /*
    log.info("CompareArticleFiles: " );
    log.info("checking html:");
    log.info("expected: "+exp.getRoleAsString(ArticleFiles.ROLE_FULL_TEXT_HTML));
    log.info("actual: "+act.getRoleAsString(ArticleFiles.ROLE_FULL_TEXT_HTML));

    
    log.info("checking pdf:");
    log.info("expected: "+exp.getRoleAsString(ArticleFiles.ROLE_FULL_TEXT_PDF));
    log.info("actual: "+act.getRoleAsString(ArticleFiles.ROLE_FULL_TEXT_PDF));
    */

    assertEquals("ROLE_FULL_TEXT_PDF: ",
        exp.getRoleAsString(ArticleFiles.ROLE_FULL_TEXT_PDF),
        act.getRoleAsString(ArticleFiles.ROLE_FULL_TEXT_PDF));
    assertEquals("ROLE_FULL_TEXT_HTML",
        exp.getRoleAsString(ArticleFiles.ROLE_FULL_TEXT_HTML),
        act.getRoleAsString(ArticleFiles.ROLE_FULL_TEXT_HTML));
/*
    log.info("checking ris:");

    log.info("ris expected: "+exp.getRoleAsString(ArticleFiles.ROLE_CITATION_RIS));
    log.info("ris actual: "+act.getRoleAsString(ArticleFiles.ROLE_CITATION_RIS));
   
    log.info("checking supplementary:");
    log.info("expected: "+exp.getRoleAsString(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS));
    log.info("actual: "+act.getRoleAsString(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS));
    */
    assertEquals("ROLE_CITATION: ",
        exp.getRoleAsString(ArticleFiles.ROLE_CITATION),
        act.getRoleAsString(ArticleFiles.ROLE_CITATION));
    assertEquals("ROLE_SUPPLEMENTARY_MATERIALS: ",
        exp.getRoleAsString(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS),
        act.getRoleAsString(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS));
  }


}
