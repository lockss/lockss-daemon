/* $Id$

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

package org.lockss.plugin.atypon.nrcresearchpress;

import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://www.amsciepub.com/doi/abs/10.2466/07.17.21.PMS.113.6.703-714 
 */
public class TestNRCResearchPressHtmlMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger(TestNRCResearchPressHtmlMetadataExtractorFactory.class);

  private MockArchivalUnit mau;
  private MockLockssDaemon theDaemon;

  private static String BASE_URL = "http://www.example.com";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    mau = new MockArchivalUnit();
    mau.setConfiguration(nrcAuConfig());
  }

  Configuration nrcAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_abbr", "jou");
    conf.put("volume_name", "nrc");
    return conf;
  }
/*
  <meta name="dc.Title" content="Skeletal muscle"></meta>
  <meta name="dc.Creator" content="LittleJonathan P."></meta>
  <meta name="dc.Creator" content="SafdarAdeel"></meta>
  <meta name="dc.Creator" content="BentonCarley R."></meta>
  <meta name="dc.Creator" content="WrightDavid C."></meta>
  <meta name="dc.Subject" content="exercise; mitochondria; muscle; adipose; AMPK; PGC-1α; exercice physique; mitochondrie; muscle; adipeux; AMPK; PGC-1α"></meta>
  <meta name="dc.Description" content="It has been known for more than 4 decades that exercise causes increases in skeletal muscle mitochondrial enzyme content and activity (i.e., mitochondrial biogenesis). Increasing evidence now suggests that exercise can induce mitochondrial biogenesis in a wide range of tissues not normally associated with the metabolic demands of exercise. Perturbations in mitochondrial content and (or) function have been linked to a wide variety of diseases, in multiple tissues, and exercise may serve as a potent approach by which to prevent and (or) treat these pathologies. In this context, the purpose of this review is to highlight the effects of exercise, and the underlying mechanisms therein, on the induction of mitochondrial biogenesis in skeletal muscle, adipose tissue, liver, brain, and kidney."></meta>
  <meta name="dc.Description" content="On sait depuis plus de quatre décennies que l’exercice physique suscite une augmentation de la concentration et de l’activité des enzymes de la mitochondrie dans le muscle squelettique : c’est la biogenèse mitochondriale. Maintenant, de plus en plus d’études scientifiques suggèrent que l’exercice physique suscite la biogenèse mitochondriale dans une vaste gamme de tissus habituellement non associés aux besoins énergétiques de l’exercice physique. Des études révèlent que des perturbations du contenu et de la fonction des mitochondries dans plusieurs tissus sont associées à bon nombre de maladies; dès lors, l’exercice physique pourrait servir d’approche préventive et curative à l’égard de ces pathologies. Cette analyse documentaire se propose donc de mettre en évidence les effets de l’exercice et d’en décrire les mécanismes dans le processus de la biogenèse mitochondriale dans le muscle squelettique, le tissu adipeux, le foie, le cerveau et le rein."></meta>
  <meta name="dc.Publisher" content=" NRC Research Press "></meta>
  <meta name="dc.Date" scheme="WTN8601" content="02 September 2011"></meta>
  <meta name="dc.Type" content="review-article"></meta>
  <meta name="dc.Format" content="text/HTML"></meta>
  <meta name="dc.Identifier" scheme="manuscript" content="2011-0089"></meta>
  <meta name="dc.Identifier" scheme="publisher-id" content="h11-076"></meta>
  <meta name="dc.Identifier" scheme="doi" content="10.1139/h11-076"></meta>
  <meta name="dc.Source" content="http://dx.doi.org/10.1139/h11-076"></meta><
  meta name="dc.Language" content="en"></meta><meta name="dc.Coverage" content="world"></meta>
  <meta name="keywords" content="exercise, mitochondria, muscle, adipose, AMPK, PGC-1α, exercice physique, mitochondrie, muscle, adipeux, AMPK, PGC-1α"></meta>
*/
  String goodDate = "Date";
  String dateScheme = "WTN8601";
  String goodTitle = "Title";
  String goodPublisher = "Publisher";
  String goodSubject = "Subject";
  String goodDescription = "Summary";
  String goodType = "Type";
  String goodFormat = "Format";
  String doiScheme = "doi";
  String goodDoi = "10.2446/123-456";
  String goodLanguage = "Language";
  String goodRights = "Rights";
  String goodCoverage = "Coverage";
  String authorA = "A. Author";
  String authorB = "B. Author";
  String authorC = "C. Author";
  String authorD = "D. Author";
  
  ArrayList<String> goodAuthors = new ArrayList<String>();
 
  //Unfortunately, it has to be on one line for an accurate representation (and to work)
  String goodContent =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
    "<html>\n" +
    "<head>\n" +        
    "<link rel=\"schema.DC\" href=\"http://purl.org/DC/elements/1.0/\">" +
                "</link><meta name=\"dc.Title\" content=\""+goodTitle+"\"></meta><meta name=\"dc.Creator\" content=\""+authorA+"\"></meta><meta name=\"dc.Creator\" content=\""+authorB+"\"></meta><meta name=\"dc.Creator\" content=\""+authorC+"\"></meta><meta name=\"dc.Creator\" content=\""+authorD+"\"></meta><meta name=\"dc.Description\" content=\""+goodDescription+"\"></meta><meta name=\"dc.Publisher\" content=\""+goodPublisher+"\"></meta><meta name=\"dc.Date\" scheme=\""+dateScheme+"\" content=\""+goodDate+"\"></meta><meta name=\"dc.Type\" content=\""+goodType+"\"></meta><meta name=\"dc.Format\" content=\""+goodFormat+
                "\"></meta>" +"</meta><meta name=\"dc.Language\" content=\""+goodLanguage+
                "\"></meta><meta name=\"dc.Coverage\" content=\""+goodCoverage+"\"></meta><meta name=\"dc.Rights\" content=\""+goodRights+"\"></meta>"+
                "\n</head>\n" +
                "</html>";

  public void testExtractFromGoodContent() throws Exception {
    goodAuthors.add(authorA);
    goodAuthors.add(authorB);
    goodAuthors.add(authorC);
    goodAuthors.add(authorD);

          
    String url = "http://www.example.com/vol1/issue2/art3/";
    CIProperties xmlHeader = new CIProperties();
    xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
    MockCachedUrl mcu = mau.addUrl(url, true, true, xmlHeader);
    mcu.setContent(goodContent);
    mcu.setContentSize(goodContent.length());
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    
    FileMetadataExtractor me =
      new BaseAtyponHtmlMetadataExtractorFactory.BaseAtyponHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(goodDate, md.get(MetadataField.DC_FIELD_DATE));
    assertEquals(goodTitle, md.get(MetadataField.DC_FIELD_TITLE));
    assertEquals(goodPublisher, md.get(MetadataField.DC_FIELD_PUBLISHER));
    assertEquals(goodAuthors, md.getList(MetadataField.DC_FIELD_CREATOR));
    assertEquals(goodDescription, md.get(MetadataField.DC_FIELD_DESCRIPTION));
    assertEquals(goodType, md.get(MetadataField.DC_FIELD_TYPE));
    assertEquals(goodFormat, md.get(MetadataField.DC_FIELD_FORMAT));
    assertEquals(goodLanguage, md.get(MetadataField.DC_FIELD_LANGUAGE));
    assertEquals(goodCoverage, md.get(MetadataField.DC_FIELD_COVERAGE));
  }
  
  String badContent =
    "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "<meta name=\"dc.Title\" content=\""+goodTitle+"\"></meta>" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    goodDescription + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, mau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me =
      new BaseAtyponHtmlMetadataExtractorFactory.BaseAtyponHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNotNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
    assertEquals(2, md.rawSize());
    assertEquals("bar", md.getRaw("foo"));
  }
  
  String allBadContent =
      "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" + 
      "<meta name=\"foo\"" +  " content=\"bar\">\n" +
      "  <div id=\"issn\">" +
      "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
      goodDescription + " </div>\n";

  /*
   * Base Atypon now declines to emit when there is no metadata of value 
   * as it's usually an indication of a bogus page - "Can't find this page" sort of html
   */
    public void testExtractFromNoCollectedContent() throws Exception {
      String url = "http://www.example.com/vol1/issue2/art3/";
      MockCachedUrl cu = new MockCachedUrl(url, mau);
      cu.setContent(allBadContent);
      cu.setContentSize(allBadContent.length());
      FileMetadataExtractor me =
        new BaseAtyponHtmlMetadataExtractorFactory.BaseAtyponHtmlMetadataExtractor();
      assertNotNull(me);
      log.debug3("Extractor: " + me.toString());
      FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
      assertEmpty(mdlist);
    }
  
}