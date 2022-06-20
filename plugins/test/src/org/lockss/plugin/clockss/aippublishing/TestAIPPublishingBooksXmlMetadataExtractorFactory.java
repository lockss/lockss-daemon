package org.lockss.plugin.clockss.aippublishing;

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

public class TestAIPPublishingBooksXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestAIPPublishingBooksXmlMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/aip-released/";
    private static String Directory = "2022_01";

    private static String getXmlFileContent(String fname) {
        String xmlContent = "";
        try {
            String currentDirectory = System.getProperty("user.dir");
            String pathname = currentDirectory +
                    "/plugins/test/src/org/lockss/plugin/clockss/aippublishing/" + fname;
            xmlContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return xmlContent;
    }

    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "9780735421547.onix.xml";
        String journalXml = getXmlFileContent(fname);

        assertNotNull(journalXml);

        String xml_url = BaseUrl + Directory + "/" + fname;
        CIProperties xmlHeader = new CIProperties();
        xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

        MockArchivalUnit mau = new MockArchivalUnit();
        MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
        mcu.setProperty(CachedUrl.PROPERTY_CONTENT_ENCODING, "UTF-8");
        mcu.setContent(journalXml);
        mcu.setContentSize(journalXml.length());
        mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
        mau.addUrl("http://source.host.org/sourcefiles/aip-released/2022_01/aipp.onix.9780735421547", true, true, xmlHeader);
        mau.addUrl("http://source.host.org/sourcefiles/aip-released/2022_01/9780735421547.pdf", true, true, xmlHeader);


        FileMetadataExtractor me =  new AIPPublishingBooksXmlMetadataExtractorFactory().
                createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(1, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        
        assertEquals("With You When You Fly: Aeronautics for Introductory Physics: ", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("2015", md.get(MetadataField.FIELD_DATE));
        assertEquals("AIP Publishing LLC", md.get(MetadataField.FIELD_PUBLISHER));
        assertEquals("10.1063/9780735421547", md.get(MetadataField.FIELD_DOI));
        assertEquals("9780735421547", md.get(MetadataField.FIELD_ISBN));
        assertEquals("NASA", md.get(MetadataField.FIELD_AUTHOR));
    }
}