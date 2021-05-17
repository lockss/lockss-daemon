package org.lockss.plugin.clockss.liverpool;

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

public class TestLiverpoolOnix3XmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestLiverpoolOnix3XmlMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/liverpool/";
    private static String Directory = "2019";

    private static String getXmlFileContent(String fname) {
        String xmlContent = "";
        try {
            String currentDirectory = System.getProperty("user.dir");
            String pathname = currentDirectory +
                    "/plugins/test/src/org/lockss/plugin/clockss/liverpool/" + fname;
            xmlContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return xmlContent;
    }

    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "sample_onix3_metadata.xml";
        String journalXml = getXmlFileContent(fname);

        assertNotNull(journalXml);

        String xml_url = BaseUrl + Directory + "/" + fname;
        CIProperties xmlHeader = new CIProperties();
        xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

        MockArchivalUnit mau = new MockArchivalUnit();
        MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
        mcu.setContent(journalXml);
        mcu.setContentSize(journalXml.length());
        mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

        FileMetadataExtractor me =  new LiverpoolUniversityPressBookOnix3XmlMetadataExtractorFactory().
                createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(1, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

        assertEquals("9781786949639", md.get(MetadataField.FIELD_ISBN));
        assertEquals("Scott, Bede", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("Affective Disorders: Emotion in Colonial and Postcolonial Literature", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("Liverpool University Press", md.get(MetadataField.FIELD_PUBLISHER));
        assertEquals("2019-09-28", md.get(MetadataField.FIELD_DATE));
    }
}

