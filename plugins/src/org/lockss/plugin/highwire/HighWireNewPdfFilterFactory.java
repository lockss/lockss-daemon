/*
 * $Id: HighWireNewPdfFilterFactory.java,v 1.1.2.1 2012-07-17 02:48:30 thib_gc Exp $
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

package org.lockss.plugin.highwire;

import java.util.*;

import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
import org.lockss.pdf.PdfTokenStreamWorker.Direction;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class HighWireNewPdfFilterFactory extends ExtractingPdfFilterFactory {

  public static class ComplexDownloadedFromWorkerTransform
      extends PdfTokenStreamWorker
      implements PdfTransform<PdfTokenStream> {
    
    public static final String DOWNLOADED_FROM = "Downloaded from ";

    public static final String URL_REGEX = "(?:http://)?[-0-9A-Za-z]+(?:\\.[-0-9A-Za-z]+)+";

    private boolean result;
    
    private int state;
    
    public ComplexDownloadedFromWorkerTransform() {
      super(Direction.BACKWARD);
    }

    @Override
    public void operatorCallback()
        throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("ComplexDownloadedFromWorkerTransform: initial: " + state);
        logger.debug3("ComplexDownloadedFromWorkerTransform: index: " + index);
        logger.debug3("ComplexDownloadedFromWorkerTransform: operator: " + opcode);
      }
      
      switch (state) {
        
        case 0: {
          if (PdfOpcodes.END_TEXT_OBJECT.equals(opcode)) { ++state; }
          else { stop(); }
        } break;
        
        case 1: {
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(opcode)) { stop(); }
          else if (   PdfOpcodes.SHOW_TEXT.equals(opcode)
                   && DOWNLOADED_FROM.equals(tokens.get(index - 1).getString())) { ++state; }
        } break;
        
        case 2: {
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(opcode)) { state = 20; }
          else if (   PdfOpcodes.SHOW_TEXT.equals(opcode)
                   && tokens.get(index - 1).getString().matches(URL_REGEX)) { ++state; }
        } break;

        case 3: {
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(opcode)) { stop(); }
          else if (PdfOpcodes.SHOW_TEXT.equals(opcode)) { ++state; }
        } break;
        
        case 4: {
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(opcode)) {
            result = true;
            stop();
          }
        } break;
        
        case 20: {
          if (PdfOpcodes.END_TEXT_OBJECT.equals(opcode)) { ++state; }
        } break;
        
        case 21: {
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(opcode)) { stop(); }
          else if (   PdfOpcodes.SHOW_TEXT.equals(opcode)
                   && tokens.get(index - 1).getString().matches(URL_REGEX)) { ++state; }
        } break;

        case 22: {
          if (PdfOpcodes.END_TEXT_OBJECT.equals(opcode)) { ++state; }
        } break;
        
        case 23: {
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(opcode)) {
            result = true;
            stop();
          }
        } break;
        
        default: {
          throw new PdfException("Invalid state in ComplexDownloadedFromWorkerTransform: " + state);
        }

      }

      if (logger.isDebug3()) {
        logger.debug3("ComplexDownloadedFromWorkerTransform: final: " + state);
        logger.debug3("ComplexDownloadedFromWorkerTransform: result: " + result);
      }
      
      if (result) {
        tokens.subList(index, tokens.size()).clear();
      }
    }
    
    @Override
    public void setUp() throws PdfException {
      state = 0;
      result = false;
    }
    
    @Override
    public void transform(ArchivalUnit au,
                          PdfTokenStream pdfTokenStream)
        throws PdfException {
      if (result) {
        pdfTokenStream.setTokens(tokens);
      }
    }
    
  }
  
  public static class BMJGroupFrontPageWorker
      extends PdfTokenStreamWorker {

    public static final String DOWNLOADED_FROM = "Downloaded from ";

    public static final String UPDATED_INFORMATION_AND_SERVICES = "Updated information and services can be found at: ";

    private boolean result;    
    
    private int state;
    
    public BMJGroupFrontPageWorker() {
      super(Direction.FORWARD);
    }
    
    @Override
    public void operatorCallback()
        throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("SagePublicationsFrontPageWorker: initial: " + state);
        logger.debug3("SagePublicationsFrontPageWorker: index: " + index);
        logger.debug3("SagePublicationsFrontPageWorker: operator: " + opcode);
      }

      switch (state) {
        
        case 0: {
          if (   PdfOpcodes.SHOW_TEXT.equals(opcode)
              && tokens.get(index - 1).getString().matches(UPDATED_INFORMATION_AND_SERVICES)) { ++state; }
        } break;
        
        case 1: {
          if (   PdfOpcodes.SHOW_TEXT.equals(opcode)
              && tokens.get(index - 1).getString().matches(DOWNLOADED_FROM)) {
            result = true;
            stop();
          }
        } break;
        
        default: {
          throw new PdfException("Invalid state in SagePublicationsFrontPageWorker: " + state);
        }

      }

      if (logger.isDebug3()) {
        logger.debug3("SagePublicationsFrontPageWorker: final: " + state);
        logger.debug3("SagePublicationsFrontPageWorker: result: " + result);
      }
    }
    
    @Override
    public void setUp() throws PdfException {
      result = false;
      state = 0;
    }
    
  }
  
  public static class SagePublicationsFrontPageWorker
      extends PdfTokenStreamWorker {
    
    public static final String ADDITIONAL_SERVICES = "Additional services and information for ";

    public static final String DOWNLOADED_FROM = "Downloaded from ";

    public static final String ONLINE_VERSION = "The online version of this article can be found at:";

    public static final String PUBLISHED_BY_REGEX = "Published (?:by:|on behalf of)";

    public static final String SAGE_PUBLICATIONS_URL = "http://www.sagepublications.com";

    public static final String URL_REGEX = "http://[-0-9A-Za-z]+(?:\\.[-0-9A-Za-z]+)+";

    private boolean result;    
        
    private int state;
    
    public SagePublicationsFrontPageWorker() {
      super(Direction.FORWARD);
    }
    
    @Override
    public void operatorCallback()
        throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("SagePublicationsFrontPageWorker: initial: " + state);
        logger.debug3("SagePublicationsFrontPageWorker: index: " + index);
        logger.debug3("SagePublicationsFrontPageWorker: operator: " + opcode);
      }
      
      switch (state) {
        
        case 0: case 5: {
          if (   PdfOpcodes.SHOW_TEXT.equals(opcode)
              && tokens.get(index - 1).getString().matches(URL_REGEX)) { ++state; }
        } break;
        
        case 1: {
          if (   PdfOpcodes.SHOW_TEXT.equals(opcode)
              && ONLINE_VERSION.equals(tokens.get(index - 1).getString())) { ++state; }
        } break;
        
        case 2: {
          if (   PdfOpcodes.SHOW_TEXT.equals(opcode)
              && tokens.get(index - 1).getString().matches(PUBLISHED_BY_REGEX)) { ++state; }
        } break;
        
        case 3: {
          if (   PdfOpcodes.SHOW_TEXT.equals(opcode)
              && SAGE_PUBLICATIONS_URL.equals(tokens.get(index - 1).getString())) { ++state; }
        } break;
        
        case 4: {
          if (   PdfOpcodes.SHOW_TEXT.equals(opcode)
              && ADDITIONAL_SERVICES.equals(tokens.get(index - 1).getString())) { ++state; }
        } break;
        
        // case 5: see case 0
        
        case 6: {
          if (   PdfOpcodes.SHOW_TEXT.equals(opcode)
              && DOWNLOADED_FROM.equals(tokens.get(index - 1).getString())) {
            result = true;
            stop();
          }
        } break;
        
        default: {
          throw new PdfException("Invalid state in SagePublicationsFrontPageWorker: " + state);
        }

      }

      if (logger.isDebug3()) {
        logger.debug3("SagePublicationsFrontPageWorker: final: " + state);
        logger.debug3("SagePublicationsFrontPageWorker: result: " + result);
      }

    }
    
    @Override
    public void setUp() throws PdfException {
      result = false;
      state = 0;
    }
    
  }
  
  public static class SimpleDownloadedFromWorkerTransform
      extends PdfTokenStreamWorker
      implements PdfTransform<PdfTokenStream> {

    public static final String DOWNLOADED_FROM = "Downloaded from ";

    private boolean result;
    
    public SimpleDownloadedFromWorkerTransform() {
      super(Direction.BACKWARD);
    }
    
    @Override
    public void operatorCallback()
        throws PdfException {
      if (   PdfOpcodes.SHOW_TEXT.equals(opcode)
          && tokens.get(index - 1).getString().startsWith(DOWNLOADED_FROM)) {
        result = true;
        stop();
        tokens.subList(index - 1, index + 1).clear();
      }
    }
    
    @Override
    public void setUp() throws PdfException {
      result = false;
    }
    
    @Override
    public void transform(ArchivalUnit au, PdfTokenStream pdfTokenStream) throws PdfException {
      if (result) {
        pdfTokenStream.setTokens(tokens);
      }
    }  
    
  }  
  
  public static class CurrentAsOfPageWorkerTransform
      extends PdfTokenStreamWorker
      implements PdfTransform<PdfTokenStream> {
    
    public static final String CURRENT_AS_OF = "current as of ";

    private boolean result;

    @Override
    public void operatorCallback() throws PdfException {
      if (   PdfOpcodes.SHOW_TEXT.equals(operator.getOperator())
          && tokens.get(index - 1).getString().startsWith(CURRENT_AS_OF)) {
        result = true; // but don't stop()
        tokens.set(index - 1, adapter.makeString(CURRENT_AS_OF));
      }
    }
    
    @Override
    public void setUp() throws PdfException {
      this.result = false;
    }
    
    public CurrentAsOfPageWorkerTransform() {
      super(Direction.FORWARD);
    }
    
    @Override
    public void transform(ArchivalUnit au, PdfTokenStream pdfTokenStream) throws PdfException {
      if (result) {
        pdfTokenStream.setTokens(tokens);
      }
    }
    
  }
  
  private static final Logger logger = Logger.getLogger(HighWireNewPdfFilterFactory.class);
  
  protected final Map<String, PdfTransform<PdfDocument>> transforms =
      new HashMap<String, PdfTransform<PdfDocument>>() {{
    PdfTransform<PdfDocument> t = null;
    // AAP, ASN and RUP
    t = new PdfTransform<PdfDocument>() {
      @Override public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
        doComplexDownloadedFrom(au, pdfDocument);
      }
    };
    put(AmericanAcademyOfPediatricsPdfTransform.class.getName(), t);
    put(AmericanAcademyOfPediatricsPdfTransform.Simplified.class.getName(), t);
    put(AmericanSocietyForNutritionPdfTransform.class.getName(), t);
    put(AmericanSocietyForNutritionPdfTransform.Simplified.class.getName(), t);
    put(RockefellerUniversityPressPdfTransform.class.getName(), t);
    put(RockefellerUniversityPressPdfTransform.Simplified.class.getName(), t);
    // AMA
    t = new PdfTransform<PdfDocument>() {
      @Override public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
        doAmericanMedicalAssociation(au, pdfDocument);
      }
    };
    put(AmericanMedicalAssociationPdfTransform.class.getName(), t);
    put(AmericanMedicalAssociationPdfTransform.Simplified.class.getName(), t);
    // APS
    t = new PdfTransform<PdfDocument>() {
      @Override public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
        doAmericanPhysiologicalSociety(au, pdfDocument);
      }
    };
    put(AmericanPhysiologicalSocietyPdfTransform.class.getName(), t);
    put(AmericanPhysiologicalSocietyPdfTransform.Simplified.class.getName(), t);
    // BMJPG
    t = new PdfTransform<PdfDocument>() {
      @Override public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
        doBritishMedicalJournalPublishingGroup(au, pdfDocument);
      }
    };
    put(BritishMedicalJournalPublishingGroupPdfTransform.class.getName(), t);
    put(BritishMedicalJournalPublishingGroupPdfTransform.Simplified.class.getName(), t);
    // MMS
    t = new PdfTransform<PdfDocument>() {
      @Override public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
        doSimpleDownloadedFrom(au, pdfDocument);
      }
    };
    put(NewEnglandJournalOfMedicinePdfTransform.class.getName(), t);
    put(NewEnglandJournalOfMedicinePdfTransform.Simplified.class.getName(), t);
    // SAGE
    t = new PdfTransform<PdfDocument>() {
      @Override public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
        sagePublications(au, pdfDocument);
      }
    };
    put(SagePublicationsPdfTransform.class.getName(), t);
    put(SagePublicationsPdfTransform.Simplified.class.getName(), t);
  }};
  
  public void doAmericanMedicalAssociation(ArchivalUnit au,
                                           PdfDocument pdfDocument)
      throws PdfException {
    ComplexDownloadedFromWorkerTransform worker = new ComplexDownloadedFromWorkerTransform();
    CurrentAsOfPageWorkerTransform worker1 = new CurrentAsOfPageWorkerTransform();
    boolean firstPage = true;
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
      worker.process(pdfTokenStream);
      if (firstPage && !worker.result) {
        return; // This PDF needs no processing
      }
      if (worker.result) {
        worker.transform(au, pdfTokenStream);
        if (firstPage) {
          worker1.transform(au, pdfTokenStream);
        }
      }
      firstPage = false;
    }
    doNormalizeMetadata(au, pdfDocument);
  }
  
  public void doAmericanPhysiologicalSociety(ArchivalUnit au,
                                             PdfDocument pdfDocument)
      throws PdfException {
    ComplexDownloadedFromWorkerTransform worker = new ComplexDownloadedFromWorkerTransform();
    CurrentAsOfPageWorkerTransform worker1 = new CurrentAsOfPageWorkerTransform();
    boolean firstPage = true;
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
      worker.process(pdfTokenStream);
      if (firstPage && !worker.result) {
        return; // This PDF needs no processing
      }
      if (worker.result) {
        worker.transform(au, pdfTokenStream);
        worker1.transform(au, pdfTokenStream);
      }
      firstPage = false;
    }
    doNormalizeMetadata(au, pdfDocument);
  }
  
  public void doBritishMedicalJournalPublishingGroup(ArchivalUnit au,
                                                     PdfDocument pdfDocument)
      throws PdfException {
    BMJGroupFrontPageWorker worker1 = new BMJGroupFrontPageWorker();
    worker1.process(pdfDocument.getPage(0).getPageTokenStream());
    if (!worker1.result) {
      return; // This PDF needs no processing
    }
    pdfDocument.removePage(0);
    ComplexDownloadedFromWorkerTransform worker = new ComplexDownloadedFromWorkerTransform();
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
      worker.process(pdfTokenStream);
      if (worker.result) {
        worker.transform(au, pdfTokenStream);
      }
    }
    doNormalizeMetadata(au, pdfDocument);
  }

  public void doComplexDownloadedFrom(ArchivalUnit au,
                                      PdfDocument pdfDocument)
      throws PdfException {
    ComplexDownloadedFromWorkerTransform worker = new ComplexDownloadedFromWorkerTransform();
    boolean firstPage = true;
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
      worker.process(pdfTokenStream);
      if (firstPage && !worker.result) {
        return; // This PDF needs no processing
      }
      if (worker.result) {
        worker.transform(au, pdfTokenStream);
      }
      firstPage = false;
    }
    doNormalizeMetadata(au, pdfDocument);
  }

  public void doNormalizeMetadata(ArchivalUnit au,
                                  PdfDocument pdfDocument)
      throws PdfException {
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetMetadata();
    PdfUtil.normalizeTrailerId(pdfDocument);
  }

  public void doSimpleDownloadedFrom(ArchivalUnit au,
                                     PdfDocument pdfDocument)
      throws PdfException {
    SimpleDownloadedFromWorkerTransform worker = new SimpleDownloadedFromWorkerTransform();
    boolean firstPage = true;
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
      worker.process(pdfTokenStream);
      if (firstPage && !worker.result) {
        return; // This PDF needs no processing
      }
      if (worker.result) {
        worker.transform(au, pdfTokenStream);
      }
      firstPage = false;
    }
    doNormalizeMetadata(au, pdfDocument);
  }

  public void sagePublications(ArchivalUnit au,
                               PdfDocument pdfDocument)
      throws PdfException {
    SagePublicationsFrontPageWorker worker1 = new SagePublicationsFrontPageWorker();
    worker1.process(pdfDocument.getPage(0).getPageTokenStream());
    if (!worker1.result) {
      return; // This PDF needs no processing
    }
    pdfDocument.removePage(0);
    ComplexDownloadedFromWorkerTransform worker = new ComplexDownloadedFromWorkerTransform();
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
      worker.process(pdfTokenStream);
      if (worker.result) {
        worker.transform(au, pdfTokenStream);
      }
    }
    doNormalizeMetadata(au, pdfDocument);
  }
  
  @Override
  public void transform(ArchivalUnit au,
                        PdfDocument pdfDocument)
      throws PdfException {
    String hint = PdfUtil.getPdfHint(au);
    if (hint == null) {
      // log
      return;
    }
    PdfTransform<PdfDocument> transform = transforms.get(hint);
    if (transform == null) {
      // log
      return;
    }
    transform.transform(au, pdfDocument);
  }

}
