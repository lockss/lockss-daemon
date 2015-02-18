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

package org.lockss.plugin.peerj;

import org.lockss.filter.pdf.SimplePdfFilterFactory;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;

public class PeerJPdfFilterFactory extends SimplePdfFilterFactory {

  public static class PeerReviewedVersionPublishedWorker extends PdfTokenStreamWorker {
    
    protected static final String CHARACTERISTIC_STRING =
        "A peer-reviewed version of this preprint was published in PeerJ";
    
    protected boolean result;
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      result = false;
    }
    
    @Override
    public void operatorCallback() throws PdfException {
      if (isShowTextStartsWith(CHARACTERISTIC_STRING)) {
        result = true;
        stop();
      }
    }
    
  }
  
  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetModificationDate();
    PdfUtil.normalizeTrailerId(pdfDocument);
    PeerReviewedVersionPublishedWorker worker = new PeerReviewedVersionPublishedWorker();
    worker.process(pdfDocument.getPage(0).getPageTokenStream());
    if (worker.result) {
      pdfDocument.removePage(0);
    }
  }

}
