package org.lockss.plugin.clockss.britishacademy;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.plugin.clockss.igpublishing.IGPublishingMetadataExtractorFactory;
import org.lockss.plugin.clockss.igpublishing.TestIGPublishingMetadataExtractorFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.CIProperties;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

public class TestBritishAcademyJournalsSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestBritishAcademyJournalsSourceXmlMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/bapub-released/";
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

        FileMetadataExtractor me =  new BritishAcademyJournalsXmlMetadataExtractorFactory().
                createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(1, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

    }
}


