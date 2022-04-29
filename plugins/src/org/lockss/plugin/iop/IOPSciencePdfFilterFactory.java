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

import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
import org.lockss.plugin.*;

/**
 * <p>
 * A synthetic front page was added to PDF files. Because the server-side
 * stamping process shuffles the object graph compared to the original file, we
 * have no choice but to use an extracting filter.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 */
public class IOPSciencePdfFilterFactory extends ExtractingPdfFilterFactory {

  protected static class RecognizeFirstPageWorker extends PdfTokenStreamStateMachine {
    
    @Override
    public void state0() throws PdfException {
      if (isShowTextContains(" has been downloaded from IOPscience. Please scroll down to see the full text")) {
        result = true;
        stop();
      }
    }
  }
  
  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
    
    RecognizeFirstPageWorker worker = new RecognizeFirstPageWorker();
    worker.process(pdfDocument.getPage(0).getPageTokenStream());
    if (worker.getResult()) {
      pdfDocument.removePage(0);
    }
  }
  
}
