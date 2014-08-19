/*
 * $Id: PalgraveBookPdfFilterFactory.java,v 1.1 2014-08-19 21:47:04 thib_gc Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.palgrave;

import java.util.List;

import org.lockss.filter.pdf.SimplePdfFilterFactory;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class PalgraveBookPdfFilterFactory extends SimplePdfFilterFactory {

  private static final Logger logger = Logger.getLoggerWithInitialLevel(PalgraveBookPdfFilterFactory.class.getName(), Logger.LEVEL_DEBUG3);
  
  protected static class Worker extends PdfTokenStreamWorker {
    
    protected boolean result;

    protected int beginIndex;
    
    protected int endIndex;
    
    protected int state;

    public Worker() {
      super(Direction.BACKWARD);
    }
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      this.state = 0;
      this.result = false;
      this.beginIndex = -1;
      this.endIndex = -1;
    }

    @Override
    public void operatorCallback() throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("Worker: initial: " + state);
        logger.debug3("Worker: index: " + getIndex());
        logger.debug3("Worker: operator: " + getOpcode());
      }
      
      switch (state) {
        
        case 0: {
          if (isEndTextObject()) {
            endIndex = getIndex();
            ++state;
          }
        } break;
        
        case 1: {
          if (isShowTextGlyphPositioningStartsWith("Copyright material from www.palgraveconnect.com")) {
            ++state;
          }
          else if (isBeginTextObject()) {
            stop();
          }
        } break;
        
        case 2: {
          if (isBeginTextObject()) {
            result = true;
            beginIndex = getIndex();
            stop();
          }
        } break;
        
        default: {
          throw new PdfException("Invalid state in Worker: " + state);
        }
        
      }
      
      if (logger.isDebug3()) {
        logger.debug3("Worker: final: " + state);
        logger.debug3("Worker: result: " + result);
      }
      
    }

  }
  
  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetMetadata();
    PdfUtil.normalizeTrailerId(pdfDocument);
    
    Worker worker = new Worker();
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      // Pages seem to be made of concatenated token streams, and the
      // target personalization is at the end -- get the last token stream
      List<PdfTokenStream> pdfTokenstreams = pdfPage.getAllTokenStreams();
      PdfTokenStream pdfTokenStream = pdfTokenstreams.get(pdfTokenstreams.size() - 1);
      worker.process(pdfTokenStream);
      if (worker.result) {
        List<PdfToken> pdfTokens = pdfTokenStream.getTokens();
        pdfTokens.subList(worker.beginIndex, worker.endIndex + 1).clear();
        pdfTokenStream.setTokens(pdfTokens);
      }
    }
  }
  
}
