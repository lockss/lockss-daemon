/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.springer;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.ConfigurationUtil;

import java.util.regex.Pattern;

public class TestSpringerJatsSourceZipXmlArticleIteratorFactory extends ArticleIteratorTestCase {

    private SimulatedArchivalUnit sau;    // Simulated AU to generate content

    private final String PLUGIN_NAME = "org.lockss.plugin.springer.ClockssSpringerJatsSourcePlugin";
    private static final String BASE_URL_KEY = "base_url";
    private static final String BASE_URL = "http://content5.lockss.org/sourcefiles/springerjats-released/";
    private static final String DIRECTORY_KEY = "directory";
    private static final String DIRECTORY = "2019_05";
    private static final int DEFAULT_FILESIZE = 3000;
    private final Configuration AU_CONFIG =
            ConfigurationUtil.fromArgs(BASE_URL_KEY, BASE_URL,
                    DIRECTORY_KEY, DIRECTORY);

    public void setUp() throws Exception {
        super.setUp();
        String tempDirPath = setUpDiskSpace();

        au = createAu();
        sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
    }

    public void tearDown() throws Exception {
        sau.deleteContentTree();
        super.tearDown();
    }

    protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
        return
                PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
    }

    Configuration simAuConfig(String rootPath) {
        Configuration conf = ConfigManager.newConfiguration();
        conf.put("root", rootPath);
        conf.put(BASE_URL_KEY, BASE_URL);
        conf.put(DIRECTORY_KEY, DIRECTORY);
        conf.put("depth", "1");
        conf.put("branch", "5");
        conf.put("numFiles", "4");
        conf.put("fileTypes",
                "" + (  SimulatedContentGenerator.FILE_TYPE_HTML
                        | SimulatedContentGenerator.FILE_TYPE_XML
                        | SimulatedContentGenerator.FILE_TYPE_PDF));
        conf.put("binFileSize", ""+DEFAULT_FILESIZE);
        return conf;
    }

    public void testCreateArticleFiles() throws Exception {
        PluginTestUtil.crawlSimAu(sau);

        String pat1 =  "/$1_Article$3_nlm.xml";
        String rep1 = "content5.lockss.org/sourcefiles/springerjats-released/2019_5/ftp_PUB_19-09-16_05-04-12.zip!/JOU=00441/VOL=2019.378/ISU=1/ART=3050/441_2019_Article_3050_nlm.xml";
        PluginTestUtil.copyAu(sau, au, "/.*\\.xml(\\.Meta)?$", pat1, rep1);
        String pat2 = "/(.*)_(Article|OnlinePDF)(_\\d+)?\\.pdf$";
        String rep2 = "content5.lockss.org/sourcefiles/springerjats-released/2019_5/ftp_PUB_19-09-16_05-04-07.zip!/JOU=41371/VOL=2019.33/ISU=S1/ART=217/BodyRef/PDF/41371_2019_Article_217.pdf";
        PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat2, rep2);

        int xmlCount = 0;
        int pdfCount = 0;

        for (CachedUrl cu : AuUtil.getCuIterable(au)) {
            String url = cu.getUrl();
            log.info("url: " + url);
            if (url.contains("Article")) {
                if (url.endsWith(".xml")) {
                    log.info("Article url = " + url);
                }
                xmlCount++;
            } else if (url.endsWith(".pdf")) {
                pdfCount++;
            }
        }
        log.info("xml count is " + xmlCount);
        log.info("pdf count is " + pdfCount);

    }

    public void testUrlsWithPrefixes() throws Exception {
        SubTreeArticleIterator artIter = createSubTreeIter();
        Pattern pat = getPattern(artIter);

        assertNotMatchesRE(pat, "http://content5.lockss.org/sourcefiles/springerjats-released/2019_5/ftp_PUB_19-09-16_05-04-12.zip!/JOU=00441/VOL=2019.378/ISU=1/ART=3050/441_2019_Article_3050_nlm.xml");
        assertNotMatchesRE(pat, "http://content5.lockss.org/sourcefiles/springerjats-released/2019_5/ftp_PUB_19-09-16_05-04-12.zip!/BSE=7911/BOK=978-3-030-31143-8/978-3-030-31143-8_Book_nlm.xml");
        assertNotMatchesRE(pat, "http://content5.lockss.org/sourcefiles/springerjats-released/2019_5/ftp_PUB_19-09-16_05-04-12.zip!/BSE=7651/BOK=978-1-4939-1346-6/978-1-4939-1346-6_Book_nlm.xml");
        assertNotMatchesRE(pat, "http://content5.lockss.org/sourcefiles/springerjats-released/2019_5/ftp_PUB_19-09-16_05-04-12.zip!/JOU=00441/VOL=2019.378/ISU=1/ART=3050/441_2019_Article_3050_nlm.xml.Metadata");
        assertNotMatchesRE(pat, "http://content5.lockss.org/sourcefiles/springerjats-released/2019_5/ftp_PUB_19-09-16_05-04-12.zip!/JOU=00441/VOL=2019.378/ISU=1/ART=3050/441_2019_Article_3050_nl.html");
        assertNotMatchesRE(pat, "http://content5.lockss.org/sourcefiles/springerjats-released/2019_5/ftp_PUB_19-09-16_05-04-12.zip!/BSE=7911/BOK=978-3-030-31143-8/978-3-030-31143-8_Book.txt");
        assertNotMatchesRE(pat, "http://content5.lockss.org/sourcefiles/springerjats-released/2019_5/ftp_PUB_19-09-16_05-04-12.zip!/BSE=7651/BOK=978-1-4939-1346-6/978-1-4939-1346-6_Book_nlm.png");
        
        assertMatchesRE(pat, "http://content5.lockss.org/sourcefiles/springerjats-released/2019_5/ftp_PUB_19-09-16_05-04-12.zip!/JOU=00441/VOL=2019.378/ISU=1/ART=3050/441_2019_Article_3050.pdf");
        assertMatchesRE(pat, "http://content5.lockss.org/sourcefiles/springerjats-released/2019_5/ftp_PUB_19-09-16_05-04-12.zip!/JOU=00441/VOL=2019.378/ISU=1/ART=3050/40278_2018_52346_OnlinePDF.pdf");
        assertNotMatchesRE(pat, "http://content5.lockss.org/sourcefiles/springerjats-released/2019_5/ftp_PUB_19-09-16_05-04-12.zip!/JOU=00441/VOL=2019.378/ISU=1/ART=3050/40278_2018_52346_PrintPDF.pdf");
    }

}
