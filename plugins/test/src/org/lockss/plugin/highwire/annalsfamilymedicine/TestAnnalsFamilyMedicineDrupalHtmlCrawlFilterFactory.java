package org.lockss.plugin.highwire.annalsfamilymedicine;

import org.lockss.plugin.FilterFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.InputStream;

public class TestAnnalsFamilyMedicineDrupalHtmlCrawlFilterFactory extends LockssTestCase {
    private static FilterFactory fact;
    private static MockArchivalUnit mau;

    public void setUp() throws Exception {
        super.setUp();
        fact = new AnnalsFamilyMedicineDrupalHtmlCrawlFilterFactory();
        mau = new MockArchivalUnit();
    }

    private static final String alsoReadHtml =
            "<html><header>head content</header><body><div id=\"should_include\">should included content</div><div id=\"mini-panel-jnl_annalsfm_art_tools\">mini panenl content</div></body><footer>footer content</footer></html>";
    private static final String alsoReadHtmlFiltered =
            "<html><body><div id=\"should_include\">should included content</div></body></html>";
    
    //Variant to test with Crawl Filter
    public static class TestCrawl extends TestAnnalsFamilyMedicineDrupalHtmlCrawlFilterFactory {

        public void setUp() throws Exception {
            super.setUp();
            fact = new AnnalsFamilyMedicineDrupalHtmlCrawlFilterFactory();
        }

    }

    public void testAlsoRead() throws Exception {
        InputStream actIn1 = fact.createFilteredInputStream(mau,
                new StringInputStream(alsoReadHtml), Constants.DEFAULT_ENCODING);

        assertEquals(alsoReadHtmlFiltered, StringUtil.fromInputStream(actIn1));
    }
}

