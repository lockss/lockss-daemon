/*
 * $Id: AARSScrapingPdfFilterFactory.java,v 1.1 2014-12-19 21:18:20 alexandraohlson Exp $
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

package org.lockss.plugin.atypon;

import java.util.List;

import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;



/**
 * A pdf filter that handles the more challenging Atypon children where pdfplus
 * files have non-substantive changes (annotation uris, ordering, etc). In this
 * case scrape out the content text for comparison.
 * @author alexohlson
 *
 */
public class AARSScrapingPdfFilterFactory extends BaseAtyponScrapingPdfFilterFactory {
  private static final Logger logger = Logger.getLogger(AARSScrapingPdfFilterFactory.class);


  // TODO 1.67 - extend the PdfTokenStreamStateMachine, avoid duplicate work 
  public static class ARJDownloadedFromWorker extends PdfTokenStreamWorker {

    public static final String DOWNLOADED_FROM = "Downloaded from www.ajronline.org";

    private boolean result;
    
    private int beginIndex;
    
    private int endIndex;
    
    private int state;
    
    // The footer is close to the bottom of each page
    public ARJDownloadedFromWorker() {
      super(Direction.BACKWARD);
    }
    
    @Override
    public void operatorCallback() throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("ARJ Worker: initial: " + state);
        logger.debug3("ARJ Worker: index: " + getIndex());
        logger.debug3("ARJ Worker: operator: " + getOpcode());
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
             //COULD STOP HERE IF assuming only one bt/et in the stream we want
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
          throw new PdfException("Invalid state in ARJ Worker: " + state);
        }
        
      }
      
      if (logger.isDebug3()) {
        logger.debug3("ARJ Worker: final: " + state);
        logger.debug3("ARJ Worker: result: " + result);
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
  /*
   * Do the BaseAtypon version, but then also add in a per-page check to 
   * remove the side-printed "downloaded from" note
   */
  @Override
  public void transform(ArchivalUnit au,
                        PdfDocument pdfDocument)
      throws PdfException {
    BaseAtyponPdfFilterFactory.doBaseTransforms(pdfDocument);
    
    ARJDownloadedFromWorker worker = new ARJDownloadedFromWorker();
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      // Pages seem to be made of concatenated token streams, and the
      // target personalization is at the end -- get the last token stream
      // NOTE - if this starts to fail - loop over all token streams
      // and then break (continue) if we find it
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
