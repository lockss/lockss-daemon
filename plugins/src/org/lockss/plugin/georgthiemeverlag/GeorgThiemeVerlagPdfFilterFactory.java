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

package org.lockss.plugin.georgthiemeverlag;

import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.exceptions.InvalidPasswordException;
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
        
        // Trivial decryption if encrypted without a password
        if (pdDocument.isEncrypted()) {
          pdDocument.decrypt("");
        }
        
        return new GTVPdfBoxDocument(pdDocument);
      }
      catch (CryptographyException ce) {
        throw new PdfCryptographyException(ce);
      }
      catch (IOException ioe) {
        throw new PdfException(ioe);
      }
      catch (Exception exc) {
        // FIXME 1.67
        // InvalidPasswordException thrown by PDFBox <= 1.8.5 but not >= 1.8.6
        // This hack will avoid 1.66 plugins being incompatible with 1.67 
        if (exc instanceof InvalidPasswordException) {
          throw new PdfCryptographyException(exc);
        }
        else {
          throw new PdfException(exc);
        }
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

  private static final Logger logger = 
      Logger.getLogger(GeorgThiemeVerlagPdfFilterFactory.class);
  
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
    
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
      worker.process(pdfTokenStream);
      if (worker.result) {
        worker.transform(au, pdfTokenStream);
      }
    }
    
  }

  public static class ProvidedByWorkerTransform extends PdfTokenStreamWorker
  implements PdfTransform<PdfTokenStream> {
    
    // Case seems to be constant, so no need to do case-independent match
    public static final String DOWNLOADED = "Heruntergeladen von: ";
    
    private boolean result;
    
    private int state;
    
    private int startIndex;
    private int endIndex;
    
    public ProvidedByWorkerTransform() {
      super(Direction.BACKWARD);
    }
    
    @Override
    public void transform(ArchivalUnit au,
        PdfTokenStream pdfTokenStream)
            throws PdfException {
      if (result) {
        pdfTokenStream.setTokens(getTokens());
      }
    }
    
    @Override
    public void operatorCallback()
        throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("ProvidedByWorkerTransform: initial: " + state);
        logger.debug3("ProvidedByWorkerTransform: index: " + getIndex());
        logger.debug3("ProvidedByWorkerTransform: operator: " + getOpcode());
      }
      
      switch (state) {
        case 0: {
          if (isEndTextObject()) {
            endIndex = getIndex();
            ++state;
          }
        } break;
        
        case 1: {
          if (isShowTextStartsWith(DOWNLOADED)) {
            ++state;
          }
          else if (isBeginTextObject()) {
            stop();
          }
        } break;
        
        case 2: {
          if (isBeginTextObject()) {
            startIndex = getIndex();
            result = true;
            stop();
          }
        } break;
        
        default: {
          throw new PdfException("Invalid state in ProvidedByWorkerTransform: " + state);
        }
        
      }
      
      if (result) {
        getTokens().subList(startIndex, endIndex).clear();
      }
    }
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      state = 0;
      result = false;
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
