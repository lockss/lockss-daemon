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

package org.lockss.plugin.georgthiemeverlag;

import java.util.List;

import org.apache.pdfbox.pdmodel.*;
import org.lockss.filter.pdf.SimplePdfFilterFactory;
import org.lockss.pdf.*;
import org.lockss.pdf.pdfbox.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.georgthiemeverlag.GeorgThiemeVerlagPdfFilterFactory.GtvPdfDocumentFactory.GtvPdfBoxDocument;
import org.lockss.util.Logger;

public class GeorgThiemeVerlagPdfFilterFactory extends SimplePdfFilterFactory {

  /** @since 1.70.3 */
  protected static class GtvPdfDocumentFactory extends PdfBoxDocumentFactory {
   
    /** @since 1.70.3 */
    protected static class GtvPdfBoxDocument extends PdfBoxDocument {
      public static final String PDFDATE = "pdfDate";
      public static final String PDFUSER = "pdfUser";
      public GtvPdfBoxDocument(PdfBoxDocumentFactory pdfBoxDocumentFactory, PDDocument pdDocument) {
        super(pdfBoxDocumentFactory, pdDocument);
      }
      public PDDocumentInformation getPdDocumentInformation() {
        return pdDocument.getDocumentInformation();
      }
    }
    
    @Override
    public PdfBoxDocument makeDocument(PdfDocumentFactory pdfDocumentFactory, Object pdfDocumentObject) throws PdfException {
      return new GtvPdfBoxDocument(this, (PDDocument)pdfDocumentObject);
    }
    
  }
  
  public GeorgThiemeVerlagPdfFilterFactory() {
    super(new GtvPdfDocumentFactory());
  }
  
  public GeorgThiemeVerlagPdfFilterFactory(PdfDocumentFactory pdfDocumentFactory) {
    super(new GtvPdfDocumentFactory());
  }
  
  private static final Logger log = Logger.getLogger(GeorgThiemeVerlagPdfFilterFactory.class);
  
  @Override
  public void transform(ArchivalUnit au,
                        PdfDocument pdfDocument)
      throws PdfException {
    pdfDocument.unsetModificationDate();
    PdfUtil.normalizeTrailerId(pdfDocument);
    pdfDocument.unsetMetadata();
    PDDocumentInformation pdDocInfo = ((GtvPdfBoxDocument)pdfDocument).getPdDocumentInformation();
    if (pdDocInfo.getCustomMetadataValue(GtvPdfBoxDocument.PDFDATE) != null) {
      pdDocInfo.setCustomMetadataValue(GtvPdfBoxDocument.PDFDATE, null);
    }
    if (pdDocInfo.getCustomMetadataValue(GtvPdfBoxDocument.PDFUSER) != null) {
      pdDocInfo.setCustomMetadataValue(GtvPdfBoxDocument.PDFUSER, null);
    }
    
    PdfStateMachineWorker worker = new PdfStateMachineWorker();
    boolean anyXform = false;
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
      worker.process(pdfTokenStream);
      if (worker.getResult()) {
        anyXform = true;
        List<PdfToken> tokens = pdfTokenStream.getTokens();
        // clear tokens including text markers
        tokens.subList(worker.getBegin(), worker.getEnd() + 1).clear();
        pdfTokenStream.setTokens(tokens);
      }
    }
    if (log.isDebug2()) {
      log.debug2("Transform: " + anyXform);
    }
  }
  
  protected static class PdfStateMachineWorker extends PdfTokenStreamStateMachine {
    
    // Case seems to be constant, so no need to do case-independent match
    // Note: as of 2015-06-30, not finding the DOWNLOADED string in PDFs
    public static final String DOWNLOADED = "Heruntergeladen von: ";
    
    public PdfStateMachineWorker() {
      super(Direction.BACKWARD, log);
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
      if (isShowTextStartsWith(DOWNLOADED)) {
        setState(2);
      }
      else if (isBeginTextObject()) {
        stop();
      }
    }
    
    @Override
    public void state2() throws PdfException {
      if (isBeginTextObject()) {
        setBegin(getIndex());
        setResult(true);
        stop(); 
      }
    }
    
  }
  
}
