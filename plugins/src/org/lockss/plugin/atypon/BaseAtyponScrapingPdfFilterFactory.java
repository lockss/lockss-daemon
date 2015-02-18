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

package org.lockss.plugin.atypon;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.lockss.filter.pdf.ExtractingPdfFilterFactory;
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
public class BaseAtyponScrapingPdfFilterFactory extends ExtractingPdfFilterFactory {
  private static final Logger log = Logger.getLogger(BaseAtyponScrapingPdfFilterFactory.class);
  
  private static final String DEFAULT_CITED_BY_STRING = "This article has been cited by:";
  private static final String DEFAULT_DOWNLOAD_REGEX = "^Downloaded from ";
  private static final Pattern DEFAULT_DOWNLOAD_PATTERN = Pattern.compile(DEFAULT_DOWNLOAD_REGEX);


  /*
   * Many Atypon pdf files have the CreationDate and ModDate and the two ID numbers in the trailer
   * vary from collection to collection. Filter them out to avoid incorrect hash failures.
   * This is a scraping filter to it also returns just the actual pdf content and not the layout.
   */
  @Override
  public void transform(ArchivalUnit au,
      PdfDocument pdfDocument)
          throws PdfException {
    if (doRemoveAllDocumentInfo()) {
      BaseAtyponPdfFilterFactory.doAllBaseTransforms(pdfDocument);
    } else {
      BaseAtyponPdfFilterFactory.doBaseTransforms(pdfDocument);
    }
    //remove any end pages before stepping through to remove download strips
    if (doRemoveCitedByPage()) {
      removeCitedByPage(pdfDocument);
    }
    if (doRemoveDownloadStrip()) {
      removeDownloadStrip(pdfDocument);
    }
  }

  /* this also exists in BaseAtyponPdfFilterFactory for those children that
   * extend that functionality instead of this one.
   * by default just do basic stuff - dates/metadata
   */
  public boolean doRemoveAllDocumentInfo() {
    return false;
  }

  /*
   * CITED BY PAGE(S): at end of document
   *  
   * Default behavior for child plugins is NOT to try to  
   * remove a citation page tacked to the end of the pdf document
   * A child plugin can override this to turn it on
   */
  public boolean doRemoveCitedByPage() {
    return false;    
  }
  /* and set the correct string to use for this publisher if this default not right */
  public String getCitedByString() {
    return DEFAULT_CITED_BY_STRING;
  }

  /*
   * Example URL: http://arc.aiaa.org/doi/pdfplus/10.2514/3.59603
   * and http://www.ajronline.org/doi/pdfplus/10.2214/AJR.13.10940
   */
  protected void removeCitedByPage(PdfDocument pdfDocument)
      throws PdfException {
    CitedByStateMachine worker = new CitedByStateMachine(getCitedByString());
    // for each page in this document, starting at the last one
    log.debug3("number of pages in document is: " + pdfDocument.getNumberOfPages());
    page_loop: for (int p = pdfDocument.getNumberOfPages() - 1 ; p >= 0 ; --p) {
      log.debug3("working on page " + p);
      List<PdfTokenStream> pdfTokenStreams = pdfDocument.getPage(p).getAllTokenStreams();
      //for each stream on this page
      for (Iterator<PdfTokenStream> iter = (pdfTokenStreams).iterator(); iter.hasNext();) {
        PdfTokenStream nextTokStream = iter.next();
        worker.process(nextTokStream);
        if (worker.getResult()) {
          // we are on the page that has the "This article has been cited by:"
          //remove pages in reverse order to ensure consistent numbering
          for (int r = pdfDocument.getNumberOfPages() - 1 ; r >= p ; --r) {
            pdfDocument.removePage(r);
          }
          // break out of the page loop; you're done!
          break page_loop;
        }
      }
    }
  }

  /*
   * DOWNLOAD STRIP ALONG EDGE OF EACH PAGE
   * 
   * Default behavior for child plugins is NOT to try to  
   * remove a downloaded by strip along the edge of every page
   * A child plugin can override this to turn it on
   */
  public boolean doRemoveDownloadStrip() {
    return false;    
  }
  /* and set the correct string to use for this publisher */
  public Pattern getDownloadStripPattern() {
    return DEFAULT_DOWNLOAD_PATTERN;
  }

  /*
   * Example URL: http://arc.aiaa.org/doi/pdfplus/10.2514/3.59603
   * and http://www.ajronline.org/doi/pdfplus/10.2214/AJR.13.10940
   */
  protected void removeDownloadStrip(PdfDocument pdfDocument) 
      throws PdfException {
    AtyponDownloadedFromStateMachine worker = new AtyponDownloadedFromStateMachine(getDownloadStripPattern());
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      List<PdfTokenStream> pdfTokenStreams = pdfPage.getAllTokenStreams();
      //PdfTokenStream pdfTokenStream = pdfTokenstreams.get(pdfTokenstreams.size() - 1);
      for (Iterator<PdfTokenStream> iter = pdfTokenStreams.iterator(); iter.hasNext();) {
        PdfTokenStream nextTokStream = iter.next();
        worker.process(nextTokStream);      
        if (worker.getResult()) {
          List<PdfToken> pdfTokens = nextTokStream.getTokens();
          pdfTokens.subList(worker.getBegin(), worker.getEnd() + 1).clear();
          nextTokStream.setTokens(pdfTokens);
          break; // out of the stream loop, go on to next page
        }
      }
    }
  }

  /*
   * HERE ARE THE WORKER CLASSES THAT KNOW HOW TO REMOVE THINGS FROM PDF DOCUMENTS
   */

  public static class AtyponDownloadedFromStateMachine extends PdfTokenStreamStateMachine {

    /* set when this worker is created */
    public static Pattern DOWNLOADED_FROM_PATTERN;

    // The footer is close to the bottom of each page
    public AtyponDownloadedFromStateMachine(Pattern downloadPattern) {
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

  /*
   * Example URL: http://arc.aiaa.org/doi/pdfplus/10.2514/3.59603
   * and http://www.ajronline.org/doi/pdfplus/10.2214/AJR.13.10940
   */
  
  public static class CitedByStateMachine extends PdfTokenStreamStateMachine {

    /* set when this worker is created */
    static private String citedByString;

    /* a version of the constructor to set the search string  - in the FORWARD DIRECTION*/
    public CitedByStateMachine(String searchString) {
      super();
      citedByString = searchString;
    }
    
    @Override
    public void state0() throws PdfException {
      if (isShowTextGlyphPositioningEquals(citedByString)) {
        setResult(true);
        stop();
      }
    } 

  }

}
