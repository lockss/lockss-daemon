package org.lockss.plugin.ojs3;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestOJS3ArchivalUnit extends LockssTestCase {

    private MockArchivalUnit mau;

    private String PDF_PATTERN_STRING = "^(journals/)?(index\\.php/)?(%s/)?article/(view(File)?|download)/[^/]+/[^/?#&amp;.]+$";

    public void setUp() throws Exception {
        super.setUp();
        mau = new MockArchivalUnit();
    }

    public void testSubstancePattern() throws Exception {

        //  https://www.clei.org/cleiej/index.php/cleiej/article/download/437/372/1777
        //  https://scholarworks.iu.edu/journals/index.php/psource/issue/view/1257/14
        //  https://journals.library.ualberta.ca/jpps/index.php/JPPS/article/download/29659/21487/80185
        String pdfString1 = "cleiej/index.php/cleiej/article/download/437/372/1777";

        String pdfString2 = "journals/index.php/psource/issue/view/1257/14"; // this is a valid pdf

        // Article: https://journals.library.ualberta.ca/jpps/index.php/JPPS/issue/view/1955
        // PDF: https://journals.library.ualberta.ca/jpps/index.php/JPPS/article/view/30339/21503
        String pdfString3 = "jpps/index.php/JPPS/article/download/29659/21487/80185";

        // JOTT:
        // Article: https://threatenedtaxa.org/index.php/JoTT/article/view/1878
        // PDF: https://threatenedtaxa.org/index.php/JoTT/article/download/1878/6616

        // Good cases
        // http://trumpeter.athabascau.ca/index.php/trumpet/article/download/1508/1739
        // http://trumpeter.athabascau.ca/index.php/trumpet/article/view/1508/1739
        String pdfString4 = "index.php/trumpet/article/download/1508/1739";
        String pdfString5 = "index.php/trumpet/article/view/1508/1739";

        //testReplacement(pdfString1, "cleiej");
        //testReplacement(pdfString3, "jpps");
        //testReplacement(pdfString2, "psource");

        // These should success
        testReplacement(pdfString4, "trumpet");
        testReplacement(pdfString5, "trumpet");
    }

    public void testReplacement(String pdfUrl, String journalID) {

        String replacedPDFPattern = PDF_PATTERN_STRING.replaceAll("%s", journalID);

        //log.info("journalID = " + journalID + ", replacedPDFPattern = " + replacedPDFPattern + ", pdfUrl = " + pdfUrl);

        Pattern PDF_PATTERN = Pattern.compile(replacedPDFPattern, Pattern.CASE_INSENSITIVE);
        Matcher mat = PDF_PATTERN.matcher(pdfUrl);
        boolean patternMatched = mat.matches();

        assertTrue(patternMatched);
    }

}

