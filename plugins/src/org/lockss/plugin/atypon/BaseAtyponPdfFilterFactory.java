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

package org.lockss.plugin.atypon;

import org.lockss.filter.pdf.SimplePdfFilterFactory;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;

/**
 * A pdf filter that handles the needs of many Atypon children
 * - handles pdfbox cryptography exception and removes creation date, modification
 * and trailer id.
 * For more complicated issues (usually seen in pdfplus - use the 
 * BaseAtyponScrapingPdfFilterFactory
 * @author alexohlson
 *
 */
public class BaseAtyponPdfFilterFactory extends SimplePdfFilterFactory {

  /*
   * Many Atypon pdf files have the CreationDate and ModDate and the two ID numbers in the trailer
   * vary from collection to collection. Filter them out to avoid incorrect hash failures.
   * A child could choose to avoid this entirely by setting it to org.lockss.util.Default
   * or they could write their own child implementation
   */
  @Override
  public void transform(ArchivalUnit au,
                        PdfDocument pdfDocument)
      throws PdfException {
    if (doRemoveAllDocumentInfo()) {
      doAllBaseTransforms(pdfDocument);
    } else {
      doBaseTransforms(pdfDocument);
    }
      
  }

//
//  TODO - after Sept 29,2016 - remove static from the following methods
//    subclassed version of these would not take precedent
//    no subclasses currently exist that use these, should be okay to clean up
  public static boolean doRemoveAllDocumentInfo() {
    return false;
  }
  
  public static void doBaseTransforms(PdfDocument pdfDocument) 
   throws PdfException {
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetMetadata();
    PdfUtil.normalizeTrailerId(pdfDocument);
  }
  
  public static void doAllBaseTransforms(PdfDocument pdfDocument)
  throws PdfException {
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetMetadata();
    pdfDocument.unsetCreator();
    pdfDocument.unsetLanguage();
    pdfDocument.unsetProducer();
    pdfDocument.unsetAuthor();
    pdfDocument.unsetTitle();
    pdfDocument.unsetSubject();
    pdfDocument.unsetKeywords();
  }

}

/*
 * NOTE - possible future improvment
 * Instead of remembering all the various things to remove (doAllBaseTransforms(
 * when doing maximal information removal, 
 * override the definition of output of document information to do nothing
 * it's not clear how I would turn this on and off depending on child whim
 *   /* FIXME 1.67 */
  //  @Override
  //  public PdfTransform<PdfDocument> getDocumentTransform(ArchivalUnit au, OutputStream os) {
  //    return new BaseDocumentExtractingTransform(os) {
  //      @Override
  //      public void outputDocumentInformation() throws PdfException {
  //        // Intentionally left blank
  //      }
  //    };
  //  }
  /* end FIXME 1.67 */
