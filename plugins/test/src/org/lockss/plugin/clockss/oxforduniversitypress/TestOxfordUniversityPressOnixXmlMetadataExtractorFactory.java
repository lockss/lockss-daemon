package org.lockss.plugin.clockss.oxforduniversitypress;

import org.apache.commons.io.FileUtils;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.CIProperties;
import org.lockss.util.Constants;
import org.lockss.util.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.lockss.plugin.clockss.oxforduniversitypress.OxfordUniversityPressOnixXmlMetadataExtractorFactory;

public class TestOxfordUniversityPressOnixXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestOxfordUniversityPressOnixXmlMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/oup/";
    private static String Directory = "2019";

    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "onix2_sample.xml";
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

        FileMetadataExtractor me =  new OxfordUniversityPressOnixXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(1, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

        assertEquals("9780198283652", md.get(MetadataField.FIELD_ISBN));
        assertEquals("Hunger and Public Action", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("Oxford University Press", md.get(MetadataField.FIELD_PUBLISHER));
        assertEquals("1991-01-24", md.get(MetadataField.FIELD_DATE));
        assertEquals("10.1093/0198283652.001.0001", md.get(MetadataField.FIELD_DOI));
    }
}

