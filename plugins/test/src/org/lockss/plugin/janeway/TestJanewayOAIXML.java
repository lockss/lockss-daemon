package org.lockss.plugin.janeway;

import com.lyncode.xml.exceptions.XmlReaderException;
import org.dspace.xoai.serviceprovider.parsers.RecordParser;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.plugin.silverchair.ScRisMetadataExtractorFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.dspace.xoai.serviceprovider.model.Context;
import java.io.InputStream;
import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.serviceprovider.model.Context.KnownTransformer;
import com.lyncode.xml.XmlReader;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;

public class TestJanewayOAIXML extends LockssTestCase {

    static Logger log = Logger.getLogger(TestJanewayOAIXML.class);

    private ScRisMetadataExtractorFactory extfact;
    private MockArchivalUnit mau;
    private MockLockssDaemon theDaemon;

    private static String PLUGIN_NAME = "org.lockss.plugin.janeway.JanewayPathModeJournalsPlugin";
    private static String BASE_URL = "http://www.source.org/";

    private Context context;
    private RecordParser parser;

    public void setUp() throws Exception {
        super.setUp();
        setUpDiskSpace(); // you need this to have startService work properly...

        theDaemon = getMockLockssDaemon();
        mau = new MockArchivalUnit();

        theDaemon.getAlertManager();
        theDaemon.getPluginManager().setLoadablePluginsReady(true);
        theDaemon.setDaemonInited(true);
        theDaemon.getPluginManager().startService();
        theDaemon.getCrawlManager();
        mau.setConfiguration(auConfig());
        extfact = new ScRisMetadataExtractorFactory();
    }

    Configuration auConfig() {
        Configuration conf = ConfigManager.newConfiguration();
        conf.put("base_url", BASE_URL);
        conf.put("year", "2012");
        conf.put("journal_id", "vmd");
        return conf;
    }

    public void tearDown() throws Exception {
        theDaemon.stopDaemon();
        super.tearDown();
    }

    public void test2017XmlValidation() {

        context = new Context().withMetadataTransformer("oai_dc",
                KnownTransformer.OAI_DC);

        InputStream input2017 = getResourceAsStream("vmd_2017_api_original.xml");

        if (input2017 != null) {
            parser = new RecordParser(context, "oai_dc");
            XmlReader reader = null;
            Record record = null;
            try {
                reader = new XmlReader(input2017);
                record = parser.parse(reader);
            } catch (XmlReaderException e) {
                throw new RuntimeException(e);
            }
            log.info(record.getMetadata().getValue().searcher().findOne("dc.title"));
        } else {
            log.info("Resource not found");
        }
    }

    public void test2015XmlValidation() {

        context = new Context().withMetadataTransformer("oai_dc",
                KnownTransformer.OAI_DC);

        InputStream input2017 = getResourceAsStream("vmd_2015_api_original.xml");

        if (input2017 != null) {
            parser = new RecordParser(context, "oai_dc");
            XmlReader reader = null;
            Record record = null;
            try {
                reader = new XmlReader(input2017);
                record = parser.parse(reader);
            } catch (XmlReaderException e) {
                throw new RuntimeException(e);
            }
            log.info(record.getMetadata().getValue().searcher().findOne("dc.title"));
        } else {
            log.info("Resource not found");
        }
    }

    //This is to test the bad xml on purpose, the test will fail on purpose
    /*
    public void test2016XmlValidation() {

        context = new Context().withMetadataTransformer("oai_dc",
                KnownTransformer.OAI_DC);

        InputStream input2017 = getResourceAsStream("vmd_2016_api_original_bad_example.xml");

        if (input2017 != null) {
            parser = new RecordParser(context, "oai_dc");
            XmlReader reader = null;
            Record record = null;
            try {
                reader = new XmlReader(input2017);
                record = parser.parse(reader);
            } catch (XmlReaderException e) {
                throw new RuntimeException(e);
            }
            log.info(record.getMetadata().getValue().searcher().findOne("dc.title"));
        } else {
            log.info("Resource not found");
        }
    }

     */

}
