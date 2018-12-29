

package org.lockss.pdf.pdfbox;

import java.util.List;

import org.lockss.pdf.*;
import org.lockss.test.*;
import org.lockss.util.FileBackedList;

public class TestPdfBoxTokenStream extends LockssTestCase {

  public void testExcessiveTokenStream() throws Exception {
    try {
      ConfigurationUtil.addFromArgs(PdfBoxTokenStream.PARAM_FILE_BACKED_LISTS_THRESHOLD, "1000");
      PdfDocumentFactory fact = new PdfBoxDocumentFactory();
      PdfDocument doc = fact.makeDocument(getClass().getResourceAsStream("lorem.pdf"));
      PdfPage page = doc.getPage(0);
      PdfTokenStream strm = page.getPageTokenStream();
      List<PdfToken> tok = strm.getTokens();
      assertEquals(8011, tok.size()); // expected size of this manufactured token stream
      assertTrue(tok instanceof FileBackedList); // check that it exceeded the in-memory limit
    }
    finally {
      ConfigurationUtil.resetConfig();
    }
  }
  
}
