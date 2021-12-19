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

package uk.org.lockss.plugin.annualreviews;

import java.util.Arrays;

import org.lockss.pdf.*;
import org.lockss.test.LockssTestCase;

import uk.org.lockss.plugin.annualreviews.AnnualReviewsPdfFilterFactory.DownloadedFromWorker;
import uk.org.lockss.plugin.annualreviews.AnnualReviewsPdfFilterFactory.ForPersonalUseWorker;

public class TestAnnualReviewsPdfFilterFactory extends LockssTestCase {

  protected PdfTokenFactory tf;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.tf = new MockPdfTokenFactory();
  }
  
  public void testDownloadedFromWorker() throws Exception {
    DownloadedFromWorker worker = new DownloadedFromWorker();
    
    for (String goodString : Arrays.asList("Downloaded from http://www.example.com/",
                                           "Downloaded from http://www.example.com",
                                           "Downloaded from www.example.com")) {
      worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"BT " +
"(This is irrelevant.) Tj " +
"1 2 3 4 5 6 Tm " +
"(This is irrelevant.) Tj " +
"(" + goodString + ") Tj " +
"(This is irrelevant.) Tj " +
"ET"
// ---- end PDF stream ----
      ));
      assertTrue(worker.result);
    }
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"BT " +
"(This is irrelevant.) Tj " +
"1 2 3 4 5 6 Tm " +
"(This is irrelevant.) Tj " +
"(Downloaded from www.example.com) Tj " +
"(This is irrelevant.) Tj " +
"ET " +
"(This is irrelevant.) Tj"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"(This is irrelevant.) Tj" +
"BT " +
"(This is irrelevant.) Tj " +
"1 2 3 4 5 6 Tm " +
"(This is irrelevant.) Tj " +
"(Downloaded from www.example.com) Tj " +
"(This is irrelevant.) Tj " +
"ET "
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"(This is irrelevant.) Tj" +
"BT " +
"(This is irrelevant.) Tj " +
"1 2 3 4 5 6 Tm " +
"(This is irrelevant.) Tj " +
"(Downloaded from www.example.com) Tj " +
"(This is irrelevant.) Tj " +
"ET "
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
   
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"BT " +
"(This is irrelevant.) Tj " +
"1 2 3 4 5 6 Tm " +
"(This is irrelevant.) Tj " +
"(Not the right string.) Tj " +
"(This is irrelevant.) Tj " +
"ET "
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"(This is irrelevant.) Tj " +
"1 2 3 4 5 6 Tm " +
"(This is irrelevant.) Tj " +
"(Downloaded from www.example.com) Tj " +
"(This is irrelevant.) Tj "
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
  }

  public void testForPersonalUseWorker() throws Exception {
    ForPersonalUseWorker worker = new ForPersonalUseWorker();
    
    for (String goodString : Arrays.asList("by <something something> on 01/02/03. For personal use only.",
                                           "by <something something> on 99/99/99. For personal use only.",
                                           "   \t\t\t   by <something something> on 01/03/03. For personal use only.   \t\t\t")) {
      worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"BT " +
"(This is irrelevant.) Tj " +
"1 2 3 4 5 6 Tm " +
"(This is irrelevant.) Tj " +
"(" + goodString + ") Tj " +
"(This is irrelevant.) Tj " +
"ET"
// ---- end PDF stream ----
      ));
      assertTrue(worker.result);
    }
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"BT " +
"(This is irrelevant.) Tj " +
"1 2 3 4 5 6 Tm " +
"(This is irrelevant.) Tj " +
"(by <something something> on 01/02/03. For personal use only.) Tj " +
"(This is irrelevant.) Tj " +
"ET" +
"(This is irrelevant.) Tj "
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"(This is irrelevant.) Tj " +
"BT " +
"(This is irrelevant.) Tj " +
"1 2 3 4 5 6 Tm " +
"(This is irrelevant.) Tj " +
"(by <something something> on 01/02/03. For personal use only.) Tj " +
"(This is irrelevant.) Tj " +
"ET"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"(This is irrelevant.) Tj " +
"BT " +
"(This is irrelevant.) Tj " +
"1 2 3 4 5 6 Tm " +
"(This is irrelevant.) Tj " +
"(by <not the right date format> on 01/02/2003. For personal use only.) Tj " +
"(This is irrelevant.) Tj " +
"ET"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"BT " +
"(This is irrelevant.) Tj " +
"(This is irrelevant.) Tj " +
"(by <something something> on 01/02/03. For personal use only.) Tj " +
"(This is irrelevant.) Tj " +
"ET"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
  }
  
}
