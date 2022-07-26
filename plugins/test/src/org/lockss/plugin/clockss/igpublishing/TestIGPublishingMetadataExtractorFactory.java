package org.lockss.plugin.clockss.igpublishing;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class TestIGPublishingMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestIGPublishingMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/igroupnet-released/";
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

        FileMetadataExtractor me =  new IGPublishingMetadataExtractorFactory().
                createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(1, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

        assertEquals("9781788398299", md.get(MetadataField.FIELD_ISBN));
        assertEquals("9781788479028", md.get(MetadataField.FIELD_EISBN));
        assertEquals("Soni, Mitesh", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("Practical AWS: networking build and manage complex networks using services such as Amazon VPC, Elastic Load Balancing, Direct Connect, and Amazon Route 53", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("IG Publishing", md.get(MetadataField.FIELD_PUBLISHER));
        assertEquals("2018", md.get(MetadataField.FIELD_DATE));
    }
}


