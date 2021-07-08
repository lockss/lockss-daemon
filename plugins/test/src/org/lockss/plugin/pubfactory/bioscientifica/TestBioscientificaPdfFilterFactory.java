package org.lockss.plugin.pubfactory.bioscientifica;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

public class TestBioscientificaPdfFilterFactory extends LockssTestCase {

  /*
   */

  public String getFileAsString(String fName) throws IOException, PluginException {
    FilterFactory BioPFF = new BioscientificaPdfFilterFactory();
    InputStream istr = BioPFF.createFilteredInputStream(
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
    String pdfStr = getFileAsString("ERP-18-0059-i06.pdf");
    String pdfStr2 = getFileAsString("ERP-18-0059-i07.pdf");
    assertEquals(pdfStr, pdfStr2);
  }
}