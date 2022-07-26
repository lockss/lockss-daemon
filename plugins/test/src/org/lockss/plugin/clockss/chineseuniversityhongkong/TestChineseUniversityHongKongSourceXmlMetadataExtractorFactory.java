package org.lockss.plugin.clockss.chineseuniversityhongkong;

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
import java.io.InputStream;
import java.util.List;
import org.lockss.test.ConfigurationUtil;
import org.lockss.util.*;

public class TestChineseUniversityHongKongSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestChineseUniversityHongKongSourceXmlMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/chineseuniversityhongkong/";
    private static String Directory = "2019";

    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "article_sample.xml";
        String journalXml = getResourceContent(fname);

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

        FileMetadataExtractor me =  new ChineseUniversityHongKongSourceXmlMetadataExtractorFactory().
                createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        assertEquals(1, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        
        assertEquals("1994-06", md.get(MetadataField.FIELD_DATE));
    }
}

