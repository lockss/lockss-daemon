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

package org.lockss.plugin.clockss.isecs;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public class TestInternationalStructuralEngineeringConstructionSocietySourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

  private static final Logger log = Logger.getLogger(TestInternationalStructuralEngineeringConstructionSocietySourceXmlMetadataExtractorFactory.class);

  private static String BaseUrl = "http://source.host.org/sourcefiles/isecs/";
  private static String Directory = "2019";

  public void testExtractArticleXmlSchema() throws Exception {

    String fname = "sample.xml";
    String journalXml = getResourceContent(fname);

    assertNotNull(journalXml);

    String xml_url = BaseUrl + Directory + "/" + fname;
    CIProperties xmlHeader = new CIProperties();
    xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

    MockArchivalUnit mau = new MockArchivalUnit();
    MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
    mcu.setContent(journalXml);
    mcu.setContentSize(journalXml.length());
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

    FileMetadataExtractor me =  new InternationalStructuralEngineeringConstructionSocietySourceXmlMetadataExtractorFactory().
            createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
    FileMetadataListExtractor mle =
            new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    assertEquals(1, mdlist.size());

    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals("3", md.get(MetadataField.FIELD_VOLUME));
    assertEquals("2", md.get(MetadataField.FIELD_ISSUE));
    assertEquals("FIRE SPREADING ANALYSIS OF A GROUP OF WOODEN HOUSES IN TOWNSCAPE FORMATION DISTRICT", md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals("2644-108X", md.get(MetadataField.FIELD_ISSN));
    assertEquals("Tomiya Takatani", md.get(MetadataField.FIELD_AUTHOR));
    assertEquals("Proceedings of International Structural Engineering and Construction", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals("ISEC Press", md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals("10-2016", md.get(MetadataField.FIELD_DATE));
  }
}

