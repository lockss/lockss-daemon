/*
 * $Id:$
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

package org.lockss.plugin.pub2web.asm;

import java.util.Iterator;
import java.util.List;
import org.lockss.filter.pdf.SimplePdfFilterFactory;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;


/**
 * A pdf filter removes the "Downloaded from... IP:.... on.... BT-ET sections
 * BT (Downloaded from www.asmscience.org by) ET
 * BT (IP:  156.56.241.164) ET
 * BT (On: Sun, 12 Apr 2015 05:59:48) ET
 * 
 * These are located in stream0, defined in the resource dictionary, followed by the content stream
 * and located on each page of the document
 * @author alexohlson
 *
 */
public class AsmPdfFilterFactory extends SimplePdfFilterFactory {
  private static final Logger log = Logger.getLogger(AsmPdfFilterFactory.class);

  private static final String DOWNLOAD_STRING = "Downloaded from ";
  private static final String ONDATE_STRING = "On: ";
  //private static final String temp= "^On: Sun, 12 Apr";
  
  private static void doBaseTransforms(PdfDocument pdfDocument) 
      throws PdfException {
       pdfDocument.unsetCreationDate();
       pdfDocument.unsetModificationDate();
       pdfDocument.unsetMetadata();
       PdfUtil.normalizeTrailerId(pdfDocument);
     }

  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument) 
      throws PdfException {
    //log.setLevel("debug3");
    
    doBaseTransforms(pdfDocument);

    ASMDownloadedFromStateMachine worker = new ASMDownloadedFromStateMachine();

    for (PdfPage pdfPage : pdfDocument.getPages()) {
      log.debug3("BEGIN PAGE");
      List<PdfTokenStream> pdfTokenStreams = pdfPage.getAllTokenStreams();
      for (Iterator<PdfTokenStream> iter = pdfTokenStreams.iterator(); iter.hasNext();) {
        PdfTokenStream nextTokStream = iter.next();
        worker.process(nextTokStream);      
        if (worker.getResult()) {
          List<PdfToken> pdfTokens = nextTokStream.getTokens();
          log.debug3("removing from " + worker.getBegin() + " thru " + worker.getEnd());        
          pdfTokens.subList(worker.getBegin(), worker.getEnd() + 1).clear();
          nextTokStream.setTokens(pdfTokens);
          break; // out of the stream loop, go on to next page
        }
      }
      log.debug3("END PAGE");
    }
  }

  /*
   * The state machine to remove the BT-ET pairs that are variable
   * 
BT
1 0 0 1 230.42 34 Tm
/F1 8 Tf
0.86275 0.86275 0.86275 rg
(Downloaded from www.asmscience.org by)Tj
0 g
ET
BT
1 0 0 1 271.08 22 Tm
/F1 8 Tf
0.86275 0.86275 0.86275 rg
(IP:  156.56.241.164)Tj
0 g
ET
BT
1 0 0 1 250.63 10 Tm
/F1 8 Tf
0.86275 0.86275 0.86275 rg
(On: Sun, 12 Apr 2015 05:59:48)Tj
0 g
ET
   */

  public static class ASMDownloadedFromStateMachine extends PdfTokenStreamStateMachine {


    public ASMDownloadedFromStateMachine() {
      // the default direction is FORWARD, but make the logger the one for this class
      super(log);
    }
    
    
    @Override
    public void state0() throws PdfException {
      if (isBeginTextObject()) {
        setBegin(getIndex());
        setState(1);
      }
    } 

    // we are at a BT....if it's the "Downloaded by" then move to state 2, otherwise, keep looking...
    @Override
    public void state1() throws PdfException {
      if (isShowTextStartsWith(DOWNLOAD_STRING)) {
        log.debug3("state1 - we have a BT with the Downloaded by");
        setState(2);
      }
      else if (isBeginTextObject()) { // not the initial BT-ET we were looking for
        setState(0);
      }
    }

    // we have our BT - downloaded, now look at following BTs
    @Override
    public void state2() throws PdfException {
      log.debug3("state2 - now looking for the next BT");
      if (isBeginTextObject()) {  
        setState(3);
      }      
    }

    // we're got a new BT, is this the final block
    @Override
    public void state3() throws PdfException {
      log.debug3("state3 - we have a 2nd BT, checking text");
      if (isShowTextStartsWith(ONDATE_STRING)) {
        setState(4);
      }
      // otherwise just keep looking - may go over a BT-ET block set  
    }

    // we found our final bit of text, now find the needed ET
    @Override
    public void state4() throws PdfException {
      log.debug3("state4 - we have all our text, looking for the next end");
      if (isEndTextObject()) {  // we need to remove this BT-ET chunk
        log.debug3("in state4 at final ET...prepare to remove");
        setEnd(getIndex());
        setResult(true);
        stop(); // found what we needed, stop processing this page
      }      
    }
    

  }  
  

}
