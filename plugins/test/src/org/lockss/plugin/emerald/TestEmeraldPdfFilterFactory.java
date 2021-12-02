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

package org.lockss.plugin.emerald;

import org.lockss.pdf.*;
import org.lockss.plugin.emerald.EmeraldPdfFilterFactory.FrontPageWorker;
import org.lockss.test.LockssTestCase;

public class TestEmeraldPdfFilterFactory extends LockssTestCase {

  public void testFrontPageWorker() throws Exception {
    FrontPageWorker worker = new FrontPageWorker();
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"(This is irrelevant.) Tj " +
"/Name1 Do " +
"(This is irrelevant.) Tj " +
"Q " +
"q " +
"(This is irrelevant.) Tj " +
"/Name2 Do " +
"(This is irrelevant.) Tj " +
"Q " +
"(This is irrelevant.) Tj " +
"BT " +
"(This is irrelevant.) Tj " +
"(Downloaded on: <something>) Tj " +
"(This is irrelevant.) Tj " +
"ET " +
"(This is irrelevant.) Tj " +
"BT " +
"(This is irrelevant.) Tj " +
"(Access to this document was granted through an Emerald subscription provided by <something>) Tj " +
"(This is irrelevant.) Tj " +
"ET " +
"(This is irrelevant.) Tj"
// ---- end PDF stream ----
    ));
    assertTrue(worker.result);
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"(This is irrelevant.) Tj " +
"/Name1 Do " +
"(This is irrelevant.) Tj " +
"Q " +
"q " +
"(This is irrelevant.) Tj " +
"/Name2 Do " +
"(This is irrelevant.) Tj " +
"Q " +
"(This is irrelevant.) Tj " +
"BT " +
"(This is irrelevant.) Tj " +
"(Downloaded on: <something>) Tj " +
"(This is irrelevant.) Tj " +
"ET " +
"(This is irrelevant.) Tj " +
"BT " +
"(This is irrelevant.) Tj " +
"(Not the right string.) Tj " +
"(This is irrelevant.) Tj " +
"ET " +
"(This is irrelevant.) Tj"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"(This is irrelevant.) Tj " +
"/Name1 Do " +
"(This is irrelevant.) Tj " +
"Q " +
"q " +
"(This is irrelevant.) Tj " +
"/Name2 Do " +
"(This is irrelevant.) Tj " +
"Q " +
"(This is irrelevant.) Tj " +
"BT " +
"(This is irrelevant.) Tj " +
"(Downloaded on: <something>) Tj " +
"(This is irrelevant.) Tj " +
"ET " +
"(This is irrelevant.) Tj"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"(This is irrelevant.) Tj " +
"/Name1 Do " +
"(This is irrelevant.) Tj " +
"Q " +
"q " +
"(This is irrelevant.) Tj " +
"/Name2 Do " +
"(This is irrelevant.) Tj " +
"Q " +
"(This is irrelevant.) Tj " +
"BT " +
"(This is irrelevant.) Tj " +
"(Not the right string.) Tj " +
"(This is irrelevant.) Tj " +
"ET " +
"(This is irrelevant.) Tj"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"(This is irrelevant.) Tj " +
"/Name1 Do " +
"(This is irrelevant.) Tj " +
"Q " +
"q " +
"(This is irrelevant.) Tj " +
"/Name2 Do " +
"(This is irrelevant.) Tj " +
"Q " +
"(This is irrelevant.) Tj"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"(This is irrelevant.) Tj " +
"/Name1 Do " +
"(This is irrelevant.) Tj " +
"Q " +
"q " +
"(This is irrelevant.) Tj " +
"Q " +
"(This is irrelevant.) Tj"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"(This is irrelevant.) Tj " +
"/Name1 Do " +
"(This is irrelevant.) Tj " +
"Q " +
"(This is irrelevant.) Tj"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"(This is irrelevant.) Tj " +
"Q " +
"(This is irrelevant.) Tj"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
  }
  
}
