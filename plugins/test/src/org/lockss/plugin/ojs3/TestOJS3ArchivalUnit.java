package org.lockss.plugin.ojs3;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestOJS3ArchivalUnit extends LockssTestCase {

    private MockArchivalUnit mau;

    private String PDF_PATTERN_STRING = "^(%s/)?(article/)?(issue/)?(view(File)?|download)/[^/]+/([^/]+/)?[^/?#&amp;.]+$";

    public void setUp() throws Exception {
        super.setUp();
        mau = new MockArchivalUnit();
    }

    public void testSubstancePattern() throws Exception {

        //  https://www.clei.org/cleiej/index.php/cleiej/article/download/437/372/1777
        //  https://scholarworks.iu.edu/journals/index.php/psource/issue/view/1257/14
        //  https://journals.library.ualberta.ca/jpps/index.php/JPPS/article/download/29659/21487/80185
        String pdfString1 = "cleiej/article/download/437/372/1777";

        String pdfString2 = "psource/issue/view/1257/14"; // this is a valid pdf

        // Article: https://journals.library.ualberta.ca/jpps/index.php/JPPS/issue/view/1955
        // PDF: https://journals.library.ualberta.ca/jpps/index.php/JPPS/article/view/30339/21503
        String pdfString3 = "JPPS/article/download/29659/21487/80185";

        // JOTT:
        // Article: https://threatenedtaxa.org/index.php/JoTT/article/view/1878
        // PDF: https://threatenedtaxa.org/index.php/JoTT/article/download/1878/6616

        // Good cases
        // http://trumpeter.athabascau.ca/index.php/trumpet/article/download/1508/1739
        // http://trumpeter.athabascau.ca/index.php/trumpet/article/view/1508/1739
        String pdfString4 = "trumpet/article/download/1508/1739";
        String pdfString5 = "trumpet/article/view/1508/1739";

        testReplacement(pdfString1, "cleiej");
        testReplacement(pdfString3, "jpps");
        testReplacement(pdfString2, "psource");

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

    public void testAddStartStem() {

        String baseUrl = "https://journals.vgtu.lt/";
        String journal_id = "BME";
        String start_stem = "index.php/";
        String url =  "https://journals.vgtu.lt/BME/gateway/lockss?year=2012";

        String expected = getStartUrl(baseUrl, url, start_stem);

        assertEquals("https://journals.vgtu.lt/index.php/BME/gateway/lockss?year=2012", expected);
    }
    
    public void testAddStartStem2() {

        String baseUrl = "https://journals.vgtu.lt/";
        String journal_id = "BME";
        String start_stem = "index.php/";
        String url =  "http://journals.vgtu.lt/BME/gateway/lockss?year=2012";

        String expected = getStartUrl(baseUrl, url, start_stem);

        assertEquals("https://journals.vgtu.lt/index.php/BME/gateway/lockss?year=2012", expected);
    }

    public void testAddStartStem3() {

        String baseUrl = "http://journals.vgtu.lt/";
        String journal_id = "BME";
        String start_stem = "index.php/";
        String url =  "http://journals.vgtu.lt/BME/gateway/lockss?year=2012";

        String expected = getStartUrl(baseUrl, url, start_stem);

        assertEquals("http://journals.vgtu.lt/index.php/BME/gateway/lockss?year=2012", expected);
    }

    public void testAddStartStem4() {

        String baseUrl = "http://journals.vgtu.lt/";
        String journal_id = "BME";
        String start_stem = "index.php/";
        String url =  "https://journals.vgtu.lt/BME/gateway/lockss?year=2012";

        String expected = getStartUrl(baseUrl, url, start_stem);

        assertEquals("http://journals.vgtu.lt/index.php/BME/gateway/lockss?year=2012", expected);
    }

    public void testAddStartStem5() {

        String baseUrl = "https://scholarworks.iu.edu/";
        String journal_id = "iusbanalecta";
        String start_stem = "journals/index.php/";
        String url =  "https://scholarworks.iu.edu/iusbanalecta/gateway/lockss?year=2012";

        String expected = getStartUrl(baseUrl, url, start_stem);

        assertEquals("https://scholarworks.iu.edu/journals/index.php/iusbanalecta/gateway/lockss?year=2012", expected);
    }

    public String getProtocal(String url) {
        String urlProtocal = "";

        if (url.startsWith("https://")) {
            urlProtocal = "https://";
        }  else if (url.startsWith("http://")) {
            urlProtocal = "http://";
        }

        return urlProtocal;
    }
    
    public String getStartUrl(String baseUrl, String url, String start_stem) {

        String urlWithoutProtocal = url.replace(getProtocal(url), "");
        String baseurlWithoutProtocal =  baseUrl.replace(getProtocal(baseUrl), "");

        StringBuilder sb = new StringBuilder(baseUrl);

        //log.info("OJS3: ------------url = " + url + ", urlWithoutProtocal = " + urlWithoutProtocal + ", base_url = "
               // + baseUrl + ", baseurlWithoutProtocal = " + baseurlWithoutProtocal);

        if (urlWithoutProtocal.startsWith(baseurlWithoutProtocal)) {
            //log.info("OJS3: sb = " + sb.toString());
            sb.append(start_stem);
            //log.info("OJS3: sb append = " + sb.toString());
            //log.info("OJS3: url substring = " + urlWithoutProtocal.substring(baseurlWithoutProtocal.length()));
            //log.info("OJS3: url substring replace= " + urlWithoutProtocal.substring(baseurlWithoutProtocal.length()).replace(start_stem, ""));
            sb.append(urlWithoutProtocal.substring(baseurlWithoutProtocal.length()).replace(start_stem, ""));
            //log.info("OJS3: adding = " + urlWithoutProtocal.substring(baseurlWithoutProtocal.length()).replace(start_stem, ""));

            //log.info("OJS3: =========final sb = " + sb.toString());
        }

        return sb.toString();
    }
}

