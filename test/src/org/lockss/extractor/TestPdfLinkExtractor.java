/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.extractor;

import java.util.*;

import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.test.LockssTestCase;

/**
 * 
 * @author Thib Guicherd-Callin
 * @since 1.76
 * @see PdfLinkExtractor
 */
public class TestPdfLinkExtractor extends LockssTestCase {

  /**
   * <p>
   * Uses a sample PDF (generated from LibreOffice), with two pages having one
   * link each. 
   * </p>
   * 
   * @throws Exception
   * @since 1.76
   */
  public void testSamplePdf() throws Exception {
    List<String> collected = new ArrayList<>();
    Callback myCallback = new Callback() {
      @Override
      public void foundLink(String url) {
        collected.add(url);
      }
    };

    PdfLinkExtractor pdfLinkExtractor = new PdfLinkExtractor();
    pdfLinkExtractor.extractUrls(null,
                                 getClass().getResourceAsStream("pdflinkextractor1.pdf"),
                                 null,
                                 null,
                                 myCallback);
    
    assertEquals(2, collected.size());
    assertEquals("https://www.lockss.org/", collected.get(0));
    assertEquals("https://github.com/lockss", collected.get(1));
  }
  
}
