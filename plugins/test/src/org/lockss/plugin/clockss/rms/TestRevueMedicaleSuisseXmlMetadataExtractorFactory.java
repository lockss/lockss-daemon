package org.lockss.plugin.clockss.rms;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;

import java.util.List;

public class TestRevueMedicaleSuisseXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestRevueMedicaleSuisseXmlMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/oup/";
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

        FileMetadataExtractor me =  new RevueMedicaleSuisseXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(1, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

        assertEquals("Revue Médicale Suisse", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("2007", md.get(MetadataField.FIELD_DATE));
        assertEquals("1660-9379", md.get(MetadataField.FIELD_ISSN));
    }
}

