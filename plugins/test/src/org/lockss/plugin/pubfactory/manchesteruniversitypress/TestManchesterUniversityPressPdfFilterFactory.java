package org.lockss.plugin.pubfactory.manchesteruniversitypress;

import org.lockss.plugin.FilterFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.util.StringUtil;

import java.io.InputStream;

public class TestManchesterUniversityPressPdfFilterFactory extends LockssTestCase {

  /*
   */
  public void testWatermarkRemoval() throws Exception {
    // the files have different timestaamps for access.
    // this article is open access
    FilterFactory ManUPPFF = new ManchesterUniversityPressPdfFilterFactory();

    InputStream istr = ManUPPFF.createFilteredInputStream(
        null,
        getResourceAsStream("jbr-5-1-article-p48.pdf"),
        null
    );
    String pdfStr = StringUtil.fromInputStream(istr);

    InputStream istr2 = ManUPPFF.createFilteredInputStream(
        null,
        getResourceAsStream("jbr-5-1-article-p48-02.pdf"),
        null
    );
    String pdfStr2 = StringUtil.fromInputStream(istr2);

    assertEquals(pdfStr, pdfStr2);
  }
}