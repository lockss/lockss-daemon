/*
 * $Id: EmeraldPdfFilterFactory.java,v 1.4 2013-02-28 01:55:28 thib_gc Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

  protected static final Logger logger = Logger.getLogger(EmeraldPdfFilterFactory.class); 
  
  public static class FrontPageWorker extends PdfTokenStreamWorker {

    protected boolean result;
    
    private int state;
    
    @Override
    public void operatorCallback() throws PdfException {
      switch (state) {

        case 0: case 3: {
          // FIXME 1.60
          if (PdfOpcodes.SAVE_GRAPHICS_STATE.equals(getOpcode())) { ++state; }
          else { stop(); }
        } break;
        
        case 1: case 4: {
          // FIXME 1.60
          if (PdfOpcodes.RESTORE_GRAPHICS_STATE.equals(getOpcode())) { stop(); }
          // FIXME 1.60
          else if (PdfOpcodes.INVOKE_XOBJECT.equals(getOpcode())) { ++state; }
        } break;
        
        case 2: case 5: {
          // FIXME 1.60
          if (PdfOpcodes.RESTORE_GRAPHICS_STATE.equals(getOpcode())) { ++state; }
        } break;
        
        // case 3: see case 0
        
        // case 4: see case 1
        
        // case 5: see case 2
        
        case 6: {
          // FIXME 1.60
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(getOpcode())) { ++state; }
        } break;
        
        case 7: {
          // FIXME 1.60
          if (PdfOpcodes.END_TEXT_OBJECT.equals(getOpcode())) { state = 6; }
          // FIXME 1.60
          if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
              && getTokens().get(getIndex() - 1).getString().startsWith("Downloaded on: ")) { ++state; }
        } break;
        
        case 8: {
          // FIXME 1.60
          if (PdfOpcodes.END_TEXT_OBJECT.equals(getOpcode())) { ++state; }
        } break;

        case 9: {
          // FIXME 1.60
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(getOpcode())) { ++state; }
        } break;
        
        case 10: {
          // FIXME 1.60
          if (PdfOpcodes.END_TEXT_OBJECT.equals(getOpcode())) { state = 9; }
          // FIXME 1.60
          if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
              && getTokens().get(getIndex() - 1).getString().startsWith("Access to this document was granted through an Emerald subscription provided by ")) { ++state; }
        } break;
        
        case 11: {
          // FIXME 1.60
          if (PdfOpcodes.END_TEXT_OBJECT.equals(getOpcode())) {
            result = true;
            stop();
          }
        } break;

        default: {
          throw new PdfException("Invalid state in " + getClass().getName() + ": " + state);
        }
      }
    }
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      this.state = 0;
      this.result = false;
    }
    
  }
  
  public EmeraldPdfFilterFactory() {
    super();
  }
  
  @Override
  public PdfTransform<PdfDocument> getDocumentTransform(ArchivalUnit au,
                                                        OutputStream os) {
    // Override the document-level transform to skip all document-level strings
    return new BaseDocumentExtractingTransform(os) {
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
