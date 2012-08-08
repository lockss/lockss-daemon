/*
 * $Id: IngentaPdfFilterFactory.java,v 1.4 2012-08-08 20:46:35 wkwilson Exp $
 */ 

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.InputStream;

import org.lockss.daemon.PluginException;
import org.lockss.filter.pdf.ExtractingPdfFilterFactory;
import org.lockss.filter.pdf.PdfTransform;
import org.lockss.filter.pdf.SimplePdfFilterFactory;
import org.lockss.pdf.DefaultPdfDocumentFactory;
import org.lockss.pdf.PdfDocument;
import org.lockss.pdf.PdfDocumentFactory;
import org.lockss.pdf.PdfException;
import org.lockss.pdf.PdfOpcodes;
import org.lockss.pdf.PdfPage;
import org.lockss.pdf.PdfTokenStream;
import org.lockss.pdf.PdfTokenStreamWorker;
import org.lockss.pdf.PdfUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;

public class IngentaPdfFilterFactory implements FilterFactory, 
						PdfTransform<PdfDocument> {
  /*
   *all of the publishers on this platform that have a pdf filter
   *lse: London School Of Economics
   *maney: Maney
   *minsoc: Mineralogical Society
   *paaf: Pacific Affairs
   *wab: Whiting And Birch
   *berghahn: Berghahn
   *cms: Clay Minerals Society
   *arn: Hodder Arnold
   */
  private enum PublisherId {UNKNOWN, ARN, BERGHAHN, CMS, LSE, MANEY, MINSOC, PAAF, WAB}
  static Logger logger = Logger.getLogger("IngentaPdfFilterFactory");
  protected PdfDocumentFactory pdfDocumentFactory;
  private FilterFactory normFiltFact = new NormalizingPdfFilterFactory();
  private FilterFactory normExtractFiltFact = new NormalizingExtractingPdfFilterFactory();
  private FilterFactory paafFiltFact = new PacificAffairsPdfFilterFactory();
  
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
	logger.debug3("", e);
      }
    }
    switch (publisherId) {
      case ARN: case CMS: case MINSOC:
	return normExtractFiltFact.createFilteredInputStream(au, in, encoding);
	
      case BERGHAHN: case LSE: case WAB: case MANEY: case UNKNOWN:
	return normFiltFact.createFilteredInputStream(au, in, encoding);
	
      case PAAF:
	return paafFiltFact.createFilteredInputStream(au, in, encoding);
     
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
          if (PdfOpcodes.RESTORE_GRAPHICS_STATE.equals(getOpcode())) {
            endIndex = getIndex();
            ++state; 
          }
        } break;
        
        case 1: {
          if (PdfOpcodes.SHOW_TEXT.equals(getOpcode())
                   && getTokens().get(getIndex() - 1).getString().matches(DELIVERED_BY_REGEX)) {
            ++state;
          }
          else if (PdfOpcodes.RESTORE_GRAPHICS_STATE.equals(getOpcode())) { stop(); }
        } break;
        
        case 2: {
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
