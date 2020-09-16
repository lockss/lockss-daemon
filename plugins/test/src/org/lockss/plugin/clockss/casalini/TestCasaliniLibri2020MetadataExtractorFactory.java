package org.lockss.plugin.clockss.casalini;

import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.converter.impl.AnselToUnicode;
import org.marc4j.marc.Record;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.CIProperties;
import org.lockss.util.Constants;
import org.lockss.util.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCasaliniLibri2020MetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestCasaliniLibriMarcXmlMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/casalini/";
    private static String Directory = "2019";
    private static String pdfUrl1 = BaseUrl + Directory + "/2000_4_2194804.pdf";
    private static String pdfUrl2 = BaseUrl + Directory + "/2002_4_2194812.pdf";

    private static String getXmlFileContent(String fname) {
        String xmlContent = "";
        try {
            String currentDirectory = System.getProperty("user.dir");
            String pathname = currentDirectory +
                    "/plugins/test/src/org/lockss/plugin/clockss/casalini/" + fname;
            xmlContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return xmlContent;
    }

    public void testGeneratedXmlFromMrcFormat() throws Exception {

        String fname = "Sample.mrc";

        String samplePath = "./plugins/test/src/org/lockss/plugin/clockss/casalini/" + fname;

        InputStream input = new FileInputStream(samplePath);
        OutputStream out = new FileOutputStream(new File("./plugins/test/src/org/lockss/plugin/clockss/casalini/generated.xml"));

        /*
        MarcReader reader = new MarcStreamReader(input);
        MarcWriter writer = new MarcXmlWriter(out, true);

        AnselToUnicode converter = new AnselToUnicode();
        writer.setConverter(converter);

        while (reader.hasNext()) {
            Record record = reader.next();
            writer.write(record);
        }
        writer.close();
         */
    }
}


