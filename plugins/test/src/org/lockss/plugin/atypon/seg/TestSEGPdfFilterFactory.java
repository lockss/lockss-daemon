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

package org.lockss.plugin.atypon.seg;

import org.lockss.pdf.MockPdfTokenStream;
import org.lockss.plugin.atypon.BaseAtyponScrapingPdfFilterFactory.AtyponDownloadedFromStateMachine;
import org.lockss.plugin.atypon.BaseAtyponScrapingPdfFilterFactory.CitedByStateMachine;
import org.lockss.test.LockssTestCase;

public class TestSEGPdfFilterFactory extends LockssTestCase {

  /*
   * Example: http://library.seg.org/doi/pdfplus/10.1190/geo2012-0531.1
   */
  public void testWorker() throws Exception {
    CitedByStateMachine citedSM = 
        new CitedByStateMachine("This article has been cited by:");// hardcode the default
    AtyponDownloadedFromStateMachine downSM = 
        new AtyponDownloadedFromStateMachine(org.lockss.plugin.atypon.seg.SEGPdfFilterFactory.SEG_DOWNLOADED_PATTERN);
    
    citedSM.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"q " +
"1 0 0 -1 0 782 cm " +
"q " +
"1 0 0 1 56.692 56.692 cm " +
"0 g " +
"BT " +
"/F16 10 Tf " +
"1 0 0 -1 -10 8.99499989 Tm [(This article has been cited by:)] TJ " +
"/F15 10 Tf " +
"1 0 0 -1 -7.19000006 35.16799927 Tm"
// ---- end PDF stream ----
    ));
    assertTrue(citedSM.getResult());
    
    citedSM.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"BT " +
"(This is irrelevant.) Tj " +
"ET " +
"q " +
"1 0 0 -1 0 782 cm " +
"q " +
"1 0 0 1 56.692 56.692 cm " +
"0 g " +
"BT " +
"/F16 10 Tf " +
"1 0 0 -1 -10 8.99499989 Tm [(This article has been cited by:)] TJ " +
"/F15 10 Tf " +
"1 0 0 -1 -7.19000006 35.16799927 Tm"
// ---- end PDF stream ----
    ));
    assertTrue(citedSM.getResult());
    
    downSM.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
/* 00 */ "BT " +
/* 01 */ "/F1 10 Tf " +
/* 04 */ "0 1 -1 0 10 121.85 Tm " +
/* 11 */ "(Downloaded 11/26/14 to 12.34.56.789. Redistribution subject to SEG license or copyright; see Terms of Use at http://library.seg.org/)Tj " +
/* 13 */ "1 0 0 1 0 0 Tm " +
/* 20 */ "ET"
// ---- end PDF stream ----
    ));
    assertTrue(downSM.getResult());
    
    downSM.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
"BT " +
"(This is irrelevant.) Tj " +
"ET " +
"BT " +
"/F1 10 Tf " +
"0 1 -1 0 10 121.85 Tm " +
"(Downloaded 11/26/14 to 12.34.56.789. Redistribution subject to SEG license or copyright; see Terms of Use at http://library.seg.org/)Tj " +
"1 0 0 1 0 0 Tm " +
"ET"
// ---- end PDF stream ----
    ));
    assertTrue(downSM.getResult());

    downSM.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
/* 00 */ "BT " +
/* 01 */ "/F1 10 Tf " +
/* 04 */ "0 1 -1 0 10 121.85 Tm " +
/* 11 */ "(Not the right string.) Tj " +
/* 13 */ "1 0 0 1 0 0 Tm " +
/* 20 */ "ET"
// ---- end PDF stream ----
    ));
    assertFalse(downSM.getResult());
    citedSM.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
/* 00 */ "BT " +
/* 01 */ "/F1 10 Tf " +
/* 04 */ "0 1 -1 0 10 121.85 Tm " +
/* 11 */ "(Not the right string.) Tj " +
/* 13 */ "1 0 0 1 0 0 Tm " +
/* 20 */ "ET"
// ---- end PDF stream ----
    ));
    assertFalse(citedSM.getResult());
    
  }
  
}
