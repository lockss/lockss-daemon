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

package org.lockss.plugin.jstor;

import java.util.List;

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
 *  may or may not be the final BT-ET chunk, eg may have
 *....two additional similar chunks for "All use subject to " and "JSTOR terms and conditions"
 *...and page ends with...
 *6599    [operator:Q]
 *6600    [operator:Q]
 */
  public static class JstorDownloadedFromWorker extends PdfTokenStreamWorker {

    public static final String DOWNLOADED_FROM = "This content downloaded from ";

    private boolean result;
    
    private int beginIndex;
    
    private int endIndex;
    
    private int state;
    
    // The footer is close to the bottom of each page
    public JstorDownloadedFromWorker() {
      super(Direction.BACKWARD);
    }
    
    @Override
    public void operatorCallback() throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("Jstor Worker: initial: " + state);
        logger.debug3("Jstor Worker: index: " + getIndex());
        logger.debug3("Jstor Worker: operator: " + getOpcode());
      }

      switch (state) {

        case 0: {
          if (isEndTextObject()) {
            endIndex = getIndex();
            ++state;
          } //If not, just continue looking until you find one.
        } break;
        
        case 1: { // We have found an ET object
          if (isShowTextStartsWith(DOWNLOADED_FROM)) {
            ++state; // It's the one we want
          }
          else if (isBeginTextObject()) { // not the BT-ET we were looking for
             --state; //reset search; keep looking
          }
        } break;
        
        case 2: {
          if (isBeginTextObject()) {  // we need to remove this BT-ET chunk
            beginIndex = getIndex();
            result = true;
            stop(); // found what we needed, stop processing this page
          }
        } break;
        
        default: {
          throw new PdfException("Invalid state in Jstor Worker: " + state);
        }
        
      }
      
      if (logger.isDebug3()) {
        logger.debug3("Jstor Worker: final: " + state);
        logger.debug3("Jstor Worker: result: " + result);
      }
      
    }
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      result = false;
      state = 0;
      beginIndex = endIndex = -1;
    }
    
  }  

  @Override
  public void transform(ArchivalUnit au,
                        PdfDocument pdfDocument)
      throws PdfException {
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetMetadata();
    PdfUtil.normalizeTrailerId(pdfDocument);

    JstorDownloadedFromWorker worker = new JstorDownloadedFromWorker();
    boolean firstPage = true;
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
      worker.process(pdfTokenStream);
      if (firstPage && !worker.result) {
        return; // This PDF needs no further processing
      }
      if (worker.result) {
        List<PdfToken> tokens = pdfTokenStream.getTokens();
        logger.debug3("REMOVE FROM: " + worker.beginIndex + " TO: " + worker.endIndex);
        tokens.subList(worker.beginIndex, worker.endIndex + 1).clear(); // The +1 is normal substring/sublist indexing (upper bound is exclusive)
        pdfTokenStream.setTokens(tokens);
      }
      firstPage = false;
    }
  }

}
