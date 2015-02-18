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
