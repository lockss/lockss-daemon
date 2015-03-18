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

package org.lockss.plugin.projmuse;

import org.lockss.pdf.*;
import org.lockss.plugin.projmuse.ProjectMusePdfFilterFactory.FrontPageWorker;
import org.lockss.test.LockssTestCase;

public class TestProjectMusePdfFilterFactory extends LockssTestCase {

  /*
   * Examples:
   * http://muse.jhu.edu/journals/perspectives_on_science/v022/22.4.oberdan.pdf 12/01/14
   */
  public void testFrontPageWorker() throws Exception {
    PdfTokenStreamStateMachine worker = new FrontPageWorker();
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q 375.12 0 0 174.96 5 607 cm /JxCBB Do Q 0.674510 0.658824 0.619608 RG 15 592 m 597 592 l S 0.674510 0.658824 0.619608 RG 15 75 m 597 75 l S " + 
"BT  /DeSaBoCBC~1416394845 16 Tf 20 TL 1 0 0 1 30 567 Tm (<something something something>) Tj T* (<something something something>) Tj T* /DeSaCBD~1416394845 10 Tf 1 0 0 1 30 517 Tm (<something something something>) Tj T* /HelvCBE~1416394845 12 Tf 16 TL 1 0 0 1 30 472 Tm [ (Journal Title, Volume 123, Number 4, Winter 2014, pp. 12-34) ] TJ T* [ (\\(Article\\)) ] TJ T* /DeSaCBF~1416394845 12 Tf 15 TL 1 0 0 1 30 418 Tm (<something something something>) Tj T* /HelvCBH~1416394845 12 Tf 1 0 0 1 50 268 Tm [ (For additional information about this article) ] TJ /HelvCBK~1416394845 10 Tf 0.674510 0.658824 0.619608 rg 1 0 0 1 15 37.500 Tm [ (                                                      Access provided by <something> (1 Dec 2014 14:32 GMT)) ] TJ  ET " + 
"q 7 0 0 7 35 266.50 cm /GxCBG Do Q " +
"BT  /HelvCBI~1416394845 9 Tf 0.325490 0.427451 0.494118 rg 1 0 0 1 50 253 Tm [ (http://www.example.com/journals/jabc/summary/v123/123.4.author.html) ] TJ  ET " + 
"0.662745 0.662745 0.662745 rg 478 426 86.400 124.56 re f q 86.400 0 0 124.56 477 427 cm /JxCBJ Do Q"
// ---- end PDF stream ----
    ));
    assertTrue(worker.getResult());
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q 375.12 0 0 174.96 5 607 cm /JxCBB Do Q 0.674510 0.658824 0.619608 RG 15 592 m 597 592 l S 0.674510 0.658824 0.619608 RG 15 75 m 597 75 l S " + 
"BT  /DeSaBoCBC~1416394845 16 Tf 20 TL 1 0 0 1 30 567 Tm (<something something something>) Tj T* (<something something something>) Tj T* /DeSaCBD~1416394845 10 Tf 1 0 0 1 30 517 Tm (<something something something>) Tj T* /HelvCBE~1416394845 12 Tf 16 TL 1 0 0 1 30 472 Tm [ (Journal Title, Volume 123, Number 4, Winter 2014, pp. 12-34) ] TJ T* [ (\\(Article\\)) ] TJ T* /DeSaCBF~1416394845 12 Tf 15 TL 1 0 0 1 30 418 Tm (<something something something>) Tj T* /HelvCBH~1416394845 12 Tf 1 0 0 1 50 268 Tm (For additional information about this article) Tj /HelvCBK~1416394845 10 Tf 0.674510 0.658824 0.619608 rg 1 0 0 1 15 37.500 Tm (                                                      Access provided by <something> (1 Dec 2014 14:32 GMT)) Tj  ET " + 
"q 7 0 0 7 35 266.50 cm /GxCBG Do Q " +
"BT  /HelvCBI~1416394845 9 Tf 0.325490 0.427451 0.494118 rg 1 0 0 1 50 253 Tm [ (http://www.example.com/journals/jabc/summary/v123/123.4.author.html) ] TJ  ET " + 
"0.662745 0.662745 0.662745 rg 478 426 86.400 124.56 re f q 86.400 0 0 124.56 477 427 cm /JxCBJ Do Q"
// ---- end PDF stream ----
    ));
    assertTrue(worker.getResult());
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q 375.12 0 0 174.96 5 607 cm /JxCBB Do Q 0.674510 0.658824 0.619608 RG 15 592 m 597 592 l S 0.674510 0.658824 0.619608 RG 15 75 m 597 75 l S " + 
"BT  /DeSaBoCBC~1416394845 16 Tf 20 TL 1 0 0 1 30 567 Tm (<something something something>) Tj T* (<something something something>) Tj T* /DeSaCBD~1416394845 10 Tf 1 0 0 1 30 517 Tm (<something something something>) Tj T* /HelvCBE~1416394845 12 Tf 16 TL 1 0 0 1 30 472 Tm [ (Journal Title, Volume 123, Number 4, Winter 2014, pp. 12-34) ] TJ T* [ (\\(Article\\)) ] TJ T* /DeSaCBF~1416394845 12 Tf 15 TL 1 0 0 1 30 418 Tm (<something something something>) Tj T* /HelvCBH~1416394845 12 Tf 1 0 0 1 50 268 Tm [ (Not the right string) ] TJ /HelvCBK~1416394845 10 Tf 0.674510 0.658824 0.619608 rg 1 0 0 1 15 37.500 Tm [ (                                                      Access provided by <something> (1 Dec 2014 14:32 GMT)) ] TJ  ET " + 
"q 7 0 0 7 35 266.50 cm /GxCBG Do Q " +
"BT  /HelvCBI~1416394845 9 Tf 0.325490 0.427451 0.494118 rg 1 0 0 1 50 253 Tm [ (http://www.example.com/journals/jabc/summary/v123/123.4.author.html) ] TJ  ET " + 
"0.662745 0.662745 0.662745 rg 478 426 86.400 124.56 re f q 86.400 0 0 124.56 477 427 cm /JxCBJ Do Q"
// ---- end PDF stream ----
    ));
    assertFalse(worker.getResult());
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q 375.12 0 0 174.96 5 607 cm /JxCBB Do Q 0.674510 0.658824 0.619608 RG 15 592 m 597 592 l S 0.674510 0.658824 0.619608 RG 15 75 m 597 75 l S " + 
"BT  /DeSaBoCBC~1416394845 16 Tf 20 TL 1 0 0 1 30 567 Tm (<something something something>) Tj T* (<something something something>) Tj T* /DeSaCBD~1416394845 10 Tf 1 0 0 1 30 517 Tm (<something something something>) Tj T* /HelvCBE~1416394845 12 Tf 16 TL 1 0 0 1 30 472 Tm [ (Journal Title, Volume 123, Number 4, Winter 2014, pp. 12-34) ] TJ T* [ (\\(Article\\)) ] TJ T* /DeSaCBF~1416394845 12 Tf 15 TL 1 0 0 1 30 418 Tm (<something something something>) Tj T* /HelvCBH~1416394845 12 Tf 1 0 0 1 50 268 Tm [ (For additional information about this article) ] TJ /HelvCBK~1416394845 10 Tf 0.674510 0.658824 0.619608 rg 1 0 0 1 15 37.500 Tm [ (                                                      Not the right string (1 Dec 2014 14:32 GMT)) ] TJ  ET " + 
"q 7 0 0 7 35 266.50 cm /GxCBG Do Q " +
"BT  /HelvCBI~1416394845 9 Tf 0.325490 0.427451 0.494118 rg 1 0 0 1 50 253 Tm [ (http://www.example.com/journals/jabc/summary/v123/123.4.author.html) ] TJ  ET " + 
"0.662745 0.662745 0.662745 rg 478 426 86.400 124.56 re f q 86.400 0 0 124.56 477 427 cm /JxCBJ Do Q"
// ---- end PDF stream ----
    ));
    assertFalse(worker.getResult());
  }
  
}
