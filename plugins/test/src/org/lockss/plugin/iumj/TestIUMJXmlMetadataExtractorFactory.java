/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.iumj;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/**
 * One of the files used to get the xml source for this plugin is:
 * http://www.iumj.indiana.edu/META/2006/2558.xml
 */
public class TestIUMJXmlMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestIUMJXmlMetadataExtractorFactory");

  private MockArchivalUnit mau;
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME = "org.lockss.plugin.iumj.IUMJPlugin";
  // If changing BASE_URL from www.example.com, also change in
  // IUMJXmlMetaDataExtractorFactory.java, where we test for this BASE_URL
  // to flag a test case
  private static String BASE_URL = "http://www.example.com/";
  //private static String SIM_ROOT = BASE_URL + "cgi/reprint/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    mau = new MockArchivalUnit();
    mau.setConfiguration(iumjAuConfig());
  }

  Configuration iumjAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    // must include all the ConfigParamDescriptors from the plugin
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", "60");
    conf.put("year", "2011");
    return conf;
  }

  String goodTitle = "Convergence of perturbed equations to forced mean curvature flow";
  String goodCreator = "[Luca  Mugnai; Matthias  Roeger]";
  String goodFPublisher = "HelloWorld University Mathematics Journal";
  String goodDate = "2011";
  String goodType = "text";
  String goodFormat = "pdf";
  String goodDescription = "We study perturbations of the equation and prove the convergence forced mean curvature.  Finally, we discuss some applications.";
  String goodIdentifier = "10.1512/iumj.2011.60.3949";
  String goodSource = "10.1512/iumj.2011.60.3949";
  String goodLanguage = "en";
  String goodRelation = "HelloWorld Indiana Univ. Math. J. 60 (2011) 41 - 76";
  String goodCoverage = "state-of-the-art mathematics";
  String goodRights = "http://www.hello.world.edu/Librarians/sublicense.pdf";

  String goodContent = 
    "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" +
    "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/ \" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">"+
    "<dc:title>\n\t" + goodTitle + "</dc:title> " +
    "<dc:creator>" + goodCreator+ "</dc:creator> " +
    "<dc:description>" + goodDescription + "</dc:description> " +
    "<dc:publisher>\n" + goodFPublisher + "</dc:publisher> " +
    "<dc:date>\n" +goodDate+ "</dc:date> " +
    "<dc:type>" + goodType + "</dc:type> " +
    "<dc:format>" + goodFormat + "</dc:format> " +
    "<dc:identifier>" + goodIdentifier + "</dc:identifier> " +
    "<dc:source>" + goodSource + "</dc:source> " + 
    "<dc:language>" + goodLanguage + "</dc:language> " +
    "<dc:relation>" + goodRelation + "</dc:relation> " +
    "<dc:coverage>" + goodCoverage + "</dc:coverage> " +
    "<dc:rights>" + goodRights + "</dc:rights> " +
    "</oai_dc:dc>" ;
  
  String oai = "<title>Citation Index</title>\n" + 
      "<div class=\"citecont\">\n" + 
      "<pre style=\"padding:15px;\"><hr /><h3>BibTeX</h3>\n" + 
      "<span  class=\"ent\">    author</span> = <span  class=\"brace\">\"</span>M. Author<span  class=\"brace\">\"</span>,\n" + 
      "<span  class=\"ent\">     title</span> = <span  class=\"brace\">\"</span>......<span  class=\"brace\">\"</span>,\n" + 
      "<span  class=\"ent\">   journal</span> = <span  class=\"brace\">\"</span>....<span  class=\"brace\">\"</span>,\n" + 
      "<span  class=\"ent\">    volume</span> = 33,\n" + 
      "<span  class=\"ent\">      year</span> = 1984,\n" + 
      "<span  class=\"ent\">     issue</span> = 1,\n" + 
      "<span  class=\"ent\">     pages</span> = <span  class=\"brace\">\"</span>1--29<span  class=\"brace\">\"</span>,\n";
  
  public void testExtractFromGoodContent() throws Exception {
    String url1 = BASE_URL+"IUMJ/FTDLOAD/1984/33/33001/pdf";
    String url2 = BASE_URL+"META/1984/33001.xml";
    String url3 = BASE_URL+"oai/1984/33/33001/33001.html";
    CIProperties lclHeader = new CIProperties();
    lclHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    MockCachedUrl mcu = mau.addUrl(url3, true, true, lclHeader);
    mcu.setContent(oai);
    mcu.setContentSize(oai.length());
    lclHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");
    mcu = mau.addUrl(url1, true, true, lclHeader);
    mcu.setContent("not much");
    mcu.setContentSize(8);
    lclHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
    mcu = mau.addUrl(url2, true, true, lclHeader);
    mcu.setContent(goodContent);
    mcu.setContentSize(goodContent.length());
    
    FileMetadataExtractor me = new IUMJXmlMetadataExtractorFactory.IUMJXmlMetadataExtractor();
    assertNotNull(me);

    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);

    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodCreator, md.get(MetadataField.DC_FIELD_CREATOR));
    assertEquals(goodCreator, md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodDescription, md.get(MetadataField.DC_FIELD_DESCRIPTION));
    assertEquals(goodFPublisher, md.get(MetadataField.DC_FIELD_PUBLISHER));
    // now get publisher name from tdb, as the metadata was inconsistent
    // assertEquals(goodFPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodDate, md.get(MetadataField.DC_FIELD_DATE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));

    assertEquals(goodType, md.get(MetadataField.DC_FIELD_TYPE));
    assertEquals(goodFormat, md.get(MetadataField.DC_FIELD_FORMAT));
    assertEquals(goodIdentifier, md.get(MetadataField.DC_FIELD_IDENTIFIER));
    assertEquals(goodSource, md.get(MetadataField.DC_FIELD_SOURCE));
    assertEquals(goodLanguage, md.get(MetadataField.DC_FIELD_LANGUAGE));
    assertEquals(goodRelation, md.get(MetadataField.DC_FIELD_RELATION));
    assertEquals(goodCoverage, md.get(MetadataField.DC_FIELD_COVERAGE));
    assertEquals(goodRights, md.get(MetadataField.DC_FIELD_RIGHTS));

  }

  String badContent = "<HTML><HEAD><TITLE>"
    + goodTitle
    + "</TITLE></HEAD><BODY>\n"
    + "<article_rec>\n<meta name=\"foo\""
    + " content=\"bar\">\n"
    + "  <div id=\"issn\">\n"
    + "_t3 0XFAKE0X 1231231 3453453 6786786\n"
    + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
    + " </div>\n" + "<fname>fake</fname>\n" + "<article_rec>\n"
    + "</article_rec>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = BASE_URL+"META/badvol1/1002/1002.xml";
    CIProperties xmlHeader = new CIProperties();
    xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
    MockCachedUrl mcu = mau.addUrl(url, true, true, xmlHeader);
    mcu.setContent(badContent);
    mcu.setContentSize(badContent.length());

    FileMetadataExtractor me = new IUMJXmlMetadataExtractorFactory.IUMJXmlMetadataExtractor();
    assertNotNull(me);
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotNull(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
 
  }
}