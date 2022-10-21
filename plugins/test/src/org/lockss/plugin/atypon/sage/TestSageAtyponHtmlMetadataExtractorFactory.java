package org.lockss.plugin.atypon.sage;

import junit.framework.Test;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.test.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.util.ListUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.*;

import org.jsoup.Jsoup;



public class TestSageAtyponHtmlMetadataExtractorFactory extends LockssTestCase {

    ArchivalUnit mau;
    String tempDirPath;
    MockLockssDaemon daemon;
    PluginManager pluginMgr;

    private static final String PLUGIN_ID =
            "org.lockss.plugin.atypon.sage.ClockssSageAtyponJournalsPlugin";


    protected ArchivalUnit createAu()
            throws ArchivalUnit.ConfigurationException {
        return PluginTestUtil.createAndStartAu(PLUGIN_ID, thisAuConfig());
    }

    private Configuration thisAuConfig() {
        Configuration conf = ConfigManager.newConfiguration();
        conf.put("base_url", "http://www.example.com/");
        conf.put("journal_id", "abc");
        conf.put("volume_name", "99");
        return conf;
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

    String goodJournalTitle = "Finance Volume 59";
    String goodDCDate = "2012-12-03";

    String goodContent = "<HTML><HEAD><TITLE>" + "blabla"
            + "</TITLE></HEAD><BODY>\n"
            + "<meta name=\"citation_journal_title\" content=\"" + goodJournalTitle  + "\"></meta>"
            + "<meta name=\"dc.Date\" scheme=\"WTN8601\" content=\"" + goodDCDate  + "\"></meta>";

    MockArchivalUnit makeAu() throws ArchivalUnit.ConfigurationException {
        MockArchivalUnit mau = new MockArchivalUnit();
        Configuration config = ConfigurationUtil.fromArgs(
                "base_url", "http://www.xyz.com/");
        mau.setConfiguration(config);
        mau.setUrlStems(ListUtil.list(
                "http://www.xyz.com/"
        ));
        return mau;
    }

    public void testExtractFromGoodContent() throws Exception {
        String url = "http://www.sage.com/content/54/1/6";

        MockCachedUrl cu = new MockCachedUrl(url, mau);
        cu.setContentSize(goodContent.length());
        cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
        FileMetadataExtractor me = new SageAtyponHtmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        //log.info("--------goodDCDate" + md.get(MetadataField.FIELD_DATE));
        //assertEquals(goodDCDate, md.get(MetadataField.FIELD_DATE));
        //assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));


        //log.info("--------getAdditionalMetadata-------");

        /*
        String initialString = "<div class=\"core-enumeration\"><a href=\"/toc/choa/9/1\"><span property=\"isPartOf\" typeof=\"PublicationVolume\">Volume <span property=\"volumeNumber\">9</span></span>, <span property=\"isPartOf\" typeof=\"PublicationIssue\">Issue <span property=\"issueNumber\">1</span></span></a></div>\n";
        InputStream in = new ByteArrayInputStream(initialString.getBytes());
        try {
            getVolumeNumber(in, cu.getEncoding(), cu.getUrl());
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }

    protected void getVolumeNumber(InputStream in, String encoding, String url) {

        Elements span_element;

        Pattern VOLUME_PAT = Pattern.compile("volume\\s*(\\d+)\\s*(\\d+)\\s*issue\\s*(\\d+)\\s*\\d+", Pattern.CASE_INSENSITIVE);
        String VOLUME_REPL = "$1";

        try {
            Document doc = Jsoup.parse(in, encoding, url);

            span_element = doc.select("span[property]"); // <span property="volumeNumber">9</span>
            log.info("--------Get volume span-------");
            String raw_volume = null;
            String volume = null;
            if ( span_element != null){
                raw_volume = span_element.text().trim().toLowerCase(); // return "volume 9 9 issue 1 1"
                log.info("--------Get volume text-------" + raw_volume);
                Matcher plosM = VOLUME_PAT.matcher(raw_volume);
                if (plosM.matches()) {
                    volume = plosM.replaceFirst(VOLUME_REPL);
                    log.debug3("raw doi cleaned: = " + volume);
                }
                log.info("--------Get volume text-------" + volume);
                return;
            } else {
                log.info("--------Get volume span Failed-------" + volume);
            }
        } catch (IOException e) {
            log.info("Error getVolumeNumber", e);
            return;
        }
        return;
    }
}
