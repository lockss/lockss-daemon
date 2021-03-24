package org.lockss.plugin.clockss;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.LineIterator;
import org.apache.cxf.helpers.IOUtils;
import org.lockss.util.Logger;
import org.lockss.util.NumberUtil;

public class MetadataStringHelperUtilities {

    private static final Logger log = Logger.getLogger(MetadataStringHelperUtilities.class);

    /*
      The original date string need some cleanup
      date: 2001 (printed 2002)
      date: [1981]
      date: 2005-2006.
      date: 2014-
      date: aprile 2020.
      date: c2001.
      date: MMXV.
      date: [MMXVI]
   */
    public static String cleanupPubDate(String originalDateString) {

        String romaDate = null;

        String cleanedUpPubDate = originalDateString.
                trim().
                replace(".", "").
                replace(",", "").
                replace(";", "").
                replace(":", "")
                .replace("[", "")
                .replace("]", "")
                .replace("(", "")
                .replace(")", "")
                .trim();

        //log.debug3(String.format("Cleanup Date: originalDateString %s | cleanedUpPubDate: %s ",originalDateString, cleanedUpPubDate));

        String DiginalDatepattern = "([^\\d]+)?(\\d{4}).*(\\d{4})?.*";
        String RomaDatepattern = "^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$";

        // Create a Pattern object
        Pattern r = Pattern.compile(DiginalDatepattern);

        // Now create matcher object.
        Matcher m = r.matcher(cleanedUpPubDate);
        String localUrl = "";
        if (m.find()) {
            //log.debug3("Found value: " + m.group(0));
            //log.debug3("Found value: " + m.group(2));

            cleanedUpPubDate = m.group(2);

            //log.debug3(String.format("Cleanup Date: originalDateString %s | cleanedUpPubDate: %s ",originalDateString, cleanedUpPubDate));

        } else {
            //log.debug3("NO MATCH");
            // handle Null case
            if (originalDateString == null) {
                //log.debug3("Null Date");
            }

            // handle roman date
            Pattern roma = Pattern.compile(RomaDatepattern);

            // Now create matcher object.
            Matcher romaMatch = roma.matcher(cleanedUpPubDate);

            //log.debug3(String.format("Cleanup Date: originalDateString %s | cleanedUpPubDate: %s ",originalDateString, cleanedUpPubDate));

            if (romaMatch.matches()) {

                //log.debug3(String.format("Matches Roma Date Pattern: originalDateString %s | cleanedUpPubDate: %s ",originalDateString, cleanedUpPubDate));

                romaDate = String.valueOf(org.lockss.util.NumberUtil.parseRomanNumber(cleanedUpPubDate));
                //log.debug3(String.format("Cleanup Date: originalDateString %s | cleanedUpPubDate: %s | romaDate: %s",originalDateString, cleanedUpPubDate, romaDate));

                cleanedUpPubDate = romaDate;
                
            }
        }

        return cleanedUpPubDate;
    }
    
    public static String cleanupPublisherName(String originalString) {
      Pattern badCharactersPat = Pattern.compile("[^-\\p{Alnum} .&']+", Pattern.UNICODE_CHARACTER_CLASS);
      String cleanupPublisherName = originalString.replaceAll("\\[\\d{4}\\]", "");
      cleanupPublisherName = badCharactersPat.matcher(cleanupPublisherName).replaceAll("");
      cleanupPublisherName = cleanupPublisherName.trim();
      cleanupPublisherName = cleanupPublisherName.replaceAll(" +", " ");
      log.debug2(String.format("Casalini-Metadata: Cleanup Publisher Name: originalString: %s, cleanupPublisherName: %s ", originalString, cleanupPublisherName));
      return cleanupPublisherName;
    }
}
