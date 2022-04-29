package org.lockss.plugin.clockss.wiley;

import org.apache.commons.io.FileUtils;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.plugin.clockss.intechopen.IntechOpenBookOnix3XmlMetadataExtractorFactory;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.CIProperties;
import org.lockss.util.Constants;
import org.lockss.util.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestWileyBooksSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestWileyBooksSourceXmlMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/wiley-books/";
    private static String Directory = "2019";

    private static String getXmlFileContent(String fname) {
        String xmlContent = "";
        try {
            String currentDirectory = System.getProperty("user.dir");
            String pathname = currentDirectory +
                    "/plugins/test/src/org/lockss/plugin/clockss/wiley/" + fname;
            xmlContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return xmlContent;
    }

    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "wiley_fmatter1.xml";
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

        FileMetadataExtractor me =  new WileyBooksSourceXmlMetadataExtractorFactory().
                createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(1, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

        assertEquals("9781444304831", md.get(MetadataField.FIELD_ISBN));
        assertEquals("10.1002/9781444304831", md.get(MetadataField.FIELD_DOI));
        //log.info("author=====" + md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("Schwarz, Daniel R.", md.get(MetadataField.FIELD_AUTHOR));

        //log.info("title=" + md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        //log.info("publisher======" + md.get(MetadataField.FIELD_PUBLISHER));

        //log.info("date======" + md.get(MetadataField.FIELD_DATE));
        assertEquals("2008-09-05", md.get(MetadataField.FIELD_DATE));
    }

    public void testExtractArticleXmlSchema2() throws Exception {

        String fname = "wiley_fmatter2.xml";
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

        FileMetadataExtractor me =  new WileyBooksSourceXmlMetadataExtractorFactory().
                createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(1, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        
        assertEquals("9783433600894", md.get(MetadataField.FIELD_ISBN));
        assertEquals("10.1002/9783433600894", md.get(MetadataField.FIELD_DOI));
        //log.info("author=====" + md.get(MetadataField.FIELD_AUTHOR));
        //assertEquals("Schwarz, Daniel R.", md.get(MetadataField.FIELD_AUTHOR));

        //log.info("title=" + md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("Baugruben: Berechnungsverfahren", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        //log.info("publisher======" + md.get(MetadataField.FIELD_PUBLISHER));

        //log.info("date======" + md.get(MetadataField.FIELD_DATE));
        //assertEquals("2010-10-20", md.get(MetadataField.FIELD_DATE));
    }
}

