/*
 * $Id$
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

package org.lockss.plugin.hindawi;

import org.lockss.filter.pdf.ExtractingPdfFilterFactory;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class HindawiPdfFilterFactory extends ExtractingPdfFilterFactory {

  private static final Logger logger = Logger.getLogger(HindawiPdfFilterFactory.class);
  
  protected static class LastPageRecognizer extends PdfTokenStreamWorker {
    
    protected boolean result;
    
    protected int state;
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      this.result = false;
      this.state = 0;
    }
    
    @Override
    public void operatorCallback() throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("LastPageRecognizer: initial: " + state);
        logger.debug3("LastPageRecognizer: index: " + getIndex());
        logger.debug3("LastPageRecognizer: operator: " + getOpcode());
      }

      switch (state) {
      
        case 0: {
          if (isBeginTextObject()) {
            ++state;
          }
        } break;
        
        case 1: {
          if (isShowTextGlyphPositioningEquals("Submit your manuscripts at")) {
            ++state;
          }
          else if (isEndTextObject()) {
            state = 0;
          }
        } break;
        
        case 2: {
          if (isShowTextGlyphPositioningEquals("http://www.hindawi.com")) {
            ++state;
          }
          else if (isEndTextObject()) {
            state = 0;
          }
        } break;
        
        case 3: {
          if (isEndTextObject()) {
            result = true;
            stop();
          }
        } break;
        
        default: {
          throw new PdfException("Invalid state in LastPageRecognizer: " + state);
        }
    
      }
    
      if (logger.isDebug3()) {
        logger.debug3("LastPageRecognizer: final: " + state);
        logger.debug3("LastPageRecognizer: result: " + result);
      }
    }
    
  }
  
  @Override
  public void transform(ArchivalUnit au,
                        PdfDocument pdfDocument)
      throws PdfException {
    // Strip ancillary data
    pdfDocument.unsetMetadata();
    pdfDocument.unsetAuthor();
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetCreator();
    pdfDocument.unsetKeywords();
    pdfDocument.unsetLanguage();
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetProducer();
    pdfDocument.unsetSubject();
    pdfDocument.unsetTitle();
    PdfUtil.normalizeTrailerId(pdfDocument);
    
    // Remove the generated last page if necessary
    LastPageRecognizer worker = new LastPageRecognizer();
    int lastPageIndex = pdfDocument.getNumberOfPages() - 1;
    worker.process(pdfDocument.getPage(lastPageIndex).getPageTokenStream());
    if (worker.result) {
      logger.debug2("Removing the last page");
      pdfDocument.removePage(lastPageIndex);
    }
    else {
      logger.debug2("Not removing the last page");
    }
  }
  
}
