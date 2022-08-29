/*
 * $Id:$
 */
/*

/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.edituraase;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.plugin.clockss.isecs.InternationalStructuralEngineeringConstructionSocietySourceXmlMetadataExtractorFactory;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;

import java.util.List;


public class TestEdituraASEXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

  private static final Logger log = Logger.getLogger(TestEdituraASEXmlMetadataExtractorFactory.class);

  private static String BaseUrl = "https://clockss-test.lockss.org/sourcefiles/ease-released/";
  private static String Directory = "2022_01";
  private static final String pdf_url =  "/60/Article_3109.pdf";

  public void testExtractArticleXmlSchema() throws Exception {

    String fname = "sample.xml";
    String journalXml = getResourceContent(fname);

    //log.info(journalXml);
    assertNotNull(journalXml);

    String xml_url = BaseUrl + Directory + "/" + fname;
    CIProperties xmlHeader = new CIProperties();
    xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

    MockArchivalUnit mau = new MockArchivalUnit();
    MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
    log.info(BaseUrl + Directory + pdf_url);
    mau.addUrl(BaseUrl + Directory + pdf_url, true, true, xmlHeader);
    mcu.setContent(journalXml);
    mcu.setContentSize(journalXml.length());
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

    FileMetadataExtractor me =  new EdituraASEXmlMetadataExtractorFactory().
            createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
    FileMetadataListExtractor mle =
            new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    assertEquals(1, mdlist.size());

    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals("24", md.get(MetadataField.FIELD_VOLUME));
    assertEquals("60", md.get(MetadataField.FIELD_ISSUE));
    assertEquals("Bibliometric Analysis of the Green Deal Policies in the Food Chain", md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals("15829146", md.get(MetadataField.FIELD_ISSN));
    assertEquals("Popescu, Dorin Vicentiu", md.get(MetadataField.FIELD_AUTHOR));
    assertEquals("www.amfiteatrueconomic.ro", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals("Editura ASE", md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals("2022", md.get(MetadataField.FIELD_DATE));
  }
}

