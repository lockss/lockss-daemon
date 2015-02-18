/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

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
