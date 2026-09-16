package org.lockss.plugin.clockss.ofhbm;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;

import java.util.List;

public class TestOrganizationForHumanBrainMappingJatsXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestOrganizationForHumanBrainMappingJatsXmlMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/ofhbm/";
    private static String Directory = "2026";

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

        FileMetadataExtractor me = new OrganizationForHumanBrainMappingJatsXmlMetadataExtractorFactory().
                createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(1, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

        assertEquals("2957-3963", md.get(MetadataField.FIELD_EISSN));
        assertEquals("Impact of multi-echo ICA modeling decisions on motor-task fMRI analysis", md.get(MetadataField.FIELD_ARTICLE_TITLE));
        assertEquals("Aperture Neuro", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("10.52294/001c.162891", md.get(MetadataField.FIELD_DOI));
        assertEquals("6", md.get(MetadataField.FIELD_VOLUME));
        assertNull(md.get(MetadataField.FIELD_ISSN));
        assertNull(md.get(MetadataField.FIELD_ISSUE));
        assertEquals("2026-9-4", md.get(MetadataField.FIELD_DATE));
    }
}
