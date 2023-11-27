/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.palgrave;

import java.util.List;
import org.lockss.filter.pdf.SimplePdfFilterFactory;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import java.util.regex.Pattern;

public class PalgraveBookPdfFilterFactory extends SimplePdfFilterFactory {
  private static final String searchPatternTemplate = "^Copyright material from www\\.palgraveconnect\\.com";
  
  private final static Pattern DOWNLOADED_FROM_PATTERN = Pattern.compile(searchPatternTemplate);
  
  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetMetadata();
    PdfUtil.normalizeTrailerId(pdfDocument);   
    removeDownloadStrip(pdfDocument);
  }
  
  private void removeDownloadStrip(PdfDocument pdfDocument) throws PdfException {
    // using StateMachine which requires 1.67
   PalgraveDownloadedFromStateMachine worker = new PalgraveDownloadedFromStateMachine();

   // check each page for an access date set on the rhs of the page
   for (PdfPage pdfPage : pdfDocument.getPages()) {
     // Pages seem to be made of concatenated token streams, and while most
     // target personalization is at the end (previously filtered the last
     // token stream), some books (9781137023803 - on pages with "Left Intentianally
     // Blank") had customization elsewhere in the token stream, so now checking all
     List<PdfTokenStream> pdfTokenstreams = pdfPage.getAllTokenStreams();
     for (PdfTokenStream pdfTokenStream : pdfTokenstreams) {
       worker.process(pdfTokenStream);
       if (worker.getResult()) {    // found it, fixing it
         List<PdfToken> pdfTokens = pdfTokenStream.getTokens();
         pdfTokens.subList(worker.getBegin(), worker.getEnd() + 1).clear();
         pdfTokenStream.setTokens(pdfTokens);
         break;        // now go check the next page
       }
     }
   }
  }
  
  public static class PalgraveDownloadedFromStateMachine extends PdfTokenStreamStateMachine {
    // The footer is close to the bottom of each page
    public PalgraveDownloadedFromStateMachine() {
      super(Direction.BACKWARD);
    }

    @Override
    public void state0() throws PdfException {
      if (isEndTextObject()) {
        setEnd(getIndex());
        setState(1);
      }
    } 

    @Override
    public void state1() throws PdfException {
      if (isShowTextGlyphPositioningFind(DOWNLOADED_FROM_PATTERN))  {
        setState(2);
      } else if (isBeginTextObject()) { // not the BT-ET we were looking for
        //STOPPING HERE: assuming only one bt/et in the stream we want
        setState(0); //we're stopping, but this is safe behavior for future changes
        stop();
      }
    }

    @Override
    public void state2() throws PdfException {
      if (isBeginTextObject()) {  
          setResult(true);
          setBegin(getIndex());
          stop(); // found what we needed, stop processing this page
      }  
    }
  }
}
