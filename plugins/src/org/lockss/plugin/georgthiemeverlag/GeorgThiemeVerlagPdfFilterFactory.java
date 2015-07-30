/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.lockss.filter.pdf.PdfTransform;
import org.lockss.filter.pdf.SimplePdfFilterFactory;
import org.lockss.pdf.*;
import org.lockss.pdf.pdfbox.PdfBoxDocument;
import org.lockss.pdf.pdfbox.PdfBoxDocumentFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;

public class GeorgThiemeVerlagPdfFilterFactory extends SimplePdfFilterFactory {

  private static final String PDFDATE = "pdfDate";
  private static final String PDFUSER = "pdfUser";
  
  private static final PdfDocumentFactory factory = new PdfBoxDocumentFactory() {
    @Override
    public PdfDocument parse(InputStream pdfInputStream) 
        throws IOException, PdfCryptographyException, PdfException {
      try {
        // Parse the input stream
        PDFParser pdfParser = new PDFParser(pdfInputStream);
        pdfParser.parse(); // Probably closes the input stream
        PDDocument pdDocument = pdfParser.getPDDocument();
        processAfterParse(pdDocument);
        return new GTVPdfBoxDocument(pdDocument);
      }
      catch (CryptographyException ce) {
        throw new PdfCryptographyException(ce);
      }
      catch (IOException ioe) {
        throw new PdfException(ioe);
      }
      finally {
        // PDFBox normally closes the input stream, but just in case
        IOUtil.safeClose(pdfInputStream);
      }
    }
  };
  
  public GeorgThiemeVerlagPdfFilterFactory() {
    super(factory);
  }
  
  public GeorgThiemeVerlagPdfFilterFactory(PdfDocumentFactory pdfDocumentFactory) {
    super(factory);
  }
  
  private static final Logger log = Logger.getLogger(GeorgThiemeVerlagPdfFilterFactory.class);
  
  @Override
  public void transform(ArchivalUnit au,
                        PdfDocument pdfDocument)
      throws PdfException {
    
    pdfDocument.unsetModificationDate();
    PdfUtil.normalizeTrailerId(pdfDocument);
    pdfDocument.unsetMetadata();
    if (pdfDocument instanceof GTVPdfBoxDocument) {
      PDDocumentInformation pdDocInfo = ((GTVPdfBoxDocument)pdfDocument).
          getPdDocument().getDocumentInformation();
      if (pdDocInfo.getCustomMetadataValue(PDFDATE) != null) {
        pdDocInfo.setCustomMetadataValue(PDFDATE, null);
      }
      if (pdDocInfo.getCustomMetadataValue(PDFUSER) != null) {
        pdDocInfo.setCustomMetadataValue(PDFUSER, null);
      }
    }
    
    ProvidedByWorkerTransform worker = new ProvidedByWorkerTransform();
    boolean anyXform = false;
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
      worker.process(pdfTokenStream);
      if (worker.getResult()) {
        anyXform = true;
        worker.transform(au, pdfTokenStream);
      }
    }
    if (log.isDebug2()) {
      log.debug2("Transform: " + anyXform);
    }
  }
  
  protected static class ProvidedByWorkerTransform extends PdfTokenStreamStateMachine
  implements PdfTransform<PdfTokenStream> {
    
    // Case seems to be constant, so no need to do case-independent match
    public static final String DOWNLOADED = "Heruntergeladen von: ";
    
    public ProvidedByWorkerTransform() {
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
    
    @Override
    public void transform(ArchivalUnit au, PdfTokenStream pdfTokenStream)
        throws PdfException {
      if (getResult()) {
        List<PdfToken> tokens = pdfTokenStream.getTokens();
        // clear tokens including text markers
        tokens.subList(getBegin(), getEnd() + 1).clear();
        pdfTokenStream.setTokens(tokens);
      }
      else {
        log.warning("Called for transform when no result");
      }
    }
  }
}

class GTVPdfBoxDocument extends PdfBoxDocument {
  
  protected GTVPdfBoxDocument(PDDocument pdDocument) {
    super(pdDocument);
  }

  public PDDocument getPdDocument() {
    return pdDocument;
  }
  
}
