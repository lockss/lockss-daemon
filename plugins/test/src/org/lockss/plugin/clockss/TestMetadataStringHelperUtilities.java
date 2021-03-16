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


        for(String originalStr : testData) {
            log.info("------------originalStr = " + originalStr);
            MetadataStringHelperUtilities.cleanupPublisherName(originalStr);
        }
    }
}
