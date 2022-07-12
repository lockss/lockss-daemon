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

package org.lockss.plugin.clockss.tangram;

import java.net.URL;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.DefinablePlugin;

/*
 * Test file used to extract metadata:
 *      basic examples to test metadata extraction
 */

public class TestTangramSourceXmlMetadataExtractorHelper
  extends LockssTestCase {

  static Logger log =
      Logger.getLogger(TestTangramSourceXmlMetadataExtractorHelper.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau; // source au
  private DefinablePlugin ap;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String ROOT_URL = "http://www.sourcecontent.com/"; //this is not a real url


  private static final String PLUGIN_NAME =
      "org.lockss.plugin.associationforcomputingmachinery.ClockssAssociationForComputingMachinerySourcePlugin";

  private static final String YEAR = "2014";

  private static final String BASE_URL =
      "http://clockss-ingest.lockss.org/sourcefiles/tangram-dev/"+ YEAR + "/";

  private static final String BASIC_CONTENT_FILE_NAME = "fromXlsx.xml";
  final static String pdfUrl = "test.pdf";
  final static String GOOD_AUTHOR1 = "John B. Lastname";
  final static String GOOD_AUTHOR1_INV = "Lastname, John B.";
  final static String GOOD_BOOK_TITLE = "Abitare ai margini della citt%C3%A0";
  final static String GOOD_SUBTITLE = "Trasformazione dei modelli insediativi residenziali moderni";
  final static String GOOD_CATEGORY = "Category";
  final static String GOOD_NPAGES = "301";
  final static String GOOD_ISBN = "9781450309950";
  final static String GOOD_PRICE = "22,00";
  final static String GOOD_PUBYEAR = "2012";
  final static String GOOD_URL = "http://www.edizioni-tangram.it/libri/abitare-ai-margini-della-citt%C3%A0";
  final static String GOOD_PDF = "9788864580517.pdf";
  final static String GOOD_BOOK_TITLE2 = "Peggio";
  final static String GOOD_SUBTITLE2 = "La violenza, il Reale";
  final static String GOOD_URL2 = "http://www.edizioni-tangram.it/libri/peggio";
  final static String GOOD_PDF2 = "9788864580937.pdf";
  final static String GOOD_AUTHOR2a = "Alessia J. Magliacane";
  final static String GOOD_AUTHOR2b = "Francesco Rubino";
  final static String GOOD_AUTHOR2_INV = "Magliacane, Alessia J., Rubino, Francesco";
  final static String GOOD_PUBYEAR2 = "2013";
  final static String GOOD_ISBN2 = "9788864580937";
  /*
  static private final Map<String, List> expectedMD =
    new HashMap<String,List>();
    static {
      expectedMD.put(BASE_URL+GOOD_PDF2, GOOD_AUTHORL);
}*/

  private static final String BASIC_ROW_CONTENT =
    "<catalogo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
      "<xml encoding=\"UTF-8\">" +
        "<node>" +
        "<Elenco_autori_curatori_principali>"+GOOD_AUTHOR1+"</Elenco_autori_curatori_principali>" +
        "<Elenco_autori_curatori_principali_INV>"+GOOD_AUTHOR1_INV+"</Elenco_autori_curatori_principali_INV>" +
        "<Titolo>"+GOOD_BOOK_TITLE+"</Titolo>" +
        "<Sottotitolo>" +GOOD_SUBTITLE+"</Sottotitolo>" +
        "<Categoria>Architettura, Moda e Design</Categoria>" +
        "<Formato>cm 15 x 19</Formato>" +
        "<Numero_pagine>"+GOOD_NPAGES+"</Numero_pagine>" +
        "<ISBN>"+GOOD_ISBN+"</ISBN>" +
        "<Prezzo_di_copertina>22,00</Prezzo_di_copertina>" +
        "<Data_pubblicazione>"+GOOD_PUBYEAR+"</Data_pubblicazione>" +
        "<PDF>"+ GOOD_PDF +"</PDF>" +
        "<URL_Libro>"+ GOOD_URL +"</URL_Libro>" +
        "</node>" +
      "</xml>" +
    "</catalogo>";

  // the following content does not have the "section" level -
  // should work with and without
  private static final String BASIC_MULTIROW_CONTENT =
    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
    "<catalogo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
      "<xml>" +
      "<node>" +
      "<Elenco_autori_curatori_principali>"+GOOD_AUTHOR1+"</Elenco_autori_curatori_principali>" +
      "<Elenco_autori_curatori_principali_INV>"+GOOD_AUTHOR1_INV+"</Elenco_autori_curatori_principali_INV>" +
      "<Titolo>"+GOOD_BOOK_TITLE+"</Titolo>" +
      "<Sottotitolo>" +GOOD_SUBTITLE+"</Sottotitolo>" +
      "<Categoria>Architettura, Moda e Design</Categoria>" +
      "<Formato>cm 15 x 19</Formato>" +
      "<Numero_pagine>301</Numero_pagine>" +
      "<ISBN>"+GOOD_ISBN+"</ISBN>" +
      "<Prezzo_di_copertina>22,00</Prezzo_di_copertina>" +
      "<Data_pubblicazione>2012</Data_pubblicazione>" +
      "<PDF>"+ GOOD_PDF +"</PDF>" +
      "<URL_Libro>"+ GOOD_URL +"</URL_Libro>" +
      "</node>" +
      "<node>" +
      "<Elenco_autori_curatori_principali>Alessia J. Magliacane, Francesco Rubino</Elenco_autori_curatori_principali>" +
      "<Elenco_autori_curatori_principali_INV>Magliacane Alessia J., Rubino Francesco</Elenco_autori_curatori_principali_INV>" +
      "<Titolo>"+GOOD_BOOK_TITLE2+"</Titolo>" +
      "<Sottotitolo>" +GOOD_SUBTITLE2+"</Sottotitolo>" +
      "<Categoria>Politica e Società</Categoria>" +
      "<Formato>cm 14,8 x 21</Formato>" +
      "<Numero_pagine>168</Numero_pagine>" +
      "<ISBN>"+GOOD_ISBN2+"</ISBN>" +
      "<Prezzo_di_copertina>15,00</Prezzo_di_copertina>" +
      "<Data_pubblicazione>"+GOOD_PUBYEAR2+"</Data_pubblicazione>" +
      "<Collana>Classi</Collana>" +
      "<PDF>"+GOOD_PDF2+"</PDF>" +
      "<URL_Libro>"+GOOD_URL2+"</URL_Libro>" +
      "</node>" +
    "</xml>" +
  "</catalogo>";

  private static final String EMPTY_CONTENT =
    "<catalogo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
    "<xml>" +
      "<node>" +
      "</node>" +
    "</xml>" +
  "</catalogo>";


  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...

    theDaemon = getMockLockssDaemon();

    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    mau = new MockArchivalUnit();
    mau.setConfiguration(auConfig());

  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  /**
   * Configuration method.
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", YEAR);
    return conf;
  }

  public TestTangramSourceXmlMetadataExtractorHelper() throws Exception {
    super.setUp();
    setUp();
  }

  //final static String TEST_XML_URL = "http://clockss-ingest.lockss.org/sourcefiles/tangram-dev/2014/Tes.xml";
  //final static String TEST_PDF_URL = "http://clockss-ingest.lockss.org/sourcefiles/tangram-dev/2014/"+GOOD_PDF;
  //final static String TEST_PDF2_URL = "http://clockss-ingest.lockss.org/sourcefiles/tangram-dev/2014/"+GOOD_PDF2;
  final static String TEST_XML_URL = BASE_URL + "Tes.xml";
  final static String TEST_PDF_URL = BASE_URL+GOOD_PDF;
  final static String TEST_PDF2_URL = BASE_URL+GOOD_PDF2;
  public void testExtractFromEmptyContent() throws Exception {
    String xml_url = TEST_XML_URL;
    String pdf_url = TEST_PDF_URL;
    String pdf2_url = TEST_PDF_URL;

    CIProperties xmlHeader = new CIProperties();
    URL base = new URL(BASE_URL);
    theDaemon.getLockssRepository(mau);
    theDaemon.getNodeManager(mau);

    MockCachedUrl xml_cu = mau.addUrl(xml_url, EMPTY_CONTENT);
    // doesn't matter what content the fake pdf_cu has
    MockCachedUrl pdf_cu = mau.addUrl(pdf_url, EMPTY_CONTENT);
    MockCachedUrl pdf2_cu = mau.addUrl(pdf2_url, EMPTY_CONTENT);

    FileMetadataExtractor me = new TangramSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), xml_cu);
    assertEmpty(mdlist);

  }

  /*
  public void testExtractFromBadContent() throws Exception {
    String xml_url = TEST_XML_URL;
    String pdf_url = TEST_PDF_URL;

    CIProperties xmlHeader = new CIProperties();

    MockCachedUrl xml_cu = mau.addUrl(xml_url, true, true, xmlHeader);
    xml_cu.setContent(BAD_CONTENT);
    xml_cu.setContentSize(BAD_CONTENT.length());
    MockCachedUrl pdf_cu = mau.addUrl(pdf_url, true, true, xmlHeader);
    // doesn't matter what content the fake pdf_cu has
    pdf_cu.setContent(BAD_CONTENT);
    pdf_cu.setContentSize(BAD_CONTENT.length());

    FileMetadataExtractor me = new TangramSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");

    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), xml_cu);
    assertEmpty(mdlist);

  }
  */

  // from original xml file from the publisher

  public void testExtractFromBasicRowContent() throws Exception {
    CIProperties xmlHeader = new CIProperties();
    try {
      String xml_url = TEST_XML_URL;
      String pdf_url = TEST_PDF_URL;
      String pdf2_url = TEST_PDF2_URL;

      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl cu = mau.addUrl(xml_url, true, true, xmlHeader);
      // need to check for this file before emitting, but contents don't matter
      MockCachedUrl pcu = mau.addUrl(pdf_url, EMPTY_CONTENT);
      MockCachedUrl pcu2 = mau.addUrl(pdf2_url, EMPTY_CONTENT);

      String string_input = BASIC_ROW_CONTENT;

      cu.setContent(string_input);
      cu.setContentSize(string_input.length());
      cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      FileMetadataExtractor me = new TangramSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
      assertNotNull(me);
      //log.debug3("Extractor: " + me.toString());
      FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
      assertNotEmpty(mdlist);
      ArticleMetadata md = mdlist.get(0);
      assertNotNull(md);

      log.info(md.get(MetadataField.FIELD_DATE));
      log.info("from cooked: "+md.get(MetadataField.FIELD_PUBLICATION_TITLE));
      log.info("from metadata: "+GOOD_BOOK_TITLE+":"+GOOD_SUBTITLE);


      assertEquals(GOOD_PUBYEAR, md.get(MetadataField.FIELD_DATE));
      //use FIELD_JOURNAL_TITLE for content5/6 until they adopt the latest daemon
      assertEquals(GOOD_BOOK_TITLE+":"+GOOD_SUBTITLE, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
      assertEquals(GOOD_ISBN, md.get(MetadataField.FIELD_ISBN));
      assertEquals(GOOD_AUTHOR1, md.get(MetadataField.FIELD_AUTHOR));


    } finally {
      //IOUtil.safeClose(file_input);
    }
  }

 /*
  // from original xml file from the publisher
  public void testExtractFromMultiRowContent() throws Exception {
    CIProperties xmlHeader = new CIProperties();
    try {
      String xml_url = BASE_URL + "basic.xml";
      String pdf_url = BASE_URL + GOOD_PDF;
      String pdf2_url = BASE_URL + GOOD_PDF2;

      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl cu = mau.addUrl(xml_url, true, true, xmlHeader);
      // need to check for this file before emitting, but contents don't matter
      MockCachedUrl pcu = mau.addUrl(pdf_url, EMPTY_CONTENT);
      MockCachedUrl pcu2 = mau.addUrl(pdf2_url, EMPTY_CONTENT);


      String string_input = BASIC_MULTIROW_CONTENT;

      cu.setContent(string_input);
      cu.setContentSize(string_input.length());
      cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      FileMetadataExtractor me = new TangramSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
      assertNotNull(me);
      log.debug3("Extractor: " + me.toString());
      FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
      assertNotEmpty(mdlist);
      ArticleMetadata md = mdlist.get(0);
      assertNotNull(md);

      assertEquals(GOOD_PUBYEAR, md.get(MetadataField.FIELD_DATE));
      assertEquals(GOOD_BOOK_TITLE+":"+GOOD_SUBTITLE, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
      assertEquals(GOOD_ISBN, md.get(MetadataField.FIELD_ISBN));
      assertEquals(GOOD_AUTHOR1, md.get(MetadataField.FIELD_AUTHOR));
      assertEquals(BASE_URL+GOOD_PDF, md.get(MetadataField.FIELD_ACCESS_URL));

       md = mdlist.get(1);
       assertNotNull(md);
       List <String> GOOD_AUTHORL = new ArrayList<String>();
       GOOD_AUTHORL.add(GOOD_AUTHOR2a);
       GOOD_AUTHORL.add(GOOD_AUTHOR2b);


       assertEquals(GOOD_PUBYEAR2, md.get(MetadataField.FIELD_DATE));
       assertEquals(GOOD_BOOK_TITLE2+":"+GOOD_SUBTITLE2, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
       assertEquals(GOOD_ISBN2, md.get(MetadataField.FIELD_ISBN));
       List<String> alist = md.getList(MetadataField.FIELD_AUTHOR);
       for (int i = 0; i < alist.size(); i++) {
         assertEquals(GOOD_AUTHORL.get(i), alist.get(i));
       }
       assertEquals(BASE_URL+GOOD_PDF2, md.get(MetadataField.FIELD_ACCESS_URL));

    } finally {
      //IOUtil.safeClose(file_input);
    }
  }
*/

 /*
  String realXMLFile = "TES.xml";
  public void testFromRealXMLFile() throws Exception {
    CIProperties xmlHeader = new CIProperties();
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile);
      //String string_input = StringUtil.fromInputStream(file_input);
      String string_input = StringUtil.fromReader(new InputStreamReader(file_input, Constants.ENCODING_UTF_8));
      IOUtil.safeClose(file_input);

      String xml_url = BASE_URL + realXMLFile;
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl cu = mau.addUrl(xml_url, true, true, xmlHeader);
      // need to check for this file before emitting
      mau.addUrl(xml_url, true, true, xmlHeader); //doesn't matter what content-type

      cu.setContent(string_input);
      cu.setContentSize(string_input.length());
      cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
      // setting content (non-pdf) just so the check can find content

      FileMetadataExtractor me = new TangramSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");

      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
      assertNotEmpty(mdlist);

      // check each returned md against expected values
      Iterator<ArticleMetadata> mdIt = mdlist.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
      }
    }finally {
      IOUtil.safeClose(file_input);
    }

  }
  */


}
