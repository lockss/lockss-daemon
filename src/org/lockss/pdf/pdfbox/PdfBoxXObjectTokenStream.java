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

import java.io.IOException;
import java.util.*;

import org.apache.pdfbox.contentstream.PDContentStream;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.lockss.pdf.*;
import org.lockss.util.Logger;

/**
 * <p>
 * A {@link PdfBoxTokenStream} implementation suitable for a form XObject, based
 * on PDFBox 1.6.0.
 * </p>
 * <p>
 * This class acts as an adapter for the {@link PDFormXObject} class.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see PdfBoxDocumentFactory
 */
public class PdfBoxXObjectTokenStream extends PdfBoxTokenStream {

  /**
   * <p>
   * A logger for use by this class.
   * </p>
   * 
   * @since 1.67.6
   */
  private static final Logger log = Logger.getLogger(PdfBoxXObjectTokenStream.class);
  
  /**
   * <p>
   * The {@link PDFormXObject} instance underpinning this instance.
   * </p>
   * 
   * @since 1.56
   */
  protected PDFormXObject pdFormXObject;
  
  /**
   * <p>
   * The form's resources, either its own ({@link PDFormXObject#getResources()})
   * or those of an enclosing context.
   * </p>
   * 
   * @since 1.67.6
   */
  protected PDResources parentResources;
  
  /**
   * <p>
   * The form's own resources.
   * </p>
   * 
   * @since 1.67.6
   */
  protected PDResources ownResources;
  
  /**
   * @since 1.76
   */
  protected String name;
  
  /**
   * <p>
   * Builds a new instance using the given parent and own resources.
   * </p>
   * 
   * @param pdfBoxPage
   *          The parent PDF page.
   * @param pdXObjectForm
   *          The {@link PDFormXObject} being wrapped.
   * @param parentResources
   *          The parent resources.
   * @param ownResources
   *          The form's own resources.
   * @since 1.67.6
   */
  public PdfBoxXObjectTokenStream(PdfBoxPage pdfBoxPage,
                                  PDFormXObject pdXObjectForm,
                                  String name,
                                  PDResources parentResources,
                                  PDResources ownResources) {
    super(pdfBoxPage);
    this.pdFormXObject = pdXObjectForm;
    this.name = name;
    this.parentResources = parentResources;
    this.ownResources = ownResources;
  }

  @Override
  public PdfBoxTokenStreamIterator getOperandsAndOperatorIterator() throws PdfException {
    return new PdfBoxTokenStreamIterator(pdFormXObject, pdfBoxPage.getPdPage());
  }
  
  @Override
  public void setTokens(Iterator<? extends PdfToken> tokenIterator) throws PdfException {
    PDStream newPdStream = makePdStreamFromTokens(tokenIterator);
    COSStream cosStream = newPdStream.getCOSObject();
    cosStream.setName(COSName.SUBTYPE, COSName.FORM.getName());
    try {
      PDXObject pdxObject = PDXObject.createXObject(cosStream, null /* FIXME which resources? */);
      parentResources.put(COSName.getPDFName(name), pdxObject);
    }
    catch (IOException ioe) {
      throw new PdfException(ioe);
    }
  }
  
  @Override
  protected PDContentStream getPdContentStream() {
    return pdFormXObject;
  }
  
  @Override
  protected PDResources getStreamResources() {
    return ownResources != null ? ownResources : parentResources;
  }
  
}
