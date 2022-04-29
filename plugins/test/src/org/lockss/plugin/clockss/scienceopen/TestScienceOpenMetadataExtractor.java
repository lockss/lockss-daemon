/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.clockss.scienceopen;

import org.lockss.config.ConfigManager;
import org.lockss.config.Tdb;
import org.lockss.config.TdbAu;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.test.ConfigurationUtil;
import org.lockss.util.*;
import org.w3c.dom.Document;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class TestScienceOpenMetadataExtractor extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestScienceOpenMetadataExtractor.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/scienceopen/";
    private static String Directory = "2020";

    private ArchivalUnit bau;
    static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        ConfigurationUtil.addFromUrl(getResource("test_scienceopen.xml"));
        Tdb tdb = ConfigManager.getCurrentConfig().getTdb();

        TdbAu tdbau1 = tdb.getTdbAusLikeName( "").get(0);
        assertNotNull("Didn't find named TdbAu",tdbau1);
        bau = PluginTestUtil.createAndStartAu(tdbau1);
        assertNotNull(bau);
        TypedEntryMap auConfig =  bau.getProperties();
        assertEquals(BaseUrl, auConfig.getString(BASE_URL_KEY));

    }

    private String getXmlFileContent(String fname) {
        String xmlContent = "";
        InputStream file_input = null;

        try {
            file_input = getResourceAsStream(fname);
            xmlContent = StringUtil.fromInputStream(file_input);
            IOUtil.safeClose(file_input);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            IOUtil.safeClose(file_input);
        }
        return xmlContent;
    }

    private List<ArticleMetadata> setupContentForAU(ArchivalUnit au,
                                                    String fname
    ) throws IOException, PluginException {
        String content = getXmlFileContent(fname);
        return setupContentForAU(au, fname, content);
    }

    private List<ArticleMetadata> setupContentForAU(ArchivalUnit au,
                                                    String fname,
                                                    String content
    ) throws IOException, PluginException {
        String url =  BaseUrl + Directory + "/" + fname;
        InputStream input = IOUtils.toInputStream(content, "utf-8");
        CIProperties xmlHead = new CIProperties();
        xmlHead.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
        FileMetadataExtractor me =
            new ScienceOpenSourceXmlMetadataExtractorFactory.JatsPublishingSourceXmlMetadataExtractor();
        UrlData ud = new UrlData(input, xmlHead, url);
        UrlCacher uc = au.makeUrlCacher(ud);
        uc.storeContent();
        CachedUrl cu = uc.getCachedUrl();
        FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
        return mle.extract(MetadataTarget.Any(), cu);
    }

    public void testExtractArticleXmlSchema() throws Exception {

        String fname = "sample_test_jats.xml";
        List<ArticleMetadata> mdlist = setupContentForAU(bau, fname);
        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        assertEquals("10.14293/S2199-1006.1.SOR-LIFE.AAC0E6.v2", md.get(MetadataField.FIELD_DOI));
        assertEquals("2014", md.get(MetadataField.FIELD_DATE));
        assertEquals("10", md.get(MetadataField.FIELD_END_PAGE));
        assertEquals("1", md.get(MetadataField.FIELD_START_PAGE));
        assertEquals(null, md.get(MetadataField.FIELD_ISSN));
        assertEquals("2199-1006", md.get(MetadataField.FIELD_EISSN));
        assertEquals("0", md.get(MetadataField.FIELD_VOLUME));
        assertEquals("0", md.get(MetadataField.FIELD_ISSUE));
        assertEquals("Ellina, Maria-Ioanna", md.get(MetadataField.FIELD_AUTHOR));
        assertEquals("ScienceOpen Research", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("BCS Learning & Development", md.get(MetadataField.FIELD_PUBLISHER));
        // ensure ScienceOpen was set from tdb file
        assertEquals("ScienceOpen", md.get(MetadataField.FIELD_PROVIDER));
        assertEquals("Epidermal growth factor/epidermal growth factor receptor signaling axis is a significant regulator of the proteasome expression and activity in colon cancer cells", md.get(MetadataField.FIELD_ARTICLE_TITLE));

    }

    public void testExtractArticleXmlSchemaFromNonStandardJATS() throws Exception {

        String fname = "sample_test_non_standard_jats.xml";
        String journalXml = getXmlFileContent(fname);
        assertNotNull(journalXml);

        String xml_url = BaseUrl + Directory + "/" + fname;

        FileMetadataExtractor me = new MyJatsPublishingSourceXmlMetadataExtractor();
        FileMetadataListExtractor mle =
                new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = extractFromContent(xml_url, "text/xml", journalXml, mle);


        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        assertEquals("10.14236/ewic/EVAC18.26", md.get(MetadataField.FIELD_DOI));
        assertEquals("2018", md.get(MetadataField.FIELD_DATE));
        assertEquals("8", md.get(MetadataField.FIELD_END_PAGE));
        assertEquals("1", md.get(MetadataField.FIELD_START_PAGE));
        //assertEquals("1477-9358", md.get(MetadataField.FIELD_ISSN));   handled in post cook process
        assertEquals(null, md.get(MetadataField.FIELD_EISSN));
        //assertEquals("BCS Learning & Development", md.get(MetadataField.FIELD_PUBLICATION_TITLE));     handled in post cook process


    }

    private class MyJatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractor {

        private SourceXmlSchemaHelper JatsPublishingHelper = null;

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
            throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
        }

        @Override
        protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document xmlDoc) {
            if (JatsPublishingHelper == null) {
                JatsPublishingHelper = new ScienceOpenSchemaHelper();
            }
            return JatsPublishingHelper;
        }

        @Override
        protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                                ArticleMetadata oneAM) {

            String url_string = cu.getUrl();
            List<String> returnList = new ArrayList<String>();
            returnList.add(url_string);
            return returnList;
        }

        @Override
        protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                       CachedUrl cu, ArticleMetadata thisAM) {

            //If we didn't get a valid date value, use the copyright year if it's there
            if (thisAM.get(MetadataField.FIELD_DATE) == null) {
                if (thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date) != null) {
                    thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date));
                } else {// last chance
                    thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_edate));
                }
            }
        }

    }

}

