package org.lockss.plugin.clockss.eastview;

import org.lockss.plugin.clockss.mpidr.MPIDRRisMetadataExtractorFactory;
import org.lockss.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EastviewISSNMetadataHelper {

    private static final Logger log = Logger.getLogger(EastviewISSNMetadataHelper.class);

    private static Pattern LINE_PAT = Pattern.compile("(.*),(.*),(.*)?");
    private static Pattern DOI_PAT = Pattern.compile("10[.][0-9a-z]{4,6}/.*");


    public Map<String, String> getISSNList() throws IOException {

        int count = 0;
        String fname = "eastview_issn.dat";
        InputStream is = null;
        Map<String, String> issnMap = new HashMap<>();

        is = getClass().getResourceAsStream(fname);

        if (is == null) {
            throw new ExceptionInInitializerError("EastviewISSNMetadataHelper external data file not found");
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
                if (isDoi(m.group(3))) {
                    issn = m.group(3);

                    issnMap.put(acronym, issn);

                    log.debug3("VALID ISSN: publicationTitle ="
                            + publicationTitle + ", acronym = " + acronym + ", issn = " + issn);
                } else {

                    log.debug3("----------INVALID ISSN: publicationTitle ="
                            + publicationTitle + ", acronym = " + acronym + ", issn = " + issn);
                }

            } else {
                log.debug3("NO MATCH, nextline = " + nextline);
            }

        }
        bufr.close();

        return issnMap;
    }

    private boolean isDoi(String doi) {

        if (doi == null) {
            return false;
        }
        Matcher m = DOI_PAT.matcher(doi);

        if(!m.matches()){
            return false;
        }
        return true;
    }
}
