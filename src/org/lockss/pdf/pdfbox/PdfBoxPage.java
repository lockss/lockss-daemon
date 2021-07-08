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

package org.lockss.pdf.pdfbox;

import java.io.*;
import java.util.*;

import org.apache.commons.collections4.iterators.*;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.lockss.pdf.*;
import org.lockss.util.Logger;

/**
 * <p>
 * A {@link PdfPage} implementation based on PDFBox 1.6.0.
 * </p>
 * <p>
 * This class acts as an adapter for the {@link PDPage} class.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see PdfBoxDocumentFactory
 */
public class PdfBoxPage implements PdfPage {

  /**
   * <p>
   * Logger for use by this class.
   * </p>
   * 
   * @since 1.56
   */
  private static final Logger log = Logger.getLogger(PdfBoxPage.class);
  
  /**
   * <p>
   * The parent {@link PdfBoxDocument} instance.
   * </p>
   * 
   * @since 1.56
   */
  protected final PdfBoxDocument pdfBoxDocument;

  /**
   * <p>
   * The {@link PDPage) instance this instance represents.
   * </p>
   * 
   * @since 1.56
   */
  protected final PDPage pdPage;

  /**
   * <p>
   * Constructor.
   * </p>
   * 
   * @param pdfBoxDocument The parent {@link PdfBoxDocument} instance.
   * @param pdPage The {@link PDPage} instance underpinning this PDF
   *          page.
   * @since 1.56
   */
  public PdfBoxPage(PdfBoxDocument pdfBoxDocument,
                    PDPage pdPage) {
    this.pdfBoxDocument = pdfBoxDocument;
    this.pdPage = pdPage;
  }

  @Override
  public List<PdfBoxToken> getAnnotations() throws PdfException {
    /*
     * IMPLEMENTATION NOTE
     * 
     * Annotations are just dictionaries, but because there are many
     * types, the PDFBox API defines a vast hierarchy of objects to
     * represent them. At this time, this is way too much detail for
     * this API, because only one type of annotation has a foreseeable
     * use case (the Link type). So for now, we are only representing
     * annotations as the dictionaries they are by circumventing the
     * PDAnnotation factory call in getAnnotations() (see PDFBox 1.8.16
     * PDAnnotation createAnnotation(), PDPage getAnnotations()).
     */
    COSDictionary pageDictionary = pdPage.getCOSObject();
    COSArray annots = (COSArray)pageDictionary.getDictionaryObject(COSName.ANNOTS);
    if (annots == null) {
      return new ArrayList<PdfBoxToken>();
    }
    List<PdfBoxToken> ret = new ArrayList<>(annots.size());
    for (int i = 0 ; i < annots.size() ; ++i) {
      ret.add(PdfBoxToken.convertOne(annots.getObject(i)));
    }
    List<PdfToken> ret = new ArrayList<>(annots.size());
    for (int i = 0 ; i < annots.size() ; ++i) {
      ret.add(PdfBoxTokens.convertOne(annots.getObject(i)));
    }
    return ret;
  }

  @Override
  public Iterator<InputStream> getByteStreamIterator() throws PdfException {
    return new PdfBoxTokenStreamIteratorHelper<InputStream>(new PdfBoxTokenStreamIterator(pdPage)) {
      @Override
      protected InputStream findNext(PdfBoxTokenStreamIterator iterator,
                                     PdfBoxOperandsAndOperator oao)
          throws Exception {
        List<PdfBoxToken> operands = oao.getOperands();
        PdfBoxToken operator = oao.getOperator();
        switch (operator.getOperator()) {
          case PdfOpcodes.BEGIN_INLINE_IMAGE:
          case PdfOpcodes.BEGIN_INLINE_IMAGE_DATA:
            return new ByteArrayInputStream(((PdfBoxToken.Op)operator).getImageData());
          case PdfOpcodes.DRAW_OBJECT:
            // See PDFBox 2.0.22 DrawObject (the one in contentstream.operator.graphics)
            if (operands.size() >= 1) {
              PdfBoxToken operand0 = operands.get(0);
              if (operand0.isName()) {
                PDXObject pdxObject = iterator.getPdResources().getXObject(((PdfBoxToken.Nam)operand0).toPdfBoxObject());
                if (pdxObject instanceof PDImageXObject) {
                  // PDFBox 2.0.22 ExtractImages (in tools JAR) tracks xobject.getCOSObject() to avoid duplicate images
                  if (processed.add(pdxObject.getCOSObject())) {
                    return ((PDImageXObject)pdxObject).getStream().getCOSObject().createRawInputStream();
                  }
                }
                else if (pdxObject instanceof PDFormXObject) {
                  if (processed.add(pdxObject.getCOSObject())) {
                    // PDFBox 2.0.22 ExtractImages (in tools JAR) doesn't keep track of non-PDImageXObject PDXObject but why not?
                    iterators.add(new PdfBoxTokenStreamIterator((PDFormXObject)pdxObject, pdPage));
                  }
                }
                // FIXME isn't PDPostScriptXObject a byte stream?
              }
            }
            break;
          default:
            // Intentionally left blank
            break;
        }
        return null;
      }
    };
  }
  
  @Override
  public PdfBoxDocument getDocument() {
    return pdfBoxDocument;
  }
  
  @Override
  public PdfBoxTokenStream getPageTokenStream() throws PdfException {
    try {
      return pdfBoxDocument.getDocumentFactory().makePageTokenStream(this, pdPage.getContents());
    }
    catch (IOException ioe) {
      throw new PdfException("Failed to get the page content stream", ioe);
    }
  }
  
  @Override
  public void setAnnotations(List<PdfToken> annotations) {
    // FIXME Possibly incorrect, based on the 1.76.0-era bug fix in getAnnotations()
    pdPage.getCOSDictionary().setItem(COSName.ANNOTS, (COSArray)Arr.of(annotations).toPdfBoxObject());
  }

  /**
   * @since 1.76
   */
  public PDPage getPdPage() {
    return pdPage;
  }
  
  @Override // Just for the covariant default method return type
  public Iterable<PdfBoxTokenStream> getTokenStreamIterable() throws PdfException {
    return (Iterable<PdfBoxTokenStream>)PdfPage.super.getTokenStreamIterable();
  }
  
  @Override
  public Iterator<PdfBoxTokenStream> getTokenStreamIterator() throws PdfException {
    Iterator<PdfBoxTokenStream> others = new PdfBoxTokenStreamIteratorHelper<PdfBoxTokenStream>(new PdfBoxTokenStreamIterator(pdPage)) {
      @Override
      protected PdfBoxTokenStream findNext(PdfBoxTokenStreamIterator iterator,
                                           PdfBoxOperandsAndOperator oao)
          throws Exception {
        List<PdfBoxToken> operands = oao.getOperands();
        PdfBoxToken operator = oao.getOperator();
        switch (operator.getOperator()) {
          case PdfOpcodes.DRAW_OBJECT:
            if (operands.size() >= 1) {
              PdfToken operand0 = operands.get(0);
              if (operand0.isName()) {
                COSName cosName = ((PdfBoxToken.Nam)operand0).toPdfBoxObject();
                PDXObject pdxObject = iterator.getPdResources().getXObject(cosName);
                if (pdxObject instanceof PDFormXObject) {
                  // FIXME what about the subtypes of PDFormXObject?
                  if (processed.add(pdxObject.getCOSObject())) {
                    PDFormXObject pdFormXObject = (PDFormXObject)pdxObject;
                    iterators.add(new PdfBoxTokenStreamIterator(pdFormXObject, pdPage));
                    return pdfBoxDocument.getDocumentFactory().makeXObjectTokenStream(PdfBoxPage.this, Arrays.asList(pdFormXObject, cosName.getName(), iterator.getPdResources(), pdFormXObject.getResources()));
                  }
                }
              }
            }
            break;
          default:
            // Intentionally left blank
            break;
        }
        return null;
      }
    };
    return new IteratorChain<>(new SingletonIterator<>(getPageTokenStream()), others);
  }
  
  @Override // Just for the covariant default method return type
  public List<PdfBoxTokenStream> getTokenStreamList() throws PdfException {
    return (List<PdfBoxTokenStream>)PdfPage.super.getTokenStreamList();
  }

  @Override
  public void setAnnotations(List<PdfToken> annotations) throws PdfException {
    List<PDAnnotation> result = null;
    if (annotations != null) {
      result = new ArrayList<>(annotations.size());
      for (PdfToken annot : annotations) {
        try {
          result.add(PDAnnotation.createAnnotation(((PdfBoxToken.Dic)annot).toPdfBoxObject()));
        }
        catch (IOException ioe) {
          throw new PdfException("Error converting annotations", ioe);
        }
      }
    }
    pdPage.setAnnotations(result);
  }

}
