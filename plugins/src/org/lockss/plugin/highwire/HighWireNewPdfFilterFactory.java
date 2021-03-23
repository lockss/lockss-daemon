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

package org.lockss.plugin.highwire;

import java.util.*;

import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
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
        logger.debug3("ComplexDownloadedFromWorkerTransform: index: " + getIndex());
        logger.debug3("ComplexDownloadedFromWorkerTransform: operator: " + getOpcode());
      }
      
      switch (state) {
        
        case 0: {
          // FIXME 1.60
          if (PdfOpcodes.END_TEXT_OBJECT.equals(getOpcode())) { ++state; }
          else { stop(); }
        } break;
        
        case 1: {
          // FIXME 1.60
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(getOpcode())) { stop(); }
          // FIXME 1.60
          else if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
                   && DOWNLOADED_FROM.equals(getTokens().get(getIndex() - 1).getString())) { ++state; }
        } break;
        
        case 2: {
          // FIXME 1.60
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(getOpcode())) { state = 20; }
          // FIXME 1.60
          else if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
                   && getTokens().get(getIndex() - 1).getString().matches(URL_REGEX)) { ++state; }
        } break;

        case 3: {
          // FIXME 1.60
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(getOpcode())) { stop(); }
          // FIXME 1.60
          else if (PdfOpcodes.SHOW_TEXT.equals(getOpcode())) { ++state; }
        } break;
        
        case 4: {
          // FIXME 1.60
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(getOpcode())) {
            result = true;
            stop();
          }
        } break;
        
        case 20: {
          // FIXME 1.60
          if (PdfOpcodes.END_TEXT_OBJECT.equals(getOpcode())) { ++state; }
        } break;
        
        case 21: {
          // FIXME 1.60
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(getOpcode())) { stop(); }
          // FIXME 1.60
          else if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
                   && getTokens().get(getIndex() - 1).getString().matches(URL_REGEX)) { ++state; }
        } break;

        case 22: {
          // FIXME 1.60
          if (PdfOpcodes.END_TEXT_OBJECT.equals(getOpcode())) { ++state; }
        } break;
        
        case 23: {
          // FIXME 1.60
          if (PdfOpcodes.BEGIN_TEXT_OBJECT.equals(getOpcode())) {
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
        getTokens().subList(getIndex(), getTokens().size()).clear();
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
  
  public static class BMJGroupFrontPageWorker
      extends PdfTokenStreamWorker {

    public static final String DOWNLOADED_FROM = "Downloaded from ";

    public static final String UPDATED_INFORMATION_AND_SERVICES = "Updated information and services can be found at: ";

    private boolean result;    
    
    private int state;
    
    public BMJGroupFrontPageWorker() {
      // FIXME 1.60
      super(Direction.FORWARD);
    }
    
    @Override
    public void operatorCallback()
        throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("SagePublicationsFrontPageWorker: initial: " + state);
        logger.debug3("SagePublicationsFrontPageWorker: index: " + getIndex());
        logger.debug3("SagePublicationsFrontPageWorker: operator: " + getOpcode());
      }

      switch (state) {
        
        case 0: {
          // FIXME 1.60
          if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
              && getTokens().get(getIndex() - 1).getString().matches(UPDATED_INFORMATION_AND_SERVICES)) { ++state; }
        } break;
        
        case 1: {
          // FIXME 1.60
          if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
              && getTokens().get(getIndex() - 1).getString().matches(DOWNLOADED_FROM)) {
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
      super.setUp();
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
        logger.debug3("SagePublicationsFrontPageWorker: index: " + getIndex());
        logger.debug3("SagePublicationsFrontPageWorker: operator: " + getOpcode());
      }
      
      switch (state) {
        
        case 0: case 5: {
          // FIXME 1.60
          if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
              && getTokens().get(getIndex() - 1).getString().matches(URL_REGEX)) { ++state; }
        } break;
        
        case 1: {
          // FIXME 1.60
          if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
              && ONLINE_VERSION.equals(getTokens().get(getIndex() - 1).getString())) { ++state; }
        } break;
        
        case 2: {
          // FIXME 1.60
          if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
              && getTokens().get(getIndex() - 1).getString().matches(PUBLISHED_BY_REGEX)) { ++state; }
        } break;
        
        case 3: {
          // FIXME 1.60
          if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
              && SAGE_PUBLICATIONS_URL.equals(getTokens().get(getIndex() - 1).getString())) { ++state; }
        } break;
        
        case 4: {
          // FIXME 1.60
          if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
              && ADDITIONAL_SERVICES.equals(getTokens().get(getIndex() - 1).getString())) { ++state; }
        } break;
        
        // case 5: see case 0
        
        case 6: {
          // FIXME 1.60
          if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
              && DOWNLOADED_FROM.equals(getTokens().get(getIndex() - 1).getString())) {
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
      super.setUp();
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
      // FIXME 1.60
      if (   PdfOpcodes.SHOW_TEXT.equals(getOpcode())
          && getTokens().get(getIndex() - 1).getString().startsWith(DOWNLOADED_FROM)) {
        result = true;
        stop();
        getTokens().subList(getIndex() - 1, getIndex() + 1).clear();
      }
    }
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      result = false;
    }
    
    @Override
    public void transform(ArchivalUnit au, PdfTokenStream pdfTokenStream) throws PdfException {
      if (result) {
        pdfTokenStream.setTokens(getTokens());
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
      // FIXME 1.60
      if (   PdfOpcodes.SHOW_TEXT.equals(getOperator().getOperator())
          && getTokens().get(getIndex() - 1).getString().startsWith(CURRENT_AS_OF)) {
        result = true; // but don't stop()
        getTokens().set(getIndex() - 1, getFactory().makeString(CURRENT_AS_OF)); // FIXME 1.60
      }
    }
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      result = false;
    }
    
    public CurrentAsOfPageWorkerTransform() {
      // FIXME 1.60
      super(Direction.FORWARD);
    }
    
    @Override
    public void transform(ArchivalUnit au, PdfTokenStream pdfTokenStream) throws PdfException {
      if (result) {
        pdfTokenStream.setTokens(getTokens());
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
//    put(AmericanAcademyOfPediatricsPdfTransform.class.getName(), t);
    put(AmericanAcademyOfPediatricsPdfTransform.Simplified.class.getName(), t);
//    put(AmericanSocietyForNutritionPdfTransform.class.getName(), t);
    put(AmericanSocietyForNutritionPdfTransform.Simplified.class.getName(), t);
//    put(RockefellerUniversityPressPdfTransform.class.getName(), t);
    put(RockefellerUniversityPressPdfTransform.Simplified.class.getName(), t);
    // AMA
    t = new PdfTransform<PdfDocument>() {
      @Override public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
        doAmericanMedicalAssociation(au, pdfDocument);
      }
    };
//    put(AmericanMedicalAssociationPdfTransform.class.getName(), t);
    put(AmericanMedicalAssociationPdfTransform.Simplified.class.getName(), t);
    // APS
    t = new PdfTransform<PdfDocument>() {
      @Override public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
        doAmericanPhysiologicalSociety(au, pdfDocument);
      }
    };
    put(AmericanPhysiologicalSocietyPdfTransform.class.getName(), t);
//    put(AmericanPhysiologicalSocietyPdfTransform.Simplified.class.getName(), t);
    // BMJPG
    t = new PdfTransform<PdfDocument>() {
      @Override public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
        doBritishMedicalJournalPublishingGroup(au, pdfDocument);
      }
    };
//    put(BritishMedicalJournalPublishingGroupPdfTransform.class.getName(), t);
    put(BritishMedicalJournalPublishingGroupPdfTransform.Simplified.class.getName(), t);
//    // MMS
//    t = new PdfTransform<PdfDocument>() {
//      @Override public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
//        doSimpleDownloadedFrom(au, pdfDocument);
//      }
//    };
//    put(NewEnglandJournalOfMedicinePdfTransform.class.getName(), t);
//    put(NewEnglandJournalOfMedicinePdfTransform.Simplified.class.getName(), t);
    // SAGE
    t = new PdfTransform<PdfDocument>() {
      @Override public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
        sagePublications(au, pdfDocument);
      }
    };
//    put(SagePublicationsPdfTransform.class.getName(), t);
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

//  public void doSimpleDownloadedFrom(ArchivalUnit au,
//                                     PdfDocument pdfDocument)
//      throws PdfException {
//    SimpleDownloadedFromWorkerTransform worker = new SimpleDownloadedFromWorkerTransform();
//    boolean firstPage = true;
//    for (PdfPage pdfPage : pdfDocument.getPages()) {
//      PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
//      worker.process(pdfTokenStream);
//      if (firstPage && !worker.result) {
//        return; // This PDF needs no processing
//      }
//      if (worker.result) {
//        worker.transform(au, pdfTokenStream);
//      }
//      firstPage = false;
//    }
//    doNormalizeMetadata(au, pdfDocument);
//  }

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
