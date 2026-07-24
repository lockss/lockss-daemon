/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package edu.fcla.plugin.arkivoc;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;

import java.util.List;

public class TestArkivocXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestArkivocXmlMetadataExtractorFactory.class);

    private static String baseUrl = "http://www.sample.com/";
    private static String year = "2019";

    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "sample.xml";
        //String fname = "full_sample.xml";
        String journalXml = getResourceContent(fname);

        assertNotNull(journalXml);

        String xml_url = baseUrl + year + "/" + fname;
        CIProperties xmlHeader = new CIProperties();
        xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

        MockArchivalUnit mau = new MockArchivalUnit();
        MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
        mcu.setProperty(CachedUrl.PROPERTY_CONTENT_ENCODING, "UTF-8");
        mcu.setContent(journalXml);
        mcu.setContentSize(journalXml.length());
        mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

        FileMetadataExtractor me = new ArkivocXmlMetadataExtractorFactory().
                createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        //full_sample.xml has 63 records
        assertEquals(2, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);


        assertEquals("1551-7004", md.get(MetadataField.FIELD_ISSN));
        assertEquals("1551-7012", md.get(MetadataField.FIELD_EISSN));
        assertEquals("2022", md.get(MetadataField.FIELD_VOLUME));
        assertEquals("2", md.get(MetadataField.FIELD_ISSUE));
        assertEquals("Synthesis of a novel series of 1,2-dihydroquinoline-8-glyoxylamide derivatives", md.get(MetadataField.FIELD_ARTICLE_TITLE));
        assertEquals("2023-01-05", md.get(MetadataField.FIELD_DATE));
        assertEquals("10.24820/ark.5550190.p011.909", md.get(MetadataField.FIELD_DOI));

        ////log.info(md.get(MetadataField.FIELD_ACCESS_URL))
    }
}

