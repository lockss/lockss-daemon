/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
