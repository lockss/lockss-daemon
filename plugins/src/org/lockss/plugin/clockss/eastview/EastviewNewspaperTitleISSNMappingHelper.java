package org.lockss.plugin.clockss.eastview;

import org.lockss.util.Logger;
import org.lockss.util.MetadataUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EastviewNewspaperTitleISSNMappingHelper {

    private static final Logger log = Logger.getLogger(EastviewNewspaperTitleISSNMappingHelper.class);

    private static Pattern LINE_PAT = Pattern.compile("(.*),(.*),(.*)?");
    public static Map<String, String> issnMap = new HashMap<>();
    public static Map<String, String> titleMap = new HashMap<>();

    public Map<String, String> getISSNList() throws IOException {

        int count = 0;
        String fname = "eastview_newspaper_issn.dat";
        InputStream is = null;

        is = getClass().getResourceAsStream(fname);

        if (is == null) {
            throw new ExceptionInInitializerError("Eastview Newspaper: EastviewISSNMetadataHelper external data file not found");
        }

        BufferedReader bufr = new BufferedReader(new InputStreamReader(is));

        String nextline = null;

        while ((nextline = bufr.readLine()) != null) {
            nextline = nextline.trim();

            // Now create matcher object.
            Matcher m = LINE_PAT.matcher(nextline);
            String publicationTitle = "";
            String acronym = "";
            String issn = "";

            if (m.find()) {
                log.debug3("Found value: " + m.group(0));
                log.debug3("Found value: " + m.group(1));
                log.debug3("Found value: " + m.group(2));
                log.debug3("Found value: " + m.group(3));

                publicationTitle = m.group(1);
                acronym = m.group(2).replace(" ", "").replace("\"", "");

                MetadataUtil.validateIssn(m.group(3));
                if (issn != null) {
                    issn = m.group(3);

                    issnMap.put(publicationTitle, issn);
                    titleMap.put(acronym, publicationTitle);

                    log.debug3("Eastview Newspaper: VALID ISSN: publicationTitle ="
                            + publicationTitle + ", acronym = " + acronym + ", issn = " + issn);
                } else {

                    log.debug3("----------Eastview Newspaper: INVALID ISSN: publicationTitle ="
                            + publicationTitle + ", acronym = " + acronym + ", issn = " + issn);
                }

            } else {
                log.debug3("Eastview Newspaper: NO MATCH, nextline = " + nextline);
            }

        }
        bufr.close();

        return issnMap;
    }
}
