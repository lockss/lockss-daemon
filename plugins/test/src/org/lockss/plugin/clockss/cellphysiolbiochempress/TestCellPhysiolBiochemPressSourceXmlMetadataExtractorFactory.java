package org.lockss.plugin.clockss.cellphysiolbiochempress;

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
import org.apache.commons.io.FileUtils;

public class TestCellPhysiolBiochemPressSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestCellPhysiolBiochemPressSourceXmlMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/cpbp-released/";
    private static String Directory = "2022_01";

    private static String getXmlFileContent(String fname) {
        String xmlContent = "";
        try {
            String currentDirectory = System.getProperty("user.dir");
            String pathname = currentDirectory +
                    "/plugins/test/src/org/lockss/plugin/clockss/cellphysiolbiochempress/" + fname;
            xmlContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return xmlContent;
    }

    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "article_sample.xml";
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

        FileMetadataExtractor me =  new CellPhysiolBiochemPressSourceXmlMetadataExtractorFactory().
                createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(1, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

        assertEquals("Cell Physiol Biochem", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("56", md.get(MetadataField.FIELD_VOLUME));
        assertEquals("1", md.get(MetadataField.FIELD_ISSUE));
        assertEquals("2022-1-12", md.get(MetadataField.FIELD_DATE));
        assertEquals("Cell Physiol Biochem Press", md.get(MetadataField.FIELD_PUBLISHER));
        assertEquals("10.33594/000000488", md.get(MetadataField.FIELD_DOI));
        assertEquals("1015-8987", md.get(MetadataField.FIELD_ISSN));
        assertEquals("Ayari, Houda", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("Apelin-13 Decreases Epithelial Sodium Channel (ENaC) Expression and Activity in Kidney Collecting Duct Cells", md.get(MetadataField.FIELD_ARTICLE_TITLE));
    }
}