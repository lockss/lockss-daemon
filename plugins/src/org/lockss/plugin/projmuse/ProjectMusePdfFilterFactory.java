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

package org.lockss.plugin.projmuse;

import java.util.regex.Pattern;

import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class ProjectMusePdfFilterFactory extends ExtractingPdfFilterFactory {
  
  private static final Logger log = Logger.getLogger(ProjectMusePdfFilterFactory.class);
  
  /*
   * Examples:
   * http://muse.jhu.edu/journals/perspectives_on_science/v022/22.4.oberdan.pdf 12/01/14
   */
  public static class FrontPageWorker extends PdfTokenStreamStateMachine {
    
    public static final Pattern ACCESS =
        Pattern.compile("^ *(Access provided by|Accessed) ", Pattern.CASE_INSENSITIVE);
    
    public FrontPageWorker() {
      super(log);
    }
    
    @Override
    public void state0() throws PdfException {
      if (isBeginTextObject()) {
        setState(1);
      }
    }
    
    @Override
    public void state1() throws PdfException {
      if (isShowTextFind(ACCESS) ||
          isShowTextGlyphPositioningFind(ACCESS)) {
        setState(2);
      }
      else if (isEndTextObject()) { 
        setState(0);
      }
    }
    
    @Override
    public void state2() throws PdfException {
      if (isEndTextObject()) {
        setResult(true);
        stop(); 
      }
    }
  
  }
    
  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetCreator();
    pdfDocument.unsetMetadata();
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetProducer();
    PdfUtil.normalizeTrailerId(pdfDocument);
    
    if (pdfDocument.getNumberOfPages() > 0) {
      PdfTokenStreamStateMachine worker = new FrontPageWorker();
      worker.process(pdfDocument.getPage(0).getPageTokenStream());
      if (worker.getResult()) {
        pdfDocument.removePage(0);
      }
    }
    
    PdfUtil.normalizeAllTokenStreams(pdfDocument);
  }
  
}
