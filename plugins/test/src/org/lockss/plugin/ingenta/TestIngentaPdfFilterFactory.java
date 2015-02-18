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

package org.lockss.plugin.ingenta;

import org.lockss.pdf.*;
import org.lockss.plugin.ingenta.IngentaPdfFilterFactory.ManeyPublishingPdfFilterFactory.ManeyPublishingWorker;
import org.lockss.plugin.ingenta.IngentaPdfFilterFactory.PacificAffairsPdfFilterFactory.PacificAffairsWorker;
import org.lockss.plugin.ingenta.IngentaPdfFilterFactory.WhiteHorsePressPdfFilterFactory.WhiteHorsePressWorker;
import org.lockss.test.LockssTestCase;

public class TestIngentaPdfFilterFactory extends LockssTestCase {

  /*
   * Example: http://api.ingentaconnect.com/content/maney/aac/2012/00000111/00000003/art00002?crawler=true&mimetype=application/pdf
   */
  public void testManeyPublishingWorker() throws Exception {
    ManeyPublishingWorker worker = new ManeyPublishingWorker();
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
/* 21 */ "BT " +
/* 22 */ "1 0 0 1 286.45 -15 Tm " +
/* 29 */ "/F1 10 Tf " +
/* 32 */ "(Published by Maney Publishing \\(c\\) IOM Communications Ltd)Tj " +
/* 34 */ "ET " +
/* 35 */ "0 -1 1 0 0 0 cm " +
/* 42 */ "q 1 0 0 1 0 0 cm /Xf1 Do Q"
// ---- end PDF stream ----
    ));
    assertTrue(worker.result);
    assertEquals(21, worker.beginIndex); // inclusive
    assertEquals(34, worker.endIndex); // inclusive
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
    assertFalse(worker.result);
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
"(Published by Maney Publishing \\(c\\) IOM Communications Ltd) Tj " +
"ET " +
"(This is irrelevant.) Tj " +
"BT " +
"(This is irrelevant.) Tj " +
"ET " +
"0 -1 1 0 0 0 cm " +
"q 1 0 0 1 0 0 cm /Xf1 Do Q"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
  }
  
  public void testPacificAffairsWorker() throws Exception {
    PacificAffairsWorker worker = new PacificAffairsWorker();
    // Example: http://api.ingentaconnect.com/content/paaf/paaf/2013/00000086/00000003/art00006?crawler=true ingest1 15:20:29 09/10/13
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
/* 00 */ "q " +
/* 01 */ "BT " +
/* 02 */ "36 612 Td " +
/* 05 */ "ET " +
/* 06 */ "Q " +
/* 07 */ "0.78431 0.78431 0.78431 rg " +
/* 11 */ "/F1 6 Tf " +
/* 14 */ "0 1 -1 0 0 0 cm " +
/* 21 */ "BT " +
/* 22 */ "1 0 0 1 263.32 -44 Tm " +
/* 29 */ "/F1 6 Tf " +
/* 32 */ "(Copyright \\(c\\) Pacific Affairs. All rights reserved.)Tj " +
/* 34 */ "1 0 0 1 207.43 -50 Tm " +
/* 41 */ "(Delivered by Publishing Technology to: ?  IP: 93.91.26.11 on: Tue, 10 Sep 2013 22:20:28)Tj " +
/* 43 */ "ET " +
/* 44 */ "0 -1 1 0 0 0 cm " +
/* 51 */ "q 1 0 0 1 0 0 cm /Xf1 Do Q"
// ---- send PDF stream ----
    ));
    assertTrue(worker.result);
    assertEquals(21, worker.beginIndex);
    assertEquals(43, worker.endIndex);
    // Example: http://api.ingentaconnect.com/content/paaf/paaf/2013/00000086/00000003/art00006?crawler=true 11/25/14
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
/* 00 */ "q " +
/* 01 */ "0.78431 0.78431 0.78431 rg " +
/* 05 */ "/Xi0 6 Tf " +
/* 08 */ "0 1 -1 0 0 0 cm " +
/* 15 */ "BT " +
/* 16 */ "1 0 0 1 263.32 -44 Tm " +
/* 23 */ "/Xi0 6 Tf " +
/* 26 */ "(Copyright \\(c\\) Pacific Affairs. All rights reserved.)Tj " +
/* 28 */ "1 0 0 1 179.42 -50 Tm " +
/* 35 */ "(Delivered by Publishing Technology to: <something>  IP: <something> on: <something>)Tj " +
/* 37 */ "ET " +
/* 38 */ "0 -1 1 0 0 0 cm " +
/* 45 */ "Q " +
/* 46 */ "q " +
/* 47 */ "q " +
/* 48 */ "BT " +
/* 49 */ "36 612 Td " +
/* 52 */ "ET " +
/* 53 */ "Q " +
/* 54 */ "q 1 0 0 1 0 0 cm /Xf1 Do Q " +
/* 65 */ "Q " +
/* 66 */ "q " +
/* 67 */ "Q"
// ---- send PDF stream ----
    ));
    assertTrue(worker.result);
    assertEquals(15, worker.beginIndex);
    assertEquals(37, worker.endIndex);
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"0.78431 0.78431 0.78431 rg " +
"/Xi0 6 Tf " +
"0 1 -1 0 0 0 cm " +
"BT " +
"1 0 0 1 263.32 -44 Tm " +
"/Xi0 6 Tf " +
"(Copyright \\(c\\) Pacific Affairs. All rights reserved.)Tj " +
"1 0 0 1 179.42 -50 Tm " +
"(Not the right string.) Tj " +
"ET " +
"0 -1 1 0 0 0 cm " +
"Q " +
"q " +
"q " +
"BT " +
"36 612 Td " +
"ET " +
"Q " +
"q 1 0 0 1 0 0 cm /Xf1 Do Q " +
"Q " +
"q " +
"Q"
// ---- send PDF stream ----
    ));
    assertFalse(worker.result);
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"0.78431 0.78431 0.78431 rg " +
"/Xi0 6 Tf " +
"0 1 -1 0 0 0 cm " +
"1 0 0 1 263.32 -44 Tm " +
"/Xi0 6 Tf " +
"(Copyright \\(c\\) Pacific Affairs. All rights reserved.)Tj " +
"1 0 0 1 179.42 -50 Tm " +
"(Delivered by Publishing Technology to: <something>  IP: <something> on: <something>)Tj " +
"0 -1 1 0 0 0 cm " +
"Q " +
"q " +
"q " +
"BT " +
"36 612 Td " +
"ET " +
"Q " +
"q 1 0 0 1 0 0 cm /Xf1 Do Q " +
"Q " +
"q " +
"Q"
// ---- send PDF stream ----
    ));
    assertFalse(worker.result);
  }  
  
  /*
   * Example: PDF from http://www.ingentaconnect.com/content/whp/eh/2014/00000020/00000001/art00005
   */
  public void testWhiteHorsePressWorker() throws Exception {
    WhiteHorsePressWorker worker = new WhiteHorsePressWorker();
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
/* 00 */ "q " +
/* 01 */ "0.86275 0.86275 0.86275 rg " +
/* 05 */ "/Xi0 10 Tf " +
/* 08 */ "BT " +
/* 09 */ "1 0 0 1 149.51 25 Tm " +
/* 16 */ "/Xi0 10 Tf " +
/* 19 */ "(<something> = username)Tj " +
/* 21 */ "1 0 0 1 149.51 15 Tm " +
/* 28 */ "(<something> = IP address)Tj " +
/* 30 */ "1 0 0 1 125.6 5 Tm " +
/* 37 */ "(<something> = Date & Time)Tj " +
/* 39 */ "ET " +
/* 40 */ "Q " +
/* 41 */ "q"
// ---- end PDF stream ----
    ));
    assertTrue(worker.result);
    assertEquals(8, worker.beginIndex); // inclusive
    assertEquals(39, worker.endIndex); // inclusive
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"0.86275 0.86275 0.86275 rg " +
"/Xi0 10 Tf " +
"BT " +
"1 0 0 1 149.51 25 Tm " +
"/Xi0 10 Tf " +
"(<something> = username)Tj " +
"1 0 0 1 149.51 15 Tm " +
"(<something> = IP address)Tj " +
"1 0 0 1 125.6 5 Tm " +
"(<something> = Not the Right String)Tj " +
"ET " +
"Q " +
"q"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"0.86275 0.86275 0.86275 rg " +
"/Xi0 10 Tf " +
"BT " +
"1 0 0 1 149.51 25 Tm " +
"/Xi0 10 Tf " +
"(<something> = username)Tj " +
"1 0 0 1 149.51 15 Tm " +
"(<something> = Not the Right String)Tj " +
"1 0 0 1 125.6 5 Tm " +
"(<something> = Date & Time)Tj " +
"ET " +
"Q " +
"q"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"0.86275 0.86275 0.86275 rg " +
"/Xi0 10 Tf " +
"1 0 0 1 149.51 25 Tm " +
"/Xi0 10 Tf " +
"(<something> = username)Tj " +
"1 0 0 1 149.51 15 Tm " +
"(<something> = IP address)Tj " +
"1 0 0 1 125.6 5 Tm " +
"(<something> = Date & Time)Tj " +
"Q " +
"q"
// ---- end PDF stream ----
    ));
    assertFalse(worker.result);
  }
  
}
