/*
 * $Id: SEGPdfFilterFactory.java,v 1.2 2014-11-01 00:17:13 thib_gc Exp $
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

package org.lockss.plugin.atypon.seg;

import java.io.*;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.lockss.filter.pdf.ExtractingPdfFilterFactory;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class SEGPdfFilterFactory extends ExtractingPdfFilterFactory {

  public static class Worker extends PdfTokenStreamWorker {

    private static final Logger logger = Logger.getLogger(Worker.class);
    
    public static final String CITED_BY_STRING = "This article has been cited by:";
    
    public static final Pattern DOWNLOADED_PATTERN =
        Pattern.compile("^Downloaded \\d+/\\d+/\\d+ to \\d+\\.\\d+\\.\\d+\\.\\d+", Pattern.CASE_INSENSITIVE);
    
    public int state;
    public boolean resultCitedBy;
    public boolean resultDownloaded;
    public int beginDownloaded;
    public int endDownloaded;
    
    @Override
    public void setUp() throws PdfException {
      this.state = 0;
      this.resultCitedBy = false;
      this.resultDownloaded = false;
      this.beginDownloaded = -1;
      this.endDownloaded = -1;
    }
    
    @Override
    public void operatorCallback() throws PdfException {
      logger.debug3("initial: " + state);
      logger.debug3("index: " + getIndex());
      logger.debug3("operator: " + getOpcode());
      switch (state) {
        case 0: {
          if (isBeginTextObject()) {
            beginDownloaded = getIndex();
            ++state;
          }
        } break;
        case 1: {
          if (isShowTextGlyphPositioningEquals(CITED_BY_STRING)) {
            resultCitedBy = true;
            stop();
          }
          else if (isShowTextFind(DOWNLOADED_PATTERN)) {
            ++state;
          }
          else if (isEndTextObject()) {
            stop(); // Both paths are only on the first BT..ET of the stream
          }
        } break;
        case 2: {
          if (isEndTextObject()) {
            resultDownloaded = true;
            endDownloaded = getIndex();
            stop();
          }
        } break;
        default: {
          throw new PdfException("Illegal state: " + state);
        }
      }
      logger.debug3("final: " + state);
      logger.debug3("result: " + resultDownloaded);
    }
    
  }
  
  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
    // Metadata completely rewritten when file is watermarked, can't compare to original
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetMetadata();
    pdfDocument.unsetCreator();
    pdfDocument.unsetProducer();
    pdfDocument.unsetAuthor();
    pdfDocument.unsetTitle();
    pdfDocument.unsetSubject();
    pdfDocument.unsetKeywords();
    
    Worker worker = new Worker();
    page_loop: for (int i = 0 ; i < pdfDocument.getNumberOfPages() ; ++i) {
      PdfPage pdfPage = pdfDocument.getPage(i);
      stream_loop: for (PdfTokenStream pdfTokenStream : pdfPage.getAllTokenStreams()) {
        worker.process(pdfTokenStream);
        if (worker.resultCitedBy) {
          for (int j = pdfDocument.getNumberOfPages() - 1 ; j >= i ; --j) {
            pdfDocument.removePage(j);
          }
          break page_loop;
        }
        if (worker.resultDownloaded) {
          List<PdfToken> tokens = pdfTokenStream.getTokens();
          tokens.subList(worker.beginDownloaded, worker.endDownloaded + 1).clear();
          pdfTokenStream.setTokens(tokens);
          break stream_loop;
        }
      }
    }
  }
  
  public static void main(String[] args) throws Exception {
    String file = "/tmp/d3/c3.pdf";
    IOUtils.copy(new SEGPdfFilterFactory().createFilteredInputStream(null, new FileInputStream(file), null),
                 new FileOutputStream(file + ".out"));
  }
  
}
