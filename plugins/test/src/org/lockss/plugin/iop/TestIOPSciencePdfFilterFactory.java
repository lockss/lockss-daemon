/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.iop;

import org.lockss.pdf.*;
import org.lockss.plugin.iop.IOPSciencePdfFilterFactory.RecognizeFirstPageWorker;
import org.lockss.test.LockssTestCase;

public class TestIOPSciencePdfFilterFactory extends LockssTestCase {

  /*
   * Examples:
   * http://muse.jhu.edu/journals/perspectives_on_science/v022/22.4.oberdan.pdf 12/01/14
   */
  public void testRecognizeFirstPageWorker() throws Exception {
    PdfTokenStreamStateMachine worker = new RecognizeFirstPageWorker();
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
        "/GS1 gs\n" + 
        "BT\n" + 
        "/F1 1 Tf\n" + 
        "11.3573 0 0 11.3573 46.8283 761.2156 Tm\n" + 
        "0 0 0 1 k\n" + 
        "0 Tc\n" + 
        "0 Tw\n" + 
        "(Erratum:)Tj\n" + 
        "/F2 1 Tf\n" + 
        "4.4576 0 TD\n" + 
        "(�)Tj\n" + 
        "/F1 1 Tf\n" + 
        ".4992 0 TD\n" + 
        "[(Effect)-339(of)-331.9(tetramethylsilane)]TJ\n" + 
        "/F3 1 Tf\n" + 
        "12.7939 0 TD\n" + 
        "(s)Tj\n" + 
        "/F1 1 Tf\n" + 
        ".604 0 TD\n" + 
        "[(ow)-333.2(on)-334.5(the)-334(deposition)-335.3(and)-336.6(tribological)-338.8(behaviors)]TJ\n" + 
        "-18.3547 -1.4077 TD\n" + 
        "[(of)-331.9(silicon)-337.3(doped)-336.6(diamond-like)-335.5(carbon)-333(rubbed)-336(against)-337.8(poly\\(oxymethylene\\))]TJ\n" + 
        "/F2 1 Tf\n" + 
        "34.8425 0 TD\n" + 
        "(�)Tj\n" + 
        "/F1 1 Tf\n" + 
        "-34.8425 -1.4027 TD\n" + 
        ".0001 Tc\n" + 
        "[([Jpn.)-331(J.)-336(Appl.)-334.3(Phys.)-336(53,)-333.1(1)52.9(1RA04)-337.3(\\(2014\\)])]TJ\n" + 
        "/F4 1 Tf\n" + 
        "9.4646 0 0 9.4646 46.8283 708.3778 Tm\n" + 
        "0 Tc\n" + 
        "[(Xingrui)-334.3(Deng)]TJ\n" + 
        "6.6251 0 0 6.6251 101.5937 711.8928 Tm\n" + 
        "(1)Tj\n" + 
        "/F5 1 Tf\n" + 
        "9.4646 0 0 9.4646 105.2787 708.3778 Tm\n" + 
        "(*)Tj\n" + 
        "/F4 1 Tf\n" + 
        ".3534 0 TD\n" + 
        "[(,)-335(Y)73(ankuang)-333(Lim)]TJ\n" + 
        "6.6251 0 0 6.6251 174.274 711.8928 Tm\n" + 
        "(1)Tj\n" + 
        "9.4646 0 0 9.4646 177.9023 708.3778 Tm\n" + 
        "[(,)-335(Hiroyuki)-331.4(Kousaka)]TJ\n" + 
        "6.6251 0 0 6.6251 257.3291 711.8928 Tm\n" + 
        "(1,2)Tj\n" + 
        "9.4646 0 0 9.4646 266.4566 708.3778 Tm\n" + 
        "[(,)-335(T)106.8(akayuki)-333.8(T)106.8(okoroyama)]TJ\n" + 
        "6.6251 0 0 6.6251 363.118 711.8928 Tm\n" + 
        "(3)Tj\n" + 
        "9.4646 0 0 9.4646 366.7463 708.3778 Tm\n"
// ---- end PDF stream ----
    ));
    assertFalse(worker.getResult());
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
        " Q\n" + 
        "q\n" + 
        "q 595 0 0 74 0 770 cm /Xi0 Do Q\n" + 
        "BT\n" + 
        "/Xi1 10 Tf\n" + 
        "1 0 0 1 50 640 Tm\n" + 
        "(This article has been downloaded from IOPscience. Please scroll down to see the full text article.)Tj\n" + 
        "1 0 0 1 50 495 Tm\n" + 
        "(Download details:)Tj\n" + 
        "1 0 0 1 50 465 Tm\n" + 
        "(IP Address: 171.66.236.16)Tj\n" + 
        "1 0 0 1 50 450 Tm\n" + 
        "(This content was downloaded on 13/06/2015 at 02:29)Tj\n" + 
        "ET\n" + 
        "0 0 0 RG\n" + 
        "0.66667 w\n" + 
        "125.05 406.67 m\n" + 
        "246.77 406.67 l\n" + 
        "S\n" + 
        "0 G\n" + 
        "1 w\n" + 
        "BT\n" + 
        "1 0 0 1 50 410 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0 0 0 rg\n" + 
        "(Please note that )Tj\n" + 
        "0 g\n" + 
        "0 0 0 rg\n" + 
        "(terms and conditions apply.)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "BT\n" + 
        "1 0 0 1 50 716 Tm\n" + 
        "/Xi1 12 Tf\n" + 
        "0 0 0 rg\n" + 
        "(Information Update)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "0 0 0 RG\n" + 
        "0.66667 w\n" + 
        "74.45 571.67 m\n" + 
        "223.42 571.67 l\n" + 
        "S\n" + 
        "0 G\n" + 
        "1 w\n" + 
        "0 0 0 RG\n" + 
        "0.66667 w\n" + 
        "282.35 571.67 m\n" + 
        "362.39 571.67 l\n" + 
        "S\n" + 
        "0 G\n" + 
        "1 w\n" + 
        "BT\n" + 
        "1 0 0 1 50 575 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0 0 0 rg\n" + 
        "(View )Tj\n" + 
        "0 g\n" + 
        "0 0 0 rg\n" + 
        "(the table of contents for this issue)Tj\n" + 
        "0 g\n" + 
        "0 0 0 rg\n" + 
        "(, or go to the )Tj\n" + 
        "0 g\n" + 
        "0 0 0 rg\n" + 
        "(journal homepage)Tj\n" + 
        "0 g\n" + 
        "0 0 0 rg\n" + 
        "( for more)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "1 0 0 1 50 620 Tm\n" + 
        "(2012 Science Foundation in China 20 30)Tj\n" + 
        "1 0 0 1 50 600 Tm\n" + 
        "(\\(http://iopscience.iop.org/1005-0841/20/2/003\\))Tj\n" + 
        "BT\n" + 
        "1 0 0 1 10.16 778 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0.23137 0.54902 0.76471 rg\n" + 
        "(Home)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "BT\n" + 
        "1 0 0 1 50.16 778 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0.23137 0.54902 0.76471 rg\n" + 
        "(Search)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "BT\n" + 
        "1 0 0 1 95.55 778 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0.23137 0.54902 0.76471 rg\n" + 
        "(Collections)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "BT\n" + 
        "1 0 0 1 155.1 778 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0.23137 0.54902 0.76471 rg\n" + 
        "(Journals)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "BT\n" + 
        "1 0 0 1 203.93 778 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0.23137 0.54902 0.76471 rg\n" + 
        "(About)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "BT\n" + 
        "1 0 0 1 240.1 778 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0.23137 0.54902 0.76471 rg\n" + 
        "(Contact us)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "BT\n" + 
        "1 0 0 1 298.38 778 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0.23137 0.54902 0.76471 rg\n" + 
        "(My IOPscience)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "Q\n"
// ---- end PDF stream ----
    ));
    assertTrue(worker.getResult());
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
        " Q\n" + 
        "q\n" + 
        "q 595 0 0 74 0 770 cm /Xi0 Do Q\n" + 
        "BT\n" + 
        "/Xi1 10 Tf\n" + 
        "1 0 0 1 50 640 Tm\n" + 
        "(This content has been downloaded from IOPscience. Please scroll down to see the full text.)Tj\n" + 
        "1 0 0 1 50 495 Tm\n" + 
        "(Download details:)Tj\n" + 
        "1 0 0 1 50 465 Tm\n" + 
        "(IP Address: 171.66.236.16)Tj\n" + 
        "1 0 0 1 50 450 Tm\n" + 
        "(This content was downloaded on 13/06/2015 at 02:29)Tj\n" + 
        "ET\n" + 
        "0 0 0 RG\n" + 
        "0.66667 w\n" + 
        "125.05 406.67 m\n" + 
        "246.77 406.67 l\n" + 
        "S\n" + 
        "0 G\n" + 
        "1 w\n" + 
        "BT\n" + 
        "1 0 0 1 50 410 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0 0 0 rg\n" + 
        "(Please note that )Tj\n" + 
        "0 g\n" + 
        "0 0 0 rg\n" + 
        "(terms and conditions apply.)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "BT\n" + 
        "1 0 0 1 50 716 Tm\n" + 
        "/Xi1 12 Tf\n" + 
        "0 0 0 rg\n" + 
        "(Information Update)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "0 0 0 RG\n" + 
        "0.66667 w\n" + 
        "74.45 571.67 m\n" + 
        "223.42 571.67 l\n" + 
        "S\n" + 
        "0 G\n" + 
        "1 w\n" + 
        "0 0 0 RG\n" + 
        "0.66667 w\n" + 
        "282.35 571.67 m\n" + 
        "362.39 571.67 l\n" + 
        "S\n" + 
        "0 G\n" + 
        "1 w\n" + 
        "BT\n" + 
        "1 0 0 1 50 575 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0 0 0 rg\n" + 
        "(View )Tj\n" + 
        "0 g\n" + 
        "0 0 0 rg\n" + 
        "(the table of contents for this issue)Tj\n" + 
        "0 g\n" + 
        "0 0 0 rg\n" + 
        "(, or go to the )Tj\n" + 
        "0 g\n" + 
        "0 0 0 rg\n" + 
        "(journal homepage)Tj\n" + 
        "0 g\n" + 
        "0 0 0 rg\n" + 
        "( for more)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "1 0 0 1 50 620 Tm\n" + 
        "(2012 Science Foundation in China 20 30)Tj\n" + 
        "1 0 0 1 50 600 Tm\n" + 
        "(\\(http://iopscience.iop.org/1005-0841/20/2/003\\))Tj\n" + 
        "BT\n" + 
        "1 0 0 1 10.16 778 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0.23137 0.54902 0.76471 rg\n" + 
        "(Home)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "BT\n" + 
        "1 0 0 1 50.16 778 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0.23137 0.54902 0.76471 rg\n" + 
        "(Search)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "BT\n" + 
        "1 0 0 1 95.55 778 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0.23137 0.54902 0.76471 rg\n" + 
        "(Collections)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "BT\n" + 
        "1 0 0 1 155.1 778 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0.23137 0.54902 0.76471 rg\n" + 
        "(Journals)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "BT\n" + 
        "1 0 0 1 203.93 778 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0.23137 0.54902 0.76471 rg\n" + 
        "(About)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "BT\n" + 
        "1 0 0 1 240.1 778 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0.23137 0.54902 0.76471 rg\n" + 
        "(Contact us)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "BT\n" + 
        "1 0 0 1 298.38 778 Tm\n" + 
        "/Xi1 10 Tf\n" + 
        "0.23137 0.54902 0.76471 rg\n" + 
        "(My IOPscience)Tj\n" + 
        "0 g\n" + 
        "ET\n" + 
        "Q\n"
// ---- end PDF stream ----
    ));
    assertTrue(worker.getResult());
    
    worker.process(MockPdfTokenStream.parse(
// ---- begin PDF stream ----
        ""
// ---- end PDF stream ----
    ));
    assertFalse(worker.getResult());
  }
  
}
