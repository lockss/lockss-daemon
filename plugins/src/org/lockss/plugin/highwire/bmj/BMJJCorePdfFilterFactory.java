/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.highwire.bmj;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.lockss.filter.pdf.SimplePdfFilterFactory;
import org.lockss.pdf.PdfDocument;
import org.lockss.pdf.PdfException;
import org.lockss.pdf.PdfPage;
import org.lockss.pdf.PdfToken;
import org.lockss.pdf.PdfTokenStream;
import org.lockss.pdf.PdfTokenStreamStateMachine;
import org.lockss.pdf.PdfTokenStreamWorker;
import org.lockss.pdf.PdfUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class BMJJCorePdfFilterFactory extends SimplePdfFilterFactory{

    private static final Logger log = Logger.getLogger(BMJJCorePdfFilterFactory.class);
    public static Pattern DOWNLOADED_FROM_PATTERN = Pattern.compile("");

    @Override
    public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
        PdfUtil.normalizeTrailerId(pdfDocument);
        BMJDownloadedFromStateMachine worker = new BMJDownloadedFromStateMachine(DOWNLOADED_FROM_PATTERN);
        for (PdfPage pdfPage : pdfDocument.getPages()) {
          List<PdfTokenStream> pdfTokenStreams = pdfPage.getAllTokenStreams();
          PdfTokenStream pdfTokenStream = pdfTokenStreams.get(pdfTokenStreams.size() - 1);
          for (Iterator<PdfTokenStream> iter = pdfTokenStreams.iterator(); iter.hasNext();) {
            PdfTokenStream nextTokStream = iter.next();
            worker.process(nextTokStream);      
            if (worker.getResult()) {
              List<PdfToken> pdfTokens = nextTokStream.getTokens();
              pdfTokens.subList(worker.getBegin(), worker.getEnd() + 1).clear();
              nextTokStream.setTokens(pdfTokens);
              break; // out of the stream loop, go on to next page
            }
          }
        }
    }

  public static class BMJDownloadedFromStateMachine extends PdfTokenStreamStateMachine {

    protected String s = "";

    // The footer is close to the bottom of each page
    public BMJDownloadedFromStateMachine(Pattern downloadPattern) {
      super(PdfTokenStreamWorker.Direction.BACKWARD);
    }

    @Override
    public void state0() throws PdfException {
        //We will be creating a backwards-going state machine. First find Q.
      if (isRestoreGraphicsState()) {
        setEnd(getIndex());
        setState(1);
      }
    } 

    @Override
    public void state1() throws PdfException {
        //then find ET
        if (isEndTextObject()) {
            setState(2);
        }else{
            setState(0);
        }
    }

    @Override
    public void state2() throws PdfException {
        //accumulates any Tj/TJ strings
    }

    @Override
    public void state3() throws PdfException{
        //then find BT
        if (isBeginTextObject()) {
            setBegin(getIndex());
            setState(4);
          }
    }

    @Override
    public void state4() throws PdfException{
        //then find q
        /*if (/*is equal to q ) {
            setState(0);
          }*/
    }

  }  
}
