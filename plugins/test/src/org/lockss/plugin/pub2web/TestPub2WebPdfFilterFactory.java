/*
 * $Id$
 */ 

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pub2web;

import org.lockss.pdf.*;
import org.lockss.plugin.pub2web.Pub2WebPdfFilterFactory.Pub2WebDownloadedFromStateMachine;
import org.lockss.test.LockssTestCase;

public class TestPub2WebPdfFilterFactory extends LockssTestCase {

  /*
   * Example: http://api.ingentaconnect.com/content/maney/aac/2012/00000111/00000003/art00002?crawler=true&mimetype=application/pdf
   */
  public void testPub2WebDownloadedFromStateMachine() throws Exception {
    Pub2WebDownloadedFromStateMachine worker = new Pub2WebDownloadedFromStateMachine();
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
/* 00 */ "q " +
/* 01 */ "BT " +
/* 02 */ "36 805.89 Td " +
/* 05 */ "ET " +
/* 06 */ "Q " +
/* 07 */ "0.86275 0.86275 0.86275 rg " +
/* 11 */ "/F1 10 Tf " +
/* 14 */ "0 1 -1 0 0 0 cm " +
/* 21 */ "BT\n" +
/* 22 */ "1 0 0 1 230.42 34 Tm\n" +
/* 29 */ "/F1 8 Tf " +
/* 32 */ "0.86275 0.86275 0.86275 rg\n" +
/* 36 */ "(Downloaded from www.asmscience.org by)Tj\n" +
/* 38 */ "0 g\n" +
/* 40 */ "ET " +
/* 41 */ "BT\n" +
/* 42 */ "1 0 0 1 271.08 22 Tm\n" +
/* 49 */ "/F1 8 Tf\n" +
/* 52 */ "0.86275 0.86275 0.86275 rg\n" +
/* 56 */ "(IP:  156.56.241.164)Tj\n" +
/* 58 */ "0 g\n" +
/* 60 */ "ET\n" +
/* 61 */ "BT\n" +
/* 62 */ "1 0 0 1 250.63 10 Tm\n" +
/* 69 */ "/F1 8 Tf\n" +
/* 72 */ "0.86275 0.86275 0.86275 rg\n" +
/* 76 */ "(On: Sun, 12 Apr 2015 05:59:48)Tj\n" +
/* 78 */ "0 g\n" +
/* 80 */ "ET" +
/* 81 */ "0 -1 1 0 0 0 cm " +
/* 88 */ "q 1 0 0 1 0 0 cm /Xf1 Do Q"
// ---- end PDF stream ----
    ));
    assertTrue(worker.getResult());
    assertEquals(21, worker.getBegin()); // inclusive
    assertEquals(80, worker.getEnd()); // inclusive
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
/* 00 */ "q " +
/* 01 */ "BT " +
/* 02 */ "36 805.89 Td " +
/* 05 */ "ET " +
/* 06 */ "Q " +
/* 07 */ "0.86275 0.86275 0.86275 rg " +
/* 11 */ "/F1 10 Tf " +
/* 14 */ "0 1 -1 0 0 0 cm " +
/* 21 */ "BT\n" +
/* 22 */ "1 0 0 1 230.42 34 Tm\n" +
/* 29 */ "/F1 8 Tf " +
/* 32 */ "0.86275 0.86275 0.86275 rg\n" +
/* 36 */ "(Downloaded from www.asmscience.org by)Tj\n" +
/* 38 */ "0 g\n" +
/* 40 */ "ET " +
/* 41 */ "BT\n" +
/* 42 */ "1 0 0 1 271.08 22 Tm\n" +
/* 49 */ "/F1 8 Tf\n" +
/* 52 */ "0.86275 0.86275 0.86275 rg\n" +
/* 56 */ "(IP:  156.56.241.164)Tj\n" +
/* 58 */ "0 g\n" +
/* 60 */ "ET\n" +
/* 61 */ "BT\n" +
/* 62 */ "1 0 0 1 250.63 10 Tm\n" +
/* 69 */ "/F1 8 Tf\n" +
/* 72 */ "0.86275 0.86275 0.86275 rg\n" +
/* 76 */ "(Not on: Sun, 12 Apr 2015 05:59:48)Tj\n" +
/* 78 */ "0 g\n" +
/* 80 */ "ET" +
/* 81 */ "0 -1 1 0 0 0 cm " +
/* 88 */ "q 1 0 0 1 0 0 cm /Xf1 Do Q"
// ---- end PDF stream ----
    ));
    assertTrue(worker.getResult());
    assertEquals(21, worker.getBegin()); // inclusive
    assertEquals(60, worker.getEnd()); // inclusive
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"BT " +
"36 805.89 Td " +
"ET " +
"Q " +
"0.86275 0.86275 0.86275 rg " +
"/F1 10 Tf " +
"0 1 -1 0 0 0 cm " +
"BT " +
"1 0 0 1 286.45 -15 Tm " +
"/F1 10 Tf " +
"(This is not the right string.) Tj " +
"ET " +
"0 -1 1 0 0 0 cm " +
"q 1 0 0 1 0 0 cm /Xf1 Do Q"
// ---- end PDF stream ----
    ));
    assertFalse(worker.getResult());
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDf stream ----
"q " +
"BT " +
"36 805.89 Td " +
"ET " +
"Q " +
"0.86275 0.86275 0.86275 rg " +
"/F1 10 Tf " +
"0 1 -1 0 0 0 cm " +
"BT " +
"1 0 0 1 286.45 -15 Tm " +
"/F1 10 Tf " +
"(Downloaded from www.asmscience.org by)Tj\n" +
"ET " +
"(This is irrelevant.) Tj " +
"BT " +
"(This is irrelevant.) Tj " +
"ET " +
"0 -1 1 0 0 0 cm " +
"q 1 0 0 1 0 0 cm /Xf1 Do Q"
// ---- end PDF stream ----
    ));
    assertFalse(worker.getResult());
  }
  
}
