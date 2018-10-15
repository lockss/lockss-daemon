/*
 * $Id:$
 */

/*

 Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.iwap;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.lockss.filter.pdf.ExtractingPdfFilterFactory;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;


/**
 * A pdf filter removes the "Downloaded from... IP:.... on.... BT-ET section
 * 
 * These are located in content stream
 * and located on each page of the document
 * @author alexohlson
 *
 */
public class IwapPdfFilterFactory extends ExtractingPdfFilterFactory {
  private static final Logger log = Logger.getLogger(IwapPdfFilterFactory.class);

  private static final String DOWNLOAD_REGEX_STRING = "^Downloaded from ";
  private static final Pattern DOWNLOAD_PATTERN = Pattern.compile(DOWNLOAD_REGEX_STRING);
  // It's all in one BT--ET, no need to find the rest
  
  private static void doBaseTransforms(PdfDocument pdfDocument) 
      throws PdfException {
	    pdfDocument.unsetCreationDate();
	    pdfDocument.unsetModificationDate();
	    pdfDocument.unsetMetadata();
	    pdfDocument.unsetCreator();
	    pdfDocument.unsetLanguage();
	    pdfDocument.unsetProducer();
	    pdfDocument.unsetAuthor();
	    pdfDocument.unsetTitle();
	    pdfDocument.unsetSubject();
	    pdfDocument.unsetKeywords();
        PdfUtil.normalizeTrailerId(pdfDocument);
     }

  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument) 
      throws PdfException {
    //log.setLevel("debug3");
    
    doBaseTransforms(pdfDocument);
    IwapDownloadedFromStateMachine worker = new IwapDownloadedFromStateMachine(DOWNLOAD_PATTERN);
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      List<PdfTokenStream> pdfTokenStreams = pdfPage.getAllTokenStreams();
      for (Iterator<PdfTokenStream> iter = pdfTokenStreams.iterator(); iter.hasNext();) {
        PdfTokenStream nextTokStream = iter.next();
        /*
         * NOTE - removing download strip was insufficient. Token processing
         * caused a difference in the token between settoken stream and not so 
         * do settoken on all streams so they match.
         */
        worker.process(nextTokStream);
        List<PdfToken> pdfTokens = nextTokStream.getTokens();
        if (worker.getResult()) {
          pdfTokens.subList(worker.getBegin(), worker.getEnd() + 1).clear();
          // Can't break - must process all tokens on the page to reset whether changed or not
          //break; // out of the stream loop, go on to next page
        }
        nextTokStream.setTokens(pdfTokens);
      }
    }
  }

  /*
   * HERE ARE THE WORKER CLASSES THAT KNOW HOW TO REMOVE THINGS FROM PDF DOCUMENTS
   */

  public static class IwapDownloadedFromStateMachine extends PdfTokenStreamStateMachine {

    /* set when this worker is created */
    public static Pattern DOWNLOADED_FROM_PATTERN;

    // The footer is close to the bottom of each page
    public IwapDownloadedFromStateMachine(Pattern downloadPattern) {
      super(Direction.BACKWARD);
      DOWNLOADED_FROM_PATTERN = downloadPattern;
    }

    @Override
    public void state0() throws PdfException {
      if (isEndTextObject()) {
        setEnd(getIndex());
        setState(1);
      }
    }

    @Override
    public void state1() throws PdfException {
      if (isShowTextFind(DOWNLOADED_FROM_PATTERN)) {
        setState(2);
      }
      else if (isBeginTextObject()) { // not the BT-ET we were looking for
        //COULD STOP HERE IF assuming only one bt/et in the stream we want
        setState(0);
      }
    }

    @Override
    public void state2() throws PdfException {
      if (isBeginTextObject()) {  // we need to remove this BT-ET chunk
        setBegin(getIndex());
        setResult(true);
        stop(); // found what we needed, stop processing this page
      }
    }
  }

}
