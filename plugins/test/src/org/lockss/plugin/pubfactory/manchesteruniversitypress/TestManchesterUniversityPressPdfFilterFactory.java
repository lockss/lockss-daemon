package org.lockss.plugin.pubfactory.manchesteruniversitypress;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

public class TestManchesterUniversityPressPdfFilterFactory extends LockssTestCase {

  /*
   */

  public String getFileAsString(String fName) throws IOException, PluginException {
    FilterFactory ManUPPFF = new ManchesterUniversityPressPdfFilterFactory();
    InputStream istr = ManUPPFF.createFilteredInputStream(
        null,
        getResourceAsStream(fName),
        null
    );
    return StringUtil.fromInputStream(istr);
  }

  public void testWatermarkRemoval() throws Exception {
    // the files have different timestaamps for access.
    // this article is open access
    // This test was successful for non-open access articles as well.
    String pdfStr = getFileAsString("jbr-5-1-article-p48.pdf");
    String pdfStr2 = getFileAsString("jbr-5-1-article-p48-02.pdf");
    assertEquals(pdfStr, pdfStr2);
  }
}