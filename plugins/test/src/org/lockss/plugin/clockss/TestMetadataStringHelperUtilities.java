package org.lockss.plugin.clockss;

import org.lockss.test.LockssTestCase;
import org.lockss.util.Logger;

import java.util.ArrayList;
import java.util.List;

public class TestMetadataStringHelperUtilities extends LockssTestCase {

    private static final Logger log = Logger.getLogger(TestMetadataStringHelperUtilities.class);

    public void testcleanupPubDate() throws Exception {

        List<String> testDates = new ArrayList<>();

        testDates.add("2001 (printed 2002)");
        testDates.add("[1981]");
        testDates.add("2005-2006.");
        testDates.add("2014-");
        testDates.add("aprile 2020.");
        testDates.add("c2001.");
        testDates.add("1998");
        testDates.add("1996.");
        testDates.add("MMXV.");
        testDates.add("[MMXVI]");

        for(String originalStr : testDates) {
            log.info("originalStr = " + originalStr);
            MetadataStringHelperUtilities.cleanupPubDate(originalStr);
        }
    }
}
