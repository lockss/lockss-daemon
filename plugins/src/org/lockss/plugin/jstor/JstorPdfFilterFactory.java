/*
 * $Id: JstorPdfFilterFactory.java,v 1.1 2014-06-06 18:37:37 alexandraohlson Exp $
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

package org.lockss.plugin.jstor;

import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class JstorPdfFilterFactory extends ExtractingPdfFilterFactory {
  private static final Logger logger = Logger.getLogger(JstorPdfFilterFactory.class);

/*
 * On each page there is a footer that says (equiv of):
 * "This content downloaded from 171.66.236.16 on Tue, 3 Jun 2014 14:34:21 PM"
 * 
 * The token stream looks like this:
 *6533    [operator:BT]
 *6534    [float:5.57]
 *6535    [integer:0]
 *6536    [integer:0]
 *6537    [float:5.57]
 *6538    [float:154.64]
 *6539    [float:-11.13]
 *6540    [operator:Tm]
 *6541    [name:TT6.0]
 *6542    [integer:1]
 *6543    [operator:Tf]
 *6544    [string:"This content downloaded from 171.66.236.16 on Tue, 3 Jun 2014 14:34:21 PM"]
 *6545    [operator:Tj]
 *6546    [operator:ET]
 */
  public static class SimpleDownloadedFromWorkerTransform
      extends PdfTokenStreamWorker
      implements PdfTransform<PdfTokenStream> {

    public static final String DOWNLOADED_FROM = "This content downloaded from ";

    private boolean result;
    
    public SimpleDownloadedFromWorkerTransform() {
      super(Direction.BACKWARD);
    }
    
    @Override
    public void operatorCallback()
        throws PdfException {
      // FIXME 1.60
      if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
          && getTokens().get(getIndex() - 1).getString().startsWith(DOWNLOADED_FROM)) {
        result = true;
        stop();
        getTokens().subList(getIndex() - 1, getIndex() + 1).clear();
      }
    }
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      result = false;
    }
    
    @Override
    public void transform(ArchivalUnit au, PdfTokenStream pdfTokenStream) throws PdfException {
      if (result) {
        pdfTokenStream.setTokens(getTokens());
      }
    }  
    
  }  

  @Override
  public void transform(ArchivalUnit au,
                        PdfDocument pdfDocument)
      throws PdfException {
    SimpleDownloadedFromWorkerTransform worker = new SimpleDownloadedFromWorkerTransform();
    boolean firstPage = true;
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
      worker.process(pdfTokenStream);
      if (firstPage && !worker.result) {
        return; // This PDF needs no processing
      }
      if (worker.result) {
        worker.transform(au, pdfTokenStream);
      }
      firstPage = false;
    }
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetMetadata();
    PdfUtil.normalizeTrailerId(pdfDocument);
  }

}
