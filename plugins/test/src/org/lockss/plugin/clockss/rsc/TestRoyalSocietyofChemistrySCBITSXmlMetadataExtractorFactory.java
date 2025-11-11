package org.lockss.plugin.clockss.rsc;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;

import java.util.List;

public class TestRoyalSocietyofChemistrySCBITSXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestRoyalSocietyofChemistrySCBITSXmlMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/oup/";
    private static String Directory = "2025_01";

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

        FileMetadataExtractor me =  new  RoyalSocietyofChemistrySCBITSXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(1, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

        assertEquals("Polyoxometalate Chemistry", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("2026-March-30", md.get(MetadataField.FIELD_DATE));
        assertEquals("2977-0084", md.get(MetadataField.FIELD_EISSN));
        assertEquals("10.1039/9781837679638", md.get(MetadataField.FIELD_DOI));
        assertEquals("978-1-83707-209-5", md.get(MetadataField.FIELD_EISBN));
        assertEquals("978-1-83707-203-3", md.get(MetadataField.FIELD_ISBN));
        assertEquals("3", md.get(MetadataField.FIELD_VOLUME));

    }
}

