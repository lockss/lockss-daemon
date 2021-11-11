/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.clockss;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.util.Logger;

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
