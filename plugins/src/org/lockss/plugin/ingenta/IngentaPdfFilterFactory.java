/*
 * $Id: IngentaPdfFilterFactory.java,v 1.8 2013-03-02 02:20:38 thib_gc Exp $
 */ 

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.List;

import org.lockss.daemon.PluginException;
import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class IngentaPdfFilterFactory
    implements FilterFactory, PdfTransform<PdfDocument> {

  /**
   * An enum for publisher IDs used by Ingenta. 
   */
  protected enum PublisherId {
    /** Unknown */
    UNKNOWN,
    /** Hodder Arnold */
    ARN,
    /** Bergahn Journals */
    BERGHAHN,
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
  
  protected PdfDocumentFactory pdfDocumentFactory;
  private FilterFactory normFiltFact = new NormalizingPdfFilterFactory();
  private FilterFactory normExtractFiltFact = new NormalizingExtractingPdfFilterFactory();
  private FilterFactory paafFiltFact = new PacificAffairsPdfFilterFactory();
  private FilterFactory whpFiltFact = new WhiteHorsePressPdfFilterFactory();
  
  /**
   * <p>
   * Makes an instance using {@link DefaultPdfDocumentFactory}.
   * </p>
   * @since 1.56
   * @see DefaultPdfDocumentFactory
   */
  public IngentaPdfFilterFactory() {
    this(DefaultPdfDocumentFactory.getInstance());
  }
  
  /**
   * <p>
   * Makes an instance using the given PDF document factory.
   * </p>
   * @param pdfDocumentFactory A PDF document factory.
   * @since 1.56
   */
  public IngentaPdfFilterFactory(PdfDocumentFactory pdfDocumentFactory) {
    this.pdfDocumentFactory = pdfDocumentFactory;
  }
  /**
   * This Transform method is not used
   */
  @Override
  public void transform(ArchivalUnit au,
                        PdfDocument pdfDocument)
      throws PdfException {
  }
  
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
            // FIXME 1.60
            if (PdfOpcodes.END_TEXT_OBJECT.equals(getOpcode())) {
              endIndex = getIndex();
              ++state; 
            }
          } break;
          
          case 1: {
            // FIXME 1.60
            if (PdfOpcodes.SHOW_TEXT.equals(getOpcode())
                && getTokens().get(getIndex() - 1).getString().endsWith(" = Date & Time")) {
              ++state;
            }
            // FIXME 1.60
            else if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(getOpcode())) { stop(); }
          } break;
          
          case 2: {
            // FIXME 1.60
            if (PdfOpcodes.SHOW_TEXT.equals(getOpcode())
                && getTokens().get(getIndex() - 1).getString().endsWith(" = IP address")) {
              ++state;
            }
            // FIXME 1.60
            else if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(getOpcode())) { stop(); }
          } break;
          
          case 3: {
            // FIXME 1.60
            if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(getOpcode())) {
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
    
    public void transform(ArchivalUnit au,
                          PdfDocument pdfDocument)
        throws PdfException {
      
      WhiteHorsePressWorker worker = new WhiteHorsePressWorker();
      for (PdfPage pdfPage : pdfDocument.getPages()) {
        PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
        worker.process(pdfTokenStream);
        if (worker.result) {
          List<PdfToken> tokens = pdfTokenStream.getTokens();
          tokens.subList(worker.beginIndex, worker.endIndex).clear();
          pdfTokenStream.setTokens(tokens);
        }
      }
    }
  }
  
  /*
   * Filter factory for each different transform because some publisher transforms are
   * simple transforms and some extracting.
   */
  private class PacificAffairsPdfFilterFactory extends SimplePdfFilterFactory {
  					
    public void transform(ArchivalUnit au,
                                 PdfDocument pdfDocument)
        throws PdfException {
      
      DeliveredByWorkerTransform worker = new DeliveredByWorkerTransform();
      for (PdfPage pdfPage : pdfDocument.getPages()) {
        PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
        worker.process(pdfTokenStream);
        if (worker.result) {
          worker.transform(au, pdfTokenStream);
        }
      }
      doNormalizeMetadata(pdfDocument);
    }
  }
  
  private class NormalizingExtractingPdfFilterFactory extends ExtractingPdfFilterFactory {
	
    public void transform(ArchivalUnit au,
                          PdfDocument pdfDocument)
        throws PdfException {
      doNormalizeMetadata(pdfDocument);
    }
  }
  
  private class NormalizingPdfFilterFactory extends SimplePdfFilterFactory {
	
    public void transform(ArchivalUnit au,
                          PdfDocument pdfDocument)
        throws PdfException {
      doNormalizeMetadata(pdfDocument);
    }
  }
  
  public void doNormalizeMetadata(PdfDocument pdfDocument)
      				    throws PdfException {
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetMetadata();
    PdfUtil.normalizeTrailerId(pdfDocument);
  }
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      					       InputStream in,
      					       String encoding)
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
      case ARN:
	return normExtractFiltFact.createFilteredInputStream(au, in, encoding);
	
      case BERGHAHN: case LSE: case WAB: case MANEY: case MANUP: case UNKNOWN:
	return normFiltFact.createFilteredInputStream(au, in, encoding);
	
      case PAAF:
	return paafFiltFact.createFilteredInputStream(au, in, encoding);
     
      case WHP:
        return whpFiltFact.createFilteredInputStream(au, in, encoding);
     
      default:
	return in;
    }
  }
  
  public static class DeliveredByWorkerTransform
      extends PdfTokenStreamWorker
      implements PdfTransform<PdfTokenStream> {
    
    public static final String DELIVERED_BY_REGEX = "\\s*Delivered by .* to: .*";
    
    private boolean result;
    
    private int state;
    
    private int endIndex;
    
    public DeliveredByWorkerTransform() {
      super(Direction.BACKWARD);
    }
    
    @Override
    public void operatorCallback()
        throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("DeliveredByWorkerTransform: initial: " + state);
        logger.debug3("DeliveredByWorkerTransform: index: " + getIndex());
        logger.debug3("DeliveredByWorkerTransform: operator: " + getOpcode());
      }
      
      switch (state) {
        
        case 0: {
          // FIXME 1.60
          if (PdfOpcodes.RESTORE_GRAPHICS_STATE.equals(getOpcode())) {
            endIndex = getIndex();
            ++state; 
          }
        } break;
        
        case 1: {
          // FIXME 1.60
          if (PdfOpcodes.SHOW_TEXT.equals(getOpcode())
                   && getTokens().get(getIndex() - 1).getString().matches(DELIVERED_BY_REGEX)) {
            ++state;
          }
          // FIXME 1.60
          else if (PdfOpcodes.RESTORE_GRAPHICS_STATE.equals(getOpcode())) { stop(); }
        } break;
        
        case 2: {
          // FIXME 1.60
          if (PdfOpcodes.SAVE_GRAPHICS_STATE.equals(getOpcode())) {
            result = true;
            stop(); 
          }
        } break;
        
        default: {
          throw new PdfException("Invalid state in DeliveredByWorkerTransform: " + state);
        }
    
      }
    
      if (logger.isDebug3()) {
        logger.debug3("DeliveredByWorkerTransform: final: " + state);
        logger.debug3("DeliveredByFromWorkerTransform: result: " + result);
      }
      
      if (result) {
        getTokens().subList(getIndex(), endIndex).clear();
      }
    }
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      state = 0;
      result = false;
    }
    
    @Override
    public void transform(ArchivalUnit au,
                           PdfTokenStream pdfTokenStream)
        throws PdfException {
      if (result) {
        pdfTokenStream.setTokens(getTokens());
      }
    }
  }
  
}
