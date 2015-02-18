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

package org.lockss.plugin.iop;

import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
import org.lockss.plugin.*;

/**
 * <p>
 * A synthetic front page was added to PDF files. Because the server-side
 * stamping process shuffles the object graph compared to the original file, we
 * have no choice but to use an extracting filter.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 */
public class IOPSciencePdfFilterFactory extends ExtractingPdfFilterFactory {

  protected static class RecognizeFirstPageWorker extends PdfTokenStreamWorker {
    
    protected boolean result;
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      this.result = false;
    }
    
    @Override
    public void operatorCallback() throws PdfException {
      if (isShowTextStartsWith("This article has been downloaded from IOPscience. Please scroll down to see the full text article.")) {
        result = true;
        stop();
      }
    }
    
  }
  
  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
    RecognizeFirstPageWorker worker = new RecognizeFirstPageWorker();
    worker.process(pdfDocument.getPage(0).getPageTokenStream());
    if (worker.result) {
      pdfDocument.removePage(0);
    }
  }
  
}
