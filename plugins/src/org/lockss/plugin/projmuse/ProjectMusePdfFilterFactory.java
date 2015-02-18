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

package org.lockss.plugin.projmuse;

import java.util.regex.Pattern;

import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class ProjectMusePdfFilterFactory extends ExtractingPdfFilterFactory {
  
  private static final Logger logger = Logger.getLogger(ProjectMusePdfFilterFactory.class);
  
  /*
   * FIXME 1.67: extend PdfTokenStreamStateMachine instead
   */
  /*
   * Examples:
   * http://muse.jhu.edu/journals/perspectives_on_science/v022/22.4.oberdan.pdf 12/01/14
   */
  public static class FrontPageWorker extends PdfTokenStreamWorker {
    
    public static final Pattern ADDITIONAL_INFORMATION =
        Pattern.compile("For additional information about this", Pattern.CASE_INSENSITIVE);
    
    public static final Pattern PROVIDED_BY =
        Pattern.compile("Access provided by", Pattern.CASE_INSENSITIVE);
    
    protected boolean result;
    
    protected int state;
    
    public FrontPageWorker() {
      super(Direction.FORWARD);
    }
    
    @Override
    public void operatorCallback() throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("FrontPageWorker: initial: " + state);
        logger.debug3("FrontPageWorker: index: " + getIndex());
        logger.debug3("FrontPageWorker: operator: " + getOpcode());
      }
      
      switch (state) {
        
        case 0: {
          if (isBeginTextObject()) {
            ++state; 
          }
        } break;
        
        case 1: {
          // FIXME 1.67: isShowTextContains/isShowTextGlyphPositioningContains
          if (isShowTextFind(ADDITIONAL_INFORMATION) || isShowTextGlyphPositioningFind(ADDITIONAL_INFORMATION)) {
            ++state;
          }
          else if (isEndTextObject()) { 
            state = 0;
          }
        } break;
        
        case 2: {
          // FIXME 1.67: isShowTextContains/isShowTextGlyphPositioningContains
          if (isShowTextFind(PROVIDED_BY) || isShowTextGlyphPositioningFind(PROVIDED_BY)) {
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
          throw new PdfException("Invalid state in FrontPageWorker: " + state);
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
      state = 0;
      result = false;
    }
    
  }
  
  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetCreator();
    pdfDocument.unsetMetadata();
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetProducer();
    PdfUtil.normalizeTrailerId(pdfDocument);
    
    if (pdfDocument.getNumberOfPages() > 0) {
      FrontPageWorker worker = new FrontPageWorker();
      worker.process(pdfDocument.getPage(0).getPageTokenStream());
      if (worker.result) {
        pdfDocument.removePage(0);
      }
    }
    
    PdfUtil.normalizeAllTokenStreams(pdfDocument);
  }
  
}
