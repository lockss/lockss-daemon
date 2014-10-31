/*
 * $Id: SEGPdfFilterFactory.java,v 1.1 2014-10-31 18:40:19 thib_gc Exp $
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

import java.util.List;
import java.util.regex.Pattern;

import org.lockss.filter.pdf.ExtractingPdfFilterFactory;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class SEGPdfFilterFactory extends ExtractingPdfFilterFactory {

  public static class Worker extends PdfTokenStreamWorker {

    private static final Logger logger = Logger.getLogger(Worker.class);
    
    public static final Pattern PATTERN =
        Pattern.compile("^Downloaded \\d+/\\d+/\\d+ to \\d+\\.\\d+\\.\\d+\\.\\d+", Pattern.CASE_INSENSITIVE);
    
    public int state;
    public boolean result;
    public int begin;
    public int end;
    
    @Override
    public void setUp() throws PdfException {
      this.state = 0;
      this.result = false;
      this.begin = -1;
      this.end = -1;
    }
    
    @Override
    public void operatorCallback() throws PdfException {
      logger.debug3("initial: " + state);
      logger.debug3("index: " + getIndex());
      logger.debug3("operator: " + getOpcode());
      switch (state) {
        case 0: {
          if (isBeginTextObject()) {
            begin = getIndex();
            ++state;
          }
          else {
            stop();
          }
        } break;
        case 1: {
          if (isShowTextFind(PATTERN)) {
            ++state;
          }
          else if (isEndTextObject()) {
            stop();
          }
        } break;
        case 2: {
          if (isEndTextObject()) {
            result = true;
            end = getIndex();
            stop();
          }
        } break;
        default: {
          throw new PdfException("Illegal state: " + state);
        }
      }
      logger.debug3("final: " + state);
      logger.debug3("result: " + result);
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
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      for (PdfTokenStream pdfTokenStream : pdfPage.getAllTokenStreams()) {
        worker.process(pdfTokenStream);
        if (worker.result) {
          List<PdfToken> tokens = pdfTokenStream.getTokens();
          tokens.subList(worker.begin, worker.end + 1).clear();
          pdfTokenStream.setTokens(tokens);
          break;
        }
      }
    }
  }
  
}
