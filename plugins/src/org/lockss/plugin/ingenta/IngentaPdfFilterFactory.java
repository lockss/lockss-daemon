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

package org.lockss.plugin.ingenta;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class IngentaPdfFilterFactory implements FilterFactory {
  
  /**
   * An enum for publisher IDs used by Ingenta. 
   */
  protected enum PublisherId {
    /** Unknown */
    UNKNOWN,
    // alpsp
    /** Association of Learned and Professional Society Publishers */
    ALPSP,
    /** Hodder Arnold */
    ARN,
    /** Bergahn Journals */
    BERGHAHN,
    /** International Glaciological Society */
    IGSOC,
    /** London School of Economics */
    LSE,
    /** Maney Publishing */
    MANEY,
    /** Manchester University Press */
    MANUP,
    /** Pacific Affairs */
    PAAF,
    /** Whiting and Birch */
    WAB,
    /** White Horse Press */
    WHP,
  }
  
  private static final Logger logger = Logger.getLogger(IngentaPdfFilterFactory.class);
  
  private FilterFactory normFiltFact = new NormalizingPdfFilterFactory();
  private FilterFactory normExtractFiltFact = new NormalizingExtractingPdfFilterFactory();
  private FilterFactory maneyFiltFact = new ManeyPublishingPdfFilterFactory();
  private FilterFactory paafFiltFact = new PacificAffairsPdfFilterFactory();
  private FilterFactory whpFiltFact = new WhiteHorsePressPdfFilterFactory();
  
  /*
   * FIXME 1.67: extend PdfTokenStreamStateMachine
   */
  /*
   * Example: PDF from http://www.ingentaconnect.com/content/whp/eh/2014/00000020/00000001/art00005
   */
  protected static class WhiteHorsePressPdfFilterFactory extends SimplePdfFilterFactory {
    
    protected static class WhiteHorsePressWorker extends PdfTokenStreamWorker {
      
      protected boolean result;
      
      protected int beginIndex;
      
      protected int endIndex;
      
      protected int state;
      
      public WhiteHorsePressWorker() {
        super(Direction.BACKWARD);
      }
      
      @Override
      public void setUp() throws PdfException {
        super.setUp();
        this.state = 0;
        this.result = false;
        this.beginIndex = -1;
        this.endIndex = -1;
      }
      
      @Override
      public void operatorCallback() throws PdfException {
        if (logger.isDebug3()) {
          logger.debug3("WhiteHorsePressWorker: initial: " + state);
          logger.debug3("WhiteHorsePressWorker: index: " + getIndex());
          logger.debug3("WhiteHorsePressWorker: operator: " + getOpcode());
        }
        
        switch (state) {
          case 0: {
            if (isEndTextObject()) {
              endIndex = getIndex();
              ++state; 
            }
          } break;
          
          case 1: {
            if (isShowTextEndsWith(" = Date & Time")) {
              ++state;
            }
            else if (isBeginTextObject()) {
              stop();
            }
          } break;
          
          case 2: {
            if (isShowTextEndsWith(" = IP address")) {
              ++state;
            }
            else if (isBeginTextObject()) {
              stop();
            }
          } break;
          
          case 3: {
            if (isBeginTextObject()) {
              beginIndex = getIndex();
              result = true;
              stop(); 
            }
          } break;
          
          default: {
            throw new PdfException("Invalid state in WhiteHorsePressWorker: " + state);
          }
        }
        
        if (logger.isDebug3()) {
          logger.debug3("WhiteHorsePressWorker: final: " + state);
          logger.debug3("WhiteHorsePressWorker: result: " + result);
        }
        
      }
      
    }
    
    public void transform(ArchivalUnit au, PdfDocument pdfDocument)
        throws PdfException {
      doNormalizeMetadata(pdfDocument);
      WhiteHorsePressWorker worker = new WhiteHorsePressWorker();
      for (PdfPage pdfPage : pdfDocument.getPages()) {
        PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
        worker.process(pdfTokenStream);
        if (worker.result) {
          List<PdfToken> tokens = pdfTokenStream.getTokens();
          // clear tokens including text markers
          tokens.subList(worker.beginIndex, worker.endIndex + 1).clear();
          pdfTokenStream.setTokens(tokens);
        }
      }
    }
    
  }
  
  /*
   * FIXME 1.67: extend PdfTokenStreamStateMachine
   */
  /*
   * Example: http://api.ingentaconnect.com/content/maney/aac/2012/00000111/00000003/art00002?crawler=true&mimetype=application/pdf
   */
  protected static class ManeyPublishingPdfFilterFactory extends ExtractingPdfFilterFactory {
    
    protected static class ManeyPublishingWorker extends PdfTokenStreamWorker {
      
      protected boolean result;
      
      protected int beginIndex;
      
      protected int endIndex;
      
      protected int state;
      
      public ManeyPublishingWorker() {
        super(Direction.BACKWARD);
      }
      
      @Override
      public void setUp() throws PdfException {
        super.setUp();
        this.state = 0;
        this.result = false;
        this.beginIndex = -1;
        this.endIndex = -1;
      }
      
      @Override
      public void operatorCallback() throws PdfException {
        if (logger.isDebug3()) {
          logger.debug3("ManeyPublishingWorker: initial: " + state);
          logger.debug3("ManeyPublishingWorker: index: " + getIndex());
          logger.debug3("ManeyPublishingWorker: operator: " + getOpcode());
        }
        
        switch (state) {
          
          case 0: {
            if (isEndTextObject()) {
              endIndex = getIndex();
              ++state; 
            }
          } break;
          
          case 1: {
            if (isShowTextStartsWith("Published by Maney Publishing")) {
              ++state;
            }
            else if (isBeginTextObject()) {
              stop();
            }
          } break;
          
          case 2: {
            if (isBeginTextObject()) {
              beginIndex = getIndex();
              result = true;
              stop(); 
            }
          } break;
          
          default: {
            throw new PdfException("Invalid state in ManeyPublishingWorker: " + state);
          }
          
        }
        
        if (logger.isDebug3()) {
          logger.debug3("ManeyPublishingWorker: final: " + state);
          logger.debug3("ManeyPublishingWorker: result: " + result);
        }
        
      }
      
    }
    
    public void transform(ArchivalUnit au, PdfDocument pdfDocument)
        throws PdfException {
      doNormalizeMetadata(pdfDocument);
      ManeyPublishingWorker worker = new ManeyPublishingWorker();
      for (PdfPage pdfPage : pdfDocument.getPages()) {
        PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
        worker.process(pdfTokenStream);
        if (worker.result) {
          List<PdfToken> tokens = pdfTokenStream.getTokens();
          // clear tokens including text markers
          tokens.subList(worker.beginIndex, worker.endIndex + 1).clear();
          pdfTokenStream.setTokens(tokens);
        }
      }
    }
  }
  
  /*
   * Filter factory for each different transform because some publisher transforms are
   * simple transforms and some extracting.
   */
  
  /*
   * FIXME 1.67: extends PdfTokenStreamStateMachine
   */
  /*
   * Examples:
   * http://api.ingentaconnect.com/content/paaf/paaf/2013/00000086/00000003/art00006?crawler=true ingest1 15:20:29 09/10/13
   * http://api.ingentaconnect.com/content/paaf/paaf/2013/00000086/00000003/art00006?crawler=true 11/25/14
   */
  protected static class PacificAffairsPdfFilterFactory extends ExtractingPdfFilterFactory {
    
    public static class PacificAffairsWorker extends PdfTokenStreamWorker {
      
      protected static final Pattern DELIVERED_BY =
          Pattern.compile("Delivered by .* to: .* IP: .* on:");
      
      protected boolean result;
      
      protected int state;
      
      protected int beginIndex;
      
      protected int endIndex;
      
      @Override
      public void setUp() throws PdfException {
        super.setUp();
        this.state = 0;
        this.result = false;
        this.beginIndex = -1;
        this.endIndex = -1;
      }
      
      @Override
      public void operatorCallback()
          throws PdfException {
        if (logger.isDebug3()) {
          logger.debug3("PacificAffairsWorker: initial: " + state);
          logger.debug3("PacificAffairsWorker: index: " + getIndex());
          logger.debug3("PacificAffairsWorker: operator: " + getOpcode());
        }
        
        switch (state) {
        
        case 0: {
          if (isBeginTextObject()) {
            beginIndex = getIndex();
            state = 1;
          }
        } break;
        
        case 1: {
          if (isShowTextFind(DELIVERED_BY)) {
            state = 2;
          }
          else if (isEndTextObject()) {
            state = 0;
          }
        } break;
        
        case 2: {
          if (isEndTextObject()) {
            endIndex = getIndex();
            result = true;
            stop(); 
          }
        } break;
        
        default: {
          throw new PdfException("Invalid state in PacificAffairsWorker: " + state);
        }
        
        }
        
        if (logger.isDebug3()) {
          logger.debug3("PacificAffairsWorker: final: " + state);
          logger.debug3("PacificAffairsWorker: result: " + result);
        }
      }
      
    }
    
    public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      /* FIXME 1.67: use the getDocumentTransform/outputDocumentInformation override instead */
      pdfDocument.unsetCreationDate();
      pdfDocument.unsetModificationDate();
      pdfDocument.unsetMetadata();
      pdfDocument.unsetCreator();
      pdfDocument.unsetProducer();
      pdfDocument.unsetAuthor();
      pdfDocument.unsetTitle();
      pdfDocument.unsetSubject();
      pdfDocument.unsetKeywords();
      /* end FIXME 1.67 */
      PacificAffairsWorker worker = new PacificAffairsWorker();
      for (PdfPage pdfPage : pdfDocument.getPages()) {
        PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
        worker.process(pdfTokenStream);
        if (worker.result) {
          List<PdfToken> tokens = pdfTokenStream.getTokens();
          tokens.subList(worker.beginIndex, worker.endIndex + 1).clear();
          pdfTokenStream.setTokens(tokens);
        }
      }
    }

    /* FIXME 1.67 */
//    @Override
//    public PdfTransform<PdfDocument> getDocumentTransform(ArchivalUnit au, OutputStream os) {
//      return new BaseDocumentExtractingTransform(os) {
//        @Override
//        public void outputDocumentInformation() throws PdfException {
//          // Intentionally left blank
//        }
//      };
//    }
    /* end FIXME 1.67 */
    
  }
  
  private class NormalizingExtractingPdfFilterFactory extends ExtractingPdfFilterFactory {
    
    public void transform(ArchivalUnit au, PdfDocument pdfDocument)
        throws PdfException {
      doNormalizeMetadata(pdfDocument);
    }
  }
  
  private class NormalizingPdfFilterFactory extends SimplePdfFilterFactory {
    
    public void transform(ArchivalUnit au, PdfDocument pdfDocument)
        throws PdfException {
      doNormalizeMetadata(pdfDocument);
    }
  }
  
  public static void doNormalizeMetadata(PdfDocument pdfDocument)
      throws PdfException {
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetMetadata();
    PdfUtil.normalizeTrailerId(pdfDocument);
  }
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding)
          throws PluginException {
    
    PublisherId publisherId = PublisherId.UNKNOWN;
    try {
      publisherId = PublisherId.valueOf(au.getProperties().getString("publisher_id").toUpperCase());
    } catch (IllegalArgumentException e) {
      if (logger.isDebug3()) {
        logger.debug3(String.format("Unknown publisher ID: %s", publisherId), e);
      }
    }
    switch (publisherId) {
      case ARN: case LSE: case IGSOC:
        return normExtractFiltFact.createFilteredInputStream(au, in, encoding);
        
      case ALPSP: case BERGHAHN: case MANUP: case WAB: case UNKNOWN:
        return normFiltFact.createFilteredInputStream(au, in, encoding);
        
      case MANEY:
        return maneyFiltFact.createFilteredInputStream(au, in, encoding);
        
      case PAAF:
        return paafFiltFact.createFilteredInputStream(au, in, encoding);
        
      case WHP:
        return whpFiltFact.createFilteredInputStream(au, in, encoding);
        
      default:
        return in;
    }
  }

}
