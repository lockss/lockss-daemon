/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.ejobsat;

import junit.framework.Test;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

public class TestEuropeanJournalBusinessScienceTechnologyHtmlFilterFactory extends LockssTestCase {

    FilterFactory variantHashFact = new EuropeanJournalBusinessScienceTechnologyHtmlFilterFactory();
    ArchivalUnit mau;
    String tempDirPath;
    MockLockssDaemon daemon;
    PluginManager pluginMgr;

    private static Logger log = Logger.getLogger(
            EuropeanJournalBusinessScienceTechnologyHtmlFilterFactory.class);


    private static final String PLUGIN_ID =
            "org.lockss.plugin.ejobsat.ClockssEuropeanJournalBusinessScienceTechnologyPlugin";

    private static String FullContent = "" +
            "<div id=\"sideLeft\">sideLeft content</div>\n" +
            "<div id=\"page_citshow\">page_citshow content</div>\n" +
            "<div id=\"page_citdown\">page_citdown content</div>\n" +
            "<ol class=\"reflist\"><li>reflist content</li></ol>\n" +
            "<div class=\"main\">main content</div>\n" +
            "<p class=\"ccinfo\">ccinfo</p>" ;

    private static final String FullContentHashFiltered = "" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "<div class=\"main\">main content</div>\n" +
            "" ;

    protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
        return PluginTestUtil.createAndStartAu(PLUGIN_ID, thisAuConfig());
    }

    private Configuration thisAuConfig() {
        Configuration conf = ConfigManager.newConfiguration();
        conf.put("base_url", "http://www.example.com/");
        conf.put("journal_id", "ejo");
        conf.put("year", "1999");
        return conf;
    }


    private static String getFilteredContent(ArchivalUnit au, FilterFactory fact, String nameToHash) {

        InputStream actIn;
        String filteredStr = "";

        try {
            actIn = fact.createFilteredInputStream(au,
                    new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);

            try {

                filteredStr = StringUtil.fromInputStream(actIn);

            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        } catch (PluginException e) {
            log.error(e.getMessage(), e);
        }

        return filteredStr;
    }

    public void startMockDaemon() {
        daemon = getMockLockssDaemon();
        pluginMgr = daemon.getPluginManager();
        pluginMgr.setLoadablePluginsReady(true);
        daemon.setDaemonInited(true);
        pluginMgr.startService();
        daemon.getAlertManager();
        daemon.getCrawlManager();
    }

    public void setUp() throws Exception {
        super.setUp();
        tempDirPath = setUpDiskSpace();
        startMockDaemon();
        mau = createAu();
    }

    public static class TestHash extends TestEuropeanJournalBusinessScienceTechnologyHtmlFilterFactory {

        public void testFullContentHash() throws Exception {
            String unicodeFilteredStr = getFilteredContent(mau, variantHashFact, FullContent);
            String unicodeExpectedStr = FullContentHashFiltered;

            assertEquals(unicodeFilteredStr, unicodeExpectedStr);
        }
    }

    public static Test suite() {
        return variantSuites(new Class[] {
                TestEuropeanJournalBusinessScienceTechnologyHtmlFilterFactory.TestHash.class
        });
    }

}


