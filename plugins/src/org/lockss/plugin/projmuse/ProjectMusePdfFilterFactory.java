/*
 * $Id: ProjectMusePdfFilterFactory.java,v 1.7 2013-03-26 22:48:52 pgust Exp $
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

package org.lockss.plugin.projmuse;

import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class ProjectMusePdfFilterFactory extends SimplePdfFilterFactory {
  
  private static final Logger logger = Logger.getLogger(ProjectMusePdfFilterFactory.class);
  
  
  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument)
      throws PdfException {
    
    ProvidedByWorkerTransform worker = new ProvidedByWorkerTransform();
    boolean removeFirstPage = false;
    
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
      worker.process(pdfTokenStream);
      if (worker.result) {
        worker.transform(au, pdfTokenStream);
        removeFirstPage = true;
      }
    }
    
    if (removeFirstPage) {
      pdfDocument.removePage(0);
    }
    
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetMetadata();
    PdfUtil.normalizeTrailerId(pdfDocument);
    
  }
  
  public static class ProvidedByWorkerTransform
      extends PdfTokenStreamWorker
      implements PdfTransform<PdfTokenStream> {
    
    // Case may vary, so need to do case-independent match
    public static final String PROVIDED_BY_REGEX = "\\s*(?i)Access Provided by(?-i) .*";
    
    private boolean result;
    
    private int state;
    
    private int startIndex;
    
    public ProvidedByWorkerTransform() {
      super(Direction.FORWARD);
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
          // FIXME 1.60
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(getOpcode())) {
            startIndex = getIndex();
            ++state; 
          }
        } break;
        
        case 1: {
          // FIXME 1.60
          if (PdfOpcodes.SHOW_TEXT.equals(getOpcode())
              && getTokens().get(getIndex() - 1).getString().matches(PROVIDED_BY_REGEX)) {
            ++state; 
          } 
          // FIXME 1.60
          else if (PdfOpcodes.SHOW_TEXT_GLYPH_POSITIONING.equals(getOpcode())) {
            PdfToken token = getTokens().get(getIndex() - 1);
            if (token.isArray()) {
              for (PdfToken t : token.getArray()) {
        	if (t.isString() && t.getString().matches(PROVIDED_BY_REGEX)) {
        	  ++state; 
        	}
              }
            }
            // FIXME The following never happens, the operand can only be an array
            else if (token.isString() && token.getString().matches(PROVIDED_BY_REGEX)) {
              ++state; 
            }
          }
          // FIXME 1.60
          else if (PdfOpcodes.END_TEXT_OBJECT.equals(getOpcode())) { 
            state = 0;
          }
        } break;
        
        case 2: {
          // FIXME 1.60
          if (PdfOpcodes.END_TEXT_OBJECT.equals(getOpcode())) {
            result = true;
            stop(); 
          }
        } break;
        
        default: {
          throw new PdfException("Invalid state in ProvidedByWorkerTransform: " + state);
        }
    
      }
    
      if (logger.isDebug3()) {
        logger.debug3("ProvidedByWorkerTransform: final: " + state);
        logger.debug3("ProvidedByWorkerTransform: result: " + result);
      }
      
      if (result) {
        getTokens().subList(startIndex, getIndex()).clear();
      }
    }
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      state = 0;
      result = false;
    }
    
    @Override
    public void transform(ArchivalUnit au,
                          PdfTokenStream pdfTokenStream)
        throws PdfException {
      if (result) {
        pdfTokenStream.setTokens(getTokens());
      }
    }
    
  }
}
