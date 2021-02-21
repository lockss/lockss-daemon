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

package org.lockss.pdf;

import java.io.*;

/**
 * <p>
 * Provides access to a PDF implementation.
 * </p>
 * <p>
 * The PDF API acts as a facade; the result of intermingling objects from
 * multiple PDF implementations is undefined and will likely not work at all.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see DefaultPdfDocumentFactory
 */
public interface PdfDocumentFactory {
  
  /**
   * <p>
   * Returns an instance of a PDF token factory suitable for this PDF
   * implementation.
   * </p>
   * 
   * @return A PDF token factory instance.
   * @since 1.70
   */
  PdfTokenFactory getTokenFactory();
  
  /**
   * <p>
   * Interprets the bytes in the given input stream as a PDF document.
   * </p>
   * <p>
   * The input stream may be closed by this call.
   * </p>
   * <p>
   * It is possible that the returned document is not fully parsed due to lazy
   * parsing, caching, and other implementation-dependent details, so full
   * parsing of parts of the document may not be triggered until these parts are
   * accessed.
   * </p>
   * <p>
   * If parsing fails and an exception is thrown, the state of the input stream
   * is undefined.
   * </p>
   * 
   * @param pdfInputStream
   *          A PDF document as an input stream
   * @return A parsed PDF document
   * @throws IOException
   *           If parsing fails at the I/O level
   * @throws PdfCryptographyException
   *           If parsing fails at the cryptography level
   * @throws PdfException
   *           If parsing fails at the PDF level
   * @since 1.70
   */
  PdfDocument makeDocument(InputStream pdfInputStream)
      throws IOException,
             PdfCryptographyException,
             PdfException;

  /**
   * <p>
   * Makes a new PDF document instance from the given PDF document factory
   * instance and from the given PDF document object data suitable for this PDF
   * implementation.
   * </p>
   * 
   * @param pdfDocumentFactory
   *          The PDF document factory the document comes from
   * @param pdfDocumentObject
   *          The PDF document object data (implementation-dependent)
   * @return A PDF document instance suitable for this PDf implementation
   * @throws PdfException
   *           If an error occurs
   * @since 1.70
   */
  PdfDocument makeDocument(PdfDocumentFactory pdfDocumentFactory,
                           Object pdfDocumentObject)
      throws PdfException;
  
  /**
   * <p>
   * Makes a new PDF page instance from the given PDF document instance and from
   * the given PDF page object data suitable for this PDF implementation.
   * </p>
   * 
   * @param pdfDocument
   *          The PDF document the page comes from
   * @param pdfPageObject
   *          The PDF page object data (implementation-dependent)
   * @return A PDF page instance suitable for this PDF implementation
   * @throws PdfException
   *           If an error occurs
   * @since 1.70
   */
  PdfPage makePage(PdfDocument pdfDocument,
                   Object pdfPageObject)
      throws PdfException;

  /**
   * <p>
   * Makes a new PDF token stream instance from the given PDF page instance and
   * from the given PDF token stream object data suitable for this PDF
   * implementation.
   * </p>
   * 
   * @param pdfPage
   *          The PDF page the tokens tream comes from
   * @param pdfTokenStreamObject
   *          The PDF token stream object data (implementation-dependent)
   * @return A PDF token stream instance suitable for this PDF implementation
   * @throws PdfException
   *           If an error occurs
   * @since 1.70
   */
  PdfTokenStream makeTokenStream(PdfPage pdfPage,
                                 Object pdfTokenStreamObject)
      throws PdfException;
  
}
