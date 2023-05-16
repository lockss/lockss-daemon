package org.lockss.plugin.clockss.isprs;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;

import java.util.List;

public class TestISPRSXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestISPRSXmlMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/isprs/";
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

        FileMetadataExtractor me =  new ISPRSXmlMetadataExtractorFactory().
                createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(1, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

        assertEquals("2194-9034", md.get(MetadataField.FIELD_EISSN));
        assertEquals("MODELLING AND VISUALIZING URBAN GROWTH TRAJECTORY IN ABU DHABI USING TIME SERIES SATELLITE IMAGERY", md.get(MetadataField.FIELD_ARTICLE_TITLE));
        assertEquals("The International Archives of the Photogrammetry, Remote Sensing and Spatial Information Sciences", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("2023", md.get(MetadataField.FIELD_DATE));
        assertEquals("10.5194/isprs-archives-XLVIII-M-1-2023-145-2023", md.get(MetadataField.FIELD_DOI));
        assertEquals("XLVIII-M-1-2023", md.get(MetadataField.FIELD_VOLUME));
        //assertEquals("Copernicus Publications", md.get(MetadataField.FIELD_PUBLISHER)); this field come from tdb file
    }
}

