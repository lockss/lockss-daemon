/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package uk.org.lockss.plugin.annualreviews;

import java.util.*;
import java.util.regex.Pattern;

import org.lockss.filter.pdf.SimplePdfFilterFactory;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class AnnualReviewsPdfFilterFactory extends SimplePdfFilterFactory {

  /**
   * <p>A logger for use by this class.</p>
   */
  private static final Logger logger = Logger.getLogger(AnnualReviewsPdfFilterFactory.class);

  /*
   * FIXME 1.67: extend PdfTokenStreamStateMachine instead
   */
  protected static class DownloadedFromWorker extends PdfTokenStreamWorker {

    public DownloadedFromWorker() {
      super(Direction.BACKWARD);
    }
    
    protected static final Pattern DOWNLOADED_FROM_PATTERN =
        Pattern.compile("Downloaded from (?:http://)?[-0-9A-Za-z]+(?:\\.[-0-9A-Za-z]+)+");
    
    protected int state;
    
    protected boolean result;
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      this.result = false;
      this.state = 0;
    }
    
    @Override
    public void operatorCallback() throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("DownloadedFromWorker: initial: " + state);
        logger.debug3("DownloadedFromWorker: index: " + getIndex());
        logger.debug3("DownloadedFromWorker: operator: " + getOpcode());
      }
      
      switch (state) {
        
        case 0: {
          if (getIndex() == getTokens().size() - 1 && isEndTextObject()) {
            ++state;
          }
          else {
            stop();
          }
        } break;
        
        case 1: {
          if (isShowTextFind(DOWNLOADED_FROM_PATTERN)) {
            ++state;
          }
          else if (isBeginTextObject()) {
            stop();
          }
        } break;
        
        case 2: {
          if (isSetTextMatrix()) {
            ++state;
          }
          else if (isBeginTextObject()) {
            stop();
          }
        } break;
        
        case 3: {
          if (getIndex() == 0 && isBeginTextObject()) {
            result = true;
            stop();
          }
        } break;

        default: {
          throw new PdfException("Invalid state in DownloadedFromWorker: " + state);
        }
    
      }
      
      if (logger.isDebug3()) {
        logger.debug3("DownloadedFromWorker: final: " + state);
        logger.debug3("DownloadedFromWorker: result: " + result);
      }
      
    }
    
  }
  
  /*
   * FIXME 1.67: extend PdfTokenStreamStateMachine instead
   */
  protected static class ForPersonalUseWorker extends PdfTokenStreamWorker {

    public ForPersonalUseWorker() {
      super(Direction.BACKWARD);
    }
    
    protected static final Pattern FOR_PERSONAL_USE_PATTERN =
        Pattern.compile("by .* on [0-9]{2}/[0-9]{2}/[0-9]{2}. For personal use only.");
    
    protected int state;
    
    protected boolean result;
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      this.result = false;
      this.state = 0;
    }
    
    @Override
    public void operatorCallback() throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("ForPersonalUseWorker: initial: " + state);
        logger.debug3("ForPersonalUseWorker: index: " + getIndex());
        logger.debug3("ForPersonalUseWorker: operator: " + getOpcode());
      }
      
      switch (state) {
        
        case 0: {
          if (getIndex() == getTokens().size() - 1 && isEndTextObject()) {
            ++state;
          }
        } break;
        
        case 1: {
          if (isShowTextFind(FOR_PERSONAL_USE_PATTERN)) {
            ++state;
          }
          else if (isBeginTextObject()) {
            stop();
          }
        } break;
        
        case 2: {
          if (isSetTextMatrix()) {
            ++state;
          }
          else if (isBeginTextObject()) {
            stop();
          }
        } break;
        
        case 3: {
          if (getIndex() == 0 && isBeginTextObject()) {
            result = true;
            stop();
          }
        } break;

        default: {
          throw new PdfException("Invalid state in ForPersonalUseWorker: " + state);
        }
    
      }
      
      if (logger.isDebug3()) {
        logger.debug3("ForPersonalUseWorker: final: " + state);
        logger.debug3("ForPersonalUseWorker: result: " + result);
      }
      
    }
    
  }
  
  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetModificationDate();
    PdfUtil.normalizeTrailerId(pdfDocument);
    DownloadedFromWorker downloadedFromWorker = new DownloadedFromWorker();
    ForPersonalUseWorker forPersonalUseWorker = new ForPersonalUseWorker();
    boolean firstPage = true;
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      boolean hasDoneSomething = false;
      for (PdfTokenStream pdfTokenStream : pdfPage.getAllTokenStreams()) {
        downloadedFromWorker.process(pdfTokenStream);
        if (downloadedFromWorker.result) {
          hasDoneSomething = true;
          pdfTokenStream.setTokens(Collections.<PdfToken>emptyList());
        }
        forPersonalUseWorker.process(pdfTokenStream);
        if (forPersonalUseWorker.result) {
          hasDoneSomething = true;
          pdfTokenStream.setTokens(Collections.<PdfToken>emptyList());
        }
      }
      if (firstPage && !hasDoneSomething) {
        break; // no need to try the other pages
      }
      else {
        firstPage = false;
      }
    }
  }
  
}
