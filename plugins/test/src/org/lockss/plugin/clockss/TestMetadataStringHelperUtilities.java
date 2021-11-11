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

import org.lockss.test.LockssTestCase;
import org.lockss.util.Logger;

import java.util.ArrayList;
import java.util.List;

public class TestMetadataStringHelperUtilities extends LockssTestCase {

    private static final Logger log = Logger.getLogger(TestMetadataStringHelperUtilities.class);

    public void testcleanupPubDate() throws Exception {

        List<String> testData = new ArrayList<>();

        testData.add("2001 (printed 2002)");
        testData.add("[1981]");
        testData.add("2005-2006.");
        testData.add("2014-");
        testData.add("aprile 2020.");
        testData.add("c2001.");
        testData.add("1998");
        testData.add("1996.");
        testData.add("MMXV.");
        testData.add("[MMXVI]");

        for(String originalStr : testData) {
            //log.info("originalStr = " + originalStr);
            MetadataStringHelperUtilities.cleanupPubDate(originalStr);
        }
    }

    public void testcleanupPublisherName() throws Exception {

        List<String> testData = new ArrayList<>();

        testData.add("Antenore :");
        testData.add("Anthropos  ;");
        testData.add("G. Giappichelli Editore");
        testData.add("XY.IT");
        testData.add("[s.n.]");
        testData.add("Di che cibo 6?");
        testData.add("G. Giappichelli");
        testData.add("G. Giappichelli Editore");
        testData.add("Agorà & Co.");
        testData.add("CPL - Centro Primo Levi");
        
        testData.add("CLUEB  ;");
        testData.add("CLUEB :");
        testData.add("Jaca book  ;");
        testData.add("Petite plaisance  ;");
        testData.add("Regione Emilia-Romagna :");
        
        for(String originalStr : testData) {
            //log.info("------------originalStr = " + originalStr);
            MetadataStringHelperUtilities.cleanupPublisherName(originalStr);
        }
    }

    /*
    This function is needed during the debugging process, not for real test purpose, but still worth keep it for later use
    public void testcleanupPublisherNameFromFile() throws Exception {
        String fileStr = "/tmp/pubs.txt";
        LineIterator iter = new LineIterator(new InputStreamReader(new FileInputStream(fileStr), StandardCharsets.UTF_8));
        PrintWriter out = new PrintWriter(fileStr + ".out", "UTF-8");

        Map<String,String> foundMap2016 = new HashMap<>();
        Map<String,String> foundMap = new HashMap<>();
        Map<String,String> missMap =  new HashMap<>();

        while (iter.hasNext()) {
            String line = iter.next();
            String cleanupName = MetadataStringHelperUtilities.cleanupPublisherName(line);
            out.format("%s -> %s%n", line, cleanupName);

            String publisherCleanName = cleanupKey(cleanupName);

            String publisherShortCut2016 = matchPublisherName2016(publisherCleanName.toLowerCase());
            String publisherShortCut = matchPublisherName(publisherCleanName.toLowerCase());


            if (!publisherNameShortcutMap.containsKey(publisherCleanName) && !publisherNameShortcutMap2016.containsKey(publisherCleanName)) {
                log.info(String.format("NOT IN ANY: %s -> %s%n", line, cleanupName));
            }

            // One key found in two places
            if (publisherShortCut2016 != null && publisherShortCut != null && !publisherShortCut2016.equalsIgnoreCase(publisherShortCut)) {
                log.info(String.format("2016 vs 2019 vs 2020: %s -> %s -> 2016:%s -> %s", line, cleanupName, publisherShortCut2016, publisherShortCut));
            }

            if (publisherShortCut2016 != null) {
                //log.info(String.format("%s -> %s -> %s", line, cleanupName, publisherShortCut));
                foundMap2016.put(publisherCleanName.toLowerCase(), publisherShortCut2016);
            } else {
                if (publisherShortCut != null){
                    foundMap.put(publisherCleanName.toLowerCase(), publisherShortCut);
                }

                if (publisherShortCut == null) {
                    //log.info(String.format("%s -> %s -> NO SHORTCUT", line, cleanupName));
                    missMap.put(publisherCleanName.toLowerCase(), line);
                }
            }

        }
        out.close();

        //log.info("===========foundMap2016==========, total = " + foundMap2016.size());
        for (String keys : foundMap2016.keySet())
        {
            //log.info(String.format("%s -> %s", keys, foundMap2016.get(keys)));
        }

        //log.info("===========foundMap==========, total = " + foundMap.size());
        for (String keys : foundMap.keySet())
        {
            //log.info(String.format("%s -> %s", keys, foundMap.get(keys)));
        }
        
        //log.info("===========missMap==========, total = " + missMap.size());
        for (String keys : missMap.keySet())
        {
            
            //log.info(String.format("----------%s ->%s", keys, missMap.get(keys)));
        }
    }
    */
}
