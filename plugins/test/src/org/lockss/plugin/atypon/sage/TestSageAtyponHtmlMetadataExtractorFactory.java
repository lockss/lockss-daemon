package org.lockss.plugin.atypon.sage;

import junit.framework.Test;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.PluginException;
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
import org.lockss.util.CIProperties;
import org.lockss.util.ListUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.*;

import org.jsoup.Jsoup;

import org.apache.commons.io.IOUtils;

public class TestSageAtyponHtmlMetadataExtractorFactory extends LockssTestCase {

    ArchivalUnit mau;
    String tempDirPath;
    MockLockssDaemon daemon;
    PluginManager pluginMgr;

    private static final String PLUGIN_ID =
            "org.lockss.plugin.atypon.sage.ClockssSageAtyponJournalsPlugin";

    private static String BASE_URL = "https://www.sample.com/";

    String goodJournalTitle = "Finance Volume 59";
    String goodDCDate = "2012-12-03";

    String goodHtmlContent = "<HTML><HEAD><TITLE>" + "blabla"
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

    public void setUp() throws Exception {
        super.setUp();
        tempDirPath = setUpDiskSpace();
        startMockDaemon();
        mau = createAu();
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

    protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
        return PluginTestUtil.createAndStartAu(PLUGIN_ID, thisAuConfig());
    }

    private Configuration thisAuConfig() {
        Configuration conf = ConfigManager.newConfiguration();
        conf.put("base_url", "http://www.example.com/");
        conf.put("journal_id", "abc");
        conf.put("volume_name", "99");
        return conf;
    }

    public void testExtractGoodHtmlContent() throws Exception {

        List<ArticleMetadata> mdlist = setupContentForAU(mau, BASE_URL, goodHtmlContent, true);
        assertNotEmpty(mdlist);
        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);
        assertEquals(goodDCDate, md.get(MetadataField.FIELD_DATE));
        assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));

        String initialString = "<div class=\"core-enumeration\"><a href=\"/toc/choa/9/1\"><span property=\"isPartOf\" typeof=\"PublicationVolume\">Volume <span property=\"volumeNumber\">9</span></span>, <span property=\"isPartOf\" typeof=\"PublicationIssue\">Issue <span property=\"issueNumber\">1</span></span></a></div>\n";
        InputStream in = new ByteArrayInputStream(initialString.getBytes());
        String volume = null;
        try {
            volume = getVolumeNumber(in, "utf-8", BASE_URL);
            in.close();
            assertEquals(volume, "9");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /* private support methods */
    private List<ArticleMetadata> setupContentForAU(ArchivalUnit au, String url,
                                                    String content,
                                                    boolean isHtmlExtractor) throws IOException, PluginException {
        FileMetadataExtractor me;

        InputStream input = null;
        CIProperties props = null;

        input = IOUtils.toInputStream(content, "utf-8");
        props = getContentHtmlProperties();
        me = new SageAtyponHtmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/html");

        UrlData ud = new UrlData(input, props, url);
        UrlCacher uc = au.makeUrlCacher(ud);
        uc.storeContent();
        CachedUrl cu = uc.getCachedUrl();
        FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
        return mle.extract(MetadataTarget.Any(), cu);
    }

    private CIProperties getContentHtmlProperties() {
        CIProperties cProps = new CIProperties();
        // the CU checks the X-Lockss-content-type, not the content-type to determine encoding
        cProps.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html; charset=UTF-8");
        cProps.put("Content-type",  "text/html; charset=UTF-8");
        return cProps;
    }

    protected String getVolumeNumber(InputStream in, String encoding, String url) {

        Elements span_element;

        Pattern VOLUME_PAT = Pattern.compile("volume\\s*(\\d+)\\s*(\\d+)\\s*issue\\s*(\\d+)\\s*\\d+", Pattern.CASE_INSENSITIVE);
        String VOLUME_REPL = "$1";

        try {
            Document doc = Jsoup.parse(in, encoding, url);

            span_element = doc.select("span[property=\"volumeNumber\"]"); // <span property="volumeNumber">9</span>
            //log.info("--------Get volume span-------");
            String raw_volume = null;
            String volume = null;
            if ( span_element != null){
                raw_volume = span_element.text().trim().toLowerCase(); // return "volume 9 9 issue 1 1"
                //log.info("--------Get volume text-------raw_volume=" + raw_volume);
                return raw_volume;

            }
        } catch (IOException e) {
            //log.info("Error getVolumeNumber", e);
            return null;
        }
        return null;
    }
}
