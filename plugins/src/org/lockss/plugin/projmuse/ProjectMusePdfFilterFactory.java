/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
  
  private static final Logger log = Logger.getLogger(ProjectMusePdfFilterFactory.class);
  
  /*
   * Examples:
   * http://muse.jhu.edu/journals/perspectives_on_science/v022/22.4.oberdan.pdf 12/01/14
   */
  public static class FrontPageWorker extends PdfTokenStreamStateMachine {
    
    public static final Pattern ACCESS =
        Pattern.compile("^ *(Access provided by|Accessed) ", Pattern.CASE_INSENSITIVE);
    
    public FrontPageWorker() {
      super(log);
    }
    
    @Override
    public void state0() throws PdfException {
      if (isBeginTextObject()) {
        setState(1);
      }
    }
    
    @Override
    public void state1() throws PdfException {
      if (isShowTextFind(ACCESS) ||
          isShowTextGlyphPositioningFind(ACCESS)) {
        setState(2);
      }
      else if (isEndTextObject()) { 
        setState(0);
      }
    }
    
    @Override
    public void state2() throws PdfException {
      if (isEndTextObject()) {
        setResult(true);
        stop(); 
      }
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
      PdfTokenStreamStateMachine worker = new FrontPageWorker();
      worker.process(pdfDocument.getPage(0).getPageTokenStream());
      if (worker.getResult()) {
        pdfDocument.removePage(0);
      }
    }
    
    PdfUtil.normalizeAllTokenStreams(pdfDocument);
  }
  
}
