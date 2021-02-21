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
import java.util.List;

import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.lockss.pdf.*;
import org.lockss.util.IOUtil;

/**
 * <p>
 * A {@link PdfDocumentFactory} implementation based on PDFBox 1.8.11.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see <a href="http://pdfbox.apache.org/">PDFBox site</a>
 */
public class PdfBoxDocumentFactory implements PdfDocumentFactory {

  @Override
  public PdfBoxToken.Factory getTokenFactory() {
    return PdfBoxToken.getFactory();
  }
  
  @Override
  public PdfBoxDocument makeDocument(InputStream pdfInputStream)
      throws IOException,
             PdfCryptographyException,
             PdfException {
    try {
      PDDocument pdDocument = makePdDocument(pdfInputStream);
      processAfterParse(pdDocument);
      return makeDocument(this, pdDocument);
    }
    catch (InvalidPasswordException ipe) {
      throw new PdfCryptographyException(ipe);
    }
    catch (IOException ioe) {
      throw new PdfException(ioe);
    }
    finally {
      // FIXME
      // PDFBox normally closes the input stream, but just in case
      IOUtil.safeClose(pdfInputStream);
    }
  }
  
  @Override
  public PdfBoxDocument makeDocument(PdfDocumentFactory pdfDocumentFactory,
                                     Object pdfDocumentObject)
      throws PdfException {
    return new PdfBoxDocument(this, (PDDocument)pdfDocumentObject);
  }

  @Override
  public PdfBoxPage makePage(PdfDocument pdfDocument,
                             Object pdfPageObject)
      throws PdfException {
    return new PdfBoxPage((PdfBoxDocument)pdfDocument, (PDPage)pdfPageObject);
  }
  
  /**
   * <p>
   * Makes a new PDF page token stream instance from the given PDF page instance
   * and from the given PDF token stream object data suitable for this PDF
   * implementation.
   * </p>
   * 
   * @param pdfPage
   *          The PDF page the XObject token stream comes from
   * @param pdfTokenStreamObject
   *          The PDF token stream object data (implementation-dependent)
   * @return A PDF page token stream instance suitable for this PDF
   *         implementation
   * @throws PdfException
   *           If an error occurs
   * @since 1.70
   */
  public PdfBoxTokenStream makePageTokenStream(PdfPage pdfPage,
                                                   Object pdfTokenStreamObject)
      throws PdfException {
    return new PdfBoxPageTokenStream((PdfBoxPage)pdfPage, null);
  }

  @Override
  public PdfBoxTokenStream makeTokenStream(PdfPage pdfPage,
                                           Object pdfTokenStreamObject)
      throws PdfException {
    throw new UnsupportedOperationException("Not supported by this class; use makePageTokenStream and makeXObjectTokenStream instead");
  }
  
  /**
   * <p>
   * Makes a new PDF XObject token stream instance from the given PDF page
   * instance and from the given PDF token stream object data suitable for this
   * PDF implementation (a list made of the {@link PDXObjectForm} instance, the
   * parent {@link PDResources} instance and the proper {@link PDResources}
   * instance).
   * </p>
   * 
   * @param pdfPage
   *          The PDF page the XObject token stream comes from
   * @param pdfTokenStreamObject
   *          The PDF token stream object data (implementation-dependent)
   * @return A PDF XObject token stream instance suitable for this PDF
   *         implementation
   * @throws PdfException
   *           If an error occurs
   * @since 1.70
   */
  public PdfBoxXObjectTokenStream makeXObjectTokenStream(PdfPage pdfPage,
                                                         Object pdfTokenStreamObject)
      throws PdfException {
    List<?> varargs = (List<?>)pdfTokenStreamObject;
    return new PdfBoxXObjectTokenStream((PdfBoxPage)pdfPage,
                                        (PDFormXObject)varargs.get(0),
                                        (PDResources)varargs.get(1),
                                        (PDResources)varargs.get(2));
  }
  
  /**
   * 
   * @param pdfInputStream
   * @return
   * @throws IOException
   * @since 1.70
   */
  protected PDDocument makePdDocument(InputStream pdfInputStream)
      throws InvalidPasswordException, IOException {
    // FIXME: memory management
    return PDDocument.load(pdfInputStream);
  }
  
  /**
   * <p>
   * Override this method to alter the processing of the {@link PDDocument}
   * instance after it has been parsed by {@link PDFParser#parse()}.
   * </p>
   * 
   * @param pdDocument
   *          A freshly parsed {@link PDDocument} instance
   * @throws IOException
   *           if an I/O exception is thrown
   * @since 1.67
   */
  protected void processAfterParse(PDDocument pdDocument)
      throws IOException {
    pdDocument.setAllSecurityToBeRemoved(true);
  }
  
}
