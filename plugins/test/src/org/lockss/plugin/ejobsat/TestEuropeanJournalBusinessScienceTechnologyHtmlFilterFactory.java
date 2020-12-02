package org.lockss.plugin.ejbst;

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
            "org.lockss.plugin.ejbst.ClockssEuropeanJournalBusinessScienceTechnologyPlugin";

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
                e.printStackTrace();
            }
        } catch (PluginException e) {
            e.printStackTrace();
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


