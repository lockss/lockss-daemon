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

package org.lockss.plugin.emerald;

import java.io.*;

import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/**
 * <p>
 * A new-style PDF filter factory for Emerald Group Publishing
 * content. 
 * </p>
 * <p>
 * When synthetic front pages were introduced, the major PDF version
 * of most documents was upped and most document-level metadata was
 * stripped. The former makes it necessary to use a scraping filter
 * instead of the older PDF-to-PDF filter.
 * </p>
 */
public class EmeraldPdfFilterFactory extends ExtractingPdfFilterFactory {

  private static final Logger logger = Logger.getLogger(EmeraldPdfFilterFactory.class);
  
  /*
   * FIXME 1.67: extend PdfTokenStreamStateMachine
   */
  public static class FrontPageWorker extends PdfTokenStreamWorker {

    protected boolean result;
    
    private int state;
    
    @Override
    public void operatorCallback() throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("FrontPageWorker: initial: " + state);
        logger.debug3("FrontPageWorker: index: " + getIndex());
        logger.debug3("FrontPageWorker: operator: " + getOpcode());
      }
      
      switch (state) {

        case 0: case 3: {
          if (isSaveGraphicsState()) {
            ++state;
          }
          else {
            stop();
          }
        } break;
        
        case 1: case 4: {
          if (isRestoreGraphicsState()) {
            stop();
          }
          else if (isInvokeXObject()) {
            ++state; 
          }
        } break;
        
        case 2: case 5: {
          if (isRestoreGraphicsState()) {
            ++state;
          }
        } break;
        
        // case 3: see case 0
        
        // case 4: see case 1
        
        // case 5: see case 2
        
        case 6: {
          if (isBeginTextObject()) {
            ++state;
          }
        } break;
        
        case 7: {
          if (isShowTextStartsWith("Downloaded on: ")) {
            ++state;
          }
          else if (isEndTextObject()) {
            state = 6;
          }
        } break;
        
        case 8: {
          if (isEndTextObject()) {
            ++state;
          }
        } break;

        case 9: {
          if (isBeginTextObject()) {
            ++state;
          }
        } break;
        
        case 10: {
          if (isShowTextStartsWith("Access to this document was granted through an Emerald subscription provided by ")) {
            ++state;
          }
          else if (isEndTextObject()) {
            state = 9;
          }
        } break;
        
        case 11: {
          if (isEndTextObject()) {
            result = true;
            stop();
          }
        } break;

        default: {
          throw new PdfException("Invalid state in " + getClass().getName() + ": " + state);
        }
      }
      
      if (logger.isDebug3()) {
        logger.debug3("FrontPageWorker: final: " + state);
        logger.debug3("FrontPageWorker: result: " + result);
      }
    }
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      this.state = 0;
      this.result = false;
    }
    
  }
  
  @Override
  public PdfTransform<PdfDocument> getDocumentTransform(ArchivalUnit au,
                                                        OutputStream os) {
    // Override the document-level transform to skip all document-level strings
    return new BaseDocumentExtractingTransform(os) {
      /*
       * FIXME 1.67: override outputDocumentInformation() instead
       */
      @Override
      public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
        this.au = au;
        this.pdfDocument = pdfDocument;
        for (PdfPage pdfPage : pdfDocument.getPages()) {
          outputPage(pdfPage);
        }
      }
      @Override
      public void outputPage(PdfPage pdfPage) throws PdfException {
        try {
          super.outputPage(pdfPage);
        } catch (java.lang.Error error) {
          if (error.getMessage().startsWith("TIFFFaxDecoder")) {
            // In PDFBox 1.6.0, TIFFFaxDecoder throws java.lang.Error
            // in various unimplemented cases. I'm not kidding.
            logger.debug("TIFFFaxDecoder threw java.lang.Error, skipping page", error);
          }
          else {
            throw error;
          }
        }
      }
    };
  }

  @Override
  public void transform(ArchivalUnit au,
                        PdfDocument pdfDocument)
      throws PdfException {
    FrontPageWorker frontPageWorker = new FrontPageWorker();
    frontPageWorker.process(pdfDocument.getPage(0).getPageTokenStream());
    if (frontPageWorker.result) {
      pdfDocument.removePage(0);
    }
  }
  
}
