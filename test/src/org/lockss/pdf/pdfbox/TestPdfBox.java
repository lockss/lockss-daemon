/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.pdf.pdfbox;

import java.util.List;

import org.lockss.pdf.*;
import org.lockss.test.*;
import org.lockss.util.FileBackedList;
import org.lockss.util.Logger;
import org.lockss.util.TimeBase;

public class TestPdfBox extends LockssTestCase {
  private static Logger log = Logger.getLogger(TestPdfBox.class);

  public void testExcessiveTokenStream() throws Exception {
    List<PdfToken> tok = null;
    try {
      ConfigurationUtil.addFromArgs(PdfBoxTokenStream.PARAM_FILE_BACKED_LISTS_THRESHOLD, "1000");
      PdfDocumentFactory fact = PdfBoxDocumentFactory.SINGLETON;
      PdfDocument doc = fact.makeDocument(getClass().getResourceAsStream("lorem.pdf"));
      PdfPage page = doc.getPage(0);
      PdfTokenStream strm = page.getPageTokenStream();
      tok = strm.getTokens();
      assertEquals(8011, tok.size()); // expected size of this manufactured token stream
      assertTrue(tok instanceof FileBackedList); // check that it exceeded the in-memory limit
      doc.close();
    } finally {
      if (tok instanceof FileBackedList) {
	try {
	  ((FileBackedList)tok).close();
	} catch (Exception e) {
	  log.warning("Couldn't close FileBackedList", e);
	}
      }
      ConfigurationUtil.resetConfig();
    }
  }

  public void testCacheFlush() throws Exception {
    TimeBase.setSimulated(1);
    ConfigurationUtil.addFromArgs(PdfBoxDocumentFactory.PARAM_CACHE_FLUSH_INTERVAL, "1000");
    PdfBoxDocumentFactory fact = PdfBoxDocumentFactory.SINGLETON;
    PdfDocument doc = fact.makeDocument(getClass().getResourceAsStream("lorem.pdf"));
    assertFalse(fact.okToFlushCaches());
    TimeBase.step(1000);
    assertFalse(fact.okToFlushCaches());
    doc.close();
    assertEquals(1, fact.getFlushCtr());
    assertFalse(fact.okToFlushCaches());

    doc = fact.makeDocument(getClass().getResourceAsStream("lorem.pdf"));
    assertEquals(1, fact.getFlushCtr());
    assertFalse(fact.okToFlushCaches());
    doc.close();
    assertEquals(1, fact.getFlushCtr());
    assertFalse(fact.okToFlushCaches());
    TimeBase.step(1000);
    assertEquals(2, fact.getFlushCtr());
    assertFalse(fact.okToFlushCaches());

    doc = fact.makeDocument(getClass().getResourceAsStream("lorem.pdf"));
    PdfDocument doc2 = fact.makeDocument(getClass().getResourceAsStream("lorem.pdf"));
    assertEquals(2, fact.getFlushCtr());
    doc.close();
    assertEquals(2, fact.getFlushCtr());
    TimeBase.step(1000);
    assertEquals(2, fact.getFlushCtr());
    doc2.close();
    assertEquals(3, fact.getFlushCtr());
  }

}
