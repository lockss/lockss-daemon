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

package org.lockss.plugin.ingenta;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
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
   * Example: PDF from http://www.ingentaconnect.com/content/whp/eh/2014/00000020/00000001/art00005
   */
  protected static class WhiteHorsePressPdfFilterFactory extends SimplePdfFilterFactory {
    
    protected static class WhiteHorsePressWorker extends PdfTokenStreamStateMachine {
      
      private static final Logger log = Logger.getLogger(WhiteHorsePressWorker.class);
      private static final String str1 = " = IP address";
      private static final String str2 = " = Date & Time";
      
      public WhiteHorsePressWorker() {
        super(log);
      }
      
      @Override
      public void state0() throws PdfException {
        if (isBeginTextObject()) {
          setBegin(getIndex());
          setState(1);
        }
      }
      
      @Override
      public void state1() throws PdfException {
        if (isShowTextEndsWith(str1)) {
          setState(2);
        }
        else if (isEndTextObject()) {
          // don't stop ...
          setState(0);
        }
      }
      
      @Override
      public void state2() throws PdfException {
        if (isShowTextEndsWith(str2)) {
          setState(3);
        }
        else if (isEndTextObject()) {
          stop();
          // if order of tokens or streams change, don't stop() ...
          // setState(0);
        }
      }
      
      @Override
      public void state3() throws PdfException {
        if (isEndTextObject()) {
          setEnd(getIndex());
          setResult(true);
          stop(); 
        }
        else if (isBeginTextObject()) {
          setBegin(getIndex());
          setState(1);
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
        if (worker.getResult()) {
          List<PdfToken> tokens = pdfTokenStream.getTokens();
          // clear tokens including text markers
          tokens.subList(worker.getBegin(), worker.getEnd() + 1).clear();
          pdfTokenStream.setTokens(tokens);
        }
        else {
          PdfUtil.normalizeTokenStream(pdfTokenStream);
        }
      }
    }
  }
  
  /*
   * Example: http://api.ingentaconnect.com/content/maney/aac/2012/00000111/00000003/art00002?crawler=true&mimetype=application/pdf
   */
  protected static class ManeyPublishingPdfFilterFactory extends ExtractingPdfFilterFactory {
    
    protected static class ManeyPublishingWorker extends PdfTokenStreamStateMachine {
      
      private static final Logger log = Logger.getLogger(ManeyPublishingWorker.class);
      private static final String str1 = "Published by Maney Publishing";
      
      public ManeyPublishingWorker() {
        super(Direction.BACKWARD, log);
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
        if (isShowTextStartsWith(str1)) {
          setState(2);
        }
        else if (isBeginTextObject()) {
          stop();
        }
      }
      
      @Override
      public void state2() throws PdfException {
        if (isBeginTextObject()) {
          setBegin(getIndex());
          setResult(true);
          stop(); 
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
        if (worker.getResult()) {
          List<PdfToken> tokens = pdfTokenStream.getTokens();
          // clear tokens including text markers
          tokens.subList(worker.getBegin(), worker.getEnd() + 1).clear();
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
   * Examples:
   * http://api.ingentaconnect.com/content/paaf/paaf/2013/00000086/00000003/art00006?crawler=true ingest1 15:20:29 09/10/13
   * http://api.ingentaconnect.com/content/paaf/paaf/2013/00000086/00000003/art00006?crawler=true 11/25/14
   */
  protected static class PacificAffairsPdfFilterFactory extends ExtractingPdfFilterFactory {
    
    public static class PacificAffairsWorker extends PdfTokenStreamStateMachine {
      
      private static final Logger log = Logger.getLogger(PacificAffairsWorker.class);
      
      protected static final Pattern DELIVERED_BY =
          Pattern.compile("Delivered by .* to: .* IP: .* on:");
      
      public PacificAffairsWorker() {
        super(log);
      }
      
      @Override
      public void state0() throws PdfException {
        if (isBeginTextObject()) {
          setBegin(getIndex());
          setState(1);
        }
      }
      
      @Override
      public void state1() throws PdfException {
        if (isShowTextFind(DELIVERED_BY)) {
          setState(2);
        }
        else if (isEndTextObject()) {
          setState(0);
        }
      }
      
      @Override
      public void state2() throws PdfException {
        if (isEndTextObject()) {
          setEnd(getIndex());
          setResult(true);
          stop(); 
        }
      }
    }
    
    public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      PacificAffairsWorker worker = new PacificAffairsWorker();
      for (PdfPage pdfPage : pdfDocument.getPages()) {
        PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
        worker.process(pdfTokenStream);
        if (worker.getResult()) {
          List<PdfToken> tokens = pdfTokenStream.getTokens();
          tokens.subList(worker.getBegin(), worker.getEnd() + 1).clear();
          pdfTokenStream.setTokens(tokens);
        }
      }
    }
    
    @Override
    public PdfTransform<PdfDocument> getDocumentTransform(ArchivalUnit au,
        OutputStream os) {
      return new BaseDocumentExtractingTransform(os) {
        
        @Override
        public void outputDocumentInformation() throws PdfException {
          // do not output anything, rather than unset all fields
        }
      };
    }
  }
  
  private static class NormalizingExtractingPdfFilterFactory extends ExtractingPdfFilterFactory {
    
    @Override
    public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      doNormalizeMetadata(pdfDocument);
    }
  }
  
  private static class NormalizingPdfFilterFactory extends SimplePdfFilterFactory {
    
    @Override
    public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      doNormalizeMetadata(pdfDocument);
    }
  }
  
  public static void doNormalizeMetadata(PdfDocument pdfDocument)
      throws PdfException {
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetMetadata();
    pdfDocument.unsetAuthor(); // added later e.g. ALPSP
    pdfDocument.unsetProducer(); // added later e.g. ALPSP
    pdfDocument.unsetTitle(); // added later e.g. ALPSP
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
      case ALPSP: case ARN: case LSE: case IGSOC:
        return normExtractFiltFact.createFilteredInputStream(au, in, encoding);
        
      case BERGHAHN: case MANUP: case WAB: case UNKNOWN:
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
  
  public static void main(String[] args) throws Exception {
    FilterFactory normFiltFact = new NormalizingPdfFilterFactory();
    FilterFactory normExtractFiltFact = new NormalizingExtractingPdfFilterFactory();
    String[] files = new String[] {
        "/tmp/ingenta.i2.v3.pdf",
        "/tmp/ingenta.i2.v4.pdf",
    }; 
    for (String file : files) {
      IOUtils.copy(normFiltFact.createFilteredInputStream(null, new FileInputStream(file), null),
                   new FileOutputStream(file + ".out1"));
      IOUtils.copy(normExtractFiltFact.createFilteredInputStream(null, new FileInputStream(file), null),
                   new FileOutputStream(file + ".out2"));
    }
  }
  
}
